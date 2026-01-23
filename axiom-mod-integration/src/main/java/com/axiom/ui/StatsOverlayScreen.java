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
        
        // TODO: Send packet to server to request stats
        // For now, use mock data
        updateMockData();
    }

    private void updateMockData() {
        // Mock data for demonstration
        balance = 15000 + (Math.random() * 1000);
        nationName = "Россия";
        techProgress = 12;
        activeWars = 2;
        citizens = 15;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        
        // Title
        gfx.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        
        if (showOverlay) {
            renderStats(gfx);
        } else {
            gfx.drawCenteredString(font, Component.literal("§7Статистика скрыта"), width / 2, height / 2, 0xFF888888);
        }
        
        super.render(gfx, mouseX, mouseY, partialTick);
        
        // Auto-update
        requestStatsUpdate();
    }

    private void renderStats(GuiGraphics gfx) {
        int x = 50;
        int y = 60;
        int lineHeight = 25;
        int cardWidth = width - 100;
        int cardHeight = 20;

        // Wallet
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "💰 Кошелёк", 
            String.format("%.2f ₽", balance), 0xFF4CAF50);
        y += lineHeight;

        // Nation
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "🏛️ Нация", 
            nationName, 0xFF2196F3);
        y += lineHeight;

        // Citizens
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "👥 Граждан", 
            String.valueOf(citizens), 0xFF9C27B0);
        y += lineHeight;

        // Tech progress
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "🔬 Технологии", 
            String.format("%d / %d (%.0f%%)", techProgress, totalTechs, 
                (techProgress * 100.0 / totalTechs)), 0xFFFFC107);
        y += lineHeight;

        // Active wars
        renderStatCard(gfx, x, y, cardWidth, cardHeight, "⚔️ Активные войны", 
            String.valueOf(activeWars), activeWars > 0 ? 0xFFF44336 : 0xFF4CAF50);
        y += lineHeight;

        // Tech progress bar
        y += 10;
        renderProgressBar(gfx, x, y, cardWidth, 15, techProgress, totalTechs, 0xFF9C27B0);
        gfx.drawString(font, "Прогресс технологий", x, y - 15, 0xFFFFFFFF, false);

        // Last update
        y += 40;
        gfx.drawString(font, "§7Обновлено: только что", x, y, 0xFF888888, false);
    }

    private void renderStatCard(GuiGraphics gfx, int x, int y, int w, int h, String label, String value, int color) {
        // Background
        int bgColor = (color & 0x00FFFFFF) | 0x40000000;
        gfx.fill(x, y, x + w, y + h, bgColor);
        
        // Border
        gfx.renderOutline(x, y, w, h, color);
        
        // Label
        gfx.drawString(font, label, x + 10, y + 6, 0xFFFFFFFF, false);
        
        // Value (right-aligned)
        int valueWidth = font.width(value);
        gfx.drawString(font, Component.literal("§l" + value), x + w - valueWidth - 10, y + 6, color, false);
    }

    private void renderProgressBar(GuiGraphics gfx, int x, int y, int w, int h, int current, int max, int color) {
        // Background
        gfx.fill(x, y, x + w, y + h, 0xFF333333);
        
        // Progress
        int progress = (int)((current / (double)max) * w);
        gfx.fill(x, y, x + progress, y + h, color);
        
        // Border
        gfx.renderOutline(x, y, w, h, 0xFFFFFFFF);
        
        // Text
        String text = String.format("%d%%", (int)((current / (double)max) * 100));
        int textWidth = font.width(text);
        gfx.drawString(font, text, x + (w - textWidth) / 2, y + 4, 0xFFFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
