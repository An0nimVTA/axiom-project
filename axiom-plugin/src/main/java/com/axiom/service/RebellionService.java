package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages rebellions and uprisings within nations. */
public class RebellionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File rebellionsDir;
    private final Map<String, Rebellion> activeRebellions = new HashMap<>(); // rebellionId -> rebellion

    public static class Rebellion {
        String id;
        String nationId;
        String leaderId; // player UUID leading the rebellion
        int strength; // 0-100
        double support; // percentage
        long startedAt;
    }

    public RebellionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.rebellionsDir = new File(plugin.getDataFolder(), "rebellions");
        this.rebellionsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateRebellions, 0, 20 * 60 * 15); // every 15 minutes
    }

    public synchronized String startRebellion(String nationId, UUID leaderId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (leaderId == null) return "Неверный лидер восстания.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (!n.isMember(leaderId)) return "Вы не в этой нации.";
        if (plugin.getHappinessService() == null) return "Сервис счастья недоступен.";
        double happiness = plugin.getHappinessService().getNationHappiness(nationId);
        if (happiness > 60) return "Счастье слишком высоко для восстания.";
        String rebellionId = UUID.randomUUID().toString().substring(0, 8);
        Rebellion r = new Rebellion();
        r.id = rebellionId;
        r.nationId = nationId;
        r.leaderId = leaderId.toString();
        r.strength = (int)((60 - happiness) * 2); // Higher when happiness is lower
        r.support = 10.0;
        r.startedAt = System.currentTimeMillis();
        activeRebellions.put(rebellionId, r);
        saveRebellion(r);
        if (n.getHistory() != null) {
            n.getHistory().add("Началось восстание!");
        }
        try { nationManager.save(n); } catch (Exception ignored) {}
        return "Восстание начато (сила: " + r.strength + "%)";
    }

    private synchronized void updateRebellions() {
        if (nationManager == null || plugin.getHappinessService() == null) return;
        Iterator<Map.Entry<String, Rebellion>> iterator = activeRebellions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Rebellion> entry = iterator.next();
            Rebellion r = entry.getValue();
            if (r == null || isBlank(r.nationId)) {
                iterator.remove();
                continue;
            }
            Nation n = nationManager.getNationById(r.nationId);
            if (n == null) continue;
            double happiness = plugin.getHappinessService().getNationHappiness(r.nationId);
            r.support = Math.max(0, Math.min(100, r.support + (60 - happiness) * 0.1));
            if (r.support >= 80 && r.strength >= 50) {
                // Rebellion succeeds - new nation or regime change
                String leaderName = r.leaderId;
                try {
                    UUID leaderUuid = UUID.fromString(r.leaderId);
                    org.bukkit.entity.Player leader = Bukkit.getPlayer(leaderUuid);
                    if (leader != null) {
                        leaderName = leader.getName();
                    }
                } catch (IllegalArgumentException ignored) {}
                if (n.getHistory() != null) {
                    n.getHistory().add("Восстание победило! Новый режим под руководством " + leaderName);
                }
                try { nationManager.save(n); } catch (Exception ignored) {}
                iterator.remove();
            } else if (r.support < 20) {
                // Rebellion fails
                if (n.getHistory() != null) {
                    n.getHistory().add("Восстание подавлено.");
                }
                try { nationManager.save(n); } catch (Exception ignored) {}
                iterator.remove();
            }
        }
    }

    private void loadAll() {
        File[] files = rebellionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Rebellion reb = new Rebellion();
                reb.id = o.get("id").getAsString();
                reb.nationId = o.get("nationId").getAsString();
                reb.leaderId = o.get("leaderId").getAsString();
                reb.strength = o.get("strength").getAsInt();
                reb.support = o.get("support").getAsDouble();
                reb.startedAt = o.get("startedAt").getAsLong();
                activeRebellions.put(reb.id, reb);
            } catch (Exception ignored) {}
        }
    }

    private void saveRebellion(Rebellion r) {
        if (r == null || isBlank(r.id)) return;
        File f = new File(rebellionsDir, r.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", r.id);
        o.addProperty("nationId", r.nationId);
        o.addProperty("leaderId", r.leaderId);
        o.addProperty("strength", r.strength);
        o.addProperty("support", r.support);
        o.addProperty("startedAt", r.startedAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive rebellion statistics for a nation.
     */
    public synchronized Map<String, Object> getRebellionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();

        if (isBlank(nationId)) return stats;
        List<Rebellion> rebellions = new ArrayList<>();
        for (Rebellion r : activeRebellions.values()) {
            if (r != null && Objects.equals(r.nationId, nationId)) {
                rebellions.add(r);
            }
        }
        
        if (rebellions.isEmpty()) {
            stats.put("hasActiveRebellions", false);
            stats.put("activeRebellions", 0);
            return stats;
        }
        
        int total = rebellions.size();
        double totalStrength = 0.0;
        double totalSupport = 0.0;
        
        for (Rebellion r : rebellions) {
            totalStrength += r.strength;
            totalSupport += r.support;
        }
        
        stats.put("hasActiveRebellions", true);
        stats.put("activeRebellions", total);
        stats.put("averageStrength", total > 0 ? totalStrength / total : 0);
        stats.put("averageSupport", total > 0 ? totalSupport / total : 0);
        
        // Rebellion threat level
        double avgSupport = total > 0 ? totalSupport / total : 0;
        double avgStrength = total > 0 ? totalStrength / total : 0;
        double threatLevel = (avgSupport + avgStrength) / 2.0;
        stats.put("threatLevel", threatLevel);
        
        String rating = "КРИТИЧЕСКИЙ";
        if (threatLevel >= 80) rating = "КРИТИЧЕСКИЙ";
        else if (threatLevel >= 60) rating = "ВЫСОКИЙ";
        else if (threatLevel >= 40) rating = "СРЕДНИЙ";
        else if (threatLevel >= 20) rating = "НИЗКИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global rebellion statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalRebellionStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalActiveRebellions", activeRebellions.size());
        
        Map<String, Integer> rebellionsByNation = new HashMap<>();
        Map<String, Double> averageStrengthByNation = new HashMap<>();
        Map<String, Double> averageSupportByNation = new HashMap<>();
        Map<String, Double> threatLevelByNation = new HashMap<>();
        
        for (Rebellion r : activeRebellions.values()) {
            if (r == null || isBlank(r.nationId)) continue;
            rebellionsByNation.put(r.nationId,
                rebellionsByNation.getOrDefault(r.nationId, 0) + 1);
            
            // Track averages per nation
            if (!averageStrengthByNation.containsKey(r.nationId)) {
                averageStrengthByNation.put(r.nationId, 0.0);
                averageSupportByNation.put(r.nationId, 0.0);
            }
            
            int count = rebellionsByNation.get(r.nationId);
            double currentAvgStrength = averageStrengthByNation.get(r.nationId);
            double currentAvgSupport = averageSupportByNation.get(r.nationId);
            
            averageStrengthByNation.put(r.nationId,
                ((currentAvgStrength * (count - 1)) + r.strength) / count);
            averageSupportByNation.put(r.nationId,
                ((currentAvgSupport * (count - 1)) + r.support) / count);
        }
        
        // Calculate threat levels
        for (String nationId : rebellionsByNation.keySet()) {
            double strength = averageStrengthByNation.get(nationId);
            double support = averageSupportByNation.get(nationId);
            double threat = (strength + support) / 2.0;
            threatLevelByNation.put(nationId, threat);
        }
        
        stats.put("rebellionsByNation", rebellionsByNation);
        stats.put("averageStrengthByNation", averageStrengthByNation);
        stats.put("averageSupportByNation", averageSupportByNation);
        stats.put("threatLevelByNation", threatLevelByNation);
        stats.put("nationsWithRebellions", rebellionsByNation.size());
        
        // Average statistics
        double totalStrength = averageStrengthByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalSupport = averageSupportByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("averageStrength", rebellionsByNation.size() > 0 ? totalStrength / rebellionsByNation.size() : 0);
        stats.put("averageSupport", rebellionsByNation.size() > 0 ? totalSupport / rebellionsByNation.size() : 0);
        
        // Top nations by rebellions (most unstable)
        List<Map.Entry<String, Integer>> topByRebellions = rebellionsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRebellions", topByRebellions);
        
        // Top nations by threat level
        List<Map.Entry<String, Double>> topByThreat = threatLevelByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByThreat", topByThreat);
        
        // Threat level distribution
        int critical = 0, high = 0, medium = 0, low = 0;
        for (Double threat : threatLevelByNation.values()) {
            if (threat >= 80) critical++;
            else if (threat >= 60) high++;
            else if (threat >= 40) medium++;
            else low++;
        }
        
        Map<String, Integer> threatDistribution = new HashMap<>();
        threatDistribution.put("critical", critical);
        threatDistribution.put("high", high);
        threatDistribution.put("medium", medium);
        threatDistribution.put("low", low);
        stats.put("threatDistribution", threatDistribution);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

