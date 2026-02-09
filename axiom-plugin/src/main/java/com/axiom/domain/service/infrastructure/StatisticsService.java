package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Tracks statistics and metrics for nations and players. */
public class StatisticsService {
    private final AXIOM plugin;
    private final File statsDir;
    private final Map<String, NationStats> nationStats = new HashMap<>();

    public static class NationStats {
        int warsWon = 0;
        int warsLost = 0;
        int treatiesSigned = 0;
        int electionsHeld = 0;
        double totalTradeVolume = 0.0;
        long totalPlaytime = 0; // total player minutes

        public int getWarsWon() {
            return warsWon;
        }

        public int getWarsLost() {
            return warsLost;
        }

        public int getTreatiesSigned() {
            return treatiesSigned;
        }

        public int getElectionsHeld() {
            return electionsHeld;
        }

        public double getTotalTradeVolume() {
            return totalTradeVolume;
        }

        public long getTotalPlaytime() {
            return totalPlaytime;
        }
    }

    public StatisticsService(AXIOM plugin) {
        this.plugin = plugin;
        this.statsDir = new File(plugin.getDataFolder(), "statistics");
        this.statsDir.mkdirs();
        loadAll();
    }

    public synchronized void recordWarWin(String nationId) {
        if (isBlank(nationId)) return;
        NationStats stats = nationStats.computeIfAbsent(nationId, k -> new NationStats());
        stats.warsWon++;
        saveStats(nationId, stats);
    }

    public synchronized void recordWarLoss(String nationId) {
        if (isBlank(nationId)) return;
        NationStats stats = nationStats.computeIfAbsent(nationId, k -> new NationStats());
        stats.warsLost++;
        saveStats(nationId, stats);
    }

    public synchronized void recordTreaty(String nationId) {
        if (isBlank(nationId)) return;
        NationStats stats = nationStats.computeIfAbsent(nationId, k -> new NationStats());
        stats.treatiesSigned++;
        saveStats(nationId, stats);
    }

    public synchronized void recordTrade(String nationId, double amount) {
        if (isBlank(nationId)) return;
        if (!Double.isFinite(amount) || amount <= 0) return;
        NationStats stats = nationStats.computeIfAbsent(nationId, k -> new NationStats());
        stats.totalTradeVolume += amount;
        saveStats(nationId, stats);
    }

    public synchronized NationStats getStats(String nationId) {
        if (isBlank(nationId)) return new NationStats();
        return nationStats.getOrDefault(nationId, new NationStats());
    }
    
    /**
     * Get comprehensive statistics for a nation.
     */
    public synchronized Map<String, Object> getComprehensiveStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        if (isBlank(nationId)) return stats;
        NationStats ns = getStats(nationId);
        
        stats.put("warsWon", ns.warsWon);
        stats.put("warsLost", ns.warsLost);
        stats.put("winRate", (ns.warsWon + ns.warsLost) > 0 ? 
            (ns.warsWon / (double) (ns.warsWon + ns.warsLost)) * 100 : 0);
        stats.put("treatiesSigned", ns.treatiesSigned);
        stats.put("electionsHeld", ns.electionsHeld);
        stats.put("totalTradeVolume", ns.totalTradeVolume);
        stats.put("totalPlaytime", ns.totalPlaytime);
        
        // Additional calculated stats
        if (plugin.getNationManager() != null) {
            Nation n = plugin.getNationManager().getNationById(nationId);
            if (n != null) {
                // Nation age calculation (simplified)
                Collection<String> claimed = n.getClaimedChunkKeys();
                Collection<java.util.UUID> citizens = n.getCitizens();
                stats.put("territories", claimed != null ? claimed.size() : 0);
                stats.put("citizens", citizens != null ? citizens.size() : 0);
            }
        }
        
        return stats;
    }
    
    /**
     * Record election held.
     */
    public synchronized void recordElection(String nationId) {
        if (isBlank(nationId)) return;
        NationStats stats = nationStats.computeIfAbsent(nationId, k -> new NationStats());
        stats.electionsHeld++;
        saveStats(nationId, stats);
    }
    
    /**
     * Record playtime (in minutes).
     */
    public synchronized void recordPlaytime(String nationId, long minutes) {
        if (isBlank(nationId) || minutes <= 0) return;
        NationStats stats = nationStats.computeIfAbsent(nationId, k -> new NationStats());
        stats.totalPlaytime += minutes;
        saveStats(nationId, stats);
    }
    
    /**
     * Get top nations by a metric.
     */
    public synchronized List<Map.Entry<String, Double>> getTopNationsByMetric(String metric, int limit) {
        if (isBlank(metric) || limit <= 0) return Collections.emptyList();
        Map<String, Double> scores = new HashMap<>();
        
        for (Map.Entry<String, NationStats> entry : nationStats.entrySet()) {
            double score = 0.0;
            NationStats ns = entry.getValue();
            
            switch (metric.toLowerCase(Locale.ROOT)) {
                case "warswon":
                    score = ns.warsWon;
                    break;
                case "tradevolume":
                    score = ns.totalTradeVolume;
                    break;
                case "treaties":
                    score = ns.treatiesSigned;
                    break;
                case "winrate":
                    score = (ns.warsWon + ns.warsLost) > 0 ? 
                        (ns.warsWon / (double) (ns.warsWon + ns.warsLost)) * 100 : 0;
                    break;
                case "playtime":
                    score = ns.totalPlaytime;
                    break;
            }
            
            scores.put(entry.getKey(), score);
        }
        
        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Record various nation achievements.
     */
    public synchronized void recordAchievement(String nationId, String achievementType) {
        if (isBlank(nationId) || isBlank(achievementType)) return;
        NationStats stats = nationStats.computeIfAbsent(nationId, k -> new NationStats());
        // Track different achievement types in future
        saveStats(nationId, stats);
    }
    
    /**
     * Get global statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalStatistics() {
        Map<String, Object> global = new HashMap<>();
        
        int totalWars = nationStats.values().stream().mapToInt(ns -> ns.warsWon + ns.warsLost).sum();
        double totalTrade = nationStats.values().stream().mapToDouble(ns -> ns.totalTradeVolume).sum();
        int totalTreaties = nationStats.values().stream().mapToInt(ns -> ns.treatiesSigned).sum();
        long totalPlaytime = nationStats.values().stream().mapToLong(ns -> ns.totalPlaytime).sum();
        
        global.put("totalWars", totalWars);
        global.put("totalTradeVolume", totalTrade);
        global.put("totalTreaties", totalTreaties);
        global.put("totalPlaytime", totalPlaytime);
        global.put("activeNations", nationStats.size());
        
        return global;
    }
    
    /**
     * Compare two nations' statistics.
     */
    public synchronized Map<String, Object> compareNations(String nationAId, String nationBId) {
        Map<String, Object> comparison = new HashMap<>();
        if (isBlank(nationAId) || isBlank(nationBId)) return comparison;
        
        NationStats statsA = getStats(nationAId);
        NationStats statsB = getStats(nationBId);
        
        comparison.put("nationA_warsWon", statsA.warsWon);
        comparison.put("nationB_warsWon", statsB.warsWon);
        comparison.put("nationA_tradeVolume", statsA.totalTradeVolume);
        comparison.put("nationB_tradeVolume", statsB.totalTradeVolume);
        comparison.put("nationA_treaties", statsA.treatiesSigned);
        comparison.put("nationB_treaties", statsB.treatiesSigned);
        
        // Calculate win rates
        double winRateA = (statsA.warsWon + statsA.warsLost) > 0 ? 
            (statsA.warsWon / (double) (statsA.warsWon + statsA.warsLost)) * 100 : 0;
        double winRateB = (statsB.warsWon + statsB.warsLost) > 0 ? 
            (statsB.warsWon / (double) (statsB.warsWon + statsB.warsLost)) * 100 : 0;
        
        comparison.put("nationA_winRate", winRateA);
        comparison.put("nationB_winRate", winRateB);
        
        return comparison;
    }
    
    /**
     * Get comprehensive statistics service statistics.
     */
    public synchronized Map<String, Object> getStatisticsServiceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalNationsTracked", nationStats.size());
        stats.put("totalWars", nationStats.values().stream().mapToInt(ns -> ns.warsWon + ns.warsLost).sum());
        stats.put("totalTreaties", nationStats.values().stream().mapToInt(ns -> ns.treatiesSigned).sum());
        stats.put("totalTradeVolume", nationStats.values().stream().mapToDouble(ns -> ns.totalTradeVolume).sum());
        stats.put("totalPlaytime", nationStats.values().stream().mapToLong(ns -> ns.totalPlaytime).sum());
        
        // Top performers
        List<Map.Entry<String, Double>> topWars = getTopNationsByMetric("warswon", 5);
        List<Map.Entry<String, Double>> topTrade = getTopNationsByMetric("tradevolume", 5);
        List<Map.Entry<String, Double>> topTreaties = getTopNationsByMetric("treaties", 5);
        
        stats.put("topWarsWon", topWars);
        stats.put("topTradeVolume", topTrade);
        stats.put("topTreaties", topTreaties);
        
        // Average stats
        if (nationStats.size() > 0) {
            stats.put("averageWarsWon", nationStats.values().stream().mapToInt(ns -> ns.warsWon).average().orElse(0));
            stats.put("averageTradeVolume", nationStats.values().stream().mapToDouble(ns -> ns.totalTradeVolume).average().orElse(0));
            stats.put("averageTreaties", nationStats.values().stream().mapToInt(ns -> ns.treatiesSigned).average().orElse(0));
        }
        
        return stats;
    }
    
    /**
     * Get leaderboard by multiple metrics.
     */
    public synchronized Map<String, List<Map.Entry<String, Double>>> getLeaderboards() {
        Map<String, List<Map.Entry<String, Double>>> leaderboards = new HashMap<>();
        leaderboards.put("warsWon", getTopNationsByMetric("warswon", 10));
        leaderboards.put("tradeVolume", getTopNationsByMetric("tradevolume", 10));
        leaderboards.put("treaties", getTopNationsByMetric("treaties", 10));
        leaderboards.put("winRate", getTopNationsByMetric("winrate", 10));
        leaderboards.put("playtime", getTopNationsByMetric("playtime", 10));
        return leaderboards;
    }
    
    /**
     * Reset statistics for a nation.
     */
    public synchronized void resetStatistics(String nationId) {
        if (isBlank(nationId)) return;
        nationStats.put(nationId, new NationStats());
        saveStats(nationId, new NationStats());
    }

    private void loadAll() {
        File[] files = statsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                NationStats stats = new NationStats();
                if (o.has("warsWon")) stats.warsWon = o.get("warsWon").getAsInt();
                if (o.has("warsLost")) stats.warsLost = o.get("warsLost").getAsInt();
                if (o.has("treatiesSigned")) stats.treatiesSigned = o.get("treatiesSigned").getAsInt();
                if (o.has("electionsHeld")) stats.electionsHeld = o.get("electionsHeld").getAsInt();
                if (o.has("totalTradeVolume")) stats.totalTradeVolume = o.get("totalTradeVolume").getAsDouble();
                if (o.has("totalPlaytime")) stats.totalPlaytime = o.get("totalPlaytime").getAsLong();
                String nationId = f.getName().replace(".json", "");
                if (!isBlank(nationId)) {
                    nationStats.put(nationId, stats);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveStats(String nationId, NationStats stats) {
        if (isBlank(nationId) || stats == null) return;
        File f = new File(statsDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("warsWon", stats.warsWon);
        o.addProperty("warsLost", stats.warsLost);
        o.addProperty("treatiesSigned", stats.treatiesSigned);
        o.addProperty("electionsHeld", stats.electionsHeld);
        o.addProperty("totalTradeVolume", stats.totalTradeVolume);
        o.addProperty("totalPlaytime", stats.totalPlaytime);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
