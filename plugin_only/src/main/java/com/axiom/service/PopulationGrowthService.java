package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages population growth and demographics. */
public class PopulationGrowthService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File populationDir;
    private final Map<String, PopulationData> nationPopulations = new HashMap<>(); // nationId -> data

    public static class PopulationData {
        double totalPopulation; // citizens + NPCs
        double growthRate; // per year %
        double birthRate;
        double deathRate;
        double migrationRate;
        long lastUpdated;
    }

    public PopulationGrowthService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.populationDir = new File(plugin.getDataFolder(), "population");
        this.populationDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processGrowth, 0, 20 * 60 * 30); // every 30 minutes
    }

    private void processGrowth() {
        for (Nation n : nationManager.getAll()) {
            PopulationData data = nationPopulations.computeIfAbsent(n.getId(), k -> {
                PopulationData d = new PopulationData();
                d.totalPopulation = n.getCitizens().size() * 10.0; // 10 NPCs per citizen
                d.growthRate = 1.0; // 1% per year default
                d.birthRate = 2.0;
                d.deathRate = 1.0;
                d.migrationRate = 0.0;
                d.lastUpdated = System.currentTimeMillis();
                return d;
            });
            // Factors affecting growth
            double happiness = plugin.getHappinessService().getNationHappiness(n.getId());
            double healthcare = n.getBudgetHealth() / 1000.0;
            // Higher happiness and healthcare = lower death rate, higher growth
            data.deathRate = Math.max(0.5, 1.0 - (happiness * 0.01) - (healthcare * 0.001));
            data.birthRate = 1.5 + (happiness * 0.02);
            data.growthRate = (data.birthRate - data.deathRate) / 100.0; // Convert to growth rate
            // Apply growth
            double growth = data.totalPopulation * data.growthRate * (30.0 / 365.0); // Per 30 minutes
            data.totalPopulation = Math.max(n.getCitizens().size(), data.totalPopulation + growth);
            data.lastUpdated = System.currentTimeMillis();
            savePopulation(n.getId(), data);
        }
    }

    public synchronized double getPopulation(String nationId) {
        PopulationData data = nationPopulations.get(nationId);
        return data != null ? data.totalPopulation : 0.0;
    }

    private void loadAll() {
        File[] files = populationDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                PopulationData data = new PopulationData();
                data.totalPopulation = o.get("totalPopulation").getAsDouble();
                data.growthRate = o.get("growthRate").getAsDouble();
                data.birthRate = o.get("birthRate").getAsDouble();
                data.deathRate = o.get("deathRate").getAsDouble();
                data.migrationRate = o.has("migrationRate") ? o.get("migrationRate").getAsDouble() : 0.0;
                data.lastUpdated = o.has("lastUpdated") ? o.get("lastUpdated").getAsLong() : System.currentTimeMillis();
                nationPopulations.put(nationId, data);
            } catch (Exception ignored) {}
        }
    }

    private void savePopulation(String nationId, PopulationData data) {
        File f = new File(populationDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("totalPopulation", data.totalPopulation);
        o.addProperty("growthRate", data.growthRate);
        o.addProperty("birthRate", data.birthRate);
        o.addProperty("deathRate", data.deathRate);
        o.addProperty("migrationRate", data.migrationRate);
        o.addProperty("lastUpdated", data.lastUpdated);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive population statistics.
     */
    public synchronized Map<String, Object> getPopulationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        PopulationData data = nationPopulations.get(nationId);
        Nation n = nationManager.getNationById(nationId);
        
        if (data == null || n == null) {
            stats.put("totalPopulation", n != null ? n.getCitizens().size() * 10.0 : 0.0);
            stats.put("growthRate", 0.0);
            stats.put("birthRate", 0.0);
            stats.put("deathRate", 0.0);
            return stats;
        }
        
        stats.put("totalPopulation", data.totalPopulation);
        stats.put("citizens", n.getCitizens().size());
        stats.put("npcs", data.totalPopulation - n.getCitizens().size());
        stats.put("growthRate", data.growthRate * 100); // Convert to percentage
        stats.put("birthRate", data.birthRate);
        stats.put("deathRate", data.deathRate);
        stats.put("migrationRate", data.migrationRate);
        
        // Population density (per chunk)
        int chunks = n.getClaimedChunkKeys().size();
        if (chunks > 0) {
            stats.put("density", data.totalPopulation / chunks);
        }
        
        // Population growth trend
        String trend = "СТАБИЛЬНО";
        if (data.growthRate > 0.02) trend = "РАСТЁТ";
        else if (data.growthRate < -0.01) trend = "СНИЖАЕТСЯ";
        stats.put("trend", trend);
        
        return stats;
    }
    
    /**
     * Calculate population capacity (max sustainable population).
     */
    public synchronized double getPopulationCapacity(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 0.0;
        
        // Base capacity from claimed chunks
        double baseCapacity = n.getClaimedChunkKeys().size() * 100.0;
        
        // Infrastructure bonus (simplified - can be enhanced when InfrastructureService is fully integrated)
        
        // Food/resources affect capacity
        if (plugin.getResourceService() != null) {
            double food = plugin.getResourceService().getResource(nationId, "food");
            if (food > 1000) baseCapacity *= 1.2; // +20% if well-fed
        }
        
        return baseCapacity;
    }
    
    /**
     * Force population migration (events, disasters, etc.).
     */
    public synchronized String applyMigration(String nationId, double migrationAmount) {
        PopulationData data = nationPopulations.get(nationId);
        if (data == null) return "Данные о населении не найдены.";
        
        data.totalPopulation = Math.max(0, data.totalPopulation + migrationAmount);
        data.migrationRate += migrationAmount / 100.0; // Update migration rate
        savePopulation(nationId, data);
        
        return "Миграция применена. Новое население: " + String.format("%.0f", data.totalPopulation);
    }
    
    /**
     * Get global population statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPopulationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalPopulation = 0.0;
        double maxPopulation = 0.0;
        double totalGrowthRate = 0.0;
        double totalBirthRate = 0.0;
        double totalDeathRate = 0.0;
        int nationsWithPopulation = 0;
        
        for (Nation n : nationManager.getAll()) {
            PopulationData data = nationPopulations.get(n.getId());
            double population = data != null ? data.totalPopulation : (n.getCitizens().size() * 10.0);
            
            if (population > 0 || data != null) {
                nationsWithPopulation++;
            }
            
            totalPopulation += population;
            maxPopulation = Math.max(maxPopulation, population);
            
            if (data != null) {
                totalGrowthRate += data.growthRate;
                totalBirthRate += data.birthRate;
                totalDeathRate += data.deathRate;
            }
        }
        
        stats.put("totalNations", nationManager.getAll().size());
        stats.put("nationsWithPopulation", nationsWithPopulation);
        stats.put("totalPopulation", totalPopulation);
        stats.put("maxPopulation", maxPopulation);
        stats.put("averagePopulation", nationsWithPopulation > 0 ? totalPopulation / nationsWithPopulation : 0);
        stats.put("averageGrowthRate", nationsWithPopulation > 0 ? (totalGrowthRate / nationsWithPopulation) * 100 : 0);
        stats.put("averageBirthRate", nationsWithPopulation > 0 ? totalBirthRate / nationsWithPopulation : 0);
        stats.put("averageDeathRate", nationsWithPopulation > 0 ? totalDeathRate / nationsWithPopulation : 0);
        
        // Top nations by population
        List<Map.Entry<String, Double>> topByPopulation = new ArrayList<>();
        for (Nation n : nationManager.getAll()) {
            PopulationData data = nationPopulations.get(n.getId());
            double population = data != null ? data.totalPopulation : (n.getCitizens().size() * 10.0);
            topByPopulation.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), population));
        }
        topByPopulation.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByPopulation", topByPopulation.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

