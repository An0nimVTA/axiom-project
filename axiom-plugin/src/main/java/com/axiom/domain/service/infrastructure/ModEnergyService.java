package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Manages mod-based energy systems and their impact on nation economy.
 * Tracks energy production from mod generators.
 */
public class ModEnergyService {
    private final AXIOM plugin;
    private final ModIntegrationService modIntegration;
    
    // Track energy production per nation (simplified)
    private final Map<String, Double> nationEnergyProduction = new HashMap<>();
    
    public ModEnergyService(AXIOM plugin, ModIntegrationService modIntegration) {
        this.plugin = plugin;
        this.modIntegration = modIntegration;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateEnergyProduction, 0, 20 * 60 * 5); // every 5 minutes
    }
    
    /**
     * Check if block is an energy generator from mods.
     */
    public boolean isEnergyGenerator(Block block) {
        return modIntegration != null && modIntegration.isEnergyBlock(block);
    }
    
    /**
     * Get energy production for a nation (estimated from mod infrastructure).
     */
    public double getEnergyProduction(String nationId) {
        if (nationId == null) return 0.0;
        return nationEnergyProduction.getOrDefault(nationId, 0.0);
    }
    
    /**
     * Calculate energy production based on nation's mod infrastructure.
     */
    private synchronized void updateEnergyProduction() {
        if (plugin.getNationManager() == null) {
            return;
        }
        for (Nation n : plugin.getNationManager().getAll()) {
            double energy = 0.0;
            
            // Base energy from claimed chunks (simplified)
            double baseEnergy = n.getClaimedChunkKeys().size() * 10.0;
            energy += baseEnergy;
            
            // Bonus if Industrial Upgrade or Quantum Generators available
            if (modIntegration != null && modIntegration.isIndustrialUpgradeAvailable()) {
                energy *= 1.5; // +50% efficiency
            }
            if (modIntegration != null && modIntegration.isQuantumGeneratorsAvailable()) {
                energy *= 2.0; // +100% from quantum generators
            }
            
            nationEnergyProduction.put(n.getId(), energy);
            
            // Energy affects industrial production and economy
            // High energy production enables better resource extraction
        }
    }
    
    /**
     * Check if nation has sufficient energy for industrial operations.
     */
    public boolean hasSufficientEnergy(String nationId, double required) {
        return getEnergyProduction(nationId) >= required;
    }
    
    /**
     * Record energy consumption (for future energy trading).
     */
    public void consumeEnergy(String nationId, double amount) {
        if (nationId == null) return;
        double current = nationEnergyProduction.getOrDefault(nationId, 0.0);
        nationEnergyProduction.put(nationId, Math.max(0, current - amount));
    }
    
    /**
     * Get comprehensive energy statistics for a nation.
     */
    public Map<String, Object> getEnergyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        double production = getEnergyProduction(nationId);
        stats.put("production", production);
        stats.put("hasIndustrialUpgrade", modIntegration != null && modIntegration.isIndustrialUpgradeAvailable());
        stats.put("hasQuantumGenerators", modIntegration != null && modIntegration.isQuantumGeneratorsAvailable());
        
        // Energy efficiency rating
        String efficiencyRating = "LOW";
        if (production >= 10000) efficiencyRating = "VERY_HIGH";
        else if (production >= 5000) efficiencyRating = "HIGH";
        else if (production >= 1000) efficiencyRating = "MEDIUM";
        stats.put("efficiencyRating", efficiencyRating);
        
        // Calculate energy surplus/deficit
        Nation n = plugin.getNationManager() != null ? plugin.getNationManager().getNationById(nationId) : null;
        if (n != null) {
            // Estimate consumption based on claimed chunks and cities
            double estimatedConsumption = n.getClaimedChunkKeys().size() * 5.0;
            if (plugin.getCityGrowthEngine() != null) {
                List<com.axiom.domain.model.City> cities = plugin.getCityGrowthEngine().getCitiesOf(nationId);
                estimatedConsumption += (cities != null ? cities.size() : 0) * 50.0;
            }
            
            double surplus = production - estimatedConsumption;
            stats.put("estimatedConsumption", estimatedConsumption);
            stats.put("surplus", surplus);
            stats.put("hasSurplus", surplus > 0);
        }
        
        return stats;
    }
    
    /**
     * Calculate energy production potential (if all mods were fully utilized).
     */
    public double getMaximumEnergyPotential(String nationId) {
        if (nationId == null) return 0.0;
        Nation n = plugin.getNationManager() != null ? plugin.getNationManager().getNationById(nationId) : null;
        if (n == null) return 0.0;
        
        double base = n.getClaimedChunkKeys().size() * 10.0;
        
        double multiplier = 1.0;
        if (modIntegration != null && modIntegration.isIndustrialUpgradeAvailable()) multiplier *= 1.5;
        if (modIntegration != null && modIntegration.isQuantumGeneratorsAvailable()) multiplier *= 2.0;
        if (modIntegration != null && modIntegration.isIEAvailable()) multiplier *= 1.3;
        
        return base * multiplier;
    }
    
    /**
     * Check if nation can export energy to others.
     */
    public boolean canExportEnergy(String nationId, double amount) {
        Map<String, Object> stats = getEnergyStatistics(nationId);
        double surplus = 0.0;
        Object surplusValue = stats.get("surplus");
        if (surplusValue instanceof Number) {
            surplus = ((Number) surplusValue).doubleValue();
        }
        return surplus >= amount;
    }
    
    /**
     * Get global mod energy statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalModEnergyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("hasIndustrialUpgrade", modIntegration != null && modIntegration.isIndustrialUpgradeAvailable());
        stats.put("hasQuantumGenerators", modIntegration != null && modIntegration.isQuantumGeneratorsAvailable());
        stats.put("hasIEAvailable", modIntegration != null && modIntegration.isIEAvailable());
        
        // Energy production by nation
        Map<String, Double> productionByNation = new HashMap<>(nationEnergyProduction);
        Map<String, Double> potentialByNation = new HashMap<>();
        Map<String, Double> surplusByNation = new HashMap<>();
        
        double totalProduction = 0.0;
        double totalPotential = 0.0;
        int nationsWithSurplus = 0;
        
        if (plugin.getNationManager() == null) {
            stats.put("productionByNation", productionByNation);
            stats.put("potentialByNation", potentialByNation);
            stats.put("surplusByNation", surplusByNation);
            stats.put("totalProduction", totalProduction);
            stats.put("totalPotential", totalPotential);
            stats.put("nationsWithSurplus", nationsWithSurplus);
            stats.put("averageProduction", 0);
            stats.put("averagePotential", 0);
            stats.put("averageSurplus", 0);
            stats.put("topByProduction", new ArrayList<>());
            stats.put("topByPotential", new ArrayList<>());
            stats.put("efficiencyDistribution", new HashMap<>());
            return stats;
        }
        for (com.axiom.domain.model.Nation n : plugin.getNationManager().getAll()) {
            String nationId = n.getId();
            double production = getEnergyProduction(nationId);
            double potential = getMaximumEnergyPotential(nationId);
            
            productionByNation.put(nationId, production);
            potentialByNation.put(nationId, potential);
            
            totalProduction += production;
            totalPotential += potential;
            
            // Calculate surplus
            Map<String, Object> nationStats = getEnergyStatistics(nationId);
            double surplus = nationStats.containsKey("surplus") ? 
                (Double) nationStats.get("surplus") : 0.0;
            surplusByNation.put(nationId, surplus);
            
            if (surplus > 0) {
                nationsWithSurplus++;
            }
        }
        
        stats.put("productionByNation", productionByNation);
        stats.put("potentialByNation", potentialByNation);
        stats.put("surplusByNation", surplusByNation);
        stats.put("totalProduction", totalProduction);
        stats.put("totalPotential", totalPotential);
        stats.put("nationsWithSurplus", nationsWithSurplus);
        
        // Average statistics
        stats.put("averageProduction", productionByNation.size() > 0 ? 
            totalProduction / productionByNation.size() : 0);
        stats.put("averagePotential", potentialByNation.size() > 0 ? 
            totalPotential / potentialByNation.size() : 0);
        stats.put("averageSurplus", surplusByNation.size() > 0 ? 
            surplusByNation.values().stream().mapToDouble(Double::doubleValue).sum() / surplusByNation.size() : 0);
        
        // Top nations by production
        List<Map.Entry<String, Double>> topByProduction = productionByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByProduction", topByProduction);
        
        // Top nations by potential
        List<Map.Entry<String, Double>> topByPotential = potentialByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPotential", topByPotential);
        
        // Energy efficiency distribution
        int veryHigh = 0, high = 0, medium = 0, low = 0;
        for (Double production : productionByNation.values()) {
            if (production >= 10000) veryHigh++;
            else if (production >= 5000) high++;
            else if (production >= 1000) medium++;
            else low++;
        }
        
        Map<String, Integer> efficiencyDistribution = new HashMap<>();
        efficiencyDistribution.put("veryHigh", veryHigh);
        efficiencyDistribution.put("high", high);
        efficiencyDistribution.put("medium", medium);
        efficiencyDistribution.put("low", low);
        stats.put("efficiencyDistribution", efficiencyDistribution);
        
        return stats;
    }
}
