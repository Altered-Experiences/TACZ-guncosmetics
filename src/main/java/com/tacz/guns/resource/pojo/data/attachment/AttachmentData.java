package com.tacz.guns.resource.pojo.data.attachment;

import com.google.common.collect.Maps;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import com.tacz.guns.api.modifier.JsonProperty;

import javax.annotation.Nullable;
import java.util.Map;

public class AttachmentData {
    @Expose(serialize = false, deserialize = false)
    private Map<String, JsonProperty<?>> modifier = Maps.newHashMap();

    @SerializedName("weight")
    private float weight = 0;

    @SerializedName("extended_mag_level")
    private int extendedMagLevel = 0;

    @SerializedName("melee")
    @Nullable
    private MeleeData meleeData = null;

    @SerializedName("cosmetic")
    @Nullable
    private CosmeticData cosmeticData = null;

    public float getWeight() {
        return weight;
    }

    public int getExtendedMagLevel() {
        return extendedMagLevel;
    }

    @Nullable
    public MeleeData getMeleeData() {
        return meleeData;
    }

    public void addModifier(String id, JsonProperty<?> jsonProperty) {
        modifier.put(id, jsonProperty);
    }

    public Map<String, JsonProperty<?>> getModifier() {
        return modifier;
    }

    @Nullable
    public CosmeticData getCosmeticData() {
        return cosmeticData;
    }

    public static class CosmeticData {
        @SerializedName("rarity")
        private String rarity = "common";

        @SerializedName("description")
        @Nullable
        private String[] description;

        @SerializedName("icon")
        @Nullable
        private ResourceLocation icon;

        @SerializedName("skin")
        @Nullable
        private SkinData skin;

        @SerializedName("keychain")
        @Nullable
        private KeychainData keychain;

        public String getRarity() {
            return rarity;
        }

        @Nullable
        public String[] getDescription() {
            return description;
        }

        @Nullable
        public ResourceLocation getIcon() {
            return icon;
        }

        @Nullable
        public SkinData getSkin() {
            return skin;
        }

        @Nullable
        public KeychainData getKeychain() {
            return keychain;
        }
    }

    public static class SkinData {
        @SerializedName("type")
        private String type = "specific";

        @SerializedName("target_gun")
        @Nullable
        private ResourceLocation targetGun;

        @SerializedName("texture")
        @Nullable
        private ResourceLocation texture;

        @SerializedName("model_override")
        @Nullable
        private ResourceLocation modelOverride;

        @SerializedName("overlay_texture")
        @Nullable
        private ResourceLocation overlayTexture;

        @SerializedName("blend_mode")
        private String blendMode = "multiply";

        public String getType() {
            return type;
        }

        @Nullable
        public ResourceLocation getTargetGun() {
            return targetGun;
        }

        @Nullable
        public ResourceLocation getTexture() {
            return texture;
        }

        @Nullable
        public ResourceLocation getModelOverride() {
            return modelOverride;
        }

        @Nullable
        public ResourceLocation getOverlayTexture() {
            return overlayTexture;
        }

        public String getBlendMode() {
            return blendMode;
        }
    }

    public static class KeychainData {
        @SerializedName("model")
        private ResourceLocation model;

        @SerializedName("texture")
        private ResourceLocation texture;

        @SerializedName("default_attachment")
        @Nullable
        private KeychainAttachmentData defaultAttachment;

        @Nullable
        public ResourceLocation getModel() {
            return model;
        }

        @Nullable
        public ResourceLocation getTexture() {
            return texture;
        }

        @Nullable
        public KeychainAttachmentData getDefaultAttachment() {
            return defaultAttachment;
        }
    }

    public static class KeychainAttachmentData {
        @SerializedName("bone")
        private String bone = "stock";

        @SerializedName("offset")
        @Nullable
        private float[] offset;

        @SerializedName("rotation")
        @Nullable
        private float[] rotation;

        @SerializedName("scale")
        @Nullable
        private float[] scale;

        public String getBone() {
            return bone;
        }

        @Nullable
        public float[] getOffset() {
            return offset;
        }

        @Nullable
        public float[] getRotation() {
            return rotation;
        }

        @Nullable
        public float[] getScale() {
            return scale;
        }
    }
}
