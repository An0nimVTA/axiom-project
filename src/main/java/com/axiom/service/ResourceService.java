package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Manages nation resource stockpiles (iron, coal, food, etc.) with balanced production rates. */
public class ResourceService {
    private final AXIOM plugin;
    private final File resourcesDir;
    private final Map<String, ResourceStock> nationResources = new ConcurrentHashMap<>(); // nationId -> stock

    // Resource production and consumption tracking
    private final Map<String, ResourceProduction> nationProduction = new ConcurrentHashMap<>();
    private final Map<String, ResourceConsumption> nationConsumption = new ConcurrentHashMap<>();
    
    // Base resource values and production rates
    private final Map<String, Double> resourceBaseValues = new ConcurrentHashMap<>();
    private final Map<String, Double> resourceProductionRates = new ConcurrentHashMap<>();
    private final Map<String, Double> resourceConsumptionRates = new ConcurrentHashMap<>();

    public static class ResourceStock {
        Map<String, Double> resources = new HashMap<>(); // resource name -> amount
    }

    public static class ResourceProduction {
        // Production rates per hour for different resource types
        double baseProduction = 100.0; // Base production of common resources
        double energyDependent = 0.0; // Production dependent on energy availability
        double infrastructureMultiplier = 1.0; // Infrastructure bonus
        double technologyMultiplier = 1.0; // Technology bonus
        long lastUpdated = System.currentTimeMillis();
        
        // Production by resource type
        Map<String, Double> resourceRates = new HashMap<>();
    }

    public static class ResourceConsumption {
        // Consumption rates per hour for different resource types
        double baseConsumption = 10.0; // Base consumption
        double industrialConsumption = 0.0; // From factories and machines
        double populationConsumption = 0.0; // From population needs
        double warfareConsumption = 0.0; // From military activities
        long lastUpdated = System.currentTimeMillis();
        
        // Consumption by resource type
        Map<String, Double> resourceRates = new HashMap<>();
    }

    public ResourceService(AXIOM plugin) {
        this.plugin = plugin;
        this.resourcesDir = new File(plugin.getDataFolder(), "resources");
        this.resourcesDir.mkdirs();
        initializeResourceValues();
        initializeResourceRates();
        loadAll();
        // Update resource production/consumption every 10 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllNationsResources, 0, 20 * 60 * 10);
    }

    /**
     * Initialize base values for common resources in the AXIOM ecosystem.
     */
    private void initializeResourceValues() {
        // Base values per unit (affects economy)
        resourceBaseValues.put("iron", 25.0);
        resourceBaseValues.put("gold", 100.0);
        resourceBaseValues.put("diamond", 500.0);
        resourceBaseValues.put("coal", 15.0);
        resourceBaseValues.put("redstone", 30.0);
        resourceBaseValues.put("lapis", 40.0);
        resourceBaseValues.put("emerald", 200.0);
        resourceBaseValues.put("copper", 10.0);
        resourceBaseValues.put("aluminum", 8.0);
        resourceBaseValues.put("uranium", 1000.0);
        resourceBaseValues.put("oil", 5.0);
        resourceBaseValues.put("gas", 3.0);
        resourceBaseValues.put("food", 2.0);
        resourceBaseValues.put("wood", 1.0);
        resourceBaseValues.put("stone", 0.5);
        resourceBaseValues.put("water", 0.1);
        resourceBaseValues.put("energy", 1.0); // Base energy unit value
    }

    /**
     * Initialize base production rates for common resources.
     */
    private void initializeResourceRates() {
        // Base production rates per hour (affects game balance)
        resourceProductionRates.put("iron", 50.0);
        resourceProductionRates.put("gold", 5.0);
        resourceProductionRates.put("diamond", 1.0);
        resourceProductionRates.put("coal", 80.0);
        resourceProductionRates.put("redstone", 15.0);
        resourceProductionRates.put("lapis", 10.0);
        resourceProductionRates.put("emerald", 3.0);
        resourceProductionRates.put("copper", 60.0);
        resourceProductionRates.put("aluminum", 40.0);
        resourceProductionRates.put("uranium", 0.5);
        resourceProductionRates.put("oil", 100.0);
        resourceProductionRates.put("gas", 120.0);
        resourceProductionRates.put("food", 200.0);
        resourceProductionRates.put("wood", 150.0);
        resourceProductionRates.put("stone", 500.0);
        resourceProductionRates.put("water", 1000.0);
        resourceProductionRates.put("energy", 1000.0); // Base energy production
    }

    /**
     * Update resources for all nations based on production and consumption.
     */
    private void updateAllNationsResources() {
        for (String nationId : nationResources.keySet()) {
            updateNationResources(nationId);
        }
    }

    /**
     * Update resources for a specific nation based on production and consumption.
     */
    public synchronized void updateNationResources(String nationId) {
        ResourceStock stock = nationResources.computeIfAbsent(nationId, k -> new ResourceStock());
        ResourceProduction production = nationProduction.computeIfAbsent(nationId, k -> new ResourceProduction());
        ResourceConsumption consumption = nationConsumption.computeIfAbsent(nationId, k -> new ResourceConsumption());
        
        // Update production and consumption data
        updateProductionData(nationId, production);
        updateConsumptionData(nationId, consumption);
        
        // Calculate time difference for resource calculations
        long now = System.currentTimeMillis();
        long timeDiff = now - production.lastUpdated;
        double hours = timeDiff / (1000.0 * 60 * 60);
        production.lastUpdated = now;
        consumption.lastUpdated = now;
        
        // Calculate net production for each resource type and update stock
        for (String resourceName : getAllTrackedResources()) {
            double baseProdRate = resourceProductionRates.getOrDefault(resourceName, 10.0);
            double currentProd = baseProdRate * production.infrastructureMultiplier * production.technologyMultiplier;
            double currentCons = consumption.resourceRates.getOrDefault(resourceName, 5.0);
            
            // Apply energy dependency if applicable
            if (resourceName.equals("energy")) {
                // Energy is special - it's produced by other systems but consumed by all
                currentProd = production.energyDependent * production.infrastructureMultiplier * production.technologyMultiplier;
            }
            
            // Calculate net change
            double netChange = (currentProd - currentCons) * hours;
            double currentAmount = stock.resources.getOrDefault(resourceName, 0.0);
            double newAmount = Math.max(0, currentAmount + netChange);
            
            // Apply storage limits based on infrastructure level (optional feature)
            double storageLimit = getStorageLimitForResource(nationId, resourceName);
            if (newAmount > storageLimit) {
                newAmount = storageLimit;
            }
            
            if (newAmount != currentAmount) {
                stock.resources.put(resourceName, newAmount);
            }
        }
        
        // Save updated data
        saveStock(nationId, stock);
    }

    /**
     * Get storage limit for a specific resource type for a nation.
     */
    private double getStorageLimitForResource(String nationId, String resourceName) {
        // Base storage is 1000 units per resource type
        double baseLimit = 1000.0;
        // Storage bonus from infrastructure
        double infraBonus = plugin.getInfrastructureService().getInfrastructureLevel(nationId, "utilities");
        return baseLimit + (infraBonus * 100); // 100 additional storage per infrastructure level
    }

    /**
     * Update production data based on nation's infrastructure, technology, etc.
     */
    private void updateProductionData(String nationId, ResourceProduction production) {
        // Base production
        production.baseProduction = 100.0;
        
        // Infrastructure bonus (10% per infrastructure level)
        double infraLevel = Math.min(100, plugin.getInfrastructureService().getInfrastructureLevel(nationId, "utilities"));
        production.infrastructureMultiplier = 1.0 + (infraLevel / 100.0);
        
        // Technology bonus
        production.technologyMultiplier = plugin.getTechnologyTreeService().getBonus(nationId, "resourceProduction");
        
        // Energy-dependent production - higher with more energy availability
        double energyLevel = plugin.getUnifiedEnergyService().getCurrentEnergy(nationId) / plugin.getUnifiedEnergyService().getMaxEnergy(nationId);
        production.energyDependent = 500.0 * energyLevel; // Up to 500 additional production with full energy
    }

    /**
     * Update consumption data based on nation's activities.
     */
    private void updateConsumptionData(String nationId, ResourceConsumption consumption) {
        // Base consumption
        consumption.baseConsumption = 10.0;
        
        // Industrial consumption (from factories, quarries, etc.)
        double industrialLevel = plugin.getTechnologyTreeService().getBonus(nationId, "productionBonus");
        consumption.industrialConsumption = Math.max(0, (industrialLevel - 1.0) * 50); // Extra consumption from industrial activities

        // Population consumption (from cities)
        int population = plugin.getCityGrowthEngine().getTotalPopulation(nationId);
        consumption.populationConsumption = population * 0.1; // 0.1 resource units per citizen per hour

        // Warfare consumption (if nation is at war)
        com.axiom.model.Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation != null && !nation.getEnemies().isEmpty()) {
            consumption.warfareConsumption = 100.0; // Additional consumption during war
        } else {
            consumption.warfareConsumption = 0.0;
        }

        // Set resource-specific consumption rates
        consumption.resourceRates.clear();
        for (String resource : getAllTrackedResources()) {
            double baseRate = 5.0; // Base consumption rate
            if (resource.equals("food")) baseRate = consumption.populationConsumption;
            else if (resource.equals("energy")) baseRate = consumption.industrialConsumption;
            consumption.resourceRates.put(resource, baseRate + consumption.warfareConsumption / getAllTrackedResources().size());
        }
    }

    /**
     * Get all tracked resource types.
     */
    private Set<String> getAllTrackedResources() {
        Set<String> allResources = new HashSet<>();
        allResources.addAll(resourceProductionRates.keySet());
        allResources.addAll(resourceBaseValues.keySet());
        allResources.add("energy"); // Always track energy
        return allResources;
    }

    public synchronized void addResource(String nationId, String resourceName, double amount) {
        ResourceStock stock = nationResources.computeIfAbsent(nationId, k -> new ResourceStock());
        double current = stock.resources.getOrDefault(resourceName, 0.0);
        double storageLimit = getStorageLimitForResource(nationId, resourceName);
        double newAmount = Math.min(current + amount, storageLimit);
        stock.resources.put(resourceName, newAmount);
        saveStock(nationId, stock);
    }

    public synchronized boolean consumeResource(String nationId, String resourceName, double amount) {
        ResourceStock stock = nationResources.get(nationId);
        if (stock == null) return false;
        double current = stock.resources.getOrDefault(resourceName, 0.0);
        if (current < amount) return false;
        stock.resources.put(resourceName, current - amount);
        saveStock(nationId, stock);
        return true;
    }

    public synchronized double getResource(String nationId, String resourceName) {
        ResourceStock stock = nationResources.get(nationId);
        return stock != null ? stock.resources.getOrDefault(resourceName, 0.0) : 0.0;
    }

    public synchronized double getResourceStockpile(String nationId, String resourceName) {
        return getResource(nationId, resourceName);
    }
    
    /**
     * Get all resources for a nation with their production/consumption rates.
     */
    public synchronized Map<String, Double> getNationResources(String nationId) {
        ResourceStock stock = nationResources.get(nationId);
        if (stock == null) return new HashMap<>();
        return new HashMap<>(stock.resources);
    }
    
    /**
     * Get comprehensive resource statistics for a nation including production/consumption data.
     */
    public synchronized Map<String, Object> getResourceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        ResourceStock stock = nationResources.get(nationId);
        ResourceProduction production = nationProduction.get(nationId);
        ResourceConsumption consumption = nationConsumption.get(nationId);
        
        if (stock == null) {
            stats.put("totalResources", 0);
            stats.put("resourceTypes", 0);
            stats.put("resources", Collections.emptyMap());
            return stats;
        }
        
        Map<String, Double> resources = stock.resources;
        double total = resources.values().stream().mapToDouble(Double::doubleValue).sum();
        double storageUtilization = 0.0; // Calculate storage utilization later if needed
        
        stats.put("totalResources", total);
        stats.put("resourceTypes", resources.size());
        stats.put("resources", new HashMap<>(resources));
        stats.put("storageUtilization", storageUtilization);
        
        // Production and consumption data
        if (production != null) {
            stats.put("production", Map.of(
                "baseProduction", production.baseProduction,
                "infrastructureMultiplier", production.infrastructureMultiplier,
                "technologyMultiplier", production.technologyMultiplier,
                "energyDependent", production.energyDependent
            ));
        }
        if (consumption != null) {
            stats.put("consumption", Map.of(
                "baseConsumption", consumption.baseConsumption,
                "industrialConsumption", consumption.industrialConsumption,
                "populationConsumption", consumption.populationConsumption,
                "warfareConsumption", consumption.warfareConsumption
            ));
        }
        
        // Top resources
        List<Map.Entry<String, Double>> topResources = resources.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topResources", topResources);
        
        // Resource efficiency rating
        String efficiencyRating = "BALANCED";
        if (consumption != null && production != null) {
            double netProduction = (production.baseProduction * production.infrastructureMultiplier * production.technologyMultiplier) -
                                 (consumption.baseConsumption + consumption.industrialConsumption + consumption.populationConsumption + consumption.warfareConsumption);
            if (netProduction < 0) efficiencyRating = "DEFICIT";
            else if (netProduction > 1000) efficiencyRating = "SURPLUS";
        }
        stats.put("efficiencyRating", efficiencyRating);
        
        return stats;
    }
    
    /**
     * Transfer resources between nations (trade, tribute, etc.).
     */
    public synchronized String transferResource(String fromNationId, String toNationId, String resourceName, double amount) {
        if (!consumeResource(fromNationId, resourceName, amount)) {
            return "Недостаточно ресурса у отправителя.";
        }
        addResource(toNationId, resourceName, amount);
        return "Переведено " + amount + " " + resourceName + " от '" + fromNationId + "' к '" + toNationId + "'";
    }
    
    /**
     * Calculate total resource value (for economy integration) with market fluctuations.
     */
    public synchronized double calculateResourceValue(String nationId) {
        ResourceStock stock = nationResources.get(nationId);
        if (stock == null) return 0.0;
        
        double totalValue = 0.0;
        for (Map.Entry<String, Double> entry : stock.resources.entrySet()) {
            String resourceType = entry.getKey();
            double amount = entry.getValue();
            double baseValue = resourceBaseValues.getOrDefault(resourceType, 10.0);
            totalValue += amount * baseValue;
        }
        return totalValue;
    }

    /**
     * Get the base value of a resource type.
     */
    public double getResourceBaseValue(String resourceName) {
        return resourceBaseValues.getOrDefault(resourceName, 10.0);
    }

    /**
     * Get the production rate of a resource type per hour.
     */
    public double getResourceProductionRate(String resourceName) {
        return resourceProductionRates.getOrDefault(resourceName, 10.0);
    }

    /**
     * Set the base value of a resource type (for admin use or balancing adjustments).
     */
    public void setResourceBaseValue(String resourceName, double value) {
        resourceBaseValues.put(resourceName, value);
    }

    /**
     * Get all tracked resource names in the system.
     */
    public Set<String> getAllResourceTypes() {
        Set<String> allTypes = new HashSet<>(resourceBaseValues.keySet());
        allTypes.addAll(resourceProductionRates.keySet());
        return allTypes;
    }

    private void loadAll() {
        File[] files = resourcesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                ResourceStock stock = new ResourceStock();
                for (var entry : o.entrySet()) {
                    stock.resources.put(entry.getKey(), entry.getValue().getAsDouble());
                }
                String nationId = f.getName().replace(".json", "");
                nationResources.put(nationId, stock);
            } catch (Exception ignored) {}
        }
    }

    private void saveStock(String nationId, ResourceStock stock) {
        File f = new File(resourcesDir, nationId + ".json");
        JsonObject o = new JsonObject();
        for (var entry : stock.resources.entrySet()) {
            o.addProperty(entry.getKey(), entry.getValue());
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get global resource statistics with production and consumption data.
     */
    public synchronized Map<String, Object> getGlobalResourceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Double> globalResources = new HashMap<>();
        double totalValue = 0.0;
        double totalProduction = 0.0;
        double totalConsumption = 0.0;
        int nationsWithResources = 0;
        int nationsWithProduction = 0;
        int nationsWithDeficit = 0;
        int nationsWithSurplus = 0;
        
        for (Map.Entry<String, ResourceStock> entry : nationResources.entrySet()) {
            String nationId = entry.getKey();
            ResourceStock stock = entry.getValue();
            nationsWithResources++;
            
            for (Map.Entry<String, Double> resEntry : stock.resources.entrySet()) {
                globalResources.put(resEntry.getKey(), 
                    globalResources.getOrDefault(resEntry.getKey(), 0.0) + resEntry.getValue());
            }
            totalValue += calculateResourceValue(nationId);
            
            // Calculate production/consumption for this nation
            ResourceProduction prod = nationProduction.get(nationId);
            ResourceConsumption cons = nationConsumption.get(nationId);
            if (prod != null && cons != null) {
                nationsWithProduction++;
                double net = (prod.baseProduction * prod.infrastructureMultiplier * prod.technologyMultiplier) -
                            (cons.baseConsumption + cons.industrialConsumption + cons.populationConsumption + cons.warfareConsumption);
                if (net > 0) nationsWithSurplus++;
                else if (net < 0) nationsWithDeficit++;
            }
        }
        
        stats.put("totalResourceTypes", globalResources.size());
        stats.put("totalResourceValue", totalValue);
        stats.put("globalResources", globalResources);
        stats.put("nationsWithResources", nationsWithResources);
        stats.put("nationsWithProduction", nationsWithProduction);
        stats.put("nationsWithDeficit", nationsWithDeficit);
        stats.put("nationsWithSurplus", nationsWithSurplus);
        
        // Top resources by total amount
        List<Map.Entry<String, Double>> topResources = globalResources.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topResources", topResources);
        
        // Resource distribution by type
        Map<String, Integer> resourceTypeDistribution = new HashMap<>();
        for (String resourceType : globalResources.keySet()) {
            int count = 0;
            for (ResourceStock stock : nationResources.values()) {
                if (stock.resources.containsKey(resourceType)) count++;
            }
            resourceTypeDistribution.put(resourceType, count);
        }
        stats.put("resourceTypeDistribution", resourceTypeDistribution);
        
        return stats;
    }
    
    private String getNationIdFromStock(ResourceStock stock) {
        for (Map.Entry<String, ResourceStock> entry : nationResources.entrySet()) {
            if (entry.getValue() == stock) {
                return entry.getKey();
            }
        }
        return null;
    }
}

