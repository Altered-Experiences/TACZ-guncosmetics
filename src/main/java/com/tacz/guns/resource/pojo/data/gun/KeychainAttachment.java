package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import org.joml.Vector3f;

public class KeychainAttachment {
    @SerializedName("bone")
    private String bone = "";

    @SerializedName("offset")
    private float[] offset = new float[]{0.0f, 0.0f, 0.0f};

    @SerializedName("rotation")
    private float[] rotation = new float[]{0.0f, 0.0f, 0.0f};

    @SerializedName("scale")
    private float[] scale = new float[]{1.0f, 1.0f, 1.0f};

    public String getBone() {
        return bone;
    }

    public Vector3f getOffset() {
        return toVector(offset, 0.0f);
    }

    public Vector3f getRotation() {
        return toVector(rotation, 0.0f);
    }

    public Vector3f getScale() {
        return toVector(scale, 1.0f);
    }

    private static Vector3f toVector(float[] values, float fallback) {
        float x = values != null && values.length > 0 ? values[0] : fallback;
        float y = values != null && values.length > 1 ? values[1] : fallback;
        float z = values != null && values.length > 2 ? values[2] : fallback;
        return new Vector3f(x, y, z);
    }
}
