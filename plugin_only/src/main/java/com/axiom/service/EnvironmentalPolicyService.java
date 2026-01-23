package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages environmental policies and regulations. */
public class EnvironmentalPolicyService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File policiesDir;
    private final Map<String, EnvironmentalPolicy> nationPolicies = new HashMap<>(); // nationId -> policy

    public static class EnvironmentalPolicy {
        String nationId;
        double pollutionLimit; // max allowed pollution
        double conservationLevel; // 0-100%
        double renewableEnergyTarget; // 0-100%
        boolean carbonTax;
        double carbonTaxRate;
    }

    public EnvironmentalPolicyService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.policiesDir = new File(plugin.getDataFolder(), "environmentalpolicies");
        this.policiesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processPolicies, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String setPolicy(String nationId, double pollutionLimit, double conservationLevel, boolean carbonTax, double taxRate) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        EnvironmentalPolicy policy = nationPolicies.computeIfAbsent(nationId, k -> new EnvironmentalPolicy());
        policy.nationId = nationId;
        policy.pollutionLimit = pollutionLimit;
        policy.conservationLevel = Math.max(0, Math.min(100, conservationLevel));
        policy.carbonTax = carbonTax;
        policy.carbonTaxRate = carbonTax ? taxRate : 0.0;
        double currentPollution = plugin.getPollutionService().getPollution(nationId);
        if (currentPollution > policy.pollutionLimit) {
            // Penalty for exceeding limit
            plugin.getHappinessService().modifyHappiness(nationId, -10.0);
        }
        n.getHistory().add("Экологическая политика обновлена");
        try {
            nationManager.save(n);
            savePolicy(nationId, policy);
        } catch (Exception ignored) {}
        return "Экологическая политика установлена.";
    }

    private void processPolicies() {
        for (Map.Entry<String, EnvironmentalPolicy> e : nationPolicies.entrySet()) {
            EnvironmentalPolicy policy = e.getValue();
            double pollution = plugin.getPollutionService().getPollution(e.getKey());
            if (pollution > policy.pollutionLimit) {
                // Apply carbon tax if enabled
                if (policy.carbonTax) {
                    Nation n = nationManager.getNationById(e.getKey());
                    if (n != null) {
                        double tax = pollution * policy.carbonTaxRate * 0.01;
                        n.setTreasury(Math.max(0, n.getTreasury() - tax));
                        try { nationManager.save(n); } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void loadAll() {
        File[] files = policiesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                EnvironmentalPolicy policy = new EnvironmentalPolicy();
                policy.nationId = nationId;
                policy.pollutionLimit = o.get("pollutionLimit").getAsDouble();
                policy.conservationLevel = o.get("conservationLevel").getAsDouble();
                policy.renewableEnergyTarget = o.has("renewableEnergyTarget") ? o.get("renewableEnergyTarget").getAsDouble() : 0.0;
                policy.carbonTax = o.has("carbonTax") && o.get("carbonTax").getAsBoolean();
                policy.carbonTaxRate = o.has("carbonTaxRate") ? o.get("carbonTaxRate").getAsDouble() : 0.0;
                nationPolicies.put(nationId, policy);
            } catch (Exception ignored) {}
        }
    }

    private void savePolicy(String nationId, EnvironmentalPolicy policy) {
        File f = new File(policiesDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", policy.nationId);
        o.addProperty("pollutionLimit", policy.pollutionLimit);
        o.addProperty("conservationLevel", policy.conservationLevel);
        o.addProperty("renewableEnergyTarget", policy.renewableEnergyTarget);
        o.addProperty("carbonTax", policy.carbonTax);
        o.addProperty("carbonTaxRate", policy.carbonTaxRate);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive environmental policy statistics for a nation.
     */
    public synchronized Map<String, Object> getEnvironmentalPolicyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        EnvironmentalPolicy policy = nationPolicies.get(nationId);
        if (policy == null) {
            stats.put("hasPolicy", false);
            return stats;
        }
        
        stats.put("hasPolicy", true);
        stats.put("pollutionLimit", policy.pollutionLimit);
        stats.put("conservationLevel", policy.conservationLevel);
        stats.put("renewableEnergyTarget", policy.renewableEnergyTarget);
        stats.put("carbonTax", policy.carbonTax);
        stats.put("carbonTaxRate", policy.carbonTaxRate);
        
        double currentPollution = plugin.getPollutionService().getPollution(nationId);
        stats.put("currentPollution", currentPollution);
        stats.put("withinLimit", currentPollution <= policy.pollutionLimit);
        stats.put("pollutionRatio", policy.pollutionLimit > 0 ? currentPollution / policy.pollutionLimit : 0);
        
        // Policy rating
        String rating = "НЕТ ПОЛИТИКИ";
        if (policy.conservationLevel >= 80 && policy.renewableEnergyTarget >= 80) rating = "ЭКОЛОГИЧЕСКАЯ";
        else if (policy.conservationLevel >= 60 || policy.renewableEnergyTarget >= 60) rating = "ЗЕЛЁНАЯ";
        else if (policy.conservationLevel >= 40 || policy.renewableEnergyTarget >= 40) rating = "УМЕРЕННАЯ";
        else if (policy.conservationLevel >= 20 || policy.renewableEnergyTarget >= 20) rating = "ОСНОВНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global environmental policy statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalEnvironmentalPolicyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPolicies = nationPolicies.size();
        int nationsWithCarbonTax = 0;
        double totalConservationLevel = 0.0;
        double totalRenewableTarget = 0.0;
        double totalPollutionLimit = 0.0;
        int nationsWithinLimit = 0;
        Map<String, Double> conservationByNation = new HashMap<>();
        Map<String, Double> renewableByNation = new HashMap<>();
        Map<String, Boolean> carbonTaxByNation = new HashMap<>();
        
        for (Map.Entry<String, EnvironmentalPolicy> entry : nationPolicies.entrySet()) {
            String nationId = entry.getKey();
            EnvironmentalPolicy policy = entry.getValue();
            
            totalConservationLevel += policy.conservationLevel;
            totalRenewableTarget += policy.renewableEnergyTarget;
            totalPollutionLimit += policy.pollutionLimit;
            if (policy.carbonTax) nationsWithCarbonTax++;
            
            conservationByNation.put(nationId, policy.conservationLevel);
            renewableByNation.put(nationId, policy.renewableEnergyTarget);
            carbonTaxByNation.put(nationId, policy.carbonTax);
            
            double currentPollution = plugin.getPollutionService().getPollution(nationId);
            if (currentPollution <= policy.pollutionLimit) {
                nationsWithinLimit++;
            }
        }
        
        stats.put("totalPolicies", totalPolicies);
        stats.put("nationsWithCarbonTax", nationsWithCarbonTax);
        stats.put("averageConservationLevel", totalPolicies > 0 ? totalConservationLevel / totalPolicies : 0);
        stats.put("averageRenewableTarget", totalPolicies > 0 ? totalRenewableTarget / totalPolicies : 0);
        stats.put("averagePollutionLimit", totalPolicies > 0 ? totalPollutionLimit / totalPolicies : 0);
        stats.put("nationsWithinLimit", nationsWithinLimit);
        stats.put("complianceRate", totalPolicies > 0 ? (double) nationsWithinLimit / totalPolicies : 0);
        stats.put("conservationByNation", conservationByNation);
        stats.put("renewableByNation", renewableByNation);
        stats.put("carbonTaxByNation", carbonTaxByNation);
        
        // Top nations by conservation
        List<Map.Entry<String, Double>> topByConservation = conservationByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByConservation", topByConservation);
        
        // Top nations by renewable target
        List<Map.Entry<String, Double>> topByRenewable = renewableByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRenewable", topByRenewable);
        
        return stats;
    }
}

