package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Tracks racial discrimination policies in nations. */
public class RacialDiscriminationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File discriminationDir;
    private final Map<String, DiscriminationPolicy> policies = new HashMap<>(); // nationId -> policy

    public static class DiscriminationPolicy {
        Map<String, Integer> raceRestrictions = new HashMap<>(); // raceId -> restriction level (0-100)
        Set<String> bannedRaces = new HashSet<>();
        boolean allowAllRaces;
    }

    public RacialDiscriminationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.discriminationDir = new File(plugin.getDataFolder(), "discrimination");
        this.discriminationDir.mkdirs();
        loadAll();
    }

    public synchronized String setRaceRestriction(String nationId, String raceId, int level) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        DiscriminationPolicy policy = policies.computeIfAbsent(nationId, k -> new DiscriminationPolicy());
        policy.raceRestrictions.put(raceId, Math.max(0, Math.min(100, level)));
        if (level >= 90) {
            policy.bannedRaces.add(raceId); // Auto-ban at high restriction
        } else {
            policy.bannedRaces.remove(raceId);
        }
        n.getHistory().add("Ограничение для расы " + raceId + ": уровень " + level);
        try {
            nationManager.save(n);
            savePolicy(nationId, policy);
        } catch (Exception ignored) {}
        return "Ограничение установлено: уровень " + level;
    }

    public synchronized String banRace(String nationId, String raceId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        DiscriminationPolicy policy = policies.computeIfAbsent(nationId, k -> new DiscriminationPolicy());
        policy.bannedRaces.add(raceId);
        policy.raceRestrictions.put(raceId, 100);
        n.getHistory().add("Раса " + raceId + " запрещена");
        try {
            nationManager.save(n);
            savePolicy(nationId, policy);
        } catch (Exception ignored) {}
        return "Раса запрещена.";
    }

    public synchronized boolean canJoinNation(UUID playerId, String nationId) {
        String playerRace = plugin.getRaceService().getPlayerRace(playerId);
        DiscriminationPolicy policy = policies.get(nationId);
        if (policy == null || policy.allowAllRaces) return true;
        if (policy.bannedRaces.contains(playerRace)) return false;
        int restriction = policy.raceRestrictions.getOrDefault(playerRace, 0);
        return restriction < 50; // Can join if restriction < 50%
    }

    public synchronized double getRacialBonusPenalty(String nationId, String raceId) {
        DiscriminationPolicy policy = policies.get(nationId);
        if (policy == null) return 1.0;
        int restriction = policy.raceRestrictions.getOrDefault(raceId, 0);
        return 1.0 - (restriction * 0.01); // -1% per restriction level
    }

    private void loadAll() {
        File[] files = discriminationDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                DiscriminationPolicy policy = new DiscriminationPolicy();
                policy.allowAllRaces = o.has("allowAllRaces") && o.get("allowAllRaces").getAsBoolean();
                if (o.has("raceRestrictions")) {
                    JsonObject restrictions = o.getAsJsonObject("raceRestrictions");
                    for (var entry : restrictions.entrySet()) {
                        policy.raceRestrictions.put(entry.getKey(), entry.getValue().getAsInt());
                    }
                }
                if (o.has("bannedRaces")) {
                    for (var elem : o.getAsJsonArray("bannedRaces")) {
                        policy.bannedRaces.add(elem.getAsString());
                    }
                }
                policies.put(nationId, policy);
            } catch (Exception ignored) {}
        }
    }

    private void savePolicy(String nationId, DiscriminationPolicy policy) {
        File f = new File(discriminationDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("allowAllRaces", policy.allowAllRaces);
        JsonObject restrictions = new JsonObject();
        for (var entry : policy.raceRestrictions.entrySet()) {
            restrictions.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("raceRestrictions", restrictions);
        com.google.gson.JsonArray banned = new com.google.gson.JsonArray();
        for (String raceId : policy.bannedRaces) {
            banned.add(raceId);
        }
        o.add("bannedRaces", banned);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive racial discrimination statistics for a nation.
     */
    public synchronized Map<String, Object> getRacialDiscriminationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        DiscriminationPolicy policy = policies.get(nationId);
        if (policy == null || policy.allowAllRaces) {
            stats.put("hasPolicy", policy != null);
            stats.put("allowsAllRaces", true);
            stats.put("totalBannedRaces", 0);
            stats.put("totalRestrictions", 0);
            return stats;
        }
        
        int totalBanned = policy.bannedRaces.size();
        int totalRestrictions = policy.raceRestrictions.size();
        double averageRestriction = policy.raceRestrictions.values().stream()
            .mapToInt(Integer::intValue).average().orElse(0.0);
        
        stats.put("hasPolicy", true);
        stats.put("allowsAllRaces", false);
        stats.put("totalBannedRaces", totalBanned);
        stats.put("totalRestrictions", totalRestrictions);
        stats.put("averageRestriction", averageRestriction);
        stats.put("bannedRaces", new HashSet<>(policy.bannedRaces));
        stats.put("raceRestrictions", new HashMap<>(policy.raceRestrictions));
        
        // Discrimination rating
        String rating = "ТОЛЕРАНТНАЯ";
        if (averageRestriction >= 80) rating = "КРАЙНЕ ДИСКРИМИНАЦИОННАЯ";
        else if (averageRestriction >= 60) rating = "ВЫСОКАЯ ДИСКРИМИНАЦИЯ";
        else if (averageRestriction >= 40) rating = "УМЕРЕННАЯ ДИСКРИМИНАЦИЯ";
        else if (averageRestriction >= 20) rating = "НИЗКАЯ ДИСКРИМИНАЦИЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global racial discrimination statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalRacialDiscriminationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPolicies = policies.size();
        int allowsAllRaces = 0;
        int hasRestrictions = 0;
        int totalBannedRaces = 0;
        Map<String, Integer> bannedRacesByNation = new HashMap<>();
        Map<String, Double> averageRestrictionByNation = new HashMap<>();
        Map<String, Integer> racesBanned = new HashMap<>(); // raceId -> count of nations banning it
        
        for (Map.Entry<String, DiscriminationPolicy> entry : policies.entrySet()) {
            String nationId = entry.getKey();
            DiscriminationPolicy policy = entry.getValue();
            
            if (policy.allowAllRaces) {
                allowsAllRaces++;
            } else {
                hasRestrictions++;
                int banned = policy.bannedRaces.size();
                totalBannedRaces += banned;
                bannedRacesByNation.put(nationId, banned);
                
                double avgRestriction = policy.raceRestrictions.values().stream()
                    .mapToInt(Integer::intValue).average().orElse(0.0);
                averageRestrictionByNation.put(nationId, avgRestriction);
                
                for (String raceId : policy.bannedRaces) {
                    racesBanned.put(raceId, racesBanned.getOrDefault(raceId, 0) + 1);
                }
            }
        }
        
        stats.put("totalPolicies", totalPolicies);
        stats.put("allowsAllRaces", allowsAllRaces);
        stats.put("hasRestrictions", hasRestrictions);
        stats.put("totalBannedRaces", totalBannedRaces);
        stats.put("averageBannedPerNation", hasRestrictions > 0 ? (double) totalBannedRaces / hasRestrictions : 0);
        stats.put("bannedRacesByNation", bannedRacesByNation);
        stats.put("averageRestrictionByNation", averageRestrictionByNation);
        stats.put("racesBanned", racesBanned);
        
        // Average restriction level
        double totalRestriction = averageRestrictionByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("averageRestrictionLevel", hasRestrictions > 0 ? totalRestriction / hasRestrictions : 0);
        
        // Top nations by banned races (most discriminatory)
        List<Map.Entry<String, Integer>> topByBanned = bannedRacesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByBanned", topByBanned);
        
        // Most banned races
        List<Map.Entry<String, Integer>> topByRace = racesBanned.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRace", topByRace);
        
        // Discrimination distribution
        int extreme = 0, high = 0, moderate = 0, low = 0, tolerant = 0;
        for (Double restriction : averageRestrictionByNation.values()) {
            if (restriction >= 80) extreme++;
            else if (restriction >= 60) high++;
            else if (restriction >= 40) moderate++;
            else if (restriction >= 20) low++;
            else tolerant++;
        }
        tolerant += allowsAllRaces;
        
        Map<String, Integer> discriminationDistribution = new HashMap<>();
        discriminationDistribution.put("extreme", extreme);
        discriminationDistribution.put("high", high);
        discriminationDistribution.put("moderate", moderate);
        discriminationDistribution.put("low", low);
        discriminationDistribution.put("tolerant", tolerant);
        stats.put("discriminationDistribution", discriminationDistribution);
        
        return stats;
    }
}

