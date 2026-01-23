package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages infrastructure maintenance and decay. */
public class InfrastructureMaintenanceService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File maintenanceDir;
    private final Map<String, InfrastructureData> nationInfrastructure = new HashMap<>(); // nationId -> data

    public static class InfrastructureData {
        double roadCondition; // 0-100%
        double buildingCondition; // 0-100%
        double lastMaintenance;
        double maintenanceBudget;
    }

    public InfrastructureMaintenanceService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.maintenanceDir = new File(plugin.getDataFolder(), "maintenance");
        this.maintenanceDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processMaintenance, 0, 20 * 60 * 15); // every 15 minutes
    }

    public synchronized String allocateMaintenance(String nationId, double budget) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < budget) return "Недостаточно средств.";
        InfrastructureData data = nationInfrastructure.computeIfAbsent(nationId, k -> {
            InfrastructureData d = new InfrastructureData();
            d.roadCondition = 80.0;
            d.buildingCondition = 80.0;
            d.lastMaintenance = System.currentTimeMillis();
            return d;
        });
        data.maintenanceBudget = budget;
        n.setTreasury(n.getTreasury() - budget);
        try {
            nationManager.save(n);
            saveInfrastructure(nationId, data);
        } catch (Exception ignored) {}
        return "Бюджет обслуживания выделен: " + budget;
    }

    private void processMaintenance() {
        for (Map.Entry<String, InfrastructureData> e : nationInfrastructure.entrySet()) {
            InfrastructureData data = e.getValue();
            // Infrastructure decays over time
            data.roadCondition = Math.max(0, data.roadCondition - 0.5);
            data.buildingCondition = Math.max(0, data.buildingCondition - 0.3);
            // Maintenance improves condition
            if (data.maintenanceBudget > 0) {
                double improvement = data.maintenanceBudget * 0.01;
                data.roadCondition = Math.min(100, data.roadCondition + improvement);
                data.buildingCondition = Math.min(100, data.buildingCondition + improvement);
                data.maintenanceBudget = Math.max(0, data.maintenanceBudget - 100); // Budget consumed
            }
            // Penalties for poor infrastructure
            if (data.roadCondition < 30 || data.buildingCondition < 30) {
                plugin.getHappinessService().modifyHappiness(e.getKey(), -5.0);
            }
            saveInfrastructure(e.getKey(), data);
        }
    }

    private void loadAll() {
        File[] files = maintenanceDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                InfrastructureData data = new InfrastructureData();
                data.roadCondition = o.get("roadCondition").getAsDouble();
                data.buildingCondition = o.get("buildingCondition").getAsDouble();
                data.lastMaintenance = o.has("lastMaintenance") ? o.get("lastMaintenance").getAsLong() : System.currentTimeMillis();
                data.maintenanceBudget = o.has("maintenanceBudget") ? o.get("maintenanceBudget").getAsDouble() : 0.0;
                nationInfrastructure.put(nationId, data);
            } catch (Exception ignored) {}
        }
    }

    private void saveInfrastructure(String nationId, InfrastructureData data) {
        File f = new File(maintenanceDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("roadCondition", data.roadCondition);
        o.addProperty("buildingCondition", data.buildingCondition);
        o.addProperty("lastMaintenance", data.lastMaintenance);
        o.addProperty("maintenanceBudget", data.maintenanceBudget);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive infrastructure maintenance statistics for a nation.
     */
    public synchronized Map<String, Object> getInfrastructureMaintenanceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        InfrastructureData data = nationInfrastructure.get(nationId);
        if (data == null) {
            stats.put("hasData", false);
            return stats;
        }
        
        stats.put("hasData", true);
        stats.put("roadCondition", data.roadCondition);
        stats.put("buildingCondition", data.buildingCondition);
        stats.put("maintenanceBudget", data.maintenanceBudget);
        stats.put("averageCondition", (data.roadCondition + data.buildingCondition) / 2.0);
        
        // Condition rating
        double avg = (data.roadCondition + data.buildingCondition) / 2.0;
        String rating = "КРИТИЧЕСКОЕ";
        if (avg >= 80) rating = "ОТЛИЧНОЕ";
        else if (avg >= 60) rating = "ХОРОШЕЕ";
        else if (avg >= 40) rating = "УДОВЛЕТВОРИТЕЛЬНОЕ";
        else if (avg >= 30) rating = "ПЛОХОЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global infrastructure maintenance statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalInfrastructureMaintenanceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalNations = nationInfrastructure.size();
        double totalRoadCondition = 0.0;
        double totalBuildingCondition = 0.0;
        double totalBudget = 0.0;
        Map<String, Double> roadConditionByNation = new HashMap<>();
        Map<String, Double> buildingConditionByNation = new HashMap<>();
        Map<String, Double> averageConditionByNation = new HashMap<>();
        
        for (Map.Entry<String, InfrastructureData> entry : nationInfrastructure.entrySet()) {
            String nationId = entry.getKey();
            InfrastructureData data = entry.getValue();
            
            totalRoadCondition += data.roadCondition;
            totalBuildingCondition += data.buildingCondition;
            totalBudget += data.maintenanceBudget;
            
            roadConditionByNation.put(nationId, data.roadCondition);
            buildingConditionByNation.put(nationId, data.buildingCondition);
            double avg = (data.roadCondition + data.buildingCondition) / 2.0;
            averageConditionByNation.put(nationId, avg);
        }
        
        stats.put("totalNations", totalNations);
        stats.put("averageRoadCondition", totalNations > 0 ? totalRoadCondition / totalNations : 0);
        stats.put("averageBuildingCondition", totalNations > 0 ? totalBuildingCondition / totalNations : 0);
        stats.put("averageOverallCondition", totalNations > 0 ? 
            (totalRoadCondition + totalBuildingCondition) / (totalNations * 2) : 0);
        stats.put("totalMaintenanceBudget", totalBudget);
        stats.put("averageMaintenanceBudget", totalNations > 0 ? totalBudget / totalNations : 0);
        stats.put("roadConditionByNation", roadConditionByNation);
        stats.put("buildingConditionByNation", buildingConditionByNation);
        stats.put("averageConditionByNation", averageConditionByNation);
        
        // Top nations by infrastructure condition
        List<Map.Entry<String, Double>> topByCondition = averageConditionByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCondition", topByCondition);
        
        // Condition distribution
        int excellent = 0, good = 0, satisfactory = 0, poor = 0, critical = 0;
        for (Double avg : averageConditionByNation.values()) {
            if (avg >= 80) excellent++;
            else if (avg >= 60) good++;
            else if (avg >= 40) satisfactory++;
            else if (avg >= 30) poor++;
            else critical++;
        }
        
        Map<String, Integer> conditionDistribution = new HashMap<>();
        conditionDistribution.put("excellent", excellent);
        conditionDistribution.put("good", good);
        conditionDistribution.put("satisfactory", satisfactory);
        conditionDistribution.put("poor", poor);
        conditionDistribution.put("critical", critical);
        stats.put("conditionDistribution", conditionDistribution);
        
        return stats;
    }
}

