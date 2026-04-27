package com.tacz.guns.cosmetic.item;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registration for GunCosmetics mod.
 */
public final class ModCosmeticItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GunCosmeticsMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GunCosmeticsMod.MOD_ID);

    public static final RegistryObject<CosmeticsKitItem> COSMETICS_KIT =
            ITEMS.register("cosmetics_kit", CosmeticsKitItem::new);
    public static final RegistryObject<SkinItem> GENERIC_SKIN =
            ITEMS.register("skin", SkinItem::new);
    public static final RegistryObject<KeychainItem> GENERIC_KEYCHAIN =
            ITEMS.register("keychain", KeychainItem::new);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("cosmetics_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Gun Cosmetics"))
                    .icon(() -> new ItemStack(COSMETICS_KIT.get()))
                    .displayItems((params, output) -> {
                        output.accept(COSMETICS_KIT.get());
                        output.accept(GENERIC_SKIN.get());
                        output.accept(GENERIC_KEYCHAIN.get());
                    })
                    .build());

    private ModCosmeticItems() {}
}
