package com.tacz.guns.cosmetic.client.renderer;

import com.tacz.guns.GunMod;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeychainModelCache {
    private static final Map<ResourceLocation, BedrockModel> MODELS = new ConcurrentHashMap<>();

    private KeychainModelCache() {}

    @Nullable
    public static BedrockModel get(KeychainDefinition keychain) {
        return MODELS.computeIfAbsent(keychain.getKeychainId(), id -> {
            try {
                BedrockModelPOJO pojo = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(keychain.getModel());
                if (pojo == null) {
                    return null;
                }
                return new BedrockModel(pojo, BedrockVersion.NEW);
            } catch (Exception e) {
                GunMod.LOGGER.warn("Failed to load keychain model: {}", id, e);
                return null;
            }
        });
    }

    public static void clear() {
        MODELS.clear();
    }
}
