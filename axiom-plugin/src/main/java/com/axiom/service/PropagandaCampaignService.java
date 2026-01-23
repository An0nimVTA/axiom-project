package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages propaganda campaigns. */
public class PropagandaCampaignService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File campaignsDir;
    private final Map<String, PropagandaCampaign> activeCampaigns = new HashMap<>(); // campaignId -> campaign

    public static class PropagandaCampaign {
        String id;
        String nationId;
        String targetNationId; // null for internal
        String message;
        double effectiveness; // 0-100
        long startTime;
        long endTime;
        double cost;
    }

    public PropagandaCampaignService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.campaignsDir = new File(plugin.getDataFolder(), "propaganda");
        this.campaignsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processCampaigns, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String startCampaign(String nationId, String targetNationId, String message, int durationHours, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (isBlank(message)) return "Сообщение кампании не может быть пустым.";
        if (durationHours <= 0) return "Длительность кампании должна быть больше нуля.";
        if (cost < 0) return "Стоимость кампании не может быть отрицательной.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        String normalizedTargetId = isBlank(targetNationId) ? null : targetNationId;
        if (normalizedTargetId != null && nationManager.getNationById(normalizedTargetId) == null) {
            return "Целевая нация не найдена.";
        }
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        String campaignId = UUID.randomUUID().toString().substring(0, 8);
        PropagandaCampaign campaign = new PropagandaCampaign();
        campaign.id = campaignId;
        campaign.nationId = nationId;
        campaign.targetNationId = normalizedTargetId;
        campaign.message = message;
        campaign.effectiveness = 30.0; // Base effectiveness
        campaign.startTime = System.currentTimeMillis();
        campaign.endTime = System.currentTimeMillis() + durationHours * 60 * 60_000L;
        campaign.cost = cost;
        activeCampaigns.put(campaignId, campaign);
        n.setTreasury(n.getTreasury() - cost);
        if (campaign.targetNationId != null) {
            Nation target = nationManager.getNationById(campaign.targetNationId);
            if (target != null) {
                if (target.getHistory() != null) {
                    target.getHistory().add("Пропагандистская кампания от " + n.getName());
                }
            }
        } else {
            if (n.getHistory() != null) {
                n.getHistory().add("Внутренняя пропагандистская кампания начата");
            }
        }
        try {
            nationManager.save(n);
            if (campaign.targetNationId != null) {
                Nation target = nationManager.getNationById(campaign.targetNationId);
                if (target != null) nationManager.save(target);
            }
            saveCampaign(campaign);
        } catch (Exception ignored) {}
        return "Пропагандистская кампания начата.";
    }

    private synchronized void processCampaigns() {
        if (nationManager == null) return;
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, PropagandaCampaign> e : activeCampaigns.entrySet()) {
            PropagandaCampaign campaign = e.getValue();
            if (campaign == null || isBlank(campaign.nationId)) {
                expired.add(e.getKey());
                continue;
            }
            if (now >= campaign.endTime) {
                expired.add(e.getKey());
            } else {
                // Increase effectiveness
                campaign.effectiveness = Math.min(100, campaign.effectiveness + 2.0);
                // Apply effects
                if (campaign.targetNationId == null) {
                    // Internal - boost happiness
                    if (plugin.getHappinessService() != null) {
                        plugin.getHappinessService().modifyHappiness(campaign.nationId, campaign.effectiveness * 0.1);
                    }
                } else {
                    // External - reduce target's approval
                    double reduction = campaign.effectiveness * 0.2;
                    Nation target = nationManager.getNationById(campaign.targetNationId);
                    if (target != null && plugin.getPublicOpinionService() != null) {
                        plugin.getPublicOpinionService().setIssueSupport(campaign.targetNationId, "foreign_influence", -reduction);
                    }
                }
            }
        }
        for (String campaignId : expired) {
            activeCampaigns.remove(campaignId);
        }
    }

    private void loadAll() {
        File[] files = campaignsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                PropagandaCampaign campaign = new PropagandaCampaign();
                campaign.id = o.get("id").getAsString();
                campaign.nationId = o.get("nationId").getAsString();
                campaign.targetNationId = o.has("targetNationId") && !o.get("targetNationId").isJsonNull() ? o.get("targetNationId").getAsString() : null;
                campaign.message = o.get("message").getAsString();
                campaign.effectiveness = o.get("effectiveness").getAsDouble();
                campaign.startTime = o.get("startTime").getAsLong();
                campaign.endTime = o.get("endTime").getAsLong();
                campaign.cost = o.get("cost").getAsDouble();
                if (campaign.endTime > System.currentTimeMillis()) {
                    activeCampaigns.put(campaign.id, campaign);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveCampaign(PropagandaCampaign campaign) {
        File f = new File(campaignsDir, campaign.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", campaign.id);
        o.addProperty("nationId", campaign.nationId);
        if (campaign.targetNationId != null) o.addProperty("targetNationId", campaign.targetNationId);
        o.addProperty("message", campaign.message);
        o.addProperty("effectiveness", campaign.effectiveness);
        o.addProperty("startTime", campaign.startTime);
        o.addProperty("endTime", campaign.endTime);
        o.addProperty("cost", campaign.cost);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive propaganda campaign statistics for a nation.
     */
    public synchronized Map<String, Object> getPropagandaCampaignStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        int activeCampaignsCount = 0;
        int internalCampaigns = 0;
        int externalCampaigns = 0;
        double totalEffectiveness = 0.0;
        double totalCost = 0.0;
        Map<String, Integer> targetsByNation = new HashMap<>();
        long now = System.currentTimeMillis();
        
        for (PropagandaCampaign campaign : activeCampaigns.values()) {
            if (campaign != null && Objects.equals(campaign.nationId, nationId) && now < campaign.endTime) {
                activeCampaignsCount++;
                totalEffectiveness += campaign.effectiveness;
                totalCost += campaign.cost;
                
                if (campaign.targetNationId == null) {
                    internalCampaigns++;
                } else {
                    externalCampaigns++;
                    targetsByNation.put(campaign.targetNationId,
                        targetsByNation.getOrDefault(campaign.targetNationId, 0) + 1);
                }
            }
        }
        
        stats.put("activeCampaigns", activeCampaignsCount);
        stats.put("internalCampaigns", internalCampaigns);
        stats.put("externalCampaigns", externalCampaigns);
        stats.put("averageEffectiveness", activeCampaignsCount > 0 ? totalEffectiveness / activeCampaignsCount : 0);
        stats.put("totalCost", totalCost);
        stats.put("targetsByNation", targetsByNation);
        stats.put("uniqueTargets", targetsByNation.size());
        
        // Campaign rating
        String rating = "НЕТ КАМПАНИЙ";
        if (activeCampaignsCount >= 10) rating = "МАССОВАЯ";
        else if (activeCampaignsCount >= 5) rating = "ИНТЕНСИВНАЯ";
        else if (activeCampaignsCount >= 3) rating = "АКТИВНАЯ";
        else if (activeCampaignsCount >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global propaganda campaign statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPropagandaCampaignStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long now = System.currentTimeMillis();
        int totalActive = 0;
        int totalInternal = 0;
        int totalExternal = 0;
        double totalEffectiveness = 0.0;
        double totalCost = 0.0;
        Map<String, Integer> campaignsByNation = new HashMap<>();
        Map<String, Integer> targetsByNation = new HashMap<>();
        Map<String, Integer> campaignsByTarget = new HashMap<>();
        
        for (PropagandaCampaign campaign : activeCampaigns.values()) {
            if (campaign != null && now < campaign.endTime) {
                totalActive++;
                totalEffectiveness += campaign.effectiveness;
                totalCost += campaign.cost;
                
                campaignsByNation.put(campaign.nationId,
                    campaignsByNation.getOrDefault(campaign.nationId, 0) + 1);
                
                if (campaign.targetNationId == null) {
                    totalInternal++;
                } else {
                    totalExternal++;
                    targetsByNation.put(campaign.targetNationId,
                        targetsByNation.getOrDefault(campaign.targetNationId, 0) + 1);
                    campaignsByTarget.put(campaign.targetNationId,
                        campaignsByTarget.getOrDefault(campaign.targetNationId, 0) + 1);
                }
            }
        }
        
        stats.put("totalActiveCampaigns", totalActive);
        stats.put("totalInternalCampaigns", totalInternal);
        stats.put("totalExternalCampaigns", totalExternal);
        stats.put("averageEffectiveness", totalActive > 0 ? totalEffectiveness / totalActive : 0);
        stats.put("totalCost", totalCost);
        stats.put("campaignsByNation", campaignsByNation);
        stats.put("targetsByNation", targetsByNation);
        stats.put("campaignsByTarget", campaignsByTarget);
        stats.put("nationsWithCampaigns", campaignsByNation.size());
        stats.put("nationsBeingTargeted", targetsByNation.size());
        
        // Top nations by campaigns
        List<Map.Entry<String, Integer>> topByCampaigns = campaignsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCampaigns", topByCampaigns);
        
        // Most targeted nations
        List<Map.Entry<String, Integer>> topByTargeted = targetsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByTargeted", topByTargeted);
        
        // Campaign type distribution
        Map<String, Integer> typeDistribution = new HashMap<>();
        typeDistribution.put("internal", totalInternal);
        typeDistribution.put("external", totalExternal);
        stats.put("typeDistribution", typeDistribution);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

