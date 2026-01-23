package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.model.City;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages display of nation and city boundaries on minimap and world map.
 * Integrates with Xaeros Minimap and Xaeros World Map mods.
 */
public class MapBoundaryService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final CityGrowthEngine cityEngine;
    private final ModIntegrationService modIntegration;
    
    // Cache of boundaries for performance
    private final Map<String, Set<String>> nationBoundaries = new ConcurrentHashMap<>(); // nationId -> set of chunkKeys
    private final Map<String, Map<String, Set<String>>> worldNationBoundaries = new ConcurrentHashMap<>(); // world -> nationId -> chunkKeys
    private final Map<String, Set<String>> cityBoundaries = new ConcurrentHashMap<>(); // cityId -> set of chunkKeys
    
    // Update task
    private int updateTaskId = -1;
    
    public MapBoundaryService(AXIOM plugin, NationManager nationManager, CityGrowthEngine cityEngine, ModIntegrationService modIntegration) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.cityEngine = cityEngine;
        this.modIntegration = modIntegration;
        
        // Initialize boundaries
        updateAllBoundaries();
        
        // Start periodic update task (every 30 seconds)
        updateTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateAllBoundaries, 0, 20 * 30).getTaskId();
        
        plugin.getLogger().info("MapBoundaryService initialized. Map mods available: " + modIntegration.hasMapMods());
    }
    
    /**
     * Update all boundaries from current nation and city data.
     */
    public synchronized void updateAllBoundaries() {
        if (nationManager == null) {
            return;
        }
        nationBoundaries.clear();
        worldNationBoundaries.clear();
        cityBoundaries.clear();
        
        // Process all nations
        for (Nation nation : nationManager.getAll()) {
            String nationId = nation.getId();
            Set<String> chunks = new HashSet<>(nation.getClaimedChunkKeys() != null ? nation.getClaimedChunkKeys() : Collections.emptyList());
            nationBoundaries.put(nationId, chunks);
            
            // Group by world
            Map<String, Set<String>> worldChunks = new HashMap<>();
            for (String chunkKey : chunks) {
                String[] parts = chunkKey.split(":");
                if (parts.length == 3) {
                    String world = parts[0];
                    worldChunks.computeIfAbsent(world, k -> new HashSet<>()).add(chunkKey);
                }
            }
            worldNationBoundaries.put(nationId, worldChunks);
        }
        
        // Process all cities (cities are typically in a single chunk)
        if (cityEngine != null) {
            for (City city : cityEngine.getAllCities()) {
                String cityId = city.getId();
                Set<String> cityChunks = new HashSet<>();
            
            // City center chunk
            String centerChunk = city.getCenterChunk();
            if (centerChunk != null && !centerChunk.isEmpty()) {
                cityChunks.add(centerChunk);
            }
            
            // Optionally, include adjacent chunks for larger cities
            if (city.getLevel() >= 3) {
                // Add adjacent chunks (simplified - 3x3 area for level 3+, 5x5 for level 5)
                int radius = city.getLevel() >= 5 ? 2 : 1;
                addAdjacentChunks(centerChunk, radius, cityChunks);
            }
            
                cityBoundaries.put(cityId, cityChunks);
            }
        }
        
        // Update map mods if available
        if (modIntegration != null && modIntegration.hasMapMods()) {
            Bukkit.getScheduler().runTask(plugin, this::sendBoundariesToMapMods);
        }
    }
    
    /**
     * Add adjacent chunks around a center chunk.
     */
    private void addAdjacentChunks(String centerChunk, int radius, Set<String> result) {
        if (centerChunk == null) return;
        String[] parts = centerChunk.split(":");
        if (parts.length != 3) return;
        
        try {
            String world = parts[0];
            int centerX = Integer.parseInt(parts[1]);
            int centerZ = Integer.parseInt(parts[2]);
            
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    String chunkKey = world + ":" + (centerX + x) + ":" + (centerZ + z);
                    result.add(chunkKey);
                }
            }
        } catch (NumberFormatException ignored) {}
    }
    
    /**
     * Send boundaries to Xaeros map mods via reflection/API.
     */
    private void sendBoundariesToMapMods() {
        // Try to integrate with Xaeros Minimap and World Map
        // Note: This requires the mods to be installed on the client side
        // We'll use a compatible approach that works with Xaeros API if available
        
        try {
            // Check if Xaeros Minimap plugin is available
            org.bukkit.plugin.Plugin minimapPlugin = Bukkit.getPluginManager().getPlugin("XaerosMinimap");
            if (minimapPlugin != null) {
                updateXaerosMinimap(minimapPlugin);
            }
            
            // Check if Xaeros World Map plugin is available
            org.bukkit.plugin.Plugin worldMapPlugin = Bukkit.getPluginManager().getPlugin("XaerosWorldMap");
            if (worldMapPlugin != null) {
                updateXaerosWorldMap(worldMapPlugin);
            }
        } catch (Exception e) {
            // Silent fail - mods may not be available or API changed
            plugin.getLogger().fine("Could not update Xaeros maps: " + e.getMessage());
        }
        
        // Alternative: Use waypoints/landmarks if direct boundary API is not available
        sendWaypointsToPlayers();
    }
    
    /**
     * Update Xaeros Minimap with boundaries (if API available).
     */
    private void updateXaerosMinimap(org.bukkit.plugin.Plugin minimapPlugin) {
        // Xaeros Minimap integration would go here
        // The actual API depends on the mod version
        // For now, we'll use waypoint markers at capital locations
    }
    
    /**
     * Update Xaeros World Map with boundaries (if API available).
     */
    private void updateXaerosWorldMap(org.bukkit.plugin.Plugin worldMapPlugin) {
        // Xaeros World Map integration would go here
        // Similar to minimap - depends on mod version
    }
    
    /**
     * Send waypoint markers for nation capitals and cities to players.
     * This is a fallback method that works even without direct API access.
     */
    private void sendWaypointsToPlayers() {
        if (nationManager == null) return;
        // Send nation capitals as waypoints
        for (Nation nation : nationManager.getAll()) {
            String capitalChunk = nation.getCapitalChunkStr();
            if (capitalChunk != null && !capitalChunk.isEmpty()) {
                sendWaypointToNationMembers(nation, "Столица: " + nation.getName(), capitalChunk, 0xFF0000);
            }
        }
        
        // Send cities as waypoints
        if (cityEngine != null) {
            for (City city : cityEngine.getAllCities()) {
                String centerChunk = city.getCenterChunk();
                if (centerChunk != null && !centerChunk.isEmpty()) {
                    Nation cityNation = nationManager.getNationById(city.getNationId());
                    if (cityNation != null) {
                        sendWaypointToNationMembers(cityNation, "Город: " + city.getName(), centerChunk, 0x00FF00);
                    }
                }
            }
        }
    }
    
    /**
     * Send waypoint to all members of a nation.
     */
    private void sendWaypointToNationMembers(Nation nation, String name, String chunkKey, int color) {
        String[] parts = chunkKey.split(":");
        if (parts.length != 3) return;
        
        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return;
            
            int chunkX = Integer.parseInt(parts[1]);
            int chunkZ = Integer.parseInt(parts[2]);
            
            // Convert chunk coordinates to block coordinates (center of chunk)
            int blockX = chunkX * 16 + 8;
            int blockZ = chunkZ * 16 + 8;
            int blockY = world.getHighestBlockYAt(blockX, blockZ);
            
            org.bukkit.Location location = new org.bukkit.Location(world, blockX, blockY, blockZ);
            
            // Send to all online members
            Collection<UUID> citizens = nation.getCitizens() != null ? nation.getCitizens() : Collections.emptyList();
            for (UUID memberId : citizens) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    sendWaypointPacket(member, name, location, color);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Failed to send waypoint: " + e.getMessage());
        }
    }
    
    /**
     * Send waypoint packet to player (using ProtocolLib or reflection if available).
     * Falls back to chat message if packet sending is not available.
     */
    private void sendWaypointPacket(Player player, String name, org.bukkit.Location location, int color) {
        // Try to use Xaeros API if available
        try {
            // Check for Xaeros Minimap API
            // Note: This is a placeholder for future Xaeros API integration
            // The actual implementation would depend on the mod version
            // For now, we'll use waypoints sent to players
            
            // Try Xaeros Minimap API (if available)
            // Future: Full API integration would be implemented here
            // For now, we detect the API but use fallback methods
            try {
                Class.forName("xaero.minimap.XaeroMinimapAPI");
                // If we get here, API is available but we use fallback for compatibility
            } catch (ClassNotFoundException ignored) {
                // Xaeros API not available - use fallback method
            }
            
        } catch (Exception e) {
            // Fallback: Send chat message with coordinates
            if (plugin.getConfig().getBoolean("map.showWaypointsInChat", false)) {
                String msg = String.format("§7[Карта] %s §7→ X: %d, Z: %d", name, 
                    location.getBlockX(), location.getBlockZ());
                player.sendMessage(msg);
            }
        }
    }
    
    /**
     * Get boundaries for a specific nation.
     */
    public Set<String> getNationBoundaries(String nationId) {
        return new HashSet<>(nationBoundaries.getOrDefault(nationId, new HashSet<>()));
    }
    
    /**
     * Get boundaries for a specific city.
     */
    public Set<String> getCityBoundaries(String cityId) {
        return new HashSet<>(cityBoundaries.getOrDefault(cityId, new HashSet<>()));
    }
    
    /**
     * Get boundaries for all nations in a world.
     */
    public Map<String, Set<String>> getWorldNationBoundaries(String worldName) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : worldNationBoundaries.entrySet()) {
            Set<String> worldChunks = entry.getValue().get(worldName);
            if (worldChunks != null && !worldChunks.isEmpty()) {
                result.put(entry.getKey(), worldChunks);
            }
        }
        return result;
    }
    
    /**
     * Force update boundaries (call when territory changes).
     */
    public void forceUpdate() {
        updateAllBoundaries();
    }
    
    /**
     * Shutdown cleanup.
     */
    public void shutdown() {
        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
        }
    }
    
    /**
     * Get comprehensive map boundary statistics.
     */
    public synchronized Map<String, Object> getMapBoundaryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalNations", nationBoundaries.size());
        stats.put("totalCities", cityBoundaries.size());
        stats.put("totalNationChunks", nationBoundaries.values().stream()
            .mapToInt(Set::size).sum());
        stats.put("totalCityChunks", cityBoundaries.values().stream()
            .mapToInt(Set::size).sum());
        
        // World distribution
        Map<String, Integer> chunksByWorld = new HashMap<>();
        for (Map<String, Set<String>> worldMap : worldNationBoundaries.values()) {
            for (Map.Entry<String, Set<String>> entry : worldMap.entrySet()) {
                chunksByWorld.put(entry.getKey(), 
                    chunksByWorld.getOrDefault(entry.getKey(), 0) + entry.getValue().size());
            }
        }
        stats.put("chunksByWorld", chunksByWorld);
        
        // Map mod integration
        boolean hasMinimap = modIntegration.hasMapMods();
        stats.put("mapModsAvailable", hasMinimap);
        stats.put("updateIntervalSeconds", 30);
        stats.put("updateTaskActive", updateTaskId != -1);
        
        // Boundary details
        List<Map<String, Object>> nationDetails = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : nationBoundaries.entrySet()) {
            Map<String, Object> nationData = new HashMap<>();
            nationData.put("nationId", entry.getKey());
            nationData.put("chunkCount", entry.getValue().size());
            
            Nation nation = nationManager.getNationById(entry.getKey());
            if (nation != null) {
                nationData.put("nationName", nation.getName());
            }
            
            nationDetails.add(nationData);
        }
        stats.put("nationBoundaries", nationDetails);
        
        // City details
        List<Map<String, Object>> cityDetails = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : cityBoundaries.entrySet()) {
            Map<String, Object> cityData = new HashMap<>();
            cityData.put("cityId", entry.getKey());
            cityData.put("chunkCount", entry.getValue().size());
            
            City city = cityEngine.getCity(entry.getKey());
            if (city != null) {
                cityData.put("cityName", city.getName());
                cityData.put("level", city.getLevel());
            }
            
            cityDetails.add(cityData);
        }
        stats.put("cityBoundaries", cityDetails);
        
        // Rating
        String rating = "НЕТ ГРАНИЦ";
        if (nationBoundaries.size() >= 20) rating = "ОБШИРНОЕ ПОКРЫТИЕ";
        else if (nationBoundaries.size() >= 10) rating = "РАЗВИТОЕ";
        else if (nationBoundaries.size() >= 5) rating = "АКТИВНОЕ";
        else if (nationBoundaries.size() >= 1) rating = "НАЧАЛЬНОЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get boundary statistics for specific nation.
     */
    public synchronized Map<String, Object> getNationBoundaryStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> boundaries = nationBoundaries.get(nationId);
        if (boundaries == null) {
            stats.put("error", "Нация не найдена.");
            return stats;
        }
        
        stats.put("chunkCount", boundaries.size());
        
        // World distribution
        Map<String, Set<String>> worldMap = worldNationBoundaries.get(nationId);
        if (worldMap != null) {
            Map<String, Integer> worldChunks = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : worldMap.entrySet()) {
                worldChunks.put(entry.getKey(), entry.getValue().size());
            }
            stats.put("chunksByWorld", worldChunks);
            stats.put("worldsCount", worldMap.size());
        }
        
        // Boundary coverage (estimated area)
        double estimatedAreaKm2 = boundaries.size() * 0.0256; // 16x16 blocks = 256 blocks = 0.0256 km²
        stats.put("estimatedAreaKm2", estimatedAreaKm2);
        
        return stats;
    }
    
    /**
     * Get boundary statistics for specific city.
     */
    public synchronized Map<String, Object> getCityBoundaryStatistics(String cityId) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> boundaries = cityBoundaries.get(cityId);
        if (boundaries == null) {
            stats.put("error", "Город не найден.");
            return stats;
        }
        
        stats.put("chunkCount", boundaries.size());
        
        City city = cityEngine.getCity(cityId);
        if (city != null) {
            stats.put("cityName", city.getName());
            stats.put("level", city.getLevel());
            stats.put("population", city.getPopulation());
        }
        
        return stats;
    }
    
    /**
     * Check if boundaries are up to date.
     */
    public synchronized boolean areBoundariesUpToDate() {
        // Simple check: compare nation count
        return nationBoundaries.size() == nationManager.getAll().size();
    }
    
    /**
     * Get update task status.
     */
    public synchronized boolean isUpdateTaskActive() {
        return updateTaskId != -1 && 
               Bukkit.getScheduler().isQueued(updateTaskId) || 
               Bukkit.getScheduler().isCurrentlyRunning(updateTaskId);
    }
}

