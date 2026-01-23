package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AnalyticsScreen extends Screen {
    private final Screen parent;
    private final int[] economyData = {50, 55, 60, 58, 65, 70, 75, 80};
    private final int[] populationData = {100, 105, 110, 115, 120, 125, 130, 135};
    private final int[] militaryData = {30, 32, 35, 40, 38, 42, 45, 50};

    public AnalyticsScreen(Screen parent) {
        super(Component.literal("Аналитика"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> minecraft.setScreen(parent))
            .bounds(10, 10, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Экспорт отчёта"), b -> 
            NotificationManager.getInstance().success("Отчёт экспортирован!"))
            .bounds(width - 180, 10, 170, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, "📊 Аналитика и отчёты", width / 2, 20, 0xFFFFFF);
        
        int y = 50;
        
        // Economy chart
        graphics.fill(20, y, width - 20, y + 120, 0xAA1E3A5F);
        graphics.drawString(font, "💰 Экономика (последние 8 месяцев)", 30, y + 10, 0xFFFFFF);
        drawChart(graphics, 30, y + 30, width - 60, 80, economyData, 0xFF00AA00);
        
        y += 130;
        
        // Population chart
        graphics.fill(20, y, width / 2 - 10, y + 120, 0xAA5F1E3A);
        graphics.drawString(font, "👥 Население", 30, y + 10, 0xFFFFFF);
        drawChart(graphics, 30, y + 30, width / 2 - 50, 80, populationData, 0xFF0088FF);
        
        // Military chart
        graphics.fill(width / 2 + 10, y, width - 20, y + 120, 0xAA5F3A1E);
        graphics.drawString(font, "⚔️ Военная мощь", width / 2 + 20, y + 10, 0xFFFFFF);
        drawChart(graphics, width / 2 + 20, y + 30, width / 2 - 50, 80, militaryData, 0xFFAA0000);
    }

    private void drawChart(GuiGraphics g, int x, int y, int w, int h, int[] data, int color) {
        g.fill(x, y, x + w, y + h, 0xFF222222);
        int max = 0;
        for (int v : data) if (v > max) max = v;
        int step = w / (data.length - 1);
        for (int i = 0; i < data.length - 1; i++) {
            int x1 = x + i * step;
            int y1 = y + h - (data[i] * h / max);
            int x2 = x + (i + 1) * step;
            int y2 = y + h - (data[i + 1] * h / max);
            drawLine(g, x1, y1, x2, y2, color);
            g.fill(x1 - 2, y1 - 2, x1 + 2, y1 + 2, color);
        }
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }
}
