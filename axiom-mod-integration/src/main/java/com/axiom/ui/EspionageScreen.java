package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public class EspionageScreen extends Screen {
    private final Screen parent;
    private final List<Spy> spies = Arrays.asList(
        new Spy("Агент 007", "Разведка", 85, true),
        new Spy("Тень", "Саботаж", 70, true),
        new Spy("Призрак", "Контрразведка", 60, false)
    );
    private final List<Mission> missions = Arrays.asList(
        new Mission("Украсть чертежи", 45, false),
        new Mission("Саботировать завод", 80, false),
        new Mission("Разведка границ", 100, true)
    );

    public EspionageScreen(Screen parent) {
        super(Component.literal("Шпионаж"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> minecraft.setScreen(parent))
            .bounds(10, 10, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Нанять шпиона"), b -> 
            NotificationManager.getInstance().success("Шпион нанят!"))
            .bounds(width - 180, 10, 170, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, "🕵️ Система шпионажа", width / 2, 20, 0xFFFFFF);
        
        int y = 50;
        graphics.drawString(font, "Шпионы:", 30, y, 0xFFFFFF);
        y += 20;
        
        for (Spy s : spies) {
            int color = s.active ? 0xAA1E1E5F : 0xAA333333;
            graphics.fill(20, y, width / 2 - 10, y + 60, color);
            String icon = s.active ? "✓" : "○";
            graphics.drawString(font, icon + " " + s.name, 30, y + 10, 0xFFFFFF);
            graphics.drawString(font, s.role + " | Опыт: " + s.experience + "%", 30, y + 30, 0xCCCCCC);
            y += 65;
        }
        
        y = 70;
        graphics.drawString(font, "Миссии:", width / 2 + 10, y, 0xFFFFFF);
        y += 20;
        
        for (Mission m : missions) {
            int color = m.completed ? 0xAA00AA00 : (m.progress > 0 ? 0xAA5F3A1E : 0xAA444444);
            graphics.fill(width / 2 + 10, y, width - 20, y + 60, color);
            String icon = m.completed ? "✓" : (m.progress > 0 ? "⟳" : "○");
            graphics.drawString(font, icon + " " + m.name, width / 2 + 20, y + 10, 0xFFFFFF);
            graphics.drawString(font, "Прогресс: " + m.progress + "%", width / 2 + 20, y + 30, 0xCCCCCC);
            y += 65;
        }
    }

    static class Spy {
        String name, role;
        int experience;
        boolean active;
        Spy(String name, String role, int experience, boolean active) {
            this.name = name; this.role = role; this.experience = experience; this.active = active;
        }
    }

    static class Mission {
        String name;
        int progress;
        boolean completed;
        Mission(String name, int progress, boolean completed) {
            this.name = name; this.progress = progress; this.completed = completed;
        }
    }
}
