package com.tacz.guns.cosmetic.network.message;

import com.tacz.guns.cosmetic.data.CosmeticRarity;
import com.tacz.guns.cosmetic.data.KeychainDefinition;
import com.tacz.guns.cosmetic.data.SkinDefinition;
import com.tacz.guns.cosmetic.registry.KeychainRegistry;
import com.tacz.guns.cosmetic.registry.SkinRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C: Full sync of skin and keychain registries on player join.
 */
public class SyncCosmeticsPacket {

    private List<SkinDefinition> skins;
    private List<KeychainDefinition> keychains;

    public SyncCosmeticsPacket() {
        this.skins = SkinRegistry.getAll();
        this.keychains = KeychainRegistry.getAll();
    }

    public SyncCosmeticsPacket(List<SkinDefinition> skins, List<KeychainDefinition> keychains) {
        this.skins = skins;
        this.keychains = keychains;
    }

    public static void encode(SyncCosmeticsPacket msg, FriendlyByteBuf buf) {
        // Skins
        buf.writeVarInt(msg.skins.size());
        for (SkinDefinition skin : msg.skins) {
            buf.writeResourceLocation(skin.getSkinId());
            buf.writeUtf(skin.getType().name());
            buf.writeUtf(skin.getDisplayName());
            buf.writeUtf(skin.getRarity().getId());
            buf.writeBoolean(skin.getTargetGun() != null);
            if (skin.getTargetGun() != null) buf.writeResourceLocation(skin.getTargetGun());
            buf.writeBoolean(skin.getTexture() != null);
            if (skin.getTexture() != null) buf.writeResourceLocation(skin.getTexture());
            buf.writeBoolean(skin.getModelOverride() != null);
            if (skin.getModelOverride() != null) buf.writeResourceLocation(skin.getModelOverride());
            buf.writeBoolean(skin.getOverlayTexture() != null);
            if (skin.getOverlayTexture() != null) buf.writeResourceLocation(skin.getOverlayTexture());
            buf.writeUtf(skin.getBlendMode().name());
            
            buf.writeVarInt(skin.getDescription().size());
            for (String desc : skin.getDescription()) buf.writeUtf(desc);
            buf.writeBoolean(skin.getIcon() != null);
            if (skin.getIcon() != null) buf.writeResourceLocation(skin.getIcon());
            // Note: binary texture/model data is NOT sent over the network
            // to avoid exceeding the 1MB packet size limit.
            // Clients load textures from their local pack files.
        }

        // Keychains
        buf.writeVarInt(msg.keychains.size());
        for (KeychainDefinition kc : msg.keychains) {
            buf.writeResourceLocation(kc.getKeychainId());
            buf.writeUtf(kc.getDisplayName());
            buf.writeUtf(kc.getRarity().getId());
            buf.writeResourceLocation(kc.getModel());
            buf.writeResourceLocation(kc.getTexture());
            buf.writeBoolean(kc.getAnimation() != null);
            if (kc.getAnimation() != null) buf.writeResourceLocation(kc.getAnimation());
            buf.writeUtf(kc.getBone());
            writeVec3(buf, kc.getOffset());
            writeVec3(buf, kc.getRotation());
            writeVec3(buf, kc.getScale());

            buf.writeVarInt(kc.getDescription().size());
            for (String desc : kc.getDescription()) buf.writeUtf(desc);
            buf.writeBoolean(kc.getIcon() != null);
            if (kc.getIcon() != null) buf.writeResourceLocation(kc.getIcon());
            // Note: binary texture/model data is NOT sent over the network.
        }
    }

    public static SyncCosmeticsPacket decode(FriendlyByteBuf buf) {
        int skinCount = buf.readVarInt();
        List<SkinDefinition> skins = new ArrayList<>(skinCount);
        for (int i = 0; i < skinCount; i++) {
            ResourceLocation skinId = buf.readResourceLocation();
            SkinDefinition.SkinType type = SkinDefinition.SkinType.valueOf(buf.readUtf());
            String displayName = buf.readUtf();
            CosmeticRarity rarity = CosmeticRarity.fromString(buf.readUtf());
            ResourceLocation targetGun = buf.readBoolean() ? buf.readResourceLocation() : null;
            ResourceLocation texture = buf.readBoolean() ? buf.readResourceLocation() : null;
            ResourceLocation modelOverride = buf.readBoolean() ? buf.readResourceLocation() : null;
            ResourceLocation overlayTexture = buf.readBoolean() ? buf.readResourceLocation() : null;
            SkinDefinition.BlendMode blendMode = SkinDefinition.BlendMode.fromString(buf.readUtf());

            int descCount = buf.readVarInt();
            List<String> description = new ArrayList<>(descCount);
            for (int j = 0; j < descCount; j++) description.add(buf.readUtf());
            ResourceLocation icon = buf.readBoolean() ? buf.readResourceLocation() : null;

            SkinDefinition skin = new SkinDefinition(skinId, type, displayName, rarity,
                    targetGun, texture, modelOverride, overlayTexture, blendMode, description, icon);
            skins.add(skin);
        }

        int kcCount = buf.readVarInt();
        List<KeychainDefinition> keychains = new ArrayList<>(kcCount);
        for (int i = 0; i < kcCount; i++) {
            ResourceLocation kcId = buf.readResourceLocation();
            String displayName = buf.readUtf();
            CosmeticRarity rarity = CosmeticRarity.fromString(buf.readUtf());
            ResourceLocation model = buf.readResourceLocation();
            ResourceLocation texture = buf.readResourceLocation();
            ResourceLocation animation = buf.readBoolean() ? buf.readResourceLocation() : null;
            String bone = buf.readUtf();
            Vector3f offset = readVec3(buf);
            Vector3f rotation = readVec3(buf);
            Vector3f scale = readVec3(buf);

            int descCount = buf.readVarInt();
            List<String> description = new ArrayList<>(descCount);
            for (int j = 0; j < descCount; j++) description.add(buf.readUtf());
            ResourceLocation icon = buf.readBoolean() ? buf.readResourceLocation() : null;

            KeychainDefinition kc = new KeychainDefinition(kcId, displayName, rarity,
                    model, texture, animation, bone, offset, rotation, scale, description, icon);
            keychains.add(kc);
        }

        return new SyncCosmeticsPacket(skins, keychains);
    }

    public static void handle(SyncCosmeticsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side: replace registries
            SkinRegistry.clear();
            for (SkinDefinition skin : msg.skins) {
                SkinRegistry.register(skin);
            }
            KeychainRegistry.clear();
            for (KeychainDefinition kc : msg.keychains) {
                KeychainRegistry.register(kc);
            }
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tacz.guns.cosmetic.client.CosmeticClientEvents.onCosmeticsSynced());
        });
        ctx.get().setPacketHandled(true);
    }

    private static void writeOptionalBytes(FriendlyByteBuf buf, byte[] data) {
        if (data != null && data.length > 0) {
            buf.writeBoolean(true);
            buf.writeByteArray(data);
        } else {
            buf.writeBoolean(false);
        }
    }

    private static byte[] readOptionalBytes(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readByteArray() : null;
    }

    private static void writeVec3(FriendlyByteBuf buf, Vector3f v) {
        buf.writeFloat(v.x());
        buf.writeFloat(v.y());
        buf.writeFloat(v.z());
    }

    private static Vector3f readVec3(FriendlyByteBuf buf) {
        return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}
