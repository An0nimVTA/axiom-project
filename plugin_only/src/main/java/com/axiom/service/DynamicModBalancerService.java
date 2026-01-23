package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dynamic Mod Balancing Service
 * Adapts mod difficulty and availability based on player activity and usage patterns
 */
public class DynamicModBalancerService {
    private final AXIOM plugin;
    private final ModIntegrationEnhancementService modIntegrationEnhancementService;
    private final RecipeIntegrationService recipeIntegrationService;
    private final EnhancedModBalanceService enhancedModBalanceService;
    private final ModdedEconomicBalanceService moddedEconomicBalanceService;
    
    // Activity tracking
    private final Map<UUID, PlayerActivityRecord> playerActivityRecords = new ConcurrentHashMap<>();
    private final Map<String, NationActivityRecord> nationActivityRecords = new ConcurrentHashMap<>();
    
    // Mod usage statistics
    private final Map<String, ModUsageStats> modUsageStats = new ConcurrentHashMap<>();
    
    // Dynamic balance parameters
    private final Map<String, ModBalanceParameters> modBalanceParameters = new ConcurrentHashMap<>();
    
    // Config file
    private FileConfiguration dynamicBalancingConfig;
    private File configFile;
    
    public static class PlayerActivityRecord {
        UUID playerId;
        long lastActive = System.currentTimeMillis();
        int totalPlayTimeMinutes = 0;
        Map<String, Integer> modInteractionCount = new HashMap<>(); // modId -> interaction count
        Map<String, Integer> modTimeSpentMinutes = new HashMap<>(); // modId -> minutes spent
        String currentNationId = null;
        double averageModEngagement = 0.0; // 0-100 scale
        
        public PlayerActivityRecord(UUID playerId) {
            this.playerId = playerId;
        }
    }
    
    public static class NationActivityRecord {
        String nationId;
        long lastActive = System.currentTimeMillis();
        int totalMembers = 0;
        Map<String, Integer> collectiveModTimeMinutes = new HashMap<>(); // modId -> total time
        Map<String, Integer> collectiveModInteractions = new HashMap<>(); // modId -> total interactions
        double averageModEngagement = 0.0; // Average across all members
        int peakSimultaneousPlayers = 0; // Peak concurrent active players
        double militaryEngagement = 0.0; // Military mod usage percentage
        double industrialEngagement = 0.0; // Industrial mod usage percentage
        double techEngagement = 0.0; // Tech mod usage percentage
        
        public NationActivityRecord(String nationId) {
            this.nationId = nationId;
        }
    }
    
    public static class ModUsageStats {
        String modId;
        long lastUsed = System.currentTimeMillis();
        int totalUses = 0;
        int activeUsers = 0; // Number of players actively using this mod
        double averageUsagePerPlayer = 0.0; // Average usage per player
        double popularityScore = 0.0; // Based on usage vs other mods (0-100)
        int trendingDirection = 0; // -1 decreasing, 0 neutral, 1 increasing
        double usageGrowthRate = 0.0; // Rate of change in usage
        int dailyActiveUsers = 0; // Users who used today
        
        public ModUsageStats(String modId) {
            this.modId = modId;
        }
    }
    
    public static class ModBalanceParameters {
        String modId;
        double difficultyMultiplier = 1.0; // 1.0 = normal, >1.0 = harder, <1.0 = easier
        double resourceAbundance = 1.0; // 1.0 = normal, >1.0 = more abundant, <1.0 = scarcer
        double craftingTimeMultiplier = 1.0; // Time multiplier for crafting
        double energyMultiplier = 1.0; // Energy cost multiplier
        double combatEffectiveness = 1.0; // Effectiveness of mod weapons/armor
        long lastAdjustment = System.currentTimeMillis();
        String adjustmentReason = "Initial";
        
        public ModBalanceParameters(String modId) {
            this.modId = modId;
        }
    }

    public DynamicModBalancerService(AXIOM plugin) {
        this.plugin = plugin;
        this.modIntegrationEnhancementService = plugin.getModIntegrationEnhancementService();
        this.recipeIntegrationService = plugin.getRecipeIntegrationService();
        this.enhancedModBalanceService = plugin.getEnhancedModBalanceService();
        this.moddedEconomicBalanceService = plugin.getModdedEconomicBalanceService();
        
        initializeConfig();
        initializeDefaultBalanceParameters();
        startDynamicBalancingTasks();
    }
    
    private void initializeConfig() {
        configFile = new File(plugin.getDataFolder(), "dynamic-balancing.yml");
        if (!configFile.exists()) {
            // Create default config
            createDefaultConfig();
        }
        dynamicBalancingConfig = YamlConfiguration.loadConfiguration(configFile);
    }
    
    private void createDefaultConfig() {
        // Dynamic balancing settings
        dynamicBalancingConfig.set("enabled", true);
        dynamicBalancingConfig.set("balancingIntervalMinutes", 30);
        dynamicBalancingConfig.set("activityTrackingEnabled", true);
        dynamicBalancingConfig.set("activityUpdateIntervalMinutes", 5);
        
        // Threshold settings
        dynamicBalancingConfig.set("popularityThresholds.low", 20.0); // Below 20% usage = low popularity
        dynamicBalancingConfig.set("popularityThresholds.medium", 60.0); // 20-60% = medium
        dynamicBalancingConfig.set("popularityThresholds.high", 100.0); // Above 60% = high
        
        // Adjustment sensitivity
        dynamicBalancingConfig.set("adjustmentSensitivity.difficulty", 0.1); // 10% max adjustment per cycle
        dynamicBalancingConfig.set("adjustmentSensitivity.resources", 0.15); // 15% max adjustment per cycle
        dynamicBalancingConfig.set("adjustmentSensitivity.crafting", 0.2); // 20% max adjustment per cycle
        
        // Thresholds for action
        dynamicBalancingConfig.set("actionThresholds.popularityDrop", -15.0); // If usage drops 15%, take action
        dynamicBalancingConfig.set("actionThresholds.popularitySurge", 25.0); // If usage surges 25%, take action
        dynamicBalancingConfig.set("actionThresholds.engagementLow", 10.0); // Low engagement action threshold
        
        try {
            dynamicBalancingConfig.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not create default dynamic balancing config: " + e.getMessage());
        }
    }
    
    /**
     * Initialize default balance parameters for known mods
     */
    private void initializeDefaultBalanceParameters() {
        // Initialize parameters for each major mod
        String[] knownMods = {
            "tacz", "pointblank", "ballistix", "superbwarfare", "warium", "capsawims",
            "immersiveengineering", "industrialupgrade", "appliedenergistics2", 
            "mekanism", "thermal", "simplyquarries", "quantumgenerators", 
            "immersivevehicles", "ashvehicle", "mcsp", "tacz", "voltaic", "embeddium"
        };
        
        for (String modId : knownMods) {
            ModBalanceParameters params = new ModBalanceParameters(modId);
            
            // Set default parameters based on mod type
            if (Arrays.asList("tacz", "pointblank", "ballistix", "superbwarfare").contains(modId)) {
                // Warfare mods - higher combat effectiveness but potentially higher costs
                params.combatEffectiveness = 1.0; // Normal effectiveness
                params.energyMultiplier = 1.1; // Slightly higher energy cost
                params.resourceAbundance = 0.9; // Slightly scarcer resources
            } else if (Arrays.asList("immersiveengineering", "industrialupgrade", "mekanism").contains(modId)) {
                // Industrial mods - balanced parameters
                params.difficultyMultiplier = 1.0;
                params.resourceAbundance = 1.0;
                params.craftingTimeMultiplier = 1.0;
                params.energyMultiplier = 1.0;
                params.combatEffectiveness = 1.0;
            } else if (Arrays.asList("appliedenergistics2", "thermal").contains(modId)) {
                // Logistics/processing mods - focus on resource efficiency
                params.difficultyMultiplier = 1.0;
                params.resourceAbundance = 1.1; // Slightly more abundant
                params.craftingTimeMultiplier = 0.9; // Slightly faster crafting
                params.energyMultiplier = 1.05;
            } else if (Arrays.asList("simplyquarries", "quantumgenerators").contains(modId)) {
                // Heavy automation/power mods - higher costs but higher rewards
                params.difficultyMultiplier = 1.2; // More difficult
                params.resourceAbundance = 1.3; // More abundant when operational
                params.energyMultiplier = 0.9; // More efficient when running
            }
            
            modBalanceParameters.put(modId, params);
        }
    }
    
    /**
     * Start dynamic balancing tasks
     */
    private void startDynamicBalancingTasks() {
        // Update player activity every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerActivities();
            }
        }.runTaskTimerAsynchronously(plugin, 20 * 60 * 5, 20 * 60 * 5); // Every 5 minutes
        
        // Update nation activities every 10 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                updateNationActivities();
            }
        }.runTaskTimerAsynchronously(plugin, 20 * 60 * 10, 20 * 60 * 10); // Every 10 minutes
        
        // Perform dynamic balancing every 30 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                performDynamicBalancing();
            }
        }.runTaskTimer(plugin, 20 * 60 * 30, 20 * 60 * 30); // Every 30 minutes
    }
    
    /**
     * Update player activity records
     */
    private void updatePlayerActivities() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            PlayerActivityRecord record = playerActivityRecords.computeIfAbsent(playerId, PlayerActivityRecord::new);
            
            // Update play time
            record.totalPlayTimeMinutes++;
            
            // Update last active
            record.lastActive = System.currentTimeMillis();
            
            // Check what mods player is interacting with
            updatePlayerModInteractions(player, record);
            
            // Calculate engagement score
            record.averageModEngagement = calculatePlayerEngagementScore(record);
        }
    }
    
    /**
     * Update mod interactions for a player
     */
    private void updatePlayerModInteractions(Player player, PlayerActivityRecord record) {
        // This would hook into various gameplay events to track mod usage
        // For now, we'll use a simplified approach
        
        // Example: Check if player is near mod blocks
        // This would require listening to block interaction events, inventory events, etc.
        // For demonstration purposes, we'll just increment some example interactions
        String[] exampleMods = {"tacz", "immersiveengineering", "industrialupgrade"};
        
        for (String mod : exampleMods) {
            if (Math.random() > 0.7) { // Simulate random mod interaction
                record.modInteractionCount.merge(mod, 1, Integer::sum);
                record.modTimeSpentMinutes.merge(mod, 1, Integer::sum);
            }
        }
    }
    
    /**
     * Calculate player engagement score based on mod usage
     */
    private double calculatePlayerEngagementScore(PlayerActivityRecord record) {
        int totalInteractions = record.modInteractionCount.values().stream().mapToInt(Integer::intValue).sum();
        if (totalInteractions == 0) return 0.0;
        
        // Calculate weighted engagement (more recent interactions worth more)
        double totalWeightedInteractions = 0.0;
        for (int interactions : record.modInteractionCount.values()) {
            totalWeightedInteractions += Math.sqrt(interactions); // Square root to prevent extreme values
        }
        
        return Math.min(100.0, (totalWeightedInteractions / Math.max(1, totalInteractions)) * 50);
    }
    
    /**
     * Update nation activity records
     */
    private void updateNationActivities() {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            String nationId = nation.getId();
            NationActivityRecord record = nationActivityRecords.computeIfAbsent(nationId, NationActivityRecord::new);
            
            // Update member count
            record.totalMembers = nation.getCitizens().size();
            
            // Calculate collective mod usage from member records
            double totalEngagement = 0.0;
            int activeMembers = 0;
            
            for (UUID citizenId : nation.getCitizens()) {
                PlayerActivityRecord playerRecord = playerActivityRecords.get(citizenId);
                if (playerRecord != null) {
                    totalEngagement += playerRecord.averageModEngagement;
                    activeMembers++;
                    
                    // Aggregate mod time from players
                    for (Map.Entry<String, Integer> entry : playerRecord.modTimeSpentMinutes.entrySet()) {
                        record.collectiveModTimeMinutes.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                    
                    // Aggregate mod interactions from players
                    for (Map.Entry<String, Integer> entry : playerRecord.modInteractionCount.entrySet()) {
                        record.collectiveModInteractions.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }
            }
            
            record.averageModEngagement = activeMembers > 0 ? totalEngagement / activeMembers : 0.0;
            
            // Calculate engagement percentages
            int totalModTime = record.collectiveModTimeMinutes.values().stream().mapToInt(Integer::intValue).sum();
            if (totalModTime > 0) {
                record.militaryEngagement = (record.collectiveModTimeMinutes.getOrDefault("tacz", 0) + 
                                           record.collectiveModTimeMinutes.getOrDefault("pointblank", 0) + 
                                           record.collectiveModTimeMinutes.getOrDefault("ballistix", 0)) / (double) totalModTime * 100;
                
                record.industrialEngagement = (record.collectiveModTimeMinutes.getOrDefault("immersiveengineering", 0) + 
                                             record.collectiveModTimeMinutes.getOrDefault("industrialupgrade", 0) + 
                                             record.collectiveModTimeMinutes.getOrDefault("mekanism", 0)) / (double) totalModTime * 100;
                
                record.techEngagement = (record.collectiveModTimeMinutes.getOrDefault("appliedenergistics2", 0) + 
                                       record.collectiveModTimeMinutes.getOrDefault("thermal", 0)) / (double) totalModTime * 100;
            }
        }
    }
    
    /**
     * Perform dynamic balancing based on activity and usage data
     */
    private void performDynamicBalancing() {
        if (!dynamicBalancingConfig.getBoolean("enabled", true)) {
            return;
        }
        
        plugin.getLogger().info("Performing dynamic mod balancing...");
        
        // Update mod usage statistics
        updateModUsageStatistics();
        
        // Analyze trends and adjust parameters
        analyzeAndAdjustModParameters();
        
        // Apply adjustments to game systems
        applyAdjustmentsToGameSystems();
        
        // Log results
        logBalancingResults();
    }
    
    /**
     * Update mod usage statistics
     */
    private void updateModUsageStatistics() {
        // Gather usage data from nation records
        for (String modId : getAllKnownMods()) {
            ModUsageStats stats = modUsageStats.computeIfAbsent(modId, ModUsageStats::new);
            
            int totalUsage = 0;
            int totalUsers = 0;
            int dailyUsers = 0;
            
            for (NationActivityRecord nationRecord : nationActivityRecords.values()) {
                int usage = nationRecord.collectiveModInteractions.getOrDefault(modId, 0);
                if (usage > 0) {
                    totalUsage += usage;
                    totalUsers++;
                    if (nationRecord.lastActive > System.currentTimeMillis() - 24 * 60 * 60_000L) { // 24 hours
                        dailyUsers++;
                    }
                }
            }
            
            stats.totalUses = totalUsage;
            stats.activeUsers = totalUsers;
            stats.dailyActiveUsers = dailyUsers;
            stats.averageUsagePerPlayer = totalUsers > 0 ? (double) totalUsage / totalUsers : 0;
            
            // Calculate popularity score (relative to other mods)
            int totalAllUses = modUsageStats.values().stream().mapToInt(s -> s.totalUses).sum();
            stats.popularityScore = totalAllUses > 0 ? (double) totalUsage / totalAllUses * 100 : 0;
            
            // Calculate trend over time (simplified)
            // This would require historical data to properly calculate trends
            stats.lastUsed = System.currentTimeMillis();
        }
    }
    
    /**
     * Analyze trends and adjust mod parameters
     */
    private void analyzeAndAdjustModParameters() {
        double popularityThresholdLow = dynamicBalancingConfig.getDouble("popularityThresholds.low", 20.0);
        double popularityThresholdHigh = dynamicBalancingConfig.getDouble("popularityThresholds.high", 100.0);
        
        double sensitivityDifficulty = dynamicBalancingConfig.getDouble("adjustmentSensitivity.difficulty", 0.1);
        double sensitivityResources = dynamicBalancingConfig.getDouble("adjustmentSensitivity.resources", 0.15);
        
        for (Map.Entry<String, ModUsageStats> entry : modUsageStats.entrySet()) {
            String modId = entry.getKey();
            ModUsageStats stats = entry.getValue();
            ModBalanceParameters params = modBalanceParameters.computeIfAbsent(modId, ModBalanceParameters::new);
            
            double previousPopularity = params.difficultyMultiplier; // Use as proxy for previous state
            params.difficultyMultiplier = calculateAdjustedParameter(
                params.difficultyMultiplier, 
                stats.popularityScore, 
                popularityThresholdLow, 
                popularityThresholdHigh,
                sensitivityDifficulty
            );
            
            params.resourceAbundance = calculateAdjustedParameter(
                params.resourceAbundance,
                stats.popularityScore,
                popularityThresholdLow,
                popularityThresholdHigh,
                sensitivityResources
            );
            
            params.lastAdjustment = System.currentTimeMillis();
            
            // Set reason for adjustment
            if (stats.popularityScore < popularityThresholdLow) {
                params.adjustmentReason = "Low popularity - adjusting for accessibility";
            } else if (stats.popularityScore > popularityThresholdHigh * 0.8) {
                params.adjustmentReason = "High popularity - adjusting for balance";
            } else {
                params.adjustmentReason = "Normal usage - maintaining balance";
            }
        }
    }
    
    /**
     * Calculate adjusted parameter based on popularity
     */
    private double calculateAdjustedParameter(double currentValue, double popularityScore, 
                                              double lowThreshold, double highThreshold, double sensitivity) {
        // For low popularity mods, make them easier/lighter to encourage usage
        if (popularityScore < lowThreshold) {
            return Math.max(0.5, currentValue - sensitivity * 0.5);
        }
        // For high popularity mods, make them slightly more challenging/heavier to balance
        else if (popularityScore > highThreshold * 0.8) {
            return Math.min(2.0, currentValue + sensitivity * 0.3);
        }
        // For normal popularity, keep relatively stable
        else {
            // Slowly drift back to 1.0 (normal) over time
            if (currentValue > 1.0) {
                return Math.max(1.0, currentValue - sensitivity * 0.1);
            } else if (currentValue < 1.0) {
                return Math.min(1.0, currentValue + sensitivity * 0.1);
            }
        }
        return currentValue;
    }
    
    /**
     * Apply parameter adjustments to game systems
     */
    private void applyAdjustmentsToGameSystems() {
        // Apply adjustments to recipe integration service
        for (Map.Entry<String, ModBalanceParameters> entry : modBalanceParameters.entrySet()) {
            String modId = entry.getKey();
            ModBalanceParameters params = entry.getValue();
            
            // Adjust recipe difficulty/costs based on parameters
            recipeIntegrationService.adjustModRecipeParameters(modId, params);
            
            // Adjust mod integration parameters
            modIntegrationEnhancementService.adjustModCompatibilityParameters(modId, params);
        }
        
        // Apply nation-specific adjustments based on their mod usage
        applyNationSpecificBalancing();
    }
    
    /**
     * Apply nation-specific balancing based on their mod usage patterns
     */
    private void applyNationSpecificBalancing() {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            String nationId = nation.getId();
            NationActivityRecord record = nationActivityRecords.get(nationId);
            
            if (record != null) {
                // Apply bonuses/punishments based on mod usage balance
                
                // If nation heavily uses military mods but little industry/tech, apply military fatigue
                if (record.militaryEngagement > 70 && record.industrialEngagement < 30) {
                    // Military effectiveness penalty for over-specialization
                    plugin.getMilitaryService().applyFatigueModifier(nationId, 0.9);
                    
                    // Send notification
                    notifyNation(nationId, ChatColor.RED + "⚠ МИЛИТАРНАЯ УСТАЛОСТЬ ДЕТЕКТИРОВАНА: " +
                        ChatColor.GRAY + "Снижена эффективность боевой техники из-за недостатка индустриальной базы");
                }
                // If nation heavily uses industry but little military, apply defense vulnerability
                else if (record.industrialEngagement > 70 && record.militaryEngagement < 20) {
                    // Defense penalty for industrial focus
                    plugin.getNationManager().applyModifier(nationId, "defense", 0.85);
                    
                    notifyNation(nationId, ChatColor.YELLOW + "⚠ УЯЗВИМОСТЬ ОБОНАРУЖЕНА: " +
                        ChatColor.GRAY + "Недостаточная военная мощь для охраны промышленных объектов");
                }
                // Balanced usage gets bonuses
                else if (Math.abs(record.militaryEngagement - record.industrialEngagement) < 20 && 
                         record.techEngagement > 30) {
                    // Balanced, tech-focused nation gets efficiency bonus
                    plugin.getNationManager().applyModifier(nationId, "all_efficiency", 1.05);
                    
                    notifyNation(nationId, ChatColor.GREEN + "✓ СТРАТЕГИЧЕСКИЙ БАЛАНС ДОСТИГНУТ: " +
                        ChatColor.GRAY + "Повышенная эффективность всех систем");
                }
            }
        }
    }
    
    /**
     * Get list of all known mods
     */
    private List<String> getAllKnownMods() {
        List<String> mods = new ArrayList<>();
        // Add known mod IDs
        mods.addAll(Arrays.asList(
            "tacz", "pointblank", "ballistix", "superbwarfare", "warium", "capsawims",
            "immersiveengineering", "industrialupgrade", "appliedenergistics2", 
            "mekanism", "thermal", "simplyquarries", "quantumgenerators", 
            "immersivevehicles", "ashvehicle", "mcsp", "voltaic", "embeddium",
            "entityculling", "ferritecore", "voicechat", "xaerominimap"
        ));
        return mods;
    }
    
    /**
     * Log balancing results
     */
    private void logBalancingResults() {
        plugin.getLogger().info("Dynamic mod balancing completed.");
        
        // Log top 5 most popular mods
        Map<String, ModUsageStats> sortedStats = modUsageStats.entrySet().stream()
            .sorted(Map.Entry.<String, ModUsageStats>comparingByValue(
                Comparator.comparingDouble(stats -> stats.popularityScore)).reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        
        plugin.getLogger().info("Top 5 popular mods:");
        int count = 0;
        for (Map.Entry<String, ModUsageStats> entry : sortedStats.entrySet()) {
            if (count++ >= 5) break;
            plugin.getLogger().info("  " + entry.getKey() + ": " + 
                String.format("%.1f%%", entry.getValue().popularityScore) + " usage");
        }
    }
    
    /**
     * Notify all players in a nation about balancing changes
     */
    private void notifyNation(String nationId, String message) {
        plugin.getNationManager().getNationById(nationId).ifPresent(nation -> {
            for (UUID citizenId : nation.getCitizens()) {
                Player player = Bukkit.getPlayer(citizenId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        });
    }
    
    /**
     * Get player activity record
     */
    public PlayerActivityRecord getPlayerActivity(UUID playerId) {
        return playerActivityRecords.get(playerId);
    }
    
    /**
     * Get nation activity record
     */
    public NationActivityRecord getNationActivity(String nationId) {
        return nationActivityRecords.get(nationId);
    }
    
    /**
     * Get mod usage statistics
     */
    public ModUsageStats getModUsageStats(String modId) {
        return modUsageStats.get(modId);
    }
    
    /**
     * Get mod balance parameters
     */
    public ModBalanceParameters getModBalanceParameters(String modId) {
        return modBalanceParameters.get(modId);
    }
    
    /**
     * Get global activity statistics
     */
    public Map<String, Object> getGlobalActivityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPlayersTracked", playerActivityRecords.size());
        stats.put("totalNationsTracked", nationActivityRecords.size());
        
        // Calculate global engagement
        double totalEngagement = playerActivityRecords.values().stream()
            .mapToDouble(record -> record.averageModEngagement)
            .sum();
        stats.put("averagePlayerEngagement", 
            playerActivityRecords.size() > 0 ? totalEngagement / playerActivityRecords.size() : 0);
        
        // Most active mods globally
        Map<String, Integer> globalModUsage = new HashMap<>();
        for (PlayerActivityRecord record : playerActivityRecords.values()) {
            for (Map.Entry<String, Integer> entry : record.modInteractionCount.entrySet()) {
                globalModUsage.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        stats.put("globalModUsage", globalModUsage);
        
        // Nation engagement statistics
        Map<String, Object> nationEngagement = new HashMap<>();
        double totalNationEngagement = nationActivityRecords.values().stream()
            .mapToDouble(record -> record.averageModEngagement)
            .sum();
        nationEngagement.put("averageNationEngagement", 
            nationActivityRecords.size() > 0 ? totalNationEngagement / nationActivityRecords.size() : 0);
        
        // Engagement by category
        double totalMilitary = nationActivityRecords.values().stream().mapToDouble(r -> r.militaryEngagement).sum();
        double totalIndustrial = nationActivityRecords.values().stream().mapToDouble(r -> r.industrialEngagement).sum();
        double totalTech = nationActivityRecords.values().stream().mapToDouble(r -> r.techEngagement).sum();
        
        nationEngagement.put("averageMilitaryEngagement", 
            nationActivityRecords.size() > 0 ? totalMilitary / nationActivityRecords.size() : 0);
        nationEngagement.put("averageIndustrialEngagement", 
            nationActivityRecords.size() > 0 ? totalIndustrial / nationActivityRecords.size() : 0);
        nationEngagement.put("averageTechEngagement", 
            nationActivityRecords.size() > 0 ? totalTech / nationActivityRecords.size() : 0);
        
        stats.put("nationEngagement", nationEngagement);
        
        return stats;
    }
    
    /**
     * Get mod-specific statistics
     */
    public Map<String, Object> getModStatistics(String modId) {
        Map<String, Object> stats = new HashMap<>();
        
        ModUsageStats usageStats = modUsageStats.get(modId);
        ModBalanceParameters balanceParams = modBalanceParameters.get(modId);
        
        if (usageStats != null) {
            stats.put("popularityScore", usageStats.popularityScore);
            stats.put("totalUses", usageStats.totalUses);
            stats.put("activeUsers", usageStats.activeUsers);
            stats.put("dailyActiveUsers", usageStats.dailyActiveUsers);
            stats.put("averageUsagePerPlayer", usageStats.averageUsagePerPlayer);
        }
        
        if (balanceParams != null) {
            stats.put("difficultyMultiplier", balanceParams.difficultyMultiplier);
            stats.put("resourceAbundance", balanceParams.resourceAbundance);
            stats.put("craftingTimeMultiplier", balanceParams.craftingTimeMultiplier);
            stats.put("energyMultiplier", balanceParams.energyMultiplier);
            stats.put("combatEffectiveness", balanceParams.combatEffectiveness);
            stats.put("lastAdjustment", balanceParams.lastAdjustment);
            stats.put("adjustmentReason", balanceParams.adjustmentReason);
        }
        
        return stats;
    }
    
    /**
     * Manually override mod balance parameters
     */
    public void setModParameters(String modId, ModBalanceParameters parameters) {
        modBalanceParameters.put(modId, parameters);
        
        // Apply immediately
        recipeIntegrationService.adjustModRecipeParameters(modId, parameters);
        modIntegrationEnhancementService.adjustModCompatibilityParameters(modId, parameters);
    }
    
    /**
     * Reset all balancing parameters to defaults
     */
    public void resetAllParameters() {
        modBalanceParameters.clear();
        initializeDefaultBalanceParameters();
        
        // Reset game systems
        recipeIntegrationService.resetAllModParameters();
        modIntegrationEnhancementService.resetAllModParameters();
    }
    
    /**
     * Get all mod parameters
     */
    public Map<String, ModBalanceParameters> getAllModParameters() {
        return new HashMap<>(modBalanceParameters);
    }
}