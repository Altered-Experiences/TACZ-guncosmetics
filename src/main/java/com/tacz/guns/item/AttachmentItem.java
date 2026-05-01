package com.tacz.guns.item;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.renderer.item.AttachmentItemRenderer;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.cosmetic.data.CosmeticRarity;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.tacz.guns.util.datafixer.AttachmentIdFix.updateAttachmentIdInTag;

public class AttachmentItem extends Item implements AttachmentItemDataAccessor {
    public AttachmentItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation attachmentId = this.getAttachmentId(stack);
        Optional<CosmeticRarity> cosmeticRarity = getCosmeticRarity(attachmentId);
        if (cosmeticRarity.isPresent()) {
            Optional<ClientAttachmentIndex> attachmentIndex = TimelessAPI.getClientAttachmentIndex(attachmentId);
            if (attachmentIndex.isPresent()) {
                return Component.translatable(attachmentIndex.get().getName())
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(cosmeticRarity.get().getColor())));
            }
            Optional<Component> cosmeticName = getCosmeticDisplayName(attachmentId);
            if (cosmeticName.isPresent()) {
                return cosmeticName.get().copy()
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(cosmeticRarity.get().getColor())));
            }
            return super.getName(stack);
        }
        Optional<ClientAttachmentIndex> attachmentIndex = TimelessAPI.getClientAttachmentIndex(attachmentId);
        if (attachmentIndex.isPresent()) {
            return Component.translatable(attachmentIndex.get().getName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        ResourceLocation attachmentId = this.getAttachmentId(stack);
        getCosmeticRarity(attachmentId).ifPresent(rarity -> {
            tooltip.add(Component.translatable("tooltip.tacz.rarity", rarity.getDisplayName()));
        });
    }

    private static Optional<Component> getCosmeticDisplayName(ResourceLocation attachmentId) {
        Optional<Component> skinName = CommonAssetsManager.getSkinAttachment(attachmentId)
                .map(skin -> Component.translatable(skin.getDisplayName()));
        if (skinName.isPresent()) {
            return skinName;
        }
        return CommonAssetsManager.getKeychainAttachment(attachmentId)
                .map(keychain -> Component.translatable(keychain.getDisplayName()));
    }

    private static Optional<CosmeticRarity> getCosmeticRarity(ResourceLocation attachmentId) {
        Optional<CosmeticRarity> skinRarity = CommonAssetsManager.getSkinAttachment(attachmentId).map(skin -> skin.getRarity());
        if (skinRarity.isPresent()) {
            return skinRarity;
        }
        return CommonAssetsManager.getKeychainAttachment(attachmentId).map(keychain -> keychain.getRarity());
    }

    private static Comparator<Map.Entry<ResourceLocation, CommonAttachmentIndex>> idNameSort() {
        return Comparator.comparingInt(m -> m.getValue().getSort());
    }

    public static NonNullList<ItemStack> fillItemCategory(AttachmentType type) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonAttachmentIndex().stream().sorted(idNameSort()).forEach(entry -> {
            if (entry.getValue().getPojo().isHidden()) {
                return;
            }
            if (type.equals(entry.getValue().getType())) {
                ItemStack itemStack = AttachmentItemBuilder.create().setId(entry.getKey()).build();
                stacks.add(itemStack);
            }
        });
        return stacks;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            AttachmentItemRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new AttachmentItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }

                return renderer;
            }
        });
    }

    @Override
    @Nonnull
    public AttachmentType getType(ItemStack attachmentStack) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (iAttachment != null) {
            ResourceLocation id = iAttachment.getAttachmentId(attachmentStack);
            return TimelessAPI.getCommonAttachmentIndex(id).map(CommonAttachmentIndex::getType).orElse(AttachmentType.NONE);
        } else {
            return AttachmentType.NONE;
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new AttachmentItemTooltip(this.getAttachmentId(stack), this.getType(stack), stack));
    }

    @Override
    public void verifyTagAfterLoad(@NotNull CompoundTag tag) {
        updateAttachmentIdInTag(tag);
    }
}
