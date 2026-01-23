package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CommandMenuScreen extends Screen {
    private final List<CommandCardWidget> cards = new ArrayList<>();
    private CommandCategory selectedCategory = null;
    private int scrollOffset = 0;
    private final int CARD_SPACING = 10;
    private final int CARDS_PER_PAGE = 5;
    
    private Button[] categoryButtons;
    private Button backButton;
    private Button favoritesButton;
    private Button historyButton;
    
    private CommandSearchWidget searchWidget;
    private final CommandHistory commandHistory = new CommandHistory();
    
    private boolean showFavorites = false;
    private boolean showHistory = false;
    
    // Optimization: cache visible area
    private int visibleAreaTop = 140;
    private int visibleAreaBottom;
    private long lastRenderTime = 0;
    private static final long RENDER_THROTTLE = 16; // ~60 FPS

    public CommandMenuScreen() {
        super(Component.literal("AXIOM - Команды плагина"));
    }

    @Override
    protected void init() {
        super.init();
        
        // Search widget
        searchWidget = new CommandSearchWidget(10, 45, 400, CommandCatalog.getAllCommands());
        
        // Category buttons (top row)
        int btnWidth = 100;
        int btnHeight = 25;
        int startX = 10;
        int y = 80;
        
        CommandCategory[] categories = CommandCategory.values();
        categoryButtons = new Button[categories.length];
        
        for (int i = 0; i < categories.length; i++) {
            CommandCategory cat = categories[i];
            int x = startX + (i % 5) * (btnWidth + 5);
            if (i > 0 && i % 5 == 0) {
                y += btnHeight + 5;
            }
            
            categoryButtons[i] = Button.builder(
                Component.literal(cat.getDisplayName()),
                (btn) -> selectCategory(cat)
            )
            .bounds(x, y, btnWidth, btnHeight)
            .build();
            
            addRenderableWidget(categoryButtons[i]);
        }
        
        // Back button
        backButton = Button.builder(Component.literal("◀ Назад"), (btn) -> onClose())
            .bounds(width - 110, 10, 100, 20)
            .build();
        addRenderableWidget(backButton);
        
        // Favorites button
        favoritesButton = Button.builder(Component.literal("★ Избранное"), (btn) -> toggleFavorites())
            .bounds(width - 220, 10, 100, 20)
            .build();
        addRenderableWidget(favoritesButton);
        
        // History button
        historyButton = Button.builder(Component.literal("🕐 История"), (btn) -> toggleHistory())
            .bounds(width - 330, 10, 100, 20)
            .build();
        addRenderableWidget(historyButton);
        
        // Tech tree button
        Button techTreeButton = Button.builder(Component.literal("🌳 Технологии"), (btn) -> {
            Minecraft.getInstance().setScreen(new TechnologyTreeScreen(this));
        })
        .bounds(width - 440, 10, 100, 20)
        .build();
        addRenderableWidget(techTreeButton);
        
        // Stats button
        Button statsButton = Button.builder(Component.literal("📊 Статистика"), (btn) -> {
            Minecraft.getInstance().setScreen(new StatsOverlayScreen(this));
        })
        .bounds(width - 550, 10, 100, 20)
        .build();
        addRenderableWidget(statsButton);
        
        // Map button
        Button mapButton = Button.builder(Component.literal("🗺️ Карта"), (btn) -> {
            Minecraft.getInstance().setScreen(new NationMapScreen(this));
        })
        .bounds(width - 660, 10, 100, 20)
        .build();
        addRenderableWidget(mapButton);
        
        // Education button
        Button educationButton = Button.builder(Component.literal("🎓 Образование"), (btn) -> {
            Minecraft.getInstance().setScreen(new EducationScreen(this));
        })
        .bounds(width - 780, 10, 110, 20)
        .build();
        addRenderableWidget(educationButton);
        
        // Culture button
        Button cultureButton = Button.builder(Component.literal("🎭 Культура"), (btn) -> {
            Minecraft.getInstance().setScreen(new CultureScreen(this));
        })
        .bounds(width - 900, 10, 110, 20)
        .build();
        addRenderableWidget(cultureButton);
        
        // Ecology button
        Button ecologyButton = Button.builder(Component.literal("🌿 Экология"), (btn) -> {
            Minecraft.getInstance().setScreen(new EcologyScreen(this));
        })
        .bounds(width - 1020, 10, 110, 20)
        .build();
        addRenderableWidget(ecologyButton);
        
        // Espionage button
        Button espionageButton = Button.builder(Component.literal("🕵️ Шпионаж"), (btn) -> {
            Minecraft.getInstance().setScreen(new EspionageScreen(this));
        })
        .bounds(width - 1140, 10, 110, 20)
        .build();
        addRenderableWidget(espionageButton);
        
        // Analytics button
        Button analyticsButton = Button.builder(Component.literal("📊 Аналитика"), (btn) -> {
            Minecraft.getInstance().setScreen(new AnalyticsScreen(this));
        })
        .bounds(width - 1260, 10, 110, 20)
        .build();
        addRenderableWidget(analyticsButton);
        
        // Balance button
        Button balanceButton = Button.builder(Component.literal("⚖️ Баланс"), (btn) -> {
            Minecraft.getInstance().setScreen(new ItemBalanceScreen(this));
        })
        .bounds(width - 1380, 10, 110, 20)
        .build();
        addRenderableWidget(balanceButton);
        
        // Recipes button
        Button recipesButton = Button.builder(Component.literal("📝 Крафты"), (btn) -> {
            Minecraft.getInstance().setScreen(new RecipeEditorScreen(this));
        })
        .bounds(width - 1500, 10, 110, 20)
        .build();
        addRenderableWidget(recipesButton);
        
        // Load all commands by default
        loadCommands(null);
    }

    private void toggleFavorites() {
        showFavorites = !showFavorites;
        showHistory = false;
        scrollOffset = 0;
        
        if (showFavorites) {
            loadFavoriteCommands();
        } else {
            loadCommands(selectedCategory);
        }
    }

    private void toggleHistory() {
        showHistory = !showHistory;
        showFavorites = false;
        scrollOffset = 0;
        
        if (showHistory) {
            loadHistoryCommands();
        } else {
            loadCommands(selectedCategory);
        }
    }

    private void loadFavoriteCommands() {
        cards.clear();
        List<String> favoriteIds = commandHistory.getFavorites();
        List<CommandInfo> allCommands = CommandCatalog.getAllCommands();
        
        int cardY = 140;
        for (String favId : favoriteIds) {
            CommandInfo cmd = allCommands.stream()
                .filter(c -> c.getCommand().equals(favId))
                .findFirst()
                .orElse(null);
            
            if (cmd != null) {
                CommandCardWidget card = createCard(cmd, cardY);
                cards.add(card);
                cardY += CommandCardWidget.HEIGHT + CARD_SPACING;
            }
        }
    }

    private void loadHistoryCommands() {
        cards.clear();
        List<CommandHistory.HistoryEntry> historyEntries = commandHistory.getHistory();
        List<CommandInfo> allCommands = CommandCatalog.getAllCommands();
        
        int cardY = 140;
        for (CommandHistory.HistoryEntry entry : historyEntries) {
            CommandInfo cmd = allCommands.stream()
                .filter(c -> c.getCommand().equals(entry.command))
                .findFirst()
                .orElse(null);
            
            if (cmd != null) {
                CommandCardWidget card = createCard(cmd, cardY);
                cards.add(card);
                cardY += CommandCardWidget.HEIGHT + CARD_SPACING;
            }
        }
    }

    private void selectCategory(CommandCategory category) {
        selectedCategory = category;
        showFavorites = false;
        showHistory = false;
        scrollOffset = 0;
        loadCommands(category);
    }

    private void loadCommands(CommandCategory category) {
        cards.clear();
        
        List<CommandInfo> commands = searchWidget.getFilteredCommands();
        if (category != null) {
            commands = commands.stream()
                .filter(cmd -> cmd.getCategory() == category)
                .toList();
        }
        
        int cardY = 140;
        for (CommandInfo cmd : commands) {
            CommandCardWidget card = createCard(cmd, cardY);
            cards.add(card);
            cardY += CommandCardWidget.HEIGHT + CARD_SPACING;
        }
    }

    private CommandCardWidget createCard(CommandInfo cmd, int cardY) {
        CommandCardWidget card = new CommandCardWidget(
            20, cardY, cmd,
            () -> executeCommand(cmd),
            () -> showDetails(cmd),
            () -> toggleFavorite(cmd)
        );
        card.setFavorite(commandHistory.isFavorite(cmd.getCommand()));
        return card;
    }

    private void toggleFavorite(CommandInfo cmd) {
        if (commandHistory.isFavorite(cmd.getCommand())) {
            commandHistory.removeFavorite(cmd.getCommand());
            NotificationManager.getInstance().info("Удалено из избранного: " + cmd.getDisplayName());
        } else {
            commandHistory.addFavorite(cmd.getCommand());
            NotificationManager.getInstance().success("Добавлено в избранное: " + cmd.getDisplayName());
        }
        // Refresh if showing favorites
        if (showFavorites) {
            loadFavoriteCommands();
        }
    }

    private void executeCommand(CommandInfo cmd) {
        // Add to history
        commandHistory.addToHistory(cmd.getCommand());
        
        // Show notification
        NotificationManager.getInstance().info("Выполнение: " + cmd.getDisplayName());
        
        // Open command input screen with pre-filled command
        Minecraft.getInstance().setScreen(new CommandInputScreen(this, cmd));
    }

    private void showDetails(CommandInfo cmd) {
        // Open detailed info screen
        Minecraft.getInstance().setScreen(new CommandDetailScreen(this, cmd));
    }

    private void openSearch() {
        searchWidget.setFocused(true);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Throttle rendering for performance
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRenderTime < RENDER_THROTTLE) {
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }
        lastRenderTime = currentTime;
        
        // Dark background
        renderBackground(gfx);
        
        // Title
        String titleText = showFavorites ? "★ Избранные команды" : 
                          showHistory ? "🕐 История команд" : 
                          "AXIOM - Команды плагина";
        gfx.drawCenteredString(font, titleText, width / 2, 15, 0xFFFFFFFF);
        
        // Search widget
        searchWidget.render(gfx, mouseX, mouseY, partialTick);
        
        // Category description
        if (selectedCategory != null && !showFavorites && !showHistory) {
            String desc = "§7" + selectedCategory.getDescription();
            gfx.drawCenteredString(font, Component.literal(desc), width / 2, 65, 0xFFAAAAAA);
        } else if (showFavorites) {
            gfx.drawCenteredString(font, Component.literal("§7Ваши избранные команды"), width / 2, 65, 0xFFAAAAAA);
        } else if (showHistory) {
            gfx.drawCenteredString(font, Component.literal("§7Последние использованные команды"), width / 2, 65, 0xFFAAAAAA);
        } else {
            gfx.drawCenteredString(font, Component.literal("§7Все команды плагина"), width / 2, 65, 0xFFAAAAAA);
        }
        
        // Render cards with scrolling and culling
        visibleAreaBottom = height - 50;
        gfx.enableScissor(0, visibleAreaTop, width, visibleAreaBottom);
        
        // Only render visible cards (culling)
        for (CommandCardWidget card : cards) {
            int cardY = card.getY();
            if (cardY + CommandCardWidget.HEIGHT >= visibleAreaTop && cardY <= visibleAreaBottom) {
                card.render(gfx, mouseX, mouseY, partialTick);
            }
        }
        
        gfx.disableScissor();
        
        // Scroll indicator
        if (cards.size() > CARDS_PER_PAGE) {
            String scrollText = String.format("§7Команд: %d | Прокрутка: колесо мыши", cards.size());
            gfx.drawCenteredString(font, Component.literal(scrollText), width / 2, height - 30, 0xFF888888);
        }
        
        // Stats footer
        String stats = String.format("§7Всего команд: %d | Избранных: %d | История: %d", 
            CommandCatalog.getAllCommands().size(),
            commandHistory.getFavorites().size(),
            commandHistory.getHistory().size());
        gfx.drawString(font, stats, 10, height - 15, 0xFF666666, false);
        
        super.render(gfx, mouseX, mouseY, partialTick);
        
        // Clear expired cache periodically
        if (currentTime % 5000 < RENDER_THROTTLE) {
            RenderCache.getInstance().clearExpired();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset -= (int)(delta * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, 
            Math.max(0, cards.size() * (CommandCardWidget.HEIGHT + CARD_SPACING) - (height - 190))));
        
        // Update card positions
        int cardY = 140 - scrollOffset;
        for (CommandCardWidget card : cards) {
            card.setPosition(20, cardY);
            cardY += CommandCardWidget.HEIGHT + CARD_SPACING;
        }
        
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchWidget.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        for (CommandCardWidget card : cards) {
            if (card.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // F1 - open menu (already handled by command registration)
        // F2 - favorites
        if (keyCode == 291) { // F2
            toggleFavorites();
            return true;
        }
        
        // F3 - history
        if (keyCode == 292) { // F3
            toggleHistory();
            return true;
        }
        
        // Ctrl+F - search
        if (keyCode == 70 && (modifiers & 2) != 0) { // F key with Ctrl
            searchWidget.setFocused(true);
            return true;
        }
        
        // Esc - clear search or close
        if (keyCode == 256) { // Esc
            if (searchWidget.getSearchField().isFocused()) {
                searchWidget.getSearchField().setValue("");
                searchWidget.setFocused(false);
                loadCommands(selectedCategory);
                return true;
            }
        }
        
        // Handle search widget input
        if (searchWidget.keyPressed(keyCode, scanCode, modifiers)) {
            // Reload commands with search filter
            if (!showFavorites && !showHistory) {
                loadCommands(selectedCategory);
            }
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchWidget.charTyped(codePoint, modifiers)) {
            // Reload commands with search filter
            if (!showFavorites && !showHistory) {
                loadCommands(selectedCategory);
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
