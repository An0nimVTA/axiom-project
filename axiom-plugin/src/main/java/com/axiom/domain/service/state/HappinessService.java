package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.axiom.domain.model.City;
import com.axiom.domain.model.Nation;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.UUID;
import com.axiom.domain.service.state.EducationService;

/** Calculates and manages citizen happiness based on various factors. */
public class HappinessService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final CityGrowthEngine cityGrowthEngine;
    private final CrimeService crimeService;
    private final EducationService educationService;

    public HappinessService(AXIOM plugin, NationManager nationManager, CityGrowthEngine cityGrowthEngine, CrimeService crimeService, EducationService educationService) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.cityGrowthEngine = cityGrowthEngine;
        this.crimeService = crimeService;
        this.educationService = educationService;
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateHappiness, 0, 20 * 60 * 10); // every 10 minutes
    }

    private void updateHappiness() {
        if (cityGrowthEngine == null) return;
        for (Nation n : nationManager.getAll()) {
            for (City c : cityGrowthEngine.getCitiesOf(n.getId())) {
                double oldHappiness = c.getHappiness();
                double newHappiness = calculateHappiness(n, c);
                c.setHappiness(newHappiness);
                
                // VISUAL EFFECTS: Notify citizens of significant happiness changes (>10 points)
                double delta = newHappiness - oldHappiness;
                if (Math.abs(delta) >= 10.0 && oldHappiness > 0) { // Only notify on significant changes
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg;
                        if (delta > 0) {
                            msg = "§a📈 Счастье в '" + c.getName() + "' выросло: §e" + String.format("%.1f", newHappiness) + "%";
                        } else {
                            msg = "§c📉 Счастье в '" + c.getName() + "' упало: §e" + String.format("%.1f", newHappiness) + "%";
                        }
                        for (UUID citizenId : n.getCitizens()) {
                            org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                            if (citizen != null && citizen.isOnline()) {
                                if (plugin.getVisualEffectsService() != null) {
                                    plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                                }
                                // Subtle particles based on direction
                                org.bukkit.Location loc = citizen.getLocation();
                                if (delta > 0) {
                                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                                } else {
                                    loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private double calculateHappiness(Nation n, City c) {
        double base = 50.0;
        // Crime reduces happiness
        double crimeRate = crimeService != null ? crimeService.getCrimeRate(n.getId()) : 0.0;
        base -= crimeRate * 0.5;
        // Education increases happiness
        double education = educationService != null ? educationService.getEducationLevel(n.getId()) : 0.0;
        base += education * 0.3;
        // Infrastructure bonuses
        if (c.hasHospital()) base += 10;
        if (c.hasSchool()) base += 5;
        if (c.hasUniversity()) base += 15;
        // Budget allocations
        base += (n.getBudgetHealth() / 1000.0) * 2;
        base += (n.getBudgetEducation() / 1000.0) * 1;
        // War reduces happiness
        if (!n.getEnemies().isEmpty()) base -= 10;
        return Math.max(0, Math.min(100, base));
    }

    public synchronized double getNationHappiness(String nationId) {
        if (cityGrowthEngine == null) return 50.0;
        List<City> cities = cityGrowthEngine.getCitiesOf(nationId);
        if (cities.isEmpty()) return 50.0;
        double sum = 0;
        for (City c : cities) sum += c.getHappiness();
        return sum / cities.size();
    }

    public synchronized void modifyHappiness(String nationId, double delta) {
        if (cityGrowthEngine == null) return;
        List<City> cities = cityGrowthEngine.getCitiesOf(nationId);
        for (City c : cities) {
            double newHappiness = Math.max(0, Math.min(100, c.getHappiness() + delta));
            c.setHappiness(newHappiness);
        }
    }
    
    /**
     * Get comprehensive happiness statistics.
     */
    public synchronized Map<String, Object> getHappinessStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        if (cityGrowthEngine == null) {
            stats.put("averageHappiness", 50.0);
            stats.put("totalCities", 0);
            return stats;
        }
        List<City> cities = cityGrowthEngine.getCitiesOf(nationId);
        if (cities.isEmpty()) {
            stats.put("averageHappiness", 50.0);
            stats.put("totalCities", 0);
            return stats;
        }
        
        double totalHappiness = 0.0;
        double minHappiness = 100.0;
        double maxHappiness = 0.0;
        
        Map<String, Double> cityHappiness = new HashMap<>();
        for (City c : cities) {
            double happiness = c.getHappiness();
            totalHappiness += happiness;
            minHappiness = Math.min(minHappiness, happiness);
            maxHappiness = Math.max(maxHappiness, happiness);
            cityHappiness.put(c.getName(), happiness);
        }
        
        double average = totalHappiness / cities.size();
        stats.put("averageHappiness", average);
        stats.put("minHappiness", minHappiness);
        stats.put("maxHappiness", maxHappiness);
        stats.put("totalCities", cities.size());
        stats.put("cityHappiness", cityHappiness);
        
        // Happiness rating
        String rating = "ОТЛИЧНО";
        if (average < 70) rating = "ХОРОШО";
        if (average < 50) rating = "СРЕДНЕ";
        if (average < 30) rating = "НИЗКО";
        if (average < 20) rating = "КРИТИЧНО";
        stats.put("rating", rating);
        
        // Factors affecting happiness
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            Map<String, Double> factors = new HashMap<>();
            factors.put("crimePenalty", crimeService.getCrimeRate(nationId) * -0.5);
            factors.put("educationBonus", educationService.getEducationLevel(nationId) * 0.3);
            factors.put("healthBudget", (n.getBudgetHealth() / 1000.0) * 2);
            factors.put("educationBudget", (n.getBudgetEducation() / 1000.0) * 1);
            factors.put("warPenalty", !n.getEnemies().isEmpty() ? -10.0 : 0.0);
            stats.put("factors", factors);
        }
        
        return stats;
    }
    
    /**
     * Calculate happiness impact from events.
     */
    public synchronized void applyHappinessEvent(String nationId, String eventType, double impact) {
        // Event types: "victory", "defeat", "festival", "disaster", "economic_boom", "economic_crisis"
        double multiplier = 1.0;
        switch (eventType.toLowerCase()) {
            case "victory":
            case "festival":
            case "economic_boom":
                multiplier = 1.5; // Positive events have more impact
                break;
            case "defeat":
            case "disaster":
            case "economic_crisis":
                multiplier = 2.0; // Negative events have stronger impact
                break;
        }
        
        modifyHappiness(nationId, impact * multiplier);
    }
    
    /**
     * Get global happiness statistics.
     */
    public synchronized Map<String, Object> getGlobalHappinessStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalHappiness = 0.0;
        double maxHappiness = 0.0;
        double minHappiness = Double.MAX_VALUE;
        int nationsWithHappiness = 0;
        
        for (Nation n : nationManager.getAll()) {
            double happiness = getNationHappiness(n.getId());
            if (happiness > 0) {
                nationsWithHappiness++;
                totalHappiness += happiness;
                maxHappiness = Math.max(maxHappiness, happiness);
                minHappiness = Math.min(minHappiness, happiness);
            }
        }
        
        stats.put("totalNations", nationManager.getAll().size());
        stats.put("nationsWithHappiness", nationsWithHappiness);
        stats.put("averageHappiness", nationsWithHappiness > 0 ? totalHappiness / nationsWithHappiness : 0);
        stats.put("maxHappiness", maxHappiness);
        stats.put("minHappiness", minHappiness == Double.MAX_VALUE ? 0 : minHappiness);
        
        // Happiness distribution
        int veryHappy = 0, happy = 0, neutral = 0, unhappy = 0, veryUnhappy = 0;
        for (Nation n : nationManager.getAll()) {
            double h = getNationHappiness(n.getId());
            if (h >= 80) veryHappy++;
            else if (h >= 60) happy++;
            else if (h >= 40) neutral++;
            else if (h >= 20) unhappy++;
            else veryUnhappy++;
        }
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("veryHappy", veryHappy);
        distribution.put("happy", happy);
        distribution.put("neutral", neutral);
        distribution.put("unhappy", unhappy);
        distribution.put("veryUnhappy", veryUnhappy);
        stats.put("happinessDistribution", distribution);
        
        // Top nations by happiness
        List<Map.Entry<String, Double>> topByHappiness = new ArrayList<>();
        for (Nation n : nationManager.getAll()) {
            topByHappiness.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), getNationHappiness(n.getId())));
        }
        topByHappiness.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByHappiness", topByHappiness.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

