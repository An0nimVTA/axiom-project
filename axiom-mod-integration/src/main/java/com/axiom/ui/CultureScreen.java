package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class CultureScreen extends Screen {
    private final Screen parent;
    
    // Mock data
    private final double cultureLevel = 72.3;
    private final List<Tradition> traditions = Arrays.asList(
        new Tradition("Фестиваль урожая", 100, true),
        new Tradition("День основания", 90, true),
        new Tradition("Музыкальный конкурс", 60, false),
        new Tradition("Спортивные игры", 30, false)
    );
    private final double culturalInfluence = 45.8;

    public CultureScreen(Screen parent) {
        super(Component.literal("Культура"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> minecraft.setScreen(parent))
            .bounds(10, 10, 80, 20).build());
        
        addRenderableWidget(Button.builder(Component.literal("Создать традицию"), b -> {
            NotificationManager.getInstance().success("Традиция создана!");
        }).bounds(width - 180, 10, 170, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        
        graphics.drawCenteredString(font, "🎭 Система культуры", width / 2, 20, 0xFFFFFF);
        
        int y = 50;
        
        // Culture level
        graphics.fill(20, y, width / 2 - 10, y + 60, 0xAA5F1E3A);
        graphics.drawString(font, "Уровень культуры:", 30, y + 10, 0xFFFFFF);
        graphics.drawString(font, String.format("%.1f%%", cultureLevel), 30, y + 25, 0xFFAA00);
        int barW = width / 2 - 60;
        graphics.fill(30, y + 40, 30 + barW, y + 50, 0xFF333333);
        graphics.fill(30, y + 40, 30 + (int)(barW * cultureLevel / 100), y + 50, 0xFFAA6600);
        
        // Cultural influence
        graphics.fill(width / 2 + 10, y, width - 20, y + 60, 0xAA3A1E5F);
        graphics.drawString(font, "Культурное влияние:", width / 2 + 20, y + 10, 0xFFFFFF);
        graphics.drawString(font, String.format("%.1f%%", culturalInfluence), width / 2 + 20, y + 25, 0xAA00FF);
        graphics.fill(width / 2 + 20, y + 40, width - 30, y + 50, 0xFF333333);
        graphics.fill(width / 2 + 20, y + 40, width / 2 + 20 + (int)((width / 2 - 50) * culturalInfluence / 100), y + 50, 0xFF8800AA);
        
        // Traditions
        y = 120;
        graphics.drawString(font, "Традиции:", 30, y, 0xFFFFFF);
        y += 20;
        
        for (Tradition t : traditions) {
            if (y + 70 > height - 10) break;
            
            int color = t.active ? 0xAA00AA00 : (t.popularity > 50 ? 0xAA0066CC : 0xAA666666);
            graphics.fill(20, y, width - 20, y + 70, color);
            
            String icon = t.active ? "✓" : (t.popularity > 50 ? "◐" : "○");
            graphics.drawString(font, icon + " " + t.name, 30, y + 10, 0xFFFFFF);
            graphics.drawString(font, "Популярность: " + t.popularity + "%", 30, y + 30, 0xCCCCCC);
            
            int tBarW = width - 60;
            graphics.fill(30, y + 50, 30 + tBarW, y + 60, 0xFF222222);
            graphics.fill(30, y + 50, 30 + (int)(tBarW * t.popularity / 100), y + 60, 0xFF00AAFF);
            
            y += 75;
        }
    }

    static class Tradition {
        String name;
        int popularity;
        boolean active;
        
        Tradition(String name, int popularity, boolean active) {
            this.name = name;
            this.popularity = popularity;
            this.active = active;
        }
    }
}
