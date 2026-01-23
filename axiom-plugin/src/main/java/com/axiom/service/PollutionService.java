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

/** Tracks pollution levels per nation and their environmental impact. */
public class PollutionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File pollutionDir;
    private final Map<String, Double> nationPollution = new HashMap<>(); // nationId -> pollution level (0-100)

    public PollutionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.pollutionDir = new File(plugin.getDataFolder(), "pollution");
        this.pollutionDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updatePollution, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized void addPollution(String nationId, double amount) {
        if (isBlank(nationId) || !Double.isFinite(amount) || amount <= 0) return;
        double current = nationPollution.getOrDefault(nationId, 0.0);
        nationPollution.put(nationId, Math.min(100, current + amount));
        savePollution(nationId);
    }

    public synchronized double getPollution(String nationId) {
        return nationPollution.getOrDefault(nationId, 0.0);
    }

    private synchronized void updatePollution() {
        if (nationManager == null) return;
        // Pollution naturally decreases over time, but industrial activity increases it
        for (Nation n : nationManager.getAll()) {
            double current = getPollution(n.getId());
            // Industrial activity (based on budget) increases pollution
            double industrialFactor = n.getBudgetMilitary() / 10000.0;
            addPollution(n.getId(), industrialFactor * 0.1);
            // Natural cleanup
            if (current > 0) {
                nationPollution.put(n.getId(), Math.max(0, current - 0.5));
            }
        }
    }

    public synchronized double getHappinessPenalty(String nationId) {
        double pollution = getPollution(nationId);
        // High pollution reduces happiness
        return -pollution * 0.3; // -30 max at 100% pollution
    }

    private void loadAll() {
        File[] files = pollutionDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                nationPollution.put(nationId, o.get("pollution").getAsDouble());
            } catch (Exception ignored) {}
        }
    }

    private void savePollution(String nationId) {
        File f = new File(pollutionDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("pollution", nationPollution.getOrDefault(nationId, 0.0));
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive pollution statistics.
     */
    public synchronized Map<String, Object> getPollutionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        double pollution = getPollution(nationId);
        stats.put("pollution", pollution);
        
        // Pollution rating
        String rating = "ОТСУТСТВУЕТ";
        if (pollution >= 80) rating = "КРИТИЧЕСКАЯ";
        else if (pollution >= 60) rating = "ВЫСОКАЯ";
        else if (pollution >= 40) rating = "СРЕДНЯЯ";
        else if (pollution >= 20) rating = "НИЗКАЯ";
        else if (pollution > 0) rating = "МИНИМАЛЬНАЯ";
        stats.put("rating", rating);
        
        // Environmental impact
        stats.put("happinessPenalty", getHappinessPenalty(nationId));
        
        // Health impact
        double healthImpact = -pollution * 0.2; // -20 max at 100% pollution
        stats.put("healthImpact", healthImpact);
        
        // Economic impact (cleanup costs)
        double cleanupCost = pollution * 50.0; // Cost per hour to reduce pollution
        stats.put("estimatedCleanupCost", cleanupCost);
        
        // Factors affecting pollution
        Nation n = nationManager != null ? nationManager.getNationById(nationId) : null;
        if (n != null) {
            Map<String, Double> factors = new HashMap<>();
            factors.put("industrialFactor", n.getBudgetMilitary() / 10000.0); // Military budget = industry
            stats.put("factors", factors);
        }
        
        return stats;
    }
    
    /**
     * Reduce pollution through environmental measures.
     */
    public synchronized String reducePollution(String nationId, double cost) throws IOException {
        if (isBlank(nationId) || !Double.isFinite(cost) || cost < 0) return "Некорректные данные.";
        Nation n = nationManager != null ? nationManager.getNationById(nationId) : null;
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        
        double current = getPollution(nationId);
        double reduction = (cost / 1000.0) * 3.0; // Each 1000 reduces pollution by 3%
        double newPollution = Math.max(0, current - reduction);
        
        nationPollution.put(nationId, newPollution);
        n.setTreasury(n.getTreasury() - cost);
        
        nationManager.save(n);
        savePollution(nationId);
        
        return "Загрязнение снижено. Текущий уровень: " + String.format("%.1f", newPollution) + "%";
    }
    
    /**
     * Calculate total environmental health score.
     */
    public synchronized double getEnvironmentalHealthScore(String nationId) {
        double pollution = getPollution(nationId);
        // Score is inverse of pollution (0-100, where 100 = no pollution)
        return Math.max(0, 100.0 - pollution);
    }
    
    /**
     * Get pollution impact on various systems.
     */
    public synchronized Map<String, Double> getPollutionImpacts(String nationId) {
        Map<String, Double> impacts = new HashMap<>();
        double pollution = getPollution(nationId);
        
        impacts.put("happiness", -pollution * 0.3); // -0.3% per 1% pollution
        impacts.put("health", -pollution * 0.2); // -0.2% per 1% pollution
        impacts.put("populationGrowth", -pollution * 0.1); // -0.1% per 1% pollution
        
        return impacts;
    }
    
    /**
     * Get global pollution statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPollutionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalPollution = 0.0;
        double maxPollution = 0.0;
        double minPollution = Double.MAX_VALUE;
        int nationsWithPollution = 0;
        
        // Pollution distribution
        int critical = 0, high = 0, medium = 0, low = 0, minimal = 0, none = 0;
        
        if (nationManager == null) {
            stats.put("totalNations", 0);
            stats.put("nationsWithPollution", 0);
            stats.put("averagePollution", 0);
            stats.put("maxPollution", 0);
            stats.put("minPollution", 0);
            stats.put("pollutionDistribution", new HashMap<>());
            stats.put("topByPollution", new ArrayList<>());
            stats.put("averageEnvironmentalHealth", 0);
            return stats;
        }
        for (Nation n : nationManager.getAll()) {
            double pollution = getPollution(n.getId());
            
            if (pollution > 0) {
                nationsWithPollution++;
            }
            
            totalPollution += pollution;
            maxPollution = Math.max(maxPollution, pollution);
            minPollution = Math.min(minPollution, pollution);
            
            if (pollution >= 80) critical++;
            else if (pollution >= 60) high++;
            else if (pollution >= 40) medium++;
            else if (pollution >= 20) low++;
            else if (pollution > 0) minimal++;
            else none++;
        }
        
        stats.put("totalNations", nationManager.getAll().size());
        stats.put("nationsWithPollution", nationsWithPollution);
        stats.put("averagePollution", nationsWithPollution > 0 ? totalPollution / nationsWithPollution : 0);
        stats.put("maxPollution", maxPollution);
        stats.put("minPollution", minPollution == Double.MAX_VALUE ? 0 : minPollution);
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("critical", critical);
        distribution.put("high", high);
        distribution.put("medium", medium);
        distribution.put("low", low);
        distribution.put("minimal", minimal);
        distribution.put("none", none);
        stats.put("pollutionDistribution", distribution);
        
        // Top nations by pollution (worst)
        List<Map.Entry<String, Double>> topByPollution = new ArrayList<>();
        for (Nation n : nationManager.getAll()) {
            topByPollution.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), getPollution(n.getId())));
        }
        topByPollution.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByPollution", topByPollution.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        // Average environmental health score
        double totalHealthScore = 0.0;
        for (Nation n : nationManager.getAll()) {
            totalHealthScore += getEnvironmentalHealthScore(n.getId());
        }
        stats.put("averageEnvironmentalHealth", nationManager.getAll().size() > 0 ? 
            totalHealthScore / nationManager.getAll().size() : 0);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

