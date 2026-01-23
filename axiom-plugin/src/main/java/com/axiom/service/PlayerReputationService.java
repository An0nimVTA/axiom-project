package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.UUID;

/** Manages player-to-player reputation system. */
public class PlayerReputationService {
    private final AXIOM plugin;
    private final File repDir;
    private final Map<UUID, Map<UUID, Integer>> playerReputation = new HashMap<>(); // player -> target -> rep (-100..+100)

    public PlayerReputationService(AXIOM plugin) {
        this.plugin = plugin;
        this.repDir = new File(plugin.getDataFolder(), "playerreputation");
        this.repDir.mkdirs();
        loadAll();
    }

    public synchronized void modifyReputation(UUID from, UUID to, int delta) {
        Map<UUID, Integer> rep = playerReputation.computeIfAbsent(from, k -> new HashMap<>());
        int current = rep.getOrDefault(to, 0);
        int newValue = Math.max(-100, Math.min(100, current + delta));
        rep.put(to, newValue);
        saveReputation(from);
    }

    public synchronized void setReputation(UUID from, UUID to, int value) {
        Map<UUID, Integer> rep = playerReputation.computeIfAbsent(from, k -> new HashMap<>());
        rep.put(to, Math.max(-100, Math.min(100, value)));
        saveReputation(from);
    }

    public synchronized int getReputation(UUID from, UUID to) {
        Map<UUID, Integer> rep = playerReputation.get(from);
        return rep != null ? rep.getOrDefault(to, 0) : 0;
    }

    public synchronized String getReputationLevel(UUID from, UUID to) {
        int rep = getReputation(from, to);
        if (rep >= 80) return "§aДружелюбный";
        if (rep >= 40) return "§bПоложительный";
        if (rep >= -40) return "§fНейтральный";
        if (rep >= -80) return "§eНегативный";
        return "§cВраждебный";
    }

    private void loadAll() {
        File[] files = repDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                UUID playerId = UUID.fromString(f.getName().replace(".json", ""));
                Map<UUID, Integer> rep = new HashMap<>();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : o.entrySet()) {
                    rep.put(UUID.fromString(entry.getKey()), entry.getValue().getAsInt());
                }
                playerReputation.put(playerId, rep);
            } catch (Exception ignored) {}
        }
    }

    private void saveReputation(UUID playerId) {
        File f = new File(repDir, playerId.toString() + ".json");
        JsonObject o = new JsonObject();
        Map<UUID, Integer> rep = playerReputation.get(playerId);
        if (rep != null) {
            for (var entry : rep.entrySet()) {
                o.addProperty(entry.getKey().toString(), entry.getValue());
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive reputation statistics.
     */
    public synchronized Map<String, Object> getReputationStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        Map<UUID, Integer> reputation = playerReputation.get(playerId);
        if (reputation == null) reputation = Collections.emptyMap();
        
        stats.put("totalRelations", reputation.size());
        
        // Reputation distribution
        int friendly = 0, positive = 0, neutral = 0, negative = 0, hostile = 0;
        double averageRep = 0.0;
        
        for (int rep : reputation.values()) {
            averageRep += rep;
            if (rep >= 80) friendly++;
            else if (rep >= 40) positive++;
            else if (rep >= -40) neutral++;
            else if (rep >= -80) negative++;
            else hostile++;
        }
        
        if (reputation.size() > 0) averageRep /= reputation.size();
        
        stats.put("averageReputation", averageRep);
        stats.put("friendlyCount", friendly);
        stats.put("positiveCount", positive);
        stats.put("neutralCount", neutral);
        stats.put("negativeCount", negative);
        stats.put("hostileCount", hostile);
        
        // Overall rating
        String rating = "НЕЙТРАЛЬНАЯ";
        if (averageRep >= 60) rating = "ОТЛИЧНАЯ";
        else if (averageRep >= 30) rating = "ХОРОШАЯ";
        else if (averageRep >= -30) rating = "НЕЙТРАЛЬНАЯ";
        else if (averageRep >= -60) rating = "ПЛОХАЯ";
        else rating = "УЖАСНАЯ";
        stats.put("overallRating", rating);
        
        return stats;
    }
    
    /**
     * Get top players by reputation (most liked/disliked).
     */
    public synchronized List<Map.Entry<UUID, Double>> getTopPlayersByReputation(int limit, boolean top) {
        Map<UUID, List<Integer>> repByTarget = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, Integer>> entry : playerReputation.entrySet()) {
            for (Map.Entry<UUID, Integer> repEntry : entry.getValue().entrySet()) {
                repByTarget.computeIfAbsent(repEntry.getKey(), k -> new ArrayList<>()).add(repEntry.getValue());
            }
        }
        
        Map<UUID, Double> avgRep = new HashMap<>();
        for (Map.Entry<UUID, List<Integer>> entry : repByTarget.entrySet()) {
            double avg = 0.0;
            for (int rep : entry.getValue()) {
                avg += rep;
            }
            if (!entry.getValue().isEmpty()) {
                avg /= entry.getValue().size();
                avgRep.put(entry.getKey(), avg);
            }
        }
        
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(avgRep.entrySet());
        sorted.sort((a, b) -> top ? 
            Double.compare(b.getValue(), a.getValue()) : 
            Double.compare(a.getValue(), b.getValue()));
        
        return sorted.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get mutual reputation between two players.
     */
    public synchronized Map<String, Integer> getMutualReputation(UUID playerA, UUID playerB) {
        Map<String, Integer> mutual = new HashMap<>();
        mutual.put("AtoB", getReputation(playerA, playerB));
        mutual.put("BtoA", getReputation(playerB, playerA));
        return mutual;
    }
    
    /**
     * Get global player reputation statistics across all players.
     */
    public synchronized Map<String, Object> getGlobalReputationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPlayers = playerReputation.size();
        int totalRelations = 0;
        double totalAverageRep = 0.0;
        
        // Reputation distribution across all players
        int friendly = 0, positive = 0, neutral = 0, negative = 0, hostile = 0;
        
        Map<UUID, Double> playerAverages = new HashMap<>();
        
        for (Map.Entry<UUID, Map<UUID, Integer>> entry : playerReputation.entrySet()) {
            Map<UUID, Integer> reputation = entry.getValue();
            totalRelations += reputation.size();
            
            double playerAvg = 0.0;
            for (int rep : reputation.values()) {
                playerAvg += rep;
                if (rep >= 80) friendly++;
                else if (rep >= 40) positive++;
                else if (rep >= -40) neutral++;
                else if (rep >= -80) negative++;
                else hostile++;
            }
            
            if (!reputation.isEmpty()) {
                playerAvg /= reputation.size();
                playerAverages.put(entry.getKey(), playerAvg);
                totalAverageRep += playerAvg;
            }
        }
        
        stats.put("totalPlayers", totalPlayers);
        stats.put("totalRelations", totalRelations);
        stats.put("averageRelationsPerPlayer", totalPlayers > 0 ? (double) totalRelations / totalPlayers : 0);
        stats.put("globalAverageReputation", playerAverages.size() > 0 ? totalAverageRep / playerAverages.size() : 0);
        
        Map<String, Integer> globalDistribution = new HashMap<>();
        globalDistribution.put("friendly", friendly);
        globalDistribution.put("positive", positive);
        globalDistribution.put("neutral", neutral);
        globalDistribution.put("negative", negative);
        globalDistribution.put("hostile", hostile);
        stats.put("globalDistribution", globalDistribution);
        
        // Top players by average reputation
        List<Map.Entry<UUID, Double>> topByReputation = playerAverages.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        
        List<Map<String, Object>> topPlayersData = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : topByReputation) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("playerId", entry.getKey().toString());
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());
            playerData.put("playerName", player.getName());
            playerData.put("averageReputation", entry.getValue());
            topPlayersData.add(playerData);
        }
        stats.put("topPlayersByReputation", topPlayersData);
        
        // Players with most relations
        List<Map.Entry<UUID, Integer>> mostRelations = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Integer>> entry : playerReputation.entrySet()) {
            mostRelations.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().size()));
        }
        mostRelations.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        stats.put("playersWithMostRelations", mostRelations.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

