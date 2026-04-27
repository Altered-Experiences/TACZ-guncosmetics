package com.tacz.guns.cosmetic.network.message;

import com.tacz.guns.cosmetic.config.CosmeticsConfig;
import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
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

import java.util.function.Supplier;

/**
 * C→S: Request to remove skin or keychain from a gun.
 */
public class RemoveCosmeticPacket {

    private final int gunSlot;
    private final ApplyCosmeticPacket.CosmeticType cosmeticType;

    public RemoveCosmeticPacket(int gunSlot, ApplyCosmeticPacket.CosmeticType type) {
        this.gunSlot = gunSlot;
        this.cosmeticType = type;
    }

    public static void encode(RemoveCosmeticPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.gunSlot);
        buf.writeEnum(msg.cosmeticType);
    }

    public static RemoveCosmeticPacket decode(FriendlyByteBuf buf) {
        return new RemoveCosmeticPacket(buf.readVarInt(), buf.readEnum(ApplyCosmeticPacket.CosmeticType.class));
    }

    public static void handle(RemoveCosmeticPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Inventory inv = player.getInventory();
            if (msg.gunSlot < 0 || msg.gunSlot >= inv.getContainerSize()) return;

            ItemStack gunStack = inv.getItem(msg.gunSlot);
            IGun iGun = IGun.getIGunOrNull(gunStack);
            if (iGun == null) return;

            if (msg.cosmeticType == ApplyCosmeticPacket.CosmeticType.SKIN) {
                ResourceLocation skinId = CosmeticNBTHelper.getSkinId(gunStack);
                if (skinId != null) {
                    // Return item if allowed
                    if (CosmeticsConfig.ALLOW_SKIN_REMOVAL.get()) {
                        ItemStack skinItem = AttachmentItemBuilder.create().setId(skinId).build();
                        if (!player.addItem(skinItem)) {
                            player.drop(skinItem, false);
                        }
                    }
                    CosmeticNBTHelper.setSkinId(gunStack, null);
                    iGun.unloadAttachment(gunStack, AttachmentType.SKIN);
                    player.sendSystemMessage(Component.translatable("gui.guncosmetics.skin_removed"));
                }
            } else {
                ResourceLocation kcId = CosmeticNBTHelper.getKeychainId(gunStack);
                if (kcId != null) {
                    if (CosmeticsConfig.ALLOW_SKIN_REMOVAL.get()) {
                        ItemStack kcItem = AttachmentItemBuilder.create().setId(kcId).build();
                        if (!player.addItem(kcItem)) {
                            player.drop(kcItem, false);
                        }
                    }
                    CosmeticNBTHelper.setKeychainId(gunStack, null);
                    iGun.unloadAttachment(gunStack, AttachmentType.KEYCHAIN);
                    player.sendSystemMessage(Component.translatable("gui.guncosmetics.keychain_removed"));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
