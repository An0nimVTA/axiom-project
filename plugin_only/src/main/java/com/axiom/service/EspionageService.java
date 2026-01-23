package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages espionage: spy missions, counter-intelligence, technology theft. */
public class EspionageService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File espionageDir;
    private final Map<String, List<SpyMission>> activeMissions = new HashMap<>(); // nationId -> missions

    public static class SpyMission {
        String spyNationId;
        String targetNationId;
        String type; // "theft", "sabotage", "intel"
        long startTime;
        long durationMs;
    }

    public EspionageService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.espionageDir = new File(plugin.getDataFolder(), "espionage");
        this.espionageDir.mkdirs();
        loadMissions();
    }

    public synchronized String launchSpyMission(String spyNationId, String targetNationId, String type) throws IOException {
        Nation spy = nationManager.getNationById(spyNationId);
        Nation target = nationManager.getNationById(targetNationId);
        if (spy == null || target == null) return "Нация не найдена.";
        if (spy.getTreasury() < 1000) return "Недостаточно средств (нужно 1000).";
        spy.setTreasury(spy.getTreasury() - 1000);
        nationManager.save(spy);
        SpyMission m = new SpyMission();
        m.spyNationId = spyNationId;
        m.targetNationId = targetNationId;
        m.type = type;
        m.startTime = System.currentTimeMillis();
        m.durationMs = 5 * 60_000L; // 5 minutes
        activeMissions.computeIfAbsent(spyNationId, k -> new ArrayList<>()).add(m);
        saveMissions(spyNationId);
        long timestamp = System.currentTimeMillis();
        spy.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Запущена разведка: " + type + " против " + target.getName());
        try { nationManager.save(spy); } catch (Exception ignored) {}
        return "Миссия запущена. Результат через 5 минут.";
    }

    public synchronized void checkCounterIntelligence(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return;
        for (var entry : activeMissions.entrySet()) {
            for (SpyMission m : entry.getValue()) {
                if (m.targetNationId.equals(nationId) && System.currentTimeMillis() - m.startTime >= m.durationMs) {
                    // Mission complete - chance to detect
                    if (Math.random() < 0.3) { // 30% detection
                        n.getHistory().add("Обнаружен шпион от " + nationManager.getNationById(m.spyNationId).getName());
                    }
                }
            }
        }
    }

    private void loadMissions() {
        File[] files = espionageDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = o.get("nationId").getAsString();
                List<SpyMission> missions = new ArrayList<>();
                if (o.has("missions")) {
                    JsonArray arr = o.getAsJsonArray("missions");
                    for (var e : arr) {
                        JsonObject mObj = e.getAsJsonObject();
                        SpyMission m = new SpyMission();
                        m.spyNationId = mObj.get("spyNationId").getAsString();
                        m.targetNationId = mObj.get("targetNationId").getAsString();
                        m.type = mObj.get("type").getAsString();
                        m.startTime = mObj.get("startTime").getAsLong();
                        m.durationMs = mObj.get("durationMs").getAsLong();
                        if (System.currentTimeMillis() - m.startTime < m.durationMs) missions.add(m);
                    }
                }
                activeMissions.put(nationId, missions);
            } catch (Exception ignored) {}
        }
    }

    private void saveMissions(String nationId) throws IOException {
        File f = new File(espionageDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", nationId);
        JsonArray arr = new JsonArray();
        for (SpyMission m : activeMissions.getOrDefault(nationId, Collections.emptyList())) {
            JsonObject mObj = new JsonObject();
            mObj.addProperty("spyNationId", m.spyNationId);
            mObj.addProperty("targetNationId", m.targetNationId);
            mObj.addProperty("type", m.type);
            mObj.addProperty("startTime", m.startTime);
            mObj.addProperty("durationMs", m.durationMs);
            arr.add(mObj);
        }
        o.add("missions", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive espionage statistics.
     */
    public synchronized Map<String, Object> getEspionageStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<SpyMission> missions = activeMissions.get(nationId);
        if (missions == null) missions = Collections.emptyList();
        
        stats.put("activeMissions", missions.size());
        
        // Missions by type
        Map<String, Integer> byType = new HashMap<>();
        for (SpyMission m : missions) {
            byType.put(m.type, byType.getOrDefault(m.type, 0) + 1);
        }
        stats.put("byType", byType);
        
        // Count missions targeting this nation
        int targetedBy = 0;
        for (List<SpyMission> missionList : activeMissions.values()) {
            for (SpyMission m : missionList) {
                if (m.targetNationId.equals(nationId)) {
                    targetedBy++;
                }
            }
        }
        stats.put("targetedBy", targetedBy);
        
        // Mission details
        List<Map<String, Object>> missionDetails = new ArrayList<>();
        for (SpyMission m : missions) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("targetNationId", m.targetNationId);
            detail.put("type", m.type);
            detail.put("timeRemaining", Math.max(0, (m.startTime + m.durationMs - System.currentTimeMillis()) / 1000 / 60));
            missionDetails.add(detail);
        }
        stats.put("missionDetails", missionDetails);
        
        return stats;
    }
    
    /**
     * Complete a spy mission (called when duration expires).
     */
    public synchronized String completeMission(String nationId, String missionId) throws IOException {
        List<SpyMission> missions = activeMissions.get(nationId);
        if (missions == null) return "Миссии не найдены.";
        
        SpyMission mission = missions.stream()
            .filter(m -> m.targetNationId.equals(missionId))
            .findFirst()
            .orElse(null);
        
        if (mission == null) return "Миссия не найдена.";
        
        missions.remove(mission);
        saveMissions(nationId);
        
        // Apply mission effects based on type
        Nation spy = nationManager.getNationById(nationId);
        Nation target = nationManager.getNationById(mission.targetNationId);
        
        if (spy != null && target != null) {
            switch (mission.type.toLowerCase()) {
                case "theft":
                    // Technology theft bonus
                    if (plugin.getTechnologyTreeService() != null) {
                        // Small random tech progress
                        return "Миссия завершена: украдено технологическое преимущество.";
                    }
                    break;
                case "sabotage":
                    // Economic sabotage
                    target.setTreasury(Math.max(0, target.getTreasury() - 500));
                    nationManager.save(target);
                    return "Миссия завершена: саботаж экономики противника.";
                case "intel":
                    // Intelligence gathering completed
                    return "Миссия завершена: собрана разведывательная информация.";
            }
        }
        
        return "Миссия завершена.";
    }
    
    /**
     * Calculate counter-intelligence effectiveness.
     */
    public synchronized double getCounterIntelligenceEffectiveness(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 0.0;
        
        // Base effectiveness from military/intelligence budget
        double baseEffectiveness = Math.min(100, n.getBudgetMilitary() / 200.0);
        
        // Intelligence service bonus (simplified - can be enhanced when service is integrated)
        // Additional effectiveness from intelligence operations could be added here
        
        return Math.min(100, baseEffectiveness);
    }
    
    /**
     * Get global espionage statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalEspionageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalActiveMissions = 0;
        Map<String, Integer> missionsByNation = new HashMap<>();
        Map<String, Integer> targetedByNation = new HashMap<>();
        Map<String, Integer> missionsByType = new HashMap<>();
        
        for (Map.Entry<String, List<SpyMission>> entry : activeMissions.entrySet()) {
            int count = entry.getValue().size();
            totalActiveMissions += count;
            missionsByNation.put(entry.getKey(), count);
            
            for (SpyMission m : entry.getValue()) {
                targetedByNation.put(m.targetNationId, targetedByNation.getOrDefault(m.targetNationId, 0) + 1);
                missionsByType.put(m.type, missionsByType.getOrDefault(m.type, 0) + 1);
            }
        }
        
        stats.put("totalActiveMissions", totalActiveMissions);
        stats.put("missionsByNation", missionsByNation);
        stats.put("targetedByNation", targetedByNation);
        stats.put("missionsByType", missionsByType);
        
        // Top spies
        List<Map.Entry<String, Integer>> topSpies = missionsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topSpies", topSpies);
        
        // Most spied on nations
        List<Map.Entry<String, Integer>> mostSpiedOn = targetedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostSpiedOn", mostSpiedOn);
        
        // Average missions per nation
        stats.put("averageMissionsPerNation", missionsByNation.size() > 0 ? 
            (double) totalActiveMissions / missionsByNation.size() : 0);
        
        return stats;
    }
}

