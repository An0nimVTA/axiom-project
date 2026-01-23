package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for modded economic balance management
 * Manages resource flows between different mod ecosystems to maintain economic balance
 */
public class ModdedEconomicBalanceService {
    private final AXIOM plugin;
    private final EconomyService economyService;
    private final RecipeIntegrationService recipeIntegrationService;
    private final ModIntegrationEnhancementService modIntegrationEnhancementService;
    private final EnhancedModBalanceService enhancedModBalanceService;
    
    // Mod resource value mappings
    private final Map<String, ModResourceValue> modResourceValues = new ConcurrentHashMap<>();
    
    // Regional market pricing based on mod availability
    private final Map<String, RegionalModMarket> regionalModMarkets = new ConcurrentHashMap<>();
    
    // Nation-specific mod economy statistics
    private final Map<String, ModEconomyStatistics> nationModEconomies = new ConcurrentHashMap<>();
    
    // Price stability tracking
    private final Map<String, ModPriceStability> modPriceStabilities = new ConcurrentHashMap<>();
    
    public static class ModResourceValue {
        String modId;
        String itemId;
        String vanillaEquivalent; // Equivalent vanilla item for valuation
        double baseValue; // Value relative to vanilla resources
        double supplyMultiplier; // How supply affects value
        double demandMultiplier; // How demand affects value
        String category; // "ore", "ingot", "tool", "weapon", "component", etc.
        double scarcityFactor; // How rare the resource is in the world
        
        public ModResourceValue(String modId, String itemId, String vanillaEquivalent, double baseValue) {
            this.modId = modId;
            this.itemId = itemId;
            this.vanillaEquivalent = vanillaEquivalent;
            this.baseValue = baseValue;
            this.supplyMultiplier = 1.0;
            this.demandMultiplier = 1.0;
            this.category = "misc";
            this.scarcityFactor = 1.0; // 1.0 = normal rarity
        }
    }
    
    public static class RegionalModMarket {
        String regionName; // Usually nation name
        Map<String, Double> modResourcePrices = new HashMap<>(); // modItemId -> current price
        Map<String, Integer> modResourceSupplies = new HashMap<>(); // modItemId -> supply amount
        Map<String, Integer> modResourceDemands = new HashMap<>(); // modItemId -> demand amount
        long lastUpdate = System.currentTimeMillis();
        
        public RegionalModMarket(String regionName) {
            this.regionName = regionName;
        }
    }
    
    public static class ModEconomyStatistics {
        String nationId;
        double totalModResourcesValue; // Total value of mod resources
        int uniqueModItemsTraded; // Number of unique mod items traded
        double modTradeVolume; // Volume of mod trades
        int modFactories; // Number of mod-based production facilities
        double modEnergyConsumption; // Energy consumed by mod factories
        double modProductionOutput; // Production output from mod systems
        long lastCalculated = System.currentTimeMillis();
        
        public ModEconomyStatistics(String nationId) {
            this.nationId = nationId;
            this.totalModResourcesValue = 0.0;
            this.uniqueModItemsTraded = 0;
            this.modTradeVolume = 0.0;
            this.modFactories = 0;
            this.modEnergyConsumption = 0.0;
            this.modProductionOutput = 0.0;
        }
    }
    
    public static class ModPriceStability {
        String modItemId;
        double historicalAveragePrice;
        double priceVolatility; // Standard deviation
        double stabilityIndex; // 0-100 scale, 100 = perfectly stable
        long trackingPeriodStart; // When tracking started
        List<Double> recentPrices = new ArrayList<>(); // Last N prices
        
        public ModPriceStability(String modItemId) {
            this.modItemId = modItemId;
            this.historicalAveragePrice = 0.0;
            this.priceVolatility = 0.0;
            this.stabilityIndex = 100.0; // Initially perfectly stable
            this.trackingPeriodStart = System.currentTimeMillis();
            // Initialize with some prices to avoid division by zero
            recentPrices.add(1.0);
            recentPrices.add(1.0);
            recentPrices.add(1.0);
        }
        
        public void addNewPrice(double newPrice) {
            recentPrices.add(newPrice);
            if (recentPrices.size() > 20) { // Keep only last 20 prices
                recentPrices.remove(0);
            }
            
            // Update statistics
            updateStabilityStats();
        }
        
        private void updateStabilityStats() {
            if (recentPrices.isEmpty()) return;
            
            // Calculate average
            double sum = recentPrices.stream().mapToDouble(Double::doubleValue).sum();
            historicalAveragePrice = sum / recentPrices.size();
            
            // Calculate volatility (standard deviation)
            double variance = 0.0;
            for (double price : recentPrices) {
                variance += Math.pow(price - historicalAveragePrice, 2);
            }
            variance /= Math.max(1, recentPrices.size()); // Avoid division by zero
            priceVolatility = Math.sqrt(variance);
            
            // Calculate stability index (inverse of volatility, scaled)
            stabilityIndex = Math.max(0, 100 - (priceVolatility * 100));
        }
    }

    public ModdedEconomicBalanceService(AXIOM plugin) {
        this.plugin = plugin;
        this.economyService = plugin.getEconomyService();
        this.recipeIntegrationService = plugin.getRecipeIntegrationService();
        this.modIntegrationEnhancementService = plugin.getModIntegrationEnhancementService();
        this.enhancedModBalanceService = plugin.getEnhancedModBalanceService();
        
        initializeModResourceValues();
        startEconomicMonitoring();
        startEconomicBalancing();
    }
    
    /**
     * Start periodic economic balancing
     */
    private void startEconomicBalancing() {
        // Run economic balancing every 30 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                applyEconomicBalanceToNations();
            }
        }.runTaskTimer(plugin, 20 * 60 * 30, 20 * 60 * 30); // Every 30 minutes
    }
    
    /**
     * Apply economic balancing to all nations based on their mod usage
     */
    private void applyEconomicBalanceToNations() {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            applyEconomicBalancing(nation.getId());
        }
    }
    
    /**
     * Initialize base values for mod resources based on their rarity and utility
     */
    private void initializeModResourceValues() {
        // Initialize values for Industrial Upgrade resources
        initializeModResourceValue("industrialupgrade", "steel_ingot", "iron_ingot", 1.5);
        initializeModResourceValue("industrialupgrade", "electrum_ingot", "gold_ingot", 2.0);
        initializeModResourceValue("industrialupgrade", "invar_ingot", "iron_ingot", 1.3);
        initializeModResourceValue("industrialupgrade", "bronze_ingot", "copper_ingot", 1.2);
        
        // Initialize values for Immersive Engineering resources
        initializeModResourceValue("immersiveengineering", "ingot_steel", "iron_ingot", 1.4);
        initializeModResourceValue("immersiveengineering", "ingot_copper", "copper_ingot", 1.1);
        initializeModResourceValue("immersiveengineering", "ingot_lead", "netherite_ingot", 3.0); // Rare
        initializeModResourceValue("immersiveengineering", "ingot_silver", "gold_ingot", 1.8);  // Rare
        initializeModResourceValue("immersiveengineering", "ingot_nickel", "iron_ingot", 1.6);  // Relatively rare
        
        // Initialize values for Applied Energistics 2 resources
        initializeModResourceValue("appliedenergistics2", "certus_quartz_crystal", "quartz", 2.5); // Rare
        initializeModResourceValue("appliedenergistics2", "fluix_crystal", "redstone", 3.0);     // Very rare
        initializeModResourceValue("appliedenergistics2", "charged_certus_quartz_crystal", "redstone", 4.0); // Extremaly rare
        
        // Initialize values for TACZ resources
        initializeModResourceValue("tacz", "bullet", "arrow", 0.5); // Consumable
        initializeModResourceValue("tacz", "gunpowder_substitute", "gunpowder", 1.2); // Better
        initializeModResourceValue("tacz", "gun_frame", "iron_ingot", 5.0); // Complex component
        initializeModResourceValue("tacz", "barrel", "iron_ingot", 3.0); // Precision component
        
        // Initialize values for PointBlank resources
        initializeModResourceValue("pointblank", "bullet", "arrow", 0.5); // Consumable
        initializeModResourceValue("pointblank", "shell", "arrow", 0.8); // Specialized
        initializeModResourceValue("pointblank", "weapon_part", "iron_ingot", 4.0); // Precision part
        
        // Initialize values for Ballistix resources
        initializeModResourceValue("ballistix", "lithium_ingot", "netherite_ingot", 3.5); // Very rare
        initializeModResourceValue("ballistix", "titanium_ingot", "netherite_ingot", 4.0); // Very rare
        initializeModResourceValue("ballistix", "explosive_component", "tnt", 2.5); // Specialized
        
        // Initialize values for Superb Warfare resources
        initializeModResourceValue("superbwarfare", "tungsten_ingot", "netherite_ingot", 4.5); // Extremely rare
        initializeModResourceValue("superbwarfare", "uranium_ingot", "netherite_ingot", 5.0); // Dangerous/rare
        initializeModResourceValue("superbwarfare", "vehicle_component", "iron_block", 6.0); // Complex
        
        // Initialize values for Warium resources
        initializeModResourceValue("warium", "explosive_compound", "tnt", 3.0); // Powerful
        initializeModResourceValue("warium", "rocket_fuel", "blaze_powder", 2.8); // Specialized
        initializeModResourceValue("warium", "advanced_weapon_part", "diamond", 7.0); // Hyper-specialized
        
        // Initialize values for Mekanism resources
        initializeModResourceValue("mekanism", "ingot_steel", "iron_ingot", 1.4);
        initializeModResourceValue("mekanism", "ingot_osmium", "netherite_ingot", 2.8); // Rare
        initializeModResourceValue("mekanism", "ingot_refined_obsidian", "obsidian", 3.2); // Specialized
        
        // Initialize values for Thermal Series resources
        initializeModResourceValue("thermal", "copper_ingot", "copper_ingot", 1.05); // Slightly better
        initializeModResourceValue("thermal", "tin_ingot", "copper_ingot", 1.1); // Alternative
        initializeModResourceValue("thermal", "silver_ingot", "gold_ingot", 1.75); // Better than vanilla
        
        // Initialize values for Simply Quarries resources
        initializeModResourceValue("simplyquarries", "quarry_frame", "iron_block", 8.0); // Very complex
        initializeModResourceValue("simplyquarries", "drill_head", "diamond", 5.0); // Specialized tool
        
        // Initialize values for Quantum Generators resources
        initializeModResourceValue("quantumgenerators", "quantum_ingot", "netherite_ingot", 6.0); // Ultimate resource
        initializeModResourceValue("quantumgenerators", "dimensional_core", "nether_star", 8.0); // Legendary
    }
    
    private void initializeModResourceValue(String modId, String itemId, String vanillaEquivalent, double baseValue) {
        String fullItemId = modId + ":" + itemId;
        ModResourceValue value = new ModResourceValue(modId, fullItemId, vanillaEquivalent, baseValue);
        
        // Set category based on item name
        if (itemId.contains("ingot") || itemId.contains("ore")) {
            value.category = "resource";
        } else if (itemId.contains("bullet") || itemId.contains("shell") || itemId.contains("ammo")) {
            value.category = "munition";
        } else if (itemId.contains("part") || itemId.contains("component") || itemId.contains("frame")) {
            value.category = "component";
        } else if (itemId.contains("weapon") || itemId.contains("gun") || itemId.contains("cannon")) {
            value.category = "weapon";
        } else if (itemId.contains("core") || itemId.contains("crystal")) {
            value.category = "component";
        }
        
        // Set scarcity based on mod tier
        switch (modId) {
            case "appliedenergistics2":
            case "mekanism":
            case "quantumgenerators":
                value.scarcityFactor = 2.0; // High-tech mods have rare materials
                break;
            case "ballistix":
            case "warium":
            case "superbwarfare":
                value.scarcityFactor = 2.5; // Military mods have rare materials
                break;
            case "industrialupgrade":
            case "immersiveengineering":
                value.scarcityFactor = 1.5; // Mid-tier industrial mods
                break;
            default:
                value.scarcityFactor = 1.0; // Standard rarity
        }
        
        modResourceValues.put(fullItemId, value);
    }
    
    /**
     * Start periodic economic monitoring and price adjustments
     */
    private void startEconomicMonitoring() {
        // Run every 15 minutes to update economic statistics
        new BukkitRunnable() {
            @Override
            public void run() {
                updateModEconomicStatistics();
                adjustModMarketPrices();
                checkEconomicImbalances();
            }
        }.runTaskTimer(plugin, 20 * 60 * 15, 20 * 60 * 15); // Every 15 minutes
    }
    
    /**
     * Update economic statistics for mod resources
     */
    private void updateEconomicStatistics() {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            updateNationModEconomy(nation.getId());
        }
    }
    
    /**
     * Update mod economy statistics for a specific nation
     */
    public void updateNationModEconomy(String nationId) {
        ModEconomyStatistics stats = nationModEconomies.computeIfAbsent(nationId, ModEconomyStatistics::new);
        
        // In a real implementation, this would scan the nation for:
        // - Stored mod resources
        // - Active mod-based machines
        // - Recent mod trades
        // - Energy consumption from mod systems
        // For now, just update timestamp
        
        stats.lastCalculated = System.currentTimeMillis();
        
        // Calculate value based on available mods in nation
        double modPowerScore = plugin.getEnhancedModBalanceService().calculateBalancedPowerScore(nationId);
        stats.totalModResourcesValue = modPowerScore * 10000; // Rough estimation
        
        // This would be more sophisticated in a full implementation
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation != null) {
            stats.modFactories = (int) (nation.getClaimedChunks() * 0.1); // Rough estimate based on territory
            stats.modTradeVolume = nation.getTreasury() * 0.05; // Rough estimate based on treasury
        }
    }
    
    /**
     * Adjust market prices based on supply and demand
     */
    private void adjustModMarketPrices() {
        for (Map.Entry<String, RegionalModMarket> entry : regionalModMarkets.entrySet()) {
            RegionalModMarket market = entry.getValue();
            
            for (Map.Entry<String, Integer> supplyEntry : market.modResourceSupplies.entrySet()) {
                String modItemId = supplyEntry.getKey();
                int supply = supplyEntry.getValue();
                int demand = market.modResourceDemands.getOrDefault(modItemId, 0);
                
                // Get base value
                ModResourceValue baseValue = modResourceValues.get(modItemId);
                if (baseValue != null) {
                    // Calculate price based on supply/demand ratio
                    double supplyDemandRatio = supply > 0 ? (double) demand / supply : Double.MAX_VALUE;
                    
                    // Higher demand than supply increases price, lower decreases it
                    double priceMultiplier = 1.0 + (supplyDemandRatio - 1.0) * 0.3; // 30% sensitivity
                    double newPrice = baseValue.baseValue * priceMultiplier * baseValue.scarcityFactor;
                    
                    market.modResourcePrices.put(modItemId, newPrice);
                    
                    // Update stability tracking
                    ModPriceStability stability = modPriceStabilities.computeIfAbsent(modItemId, ModPriceStability::new);
                    stability.addNewPrice(newPrice);
                }
            }
            
            market.lastUpdate = System.currentTimeMillis();
        }
    }
    
    /**
     * Check for economic imbalances between mod ecosystems
     */
    private void checkEconomicImbalances() {
        for (Map.Entry<String, RegionalModMarket> entry : regionalModMarkets.entrySet()) {
            String regionName = entry.getKey();
            RegionalModMarket market = entry.getValue();
            
            // Find extremely overvalued or undervalued items
            Map<String, Double> prices = market.modResourcePrices;
            double averagePrice = prices.values().stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
            
            for (Map.Entry<String, Double> priceEntry : prices.entrySet()) {
                String modItemId = priceEntry.getKey();
                double currentPrice = priceEntry.getValue();
                
                double deviation = Math.abs(currentPrice - averagePrice) / averagePrice;
                
                // If price is 2x or 0.5x of average, it's imbalanced
                if (deviation > 1.0) {
                    // Log economic imbalance
                    plugin.getLogger().info("Economic imbalance detected in " + regionName + 
                        " for " + modItemId + " (price: " + String.format("%.2f", currentPrice) + 
                        ", avg: " + String.format("%.2f", averagePrice) + ")");
                    
                    // Apply subtle market correction
                    applyMarketCorrection(regionName, modItemId, currentPrice, averagePrice);
                }
            }
        }
    }
    
    /**
     * Apply market correction to bring prices closer to equilibrium
     */
    private void applyMarketCorrection(String regionName, String modItemId, double currentPrice, double targetPrice) {
        RegionalModMarket market = regionalModMarkets.get(regionName);
        if (market != null) {
            // Gradually move price toward target (don't do immediate correction)
            double correctedPrice = currentPrice * 0.95 + targetPrice * 0.05; // 5% movement toward target
            market.modResourcePrices.put(modItemId, correctedPrice);
        }
    }
    
    /**
     * Get the economic value of a mod item
     */
    public double getModItemValue(String modItemId) {
        ModResourceValue value = modResourceValues.get(modItemId);
        if (value != null) {
            // Apply current market conditions
            double baseValue = value.baseValue;
            
            // Get regional market if player is online
            // For simplicity, we'll use a global average
            return baseValue * value.scarcityFactor;
        }
        
        // If not in our system, assume it's worth vanilla equivalent
        String[] parts = modItemId.split(":", 2);
        if (parts.length == 2) {
            // Try to map to vanilla equivalent
            String vanillaEquivalent = getVanillaEquivalent(parts[1]);
            if (vanillaEquivalent != null) {
                Material vanillaMat = Material.getMaterial(vanillaEquivalent.toUpperCase());
                if (vanillaMat != null) {
                    return getBaseVanillaValue(vanillaMat);
                }
            }
        }
        
        return 1.0; // Default value
    }
    
    /**
     * Get vanilla equivalent for a mod resource name
     */
    private String getVanillaEquivalent(String itemName) {
        itemName = itemName.toLowerCase();
        
        if (itemName.contains("ingot")) {
            if (itemName.contains("steel")) return "IRON_INGOT";
            else if (itemName.contains("gold")) return "GOLD_INGOT";
            else if (itemName.contains("copper")) return "COPPER_INGOT";
            else return "IRON_INGOT"; // Default
        } else if (itemName.contains("ore")) {
            if (itemName.contains("copper")) return "COPPER_ORE";
            else if (itemName.contains("iron")) return "IRON_ORE";
            else if (itemName.contains("gold")) return "GOLD_ORE";
            else return "STONE"; // Default ore equivalent
        } else if (itemName.contains("bullet") || itemName.contains("ammo")) {
            return "ARROW";
        } else if (itemName.contains("crystal")) {
            return "DIAMOND";
        } else if (itemName.contains("core")) {
            return "NETHER_STAR";
        } else if (itemName.contains("dust")) {
            return "REDSTONE";
        } else {
            return "STONE"; // Default
        }
    }
    
    /**
     * Get base vanilla value for a material
     */
    private double getBaseVanillaValue(Material material) {
        switch (material) {
            case DIAMOND: return 10.0;
            case EMERALD: return 8.0;
            case NETHERITE_INGOT: return 15.0;
            case GOLD_INGOT: return 5.0;
            case IRON_INGOT: return 2.0;
            case COPPER_INGOT: return 1.5;
            case REDSTONE: return 3.0;
            case GLOWSTONE_DUST: return 2.5;
            case BLAZE_POWDER: return 7.0;
            case GHAST_TEAR: return 12.0;
            case NETHER_STAR: return 50.0;
            default: return 1.0;
        }
    }
    
    /**
     * Get regional market price for a mod item
     */
    public double getRegionalModPrice(String regionName, String modItemId) {
        RegionalModMarket market = regionalModMarkets.computeIfAbsent(regionName, RegionalModMarket::new);
        
        Double price = market.modResourcePrices.get(modItemId);
        if (price == null) {
            // Calculate initial price based on base value
            double baseValue = getModItemValue(modItemId);
            market.modResourcePrices.put(modItemId, baseValue);
            price = baseValue;
        }
        
        return price;
    }
    
    /**
     * Track mod resource transaction
     */
    public void trackModResourceTransaction(String regionName, String modItemId, int amount, boolean isSupply) {
        RegionalModMarket market = regionalModMarkets.computeIfAbsent(regionName, RegionalModMarket::new);
        
        if (isSupply) {
            market.modResourceSupplies.merge(modItemId, amount, Integer::sum);
        } else {
            market.modResourceDemands.merge(modItemId, amount, Integer::sum);
        }
    }
    
    /**
     * Get mod economic statistics for a nation
     */
    public Map<String, Object> getNationModEconomyStats(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        ModEconomyStatistics modStats = nationModEconomies.get(nationId);
        if (modStats != null) {
            stats.put("totalModResourcesValue", modStats.totalModResourcesValue);
            stats.put("uniqueModItemsTraded", modStats.uniqueModItemsTraded);
            stats.put("modTradeVolume", modStats.modTradeVolume);
            stats.put("modFactories", modStats.modFactories);
            stats.put("modEnergyConsumption", modStats.modEnergyConsumption);
            stats.put("modProductionOutput", modStats.modProductionOutput);
            stats.put("lastCalculated", modStats.lastCalculated);
        } else {
            stats.put("totalModResourcesValue", 0.0);
            stats.put("uniqueModItemsTraded", 0);
            stats.put("modTradeVolume", 0.0);
            stats.put("modFactories", 0);
            stats.put("modEnergyConsumption", 0.0);
            stats.put("modProductionOutput", 0.0);
            stats.put("lastCalculated", 0L);
        }
        
        // Add regional market data
        stats.put("regionalMarket", getRegionalMarketData(nationId));
        
        return stats;
    }
    
    /**
     * Get regional market data
     */
    private Map<String, Object> getRegionalMarketData(String regionName) {
        Map<String, Object> marketData = new HashMap<>();
        RegionalModMarket market = regionalModMarkets.get(regionName);
        
        if (market != null) {
            marketData.put("lastUpdate", market.lastUpdate);
            marketData.put("resourceCount", market.modResourcePrices.size());
            
            // Top 10 most expensive mod resources
            List<Map.Entry<String, Double>> topResources = new ArrayList<>(market.modResourcePrices.entrySet());
            topResources.sort(Map.Entry.<String, Double>comparingByValue().reversed());
            
            List<Map<String, Object>> top10 = new ArrayList<>();
            for (int i = 0; i < Math.min(10, topResources.size()); i++) {
                Map.Entry<String, Double> entry = topResources.get(i);
                Map<String, Object> resource = new HashMap<>();
                resource.put("id", entry.getKey());
                resource.put("price", entry.getValue());
                resource.put("supply", market.modResourceSupplies.getOrDefault(entry.getKey(), 0));
                resource.put("demand", market.modResourceDemands.getOrDefault(entry.getKey(), 0));
                top10.add(resource);
            }
            marketData.put("topExpensiveResources", top10);
            
            // Price stability data for key resources
            List<Map<String, Object>> stabilityData = new ArrayList<>();
            for (String resourceId : Arrays.asList("tacz:gun_frame", "appliedenergistics2:certus_quartz_crystal", 
                "immersiveengineering:ingot_steel", "industrialupgrade:steel_ingot")) {
                ModPriceStability stability = modPriceStabilities.get(resourceId);
                if (stability != null) {
                    Map<String, Object> stabilityInfo = new HashMap<>();
                    stabilityInfo.put("resourceId", resourceId);
                    stabilityInfo.put("averagePrice", stability.historicalAveragePrice);
                    stabilityInfo.put("volatility", stability.priceVolatility);
                    stabilityInfo.put("stabilityIndex", stability.stabilityIndex);
                    stabilityData.add(stabilityInfo);
                }
            }
            marketData.put("priceStabilities", stabilityData);
        } else {
            marketData.put("lastUpdate", 0L);
            marketData.put("resourceCount", 0);
            marketData.put("topExpensiveResources", new ArrayList<>());
            marketData.put("priceStabilities", new ArrayList<>());
        }
        
        return marketData;
    }
    
    /**
     * Get global mod economy statistics
     */
    public Map<String, Object> getGlobalModEconomyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNationsTracked", nationModEconomies.size());
        
        double totalResourceValue = 0.0;
        double averageFactories = 0.0;
        double averageProduction = 0.0;
        
        for (ModEconomyStatistics econStat : nationModEconomies.values()) {
            totalResourceValue += econStat.totalModResourcesValue;
            averageFactories += econStat.modFactories;
            averageProduction += econStat.modProductionOutput;
        }
        
        stats.put("totalGlobalModValue", totalResourceValue);
        stats.put("averageModFactories", nationModEconomies.size() > 0 ? averageFactories / nationModEconomies.size() : 0);
        stats.put("averageModProduction", nationModEconomies.size() > 0 ? averageProduction / nationModEconomies.size() : 0);
        
        // Most popular mod resources globally
        Map<String, Integer> globalSupply = new HashMap<>();
        Map<String, Integer> globalDemand = new HashMap<>();
        
        for (RegionalModMarket market : regionalModMarkets.values()) {
            for (Map.Entry<String, Integer> supplyEntry : market.modResourceSupplies.entrySet()) {
                globalSupply.merge(supplyEntry.getKey(), supplyEntry.getValue(), Integer::sum);
            }
            for (Map.Entry<String, Integer> demandEntry : market.modResourceDemands.entrySet()) {
                globalDemand.merge(demandEntry.getKey(), demandEntry.getValue(), Integer::sum);
            }
        }
        
        stats.put("globalSupply", globalSupply);
        stats.put("globalDemand", globalDemand);
        
        // Top 20 most traded mod resources
        Map<String, Integer> globalTradeVolume = new HashMap<>();
        for (String resourceId : globalSupply.keySet()) {
            globalTradeVolume.put(resourceId, globalSupply.get(resourceId) + 
                globalDemand.getOrDefault(resourceId, 0));
        }
        
        List<Map.Entry<String, Integer>> topTraded = new ArrayList<>(globalTradeVolume.entrySet());
        topTraded.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        
        List<Map<String, Object>> topTradedList = new ArrayList<>();
        for (int i = 0; i < Math.min(20, topTraded.size()); i++) {
            Map.Entry<String, Integer> entry = topTraded.get(i);
            Map<String, Object> resource = new HashMap<>();
            resource.put("id", entry.getKey());
            resource.put("totalVolume", entry.getValue());
            resource.put("supply", globalSupply.get(entry.getKey()));
            resource.put("demand", globalDemand.getOrDefault(entry.getKey(), 0));
            topTradedList.add(resource);
        }
        stats.put("topTradedResources", topTradedList);
        
        return stats;
    }
    
    /**
     * Apply economic balancing measures to a nation
     */
    public void applyEconomicBalancing(String nationId) {
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return;
        
        ModEconomyStatistics stats = nationModEconomies.get(nationId);
        if (stats == null) {
            updateNationModEconomy(nationId);
            stats = nationModEconomies.get(nationId);
        }
        
        if (stats != null) {
            // Check if nation is economically unbalanced
            double modPowerScore = plugin.getEnhancedModBalanceService().calculateBalancedPowerScore(nationId);
            double expectedEconomicValue = modPowerScore * 10000; // Expected value based on tech level
            
            if (stats.totalModResourcesValue > expectedEconomicValue * 2.0) {
                // Nation is economically overpowered relative to tech level
                plugin.getLogger().info("Applying economic balancing to overpowered nation: " + nationId);
                
                // Reduce effectiveness of mod-based income sources
                double reductionFactor = 0.8; // 20% reduction
                nation.setIncomeMultiplier(nation.getIncomeMultiplier() * reductionFactor);
                
                // Send notification to nation members
                String message = "§c[ЭКОНОМИЧЕСКИЙ БАЛАНС] §fВаша нация имеет §cПРЕВЫШЕННУЮ§f экономическую мощь. " +
                    "§7(Понижена эффективность доходов от модов)";
                
                for (UUID citizenId : nation.getCitizens()) {
                    Player player = Bukkit.getPlayer(citizenId);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(message);
                    }
                }
            } else if (stats.totalModResourcesValue < expectedEconomicValue * 0.5) {
                // Nation might need economic incentives
                double incentiveFactor = 1.2; // 20% boost
                nation.setIncomeMultiplier(nation.getIncomeMultiplier() * incentiveFactor);
            }
        }
    }
    
    /**
     * Get available modpacks
     */
    public Collection<ModResourceValue> getAvailableModPacks() {
        // This would return a list of modpacks, but for now return all mod resource values as example
        return new ArrayList<>(modResourceValues.values());
    }
    
    /**
     * Get modpack by ID
     */
    public ModResourceValue getModPack(String modPackId) {
        // This would look up a real modpack, but for now return the resource value
        return modResourceValues.get(modPackId);
    }
    
    /**
     * Get active modpack for a nation
     */
    public String getActiveModPackForNation(String nationId) {
        // This would return the actual active modpack, but for now just return null
        return null;
    }
    
    /**
     * Get all available modpacks
     */
    public Collection<ModResourceValue> getAvailableModPacks() {
        return new ArrayList<>(modResourceValues.values());
    }
    
    /**
     * Get nations mod usage for recommendations
     */
    public List<String> getNationsModUsage(String nationId) {
        List<String> modUsage = new ArrayList<>();
        // In a real implementation this would analyze the nation's actual mod usage
        // For now return empty list
        return modUsage;
    }
    
    /**
     * Apply economic balancing to all nations
     */
    public void applyEconomicBalanceToNations() {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            applyEconomicBalancing(nation.getId());
        }
    }
}