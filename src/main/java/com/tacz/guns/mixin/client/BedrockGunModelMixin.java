package com.tacz.guns.mixin.client;

import com.tacz.guns.cosmetic.client.renderer.CosmeticRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.BedrockGunModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockGunModel.class, remap = false)
public abstract class BedrockGunModelMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void tacz$beginKeychainBoneRender(PoseStack poseStack, ItemStack gunItem,
                                                      ItemDisplayContext transformType, RenderType renderType,
                                                      int light, int overlay, CallbackInfo ci) {
        if (!isKeychainRenderContext(transformType)) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        CosmeticRenderHelper.beginGunModelRender(gunItem, iGun, (BedrockGunModel) (Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void tacz$endKeychainBoneRender(PoseStack poseStack, ItemStack gunItem,
                                                    ItemDisplayContext transformType, RenderType renderType,
                                                    int light, int overlay, CallbackInfo ci) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        try {
            if (isKeychainRenderContext(transformType)) {
                CosmeticRenderHelper.renderPendingKeychainFallback(poseStack, bufferSource, light);
                CosmeticRenderHelper.flushKeychainBuffers();
            }
        } finally {
            CosmeticRenderHelper.endGunModelRender();
        }
    }

    private boolean isKeychainRenderContext(ItemDisplayContext transformType) {
        return transformType.firstPerson();
    }
}
