package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages cultural revolutions and major cultural shifts. */
public class CulturalRevolutionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File revolutionsDir;
    private final Map<String, CulturalRevolution> activeRevolutions = new HashMap<>(); // nationId -> revolution

    public static class CulturalRevolution {
        String nationId;
        String type; // "progressive", "conservative", "religious", "secular"
        double progress; // 0-100%
        double intensity;
        long startedAt;
        boolean active;
    }

    public CulturalRevolutionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.revolutionsDir = new File(plugin.getDataFolder(), "culturalrevolutions");
        this.revolutionsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processRevolutions, 0, 20 * 60 * 15); // every 15 minutes
    }

    public synchronized String startRevolution(String nationId, String type, double cost) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        if (activeRevolutions.containsKey(nationId)) return "Революция уже активна.";
        CulturalRevolution rev = new CulturalRevolution();
        rev.nationId = nationId;
        rev.type = type;
        rev.progress = 0.0;
        rev.intensity = 50.0;
        rev.startedAt = System.currentTimeMillis();
        rev.active = true;
        activeRevolutions.put(nationId, rev);
        n.setTreasury(n.getTreasury() - cost);
        n.getHistory().add("Культурная революция начата: " + type);
        try {
            nationManager.save(n);
            saveRevolution(rev);
        } catch (Exception ignored) {}
        return "Культурная революция начата: " + type;
    }

    private void processRevolutions() {
        List<String> completed = new ArrayList<>();
        for (Map.Entry<String, CulturalRevolution> e : activeRevolutions.entrySet()) {
            CulturalRevolution rev = e.getValue();
            if (!rev.active) {
                completed.add(e.getKey());
                continue;
            }
            // Progress depends on happiness and education
            double happiness = plugin.getHappinessService().getNationHappiness(e.getKey());
            double education = plugin.getEducationService().getEducationLevel(e.getKey());
            double progressRate = (happiness + education) * 0.01; // Combined factor
            rev.progress = Math.min(100, rev.progress + progressRate);
            // Effects during revolution
            switch (rev.type.toLowerCase()) {
                case "progressive":
                    plugin.getEducationService().addResearchProgress(e.getKey(), 10.0);
                    break;
                case "conservative":
                    plugin.getHappinessService().modifyHappiness(e.getKey(), 5.0);
                    break;
                case "religious":
                    plugin.getCultureService().addCulturalScore(e.getKey(), 5.0);
                    break;
                case "secular":
                    plugin.getEducationService().addResearchProgress(e.getKey(), 15.0);
                    break;
            }
            if (rev.progress >= 100) {
                rev.active = false;
                Nation n = nationManager.getNationById(e.getKey());
                if (n != null) {
                    n.getHistory().add("Культурная революция завершена: " + rev.type);
                    // Permanent effects
                    switch (rev.type.toLowerCase()) {
                        case "progressive":
                            plugin.getEducationService().addResearchProgress(e.getKey(), 100.0);
                            break;
                        case "conservative":
                            plugin.getHappinessService().modifyHappiness(e.getKey(), 20.0);
                            break;
                        case "religious":
                            plugin.getCultureService().addCulturalScore(e.getKey(), 50.0);
                            break;
                        case "secular":
                            plugin.getEducationService().addResearchProgress(e.getKey(), 150.0);
                            break;
                    }
                    try { nationManager.save(n); } catch (Exception ignored) {}
                }
                completed.add(e.getKey());
            }
            saveRevolution(rev);
        }
        for (String nationId : completed) {
            activeRevolutions.remove(nationId);
        }
    }

    private void loadAll() {
        File[] files = revolutionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                CulturalRevolution rev = new CulturalRevolution();
                rev.nationId = nationId;
                rev.type = o.get("type").getAsString();
                rev.progress = o.get("progress").getAsDouble();
                rev.intensity = o.get("intensity").getAsDouble();
                rev.startedAt = o.get("startedAt").getAsLong();
                rev.active = o.has("active") && o.get("active").getAsBoolean();
                if (rev.active) activeRevolutions.put(nationId, rev);
            } catch (Exception ignored) {}
        }
    }

    private void saveRevolution(CulturalRevolution rev) {
        File f = new File(revolutionsDir, rev.nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", rev.nationId);
        o.addProperty("type", rev.type);
        o.addProperty("progress", rev.progress);
        o.addProperty("intensity", rev.intensity);
        o.addProperty("startedAt", rev.startedAt);
        o.addProperty("active", rev.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive cultural revolution statistics.
     */
    public synchronized Map<String, Object> getCulturalRevolutionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        CulturalRevolution rev = activeRevolutions.get(nationId);
        if (rev != null) {
            stats.put("hasActiveRevolution", true);
            stats.put("type", rev.type);
            stats.put("progress", rev.progress);
            stats.put("intensity", rev.intensity);
            stats.put("startedAt", rev.startedAt);
            stats.put("duration", (System.currentTimeMillis() - rev.startedAt) / 1000 / 60); // minutes
            
            // Progress percentage
            stats.put("progressPercent", rev.progress);
            
            // Estimated completion time
            double happiness = plugin.getHappinessService().getNationHappiness(nationId);
            double education = plugin.getEducationService().getEducationLevel(nationId);
            double progressRate = (happiness + education) * 0.01;
            double remaining = 100 - rev.progress;
            long estimatedMinutes = progressRate > 0 ? (long)(remaining / progressRate) : -1;
            stats.put("estimatedCompletionMinutes", estimatedMinutes);
            
            // Revolution rating
            String rating = "НАЧИНАЮЩАЯСЯ";
            if (rev.progress >= 80) rating = "ПОЧТИ ЗАВЕРШЕНА";
            else if (rev.progress >= 60) rating = "ПРОДВИНУТАЯ";
            else if (rev.progress >= 40) rating = "СРЕДНЯЯ";
            else if (rev.progress >= 20) rating = "РАЗВИВАЮЩАЯСЯ";
            stats.put("rating", rating);
        } else {
            stats.put("hasActiveRevolution", false);
        }
        
        return stats;
    }
    
    /**
     * Increase revolution intensity (through funding, etc.).
     */
    public synchronized String increaseIntensity(String nationId, double cost) throws IOException {
        CulturalRevolution rev = activeRevolutions.get(nationId);
        if (rev == null) return "Революции нет.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        rev.intensity = Math.min(100, rev.intensity + 10.0);
        n.setTreasury(n.getTreasury() - cost);
        
        nationManager.save(n);
        saveRevolution(rev);
        
        return "Интенсивность революции: " + String.format("%.1f", rev.intensity);
    }
    
    /**
     * End revolution prematurely.
     */
    public synchronized String endRevolution(String nationId) throws IOException {
        CulturalRevolution rev = activeRevolutions.remove(nationId);
        if (rev == null) return "Революции нет.";
        
        rev.active = false;
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Культурная революция прервана на " + String.format("%.1f", rev.progress) + "%");
            nationManager.save(n);
        }
        
        saveRevolution(rev);
        
        return "Революция прервана.";
    }
    
    /**
     * Get revolution progress.
     */
    public synchronized double getRevolutionProgress(String nationId) {
        CulturalRevolution rev = activeRevolutions.get(nationId);
        return rev != null ? rev.progress : 0.0;
    }
    
    /**
     * Get global cultural revolution statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCulturalRevolutionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeRevolutions", activeRevolutions.size());
        
        Map<String, Integer> revolutionsByType = new HashMap<>();
        double totalProgress = 0.0;
        double totalIntensity = 0.0;
        Map<String, Double> progressByNation = new HashMap<>();
        
        for (Map.Entry<String, CulturalRevolution> entry : activeRevolutions.entrySet()) {
            CulturalRevolution rev = entry.getValue();
            
            revolutionsByType.put(rev.type, revolutionsByType.getOrDefault(rev.type, 0) + 1);
            totalProgress += rev.progress;
            totalIntensity += rev.intensity;
            progressByNation.put(entry.getKey(), rev.progress);
        }
        
        stats.put("totalActiveRevolutions", activeRevolutions.size());
        stats.put("revolutionsByType", revolutionsByType);
        stats.put("averageProgress", activeRevolutions.size() > 0 ? totalProgress / activeRevolutions.size() : 0);
        stats.put("averageIntensity", activeRevolutions.size() > 0 ? totalIntensity / activeRevolutions.size() : 0);
        
        // Progress distribution
        int almostComplete = 0, advanced = 0, medium = 0, developing = 0, starting = 0;
        for (CulturalRevolution rev : activeRevolutions.values()) {
            if (rev.progress >= 80) almostComplete++;
            else if (rev.progress >= 60) advanced++;
            else if (rev.progress >= 40) medium++;
            else if (rev.progress >= 20) developing++;
            else starting++;
        }
        
        Map<String, Integer> progressDistribution = new HashMap<>();
        progressDistribution.put("almostComplete", almostComplete);
        progressDistribution.put("advanced", advanced);
        progressDistribution.put("medium", medium);
        progressDistribution.put("developing", developing);
        progressDistribution.put("starting", starting);
        stats.put("progressDistribution", progressDistribution);
        
        // Top nations by progress
        List<Map.Entry<String, Double>> topByProgress = progressByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByProgress", topByProgress);
        
        // Most common revolution types
        List<Map.Entry<String, Integer>> mostCommonTypes = revolutionsByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTypes", mostCommonTypes);
        
        // Average duration
        long now = System.currentTimeMillis();
        double totalDuration = 0.0;
        int count = 0;
        for (CulturalRevolution rev : activeRevolutions.values()) {
            totalDuration += (now - rev.startedAt) / 1000.0 / 60.0; // minutes
            count++;
        }
        stats.put("averageDurationMinutes", count > 0 ? totalDuration / count : 0);
        
        return stats;
    }
}

