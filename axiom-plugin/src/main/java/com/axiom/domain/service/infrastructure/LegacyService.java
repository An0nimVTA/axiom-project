package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Tracks nation legacy and historical achievements. */
public class LegacyService {
    private final AXIOM plugin;
    private final File legacyDir;
    private final Map<String, Legacy> nationLegacy = new HashMap<>(); // nationId -> legacy

    public static class Legacy {
        List<String> achievements = new ArrayList<>();
        List<String> majorEvents = new ArrayList<>();
        long totalExistedTime = 0;
        int warsWon = 0;
        int treatiesSigned = 0;
        double totalGDP = 0.0;
    }

    public LegacyService(AXIOM plugin) {
        this.plugin = plugin;
        this.legacyDir = new File(plugin.getDataFolder(), "legacy");
        this.legacyDir.mkdirs();
        loadAll();
    }

    public synchronized void addAchievement(String nationId, String achievement) {
        if (nationId == null || achievement == null || achievement.trim().isEmpty()) return;
        Legacy l = nationLegacy.computeIfAbsent(nationId, k -> new Legacy());
        l.achievements.add(achievement);
        saveLegacy(nationId, l);
    }

    public synchronized void recordEvent(String nationId, String event) {
        if (nationId == null || event == null || event.trim().isEmpty()) return;
        Legacy l = nationLegacy.computeIfAbsent(nationId, k -> new Legacy());
        l.majorEvents.add(event);
        if (l.majorEvents.size() > 100) l.majorEvents.remove(0); // Keep last 100
        saveLegacy(nationId, l);
    }

    public synchronized Legacy getLegacy(String nationId) {
        return nationLegacy.computeIfAbsent(nationId, k -> new Legacy());
    }

    private void loadAll() {
        File[] files = legacyDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Legacy l = new Legacy();
                if (o.has("achievements")) {
                    JsonArray arr = o.getAsJsonArray("achievements");
                    for (var e : arr) l.achievements.add(e.getAsString());
                }
                if (o.has("majorEvents")) {
                    JsonArray arr = o.getAsJsonArray("majorEvents");
                    for (var e : arr) l.majorEvents.add(e.getAsString());
                }
                l.totalExistedTime = o.has("totalExistedTime") ? o.get("totalExistedTime").getAsLong() : 0;
                l.warsWon = o.has("warsWon") ? o.get("warsWon").getAsInt() : 0;
                l.treatiesSigned = o.has("treatiesSigned") ? o.get("treatiesSigned").getAsInt() : 0;
                l.totalGDP = o.has("totalGDP") ? o.get("totalGDP").getAsDouble() : 0.0;
                String nationId = f.getName().replace(".json", "");
                nationLegacy.put(nationId, l);
            } catch (Exception ignored) {}
        }
    }

    private void saveLegacy(String nationId, Legacy l) {
        File f = new File(legacyDir, nationId + ".json");
        JsonObject o = new JsonObject();
        JsonArray achievements = new JsonArray();
        for (String a : l.achievements) achievements.add(a);
        o.add("achievements", achievements);
        JsonArray events = new JsonArray();
        for (String e : l.majorEvents) events.add(e);
        o.add("majorEvents", events);
        o.addProperty("totalExistedTime", l.totalExistedTime);
        o.addProperty("warsWon", l.warsWon);
        o.addProperty("treatiesSigned", l.treatiesSigned);
        o.addProperty("totalGDP", l.totalGDP);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive legacy statistics.
     */
    public synchronized Map<String, Object> getLegacyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Legacy l = nationLegacy.get(nationId);
        if (l == null) {
            l = new Legacy();
        }
        
        stats.put("achievementsCount", l.achievements.size());
        stats.put("achievements", new ArrayList<>(l.achievements));
        stats.put("majorEventsCount", l.majorEvents.size());
        stats.put("majorEvents", new ArrayList<>(l.majorEvents).subList(
            Math.max(0, l.majorEvents.size() - 10), l.majorEvents.size())); // Last 10
        stats.put("warsWon", l.warsWon);
        stats.put("treatiesSigned", l.treatiesSigned);
        stats.put("totalGDP", l.totalGDP);
        stats.put("totalExistedTime", l.totalExistedTime);
        stats.put("existedDays", l.totalExistedTime / 1000 / 60 / 60 / 24);
        
        // Legacy score calculation
        double legacyScore = l.achievements.size() * 10.0
            + l.warsWon * 50
            + l.treatiesSigned * 5
            + (l.totalGDP / 1000.0)
            + (l.totalExistedTime / 1000 / 60 / 60 / 24); // Days
        stats.put("legacyScore", legacyScore);
        
        // Legacy rating
        String rating = "НАЧАЛЬНАЯ";
        if (legacyScore >= 10000) rating = "ЛЕГЕНДАРНАЯ";
        else if (legacyScore >= 5000) rating = "ВЕЛИКАЯ";
        else if (legacyScore >= 2000) rating = "ЗНАЧИТЕЛЬНАЯ";
        else if (legacyScore >= 500) rating = "УВАЖАЕМАЯ";
        else if (legacyScore >= 100) rating = "ИЗВЕСТНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Record war victory.
     */
    public synchronized void recordWarVictory(String nationId) {
        Legacy l = nationLegacy.computeIfAbsent(nationId, k -> new Legacy());
        l.warsWon++;
        saveLegacy(nationId, l);
    }
    
    /**
     * Record treaty signature.
     */
    public synchronized void recordTreatySignature(String nationId) {
        Legacy l = nationLegacy.computeIfAbsent(nationId, k -> new Legacy());
        l.treatiesSigned++;
        saveLegacy(nationId, l);
    }
    
    /**
     * Add GDP to total.
     */
    public synchronized void addGDP(String nationId, double gdp) {
        Legacy l = nationLegacy.computeIfAbsent(nationId, k -> new Legacy());
        l.totalGDP += gdp;
        saveLegacy(nationId, l);
    }
    
    /**
     * Update existence time.
     */
    public synchronized void updateExistenceTime(String nationId, long timeMs) {
        Legacy l = nationLegacy.computeIfAbsent(nationId, k -> new Legacy());
        l.totalExistedTime += timeMs;
        saveLegacy(nationId, l);
    }
    
    /**
     * Get legacy score.
     */
    public synchronized double getLegacyScore(String nationId) {
        Legacy l = nationLegacy.get(nationId);
        if (l == null) return 0.0;
        
        return l.achievements.size() * 10.0
            + l.warsWon * 50
            + l.treatiesSigned * 5
            + (l.totalGDP / 1000.0)
            + (l.totalExistedTime / 1000 / 60 / 60 / 24);
    }
    
    /**
     * Get global legacy statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalLegacyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalLegacies = nationLegacy.size();
        int totalAchievements = 0;
        int totalWarsWon = 0;
        int totalTreatiesSigned = 0;
        double totalGDP = 0.0;
        long totalExistedTime = 0L;
        Map<String, Double> legacyScoreByNation = new HashMap<>();
        
        for (Map.Entry<String, Legacy> entry : nationLegacy.entrySet()) {
            Legacy l = entry.getValue();
            
            totalAchievements += l.achievements.size();
            totalWarsWon += l.warsWon;
            totalTreatiesSigned += l.treatiesSigned;
            totalGDP += l.totalGDP;
            totalExistedTime += l.totalExistedTime;
            
            double score = getLegacyScore(entry.getKey());
            legacyScoreByNation.put(entry.getKey(), score);
        }
        
        stats.put("totalLegacies", totalLegacies);
        stats.put("totalAchievements", totalAchievements);
        stats.put("totalWarsWon", totalWarsWon);
        stats.put("totalTreatiesSigned", totalTreatiesSigned);
        stats.put("totalGDP", totalGDP);
        stats.put("totalExistedTime", totalExistedTime);
        stats.put("averageAchievements", totalLegacies > 0 ? (double) totalAchievements / totalLegacies : 0);
        stats.put("averageWarsWon", totalLegacies > 0 ? (double) totalWarsWon / totalLegacies : 0);
        stats.put("averageTreatiesSigned", totalLegacies > 0 ? (double) totalTreatiesSigned / totalLegacies : 0);
        stats.put("averageGDP", totalLegacies > 0 ? totalGDP / totalLegacies : 0);
        stats.put("averageExistedTime", totalLegacies > 0 ? totalExistedTime / totalLegacies : 0);
        
        // Top nations by legacy score
        List<Map.Entry<String, Double>> topByLegacy = legacyScoreByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByLegacy", topByLegacy);
        
        // Legacy rating distribution
        int legendary = 0, great = 0, significant = 0, respected = 0, known = 0, starting = 0;
        for (Double score : legacyScoreByNation.values()) {
            if (score >= 10000) legendary++;
            else if (score >= 5000) great++;
            else if (score >= 2000) significant++;
            else if (score >= 500) respected++;
            else if (score >= 100) known++;
            else starting++;
        }
        
        Map<String, Integer> ratingDistribution = new HashMap<>();
        ratingDistribution.put("legendary", legendary);
        ratingDistribution.put("great", great);
        ratingDistribution.put("significant", significant);
        ratingDistribution.put("respected", respected);
        ratingDistribution.put("known", known);
        ratingDistribution.put("starting", starting);
        stats.put("ratingDistribution", ratingDistribution);
        
        // Average legacy score
        double totalScore = legacyScoreByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("averageLegacyScore", totalLegacies > 0 ? totalScore / totalLegacies : 0);
        
        return stats;
    }
}

