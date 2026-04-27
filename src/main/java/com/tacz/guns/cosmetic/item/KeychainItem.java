package com.tacz.guns.cosmetic.item;

import com.tacz.guns.cosmetic.config.CosmeticsConfig;
import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.cosmetic.data.CosmeticRarity;
import com.tacz.guns.cosmetic.pack.CosmeticLocalization;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
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
 * An item representing a consumable keychain that can be attached to a gun.
 */
public class KeychainItem extends Item implements IAttachment {

    @Nullable
    private final ResourceLocation keychainId;

    public KeychainItem() {
        this(null);
    }

    public KeychainItem(@Nullable ResourceLocation keychainId) {
        super(new Properties().stacksTo(1));
        this.keychainId = keychainId;
    }

    @Nullable
    public ResourceLocation getKeychainId() {
        return keychainId;
    }

    @Nullable
    public ResourceLocation getKeychainId(ItemStack stack) {
        return keychainId != null ? keychainId : CosmeticNBTHelper.getKeychainId(stack);
    }

    public static ItemStack create(ResourceLocation keychainId) {
        ItemStack stack = com.tacz.guns.cosmetic.item.ModCosmeticItems.GENERIC_KEYCHAIN.get().getDefaultInstance();
        CosmeticNBTHelper.setKeychainId(stack, keychainId);
        return stack;
    }

    @Override
    public ResourceLocation getAttachmentId(ItemStack attachmentStack) {
        ResourceLocation id = getKeychainId(attachmentStack);
        return id == null ? com.tacz.guns.api.DefaultAssets.EMPTY_ATTACHMENT_ID : id;
    }

    @Override
    public void setAttachmentId(ItemStack attachmentStack, @Nullable ResourceLocation attachmentId) {
        CosmeticNBTHelper.setKeychainId(attachmentStack, attachmentId);
    }

    @Override
    public ResourceLocation getSkinId(ItemStack attachmentStack) {
        return null;
    }

    @Override
    public void setSkinId(ItemStack attachmentStack, @Nullable ResourceLocation skinId) {
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
        return AttachmentType.KEYCHAIN;
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
        ResourceLocation id = getKeychainId(stack);
        if (id == null) return Component.translatable("item.guncosmetics.keychain_item", "");
        return KeychainRegistry.get(id)
                .map(kc -> {
                    CosmeticRarity rarity = kc.getRarity();
                    return Component.literal(readableName(kc.getDisplayName()))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rarity.getColor())));
                })
                .orElse(Component.translatable("item.guncosmetics.keychain_item", id.toString()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = getKeychainId(stack);
        if (id == null) return;
        KeychainRegistry.get(id).ifPresent(kc -> {
            kc.getDescription().forEach(line -> tooltip.add(Component.translatable(line).withStyle(ChatFormatting.GRAY)));
            if (CosmeticsConfig.SHOW_RARITY_IN_TOOLTIP.get()) {
                tooltip.add(Component.translatable("tooltip.guncosmetics.rarity",
                        kc.getRarity().getDisplayName()));
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
