package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.UUID;

/** Enhanced alliance management with chat integration. */
public class AllianceService {
    private final AXIOM plugin;
    private final DiplomacySystem diplomacySystem;

    public AllianceService(AXIOM plugin, DiplomacySystem diplomacySystem) {
        this.plugin = plugin;
        this.diplomacySystem = diplomacySystem;
    }

    public synchronized void broadcastToAllies(String senderNationId, String message) {
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) return;
        Nation sender = nationManager.getNationById(senderNationId);
        if (sender == null) return;
        String formatted = "§b[A] §f" + sender.getName() + ": §7" + message;
        Set<String> allies = sender.getAllies();
        if (allies == null) return;
        for (String allyId : allies) {
            Nation ally = nationManager.getNationById(allyId);
            if (ally == null) continue;
            for (UUID citizen : ally.getCitizens()) {
                Player p = Bukkit.getPlayer(citizen);
                if (p != null && p.isOnline()) {
                    p.sendMessage(formatted);
                }
            }
        }
    }

    public synchronized boolean canAccessAlliedTerritory(UUID playerId, String targetNationId) {
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        NationManager nationManager = plugin.getNationManager();
        if (playerDataManager == null || nationManager == null) return false;
        String playerNationId = playerDataManager.getNation(playerId);
        if (playerNationId == null) return false;
        Nation playerNation = nationManager.getNationById(playerNationId);
        if (playerNation == null) return false;
        Set<String> allies = playerNation.getAllies();
        return (allies != null && allies.contains(targetNationId)) || playerNation.getId().equals(targetNationId);
    }
    
    /**
     * Get comprehensive alliance statistics.
     */
    public synchronized Map<String, Object> getAllianceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) {
            stats.put("allies", 0);
            stats.put("alliesList", Collections.emptyList());
            return stats;
        }
        Nation n = nationManager.getNationById(nationId);
        if (n == null) {
            stats.put("allies", 0);
            stats.put("alliesList", Collections.emptyList());
            return stats;
        }
        
        List<String> allies = n.getAllies() != null ? new ArrayList<>(n.getAllies()) : new ArrayList<>();
        stats.put("allies", allies.size());
        stats.put("alliesList", allies);
        
        // Calculate combined alliance strength
        double totalStrength = 0.0;
        for (String allyId : allies) {
            Nation ally = nationManager.getNationById(allyId);
            if (ally != null) {
                if (plugin.getMilitaryService() != null) {
                    totalStrength += plugin.getMilitaryService().getMilitaryStrength(allyId);
                } else {
                    totalStrength += ally.getCitizens().size() * 10.0; // Estimate
                }
            }
        }
        stats.put("combinedStrength", totalStrength);
        
        // Alliance rating
        String rating = "МАЛЕНЬКИЙ";
        if (allies.size() >= 10) rating = "ОГРОМНЫЙ";
        else if (allies.size() >= 7) rating = "БОЛЬШОЙ";
        else if (allies.size() >= 5) rating = "ЗНАЧИТЕЛЬНЫЙ";
        else if (allies.size() >= 3) rating = "СРЕДНИЙ";
        else if (allies.size() >= 1) rating = "МАЛЕНЬКИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get alliance benefits for a nation.
     */
    public synchronized Map<String, Double> getAllianceBenefits(String nationId) {
        Map<String, Double> benefits = new HashMap<>();
        
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) return benefits;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return benefits;
        
        int allyCount = n.getAllies() != null ? n.getAllies().size() : 0;
        
        // Trade bonus from allies
        benefits.put("tradeBonus", 1.0 + (allyCount * 0.02)); // +2% per ally
        
        // Defense bonus (military support)
        benefits.put("defenseBonus", 1.0 + (allyCount * 0.05)); // +5% per ally
        
        // Diplomatic bonus
        benefits.put("diplomacyBonus", 1.0 + (allyCount * 0.03)); // +3% per ally
        
        return benefits;
    }
    
    /**
     * Check if two nations are allies.
     */
    public synchronized boolean areAllies(String nationA, String nationB) {
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) return false;
        Nation a = nationManager.getNationById(nationA);
        Nation b = nationManager.getNationById(nationB);
        if (a == null || b == null) return false;
        Set<String> aAllies = a.getAllies();
        Set<String> bAllies = b.getAllies();
        return (aAllies != null && aAllies.contains(nationB)) || (bAllies != null && bAllies.contains(nationA));
    }
    
    /**
     * Get global alliance statistics.
     */
    public synchronized Map<String, Object> getGlobalAllianceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalAlliances = 0;
        int maxAllies = 0;
        int nationsWithAllies = 0;
        double totalCombinedStrength = 0.0;
        
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) {
            stats.put("totalAllianceRelations", 0);
            stats.put("nationsWithAllies", 0);
            stats.put("maxAllies", 0);
            stats.put("averageAllies", 0);
            stats.put("totalCombinedStrength", 0.0);
            return stats;
        }
        for (Nation n : nationManager.getAll()) {
            int allies = n.getAllies() != null ? n.getAllies().size() : 0;
            if (allies > 0) {
                nationsWithAllies++;
                totalAlliances += allies;
                maxAllies = Math.max(maxAllies, allies);
                
                if (plugin.getMilitaryService() != null) {
                    double combinedStrength = 0.0;
                    for (String allyId : n.getAllies()) {
                        combinedStrength += plugin.getMilitaryService().getMilitaryStrength(allyId);
                    }
                    totalCombinedStrength += combinedStrength;
                }
            }
        }
        
        stats.put("totalAllianceRelations", totalAlliances / 2); // Each alliance counted twice
        stats.put("nationsWithAllies", nationsWithAllies);
        stats.put("maxAllies", maxAllies);
        stats.put("averageAllies", nationsWithAllies > 0 ? (double) totalAlliances / nationsWithAllies : 0);
        stats.put("totalCombinedStrength", totalCombinedStrength);
        
        // Alliance distribution
        Map<Integer, Integer> distribution = new HashMap<>();
        for (Nation n : nationManager.getAll()) {
            int count = n.getAllies() != null ? n.getAllies().size() : 0;
            distribution.put(count, distribution.getOrDefault(count, 0) + 1);
        }
        stats.put("allianceDistribution", distribution);
        
        // Top nations by alliance count
        List<Map.Entry<String, Integer>> topByAllies = new ArrayList<>();
        for (Nation n : nationManager.getAll()) {
            topByAllies.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), n.getAllies() != null ? n.getAllies().size() : 0));
        }
        topByAllies.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        stats.put("topByAllies", topByAllies.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

