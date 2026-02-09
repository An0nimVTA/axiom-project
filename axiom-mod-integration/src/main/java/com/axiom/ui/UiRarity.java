package com.axiom.ui;

public enum UiRarity {
    COMMON(0xFF9E9E9E),
    RARE(0xFF2196F3),
    LEGENDARY(0xFFFFC107);

    private final int color;

    UiRarity(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
