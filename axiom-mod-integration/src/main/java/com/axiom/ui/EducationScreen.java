package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class EducationScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;
    private static final int CARD_HEIGHT = 80;
    
    // Mock data
    private final double educationLevel = 67.5;
    private final List<Research> researches = Arrays.asList(
        new Research("Математика", 100, true),
        new Research("Физика", 85, false),
        new Research("Химия", 45, false),
        new Research("Биология", 0, false)
    );

    public EducationScreen(Screen parent) {
        super(Component.literal("Образование"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> minecraft.setScreen(parent))
            .bounds(10, 10, 80, 20).build());
        
        addRenderableWidget(Button.builder(Component.literal("Начать исследование"), b -> {
            NotificationManager.getInstance().success("Исследование начато!");
        }).bounds(width - 180, 10, 170, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        UiTheme.drawBackdrop(graphics, width, height);
        
        // Title
        graphics.drawCenteredString(font, "Система образования", width / 2, 20, UiTheme.TEXT_PRIMARY);
        
        // Education level
        int y = 50;
        UiTheme.drawMinimalCard(graphics, 20, y, width - 20, y + 60, 0xFF1E3A5F, false);
        graphics.drawString(font, "Уровень образования:", 30, y + 10, UiTheme.TEXT_PRIMARY, false);
        graphics.drawString(font, String.format("%.1f%%", educationLevel), 30, y + 25, UiTheme.ACCENT, false);
        
        // Progress bar
        int barWidth = width - 60;
        graphics.fill(30, y + 40, 30 + barWidth, y + 50, UiTheme.BUTTON_BG);
        graphics.fill(30, y + 40, 30 + (int)(barWidth * educationLevel / 100), y + 50, UiTheme.ACCENT);
        
        // Researches
        y = 120;
        graphics.drawString(font, "Исследования:", 30, y, UiTheme.TEXT_PRIMARY, false);
        y += 20;
        
        for (Research r : researches) {
            if (y + CARD_HEIGHT > height - 10) break;
            
            int accent = r.completed ? 0xFF00AA00 : (r.progress > 0 ? 0xFF1E6BC1 : 0xFF444C56);
            UiTheme.drawMinimalCard(graphics, 20, y, width - 20, y + CARD_HEIGHT, accent, false);
            
            String icon = r.completed ? "✓" : (r.progress > 0 ? "⟳" : "○");
            graphics.drawString(font, icon + " " + r.name, 30, y + 10, UiTheme.TEXT_PRIMARY, false);
            graphics.drawString(font, "Прогресс: " + r.progress + "%", 30, y + 30, UiTheme.TEXT_MUTED, false);
            
            if (r.progress > 0 && !r.completed) {
                int rBarWidth = width - 60;
                graphics.fill(30, y + 50, 30 + rBarWidth, y + 60, UiTheme.BUTTON_BG);
                graphics.fill(30, y + 50, 30 + (int)(rBarWidth * r.progress / 100), y + 60, 0xFF0088FF);
            }
            
            y += CARD_HEIGHT + 5;
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Math.max(0, scrollOffset - (int)(scrollY * 20));
        return true;
    }

    static class Research {
        String name;
        int progress;
        boolean completed;
        
        Research(String name, int progress, boolean completed) {
            this.name = name;
            this.progress = progress;
            this.completed = completed;
        }
    }
}
