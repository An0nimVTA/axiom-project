package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Multi-world support service.
 * Handles territories, wars, and resources across different worlds (Overworld, Nether, End, custom).
 */
public class MultiWorldService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    
    // World-specific settings
    private final Map<String, WorldSettings> worldSettings = new HashMap<>(); // worldName -> settings
    
    public static class WorldSettings {
        String worldName;
        String type; // "overworld", "nether", "end", "custom"
        boolean allowClaims; // Can claim chunks in this world
        boolean allowWar; // Can declare war in this world
        boolean allowPortals; // Portal control (for Nether)
        double resourceMultiplier; // Resource bonus/penalty
        String description; // World description
    }
    
    public MultiWorldService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        initializeWorldSettings();
    }
    
    private void initializeWorldSettings() {
        // Overworld (default)
        WorldSettings overworld = new WorldSettings();
        overworld.worldName = "world";
        overworld.type = "overworld";
        overworld.allowClaims = true;
        overworld.allowWar = true;
        overworld.allowPortals = false;
        overworld.resourceMultiplier = 1.0;
        overworld.description = "Основной мир";
        worldSettings.put("world", overworld);
        
        // Nether
        WorldSettings nether = new WorldSettings();
        nether.worldName = "world_nether";
        nether.type = "nether";
        nether.allowClaims = true;
        nether.allowWar = true;
        nether.allowPortals = true; // Portal control is strategic
        nether.resourceMultiplier = 1.5; // More resources in Nether
        nether.description = "Нижний мир (больше ресурсов, контроль порталов)";
        worldSettings.put("world_nether", nether);
        
        // End
        WorldSettings end = new WorldSettings();
        end.worldName = "world_the_end";
        end.type = "end";
        end.allowClaims = true;
        end.allowWar = true;
        end.allowPortals = false;
        end.resourceMultiplier = 2.0; // End resources are rare and valuable
        end.description = "Край (редкие ресурсы, эндер-перлы)";
        worldSettings.put("world_the_end", end);
    }
    
    /**
     * Check if world allows claims.
     */
    public boolean canClaimInWorld(String worldName) {
        if (isBlank(worldName)) return false;
        WorldSettings settings = worldSettings.get(worldName);
        return settings != null && settings.allowClaims;
    }
    
    /**
     * Get resource multiplier for world.
     */
    public double getResourceMultiplier(String worldName) {
        if (isBlank(worldName)) return 1.0;
        WorldSettings settings = worldSettings.get(worldName);
        return settings != null ? settings.resourceMultiplier : 1.0;
    }
    
    /**
     * Check if portal control is strategic (Nether).
     */
    public boolean isPortalStrategic(String worldName) {
        if (isBlank(worldName)) return false;
        WorldSettings settings = worldSettings.get(worldName);
        return settings != null && settings.allowPortals;
    }
    
    /**
     * Get all nations with claims in specific world.
     */
    public List<Nation> getNationsInWorld(String worldName) {
        List<Nation> result = new ArrayList<>();
        if (nationManager == null || isBlank(worldName)) return result;
        for (Nation n : nationManager.getAll()) {
            if (n.getClaimedChunkKeys() == null) continue;
            for (String chunkKey : n.getClaimedChunkKeys()) {
                if (chunkKey.startsWith(worldName + ":")) {
                    result.add(n);
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * Get total chunks claimed in world.
     */
    public int getTotalChunksInWorld(String worldName) {
        int total = 0;
        if (nationManager == null || isBlank(worldName)) return total;
        for (Nation n : nationManager.getAll()) {
            if (n.getClaimedChunkKeys() == null) continue;
            for (String chunkKey : n.getClaimedChunkKeys()) {
                if (chunkKey.startsWith(worldName + ":")) {
                    total++;
                }
            }
        }
        return total;
    }
    
    /**
     * Check if war can be declared in world.
     */
    public boolean canWarInWorld(String worldName) {
        if (isBlank(worldName)) return false;
        WorldSettings settings = worldSettings.get(worldName);
        return settings != null && settings.allowWar;
    }
    
    /**
     * Register custom world settings.
     */
    public void registerWorld(String worldName, String type, boolean allowClaims, boolean allowWar, 
                             double resourceMultiplier) {
        if (isBlank(worldName) || !Double.isFinite(resourceMultiplier)) return;
        WorldSettings settings = new WorldSettings();
        settings.worldName = worldName;
        settings.type = type;
        settings.allowClaims = allowClaims;
        settings.allowWar = allowWar;
        settings.allowPortals = false;
        settings.resourceMultiplier = resourceMultiplier;
        settings.description = "Пользовательский мир";
        worldSettings.put(worldName, settings);
    }
    
    /**
     * Get world settings.
     */
    public WorldSettings getWorldSettings(String worldName) {
        return worldSettings.get(worldName);
    }
    
    /**
     * Get all registered worlds.
     */
    public Set<String> getRegisteredWorlds() {
        return new HashSet<>(worldSettings.keySet());
    }
    
    /**
     * Check if player is in strategic portal location (Nether).
     */
    public boolean isNearPortal(Player player) {
        if (player == null || player.getWorld() == null) return false;
        String worldName = player.getWorld().getName();
        WorldSettings settings = worldSettings.get(worldName);
        if (settings == null || !settings.allowPortals) return false;
        
        // Check if near portal (simplified: check for portal blocks nearby)
        // This would require actual portal detection logic
        return false; // Placeholder
    }
    
    /**
     * Get comprehensive multi-world statistics.
     */
    public synchronized Map<String, Object> getMultiWorldStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null || isBlank(nationId)) {
            stats.put("error", "Нация не найдена.");
            return stats;
        }
        Nation n = nationManager.getNationById(nationId);
        if (n == null) {
            stats.put("error", "Нация не найдена.");
            return stats;
        }
        
        // Territories by world
        Map<String, Integer> chunksByWorld = new HashMap<>();
        if (n.getClaimedChunkKeys() != null) {
            for (String chunkKey : n.getClaimedChunkKeys()) {
                String[] parts = chunkKey.split(":");
                if (parts.length >= 1) {
                    String world = parts[0];
                    chunksByWorld.put(world, chunksByWorld.getOrDefault(world, 0) + 1);
                }
            }
        }
        stats.put("chunksByWorld", chunksByWorld);
        stats.put("worldsClaimed", chunksByWorld.size());
        
        // World details
        List<Map<String, Object>> worldsList = new ArrayList<>();
        for (String worldName : chunksByWorld.keySet()) {
            WorldSettings settings = worldSettings.get(worldName);
            Map<String, Object> worldData = new HashMap<>();
            worldData.put("worldName", worldName);
            worldData.put("chunks", chunksByWorld.get(worldName));
            if (settings != null) {
                worldData.put("type", settings.type);
                worldData.put("allowClaims", settings.allowClaims);
                worldData.put("allowWar", settings.allowWar);
                worldData.put("resourceMultiplier", settings.resourceMultiplier);
                worldData.put("description", settings.description);
            }
            worldsList.add(worldData);
        }
        stats.put("worldsList", worldsList);
        
        // Calculate world bonuses
        double totalResourceBonus = 1.0;
        for (String worldName : chunksByWorld.keySet()) {
            WorldSettings settings = worldSettings.get(worldName);
            if (settings != null) {
                totalResourceBonus *= settings.resourceMultiplier;
            }
        }
        stats.put("totalResourceBonus", totalResourceBonus);
        
        // Multi-world rating
        String rating = "ОДНОМИРОВАЯ";
        if (chunksByWorld.size() >= 5) rating = "МНОГОМИРОВАЯ ИМПЕРИЯ";
        else if (chunksByWorld.size() >= 3) rating = "МНОГОМИРОВАЯ";
        else if (chunksByWorld.size() >= 2) rating = "РАСШИРЕННАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get world settings for nation's worlds.
     */
    public synchronized Map<String, WorldSettings> getNationWorldSettings(String nationId) {
        Map<String, WorldSettings> result = new HashMap<>();
        if (nationManager == null || isBlank(nationId)) return result;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return result;
        
        Set<String> worlds = new HashSet<>();
        if (n.getClaimedChunkKeys() != null) {
            for (String chunkKey : n.getClaimedChunkKeys()) {
                String[] parts = chunkKey.split(":");
                if (parts.length >= 1) {
                    worlds.add(parts[0]);
                }
            }
        }
        
        for (String worldName : worlds) {
            WorldSettings settings = worldSettings.get(worldName);
            if (settings != null) {
                result.put(worldName, settings);
            }
        }
        
        return result;
    }
    
    /**
     * Calculate total resource bonus from all worlds.
     */
    public synchronized double getTotalResourceBonus(String nationId) {
        double bonus = 1.0;
        if (nationManager == null || isBlank(nationId)) return bonus;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return bonus;
        
        Set<String> worlds = new HashSet<>();
        if (n.getClaimedChunkKeys() != null) {
            for (String chunkKey : n.getClaimedChunkKeys()) {
                String[] parts = chunkKey.split(":");
                if (parts.length >= 1) {
                    worlds.add(parts[0]);
                }
            }
        }
        
        for (String worldName : worlds) {
            WorldSettings settings = worldSettings.get(worldName);
            if (settings != null) {
                bonus *= settings.resourceMultiplier;
            }
        }
        
        // Cap at +200%
        return Math.min(3.0, bonus);
    }
    
    /**
     * Get world distribution statistics.
     */
    public synchronized Map<String, Integer> getWorldDistribution(String nationId) {
        Map<String, Integer> distribution = new HashMap<>();
        if (nationManager == null || isBlank(nationId)) return distribution;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return distribution;
        
        if (n.getClaimedChunkKeys() != null) {
            for (String chunkKey : n.getClaimedChunkKeys()) {
                String[] parts = chunkKey.split(":");
                if (parts.length >= 1) {
                    String world = parts[0];
                    distribution.put(world, distribution.getOrDefault(world, 0) + 1);
                }
            }
        }
        
        return distribution;
    }
    
    /**
     * Get global multi-world statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalMultiWorldStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRegisteredWorlds", worldSettings.size());
        
        // World type distribution
        Map<String, Integer> worldsByType = new HashMap<>();
        int worldsWithClaims = 0;
        int worldsWithWar = 0;
        Map<String, Integer> chunksByWorld = new HashMap<>();
        Map<String, Integer> nationsByWorld = new HashMap<>();
        Map<String, Double> resourceMultipliersByWorld = new HashMap<>();
        
        for (WorldSettings settings : worldSettings.values()) {
            worldsByType.put(settings.type, worldsByType.getOrDefault(settings.type, 0) + 1);
            if (settings.allowClaims) worldsWithClaims++;
            if (settings.allowWar) worldsWithWar++;
            resourceMultipliersByWorld.put(settings.worldName, settings.resourceMultiplier);
        }
        
        stats.put("worldsByType", worldsByType);
        stats.put("worldsWithClaims", worldsWithClaims);
        stats.put("worldsWithWar", worldsWithWar);
        
        // Calculate chunks and nations by world
        if (nationManager == null) {
            stats.put("worldsByType", worldsByType);
            stats.put("worldsWithClaims", worldsWithClaims);
            stats.put("worldsWithWar", worldsWithWar);
            stats.put("chunksByWorld", chunksByWorld);
            stats.put("nationsByWorld", nationsByWorld);
            stats.put("resourceMultipliersByWorld", resourceMultipliersByWorld);
            stats.put("totalChunks", 0);
            stats.put("averageChunksPerWorld", 0);
            stats.put("averageNationsPerWorld", 0);
            stats.put("topByChunks", new ArrayList<>());
            stats.put("topByNations", new ArrayList<>());
            stats.put("worldUtilizationRate", 0);
            stats.put("multiWorldNations", 0);
            stats.put("averageWorldsPerNation", 0);
            return stats;
        }
        for (Nation n : nationManager.getAll()) {
            Set<String> worlds = new HashSet<>();
            if (n.getClaimedChunkKeys() != null) {
                for (String chunkKey : n.getClaimedChunkKeys()) {
                    String[] parts = chunkKey.split(":");
                    if (parts.length >= 1) {
                        String world = parts[0];
                        chunksByWorld.put(world, chunksByWorld.getOrDefault(world, 0) + 1);
                        worlds.add(world);
                    }
                }
            }
            for (String world : worlds) {
                nationsByWorld.put(world, nationsByWorld.getOrDefault(world, 0) + 1);
            }
        }
        
        stats.put("chunksByWorld", chunksByWorld);
        stats.put("nationsByWorld", nationsByWorld);
        stats.put("resourceMultipliersByWorld", resourceMultipliersByWorld);
        
        // Total chunks across all worlds
        int totalChunks = chunksByWorld.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("totalChunks", totalChunks);
        
        // Average chunks per world
        stats.put("averageChunksPerWorld", chunksByWorld.size() > 0 ? 
            (double) totalChunks / chunksByWorld.size() : 0);
        
        // Average nations per world
        int totalNations = nationsByWorld.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageNationsPerWorld", nationsByWorld.size() > 0 ? 
            (double) totalNations / nationsByWorld.size() : 0);
        
        // Top worlds by chunks
        List<Map.Entry<String, Integer>> topByChunks = chunksByWorld.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByChunks", topByChunks);
        
        // Top worlds by nations
        List<Map.Entry<String, Integer>> topByNations = nationsByWorld.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByNations", topByNations);
        
        // World utilization rate (worlds with claims / total worlds)
        stats.put("worldUtilizationRate", worldSettings.size() > 0 ? 
            (double) chunksByWorld.size() / worldSettings.size() : 0);
        
        // Nations with multi-world presence
        int multiWorldNations = 0;
        for (Nation n : nationManager.getAll()) {
            Set<String> worlds = new HashSet<>();
            if (n.getClaimedChunkKeys() != null) {
                for (String chunkKey : n.getClaimedChunkKeys()) {
                    String[] parts = chunkKey.split(":");
                    if (parts.length >= 1) {
                        worlds.add(parts[0]);
                    }
                }
            }
            if (worlds.size() > 1) {
                multiWorldNations++;
            }
        }
        stats.put("multiWorldNations", multiWorldNations);
        
        // Average worlds per nation
        int totalWorlds = 0;
        for (Nation n : nationManager.getAll()) {
            Set<String> worlds = new HashSet<>();
            if (n.getClaimedChunkKeys() != null) {
                for (String chunkKey : n.getClaimedChunkKeys()) {
                    String[] parts = chunkKey.split(":");
                    if (parts.length >= 1) {
                        worlds.add(parts[0]);
                    }
                }
            }
            totalWorlds += worlds.size();
        }
        stats.put("averageWorldsPerNation", nationManager.getAll().size() > 0 ? 
            (double) totalWorlds / nationManager.getAll().size() : 0);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

