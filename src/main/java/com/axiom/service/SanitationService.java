package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manages sanitation and public health infrastructure. */
public class SanitationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File sanitationDir;
    private final Map<String, Double> nationSanitation = new HashMap<>(); // nationId -> level (0-100)

    public SanitationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.sanitationDir = new File(plugin.getDataFolder(), "sanitation");
        this.sanitationDir.mkdirs();
        loadAll();
    }

    public synchronized void improveSanitation(String nationId, double amount) {
        double current = nationSanitation.getOrDefault(nationId, 50.0);
        nationSanitation.put(nationId, Math.min(100, current + amount));
        saveSanitation(nationId);
    }

    public synchronized double getSanitationLevel(String nationId) {
        return nationSanitation.getOrDefault(nationId, 50.0);
    }

    public synchronized double getHealthBonus(String nationId) {
        double level = getSanitationLevel(nationId);
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 1.0;
        // Health budget also improves sanitation
        level += n.getBudgetHealth() / 5000.0;
        return 1.0 + (level / 200.0); // Up to 1.5x bonus
    }

    private void loadAll() {
        File[] files = sanitationDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                nationSanitation.put(nationId, o.get("sanitation").getAsDouble());
            } catch (Exception ignored) {}
        }
    }

    private void saveSanitation(String nationId) {
        File f = new File(sanitationDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("sanitation", nationSanitation.getOrDefault(nationId, 50.0));
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive sanitation statistics.
     */
    public synchronized Map<String, Object> getSanitationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        double level = getSanitationLevel(nationId);
        double healthBonus = getHealthBonus(nationId);
        
        stats.put("sanitationLevel", level);
        stats.put("healthBonus", healthBonus);
        stats.put("healthBonusPercent", (healthBonus - 1.0) * 100);
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            double budgetImpact = n.getBudgetHealth() / 5000.0;
            stats.put("budgetHealth", n.getBudgetHealth());
            stats.put("budgetImpact", budgetImpact);
            stats.put("effectiveLevel", Math.min(100, level + budgetImpact));
        }
        
        // Sanitation rating
        String rating = "ОТСУТСТВУЕТ";
        if (level >= 90) rating = "ОТЛИЧНАЯ";
        else if (level >= 75) rating = "ХОРОШАЯ";
        else if (level >= 60) rating = "НОРМАЛЬНАЯ";
        else if (level >= 40) rating = "НИЗКАЯ";
        else rating = "КРИТИЧЕСКАЯ";
        stats.put("rating", rating);
        
        // Required investment for next level
        double remaining = 100 - level;
        stats.put("remainingToMax", remaining);
        stats.put("estimatedCost", remaining * 100); // 100 per level point
        
        return stats;
    }
    
    /**
     * Invest in sanitation.
     */
    public synchronized String investInSanitation(String nationId, double amount) throws IOException {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < amount) return "Недостаточно средств.";
        
        double improvement = amount / 100.0; // 100 currency = 1 level point
        improveSanitation(nationId, improvement);
        n.setTreasury(n.getTreasury() - amount);
        
        nationManager.save(n);
        
        double newLevel = getSanitationLevel(nationId);
        return "Санитария улучшена: " + String.format("%.1f", newLevel) + "/100";
    }
    
    /**
     * Get sanitation impact on population growth.
     */
    public synchronized double getPopulationGrowthBonus(String nationId) {
        double level = getSanitationLevel(nationId);
        // +0.1% growth per 10 sanitation points
        return 1.0 + (level / 10.0 * 0.001); // Max +1% at 100
    }
    
    /**
     * Get disease prevention effectiveness.
     */
    public synchronized double getDiseasePrevention(String nationId) {
        double level = getSanitationLevel(nationId);
        // Prevents up to 50% of diseases at max level
        return level / 200.0; // Max 0.5 (50%)
    }
    
    /**
     * Get global sanitation statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalSanitationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalNations = nationManager.getAll().size();
        double totalSanitation = 0.0;
        double maxSanitation = 0.0;
        double minSanitation = Double.MAX_VALUE;
        Map<String, Double> sanitationByNation = new HashMap<>();
        
        for (Nation n : nationManager.getAll()) {
            double level = getSanitationLevel(n.getId());
            totalSanitation += level;
            sanitationByNation.put(n.getId(), level);
            
            if (level > maxSanitation) maxSanitation = level;
            if (level < minSanitation) minSanitation = level;
        }
        
        stats.put("totalNations", totalNations);
        stats.put("averageSanitation", totalNations > 0 ? totalSanitation / totalNations : 0);
        stats.put("maxSanitation", maxSanitation);
        stats.put("minSanitation", minSanitation == Double.MAX_VALUE ? 0 : minSanitation);
        stats.put("sanitationByNation", sanitationByNation);
        
        // Top nations by sanitation
        List<Map.Entry<String, Double>> topBySanitation = sanitationByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySanitation", topBySanitation);
        
        // Sanitation distribution
        int excellent = 0, good = 0, fair = 0, poor = 0;
        for (Double level : sanitationByNation.values()) {
            if (level >= 80) excellent++;
            else if (level >= 60) good++;
            else if (level >= 40) fair++;
            else poor++;
        }
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("excellent", excellent);
        distribution.put("good", good);
        distribution.put("fair", fair);
        distribution.put("poor", poor);
        stats.put("distribution", distribution);
        
        // Calculate average disease prevention
        double totalPrevention = 0.0;
        for (Nation n : nationManager.getAll()) {
            totalPrevention += getDiseasePrevention(n.getId());
        }
        stats.put("averageDiseasePrevention", totalNations > 0 ? totalPrevention / totalNations : 0);
        
        // Calculate average population growth bonus
        double totalGrowthBonus = 0.0;
        for (Nation n : nationManager.getAll()) {
            totalGrowthBonus += getPopulationGrowthBonus(n.getId());
        }
        stats.put("averageGrowthBonus", totalNations > 0 ? (totalGrowthBonus / totalNations - 1.0) * 100 : 0);
        
        return stats;
    }
}

