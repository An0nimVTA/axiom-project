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
 * Unified energy system that integrates all energy sources, consumers, and storage.
 * Provides a comprehensive framework for balancing energy across all AXIOM services.
 */
public class UnifiedEnergyService {
    private final AXIOM plugin;
    private final File energyDir;
    private final File energyStorageDir;
    private final File energyBalanceDir;
    
    // Primary energy storage: nationId -> energy data
    private final Map<String, EnergyData> nationEnergyData = new ConcurrentHashMap<>();
    
    // Energy production tracking
    private final Map<String, EnergyProduction> nationProduction = new ConcurrentHashMap<>();
    
    // Energy consumption tracking
    private final Map<String, EnergyConsumption> nationConsumption = new ConcurrentHashMap();
    
    // Energy resource mapping (for fuel integration)
    private final Map<String, EnergyResource> fuelResources = new ConcurrentHashMap<>();
    
    // Energy generation mappings for different sources
    private final Map<String, Double> energyGenerators = new ConcurrentHashMap<>();
    
    // Energy conversion rates
    private final Map<String, Double> conversionRates = new ConcurrentHashMap<>();
    
    /**
     * Energy storage and production data for a nation.
     */
    public static class EnergyData {
        double currentEnergy = 0.0;
        double maxEnergy = 10000.0; // Base storage, expandable via infrastructure
        double netProduction = 0.0; // Production - consumption
        long lastUpdated = System.currentTimeMillis();
        EnergyStorage storageType = EnergyStorage.BATTERY;
        
        public enum EnergyStorage {
            BATTERY(1.0), // Standard battery storage
            QUANTUM(5.0), // Quantum storage (5x capacity)
            FUSION(10.0); // Fusion storage (10x capacity)
            
            final double capacityMultiplier;
            EnergyStorage(double multiplier) {
                this.capacityMultiplier = multiplier;
            }
        }
    }
    
    /**
     * Energy production statistics for a nation.
     */
    public static class EnergyProduction {
        double baseProduction = 0.0;
        double renewableProduction = 0.0; // Solar, wind, etc.
        double fossilProduction = 0.0; // Coal, oil, etc.
        double modProduction = 0.0; // From mod integration
        double infrastructureBonus = 0.0; // From infrastructure
        String primarySource = "none";
        
        // Production by source breakdown
        Map<String, Double> sourceBreakdown = new HashMap<>();
    }
    
    /**
     * Energy consumption statistics for a nation.
     */
    public static class EnergyConsumption {
        double baseConsumption = 100.0; // Base consumption from population
        double industrialConsumption = 0.0; // From factories, quarries
        double warfareConsumption = 0.0; // From military equipment
        double infrastructureConsumption = 0.0; // From transportation, utilities
        double modConsumption = 0.0; // From mod integration
        
        // Consumption by sector breakdown
        Map<String, Double> sectorBreakdown = new HashMap<>();
    }
    
    /**
     * Energy resource with conversion efficiency.
     */
    public static class EnergyResource {
        String resourceId;
        double fePerUnit; // Forge Energy per unit
        double efficiency; // Conversion efficiency (0.0-1.0)
        String fuelType; // "solid", "liquid", "gas", "mod"
        
        public EnergyResource(String id, double fe, double eff, String type) {
            this.resourceId = id;
            this.fePerUnit = fe;
            this.efficiency = eff;
            this.fuelType = type;
        }
    }
    
    public UnifiedEnergyService(AXIOM plugin) {
        this.plugin = plugin;
        this.energyDir = new File(plugin.getDataFolder(), "energy");
        this.energyStorageDir = new File(energyDir, "storage");
        this.energyBalanceDir = new File(energyDir, "balance");
        
        energyDir.mkdirs();
        energyStorageDir.mkdirs();
        energyBalanceDir.mkdirs();
        
        initializeFuelResources();
        initializeConversionRates();
        loadAllData();
        
        // Update energy every 10 minutes (600 seconds)
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAllNations, 0, 20 * 60 * 10);
    }
    
    /**
     * Initialize fuel resources with FE (Forge Energy) conversion values.
     */
    private void initializeFuelResources() {
        // Standard Minecraft fuel resources
        fuelResources.put("coal", new EnergyResource("coal", 1600, 0.8, "solid"));
        fuelResources.put("charcoal", new EnergyResource("charcoal", 1600, 0.85, "solid"));
        fuelResources.put("coal_block", new EnergyResource("coal_block", 14400, 0.85, "solid"));
        fuelResources.put("wood", new EnergyResource("wood", 300, 0.6, "solid"));
        fuelResources.put("wood_planks", new EnergyResource("wood_planks", 300, 0.65, "solid"));
        
        // Mod-specific resources
        fuelResources.put("immersiveengineering_coal_coke", new EnergyResource("ie_coal_coke", 6400, 0.95, "solid"));
        fuelResources.put("immersiveengineering_hemp_oil", new EnergyResource("ie_hemp_oil", 1200, 0.7, "liquid"));
        fuelResources.put("industrialupgrade_uranium", new EnergyResource("iu_uranium", 200000, 0.98, "solid"));
        
        // AE2 energy resources
        fuelResources.put("ae2_matter_ball", new EnergyResource("ae2_matter_ball", 50000, 0.9, "solid"));
        fuelResources.put("ae2_singularity", new EnergyResource("ae2_singularity", 2000000, 0.95, "solid"));
        
        // Custom AXIOM resources
        fuelResources.put("axiom_energy_crystal", new EnergyResource("axiom_energy_crystal", 50000, 0.95, "crystal"));
        fuelResources.put("axiom_quantum_fuel", new EnergyResource("axiom_quantum_fuel", 500000, 0.99, "crystal"));
    }
    
    /**
     * Initialize conversion rates between different energy systems.
     */
    private void initializeConversionRates() {
        // Standard conversion rates (FE to AXIOM Energy, and vice versa)
        conversionRates.put("fe_to_axiom", 1.0); // 1 FE = 1 AXIOM Energy Unit
        conversionRates.put("axiom_to_fe", 1.0); // 1 AXIOM Energy Unit = 1 FE
        
        // Mod-specific conversion rates
        conversionRates.put("rf_to_axiom", 1.0); // 1 RF = 1 AXIOM Energy Unit
        conversionRates.put("axiom_to_rf", 1.0); // 1 AXIOM Energy Unit = 1 RF
        
        // Efficiency modifiers
        conversionRates.put("mod_efficiency", 0.95); // 5% loss in mod conversions
        conversionRates.put("transport_efficiency", 0.98); // 2% loss in transport
        conversionRates.put("storage_efficiency", 0.99); // 1% loss in storage
    }
    
    /**
     * Update all nations' energy data.
     */
    private void updateAllNations() {
        for (Nation nation : plugin.getNationManager().getAll()) {
            updateNationEnergy(nation.getId());
        }
    }
    
    /**
     * Update a specific nation's energy data.
     */
    public synchronized void updateNationEnergy(String nationId) {
        EnergyData data = nationEnergyData.computeIfAbsent(nationId, k -> new EnergyData());
        EnergyProduction production = nationProduction.computeIfAbsent(nationId, k -> new EnergyProduction());
        EnergyConsumption consumption = nationConsumption.computeIfAbsent(nationId, k -> new EnergyConsumption());
        
        // Calculate new production and consumption
        updateProduction(nationId, production);
        updateConsumption(nationId, consumption);
        
        // Calculate net production (production - consumption)
        double net = (production.baseProduction + production.renewableProduction + 
                     production.fossilProduction + production.modProduction + production.infrastructureBonus) -
                    (consumption.baseConsumption + consumption.industrialConsumption + 
                     consumption.warfareConsumption + consumption.infrastructureConsumption + consumption.modConsumption);
        
        data.netProduction = net;
        
        // Update energy storage with net production
        long now = System.currentTimeMillis();
        long timeDiff = now - data.lastUpdated;
        double timeInHours = timeDiff / (1000.0 * 60 * 60);
        
        // Add net production to current energy
        double energyChange = net * timeInHours;
        data.currentEnergy = Math.max(0, Math.min(data.maxEnergy, data.currentEnergy + energyChange));
        
        // Apply storage efficiency loss
        data.currentEnergy *= conversionRates.getOrDefault("storage_efficiency", 0.99);
        
        data.lastUpdated = now;
        
        // Save updated data
        saveEnergyData(nationId, data, production, consumption);
        
        // Trigger events based on energy status
        checkEnergyStatus(nationId, data, production, consumption);
    }
    
    /**
     * Update a nation's energy production.
     */
    private void updateProduction(String nationId, EnergyProduction production) {
        // Base production from infrastructure
        production.baseProduction = plugin.getInfrastructureService().getInfrastructureLevel(nationId, "utility") * 50;
        
        // Mod-based production (from Industrial Upgrade, Quantum Generators, etc.)
        if (plugin.getModIntegrationService().isIndustrialUpgradeAvailable()) {
            production.modProduction += 5000; // Base industrial production
            production.primarySource = "industrial";
        }
        if (plugin.getModIntegrationService().isQuantumGeneratorsAvailable()) {
            production.modProduction += 10000; // Quantum production
            production.primarySource = "quantum";
        }
        if (plugin.getModIntegrationService().isIEAvailable()) {
            production.modProduction += 3000; // IE production
            if (production.primarySource.equals("none")) production.primarySource = "industrial";
        }
        
        // Renewable production (from technology research)
        double renewableBonus = plugin.getTechnologyTreeService().getBonus(nationId, "energyProduction");
        production.renewableProduction = 1000 * renewableBonus;
        
        // Infrastructure bonus
        double infraBonus = plugin.getTechnologyTreeService().getBonus(nationId, "energyEfficiency");
        production.infrastructureBonus = production.baseProduction * (infraBonus - 1.0);
        
        // Update breakdown
        production.sourceBreakdown.clear();
        production.sourceBreakdown.put("base", production.baseProduction);
        production.sourceBreakdown.put("renewable", production.renewableProduction);
        production.sourceBreakdown.put("mod", production.modProduction);
        production.sourceBreakdown.put("infrastructure", production.infrastructureBonus);
    }
    
    /**
     * Update a nation's energy consumption.
     */
    private void updateConsumption(String nationId, EnergyConsumption consumption) {
        // Base consumption from population (cities)
        int population = plugin.getCityGrowthEngine().getTotalPopulation(nationId);
        consumption.baseConsumption = Math.max(100, population * 0.5); // 0.5 units per citizen
        
        // Industrial consumption (from factories, quarries, etc.)
        double industrialLevel = plugin.getTechnologyTreeService().getBonus(nationId, "productionBonus");
        consumption.industrialConsumption = Math.max(0, (industrialLevel - 1.0) * 1000);
        
        // Warfare consumption (from military activities)
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation != null && !nation.getEnemies().isEmpty()) {
            consumption.warfareConsumption = 2000; // High consumption during war
        } else {
            consumption.warfareConsumption = 500; // Base military consumption
        }
        
        // Infrastructure consumption (from transportation, utilities)
        double infraLevel = plugin.getInfrastructureService().getInfrastructureLevel(nationId, "utilities");
        consumption.infrastructureConsumption = infraLevel * 25;
        
        // Mod consumption (from mod usage)
        if (plugin.getModIntegrationService().getDetectedMods().size() > 0) {
            consumption.modConsumption = 800; // Base mod consumption
        }
        
        // Update sector breakdown
        consumption.sectorBreakdown.clear();
        consumption.sectorBreakdown.put("base", consumption.baseConsumption);
        consumption.sectorBreakdown.put("industrial", consumption.industrialConsumption);
        consumption.sectorBreakdown.put("warfare", consumption.warfareConsumption);
        consumption.sectorBreakdown.put("infrastructure", consumption.infrastructureConsumption);
        consumption.sectorBreakdown.put("mod", consumption.modConsumption);
    }
    
    /**
     * Check energy status and trigger appropriate events.
     */
    private void checkEnergyStatus(String nationId, EnergyData data, EnergyProduction production, EnergyConsumption consumption) {
        double storagePercentage = data.currentEnergy / data.maxEnergy;
        
        if (storagePercentage < 0.1) { // Critical low energy
            // Apply penalties
            plugin.getHappinessService().modifyHappiness(nationId, -10.0);
            plugin.getEconomyService().recordTransaction(nationId, -production.baseProduction * 0.1); // Economic penalty
        } else if (storagePercentage < 0.3) { // Low energy
            // Apply minor penalties
            plugin.getHappinessService().modifyHappiness(nationId, -5.0);
        } else if (storagePercentage > 0.9) { // High energy
            // Apply bonuses
            plugin.getHappinessService().modifyHappiness(nationId, 5.0);
        }
    }
    
    /**
     * Get current energy level for a nation.
     */
    public synchronized double getCurrentEnergy(String nationId) {
        EnergyData data = nationEnergyData.get(nationId);
        return data != null ? data.currentEnergy : 0.0;
    }
    
    /**
     * Get max energy storage for a nation.
     */
    public synchronized double getMaxEnergy(String nationId) {
        EnergyData data = nationEnergyData.get(nationId);
        return data != null ? data.maxEnergy : 10000.0; // Default base storage
    }
    
    /**
     * Check if nation has sufficient energy for a specific need.
     */
    public synchronized boolean hasSufficientEnergy(String nationId, double requiredEnergy) {
        return getCurrentEnergy(nationId) >= requiredEnergy;
    }
    
    /**
     * Consume energy from a nation's storage.
     */
    public synchronized boolean consumeEnergy(String nationId, double amount) {
        EnergyData data = nationEnergyData.get(nationId);
        if (data == null || data.currentEnergy < amount) {
            return false;
        }
        
        data.currentEnergy -= amount;
        // Don't save immediately - let the timer handle it
        return true;
    }
    
    /**
     * Add energy to a nation's storage.
     */
    public synchronized void addEnergy(String nationId, double amount) {
        EnergyData data = nationEnergyData.get(nationId);
        if (data == null) {
            data = new EnergyData();
            nationEnergyData.put(nationId, data);
        }
        
        data.currentEnergy = Math.min(data.maxEnergy, data.currentEnergy + amount);
        // Don't save immediately - let the timer handle it
    }
    
    /**
     * Convert fuel item to energy.
     */
    public synchronized double convertFuelToEnergy(String nationId, ItemStack fuelItem) {
        if (fuelItem == null) return 0.0;
        
        String fuelName = fuelItem.getType().name().toLowerCase();
        EnergyResource resource = fuelResources.get(fuelName);
        if (resource == null) return 0.0;
        
        // Calculate energy from fuel
        double baseEnergy = resource.fePerUnit * fuelItem.getAmount();
        double efficiency = resource.efficiency;
        
        // Apply nation-specific bonuses
        double techBonus = plugin.getTechnologyTreeService().getBonus(nationId, "energyEfficiency");
        
        return baseEnergy * efficiency * techBonus;
    }
    
    /**
     * Convert fuel block to energy.
     */
    public synchronized double convertFuelToEnergy(String nationId, Block fuelBlock) {
        if (fuelBlock == null) return 0.0;
        
        String fuelName = fuelBlock.getType().name().toLowerCase();
        EnergyResource resource = fuelResources.get(fuelName);
        if (resource == null) return 0.0;
        
        // Calculate energy from block
        double baseEnergy = resource.fePerUnit;
        double efficiency = resource.efficiency;
        
        // Apply nation-specific bonuses
        double techBonus = plugin.getTechnologyTreeService().getBonus(nationId, "energyEfficiency");
        
        return baseEnergy * efficiency * techBonus;
    }
    
    /**
     * Get comprehensive energy statistics for a nation.
     */
    public synchronized Map<String, Object> getEnergyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        EnergyData data = nationEnergyData.get(nationId);
        EnergyProduction production = nationProduction.get(nationId);
        EnergyConsumption consumption = nationConsumption.get(nationId);
        
        if (data == null) {
            stats.put("error", "No energy data found for nation: " + nationId);
            return stats;
        }
        
        // Core energy values
        stats.put("currentEnergy", data.currentEnergy);
        stats.put("maxEnergy", data.maxEnergy);
        stats.put("storagePercentage", (data.currentEnergy / data.maxEnergy) * 100);
        stats.put("netProduction", data.netProduction);
        stats.put("storageType", data.storageType.name());
        
        // Production values
        if (production != null) {
            stats.put("production", Map.of(
                "base", production.baseProduction,
                "renewable", production.renewableProduction,
                "fossil", production.fossilProduction,
                "mod", production.modProduction,
                "infrastructure", production.infrastructureBonus,
                "primarySource", production.primarySource,
                "breakdown", new HashMap<>(production.sourceBreakdown)
            ));
        }
        
        // Consumption values
        if (consumption != null) {
            stats.put("consumption", Map.of(
                "base", consumption.baseConsumption,
                "industrial", consumption.industrialConsumption,
                "warfare", consumption.warfareConsumption,
                "infrastructure", consumption.infrastructureConsumption,
                "mod", consumption.modConsumption,
                "breakdown", new HashMap<>(consumption.sectorBreakdown)
            ));
        }
        
        // Efficiency and rating
        double storageEfficiency = conversionRates.getOrDefault("storage_efficiency", 0.99);
        stats.put("storageEfficiency", storageEfficiency);
        
        String rating = "EXCELLENT";
        if (data.currentEnergy < data.maxEnergy * 0.2) rating = "CRITICAL";
        else if (data.currentEnergy < data.maxEnergy * 0.4) rating = "LOW";
        else if (data.currentEnergy < data.maxEnergy * 0.6) rating = "AVERAGE";
        else if (data.currentEnergy < data.maxEnergy * 0.8) rating = "GOOD";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get all fuel resources available.
     */
    public synchronized Map<String, EnergyResource> getAllFuelResources() {
        return new HashMap<>(fuelResources);
    }
    
    /**
     * Upgrade a nation's energy storage capacity.
     */
    public synchronized String upgradeStorage(String nationId, EnergyData.EnergyStorage newStorage, double cost) {
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return "Nation not found";
        
        if (nation.getTreasury() < cost) return "Insufficient funds";
        
        EnergyData data = nationEnergyData.computeIfAbsent(nationId, k -> new EnergyData());
        
        // Deduct cost
        nation.setTreasury(nation.getTreasury() - cost);
        try {
            plugin.getNationManager().save(nation);
        } catch (Exception e) {
            return "Failed to update nation treasury: " + e.getMessage();
        }
        
        // Update storage capacity
        data.storageType = newStorage;
        data.maxEnergy = 10000.0 * newStorage.capacityMultiplier;
        
        // Preserve current energy percentage after upgrade
        if (data.maxEnergy > 0) {
            double currentPercentage = data.currentEnergy / data.maxEnergy;
            data.currentEnergy = currentPercentage * data.maxEnergy;
        }
        
        // Save updated data
        saveEnergyData(nationId, data, 
            nationProduction.getOrDefault(nationId, new EnergyProduction()),
            nationConsumption.getOrDefault(nationId, new EnergyConsumption()));
        
        return "Energy storage upgraded to " + newStorage.name() + 
               " (Max: " + String.format("%.0f", data.maxEnergy) + " EU)";
    }
    
    /**
     * Get conversion rate between energy types.
     */
    public double getConversionRate(String fromType, String toType) {
        String key = fromType + "_to_" + toType;
        return conversionRates.getOrDefault(key, 1.0);
    }
    
    /**
     * Load all energy data from disk.
     */
    private void loadAllData() {
        loadEnergyData();
        loadProductionData();
        loadConsumptionData();
    }
    
    /**
     * Load energy storage data from disk.
     */
    private void loadEnergyData() {
        File[] files = energyStorageDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                
                EnergyData data = new EnergyData();
                data.currentEnergy = obj.has("currentEnergy") ? obj.get("currentEnergy").getAsDouble() : 0.0;
                data.maxEnergy = obj.has("maxEnergy") ? obj.get("maxEnergy").getAsDouble() : 10000.0;
                data.netProduction = obj.has("netProduction") ? obj.get("netProduction").getAsDouble() : 0.0;
                data.lastUpdated = obj.has("lastUpdated") ? obj.get("lastUpdated").getAsLong() : System.currentTimeMillis();
                
                if (obj.has("storageType")) {
                    try {
                        data.storageType = EnergyData.EnergyStorage.valueOf(obj.get("storageType").getAsString());
                    } catch (IllegalArgumentException e) {
                        data.storageType = EnergyData.EnergyStorage.BATTERY; // Default
                    }
                }
                
                nationEnergyData.put(nationId, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load energy data for " + f.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Load production data from disk.
     */
    private void loadProductionData() {
        File[] files = new File(energyDir, "production").listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                
                EnergyProduction prod = new EnergyProduction();
                prod.baseProduction = obj.has("baseProduction") ? obj.get("baseProduction").getAsDouble() : 0.0;
                prod.renewableProduction = obj.has("renewableProduction") ? obj.get("renewableProduction").getAsDouble() : 0.0;
                prod.fossilProduction = obj.has("fossilProduction") ? obj.get("fossilProduction").getAsDouble() : 0.0;
                prod.modProduction = obj.has("modProduction") ? obj.get("modProduction").getAsDouble() : 0.0;
                prod.infrastructureBonus = obj.has("infrastructureBonus") ? obj.get("infrastructureBonus").getAsDouble() : 0.0;
                prod.primarySource = obj.has("primarySource") ? obj.get("primarySource").getAsString() : "none";
                
                if (obj.has("sourceBreakdown")) {
                    JsonObject breakdownObj = obj.getAsJsonObject("sourceBreakdown");
                    for (String key : breakdownObj.keySet()) {
                        prod.sourceBreakdown.put(key, breakdownObj.get(key).getAsDouble());
                    }
                }
                
                nationProduction.put(nationId, prod);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load production data for " + f.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Load consumption data from disk.
     */
    private void loadConsumptionData() {
        File[] files = new File(energyDir, "consumption").listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                
                EnergyConsumption cons = new EnergyConsumption();
                cons.baseConsumption = obj.has("baseConsumption") ? obj.get("baseConsumption").getAsDouble() : 100.0;
                cons.industrialConsumption = obj.has("industrialConsumption") ? obj.get("industrialConsumption").getAsDouble() : 0.0;
                cons.warfareConsumption = obj.has("warfareConsumption") ? obj.get("warfareConsumption").getAsDouble() : 0.0;
                cons.infrastructureConsumption = obj.has("infrastructureConsumption") ? obj.get("infrastructureConsumption").getAsDouble() : 0.0;
                cons.modConsumption = obj.has("modConsumption") ? obj.get("modConsumption").getAsDouble() : 0.0;
                
                if (obj.has("sectorBreakdown")) {
                    JsonObject breakdownObj = obj.getAsJsonObject("sectorBreakdown");
                    for (String key : breakdownObj.keySet()) {
                        cons.sectorBreakdown.put(key, breakdownObj.get(key).getAsDouble());
                    }
                }
                
                nationConsumption.put(nationId, cons);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load consumption data for " + f.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Save energy data to disk.
     */
    private void saveEnergyData(String nationId, EnergyData data, EnergyProduction production, EnergyConsumption consumption) {
        saveEnergyData(nationId, data);
        saveProductionData(nationId, production);
        saveConsumptionData(nationId, consumption);
    }
    
    /**
     * Save energy storage data to disk.
     */
    private void saveEnergyData(String nationId, EnergyData data) {
        File f = new File(energyStorageDir, nationId + ".json");
        JsonObject obj = new JsonObject();
        obj.addProperty("currentEnergy", data.currentEnergy);
        obj.addProperty("maxEnergy", data.maxEnergy);
        obj.addProperty("netProduction", data.netProduction);
        obj.addProperty("lastUpdated", data.lastUpdated);
        obj.addProperty("storageType", data.storageType.name());
        
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(obj.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save energy data for " + nationId + ": " + e.getMessage());
        }
    }
    
    /**
     * Save production data to disk.
     */
    private void saveProductionData(String nationId, EnergyProduction production) {
        File prodDir = new File(energyDir, "production");
        prodDir.mkdirs();
        
        File f = new File(prodDir, nationId + ".json");
        JsonObject obj = new JsonObject();
        obj.addProperty("baseProduction", production.baseProduction);
        obj.addProperty("renewableProduction", production.renewableProduction);
        obj.addProperty("fossilProduction", production.fossilProduction);
        obj.addProperty("modProduction", production.modProduction);
        obj.addProperty("infrastructureBonus", production.infrastructureBonus);
        obj.addProperty("primarySource", production.primarySource);
        
        JsonObject breakdownObj = new JsonObject();
        for (Map.Entry<String, Double> entry : production.sourceBreakdown.entrySet()) {
            breakdownObj.addProperty(entry.getKey(), entry.getValue());
        }
        obj.add("sourceBreakdown", breakdownObj);
        
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(obj.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save production data for " + nationId + ": " + e.getMessage());
        }
    }
    
    /**
     * Save consumption data to disk.
     */
    private void saveConsumptionData(String nationId, EnergyConsumption consumption) {
        File consDir = new File(energyDir, "consumption");
        consDir.mkdirs();
        
        File f = new File(consDir, nationId + ".json");
        JsonObject obj = new JsonObject();
        obj.addProperty("baseConsumption", consumption.baseConsumption);
        obj.addProperty("industrialConsumption", consumption.industrialConsumption);
        obj.addProperty("warfareConsumption", consumption.warfareConsumption);
        obj.addProperty("infrastructureConsumption", consumption.infrastructureConsumption);
        obj.addProperty("modConsumption", consumption.modConsumption);
        
        JsonObject breakdownObj = new JsonObject();
        for (Map.Entry<String, Double> entry : consumption.sectorBreakdown.entrySet()) {
            breakdownObj.addProperty(entry.getKey(), entry.getValue());
        }
        obj.add("sectorBreakdown", breakdownObj);
        
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(obj.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save consumption data for " + nationId + ": " + e.getMessage());
        }
    }
    
    /**
     * Get global energy statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalEnergyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalCurrentEnergy = 0.0;
        double totalMaxEnergy = 0.0;
        double totalNetProduction = 0.0;
        int nationsWithEnergy = 0;
        int nationsWithSurplus = 0;
        int nationsWithDeficit = 0;
        
        double totalProduction = 0.0;
        double totalConsumption = 0.0;
        
        // Aggregate statistics
        for (Map.Entry<String, EnergyData> entry : nationEnergyData.entrySet()) {
            EnergyData data = entry.getValue();
            totalCurrentEnergy += data.currentEnergy;
            totalMaxEnergy += data.maxEnergy;
            totalNetProduction += data.netProduction;
            nationsWithEnergy++;
            
            if (data.netProduction > 0) nationsWithSurplus++;
            else if (data.netProduction < 0) nationsWithDeficit++;
        }
        
        for (Map.Entry<String, EnergyProduction> entry : nationProduction.entrySet()) {
            EnergyProduction prod = entry.getValue();
            totalProduction += (prod.baseProduction + prod.renewableProduction + 
                              prod.fossilProduction + prod.modProduction + prod.infrastructureBonus);
        }
        
        for (Map.Entry<String, EnergyConsumption> entry : nationConsumption.entrySet()) {
            EnergyConsumption cons = entry.getValue();
            totalConsumption += (cons.baseConsumption + cons.industrialConsumption + 
                               cons.warfareConsumption + cons.infrastructureConsumption + cons.modConsumption);
        }
        
        stats.put("totalNations", plugin.getNationManager().getAll().size());
        stats.put("nationsWithEnergy", nationsWithEnergy);
        stats.put("totalCurrentEnergy", totalCurrentEnergy);
        stats.put("totalMaxEnergy", totalMaxEnergy);
        stats.put("totalNetProduction", totalNetProduction);
        stats.put("totalProduction", totalProduction);
        stats.put("totalConsumption", totalConsumption);
        stats.put("nationsWithSurplus", nationsWithSurplus);
        stats.put("nationsWithDeficit", nationsWithDeficit);
        
        if (nationsWithEnergy > 0) {
            stats.put("averageCurrentEnergy", totalCurrentEnergy / nationsWithEnergy);
            stats.put("averageMaxEnergy", totalMaxEnergy / nationsWithEnergy);
            stats.put("averageNetProduction", totalNetProduction / nationsWithEnergy);
        }
        
        // Energy efficiency distribution
        Map<String, Integer> efficiencyDistribution = new HashMap<>();
        int excellent = 0, good = 0, average = 0, low = 0, critical = 0;
        for (EnergyData data : nationEnergyData.values()) {
            double percentage = data.currentEnergy / data.maxEnergy * 100;
            if (percentage >= 80) excellent++;
            else if (percentage >= 60) good++;
            else if (percentage >= 40) average++;
            else if (percentage >= 20) low++;
            else critical++;
        }
        efficiencyDistribution.put("excellent", excellent);
        efficiencyDistribution.put("good", good);
        efficiencyDistribution.put("average", average);
        efficiencyDistribution.put("low", low);
        efficiencyDistribution.put("critical", critical);
        stats.put("efficiencyDistribution", efficiencyDistribution);
        
        // Top nations by energy storage
        List<Map.Entry<String, Double>> topByEnergy = new ArrayList<>();
        for (Map.Entry<String, EnergyData> entry : nationEnergyData.entrySet()) {
            topByEnergy.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().currentEnergy));
        }
        topByEnergy.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByEnergy", topByEnergy.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        // Top nations by net production
        List<Map.Entry<String, Double>> topByNet = new ArrayList<>();
        for (Map.Entry<String, EnergyData> entry : nationEnergyData.entrySet()) {
            topByNet.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().netProduction));
        }
        topByNet.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByNetProduction", topByNet.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
    
    /**
     * Reset all energy data (for testing purposes).
     */
    public synchronized void resetAllEnergyData() {
        nationEnergyData.clear();
        nationProduction.clear();
        nationConsumption.clear();
    }
    
    /**
     * Get energy resource by ID.
     */
    public EnergyResource getEnergyResource(String resourceId) {
        return fuelResources.get(resourceId);
    }
}