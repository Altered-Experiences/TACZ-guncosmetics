package com.tacz.guns.cosmetic.item;

import com.tacz.guns.cosmetic.config.CosmeticsConfig;
import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.cosmetic.data.CosmeticRarity;
import com.tacz.guns.cosmetic.pack.CosmeticLocalization;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.List;

/**
 * An item representing a consumable skin that can be applied to a gun.
 * Stores the skin ID in its own NBT at tag.SkinId.
 */
public class SkinItem extends Item implements IAttachment {

    @Nullable
    private final ResourceLocation skinId;

    public SkinItem() {
        this(null);
    }

    public SkinItem(@Nullable ResourceLocation skinId) {
        super(new Properties().stacksTo(1));
        this.skinId = skinId;
    }

    @Nullable
    public ResourceLocation getSkinId() {
        return skinId;
    }

    @Nullable
    public ResourceLocation getSkinId(ItemStack stack) {
        return skinId != null ? skinId : CosmeticNBTHelper.getSkinId(stack);
    }

    public static ItemStack create(ResourceLocation skinId) {
        ItemStack stack = com.tacz.guns.cosmetic.item.ModCosmeticItems.GENERIC_SKIN.get().getDefaultInstance();
        CosmeticNBTHelper.setSkinId(stack, skinId);
        return stack;
    }

    @Override
    public ResourceLocation getAttachmentId(ItemStack attachmentStack) {
        ResourceLocation id = getSkinId(attachmentStack);
        return id == null ? com.tacz.guns.api.DefaultAssets.EMPTY_ATTACHMENT_ID : id;
    }

    @Override
    public void setAttachmentId(ItemStack attachmentStack, @Nullable ResourceLocation attachmentId) {
        CosmeticNBTHelper.setSkinId(attachmentStack, attachmentId);
    }

    @Override
    public void setSkinId(ItemStack attachmentStack, @Nullable ResourceLocation skinId) {
        CosmeticNBTHelper.setSkinId(attachmentStack, skinId);
    }

    @Override
    public int getZoomNumber(ItemStack attachmentStack) {
        return 0;
    }

    @Override
    public void setZoomNumber(ItemStack attachmentStack, int zoomNumber) {
    }

    @Override
    public AttachmentType getType(ItemStack attachmentStack) {
        return AttachmentType.SKIN;
    }

    @Override
    public boolean hasCustomLaserColor(ItemStack attachmentStack) {
        return false;
    }

    @Override
    public int getLaserColor(ItemStack attachmentStack) {
        return 0xFF0000;
    }

    @Override
    public void setLaserColor(ItemStack attachmentStack, int color) {
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final com.tacz.guns.cosmetic.client.renderer.CosmeticItemRenderer renderer =
                    new com.tacz.guns.cosmetic.client.renderer.CosmeticItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = getSkinId(stack);
        if (id == null) return Component.translatable("item.guncosmetics.skin_item", "");
        return SkinRegistry.get(id)
                .map(skin -> {
                    CosmeticRarity rarity = skin.getRarity();
                    return Component.literal(readableName(skin.getDisplayName()))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rarity.getColor())));
                })
                .orElse(Component.translatable("item.guncosmetics.skin_item", id.toString()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = getSkinId(stack);
        if (id == null) return;
        SkinRegistry.get(id).ifPresent(skin -> {
            skin.getDescription().forEach(line -> tooltip.add(Component.translatable(line).withStyle(ChatFormatting.GRAY)));
            if (CosmeticsConfig.SHOW_RARITY_IN_TOOLTIP.get()) {
                tooltip.add(Component.translatable("tooltip.guncosmetics.rarity",
                        skin.getRarity().getDisplayName()));
            }
            if (skin.getTargetGun() != null) {
                tooltip.add(Component.translatable("tooltip.guncosmetics.target_gun",
                        skin.getTargetGun().toString()).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.guncosmetics.universal")
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    private static String readableName(String key) {
        String translated = CosmeticLocalization.translate(key, "en_us");
        if (!translated.equals(key)) return translated;
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
}
