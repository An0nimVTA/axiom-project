package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class NationMapScreen extends Screen {
    private final Screen parent;
    private final List<NationMarker> nations = new ArrayList<>();
    private int offsetX = 0;
    private int offsetY = 0;
    private int zoom = 1;
    private NationMarker hoveredNation = null;

    public static class NationMarker {
        public String name;
        public int x, y;
        public int size;
        public int color;
        public int citizens;
        public int territory;
        public String leader;

        public NationMarker(String name, int x, int y, int size, int color, int citizens, int territory, String leader) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
            this.citizens = citizens;
            this.territory = territory;
            this.leader = leader;
        }
    }

    public NationMapScreen(Screen parent) {
        super(Component.literal("Карта наций"));
        this.parent = parent;
        loadNations();
    }

    private void loadNations() {
        // Mock data (will be replaced with real data from server)
        nations.add(new NationMarker("Россия", 100, 100, 50, 0xFFFF0000, 25, 150, "Player1"));
        nations.add(new NationMarker("США", 300, 150, 45, 0xFF0000FF, 20, 120, "Player2"));
        nations.add(new NationMarker("Китай", 200, 250, 40, 0xFFFFFF00, 18, 100, "Player3"));
        nations.add(new NationMarker("Германия", 150, 180, 35, 0xFF00FF00, 15, 80, "Player4"));
        nations.add(new NationMarker("Франция", 120, 220, 30, 0xFFFF00FF, 12, 60, "Player5"));
    }

    @Override
    protected void init() {
        super.init();

        // Zoom buttons
        Button zoomInButton = Button.builder(Component.literal("+"), (btn) -> {
            zoom = Math.min(3, zoom + 1);
        })
        .bounds(width - 60, 50, 20, 20)
        .build();
        addRenderableWidget(zoomInButton);

        Button zoomOutButton = Button.builder(Component.literal("-"), (btn) -> {
            zoom = Math.max(1, zoom - 1);
        })
        .bounds(width - 60, 75, 20, 20)
        .build();
        addRenderableWidget(zoomOutButton);

        // Reset button
        Button resetButton = Button.builder(Component.literal("⟲"), (btn) -> {
            offsetX = 0;
            offsetY = 0;
            zoom = 1;
        })
        .bounds(width - 60, 100, 20, 20)
        .build();
        addRenderableWidget(resetButton);

        // Back button
        Button backButton = Button.builder(Component.literal("◀ Назад"), (btn) -> {
            Minecraft.getInstance().setScreen(parent);
        })
        .bounds(10, height - 30, 100, 20)
        .build();
        addRenderableWidget(backButton);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);

        // Title
        gfx.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        gfx.drawCenteredString(font, Component.literal("§7Наций: " + nations.size()), width / 2, 25, 0xFFAAAAAA);

        // Map area
        int mapX = 50;
        int mapY = 50;
        int mapW = width - 150;
        int mapH = height - 100;

        // Map background
        gfx.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF1A1A1A);
        gfx.renderOutline(mapX, mapY, mapW, mapH, 0xFFFFFFFF);

        // Grid
        renderGrid(gfx, mapX, mapY, mapW, mapH);

        // Nations
        hoveredNation = null;
        for (NationMarker nation : nations) {
            int nx = mapX + nation.x * zoom + offsetX;
            int ny = mapY + nation.y * zoom + offsetY;
            int size = nation.size * zoom;

            // Check hover
            if (mouseX >= nx && mouseX <= nx + size && mouseY >= ny && mouseY <= ny + size) {
                hoveredNation = nation;
            }

            renderNation(gfx, nation, nx, ny, size, hoveredNation == nation);
        }

        // Info panel
        renderInfoPanel(gfx, width - 130, 130);

        // Legend
        renderLegend(gfx, 10, 50);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphics gfx, int x, int y, int w, int h) {
        int gridSize = 50 * zoom;
        int color = 0x40FFFFFF;

        // Vertical lines
        for (int i = 0; i < w; i += gridSize) {
            gfx.fill(x + i, y, x + i + 1, y + h, color);
        }

        // Horizontal lines
        for (int i = 0; i < h; i += gridSize) {
            gfx.fill(x, y + i, x + w, y + i + 1, color);
        }
    }

    private void renderNation(GuiGraphics gfx, NationMarker nation, int x, int y, int size, boolean hovered) {
        // Territory (larger circle)
        int territorySize = (int)(size * 1.5);
        int territoryColor = (nation.color & 0x00FFFFFF) | 0x40000000;
        gfx.fill(x - size / 4, y - size / 4, x + territorySize, y + territorySize, territoryColor);

        // Nation marker (circle approximation)
        gfx.fill(x, y, x + size, y + size, nation.color);
        
        if (hovered) {
            gfx.renderOutline(x - 2, y - 2, size + 4, size + 4, 0xFFFFFFFF);
        }

        // Name
        gfx.drawString(font, nation.name, x, y - 12, 0xFFFFFFFF, true);
    }

    private void renderInfoPanel(GuiGraphics gfx, int x, int y) {
        if (hoveredNation == null) {
            gfx.drawString(font, "§7Наведите на", x, y, 0xFF888888, false);
            gfx.drawString(font, "§7нацию для", x, y + 12, 0xFF888888, false);
            gfx.drawString(font, "§7информации", x, y + 24, 0xFF888888, false);
            return;
        }

        int w = 110;
        int h = 100;
        
        // Background
        gfx.fill(x, y, x + w, y + h, 0xCC000000);
        gfx.renderOutline(x, y, w, h, hoveredNation.color);

        // Info
        int ty = y + 10;
        gfx.drawString(font, "§l" + hoveredNation.name, x + 5, ty, hoveredNation.color, false);
        ty += 15;
        gfx.drawString(font, "§7Лидер:", x + 5, ty, 0xFFAAAAAA, false);
        ty += 10;
        gfx.drawString(font, hoveredNation.leader, x + 5, ty, 0xFFFFFFFF, false);
        ty += 12;
        gfx.drawString(font, "§7Граждан: §f" + hoveredNation.citizens, x + 5, ty, 0xFFAAAAAA, false);
        ty += 10;
        gfx.drawString(font, "§7Территория: §f" + hoveredNation.territory, x + 5, ty, 0xFFAAAAAA, false);
    }

    private void renderLegend(GuiGraphics gfx, int x, int y) {
        gfx.drawString(font, "§lЛегенда:", x, y, 0xFFFFFFFF, false);
        y += 15;
        
        // Circle = nation
        gfx.fill(x, y, x + 10, y + 10, 0xFFFF0000);
        gfx.drawString(font, "§7= Нация", x + 15, y + 2, 0xFFAAAAAA, false);
        y += 15;
        
        // Larger circle = territory
        gfx.fill(x, y, x + 15, y + 15, 0x40FF0000);
        gfx.drawString(font, "§7= Территория", x + 20, y + 4, 0xFFAAAAAA, false);
        y += 20;
        
        gfx.drawString(font, "§7Управление:", x, y, 0xFFFFFFFF, false);
        y += 12;
        gfx.drawString(font, "§7ЛКМ - перетащить", x, y, 0xFF888888, false);
        y += 10;
        gfx.drawString(font, "§7+/- - зум", x, y, 0xFF888888, false);
        y += 10;
        gfx.drawString(font, "§7⟲ - сброс", x, y, 0xFF888888, false);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            offsetX += (int)dragX;
            offsetY += (int)dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        zoom = Math.max(1, Math.min(3, zoom + (int)delta));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
