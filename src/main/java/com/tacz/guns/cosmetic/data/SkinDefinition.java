package com.tacz.guns.cosmetic.data;

import net.minecraft.resources.ResourceLocation;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Immutable definition of a weapon skin, built from normal TACZ attachment data.
 */
public class SkinDefinition {

    public enum SkinType {
        SPECIFIC,
        UNIVERSAL
    }

    public enum BlendMode {
        MULTIPLY,
        OVERLAY,
        SCREEN,
        ADD;

        public static BlendMode fromString(String s) {
            if (s == null) return MULTIPLY;
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return MULTIPLY;
            }
        }
    }

    private final ResourceLocation skinId;
    private final SkinType type;
    private final String displayName;
    private final CosmeticRarity rarity;

    // For SPECIFIC skins
    @Nullable
    private final ResourceLocation targetGun;
    @Nullable
    private final ResourceLocation texture;
    @Nullable
    private final ResourceLocation modelOverride;

    // For UNIVERSAL skins
    @Nullable
    private final ResourceLocation overlayTexture;
    private final BlendMode blendMode;

    private final List<String> description;
    private final ResourceLocation icon;
    private final AttachmentData attachmentData;

    public SkinDefinition(ResourceLocation skinId, SkinType type, String displayName, CosmeticRarity rarity,
                          @Nullable ResourceLocation targetGun, @Nullable ResourceLocation texture,
                          @Nullable ResourceLocation modelOverride, @Nullable ResourceLocation overlayTexture,
                          BlendMode blendMode, List<String> description, ResourceLocation icon) {
        this(skinId, type, displayName, rarity, targetGun, texture, modelOverride, overlayTexture,
                blendMode, description, icon, new AttachmentData());
    }

    public SkinDefinition(ResourceLocation skinId, SkinType type, String displayName, CosmeticRarity rarity,
                          @Nullable ResourceLocation targetGun, @Nullable ResourceLocation texture,
                          @Nullable ResourceLocation modelOverride, @Nullable ResourceLocation overlayTexture,
                          BlendMode blendMode, List<String> description, ResourceLocation icon,
                          AttachmentData attachmentData) {
        this.skinId = skinId;
        this.type = type;
        this.displayName = displayName;
        this.rarity = rarity;
        this.targetGun = targetGun;
        this.texture = texture;
        this.modelOverride = modelOverride;
        this.overlayTexture = overlayTexture;
        this.blendMode = blendMode;
        this.description = description == null ? List.of() : description;
        this.icon = icon;
        this.attachmentData = attachmentData == null ? new AttachmentData() : attachmentData;
    }

    public ResourceLocation getSkinId() { return skinId; }
    public SkinType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public CosmeticRarity getRarity() { return rarity; }
    @Nullable public ResourceLocation getTargetGun() { return targetGun; }
    @Nullable public ResourceLocation getTexture() { return texture; }
    @Nullable public ResourceLocation getModelOverride() { return modelOverride; }
    @Nullable public ResourceLocation getOverlayTexture() { return overlayTexture; }
    public BlendMode getBlendMode() { return blendMode; }
    public List<String> getDescription() { return description; }
    public ResourceLocation getIcon() { return icon; }
    public AttachmentData getAttachmentData() { return attachmentData; }

    /**
     * Check if this skin is applicable to the given gun ID.
     */
    public boolean isApplicableTo(ResourceLocation gunId) {
        if (type == SkinType.UNIVERSAL) return true;
        return targetGun != null && targetGun.equals(gunId);
    }
}
