package com.tacz.guns.cosmetic.network;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.cosmetic.network.message.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Network handler for cosmetics packets.
 */
public final class CosmeticsNetworkHandler {

    private static final String VERSION = "1.0.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GunCosmeticsMod.MOD_ID, "network"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private static final AtomicInteger ID = new AtomicInteger(0);

    private CosmeticsNetworkHandler() {}

    public static void init() {
        // S→C: Sync all cosmetic registries
        CHANNEL.registerMessage(ID.getAndIncrement(), SyncCosmeticsPacket.class,
                SyncCosmeticsPacket::encode, SyncCosmeticsPacket::decode, SyncCosmeticsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // C→S: Apply skin or keychain
        CHANNEL.registerMessage(ID.getAndIncrement(), ApplyCosmeticPacket.class,
                ApplyCosmeticPacket::encode, ApplyCosmeticPacket::decode, ApplyCosmeticPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // C→S: Remove skin or keychain
        CHANNEL.registerMessage(ID.getAndIncrement(), RemoveCosmeticPacket.class,
                RemoveCosmeticPacket::encode, RemoveCosmeticPacket::decode, RemoveCosmeticPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // S→C: Confirmation broadcast
        CHANNEL.registerMessage(ID.getAndIncrement(), CosmeticAppliedPacket.class,
                CosmeticAppliedPacket::encode, CosmeticAppliedPacket::decode, CosmeticAppliedPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // C->S: Update keychain transform
        CHANNEL.registerMessage(ID.getAndIncrement(), UpdateKeychainTransformPacket.class,
                UpdateKeychainTransformPacket::encode, UpdateKeychainTransformPacket::decode, UpdateKeychainTransformPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
