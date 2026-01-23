package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages emergency declarations and special powers. */
public class EmergencyService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File emergenciesDir;
    private final Map<String, Emergency> activeEmergencies = new HashMap<>(); // nationId -> emergency

    public static class Emergency {
        String nationId;
        String type; // "war", "disaster", "economic", "pandemic"
        long declaredAt;
        long expiresAt;
        boolean active;
        Map<String, Object> powers = new HashMap<>(); // special powers granted
    }

    public EmergencyService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.emergenciesDir = new File(plugin.getDataFolder(), "emergencies");
        this.emergenciesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkExpiry, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String declareEmergency(String nationId, String type, int durationHours) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (activeEmergencies.containsKey(nationId)) return "Чрезвычайное положение уже активно.";
        Emergency e = new Emergency();
        e.nationId = nationId;
        e.type = type;
        e.declaredAt = System.currentTimeMillis();
        e.expiresAt = System.currentTimeMillis() + durationHours * 60 * 60_000L;
        e.active = true;
        // Grant special powers based on type
        switch (type.toLowerCase()) {
            case "war":
                e.powers.put("conscription", true);
                e.powers.put("taxIncrease", 1.5); // 50% tax increase
                break;
            case "disaster":
                e.powers.put("resourceAllocation", true);
                e.powers.put("priceControl", true);
                break;
            case "economic":
                e.powers.put("currencyControl", true);
                e.powers.put("tradeRestriction", true);
                break;
            case "pandemic":
                e.powers.put("travelBan", true);
                e.powers.put("healthcarePriority", true);
                break;
        }
        activeEmergencies.put(nationId, e);
        n.getHistory().add("Объявлено чрезвычайное положение: " + type);
        try {
            nationManager.save(n);
            saveEmergency(e);
        } catch (Exception ignored) {}
        return "Чрезвычайное положение объявлено: " + type;
    }

    private void checkExpiry() {
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, Emergency> e : activeEmergencies.entrySet()) {
            if (e.getValue().expiresAt <= now) {
                expired.add(e.getKey());
            }
        }
        for (String nationId : expired) {
            Emergency e = activeEmergencies.remove(nationId);
            e.active = false;
            Nation n = nationManager.getNationById(nationId);
            if (n != null) {
                n.getHistory().add("Чрезвычайное положение завершено.");
                try { nationManager.save(n); } catch (Exception ignored) {}
            }
        }
    }

    public synchronized boolean isEmergencyActive(String nationId) {
        Emergency e = activeEmergencies.get(nationId);
        return e != null && e.active;
    }

    private void loadAll() {
        File[] files = emergenciesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Emergency e = new Emergency();
                e.nationId = o.get("nationId").getAsString();
                e.type = o.get("type").getAsString();
                e.declaredAt = o.get("declaredAt").getAsLong();
                e.expiresAt = o.get("expiresAt").getAsLong();
                e.active = o.has("active") && o.get("active").getAsBoolean() && e.expiresAt > System.currentTimeMillis();
                if (e.active) activeEmergencies.put(e.nationId, e);
            } catch (Exception ignored) {}
        }
    }

    private void saveEmergency(Emergency e) {
        File f = new File(emergenciesDir, e.nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", e.nationId);
        o.addProperty("type", e.type);
        o.addProperty("declaredAt", e.declaredAt);
        o.addProperty("expiresAt", e.expiresAt);
        o.addProperty("active", e.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive emergency statistics for a nation.
     */
    public synchronized Map<String, Object> getEmergencyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Emergency emergency = activeEmergencies.get(nationId);
        if (emergency == null || !emergency.active) {
            stats.put("hasActiveEmergency", false);
            return stats;
        }
        
        long now = System.currentTimeMillis();
        long remainingMinutes = Math.max(0, (emergency.expiresAt - now) / (60 * 1000));
        
        stats.put("hasActiveEmergency", true);
        stats.put("type", emergency.type);
        stats.put("declaredAt", emergency.declaredAt);
        stats.put("expiresAt", emergency.expiresAt);
        stats.put("remainingMinutes", remainingMinutes);
        stats.put("powers", new HashMap<>(emergency.powers));
        
        // Emergency rating
        String rating = "КРИТИЧЕСКОЕ";
        if (emergency.type.equals("pandemic")) rating = "ПАНДЕМИЯ";
        else if (emergency.type.equals("war")) rating = "ВОЕННОЕ";
        else if (emergency.type.equals("economic")) rating = "ЭКОНОМИЧЕСКОЕ";
        else if (emergency.type.equals("disaster")) rating = "КАТАСТРОФА";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global emergency statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalEmergencyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long now = System.currentTimeMillis();
        int totalActive = 0;
        Map<String, Integer> emergenciesByNation = new HashMap<>();
        Map<String, Integer> emergenciesByType = new HashMap<>();
        
        for (Emergency emergency : activeEmergencies.values()) {
            if (emergency.active && now < emergency.expiresAt) {
                totalActive++;
                emergenciesByNation.put(emergency.nationId,
                    emergenciesByNation.getOrDefault(emergency.nationId, 0) + 1);
                emergenciesByType.put(emergency.type,
                    emergenciesByType.getOrDefault(emergency.type, 0) + 1);
            }
        }
        
        stats.put("totalActiveEmergencies", totalActive);
        stats.put("emergenciesByNation", emergenciesByNation);
        stats.put("emergenciesByType", emergenciesByType);
        stats.put("nationsWithEmergencies", emergenciesByNation.size());
        
        // Top nations with emergencies
        List<Map.Entry<String, Integer>> topByEmergencies = emergenciesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByEmergencies", topByEmergencies);
        
        // Most common emergency types
        List<Map.Entry<String, Integer>> topByType = emergenciesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByType", topByType);
        
        // Emergency type distribution
        stats.put("typeDistribution", emergenciesByType);
        
        return stats;
    }
}

