package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for modpack management and cross-mod balancing
 * Manages mod integration, energy conversion, and gameplay balance across modpack
 */
public class ModBalancerService {
    private final AXIOM plugin;
    private final BalancingService balancingService;
    
    // Mod compatibility and balancing
    private final Map<String, ModCompatibilityInfo> modCompatibility = new ConcurrentHashMap<>();
    
    // Energy system balancing
    private final Map<String, EnergyConversionRate> energyConversion = new ConcurrentHashMap<>();
    
    // Mod usage statistics
    private final Map<String, ModUsageStats> modUsageStats = new ConcurrentHashMap<>();
    
    // Nation-specific mod settings
    private final Map<String, NationModSettings> nationModSettings = new ConcurrentHashMap<>();

    public static class ModCompatibilityInfo {
        String modId;
        String name;
        String version;
        List<String> conflicts = new ArrayList<>();  // mods this conflicts with
        List<String> dependencies = new ArrayList<>();  // mods this depends on
        List<String> synergies = new ArrayList<>();  // mods this synergizes with
        double compatibilityScore = 1.0;  // 0.0-2.0 scale
        boolean enabled = true;
        
        public ModCompatibilityInfo(String modId, String name) {
            this.modId = modId;
            this.name = name;
        }
    }
    
    public static class EnergyConversionRate {
        String fromMod;
        String toMod;
        double rate;  // Conversion efficiency (0.1 = 10% efficiency, 1.0 = 100%)
        double maxTransfer;  // Max amount that can be transferred per operation
        
        public EnergyConversionRate(String from, String to, double rate, double maxTransfer) {
            this.fromMod = from;
            this.toMod = to;
            this.rate = rate;
            this.maxTransfer = maxTransfer;
        }
    }
    
    public static class ModUsageStats {
        String modId;
        int blocksPlaced = 0;
        int itemsCrafted = 0;
        int energyConsumed = 0;
        int energyProduced = 0;
        double efficiencyRating = 1.0;
        long lastUpdate = System.currentTimeMillis();
        
        public ModUsageStats(String modId) {
            this.modId = modId;
        }
    }
    
    public static class NationModSettings {
        String nationId;
        Map<String, Boolean> modEnabled = new HashMap<>();  // Per-nation mod enable/disable
        Map<String, Double> modBalanceMultiplier = new HashMap<>();  // Per-nation balance adjustments
        boolean allowModIntegration = true;
        boolean allowEnergyConversion = true;
        double maxEnergyTransfer = 100000.0;
        
        public NationModSettings(String nationId) {
            this.nationId = nationId;
            // Initialize with default settings from config
        }
    }

    public ModBalancerService(AXIOM plugin) {
        this.plugin = plugin;
        this.balancingService = plugin.getBalancingService();
        
        initializeModCompatibility();
        initializeEnergyConversionRates();
        initializeDefaultSettings();
        
        // Update mod stats every 10 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateModStats, 0, 20 * 60 * 10);
    }
    
    private void initializeModCompatibility() {
        // Applied Energistics 2 - high-tech automation
        ModCompatibilityInfo ae2 = new ModCompatibilityInfo("appliedenergistics2", "Applied Energistics 2");
        ae2.synergies.addAll(Arrays.asList("immersiveengineering", "industrialupgrade"));
        ae2.compatibilityScore = 1.2; // Very good compatibility
        modCompatibility.put("appliedenergistics2", ae2);
        
        // Immersive Engineering - industrial backbone
        ModCompatibilityInfo ie = new ModCompatibilityInfo("immersiveengineering", "Immersive Engineering");
        ie.synergies.addAll(Arrays.asList("appliedenergistics2", "industrialupgrade"));
        ie.conflicts.add("some_old_mod"); // example conflict
        ie.compatibilityScore = 1.1;
        modCompatibility.put("immersiveengineering", ie);
        
        // Industrial Upgrade - modern industrial
        ModCompatibilityInfo iu = new ModCompatibilityInfo("industrialupgrade", "Industrial Upgrade");
        iu.synergies.addAll(Arrays.asList("immersiveengineering", "appliedenergistics2"));
        iu.compatibilityScore = 1.3; // Excellent compatibility as main industrial mod
        modCompatibility.put("industrialupgrade", iu);
        
        // TACZ - modern firearms
        ModCompatibilityInfo tacz = new ModCompatibilityInfo("tacz", "TACZ");
        tacz.synergies.add("capsawims"); // Tactical equipment synergy
        tacz.compatibilityScore = 1.0;
        modCompatibility.put("tacz", tacz);
        
        // PointBlank - alternative firearms
        ModCompatibilityInfo pb = new ModCompatibilityInfo("pointblank", "Point Blank");
        pb.synergies.add("capsawims");
        pb.compatibilityScore = 0.9; // Good but lower priority than TACZ
        modCompatibility.put("pointblank", pb);
        
        // Ballistix - artillery and explosives
        ModCompatibilityInfo ballistix = new ModCompatibilityInfo("ballistix", "Ballistix");
        ballistix.synergies.addAll(Arrays.asList("tacz", "superbwarfare"));
        ballistix.compatibilityScore = 1.1;
        modCompatibility.put("ballistix", ballistix);
        
        // Superb Warfare - military vehicles
        ModCompatibilityInfo sw = new ModCompatibilityInfo("superbwarfare", "Superb Warfare");
        sw.synergies.add("immersivevehicles");
        sw.compatibilityScore = 1.0;
        modCompatibility.put("superbwarfare", sw);
        
        // Immersive Vehicles - transportation
        ModCompatibilityInfo iv = new ModCompatibilityInfo("immersivevehicles", "Immersive Vehicles");
        iv.synergies.add("ashvehicle");
        iv.compatibilityScore = 1.0;
        modCompatibility.put("immersivevehicles", iv);
        
        // Warium - weapons and warfare
        ModCompatibilityInfo warium = new ModCompatibilityInfo("warium", "Warium");
        warium.synergies.addAll(Arrays.asList("ballistix", "superbwarfare"));
        warium.compatibilityScore = 1.05;
        modCompatibility.put("warium", warium);
        
        // Simply Quarries - automated mining
        ModCompatibilityInfo sq = new ModCompatibilityInfo("simplyquarries", "Simply Quarries");
        sq.synergies.addAll(Arrays.asList("industrialupgrade", "immersiveengineering"));
        sq.compatibilityScore = 1.0;
        modCompatibility.put("simplyquarries", sq);
        
        // Quantum Generators - advanced power
        ModCompatibilityInfo qg = new ModCompatibilityInfo("quantumgenerators", "Quantum Generators");
        qg.synergies.add("industrialupgrade");
        qg.compatibilityScore = 1.2;
        modCompatibility.put("quantumgenerators", qg);
        
        // PowerUtils - energy optimization
        ModCompatibilityInfo pu = new ModCompatibilityInfo("powerutils", "Power Utils");
        pu.synergies.addAll(Arrays.asList("industrialupgrade", "immersiveengineering"));
        pu.compatibilityScore = 0.9; // Supportive mod, not primary
        modCompatibility.put("powerutils", pu);
        
        // Caps Awims - tactical equipment
        ModCompatibilityInfo caps = new ModCompatibilityInfo("capsawims", "CAPS Awims Tactical Equipment");
        caps.synergies.addAll(Arrays.asList("tacz", "pointblank", "warium"));
        caps.compatibilityScore = 0.95;
        modCompatibility.put("capsawims", caps);
        
        // Voice Chat - communication
        ModCompatibilityInfo voice = new ModCompatibilityInfo("voicechat", "Voice Chat");
        voice.compatibilityScore = 1.0; // Communication mod, high compatibility
        modCompatibility.put("voicechat", voice);
        
        // Xaero's maps - navigation
        ModCompatibilityInfo xaero = new ModCompatibilityInfo("xaerominimap", "Xaero's Minimap");
        xaero.compatibilityScore = 1.0;
        modCompatibility.put("xaerominimap", xaero);
        
        // AshVehicle - civilian vehicles
        ModCompatibilityInfo ashv = new ModCompatibilityInfo("ashvehicle", "Ash Vehicle");
        ashv.synergies.add("immersivevehicles");
        ashv.compatibilityScore = 0.9;
        modCompatibility.put("ashvehicle", ashv);
        
        // MCSP - military vehicles
        ModCompatibilityInfo mcsp = new ModCompatibilityInfo("mcsp", "MCSP");
        mcsp.synergies.add("superbwarfare");
        mcsp.compatibilityScore = 0.95;
        modCompatibility.put("mcsp", mcsp);
        
        // Voltaic - energy API
        ModCompatibilityInfo voltaic = new ModCompatibilityInfo("voltaic", "Voltaic");
        voltaic.synergies.add("immersiveengineering");
        voltaic.compatibilityScore = 1.0;
        modCompatibility.put("voltaic", voltaic);
    }
    
    private void initializeEnergyConversionRates() {
        // Industrial Upgrade as base energy system (FE)
        // Set conversion rates between different energy systems
        energyConversion.put("iu_to_fe", new EnergyConversionRate("industrialupgrade", "fe", 1.0, 1000000.0));
        energyConversion.put("ie_to_fe", new EnergyConversionRate("immersiveengineering", "fe", 0.9, 500000.0));
        energyConversion.put("ae2_to_fe", new EnergyConversionRate("appliedenergistics2", "fe", 0.95, 250000.0));
        energyConversion.put("quantum_to_fe", new EnergyConversionRate("quantumgenerators", "fe", 1.1, 2000000.0));
        energyConversion.put("voltaic_to_fe", new EnergyConversionRate("voltaic", "fe", 1.0, 100000.0));
    }
    
    private void initializeDefaultSettings() {
        // Default settings for all nations
        // These can be overridden per nation
    }
    
    /**
     * Check if two mods are compatible for a specific nation
     */
    public boolean areModsCompatible(String nationId, String modId1, String modId2) {
        ModCompatibilityInfo mod1 = modCompatibility.get(modId1);
        ModCompatibilityInfo mod2 = modCompatibility.get(modId2);
        
        if (mod1 == null || mod2 == null) return true; // Unknown mods are assumed compatible
        
        // Check for conflicts
        if (mod1.conflicts.contains(modId2) || mod2.conflicts.contains(modId1)) {
            return false;
        }
        
        // Apply nation-specific settings
        NationModSettings settings = nationModSettings.computeIfAbsent(nationId, 
            k -> new NationModSettings(nationId));
        
        if (!settings.allowModIntegration) return false;
        if (!settings.modEnabled.getOrDefault(modId1, true)) return false;
        if (!settings.modEnabled.getOrDefault(modId2, true)) return false;
        
        return true;
    }
    
    /**
     * Calculate effective mod synergy for a nation
     */
    public double getModSynergy(String nationId, List<String> activeMods) {
        double synergy = 1.0;
        Set<String> activeSet = new HashSet<>(activeMods);
        
        for (String modId : activeMods) {
            ModCompatibilityInfo mod = modCompatibility.get(modId);
            if (mod == null) continue;
            
            // Add compatibility score
            synergy *= mod.compatibilityScore;
            
            // Check for synergies with other active mods
            for (String synergyMod : mod.synergies) {
                if (activeSet.contains(synergyMod)) {
                    synergy *= 1.1; // 10% synergy bonus
                }
            }
        }
        
        // Apply nation-specific multipliers
        NationModSettings settings = nationModSettings.get(nationId);
        if (settings != null) {
            for (String modId : activeMods) {
                synergy *= settings.modBalanceMultiplier.getOrDefault(modId, 1.0);
            }
        }
        
        return Math.min(synergy, 2.0); // Cap at 2x for balance
    }
    
    /**
     * Convert energy between mod systems
     */
    public double convertEnergy(String fromMod, String toMod, double amount) {
        String key = fromMod + "_to_" + toMod;
        EnergyConversionRate rate = energyConversion.get(key);
        
        if (rate == null) return 0.0; // No conversion available
        
        if (amount > rate.maxTransfer) {
            amount = rate.maxTransfer; // Cap at maximum transfer
        }
        
        return amount * rate.rate;
    }
    
    /**
     * Get mod usage statistics for a nation
     */
    public ModUsageStats getModUsageStats(String modId) {
        return modUsageStats.computeIfAbsent(modId, ModUsageStats::new);
    }
    
    /**
     * Update mod usage statistics (called periodically)
     */
    private void updateModStats() {
        for (Nation nation : plugin.getNationManager().getAll()) {
            // Update stats for each nation
            updateNationModStats(nation.getId());
        }
    }
    
    /**
     * Update mod stats for a specific nation
     */
    private void updateNationModStats(String nationId) {
        // This would typically scan a nation's chunks/blocks to count mod usage
        // For now, we'll use technology unlocks as a proxy
        
        Set<String> unlockedTechs = plugin.getTechnologyTreeService().getUnlockedTechs(nationId);
        
        for (String techId : unlockedTechs) {
            String modId = getModIdForTechnology(techId);
            if (modId != null) {
                ModUsageStats stats = getModUsageStats(modId);
                stats.itemsCrafted++; // Proxy for technology usage
                stats.lastUpdate = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Map technology ID to mod ID
     */
    private String getModIdForTechnology(String techId) {
        if (techId.contains("tacz")) return "tacz";
        if (techId.contains("pointblank")) return "pointblank";
        if (techId.contains("ballistix")) return "ballistix";
        if (techId.contains("immersiveengineering")) return "immersiveengineering";
        if (techId.contains("appliedenergistics")) return "appliedenergistics2";
        if (techId.contains("industrial")) return "industrialupgrade";
        if (techId.contains("superbwarfare")) return "superbwarfare";
        if (techId.contains("immersivevehicles")) return "immersivevehicles";
        if (techId.contains("warium")) return "warium";
        if (techId.contains("simplyquarries")) return "simplyquarries";
        if (techId.contains("quantum")) return "quantumgenerators";
        if (techId.contains("capsawims")) return "capsawims";
        if (techId.contains("ae2")) return "appliedenergistics2";
        if (techId.contains("ie_")) return "immersiveengineering";
        if (techId.contains("iu_")) return "industrialupgrade";
        
        return null;
    }
    
    /**
     * Get mod compatibility information
     */
    public ModCompatibilityInfo getModCompatibilityInfo(String modId) {
        return modCompatibility.get(modId);
    }
    
    /**
     * Get all available mods
     */
    public Collection<ModCompatibilityInfo> getAvailableMods() {
        return modCompatibility.values();
    }
    
    /**
     * Get all compatible mods for a base mod
     */
    public List<String> getCompatibleMods(String baseModId) {
        List<String> compatible = new ArrayList<>();
        ModCompatibilityInfo baseMod = modCompatibility.get(baseModId);
        
        if (baseMod == null) return compatible;
        
        for (ModCompatibilityInfo mod : modCompatibility.values()) {
            if (mod.modId.equals(baseModId)) continue;
            if (!baseMod.conflicts.contains(mod.modId)) {
                compatible.add(mod.modId);
            }
        }
        
        return compatible;
    }
    
    /**
     * Get nation-specific mod settings
     */
    public NationModSettings getNationModSettings(String nationId) {
        return nationModSettings.computeIfAbsent(nationId, 
            k -> new NationModSettings(nationId));
    }
    
    /**
     * Set mod enabled/disabled for a nation
     */
    public void setModEnabledForNation(String nationId, String modId, boolean enabled) {
        NationModSettings settings = getNationModSettings(nationId);
        settings.modEnabled.put(modId, enabled);
    }
    
    /**
     * Set balance multiplier for a mod in a nation
     */
    public void setModBalanceMultiplier(String nationId, String modId, double multiplier) {
        NationModSettings settings = getNationModSettings(nationId);
        settings.modBalanceMultiplier.put(modId, multiplier);
    }
    
    /**
     * Check if energy conversion is allowed between mods for a nation
     */
    public boolean isEnergyConversionAllowed(String nationId, String fromMod, String toMod) {
        NationModSettings settings = getNationModSettings(nationId);
        return settings.allowEnergyConversion;
    }
    
    /**
     * Get total mod balance statistics
     */
    public Map<String, Object> getModBalancerStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalMods", modCompatibility.size());
        stats.put("totalEnergyConversions", energyConversion.size());
        stats.put("trackedNations", nationModSettings.size());
        stats.put("modUsageStats", modUsageStats.size());
        
        // Mod usage distribution
        Map<String, Integer> usageDistribution = new HashMap<>();
        for (Map.Entry<String, ModUsageStats> entry : modUsageStats.entrySet()) {
            usageDistribution.put(entry.getKey(), entry.getValue().itemsCrafted);
        }
        stats.put("usageDistribution", usageDistribution);
        
        // Most used mods
        List<Map.Entry<String, Integer>> topMods = new ArrayList<>(usageDistribution.entrySet());
        topMods.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        stats.put("topUsedMods", topMods.subList(0, Math.min(10, topMods.size())));
        
        return stats;
    }
    
    /**
     * Get mod balance statistics for a nation
     */
    public Map<String, Object> getNationModBalanceStats(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get unlocked technologies to determine active mods
        Set<String> unlockedTechs = plugin.getTechnologyTreeService().getUnlockedTechs(nationId);
        List<String> activeMods = new ArrayList<>();
        
        for (String techId : unlockedTechs) {
            String modId = getModIdForTechnology(techId);
            if (modId != null && !activeMods.contains(modId)) {
                activeMods.add(modId);
            }
        }
        
        stats.put("activeMods", activeMods);
        stats.put("modSynergy", getModSynergy(nationId, activeMods));
        stats.put("totalActiveMods", activeMods.size());
        
        // Mod-specific stats
        Map<String, Object> modStats = new HashMap<>();
        for (String modId : activeMods) {
            ModUsageStats usage = getModUsageStats(modId);
            modStats.put(modId, Map.of(
                "efficiency", usage.efficiencyRating,
                "itemsCrafted", usage.itemsCrafted,
                "energyProduced", usage.energyProduced,
                "energyConsumed", usage.energyConsumed
            ));
        }
        stats.put("modStats", modStats);
        
        // Military balance (if applicable)
        stats.put("militaryBalance", plugin.getBalancingService().getMilitaryBalanceStats(nationId));
        stats.put("energyBalance", plugin.getBalancingService().getEnergyBalanceStats(nationId));
        
        return stats;
    }
}