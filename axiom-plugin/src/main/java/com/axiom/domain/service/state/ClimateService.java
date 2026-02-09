package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Simulates climate changes affecting biomes over time. */
public class ClimateService {
    private final AXIOM plugin;
    private final Random random = new Random();
    private final Map<String, BiomeChange> trackedBiomes = new HashMap<>(); // chunkKey -> change

    public static class BiomeChange {
        Biome originalBiome;
        Biome currentBiome;
        long changeTime;
    }

    public ClimateService(AXIOM plugin) {
        this.plugin = plugin;
        long interval = plugin.getConfig().getLong("climate.changeIntervalHours", 168) * 60L * 60L * 20L; // default 1 week
        if (interval > 0) {
            Bukkit.getScheduler().runTaskTimer(plugin, this::tickClimate, interval, interval);
        }
    }

    private void tickClimate() {
        // Simulate gradual climate change affecting biomes
        Bukkit.getServer().broadcastMessage("§b[AXIOM Climate] §fКлиматические изменения замечены в некоторых регионах.");
    }

    public synchronized boolean isBiomeChanged(String chunkKey) {
        return trackedBiomes.containsKey(chunkKey);
    }

    public synchronized Biome getCurrentBiome(String chunkKey, Biome original) {
        BiomeChange change = trackedBiomes.get(chunkKey);
        return change != null ? change.currentBiome : original;
    }
    
    /**
     * Get comprehensive climate statistics.
     */
    public synchronized Map<String, Object> getClimateStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) {
            stats.put("affectedChunks", 0);
            stats.put("climateStability", 100.0);
            return stats;
        }
        com.axiom.domain.model.Nation n = nationManager.getNationById(nationId);
        if (n == null) {
            stats.put("affectedChunks", 0);
            stats.put("climateStability", 100.0);
            return stats;
        }
        
        // Count affected chunks
        int affectedChunks = 0;
        Set<String> claimedChunks = n.getClaimedChunkKeys();
        if (claimedChunks != null) {
            for (String chunkKey : claimedChunks) {
                if (isBiomeChanged(chunkKey)) {
                    affectedChunks++;
                }
            }
        }
        
        stats.put("affectedChunks", affectedChunks);
        stats.put("totalChunks", claimedChunks != null ? claimedChunks.size() : 0);
        
        // Climate stability rating
        double stability = claimedChunks != null && !claimedChunks.isEmpty() ? 
            Math.max(0, 100.0 - (affectedChunks / (double) claimedChunks.size()) * 100.0) : 100.0;
        stats.put("climateStability", stability);
        
        // Climate rating
        String rating = "СТАБИЛЬНЫЙ";
        if (stability < 70) rating = "ИЗМЕНЯЮЩИЙСЯ";
        if (stability < 50) rating = "НЕСТАБИЛЬНЫЙ";
        if (stability < 30) rating = "КРИТИЧЕСКИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Apply climate change to a chunk.
     */
    public synchronized String applyClimateChange(String chunkKey, Biome newBiome) {
        String[] parts = chunkKey.split(":");
        if (parts.length != 3) return "Некорректный ключ чанка.";
        org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
        if (world == null) return "Мир не найден.";
        int chunkX;
        int chunkZ;
        try {
            chunkX = Integer.parseInt(parts[1]);
            chunkZ = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return "Некорректные координаты чанка.";
        }
        
        BiomeChange change = trackedBiomes.computeIfAbsent(chunkKey, k -> new BiomeChange());
        if (change.originalBiome == null) {
            org.bukkit.Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            change.originalBiome = chunk.getBlock(8, 64, 8).getBiome();
        }
        change.currentBiome = newBiome;
        change.changeTime = System.currentTimeMillis();
        return "Климатическое изменение применено к чанку.";
    }
    
    /**
     * Get global climate statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalClimateStatistics() {
        Map<String, Object> stats = new HashMap<>();
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) {
            stats.put("totalTrackedChanges", trackedBiomes.size());
            stats.put("averageStability", 100.0);
            return stats;
        }
        
        stats.put("totalTrackedChanges", trackedBiomes.size());
        
        int totalAffectedChunks = 0;
        Map<String, Integer> affectedByNation = new HashMap<>();
        Map<String, Double> stabilityByNation = new HashMap<>();
        double totalStability = 0.0;
        int nationsWithChanges = 0;
        
        for (com.axiom.domain.model.Nation n : nationManager.getAll()) {
            String nationId = n.getId();
            int affected = 0;
            
            Set<String> claimedChunks = n.getClaimedChunkKeys();
            if (claimedChunks != null) {
                for (String chunkKey : claimedChunks) {
                    if (isBiomeChanged(chunkKey)) {
                        affected++;
                        totalAffectedChunks++;
                    }
                }
            }
            
            if (affected > 0) {
                nationsWithChanges++;
                affectedByNation.put(nationId, affected);
                
                double stability = claimedChunks != null && !claimedChunks.isEmpty() ? 
                    Math.max(0, 100.0 - (affected / (double) claimedChunks.size()) * 100.0) : 100.0;
                stabilityByNation.put(nationId, stability);
                totalStability += stability;
            } else {
                stabilityByNation.put(nationId, 100.0);
                totalStability += 100.0;
            }
        }
        
        stats.put("totalAffectedChunks", totalAffectedChunks);
        stats.put("affectedByNation", affectedByNation);
        stats.put("stabilityByNation", stabilityByNation);
        stats.put("nationsWithChanges", nationsWithChanges);
        
        // Average climate stability
        stats.put("averageStability", stabilityByNation.size() > 0 ? 
            totalStability / stabilityByNation.size() : 100.0);
        
        // Top nations by affected chunks (most affected)
        List<Map.Entry<String, Integer>> topByAffected = affectedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByAffected", topByAffected);
        
        // Top nations by stability (most stable)
        List<Map.Entry<String, Double>> topByStability = stabilityByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByStability", topByStability);
        
        // Stability distribution
        int stable = 0, changing = 0, unstable = 0, critical = 0;
        for (Double stability : stabilityByNation.values()) {
            if (stability >= 70) stable++;
            else if (stability >= 50) changing++;
            else if (stability >= 30) unstable++;
            else critical++;
        }
        
        Map<String, Integer> stabilityDistribution = new HashMap<>();
        stabilityDistribution.put("stable", stable);
        stabilityDistribution.put("changing", changing);
        stabilityDistribution.put("unstable", unstable);
        stabilityDistribution.put("critical", critical);
        stats.put("stabilityDistribution", stabilityDistribution);
        
        // Average affected chunks per nation
        stats.put("averageAffectedPerNation", affectedByNation.size() > 0 ? 
            (double) totalAffectedChunks / affectedByNation.size() : 0);
        
        return stats;
    }
    
    /**
     * Restore original biome (mitigation efforts).
     */
    public synchronized String restoreBiome(String chunkKey) {
        BiomeChange change = trackedBiomes.remove(chunkKey);
        if (change == null) return "Чанк не был изменён.";
        return "Биом восстановлен.";
    }
    
    /**
     * Calculate climate change impact on economy.
     */
    public synchronized double getClimateEconomicImpact(String nationId) {
        Map<String, Object> stats = getClimateStatistics(nationId);
        double stability = (Double) stats.get("climateStability");
        
        // Economic impact: -1% per 10% stability loss
        return (100.0 - stability) * 0.1;
    }
}

