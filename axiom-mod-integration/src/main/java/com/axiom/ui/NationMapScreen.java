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
    private final List<TerritorySquare> territories = new ArrayList<>();
    private final Map<String, Integer> nationColors = new HashMap<>();
    private final Map<String, String> nationNames = new HashMap<>();
    private final List<String> nationOrder = new ArrayList<>();
    private int offsetX = 0;
    private int offsetY = 0;
    private int zoom = 1;
    private NationMarker hoveredNation = null;
    private HoveredTerritory hoveredTerritory = null;
    private int minX = 0;
    private int maxX = 0;
    private int minZ = 0;
    private int maxZ = 0;
    private boolean boundsInitialized = false;
    private boolean dragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private int mapX = 50;
    private int mapY = 50;
    private int mapW = 0;
    private int mapH = 0;
    private int cachedCount = -1;
    private long lastTerritoryRevision = -1L;
    private String worldFilter = null;
    private String renderKey = "";
    private final List<RenderTile> renderTiles = new ArrayList<>();
    private String filterNationId = null;
    private int filterIndex = -1;
    private int territoryAlpha = 0x55;
    private Button filterButton;
    private Button opacityDownButton;
    private Button opacityUpButton;

    public static class NationMarker {
        public String name;
        public int x, z;
        public int size;
        public int color;
        public int citizens;
        public int territory;
        public String leader;

        public NationMarker(String name, int x, int z, int size, int color, int citizens, int territory, String leader) {
            this.name = name;
            this.x = x;
            this.z = z;
            this.size = size;
            this.color = color;
            this.citizens = citizens;
            this.territory = territory;
            this.leader = leader;
        }
    }

    public NationMapScreen(Screen parent) {
        super(UiText.text("Карта наций", "Nation Map"));
        this.parent = parent;
        loadNations();
        loadTerritories();
    }

    private void loadNations() {
        nations.clear();
        nationColors.clear();
        nationNames.clear();
        nationOrder.clear();
        AxiomUiMod.requestUpdate("nations");
        resetBounds();

        if (AxiomUiMod.cachedNations == null || AxiomUiMod.cachedNations.isEmpty()) {
            cachedCount = 0;
            return;
        }
        cachedCount = AxiomUiMod.cachedNations.size();

        for (var entry : AxiomUiMod.cachedNations) {
            if (entry == null) continue;
            String name = value(entry.get("name"), UiText.pick("Неизвестно", "Unknown"));
            int centerX = value(entry.get("centerX"), 0);
            int centerZ = value(entry.get("centerZ"), 0);
            int territory = value(entry.get("territory"), 0);
            int citizens = value(entry.get("population"), 0);
            String leader = value(entry.get("leader"), UiText.pick("Неизвестно", "Unknown"));
            int size = Math.max(8, Math.min(30, 6 + (int) (Math.sqrt(Math.max(1, territory)) * 2)));

            int color = generateColor(name);
            String id = value(entry.get("id"), name);
            nationColors.put(id, color);
            nationNames.put(id, name);
            nationOrder.add(id);

            updateBounds(centerX, centerZ);

            nations.add(new NationMarker(name, centerX, centerZ, size, color, citizens, territory, leader));
        }
        if (filterIndex >= nationOrder.size()) {
            filterIndex = -1;
            filterNationId = null;
        } else if (filterIndex >= 0 && filterIndex < nationOrder.size()) {
            filterNationId = nationOrder.get(filterIndex);
        }
        updateFilterButtonLabel();
    }

    private void loadTerritories() {
        territories.clear();
        worldFilter = resolveWorldFilter();

        List<AxiomUiMod.TerritoryTile> tiles = AxiomUiMod.getTerritoryTiles();
        if (tiles == null || tiles.isEmpty()) {
            lastTerritoryRevision = AxiomUiMod.getTerritoryRevision();
            return;
        }

        for (AxiomUiMod.TerritoryTile tile : tiles) {
            if (tile == null) continue;
            String world = tile.world;
            if (worldFilter != null && !worldFilter.equals(world)) {
                continue;
            }
            territories.add(new TerritorySquare(world, tile.x, tile.z, tile.nationId));
            updateBounds(tile.x, tile.z);
        }
        lastTerritoryRevision = AxiomUiMod.getTerritoryRevision();
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

        filterButton = Button.builder(Component.literal("Фильтр: Все"), (btn) -> cycleFilter())
            .bounds(width - 140, 130, 100, 20)
            .build();
        addRenderableWidget(filterButton);

        opacityDownButton = Button.builder(Component.literal("-α"), (btn) -> adjustOpacity(-0x10))
            .bounds(width - 60, 130, 20, 20)
            .build();
        addRenderableWidget(opacityDownButton);

        opacityUpButton = Button.builder(Component.literal("+α"), (btn) -> adjustOpacity(0x10))
            .bounds(width - 60, 155, 20, 20)
            .build();
        addRenderableWidget(opacityUpButton);

        // Back button
        Button backButton = Button.builder(UiText.text("Назад", "Back"), (btn) -> {
            Minecraft.getInstance().setScreen(parent);
        })
        .bounds(10, height - 30, 100, 20)
        .build();
        addRenderableWidget(backButton);

        updateFilterButtonLabel();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if ((AxiomUiMod.cachedNations != null && AxiomUiMod.cachedNations.size() != cachedCount)) {
            loadNations();
        }
        if (!Objects.equals(worldFilter, resolveWorldFilter())) {
            loadTerritories();
        }
        if (AxiomUiMod.getTerritoryRevision() != lastTerritoryRevision) {
            loadTerritories();
        }
        renderBackground(gfx);

        // Title
        gfx.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        String countText = UiText.pick("§7Наций: ", "§7Nations: ") + nations.size();
        gfx.drawCenteredString(font, Component.literal(countText), width / 2, 25, 0xFFAAAAAA);

        // Map area
        mapX = 50;
        mapY = 50;
        mapW = width - 150;
        mapH = height - 100;

        // Map background
        gfx.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF1A1A1A);
        gfx.renderOutline(mapX, mapY, mapW, mapH, 0xFFFFFFFF);

        // Grid
        renderGrid(gfx, mapX, mapY, mapW, mapH);

        // Territories
        renderTerritories(gfx, mapX, mapY, mapW, mapH);
        hoveredTerritory = resolveHoveredTerritory(mouseX, mouseY);
        if (hoveredTerritory != null) {
            renderTerritoryTooltip(gfx, mouseX, mouseY, hoveredTerritory);
        }

        // Nations
        hoveredNation = null;
        int spanX = Math.max(1, maxX - minX);
        int spanZ = Math.max(1, maxZ - minZ);
        int padding = 20;
        int usableW = Math.max(1, mapW - padding * 2);
        int usableH = Math.max(1, mapH - padding * 2);
        for (NationMarker nation : nations) {
            double nxNorm = (nation.x - minX) / (double) spanX;
            double nzNorm = (nation.z - minZ) / (double) spanZ;
            int nx = mapX + padding + (int) (nxNorm * usableW) + offsetX;
            int ny = mapY + padding + (int) (nzNorm * usableH) + offsetY;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideMap(mouseX, mouseY)) {
            dragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            offsetX += (int) (mouseX - lastMouseX);
            offsetY += (int) (mouseY - lastMouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isInsideMap(double mouseX, double mouseY) {
        return mouseX >= mapX && mouseX <= mapX + mapW && mouseY >= mapY && mouseY <= mapY + mapH;
    }

    public boolean hasData() {
        return !nations.isEmpty() || !territories.isEmpty();
    }

    private void renderTerritories(GuiGraphics gfx, int x, int y, int w, int h) {
        if (territories.isEmpty()) {
            return;
        }
        String nextKey = buildRenderKey();
        if (!nextKey.equals(renderKey)) {
            rebuildRenderTiles();
            renderKey = nextKey;
        }

        for (RenderTile tile : renderTiles) {
            gfx.fill(tile.x, tile.y, tile.x + tile.size, tile.y + tile.size, tile.color);
        }
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
            gfx.drawString(font, UiText.pick("§7Наведите на", "§7Hover over"), x, y, 0xFF888888, false);
            gfx.drawString(font, UiText.pick("§7нацию для", "§7a nation"), x, y + 12, 0xFF888888, false);
            gfx.drawString(font, UiText.pick("§7информации", "§7for info"), x, y + 24, 0xFF888888, false);
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
        gfx.drawString(font, UiText.pick("§7Лидер:", "§7Leader:"), x + 5, ty, 0xFFAAAAAA, false);
        ty += 10;
        gfx.drawString(font, hoveredNation.leader, x + 5, ty, 0xFFFFFFFF, false);
        ty += 12;
        gfx.drawString(font, UiText.pick("§7Граждан: §f", "§7Citizens: §f") + hoveredNation.citizens, x + 5, ty, 0xFFAAAAAA, false);
        ty += 10;
        gfx.drawString(font, UiText.pick("§7Территория: §f", "§7Territory: §f") + hoveredNation.territory, x + 5, ty, 0xFFAAAAAA, false);
        if (hoveredTerritory != null) {
            ty += 12;
            String ownerName = resolveNationName(hoveredTerritory.nationId);
            gfx.drawString(font, UiText.pick("§7Чанк: §f", "§7Chunk: §f") + hoveredTerritory.x + "," + hoveredTerritory.z, x + 5, ty, 0xFFAAAAAA, false);
            ty += 10;
            gfx.drawString(font, UiText.pick("§7Владелец: §f", "§7Owner: §f") + ownerName, x + 5, ty, 0xFFAAAAAA, false);
        }
    }

    private void renderLegend(GuiGraphics gfx, int x, int y) {
        gfx.drawString(font, UiText.pick("§lЛегенда:", "§lLegend:"), x, y, 0xFFFFFFFF, false);
        y += 15;
        
        // Circle = nation
        gfx.fill(x, y, x + 10, y + 10, 0xFFFF0000);
        gfx.drawString(font, UiText.pick("§7= Нация", "§7= Nation"), x + 15, y + 2, 0xFFAAAAAA, false);
        y += 15;
        
        // Larger circle = territory
        gfx.fill(x, y, x + 15, y + 15, 0x40FF0000);
        gfx.drawString(font, UiText.pick("§7= Территория", "§7= Territory"), x + 20, y + 4, 0xFFAAAAAA, false);
        y += 20;
        
        gfx.drawString(font, UiText.pick("§7Управление:", "§7Controls:"), x, y, 0xFFFFFFFF, false);
        y += 12;
        gfx.drawString(font, UiText.pick("§7ЛКМ - перетащить", "§7LMB - drag"), x, y, 0xFF888888, false);
        y += 10;
        gfx.drawString(font, UiText.pick("§7+/- - зум", "§7+/- - zoom"), x, y, 0xFF888888, false);
        y += 10;
        gfx.drawString(font, UiText.pick("§7⟲ - сброс", "§7⟲ - reset"), x, y, 0xFF888888, false);
        y += 12;
        String filterLabel = filterNationId == null ? UiText.pick("§7Фильтр: все", "§7Filter: all")
            : UiText.pick("§7Фильтр: ", "§7Filter: ") + resolveNationName(filterNationId);
        gfx.drawString(font, filterLabel, x, y, 0xFF888888, false);
        y += 10;
        gfx.drawString(font, UiText.pick("§7Прозрачность: ", "§7Opacity: ") + territoryAlpha, x, y, 0xFF888888, false);
    }

    private static String value(Object obj, String fallback) {
        if (obj == null) return fallback;
        String s = obj.toString();
        return s.isBlank() ? fallback : s;
    }

    private static int value(Object obj, int fallback) {
        if (obj == null) return fallback;
        if (obj instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int generateColor(String seed) {
        int hash = seed == null ? 0 : seed.hashCode();
        int r = 80 + Math.abs(hash) % 140;
        int g = 80 + Math.abs(hash / 3) % 140;
        int b = 80 + Math.abs(hash / 7) % 140;
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private HoveredTerritory resolveHoveredTerritory(double mouseX, double mouseY) {
        if (!isInsideMap(mouseX, mouseY)) {
            return null;
        }
        if (territories.isEmpty()) {
            return null;
        }
        int spanX = Math.max(1, maxX - minX);
        int spanZ = Math.max(1, maxZ - minZ);
        int padding = 20;
        int usableW = Math.max(1, mapW - padding * 2);
        int usableH = Math.max(1, mapH - padding * 2);
        double relX = mouseX - mapX - padding - offsetX;
        double relZ = mouseY - mapY - padding - offsetY;
        if (relX < 0 || relZ < 0 || relX > usableW || relZ > usableH) {
            return null;
        }
        int chunkX = minX + (int) Math.round((relX / (double) usableW) * spanX);
        int chunkZ = minZ + (int) Math.round((relZ / (double) usableH) * spanZ);
        String world = resolveHoverWorld();
        if (world == null) {
            return null;
        }
        String owner = AxiomUiMod.getTerritoryOwner(world, chunkX, chunkZ);
        if (owner == null) {
            return null;
        }
        if (filterNationId != null && !filterNationId.equals(owner)) {
            return null;
        }
        return new HoveredTerritory(world, chunkX, chunkZ, owner);
    }

    private String resolveHoverWorld() {
        if (worldFilter != null) {
            return worldFilter;
        }
        if (!territories.isEmpty()) {
            return territories.get(0).world;
        }
        return null;
    }

    private void renderTerritoryTooltip(GuiGraphics gfx, int mouseX, int mouseY, HoveredTerritory info) {
        String ownerName = resolveNationName(info.nationId);
        String line1 = UiText.pick("Чанк ", "Chunk ") + info.x + "," + info.z;
        String line2 = UiText.pick("Владелец: ", "Owner: ") + ownerName;
        int width1 = font.width(line1);
        int width2 = font.width(line2);
        int w = Math.max(width1, width2) + 8;
        int h = 22;
        int x = Math.min(mouseX + 12, this.width - w - 4);
        int y = Math.min(mouseY + 12, this.height - h - 4);
        gfx.fill(x, y, x + w, y + h, 0xCC000000);
        gfx.renderOutline(x, y, w, h, 0xFFFFFFFF);
        gfx.drawString(font, line1, x + 4, y + 4, 0xFFFFFFFF, false);
        gfx.drawString(font, line2, x + 4, y + 14, 0xFFCCCCCC, false);
    }

    private void cycleFilter() {
        if (nationOrder.isEmpty()) {
            filterNationId = null;
            filterIndex = -1;
            updateFilterButtonLabel();
            renderKey = "";
            return;
        }
        filterIndex++;
        if (filterIndex >= nationOrder.size()) {
            filterIndex = -1;
            filterNationId = null;
        } else {
            filterNationId = nationOrder.get(filterIndex);
        }
        updateFilterButtonLabel();
        renderKey = "";
    }

    private void updateFilterButtonLabel() {
        if (filterButton == null) {
            return;
        }
        String label = filterNationId == null
            ? UiText.pick("Фильтр: все", "Filter: all")
            : UiText.pick("Фильтр: ", "Filter: ") + resolveNationName(filterNationId);
        filterButton.setMessage(Component.literal(label));
    }

    private String resolveNationName(String nationId) {
        if (nationId == null || nationId.isBlank()) {
            return UiText.pick("Нет", "None");
        }
        return nationNames.getOrDefault(nationId, nationId);
    }

    private void adjustOpacity(int delta) {
        int next = Math.max(0x20, Math.min(0xAA, territoryAlpha + delta));
        if (next != territoryAlpha) {
            territoryAlpha = next;
            renderKey = "";
        }
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

    private void resetBounds() {
        boundsInitialized = false;
        minX = 0;
        maxX = 0;
        minZ = 0;
        maxZ = 0;
    }

    private void updateBounds(int x, int z) {
        if (!boundsInitialized) {
            minX = maxX = x;
            minZ = maxZ = z;
            boundsInitialized = true;
            return;
        }
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
    }

    private String resolveWorldFilter() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        String dim = player.level().dimension().location().toString();
        if ("minecraft:overworld".equals(dim)) {
            return "world";
        }
        if ("minecraft:the_nether".equals(dim)) {
            return "world_nether";
        }
        if ("minecraft:the_end".equals(dim)) {
            return "world_the_end";
        }
        return null;
    }

    private String buildRenderKey() {
        return mapX + ":" + mapY + ":" + mapW + ":" + mapH + ":" + offsetX + ":" + offsetY + ":" + zoom + ":" +
            minX + ":" + maxX + ":" + minZ + ":" + maxZ + ":" + lastTerritoryRevision + ":" + worldFilter + ":" +
            filterNationId + ":" + territoryAlpha;
    }

    private void rebuildRenderTiles() {
        renderTiles.clear();
        if (territories.isEmpty()) {
            return;
        }
        int spanX = Math.max(1, maxX - minX);
        int spanZ = Math.max(1, maxZ - minZ);
        int padding = 20;
        int usableW = Math.max(1, mapW - padding * 2);
        int usableH = Math.max(1, mapH - padding * 2);
        int baseSize = Math.max(1, Math.min(8, (int) Math.round(Math.min(usableW, usableH) / (double) Math.max(spanX, spanZ))));
        int size = Math.max(1, baseSize * zoom);

        for (TerritorySquare square : territories) {
            double nxNorm = (square.x - minX) / (double) spanX;
            double nzNorm = (square.z - minZ) / (double) spanZ;
            int nx = mapX + padding + (int) (nxNorm * usableW) + offsetX;
            int ny = mapY + padding + (int) (nzNorm * usableH) + offsetY;
            if (nx + size < mapX || ny + size < mapY || nx > mapX + mapW || ny > mapY + mapH) {
                continue;
            }
            if (filterNationId != null && !filterNationId.equals(square.nationId)) {
                continue;
            }
            int baseColor = nationColors.getOrDefault(square.nationId, 0xFF666666);
            int color = (baseColor & 0x00FFFFFF) | (territoryAlpha << 24);
            renderTiles.add(new RenderTile(nx, ny, size, color));
        }
    }

    private static final class TerritorySquare {
        private final String world;
        private final int x;
        private final int z;
        private final String nationId;

        private TerritorySquare(String world, int x, int z, String nationId) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.nationId = nationId;
        }
    }

    private static final class HoveredTerritory {
        private final String world;
        private final int x;
        private final int z;
        private final String nationId;

        private HoveredTerritory(String world, int x, int z, String nationId) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.nationId = nationId;
        }
    }

    private static final class RenderTile {
        private final int x;
        private final int y;
        private final int size;
        private final int color;

        private RenderTile(int x, int y, int size, int color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
        }
    }
}
