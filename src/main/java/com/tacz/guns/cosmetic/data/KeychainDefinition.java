package com.tacz.guns.cosmetic.data;

import net.minecraft.resources.ResourceLocation;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Immutable definition of a keychain, loaded from a cosmetic pack JSON.
 */
public class KeychainDefinition {

    private final ResourceLocation keychainId;
    private final String displayName;
    private final CosmeticRarity rarity;
    private final ResourceLocation model;
    private final ResourceLocation texture;
    @Nullable
    private final ResourceLocation animation;

    // Default attachment transform
    private final String bone;
    private final Vector3f offset;
    private final Vector3f rotation;
    private final Vector3f scale;

    private final List<String> description;
    private final ResourceLocation icon;
    private final AttachmentData attachmentData;

    // Raw byte data for network sync
    @Nullable
    private byte[] textureData;
    @Nullable
    private byte[] modelData;
    @Nullable
    private byte[] animationData;

    public KeychainDefinition(ResourceLocation keychainId, String displayName, CosmeticRarity rarity,
                              ResourceLocation model, ResourceLocation texture,
                              @Nullable ResourceLocation animation,
                              String bone, Vector3f offset, Vector3f rotation, Vector3f scale,
                              List<String> description, ResourceLocation icon) {
        this(keychainId, displayName, rarity, model, texture, animation, bone, offset, rotation, scale,
                description, icon, new AttachmentData());
    }

    public KeychainDefinition(ResourceLocation keychainId, String displayName, CosmeticRarity rarity,
                              ResourceLocation model, ResourceLocation texture,
                              @Nullable ResourceLocation animation,
                              String bone, Vector3f offset, Vector3f rotation, Vector3f scale,
                              List<String> description, ResourceLocation icon, AttachmentData attachmentData) {
        this.keychainId = keychainId;
        this.displayName = displayName;
        this.rarity = rarity;
        this.model = model;
        this.texture = texture;
        this.animation = animation;
        this.bone = bone;
        this.offset = offset;
        this.rotation = rotation;
        this.scale = scale;
        this.description = description == null ? List.of() : description;
        this.icon = icon;
        this.attachmentData = attachmentData == null ? new AttachmentData() : attachmentData;
    }

    public ResourceLocation getKeychainId() { return keychainId; }
    public String getDisplayName() { return displayName; }
    public CosmeticRarity getRarity() { return rarity; }
    public ResourceLocation getModel() { return model; }
    public ResourceLocation getTexture() { return texture; }
    @Nullable public ResourceLocation getAnimation() { return animation; }
    public String getBone() { return bone; }
    public Vector3f getOffset() { return new Vector3f(offset); }
    public Vector3f getRotation() { return new Vector3f(rotation); }
    public Vector3f getScale() { return new Vector3f(scale); }
    public List<String> getDescription() { return description; }
    public ResourceLocation getIcon() { return icon; }
    public AttachmentData getAttachmentData() { return attachmentData; }

    @Nullable public byte[] getTextureData() { return textureData; }
    public void setTextureData(@Nullable byte[] data) { this.textureData = data; }
    @Nullable public byte[] getModelData() { return modelData; }
    public void setModelData(@Nullable byte[] data) { this.modelData = data; }
    @Nullable public byte[] getAnimationData() { return animationData; }
    public void setAnimationData(@Nullable byte[] data) { this.animationData = data; }
}
