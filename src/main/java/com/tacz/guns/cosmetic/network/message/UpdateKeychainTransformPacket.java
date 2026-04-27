package com.tacz.guns.cosmetic.network.message;

import com.tacz.guns.cosmetic.data.CosmeticNBTHelper;
import com.tacz.guns.api.item.IGun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C->S: Updates keychain transform on an already-customized gun.
 */
public class UpdateKeychainTransformPacket {
    private final int gunSlot;
    private final String bone;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float rotX;
    private final float rotY;
    private final float rotZ;
    private final float scale;

    public UpdateKeychainTransformPacket(int gunSlot, String bone, float offsetX, float offsetY, float offsetZ,
                                         float rotX, float rotY, float rotZ, float scale) {
        this.gunSlot = gunSlot;
        this.bone = bone;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
        this.scale = scale;
    }

    public static void encode(UpdateKeychainTransformPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.gunSlot);
        buf.writeUtf(msg.bone == null ? "" : msg.bone);
        buf.writeFloat(msg.offsetX);
        buf.writeFloat(msg.offsetY);
        buf.writeFloat(msg.offsetZ);
        buf.writeFloat(msg.rotX);
        buf.writeFloat(msg.rotY);
        buf.writeFloat(msg.rotZ);
        buf.writeFloat(msg.scale);
    }

    public static UpdateKeychainTransformPacket decode(FriendlyByteBuf buf) {
        return new UpdateKeychainTransformPacket(
                buf.readVarInt(),
                buf.readUtf(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat());
    }

    public static void handle(UpdateKeychainTransformPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Inventory inv = player.getInventory();
            if (msg.gunSlot < 0 || msg.gunSlot >= inv.getContainerSize()) return;

            ItemStack gunStack = inv.getItem(msg.gunSlot);
            if (IGun.getIGunOrNull(gunStack) == null) return;

            CosmeticNBTHelper.setKeychainTransform(gunStack, msg.bone,
                    msg.offsetX, msg.offsetY, msg.offsetZ,
                    msg.rotX, msg.rotY, msg.rotZ,
                    msg.scale, msg.scale, msg.scale);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
