package com.tacz.guns.cosmetic.network.message;

import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.tacz.guns.cosmetic.network.CosmeticsNetworkHandler;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * C→S: Request to apply a skin or keychain to the gun in the specified slot.
 */
public class ApplyCosmeticPacket {

    public enum CosmeticType { SKIN, KEYCHAIN }

    private final int gunSlot;
    private final CosmeticType cosmeticType;
    private final ResourceLocation cosmeticId;
    @Nullable
    private final String transformOverrideJson; // only for keychains

    public ApplyCosmeticPacket(int gunSlot, CosmeticType type, ResourceLocation id, @Nullable String transformJson) {
        this.gunSlot = gunSlot;
        this.cosmeticType = type;
        this.cosmeticId = id;
        this.transformOverrideJson = transformJson;
    }

    public static void encode(ApplyCosmeticPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.gunSlot);
        buf.writeEnum(msg.cosmeticType);
        buf.writeResourceLocation(msg.cosmeticId);
        buf.writeBoolean(msg.transformOverrideJson != null);
        if (msg.transformOverrideJson != null) buf.writeUtf(msg.transformOverrideJson);
    }

    public static ApplyCosmeticPacket decode(FriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        CosmeticType type = buf.readEnum(CosmeticType.class);
        ResourceLocation id = buf.readResourceLocation();
        String transform = buf.readBoolean() ? buf.readUtf() : null;
        return new ApplyCosmeticPacket(slot, type, id, transform);
    }

    public static void handle(ApplyCosmeticPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Inventory inv = player.getInventory();
            if (msg.gunSlot < 0 || msg.gunSlot >= inv.getContainerSize()) return;

            ItemStack gunStack = inv.getItem(msg.gunSlot);
            IGun iGun = IGun.getIGunOrNull(gunStack);
            if (iGun == null) {
                player.sendSystemMessage(Component.translatable("gui.guncosmetics.no_gun_in_hand"));
                return;
            }

            boolean applied;
            if (msg.cosmeticType == CosmeticType.SKIN) {
                applied = applySkin(player, gunStack, iGun, msg.cosmeticId);
            } else {
                applied = applyKeychain(player, gunStack, msg.cosmeticId);
            }
            if (!applied) return;

            // Broadcast confirmation
            CosmeticsNetworkHandler.sendToAll(new CosmeticAppliedPacket(
                    player.getId(), msg.gunSlot, msg.cosmeticType, msg.cosmeticId));
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean applySkin(ServerPlayer player, ItemStack gunStack, IGun iGun, ResourceLocation skinId) {
        // Validate skin exists
        var skinOpt = SkinRegistry.get(skinId);
        if (skinOpt.isEmpty()) return false;

        SkinDefinition skin = skinOpt.get();
        ResourceLocation gunId = iGun.getGunId(gunStack);

        // Validate compatibility
        if (!skin.isApplicableTo(gunId)) {
            player.sendSystemMessage(Component.translatable("gui.guncosmetics.incompatible"));
            return false;
        }

        // Find and consume skin item from inventory
        int skinSlot = findCosmeticItem(player, skinId, true);
        boolean alreadyApplied = skinId.equals(CosmeticNBTHelper.getSkinId(gunStack));
        if (skinSlot == -1 && !alreadyApplied) {
            player.sendSystemMessage(Component.translatable("gui.guncosmetics.no_item"));
            return false;
        }

        if (skinSlot != -1 && !alreadyApplied) {
            player.getInventory().getItem(skinSlot).shrink(1);
        }

        CosmeticNBTHelper.setSkinId(gunStack, skinId);
        iGun.installAttachment(gunStack, AttachmentItemBuilder.create().setId(skinId).build());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("gui.guncosmetics.skin_applied"));
        return true;
    }

    private static boolean applyKeychain(ServerPlayer player, ItemStack gunStack, ResourceLocation keychainId) {
        if (KeychainRegistry.get(keychainId).isEmpty()) return false;

        int kcSlot = findCosmeticItem(player, keychainId, false);
        boolean alreadyApplied = keychainId.equals(CosmeticNBTHelper.getKeychainId(gunStack));
        if (kcSlot == -1 && !alreadyApplied) {
            player.sendSystemMessage(Component.translatable("gui.guncosmetics.no_item"));
            return false;
        }

        if (kcSlot != -1 && !alreadyApplied) {
            player.getInventory().getItem(kcSlot).shrink(1);
        }
        IGun iGun = IGun.getIGunOrNull(gunStack);
        CosmeticNBTHelper.setKeychainId(gunStack, keychainId);
        if (iGun != null) {
            iGun.installAttachment(gunStack, AttachmentItemBuilder.create().setId(keychainId).build());
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("gui.guncosmetics.keychain_applied"));
        return true;
    }

    private static int findCosmeticItem(ServerPlayer player, ResourceLocation id, boolean isSkin) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            IAttachment attachment = IAttachment.getIAttachmentOrNull(stack);
            if (attachment == null) {
                continue;
            }
            AttachmentType expectedType = isSkin ? AttachmentType.SKIN : AttachmentType.KEYCHAIN;
            if (attachment.getType(stack) == expectedType && id.equals(attachment.getAttachmentId(stack))) {
                return i;
            }
        }
        return -1;
    }
}
