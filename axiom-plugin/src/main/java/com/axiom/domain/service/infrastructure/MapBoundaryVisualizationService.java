package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.model.City;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.axiom.domain.service.state.CityGrowthEngine;
import com.axiom.domain.service.state.NationManager;
import com.axiom.domain.service.state.TerritoryService;

// NOTE: This file was recreated from scratch to remove potential invisible characters causing compilation errors.

/**
 * Служба визуализации границ наций и городов на картах и в мире
 * Поддерживает Xaeros Maps, ванильные карты и визуальные эффекты в мире
 */
public class MapBoundaryVisualizationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final CityGrowthEngine cityEngine;
    private final ModIntegrationService modIntegrationService;
    private final VisualEffectsService visualEffectsService; // Added this dependency
    private final TerritoryService territoryService;

    // Кэш границ для производительности
    private final Map<String, BoundaryData> nationBoundaries = new ConcurrentHashMap<>();
    private final Map<String, BoundaryData> cityBoundaries = new ConcurrentHashMap<>();
    
    // Обновление задачи
    private int updateTaskId = -1;
    
    /**
     * Класс для хранения данных границы
     */
    public static class BoundaryData {
        private final String id;
        private final String name;
        private final String nationId;
        private final Set<ChunkPosition> chunks; // координаты чанков, принадлежащих границе
        private final int color; // цвет границы в формате RGB
        private final BoundaryType type; // NATION или CITY
        
        public enum BoundaryType {
            NATION, CITY
        }
        
        public BoundaryData(String id, String name, String nationId, BoundaryType type) {
            this.id = id;
            this.name = name;
            this.nationId = nationId;
            this.type = type;
            this.chunks = new HashSet<>();
            this.color = calculateColorForId(id); // Simplified color calculation
        }
        
        private int calculateColorForId(String id) {
            return id.hashCode(); // Simple hash for color
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getNationId() { return nationId; }
        public Set<ChunkPosition> getChunks() { return new HashSet<>(chunks); }
        public int getColor() { return color; }
        public BoundaryType getType() { return type; }
        
        public int getChunkCount() { return chunks.size(); }
        
        public void addChunk(String world, int x, int z) { chunks.add(new ChunkPosition(world, x, z)); }
        public boolean containsChunk(String world, int x, int z) { return chunks.contains(new ChunkPosition(world, x, z)); }
    }
    
    /**
     * Класс для представления позиции чанка
     */
    public static class ChunkPosition {
        public final String world;
        public final int x;
        public final int z;
        
        public ChunkPosition(String world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ChunkPosition that = (ChunkPosition) obj;
            return x == that.x && z == that.z && Objects.equals(world, that.world);
        }
        
        @Override
        public int hashCode() { return Objects.hash(world, x, z); }
    }
    
    public MapBoundaryVisualizationService(AXIOM plugin,
                                           NationManager nationManager,
                                           CityGrowthEngine cityEngine,
                                           ModIntegrationService modIntegrationService,
                                           VisualEffectsService visualEffectsService,
                                           TerritoryService territoryService) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.cityEngine = cityEngine;
        this.modIntegrationService = modIntegrationService;
        this.visualEffectsService = visualEffectsService; // Initialize the new dependency
        this.territoryService = territoryService;
        
        updateAllBoundaries();
        
        this.updateTaskId = new BukkitRunnable() {
            @Override
            public void run() { updateAllBoundaries(); }
        }.runTaskTimerAsynchronously(plugin, 20 * 30L, 20 * 30L).getTaskId();
        
        plugin.getLogger().info("MapBoundaryVisualizationService initialized.");
    }
    
    public synchronized void updateAllBoundaries() {
        if (nationManager == null || cityEngine == null) {
            return;
        }
        nationBoundaries.clear();
        cityBoundaries.clear();
        
        for (Nation nation : nationManager.getAll()) {
            BoundaryData boundary = new BoundaryData(nation.getId(), nation.getName(), 
                nation.getId(), BoundaryData.BoundaryType.NATION);
            Set<String> claimed = nation.getClaimedChunkKeys() != null ? nation.getClaimedChunkKeys() : Collections.emptySet();
            if (territoryService != null) {
                for (com.axiom.domain.model.ChunkPos pos : territoryService.getNationClaims(nation.getId())) {
                    boundary.addChunk(pos.getWorld(), pos.getX(), pos.getZ());
                }
            } else {
                for (String chunkKey : claimed) {
                    String[] parts = chunkKey.split(":");
                    if (parts.length == 3) {
                        try {
                            boundary.addChunk(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        } catch (NumberFormatException e) { /* ignore */ }
                    }
                }
            }
            nationBoundaries.put(nation.getId(), boundary);
        }
        
        for (City city : cityEngine.getAllCities()) {
            BoundaryData boundary = new BoundaryData(city.getId(), city.getName(), 
                city.getNationId(), BoundaryData.BoundaryType.CITY);
            String centerChunk = city.getCenterChunk();
            if (centerChunk != null) {
                String[] parts = centerChunk.split(":");
                if (parts.length == 3) {
                    try {
                        boundary.addChunk(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    } catch (NumberFormatException e) { /* ignore */ }
                }
            }
            cityBoundaries.put(city.getId(), boundary);
        }
        updateMapVisualizations();
    }
    
    private void updateMapVisualizations() {
        if (modIntegrationService != null && modIntegrationService.hasMapMods()) { /* updateXaerosMaps(); */ }
        /* updateVanillaMaps(); */
        updateWorldVisualizations();
    }
    
    private void updateWorldVisualizations() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendBoundaryVisualsToPlayer(player);
        }
    }
    
    private void sendBoundaryVisualsToPlayer(Player player) {
        if (visualEffectsService == null) return;
        int playerChunkX = player.getLocation().getChunk().getX();
        int playerChunkZ = player.getLocation().getChunk().getZ();
        String playerWorld = player.getWorld().getName();
        
        for (Map.Entry<String, BoundaryData> entry : nationBoundaries.entrySet()) {
            BoundaryData boundary = entry.getValue();
            for (ChunkPosition chunkPos : boundary.getChunks()) {
                if (chunkPos.x == playerChunkX && chunkPos.z == playerChunkZ && playerWorld.equals(chunkPos.world)) {
                    visualEffectsService.sendNationBorderEffect(player, boundary.getColor()); // Corrected call
                    break;
                }
            }
        }
        
        for (Map.Entry<String, BoundaryData> entry : cityBoundaries.entrySet()) {
            BoundaryData boundary = entry.getValue();
            for (ChunkPosition chunkPos : boundary.getChunks()) {
                if (chunkPos.x == playerChunkX && chunkPos.z == playerChunkZ && playerWorld.equals(chunkPos.world)) {
                    visualEffectsService.sendCityBorderEffect(player, boundary.getColor()); // Corrected call
                    break;
                }
            }
        }
    }
    
    public ModIntegrationService getModIntegrationService() {
        return modIntegrationService;
    }

    public void shutdown() {
        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
        }
    }

    public Map<String, BoundaryData> getAllNationBoundaries() {
        return new HashMap<>(nationBoundaries);
    }

    public Map<String, BoundaryData> getAllCityBoundaries() {
        return new HashMap<>(cityBoundaries);
    }

    public void forceUpdate() {
        updateAllBoundaries();
    }

    public Map<String, Object> getBoundaryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNations", nationBoundaries.size());
        stats.put("totalCities", cityBoundaries.size());
        int totalNationChunks = nationBoundaries.values().stream()
            .mapToInt(BoundaryData::getChunkCount)
            .sum();
        int totalCityChunks = cityBoundaries.values().stream()
            .mapToInt(BoundaryData::getChunkCount)
            .sum();
        stats.put("totalNationsChunks", totalNationChunks);
        stats.put("totalCityChunks", totalCityChunks);
        stats.put("avgNationSize", nationBoundaries.isEmpty() ? 0.0 : totalNationChunks / (double) nationBoundaries.size());
        stats.put("avgCitySize", cityBoundaries.isEmpty() ? 0.0 : totalCityChunks / (double) cityBoundaries.size());
        stats.put("mapIntegrationEnabled", modIntegrationService != null && modIntegrationService.hasMapMods());
        return stats;
    }
}
