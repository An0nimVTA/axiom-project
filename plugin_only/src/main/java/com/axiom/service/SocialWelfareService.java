package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages social welfare programs and benefits. */
public class SocialWelfareService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File welfareDir;
    private final Map<String, WelfareProgram> programs = new HashMap<>(); // nationId -> program

    public static class WelfareProgram {
        String nationId;
        double unemploymentBenefits;
        double healthcareCoverage; // 0-100%
        double pensionAmount;
        double educationSubsidies;
        double totalCost;
        boolean active;
    }

    public SocialWelfareService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.welfareDir = new File(plugin.getDataFolder(), "welfare");
        this.welfareDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processWelfare, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String setupProgram(String nationId, double unemployment, double healthcare, double pension, double education) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        WelfareProgram program = programs.computeIfAbsent(nationId, k -> new WelfareProgram());
        program.nationId = nationId;
        program.unemploymentBenefits = unemployment;
        program.healthcareCoverage = Math.max(0, Math.min(100, healthcare));
        program.pensionAmount = pension;
        program.educationSubsidies = education;
        program.totalCost = (unemployment + healthcare + pension + education) * n.getCitizens().size();
        program.active = true;
        n.getHistory().add("Социальная программа создана");
        try {
            nationManager.save(n);
            saveProgram(nationId, program);
        } catch (Exception ignored) {}
        return "Социальная программа установлена. Стоимость: " + program.totalCost;
    }

    private void processWelfare() {
        for (Map.Entry<String, WelfareProgram> e : programs.entrySet()) {
            WelfareProgram program = e.getValue();
            if (!program.active) continue;
            Nation n = nationManager.getNationById(e.getKey());
            if (n == null) continue;
            // Pay welfare costs
            if (n.getTreasury() >= program.totalCost) {
                n.setTreasury(n.getTreasury() - program.totalCost);
                // Welfare boosts happiness
                double happinessBoost = (program.healthcareCoverage / 100.0) * 10.0;
                plugin.getHappinessService().modifyHappiness(e.getKey(), happinessBoost);
                try { nationManager.save(n); } catch (Exception ignored) {}
            } else {
                // Can't afford - reduce happiness
                plugin.getHappinessService().modifyHappiness(e.getKey(), -5.0);
            }
        }
    }

    private void loadAll() {
        File[] files = welfareDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                WelfareProgram program = new WelfareProgram();
                program.nationId = nationId;
                program.unemploymentBenefits = o.get("unemploymentBenefits").getAsDouble();
                program.healthcareCoverage = o.get("healthcareCoverage").getAsDouble();
                program.pensionAmount = o.get("pensionAmount").getAsDouble();
                program.educationSubsidies = o.get("educationSubsidies").getAsDouble();
                program.totalCost = o.get("totalCost").getAsDouble();
                program.active = o.has("active") && o.get("active").getAsBoolean();
                programs.put(nationId, program);
            } catch (Exception ignored) {}
        }
    }

    private void saveProgram(String nationId, WelfareProgram program) {
        File f = new File(welfareDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", program.nationId);
        o.addProperty("unemploymentBenefits", program.unemploymentBenefits);
        o.addProperty("healthcareCoverage", program.healthcareCoverage);
        o.addProperty("pensionAmount", program.pensionAmount);
        o.addProperty("educationSubsidies", program.educationSubsidies);
        o.addProperty("totalCost", program.totalCost);
        o.addProperty("active", program.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive social welfare statistics for a nation.
     */
    public synchronized Map<String, Object> getSocialWelfareStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        WelfareProgram program = programs.get(nationId);
        if (program == null || !program.active) {
            stats.put("hasProgram", false);
            return stats;
        }
        
        stats.put("hasProgram", true);
        stats.put("unemploymentBenefits", program.unemploymentBenefits);
        stats.put("healthcareCoverage", program.healthcareCoverage);
        stats.put("pensionAmount", program.pensionAmount);
        stats.put("educationSubsidies", program.educationSubsidies);
        stats.put("totalCost", program.totalCost);
        
        Nation n = nationManager.getNationById(nationId);
        double costPerCitizen = n != null && n.getCitizens().size() > 0 ? 
            program.totalCost / n.getCitizens().size() : 0;
        stats.put("costPerCitizen", costPerCitizen);
        
        // Welfare rating
        String rating = "НЕАКТИВНАЯ";
        if (program.active && program.healthcareCoverage >= 80) rating = "ВСЕОБЪЕМЛЮЩАЯ";
        else if (program.active && program.healthcareCoverage >= 60) rating = "РАЗВИТАЯ";
        else if (program.active && program.healthcareCoverage >= 40) rating = "БАЗОВАЯ";
        else if (program.active) rating = "МИНИМАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global social welfare statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalSocialWelfareStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPrograms = 0;
        int activePrograms = 0;
        double totalCost = 0.0;
        double totalUnemployment = 0.0;
        double totalHealthcare = 0.0;
        double totalPension = 0.0;
        double totalEducation = 0.0;
        Map<String, Double> costByNation = new HashMap<>();
        Map<String, Double> healthcareByNation = new HashMap<>();
        
        for (Map.Entry<String, WelfareProgram> entry : programs.entrySet()) {
            WelfareProgram program = entry.getValue();
            if (program.active) {
                totalPrograms++;
                activePrograms++;
                totalCost += program.totalCost;
                totalUnemployment += program.unemploymentBenefits;
                totalHealthcare += program.healthcareCoverage;
                totalPension += program.pensionAmount;
                totalEducation += program.educationSubsidies;
                
                costByNation.put(entry.getKey(), program.totalCost);
                healthcareByNation.put(entry.getKey(), program.healthcareCoverage);
            }
        }
        
        stats.put("totalPrograms", totalPrograms);
        stats.put("activePrograms", activePrograms);
        stats.put("totalCost", totalCost);
        stats.put("averageCost", activePrograms > 0 ? totalCost / activePrograms : 0);
        stats.put("averageUnemploymentBenefits", activePrograms > 0 ? totalUnemployment / activePrograms : 0);
        stats.put("averageHealthcareCoverage", activePrograms > 0 ? totalHealthcare / activePrograms : 0);
        stats.put("averagePensionAmount", activePrograms > 0 ? totalPension / activePrograms : 0);
        stats.put("averageEducationSubsidies", activePrograms > 0 ? totalEducation / activePrograms : 0);
        stats.put("costByNation", costByNation);
        stats.put("healthcareByNation", healthcareByNation);
        
        // Top nations by welfare spending
        List<Map.Entry<String, Double>> topByCost = costByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCost", topByCost);
        
        // Top nations by healthcare coverage
        List<Map.Entry<String, Double>> topByHealthcare = healthcareByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByHealthcare", topByHealthcare);
        
        return stats;
    }
}

