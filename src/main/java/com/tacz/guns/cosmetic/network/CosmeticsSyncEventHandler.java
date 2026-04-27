package com.tacz.guns.cosmetic.network;

import com.tacz.guns.cosmetic.network.message.SyncCosmeticsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sends cosmetic registry data to players on join.
 */
@Mod.EventBusSubscriber
public class CosmeticsSyncEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CosmeticsNetworkHandler.sendToPlayer(new SyncCosmeticsPacket(), serverPlayer);
        }
    }
}
