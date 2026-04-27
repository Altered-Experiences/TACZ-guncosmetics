package com.tacz.guns.cosmetic;

import com.tacz.guns.cosmetic.command.GunCosmeticsCommand;
import com.tacz.guns.cosmetic.config.CosmeticsConfig;
import com.tacz.guns.cosmetic.network.CosmeticsNetworkHandler;
import com.tacz.guns.cosmetic.network.CosmeticsSyncEventHandler;
import com.tacz.guns.cosmetic.pack.CosmeticPackLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.network.chat.Component;

public class GunCosmeticsMod {
    public static final String MOD_ID = "guncosmetics";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GunCosmeticsCommand.register(event.getDispatcher());
    }

    public GunCosmeticsMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CosmeticsConfig.SPEC, "guncosmetics-common.toml");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onAddPackFinders);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(CosmeticsSyncEventHandler.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                MinecraftForge.EVENT_BUS.register(com.tacz.guns.cosmetic.client.CosmeticClientEvents.class));
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CosmeticsNetworkHandler::init);
        // CosmeticPackLoader.loadAllPacks(null) moved to AddPackFindersEvent or ClientSetup
    }

    private void onAddPackFinders(net.minecraftforge.event.AddPackFindersEvent event) {
        if (event.getPackType() == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {
            // Pre-load cosmetic packs metadata before finding resources
            CosmeticPackLoader.loadAllPacks(null);
            java.nio.file.Path packsDir = FMLPaths.GAMEDIR.get().resolve("guncosmetics");
            if (java.nio.file.Files.isDirectory(packsDir)) {
                event.addRepositorySource((infoConsumer) -> {
                    try (java.nio.file.DirectoryStream<java.nio.file.Path> stream = java.nio.file.Files.newDirectoryStream(packsDir)) {
                        for (java.nio.file.Path entry : stream) {
                            if (java.nio.file.Files.isDirectory(entry) || entry.toString().endsWith(".zip")) {
                                String packId = "guncosmetics_" + entry.getFileName().toString();
                                net.minecraft.server.packs.repository.Pack pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
                                        packId,
                                        Component.literal(entry.getFileName().toString()),
                                        true,
                                        (id) -> entry.toString().endsWith(".zip")
                                                ? new net.minecraft.server.packs.FilePackResources(id, entry.toFile(), true)
                                                : new net.minecraft.server.packs.PathPackResources(id, entry, true),
                                        net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                                        net.minecraft.server.packs.repository.Pack.Position.TOP,
                                        net.minecraft.server.packs.repository.PackSource.BUILT_IN
                                );
                                if (pack != null) {
                                    infoConsumer.accept(pack);
                                }
                            }
                        }
                    } catch (java.io.IOException e) {
                        LOGGER.error("Failed to load cosmetic resource packs", e);
                    }
                });
            }
        }
    }
}
