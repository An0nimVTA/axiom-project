package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Manages research funding and project allocation. */
public class ResearchFundingService {
    private final AXIOM plugin;
    private final File fundingDir;
    private final Map<String, ResearchBudget> budgets = new HashMap<>(); // nationId -> budget

    public static class ResearchBudget {
        double totalBudget;
        Map<String, Double> projectFunding = new HashMap<>(); // projectId -> funding
        double military;
        double economy;
        double science;
        double medicine;
    }

    public ResearchFundingService(AXIOM plugin) {
        this.plugin = plugin;
        this.fundingDir = new File(plugin.getDataFolder(), "researchfunding");
        this.fundingDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processFunding, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String allocateBudget(String nationId, double totalBudget, double military, double economy, double science, double medicine) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < totalBudget) return "Недостаточно средств.";
        ResearchBudget budget = budgets.computeIfAbsent(nationId, k -> new ResearchBudget());
        budget.totalBudget = totalBudget;
        budget.military = military;
        budget.economy = economy;
        budget.science = science;
        budget.medicine = medicine;
        n.setTreasury(n.getTreasury() - totalBudget);
        try {
            plugin.getNationManager().save(n);
            saveBudget(nationId, budget);
        } catch (Exception ignored) {}
        return "Бюджет исследований выделен.";
    }

    private void processFunding() {
        for (Map.Entry<String, ResearchBudget> e : budgets.entrySet()) {
            ResearchBudget budget = e.getValue();
            // Research progresses based on funding
            if (budget.military > 0) {
                plugin.getTechnologyTreeService().addResearchPoints(e.getKey(), "military", budget.military * 0.1);
            }
            if (budget.economy > 0) {
                plugin.getTechnologyTreeService().addResearchPoints(e.getKey(), "economy", budget.economy * 0.1);
            }
            if (budget.science > 0) {
                plugin.getEducationService().addResearchProgress(e.getKey(), budget.science * 0.1);
            }
        }
    }

    private void loadAll() {
        File[] files = fundingDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                ResearchBudget budget = new ResearchBudget();
                budget.totalBudget = o.get("totalBudget").getAsDouble();
                budget.military = o.get("military").getAsDouble();
                budget.economy = o.get("economy").getAsDouble();
                budget.science = o.get("science").getAsDouble();
                budget.medicine = o.has("medicine") ? o.get("medicine").getAsDouble() : 0.0;
                if (o.has("projectFunding")) {
                    for (var entry : o.getAsJsonObject("projectFunding").entrySet()) {
                        budget.projectFunding.put(entry.getKey(), entry.getValue().getAsDouble());
                    }
                }
                budgets.put(nationId, budget);
            } catch (Exception ignored) {}
        }
    }

    private void saveBudget(String nationId, ResearchBudget budget) {
        File f = new File(fundingDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("totalBudget", budget.totalBudget);
        o.addProperty("military", budget.military);
        o.addProperty("economy", budget.economy);
        o.addProperty("science", budget.science);
        o.addProperty("medicine", budget.medicine);
        JsonObject projectFunding = new JsonObject();
        for (var entry : budget.projectFunding.entrySet()) {
            projectFunding.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("projectFunding", projectFunding);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive research funding statistics for a nation.
     */
    public synchronized Map<String, Object> getResearchFundingStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        ResearchBudget budget = budgets.get(nationId);
        if (budget == null || budget.totalBudget == 0) {
            stats.put("hasBudget", false);
            stats.put("totalBudget", 0);
            return stats;
        }
        
        stats.put("hasBudget", true);
        stats.put("totalBudget", budget.totalBudget);
        stats.put("military", budget.military);
        stats.put("economy", budget.economy);
        stats.put("science", budget.science);
        stats.put("medicine", budget.medicine);
        stats.put("projectFunding", new HashMap<>(budget.projectFunding));
        stats.put("totalProjectFunding", budget.projectFunding.values().stream().mapToDouble(Double::doubleValue).sum());
        
        // Funding distribution percentages
        double total = budget.military + budget.economy + budget.science + budget.medicine;
        if (total > 0) {
            stats.put("militaryPercent", (budget.military / total) * 100);
            stats.put("economyPercent", (budget.economy / total) * 100);
            stats.put("sciencePercent", (budget.science / total) * 100);
            stats.put("medicinePercent", (budget.medicine / total) * 100);
        }
        
        // Funding rating
        String rating = "МИНИМАЛЬНЫЙ";
        if (budget.totalBudget >= 100000) rating = "МАССИВНЫЙ";
        else if (budget.totalBudget >= 50000) rating = "КРУПНЫЙ";
        else if (budget.totalBudget >= 20000) rating = "ЗНАЧИТЕЛЬНЫЙ";
        else if (budget.totalBudget >= 10000) rating = "СРЕДНИЙ";
        else if (budget.totalBudget >= 5000) rating = "УМЕРЕННЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global research funding statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalResearchFundingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalNations = budgets.size();
        double totalBudget = 0.0;
        double totalMilitary = 0.0;
        double totalEconomy = 0.0;
        double totalScience = 0.0;
        double totalMedicine = 0.0;
        Map<String, Double> budgetByNation = new HashMap<>();
        Map<String, Double> militaryByNation = new HashMap<>();
        Map<String, Double> economyByNation = new HashMap<>();
        Map<String, Double> scienceByNation = new HashMap<>();
        
        for (Map.Entry<String, ResearchBudget> entry : budgets.entrySet()) {
            String nationId = entry.getKey();
            ResearchBudget budget = entry.getValue();
            
            totalBudget += budget.totalBudget;
            totalMilitary += budget.military;
            totalEconomy += budget.economy;
            totalScience += budget.science;
            totalMedicine += budget.medicine;
            
            budgetByNation.put(nationId, budget.totalBudget);
            militaryByNation.put(nationId, budget.military);
            economyByNation.put(nationId, budget.economy);
            scienceByNation.put(nationId, budget.science);
        }
        
        stats.put("totalNations", totalNations);
        stats.put("totalBudget", totalBudget);
        stats.put("totalMilitary", totalMilitary);
        stats.put("totalEconomy", totalEconomy);
        stats.put("totalScience", totalScience);
        stats.put("totalMedicine", totalMedicine);
        stats.put("averageBudget", totalNations > 0 ? totalBudget / totalNations : 0);
        stats.put("budgetByNation", budgetByNation);
        stats.put("militaryByNation", militaryByNation);
        stats.put("economyByNation", economyByNation);
        stats.put("scienceByNation", scienceByNation);
        
        // Top nations by total budget
        List<Map.Entry<String, Double>> topByBudget = budgetByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByBudget", topByBudget);
        
        // Top nations by military funding
        List<Map.Entry<String, Double>> topByMilitary = militaryByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMilitary", topByMilitary);
        
        // Funding category distribution
        double globalTotal = totalMilitary + totalEconomy + totalScience + totalMedicine;
        if (globalTotal > 0) {
            stats.put("globalMilitaryPercent", (totalMilitary / globalTotal) * 100);
            stats.put("globalEconomyPercent", (totalEconomy / globalTotal) * 100);
            stats.put("globalSciencePercent", (totalScience / globalTotal) * 100);
            stats.put("globalMedicinePercent", (totalMedicine / globalTotal) * 100);
        }
        
        return stats;
    }
}

