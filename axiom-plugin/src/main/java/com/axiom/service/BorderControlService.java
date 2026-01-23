package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.entity.Player;

import java.util.*;

/** Manages border controls and immigration policies. */
public class BorderControlService {
    private final AXIOM plugin;
    private final Map<String, BorderPolicy> policies = new HashMap<>(); // nationId -> policy

    public static class BorderPolicy {
        boolean open; // true = open borders, false = closed
        Set<String> allowedNations = new HashSet<>();
        Set<String> bannedNations = new HashSet<>();
        boolean requireVisa;
        double visaCost;
    }

    public BorderControlService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized String setBorderPolicy(String nationId, boolean open, boolean requireVisa, double visaCost) {
        if (visaCost < 0) return "Неверная стоимость визы.";
        BorderPolicy policy = policies.computeIfAbsent(nationId, k -> new BorderPolicy());
        policy.open = open;
        policy.requireVisa = requireVisa;
        policy.visaCost = visaCost;
        return "Политика границ обновлена.";
    }

    public synchronized boolean canEnter(Player player, String nationId) {
        BorderPolicy policy = policies.get(nationId);
        if (policy == null || policy.open) return true; // Default: open
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        if (playerDataManager == null) return policy.open;
        String playerNationId = playerDataManager.getNation(player.getUniqueId());
        if (playerNationId == null || playerNationId.equals(nationId)) return true; // Own nation
        if (policy.bannedNations.contains(playerNationId)) return false;
        if (!policy.allowedNations.isEmpty() && !policy.allowedNations.contains(playerNationId)) return false;
        return true;
    }

    public synchronized String allowNation(String nationId, String allowedNationId) {
        BorderPolicy policy = policies.computeIfAbsent(nationId, k -> new BorderPolicy());
        policy.allowedNations.add(allowedNationId);
        return "Нация добавлена в разрешённый список.";
    }

    public synchronized String banNation(String nationId, String bannedNationId) {
        BorderPolicy policy = policies.computeIfAbsent(nationId, k -> new BorderPolicy());
        policy.bannedNations.add(bannedNationId);
        return "Нация добавлена в запрещённый список.";
    }
    
    /**
     * Get comprehensive border control statistics.
     */
    public synchronized Map<String, Object> getBorderControlStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        BorderPolicy policy = policies.get(nationId);
        if (policy == null) {
            policy = new BorderPolicy();
            policy.open = true; // Default
        }
        
        stats.put("open", policy.open);
        stats.put("requireVisa", policy.requireVisa);
        stats.put("visaCost", policy.visaCost);
        stats.put("allowedNations", new ArrayList<>(policy.allowedNations));
        stats.put("bannedNations", new ArrayList<>(policy.bannedNations));
        stats.put("allowedCount", policy.allowedNations.size());
        stats.put("bannedCount", policy.bannedNations.size());
        
        // Border control rating
        String rating = "ОТКРЫТЫЕ";
        if (!policy.open && !policy.allowedNations.isEmpty()) rating = "ОГРАНИЧЕННЫЕ";
        else if (!policy.open) rating = "ЗАКРЫТЫЕ";
        else if (policy.requireVisa) rating = "С ВИЗОЙ";
        stats.put("rating", rating);
        
        // Economic impact (tourism, trade, etc.)
        double impact = policy.open ? 1.05 : 0.95; // Open borders = +5% economy
        if (policy.requireVisa) impact *= 0.98; // Visa requirement = slight penalty
        stats.put("economicImpact", impact);
        
        return stats;
    }
    
    /**
     * Remove nation from allowed list.
     */
    public synchronized String removeAllowedNation(String nationId, String targetNationId) {
        BorderPolicy policy = policies.get(nationId);
        if (policy == null) return "Политика не найдена.";
        if (!policy.allowedNations.remove(targetNationId)) return "Нация не была в списке.";
        return "Нация удалена из разрешённого списка.";
    }
    
    /**
     * Remove nation from banned list.
     */
    public synchronized String removeBannedNation(String nationId, String targetNationId) {
        BorderPolicy policy = policies.get(nationId);
        if (policy == null) return "Политика не найдена.";
        if (!policy.bannedNations.remove(targetNationId)) return "Нация не была в списке.";
        return "Нация удалена из запрещённого списка.";
    }
    
    /**
     * Check if player needs visa to enter.
     */
    public synchronized boolean requiresVisa(Player player, String nationId) {
        BorderPolicy policy = policies.get(nationId);
        if (policy == null || !policy.requireVisa) return false;
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        if (playerDataManager == null) return false;
        String playerNationId = playerDataManager.getNation(player.getUniqueId());
        if (playerNationId == null || playerNationId.equals(nationId)) return false; // Own nation
        
        return true;
    }
    
    /**
     * Get visa cost for entering nation.
     */
    public synchronized double getVisaCost(String nationId) {
        BorderPolicy policy = policies.get(nationId);
        return policy != null && policy.requireVisa ? policy.visaCost : 0.0;
    }
    
    /**
     * Get global border control statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalBorderControlStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPolicies = policies.size();
        int openBorders = 0;
        int closedBorders = 0;
        int requireVisa = 0;
        Map<String, Integer> allowedByNation = new HashMap<>();
        Map<String, Integer> bannedByNation = new HashMap<>();
        double totalVisaCost = 0.0;
        int nationsWithVisa = 0;
        
        for (Map.Entry<String, BorderPolicy> entry : policies.entrySet()) {
            String nationId = entry.getKey();
            BorderPolicy policy = entry.getValue();
            
            if (policy.open) openBorders++;
            else closedBorders++;
            
            if (policy.requireVisa) {
                requireVisa++;
                totalVisaCost += policy.visaCost;
                nationsWithVisa++;
            }
            
            allowedByNation.put(nationId, policy.allowedNations.size());
            bannedByNation.put(nationId, policy.bannedNations.size());
        }
        
        stats.put("totalPolicies", totalPolicies);
        stats.put("openBorders", openBorders);
        stats.put("closedBorders", closedBorders);
        stats.put("requireVisa", requireVisa);
        stats.put("allowedByNation", allowedByNation);
        stats.put("bannedByNation", bannedByNation);
        stats.put("averageVisaCost", nationsWithVisa > 0 ? totalVisaCost / nationsWithVisa : 0);
        stats.put("nationsWithVisa", nationsWithVisa);
        
        // Border openness rate
        stats.put("opennessRate", totalPolicies > 0 ? (double) openBorders / totalPolicies : 0);
        
        // Top nations by allowed nations
        List<Map.Entry<String, Integer>> topByAllowed = allowedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByAllowed", topByAllowed);
        
        // Top nations by banned nations
        List<Map.Entry<String, Integer>> topByBanned = bannedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByBanned", topByBanned);
        
        // Policy type distribution
        Map<String, Integer> policyDistribution = new HashMap<>();
        policyDistribution.put("open", openBorders);
        policyDistribution.put("closed", closedBorders);
        policyDistribution.put("visa", requireVisa);
        stats.put("policyDistribution", policyDistribution);
        
        // Average allowed per nation
        int totalAllowed = allowedByNation.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageAllowedPerNation", totalPolicies > 0 ? (double) totalAllowed / totalPolicies : 0);
        
        // Average banned per nation
        int totalBanned = bannedByNation.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageBannedPerNation", totalPolicies > 0 ? (double) totalBanned / totalPolicies : 0);
        
        return stats;
    }
}

