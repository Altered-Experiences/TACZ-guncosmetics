package com.tacz.guns.cosmetic.client.renderer;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.cosmetic.client.renderer.keychain.KeychainGeoRenderCache;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic registration of pack textures with Minecraft's texture manager.
 * Pack textures are not in the vanilla resource system, so we register them as
 * DynamicTexture instances at runtime.
 */
public final class CosmeticTextureManager {

    /**
     * Maps logical ResourceLocation (e.g. awe_example:textures/ak47_military_camo.png)
     * to the actual registered ResourceLocation in the texture manager.
     */
    private static final Map<ResourceLocation, ResourceLocation> registeredTextures = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> paintjobTextures = new ConcurrentHashMap<>();
    private static final FileToIdConverter TEXTURE_CONVERTER = new FileToIdConverter("textures", ".png");

    private CosmeticTextureManager() {}

    /**
     * Register a texture from raw bytes (e.g. loaded from a pack).
     * Returns the ResourceLocation that can be used for rendering.
     */
    @Nullable
    public static ResourceLocation registerTexture(ResourceLocation logicalId, byte[] pngData) {
        if (registeredTextures.containsKey(logicalId)) {
            return registeredTextures.get(logicalId);
        }

        try {
            ByteBuffer buffer = MemoryUtil.memAlloc(pngData.length);
            NativeImage image;
            try {
                buffer.put(pngData);
                buffer.flip();
                image = NativeImage.read(buffer);
            } finally {
                MemoryUtil.memFree(buffer);
            }
            DynamicTexture dynamicTexture = new DynamicTexture(image);

            // Register with a unique path to avoid collisions
            ResourceLocation registeredId = new ResourceLocation(
                    GunCosmeticsMod.MOD_ID,
                    "dynamic/" + logicalId.getNamespace() + "/" + logicalId.getPath()
            );

            Minecraft.getInstance().getTextureManager().register(registeredId, dynamicTexture);
            registeredTextures.put(logicalId, registeredId);

            GunCosmeticsMod.LOGGER.debug("Registered dynamic texture: {} -> {}", logicalId, registeredId);
            return registeredId;
        } catch (IOException e) {
            GunCosmeticsMod.LOGGER.error("Failed to register texture: {}", logicalId, e);
            return null;
        }
    }

    /**
     * Register a texture from a file path.
     */
    @Nullable
    public static ResourceLocation registerTexture(ResourceLocation logicalId, Path filePath) {
        if (registeredTextures.containsKey(logicalId)) {
            return registeredTextures.get(logicalId);
        }

        try {
            byte[] data = Files.readAllBytes(filePath);
            return registerTexture(logicalId, data);
        } catch (IOException e) {
            GunCosmeticsMod.LOGGER.error("Failed to read texture file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Get the registered rendering ResourceLocation for a logical pack texture.
     * Returns null if the texture hasn't been registered.
     */
    @Nullable
    public static ResourceLocation getRegisteredTexture(ResourceLocation logicalId) {
        return registeredTextures.get(logicalId);
    }

    /**
     * Resolve a texture ResourceLocation for rendering.
     * If it's a dynamic pack texture, returns the registered version.
     * Otherwise returns the original (for vanilla/mod textures).
     */
    public static ResourceLocation resolveTexture(ResourceLocation logicalId) {
        ResourceLocation registered = registeredTextures.get(logicalId);
        return registered != null ? registered : normalizeTextureId(logicalId);
    }

    private static ResourceLocation normalizeTextureId(ResourceLocation logicalId) {
        String path = logicalId.getPath();
        if (path.startsWith("textures/") && path.endsWith(".png")) {
            return logicalId;
        }
        return TEXTURE_CONVERTER.idToFile(logicalId);
    }

    public static ResourceLocation resolvePaintjobTexture(ResourceLocation baseTexture, SkinDefinition paintjob) {
        if (paintjob.getOverlayTexture() == null) {
            return baseTexture;
        }

        String key = baseTexture + "|" + paintjob.getSkinId();
        ResourceLocation existing = paintjobTextures.get(key);
        if (existing != null) return existing;

        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(baseTexture).orElse(null);
            if (resource == null) {
                GunCosmeticsMod.LOGGER.warn("Paintjob base texture not found: {}", baseTexture);
                return baseTexture;
            }

            NativeImage baseImage;
            try (InputStream stream = resource.open()) {
                baseImage = NativeImage.read(stream);
            }

            NativeImage overlayImage = loadOverlayImage(paintjob);
            NativeImage result = new NativeImage(baseImage.getWidth(), baseImage.getHeight(), false);
            for (int y = 0; y < baseImage.getHeight(); y++) {
                for (int x = 0; x < baseImage.getWidth(); x++) {
                    int base = baseImage.getPixelRGBA(x, y);
                    int overlay = overlayImage.getPixelRGBA(
                            x * overlayImage.getWidth() / baseImage.getWidth(),
                            y * overlayImage.getHeight() / baseImage.getHeight());
                    result.setPixelRGBA(x, y, blendOver(base, overlay, paintjob.getBlendMode()));
                }
            }
            baseImage.close();
            overlayImage.close();

            ResourceLocation registeredId = new ResourceLocation(
                    GunCosmeticsMod.MOD_ID,
                    "dynamic/paintjob/" + paintjob.getSkinId().getNamespace() + "/" +
                            paintjob.getSkinId().getPath() + "/" +
                            baseTexture.getNamespace() + "/" + baseTexture.getPath()
            );
            Minecraft.getInstance().getTextureManager().register(registeredId, new DynamicTexture(result));
            paintjobTextures.put(key, registeredId);
            GunCosmeticsMod.LOGGER.info("Registered paintjob texture: {} + {} -> {}",
                    baseTexture, paintjob.getSkinId(), registeredId);
            return registeredId;
        } catch (Exception e) {
            GunCosmeticsMod.LOGGER.warn("Failed to compose paintjob texture: {} on {}",
                    paintjob.getSkinId(), baseTexture, e);
            return baseTexture;
        }
    }

    private static NativeImage readPng(byte[] pngData) throws IOException {
        ByteBuffer buffer = MemoryUtil.memAlloc(pngData.length);
        try {
            buffer.put(pngData);
            buffer.flip();
            return NativeImage.read(buffer);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    private static NativeImage loadOverlayImage(SkinDefinition paintjob) throws IOException {
        if (paintjob.getTextureData() != null) {
            return readPng(paintjob.getTextureData());
        }
        ResourceLocation overlayTexture = resolveTexture(paintjob.getOverlayTexture());
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(overlayTexture).orElse(null);
        if (resource == null) {
            throw new IOException("Overlay texture not found: " + overlayTexture);
        }
        try (InputStream stream = resource.open()) {
            return NativeImage.read(stream);
        }
    }

    private static int blendOver(int base, int overlay, SkinDefinition.BlendMode mode) {
        if (isAmmoPixel(base)) {
            return base;
        }
        int br = base & 0xFF;
        int bg = (base >> 8) & 0xFF;
        int bb = (base >> 16) & 0xFF;
        int ba = (base >> 24) & 0xFF;

        int or = overlay & 0xFF;
        int og = (overlay >> 8) & 0xFF;
        int ob = (overlay >> 16) & 0xFF;
        int oa = (overlay >> 24) & 0xFF;
        if (oa == 0) return base;

        float alpha = oa / 255.0f;
        int rr = applyBlend(br, or, alpha, mode);
        int rg = applyBlend(bg, og, alpha, mode);
        int rb = applyBlend(bb, ob, alpha, mode);
        return (ba << 24) | (rb << 16) | (rg << 8) | rr;
    }

    private static boolean isAmmoPixel(int base) {
        int r = base & 0xFF;
        int g = (base >> 8) & 0xFF;
        int b = (base >> 16) & 0xFF;
        int a = (base >> 24) & 0xFF;
        if (a == 0) return false;
        int brightness = (r + g + b) / 3;
        return brightness > 24 && r > g + 12 && g > b + 8;
    }

    private static int applyBlend(int base, int overlay, float alpha, SkinDefinition.BlendMode mode) {
        int blended = switch (mode) {
            case MULTIPLY -> base * overlay / 255;
            case SCREEN -> 255 - (255 - base) * (255 - overlay) / 255;
            case ADD -> Math.min(255, base + overlay);
            case OVERLAY -> base < 128 ? (2 * base * overlay / 255)
                    : (255 - 2 * (255 - base) * (255 - overlay) / 255);
        };
        return Math.max(0, Math.min(255, Math.round(base * (1.0f - alpha) + blended * alpha)));
    }

    /**
     * Clear all registered dynamic textures (called on pack reload).
     */
    public static void clear() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation registeredId : registeredTextures.values()) {
            textureManager.release(registeredId);
        }
        for (ResourceLocation registeredId : paintjobTextures.values()) {
            textureManager.release(registeredId);
        }
        registeredTextures.clear();
        paintjobTextures.clear();
        KeychainModelCache.clear();
        KeychainGeoRenderCache.clear();
        KeychainAutoAttach.clearCache();
        GunCosmeticsMod.LOGGER.debug("Cleared all dynamic cosmetic textures");
    }
}
