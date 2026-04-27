package com.tacz.guns.cosmetic.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;

public final class CosmeticAttachmentData {
    private CosmeticAttachmentData() {
    }

    public static AttachmentData parse(JsonObject cosmeticJson) {
        JsonElement element = cosmeticJson.has("attachment_data")
                ? cosmeticJson.get("attachment_data")
                : cosmeticJson;
        AttachmentData data = CommonAssetsManager.GSON.fromJson(element, AttachmentData.class);
        if (data == null || !element.isJsonObject()) {
            return data;
        }

        JsonObject object = element.getAsJsonObject();
        String json = CommonAssetsManager.GSON.toJson(element);
        AttachmentPropertyManager.getModifiers().forEach((key, value) -> {
            if (object.has(key) || object.has(value.getOptionalFields())) {
                JsonProperty<?> property = value.readJson(json);
                property.initComponents();
                data.addModifier(key, property);
            }
        });
        return data;
    }
}
