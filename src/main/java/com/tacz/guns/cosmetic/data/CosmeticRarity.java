package com.tacz.guns.cosmetic.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Cosmetic rarity tiers — affects only visual display (name color, border).
 */
public enum CosmeticRarity {
    COMMON("common", 0xFFFFFF),
    UNCOMMON("uncommon", 0x55FF55),
    RARE("rare", 0x5555FF),
    EPIC("epic", 0xAA00AA),
    LEGENDARY("legendary", 0xFFAA00);

    private final String id;
    private final int color;

    CosmeticRarity(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public int getColor() {
        return color;
    }

    public Component getDisplayName() {
        return Component.translatable("rarity.guncosmetics." + id)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    public static CosmeticRarity fromString(String s) {
        if (s == null) return COMMON;
        for (CosmeticRarity r : values()) {
            if (r.id.equalsIgnoreCase(s)) return r;
        }
        return COMMON;
    }
}
