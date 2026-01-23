package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;

/** Manages propaganda campaigns and information warfare. */
public class PropagandaService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, PropagandaCampaign> activeCampaigns = new HashMap<>(); // nationId -> campaign

    public static class PropagandaCampaign {
        String nationId;
        String targetNationId;
        String message;
        long startTime;
        long durationMinutes;
        double cost;
    }

    public PropagandaService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public synchronized String launchCampaign(String nationId, String targetNationId, String message, long durationMinutes) {
        Nation n = nationManager.getNationById(nationId);
        Nation target = nationManager.getNationById(targetNationId);
        if (n == null || target == null) return "Нация не найдена.";
        double cost = durationMinutes * 10.0;
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        PropagandaCampaign c = new PropagandaCampaign();
        c.nationId = nationId;
        c.targetNationId = targetNationId;
        c.message = message;
        c.startTime = System.currentTimeMillis();
        c.durationMinutes = durationMinutes;
        c.cost = cost;
        n.setTreasury(n.getTreasury() - cost);
        activeCampaigns.put(nationId + "_" + targetNationId, c);
        try {
            nationManager.save(n);
        } catch (Exception ignored) {}
        broadcastPropaganda(targetNationId, message);
        return "Пропагандистская кампания запущена.";
    }

    private void broadcastPropaganda(String targetNationId, String message) {
        Nation target = nationManager.getNationById(targetNationId);
        if (target == null) return;
        String formatted = "§e[Пропаганда] §f" + message;
        for (UUID citizen : target.getCitizens()) {
            org.bukkit.entity.Player p = Bukkit.getPlayer(citizen);
            if (p != null && p.isOnline()) {
                p.sendMessage(formatted);
            }
        }
    }
    
    /**
     * Get comprehensive propaganda statistics.
     */
    public synchronized Map<String, Object> getPropagandaStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        int activeCampaignsCount = 0;
        double totalCost = 0.0;
        List<Map<String, Object>> campaigns = new ArrayList<>();
        
        for (PropagandaCampaign c : activeCampaigns.values()) {
            if (c.nationId.equals(nationId)) {
                activeCampaignsCount++;
                totalCost += c.cost;
                
                Map<String, Object> campaignInfo = new HashMap<>();
                campaignInfo.put("targetNationId", c.targetNationId);
                campaignInfo.put("message", c.message);
                campaignInfo.put("durationMinutes", c.durationMinutes);
                campaignInfo.put("timeRemaining", Math.max(0, (c.startTime + c.durationMinutes * 60_000L - System.currentTimeMillis()) / 1000 / 60));
                campaigns.add(campaignInfo);
            }
        }
        
        stats.put("activeCampaigns", activeCampaignsCount);
        stats.put("totalCost", totalCost);
        stats.put("campaigns", campaigns);
        
        // Count campaigns targeting this nation
        int targetedBy = 0;
        for (PropagandaCampaign c : activeCampaigns.values()) {
            if (c.targetNationId.equals(nationId)) {
                targetedBy++;
            }
        }
        stats.put("targetedBy", targetedBy);
        
        return stats;
    }
    
    /**
     * End a propaganda campaign early.
     */
    public synchronized String endCampaign(String nationId, String targetNationId) throws IOException {
        String key = nationId + "_" + targetNationId;
        PropagandaCampaign c = activeCampaigns.remove(key);
        if (c == null) return "Кампания не найдена.";
        
        return "Пропагандистская кампания завершена.";
    }
    
    /**
     * Get all active campaigns for a nation.
     */
    public synchronized List<PropagandaCampaign> getActiveCampaigns(String nationId) {
        List<PropagandaCampaign> result = new ArrayList<>();
        for (PropagandaCampaign c : activeCampaigns.values()) {
            if (c.nationId.equals(nationId)) {
                result.add(c);
            }
        }
        return result;
    }
    
    /**
     * Calculate propaganda effectiveness.
     */
    public synchronized double getPropagandaEffectiveness(String nationId, String targetNationId) {
        // Base effectiveness
        double effectiveness = 0.5;
        
        // Cultural influence increases effectiveness
        if (plugin.getCultureService() != null) {
            double culturalInfluence = plugin.getCultureService().getCultureLevel(nationId);
            effectiveness += (culturalInfluence / 200.0); // Up to +0.5
        }
        
        // Economic power affects reach
        Nation n = nationManager.getNationById(nationId);
        if (n != null && n.getTreasury() > 50000) {
            effectiveness += 0.2; // +20% if rich
        }
        
        return Math.min(1.0, effectiveness);
    }
    
    /**
     * Get global propaganda statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPropagandaStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveCampaigns", activeCampaigns.size());
        
        Map<String, Integer> campaignsByNation = new HashMap<>();
        Map<String, Integer> targetedByNation = new HashMap<>();
        double totalCost = 0.0;
        
        for (PropagandaCampaign c : activeCampaigns.values()) {
            campaignsByNation.put(c.nationId, campaignsByNation.getOrDefault(c.nationId, 0) + 1);
            targetedByNation.put(c.targetNationId, targetedByNation.getOrDefault(c.targetNationId, 0) + 1);
            totalCost += c.cost;
        }
        
        stats.put("campaignsByNation", campaignsByNation);
        stats.put("targetedByNation", targetedByNation);
        stats.put("totalCost", totalCost);
        stats.put("averageCampaignCost", activeCampaigns.size() > 0 ? totalCost / activeCampaigns.size() : 0);
        
        // Top propagandists
        List<Map.Entry<String, Integer>> topPropagandists = campaignsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topPropagandists", topPropagandists);
        
        // Most targeted nations
        List<Map.Entry<String, Integer>> mostTargeted = targetedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostTargeted", mostTargeted);
        
        return stats;
    }
}

