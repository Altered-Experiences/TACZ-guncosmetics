package com.tacz.guns.mixin.client;

import com.tacz.guns.cosmetic.client.renderer.CosmeticRenderHelper;
import com.tacz.guns.client.resource.GunDisplayInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into GunDisplayInstance to intercept getModelTexture().
 * If the gun currently being rendered has a skin attachment applied,
 * returns that skin texture instead of the original.
 */
@Mixin(value = GunDisplayInstance.class, remap = false)
public abstract class GunDisplayInstanceMixin {

    @Inject(method = "getModelTexture", at = @At("RETURN"), cancellable = true)
    private void tacz$overrideTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        ItemStack stack = CosmeticRenderHelper.getCurrentRenderingStack();
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation skinTexture = CosmeticRenderHelper.resolveGunTexture(stack, cir.getReturnValue());
        if (skinTexture != null && !skinTexture.getPath().contains("missingno")) {
            cir.setReturnValue(skinTexture);
        }
    }
}
