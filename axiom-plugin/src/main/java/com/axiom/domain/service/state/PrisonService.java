package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages prisons and prisoner systems. */
public class PrisonService {
    private final AXIOM plugin;
    private final File prisonsDir;
    private final Map<String, List<Prisoner>> nationPrisons = new HashMap<>(); // nationId -> prisoners

    public static class Prisoner {
        UUID playerId;
        String crime;
        long imprisonedAt;
        long sentenceMinutes;
        String imprisonerNationId;
    }

    public PrisonService(AXIOM plugin) {
        this.plugin = plugin;
        this.prisonsDir = new File(plugin.getDataFolder(), "prisons");
        this.prisonsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkSentences, 0, 20 * 60); // every minute
    }

    public synchronized String imprisonPlayer(UUID playerId, String nationId, String crime, long sentenceMinutes) throws IOException {
        if (playerId == null || nationId == null || nationId.trim().isEmpty() || crime == null || crime.trim().isEmpty() || sentenceMinutes <= 0) {
            return "Некорректные данные.";
        }
        Prisoner p = new Prisoner();
        p.playerId = playerId;
        p.crime = crime;
        p.imprisonedAt = System.currentTimeMillis();
        p.sentenceMinutes = sentenceMinutes;
        p.imprisonerNationId = nationId;
        nationPrisons.computeIfAbsent(nationId, k -> new ArrayList<>()).add(p);
        savePrisoners(nationId);
        return "Игрок заключён: " + crime + " (срок: " + sentenceMinutes + " мин)";
    }

    public synchronized boolean isImprisoned(UUID playerId) {
        if (playerId == null) return false;
        for (List<Prisoner> prisoners : nationPrisons.values()) {
            for (Prisoner p : prisoners) {
                if (p.playerId.equals(playerId)) {
                    long elapsed = (System.currentTimeMillis() - p.imprisonedAt) / 1000 / 60;
                    if (elapsed < p.sentenceMinutes) return true;
                }
            }
        }
        return false;
    }

    private synchronized void checkSentences() {
        long now = System.currentTimeMillis();
        List<String> changedNations = new ArrayList<>();
        for (var entry : nationPrisons.entrySet()) {
            int before = entry.getValue().size();
            entry.getValue().removeIf(p -> {
                long elapsed = (now - p.imprisonedAt) / 1000 / 60;
                return elapsed >= p.sentenceMinutes;
            });
            if (entry.getValue().size() != before) {
                changedNations.add(entry.getKey());
            }
        }
        for (String nationId : changedNations) {
            try {
                savePrisoners(nationId);
            } catch (IOException ignored) {}
        }
    }

    private void loadAll() {
        File[] files = prisonsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                List<Prisoner> prisoners = new ArrayList<>();
                if (o.has("prisoners")) {
                    JsonArray arr = o.getAsJsonArray("prisoners");
                    for (var e : arr) {
                        JsonObject pObj = e.getAsJsonObject();
                        Prisoner p = new Prisoner();
                        p.playerId = UUID.fromString(pObj.get("playerId").getAsString());
                        p.crime = pObj.get("crime").getAsString();
                        p.imprisonedAt = pObj.get("imprisonedAt").getAsLong();
                        p.sentenceMinutes = pObj.get("sentenceMinutes").getAsLong();
                        p.imprisonerNationId = nationId;
                        long elapsed = (System.currentTimeMillis() - p.imprisonedAt) / 1000 / 60;
                        if (elapsed < p.sentenceMinutes) prisoners.add(p);
                    }
                }
                nationPrisons.put(nationId, prisoners);
            } catch (Exception ignored) {}
        }
    }

    private void savePrisoners(String nationId) throws IOException {
        File f = new File(prisonsDir, nationId + ".json");
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (Prisoner p : nationPrisons.getOrDefault(nationId, Collections.emptyList())) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("playerId", p.playerId.toString());
            pObj.addProperty("crime", p.crime);
            pObj.addProperty("imprisonedAt", p.imprisonedAt);
            pObj.addProperty("sentenceMinutes", p.sentenceMinutes);
            arr.add(pObj);
        }
        o.add("prisoners", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive prison statistics.
     */
    public synchronized Map<String, Object> getPrisonStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        if (nationId == null || nationId.trim().isEmpty()) {
            stats.put("totalPrisoners", 0);
            stats.put("byCrime", new HashMap<>());
            stats.put("prisonersList", new ArrayList<>());
            stats.put("averageSentenceMinutes", 0.0);
            stats.put("rating", "НЕТ ЗАКЛЮЧЁННЫХ");
            return stats;
        }
        
        List<Prisoner> prisoners = nationPrisons.getOrDefault(nationId, Collections.emptyList());
        
        stats.put("totalPrisoners", prisoners.size());
        
        // Prisoners by crime
        Map<String, Integer> byCrime = new HashMap<>();
        for (Prisoner p : prisoners) {
            byCrime.put(p.crime, byCrime.getOrDefault(p.crime, 0) + 1);
        }
        stats.put("byCrime", byCrime);
        
        // Prisoner details
        List<Map<String, Object>> prisonersList = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Prisoner p : prisoners) {
            Map<String, Object> prisonerData = new HashMap<>();
            prisonerData.put("playerId", p.playerId.toString());
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(p.playerId);
            prisonerData.put("playerName", player.getName());
            prisonerData.put("crime", p.crime);
            prisonerData.put("imprisonedAt", p.imprisonedAt);
            long elapsed = (now - p.imprisonedAt) / 1000 / 60; // minutes
            long remaining = p.sentenceMinutes - elapsed;
            prisonerData.put("timeRemaining", Math.max(0, remaining));
            prisonerData.put("timeRemainingHours", Math.max(0, remaining) / 60);
            prisonerData.put("sentenceMinutes", p.sentenceMinutes);
            prisonersList.add(prisonerData);
        }
        stats.put("prisonersList", prisonersList);
        
        // Average sentence
        double avgSentence = prisoners.stream()
            .mapToLong(p -> p.sentenceMinutes)
            .average()
            .orElse(0.0);
        stats.put("averageSentenceMinutes", avgSentence);
        
        // Prison rating
        String rating = "НЕТ ЗАКЛЮЧЁННЫХ";
        if (prisoners.size() >= 50) rating = "ПЕРЕПОЛНЕННАЯ";
        else if (prisoners.size() >= 20) rating = "ЗАПОЛНЕННАЯ";
        else if (prisoners.size() >= 10) rating = "АКТИВНАЯ";
        else if (prisoners.size() >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Release prisoner early.
     */
    public synchronized String releasePrisoner(String nationId, UUID playerId) throws IOException {
        if (nationId == null || nationId.trim().isEmpty() || playerId == null) return "Некорректные данные.";
        List<Prisoner> prisoners = nationPrisons.get(nationId);
        if (prisoners == null) return "Тюрьма не найдена.";
        
        boolean removed = prisoners.removeIf(p -> p.playerId.equals(playerId));
        if (!removed) return "Заключённый не найден.";
        
        savePrisoners(nationId);
        
        return "Заключённый освобождён.";
    }
    
    /**
     * Get prisoner information.
     */
    public synchronized Prisoner getPrisoner(UUID playerId) {
        if (playerId == null) return null;
        for (List<Prisoner> prisoners : nationPrisons.values()) {
            for (Prisoner p : prisoners) {
                if (p.playerId.equals(playerId)) {
                    long elapsed = (System.currentTimeMillis() - p.imprisonedAt) / 1000 / 60;
                    if (elapsed < p.sentenceMinutes) {
                        return p;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Get all prisoners for nation.
     */
    public synchronized List<Prisoner> getNationPrisoners(String nationId) {
        if (nationId == null || nationId.trim().isEmpty()) return Collections.emptyList();
        return new ArrayList<>(nationPrisons.getOrDefault(nationId, Collections.emptyList()));
    }
    
    /**
     * Calculate prison capacity usage.
     */
    public synchronized double getPrisonCapacityUsage(String nationId, int maxCapacity) {
        if (nationId == null || nationId.trim().isEmpty()) return 0.0;
        int current = getNationPrisoners(nationId).size();
        return maxCapacity > 0 ? (double) current / maxCapacity : 0.0;
    }
    
    /**
     * Get global prison statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPrisonStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPrisoners = 0;
        Map<String, Integer> prisonersByNation = new HashMap<>();
        Map<String, Integer> prisonersByCrime = new HashMap<>();
        long totalSentenceMinutes = 0L;
        Map<String, Double> averageSentenceByNation = new HashMap<>();
        
        for (Map.Entry<String, List<Prisoner>> entry : nationPrisons.entrySet()) {
            List<Prisoner> prisoners = entry.getValue();
            int count = prisoners.size();
            totalPrisoners += count;
            prisonersByNation.put(entry.getKey(), count);
            
            long nationTotalSentence = 0L;
            for (Prisoner p : prisoners) {
                prisonersByCrime.put(p.crime, prisonersByCrime.getOrDefault(p.crime, 0) + 1);
                totalSentenceMinutes += p.sentenceMinutes;
                nationTotalSentence += p.sentenceMinutes;
            }
            
            if (count > 0) {
                averageSentenceByNation.put(entry.getKey(), (double) nationTotalSentence / count);
            }
        }
        
        stats.put("totalPrisoners", totalPrisoners);
        stats.put("prisonersByNation", prisonersByNation);
        stats.put("prisonersByCrime", prisonersByCrime);
        stats.put("totalSentenceMinutes", totalSentenceMinutes);
        stats.put("averageSentenceMinutes", totalPrisoners > 0 ? (double) totalSentenceMinutes / totalPrisoners : 0);
        stats.put("nationsWithPrisons", prisonersByNation.size());
        
        // Top nations by prisoners
        List<Map.Entry<String, Integer>> topByPrisoners = prisonersByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPrisoners", topByPrisoners);
        
        // Most common crimes
        List<Map.Entry<String, Integer>> mostCommonCrimes = prisonersByCrime.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonCrimes", mostCommonCrimes);
        
        // Average prisoners per nation
        stats.put("averagePrisonersPerNation", prisonersByNation.size() > 0 ? 
            (double) totalPrisoners / prisonersByNation.size() : 0);
        
        // Top nations by average sentence
        List<Map.Entry<String, Double>> topBySentence = averageSentenceByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySentence", topBySentence);
        
        return stats;
    }
}

