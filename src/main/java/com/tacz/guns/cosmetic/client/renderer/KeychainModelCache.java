package com.tacz.guns.cosmetic.client.renderer;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeychainModelCache {
    private static final Map<ResourceLocation, BedrockModel> MODELS = new ConcurrentHashMap<>();

    private KeychainModelCache() {}

    @Nullable
    public static BedrockModel get(KeychainDefinition keychain) {
        byte[] modelData = keychain.getModelData();
        if (modelData == null || modelData.length == 0) return null;

        return MODELS.computeIfAbsent(keychain.getKeychainId(), id -> {
            try (InputStreamReader reader = new InputStreamReader(
                    new ByteArrayInputStream(modelData), StandardCharsets.UTF_8)) {
                BedrockModelPOJO pojo = ClientAssetsManager.GSON.fromJson(reader, BedrockModelPOJO.class);
                return new BedrockModel(pojo, BedrockVersion.NEW);
            } catch (Exception e) {
                GunCosmeticsMod.LOGGER.warn("Failed to load keychain model: {}", id, e);
                return null;
            }
        });
    }

    public static void clear() {
        MODELS.clear();
    }
}
