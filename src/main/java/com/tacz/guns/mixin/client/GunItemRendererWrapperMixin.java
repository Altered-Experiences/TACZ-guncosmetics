package com.tacz.guns.mixin.client;

import com.tacz.guns.cosmetic.client.renderer.CosmeticRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into TACZ's GunItemRendererWrapper.
 * Sets the current rendering stack context and renders keychains after the gun.
 * Texture swap is handled by GunDisplayInstanceMixin.
 */
@Mixin(value = GunItemRendererWrapper.class, remap = false)
public abstract class GunItemRendererWrapperMixin {

    @Inject(method = "renderFirstPerson", at = @At("HEAD"))
    private void tacz$beforeFP(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int light, float partialTick, CallbackInfo ci) {
        CosmeticRenderHelper.setCurrentRenderingStack(stack);
    }

    @Inject(method = "renderFirstPerson", at = @At("TAIL"))
    private void tacz$afterFP(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       int light, float partialTick, CallbackInfo ci) {
        CosmeticRenderHelper.clearCurrentRenderingStack();
    }

    @Inject(method = "lambda$renderFirstPerson$5",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/client/model/BedrockGunModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V",
                    shift = At.Shift.AFTER),
            require = 0)
    private void tacz$renderKeychainFirstPerson(ItemStack stack, LocalPlayer player, float partialTick,
                                                        PoseStack poseStack, ItemDisplayContext ctx, int light,
                                                        GunDisplayInstance display, CallbackInfo ci) {
        // Keychains are rendered from BedrockGunModelMixin so they inherit the gun bone transform.
    }

    @Inject(method = "renderByItem", at = @At("HEAD"), remap = true)
    private void tacz$beforeRBI(ItemStack stack, ItemDisplayContext transformType,
                                         PoseStack poseStack, MultiBufferSource pBuffer,
                                         int pPackedLight, int pPackedOverlay, CallbackInfo ci) {
        CosmeticRenderHelper.setCurrentRenderingStack(stack);
    }

    @Inject(method = "renderByItem", at = @At("TAIL"), remap = true)
    private void tacz$afterRBI(ItemStack stack, ItemDisplayContext transformType,
                                        PoseStack poseStack, MultiBufferSource pBuffer,
                                        int pPackedLight, int pPackedOverlay, CallbackInfo ci) {
        CosmeticRenderHelper.clearCurrentRenderingStack();
    }

    @Inject(method = "lambda$renderByItem$6",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/client/model/BedrockGunModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V",
                    shift = At.Shift.AFTER),
            require = 0)
    private static void tacz$renderKeychainByItem(ItemDisplayContext transformType, PoseStack poseStack,
                                                          MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay,
                                                          ItemStack stack, GunDisplayInstance display, CallbackInfo ci) {
        // Keychains are rendered from BedrockGunModelMixin so they inherit the gun bone transform.
    }
}
