package com.tacz.guns.cosmetic.client.renderer.keychain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import net.minecraft.client.renderer.MultiBufferSource;

public final class KeychainGeoRenderCache {
    private KeychainGeoRenderCache() {}

    public static boolean render(KeychainDefinition keychain, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int light) {
        return false;
    }

    public static void clear() {
    }
}
