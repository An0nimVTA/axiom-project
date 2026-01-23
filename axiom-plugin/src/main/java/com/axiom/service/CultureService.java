package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages cultural development and cultural influence of nations. */
public class CultureService {
    private final AXIOM plugin;
    private final File cultureDir;
    private final Map<String, CulturalData> nationCulture = new HashMap<>(); // nationId -> data

    public static class CulturalData {
        double cultureLevel = 0.0; // 0-100
        String dominantCulture;
        Map<String, Double> culturalInfluence = new HashMap<>(); // culture -> influence %
    }

    public CultureService(AXIOM plugin) {
        this.plugin = plugin;
        this.cultureDir = new File(plugin.getDataFolder(), "culture");
        this.cultureDir.mkdirs();
        loadAll();
    }

    public synchronized void developCulture(String nationId, double amount) {
        CulturalData data = nationCulture.computeIfAbsent(nationId, k -> new CulturalData());
        data.cultureLevel = Math.max(0, Math.min(100, data.cultureLevel + amount));
        saveCulture(nationId, data);
    }

    public synchronized double getCultureLevel(String nationId) {
        CulturalData data = nationCulture.get(nationId);
        return data != null ? data.cultureLevel : 0.0;
    }

    public synchronized double getCulturalScore(String nationId) {
        return getCultureLevel(nationId);
    }

    public synchronized void addCulturalScore(String nationId, double amount) {
        developCulture(nationId, amount);
    }

    public synchronized void spreadCulturalInfluence(String sourceId, String targetId, double amount) {
        if (sourceId == null || targetId == null || amount <= 0) return;
        CulturalData source = nationCulture.computeIfAbsent(sourceId, k -> new CulturalData());
        CulturalData target = nationCulture.computeIfAbsent(targetId, k -> new CulturalData());
        String culture = source.dominantCulture != null ? source.dominantCulture : sourceId;
        double current = target.culturalInfluence.getOrDefault(culture, 0.0);
        target.culturalInfluence.put(culture, Math.min(100, current + amount));
        saveCulture(targetId, target);
    }

    private void loadAll() {
        File[] files = cultureDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                CulturalData data = new CulturalData();
                data.cultureLevel = o.has("cultureLevel") ? o.get("cultureLevel").getAsDouble() : 0.0;
                data.dominantCulture = o.has("dominantCulture") ? o.get("dominantCulture").getAsString() : null;
                if (o.has("culturalInfluence")) {
                    JsonObject inf = o.getAsJsonObject("culturalInfluence");
                    for (var e : inf.entrySet()) {
                        data.culturalInfluence.put(e.getKey(), e.getValue().getAsDouble());
                    }
                }
                String nationId = f.getName().replace(".json", "");
                nationCulture.put(nationId, data);
            } catch (Exception ignored) {}
        }
    }

    private void saveCulture(String nationId, CulturalData data) {
        File f = new File(cultureDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("cultureLevel", data.cultureLevel);
        if (data.dominantCulture != null) o.addProperty("dominantCulture", data.dominantCulture);
        JsonObject inf = new JsonObject();
        for (var e : data.culturalInfluence.entrySet()) {
            inf.addProperty(e.getKey(), e.getValue());
        }
        o.add("culturalInfluence", inf);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive culture statistics.
     */
    public synchronized Map<String, Object> getCultureStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        CulturalData data = nationCulture.get(nationId);
        if (data == null) {
            data = new CulturalData();
            nationCulture.put(nationId, data);
        }
        
        stats.put("cultureLevel", data.cultureLevel);
        stats.put("dominantCulture", data.dominantCulture != null ? data.dominantCulture : "Не определено");
        stats.put("culturalInfluence", new HashMap<>(data.culturalInfluence));
        
        // Calculate cultural diversity
        int influenceCount = data.culturalInfluence.size();
        double diversity = influenceCount > 0 ? Math.min(100, influenceCount * 10.0) : 0.0;
        stats.put("culturalDiversity", diversity);
        
        // Cultural rating
        String rating = "ВЕЛИКАЯ";
        if (data.cultureLevel < 70) rating = "РАЗВИТАЯ";
        if (data.cultureLevel < 50) rating = "СРЕДНЯЯ";
        if (data.cultureLevel < 30) rating = "НАЧАЛЬНАЯ";
        if (data.cultureLevel < 10) rating = "ЗАРОЖДАЮЩАЯСЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Set dominant culture.
     */
    public synchronized String setDominantCulture(String nationId, String cultureName) {
        if (cultureName == null || cultureName.trim().isEmpty()) return "Некорректное название культуры.";
        CulturalData data = nationCulture.computeIfAbsent(nationId, k -> new CulturalData());
        data.dominantCulture = cultureName;
        saveCulture(nationId, data);
        return "Доминирующая культура установлена: " + cultureName;
    }
    
    /**
     * Calculate cultural bonus (affects happiness, trade, etc.).
     */
    public synchronized double getCulturalBonus(String nationId) {
        CulturalData data = nationCulture.get(nationId);
        if (data == null) return 1.0;
        
        // Culture level provides bonus (up to +20%)
        return 1.0 + (data.cultureLevel / 500.0);
    }
    
    /**
     * Get top culturally influenced nations.
     */
    public synchronized List<String> getTopCulturallyInfluenced(String sourceId) {
        CulturalData source = nationCulture.get(sourceId);
        if (source == null) return Collections.emptyList();
        
        // Find nations with highest influence from this source
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, CulturalData> entry : nationCulture.entrySet()) {
            if (entry.getKey().equals(sourceId)) continue;
            double influence = entry.getValue().culturalInfluence.getOrDefault(
                source.dominantCulture != null ? source.dominantCulture : sourceId, 0.0);
            if (influence > 50) {
                result.add(entry.getKey());
            }
        }
        
        // Sort by influence level
        result.sort((a, b) -> {
            CulturalData aData = nationCulture.get(a);
            CulturalData bData = nationCulture.get(b);
            String culture = source.dominantCulture != null ? source.dominantCulture : sourceId;
            double aInf = aData != null ? aData.culturalInfluence.getOrDefault(culture, 0.0) : 0.0;
            double bInf = bData != null ? bData.culturalInfluence.getOrDefault(culture, 0.0) : 0.0;
            return Double.compare(bInf, aInf);
        });
        
        return result;
    }
    
    /**
     * Get global culture statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCultureStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        double totalCultureLevel = 0.0;
        double maxCultureLevel = 0.0;
        double minCultureLevel = Double.MAX_VALUE;
        int nationsWithCulture = 0;
        Map<String, Integer> byDominantCulture = new HashMap<>();
        
        for (Nation n : nationManager.getAll()) {
            CulturalData data = nationCulture.get(n.getId());
            double cultureLevel = data != null ? data.cultureLevel : 0.0;
            
            if (cultureLevel > 0 || (data != null && !data.culturalInfluence.isEmpty())) {
                nationsWithCulture++;
            }
            
            totalCultureLevel += cultureLevel;
            maxCultureLevel = Math.max(maxCultureLevel, cultureLevel);
            minCultureLevel = Math.min(minCultureLevel, cultureLevel);
            
            if (data != null && data.dominantCulture != null) {
                byDominantCulture.put(data.dominantCulture, byDominantCulture.getOrDefault(data.dominantCulture, 0) + 1);
            }
        }
        
        stats.put("totalNations", nationManager.getAll().size());
        stats.put("nationsWithCulture", nationsWithCulture);
        stats.put("averageCultureLevel", nationsWithCulture > 0 ? totalCultureLevel / nationsWithCulture : 0);
        stats.put("maxCultureLevel", maxCultureLevel);
        stats.put("minCultureLevel", minCultureLevel == Double.MAX_VALUE ? 0 : minCultureLevel);
        stats.put("dominantCultures", byDominantCulture);
        
        // Top nations by culture level
        List<Map.Entry<String, Double>> topByCulture = new ArrayList<>();
        for (Nation n : nationManager.getAll()) {
            CulturalData data = nationCulture.get(n.getId());
            double cultureLevel = data != null ? data.cultureLevel : 0.0;
            topByCulture.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), cultureLevel));
        }
        topByCulture.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByCulture", topByCulture.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

