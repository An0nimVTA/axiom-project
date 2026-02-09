package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EcologyScreen extends Screen {
    private final Screen parent;
    
    // Mock data
    private final double pollution = 34.2;
    private final double forestCoverage = 68.5;
    private final double waterQuality = 82.1;
    private final double airQuality = 76.3;
    private final String climate = "Умеренный";

    public EcologyScreen(Screen parent) {
        super(Component.literal("Экология"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> minecraft.setScreen(parent))
            .bounds(10, 10, 80, 20).build());
        
        addRenderableWidget(Button.builder(Component.literal("Очистить территорию"), b -> {
            NotificationManager.getInstance().success("Очистка начата!");
        }).bounds(width - 180, 10, 170, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        UiTheme.drawBackdrop(graphics, width, height);
        
        graphics.drawCenteredString(font, "Система экологии", width / 2, 20, UiTheme.TEXT_PRIMARY);
        
        int y = 50;
        int cardW = (width - 60) / 2;
        
        // Pollution
        UiTheme.drawMinimalCard(graphics, 20, y, 20 + cardW, y + 80, 0xFF5F3A1E, false);
        graphics.drawString(font, "Загрязнение", 30, y + 10, UiTheme.TEXT_PRIMARY, false);
        graphics.drawString(font, String.format("%.1f%%", pollution), 30, y + 30,
            pollution > 50 ? 0xFFB84C4C : 0xFFB28E3A, false);
        int barW = cardW - 20;
        graphics.fill(30, y + 55, 30 + barW, y + 65, UiTheme.BUTTON_BG);
        graphics.fill(30, y + 55, 30 + (int)(barW * pollution / 100), y + 65,
            pollution > 50 ? 0xFFAA0000 : 0xFFAAAA00);
        
        // Forest coverage
        UiTheme.drawMinimalCard(graphics, 40 + cardW, y, 40 + cardW * 2, y + 80, 0xFF1E5F3A, false);
        graphics.drawString(font, "Лесистость", 50 + cardW, y + 10, UiTheme.TEXT_PRIMARY, false);
        graphics.drawString(font, String.format("%.1f%%", forestCoverage), 50 + cardW, y + 30, UiTheme.ACCENT, false);
        graphics.fill(50 + cardW, y + 55, 50 + cardW + barW, y + 65, UiTheme.BUTTON_BG);
        graphics.fill(50 + cardW, y + 55, 50 + cardW + (int)(barW * forestCoverage / 100), y + 65, 0xFF00AA00);
        
        y += 90;
        
        // Water quality
        UiTheme.drawMinimalCard(graphics, 20, y, 20 + cardW, y + 80, 0xFF1E3A5F, false);
        graphics.drawString(font, "Качество воды", 30, y + 10, UiTheme.TEXT_PRIMARY, false);
        graphics.drawString(font, String.format("%.1f%%", waterQuality), 30, y + 30, 0xFF7FB7FF, false);
        graphics.fill(30, y + 55, 30 + barW, y + 65, UiTheme.BUTTON_BG);
        graphics.fill(30, y + 55, 30 + (int)(barW * waterQuality / 100), y + 65, 0xFF0088FF);
        
        // Air quality
        UiTheme.drawMinimalCard(graphics, 40 + cardW, y, 40 + cardW * 2, y + 80, 0xFF3A5F1E, false);
        graphics.drawString(font, "Качество воздуха", 50 + cardW, y + 10, UiTheme.TEXT_PRIMARY, false);
        graphics.drawString(font, String.format("%.1f%%", airQuality), 50 + cardW, y + 30, 0xFFB7D96A, false);
        graphics.fill(50 + cardW, y + 55, 50 + cardW + barW, y + 65, UiTheme.BUTTON_BG);
        graphics.fill(50 + cardW, y + 55, 50 + cardW + (int)(barW * airQuality / 100), y + 65, 0xFF88AA00);
        
        y += 90;
        
        // Climate
        UiTheme.drawMinimalCard(graphics, 20, y, width - 20, y + 60, 0xFF5F5F1E, false);
        graphics.drawString(font, "Климат", 30, y + 10, UiTheme.TEXT_PRIMARY, false);
        graphics.drawString(font, climate, 30, y + 30, UiTheme.ACCENT_WARM, false);
        graphics.drawString(font, "Температура: +18°C | Осадки: 650мм/год", 30, y + 45, UiTheme.TEXT_MUTED, false);

        super.render(graphics, mouseX, mouseY, delta);
    }
}
