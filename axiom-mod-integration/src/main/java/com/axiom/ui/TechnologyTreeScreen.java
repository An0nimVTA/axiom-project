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
        UiTheme.drawBackdrop(gfx, width, height);

        // Title
        gfx.drawCenteredString(font, title, width / 2, 45, UiTheme.TEXT_PRIMARY);
        gfx.drawCenteredString(font, Component.literal("Уровень " + selectedTier + " из 5"), width / 2, 60, UiTheme.TEXT_MUTED);

        // Render tech nodes
        List<TechNode> techs = techsByTier.getOrDefault(selectedTier, new ArrayList<>());
        LayoutInfo layout = layoutFor(techs.size());

        for (int i = 0; i < techs.size(); i++) {
            TechNode tech = techs.get(i);
            int col = i % layout.columns;
            int row = i / layout.columns;
            int x = layout.startX + col * (layout.cardWidth + layout.gap);
            int y = layout.startY + row * (layout.cardHeight + layout.gap);

            renderTechCard(gfx, tech, x, y, layout.cardWidth, layout.cardHeight, mouseX, mouseY);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderTechCard(GuiGraphics gfx, TechNode tech, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        boolean compact = h < 90 || w < 140;
        int accent = tech.unlocked ? tech.color :
            tech.available ? UiTheme.withAlpha(tech.color, 200) : UiTheme.CARD_BORDER_MINIMAL;
        UiTheme.drawMinimalCard(gfx, x, y, x + w, y + h, accent, hovered);

        int padding = 12;
        int dotColor = tech.unlocked ? UiTheme.ACCENT : tech.available ? UiTheme.withAlpha(UiTheme.ACCENT, 180) : UiTheme.TEXT_DIM;
        String dot = "●";
        gfx.drawString(font, dot, x + w - padding - font.width(dot), y + padding, dotColor, false);

        gfx.drawString(font, tech.name, x + padding, y + padding, UiTheme.TEXT_PRIMARY, false);

        if (!compact) {
            gfx.drawString(font, tech.description, x + padding, y + padding + 14, UiTheme.TEXT_MUTED, false);
        }

        if (!compact && !tech.prerequisites.isEmpty()) {
            String prereqText = "Требует: " + String.join(", ", tech.prerequisites);
            gfx.drawString(font, prereqText, x + padding, y + padding + 28, UiTheme.TEXT_DIM, false);
        }

        if (!compact && tech.available && !tech.unlocked) {
            String label = "Исследовать";
            int btnW = font.width(label) + 10;
            int btnH = 14;
            int btnX = x + w - btnW - padding;
            int btnY = y + h - btnH - padding;
            boolean hover = mouseX >= btnX && mouseX <= btnX + btnW &&
                mouseY >= btnY && mouseY <= btnY + btnH;
            int border = hover ? UiTheme.TEXT_PRIMARY : accent;
            UiTheme.drawChip(gfx, btnX, btnY, btnX + btnW, btnY + btnH, border, UiTheme.BUTTON_BG);
            gfx.drawString(font, label, btnX + 5, btnY + 3, UiTheme.TEXT_PRIMARY, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check tech card clicks
        List<TechNode> techs = techsByTier.getOrDefault(selectedTier, new ArrayList<>());
        LayoutInfo layout = layoutFor(techs.size());

        for (int i = 0; i < techs.size(); i++) {
            TechNode tech = techs.get(i);
            int col = i % layout.columns;
            int row = i / layout.columns;
            int x = layout.startX + col * (layout.cardWidth + layout.gap);
            int y = layout.startY + row * (layout.cardHeight + layout.gap);

            if (tech.available && !tech.unlocked) {
                int padding = 12;
                String label = "Исследовать";
                int btnW = font.width(label) + 10;
                int btnH = 14;
                int btnX = x + layout.cardWidth - btnW - padding;
                int btnY = y + layout.cardHeight - btnH - padding;
                if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
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

    private LayoutInfo layoutFor(int count) {
        float scale = UiLayout.scaleFor(width, height);
        int padding = UiLayout.scaled(24, scale);
        int availableWidth = Math.max(0, width - padding * 2);
        int cardWidth = UiCardSizing.techGridCardWidth(scale, availableWidth);
        int cardHeight = UiCardSizing.techGridCardHeight(scale);
        int gap = UiCardSizing.techGridGap(scale);
        int columns = Math.max(1, (availableWidth + gap) / (cardWidth + gap));
        int totalWidth = columns * cardWidth + (columns - 1) * gap;
        int startX = Math.max(padding, (width - totalWidth) / 2);
        int startY = UiCardSizing.techGridStartY(scale);
        return new LayoutInfo(cardWidth, cardHeight, gap, columns, startX, startY);
    }

    private static final class LayoutInfo {
        private final int cardWidth;
        private final int cardHeight;
        private final int gap;
        private final int columns;
        private final int startX;
        private final int startY;

        private LayoutInfo(int cardWidth, int cardHeight, int gap, int columns, int startX, int startY) {
            this.cardWidth = cardWidth;
            this.cardHeight = cardHeight;
            this.gap = gap;
            this.columns = columns;
            this.startX = startX;
            this.startY = startY;
        }
    }
}
