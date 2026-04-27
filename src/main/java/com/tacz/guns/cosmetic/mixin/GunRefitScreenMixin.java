package com.tacz.guns.cosmetic.mixin;

import com.tacz.guns.cosmetic.client.gui.components.CosmeticSlotWidget;
import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.cosmetic.pack.CosmeticLocalization;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into TACZ's GunRefitScreen to add cosmetic slots.
 * Adds "Skin" and "Keychain" fields at the bottom of the refit screen.
 */
@Mixin(value = GunRefitScreen.class, remap = false)
public abstract class GunRefitScreenMixin extends Screen {

    protected GunRefitScreenMixin(Component title) {
        super(title);
    }

    /**
     * Inject after init() to add cosmetic slot widgets.
     */
    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void guncosmetics$addCosmeticSlots(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack gunStack = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) return;

        int bottomY = this.height - 30;
        int centerX = this.width / 2;

        // Skin slot
        ResourceLocation currentSkin = CosmeticNBTHelper.getSkinId(gunStack);
        String skinLabel = currentSkin != null
                ? SkinRegistry.get(currentSkin)
                    .map(s -> readableName(s.getDisplayName()))
                    .orElse("???")
                : Component.translatable("gui.guncosmetics.no_skin").getString();

        CosmeticSlotWidget skinSlot = new CosmeticSlotWidget(
                centerX - 110, bottomY, 100, 18,
                Component.literal("Skin: " + skinLabel),
                CosmeticSlotWidget.SlotType.SKIN,
                player.getInventory().selected
        );
        this.addRenderableWidget(skinSlot);

        // Keychain slot
        ResourceLocation currentKc = CosmeticNBTHelper.getKeychainId(gunStack);
        String kcLabel = currentKc != null
                ? KeychainRegistry.get(currentKc)
                    .map(k -> readableName(k.getDisplayName()))
                    .orElse("???")
                : Component.translatable("gui.guncosmetics.no_keychain").getString();

        CosmeticSlotWidget kcSlot = new CosmeticSlotWidget(
                centerX + 10, bottomY, 100, 18,
                Component.literal("Keychain: " + kcLabel),
                CosmeticSlotWidget.SlotType.KEYCHAIN,
                player.getInventory().selected
        );
        this.addRenderableWidget(kcSlot);
    }

    private String readableName(String key) {
        String locale = Minecraft.getInstance().getLanguageManager().getSelected();
        String translated = CosmeticLocalization.translate(key, locale);
        if (!translated.equals(key)) {
            return translated;
        }
        String vanilla = Component.translatable(key).getString();
        if (!vanilla.equals(key)) {
            return vanilla;
        }
        int dot = key.lastIndexOf('.');
        String raw = dot >= 0 ? key.substring(dot + 1) : key;
        String[] parts = raw.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.isEmpty() ? key : result.toString();
    }

    /**
     * Inject after render() to draw the "COSMETICS:" label.
     */
    @Inject(method = "render", at = @At("TAIL"), remap = true)
    private void guncosmetics$renderCosmeticsLabel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int bottomY = this.height - 42;
        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, "COSMETICS", centerX, bottomY, 0xFFAAAAAA);
    }
}
