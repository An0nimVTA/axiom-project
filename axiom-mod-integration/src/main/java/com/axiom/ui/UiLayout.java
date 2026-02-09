package com.axiom.ui;

import net.minecraft.util.Mth;

public final class UiLayout {
    private UiLayout() {}

    public static float scaleFor(int width, int height) {
        int base = Math.min(width, height);
        float scale = base / 900.0f;
        return Mth.clamp(scale, 0.80f, 1.25f);
    }

    public static int scaled(int value, float scale) {
        return Math.round(value * scale);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
