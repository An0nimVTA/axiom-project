package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;

import java.util.*;

/** Extended trade embargo system with more features. */
public class TradeEmbargoExpansionService {
    private final AXIOM plugin;
    private final Map<String, Set<String>> embargoes = new HashMap<>(); // embargoerId -> set of embargoed nation IDs
    private final Map<String, Double> embargoSeverity = new HashMap<>(); // embargoerId_embargoedId -> severity (0-100)

    public TradeEmbargoExpansionService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized String imposeEmbargo(String embargoerId, String targetId, double severity, double cost) {
        Nation embargoer = plugin.getNationManager().getNationById(embargoerId);
        if (embargoer == null) return "Нация не найдена.";
        if (embargoer.getTreasury() < cost) return "Недостаточно средств.";
        embargoes.computeIfAbsent(embargoerId, k -> new HashSet<>()).add(targetId);
        embargoSeverity.put(embargoerId + "_" + targetId, Math.max(0, Math.min(100, severity)));
        embargoer.setTreasury(embargoer.getTreasury() - cost);
        Nation target = plugin.getNationManager().getNationById(targetId);
        if (target != null) {
            target.getHistory().add("Торговое эмбарго от " + embargoer.getName() + " (тяжесть: " + severity + "%)");
            try { plugin.getNationManager().save(target); } catch (Exception ignored) {}
        }
        try {
            plugin.getNationManager().save(embargoer);
        } catch (Exception ignored) {}
        return "Эмбарго наложено (тяжесть: " + severity + "%)";
    }

    public synchronized boolean isEmbargoed(String embargoerId, String targetId) {
        Set<String> embargoed = embargoes.get(embargoerId);
        return embargoed != null && embargoed.contains(targetId);
    }

    public synchronized double getEmbargoSeverity(String embargoerId, String targetId) {
        return embargoSeverity.getOrDefault(embargoerId + "_" + targetId, 0.0);
    }

    public synchronized String liftEmbargo(String embargoerId, String targetId) {
        Set<String> embargoed = embargoes.get(embargoerId);
        if (embargoed == null || !embargoed.remove(targetId)) return "Эмбарго не найдено.";
        embargoSeverity.remove(embargoerId + "_" + targetId);
        return "Эмбарго снято.";
    }

    public synchronized double getTradePenalty(String nationA, String nationB) {
        // Check if either nation has embargo on the other
        double penalty = 0.0;
        if (isEmbargoed(nationA, nationB)) {
            penalty += getEmbargoSeverity(nationA, nationB);
        }
        if (isEmbargoed(nationB, nationA)) {
            penalty += getEmbargoSeverity(nationB, nationA);
        }
        return Math.min(100, penalty); // Max 100% penalty = complete trade block
    }
    
    /**
     * Get comprehensive trade embargo expansion statistics for a nation.
     */
    public synchronized Map<String, Object> getTradeEmbargoExpansionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> embargoed = embargoes.get(nationId);
        if (embargoed == null || embargoed.isEmpty()) {
            stats.put("hasEmbargoes", false);
            stats.put("totalEmbargoes", 0);
            return stats;
        }
        
        int total = embargoed.size();
        double totalSeverity = 0.0;
        Map<String, Double> severityByTarget = new HashMap<>();
        
        for (String targetId : embargoed) {
            double severity = getEmbargoSeverity(nationId, targetId);
            totalSeverity += severity;
            severityByTarget.put(targetId, severity);
        }
        
        stats.put("hasEmbargoes", true);
        stats.put("totalEmbargoes", total);
        stats.put("averageSeverity", total > 0 ? totalSeverity / total : 0);
        stats.put("severityByTarget", severityByTarget);
        
        // Embargo rating
        String rating = "НЕТ ЭМБАРГО";
        if (total >= 10) rating = "АГРЕССИВНЫЙ";
        else if (total >= 5) rating = "АКТИВНЫЙ";
        else if (total >= 3) rating = "УМЕРЕННЫЙ";
        else if (total >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global trade embargo expansion statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalTradeEmbargoExpansionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalEmbargoes = 0;
        double totalSeverity = 0.0;
        Map<String, Integer> embargoesByNation = new HashMap<>(); // embargoer -> count
        Map<String, Integer> embargoedByNation = new HashMap<>(); // target -> count received
        Map<String, Double> averageSeverityByNation = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : embargoes.entrySet()) {
            String embargoerId = entry.getKey();
            Set<String> targets = entry.getValue();
            
            int count = targets.size();
            totalEmbargoes += count;
            embargoesByNation.put(embargoerId, count);
            
            double nationSeverity = 0.0;
            for (String targetId : targets) {
                embargoedByNation.put(targetId, embargoedByNation.getOrDefault(targetId, 0) + 1);
                
                double severity = getEmbargoSeverity(embargoerId, targetId);
                totalSeverity += severity;
                nationSeverity += severity;
            }
            
            averageSeverityByNation.put(embargoerId, count > 0 ? nationSeverity / count : 0);
        }
        
        stats.put("totalEmbargoes", totalEmbargoes);
        stats.put("averageSeverity", totalEmbargoes > 0 ? totalSeverity / totalEmbargoes : 0);
        stats.put("embargoesByNation", embargoesByNation);
        stats.put("embargoedByNation", embargoedByNation);
        stats.put("averageSeverityByNation", averageSeverityByNation);
        stats.put("nationsImposingEmbargoes", embargoesByNation.size());
        stats.put("nationsEmbargoed", embargoedByNation.size());
        
        // Top nations imposing embargoes
        List<Map.Entry<String, Integer>> topByImposing = embargoesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByImposing", topByImposing);
        
        // Most embargoed nations
        List<Map.Entry<String, Integer>> topByEmbargoed = embargoedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByEmbargoed", topByEmbargoed);
        
        return stats;
    }
}

