package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;

/** Manages plague outbreaks (different from pandemics - more localized). */
public class PlagueService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, Plague> activePlagues = new HashMap<>(); // cityId -> plague

    public static class Plague {
        String cityId;
        String nationId;
        double mortalityRate; // 0-100%
        long startTime;
        long durationMinutes;
    }

    public PlagueService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::processPlagues, 0, 20 * 60 * 5); // every 5 minutes
    }

    public synchronized void triggerPlague(String cityId, String nationId, double mortalityRate, long durationMinutes) {
        Plague p = new Plague();
        p.cityId = cityId;
        p.nationId = nationId;
        p.mortalityRate = mortalityRate;
        p.startTime = System.currentTimeMillis();
        p.durationMinutes = durationMinutes;
        activePlagues.put(cityId, p);
        applyPlagueEffects(p);
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Чума в городе! Смертность: " + mortalityRate + "%");
            try { nationManager.save(n); } catch (Exception ignored) {}
        }
    }

    private void applyPlagueEffects(Plague p) {
        // Reduce population
        com.axiom.model.City city = plugin.getCityGrowthEngine().getCitiesOf(p.nationId).stream()
            .filter(c -> c.getId().equals(p.cityId))
            .findFirst()
            .orElse(null);
        if (city != null) {
            int loss = (int)(city.getPopulation() * (p.mortalityRate / 100.0));
            city.setPopulation(Math.max(10, city.getPopulation() - loss));
        }
        // Reduce happiness
        plugin.getNationModifierService().addModifier(p.nationId, "happiness", "penalty", p.mortalityRate / 100.0, p.durationMinutes);
    }

    private void processPlagues() {
        long now = System.currentTimeMillis();
        for (var entry : new HashMap<>(activePlagues).entrySet()) {
            Plague p = entry.getValue();
            if (now >= p.startTime + (p.durationMinutes * 60_000L)) {
                activePlagues.remove(entry.getKey());
            }
        }
    }

    public synchronized boolean hasPlague(String cityId) {
        return activePlagues.containsKey(cityId);
    }
    
    /**
     * Get comprehensive plague statistics.
     */
    public synchronized Map<String, Object> getPlagueStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Plagues in this nation's cities
        List<Plague> nationPlagues = new ArrayList<>();
        double totalMortalityRate = 0.0;
        
        for (Plague p : activePlagues.values()) {
            if (p.nationId.equals(nationId)) {
                nationPlagues.add(p);
                totalMortalityRate += p.mortalityRate;
            }
        }
        
        stats.put("activePlagues", nationPlagues.size());
        
        // Plague details
        List<Map<String, Object>> plaguesList = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Plague p : nationPlagues) {
            Map<String, Object> plagueData = new HashMap<>();
            plagueData.put("cityId", p.cityId);
            plagueData.put("mortalityRate", p.mortalityRate);
            plagueData.put("startTime", p.startTime);
            long elapsed = (now - p.startTime) / 1000 / 60; // minutes
            long remaining = p.durationMinutes - elapsed;
            plagueData.put("timeRemaining", Math.max(0, remaining));
            plagueData.put("timeRemainingHours", Math.max(0, remaining) / 60);
            plagueData.put("durationMinutes", p.durationMinutes);
            plaguesList.add(plagueData);
        }
        stats.put("plaguesList", plaguesList);
        
        // Average mortality rate
        double avgMortality = nationPlagues.stream()
            .mapToDouble(p -> p.mortalityRate)
            .average()
            .orElse(0.0);
        stats.put("averageMortalityRate", avgMortality);
        stats.put("totalMortalityRate", totalMortalityRate);
        
        // Plague rating
        String rating = "НЕТ ЧУМЫ";
        if (nationPlagues.size() >= 5) rating = "ЭПИДЕМИЯ";
        else if (nationPlagues.size() >= 3) rating = "ВСПЫШКА";
        else if (nationPlagues.size() >= 1) rating = "ЛОКАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * End plague early (through treatment/investment).
     */
    public synchronized String endPlague(String cityId, String nationId, double cost) throws IOException {
        Plague p = activePlagues.get(cityId);
        if (p == null) return "Чума не найдена.";
        if (!p.nationId.equals(nationId)) return "Это не ваш город.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        n.setTreasury(n.getTreasury() - cost);
        activePlagues.remove(cityId);
        nationManager.save(n);
        
        return "Чума вылечена. Город очищен.";
    }
    
    /**
     * Get all plagues for nation.
     */
    public synchronized List<Plague> getNationPlagues(String nationId) {
        return activePlagues.values().stream()
            .filter(p -> p.nationId.equals(nationId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate plague economic impact.
     */
    public synchronized double getPlagueEconomicImpact(String nationId) {
        double totalImpact = 0.0;
        for (Plague p : activePlagues.values()) {
            if (p.nationId.equals(nationId)) {
                // -1% economy per 10% mortality rate
                totalImpact += (p.mortalityRate / 10.0) * 0.01;
            }
        }
        return Math.min(0.50, totalImpact); // Max -50% impact
    }
    
    /**
     * Check if nation has any active plagues.
     */
    public synchronized boolean hasActivePlagues(String nationId) {
        return activePlagues.values().stream()
            .anyMatch(p -> p.nationId.equals(nationId));
    }
    
    /**
     * Get global plague statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPlagueStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActivePlagues", activePlagues.size());
        
        Map<String, Integer> plaguesByNation = new HashMap<>();
        double totalMortalityRate = 0.0;
        Map<String, Double> averageMortalityByNation = new HashMap<>();
        Map<String, Integer> plaguesByCity = new HashMap<>();
        
        for (Plague p : activePlagues.values()) {
            plaguesByNation.put(p.nationId, plaguesByNation.getOrDefault(p.nationId, 0) + 1);
            totalMortalityRate += p.mortalityRate;
            plaguesByCity.put(p.cityId, 1);
            
            // Track average mortality per nation
            averageMortalityByNation.put(p.nationId,
                (averageMortalityByNation.getOrDefault(p.nationId, 0.0) + p.mortalityRate) / 2.0);
        }
        
        stats.put("plaguesByNation", plaguesByNation);
        stats.put("averageMortalityRate", activePlagues.size() > 0 ? totalMortalityRate / activePlagues.size() : 0);
        stats.put("totalMortalityRate", totalMortalityRate);
        stats.put("nationsAffected", plaguesByNation.size());
        stats.put("citiesAffected", plaguesByCity.size());
        
        // Top nations by plagues
        List<Map.Entry<String, Integer>> topByPlagues = plaguesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPlagues", topByPlagues);
        
        // Top nations by average mortality
        List<Map.Entry<String, Double>> topByMortality = averageMortalityByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMortality", topByMortality);
        
        // Average plagues per nation
        stats.put("averagePlaguesPerNation", plaguesByNation.size() > 0 ? 
            (double) activePlagues.size() / plaguesByNation.size() : 0);
        
        // Plague severity distribution
        int critical = 0, severe = 0, moderate = 0, mild = 0;
        for (Plague p : activePlagues.values()) {
            if (p.mortalityRate >= 80) critical++;
            else if (p.mortalityRate >= 50) severe++;
            else if (p.mortalityRate >= 20) moderate++;
            else mild++;
        }
        
        Map<String, Integer> severityDistribution = new HashMap<>();
        severityDistribution.put("critical", critical);
        severityDistribution.put("severe", severe);
        severityDistribution.put("moderate", moderate);
        severityDistribution.put("mild", mild);
        stats.put("severityDistribution", severityDistribution);
        
        // Average plague duration
        long totalDuration = activePlagues.values().stream()
            .mapToLong(p -> p.durationMinutes)
            .sum();
        stats.put("averageDurationMinutes", activePlagues.size() > 0 ? 
            (double) totalDuration / activePlagues.size() : 0);
        
        // Total economic impact
        double totalEconomicImpact = 0.0;
        for (String nationId : plaguesByNation.keySet()) {
            totalEconomicImpact += getPlagueEconomicImpact(nationId);
        }
        stats.put("totalEconomicImpact", totalEconomicImpact);
        
        return stats;
    }
}

