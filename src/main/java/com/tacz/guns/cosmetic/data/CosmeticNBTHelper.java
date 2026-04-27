package com.tacz.guns.cosmetic.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Utility class for reading/writing cosmetic NBT data on gun ItemStacks.
 *
 * NBT layout:
 *   tag.GunCosmetics.SkinId          — string
 *   tag.GunCosmetics.KeychainId      — string
 *   tag.GunCosmetics.KeychainTransform — compound (OffsetX/Y/Z, RotX/Y/Z, ScaleX/Y/Z)
 */
public final class CosmeticNBTHelper {

    private static final String TAG_ROOT = "GunCosmetics";
    private static final String TAG_SKIN_ID = "SkinId";
    private static final String TAG_KEYCHAIN_ID = "KeychainId";
    private static final String TAG_KEYCHAIN_TRANSFORM = "KeychainTransform";

    private CosmeticNBTHelper() {}

    // ── Skin ──────────────────────────────────────────

    @Nullable
    public static ResourceLocation getSkinId(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(TAG_SKIN_ID)) return null;
        return ResourceLocation.tryParse(root.getString(TAG_SKIN_ID));
    }

    public static void setSkinId(ItemStack stack, @Nullable ResourceLocation skinId) {
        if (skinId == null) {
            CompoundTag root = getRoot(stack);
            if (root != null) {
                root.remove(TAG_SKIN_ID);
                cleanupRoot(stack, root);
            }
        } else {
            getOrCreateRoot(stack).putString(TAG_SKIN_ID, skinId.toString());
        }
    }

    // ── Keychain ──────────────────────────────────────

    @Nullable
    public static ResourceLocation getKeychainId(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(TAG_KEYCHAIN_ID)) return null;
        return ResourceLocation.tryParse(root.getString(TAG_KEYCHAIN_ID));
    }

    public static void setKeychainId(ItemStack stack, @Nullable ResourceLocation keychainId) {
        if (keychainId == null) {
            CompoundTag root = getRoot(stack);
            if (root != null) {
                root.remove(TAG_KEYCHAIN_ID);
                root.remove(TAG_KEYCHAIN_TRANSFORM);
                cleanupRoot(stack, root);
            }
        } else {
            getOrCreateRoot(stack).putString(TAG_KEYCHAIN_ID, keychainId.toString());
        }
    }

    // ── Keychain Transform ────────────────────────────

    @Nullable
    public static CompoundTag getKeychainTransform(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(TAG_KEYCHAIN_TRANSFORM)) return null;
        return root.getCompound(TAG_KEYCHAIN_TRANSFORM);
    }

    public static void setKeychainTransform(ItemStack stack, String bone, float ox, float oy, float oz,
                                            float rx, float ry, float rz,
                                            float sx, float sy, float sz) {
        CompoundTag transform = new CompoundTag();
        if (bone != null && !bone.isEmpty()) {
            transform.putString("Bone", bone);
        }
        transform.putFloat("OffsetX", ox);
        transform.putFloat("OffsetY", oy);
        transform.putFloat("OffsetZ", oz);
        transform.putFloat("RotX", rx);
        transform.putFloat("RotY", ry);
        transform.putFloat("RotZ", rz);
        transform.putFloat("ScaleX", sx);
        transform.putFloat("ScaleY", sy);
        transform.putFloat("ScaleZ", sz);
        getOrCreateRoot(stack).put(TAG_KEYCHAIN_TRANSFORM, transform);
    }

    public static void removeKeychainTransform(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root != null) {
            root.remove(TAG_KEYCHAIN_TRANSFORM);
            cleanupRoot(stack, root);
        }
    }

    // ── Internal ──────────────────────────────────────

    @Nullable
    private static CompoundTag getRoot(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ROOT)) return null;
        return tag.getCompound(TAG_ROOT);
    }

    private static CompoundTag getOrCreateRoot(ItemStack stack) {
        return stack.getOrCreateTag().getCompound(TAG_ROOT).isEmpty()
                ? createRoot(stack)
                : stack.getTag().getCompound(TAG_ROOT);
    }

    private static CompoundTag createRoot(ItemStack stack) {
        CompoundTag root = new CompoundTag();
        stack.getOrCreateTag().put(TAG_ROOT, root);
        return root;
    }

    private static void cleanupRoot(ItemStack stack, CompoundTag root) {
        if (root.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(TAG_ROOT);
            }
        }
    }
}
