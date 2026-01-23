package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;

/** Manages assassination attempts on nation leaders. */
public class AssassinationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, Long> lastAssassinationAttempt = new HashMap<>(); // nationId -> timestamp

    public AssassinationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public synchronized String attemptAssassination(String targetNationId, String attackerNationId, double cost) {
        Nation target = nationManager.getNationById(targetNationId);
        Nation attacker = nationManager.getNationById(attackerNationId);
        if (target == null || attacker == null) return "Нация не найдена.";
        if (attacker.getTreasury() < cost) return "Недостаточно средств.";
        Long last = lastAssassinationAttempt.get(targetNationId);
        if (last != null && System.currentTimeMillis() - last < 24 * 60 * 60 * 1000L) {
            return "Попытка на перезарядке (24 часа).";
        }
        attacker.setTreasury(attacker.getTreasury() - cost);
        lastAssassinationAttempt.put(targetNationId, System.currentTimeMillis());
        // 30% success chance
        boolean success = Math.random() < 0.3;
        if (success) {
            // Assassinate leader (depose them)
            UUID oldLeader = target.getLeader();
            if (!target.getCitizens().isEmpty()) {
                UUID newLeader = target.getCitizens().iterator().next();
                target.getRoles().put(oldLeader, Nation.Role.CITIZEN);
                target.getRoles().put(newLeader, Nation.Role.LEADER);
                target.setLeader(newLeader);
                String leaderName = Bukkit.getPlayer(newLeader) != null ? Bukkit.getPlayer(newLeader).getName() : newLeader.toString();
                target.getHistory().add("Лидер убит! Новый лидер: " + leaderName);
            }
        } else {
            target.getHistory().add("Неудачное покушение на лидера!");
        }
        try {
            nationManager.save(attacker);
            nationManager.save(target);
        } catch (Exception ignored) {}
        return success ? "Покушение успешно!" : "Покушение провалилось.";
    }
    
    /**
     * Get comprehensive assassination statistics.
     */
    public synchronized Map<String, Object> getAssassinationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Assassination attempts on this nation
        Long lastAttempt = lastAssassinationAttempt.get(nationId);
        if (lastAttempt != null) {
            stats.put("lastAttempt", lastAttempt);
            long timeSince = System.currentTimeMillis() - lastAttempt;
            stats.put("timeSinceLastAttempt", timeSince / 1000 / 60 / 60); // hours
            stats.put("cooldownRemaining", Math.max(0, (24 * 60 * 60 * 1000L - timeSince) / 1000 / 60)); // minutes
        } else {
            stats.put("lastAttempt", null);
            stats.put("timeSinceLastAttempt", -1);
            stats.put("cooldownRemaining", 0);
        }
        
        // Assassination risk (based on diplomatic relations)
        double risk = 5.0; // Base 5% risk
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            // Risk increases with enemies (check war status, low reputation nations, etc.)
            int enemyCount = 0;
            try {
                // Check for active wars
                if (plugin.getAdvancedWarSystem() != null) {
                    List<AdvancedWarSystem.War> wars = plugin.getAdvancedWarSystem().getNationWars(nationId);
                    if (wars != null) {
                        enemyCount += wars.size();
                    }
                }
                // Check allies (less risk if many allies)
                int allyCount = n.getAllies().size();
                risk -= allyCount * 0.5; // -0.5% per ally
            } catch (Exception ignored) {}
            risk += enemyCount * 5.0; // +5% per war/enemy
        }
        stats.put("assassinationRisk", Math.min(100, risk));
        
        // Risk rating
        String rating = "НИЗКИЙ";
        if (risk >= 30) rating = "ОЧЕНЬ ВЫСОКИЙ";
        else if (risk >= 20) rating = "ВЫСОКИЙ";
        else if (risk >= 15) rating = "СРЕДНИЙ";
        else if (risk >= 10) rating = "УМЕРЕННЫЙ";
        stats.put("riskRating", rating);
        
        return stats;
    }
    
    /**
     * Increase assassination protection (security measures).
     */
    public synchronized String increaseProtection(String nationId, double cost) throws IOException {
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        // Extend cooldown by 12 hours per investment
        Long lastAttempt = lastAssassinationAttempt.get(nationId);
        if (lastAttempt != null) {
            lastAssassinationAttempt.put(nationId, lastAttempt - (12 * 60 * 60 * 1000L));
        }
        
        n.setTreasury(n.getTreasury() - cost);
        nationManager.save(n);
        
        return "Защита от покушений усилена.";
    }
    
    /**
     * Check if nation is vulnerable to assassination.
     */
    public synchronized boolean isVulnerable(String nationId) {
        Long lastAttempt = lastAssassinationAttempt.get(nationId);
        if (lastAttempt == null) return true;
        
        long timeSince = System.currentTimeMillis() - lastAttempt;
        return timeSince >= 24 * 60 * 60 * 1000L; // 24 hours cooldown
    }
    
    /**
     * Get global assassination statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalAssassinationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalAttemptsRecorded", lastAssassinationAttempt.size());
        
        // Nations by vulnerability
        int vulnerableNations = 0;
        int protectedNations = 0;
        Map<String, Double> riskByNation = new HashMap<>();
        
        for (Nation n : nationManager.getAll()) {
            boolean isVulnerable = isVulnerable(n.getId());
            
            if (isVulnerable) {
                vulnerableNations++;
            } else {
                protectedNations++;
            }
            
            // Calculate risk for each nation
            double risk = 5.0;
            int enemyCount = 0;
            try {
                if (plugin.getAdvancedWarSystem() != null) {
                    List<AdvancedWarSystem.War> wars = plugin.getAdvancedWarSystem().getNationWars(n.getId());
                    if (wars != null) {
                        enemyCount += wars.size();
                    }
                }
                int allyCount = n.getAllies().size();
                risk -= allyCount * 0.5;
            } catch (Exception ignored) {}
            risk += enemyCount * 5.0;
            riskByNation.put(n.getId(), Math.min(100, risk));
        }
        
        stats.put("vulnerableNations", vulnerableNations);
        stats.put("protectedNations", protectedNations);
        stats.put("averageRisk", riskByNation.size() > 0 ? 
            riskByNation.values().stream().mapToDouble(Double::doubleValue).average().orElse(0) : 0);
        
        // Top nations by risk (most vulnerable)
        List<Map.Entry<String, Double>> topByRisk = riskByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRisk", topByRisk);
        
        // Risk distribution
        int veryHigh = 0, high = 0, medium = 0, low = 0;
        for (double risk : riskByNation.values()) {
            if (risk >= 30) veryHigh++;
            else if (risk >= 20) high++;
            else if (risk >= 15) medium++;
            else low++;
        }
        
        Map<String, Integer> riskDistribution = new HashMap<>();
        riskDistribution.put("veryHigh", veryHigh);
        riskDistribution.put("high", high);
        riskDistribution.put("medium", medium);
        riskDistribution.put("low", low);
        stats.put("riskDistribution", riskDistribution);
        
        // Recent attempts (within last 24 hours)
        long now = System.currentTimeMillis();
        int recentAttempts = 0;
        for (Long attemptTime : lastAssassinationAttempt.values()) {
            if (now - attemptTime < 24 * 60 * 60 * 1000L) {
                recentAttempts++;
            }
        }
        stats.put("recentAttempts24h", recentAttempts);
        
        return stats;
    }
}

