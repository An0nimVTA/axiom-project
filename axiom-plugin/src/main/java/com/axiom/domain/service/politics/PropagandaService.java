package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

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
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(targetNationId)) return "Неверный идентификатор нации.";
        if (isBlank(message)) return "Сообщение кампании не может быть пустым.";
        if (durationMinutes <= 0) return "Длительность кампании должна быть больше нуля.";
        Nation n = nationManager.getNationById(nationId);
        Nation target = nationManager.getNationById(targetNationId);
        if (n == null || target == null) return "Нация не найдена.";
        double cost = durationMinutes * 10.0;
        if (!Double.isFinite(cost)) return "Некорректная стоимость кампании.";
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
        if (nationManager == null || isBlank(targetNationId) || message == null) return;
        Nation target = nationManager.getNationById(targetNationId);
        if (target == null) return;
        String formatted = "§e[Пропаганда] §f" + message;
        Collection<UUID> citizens = target.getCitizens();
        if (citizens == null) return;
        for (UUID citizen : citizens) {
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

        cleanupExpired(System.currentTimeMillis());

        int activeCampaignsCount = 0;
        double totalCost = 0.0;
        List<Map<String, Object>> campaigns = new ArrayList<>();

        for (PropagandaCampaign c : activeCampaigns.values()) {
            if (c != null && Objects.equals(c.nationId, nationId)) {
                activeCampaignsCount++;
                totalCost += c.cost;

                Map<String, Object> campaignInfo = new HashMap<>();
                campaignInfo.put("targetNationId", c.targetNationId);
                campaignInfo.put("message", c.message);
                campaignInfo.put("durationMinutes", c.durationMinutes);
                campaignInfo.put("timeRemaining", Math.max(0, (getCampaignEndTime(c) - System.currentTimeMillis()) / 1000 / 60));
                campaigns.add(campaignInfo);
            }
        }
        
        stats.put("activeCampaigns", activeCampaignsCount);
        stats.put("totalCost", totalCost);
        stats.put("campaigns", campaigns);
        
        // Count campaigns targeting this nation
        int targetedBy = 0;
        for (PropagandaCampaign c : activeCampaigns.values()) {
            if (c != null && Objects.equals(c.targetNationId, nationId)) {
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
        cleanupExpired(System.currentTimeMillis());
        for (PropagandaCampaign c : activeCampaigns.values()) {
            if (c != null && Objects.equals(c.nationId, nationId)) {
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
        Nation n = nationManager != null ? nationManager.getNationById(nationId) : null;
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

        cleanupExpired(System.currentTimeMillis());
        stats.put("totalActiveCampaigns", activeCampaigns.size());
        
        Map<String, Integer> campaignsByNation = new HashMap<>();
        Map<String, Integer> targetedByNation = new HashMap<>();
        double totalCost = 0.0;

        for (PropagandaCampaign c : activeCampaigns.values()) {
            if (c == null) continue;
            campaignsByNation.put(c.nationId, campaignsByNation.getOrDefault(c.nationId, 0) + 1);
            if (c.targetNationId != null) {
                targetedByNation.put(c.targetNationId, targetedByNation.getOrDefault(c.targetNationId, 0) + 1);
            }
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

    private void cleanupExpired(long now) {
        Iterator<Map.Entry<String, PropagandaCampaign>> iterator = activeCampaigns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PropagandaCampaign> entry = iterator.next();
            PropagandaCampaign campaign = entry.getValue();
            if (campaign == null) {
                iterator.remove();
                continue;
            }
            if (now >= getCampaignEndTime(campaign)) {
                iterator.remove();
            }
        }
    }

    private long getCampaignEndTime(PropagandaCampaign campaign) {
        if (campaign == null) return 0L;
        long durationMillis;
        try {
            durationMillis = Math.multiplyExact(campaign.durationMinutes, 60_000L);
        } catch (ArithmeticException e) {
            durationMillis = Long.MAX_VALUE;
        }
        if (campaign.startTime > Long.MAX_VALUE - durationMillis) return Long.MAX_VALUE;
        return campaign.startTime + durationMillis;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

