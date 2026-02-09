package com.axiom.ui;

public final class UiCardSizing {
    private UiCardSizing() {}

    // Global card scale. 0.2 ~= 5x smaller, 0.4 ~= 2.5x smaller.
    private static final float CARD_SCALE = 0.2f;

    // Main menu cards
    private static final int MAIN_MENU_BASE_W = 240;
    private static final int MAIN_MENU_MIN_W = 190;
    private static final int MAIN_MENU_BASE_H = 260;
    private static final int MAIN_MENU_MIN_H = 220;
    private static final int MAIN_MENU_MAX_H = 300;
    private static final int MAIN_MENU_GAP = 14;

    // Command menu cards
    private static final int COMMAND_MENU_BASE_W = 230;
    private static final int COMMAND_MENU_MIN_W = 185;
    private static final int COMMAND_MENU_BASE_H = 260;
    private static final int COMMAND_MENU_MIN_H = 220;
    private static final int COMMAND_MENU_MAX_H = 300;
    private static final int COMMAND_MENU_GAP = 12;

    // Religion cards
    private static final int RELIGION_BASE_W = 200;
    private static final int RELIGION_MIN_W = 170;
    private static final int RELIGION_BASE_H = 220;
    private static final int RELIGION_MIN_H = 200;
    private static final int RELIGION_MAX_H = 280;
    private static final int RELIGION_GAP = 14;
    private static final int RELIGION_START_Y = 54;

    // Technology tree (grid)
    private static final int TECH_GRID_BASE_W = 230;
    private static final int TECH_GRID_MIN_W = 200;
    private static final int TECH_GRID_BASE_H = 120;
    private static final int TECH_GRID_MIN_H = 110;
    private static final int TECH_GRID_MAX_H = 150;
    private static final int TECH_GRID_GAP = 12;
    private static final int TECH_GRID_START_Y = 96;

    // Language select cards
    private static final int LANG_CARD_W = 230;
    private static final int LANG_CARD_H = 140;
    private static final int LANG_CARD_GAP = 18;

    // Horizontal tech cards
    public static final int TECH_HORIZONTAL_W = cardBase(280);
    public static final int TECH_HORIZONTAL_H = cardBase(68);
    public static final int TECH_HORIZONTAL_BTN_W = cardBase(64);
    public static final int TECH_HORIZONTAL_BTN_H = cardBase(16);

    private static int cardBase(int value) {
        return Math.max(1, Math.round(value * CARD_SCALE));
    }

    private static int scaledCard(int value, float scale) {
        return UiLayout.scaled(cardBase(value), scale);
    }

    public static int mainMenuCardWidth(float scale, int availableWidth) {
        return UiLayout.clamp(Math.min(scaledCard(MAIN_MENU_BASE_W, scale), availableWidth),
            scaledCard(MAIN_MENU_MIN_W, scale), availableWidth);
    }

    public static int mainMenuCardHeight(float scale) {
        return UiLayout.clamp(scaledCard(MAIN_MENU_BASE_H, scale),
            scaledCard(MAIN_MENU_MIN_H, scale), scaledCard(MAIN_MENU_MAX_H, scale));
    }

    public static int mainMenuGap(float scale) {
        return scaledCard(MAIN_MENU_GAP, scale);
    }

    public static int commandMenuCardWidth(float scale, int availableWidth) {
        return UiLayout.clamp(Math.min(scaledCard(COMMAND_MENU_BASE_W, scale), availableWidth),
            scaledCard(COMMAND_MENU_MIN_W, scale), availableWidth);
    }

    public static int commandMenuCardHeight(float scale) {
        return UiLayout.clamp(scaledCard(COMMAND_MENU_BASE_H, scale),
            scaledCard(COMMAND_MENU_MIN_H, scale), scaledCard(COMMAND_MENU_MAX_H, scale));
    }

    public static int commandMenuGap(float scale) {
        return scaledCard(COMMAND_MENU_GAP, scale);
    }

    public static int religionCardWidth(float scale, int availableWidth) {
        return UiLayout.clamp(Math.min(scaledCard(RELIGION_BASE_W, scale), availableWidth),
            scaledCard(RELIGION_MIN_W, scale), availableWidth);
    }

    public static int religionCardHeight(float scale) {
        return UiLayout.clamp(scaledCard(RELIGION_BASE_H, scale),
            scaledCard(RELIGION_MIN_H, scale), scaledCard(RELIGION_MAX_H, scale));
    }

    public static int religionGap(float scale) {
        return scaledCard(RELIGION_GAP, scale);
    }

    public static int religionStartY(float scale) {
        return scaledCard(RELIGION_START_Y, scale);
    }

    public static int techGridCardWidth(float scale, int availableWidth) {
        return UiLayout.clamp(Math.min(scaledCard(TECH_GRID_BASE_W, scale), availableWidth),
            scaledCard(TECH_GRID_MIN_W, scale), availableWidth);
    }

    public static int techGridCardHeight(float scale) {
        return UiLayout.clamp(scaledCard(TECH_GRID_BASE_H, scale),
            scaledCard(TECH_GRID_MIN_H, scale), scaledCard(TECH_GRID_MAX_H, scale));
    }

    public static int techGridGap(float scale) {
        return scaledCard(TECH_GRID_GAP, scale);
    }

    public static int techGridStartY(float scale) {
        return scaledCard(TECH_GRID_START_Y, scale);
    }

    public static int languageCardWidth(float scale) {
        return scaledCard(LANG_CARD_W, scale);
    }

    public static int languageCardHeight(float scale) {
        return scaledCard(LANG_CARD_H, scale);
    }

    public static int languageCardGap(float scale) {
        return scaledCard(LANG_CARD_GAP, scale);
    }
}
