package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandMenuScreen extends Screen {
    private int padding = 16;
    private int searchY = 34;
    private int filterHeight = 24;
    private int filterGap = 8;
    private int categoryWidth = 140;
    private int categoryHeight = 52;
    private int categoryGap = 10;
    private int cardSpacing = 12;
    private int footerHeight = 22;
    private float layoutScale = 1.0f;

    private final List<CardEntry> cardEntries = new ArrayList<>();
    private final List<FilterEntry> filterEntries = new ArrayList<>();
    private final List<FilterEntry> sortEntries = new ArrayList<>();
    private final List<CategoryEntry> categoryEntries = new ArrayList<>();

    private final CommandCategory initialCategory;
    private CommandCategory selectedCategory;
    private boolean showFavorites;
    private boolean showHistory;
    private SortMode sortMode = SortMode.ALPHA;

    private int scrollOffset;
    private int maxScroll;
    private int commandsTop;
    private int commandsBottom;
    private int headerBottom;

    private Button backButton;
    private CommandSearchWidget searchWidget;
    private final CommandHistory commandHistory = new CommandHistory();

    private enum SortMode {
        ALPHA,
        RARITY,
        POPULAR,
        RECENT
    }

    public CommandMenuScreen() {
        this(null);
    }

    public CommandMenuScreen(CommandCategory initialCategory) {
        super(UiText.text("AXIOM - Команды плагина", "AXIOM - Plugin Commands"));
        this.initialCategory = initialCategory;
    }

    @Override
    protected void init() {
        super.init();

        layoutScale = UiLayout.scaleFor(width, height);
        padding = UiLayout.scaled(18, layoutScale);
        searchY = UiLayout.scaled(44, layoutScale);
        filterHeight = UiLayout.scaled(24, layoutScale);
        filterGap = UiLayout.scaled(8, layoutScale);
        categoryWidth = UiLayout.scaled(150, layoutScale);
        categoryHeight = UiLayout.scaled(54, layoutScale);
        categoryGap = UiLayout.scaled(10, layoutScale);
        cardSpacing = UiCardSizing.commandMenuGap(layoutScale);
        footerHeight = UiLayout.scaled(24, layoutScale);

        int searchWidth = Math.max(UiLayout.scaled(220, layoutScale),
            Math.min(UiLayout.scaled(440, layoutScale), width - padding * 2 - UiLayout.scaled(140, layoutScale)));
        searchWidget = new CommandSearchWidget(padding, searchY, searchWidth, CommandCatalog.getAllCommands());

        backButton = Button.builder(UiText.text("Назад", "Back"), (btn) -> onClose())
            .bounds(width - UiLayout.scaled(110, layoutScale), UiLayout.scaled(10, layoutScale),
                UiLayout.scaled(100, layoutScale), UiLayout.scaled(20, layoutScale))
            .build();
        addRenderableWidget(backButton);

        selectedCategory = initialCategory;
        showFavorites = false;
        showHistory = false;

        rebuildLayout();
    }

    private void rebuildLayout() {
        int filtersTop = searchY + UiLayout.scaled(26, layoutScale);
        int filtersBottom = layoutFilters(filtersTop);
        int categoriesTop = filtersBottom + 8;
        int categoriesBottom = layoutCategories(categoriesTop);

        headerBottom = categoriesBottom + 4;
        commandsTop = headerBottom + 10;
        commandsBottom = height - footerHeight;

        reloadCommands();
    }

    private int layoutFilters(int startY) {
        filterEntries.clear();
        sortEntries.clear();

        int x = padding;
        int y = startY;
        int maxX = width - padding;

        x = addFilterEntry(UiText.pick("Все", "All"), this::selectAll, x, y, maxX);
        if (x < padding) {
            y += filterHeight + filterGap;
            x = padding;
        }
        x = addFilterEntry(UiText.pick("Избранное", "Favorites"), this::toggleFavorites, x, y, maxX);
        if (x < padding) {
            y += filterHeight + filterGap;
            x = padding;
        }
        addFilterEntry(UiText.pick("История", "History"), this::toggleHistory, x, y, maxX);

        int sortY = y + filterHeight + filterGap;
        layoutSorts(sortY);

        return sortY + filterHeight;
    }

    private void layoutSorts(int y) {
        int x = padding;
        int maxX = width - padding;
        x = addSortEntry(UiText.pick("A-Я", "A-Z"), SortMode.ALPHA, x, y, maxX);
        if (x < padding) {
            y += filterHeight + filterGap;
            x = padding;
        }
        x = addSortEntry(UiText.pick("Редкость", "Rarity"), SortMode.RARITY, x, y, maxX);
        if (x < padding) {
            y += filterHeight + filterGap;
            x = padding;
        }
        x = addSortEntry(UiText.pick("Популярные", "Popular"), SortMode.POPULAR, x, y, maxX);
        if (x < padding) {
            y += filterHeight + filterGap;
            x = padding;
        }
        addSortEntry(UiText.pick("Недавние", "Recent"), SortMode.RECENT, x, y, maxX);
    }

    private int addSortEntry(String label, SortMode mode, int x, int y, int maxX) {
        int widthNeeded = Math.max(UiLayout.scaled(48, layoutScale), font.width(label) + UiLayout.scaled(16, layoutScale));
        if (x + widthNeeded > maxX) {
            return -1;
        }
        sortEntries.add(new FilterEntry(label, () -> setSortMode(mode), x, y, widthNeeded, filterHeight));
        return x + widthNeeded + filterGap;
    }

    private int addFilterEntry(String label, Runnable action, int x, int y, int maxX) {
        int widthNeeded = Math.max(UiLayout.scaled(40, layoutScale), font.width(label) + UiLayout.scaled(16, layoutScale));
        if (x + widthNeeded > maxX) {
            return -1;
        }
        filterEntries.add(new FilterEntry(label, action, x, y, widthNeeded, filterHeight));
        return x + widthNeeded + filterGap;
    }

    private int layoutCategories(int startY) {
        categoryEntries.clear();

        CommandCategory[] categories = CommandCategory.values();
        int availableWidth = Math.max(0, width - padding * 2);
        int columns = Math.max(1, Math.min(4, (availableWidth + categoryGap) / (categoryWidth + categoryGap)));
        int rows = (int) Math.ceil(categories.length / (double) columns);

        int totalWidth = columns * categoryWidth + (columns - 1) * categoryGap;
        int startX = Math.max(padding, (width - totalWidth) / 2);

        for (int i = 0; i < categories.length; i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (categoryWidth + categoryGap);
            int y = startY + row * (categoryHeight + categoryGap);
            categoryEntries.add(new CategoryEntry(categories[i], x, y, categoryWidth, categoryHeight));
        }

        int totalHeight = rows * categoryHeight + Math.max(0, rows - 1) * categoryGap;
        return startY + totalHeight;
    }

    private void selectAll() {
        selectedCategory = null;
        showFavorites = false;
        showHistory = false;
        scrollOffset = 0;
        reloadCommands();
    }

    private void toggleFavorites() {
        showFavorites = !showFavorites;
        showHistory = false;
        if (showFavorites) {
            selectedCategory = null;
        }
        scrollOffset = 0;
        reloadCommands();
    }

    private void toggleHistory() {
        showHistory = !showHistory;
        showFavorites = false;
        if (showHistory) {
            selectedCategory = null;
        }
        scrollOffset = 0;
        reloadCommands();
    }

    private void selectCategory(CommandCategory category) {
        selectedCategory = category;
        showFavorites = false;
        showHistory = false;
        scrollOffset = 0;
        reloadCommands();
    }

    private void setSortMode(SortMode mode) {
        sortMode = mode == null ? SortMode.ALPHA : mode;
        scrollOffset = 0;
        reloadCommands();
    }

    private void reloadCommands() {
        if (showFavorites) {
            loadFavoriteCommands();
        } else if (showHistory) {
            loadHistoryCommands();
        } else {
            loadCommands(selectedCategory);
        }
    }

    private void loadFavoriteCommands() {
        List<String> favoriteIds = commandHistory.getFavorites();
        List<CommandInfo> allCommands = CommandCatalog.getAllCommands();
        List<CommandInfo> commands = new ArrayList<>();

        for (String favId : favoriteIds) {
            for (CommandInfo cmd : allCommands) {
                if (cmd.getCommand().equals(favId)) {
                    commands.add(cmd);
                    break;
                }
            }
        }

        buildCardEntries(applySort(applySearchFilter(commands)));
    }

    private void loadHistoryCommands() {
        List<CommandHistory.HistoryEntry> historyEntries = commandHistory.getHistory();
        List<CommandInfo> allCommands = CommandCatalog.getAllCommands();
        List<CommandInfo> commands = new ArrayList<>();

        for (CommandHistory.HistoryEntry entry : historyEntries) {
            for (CommandInfo cmd : allCommands) {
                if (cmd.getCommand().equals(entry.command)) {
                    commands.add(cmd);
                    break;
                }
            }
        }

        buildCardEntries(applySort(applySearchFilter(commands)));
    }

    private void loadCommands(CommandCategory category) {
        List<CommandInfo> commands = searchWidget.getFilteredCommands();
        if (category != null) {
            commands = commands.stream()
                .filter(cmd -> cmd.getCategory() == category)
                .toList();
        }
        buildCardEntries(applySort(commands));
    }

    private List<CommandInfo> applySort(List<CommandInfo> commands) {
        List<CommandInfo> sorted = new ArrayList<>(commands);
        switch (sortMode) {
            case RARITY -> sorted.sort((a, b) -> Integer.compare(b.getRarity().ordinal(), a.getRarity().ordinal()));
            case POPULAR -> sorted.sort((a, b) -> {
                int ua = commandHistory.getUseCount(a.getCommand());
                int ub = commandHistory.getUseCount(b.getCommand());
                if (ua != ub) return Integer.compare(ub, ua);
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            });
            case RECENT -> sorted.sort((a, b) -> {
                int ia = indexInHistory(a.getCommand());
                int ib = indexInHistory(b.getCommand());
                if (ia != ib) return Integer.compare(ia, ib);
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            });
            case ALPHA -> sorted.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
            default -> {
            }
        }
        return sorted;
    }

    private int indexInHistory(String commandId) {
        List<CommandHistory.HistoryEntry> history = commandHistory.getHistory();
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).command.equals(commandId)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private List<CommandInfo> applySearchFilter(List<CommandInfo> commands) {
        String query = searchWidget.getSearchField().getValue();
        if (query == null || query.isBlank()) {
            return commands;
        }
        Set<CommandInfo> allowed = new HashSet<>(searchWidget.getFilteredCommands());
        return commands.stream().filter(allowed::contains).toList();
    }

    private void buildCardEntries(List<CommandInfo> commands) {
        cardEntries.clear();

        int availableWidth = Math.max(0, width - padding * 2);
        int cardWidth = UiCardSizing.commandMenuCardWidth(layoutScale, availableWidth);
        int cardHeight = UiCardSizing.commandMenuCardHeight(layoutScale);
        int columns = Math.max(1, (availableWidth + cardSpacing) / (cardWidth + cardSpacing));
        int totalWidth = columns * cardWidth + (columns - 1) * cardSpacing;
        int startX = Math.max(padding, (width - totalWidth) / 2);

        for (int i = 0; i < commands.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (cardWidth + cardSpacing);
            int y = commandsTop + row * (cardHeight + cardSpacing);

            CommandInfo cmd = commands.get(i);
            CommandCardWidget card = new CommandCardWidget(
                x, y, cardWidth, cardHeight, cmd,
                () -> executeCommand(cmd),
                () -> executeCommand(cmd),
                () -> toggleFavorite(cmd)
            );
            card.setFavorite(commandHistory.isFavorite(cmd.getCommand()));
            cardEntries.add(new CardEntry(card, x, y));
        }

        int rows = (int) Math.ceil(commands.size() / (double) columns);
        int totalHeight = rows * cardHeight + Math.max(0, rows - 1) * cardSpacing;
        int viewHeight = Math.max(0, commandsBottom - commandsTop);
        maxScroll = Math.max(0, totalHeight - viewHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        applyScroll();
    }

    private void applyScroll() {
        for (CardEntry entry : cardEntries) {
            entry.card.setPosition(entry.baseX, entry.baseY - scrollOffset);
        }
    }

    private void toggleFavorite(CommandInfo cmd) {
        if (commandHistory.isFavorite(cmd.getCommand())) {
            commandHistory.removeFavorite(cmd.getCommand());
            NotificationManager.getInstance().info("Удалено из избранного: " + cmd.getDisplayName());
        } else {
            commandHistory.addFavorite(cmd.getCommand());
            NotificationManager.getInstance().success("Добавлено в избранное: " + cmd.getDisplayName());
        }
        if (showFavorites) {
            loadFavoriteCommands();
        }
    }

    private void executeCommand(CommandInfo cmd) {
        commandHistory.addToHistory(cmd.getCommand());
        NotificationManager.getInstance().info("Выполнение: " + cmd.getDisplayName());
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendCommand(cmd.getCommand());
        }
        Minecraft.getInstance().setScreen(null);
    }

    private void showDetails(CommandInfo cmd) {
        executeCommand(cmd);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UiTheme.drawBackdrop(gfx, width, height);

        gfx.fill(0, 0, width, 38, UiTheme.PANEL);
        gfx.fill(0, 36, width, 38, UiTheme.PANEL_BORDER);
        gfx.drawCenteredString(font, title, width / 2, 10, UiTheme.TEXT_PRIMARY);

        String subtitle = UiText.pick("Все команды плагина", "All plugin commands");
        if (showFavorites) {
            subtitle = UiText.pick("Избранные команды", "Favorite commands");
        } else if (showHistory) {
            subtitle = UiText.pick("История команд", "Command history");
        } else if (selectedCategory != null) {
            subtitle = selectedCategory.getDescription();
        }
        gfx.drawCenteredString(font, Component.literal("§7" + subtitle), width / 2, 24, UiTheme.TEXT_MUTED);

        int panelLeft = Math.max(0, padding - 10);
        int panelRight = Math.min(width, width - padding + 10);
        int panelTop = searchY - 10;
        int panelBottom = Math.min(height - footerHeight, headerBottom);
        UiTheme.drawPanel(gfx, panelLeft, panelTop, panelRight, panelBottom);

        searchWidget.render(gfx, mouseX, mouseY, partialTick);

        renderFilters(gfx, mouseX, mouseY);
        renderSorts(gfx, mouseX, mouseY);
        renderCategories(gfx, mouseX, mouseY);

        int top = commandsTop;
        int bottom = commandsBottom;
        CommandCardWidget hoveredCard = null;
        gfx.enableScissor(0, top, width, bottom);
        for (CardEntry entry : cardEntries) {
            int drawY = entry.baseY - scrollOffset;
            if (drawY + entry.card.getHeight() < top || drawY > bottom) {
                continue;
            }
            entry.card.render(gfx, mouseX, mouseY, partialTick);
            if (hoveredCard == null && entry.card.isHovered(mouseX, mouseY)) {
                hoveredCard = entry.card;
            }
        }
        gfx.disableScissor();

        if (cardEntries.isEmpty()) {
            gfx.drawCenteredString(font, Component.literal(UiText.pick("Нет команд", "No commands")), width / 2, commandsTop + 12, 0xFFCCCCCC);
        }

        String statsTemplate = UiText.pick("Команд: %d | Избранных: %d | История: %d",
            "Commands: %d | Favorites: %d | History: %d");
        String stats = String.format(statsTemplate,
            CommandCatalog.getAllCommands().size(),
            commandHistory.getFavorites().size(),
            commandHistory.getHistory().size());
        gfx.drawString(font, stats, padding, height - 14, UiTheme.TEXT_DIM, false);

        String hint = UiText.pick("Клик по карточке — выполнить", "Click card to run");
        gfx.drawString(font, hint, width - padding - font.width(hint), height - 14, 0xFF5F6A76, false);

        if (hoveredCard != null) {
            CommandInfo cmd = hoveredCard.getCommand();
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(cmd.getDisplayName()));
            tooltip.add(Component.literal("§7/" + cmd.getCommand()));
            if (cmd.getShortDesc() != null && !cmd.getShortDesc().isBlank()) {
                tooltip.add(Component.literal("§8" + cmd.getShortDesc()));
            }
            gfx.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderFilters(GuiGraphics gfx, int mouseX, int mouseY) {
        for (FilterEntry entry : filterEntries) {
            boolean hovered = entry.contains(mouseX, mouseY);
            boolean active = isFilterActive(entry.label);
            int base = active ? UiTheme.BUTTON_BG_ACTIVE : UiTheme.BUTTON_BG;
            int border = hovered ? 0xFFFFFFFF : (active ? UiTheme.ACCENT : UiTheme.BUTTON_BORDER);
            gfx.fill(entry.x, entry.y, entry.x + entry.width, entry.y + entry.height, base);
            gfx.renderOutline(entry.x, entry.y, entry.width, entry.height, border);
            if (active) {
                gfx.fill(entry.x + 2, entry.y + 2, entry.x + entry.width - 2, entry.y + 4, UiTheme.ACCENT);
            }
            int textX = entry.x + (entry.width - font.width(entry.label)) / 2;
            gfx.drawString(font, entry.label, textX, entry.y + 7, active ? UiTheme.TEXT_PRIMARY : 0xFFBEC7D1, false);
        }
    }

    private void renderSorts(GuiGraphics gfx, int mouseX, int mouseY) {
        for (FilterEntry entry : sortEntries) {
            boolean hovered = entry.contains(mouseX, mouseY);
            boolean active = isSortActive(entry.label);
            int base = active ? UiTheme.BUTTON_BG_ACTIVE : UiTheme.BUTTON_BG;
            int border = hovered ? 0xFFFFFFFF : (active ? UiTheme.ACCENT : UiTheme.BUTTON_BORDER);
            gfx.fill(entry.x, entry.y, entry.x + entry.width, entry.y + entry.height, base);
            gfx.renderOutline(entry.x, entry.y, entry.width, entry.height, border);
            if (active) {
                gfx.fill(entry.x + 2, entry.y + 2, entry.x + entry.width - 2, entry.y + 4, UiTheme.ACCENT);
            }
            int textX = entry.x + (entry.width - font.width(entry.label)) / 2;
            gfx.drawString(font, entry.label, textX, entry.y + 7, active ? UiTheme.TEXT_PRIMARY : 0xFFBEC7D1, false);
        }
    }

    private boolean isSortActive(String label) {
        String alpha = UiText.pick("A-Я", "A-Z");
        String rarity = UiText.pick("Редкость", "Rarity");
        String popular = UiText.pick("Популярные", "Popular");
        String recent = UiText.pick("Недавние", "Recent");
        if (label.equals(alpha)) {
            return sortMode == SortMode.ALPHA;
        }
        if (label.equals(rarity)) {
            return sortMode == SortMode.RARITY;
        }
        if (label.equals(popular)) {
            return sortMode == SortMode.POPULAR;
        }
        if (label.equals(recent)) {
            return sortMode == SortMode.RECENT;
        }
        return false;
    }

    private boolean isFilterActive(String label) {
        String all = UiText.pick("Все", "All");
        String fav = UiText.pick("Избранное", "Favorites");
        String hist = UiText.pick("История", "History");
        if (label.equals(all)) {
            return !showFavorites && !showHistory && selectedCategory == null;
        }
        if (label.equals(fav)) {
            return showFavorites;
        }
        if (label.equals(hist)) {
            return showHistory;
        }
        return false;
    }

    private void renderCategories(GuiGraphics gfx, int mouseX, int mouseY) {
        for (CategoryEntry entry : categoryEntries) {
            boolean hovered = entry.contains(mouseX, mouseY);
            boolean active = entry.category == selectedCategory && !showFavorites && !showHistory;

            int base = hovered ? UiTheme.CARD_BG_MINIMAL_HOVER : UiTheme.CARD_BG_MINIMAL;
            int border = active ? UiTheme.ACCENT : UiTheme.CARD_BORDER_MINIMAL;
            gfx.fill(entry.x, entry.y, entry.x + entry.width, entry.y + entry.height, base);
            gfx.fill(entry.x, entry.y, entry.x + 3, entry.y + entry.height, entry.category.getColor());
            gfx.renderOutline(entry.x, entry.y, entry.width, entry.height, border);

            ItemStack icon = UiIcons.resolveItem(entry.category.getIconItemId(), Items.PAPER);
            gfx.renderItem(icon, entry.x + 8, entry.y + 10);

            String label = entry.category.getDisplayName();
            int textX = entry.x + 28;
            int textY = entry.y + (entry.height - 10) / 2 - 4;
            gfx.drawString(font, label, textX, textY, UiTheme.TEXT_PRIMARY, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0) {
            return false;
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (delta * 18), 0, maxScroll);
        applyScroll();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchWidget.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (FilterEntry entry : filterEntries) {
            if (entry.contains(mouseX, mouseY)) {
                entry.action.run();
                return true;
            }
        }
        for (FilterEntry entry : sortEntries) {
            if (entry.contains(mouseX, mouseY)) {
                entry.action.run();
                return true;
            }
        }

        for (CategoryEntry entry : categoryEntries) {
            if (entry.contains(mouseX, mouseY)) {
                selectCategory(entry.category);
                return true;
            }
        }

        for (CardEntry entry : cardEntries) {
            if (entry.card.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 291) { // F2
            toggleFavorites();
            return true;
        }
        if (keyCode == 292) { // F3
            toggleHistory();
            return true;
        }
        if (keyCode == 70 && (modifiers & 2) != 0) { // Ctrl+F
            searchWidget.setFocused(true);
            return true;
        }
        if (keyCode == 256) { // Esc
            if (searchWidget.getSearchField().isFocused()) {
                searchWidget.getSearchField().setValue("");
                searchWidget.setFocused(false);
                reloadCommands();
                return true;
            }
        }

        if (searchWidget.keyPressed(keyCode, scanCode, modifiers)) {
            reloadCommands();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchWidget.charTyped(codePoint, modifiers)) {
            reloadCommands();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean openFirstCardDetails() {
        if (cardEntries.isEmpty()) {
            return false;
        }
        CommandInfo cmd = cardEntries.get(0).card.getCommand();
        Minecraft.getInstance().setScreen(new CommandDetailScreen(this, cmd));
        return true;
    }

    private static final class CardEntry {
        private final CommandCardWidget card;
        private final int baseX;
        private final int baseY;

        private CardEntry(CommandCardWidget card, int baseX, int baseY) {
            this.card = card;
            this.baseX = baseX;
            this.baseY = baseY;
        }
    }

    private static final class FilterEntry {
        private final String label;
        private final Runnable action;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private FilterEntry(String label, Runnable action, int x, int y, int width, int height) {
            this.label = label;
            this.action = action;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private static final class CategoryEntry {
        private final CommandCategory category;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private CategoryEntry(CommandCategory category, int x, int y, int width, int height) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
