package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Tracks tax evasion and applies penalties. */
public class TaxEvasionService {
    private final AXIOM plugin;
    private final File evasionDir;
    private final Map<String, List<EvasionRecord>> playerRecords = new HashMap<>(); // playerUUID -> records

    public static class EvasionRecord {
        String nationId;
        double amount;
        long timestamp;
        boolean caught;
        double penalty;
    }

    public TaxEvasionService(AXIOM plugin) {
        this.plugin = plugin;
        this.evasionDir = new File(plugin.getDataFolder(), "taxevasion");
        this.evasionDir.mkdirs();
        loadAll();
    }

    public synchronized boolean attemptEvasion(UUID playerId, String nationId, double taxAmount) {
        if (playerId == null || isBlank(nationId)) return false;
        if (!Double.isFinite(taxAmount) || taxAmount <= 0) return false;
        // 5% chance of being caught per evasion attempt
        boolean caught = Math.random() < 0.05;
        EvasionRecord record = new EvasionRecord();
        record.nationId = nationId;
        record.amount = taxAmount;
        record.timestamp = System.currentTimeMillis();
        record.caught = caught;
        record.penalty = caught ? taxAmount * 2.0 : 0.0; // 2x penalty if caught
        playerRecords.computeIfAbsent(playerId.toString(), k -> new ArrayList<>()).add(record);
        if (caught) {
            if (plugin.getNationManager() == null) {
                saveRecords(playerId.toString());
                return false;
            }
            Nation n = plugin.getNationManager().getNationById(nationId);
            if (n != null) {
                Player p = plugin.getServer().getPlayer(playerId);
                if (p != null && plugin.getWalletService() != null) {
                    double playerBalance = plugin.getWalletService().getBalance(playerId);
                    double fine = Math.min(playerBalance, record.penalty);
                    plugin.getWalletService().withdraw(playerId, fine);
                    n.setTreasury(n.getTreasury() + fine);
                    try {
                        plugin.getNationManager().save(n);
                    } catch (Exception ignored) {}
                }
            }
        }
        saveRecords(playerId.toString());
        return !caught; // true if evasion successful
    }

    private void loadAll() {
        File[] files = evasionDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String playerId = f.getName().replace(".json", "");
                List<EvasionRecord> records = new ArrayList<>();
                if (o.has("records")) {
                    for (var elem : o.getAsJsonArray("records")) {
                        JsonObject rObj = elem.getAsJsonObject();
                        EvasionRecord rec = new EvasionRecord();
                        rec.nationId = rObj.has("nationId") ? rObj.get("nationId").getAsString() : null;
                        rec.amount = rObj.has("amount") ? rObj.get("amount").getAsDouble() : 0.0;
                        rec.timestamp = rObj.has("timestamp") ? rObj.get("timestamp").getAsLong() : 0L;
                        rec.caught = rObj.has("caught") && rObj.get("caught").getAsBoolean();
                        rec.penalty = rObj.has("penalty") ? rObj.get("penalty").getAsDouble() : 0.0;
                        records.add(rec);
                    }
                }
                if (!isBlank(playerId)) {
                    playerRecords.put(playerId, records);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveRecords(String playerId) {
        if (isBlank(playerId)) return;
        File f = new File(evasionDir, playerId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<EvasionRecord> records = playerRecords.get(playerId);
        if (records != null) {
            for (EvasionRecord rec : records) {
                if (rec == null) continue;
                JsonObject rObj = new JsonObject();
                rObj.addProperty("nationId", rec.nationId);
                rObj.addProperty("amount", rec.amount);
                rObj.addProperty("timestamp", rec.timestamp);
                rObj.addProperty("caught", rec.caught);
                rObj.addProperty("penalty", rec.penalty);
                arr.add(rObj);
            }
        }
        o.add("records", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive tax evasion statistics.
     */
    public synchronized Map<String, Object> getTaxEvasionStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (playerId == null) return stats;
        List<EvasionRecord> records = playerRecords.get(playerId.toString());
        if (records == null) records = Collections.emptyList();
        
        int totalAttempts = records.size();
        int caughtCount = 0;
        int successfulCount = 0;
        double totalEvaded = 0.0;
        double totalPenalties = 0.0;
        
        Map<String, Integer> byNation = new HashMap<>();
        
        for (EvasionRecord record : records) {
            if (record == null) continue;
            if (record.caught) {
                caughtCount++;
                totalPenalties += record.penalty;
            } else {
                successfulCount++;
                totalEvaded += record.amount;
            }
            if (record.nationId != null) {
                byNation.put(record.nationId, byNation.getOrDefault(record.nationId, 0) + 1);
            }
        }
        
        stats.put("totalAttempts", totalAttempts);
        stats.put("caughtCount", caughtCount);
        stats.put("successfulCount", successfulCount);
        stats.put("catchRate", totalAttempts > 0 ? (caughtCount / (double) totalAttempts) * 100 : 0);
        stats.put("totalEvaded", totalEvaded);
        stats.put("totalPenalties", totalPenalties);
        stats.put("netBenefit", totalEvaded - totalPenalties);
        stats.put("byNation", byNation);
        
        // Evasion rating
        String rating = "ЧЕСТНЫЙ";
        if (totalAttempts >= 20 && (successfulCount / (double) totalAttempts) > 0.8) rating = "ОПЫТНЫЙ УКЛОНЯЮЩИЙСЯ";
        else if (totalAttempts >= 10 && (successfulCount / (double) totalAttempts) > 0.7) rating = "СКРЫВАЮЩИЙСЯ";
        else if (totalAttempts >= 5) rating = "РИСКУЮЩИЙ";
        else if (totalAttempts > 0) rating = "НАЧИНАЮЩИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get tax evasion statistics for a nation.
     */
    public synchronized Map<String, Object> getNationTaxEvasionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        int totalAttempts = 0;
        int caughtCount = 0;
        int successfulCount = 0;
        double totalEvaded = 0.0;
        double totalPenalties = 0.0;
        Set<String> evaders = new HashSet<>();
        
        for (Map.Entry<String, List<EvasionRecord>> entry : playerRecords.entrySet()) {
            List<EvasionRecord> records = entry.getValue();
            if (records == null) continue;
            for (EvasionRecord record : records) {
                if (record == null || !nationId.equals(record.nationId)) continue;
                    totalAttempts++;
                    evaders.add(entry.getKey()); // Player ID
                    if (record.caught) {
                        caughtCount++;
                        totalPenalties += record.penalty;
                    } else {
                        successfulCount++;
                        totalEvaded += record.amount;
                    }
            }
        }
        
        stats.put("totalAttempts", totalAttempts);
        stats.put("caughtCount", caughtCount);
        stats.put("successfulCount", successfulCount);
        stats.put("totalEvaded", totalEvaded);
        stats.put("totalPenalties", totalPenalties);
        stats.put("netLoss", totalEvaded - totalPenalties);
        stats.put("evaderCount", evaders.size());
        
        // Evasion rate (as percentage of total tax collected)
        double evasionRate = totalAttempts > 0 ? (successfulCount / (double) totalAttempts) * 100 : 0;
        stats.put("evasionRate", evasionRate);
        
        return stats;
    }
    
    /**
     * Clear old evasion records (older than specified days).
     */
    public synchronized void clearOldRecords(int daysOld) {
        if (daysOld < 0) return;
        long cutoff = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
        
        Iterator<Map.Entry<String, List<EvasionRecord>>> iterator = playerRecords.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<EvasionRecord>> entry = iterator.next();
            List<EvasionRecord> records = entry.getValue();
            if (records == null) {
                iterator.remove();
                continue;
            }
            records.removeIf(record -> record != null && record.timestamp < cutoff);
            if (records.isEmpty()) {
                iterator.remove();
                // Delete file
                File f = new File(evasionDir, entry.getKey() + ".json");
                if (f.exists()) f.delete();
            } else {
                saveRecords(entry.getKey());
            }
        }
    }
    
    /**
     * Get evasion risk for a player (based on history).
     */
    public synchronized double getEvasionRisk(UUID playerId) {
        if (playerId == null) return 0.0;
        List<EvasionRecord> records = playerRecords.get(playerId.toString());
        if (records == null || records.isEmpty()) return 0.0;
        
        // Risk increases with number of successful evasions
        long successfulCount = records.stream().filter(r -> r != null && !r.caught).count();
        return Math.min(100, successfulCount * 5.0); // +5% risk per successful evasion
    }
    
    /**
     * Get global tax evasion statistics across all players and nations.
     */
    public synchronized Map<String, Object> getGlobalTaxEvasionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalAttempts = 0;
        int totalCaught = 0;
        int totalSuccessful = 0;
        double totalEvaded = 0.0;
        double totalPenalties = 0.0;
        Set<String> totalEvaders = new HashSet<>();
        Map<String, Integer> attemptsByNation = new HashMap<>();
        
        for (Map.Entry<String, List<EvasionRecord>> entry : playerRecords.entrySet()) {
            totalEvaders.add(entry.getKey());
            List<EvasionRecord> records = entry.getValue();
            if (records == null) continue;
            for (EvasionRecord record : records) {
                if (record == null) continue;
                totalAttempts++;
                if (record.nationId != null) {
                    attemptsByNation.put(record.nationId, attemptsByNation.getOrDefault(record.nationId, 0) + 1);
                }
                
                if (record.caught) {
                    totalCaught++;
                    totalPenalties += record.penalty;
                } else {
                    totalSuccessful++;
                    totalEvaded += record.amount;
                }
            }
        }
        
        stats.put("totalAttempts", totalAttempts);
        stats.put("totalCaught", totalCaught);
        stats.put("totalSuccessful", totalSuccessful);
        stats.put("globalCatchRate", totalAttempts > 0 ? (totalCaught / (double) totalAttempts) * 100 : 0);
        stats.put("totalEvaded", totalEvaded);
        stats.put("totalPenalties", totalPenalties);
        stats.put("netGlobalBenefit", totalEvaded - totalPenalties);
        stats.put("totalEvaders", totalEvaders.size());
        stats.put("attemptsByNation", attemptsByNation);
        
        // Top nations by evasion attempts
        List<Map.Entry<String, Integer>> topByAttempts = attemptsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByAttempts", topByAttempts);
        
        // Average attempts per evader
        stats.put("averageAttemptsPerEvader", totalEvaders.size() > 0 ? 
            (double) totalAttempts / totalEvaders.size() : 0);
        
        // Success rate distribution
        Map<String, Integer> successRateDistribution = new HashMap<>();
        for (String playerId : totalEvaders) {
            List<EvasionRecord> records = playerRecords.get(playerId);
            if (records != null && !records.isEmpty()) {
                long successful = records.stream().filter(r -> r != null && !r.caught).count();
                double successRate = (successful / (double) records.size()) * 100;
                
                if (successRate >= 80) successRateDistribution.put("excellent", 
                    successRateDistribution.getOrDefault("excellent", 0) + 1);
                else if (successRate >= 60) successRateDistribution.put("good", 
                    successRateDistribution.getOrDefault("good", 0) + 1);
                else if (successRate >= 40) successRateDistribution.put("average", 
                    successRateDistribution.getOrDefault("average", 0) + 1);
                else if (successRate >= 20) successRateDistribution.put("poor", 
                    successRateDistribution.getOrDefault("poor", 0) + 1);
                else successRateDistribution.put("veryPoor", 
                    successRateDistribution.getOrDefault("veryPoor", 0) + 1);
            }
        }
        stats.put("successRateDistribution", successRateDistribution);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

