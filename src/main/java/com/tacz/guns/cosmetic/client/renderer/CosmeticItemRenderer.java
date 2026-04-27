package com.tacz.guns.cosmetic.client.renderer;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.cosmetic.item.KeychainItem;
import com.tacz.guns.cosmetic.item.SkinItem;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CosmeticItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation DEFAULT_SKIN = new ResourceLocation(GunCosmeticsMod.MOD_ID, "textures/item/skin.png");
    private static final ResourceLocation DEFAULT_KEYCHAIN = new ResourceLocation(GunCosmeticsMod.MOD_ID, "textures/item/keychain.png");

    public CosmeticItemRenderer() {
        this(Minecraft.getInstance().getBlockEntityRenderDispatcher());
    }

    private CosmeticItemRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher, Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int light, int overlay) {
        ResourceLocation texture = resolveTexture(stack);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(bufferSource, RenderType.entityTranslucent(texture), true, stack.hasFoil());
        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();

        consumer.vertex(pose, 0, 1, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(overlay).uv2(light).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, 1, 1, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(overlay).uv2(light).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, 1, 0, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(overlay).uv2(light).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, 0, 0, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(overlay).uv2(light).normal(normal, 0, 0, 1).endVertex();
    }

    private ResourceLocation resolveTexture(ItemStack stack) {
        if (stack.getItem() instanceof SkinItem) {
            ResourceLocation skinId = CosmeticNBTHelper.getSkinId(stack);
            if (skinId != null) {
                return SkinRegistry.get(skinId)
                        .map(skin -> skin.getIcon() != null ? CosmeticTextureManager.resolveTexture(skin.getIcon()) : DEFAULT_SKIN)
                        .orElse(DEFAULT_SKIN);
            }
            return DEFAULT_SKIN;
        }
        if (stack.getItem() instanceof KeychainItem) {
            ResourceLocation keychainId = CosmeticNBTHelper.getKeychainId(stack);
            if (keychainId != null) {
                return KeychainRegistry.get(keychainId)
                        .map(keychain -> keychain.getIcon() != null ? CosmeticTextureManager.resolveTexture(keychain.getIcon()) : DEFAULT_KEYCHAIN)
                        .orElse(DEFAULT_KEYCHAIN);
            }
            return DEFAULT_KEYCHAIN;
        }
        return DEFAULT_SKIN;
    }
}
