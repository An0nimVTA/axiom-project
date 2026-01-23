package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class TechnologyTreeScreen extends Screen {
    private final Screen parent;
    private final Map<Integer, List<TechNode>> techsByTier = new HashMap<>();
    private int selectedTier = 1;
    private Button backButton;
    private Button[] tierButtons;

    public static class TechNode {
        public String id;
        public String name;
        public String description;
        public int tier;
        public List<String> prerequisites;
        public boolean unlocked;
        public boolean available;
        public int color;

        public TechNode(String id, String name, String desc, int tier, List<String> prereqs) {
            this.id = id;
            this.name = name;
            this.description = desc;
            this.tier = tier;
            this.prerequisites = prereqs;
            this.unlocked = false;
            this.available = prereqs.isEmpty();
            this.color = getTierColor(tier);
        }

        private int getTierColor(int tier) {
            return switch (tier) {
                case 1 -> 0xFF4CAF50; // Green
                case 2 -> 0xFF2196F3; // Blue
                case 3 -> 0xFF9C27B0; // Purple
                case 4 -> 0xFFFFC107; // Yellow
                case 5 -> 0xFFF44336; // Red
                default -> 0xFF888888;
            };
        }
    }

    public TechnologyTreeScreen(Screen parent) {
        super(Component.literal("Дерево технологий"));
        this.parent = parent;
        loadTechnologies();
    }

    private void loadTechnologies() {
        // Tier 1
        addTech(1, "basic_military", "Базовая военная тактика", "Основы военного дела", List.of());
        addTech(1, "basic_construction", "Базовое строительство", "Основы строительства", List.of());
        addTech(1, "basic_trade", "Базовая торговля", "Простая торговля", List.of());
        addTech(1, "basic_education", "Базовое образование", "Начальные школы", List.of());

        // Tier 2
        addTech(2, "fortifications", "Фортификации", "Стены и укрепления", List.of("basic_military"));
        addTech(2, "basic_industry", "Базовая промышленность", "Простые станки", List.of("basic_construction"));
        addTech(2, "banking", "Банковское дело", "Система займов", List.of("basic_trade"));
        addTech(2, "advanced_education", "Продвинутое образование", "Университеты", List.of("basic_education"));

        // Tier 3
        addTech(3, "firearms_tech", "Стрелковое оружие", "Винтовки и пистолеты", List.of("fortifications"));
        addTech(3, "industrial_engineering", "Промышленное производство", "Заводы", List.of("basic_industry"));
        addTech(3, "automation_tech", "Автоматизация", "ME-сети", List.of("banking"));
        addTech(3, "research_labs", "Исследовательские лаборатории", "Ускорение исследований", List.of("advanced_education"));

        // Tier 4
        addTech(4, "elite_equipment", "Элитное снаряжение", "Тактическое оборудование", List.of("firearms_tech"));
        addTech(4, "advanced_industry", "Продвинутая индустрия", "Модернизация машин", List.of("industrial_engineering"));
        addTech(4, "quantum_energy", "Квантовая энергетика", "Сверхмощные генераторы", List.of("automation_tech"));
        addTech(4, "space_program", "Космическая программа", "Исследования космоса", List.of("research_labs"));

        // Tier 5
        addTech(5, "nuclear_weapons", "Ядерное оружие", "Атомное сдерживание", List.of("space_program", "quantum_energy"));
        addTech(5, "total_warfare", "Тотальная война", "Максимальная военная мощь", List.of("elite_equipment"));
        addTech(5, "mega_production", "Мегапроизводство", "Промышленная сверхдержава", List.of("advanced_industry"));
    }

    private void addTech(int tier, String id, String name, String desc, List<String> prereqs) {
        techsByTier.computeIfAbsent(tier, k -> new ArrayList<>()).add(
            new TechNode(id, name, desc, tier, prereqs)
        );
    }

    @Override
    protected void init() {
        super.init();

        // Tier buttons
        tierButtons = new Button[5];
        for (int i = 0; i < 5; i++) {
            final int tier = i + 1;
            tierButtons[i] = Button.builder(
                Component.literal("Уровень " + tier),
                (btn) -> selectTier(tier)
            )
            .bounds(10 + i * 105, 10, 100, 20)
            .build();
            addRenderableWidget(tierButtons[i]);
        }

        // Back button
        backButton = Button.builder(Component.literal("◀ Назад"), (btn) -> {
            Minecraft.getInstance().setScreen(parent);
        })
        .bounds(width - 110, 10, 100, 20)
        .build();
        addRenderableWidget(backButton);
    }

    private void selectTier(int tier) {
        selectedTier = tier;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);

        // Title
        gfx.drawCenteredString(font, title, width / 2, 45, 0xFFFFFFFF);
        gfx.drawCenteredString(font, Component.literal("§7Уровень " + selectedTier + " из 5"), width / 2, 60, 0xFFAAAAAA);

        // Render tech nodes
        List<TechNode> techs = techsByTier.getOrDefault(selectedTier, new ArrayList<>());
        int startY = 100;
        int cardWidth = 350;
        int cardHeight = 80;
        int spacing = 10;

        for (int i = 0; i < techs.size(); i++) {
            TechNode tech = techs.get(i);
            int x = (width - cardWidth) / 2;
            int y = startY + i * (cardHeight + spacing);

            renderTechCard(gfx, tech, x, y, cardWidth, cardHeight, mouseX, mouseY);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderTechCard(GuiGraphics gfx, TechNode tech, int x, int y, int w, int h, int mouseX, int mouseY) {
        // Background
        int bgColor = tech.unlocked ? (tech.color & 0x00FFFFFF) | 0x80000000 :
                      tech.available ? (tech.color & 0x00FFFFFF) | 0x40000000 :
                      0x40333333;
        gfx.fill(x, y, x + w, y + h, bgColor);

        // Border
        int borderColor = tech.unlocked ? tech.color :
                         tech.available ? (tech.color & 0x00FFFFFF) | 0x80000000 :
                         0xFF666666;
        gfx.renderOutline(x, y, w, h, borderColor);

        // Status icon
        String icon = tech.unlocked ? "✓" : tech.available ? "○" : "✗";
        int iconColor = tech.unlocked ? 0xFF00FF00 : tech.available ? 0xFFFFFF00 : 0xFFFF0000;
        gfx.drawString(font, icon, x + 10, y + 10, iconColor, false);

        // Name
        gfx.drawString(font, Component.literal("§l" + tech.name), x + 30, y + 10, 0xFFFFFFFF, false);

        // Description
        gfx.drawString(font, tech.description, x + 30, y + 30, 0xFFCCCCCC, false);

        // Prerequisites
        if (!tech.prerequisites.isEmpty()) {
            String prereqText = "§7Требует: " + String.join(", ", tech.prerequisites);
            gfx.drawString(font, prereqText, x + 30, y + 50, 0xFF888888, false);
        }

        // Research button (if available)
        if (tech.available && !tech.unlocked) {
            boolean hover = mouseX >= x + w - 110 && mouseX <= x + w - 10 &&
                           mouseY >= y + h - 30 && mouseY <= y + h - 10;
            int btnColor = hover ? 0xFF00AA00 : 0xFF008800;
            gfx.fill(x + w - 110, y + h - 30, x + w - 10, y + h - 10, btnColor);
            gfx.drawCenteredString(font, "Исследовать", x + w - 60, y + h - 23, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check tech card clicks
        List<TechNode> techs = techsByTier.getOrDefault(selectedTier, new ArrayList<>());
        int startY = 100;
        int cardWidth = 350;
        int cardHeight = 80;
        int spacing = 10;

        for (int i = 0; i < techs.size(); i++) {
            TechNode tech = techs.get(i);
            int x = (width - cardWidth) / 2;
            int y = startY + i * (cardHeight + spacing);

            if (tech.available && !tech.unlocked) {
                int btnX = x + cardWidth - 110;
                int btnY = y + cardHeight - 30;
                if (mouseX >= btnX && mouseX <= btnX + 100 && mouseY >= btnY && mouseY <= btnY + 20) {
                    researchTech(tech);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void researchTech(TechNode tech) {
        // Send command to server
        String cmd = "tech research " + tech.id;
        Minecraft.getInstance().player.connection.sendCommand(cmd);
        NotificationManager.getInstance().info("Исследование: " + tech.name);
        tech.unlocked = true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
