package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Set;
import java.util.HashSet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Collects and manages intelligence about other nations. */
public class IntelligenceService {
    private final AXIOM plugin;
    private final File intelligenceDir;
    private final Map<String, Map<String, IntelligenceReport>> reports = new HashMap<>(); // collector -> target -> report

    public static class IntelligenceReport {
        String targetNationId;
        double treasuryEstimate;
        int militaryStrength;
        int population;
        Set<String> knownAllies = new HashSet<>();
        long lastUpdated;
    }

    public IntelligenceService(AXIOM plugin) {
        this.plugin = plugin;
        this.intelligenceDir = new File(plugin.getDataFolder(), "intelligence");
        this.intelligenceDir.mkdirs();
        loadAll();
    }

    public synchronized void updateIntelligence(String collectorId, String targetId) {
        Nation target = plugin.getNationManager().getNationById(targetId);
        if (target == null) return;
        IntelligenceReport r = new IntelligenceReport();
        r.targetNationId = targetId;
        r.treasuryEstimate = target.getTreasury();
        r.militaryStrength = target.getCitizens().size(); // Simplified
        r.population = target.getCitizens().size();
        r.knownAllies.addAll(target.getAllies());
        r.lastUpdated = System.currentTimeMillis();
        reports.computeIfAbsent(collectorId, k -> new HashMap<>()).put(targetId, r);
        saveReport(collectorId, targetId, r);
    }

    public synchronized IntelligenceReport getReport(String collectorId, String targetId) {
        Map<String, IntelligenceReport> collectorReports = reports.get(collectorId);
        return collectorReports != null ? collectorReports.get(targetId) : null;
    }

    private void loadAll() {
        File[] files = intelligenceDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String[] parts = f.getName().replace(".json", "").split("_");
                if (parts.length != 2) continue;
                String collectorId = parts[0];
                String targetId = parts[1];
                IntelligenceReport report = new IntelligenceReport();
                report.targetNationId = targetId;
                report.treasuryEstimate = o.has("treasuryEstimate") ? o.get("treasuryEstimate").getAsDouble() : 0;
                report.militaryStrength = o.has("militaryStrength") ? o.get("militaryStrength").getAsInt() : 0;
                report.population = o.has("population") ? o.get("population").getAsInt() : 0;
                report.lastUpdated = o.has("lastUpdated") ? o.get("lastUpdated").getAsLong() : 0;
                if (o.has("knownAllies")) {
                    for (var e : o.getAsJsonArray("knownAllies")) {
                        report.knownAllies.add(e.getAsString());
                    }
                }
                reports.computeIfAbsent(collectorId, k -> new HashMap<>()).put(targetId, report);
            } catch (Exception ignored) {}
        }
    }

    private void saveReport(String collectorId, String targetId, IntelligenceReport r) {
        File f = new File(intelligenceDir, collectorId + "_" + targetId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("targetNationId", targetId);
        o.addProperty("treasuryEstimate", r.treasuryEstimate);
        o.addProperty("militaryStrength", r.militaryStrength);
        o.addProperty("population", r.population);
        o.addProperty("lastUpdated", r.lastUpdated);
        com.google.gson.JsonArray allies = new com.google.gson.JsonArray();
        for (String a : r.knownAllies) allies.add(a);
        o.add("knownAllies", allies);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive intelligence statistics.
     */
    public synchronized Map<String, Object> getIntelligenceStatistics(String collectorId) {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, IntelligenceReport> collectorReports = reports.get(collectorId);
        if (collectorReports == null) {
            stats.put("targets", 0);
            stats.put("reports", Collections.emptyList());
            return stats;
        }
        
        stats.put("targets", collectorReports.size());
        
        List<Map<String, Object>> reportList = new ArrayList<>();
        for (IntelligenceReport r : collectorReports.values()) {
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("targetNationId", r.targetNationId);
            reportData.put("treasuryEstimate", r.treasuryEstimate);
            reportData.put("militaryStrength", r.militaryStrength);
            reportData.put("population", r.population);
            reportData.put("knownAllies", new ArrayList<>(r.knownAllies));
            reportData.put("lastUpdated", r.lastUpdated);
            reportData.put("ageHours", (System.currentTimeMillis() - r.lastUpdated) / (1000 * 60 * 60));
            reportList.add(reportData);
        }
        stats.put("reports", reportList);
        
        return stats;
    }
    
    /**
     * Get intelligence summary for a target nation.
     */
    public synchronized Map<String, Object> getIntelligenceSummary(String collectorId, String targetId) {
        IntelligenceReport r = getReport(collectorId, targetId);
        if (r == null) return Collections.emptyMap();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("treasury", r.treasuryEstimate);
        summary.put("military", r.militaryStrength);
        summary.put("population", r.population);
        summary.put("allies", r.knownAllies.size());
        summary.put("knownAllies", new ArrayList<>(r.knownAllies));
        summary.put("dataAge", (System.currentTimeMillis() - r.lastUpdated) / (1000 * 60 * 60));
        
        // Calculate intelligence completeness (0-100%)
        double completeness = 50.0; // Base 50%
        if (r.treasuryEstimate > 0) completeness += 10;
        if (r.militaryStrength > 0) completeness += 10;
        if (r.population > 0) completeness += 10;
        if (!r.knownAllies.isEmpty()) completeness += 20;
        summary.put("completeness", Math.min(100, completeness));
        
        return summary;
    }
    
    /**
     * Get all targets for a collector nation.
     */
    public synchronized List<String> getIntelligenceTargets(String collectorId) {
        Map<String, IntelligenceReport> collectorReports = reports.get(collectorId);
        if (collectorReports == null) return Collections.emptyList();
        return new ArrayList<>(collectorReports.keySet());
    }
    
    /**
     * Check if intelligence is outdated (older than threshold).
     */
    public synchronized boolean isIntelligenceOutdated(String collectorId, String targetId, long hoursThreshold) {
        IntelligenceReport r = getReport(collectorId, targetId);
        if (r == null) return true;
        
        long ageHours = (System.currentTimeMillis() - r.lastUpdated) / (1000 * 60 * 60);
        return ageHours > hoursThreshold;
    }
    
    /**
     * Get global intelligence statistics.
     */
    public synchronized Map<String, Object> getGlobalIntelligenceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalReports = 0;
        Map<String, Integer> reportsByCollector = new HashMap<>();
        Map<String, Integer> reportsByTarget = new HashMap<>();
        
        for (Map.Entry<String, Map<String, IntelligenceReport>> entry : reports.entrySet()) {
            int count = entry.getValue().size();
            totalReports += count;
            reportsByCollector.put(entry.getKey(), count);
            for (String targetId : entry.getValue().keySet()) {
                reportsByTarget.put(targetId, reportsByTarget.getOrDefault(targetId, 0) + 1);
            }
        }
        
        stats.put("totalReports", totalReports);
        stats.put("reportsByCollector", reportsByCollector);
        stats.put("reportsByTarget", reportsByTarget);
        
        // Top collectors
        List<Map.Entry<String, Integer>> topCollectors = reportsByCollector.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topCollectors", topCollectors);
        
        // Most spied on nations
        List<Map.Entry<String, Integer>> mostSpiedOn = reportsByTarget.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostSpiedOn", mostSpiedOn);
        
        // Average report age
        long totalAge = 0;
        int reportCount = 0;
        long now = System.currentTimeMillis();
        for (Map<String, IntelligenceReport> collectorReports : reports.values()) {
            for (IntelligenceReport r : collectorReports.values()) {
                totalAge += (now - r.lastUpdated);
                reportCount++;
            }
        }
        stats.put("averageReportAgeHours", reportCount > 0 ? (totalAge / reportCount) / (1000 * 60 * 60) : 0);
        
        return stats;
    }
}

