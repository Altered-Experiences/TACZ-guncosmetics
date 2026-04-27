package com.tacz.guns.cosmetic.client.gui.components;

import com.tacz.guns.cosmetic.client.gui.CosmeticsEditorScreen;
import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.cosmetic.network.CosmeticsNetworkHandler;
import com.tacz.guns.cosmetic.network.message.RemoveCosmeticPacket;
import com.tacz.guns.cosmetic.network.message.ApplyCosmeticPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A small widget shown in the GunRefitScreen for skin/keychain slots.
 * Shows the current cosmetic name and allows opening the editor or removing.
 */
public class CosmeticSlotWidget extends AbstractWidget {

    public enum SlotType { SKIN, KEYCHAIN }

    private final SlotType slotType;
    private final int gunSlot;

    public CosmeticSlotWidget(int x, int y, int width, int height, Component message,
                               SlotType slotType, int gunSlot) {
        super(x, y, width, height, message);
        this.slotType = slotType;
        this.gunSlot = gunSlot;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        int bgColor = this.isHovered ? 0x80444444 : 0x80222222;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

        // Border (colored by slot type)
        int borderColor = slotType == SlotType.SKIN ? 0xFFFFAA00 : 0xFF55FF55;
        graphics.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);

        // Label
        graphics.drawString(Minecraft.getInstance().font, this.getMessage(),
                this.getX() + 3, this.getY() + 5, 0xFFFFFF);

        // Remove button "✕" at the right edge
        int btnX = this.getX() + this.width - 14;
        int btnY = this.getY() + 2;
        boolean hoverRemove = mouseX >= btnX && mouseX <= btnX + 12 && mouseY >= btnY && mouseY <= btnY + 14;
        graphics.drawString(Minecraft.getInstance().font, "✕",
                btnX + 2, btnY + 3, hoverRemove ? 0xFF5555 : 0xAAAAAA);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int btnX = this.getX() + this.width - 14;
        int btnY = this.getY() + 2;

        if (mouseX >= btnX && mouseX <= btnX + 12 && mouseY >= btnY && mouseY <= btnY + 14) {
            // Remove cosmetic
            ApplyCosmeticPacket.CosmeticType type = slotType == SlotType.SKIN
                    ? ApplyCosmeticPacket.CosmeticType.SKIN
                    : ApplyCosmeticPacket.CosmeticType.KEYCHAIN;
            CosmeticsNetworkHandler.sendToServer(new RemoveCosmeticPacket(gunSlot, type));
        } else {
            // Open cosmetics editor
            Minecraft.getInstance().setScreen(new CosmeticsEditorScreen());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
