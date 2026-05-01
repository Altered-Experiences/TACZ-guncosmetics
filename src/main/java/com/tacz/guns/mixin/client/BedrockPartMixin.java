package com.tacz.guns.mixin.client;

import com.tacz.guns.cosmetic.client.renderer.CosmeticRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockPart.class, remap = false)
public abstract class BedrockPartMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/client/model/bedrock/BedrockPart;translateAndRotateAndScale(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                    shift = At.Shift.AFTER),
            require = 0)
    private void tacz$renderKeychainAsBoneChild(PoseStack poseStack, ItemDisplayContext transformType,
                                                        VertexConsumer consumer, int light, int overlay,
                                                        float red, float green, float blue, float alpha,
                                                        CallbackInfo ci) {
        if (!transformType.firstPerson()) {
            return;
        }
        CosmeticRenderHelper.renderKeychainOnCurrentBone((BedrockPart) (Object) this, poseStack,
                Minecraft.getInstance().renderBuffers().bufferSource(), light);
    }
}
