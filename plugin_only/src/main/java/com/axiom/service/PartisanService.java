package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manages partisan warfare in occupied territories. */
public class PartisanService {
    private final AXIOM plugin;
    private final File partisansDir;
    private final Map<String, PartisanGroup> groups = new HashMap<>(); // territoryKey -> group

    public static class PartisanGroup {
        String territoryKey;
        String originalNationId;
        String occupierNationId;
        double activityLevel; // 0-100%
        long establishedAt;
    }

    public PartisanService(AXIOM plugin) {
        this.plugin = plugin;
        this.partisansDir = new File(plugin.getDataFolder(), "partisans");
        this.partisansDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::partisanActions, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized void establishPartisans(String territoryKey, String originalNationId, String occupierNationId) {
        PartisanGroup pg = new PartisanGroup();
        pg.territoryKey = territoryKey;
        pg.originalNationId = originalNationId;
        pg.occupierNationId = occupierNationId;
        pg.activityLevel = 20.0;
        pg.establishedAt = System.currentTimeMillis();
        groups.put(territoryKey, pg);
        saveGroup(pg);
        Nation occupier = plugin.getNationManager().getNationById(occupierNationId);
        if (occupier != null) {
            occupier.getHistory().add("Партизанская активность в " + territoryKey);
        }
    }

    private void partisanActions() {
        for (PartisanGroup pg : groups.values()) {
            if (pg.activityLevel < 100 && Math.random() < 0.1) {
                pg.activityLevel = Math.min(100, pg.activityLevel + 5);
                if (pg.activityLevel >= 80) {
                    sabotageOccupier(pg);
                }
            }
        }
    }

    private void sabotageOccupier(PartisanGroup pg) {
        Nation occupier = plugin.getNationManager().getNationById(pg.occupierNationId);
        if (occupier == null) return;
        // Sabotage effects
        plugin.getNationModifierService().addModifier(pg.occupierNationId, "economy", "penalty", 0.1, 720);
        occupier.getHistory().add("Партизанский саботаж в " + pg.territoryKey);
        try { plugin.getNationManager().save(occupier); } catch (Exception ignored) {}
    }

    private void loadAll() {
        File[] files = partisansDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                PartisanGroup pg = new PartisanGroup();
                pg.territoryKey = o.get("territoryKey").getAsString();
                pg.originalNationId = o.get("originalNationId").getAsString();
                pg.occupierNationId = o.get("occupierNationId").getAsString();
                pg.activityLevel = o.get("activityLevel").getAsDouble();
                pg.establishedAt = o.get("establishedAt").getAsLong();
                groups.put(pg.territoryKey, pg);
            } catch (Exception ignored) {}
        }
    }

    private void saveGroup(PartisanGroup pg) {
        File f = new File(partisansDir, pg.territoryKey.replace(":", "_") + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("territoryKey", pg.territoryKey);
        o.addProperty("originalNationId", pg.originalNationId);
        o.addProperty("occupierNationId", pg.occupierNationId);
        o.addProperty("activityLevel", pg.activityLevel);
        o.addProperty("establishedAt", pg.establishedAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive partisan statistics.
     */
    public synchronized Map<String, Object> getPartisanStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Partisan groups in territories occupied by this nation
        List<PartisanGroup> occupyingGroups = new ArrayList<>();
        // Partisan groups created by this nation
        List<PartisanGroup> createdGroups = new ArrayList<>();
        
        for (PartisanGroup pg : groups.values()) {
            if (pg.occupierNationId.equals(nationId)) {
                occupyingGroups.add(pg);
            }
            if (pg.originalNationId.equals(nationId)) {
                createdGroups.add(pg);
            }
        }
        
        stats.put("occupyingTerritories", occupyingGroups.size());
        stats.put("createdGroups", createdGroups.size());
        
        // Group details
        List<Map<String, Object>> groupsList = new ArrayList<>();
        for (PartisanGroup pg : occupyingGroups) {
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("territoryKey", pg.territoryKey);
            groupData.put("originalNation", pg.originalNationId);
            groupData.put("activityLevel", pg.activityLevel);
            groupData.put("establishedAt", pg.establishedAt);
            groupData.put("age", (System.currentTimeMillis() - pg.establishedAt) / 1000 / 60 / 60 / 24); // days
            groupData.put("isDangerous", pg.activityLevel >= 80);
            groupsList.add(groupData);
        }
        stats.put("groupsList", groupsList);
        
        // Average activity level
        double avgActivity = occupyingGroups.stream()
            .mapToDouble(pg -> pg.activityLevel)
            .average()
            .orElse(0.0);
        stats.put("averageActivityLevel", avgActivity);
        
        // Partisan rating
        String rating = "НЕТ УГРОЗ";
        if (occupyingGroups.size() >= 10) rating = "КРИТИЧЕСКАЯ СИТУАЦИЯ";
        else if (occupyingGroups.size() >= 5) rating = "СЕРЬЁЗНАЯ УГРОЗА";
        else if (occupyingGroups.size() >= 3) rating = "УГРОЗА";
        else if (occupyingGroups.size() >= 1) rating = "НИЗКАЯ УГРОЗА";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Suppress partisan group.
     */
    public synchronized String suppressPartisans(String territoryKey, String nationId, double cost) throws IOException {
        PartisanGroup pg = groups.get(territoryKey);
        if (pg == null) return "Партизанская группа не найдена.";
        if (!pg.occupierNationId.equals(nationId)) return "Это не ваша территория.";
        
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        pg.activityLevel = Math.max(0, pg.activityLevel - 30);
        n.setTreasury(n.getTreasury() - cost);
        
        if (pg.activityLevel <= 0) {
            groups.remove(territoryKey);
            File f = new File(partisansDir, territoryKey.replace(":", "_") + ".json");
            if (f.exists()) f.delete();
        } else {
            saveGroup(pg);
        }
        
        plugin.getNationManager().save(n);
        
        return "Партизаны подавлены. Уровень активности: " + String.format("%.1f", pg.activityLevel) + "%";
    }
    
    /**
     * Get all groups in occupied territories.
     */
    public synchronized List<PartisanGroup> getOccupyingGroups(String nationId) {
        return groups.values().stream()
            .filter(pg -> pg.occupierNationId.equals(nationId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate partisan threat level.
     */
    public synchronized double getThreatLevel(String nationId) {
        double totalThreat = 0.0;
        for (PartisanGroup pg : groups.values()) {
            if (pg.occupierNationId.equals(nationId)) {
                totalThreat += pg.activityLevel / 100.0; // 0-1 per group
            }
        }
        return Math.min(1.0, totalThreat / 5.0); // Normalized to 0-1
    }
    
    /**
     * Get global partisan statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPartisanStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalGroups", groups.size());
        
        Map<String, Integer> groupsByOccupier = new HashMap<>();
        Map<String, Integer> groupsByOriginal = new HashMap<>();
        double totalActivityLevel = 0.0;
        Map<String, Double> averageActivityByNation = new HashMap<>();
        
        for (PartisanGroup pg : groups.values()) {
            groupsByOccupier.put(pg.occupierNationId, groupsByOccupier.getOrDefault(pg.occupierNationId, 0) + 1);
            groupsByOriginal.put(pg.originalNationId, groupsByOriginal.getOrDefault(pg.originalNationId, 0) + 1);
            totalActivityLevel += pg.activityLevel;
            
            // Track average activity per occupier nation
            averageActivityByNation.put(pg.occupierNationId,
                (averageActivityByNation.getOrDefault(pg.occupierNationId, 0.0) + pg.activityLevel) / 2.0);
        }
        
        stats.put("groupsByOccupier", groupsByOccupier);
        stats.put("groupsByOriginal", groupsByOriginal);
        stats.put("averageActivityLevel", groups.size() > 0 ? totalActivityLevel / groups.size() : 0);
        stats.put("nationsWithOccupiedTerritories", groupsByOccupier.size());
        stats.put("nationsWithPartisanGroups", groupsByOriginal.size());
        
        // Top nations by occupied territories (most threatened)
        List<Map.Entry<String, Integer>> topByOccupied = groupsByOccupier.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByOccupied", topByOccupied);
        
        // Top nations by partisan groups created
        List<Map.Entry<String, Integer>> topByCreated = groupsByOriginal.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCreated", topByCreated);
        
        // Top nations by average activity (most dangerous)
        List<Map.Entry<String, Double>> topByActivity = averageActivityByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByActivity", topByActivity);
        
        // Threat level distribution
        Map<String, Integer> threatDistribution = new HashMap<>();
        for (String nationId : groupsByOccupier.keySet()) {
            double threat = getThreatLevel(nationId);
            if (threat >= 0.8) {
                threatDistribution.put("critical", threatDistribution.getOrDefault("critical", 0) + 1);
            } else if (threat >= 0.5) {
                threatDistribution.put("high", threatDistribution.getOrDefault("high", 0) + 1);
            } else if (threat >= 0.2) {
                threatDistribution.put("moderate", threatDistribution.getOrDefault("moderate", 0) + 1);
            } else {
                threatDistribution.put("low", threatDistribution.getOrDefault("low", 0) + 1);
            }
        }
        stats.put("threatDistribution", threatDistribution);
        
        // Average groups per occupier
        stats.put("averageGroupsPerOccupier", groupsByOccupier.size() > 0 ? 
            (double) groups.size() / groupsByOccupier.size() : 0);
        
        // Average group age
        long now = System.currentTimeMillis();
        double totalAge = 0.0;
        for (PartisanGroup pg : groups.values()) {
            totalAge += (now - pg.establishedAt) / 1000.0 / 60.0 / 60.0 / 24.0; // days
        }
        stats.put("averageGroupAgeDays", groups.size() > 0 ? totalAge / groups.size() : 0);
        
        return stats;
    }
}

