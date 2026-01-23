package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages military intelligence and reconnaissance. */
public class MilitaryIntelligenceService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File intelligenceDir;
    private final Map<String, IntelligenceReport> reports = new HashMap<>(); // reportId -> report

    public static class IntelligenceReport {
        String id;
        String collectorNationId;
        String targetNationId;
        String type; // "military", "economic", "diplomatic", "technological"
        Map<String, Object> data = new HashMap<>();
        long collectedAt;
        double accuracy; // 0-100%
    }

    public MilitaryIntelligenceService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.intelligenceDir = new File(plugin.getDataFolder(), "intelligence");
        this.intelligenceDir.mkdirs();
        loadAll();
    }

    public synchronized String collectIntelligence(String collectorId, String targetId, String type, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(collectorId) || isBlank(targetId)) return "Нация не найдена.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        Nation collector = nationManager.getNationById(collectorId);
        Nation target = nationManager.getNationById(targetId);
        if (collector == null || target == null) return "Нация не найдена.";
        if (collector.getTreasury() < cost) return "Недостаточно средств.";
        String reportId = UUID.randomUUID().toString().substring(0, 8);
        IntelligenceReport report = new IntelligenceReport();
        report.id = reportId;
        report.collectorNationId = collectorId;
        report.targetNationId = targetId;
        report.type = (type == null || type.trim().isEmpty()) ? "military" : type;
        report.collectedAt = System.currentTimeMillis();
        report.accuracy = 50.0 + Math.random() * 40.0; // 50-90% accuracy
        
        // Gather data based on type
        switch (report.type.toLowerCase()) {
            case "military":
                report.data.put("strength", plugin.getMilitaryService() != null ? plugin.getMilitaryService().getMilitaryStrength(targetId) : 0.0);
                report.data.put("units", "Various");
                break;
            case "economic":
                report.data.put("treasury", target.getTreasury());
                report.data.put("inflation", target.getInflation());
                break;
            case "diplomatic":
                report.data.put("allies", target.getAllies() != null ? target.getAllies().size() : 0);
                report.data.put("enemies", target.getEnemies() != null ? target.getEnemies().size() : 0);
                break;
            case "technological":
                report.data.put("education", plugin.getEducationService() != null ? plugin.getEducationService().getEducationLevel(targetId) : 0.0);
                break;
            default:
                report.data.put("info", "Unknown intelligence type");
        }
        
        reports.put(reportId, report);
        collector.setTreasury(collector.getTreasury() - cost);
        if (collector.getHistory() != null) {
            collector.getHistory().add("Собрана разведка: " + report.type + " от " + target.getName());
        }
        try {
            nationManager.save(collector);
            saveReport(report);
        } catch (Exception ignored) {}
        return "Разведка собрана (точность: " + String.format("%.1f", report.accuracy) + "%)";
    }

    public synchronized IntelligenceReport getReport(String reportId) {
        if (isBlank(reportId)) return null;
        return reports.get(reportId);
    }

    private void loadAll() {
        File[] files = intelligenceDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                IntelligenceReport report = new IntelligenceReport();
                report.id = o.get("id").getAsString();
                report.collectorNationId = o.get("collectorNationId").getAsString();
                report.targetNationId = o.get("targetNationId").getAsString();
                report.type = o.get("type").getAsString();
                report.collectedAt = o.get("collectedAt").getAsLong();
                report.accuracy = o.get("accuracy").getAsDouble();
                if (o.has("data")) {
                    JsonObject data = o.getAsJsonObject("data");
                    for (var entry : data.entrySet()) {
                        report.data.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                reports.put(report.id, report);
            } catch (Exception ignored) {}
        }
    }

    private void saveReport(IntelligenceReport report) {
        File f = new File(intelligenceDir, report.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", report.id);
        o.addProperty("collectorNationId", report.collectorNationId);
        o.addProperty("targetNationId", report.targetNationId);
        o.addProperty("type", report.type);
        o.addProperty("collectedAt", report.collectedAt);
        o.addProperty("accuracy", report.accuracy);
        JsonObject data = new JsonObject();
        for (var entry : report.data.entrySet()) {
            data.addProperty(entry.getKey(), entry.getValue().toString());
        }
        o.add("data", data);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive military intelligence statistics for a nation.
     */
    public synchronized Map<String, Object> getMilitaryIntelligenceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) {
            stats.put("reportsCollected", 0);
            stats.put("reportsOnThis", 0);
            stats.put("reportsByType", new HashMap<>());
            stats.put("targetsByNation", new HashMap<>());
            stats.put("averageAccuracy", 0);
            stats.put("uniqueTargets", 0);
            stats.put("rating", "НЕТ РАЗВЕДКИ");
            return stats;
        }
        int reportsCollected = 0;
        int reportsOnThis = 0;
        Map<String, Integer> reportsByType = new HashMap<>();
        Map<String, Integer> targetsByNation = new HashMap<>();
        double totalAccuracy = 0.0;
        
        for (IntelligenceReport report : reports.values()) {
            if (report.collectorNationId.equals(nationId)) {
                reportsCollected++;
                reportsByType.put(report.type, reportsByType.getOrDefault(report.type, 0) + 1);
                targetsByNation.put(report.targetNationId, targetsByNation.getOrDefault(report.targetNationId, 0) + 1);
                totalAccuracy += report.accuracy;
            }
            if (report.targetNationId.equals(nationId)) {
                reportsOnThis++;
            }
        }
        
        stats.put("reportsCollected", reportsCollected);
        stats.put("reportsOnThis", reportsOnThis);
        stats.put("reportsByType", reportsByType);
        stats.put("targetsByNation", targetsByNation);
        stats.put("averageAccuracy", reportsCollected > 0 ? totalAccuracy / reportsCollected : 0);
        stats.put("uniqueTargets", targetsByNation.size());
        
        // Intelligence rating
        String rating = "НЕТ РАЗВЕДКИ";
        if (reportsCollected >= 50) rating = "ВСЕОБЪЕМЛЮЩАЯ";
        else if (reportsCollected >= 20) rating = "РАЗВИТАЯ";
        else if (reportsCollected >= 10) rating = "АКТИВНАЯ";
        else if (reportsCollected >= 5) rating = "БАЗОВАЯ";
        else if (reportsCollected >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global military intelligence statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalMilitaryIntelligenceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalReports", reports.size());
        
        Map<String, Integer> reportsCollectedByNation = new HashMap<>();
        Map<String, Integer> reportsOnNation = new HashMap<>();
        Map<String, Integer> reportsByType = new HashMap<>();
        double totalAccuracy = 0.0;
        
        for (IntelligenceReport report : reports.values()) {
            reportsCollectedByNation.put(report.collectorNationId,
                reportsCollectedByNation.getOrDefault(report.collectorNationId, 0) + 1);
            reportsOnNation.put(report.targetNationId,
                reportsOnNation.getOrDefault(report.targetNationId, 0) + 1);
            reportsByType.put(report.type, reportsByType.getOrDefault(report.type, 0) + 1);
            totalAccuracy += report.accuracy;
        }
        
        stats.put("reportsCollectedByNation", reportsCollectedByNation);
        stats.put("reportsOnNation", reportsOnNation);
        stats.put("reportsByType", reportsByType);
        stats.put("averageAccuracy", reports.size() > 0 ? totalAccuracy / reports.size() : 0);
        stats.put("nationsCollecting", reportsCollectedByNation.size());
        stats.put("nationsBeingSpiedOn", reportsOnNation.size());
        
        // Top nations by reports collected
        List<Map.Entry<String, Integer>> topByCollected = reportsCollectedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCollected", topByCollected);
        
        // Most spied on nations
        List<Map.Entry<String, Integer>> topBySpiedOn = reportsOnNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySpiedOn", topBySpiedOn);
        
        // Most common intelligence types
        List<Map.Entry<String, Integer>> topByType = reportsByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByType", topByType);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

