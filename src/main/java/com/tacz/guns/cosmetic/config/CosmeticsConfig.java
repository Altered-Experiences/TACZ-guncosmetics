package com.tacz.guns.cosmetic.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Mod configuration — guncosmetics-common.toml
 */
public final class CosmeticsConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ALLOW_SKIN_REMOVAL;
    public static final ForgeConfigSpec.BooleanValue ALLOW_KEYCHAIN_TRANSFORM_EDITING;
    public static final ForgeConfigSpec.DoubleValue MAX_KEYCHAIN_OFFSET;
    public static final ForgeConfigSpec.BooleanValue SHOW_RARITY_IN_TOOLTIP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");

        ALLOW_SKIN_REMOVAL = builder
                .comment("Allow players to remove a skin back as an item (false = skin is lost on removal)")
                .define("allow_skin_removal", true);

        ALLOW_KEYCHAIN_TRANSFORM_EDITING = builder
                .comment("Allow players to adjust keychain position/rotation/scale")
                .define("allow_keychain_transform_editing", true);

        MAX_KEYCHAIN_OFFSET = builder
                .comment("Maximum keychain offset from bone (in blocks)")
                .defineInRange("max_keychain_offset", 5.0, 0.0, 20.0);

        SHOW_RARITY_IN_TOOLTIP = builder
                .comment("Show rarity in tooltip of skin/keychain items")
                .define("show_rarity_in_tooltip", true);

        builder.pop();

        SPEC = builder.build();
    }

    private CosmeticsConfig() {}
}
