package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Tracks war crimes and violations of international law. */
public class WarCrimeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File crimesDir;
    private final Map<String, List<WarCrime>> nationCrimes = new HashMap<>(); // nationId -> crimes

    public static class WarCrime {
        String type; // "genocide", "torture", "pillage", "civilian_targeting"
        String description;
        long timestamp;
        double severity;
        String accuserNationId;
    }

    public WarCrimeService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.crimesDir = new File(plugin.getDataFolder(), "warcrimes");
        this.crimesDir.mkdirs();
        loadAll();
    }

    public synchronized String reportWarCrime(String accuserId, String targetId, String type, String description, double severity) {
        Nation accuser = nationManager.getNationById(accuserId);
        Nation target = nationManager.getNationById(targetId);
        if (accuser == null || target == null) return "Нация не найдена.";
        WarCrime crime = new WarCrime();
        crime.type = type;
        crime.description = description;
        crime.timestamp = System.currentTimeMillis();
        crime.severity = Math.max(0, Math.min(100, severity));
        crime.accuserNationId = accuserId;
        nationCrimes.computeIfAbsent(targetId, k -> new ArrayList<>()).add(crime);
        // Apply penalties
        try {
            plugin.getDiplomacySystem().setReputation(target, accuser, -10); // Reputation hit
        } catch (Exception ignored) {}
        target.getHistory().add("Обвинение в военных преступлениях: " + type);
        try {
            nationManager.save(target);
            saveCrimes(targetId);
        } catch (Exception ignored) {}
        return "Военное преступление зафиксировано.";
    }

    public synchronized double getWarCrimeLevel(String nationId) {
        List<WarCrime> crimes = nationCrimes.get(nationId);
        if (crimes == null) return 0.0;
        return crimes.stream().mapToDouble(c -> c.severity).sum();
    }

    private void loadAll() {
        File[] files = crimesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                List<WarCrime> crimes = new ArrayList<>();
                if (o.has("crimes")) {
                    for (var elem : o.getAsJsonArray("crimes")) {
                        JsonObject cObj = elem.getAsJsonObject();
                        WarCrime crime = new WarCrime();
                        crime.type = cObj.get("type").getAsString();
                        crime.description = cObj.get("description").getAsString();
                        crime.timestamp = cObj.get("timestamp").getAsLong();
                        crime.severity = cObj.get("severity").getAsDouble();
                        crime.accuserNationId = cObj.get("accuserNationId").getAsString();
                        crimes.add(crime);
                    }
                }
                nationCrimes.put(nationId, crimes);
            } catch (Exception ignored) {}
        }
    }

    private void saveCrimes(String nationId) {
        File f = new File(crimesDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<WarCrime> crimes = nationCrimes.get(nationId);
        if (crimes != null) {
            for (WarCrime crime : crimes) {
                JsonObject cObj = new JsonObject();
                cObj.addProperty("type", crime.type);
                cObj.addProperty("description", crime.description);
                cObj.addProperty("timestamp", crime.timestamp);
                cObj.addProperty("severity", crime.severity);
                cObj.addProperty("accuserNationId", crime.accuserNationId);
                arr.add(cObj);
            }
        }
        o.add("crimes", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive war crime statistics for a nation.
     */
    public synchronized Map<String, Object> getWarCrimeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<WarCrime> crimes = nationCrimes.get(nationId);
        if (crimes == null || crimes.isEmpty()) {
            stats.put("hasCrimes", false);
            stats.put("totalCrimes", 0);
            stats.put("warCrimeLevel", 0.0);
            return stats;
        }
        
        int total = crimes.size();
        double totalSeverity = crimes.stream().mapToDouble(c -> c.severity).sum();
        Map<String, Integer> crimesByType = new HashMap<>();
        Map<String, Integer> crimesByAccuser = new HashMap<>();
        
        for (WarCrime crime : crimes) {
            crimesByType.put(crime.type, crimesByType.getOrDefault(crime.type, 0) + 1);
            crimesByAccuser.put(crime.accuserNationId,
                crimesByAccuser.getOrDefault(crime.accuserNationId, 0) + 1);
        }
        
        stats.put("hasCrimes", true);
        stats.put("totalCrimes", total);
        stats.put("warCrimeLevel", totalSeverity);
        stats.put("averageSeverity", total > 0 ? totalSeverity / total : 0);
        stats.put("crimesByType", crimesByType);
        stats.put("crimesByAccuser", crimesByAccuser);
        stats.put("uniqueAccusers", crimesByAccuser.size());
        
        // Crime rating
        String rating = "ЧИСТЫЙ";
        if (totalSeverity >= 500) rating = "ВОЕННЫЙ ПРЕСТУПНИК";
        else if (totalSeverity >= 300) rating = "СЕРЬЕЗНЫЙ";
        else if (totalSeverity >= 150) rating = "ПОДОЗРИТЕЛЬНЫЙ";
        else if (totalSeverity >= 50) rating = "МИНОРНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global war crime statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalWarCrimeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalCrimes = 0;
        double totalSeverity = 0.0;
        Map<String, Integer> crimesByNation = new HashMap<>();
        Map<String, Double> severityByNation = new HashMap<>();
        Map<String, Integer> crimesByType = new HashMap<>();
        Map<String, Integer> accusationsByNation = new HashMap<>(); // nations that accused
        
        for (Map.Entry<String, List<WarCrime>> entry : nationCrimes.entrySet()) {
            String nationId = entry.getKey();
            List<WarCrime> crimes = entry.getValue();
            
            int count = crimes.size();
            double severity = crimes.stream().mapToDouble(c -> c.severity).sum();
            
            totalCrimes += count;
            totalSeverity += severity;
            crimesByNation.put(nationId, count);
            severityByNation.put(nationId, severity);
            
            for (WarCrime crime : crimes) {
                crimesByType.put(crime.type, crimesByType.getOrDefault(crime.type, 0) + 1);
                accusationsByNation.put(crime.accuserNationId,
                    accusationsByNation.getOrDefault(crime.accuserNationId, 0) + 1);
            }
        }
        
        stats.put("totalCrimes", totalCrimes);
        stats.put("totalSeverity", totalSeverity);
        stats.put("averageSeverity", totalCrimes > 0 ? totalSeverity / totalCrimes : 0);
        stats.put("crimesByNation", crimesByNation);
        stats.put("severityByNation", severityByNation);
        stats.put("crimesByType", crimesByType);
        stats.put("accusationsByNation", accusationsByNation);
        stats.put("nationsWithCrimes", crimesByNation.size());
        stats.put("nationsAccusing", accusationsByNation.size());
        
        // Top nations by crimes (most accused)
        List<Map.Entry<String, Integer>> topByCrimes = crimesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCrimes", topByCrimes);
        
        // Top nations by severity
        List<Map.Entry<String, Double>> topBySeverity = severityByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySeverity", topBySeverity);
        
        // Most common crime types
        List<Map.Entry<String, Integer>> topByType = crimesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByType", topByType);
        
        return stats;
    }
}

