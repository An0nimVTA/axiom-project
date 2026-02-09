package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Tracks cultural and economic influence between nations. */
public class InfluenceService {
    private final AXIOM plugin;
    private final File influenceDir;
    private final Map<String, Map<String, Double>> influence = new HashMap<>(); // influencer -> target -> amount

    public InfluenceService(AXIOM plugin) {
        this.plugin = plugin;
        this.influenceDir = new File(plugin.getDataFolder(), "influence");
        this.influenceDir.mkdirs();
        loadAll();
    }

    public synchronized void addInfluence(String influencerId, String targetId, double amount) {
        if (influencerId == null || targetId == null || amount <= 0) return;
        Map<String, Double> targetMap = influence.computeIfAbsent(influencerId, k -> new HashMap<>());
        targetMap.put(targetId, targetMap.getOrDefault(targetId, 0.0) + amount);
        saveInfluence(influencerId);
    }

    public synchronized double getInfluence(String influencerId, String targetId) {
        if (influencerId == null || targetId == null) return 0.0;
        Map<String, Double> targetMap = influence.get(influencerId);
        return targetMap != null ? targetMap.getOrDefault(targetId, 0.0) : 0.0;
    }

    public synchronized String getInfluenceLevel(String influencerId, String targetId) {
        double inf = getInfluence(influencerId, targetId);
        if (inf >= 1000) return "§aДоминирующее";
        if (inf >= 500) return "§bСильное";
        if (inf >= 100) return "§fУмеренное";
        if (inf >= 50) return "§eСлабое";
        return "§7Минимальное";
    }

    private void loadAll() {
        File[] files = influenceDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String influencerId = f.getName().replace(".json", "");
                Map<String, Double> targetMap = new HashMap<>();
                for (var entry : o.entrySet()) {
                    targetMap.put(entry.getKey(), entry.getValue().getAsDouble());
                }
                influence.put(influencerId, targetMap);
            } catch (Exception ignored) {}
        }
    }

    private void saveInfluence(String influencerId) {
        File f = new File(influenceDir, influencerId + ".json");
        JsonObject o = new JsonObject();
        Map<String, Double> targetMap = influence.get(influencerId);
        if (targetMap != null) {
            for (var entry : targetMap.entrySet()) {
                o.addProperty(entry.getKey(), entry.getValue());
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive influence statistics.
     */
    public synchronized Map<String, Object> getInfluenceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Influence as influencer (how much we influence others)
        Map<String, Double> targets = influence.get(nationId);
        if (targets != null) {
            stats.put("influencedNations", targets.size());
            double totalInfluence = targets.values().stream().mapToDouble(Double::doubleValue).sum();
            stats.put("totalInfluence", totalInfluence);
            stats.put("averageInfluence", targets.size() > 0 ? totalInfluence / targets.size() : 0.0);
            
            // Top influenced nations
            List<Map.Entry<String, Double>> topTargets = targets.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
            stats.put("topTargets", topTargets);
        } else {
            stats.put("influencedNations", 0);
            stats.put("totalInfluence", 0.0);
        }
        
        // Influence as target (how much others influence us)
        int influencers = 0;
        double totalInfluencedBy = 0.0;
        for (Map<String, Double> targetMap : influence.values()) {
            if (targetMap.containsKey(nationId)) {
                influencers++;
                totalInfluencedBy += targetMap.get(nationId);
            }
        }
        stats.put("influencers", influencers);
        stats.put("totalInfluencedBy", totalInfluencedBy);
        
        // Influence rating
        double total = targets != null ? targets.values().stream().mapToDouble(Double::doubleValue).sum() : 0.0;
        String rating = "МАЛЕНЬКОЕ";
        if (total >= 5000) rating = "ОГРОМНОЕ";
        else if (total >= 2000) rating = "БОЛЬШОЕ";
        else if (total >= 1000) rating = "ЗНАЧИТЕЛЬНОЕ";
        else if (total >= 500) rating = "УМЕРЕННОЕ";
        else if (total >= 100) rating = "НЕБОЛЬШОЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get total influence power of a nation.
     */
    public synchronized double getTotalInfluencePower(String nationId) {
        Map<String, Double> targets = influence.get(nationId);
        if (targets == null) return 0.0;
        return targets.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    
    /**
     * Reduce influence (resistance).
     */
    public synchronized String resistInfluence(String targetId, String influencerId, double amount) {
        if (targetId == null || influencerId == null || amount <= 0) return "Неверные параметры.";
        Map<String, Double> targetMap = influence.get(influencerId);
        if (targetMap == null || !targetMap.containsKey(targetId)) {
            return "Нет влияния для сопротивления.";
        }
        
        double current = targetMap.get(targetId);
        double newInfluence = Math.max(0, current - amount);
        targetMap.put(targetId, newInfluence);
        
        if (newInfluence == 0) {
            targetMap.remove(targetId);
        }
        
        saveInfluence(influencerId);
        return "Влияние уменьшено. Текущий уровень: " + String.format("%.1f", newInfluence);
    }
    
    /**
     * Get global influence statistics.
     */
    public synchronized Map<String, Object> getGlobalInfluenceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalInfluenceRelations = 0;
        double totalInfluencePower = 0.0;
        Map<String, Double> influencePowerByNation = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Double>> entry : influence.entrySet()) {
            double power = entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
            totalInfluenceRelations += entry.getValue().size();
            totalInfluencePower += power;
            influencePowerByNation.put(entry.getKey(), power);
        }
        
        stats.put("totalInfluenceRelations", totalInfluenceRelations);
        stats.put("totalInfluencePower", totalInfluencePower);
        stats.put("averageInfluencePower", influencePowerByNation.size() > 0 ? 
            totalInfluencePower / influencePowerByNation.size() : 0);
        
        // Top influencers
        List<Map.Entry<String, Double>> topInfluencers = influencePowerByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topInfluencers", topInfluencers);
        
        return stats;
    }
}

