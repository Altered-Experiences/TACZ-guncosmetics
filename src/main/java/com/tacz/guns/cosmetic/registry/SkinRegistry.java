package com.tacz.guns.cosmetic.registry;

import com.tacz.guns.cosmetic.data.SkinDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for all loaded skin definitions.
 * Thread-safe — populated on server thread, read on render thread.
 */
public final class SkinRegistry {

    private static final Map<ResourceLocation, SkinDefinition> REGISTRY = new ConcurrentHashMap<>();

    private SkinRegistry() {}

    public static void register(SkinDefinition skin) {
        REGISTRY.put(skin.getSkinId(), skin);
    }

    public static Optional<SkinDefinition> get(ResourceLocation skinId) {
        return Optional.ofNullable(REGISTRY.get(skinId));
    }

    /**
     * Returns all skins applicable to the given gun:
     * weapon-specific skins matching the gun ID + all universal skins.
     */
    public static List<SkinDefinition> getForGun(ResourceLocation gunId) {
        return REGISTRY.values().stream()
                .filter(skin -> skin.isApplicableTo(gunId))
                .collect(Collectors.toList());
    }

    public static List<SkinDefinition> getAll() {
        return new ArrayList<>(REGISTRY.values());
    }

    public static int size() {
        return REGISTRY.size();
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
