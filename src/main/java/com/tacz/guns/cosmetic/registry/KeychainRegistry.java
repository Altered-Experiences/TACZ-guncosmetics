package com.tacz.guns.cosmetic.registry;

import com.tacz.guns.cosmetic.data.KeychainDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all loaded keychain definitions.
 */
public final class KeychainRegistry {

    private static final Map<ResourceLocation, KeychainDefinition> REGISTRY = new ConcurrentHashMap<>();

    private KeychainRegistry() {}

    public static void register(KeychainDefinition keychain) {
        REGISTRY.put(keychain.getKeychainId(), keychain);
    }

    public static Optional<KeychainDefinition> get(ResourceLocation keychainId) {
        return Optional.ofNullable(REGISTRY.get(keychainId));
    }

    public static List<KeychainDefinition> getAll() {
        return new ArrayList<>(REGISTRY.values());
    }

    public static int size() {
        return REGISTRY.size();
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
