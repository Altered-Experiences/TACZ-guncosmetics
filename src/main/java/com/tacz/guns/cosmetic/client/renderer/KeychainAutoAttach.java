package com.tacz.guns.cosmetic.client.renderer;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatically determines the best attachment point for a keychain on any gun model.
 *
 * Strategy:
 * 1. Try preferred bone names (stock variants, grip, body, main gun bone).
 * 2. If none found, walk the model tree to find a "leaf-ish" bone that has geometry
 *    (cubes) and is at the rear/bottom of the model — typical keychain spot.
 * 3. Cache results per gun ID to avoid re-scanning every frame.
 *
 * Also computes a smart offset so the keychain hangs off the bottom of the chosen bone.
 */
public final class KeychainAutoAttach {

    /**
     * Result of the auto-attachment computation.
     */
    public static class AttachResult {
        /** The bone to attach to */
        public final BedrockPart bone;
        /** Computed offset (in model units, will be /16 by caller) */
        public final Vector3f offset;
        /** Computed rotation (degrees) */
        public final Vector3f rotation;
        /** Computed scale */
        public final Vector3f scale;

        public AttachResult(BedrockPart bone, Vector3f offset, Vector3f rotation, Vector3f scale) {
            this.bone = bone;
            this.offset = offset;
            this.rotation = rotation;
            this.scale = scale;
        }
    }

    /**
     * Bones we NEVER attach to — these are special-purpose nodes.
     */
    private static final Set<String> BLACKLIST = Set.of(
            "camera", "view", "views", "positioning",
            "iron_view", "idle_view", "refit_view",
            "thirdperson_hand", "fixed", "ground",
            "lefthand_pos", "righthand_pos",
            "lefthand", "righthand",
            "muzzle_flash", "muzzle_pos",
            "shell", "constraint",
            "bullet_in_barrel", "bullet_in_mag", "bullet_chain",
            "bullet", "additional_magazine",
            "scope_pos", "laser_pos", "laser_beam",
            "muzzle_default",
            "attachment_adapter"
    );

    /**
     * Preferred bone names, in priority order. The first one found wins.
     * These are the most common "rear body" bones across TACZ guns.
     */
    private static final List<String> PREFERRED_BONES = List.of(
            // Stock variants — ideal for hanging keychains
            "stock_default", "stock", "stock_pos",
            // Grip area — also good
            "grip", "grip2", "grip_lower",
            // Trigger guard — visible area
            "trigger_guard", "trigger_guard2",
            // Magazine area — visible, rear-ish
            "magazine",
            // Main body bones (fallback)
            "body", "lower", "lower2",
            // Root — last resort
            "root"
    );

    /**
     * Suffixes that indicate a bone is a positional/functional node, not geometry.
     */
    private static final List<String> SKIP_SUFFIXES = List.of(
            "_pos", "_view", "_default", "_illuminated", "_origin"
    );

    /** Cache: gunId -> AttachResult */
    private static final Map<ResourceLocation, AttachResult> CACHE = new ConcurrentHashMap<>();

    private KeychainAutoAttach() {}

    /**
     * Clears the cache (called on resource reload / disconnect).
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Find the best attachment point for a keychain on the given gun model.
     *
     * @param gunId    The gun's ResourceLocation (used for caching)
     * @param gunModel The gun's BedrockGunModel
     * @return An AttachResult with the bone and computed transform, or null if nothing found.
     */
    @Nullable
    public static AttachResult findAttachPoint(ResourceLocation gunId, BedrockGunModel gunModel) {
        AttachResult cached = CACHE.get(gunId);
        if (cached != null) {
            // Verify the bone is still valid (model could have been reloaded)
            if (gunModel.getNode(cached.bone.name) == cached.bone) {
                return cached;
            }
            CACHE.remove(gunId);
        }

        AttachResult result = computeAttachPoint(gunModel);
        if (result != null) {
            CACHE.put(gunId, result);
            GunCosmeticsMod.LOGGER.debug("Auto-attach keychain for {}: bone='{}', offset={}",
                    gunId, result.bone.name, result.offset);
        }
        return result;
    }

    @Nullable
    private static AttachResult computeAttachPoint(BedrockGunModel gunModel) {
        // Strategy 1: Try preferred bone names
        for (String boneName : PREFERRED_BONES) {
            BedrockPart bone = gunModel.getNode(boneName);
            if (bone != null && bone.visible) {
                return createResult(bone, boneName);
            }
        }

        // Strategy 2: Find the main gun body bone.
        // In TACZ, the main gun geometry bone is typically a direct child of "root"
        // that is NOT one of the known functional bones (lefthand, righthand, etc.)
        BedrockPart root = gunModel.getNode("root");
        if (root != null) {
            BedrockPart mainGunBone = findMainGunBone(root, gunModel);
            if (mainGunBone != null) {
                // Try to find a "lower" or "grip" child within the main gun bone
                BedrockPart child = findBestChildBone(mainGunBone);
                if (child != null) {
                    return createResult(child, child.name);
                }
                // Use the main gun bone itself
                return createResult(mainGunBone, mainGunBone.name);
            }
        }

        // Strategy 3: Scan all bones for the first one with geometry (cubes) that isn't blacklisted
        Map<String, com.tacz.guns.client.resource.pojo.model.BonesItem> allBones = gunModel.getIndexBones();
        for (Map.Entry<String, com.tacz.guns.client.resource.pojo.model.BonesItem> entry : allBones.entrySet()) {
            String name = entry.getKey();
            if (isBlacklisted(name)) continue;

            BedrockPart bone = gunModel.getNode(name);
            if (bone != null && bone.visible && !bone.cubes.isEmpty()) {
                return createResult(bone, name);
            }
        }

        // Strategy 4: Absolute fallback — use root
        if (root != null) {
            return createResult(root, "root");
        }

        GunCosmeticsMod.LOGGER.warn("Could not find any suitable bone for keychain attachment");
        return null;
    }

    /**
     * Find the main gun geometry bone — usually a named bone like "AKM", "Deagle", etc.
     * It's typically a child (or grandchild) of "root" that isn't a known functional bone
     * and has substantial geometry or children.
     */
    @Nullable
    private static BedrockPart findMainGunBone(BedrockPart root, BedrockGunModel gunModel) {
        // Direct children of root
        for (BedrockPart child : root.children) {
            if (child.name == null) continue;
            if (isBlacklisted(child.name)) continue;
            if (hasSkipSuffix(child.name)) continue;

            // Common wrapper bones — look deeper
            if (child.name.startsWith("gun_and_") || child.name.startsWith("bone")) {
                for (BedrockPart grandchild : child.children) {
                    if (grandchild.name == null) continue;
                    if (isBlacklisted(grandchild.name)) continue;
                    if (hasSkipSuffix(grandchild.name)) continue;
                    // If it has children or cubes, it's likely the main gun bone
                    if (!grandchild.children.isEmpty() || !grandchild.cubes.isEmpty()) {
                        return grandchild;
                    }
                }
            }

            // Check if this child has significant content
            if (countGeometryDescendants(child) > 3) {
                return child;
            }
        }

        // Fallback: any root child with the most descendants
        BedrockPart best = null;
        int bestCount = 0;
        for (BedrockPart child : root.children) {
            if (child.name == null || isBlacklisted(child.name)) continue;
            int count = countGeometryDescendants(child);
            if (count > bestCount) {
                bestCount = count;
                best = child;
            }
        }
        return best;
    }

    /**
     * Within a main gun bone, find the best sub-bone to attach a keychain to.
     * Prefers "lower", "grip", "trigger_guard" children — the bottom/rear of the gun.
     */
    @Nullable
    private static BedrockPart findBestChildBone(BedrockPart parent) {
        // Priority sub-bone names
        String[] subPriority = {"grip", "grip2", "grip_lower", "lower", "lower2",
                "trigger_guard", "trigger_guard2", "stock_default", "stock"};

        for (String name : subPriority) {
            BedrockPart found = findChildRecursive(parent, name, 3);
            if (found != null && found.visible) {
                return found;
            }
        }
        return null;
    }

    /**
     * Recursively search for a named child within maxDepth levels.
     */
    @Nullable
    private static BedrockPart findChildRecursive(BedrockPart parent, String targetName, int maxDepth) {
        if (maxDepth <= 0) return null;
        for (BedrockPart child : parent.children) {
            if (targetName.equals(child.name)) return child;
            BedrockPart found = findChildRecursive(child, targetName, maxDepth - 1);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Count how many descendants have actual geometry (cubes).
     */
    private static int countGeometryDescendants(BedrockPart part) {
        int count = part.cubes.isEmpty() ? 0 : 1;
        for (BedrockPart child : part.children) {
            count += countGeometryDescendants(child);
        }
        return count;
    }

    private static boolean isBlacklisted(String name) {
        if (name == null) return true;
        String lower = name.toLowerCase(Locale.ROOT);
        if (BLACKLIST.contains(lower)) return true;
        // Also skip known prefixes
        if (lower.startsWith("refit_")) return true;
        if (lower.startsWith("mag_extended")) return true;
        if (lower.startsWith("mag_additional")) return true;
        return false;
    }

    private static boolean hasSkipSuffix(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        for (String suffix : SKIP_SUFFIXES) {
            if (lower.endsWith(suffix)) return true;
        }
        return false;
    }

    /**
     * Create the final AttachResult with a smart offset computed from the bone's properties.
     * The keychain should hang from the bottom of the bone.
     */
    private static AttachResult createResult(BedrockPart bone, String boneName) {
        Vector3f offset;
        Vector3f rotation = new Vector3f(0, 0, 0);
        Vector3f scale = new Vector3f(0.35f, 0.35f, 0.35f);

        String lower = boneName.toLowerCase(Locale.ROOT);

        if (lower.contains("stock")) {
            // Stock — hang off the end
            offset = new Vector3f(0, -2.0f, -1.0f);
            rotation = new Vector3f(0, 0, 15);
        } else if (lower.contains("grip")) {
            // Grip — hang below
            offset = new Vector3f(0, -3.0f, 0);
            rotation = new Vector3f(0, 0, 10);
        } else if (lower.contains("trigger")) {
            // Trigger guard — hang below
            offset = new Vector3f(0, -2.5f, 0);
        } else if (lower.contains("magazine")) {
            // Magazine — hang off the bottom of the mag
            offset = new Vector3f(0, -4.0f, 0);
            rotation = new Vector3f(5, 0, 0);
        } else if (lower.contains("lower")) {
            // Lower receiver — hang below grip area
            offset = new Vector3f(0, -2.5f, 0.5f);
            rotation = new Vector3f(0, 0, 5);
        } else if (lower.equals("root")) {
            // Root — use a generous offset downward
            offset = new Vector3f(0, -3.0f, -2.0f);
        } else {
            // Unknown bone — generic offset below
            offset = new Vector3f(0, -2.5f, 0);
            rotation = new Vector3f(0, 0, 5);
        }

        return new AttachResult(bone, offset, rotation, scale);
    }
}
