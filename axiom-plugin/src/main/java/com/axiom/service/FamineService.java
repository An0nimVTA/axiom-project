package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;

/** Manages famine events affecting nations. */
public class FamineService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, Famine> activeFamines = new HashMap<>(); // nationId -> famine

    public static class Famine {
        String nationId;
        double severity; // 0-100
        long startTime;
        long durationMinutes;
    }

    public FamineService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkFamineConditions, 0, 20 * 60 * 15); // every 15 minutes
    }

    private void checkFamineConditions() {
        if (plugin.getResourceService() == null) return;
        // Check for food resource shortage
        for (Nation n : nationManager.getAll()) {
            if (hasFamine(n.getId())) continue;
            double food = plugin.getResourceService().getResource(n.getId(), "food");
            int population = n.getCitizens().size();
            if (food < population * 10 && Math.random() < 0.2) {
                triggerFamine(n.getId(), 30.0, 1440); // 30% severity, 24h
            }
        }
    }

    public synchronized void triggerFamine(String nationId, double severity, long durationMinutes) {
        Famine f = new Famine();
        f.nationId = nationId;
        f.severity = severity;
        f.startTime = System.currentTimeMillis();
        f.durationMinutes = durationMinutes;
        activeFamines.put(nationId, f);
        applyFamineEffects(f);
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Голод! Тяжесть: " + severity + "%");
            try { nationManager.save(n); } catch (Exception ignored) {}
        }
    }

    private void applyFamineEffects(Famine f) {
        // Reduce happiness significantly
        if (plugin.getNationModifierService() != null) {
            plugin.getNationModifierService().addModifier(f.nationId, "happiness", "penalty", f.severity / 100.0, f.durationMinutes);
        }
        // Reduce population
        if (plugin.getCityGrowthEngine() != null) {
            List<com.axiom.model.City> cities = plugin.getCityGrowthEngine().getCitiesOf(f.nationId);
            for (com.axiom.model.City c : cities) {
                int loss = (int)(c.getPopulation() * (f.severity / 200.0));
                c.setPopulation(Math.max(10, c.getPopulation() - loss));
            }
        }
        // Broadcast
        Nation n = nationManager.getNationById(f.nationId);
        if (n != null) {
            String msg = "§c[Голод] §fНация " + n.getName() + " страдает от голода!";
            for (UUID citizen : n.getCitizens()) {
                org.bukkit.entity.Player p = Bukkit.getPlayer(citizen);
                if (p != null && p.isOnline()) {
                    p.sendMessage(msg);
                }
            }
        }
    }

    public synchronized boolean hasFamine(String nationId) {
        Famine f = activeFamines.get(nationId);
        if (f == null) return false;
        long now = System.currentTimeMillis();
        if (now >= f.startTime + (f.durationMinutes * 60_000L)) {
            activeFamines.remove(nationId);
            return false;
        }
        return true;
    }
    
    /**
     * Get comprehensive famine statistics.
     */
    public synchronized Map<String, Object> getFamineStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Famine f = activeFamines.get(nationId);
        if (f == null) {
            stats.put("hasFamine", false);
            return stats;
        }
        
        long now = System.currentTimeMillis();
        long elapsed = (now - f.startTime) / 1000 / 60; // minutes
        long remaining = f.durationMinutes - elapsed;
        
        stats.put("hasFamine", true);
        stats.put("severity", f.severity);
        stats.put("startTime", f.startTime);
        stats.put("durationMinutes", f.durationMinutes);
        stats.put("elapsedMinutes", elapsed);
        stats.put("remainingMinutes", Math.max(0, remaining));
        stats.put("remainingHours", Math.max(0, remaining) / 60);
        
        // Calculate impact
        double happinessImpact = f.severity / 100.0;
        double populationImpact = f.severity / 200.0;
        stats.put("happinessImpact", happinessImpact);
        stats.put("populationImpact", populationImpact);
        
        // Famine rating
        String rating = "ЛЁГКИЙ";
        if (f.severity >= 80) rating = "КРИТИЧЕСКИЙ";
        else if (f.severity >= 60) rating = "ТЯЖЁЛЫЙ";
        else if (f.severity >= 40) rating = "СРЕДНИЙ";
        else if (f.severity >= 20) rating = "УМЕРЕННЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * End famine early (through aid/investment).
     */
    public synchronized String endFamine(String nationId, double cost) throws IOException {
        Famine f = activeFamines.get(nationId);
        if (f == null) return "Голода нет.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (cost <= 0) return "Неверная сумма.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        
        n.setTreasury(n.getTreasury() - cost);
        activeFamines.remove(nationId);
        nationManager.save(n);
        
        n.getHistory().add("Голод преодолён через помощь");
        nationManager.save(n);
        
        return "Голод преодолён.";
    }
    
    /**
     * Get all active famines.
     */
    public synchronized List<Famine> getActiveFamines() {
        long now = System.currentTimeMillis();
        return activeFamines.values().stream()
            .filter(f -> now < f.startTime + (f.durationMinutes * 60_000L))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate famine economic impact.
     */
    public synchronized double getFamineEconomicImpact(String nationId) {
        Famine f = activeFamines.get(nationId);
        if (f == null) return 0.0;
        
        // -1% economy per 10% severity
        return -(f.severity / 10.0) * 0.01;
    }
    
    /**
     * Get total population loss from famine.
     */
    public synchronized int getPopulationLoss(String nationId) {
        Famine f = activeFamines.get(nationId);
        if (f == null) return 0;
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 0;
        if (plugin.getCityGrowthEngine() == null) return 0;
        
        List<com.axiom.model.City> cities = plugin.getCityGrowthEngine().getCitiesOf(nationId);
        int totalPopulation = cities.stream().mapToInt(com.axiom.model.City::getPopulation).sum();
        
        return (int)(totalPopulation * (f.severity / 200.0));
    }
    
    /**
     * Get global famine statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalFamineStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveFamines", activeFamines.size());
        
        Map<String, Double> severityByNation = new HashMap<>();
        double totalSeverity = 0.0;
        long totalDuration = 0;
        Map<String, Double> economicImpactByNation = new HashMap<>();
        double totalEconomicImpact = 0.0;
        
        for (Famine f : activeFamines.values()) {
            severityByNation.put(f.nationId, f.severity);
            totalSeverity += f.severity;
            totalDuration += f.durationMinutes;
            
            double impact = getFamineEconomicImpact(f.nationId);
            economicImpactByNation.put(f.nationId, impact);
            totalEconomicImpact += impact;
        }
        
        stats.put("severityByNation", severityByNation);
        stats.put("averageSeverity", activeFamines.size() > 0 ? totalSeverity / activeFamines.size() : 0);
        stats.put("totalSeverity", totalSeverity);
        stats.put("averageDurationMinutes", activeFamines.size() > 0 ? (double) totalDuration / activeFamines.size() : 0);
        stats.put("nationsAffected", activeFamines.size());
        stats.put("economicImpactByNation", economicImpactByNation);
        stats.put("totalEconomicImpact", totalEconomicImpact);
        
        // Top nations by severity
        List<Map.Entry<String, Double>> topBySeverity = severityByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySeverity", topBySeverity);
        
        // Severity distribution
        int critical = 0, severe = 0, moderate = 0, mild = 0;
        for (Famine f : activeFamines.values()) {
            if (f.severity >= 80) critical++;
            else if (f.severity >= 60) severe++;
            else if (f.severity >= 40) moderate++;
            else mild++;
        }
        
        Map<String, Integer> severityDistribution = new HashMap<>();
        severityDistribution.put("critical", critical);
        severityDistribution.put("severe", severe);
        severityDistribution.put("moderate", moderate);
        severityDistribution.put("mild", mild);
        stats.put("severityDistribution", severityDistribution);
        
        // Total population loss
        int totalPopulationLoss = 0;
        for (String nationId : activeFamines.keySet()) {
            totalPopulationLoss += getPopulationLoss(nationId);
        }
        stats.put("totalPopulationLoss", totalPopulationLoss);
        
        return stats;
    }
}

