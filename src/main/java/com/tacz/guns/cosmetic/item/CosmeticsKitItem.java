package com.tacz.guns.cosmetic.item;

import com.tacz.guns.cosmetic.GunCosmeticsMod;
import com.tacz.guns.cosmetic.client.gui.CosmeticsEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Cosmetics Kit — right-click to open the Cosmetics Editor GUI.
 */
public class CosmeticsKitItem extends Item {

    public CosmeticsKitItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            openScreen();
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @OnlyIn(Dist.CLIENT)
    private void openScreen() {
        Minecraft.getInstance().setScreen(new CosmeticsEditorScreen());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.guncosmetics.cosmetics_kit");
    }
}
