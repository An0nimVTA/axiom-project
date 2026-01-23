package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced balancing service to manage mod disbalance issues
 * Specifically addresses overpowered combinations and creates balance
 */
public class EnhancedModBalanceService {
    private final AXIOM plugin;
    private final BalancingService balancingService;
    private final RecipeIntegrationService recipeIntegrationService;
    private final ModIntegrationEnhancementService modIntegrationEnhancementService;
    private final ModPackManagerService modPackManagerService;
    
    // Mod power levels and balance data
    private final Map<String, ModPowerLevel> modPowerLevels = new ConcurrentHashMap<>();
    private final Map<String, ModUsageTracking> nationModUsage = new ConcurrentHashMap<>();
    private final Map<String, WarfareBalance> warfareBalances = new ConcurrentHashMap<>();
    private final BalanceMonitor balanceMonitor = new BalanceMonitor();
    
    public static class ModPowerLevel {
        String modId;
        String modName;
        double powerLevel; // Base power level (1.0 = normal)
        double damageMultiplier; // For weapons
        double durabilityMultiplier; // For equipment
        double resourceMultiplier; // For resource generation
        double speedMultiplier; // For automation
        int tier; // 1-5 scale (5 = most powerful)
        Set<String> conflictingMods; // Mods that don't work well together
        Set<String> synergisticMods; // Mods that work well together
        String category; // "warfare", "industrial", "logistics", etc.
        
        public ModPowerLevel(String modId, String modName, double powerLevel) {
            this.modId = modId;
            this.modName = modName;
            this.powerLevel = powerLevel;
            this.damageMultiplier = 1.0;
            this.durabilityMultiplier = 1.0;
            this.resourceMultiplier = 1.0;
            this.speedMultiplier = 1.0;
            this.tier = 3; // Default middle tier
            this.conflictingMods = new HashSet<>();
            this.synergisticMods = new HashSet<>();
            this.category = "general";
        }
    }
    
    public static class ModUsageTracking {
        String nationId;
        Map<String, Integer> modUsageCount = new HashMap<>(); // modId -> usage frequency
        double accumulatedPower = 0.0;
        int activeModsCount = 0;
        long lastUpdated = System.currentTimeMillis();
        
        public ModUsageTracking(String nationId) {
            this.nationId = nationId;
        }
        
        public void updateUsage(String modId, int usageCount) {
            modUsageCount.put(modId, usageCount);
            accumulatedPower = calculateAccumulatedPower();
            activeModsCount = modUsageCount.size();
            lastUpdated = System.currentTimeMillis();
        }
        
        private double calculateAccumulatedPower() {
            double totalPower = 0.0;
            for (Map.Entry<String, Integer> entry : modUsageCount.entrySet()) {
                String modId = entry.getKey();
                Integer usage = entry.getValue();
                
                ModPowerLevel powerLevel = modPowerLevels.get(modId);
                double modPower = powerLevel != null ? powerLevel.powerLevel : 1.0;
                
                // Apply usage scaling (logarithmic to prevent extreme scaling)
                double scaledUsage = Math.log(usage + 1) + 1;
                totalPower += modPower * scaledUsage;
            }
            return totalPower;
        }
    }
    
    public static class WarfareBalance {
        String nationId;
        double offensivePower; // Combined offensive capabilities
        double defensivePower; // Combined defensive capabilities
        double totalMilitaryScore; // Overall military strength
        double balanceRatio; // Offensive/Defensive ratio
        long lastCalculated = System.currentTimeMillis();
        
        public WarfareBalance(String nationId) {
            this.nationId = nationId;
            this.offensivePower = 1.0;
            this.defensivePower = 1.0;
            this.totalMilitaryScore = 2.0;
            this.balanceRatio = 1.0;
        }
    }
    
    public static class BalanceMonitor {
        int totalNationsTracked = 0;
        double averagePowerLevel = 1.0;
        double powerDeviation = 0.0;
        int overpoweringNations = 0;
        int underwhelmingNations = 0;
        long lastUpdate = System.currentTimeMillis();
        
        public void updateStats(Map<String, ModUsageTracking> usageTracking) {
            totalNationsTracked = usageTracking.size();
            
            if (totalNationsTracked > 0) {
                // Calculate average power
                double totalPower = 0.0;
                for (ModUsageTracking tracking : usageTracking.values()) {
                    totalPower += tracking.accumulatedPower;
                }
                averagePowerLevel = totalPower / totalNationsTracked;
                
                // Calculate deviation
                double variance = 0.0;
                for (ModUsageTracking tracking : usageTracking.values()) {
                    variance += Math.pow(tracking.accumulatedPower - averagePowerLevel, 2);
                }
                variance /= totalNationsTracked;
                powerDeviation = Math.sqrt(variance);
            }
        }
    }

    public EnhancedModBalanceService(AXIOM plugin) {
        this.plugin = plugin;
        this.balancingService = plugin.getBalancingService();
        this.recipeIntegrationService = plugin.getRecipeIntegrationService();
        this.modIntegrationEnhancementService = plugin.getModIntegrationEnhancementService();
        this.modPackManagerService = plugin.getModPackManagerService();
        
        initializeModPowerLevels();
        startBalanceMonitoring();
    }
    
    /**
     * Initialize power levels for each major mod
     */
    private void initializeModPowerLevels() {
        // Initialize all mod power levels based on our 5-stage philosophy
        initializeStage1Mods();    // Stone Age - Basic survival
        initializeStage2Mods();    // Industrial Revolution - Energy and automation
        initializeStage3Mods();    // Era of Accumulation - Storage and acceleration
        initializeStage4Mods();    // High Tech - Optimization and integration
        initializeStage5Mods();    // Military-Industrial Complex - Power and dominance
    }
    
    private void initializeStage1Mods() {
        // Stone Age stage mods (Tier 1)
        ModPowerLevel vanillaMod = new ModPowerLevel("minecraft", "Vanilla Minecraft", 1.0);
        vanillaMod.category = "foundation";
        modPowerLevels.put("minecraft", vanillaMod);
    }
    
    private void initializeStage2Mods() {
        // Industrial Revolution stage mods (Tier 2)
        
        // Industrial Upgrade - Main industrial mod
        ModPowerLevel industrialUpgradeMod = new ModPowerLevel("industrialupgrade", "Industrial Upgrade", 1.1);
        industrialUpgradeMod.resourceMultiplier = 1.2;
        industrialUpgradeMod.speedMultiplier = 1.1;
        industrialUpgradeMod.tier = 2;
        industrialUpgradeMod.category = "industrial";
        modPowerLevels.put("industrialupgrade", industrialUpgradeMod);
        
        // Immersive Engineering - Industrial backbone
        ModPowerLevel immersiveEngineeringMod = new ModPowerLevel("immersiveengineering", "Immersive Engineering", 1.0);
        immersiveEngineeringMod.resourceMultiplier = 1.1;
        immersiveEngineeringMod.speedMultiplier = 1.05;
        immersiveEngineeringMod.tier = 2;
        immersiveEngineeringMod.category = "industrial";
        immersiveEngineeringMod.synergisticMods.add("industrialupgrade");
        modPowerLevels.put("immersiveengineering", immersiveEngineeringMod);
    }
    
    private void initializeStage3Mods() {
        // Era of Accumulation stage mods (Tier 3)
        
        // Applied Energistics 2 - Storage and logistics
        ModPowerLevel ae2Mod = new ModPowerLevel("appliedenergistics2", "Applied Energistics 2", 1.2);
        ae2Mod.speedMultiplier = 1.3;
        ae2Mod.resourceMultiplier = 1.25;
        ae2Mod.tier = 3;
        ae2Mod.category = "logistics";
        ae2Mod.synergisticMods.add("immersiveengineering");
        modPowerLevels.put("appliedenergistics2", ae2Mod);
        
        // Simply Quarries - Automated extraction
        ModPowerLevel simplyQuarriesMod = new ModPowerLevel("simplyquarries", "Simply Quarries", 1.1);
        simplyQuarriesMod.resourceMultiplier = 1.8;
        simplyQuarriesMod.speedMultiplier = 1.2;
        simplyQuarriesMod.tier = 3;
        simplyQuarriesMod.category = "industrial";
        modPowerLevels.put("simplyquarries", simplyQuarriesMod);
    }
    
    private void initializeStage4Mods() {
        // High Tech stage mods (Tier 4)
        
        // TACZ - Modern firearms and warfare
        ModPowerLevel taczMod = new ModPowerLevel("tacz", "TACZ - Modern Firearms", 1.3);
        taczMod.damageMultiplier = 1.4;
        taczMod.tier = 4;
        taczMod.category = "warfare";
        modPowerLevels.put("tacz", taczMod);
        
        // PointBlank - Alternative firearms
        ModPowerLevel pointBlankMod = new ModPowerLevel("pointblank", "PointBlank", 1.3);
        pointBlankMod.damageMultiplier = 1.4;
        pointBlankMod.tier = 4;
        pointBlankMod.category = "warfare";
        pointBlankMod.conflictingMods.add("tacz");
        modPowerLevels.put("pointblank", pointBlankMod);
        
        // Quantum Generators - High-power energy
        ModPowerLevel quantumGenMod = new ModPowerLevel("quantumgenerators", "Quantum Generators", 1.4);
        quantumGenMod.speedMultiplier = 1.6;
        quantumGenMod.resourceMultiplier = 1.2;
        quantumGenMod.tier = 4;
        quantumGenMod.category = "energy";
        modPowerLevels.put("quantumgenerators", quantumGenMod);
        
        // Superb Warfare - Military vehicles
        ModPowerLevel superbWarfareMod = new ModPowerLevel("superbwarfare", "Superb Warfare", 1.35);
        superbWarfareMod.damageMultiplier = 1.5;
        superbWarfareMod.tier = 4;
        superbWarfareMod.category = "warfare";
        modPowerLevels.put("superbwarfare", superbWarfareMod);
    }
    
    private void initializeStage5Mods() {
        // Military-Industrial Complex stage mods (Tier 5)
        
        // Ballistix - Heavy weapons and explosives
        ModPowerLevel ballistixMod = new ModPowerLevel("ballistix", "Ballistix", 1.6);
        ballistixMod.damageMultiplier = 2.0;
        ballistixMod.tier = 5;
        ballistixMod.category = "warfare";
        ballistixMod.conflictingMods.addAll(Arrays.asList("superbwarfare", "warium"));
        modPowerLevels.put("ballistix", ballistixMod);
        
        // Warium - Advanced military technology
        ModPowerLevel wariumMod = new ModPowerLevel("warium", "Warium", 1.55);
        wariumMod.damageMultiplier = 1.9;
        wariumMod.tier = 5;
        wariumMod.category = "warfare";
        wariumMod.conflictingMods.addAll(Arrays.asList("ballistix", "superbwarfare"));
        modPowerLevels.put("warium", wariumMod);
        
        // Immersive Vehicles - Military transport
        ModPowerLevel immersiveVehiclesMod = new ModPowerLevel("immersivevehicles", "Immersive Vehicles", 1.4);
        immersiveVehiclesMod.speedMultiplier = 1.3;
        immersiveVehiclesMod.damageMultiplier = 1.2;
        immersiveVehiclesMod.tier = 4;
        immersiveVehiclesMod.category = "transport";
        immersiveVehiclesMod.synergisticMods.add("superbwarfare");
        modPowerLevels.put("immersivevehicles", immersiveVehiclesMod);
        
        // MTS - Vehicle pack for Immersive Vehicles
        ModPowerLevel mtsMod = new ModPowerLevel("mts", "MTS Official Pack", 1.1);
        mtsMod.speedMultiplier = 1.15;
        mtsMod.tier = 3;
        mtsMod.category = "transport";
        modPowerLevels.put("mts", mtsMod);
        
        // AshVehicle - Civilian transportation
        ModPowerLevel ashVehicleMod = new ModPowerLevel("ashvehicle", "AshVehicle", 1.0);
        ashVehicleMod.speedMultiplier = 1.1;
        ashVehicleMod.tier = 2;
        ashVehicleMod.category = "transport";
        modPowerLevels.put("ashvehicle", ashVehicleMod);
        
        // MCSP - Military Combat Simulation Pack
        ModPowerLevel mcspMod = new ModPowerLevel("mcsp", "MCSP", 1.3);
        mcspMod.damageMultiplier = 1.35;
        mcspMod.tier = 4;
        mcspMod.category = "warfare";
        modPowerLevels.put("mcsp", mcspMod);
        
        // CAPS Awims - Tactical equipment
        ModPowerLevel capsAwimsMod = new ModPowerLevel("capsawims", "CAPS Awims", 1.25);
        capsAwimsMod.durabilityMultiplier = 1.4;
        capsAwimsMod.tier = 4;
        capsAwimsMod.category = "warfare";
        capsAwimsMod.synergisticMods.add("tacz");
        capsAwimsMod.synergisticMods.add("pointblank");
        modPowerLevels.put("capsawims", capsAwimsMod);
        
        // Optimizations and utilities
        ModPowerLevel embeddiumMod = new ModPowerLevel("embeddium", "Embeddium", 0.1);
        embeddiumMod.speedMultiplier = 1.05;
        embeddiumMod.tier = 1;
        embeddiumMod.category = "optimization";
        modPowerLevels.put("embeddium", embeddiumMod);
        
        ModPowerLevel entityCullingMod = new ModPowerLevel("entityculling", "Entity Culling", 0.1);
        entityCullingMod.speedMultiplier = 1.02;
        entityCullingMod.tier = 1;
        entityCullingMod.category = "optimization";
        modPowerLevels.put("entityculling", entityCullingMod);
    }
    
    /**
     * Start periodic balance monitoring
     */
    private void startBalanceMonitoring() {
        // Run every 10 minutes to update balance statistics
        new BukkitRunnable() {
            @Override
            public void run() {
                updateBalances();
                checkForImbalances();
            }
        }.runTaskTimer(plugin, 20 * 60 * 10, 20 * 60 * 10); // Every 10 minutes
    }
    
    /**
     * Update all balance calculations
     */
    private void updateBalances() {
        // Update nation mod usage
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            updateNationModUsage(nation.getId());
            updateWarfareBalance(nation.getId());
        }
        
        // Update global balance statistics
        balanceMonitor.updateStats(nationModUsage);
    }
    
    /**
     * Update mod usage for a nation
     */
    public void updateNationModUsage(String nationId) {
        ModUsageTracking tracking = nationModUsage.computeIfAbsent(nationId, ModUsageTracking::new);
        // In a real implementation, this would analyze the nation's structures, equipment, etc.
        // For now, just update timestamp
        tracking.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Calculate warfare balance for a nation
     */
    public void updateWarfareBalance(String nationId) {
        ModUsageTracking tracking = nationModUsage.get(nationId);
        if (tracking == null) {
            return;
        }
        
        double offensive = 1.0;
        double defensive = 1.0;
        
        for (Map.Entry<String, Integer> entry : tracking.modUsageCount.entrySet()) {
            String modId = entry.getKey();
            int usage = entry.getValue();
            
            ModPowerLevel powerLevel = modPowerLevels.get(modId);
            if (powerLevel != null) {
                // Determine if mod is offensive or defensive
                boolean isOffensive = isOffensiveMod(modId);
                boolean isDefensive = isDefensiveMod(modId);
                
                // Calculate usage factor
                double usageFactor = Math.min(usage / 10.0, 2.0); // Cap at 2x for heavy usage
                
                if (isOffensive) {
                    offensive *= (1.0 + (powerLevel.damageMultiplier - 1.0) * usageFactor);
                } else if (isDefensive) {
                    defensive *= (1.0 + (powerLevel.durabilityMultiplier - 1.0) * usageFactor);
                }
            }
        }
        
        WarfareBalance balance = warfareBalances.computeIfAbsent(nationId, WarfareBalance::new);
        balance.offensivePower = offensive;
        balance.defensivePower = defensive;
        balance.totalMilitaryScore = offensive + defensive;
        balance.balanceRatio = offensive / Math.max(defensive, 0.001); // Avoid division by zero
        balance.lastCalculated = System.currentTimeMillis();
        
        // Check for imbalances
        if (Math.abs(balance.balanceRatio - 1.0) > 1.0) {
            // Log imbalance for potential intervention
            plugin.getLogger().info("Warfare imbalance detected for nation " + nationId + 
                ": Off/Def ratio = " + String.format("%.2f", balance.balanceRatio));
        }
    }
    
    /**
     * Check if a mod is primarily offensive
     */
    private boolean isOffensiveMod(String modId) {
        return modId.contains("tacz") || modId.contains("pointblank") || 
               modId.contains("ballistix") || modId.contains("warium") || 
               modId.contains("superbwarfare") || modId.contains("mcsp");
    }
    
    /**
     * Check if a mod is primarily defensive
     */
    private boolean isDefensiveMod(String modId) {
        return modId.contains("armor") || modId.contains("shield") || 
               modId.contains("fortification") || modId.contains("defense");
    }
    
    /**
     * Check for balance imbalances
     */
    private void checkForImbalances() {
        balanceMonitor.overpoweringNations = 0;
        balanceMonitor.underwhelmingNations = 0;
        
        double threshold = balanceMonitor.averagePowerLevel + (balanceMonitor.powerDeviation * 1.5);
        double lowThreshold = balanceMonitor.averagePowerLevel - (balanceMonitor.powerDeviation * 1.5);
        
        for (Map.Entry<String, ModUsageTracking> entry : nationModUsage.entrySet()) {
            String nationId = entry.getKey();
            ModUsageTracking tracking = entry.getValue();
            
            if (tracking.accumulatedPower > threshold) {
                balanceMonitor.overpoweringNations++;
                
                // Apply balancing measures for overpowering nations
                applyBalancingMeasures(nationId, tracking.accumulatedPower);
            } else if (tracking.accumulatedPower < lowThreshold) {
                balanceMonitor.underwhelmingNations++;
            }
        }
    }
    
    /**
     * Apply balancing measures to an overpowered nation
     */
    private void applyBalancingMeasures(String nationId, double actualPower) {
        // Log the situation
        plugin.getLogger().info("Applying balancing measures for overpowered nation " + nationId + 
            " (power: " + String.format("%.2f", actualPower) + ", avg: " + 
            String.format("%.2f", balanceMonitor.averagePowerLevel) + ")");
        
        // Notify nation leaders about the imbalance
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation != null) {
            String message = "§c[СИСТЕМА БАЛАНСА] §fВаша нация имеет §cПРЕВЫШЕННУЮ§f силу. " +
                "Рассмотрите §eдипломатические меры§f для восстановления равновесия.";
            
            for (UUID citizenId : nation.getCitizens()) {
                Player player = Bukkit.getPlayer(citizenId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
    }
    
    /**
     * Get mod power level information
     */
    public ModPowerLevel getModPowerLevel(String modId) {
        return modPowerLevels.get(modId);
    }
    
    /**
     * Get all mod power levels
     */
    public Collection<ModPowerLevel> getAllModPowerLevels() {
        return new ArrayList<>(modPowerLevels.values());
    }
    
    /**
     * Check if two mods conflict with each other
     */
    public boolean doModsConflict(String modId1, String modId2) {
        ModPowerLevel level1 = modPowerLevels.get(modId1);
        ModPowerLevel level2 = modPowerLevels.get(modId2);
        
        if (level1 != null && level2 != null) {
            return level1.conflictingMods.contains(modId2) || 
                   level2.conflictingMods.contains(modId1);
        }
        
        return false;
    }
    
    /**
     * Check if two mods synergize
     */
    public boolean doModsSynergize(String modId1, String modId2) {
        ModPowerLevel level1 = modPowerLevels.get(modId1);
        ModPowerLevel level2 = modPowerLevels.get(modId2);
        
        if (level1 != null && level2 != null) {
            return level1.synergisticMods.contains(modId2) || 
                   level2.synergisticMods.contains(modId1);
        }
        
        return false;
    }
    
    /**
     * Get recommended mods for a given mod
     */
    public List<String> getRecommendedMods(String modId) {
        ModPowerLevel level = modPowerLevels.get(modId);
        if (level != null) {
            return new ArrayList<>(level.synergisticMods);
        }
        return new ArrayList<>();
    }
    
    /**
     * Get conflicting mods for a given mod
     */
    public List<String> getConflictingMods(String modId) {
        ModPowerLevel level = modPowerLevels.get(modId);
        if (level != null) {
            return new ArrayList<>(level.conflictingMods);
        }
        return new ArrayList<>();
    }
    
    /**
     * Get nation-specific mod usage statistics
     */
    public Map<String, Object> getNationModUsageStats(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        ModUsageTracking tracking = nationModUsage.get(nationId);
        
        if (tracking != null) {
            stats.put("accumulatedPower", tracking.accumulatedPower);
            stats.put("activeModsCount", tracking.activeModsCount);
            stats.put("modUsage", new HashMap<>(tracking.modUsageCount));
            stats.put("lastUpdated", tracking.lastUpdated);
            
            // Include warfare balance if calculated
            WarfareBalance warfare = warfareBalances.get(nationId);
            if (warfare != null) {
                Map<String, Object> warfareStats = new HashMap<>();
                warfareStats.put("offensivePower", warfare.offensivePower);
                warfareStats.put("defensivePower", warfare.defensivePower);
                warfareStats.put("totalMilitaryScore", warfare.totalMilitaryScore);
                warfareStats.put("balanceRatio", warfare.balanceRatio);
                warfareStats.put("lastCalculated", warfare.lastCalculated);
                stats.put("warfareBalance", warfareStats);
            }
        } else {
            stats.put("accumulatedPower", 1.0);
            stats.put("activeModsCount", 0);
            stats.put("modUsage", new HashMap<>());
            stats.put("lastUpdated", 0L);
            stats.put("warfareBalance", null);
        }
        
        return stats;
    }
    
    /**
     * Get global balance statistics
     */
    public Map<String, Object> getGlobalBalanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNationsTracked", balanceMonitor.totalNationsTracked);
        stats.put("averagePowerLevel", balanceMonitor.averagePowerLevel);
        stats.put("powerDeviation", balanceMonitor.powerDeviation);
        stats.put("overpoweringNations", balanceMonitor.overpoweringNations);
        stats.put("underwhelmingNations", balanceMonitor.underwhelmingNations);
        stats.put("lastUpdate", balanceMonitor.lastUpdate);
        
        // Additional stats
        stats.put("totalActiveMods", modPowerLevels.size());
        stats.put("powerfulModIds", getPowerfulModIds());
        
        return stats;
    }
    
    /**
     * Get IDs of powerful mods
     */
    private List<String> getPowerfulModIds() {
        List<String> powerful = new ArrayList<>();
        for (Map.Entry<String, ModPowerLevel> entry : modPowerLevels.entrySet()) {
            if (entry.getValue().powerLevel > 1.2) {
                powerful.add(entry.getKey());
            }
        }
        return powerful;
    }
    
    /**
     * Calculate a balanced power score for a nation
     * Adjusts raw mod usage based on balance considerations
     */
    public double calculateBalancedPowerScore(String nationId) {
        ModUsageTracking tracking = nationModUsage.get(nationId);
        if (tracking == null) {
            return 1.0; // Neutral baseline
        }
        
        double rawPower = tracking.accumulatedPower;
        double balanceAdjustment = 1.0;
        
        // Apply penalties for using conflicting mods
        List<String> usedMods = new ArrayList<>(tracking.modUsageCount.keySet());
        for (int i = 0; i < usedMods.size(); i++) {
            for (int j = i + 1; j < usedMods.size(); j++) {
                if (doModsConflict(usedMods.get(i), usedMods.get(j))) {
                    balanceAdjustment *= 0.85; // 15% penalty for each conflict
                }
            }
        }
        
        // Apply bonuses for synergistic mods
        for (int i = 0; i < usedMods.size(); i++) {
            for (int j = i + 1; j < usedMods.size(); j++) {
                if (doModsSynergize(usedMods.get(i), usedMods.get(j))) {
                    balanceAdjustment *= 1.05; // 5% bonus for each synergy
                }
            }
        }
        
        // Apply soft cap to prevent exponential growth
        double softCappedPower = rawPower;
        if (rawPower > balanceMonitor.averagePowerLevel * 2) {
            softCappedPower = balanceMonitor.averagePowerLevel * 2 + 
                             (rawPower - balanceMonitor.averagePowerLevel * 2) * 0.5;
        }
        
        return softCappedPower * balanceAdjustment;
    }
    
    /**
     * Check if a nation is compliant with balance guidelines
     */
    public boolean isNationBalanceCompliant(String nationId) {
        double balancedScore = calculateBalancedPowerScore(nationId);
        double threshold = balanceMonitor.averagePowerLevel * 2.5; // 2.5x tolerance
        
        return balancedScore <= threshold;
    }
    
    /**
     * Get recommendations for balancing a nation's mod usage
     */
    public List<String> getBalanceRecommendations(String nationId) {
        List<String> recommendations = new ArrayList<>();
        ModUsageTracking tracking = nationModUsage.get(nationId);
        
        if (tracking == null) {
            return recommendations;
        }
        
        List<String> usedMods = new ArrayList<>(tracking.modUsageCount.keySet());
        
        // Check for conflicting mod combinations
        for (int i = 0; i < usedMods.size(); i++) {
            for (int j = i + 1; j < usedMods.size(); j++) {
                if (doModsConflict(usedMods.get(i), usedMods.get(j))) {
                    recommendations.add("Конфликт модов: " + usedMods.get(i) + " и " + usedMods.get(j) + 
                                      " - рассмотрите удаление одного из них");
                }
            }
        }
        
        // Suggest synergistic mods
        Set<String> existingMods = new HashSet<>(usedMods);
        Set<String> suggestedMods = new HashSet<>();
        
        for (String modId : usedMods) {
            List<String> synergistic = getRecommendedMods(modId);
            for (String synMod : synergistic) {
                if (!existingMods.contains(synMod) && !suggestedMods.contains(synMod)) {
                    recommendations.add("Дополните " + modId + " с " + synMod + " для синергии");
                    suggestedMods.add(synMod);
                }
            }
        }
        
        return recommendations;
    }
}