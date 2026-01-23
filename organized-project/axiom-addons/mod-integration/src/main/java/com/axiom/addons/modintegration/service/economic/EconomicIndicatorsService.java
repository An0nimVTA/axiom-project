package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.api.ModIntegrationAPI;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for economic indicators that integrates with mod resources.
 * Provides comprehensive economic data including mod-based resources.
 */
public class EconomicIndicatorsService {
    private final AXIOM plugin;
    private final ModIntegrationAPI modAPI;
    private final EconomyService economyService;
    private final ResourceService resourceService;
    private final Map<String, Double> cachedIndicators = new HashMap<>();
    private final long CACHE_DURATION = 30000; // 30 seconds
    private final Map<String, Long> lastUpdate = new HashMap<>();

    public EconomicIndicatorsService(AXIOM plugin) {
        this.plugin = plugin;
        this.modAPI = plugin.getModIntegrationAPI();
        this.economyService = plugin.getEconomyService();
        this.resourceService = plugin.getResourceService();
        
        // Schedule periodic updates
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateGlobalIndicators, 0, 20 * 60 * 5); // every 5 minutes
    }

    /**
     * Get all economic indicators for a specific nation.
     */
    public Map<String, Object> getNationIndicators(String nationId) {
        Map<String, Object> indicators = new HashMap<>();

        // Basic economic indicators
        indicators.put("gdp", calculateGDP(nationId));
        indicators.put("inflation", calculateInflationRate(nationId));
        indicators.put("unemployment", calculateUnemploymentRate(nationId));
        indicators.put("trade_balance", calculateTradeBalance(nationId));
        indicators.put("budget", calculateBudget(nationId));
        indicators.put("debt_to_gdp", calculateDebtToGDP(nationId));

        // Mod-integrated indicators
        indicators.put("mod_resource_diversity", calculateResourceDiversity(nationId));
        indicators.put("mod_industrial_capacity", calculateIndustrialCapacity(nationId));
        indicators.put("mod_logistics_efficiency", calculateLogisticsEfficiency(nationId));
        indicators.put("mod_energy_efficiency", calculateEnergyEfficiency(nationId));

        // Composite index
        indicators.put("economic_strength_index", calculateEconomicStrengthIndex(nationId));

        return indicators;
    }

    /**
     * Get global economic indicators for the entire server.
     */
    public Map<String, Object> getGlobalIndicators() {
        String cacheKey = "global";
        if (shouldUseCache(cacheKey)) {
            Map<String, Object> cached = new HashMap<>();
            cached.put("gdp", cachedIndicators.getOrDefault(cacheKey + "_gdp", 0.0));
            cached.put("inflation", cachedIndicators.getOrDefault(cacheKey + "_inflation", 0.0));
            cached.put("total_wealth", cachedIndicators.getOrDefault(cacheKey + "_total_wealth", 0.0));
            cached.put("nations_count", cachedIndicators.getOrDefault(cacheKey + "_nations_count", 0.0));
            return cached;
        }

        Map<String, Object> indicators = new HashMap<>();

        double totalGDP = 0;
        double totalWealth = 0;
        double avgInflation = 0;
        int nationCount = 0;

        for (com.axiom.model.Nation nation : plugin.getNationManager().getAll()) {
            String nationId = nation.getId();
            Map<String, Object> nationIndicators = getNationIndicators(nationId);
            
            Double gdp = (Double) nationIndicators.get("gdp");
            if (gdp != null) totalGDP += gdp;
            
            Double inflation = (Double) nationIndicators.get("inflation");
            if (inflation != null) avgInflation += inflation;
            
            Double wealth = economyService.getTreasury(nationId); // Simplified wealth calculation
            if (wealth != null) totalWealth += wealth;
            
            nationCount++;
        }

        indicators.put("gdp", totalGDP);
        indicators.put("total_wealth", totalWealth);
        indicators.put("nations_count", nationCount);
        indicators.put("average_inflation", nationCount > 0 ? avgInflation / nationCount : 0);

        // Mod-integrated global indicators
        indicators.put("available_mods_count", modAPI.getDetectedMods().size());
        indicators.put("warfare_mods_count", modAPI.getWarfareMods().size());
        indicators.put("industrial_mods_count", modAPI.getIndustrialMods().size());
        indicators.put("energy_mods_count", modAPI.hasEnergyMods() ? 1 : 0);

        // Cache results
        cachedIndicators.put(cacheKey + "_gdp", totalGDP);
        cachedIndicators.put(cacheKey + "_inflation", avgInflation / Math.max(1, nationCount));
        cachedIndicators.put(cacheKey + "_total_wealth", totalWealth);
        cachedIndicators.put(cacheKey + "_nations_count", (double) nationCount);
        lastUpdate.put(cacheKey, System.currentTimeMillis());

        return indicators;
    }

    /**
     * Calculate GDP for a nation integrating mod resources.
     */
    public double calculateGDP(String nationId) {
        // Base GDP from treasury and transactions
        double baseGDP = economyService.getTreasury(nationId);
        
        // Factor in resource value
        Map<String, Object> resourceStats = modAPI.getResourceStatistics(nationId);
        Double resourceValue = (Double) resourceStats.getOrDefault("totalResourceValue", 0.0);
        baseGDP += resourceValue;

        // Factor in energy production (industrial capacity)
        double energyFactor = modAPI.getEnergyProduction(nationId) / 1000.0;
        baseGDP += energyFactor * 50; // Energy contributes to GDP

        // Factor in mod diversity bonus
        long diversity = (Long) resourceStats.getOrDefault("resourceDiversity", 0L);
        baseGDP += diversity * 100; // Bonus for resource diversity

        return baseGDP;
    }

    /**
     * Calculate inflation rate integrating mod resource fluctuations.
     */
    public double calculateInflationRate(String nationId) {
        // Simplified calculation - in a real implementation, this would track price changes
        // For now, we'll make it dependent on resource extraction and money supply
        double moneySupply = economyService.getTreasury(nationId);
        double resourceExtraction = getRecentResourceExtraction(nationId);

        // If resources are being extracted faster than money supply grows, deflation
        // If money supply grows faster than resource extraction, inflation
        if (resourceExtraction == 0) return 2.0; // Default inflation if no resource data

        double ratio = moneySupply / Math.max(1, resourceExtraction);
        double inflation = (ratio - 1.0) * 5; // 5% per ratio unit

        // Cap inflation between -10% and 50%
        return Math.max(-10.0, Math.min(50.0, inflation));
    }

    /**
     * Calculate unemployment rate (simplified).
     */
    public double calculateUnemploymentRate(String nationId) {
        // Simplified: estimate based on nation size vs active players
        com.axiom.model.Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return 100.0;

        int totalCitizens = nation.getCitizens().size();
        int onlineCitizens = 0;

        for (UUID citizenId : nation.getCitizens()) {
            if (Bukkit.getPlayer(citizenId) != null) {
                onlineCitizens++;
            }
        }

        // Unemployment based on economic activity: online citizens vs potential workers
        if (totalCitizens == 0) return 100.0;
        
        // Consider economic factors - nations with more mod resources might have lower unemployment
        double modFactor = 1.0;
        Map<String, Object> modStats = modAPI.getAdvancedEconomicMetrics(nationId);
        if ((double) modStats.getOrDefault("industrialCapacity", 0.0) > 0) {
            modFactor = 0.8; // Industrial capacity reduces unemployment
        }

        return Math.max(0, (1.0 - ((double) onlineCitizens / totalCitizens)) * 100 * modFactor);
    }

    /**
     * Calculate trade balance including mod resource exports/imports.
     */
    public double calculateTradeBalance(String nationId) {
        // Simplified calculation - would be more complex in real implementation
        // For now, we'll base it on resource diversity and logistics
        Map<String, Object> resourceStats = modAPI.getResourceStatistics(nationId);
        long diversity = (Long) resourceStats.getOrDefault("resourceDiversity", 0L);

        // Higher resource diversity allows for more exports
        double exportPotential = diversity * 50;

        // Logistics efficiency affects ability to trade
        double logisticsEfficiency = (double) modAPI.getAdvancedEconomicMetrics(nationId).getOrDefault("logisticsEfficiency", 1.0);
        exportPotential *= logisticsEfficiency;

        // Estimate imports based on energy needs
        double energyProduction = modAPI.getEnergyProduction(nationId);
        double energyConsumption = estimateEnergyConsumption(nationId);
        double netEnergy = energyProduction - energyConsumption;

        // If energy deficit, import costs increase
        double importCosts = Math.max(0, -netEnergy) * 0.1;

        return exportPotential - importCosts;
    }

    /**
     * Calculate budget (income - expenses).
     */
    public double calculateBudget(String nationId) {
        double income = calculateIncome(nationId);
        double expenses = calculateExpenses(nationId);
        return income - expenses;
    }

    /**
     * Calculate debt to GDP ratio.
     */
    public double calculateDebtToGDP(String nationId) {
        double gdp = calculateGDP(nationId);
        if (gdp == 0) return 0;

        double debt = calculateDebt(nationId);
        return (debt / gdp) * 100; // Percentage
    }

    /**
     * Calculate resource diversity index (0-100).
     */
    public double calculateResourceDiversity(String nationId) {
        Map<String, Object> resourceStats = modAPI.getResourceStatistics(nationId);
        long diversity = (Long) resourceStats.getOrDefault("resourceDiversity", 0L);
        
        // Scale diversity to 0-100 range (assuming max diversity of 20 for example)
        return Math.min(100.0, diversity * 5.0); // 5 points per diverse resource type
    }

    /**
     * Calculate industrial capacity based on mod resources.
     */
    public double calculateIndustrialCapacity(String nationId) {
        double baseCapacity = modAPI.getEnergyProduction(nationId) / 100;
        
        // Factor in industrial mods
        if (modAPI.hasIndustrialMods()) {
            baseCapacity *= 1.5;
        }

        // Factor in available resources
        Map<String, Object> resourceStats = modAPI.getResourceStatistics(nationId);
        Double totalValue = (Double) resourceStats.getOrDefault("totalResourceValue", 0.0);
        baseCapacity += totalValue / 1000;

        return baseCapacity;
    }

    /**
     * Calculate logistics efficiency based on mod integrations.
     */
    public double calculateLogisticsEfficiency(String nationId) {
        double efficiency = 1.0;

        if (modAPI.hasLogisticsMods()) {
            efficiency *= 1.3; // Logistics mods provide 30% efficiency boost
        }

        // Energy affects logistics efficiency
        double energyFactor = Math.min(1.5, 1.0 + (modAPI.getEnergyProduction(nationId) / 10000.0));
        efficiency *= energyFactor;

        return efficiency;
    }

    /**
     * Calculate energy efficiency score.
     */
    public double calculateEnergyEfficiency(String nationId) {
        double production = modAPI.getEnergyProduction(nationId);
        double potential = modAPI.getMaximumEnergyPotential(nationId);

        if (potential == 0) return 0;

        double efficiency = (production / potential) * 100;
        return Math.min(100.0, efficiency);
    }

    /**
     * Calculate economic strength index (composite score).
     */
    public double calculateEconomicStrengthIndex(String nationId) {
        Map<String, Object> indicators = getNationIndicators(nationId);

        double gdp = (Double) indicators.getOrDefault("gdp", 0.0);
        double diversity = (Double) indicators.getOrDefault("mod_resource_diversity", 0.0);
        double industrial = (Double) indicators.getOrDefault("mod_industrial_capacity", 0.0);
        double logistics = (Double) indicators.getOrDefault("mod_logistics_efficiency", 1.0);
        double energyEff = (Double) indicators.getOrDefault("mod_energy_efficiency", 0.0);

        // Normalize and weight the factors (GDP: 40%, Diversity: 15%, Industrial: 20%, Logistics: 10%, Energy: 15%)
        double normalizedGDP = Math.log10(Math.max(1, gdp / 1000)) * 20; // Log scale for GDP
        double diversityScore = diversity * 0.5; // Scale diversity to 0-50
        double industrialScore = Math.log10(Math.max(1, industrial)) * 10; // Log scale for industrial capacity
        double logisticsScore = (logistics - 1.0) * 100; // Convert multiplier to percentage
        double energyScore = energyEff * 0.3; // Scale energy efficiency

        return normalizedGDP * 0.4 + diversityScore * 0.15 + industrialScore * 0.2 + 
               logisticsScore * 0.1 + energyScore * 0.15;
    }

    // Helper methods

    private double calculateIncome(String nationId) {
        // Simplified income calculation
        double treasury = economyService.getTreasury(nationId);
        double resourceValue = getRecentResourceExtraction(nationId);
        return treasury + resourceValue;
    }

    private double calculateExpenses(String nationId) {
        // Simplified expense calculation
        com.axiom.model.Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return 0;

        // Expenses based on nation size and infrastructure maintenance
        double baseExpenses = nation.getCitizens().size() * 10;
        return baseExpenses;
    }

    private double calculateDebt(String nationId) {
        // Simplified - in real implementation, would track actual debts
        return 0;
    }

    private double getRecentResourceExtraction(String nationId) {
        // In a real implementation, this would track recent resource extraction
        // For now, we'll estimate based on resource service
        Map<String, Double> resources = resourceService.getNationResources(nationId);
        return resources.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private double estimateEnergyConsumption(String nationId) {
        com.axiom.model.Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return 0;

        // Estimate consumption based on nation size
        return nation.getClaimedChunkKeys().size() * 10.0;
    }

    private boolean shouldUseCache(String key) {
        Long last = lastUpdate.get(key);
        return last != null && (System.currentTimeMillis() - last) < CACHE_DURATION;
    }

    /**
     * Update global indicators periodically.
     */
    private void updateGlobalIndicators() {
        // This method runs periodically to update global indicators
        // In a real implementation, it might update statistics or trigger events
    }

    /**
     * Get economic trends for a nation (comparing current to previous values).
     */
    public Map<String, Object> getEconomicTrends(String nationId) {
        Map<String, Object> trends = new HashMap<>();
        
        // In a real implementation, this would compare current values to historical data
        // For now, we'll provide some simple trend estimations
        Map<String, Object> current = getNationIndicators(nationId);
        
        trends.put("gdp_trend", "stable"); // Would be calculated based on historical data
        trends.put("inflation_trend", "stable");
        trends.put("resource_trend", "growing");
        trends.put("investment_trend", "stable");
        
        return trends;
    }

    /**
     * Get economic rankings across all nations.
     */
    public Map<String, Object> getEconomicRankings() {
        Map<String, Object> rankings = new HashMap<>();
        
        List<Map.Entry<String, Double>> gdpRanking = new ArrayList<>();
        List<Map.Entry<String, Double>> strengthRanking = new ArrayList<>();
        
        for (com.axiom.model.Nation nation : plugin.getNationManager().getAll()) {
            String nationId = nation.getId();
            double gdp = calculateGDP(nationId);
            double strength = calculateEconomicStrengthIndex(nationId);
            
            gdpRanking.add(new AbstractMap.SimpleEntry<>(nationId, gdp));
            strengthRanking.add(new AbstractMap.SimpleEntry<>(nationId, strength));
        }
        
        // Sort rankings
        gdpRanking.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        strengthRanking.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        rankings.put("by_gdp", gdpRanking);
        rankings.put("by_strength", strengthRanking);
        
        return rankings;
    }
}