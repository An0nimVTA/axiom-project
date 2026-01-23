package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.util.*;

/** Manages coup attempts and regime changes. */
public class CoupService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, Long> lastCoupAttempt = new HashMap<>(); // nationId -> timestamp

    public CoupService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public synchronized String attemptCoup(String nationId, UUID instigatorId, double cost) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (!n.isMember(instigatorId)) return "Вы не в нации.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        Long last = lastCoupAttempt.get(nationId);
        if (last != null && System.currentTimeMillis() - last < 7 * 24 * 60 * 60 * 1000L) {
            return "Попытка переворота на перезарядке (7 дней).";
        }
        n.setTreasury(n.getTreasury() - cost);
        lastCoupAttempt.put(nationId, System.currentTimeMillis());
        // Success chance depends on happiness
        double happiness = plugin.getHappinessService().getNationHappiness(nationId);
        double successChance = Math.max(0.1, 1.0 - (happiness / 100.0)); // Lower happiness = higher chance
        boolean success = Math.random() < successChance;
        if (success) {
            // Coup successful - new leader
            UUID oldLeader = n.getLeader();
            n.getRoles().put(oldLeader, Nation.Role.CITIZEN);
            n.getRoles().put(instigatorId, Nation.Role.LEADER);
            n.setLeader(instigatorId);
            String instigatorName = Bukkit.getPlayer(instigatorId) != null ? Bukkit.getPlayer(instigatorId).getName() : instigatorId.toString();
            n.getHistory().add("Государственный переворот! Новый лидер: " + instigatorName);
        } else {
            n.getHistory().add("Неудачная попытка переворота!");
        }
        try { nationManager.save(n); } catch (Exception ignored) {}
        return success ? "Переворот успешен! Вы новый лидер." : "Переворот провалился.";
    }

    public synchronized boolean canAttemptCoup(String nationId) {
        Long last = lastCoupAttempt.get(nationId);
        return last == null || System.currentTimeMillis() - last >= 7 * 24 * 60 * 60 * 1000L;
    }
    
    /**
     * Get comprehensive coup statistics for a nation.
     */
    public synchronized Map<String, Object> getCoupStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Long lastAttempt = lastCoupAttempt.get(nationId);
        boolean canAttempt = canAttemptCoup(nationId);
        
        stats.put("canAttempt", canAttempt);
        stats.put("hasAttempted", lastAttempt != null);
        if (lastAttempt != null) {
            long daysSince = (System.currentTimeMillis() - lastAttempt) / (24 * 60 * 60 * 1000L);
            stats.put("daysSinceLastAttempt", daysSince);
            stats.put("cooldownRemainingDays", Math.max(0, 7 - daysSince));
        } else {
            stats.put("daysSinceLastAttempt", -1);
            stats.put("cooldownRemainingDays", 0);
        }
        
        // Coup risk level based on happiness
        double happiness = plugin.getHappinessService().getNationHappiness(nationId);
        double coupRisk = Math.max(0.1, 1.0 - (happiness / 100.0));
        stats.put("coupRisk", coupRisk);
        
        String riskRating = "НИЗКИЙ";
        if (coupRisk >= 0.8) riskRating = "КРИТИЧЕСКИЙ";
        else if (coupRisk >= 0.6) riskRating = "ВЫСОКИЙ";
        else if (coupRisk >= 0.4) riskRating = "СРЕДНИЙ";
        else if (coupRisk >= 0.2) riskRating = "НИЗКИЙ";
        stats.put("riskRating", riskRating);
        
        return stats;
    }
    
    /**
     * Get global coup statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCoupStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalAttempts = lastCoupAttempt.size();
        int canAttemptNow = 0;
        Map<String, Long> daysSinceByNation = new HashMap<>();
        Map<String, Double> coupRiskByNation = new HashMap<>();
        
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastCoupAttempt.entrySet()) {
            String nationId = entry.getKey();
            long lastAttempt = entry.getValue();
            long daysSince = (now - lastAttempt) / (24 * 60 * 60 * 1000L);
            daysSinceByNation.put(nationId, daysSince);
            
            if (daysSince >= 7) {
                canAttemptNow++;
            }
        }
        
        // Calculate coup risk for all nations
        for (Nation n : nationManager.getAll()) {
            double happiness = plugin.getHappinessService().getNationHappiness(n.getId());
            double coupRisk = Math.max(0.1, 1.0 - (happiness / 100.0));
            coupRiskByNation.put(n.getId(), coupRisk);
        }
        
        stats.put("totalAttempts", totalAttempts);
        stats.put("nationsWithAttempts", totalAttempts);
        stats.put("canAttemptNow", canAttemptNow);
        stats.put("daysSinceByNation", daysSinceByNation);
        stats.put("coupRiskByNation", coupRiskByNation);
        
        // Average coup risk
        double totalRisk = coupRiskByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("averageCoupRisk", coupRiskByNation.size() > 0 ? totalRisk / coupRiskByNation.size() : 0);
        
        // Top nations by coup risk (most unstable)
        List<Map.Entry<String, Double>> topByRisk = coupRiskByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRisk", topByRisk);
        
        // Risk distribution
        int critical = 0, high = 0, medium = 0, low = 0;
        for (Double risk : coupRiskByNation.values()) {
            if (risk >= 0.8) critical++;
            else if (risk >= 0.6) high++;
            else if (risk >= 0.4) medium++;
            else low++;
        }
        
        Map<String, Integer> riskDistribution = new HashMap<>();
        riskDistribution.put("critical", critical);
        riskDistribution.put("high", high);
        riskDistribution.put("medium", medium);
        riskDistribution.put("low", low);
        stats.put("riskDistribution", riskDistribution);
        
        return stats;
    }
}

