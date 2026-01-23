package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages diplomatic missions and embassies. */
public class DiplomaticMissionService {
    private final AXIOM plugin;
    private final File missionsDir;
    private final Map<String, List<Mission>> nationMissions = new HashMap<>(); // nationId -> missions

    public static class Mission {
        String id;
        String sendingNationId;
        String receivingNationId;
        String type; // "embassy", "trade_delegation", "military_attaché"
        long establishedAt;
        boolean active;
    }

    public DiplomaticMissionService(AXIOM plugin) {
        this.plugin = plugin;
        this.missionsDir = new File(plugin.getDataFolder(), "diplomaticmissions");
        this.missionsDir.mkdirs();
        loadAll();
    }

    public synchronized String establishMission(String sendingId, String receivingId, String type) throws IOException {
        if (sendingId == null || receivingId == null) return "Неверные параметры.";
        if (sendingId.equals(receivingId)) return "Нельзя открыть миссию в своей нации.";
        if (type == null || type.trim().isEmpty()) return "Тип миссии не указан.";
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        String normalizedType = type.trim().toLowerCase(java.util.Locale.ROOT);
        Nation sending = plugin.getNationManager().getNationById(sendingId);
        Nation receiving = plugin.getNationManager().getNationById(receivingId);
        if (sending == null || receiving == null) return "Нация не найдена.";
        double cost = normalizedType.equals("embassy") ? 5000 : 2000;
        if (sending.getTreasury() < cost) return "Недостаточно средств.";
        String id = UUID.randomUUID().toString();
        Mission m = new Mission();
        m.id = id;
        m.sendingNationId = sendingId;
        m.receivingNationId = receivingId;
        m.type = normalizedType;
        m.establishedAt = System.currentTimeMillis();
        m.active = true;
        sending.setTreasury(sending.getTreasury() - cost);
        nationMissions.computeIfAbsent(sendingId, k -> new ArrayList<>()).add(m);
        plugin.getNationManager().save(sending);
        saveMission(m);
        return "Миссия установлена: " + type;
    }

    public synchronized double getDiplomaticBonus(String nationA, String nationB) {
        List<Mission> missions = nationMissions.get(nationA);
        if (missions == null) return 1.0;
        boolean hasEmbassy = missions.stream()
            .anyMatch(m -> m.active && m.receivingNationId.equals(nationB) && "embassy".equals(m.type));
        return hasEmbassy ? 1.2 : 1.0; // 20% bonus
    }

    private void loadAll() {
        File[] files = missionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Mission m = new Mission();
                m.id = o.get("id").getAsString();
                m.sendingNationId = o.get("sendingNationId").getAsString();
                m.receivingNationId = o.get("receivingNationId").getAsString();
                m.type = o.get("type").getAsString();
                m.establishedAt = o.get("establishedAt").getAsLong();
                m.active = o.has("active") && o.get("active").getAsBoolean();
                nationMissions.computeIfAbsent(m.sendingNationId, k -> new ArrayList<>()).add(m);
            } catch (Exception ignored) {}
        }
    }

    private void saveMission(Mission m) throws IOException {
        File f = new File(missionsDir, m.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", m.id);
        o.addProperty("sendingNationId", m.sendingNationId);
        o.addProperty("receivingNationId", m.receivingNationId);
        o.addProperty("type", m.type);
        o.addProperty("establishedAt", m.establishedAt);
        o.addProperty("active", m.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive diplomatic mission statistics.
     */
    public synchronized Map<String, Object> getMissionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Mission> missions = nationMissions.get(nationId);
        if (missions == null) missions = Collections.emptyList();
        
        stats.put("totalMissions", missions.size());
        stats.put("activeMissions", missions.stream().filter(m -> m.active).count());
        
        // Missions by type
        Map<String, Integer> byType = new HashMap<>();
        for (Mission m : missions) {
            if (m.active) {
                byType.put(m.type, byType.getOrDefault(m.type, 0) + 1);
            }
        }
        stats.put("byType", byType);
        
        // Mission details
        List<Map<String, Object>> missionsList = new ArrayList<>();
        for (Mission m : missions) {
            Map<String, Object> missionData = new HashMap<>();
            missionData.put("id", m.id);
            missionData.put("type", m.type);
            missionData.put("receivingNation", m.receivingNationId);
            missionData.put("establishedAt", m.establishedAt);
            missionData.put("age", (System.currentTimeMillis() - m.establishedAt) / 1000 / 60 / 60 / 24); // days
            missionData.put("isActive", m.active);
            missionsList.add(missionData);
        }
        stats.put("missions", missionsList);
        
        // Mission rating
        String rating = "НЕТ МИССИЙ";
        if (missions.size() >= 20) rating = "ГЛОБАЛЬНАЯ ДИПЛОМАТИЯ";
        else if (missions.size() >= 10) rating = "РАСШИРЕННАЯ СЕТЬ";
        else if (missions.size() >= 5) rating = "АКТИВНАЯ";
        else if (missions.size() >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Close mission.
     */
    public synchronized String closeMission(String nationId, String missionId) throws IOException {
        List<Mission> missions = nationMissions.get(nationId);
        if (missions == null) return "Миссия не найдена.";
        
        Mission m = missions.stream()
            .filter(mission -> mission.id.equals(missionId))
            .findFirst()
            .orElse(null);
        
        if (m == null) return "Миссия не найдена.";
        
        m.active = false;
        saveMission(m);
        
        return "Миссия закрыта.";
    }
    
    /**
     * Get active missions for nation.
     */
    public synchronized List<Mission> getActiveMissions(String nationId) {
        List<Mission> missions = nationMissions.get(nationId);
        if (missions == null) return Collections.emptyList();
        return missions.stream()
            .filter(m -> m.active)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Check if embassy exists between nations.
     */
    public synchronized boolean hasEmbassy(String nationA, String nationB) {
        List<Mission> missions = nationMissions.get(nationA);
        if (missions == null) return false;
        return missions.stream()
            .anyMatch(m -> m.receivingNationId.equals(nationB) 
                && m.type.equals("embassy") 
                && m.active);
    }
    
    /**
     * Calculate total diplomatic bonus from missions.
     */
    public synchronized double getTotalDiplomaticBonus(String nationId) {
        long embassyCount = getActiveMissions(nationId).stream()
            .filter(m -> m.type.equals("embassy"))
            .count();
        
        // +5% per embassy (capped at +50%)
        return 1.0 + Math.min(0.50, embassyCount * 0.05);
    }
    
    /**
     * Get global diplomatic mission statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalDiplomaticMissionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalMissions = 0;
        int activeMissions = 0;
        Map<String, Integer> missionsByNation = new HashMap<>();
        Map<String, Integer> missionsByType = new HashMap<>();
        Map<String, Integer> missionsByReceiving = new HashMap<>();
        
        for (Map.Entry<String, List<Mission>> entry : nationMissions.entrySet()) {
            List<Mission> missions = entry.getValue();
            int nationTotal = missions.size();
            int nationActive = (int) missions.stream().filter(m -> m.active).count();
            
            totalMissions += nationTotal;
            activeMissions += nationActive;
            missionsByNation.put(entry.getKey(), nationTotal);
            
            for (Mission m : missions) {
                if (m.active) {
                    missionsByType.put(m.type, missionsByType.getOrDefault(m.type, 0) + 1);
                    missionsByReceiving.put(m.receivingNationId, 
                        missionsByReceiving.getOrDefault(m.receivingNationId, 0) + 1);
                }
            }
        }
        
        stats.put("totalMissions", totalMissions);
        stats.put("activeMissions", activeMissions);
        stats.put("missionsByNation", missionsByNation);
        stats.put("missionsByType", missionsByType);
        stats.put("missionsByReceiving", missionsByReceiving);
        stats.put("nationsWithMissions", missionsByNation.size());
        stats.put("nationsReceivingMissions", missionsByReceiving.size());
        
        // Top nations by missions
        List<Map.Entry<String, Integer>> topByMissions = missionsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMissions", topByMissions);
        
        // Most common mission types
        List<Map.Entry<String, Integer>> mostCommonTypes = missionsByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTypes", mostCommonTypes);
        
        // Most receiving nations
        List<Map.Entry<String, Integer>> mostReceiving = missionsByReceiving.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostReceiving", mostReceiving);
        
        // Average missions per nation
        stats.put("averageMissionsPerNation", missionsByNation.size() > 0 ? 
            (double) totalMissions / missionsByNation.size() : 0);
        
        // Embassy count
        int totalEmbassies = missionsByType.getOrDefault("embassy", 0);
        stats.put("totalEmbassies", totalEmbassies);
        
        return stats;
    }
}

