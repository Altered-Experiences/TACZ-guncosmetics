package com.tacz.guns.cosmetic.client.renderer;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.tacz.guns.cosmetic.client.renderer.keychain.KeychainGeoRenderCache;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.resource.pojo.data.gun.KeychainAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Helper class for cosmetic rendering — skin texture swap, overlay, and keychain.
 */
public final class CosmeticRenderHelper {

    /**
     * The ItemStack currently being rendered. Set by the mixin before rendering,
     * read by GunDisplayInstanceMixin to swap textures, cleared after render.
     */
    private static ItemStack currentRenderingStack = null;
    private static KeychainRenderContext currentKeychainContext = null;
    private static boolean currentKeychainRenderedOnBone = false;

    private CosmeticRenderHelper() {}

    // ── Current Stack Context ─────────────────────────────

    public static void setCurrentRenderingStack(ItemStack stack) {
        currentRenderingStack = stack;
    }

    public static void clearCurrentRenderingStack() {
        currentRenderingStack = null;
    }

    @Nullable
    public static ItemStack getCurrentRenderingStack() {
        return currentRenderingStack;
    }

    public static ResourceLocation resolveGunTexture(ItemStack stack, ResourceLocation originalTexture) {
        if (stack == null || stack.isEmpty()) return originalTexture;

        ResourceLocation skinId = CosmeticNBTHelper.getSkinId(stack);
        if ((skinId == null || com.tacz.guns.api.DefaultAssets.isEmptyAttachmentId(skinId))
                && stack.getItem() instanceof IGun iGun) {
            skinId = iGun.getAttachmentId(stack, AttachmentType.SKIN);
        }
        if (skinId == null) return originalTexture;
        if (com.tacz.guns.api.DefaultAssets.isEmptyAttachmentId(skinId)) return originalTexture;

        return SkinRegistry.get(skinId)
                .map(skin -> {
                    if (skin.getType() == SkinDefinition.SkinType.UNIVERSAL) {
                        return CosmeticTextureManager.resolvePaintjobTexture(originalTexture, skin);
                    }
                    return skin.getTexture() != null
                            ? CosmeticTextureManager.resolveTexture(skin.getTexture())
                            : originalTexture;
                })
                .orElse(originalTexture);
    }

    public static void beginGunModelRender(ItemStack stack, IGun iGun, BedrockGunModel gunModel) {
        currentKeychainContext = createKeychainContext(stack, iGun, gunModel);
        currentKeychainRenderedOnBone = false;
    }

    public static void endGunModelRender() {
        currentKeychainContext = null;
        currentKeychainRenderedOnBone = false;
    }

    public static void flushKeychainBuffers() {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    public static void renderKeychainOnCurrentBone(BedrockPart bone, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, int light) {
        KeychainRenderContext context = currentKeychainContext;
        if (context == null || context.bone != bone) return;
        currentKeychainRenderedOnBone = true;

        poseStack.pushPose();
        applyKeychainTransformAndRender(context, poseStack, bufferSource, light);
        poseStack.popPose();
    }

    public static void renderPendingKeychainFallback(PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        KeychainRenderContext context = currentKeychainContext;
        if (context == null) return;

        if (currentKeychainRenderedOnBone) return;

        poseStack.pushPose();
        applyBonePath(poseStack, context.bone);
        applyKeychainTransformAndRender(context, poseStack, bufferSource, light);
        poseStack.popPose();
    }

    public static void renderKeychainForGun(ItemStack stack, IGun iGun, BedrockGunModel gunModel,
                                            PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        KeychainRenderContext context = createKeychainContext(stack, iGun, gunModel);
        if (context == null) {
            return;
        }

        poseStack.pushPose();
        applyBonePath(poseStack, context.bone);
        applyKeychainTransformAndRender(context, poseStack, bufferSource, light);
        poseStack.popPose();
    }

    private static void applyKeychainTransformAndRender(KeychainRenderContext context, PoseStack poseStack,
                                                        MultiBufferSource bufferSource, int light) {
        poseStack.translate(context.offset.x() / 16.0, context.offset.y() / 16.0, context.offset.z() / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(context.rotation.x()));
        poseStack.mulPose(Axis.YP.rotationDegrees(context.rotation.y()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(context.rotation.z()));
        poseStack.scale(context.scale.x(), context.scale.y(), context.scale.z());
        renderKeychainModel(context.keychain, poseStack, bufferSource, light);
    }

    @Nullable
    private static KeychainRenderContext createKeychainContext(ItemStack stack, IGun iGun, BedrockGunModel gunModel) {
        ResourceLocation keychainId = CosmeticNBTHelper.getKeychainId(stack);
        if (keychainId == null || com.tacz.guns.api.DefaultAssets.isEmptyAttachmentId(keychainId)) {
            keychainId = iGun.getAttachmentId(stack, AttachmentType.KEYCHAIN);
        }
        if (keychainId == null) return null;
        if (com.tacz.guns.api.DefaultAssets.isEmptyAttachmentId(keychainId)) return null;

        ResourceLocation finalKeychainId = keychainId;
        return KeychainRegistry.get(finalKeychainId).map(kc -> {
            // Get the gun ID for auto-attach caching
            ResourceLocation gunId = iGun.getGunId(stack);

            CompoundTag transform = CosmeticNBTHelper.getKeychainTransform(stack);
            KeychainAttachment gunAttach = TimelessAPI.getCommonGunIndex(gunId)
                    .map(index -> index.getGunData().getKeychainAttachment())
                    .orElse(null);
            String boneName = transform != null && transform.contains("Bone") ? transform.getString("Bone") :
                    (gunAttach != null ? gunAttach.getBone() : kc.getBone());

            BedrockPart bone = gunModel.getNode(boneName);
            Vector3f offset = gunAttach != null ? gunAttach.getOffset() : kc.getOffset();
            Vector3f rotation = gunAttach != null ? gunAttach.getRotation() : kc.getRotation();
            Vector3f scale = gunAttach != null ? gunAttach.getScale() : kc.getScale();
            boolean hasValidTransformBone = transform != null && bone != null;

            if (bone == null && transform != null) {
                boneName = gunAttach != null ? gunAttach.getBone() : kc.getBone();
                bone = gunModel.getNode(boneName);
            }

            if (bone == null) {
                KeychainAutoAttach.AttachResult autoResult =
                        KeychainAutoAttach.findAttachPoint(gunId, gunModel);
                if (autoResult == null) return null;

                bone = autoResult.bone;
                offset = autoResult.offset;
                rotation = autoResult.rotation;
                scale = autoResult.scale;
                GunCosmeticsMod.LOGGER.debug("Keychain '{}' auto-attached to bone '{}' on gun '{}'",
                        finalKeychainId, bone.name, gunId);
            }

            if (transform != null && hasValidTransformBone) {
                offset = new Vector3f(
                        transform.getFloat("OffsetX"),
                        transform.getFloat("OffsetY"),
                        transform.getFloat("OffsetZ"));
                rotation = new Vector3f(
                        transform.getFloat("RotX"),
                        transform.getFloat("RotY"),
                        transform.getFloat("RotZ"));
                scale = new Vector3f(
                        transform.getFloat("ScaleX"),
                        transform.getFloat("ScaleY"),
                        transform.getFloat("ScaleZ"));
            }

            return new KeychainRenderContext(kc, bone, offset, rotation, scale);
        }).orElse(null);
    }

    /**
     * Returns true if the keychain definition has default/zero transform values,
     * meaning the auto-attach system should provide its own computed transform.
     */
    private static boolean isDefaultTransform(KeychainDefinition kc) {
        Vector3f o = kc.getOffset();
        Vector3f r = kc.getRotation();
        Vector3f s = kc.getScale();
        boolean defaultOffset = o.x() == 0 && o.y() == 0 && o.z() == 0;
        boolean defaultRotation = r.x() == 0 && r.y() == 0 && r.z() == 0;
        boolean defaultScale = s.x() == 1 && s.y() == 1 && s.z() == 1;
        return defaultOffset && defaultRotation && defaultScale;
    }

    private static void applyBonePath(PoseStack poseStack, BedrockPart bone) {
        Deque<BedrockPart> path = new ArrayDeque<>();
        BedrockPart current = bone;
        while (current != null) {
            path.push(current);
            current = current.getParent();
        }
        while (!path.isEmpty()) {
            path.pop().translateAndRotateAndScale(poseStack);
        }
    }

    /**
     * Renders the keychain's model as a textured cube.
     */
    private static void renderKeychainModel(KeychainDefinition kc, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int light) {
        ResourceLocation texture = CosmeticTextureManager.resolveTexture(kc.getTexture());
        if (KeychainGeoRenderCache.render(kc, poseStack, bufferSource, light)) {
            return;
        }

        applySwayAnimation(kc, poseStack);

        if (KeychainModelCache.get(kc) != null) {
            poseStack.pushPose();
            poseStack.translate(0, -0.55, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            KeychainModelCache.get(kc).render(poseStack, net.minecraft.world.item.ItemDisplayContext.FIXED,
                    RenderType.entityCutoutNoCull(texture), light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }

        RenderType renderType = RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        // Visible fallback for packs whose geo model could not be parsed.
        float s = 0.42f;
        poseStack.pushPose();
        poseStack.scale(s, s, s);

        // Front face
        drawQuad(consumer, poseStack, -1, -1, 1, 1, 0, 0, 1, 1, light, 0, 0, 1);
        // Back face
        drawQuad(consumer, poseStack, 1, -1, -1, 1, 0, 0, 1, 1, light, 0, 0, -1);
        // Top face
        drawQuadHoriz(consumer, poseStack, -1, 1, 1, -1, 0, 0, 1, 1, light, 0, 1, 0);
        // Bottom face
        drawQuadHoriz(consumer, poseStack, -1, -1, 1, -1, 0, 0, 1, 1, light, 0, -1, 0);

        poseStack.popPose();
    }

    private static void applySwayAnimation(KeychainDefinition kc, PoseStack poseStack) {
        long millis = System.currentTimeMillis();
        float phase = (millis % 2400L) / 2400.0F * ((float) Math.PI * 2.0F);
        float idOffset = Math.abs(kc.getKeychainId().hashCode() % 360) * 0.017453292F;
        float swing = (float) Math.sin(phase + idOffset);
        float twist = (float) Math.cos(phase * 0.62F + idOffset);
        poseStack.translate(0.0F, Math.abs(swing) * 0.006F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(swing * 7.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(twist * 3.0F));
    }

    private static void drawQuad(VertexConsumer consumer, PoseStack poseStack,
                                  float x1, float y1, float x2, float y2,
                                  float u1, float v1, float u2, float v2, int light,
                                  float nx, float ny, float nz) {
        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();
        float z = nz > 0 ? 0.01f : -0.01f;

        consumer.vertex(pose, x1, y1, z).color(255, 255, 255, 255)
                .uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x2, y1, z).color(255, 255, 255, 255)
                .uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x2, y2, z).color(255, 255, 255, 255)
                .uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x1, y2, z).color(255, 255, 255, 255)
                .uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
    }

    private static void drawQuadHoriz(VertexConsumer consumer, PoseStack poseStack,
                                       float x1, float y, float x2, float z,
                                       float u1, float v1, float u2, float v2, int light,
                                       float nx, float ny, float nz) {
        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();

        consumer.vertex(pose, x1, y, z).color(255, 255, 255, 255)
                .uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x2, y, z).color(255, 255, 255, 255)
                .uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x2, y, -z).color(255, 255, 255, 255)
                .uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x1, y, -z).color(255, 255, 255, 255)
                .uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
    }

    private record KeychainRenderContext(KeychainDefinition keychain, BedrockPart bone,
                                         Vector3f offset, Vector3f rotation, Vector3f scale) {}
}
