package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class MainMenuScreen extends Screen {
    private int cardWidth = 220;
    private int cardHeight = 90;
    private int cardGap = 12;
    private int detailHeight = 18;

    private final List<MenuCard> cards = new ArrayList<>();
    private final List<CardEntry> entries = new ArrayList<>();
    private int scrollOffset;
    private int maxScroll;

    public MainMenuScreen() {
        super(UiText.text("AXIOM - Главное меню", "AXIOM - Main Menu"));
    }

    public static void open() {
        if (!UiText.hasServerPreference()) {
            Minecraft.getInstance().setScreen(new LanguageSelectScreen(null));
            return;
        }
        Minecraft.getInstance().setScreen(new MainMenuScreen());
    }

    @Override
    protected void init() {
        buildCards();
        rebuildLayout();

        Button langButton = Button.builder(
                Component.literal(UiText.getLanguage().getCode().toUpperCase()),
                btn -> Minecraft.getInstance().setScreen(new LanguageSelectScreen(this))
            )
            .bounds(width - 60, 10, 50, 20)
            .build();
        addRenderableWidget(langButton);
    }

    private void buildCards() {
        cards.clear();

        addCategoryCard(CommandCategory.NATION, UiRarity.LEGENDARY);
        addCategoryCard(CommandCategory.ECONOMY, UiRarity.RARE);
        addCategoryCard(CommandCategory.DIPLOMACY, UiRarity.RARE);
        addCategoryCard(CommandCategory.MILITARY, UiRarity.LEGENDARY);
        addCategoryCard(CommandCategory.CITY, UiRarity.COMMON);
        addCategoryCard(CommandCategory.RELIGION, UiRarity.COMMON);

        cards.add(new MenuCard(
            UiText.pick("Технологии", "Technology"),
            UiText.pick("Дерево исследований и прогресс", "Research tree and progress"),
            "minecraft:redstone",
            UiRarity.RARE,
            0xFF9C27B0,
            () -> Minecraft.getInstance().setScreen(new TechnologyTreeScreen(this))
        ));

        cards.add(new MenuCard(
            UiText.pick("Карта территорий", "Territory Map"),
            UiText.pick("Просмотр владений и границ", "View nations and borders"),
            "minecraft:map",
            UiRarity.COMMON,
            0xFF2196F3,
            () -> Minecraft.getInstance().setScreen(new NationMapScreen(this))
        ));

        cards.add(new MenuCard(
            UiText.pick("Баланс предметов", "Item Balance"),
            UiText.pick("Настройка стоимости и параметров", "Tune costs and parameters"),
            "minecraft:anvil",
            UiRarity.RARE,
            0xFFFFC107,
            () -> Minecraft.getInstance().setScreen(new ItemBalanceScreen(this))
        ));

        cards.add(new MenuCard(
            UiText.pick("Крафты", "Recipes"),
            UiText.pick("Редактор и проверка рецептов", "Recipe editor and validation"),
            "minecraft:crafting_table",
            UiRarity.COMMON,
            0xFF8D6E63,
            () -> Minecraft.getInstance().setScreen(new RecipeEditorScreen(this))
        ));

        cards.add(new MenuCard(
            UiText.pick("Аналитика", "Analytics"),
            UiText.pick("Отчёты и метрики", "Reports and metrics"),
            "minecraft:paper",
            UiRarity.COMMON,
            0xFF607D8B,
            () -> Minecraft.getInstance().setScreen(new AnalyticsScreen(this))
        ));
    }

    private void addCategoryCard(CommandCategory category, UiRarity rarity) {
        cards.add(new MenuCard(
            category.getDisplayName(),
            category.getDescription(),
            category.getIconItemId(),
            rarity,
            category.getColor(),
            () -> Minecraft.getInstance().setScreen(new CommandMenuScreen(category))
        ));
    }

    private void rebuildLayout() {
        entries.clear();
        scrollOffset = 0;

        float scale = UiLayout.scaleFor(width, height);
        int padding = UiLayout.scaled(24, scale);
        int availableWidth = Math.max(0, width - padding * 2);
        cardWidth = UiCardSizing.mainMenuCardWidth(scale, availableWidth);
        cardHeight = UiCardSizing.mainMenuCardHeight(scale);
        cardGap = UiCardSizing.mainMenuGap(scale);
        detailHeight = UiLayout.scaled(22, scale);

        int columns = Math.max(1, (availableWidth + cardGap) / (cardWidth + cardGap));
        int rows = (int) Math.ceil(cards.size() / (double) columns);

        int totalWidth = columns * cardWidth + (columns - 1) * cardGap;
        int startX = Math.max(padding, (width - totalWidth) / 2);
        int startY = UiLayout.scaled(58, scale);

        for (int i = 0; i < cards.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (cardWidth + cardGap);
            int y = startY + row * (cardHeight + cardGap);
            entries.add(new CardEntry(cards.get(i), x, y));
        }

        int totalHeight = rows * cardHeight + Math.max(0, rows - 1) * cardGap;
        int viewHeight = height - startY - 24;
        maxScroll = Math.max(0, totalHeight - viewHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0) {
            return false;
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (delta * 12), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CardEntry entry : entries) {
            int drawY = entry.y - scrollOffset;
            if (entry.contains(mouseX, mouseY, drawY)) {
                entry.card.action.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UiTheme.drawBackdrop(gfx, width, height);
        gfx.fill(0, 0, width, 38, UiTheme.PANEL);
        gfx.fill(0, 36, width, 38, UiTheme.PANEL_BORDER);
        gfx.drawCenteredString(font, title, width / 2, 10, UiTheme.TEXT_PRIMARY);
        gfx.drawCenteredString(font,
            Component.literal(UiText.pick("Выберите раздел", "Choose a section")),
            width / 2, 24, UiTheme.TEXT_MUTED);
        String hotkey = UiText.pick("Горячая клавиша: I", "Hotkey: I");
        gfx.drawString(font, hotkey, width - 12 - font.width(hotkey), 10, UiTheme.TEXT_DIM, false);

        for (CardEntry entry : entries) {
            int drawY = entry.y - scrollOffset;
            if (drawY + cardHeight < 0 || drawY > height) {
                continue;
            }
            boolean hovered = entry.contains(mouseX, mouseY, drawY);
            entry.card.render(gfx, entry.x, drawY, hovered);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean openCard(int index) {
        if (index < 0 || index >= cards.size()) {
            return false;
        }
        cards.get(index).action.run();
        return true;
    }

    private final class MenuCard {
        private final String title;
        private final String description;
        private final String iconItemId;
        private final UiRarity rarity;
        private final int accentColor;
        private final Runnable action;

        private MenuCard(String title, String description, String iconItemId, UiRarity rarity, int accentColor, Runnable action) {
            this.title = title;
            this.description = description;
            this.iconItemId = iconItemId;
            this.rarity = rarity == null ? UiRarity.COMMON : rarity;
            this.accentColor = accentColor;
            this.action = action;
        }

        private void render(GuiGraphics gfx, int x, int y, boolean hovered) {
            if (hovered) {
                gfx.fill(x - 2, y - 2, x + cardWidth + 2, y + cardHeight + 2, 0x16000000);
            }

            UiTheme.drawMinimalCard(gfx, x, y, x + cardWidth, y + cardHeight, accentColor, hovered);

            boolean compact = cardHeight < 140 || cardWidth < 140;
            float scale = Mth.clamp(cardHeight / 260.0f, 0.35f, 1.15f);
            int padding = Math.max(6, Math.round(14 * scale));
            int iconSize = Math.max(12, Math.round(Math.min(cardWidth, cardHeight) * (compact ? 0.42f : 0.16f)));
            int iconX = x + padding;
            int iconY = y + padding;
            gfx.renderItem(UiIcons.resolveItem(iconItemId, net.minecraft.world.item.Items.PAPER), iconX, iconY);

            if (compact) {
                int titleX = iconX + iconSize + 6;
                int titleY = iconY + Math.max(0, (iconSize - font.lineHeight) / 2);
                String titleLine = Minecraft.getInstance().font.plainSubstrByWidth(title, cardWidth - (titleX - x) - padding);
                gfx.drawString(Minecraft.getInstance().font, titleLine, titleX, titleY, UiTheme.TEXT_PRIMARY, false);
                return;
            }

            int textX = x + padding;
            int titleY = iconY + iconSize + 10;
            String titleLine = Minecraft.getInstance().font.plainSubstrByWidth(title, cardWidth - padding * 2);
            gfx.drawString(Minecraft.getInstance().font, titleLine, textX, titleY, UiTheme.TEXT_PRIMARY, false);

            int descY = titleY + 12;
            List<FormattedCharSequence> lines = Minecraft.getInstance().font
                .split(Component.literal(description), Math.max(10, cardWidth - padding * 2));
            int maxLines = Math.min(2, lines.size());
            for (int i = 0; i < maxLines; i++) {
                gfx.drawString(Minecraft.getInstance().font, lines.get(i), textX, descY, UiTheme.TEXT_MUTED, false);
                descY += 10;
            }
        }
    }

    private final class CardEntry {
        private final MenuCard card;
        private final int x;
        private final int y;

        private CardEntry(MenuCard card, int x, int y) {
            this.card = card;
            this.x = x;
            this.y = y;
        }

        private boolean contains(double mouseX, double mouseY, int drawY) {
            return mouseX >= x && mouseX <= x + cardWidth && mouseY >= drawY && mouseY <= drawY + cardHeight;
        }
    }
}
