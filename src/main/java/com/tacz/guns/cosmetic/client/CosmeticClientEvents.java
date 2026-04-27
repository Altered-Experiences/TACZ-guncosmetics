package com.tacz.guns.cosmetic.client;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.cosmetic.client.renderer.CosmeticTextureManager;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.tacz.guns.cosmetic.pack.CosmeticLocalization;
import com.tacz.guns.cosmetic.pack.CosmeticPackLoader;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

/**
 * Client-side event handler that registers pack textures with
 * Minecraft's TextureManager after packs are loaded by the server.
 */
@Mod.EventBusSubscriber(modid = GunCosmeticsMod.MOD_ID, value = Dist.CLIENT)
public class CosmeticClientEvents {

    private static boolean texturesRegistered = false;

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        // Schedule texture registration for next tick to avoid blocking login
        Minecraft.getInstance().tell(CosmeticClientEvents::registerAllTextures);
    }

    public static void onCosmeticsSynced() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.tell(() -> {
            texturesRegistered = false;
            CosmeticLocalization.clear();
            int attached = 0;
            for (Path packsDir : getCandidatePackDirs(minecraft)) {
                attached += CosmeticPackLoader.attachLocalAssetsToRegistries(packsDir);
            }
            if (attached > 0) {
                GunCosmeticsMod.LOGGER.info("Attached {} local cosmetic asset(s)", attached);
            }
            registerAllTextures();
        });
    }

    private static java.util.List<Path> getCandidatePackDirs(Minecraft minecraft) {
        Path gameDir = minecraft.gameDirectory.toPath();
        java.util.LinkedHashSet<Path> dirs = new java.util.LinkedHashSet<>();
        dirs.add(gameDir.resolve("guncosmetics"));
        addTaczCosmeticDirs(dirs, gameDir.resolve("tacz"));
        dirs.add(gameDir.resolve("client").resolve("guncosmetics"));
        Path parent = gameDir.getParent();
        if (parent != null) {
            dirs.add(parent.resolve("guncosmetics"));
            addTaczCosmeticDirs(dirs, parent.resolve("tacz"));
            dirs.add(parent.resolve("run").resolve("client").resolve("guncosmetics"));
            addTaczCosmeticDirs(dirs, parent.resolve("run").resolve("client").resolve("tacz"));
            addTaczCosmeticDirs(dirs, parent.resolve("run").resolve("client_b").resolve("tacz"));
            dirs.add(parent.resolve("client").resolve("guncosmetics"));
        }
        return dirs.stream().filter(java.nio.file.Files::isDirectory).toList();
    }

    private static void addTaczCosmeticDirs(java.util.LinkedHashSet<Path> dirs, Path taczDir) {
        if (!java.nio.file.Files.isDirectory(taczDir)) {
            return;
        }
        try (java.nio.file.DirectoryStream<Path> stream = java.nio.file.Files.newDirectoryStream(taczDir)) {
            for (Path gunPack : stream) {
                Path cosmeticsDir = gunPack.resolve("guncosmetics");
                if (java.nio.file.Files.isDirectory(cosmeticsDir)) {
                    dirs.add(cosmeticsDir);
                }
            }
        } catch (java.io.IOException ignored) {
        }
    }

    /**
     * Registers all loaded pack textures with the Minecraft texture manager.
     * Must be called on the render thread. Packs are already loaded by the server.
     */
    public static void registerAllTextures() {
        if (texturesRegistered) return;

        int count = 0;

        // Register skin textures
        for (SkinDefinition skin : SkinRegistry.getAll()) {
            if (skin.getTexture() != null && skin.getTextureData() != null) {
                if (CosmeticTextureManager.registerTexture(skin.getTexture(), skin.getTextureData()) != null) {
                    count++;
                }
            }
            if (skin.getOverlayTexture() != null && skin.getTextureData() != null) {
                if (CosmeticTextureManager.registerTexture(skin.getOverlayTexture(), skin.getTextureData()) != null) {
                    count++;
                }
            }
        }

        // Register keychain textures
        for (KeychainDefinition kc : KeychainRegistry.getAll()) {
            if (kc.getTexture() != null && kc.getTextureData() != null) {
                if (CosmeticTextureManager.registerTexture(kc.getTexture(), kc.getTextureData()) != null) {
                    count++;
                }
            }
        }

        if (count > 0) {
            texturesRegistered = true;
            GunCosmeticsMod.LOGGER.info("Registered {} dynamic cosmetic textures", count);
        } else {
            GunCosmeticsMod.LOGGER.info("No cosmetic textures to register (packs may not be loaded yet)");
        }
    }

    /**
     * Reset registration state on disconnect, so textures re-register on next join.
     */
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        texturesRegistered = false;
        CosmeticTextureManager.clear();
    }
}
