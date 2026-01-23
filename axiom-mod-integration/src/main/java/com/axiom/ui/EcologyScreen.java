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
        super.render(graphics, mouseX, mouseY, delta);
        
        graphics.drawCenteredString(font, "🌿 Система экологии", width / 2, 20, 0xFFFFFF);
        
        int y = 50;
        int cardW = (width - 60) / 2;
        
        // Pollution
        graphics.fill(20, y, 20 + cardW, y + 80, 0xAA5F3A1E);
        graphics.drawString(font, "☢ Загрязнение:", 30, y + 10, 0xFFFFFF);
        graphics.drawString(font, String.format("%.1f%%", pollution), 30, y + 30, pollution > 50 ? 0xFF0000 : 0xFFAA00);
        int barW = cardW - 20;
        graphics.fill(30, y + 55, 30 + barW, y + 65, 0xFF333333);
        graphics.fill(30, y + 55, 30 + (int)(barW * pollution / 100), y + 65, pollution > 50 ? 0xFFAA0000 : 0xFFAAAA00);
        
        // Forest coverage
        graphics.fill(40 + cardW, y, 40 + cardW * 2, y + 80, 0xAA1E5F3A);
        graphics.drawString(font, "🌲 Лесистость:", 50 + cardW, y + 10, 0xFFFFFF);
        graphics.drawString(font, String.format("%.1f%%", forestCoverage), 50 + cardW, y + 30, 0x00FF00);
        graphics.fill(50 + cardW, y + 55, 50 + cardW + barW, y + 65, 0xFF333333);
        graphics.fill(50 + cardW, y + 55, 50 + cardW + (int)(barW * forestCoverage / 100), y + 65, 0xFF00AA00);
        
        y += 90;
        
        // Water quality
        graphics.fill(20, y, 20 + cardW, y + 80, 0xAA1E3A5F);
        graphics.drawString(font, "💧 Качество воды:", 30, y + 10, 0xFFFFFF);
        graphics.drawString(font, String.format("%.1f%%", waterQuality), 30, y + 30, 0x00AAFF);
        graphics.fill(30, y + 55, 30 + barW, y + 65, 0xFF333333);
        graphics.fill(30, y + 55, 30 + (int)(barW * waterQuality / 100), y + 65, 0xFF0088FF);
        
        // Air quality
        graphics.fill(40 + cardW, y, 40 + cardW * 2, y + 80, 0xAA3A5F1E);
        graphics.drawString(font, "🌬 Качество воздуха:", 50 + cardW, y + 10, 0xFFFFFF);
        graphics.drawString(font, String.format("%.1f%%", airQuality), 50 + cardW, y + 30, 0xAAFF00);
        graphics.fill(50 + cardW, y + 55, 50 + cardW + barW, y + 65, 0xFF333333);
        graphics.fill(50 + cardW, y + 55, 50 + cardW + (int)(barW * airQuality / 100), y + 65, 0xFF88AA00);
        
        y += 90;
        
        // Climate
        graphics.fill(20, y, width - 20, y + 60, 0xAA5F5F1E);
        graphics.drawString(font, "🌡 Климат:", 30, y + 10, 0xFFFFFF);
        graphics.drawString(font, climate, 30, y + 30, 0xFFFF00);
        graphics.drawString(font, "Температура: +18°C | Осадки: 650мм/год", 30, y + 45, 0xCCCCCC);
    }
}
