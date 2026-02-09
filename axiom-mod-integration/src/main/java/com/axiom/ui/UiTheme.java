package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class UiTheme {
    public static final int ACCENT = 0xFF56C2B6;
    public static final int ACCENT_WARM = 0xFFE5B35A;
    public static final int BACKDROP_TOP = 0xFF0D1117;
    public static final int BACKDROP_MID = 0xFF10161E;
    public static final int BACKDROP_BOTTOM = 0xFF0A0F14;

    public static final int PANEL = 0xFF121821;
    public static final int PANEL_BORDER = 0xFF232C37;
    public static final int PANEL_HILITE = 0x1A56C2B6;

    public static final int TEXT_PRIMARY = 0xFFE9EEF3;
    public static final int TEXT_MUTED = 0xFFA5AFBA;
    public static final int TEXT_DIM = 0xFF7A848F;

    public static final int BUTTON_BG = 0xFF141B24;
    public static final int BUTTON_BG_ACTIVE = 0xFF1B2430;
    public static final int BUTTON_BORDER = 0xFF2A3440;

    public static final int CARD_BG = 0xFF141B24;
    public static final int CARD_BG_HOVER = 0xFF1A2330;
    public static final int CARD_BORDER = 0xFF2B3542;
    public static final int CARD_ACCENT = 0xFF56C2B6;
    public static final int CARD_BG_MINIMAL = 0xFF0F141B;
    public static final int CARD_BG_MINIMAL_HOVER = 0xFF141A22;
    public static final int CARD_BORDER_MINIMAL = 0xFF1F2932;
    public static final int CARD_DIVIDER = 0xFF1C2430;

    private UiTheme() {}

    public static void drawBackdrop(GuiGraphics gfx, int width, int height) {
        fillVerticalGradient(gfx, 0, 0, width, height, BACKDROP_TOP, BACKDROP_BOTTOM);
        fillVerticalGradient(gfx, 0, 0, width, height / 2, BACKDROP_TOP, BACKDROP_MID);
        gfx.fill(0, 0, width, 1, ACCENT);
        gfx.fill(0, height - 1, width, height, 0x80222A35);

        int band = 32;
        for (int y = 0; y < height; y += band) {
            gfx.fill(0, y, width, y + 1, 0x0E000000);
        }
    }

    public static void drawPanel(GuiGraphics gfx, int x0, int y0, int x1, int y1) {
        gfx.fill(x0, y0, x1, y1, PANEL);
        gfx.renderOutline(x0, y0, x1 - x0, y1 - y0, PANEL_BORDER);
        gfx.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, PANEL_HILITE);
    }

    public static void drawCard(GuiGraphics gfx, int x0, int y0, int x1, int y1, int accent, boolean hovered) {
        int bg = hovered ? CARD_BG_HOVER : CARD_BG;
        gfx.fill(x0, y0, x1, y1, bg);
        gfx.renderOutline(x0, y0, x1 - x0, y1 - y0, CARD_BORDER);
        gfx.fill(x0, y0, x0 + 3, y1, accent);
    }

    public static void drawMinimalCard(GuiGraphics gfx, int x0, int y0, int x1, int y1, int accent, boolean hovered) {
        int bg = hovered ? CARD_BG_MINIMAL_HOVER : CARD_BG_MINIMAL;
        int border = hovered ? withAlpha(accent, 190) : CARD_BORDER_MINIMAL;
        gfx.fill(x0, y0, x1, y1, bg);
        gfx.renderOutline(x0, y0, x1 - x0, y1 - y0, border);
        gfx.fill(x0, y0, x1, y0 + 1, withAlpha(accent, 120));
    }

    public static void drawChip(GuiGraphics gfx, int x0, int y0, int x1, int y1, int border, int bg) {
        gfx.fill(x0, y0, x1, y1, bg);
        gfx.renderOutline(x0, y0, x1 - x0, y1 - y0, border);
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static void fillVerticalGradient(GuiGraphics gfx, int x0, int y0, int x1, int y1, int top, int bottom) {
        int height = Math.max(1, y1 - y0);
        int steps = Math.min(32, height);
        for (int i = 0; i < steps; i++) {
            float t0 = i / (float) steps;
            float t1 = (i + 1) / (float) steps;
            int ys = y0 + Math.round(t0 * height);
            int ye = y0 + Math.round(t1 * height);
            int color = lerpColor(top, bottom, (t0 + t1) * 0.5f);
            gfx.fill(x0, ys, x1, ye, color);
        }
    }

    public static int lerpColor(int a, int b, float t) {
        int aA = (a >> 24) & 0xFF;
        int aR = (a >> 16) & 0xFF;
        int aG = (a >> 8) & 0xFF;
        int aB = a & 0xFF;
        int bA = (b >> 24) & 0xFF;
        int bR = (b >> 16) & 0xFF;
        int bG = (b >> 8) & 0xFF;
        int bB = b & 0xFF;
        int rA = Mth.clamp((int) Mth.lerp(t, aA, bA), 0, 255);
        int rR = Mth.clamp((int) Mth.lerp(t, aR, bR), 0, 255);
        int rG = Mth.clamp((int) Mth.lerp(t, aG, bG), 0, 255);
        int rB = Mth.clamp((int) Mth.lerp(t, aB, bB), 0, 255);
        return (rA << 24) | (rR << 16) | (rG << 8) | rB;
    }

    public static int shade(int color, float amount) {
        int target = amount >= 0 ? 0xFFFFFFFF : 0xFF000000;
        float t = Math.min(Math.abs(amount), 1.0f);
        return lerpColor(color, target, t);
    }
}
