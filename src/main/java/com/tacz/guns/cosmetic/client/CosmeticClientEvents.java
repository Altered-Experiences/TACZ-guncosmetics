package com.tacz.guns.cosmetic.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.cosmetic.client.renderer.CosmeticTextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Clears generated attachment render caches when the client leaves a world.
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Dist.CLIENT)
public class CosmeticClientEvents {
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CosmeticTextureManager.clear();
    }
}
