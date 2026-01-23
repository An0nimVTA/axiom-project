package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing modpacks and their configurations
 * Allows enabling/disabling sets of mods with specific configurations
 */
public class ModPackManagerService {
    private final AXIOM plugin;
    private final RecipeIntegrationService recipeIntegrationService;
    private final ModIntegrationEnhancementService modIntegrationEnhancementService;
    
    // Available modpacks with their configurations
    private final Map<String, ModPack> modPacks = new ConcurrentHashMap<>();
    
    // Active modpacks per nation
    private final Map<String, String> activeModPacks = new ConcurrentHashMap<>();
    
    // Modpack configuration
    private FileConfiguration modPackConfig;
    private File modPackFile;
    
    public static class ModPack {
        String id;
        String name;
        String description;
        List<String> requiredMods;
        List<String> optionalMods;
        Map<String, Object> configuration; // Specific settings for this modpack
        boolean enabled;
        int recommendedPlayers; // Recommended player count for this modpack
        String compatibilityLevel; // "balanced", "hardcore", "casual"
        List<ModIntegrationRule> integrationRules;
        
        public ModPack(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.requiredMods = new ArrayList<>();
            this.optionalMods = new ArrayList<>();
            this.configuration = new HashMap<>();
            this.enabled = true;
            this.integrationRules = new ArrayList<>();
            this.compatibilityLevel = "balanced";
        }
    }
    
    public static class ModIntegrationRule {
        String sourceMod;
        String targetMod;
        String conversionType; // "direct", "recipe", "effect", "stat_boost"
        String conversionItem;
        double conversionRate;
        boolean enabled;
        
        public ModIntegrationRule(String sourceMod, String targetMod, String conversionType, String conversionItem) {
            this.sourceMod = sourceMod;
            this.targetMod = targetMod;
            this.conversionType = conversionType;
            this.conversionItem = conversionItem;
            this.conversionRate = 1.0;
            this.enabled = true;
        }
    }
    
    public ModPackManagerService(AXIOM plugin) {
        this.plugin = plugin;
        this.recipeIntegrationService = plugin.getRecipeIntegrationService();
        this.modIntegrationEnhancementService = plugin.getModIntegrationEnhancementService();
        
        initializeConfig();
        loadModPacks();
        
        // Register default modpacks if none exist
        registerDefaultModPacks();
    }
    
    private void initializeConfig() {
        modPackFile = new File(plugin.getDataFolder(), "modpacks.yml");
        if (!modPackFile.exists()) {
            // Create default config
            createDefaultModPackConfig();
        }
        modPackConfig = YamlConfiguration.loadConfiguration(modPackFile);
    }
    
    private void createDefaultModPackConfig() {
        // Create a default configuration with common modpacks
        modPackConfig.set("modpacks.warfare.name", "Warfare Expansion");
        modPackConfig.set("modpacks.warfare.description", "Military mods: TACZ, PointBlank, Ballistix, etc.");
        modPackConfig.set("modpacks.warfare.requiredMods", Arrays.asList("tacz", "pointblank"));
        modPackConfig.set("modpacks.warfare.optionalMods", Arrays.asList("ballistix", "superbwarfare", "warium"));
        modPackConfig.set("modpacks.warfare.compatibilityLevel", "balanced");
        modPackConfig.set("modpacks.warfare.recommendedPlayers", 10);
        
        modPackConfig.set("modpacks.industrial.name", "Industrial Revolution");
        modPackConfig.set("modpacks.industrial.description", "Industrial mods: Industrial Upgrade, IE, AE2, etc.");
        modPackConfig.set("modpacks.industrial.requiredMods", Arrays.asList("industrialupgrade", "immersiveengineering"));
        modPackConfig.set("modpacks.industrial.optionalMods", Arrays.asList("appliedenergistics2", "mekanism", "thermal"));
        modPackConfig.set("modpacks.industrial.compatibilityLevel", "balanced");
        modPackConfig.set("modpacks.industrial.recommendedPlayers", 15);
        
        modPackConfig.set("modpacks.technology.name", "Tech Revolution");
        modPackConfig.set("modpacks.technology.description", "High tech mods: AE2, IE, Quantum Generators, etc.");
        modPackConfig.set("modpacks.technology.requiredMods", Arrays.asList("appliedenergistics2", "immersiveengineering"));
        modPackConfig.set("modpacks.technology.optionalMods", Arrays.asList("quantumgenerators", "powerutils", "voltaic"));
        modPackConfig.set("modpacks.technology.compatibilityLevel", "hardcore");
        modPackConfig.set("modpacks.technology.recommendedPlayers", 20);
        
        try {
            modPackConfig.save(modPackFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create default modpack config: " + e.getMessage());
        }
    }
    
    /**
     * Load modpacks from configuration
     */
    private void loadModPacks() {
        if (modPackConfig.contains("modpacks")) {
            for (String modPackId : modPackConfig.getConfigurationSection("modpacks").getKeys(false)) {
                String path = "modpacks." + modPackId;
                
                String name = modPackConfig.getString(path + ".name", modPackId);
                String description = modPackConfig.getString(path + ".description", "No description");
                
                ModPack modPack = new ModPack(modPackId, name, description);
                modPack.requiredMods = modPackConfig.getStringList(path + ".requiredMods");
                modPack.optionalMods = modPackConfig.getStringList(path + ".optionalMods");
                modPack.compatibilityLevel = modPackConfig.getString(path + ".compatibilityLevel", "balanced");
                modPack.recommendedPlayers = modPackConfig.getInt(path + ".recommendedPlayers", 5);
                modPack.enabled = modPackConfig.getBoolean(path + ".enabled", true);
                
                // Load integration rules
                if (modPackConfig.contains(path + ".integrationRules")) {
                    for (String ruleId : modPackConfig.getConfigurationSection(path + ".integrationRules").getKeys(false)) {
                        String rulePath = path + ".integrationRules." + ruleId;
                        String sourceMod = modPackConfig.getString(rulePath + ".sourceMod");
                        String targetMod = modPackConfig.getString(rulePath + ".targetMod");
                        String conversionType = modPackConfig.getString(rulePath + ".conversionType", "direct");
                        String conversionItem = modPackConfig.getString(rulePath + ".conversionItem");
                        
                        if (sourceMod != null && targetMod != null && conversionItem != null) {
                            ModIntegrationRule rule = new ModIntegrationRule(sourceMod, targetMod, conversionType, conversionItem);
                            rule.enabled = modPackConfig.getBoolean(rulePath + ".enabled", true);
                            rule.conversionRate = modPackConfig.getDouble(rulePath + ".conversionRate", 1.0);
                            modPack.integrationRules.add(rule);
                        }
                    }
                }
                
                modPacks.put(modPackId, modPack);
            }
        }
    }
    
    /**
     * Register default modpacks if none are configured
     */
    private void registerDefaultModPacks() {
        if (modPacks.isEmpty()) {
            // Warfare modpack
            ModPack warfare = new ModPack(
                "warfare", 
                "Warfare Expansion", 
                "Military mods: TACZ, PointBlank, Ballistix, Superb Warfare, Warium"
            );
            warfare.requiredMods.addAll(Arrays.asList("tacz", "pointblank"));
            warfare.optionalMods.addAll(Arrays.asList("ballistix", "superbwarfare", "warium", "capsawims"));
            warfare.compatibilityLevel = "balanced";
            warfare.recommendedPlayers = 10;
            
            // Add integration rules for warfare
            warfare.integrationRules.add(new ModIntegrationRule("tacz", "pointblank", "ammo_compatibility", "universal_ammo"));
            warfare.integrationRules.add(new ModIntegrationRule("pointblank", "tacz", "component_sharing", "firearm_parts"));
            warfare.integrationRules.add(new ModIntegrationRule("superbwarfare", "ballistix", "munition_exchange", "explosives"));
            
            modPacks.put("warfare", warfare);
            
            // Industrial modpack
            ModPack industrial = new ModPack(
                "industrial", 
                "Industrial Revolution", 
                "Industrial mods: Industrial Upgrade, Immersive Engineering, Applied Energistics 2"
            );
            industrial.requiredMods.addAll(Arrays.asList("industrialupgrade", "immersiveengineering"));
            industrial.optionalMods.addAll(Arrays.asList("appliedenergistics2", "mekanism", "thermal"));
            industrial.compatibilityLevel = "balanced";
            industrial.recommendedPlayers = 15;
            
            // Add integration rules for industry
            industrial.integrationRules.add(new ModIntegrationRule("industrialupgrade", "immersiveengineering", "energy_exchange", "fe_conversion"));
            industrial.integrationRules.add(new ModIntegrationRule("immersiveengineering", "appliedenergistics2", "component_recycling", "material_processing"));
            industrial.integrationRules.add(new ModIntegrationRule("appliedenergistics2", "industrialupgrade", "automation_sync", "machines"));
            
            modPacks.put("industrial", industrial);
            
            // Technology modpack
            ModPack technology = new ModPack(
                "technology", 
                "Tech Revolution", 
                "High tech mods: Applied Energistics 2, Immersive Engineering, Quantum Generators"
            );
            technology.requiredMods.addAll(Arrays.asList("appliedenergistics2", "immersiveengineering"));
            technology.optionalMods.addAll(Arrays.asList("quantumgenerators", "powerutils", "voltaic"));
            technology.compatibilityLevel = "hardcore";
            technology.recommendedPlayers = 20;
            
            // Add integration rules for technology
            technology.integrationRules.add(new ModIntegrationRule("appliedenergistics2", "immersiveengineering", "network_integration", "me_systems"));
            technology.integrationRules.add(new ModIntegrationRule("immersiveengineering", "quantumgenerators", "power_scalability", "energy_production"));
            technology.integrationRules.add(new ModIntegrationRule("quantumgenerators", "powerutils", "distribution", "grid_management"));
            
            modPacks.put("technology", technology);
            
            // Balanced modpack combining all
            ModPack balanced = new ModPack(
                "balanced", 
                "Balanced Experience", 
                "Well-balanced mix of all mod types for optimal gameplay"
            );
            balanced.requiredMods.addAll(Arrays.asList("industrialupgrade", "immersiveengineering", "tacz"));
            balanced.optionalMods.addAll(Arrays.asList("appliedenergistics2", "pointblank", "ballistix", "superbwarfare"));
            balanced.compatibilityLevel = "balanced";
            balanced.recommendedPlayers = 25;
            
            // Add comprehensive integration rules
            balanced.integrationRules.add(new ModIntegrationRule("industrialupgrade", "tacz", "material_supply", "steel_ingots"));
            balanced.integrationRules.add(new ModIntegrationRule("tacz", "appliedenergistics2", "ammo_storage", "me_security"));
            balanced.integrationRules.add(new ModIntegrationRule("appliedenergistics2", "immersiveengineering", "resource_auto_processing", "automation_chain"));
            balanced.integrationRules.add(new ModIntegrationRule("immersiveengineering", "industrialupgrade", "power_distribution", "energy_grid"));
            
            modPacks.put("balanced", balanced);
        }
    }
    
    /**
     * Enable a modpack for a nation
     */
    public boolean enableModPackForNation(String nationId, String modPackId) {
        ModPack modPack = modPacks.get(modPackId);
        if (modPack == null) {
            return false;
        }
        
        if (!modPack.enabled) {
            return false;
        }
        
        // Check if required mods are available
        if (!areRequiredModsAvailable(modPack)) {
            return false;
        }
        
        // Set as active modpack for nation
        activeModPacks.put(nationId, modPackId);
        
        // Activate integration rules for this nation
        activateIntegrationRules(nationId, modPack);
        
        // Log the activation
        plugin.getLogger().info("Modpack '" + modPackId + "' activated for nation '" + nationId + "'");
        
        // Notify nation members
        notifyNationMembers(nationId, 
            "§aМодпак активирован: §e" + modPack.name + "§a! §7(" + modPack.description + ")");
        
        return true;
    }
    
    /**
     * Disable a modpack for a nation
     */
    public boolean disableModPackForNation(String nationId, String modPackId) {
        if (!activeModPacks.getOrDefault(nationId, "").equals(modPackId)) {
            return false;
        }
        
        // Deactivate integration rules for this nation
        ModPack modPack = modPacks.get(modPackId);
        if (modPack != null) {
            deactivateIntegrationRules(nationId, modPack);
        }
        
        // Remove from active
        activeModPacks.remove(nationId);
        
        plugin.getLogger().info("Modpack '" + modPackId + "' deactivated for nation '" + nationId + "'");
        
        // Notify nation members
        notifyNationMembers(nationId, 
            "§cМодпак деактивирован: §e" + (modPack != null ? modPack.name : modPackId));
        
        return true;
    }
    
    /**
     * Disable a modpack for a nation
     */
    public boolean disableModPackForNation(String nationId, String modPackId) {
        if (!activeModPacks.getOrDefault(nationId, "").equals(modPackId)) {
            return false;
        }
        
        // Deactivate integration rules for this nation
        deactivateIntegrationRules(nationId, modPacks.get(modPackId));
        
        // Remove from active
        activeModPacks.remove(nationId);
        
        plugin.getLogger().info("Modpack '" + modPackId + "' deactivated for nation '" + nationId + "'");
        
        // Notify nation members
        notifyNationMembers(nationId, 
            "§cМодпак деактивирован: §e" + modPacks.get(modPackId).name);
        
        return true;
    }
    
    /**
     * Activate integration rules for a modpack
     */
    private void activateIntegrationRules(String nationId, ModPack modPack) {
        for (ModIntegrationRule rule : modPack.integrationRules) {
            if (rule.enabled) {
                // Enable specific integration based on rule type
                switch (rule.conversionType.toLowerCase()) {
                    case "direct":
                        recipeIntegrationService.enableIntegration(rule.sourceMod + "_to_" + rule.targetMod);
                        break;
                    case "recipe":
                        // Enable cross-mod recipe integration
                        enableCrossModRecipe(rule);
                        break;
                    case "effect":
                        // Apply nation-wide effect (handled by other services)
                        break;
                    case "stat_boost":
                        // Apply stat boosts (handled by other services)
                        break;
                    case "ammo_compatibility":
                        // Enable ammo compatibility
                        recipeIntegrationService.enableIntegration("ammo_cross_compatibility");
                        break;
                    case "energy_exchange":
                        // Enable energy system compatibility
                        recipeIntegrationService.enableIntegration("energy_system_compatibility");
                        break;
                }
            }
        }
    }
    
    /**
     * Deactivate integration rules for a modpack
     */
    private void deactivateIntegrationRules(String nationId, ModPack modPack) {
        for (ModIntegrationRule rule : modPack.integrationRules) {
            if (rule.enabled) {
                switch (rule.conversionType.toLowerCase()) {
                    case "direct":
                        recipeIntegrationService.disableIntegration(rule.sourceMod + "_to_" + rule.targetMod);
                        break;
                    case "recipe":
                        disableCrossModRecipe(rule);
                        break;
                    case "ammo_compatibility":
                        recipeIntegrationService.disableIntegration("ammo_cross_compatibility");
                        break;
                    case "energy_exchange":
                        recipeIntegrationService.disableIntegration("energy_system_compatibility");
                        break;
                }
            }
        }
    }
    
    /**
     * Enable a cross-mod recipe based on rule
     */
    private void enableCrossModRecipe(ModIntegrationRule rule) {
        // This would create dynamic recipes based on the rule
        // For now, we'll just enable the corresponding integration
        String integrationId = rule.sourceMod + "_cross_" + rule.targetMod;
        recipeIntegrationService.enableIntegration(integrationId);
    }
    
    /**
     * Disable a cross-mod recipe based on rule
     */
    private void disableCrossModRecipe(ModIntegrationRule rule) {
        String integrationId = rule.sourceMod + "_cross_" + rule.targetMod;
        recipeIntegrationService.disableIntegration(integrationId);
    }
    
    /**
     * Check if required mods for a modpack are available
     */
    private boolean areRequiredModsAvailable(ModPack modPack) {
        for (String requiredMod : modPack.requiredMods) {
            if (!plugin.getModIntegrationService().isModAvailable(requiredMod)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get available modpacks
     */
    public Collection<ModPack> getAvailableModPacks() {
        return new ArrayList<>(modPacks.values());
    }
    
    /**
     * Get modpack by ID
     */
    public ModPack getModPack(String modPackId) {
        return modPacks.get(modPackId);
    }
    
    /**
     * Get active modpack for a nation
     */
    public String getActiveModPackForNation(String nationId) {
        return activeModPacks.get(nationId);
    }
    
    /**
     * Check if a modpack is available for a nation
     */
    public boolean isModPackAvailableForNation(String nationId, String modPackId) {
        ModPack modPack = modPacks.get(modPackId);
        if (modPack == null) return false;
        
        // Check if required mods are available
        return areRequiredModsAvailable(modPack);
    }
    
    /**
     * Get modpacks compatible with the available mods
     */
    public List<ModPack> getCompatibleModPacks() {
        List<ModPack> compatible = new ArrayList<>();
        for (ModPack modPack : modPacks.values()) {
            if (areRequiredModsAvailable(modPack)) {
                compatible.add(modPack);
            }
        }
        return compatible;
    }
    
    /**
     * Notify nation members about modpack change
     */
    private void notifyNationMembers(String nationId, String message) {
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation != null) {
            for (UUID citizenId : nation.getCitizens()) {
                Player player = org.bukkit.Bukkit.getPlayer(citizenId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
    }
    
    /**
     * Apply nation-specific modpack bonuses
     */
    public void applyNationModPackBonuses(String nationId) {
        String activeModPackId = getActiveModPackForNation(nationId);
        if (activeModPackId != null) {
            ModPack modPack = getModPack(activeModPackId);
            if (modPack != null) {
                // Apply bonuses based on modpack configuration
                // This would connect to other AXIOM services to apply bonuses
                
                // For example, if it's a warfare modpack, boost military stats
                if (activeModPackId.contains("warfare") || activeModPackId.contains("military")) {
                    // Apply military bonuses through diplomacy/economy systems
                    // This would be implemented in conjunction with other services
                }
                
                // If it's an industrial modpack, boost production
                if (activeModPackId.contains("industrial") || activeModPackId.contains("tech")) {
                    // Apply industrial bonuses
                }
            }
        }
    }
    
    /**
     * Get nation-specific modpack statistics
     */
    public Map<String, Object> getNationModPackStats(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        String activeModPackId = getActiveModPackForNation(nationId);
        
        if (activeModPackId != null) {
            ModPack modPack = getModPack(activeModPackId);
            if (modPack != null) {
                stats.put("activeModPack", modPack.name);
                stats.put("activeModPackId", activeModPackId);
                stats.put("description", modPack.description);
                stats.put("requiredMods", modPack.requiredMods);
                stats.put("optionalMods", modPack.optionalMods);
                stats.put("compatibilityLevel", modPack.compatibilityLevel);
                stats.put("recommendedPlayers", modPack.recommendedPlayers);
                
                // Count active integration rules
                int activeRules = 0;
                for (ModIntegrationRule rule : modPack.integrationRules) {
                    if (rule.enabled) activeRules++;
                }
                stats.put("activeIntegrationRules", activeRules);
                stats.put("totalIntegrationRules", modPack.integrationRules.size());
            }
        } else {
            stats.put("activeModPack", "None");
            stats.put("activeModPackId", null);
        }
        
        return stats;
    }
    
    /**
     * Get modpack statistics
     */
    public Map<String, Object> getModPackStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalModPacks", modPacks.size());
        stats.put("activeNations", activeModPacks.size());
        
        // Count modpacks by compatibility level
        Map<String, Integer> byLevel = new HashMap<>();
        for (ModPack modPack : modPacks.values()) {
            String level = modPack.compatibilityLevel;
            byLevel.put(level, byLevel.getOrDefault(level, 0) + 1);
        }
        stats.put("byCompatibilityLevel", byLevel);
        
        // Count nations by modpack
        Map<String, Integer> nationsByModPack = new HashMap<>();
        for (String activeModPackId : activeModPacks.values()) {
            nationsByModPack.put(activeModPackId, nationsByModPack.getOrDefault(activeModPackId, 0) + 1);
        }
        stats.put("nationsByModPack", nationsByModPack);
        
        return stats;
    }
    
    /**
     * Save modpack configuration to file
     */
    public void saveConfig() {
        try {
            modPackConfig.set("modpacks", null); // Clear
            
            for (ModPack modPack : modPacks.values()) {
                String path = "modpacks." + modPack.id;
                modPackConfig.set(path + ".name", modPack.name);
                modPackConfig.set(path + ".description", modPack.description);
                modPackConfig.set(path + ".requiredMods", modPack.requiredMods);
                modPackConfig.set(path + ".optionalMods", modPack.optionalMods);
                modPackConfig.set(path + ".compatibilityLevel", modPack.compatibilityLevel);
                modPackConfig.set(path + ".recommendedPlayers", modPack.recommendedPlayers);
                modPackConfig.set(path + ".enabled", modPack.enabled);
                
                // Save integration rules
                int i = 0;
                for (ModIntegrationRule rule : modPack.integrationRules) {
                    String rulePath = path + ".integrationRules.rule" + (i++);
                    modPackConfig.set(rulePath + ".sourceMod", rule.sourceMod);
                    modPackConfig.set(rulePath + ".targetMod", rule.targetMod);
                    modPackConfig.set(rulePath + ".conversionType", rule.conversionType);
                    modPackConfig.set(rulePath + ".conversionItem", rule.conversionItem);
                    modPackConfig.set(rulePath + ".enabled", rule.enabled);
                    modPackConfig.set(rulePath + ".conversionRate", rule.conversionRate);
                }
            }
            
            modPackConfig.save(modPackFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save modpack config: " + e.getMessage());
        }
    }
    
    /**
     * Reload modpack configuration from file
     */
    public void reloadConfig() {
        modPackConfig = YamlConfiguration.loadConfiguration(modPackFile);
        modPacks.clear();
        activeModPacks.clear();
        loadModPacks();
    }
    
    /**
     * Add a new modpack
     */
    public void addModPack(ModPack modPack) {
        modPacks.put(modPack.id, modPack);
        saveConfig();
    }
    
    /**
     * Remove a modpack
     */
    public boolean removeModPack(String modPackId) {
        ModPack removed = modPacks.remove(modPackId);
        if (removed != null) {
            // Disable for all nations using it
            for (Map.Entry<String, String> entry : new HashMap<>(activeModPacks).entrySet()) {
                if (entry.getValue().equals(modPackId)) {
                    disableModPackForNation(entry.getKey(), modPackId);
                }
            }
            saveConfig();
            return true;
        }
        return false;
    }
    
    /**
     * Load modpacks from external config file (new format)
     */
    private void loadModPacksFromConfig() {
        File externalConfigFile = new File(plugin.getDataFolder(), "modpacks.yml");
        if (!externalConfigFile.exists()) {
            plugin.getLogger().info("No external modpacks.yml found, using defaults");
            return;
        }
        
        FileConfiguration externalConfig = YamlConfiguration.loadConfiguration(externalConfigFile);
        
        if (externalConfig.contains("definitions")) {
            for (String modPackId : externalConfig.getConfigurationSection("definitions").getKeys(false)) {
                String path = "definitions." + modPackId;
                
                String name = externalConfig.getString(path + ".name", modPackId);
                String description = externalConfig.getString(path + ".description", "No description");
                
                ModPack modPack = new ModPack(modPackId, name, description);
                modPack.requiredMods = externalConfig.getStringList(path + ".requiredMods");
                modPack.optionalMods = externalConfig.getStringList(path + ".optionalMods");
                modPack.compatibilityLevel = externalConfig.getString(path + ".compatibilityLevel", "balanced");
                modPack.recommendedPlayers = externalConfig.getInt(path + ".recommendedPlayers", 5);
                modPack.enabled = externalConfig.getBoolean(path + ".enabled", true);
                
                // Load integration rules from new format
                if (externalConfig.contains(path + ".integrationRules")) {
                    for (String ruleId : externalConfig.getConfigurationSection(path + ".integrationRules").getKeys(false)) {
                        String rulePath = path + ".integrationRules." + ruleId;
                        String sourceMod = externalConfig.getString(rulePath + ".sourceMod");
                        String targetMod = externalConfig.getString(rulePath + ".targetMod");
                        String conversionType = externalConfig.getString(rulePath + ".conversionType", "direct");
                        String conversionItem = externalConfig.getString(rulePath + ".conversionItem");
                        
                        if (sourceMod != null && targetMod != null && conversionItem != null) {
                            ModIntegrationRule rule = new ModIntegrationRule(sourceMod, targetMod, conversionType, conversionItem);
                            rule.enabled = externalConfig.getBoolean(rulePath + ".enabled", true);
                            rule.conversionRate = externalConfig.getDouble(rulePath + ".conversionRate", 1.0);
                            modPack.integrationRules.add(rule);
                        }
                    }
                }
                
                modPacks.put(modPackId, modPack);
            }
        }
    }
}