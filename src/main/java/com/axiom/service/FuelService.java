package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive fuel management system that provides balanced fuel consumption
 * across all energy-consuming services in the AXIOM ecosystem.
 */
public class FuelService {
    private final AXIOM plugin;
    private final File fuelDir;
    private final File fuelConsumptionDir;
    
    // Fuel resource definitions with properties
    private final Map<String, FuelResource> fuelResources = new ConcurrentHashMap<>();
    
    // Nation-specific fuel consumption data
    private final Map<String, NationFuelData> nationFuelData = new ConcurrentHashMap<>();
    
    // Energy conversion factors
    private final Map<String, Double> energyConversionFactors = new ConcurrentHashMap<>();
    
    // Fuel consumption by service type
    private final Map<String, Map<String, Double>> serviceFuelConsumption = new ConcurrentHashMap<>();

    public static class FuelResource {
        String resourceId;
        String displayName;
        double energyPerUnit; // Energy units per fuel item/block
        double burnTime; // Burn duration in seconds 
        double efficiency; // Conversion efficiency (0.0-1.0)
        String fuelType; // "solid", "liquid", "gas", "crystal", "mod"
        double environmentalImpact; // Environmental cost factor
        double rarity; // Rarity factor (affects availability)
        
        public FuelResource(String id, String name, double energy, double burn, double eff, String type, double envImpact, double rarity) {
            this.resourceId = id;
            this.displayName = name;
            this.energyPerUnit = energy;
            this.burnTime = burn;
            this.efficiency = eff;
            this.fuelType = type;
            this.environmentalImpact = envImpact;
            this.rarity = rarity;
        }
    }
    
    public static class NationFuelData {
        Map<String, Double> fuelStock = new HashMap<>(); // fuelId -> amount
        Map<String, Double> fuelConsumptionRates = new HashMap<>(); // fuelId -> consumption rate per hour
        Map<String, Double> fuelEfficiencyModifiers = new HashMap<>(); // fuelId -> efficiency modifier
        double totalEnergyConsumption = 0.0; // Total energy consumed per hour
        double totalFuelCost = 0.0; // Total cost of fuel consumed
        long lastUpdated = System.currentTimeMillis();
    }

    public FuelService(AXIOM plugin) {
        this.plugin = plugin;
        this.fuelDir = new File(plugin.getDataFolder(), "fuel");
        this.fuelConsumptionDir = new File(fuelDir, "consumption");
        
        fuelDir.mkdirs();
        fuelConsumptionDir.mkdirs();
        
        initializeFuelResources();
        initializeEnergyConversionFactors();
        loadAllFuelData();
        
        // Update fuel consumption every 10 minutes
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAllNationsFuel, 0, 20 * 60 * 10);
    }
    
    /**
     * Initialize standard and mod-specific fuel resources with balanced properties.
     */
    private void initializeFuelResources() {
        // Standard Minecraft fuels
        fuelResources.put("coal", new FuelResource("coal", "Coal", 1600, 80, 0.8, "solid", 0.7, 0.8));
        fuelResources.put("charcoal", new FuelResource("charcoal", "Charcoal", 1600, 80, 0.85, "solid", 0.6, 0.9));
        fuelResources.put("coal_block", new FuelResource("coal_block", "Coal Block", 14400, 720, 0.85, "solid", 0.7, 0.6));
        fuelResources.put("wood", new FuelResource("wood", "Wood", 300, 15, 0.6, "solid", 0.3, 1.0));
        fuelResources.put("wood_planks", new FuelResource("wood_planks", "Wood Planks", 300, 15, 0.65, "solid", 0.2, 1.0));
        fuelResources.put("blaze_rod", new FuelResource("blaze_rod", "Blaze Rod", 1200, 120, 0.9, "crystal", 0.1, 0.4));
        
        // Higher tier fuels
        fuelResources.put("lava_bucket", new FuelResource("lava_bucket", "Lava Bucket", 20000, 1000, 0.85, "liquid", 0.2, 0.5));
        fuelResources.put("magma_cream", new FuelResource("magma_cream", "Magma Cream", 2400, 120, 0.92, "liquid", 0.15, 0.3));
        
        // Mod-specific fuels from Immersive Engineering
        fuelResources.put("ie_coal_coke", new FuelResource("ie_coal_coke", "Coal Coke", 6400, 320, 0.95, "solid", 0.8, 0.5));
        fuelResources.put("ie_hemp_oil", new FuelResource("ie_hemp_oil", "Hemp Oil", 1200, 60, 0.7, "liquid", 0.1, 0.7));
        
        // Mod-specific fuels from Industrial Upgrade
        fuelResources.put("iu_uranium", new FuelResource("iu_uranium", "Uranium Fuel", 200000, 10000, 0.98, "solid", 0.95, 0.1));
        
        // Applied Energistics 2 fuels
        fuelResources.put("ae2_matter_ball", new FuelResource("ae2_matter_ball", "Matter Ball", 50000, 2500, 0.9, "crystal", 0.05, 0.2));
        fuelResources.put("ae2_singularity", new FuelResource("ae2_singularity", "Singularity", 2000000, 100000, 0.95, "crystal", 0.02, 0.05));
        
        // AXIOM custom fuels
        fuelResources.put("axiom_energy_crystal", new FuelResource("axiom_energy_crystal", "Energy Crystal", 50000, 2500, 0.95, "crystal", 0.05, 0.15));
        fuelResources.put("axiom_quantum_fuel", new FuelResource("axiom_quantum_fuel", "Quantum Fuel", 500000, 25000, 0.99, "crystal", 0.01, 0.08));
    }
    
    /**
     * Initialize energy conversion factors between different systems.
     */
    private void initializeEnergyConversionFactors() {
        // Standard conversion rates (FE/RF to AXIOM Energy Units)
        energyConversionFactors.put("minecraft_to_axiom", 1.0);
        energyConversionFactors.put("fe_to_axiom", 1.0);
        energyConversionFactors.put("rf_to_axiom", 1.0);
        
        // Mod-specific conversion efficiency
        energyConversionFactors.put("mod_efficiency", 0.95);
        energyConversionFactors.put("transport_efficiency", 0.98);
        energyConversionFactors.put("storage_efficiency", 0.99);
    }
    
    /**
     * Update fuel data for all nations.
     */
    private void updateAllNationsFuel() {
        for (Nation nation : plugin.getNationManager().getAll()) {
            updateNationFuel(nation.getId());
        }
    }
    
    /**
     * Update fuel data for a specific nation.
     */
    public synchronized void updateNationFuel(String nationId) {
        NationFuelData data = nationFuelData.computeIfAbsent(nationId, k -> new NationFuelData());
        
        // Calculate time since last update
        long now = System.currentTimeMillis();
        long timeDiff = now - data.lastUpdated;
        double hours = timeDiff / (1000.0 * 60 * 60);
        data.lastUpdated = now;
        
        // Calculate fuel consumption based on current activities
        updateFuelConsumption(nationId, data, hours);
        
        // Apply fuel consumption from stock
        applyFuelConsumption(nationId, data, hours);
        
        // Save updated data
        saveFuelData(nationId, data);
    }
    
    /**
     * Update fuel consumption based on nation's activities and services.
     */
    private void updateFuelConsumption(String nationId, NationFuelData data, double hours) {
        // Reset consumption rates
        data.fuelConsumptionRates.clear();
        data.totalEnergyConsumption = 0.0;
        
        // Calculate base consumption from infrastructure
        double infraLevel = plugin.getInfrastructureService().getInfrastructureLevel(nationId, "utilities");
        double baseConsumption = infraLevel * 5; // 5 units per infrastructure level per hour
        
        // Calculate industrial consumption from factories, quarries, etc.
        double industrialLevel = plugin.getTechnologyTreeService().getBonus(nationId, "productionBonus");
        double industrialConsumption = (industrialLevel - 1.0) * 50; // Extra consumption from industrial activities
        
        // Calculate military consumption
        Nation nation = plugin.getNationManager().getNationById(nationId);
        double militaryConsumption = 0.0;
        if (nation != null && !nation.getEnemies().isEmpty()) {
            militaryConsumption = 100.0; // High consumption during war
        } else {
            militaryConsumption = 20.0; // Base military consumption
        }
        
        // Calculate total consumption
        double totalConsumption = baseConsumption + industrialConsumption + militaryConsumption;
        data.totalEnergyConsumption = totalConsumption;
        
        // Distribute consumption across fuel types based on availability and efficiency
        distributeFuelConsumption(nationId, data, totalConsumption);
    }
    
    /**
     * Distribute total consumption across available fuel types.
     */
    private void distributeFuelConsumption(String nationId, NationFuelData data, double totalConsumption) {
        // Get available fuels and prioritize by efficiency and availability
        List<Map.Entry<String, Double>> availableFuels = new ArrayList<>();
        for (Map.Entry<String, Double> stockEntry : data.fuelStock.entrySet()) {
            String fuelId = stockEntry.getKey();
            double amount = stockEntry.getValue();
            if (amount > 0 && fuelResources.containsKey(fuelId)) {
                FuelResource fuel = fuelResources.get(fuelId);
                // Priority score: efficiency * availability
                double priority = fuel.efficiency * amount;
                availableFuels.add(new AbstractMap.SimpleEntry<>(fuelId, priority));
            }
        }
        
        // Sort by priority (descending)
        availableFuels.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // Distribute consumption across available fuels
        double remainingConsumption = totalConsumption;
        for (Map.Entry<String, Double> fuelEntry : availableFuels) {
            if (remainingConsumption <= 0) break;
            
            String fuelId = fuelEntry.getKey();
            FuelResource fuel = fuelResources.get(fuelId);
            double fuelAmount = data.fuelStock.get(fuelId);
            
            // Calculate how much of this fuel to consume based on its energy density
            double energyFromFuel = fuelAmount * fuel.energyPerUnit * fuel.efficiency;
            double consumptionForFuel = Math.min(remainingConsumption, energyFromFuel);
            
            // Convert energy consumption back to fuel units
            double fuelUnitsToConsume = consumptionForFuel / (fuel.energyPerUnit * fuel.efficiency);
            fuelUnitsToConsume = Math.min(fuelUnitsToConsume, fuelAmount); // Don't consume more than available
            
            data.fuelConsumptionRates.put(fuelId, fuelUnitsToConsume);
            remainingConsumption -= consumptionForFuel;
        }
    }
    
    /**
     * Apply fuel consumption to the nation's fuel stock.
     */
    private void applyFuelConsumption(String nationId, NationFuelData data, double hours) {
        for (Map.Entry<String, Double> consumptionEntry : data.fuelConsumptionRates.entrySet()) {
            String fuelId = consumptionEntry.getKey();
            double consumptionRate = consumptionEntry.getValue(); // per hour
            double totalConsumption = consumptionRate * hours;
            
            double currentStock = data.fuelStock.getOrDefault(fuelId, 0.0);
            double newStock = Math.max(0, currentStock - totalConsumption);
            data.fuelStock.put(fuelId, newStock);
        }
        
        // Apply fuel cost to nation treasury if fuel runs out of a critical type
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation != null) {
            // If no fuel is available for critical operations, apply economic penalty
            if (data.fuelStock.isEmpty() || data.fuelStock.values().stream().mapToDouble(Double::doubleValue).sum() == 0) {
                // Apply penalty for running out of fuel
                plugin.getEconomyService().recordTransaction(nationId, -100 * hours); // Economic penalty
                plugin.getHappinessService().modifyHappiness(nationId, -2.0 * hours); // Happiness penalty
            }
        }
    }
    
    /**
     * Add fuel to a nation's stock.
     */
    public synchronized boolean addFuel(String nationId, String fuelId, double amount) {
        if (!fuelResources.containsKey(fuelId)) {
            return false; // Invalid fuel type
        }
        
        NationFuelData data = nationFuelData.computeIfAbsent(nationId, k -> new NationFuelData());
        double currentStock = data.fuelStock.getOrDefault(fuelId, 0.0);
        double newStock = currentStock + amount;
        
        data.fuelStock.put(fuelId, newStock);
        saveFuelData(nationId, data);
        
        return true;
    }
    
    /**
     * Remove fuel from a nation's stock.
     */
    public synchronized boolean consumeFuel(String nationId, String fuelId, double amount) {
        NationFuelData data = nationFuelData.get(nationId);
        if (data == null) return false;
        
        double currentStock = data.fuelStock.getOrDefault(fuelId, 0.0);
        if (currentStock < amount) return false;
        
        data.fuelStock.put(fuelId, currentStock - amount);
        saveFuelData(nationId, data);
        
        return true;
    }
    
    /**
     * Get fuel amount available for a nation.
     */
    public synchronized double getFuelAmount(String nationId, String fuelId) {
        NationFuelData data = nationFuelData.get(nationId);
        if (data == null) return 0.0;
        return data.fuelStock.getOrDefault(fuelId, 0.0);
    }
    
    /**
     * Convert a fuel ItemStack to energy value.
     */
    public synchronized double convertFuelToEnergy(ItemStack fuelItem) {
        if (fuelItem == null) return 0.0;
        
        String fuelName = fuelItem.getType().name().toLowerCase();
        String fuelId = fuelName.replaceAll("_", "");
        
        // Check for exact matches first
        if (fuelResources.containsKey(fuelId)) {
            FuelResource fuel = fuelResources.get(fuelId);
            return fuel.energyPerUnit * fuelItem.getAmount() * fuel.efficiency;
        }
        
        // Fallback: check by material type
        for (Map.Entry<String, FuelResource> entry : fuelResources.entrySet()) {
            if (entry.getValue().resourceId.contains(fuelName) || 
                fuelName.contains(entry.getValue().resourceId)) {
                FuelResource fuel = entry.getValue();
                return fuel.energyPerUnit * fuelItem.getAmount() * fuel.efficiency;
            }
        }
        
        return 0.0;
    }
    
    /**
     * Convert a block to fuel value.
     */
    public synchronized double convertBlockToFuel(Block block) {
        if (block == null) return 0.0;
        
        String blockName = block.getType().name().toLowerCase();
        String fuelId = blockName.replaceAll("_", "");
        
        // Check for exact matches first
        if (fuelResources.containsKey(fuelId)) {
            FuelResource fuel = fuelResources.get(fuelId);
            return fuel.energyPerUnit * fuel.efficiency;
        }
        
        // Fallback: check by material type
        for (Map.Entry<String, FuelResource> entry : fuelResources.entrySet()) {
            if (entry.getValue().resourceId.contains(blockName) || 
                blockName.contains(entry.getValue().resourceId)) {
                FuelResource fuel = entry.getValue();
                return fuel.energyPerUnit * fuel.efficiency;
            }
        }
        
        return 0.0;
    }
    
    /**
     * Get fuel resource by ID.
     */
    public FuelResource getFuelResource(String fuelId) {
        return fuelResources.get(fuelId);
    }
    
    /**
     * Get all available fuel resources.
     */
    public Map<String, FuelResource> getAllFuelResources() {
        return new HashMap<>(fuelResources);
    }
    
    /**
     * Get comprehensive fuel statistics for a nation.
     */
    public synchronized Map<String, Object> getFuelStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        NationFuelData data = nationFuelData.get(nationId);
        if (data == null) {
            stats.put("fuelStock", Collections.emptyMap());
            stats.put("fuelConsumptionRates", Collections.emptyMap());
            stats.put("totalEnergyConsumption", 0.0);
            stats.put("hasFuel", false);
            return stats;
        }
        
        stats.put("fuelStock", new HashMap<>(data.fuelStock));
        stats.put("fuelConsumptionRates", new HashMap<>(data.fuelConsumptionRates));
        stats.put("totalEnergyConsumption", data.totalEnergyConsumption);
        stats.put("hasFuel", !data.fuelStock.isEmpty());
        
        // Calculate fuel efficiency
        double totalEfficiency = 0.0;
        int fuelTypes = 0;
        for (String fuelId : data.fuelStock.keySet()) {
            if (fuelResources.containsKey(fuelId)) {
                totalEfficiency += fuelResources.get(fuelId).efficiency;
                fuelTypes++;
            }
        }
        stats.put("averageFuelEfficiency", fuelTypes > 0 ? totalEfficiency / fuelTypes : 0.0);
        
        // Calculate fuel sufficiency (how long current fuel will last)
        double currentTotalEnergy = 0.0;
        for (Map.Entry<String, Double> stockEntry : data.fuelStock.entrySet()) {
            String fuelId = stockEntry.getKey();
            double amount = stockEntry.getValue();
            if (fuelResources.containsKey(fuelId)) {
                FuelResource fuel = fuelResources.get(fuelId);
                currentTotalEnergy += amount * fuel.energyPerUnit * fuel.efficiency;
            }
        }
        
        double hoursRemaining = data.totalEnergyConsumption > 0 ? currentTotalEnergy / data.totalEnergyConsumption : Double.POSITIVE_INFINITY;
        stats.put("hoursOfFuelRemaining", hoursRemaining);
        
        // Fuel diversity index
        double diversityIndex = fuelTypes > 0 ? fuelTypes / 10.0 : 0.0; // Normalize to 0-1 scale (assuming 10 is max diversity)
        stats.put("fuelDiversityIndex", Math.min(1.0, diversityIndex));
        
        // Overall fuel rating
        String rating = "EXCELLENT";
        if (hoursRemaining < 1) rating = "CRITICAL";
        else if (hoursRemaining < 24) rating = "LOW";
        else if (hoursRemaining < 168) rating = "MODERATE"; // 1 week
        else if (hoursRemaining < 720) rating = "GOOD"; // 1 month
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get fuel efficiency bonus for a specific fuel type and nation.
     */
    public double getFuelEfficiencyBonus(String nationId, String fuelId) {
        // Base efficiency from fuel type
        FuelResource fuel = fuelResources.get(fuelId);
        if (fuel == null) return 0.0;
        
        double baseEfficiency = fuel.efficiency;
        
        // Add technology bonus
        double techBonus = plugin.getTechnologyTreeService().getBonus(nationId, "energyEfficiency");
        
        // Add infrastructure bonus
        double infraBonus = plugin.getInfrastructureService().getInfrastructureLevel(nationId, "utilities") / 100.0 * 0.2;
        
        return baseEfficiency * techBonus * (1.0 + infraBonus);
    }
    
    /**
     * Calculate cost of fuel consumption.
     */
    public double calculateFuelCost(String nationId, String fuelId, double amount) {
        FuelResource fuel = fuelResources.get(fuelId);
        if (fuel == null) return 0.0;
        
        // Base cost based on rarity and energy content
        double baseCost = fuel.rarity * fuel.energyPerUnit * 0.01 * amount;
        
        // Adjust for nation's economic status
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation != null) {
            // Wealthier nations pay less per unit
            double economicFactor = Math.max(0.5, 1.0 - (nation.getTreasury() / 1000000.0)); // Cap at 50% discount
            return baseCost * economicFactor;
        }
        
        return baseCost;
    }
    
    /**
     * Get fuel consumption by service for a nation.
     */
    public Map<String, Double> getServiceFuelConsumption(String nationId) {
        return serviceFuelConsumption.getOrDefault(nationId, new HashMap<>());
    }
    
    /**
     * Get conversion factor between different energy systems.
     */
    public double getConversionFactor(String fromSystem, String toSystem) {
        String key = fromSystem + "_to_" + toSystem;
        return energyConversionFactors.getOrDefault(key, 1.0);
    }
    
    /**
     * Load all fuel data from disk.
     */
    private void loadAllFuelData() {
        File[] files = fuelConsumptionDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                
                NationFuelData data = new NationFuelData();
                
                // Load fuel stock
                if (obj.has("fuelStock")) {
                    JsonObject stockObj = obj.getAsJsonObject("fuelStock");
                    for (String fuelId : stockObj.keySet()) {
                        data.fuelStock.put(fuelId, stockObj.get(fuelId).getAsDouble());
                    }
                }
                
                // Load fuel consumption rates
                if (obj.has("fuelConsumptionRates")) {
                    JsonObject ratesObj = obj.getAsJsonObject("fuelConsumptionRates");
                    for (String fuelId : ratesObj.keySet()) {
                        data.fuelConsumptionRates.put(fuelId, ratesObj.get(fuelId).getAsDouble());
                    }
                }
                
                // Load other values
                if (obj.has("totalEnergyConsumption")) {
                    data.totalEnergyConsumption = obj.get("totalEnergyConsumption").getAsDouble();
                }
                if (obj.has("totalFuelCost")) {
                    data.totalFuelCost = obj.get("totalFuelCost").getAsDouble();
                }
                if (obj.has("lastUpdated")) {
                    data.lastUpdated = obj.get("lastUpdated").getAsLong();
                }
                
                nationFuelData.put(nationId, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load fuel data for " + f.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Save fuel data to disk.
     */
    private void saveFuelData(String nationId, NationFuelData data) {
        File f = new File(fuelConsumptionDir, nationId + ".json");
        JsonObject obj = new JsonObject();
        
        // Save fuel stock
        JsonObject stockObj = new JsonObject();
        for (Map.Entry<String, Double> entry : data.fuelStock.entrySet()) {
            stockObj.addProperty(entry.getKey(), entry.getValue());
        }
        obj.add("fuelStock", stockObj);
        
        // Save fuel consumption rates
        JsonObject ratesObj = new JsonObject();
        for (Map.Entry<String, Double> entry : data.fuelConsumptionRates.entrySet()) {
            ratesObj.addProperty(entry.getKey(), entry.getValue());
        }
        obj.add("fuelConsumptionRates", ratesObj);
        
        // Save other values
        obj.addProperty("totalEnergyConsumption", data.totalEnergyConsumption);
        obj.addProperty("totalFuelCost", data.totalFuelCost);
        obj.addProperty("lastUpdated", data.lastUpdated);
        
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(obj.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save fuel data for " + nationId + ": " + e.getMessage());
        }
    }
    
    /**
     * Get global fuel statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalFuelStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int nationsWithFuel = 0;
        double totalFuelStock = 0.0;
        double totalEnergyConsumption = 0.0;
        int fuelTypesAvailable = 0;
        
        Map<String, Integer> fuelDistribution = new HashMap<>();
        Map<String, Double> totalFuelByType = new HashMap<>();
        
        for (Map.Entry<String, NationFuelData> entry : nationFuelData.entrySet()) {
            nationsWithFuel++;
            NationFuelData data = entry.getValue();
            
            for (Map.Entry<String, Double> stockEntry : data.fuelStock.entrySet()) {
                String fuelType = stockEntry.getKey();
                double amount = stockEntry.getValue();
                
                totalFuelStock += amount;
                totalFuelByType.merge(fuelType, amount, Double::sum);
                fuelDistribution.merge(fuelType, 1, Integer::sum);
            }
            
            totalEnergyConsumption += data.totalEnergyConsumption;
        }
        
        stats.put("nationsWithFuel", nationsWithFuel);
        stats.put("totalFuelStock", totalFuelStock);
        stats.put("totalEnergyConsumption", totalEnergyConsumption);
        stats.put("fuelTypesAvailable", fuelDistribution.size());
        
        if (nationsWithFuel > 0) {
            stats.put("averageFuelStockPerNation", totalFuelStock / nationsWithFuel);
            stats.put("averageEnergyConsumptionPerNation", totalEnergyConsumption / nationsWithFuel);
        }
        
        // Top fuel types by availability
        List<Map.Entry<String, Double>> topFuelTypes = new ArrayList<>(totalFuelByType.entrySet());
        topFuelTypes.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topFuelTypes", topFuelTypes.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        // Fuel distribution
        stats.put("fuelDistribution", new HashMap<>(fuelDistribution));
        stats.put("totalFuelByType", new HashMap<>(totalFuelByType));
        
        // Nations with critical fuel levels
        int nationsWithCriticalFuel = 0;
        for (NationFuelData data : nationFuelData.values()) {
            double currentTotalEnergy = 0.0;
            for (Map.Entry<String, Double> stockEntry : data.fuelStock.entrySet()) {
                String fuelId = stockEntry.getKey();
                double amount = stockEntry.getValue();
                if (fuelResources.containsKey(fuelId)) {
                    FuelResource fuel = fuelResources.get(fuelId);
                    currentTotalEnergy += amount * fuel.energyPerUnit * fuel.efficiency;
                }
            }
            
            if (data.totalEnergyConsumption > 0) {
                double hoursRemaining = currentTotalEnergy / data.totalEnergyConsumption;
                if (hoursRemaining < 24) nationsWithCriticalFuel++;
            }
        }
        stats.put("nationsWithCriticalFuel", nationsWithCriticalFuel);
        
        return stats;
    }
    
    /**
     * Get fuel recommendations for a nation based on their needs and available resources.
     */
    public synchronized List<String> getFuelRecommendations(String nationId) {
        List<String> recommendations = new ArrayList<>();
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return recommendations;
        
        // Get current fuel statistics
        Map<String, Object> stats = getFuelStatistics(nationId);
        double hoursRemaining = (Double) stats.getOrDefault("hoursOfFuelRemaining", 0.0);
        double avgEfficiency = (Double) stats.getOrDefault("averageFuelEfficiency", 0.0);
        
        // If fuel is critical, recommend stockpiling
        if (hoursRemaining < 24) {
            recommendations.add("CRITICAL_FUEL_LEVEL: Immediate fuel stockpiling recommended");
        }
        
        // If efficiency is low, recommend upgrading to better fuel types
        if (avgEfficiency < 0.7) {
            recommendations.add("LOW_EFFICIENCY: Consider upgrading to higher-efficiency fuels");
        }
        
        // Recommend specific fuel types based on nation's technology and needs
        String primaryTech = plugin.getTechnologyTreeService().getTechnologyStatistics(nationId)
            .getOrDefault("topByTier", Collections.emptyList()).toString();
        
        if (primaryTech.contains("industrial")) {
            recommendations.add("INDUSTRIAL_FOCUSED: Consider Coal Coke, Lava Buckets, or Industrial Upgrade fuels");
        }
        if (primaryTech.contains("science")) {
            recommendations.add("SCIENCE_FOCUSED: Consider Matter Balls, Energy Crystals, or Uranium for high-efficiency");
        }
        if (primaryTech.contains("military")) {
            recommendations.add("MILITARY_FOCUSED: Ensure adequate supply of efficient fuels for military operations");
        }
        
        return recommendations;
    }
}