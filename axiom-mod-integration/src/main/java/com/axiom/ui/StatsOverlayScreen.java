package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatsOverlayScreen extends Screen {
    private final Screen parent;
    private boolean showOverlay = true;
    private Button toggleButton;
    
    // Mock data (will be replaced with real data from server)
    private double balance = 0;
    private String nationName = "Нет нации";
    private int techProgress = 0;
    private int totalTechs = 45;
    private int activeWars = 0;
    private int citizens = 0;
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 5000; // 5 seconds

    public StatsOverlayScreen(Screen parent) {
        super(Component.literal("Статистика"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        
        // Toggle button
        toggleButton = Button.builder(
            Component.literal(showOverlay ? "Скрыть статистику" : "Показать статистику"),
            (btn) -> {
                showOverlay = !showOverlay;
                btn.setMessage(Component.literal(showOverlay ? "Скрыть статистику" : "Показать статистику"));
            }
        )
        .bounds(10, height - 30, 150, 20)
        .build();
        addRenderableWidget(toggleButton);
        
        // Back button
        Button backButton = Button.builder(Component.literal("◀ Назад"), (btn) -> {
            Minecraft.getInstance().setScreen(parent);
        })
        .bounds(width - 110, height - 30, 100, 20)
        .build();
        addRenderableWidget(backButton);
        
        // Request data from server
        requestStatsUpdate();
    }

    private void requestStatsUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < UPDATE_INTERVAL) {
            return;
        }
        lastUpdate = currentTime;
        
        // Request real data from server
        AxiomUiMod.requestUpdate("stats");
        updateFromCache();
    }

    private void updateFromCache() {
        var stats = AxiomUiMod.cachedStats;
        if (stats.isEmpty()) {
            updateMockData();
            return;
        }
        
        balance = ((Number) stats.getOrDefault("balance", 0.0)).doubleValue();
        nationName = (String) stats.getOrDefault("nationName", "Нет нации");
        citizens = ((Number) stats.getOrDefault("population", 0)).intValue();
        
        // Calculate tech progress from cached techs
        long unlocked = AxiomUiMod.cachedTechs.stream()
            .filter(t -> Boolean.TRUE.equals(t.get("unlocked")))
            .count();
        techProgress = (int) unlocked;
        totalTechs = AxiomUiMod.cachedTechs.size();
    }

    private void updateMockData() {
        balance = 15000 + (Math.random() * 1000);
        nationName = "Россия";
        techProgress = 12;
        activeWars = 2;
        citizens = 15;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UiTheme.drawBackdrop(gfx, width, height);
        
        // Title
        gfx.drawCenteredString(font, title, width / 2, 20, UiTheme.TEXT_PRIMARY);
        
        if (showOverlay) {
            renderStats(gfx);
        } else {
            gfx.drawCenteredString(font, Component.literal("Статистика скрыта"), width / 2, height / 2, UiTheme.TEXT_DIM);
        }
        
        super.render(gfx, mouseX, mouseY, partialTick);
        
        // Auto-update
        requestStatsUpdate();
    }

    private void renderStats(GuiGraphics gfx) {
        float scale = UiLayout.scaleFor(width, height);
        int x = UiLayout.scaled(24, scale);
        int y = UiLayout.scaled(60, scale);
        int lineHeight = UiLayout.scaled(34, scale);
        int cardWidth = width - x * 2;
        int cardHeight = UiLayout.scaled(28, scale);

        // Wallet
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "Кошелёк", 
            String.format("%.2f ₽", balance), 0xFF4CAF50);
        y += lineHeight;

        // Nation
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "Нация", 
            nationName, 0xFF2196F3);
        y += lineHeight;

        // Citizens
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "Граждан", 
            String.valueOf(citizens), 0xFF9C27B0);
        y += lineHeight;

        // Tech progress
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "Технологии", 
            String.format("%d / %d (%.0f%%)", techProgress, totalTechs, 
                (techProgress * 100.0 / totalTechs)), 0xFFFFC107);
        y += lineHeight;

        // Active wars
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "Активные войны", 
            String.valueOf(activeWars), activeWars > 0 ? 0xFFF44336 : 0xFF4CAF50);
        y += lineHeight;

        // Tech progress bar
        y += 10;
        renderProgressBar(gfx, x, y, cardWidth, UiLayout.scaled(14, scale), techProgress, totalTechs, 0xFF9C27B0);
        gfx.drawString(font, "Прогресс технологий", x, y - UiLayout.scaled(14, scale), UiTheme.TEXT_PRIMARY, false);

        // Last update
        y += 40;
        gfx.drawString(font, "Обновлено: только что", x, y, UiTheme.TEXT_DIM, false);
    }

    private void renderStatCard(GuiGraphics gfx, int x, int y, int w, int h, String label, String value, int color) {
        UiTheme.drawMinimalCard(gfx, x, y, x + w, y + h, color, false);
        
        // Label
        gfx.drawString(font, label, x + 10, y + 7, UiTheme.TEXT_PRIMARY, false);
        
        // Value (right-aligned)
        int valueWidth = font.width(value);
        gfx.drawString(font, Component.literal(value), x + w - valueWidth - 10, y + 7, UiTheme.TEXT_PRIMARY, false);
    }

    private void renderProgressBar(GuiGraphics gfx, int x, int y, int w, int h, int current, int max, int color) {
        // Background
        gfx.fill(x, y, x + w, y + h, UiTheme.BUTTON_BG);
        
        // Progress
        int progress = (int)((current / (double)max) * w);
        gfx.fill(x, y, x + progress, y + h, color);
        
        // Border
        gfx.renderOutline(x, y, w, h, UiTheme.CARD_BORDER_MINIMAL);
        
        // Text
        String text = String.format("%d%%", (int)((current / (double)max) * 100));
        int textWidth = font.width(text);
        gfx.drawString(font, text, x + (w - textWidth) / 2, y + 3, UiTheme.TEXT_PRIMARY, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
