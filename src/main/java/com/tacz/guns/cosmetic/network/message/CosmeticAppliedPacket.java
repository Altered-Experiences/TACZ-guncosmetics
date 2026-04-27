package com.tacz.guns.cosmetic.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S→C: Broadcast to all players that a cosmetic was applied/removed.
 * Clients can use this to refresh rendering caches.
 */
public class CosmeticAppliedPacket {

    private final int entityId;
    private final int gunSlot;
    private final ApplyCosmeticPacket.CosmeticType type;
    private final ResourceLocation cosmeticId;

    public CosmeticAppliedPacket(int entityId, int gunSlot, ApplyCosmeticPacket.CosmeticType type, ResourceLocation cosmeticId) {
        this.entityId = entityId;
        this.gunSlot = gunSlot;
        this.type = type;
        this.cosmeticId = cosmeticId;
    }

    public static void encode(CosmeticAppliedPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.gunSlot);
        buf.writeEnum(msg.type);
        buf.writeResourceLocation(msg.cosmeticId);
    }

    public static CosmeticAppliedPacket decode(FriendlyByteBuf buf) {
        return new CosmeticAppliedPacket(
                buf.readVarInt(), buf.readVarInt(),
                buf.readEnum(ApplyCosmeticPacket.CosmeticType.class),
                buf.readResourceLocation());
    }

    public static void handle(CosmeticAppliedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client: could refresh render caches here if needed
            // For now, NBT sync via vanilla handles visual updates
        });
        ctx.get().setPacketHandled(true);
    }
}
