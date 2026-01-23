package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;

/** Manages pandemic outbreaks and their spread between nations. */
public class PandemicService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, Pandemic> activePandemics = new HashMap<>(); // nationId -> pandemic

    public static class Pandemic {
        String nationId;
        String diseaseName;
        double severity; // 0-100
        double spreadRate;
        long startTime;
    }

    public PandemicService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::spreadPandemics, 0, 20 * 60 * 5); // every 5 minutes
    }

    public synchronized void triggerPandemic(String nationId, String diseaseName, double severity) {
        Pandemic p = new Pandemic();
        p.nationId = nationId;
        p.diseaseName = diseaseName;
        p.severity = severity;
        p.spreadRate = severity / 10.0;
        p.startTime = System.currentTimeMillis();
        activePandemics.put(nationId, p);
        applyPandemicEffects(p);
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Пандемия: " + diseaseName + " (тяжесть: " + severity + "%)");
            try { nationManager.save(n); } catch (Exception ignored) {}
        }
    }

    private void spreadPandemics() {
        List<String> toSpread = new ArrayList<>();
        for (var entry : activePandemics.entrySet()) {
            Pandemic p = entry.getValue();
            // Spread to allies and neighbors
            Nation n = nationManager.getNationById(p.nationId);
            if (n != null && Math.random() < p.spreadRate / 100.0) {
                for (String allyId : n.getAllies()) {
                    if (!activePandemics.containsKey(allyId) && Math.random() < 0.3) {
                        toSpread.add(allyId);
                    }
                }
            }
        }
        for (String targetId : toSpread) {
            Pandemic source = activePandemics.values().iterator().next();
            triggerPandemic(targetId, source.diseaseName, source.severity * 0.8);
        }
    }

    private void applyPandemicEffects(Pandemic p) {
        // Reduce happiness and economy
        plugin.getNationModifierService().addModifier(p.nationId, "economy", "penalty", p.severity / 200.0, 1440);
        
        // VISUAL EFFECTS: Broadcast pandemic warning with effects
        Nation n = nationManager.getNationById(p.nationId);
        if (n != null) {
            String msg = "§c[ПАНДЕМИЯ] §f" + p.diseaseName + " распространяется в " + n.getName() + "!";
            
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID citizen : n.getCitizens()) {
                    org.bukkit.entity.Player pl = Bukkit.getPlayer(citizen);
                    if (pl != null && pl.isOnline()) {
                        // Title announcement
                        pl.sendTitle("§c§l[ПАНДЕМИЯ]", "§f" + p.diseaseName + " в вашей нации!", 10, 100, 20);
                        
                        // Actionbar
                        plugin.getVisualEffectsService().sendActionBar(pl, msg);
                        
                        // Gray smoke particles (disease effect)
                        org.bukkit.Location loc = pl.getLocation();
                        loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc, 30, 2, 2, 2, 0.15);
                        
                        // Sound
                        pl.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.6f);
                    }
                }
            });
        }
    }

    public synchronized boolean hasPandemic(String nationId) {
        return activePandemics.containsKey(nationId);
    }
    
    /**
     * Get comprehensive pandemic statistics.
     */
    public synchronized Map<String, Object> getPandemicStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Pandemic pandemic = activePandemics.get(nationId);
        if (pandemic != null) {
            stats.put("hasPandemic", true);
            stats.put("diseaseName", pandemic.diseaseName);
            stats.put("severity", pandemic.severity);
            stats.put("spreadRate", pandemic.spreadRate);
            stats.put("startTime", pandemic.startTime);
            stats.put("duration", (System.currentTimeMillis() - pandemic.startTime) / 1000 / 60); // minutes
            
            // Pandemic severity rating
            String rating = "ЛЕГКАЯ";
            if (pandemic.severity >= 80) rating = "КРИТИЧЕСКАЯ";
            else if (pandemic.severity >= 60) rating = "ТЯЖЕЛАЯ";
            else if (pandemic.severity >= 40) rating = "СРЕДНЯЯ";
            else if (pandemic.severity >= 20) rating = "УМЕРЕННАЯ";
            stats.put("severityRating", rating);
            
            // Economic impact
            double economicImpact = -(pandemic.severity / 200.0); // -0.01% per 1% severity
            stats.put("economicImpact", economicImpact);
            
            // Population impact
            double populationImpact = -(pandemic.severity / 100.0); // -0.01% per 1% severity
            stats.put("populationImpact", populationImpact);
        } else {
            stats.put("hasPandemic", false);
            
            // Check risk of pandemic (based on neighbors with pandemics)
            int neighborsWithPandemics = 0;
            Nation n = nationManager.getNationById(nationId);
            if (n != null) {
                for (String allyId : n.getAllies()) {
                    if (activePandemics.containsKey(allyId)) {
                        neighborsWithPandemics++;
                    }
                }
            }
            stats.put("riskLevel", neighborsWithPandemics * 10); // Risk % based on neighbors
            stats.put("neighborsWithPandemics", neighborsWithPandemics);
        }
        
        return stats;
    }
    
    /**
     * End pandemic (through treatment, etc.).
     */
    public synchronized String endPandemic(String nationId, double cost) throws IOException {
        Pandemic pandemic = activePandemics.remove(nationId);
        if (pandemic == null) return "Пандемии нет.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        n.setTreasury(n.getTreasury() - cost);
        n.getHistory().add("Пандемия " + pandemic.diseaseName + " побеждена");
        nationManager.save(n);
        
        return "Пандемия прекращена.";
    }
    
    /**
     * Reduce pandemic severity (through medical measures).
     */
    public synchronized String reduceSeverity(String nationId, double reduction, double cost) throws IOException {
        Pandemic pandemic = activePandemics.get(nationId);
        if (pandemic == null) return "Пандемии нет.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        pandemic.severity = Math.max(0, pandemic.severity - reduction);
        pandemic.spreadRate = pandemic.severity / 10.0;
        
        n.setTreasury(n.getTreasury() - cost);
        if (pandemic.severity <= 0) {
            activePandemics.remove(nationId);
            n.getHistory().add("Пандемия полностью побеждена");
        } else {
            n.getHistory().add("Тяжесть пандемии снижена до " + String.format("%.1f", pandemic.severity) + "%");
        }
        
        nationManager.save(n);
        
        return "Тяжесть пандемии: " + String.format("%.1f", pandemic.severity) + "%";
    }
    
    /**
     * Get pandemic severity.
     */
    public synchronized double getPandemicSeverity(String nationId) {
        Pandemic pandemic = activePandemics.get(nationId);
        return pandemic != null ? pandemic.severity : 0.0;
    }
    
    /**
     * Calculate treatment cost based on severity.
     */
    public synchronized double calculateTreatmentCost(String nationId) {
        Pandemic pandemic = activePandemics.get(nationId);
        if (pandemic == null) return 0.0;
        
        // Base cost scales with severity
        return pandemic.severity * 50.0;
    }
    
    /**
     * Get global pandemic statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPandemicStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activePandemics", activePandemics.size());
        
        Map<String, Integer> pandemicsByDisease = new HashMap<>();
        double totalSeverity = 0.0;
        double totalSpreadRate = 0.0;
        Map<String, Double> severityByNation = new HashMap<>();
        
        for (Map.Entry<String, Pandemic> entry : activePandemics.entrySet()) {
            Pandemic pandemic = entry.getValue();
            
            pandemicsByDisease.put(pandemic.diseaseName, pandemicsByDisease.getOrDefault(pandemic.diseaseName, 0) + 1);
            totalSeverity += pandemic.severity;
            totalSpreadRate += pandemic.spreadRate;
            severityByNation.put(entry.getKey(), pandemic.severity);
        }
        
        stats.put("totalActivePandemics", activePandemics.size());
        stats.put("pandemicsByDisease", pandemicsByDisease);
        stats.put("averageSeverity", activePandemics.size() > 0 ? totalSeverity / activePandemics.size() : 0);
        stats.put("averageSpreadRate", activePandemics.size() > 0 ? totalSpreadRate / activePandemics.size() : 0);
        stats.put("nationsWithPandemics", activePandemics.size());
        
        // Severity distribution
        int critical = 0, severe = 0, moderate = 0, mild = 0;
        for (Pandemic pandemic : activePandemics.values()) {
            if (pandemic.severity >= 80) critical++;
            else if (pandemic.severity >= 60) severe++;
            else if (pandemic.severity >= 40) moderate++;
            else mild++;
        }
        
        Map<String, Integer> severityDistribution = new HashMap<>();
        severityDistribution.put("critical", critical);
        severityDistribution.put("severe", severe);
        severityDistribution.put("moderate", moderate);
        severityDistribution.put("mild", mild);
        stats.put("severityDistribution", severityDistribution);
        
        // Top nations by severity (worst)
        List<Map.Entry<String, Double>> topBySeverity = severityByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySeverity", topBySeverity);
        
        // Most common diseases
        List<Map.Entry<String, Integer>> mostCommonDiseases = pandemicsByDisease.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonDiseases", mostCommonDiseases);
        
        // Calculate nations at risk
        int nationsAtRisk = 0;
        for (Nation n : nationManager.getAll()) {
            if (!activePandemics.containsKey(n.getId())) {
                int neighborsWithPandemics = 0;
                for (String allyId : n.getAllies()) {
                    if (activePandemics.containsKey(allyId)) {
                        neighborsWithPandemics++;
                    }
                }
                if (neighborsWithPandemics > 0) {
                    nationsAtRisk++;
                }
            }
        }
        stats.put("nationsAtRisk", nationsAtRisk);
        
        return stats;
    }
}

