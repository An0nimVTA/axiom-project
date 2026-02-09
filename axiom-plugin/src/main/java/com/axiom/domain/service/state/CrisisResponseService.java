package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;

import java.util.*;

/** Manages nation responses to global crises. */
public class CrisisResponseService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, CrisisResponse> responses = new HashMap<>(); // nationId -> response

    public static class CrisisResponse {
        String crisisType; // "pandemic", "economic", "natural_disaster"
        String action; // "isolation", "aid", "military", "trade"
        double investment;
    }

    public CrisisResponseService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public synchronized String respondToCrisis(String nationId, String crisisType, String action, double investment) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (investment <= 0) return "Неверная сумма.";
        if (n.getTreasury() < investment) return "Недостаточно средств.";
        n.setTreasury(n.getTreasury() - investment);
        CrisisResponse r = new CrisisResponse();
        r.crisisType = crisisType;
        r.action = action;
        r.investment = investment;
        responses.put(nationId, r);
        applyResponseEffects(n, r);
        try { nationManager.save(n); } catch (Exception ignored) {}
        return "Ответ на кризис: " + action + " (инвестиции: " + investment + ")";
    }

    private void applyResponseEffects(Nation n, CrisisResponse r) {
        switch (r.action) {
            case "isolation":
                // Reduces spread but hurts economy
                if (plugin.getNationModifierService() != null) {
                    plugin.getNationModifierService().addModifier(n.getId(), "economy", "penalty", 0.1, 1440); // 24h
                }
                break;
            case "aid":
                // Helps allies, costs money, improves reputation
                Set<String> allies = n.getAllies();
                if (allies == null || allies.isEmpty()) break;
                double perAlly = r.investment / allies.size();
                for (String allyId : allies) {
                    Nation ally = nationManager.getNationById(allyId);
                    if (ally != null) {
                        ally.setTreasury(ally.getTreasury() + perAlly);
                        try { nationManager.save(ally); } catch (Exception ignored) {}
                    }
                }
                break;
            case "military":
                // Military response improves security
                if (plugin.getNationModifierService() != null) {
                    plugin.getNationModifierService().addModifier(n.getId(), "military", "bonus", 0.15, 720); // 12h
                }
                break;
        }
    }
    
    /**
     * Get comprehensive crisis response statistics.
     */
    public synchronized Map<String, Object> getCrisisResponseStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        CrisisResponse response = responses.get(nationId);
        if (response != null) {
            stats.put("hasActiveResponse", true);
            stats.put("crisisType", response.crisisType);
            stats.put("action", response.action);
            stats.put("investment", response.investment);
            
            // Response effectiveness rating
            String rating = "НЕЭФФЕКТИВНЫЙ";
            if (response.investment >= 10000) rating = "ОЧЕНЬ ЭФФЕКТИВНЫЙ";
            else if (response.investment >= 5000) rating = "ЭФФЕКТИВНЫЙ";
            else if (response.investment >= 2000) rating = "УМЕРЕННЫЙ";
            else if (response.investment >= 1000) rating = "СЛАБЫЙ";
            stats.put("effectivenessRating", rating);
        } else {
            stats.put("hasActiveResponse", false);
        }
        
        return stats;
    }
    
    /**
     * End crisis response.
     */
    public synchronized String endCrisisResponse(String nationId) {
        CrisisResponse response = responses.remove(nationId);
        if (response == null) return "Активный ответ на кризис не найден.";
        
        return "Ответ на кризис завершён.";
    }
    
    /**
     * Get recommended action for crisis type.
     */
    public synchronized String getRecommendedAction(String crisisType) {
        switch (crisisType.toLowerCase()) {
            case "pandemic":
                return "isolation"; // Isolate to prevent spread
            case "economic":
                return "aid"; // Provide economic aid
            case "natural_disaster":
                return "aid"; // Humanitarian aid
            case "war":
                return "military"; // Military response
            default:
                return "aid"; // Default to aid
        }
    }
    
    /**
     * Calculate recommended investment based on nation size.
     */
    public synchronized double calculateRecommendedInvestment(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 1000.0;
        
        // Base investment scales with nation size
        return n.getCitizens().size() * 100.0 + n.getClaimedChunkKeys().size() * 10.0;
    }
    
    /**
     * Get global crisis response statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCrisisResponseStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeResponses", responses.size());
        
        Map<String, Integer> responsesByType = new HashMap<>();
        Map<String, Integer> responsesByAction = new HashMap<>();
        double totalInvestment = 0.0;
        Map<String, Double> investmentByNation = new HashMap<>();
        
        for (Map.Entry<String, CrisisResponse> entry : responses.entrySet()) {
            CrisisResponse response = entry.getValue();
            
            responsesByType.put(response.crisisType, responsesByType.getOrDefault(response.crisisType, 0) + 1);
            responsesByAction.put(response.action, responsesByAction.getOrDefault(response.action, 0) + 1);
            totalInvestment += response.investment;
            investmentByNation.put(entry.getKey(), response.investment);
        }
        
        stats.put("responsesByType", responsesByType);
        stats.put("responsesByAction", responsesByAction);
        stats.put("totalInvestment", totalInvestment);
        stats.put("averageInvestment", responses.size() > 0 ? totalInvestment / responses.size() : 0);
        stats.put("investmentByNation", investmentByNation);
        stats.put("nationsResponding", responses.size());
        
        // Top investors
        List<Map.Entry<String, Double>> topInvestors = investmentByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topInvestors", topInvestors);
        
        // Most common crisis types
        List<Map.Entry<String, Integer>> mostCommonTypes = responsesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTypes", mostCommonTypes);
        
        // Most common actions
        List<Map.Entry<String, Integer>> mostCommonActions = responsesByAction.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonActions", mostCommonActions);
        
        return stats;
    }
}

