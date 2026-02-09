package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Comprehensive mod integration service for Mohist servers.
 * Detects and provides hooks for all installed Forge mods.
 */
public class ModIntegrationService {
    private final AXIOM plugin;
    
    // Mod detection flags
    private final Map<String, Boolean> detectedMods = new HashMap<>();
    
    // Mod categories for easy querying
    private final Set<String> warfareMods = new HashSet<>(); // Tacz, PointBlank, SuperWarfare, Ballistix
    private final Set<String> industrialMods = new HashSet<>(); // IE, Industrial Upgrade, Simply Quarries
    private final Set<String> logisticsMods = new HashSet<>(); // AE2, Immersive Vehicles, AshVehicle
    private final Set<String> energyMods = new HashSet<>(); // IE, Quantum Generators, PowerUtils, Voltaic
    private final Set<String> equipmentMods = new HashSet<>(); // CAPS Awims, Curios, Warium
    private final Set<String> communicationMods = new HashSet<>(); // VoiceChat
    private final Set<String> mapMods = new HashSet<>(); // Xaeros Minimap, Xaeros World Map
    
    // Mod-specific namespaces and identifiers
    private final Map<String, Set<String>> modItemPatterns = new HashMap<>();
    private final Map<String, Set<String>> modBlockPatterns = new HashMap<>();
    
    public ModIntegrationService(AXIOM plugin) {
        this.plugin = plugin;
        initializePatterns();
        detectMods();
        logDetectionResults();
    }

    /**
     * Initialize detection patterns for all mods.
     */
    private void initializePatterns() {
        // Applied Energistics 2
        addPatterns("appliedenergistics2", "AE2_", "APPLIED_ENERGISTICS", "ME_");
        logisticsMods.add("appliedenergistics2");
        
        // CAPS Awims Tactical Equipment
        addPatterns("capsawims", "CAPS_", "AWIMS_", "TACTICAL_");
        equipmentMods.add("capsawims");
        
        // Curios API
        addPatterns("curios", "CURIOS_");
        equipmentMods.add("curios");
        
        // Immersive Engineering
        addPatterns("immersiveengineering", "IE_", "IMMERSIVE_ENGINEERING", "IMMERSIVE_");
        industrialMods.add("immersiveengineering");
        energyMods.add("immersiveengineering");
        warfareMods.add("immersiveengineering");
        
        // PointBlank
        addPatterns("pointblank", "POINTBLANK_", "PB_", "RIFLE", "PISTOL");
        warfareMods.add("pointblank");
        
        // SuperWarfare
        addPatterns("superwarfare", "SUPERWARFARE_", "SW_", "TANK", "DRONE", "BTR");
        warfareMods.add("superwarfare");
        
        // TL Skin Cape
        addPatterns("tlskincape", "TL_", "SKIN_CAPE");
        
        // Warium
        addPatterns("warium", "WARIUM_", "ELITE_");
        equipmentMods.add("warium");
        
        // AshVehicle
        addPatterns("ashvehicle", "ASH_", "MOTORCYCLE", "JEEP");
        logisticsMods.add("ashvehicle");
        
        // Immersive Vehicles
        addPatterns("immersivevehicles", "IV_", "VEHICLE_");
        logisticsMods.add("immersivevehicles");
        
        // Simply Quarries
        addPatterns("simplyquarries", "QUARRY", "SIMPLY_QUARRY");
        industrialMods.add("simplyquarries");
        
        // Tacz
        addPatterns("tacz", "TAZ_", "TACZ_", "GUN_", "_GUN");
        warfareMods.add("tacz");
        
        // Ballistix
        addPatterns("ballistix", "BALLISTIX_", "EXPLOSIVE_", "ARTILLERY");
        warfareMods.add("ballistix");
        
        // Industrial Upgrade
        addPatterns("industrialupgrade", "IU_", "INDUSTRIAL_UPGRADE", "QUANTUM_GEN");
        industrialMods.add("industrialupgrade");
        energyMods.add("industrialupgrade");
        
        // Quantum Generators
        addPatterns("quantumgenerators", "QUANTUM_GEN", "QUANTUM_GENERATOR");
        energyMods.add("quantumgenerators");
        
        // Voltaic
        addPatterns("voltaic", "VOLTAIC_", "ELECTRIC_");
        energyMods.add("voltaic");
        
        // PowerUtils
        addPatterns("powerutils", "POWER_UTILS", "ENERGY_MONITOR");
        energyMods.add("powerutils");
        
        // MTS Official Pack (for Immersive Vehicles)
        addPatterns("mts", "MTS_", "OFFICIAL_PACK");
        logisticsMods.add("mts");
    }
    
    private void addPatterns(String modId, String... patterns) {
        Set<String> itemPatterns = modItemPatterns.computeIfAbsent(modId, k -> new HashSet<>());
        Set<String> blockPatterns = modBlockPatterns.computeIfAbsent(modId, k -> new HashSet<>());
        for (String pattern : patterns) {
            itemPatterns.add(pattern);
            blockPatterns.add(pattern);
        }
    }

    /**
     * Enhanced mod detection using multiple methods.
     */
    private void detectMods() {
        detectViaMaterial();
        detectViaReflection();
        detectViaServerPlugins();
    }
    
    /**
     * Method 1: Try Material enum values (works for some mods on Mohist).
     */
    private void detectViaMaterial() {
        String[] testMaterials = {
            "AE2_CRAFTING_TERMINAL", // Applied Energistics 2
            "IE_METAL_PRESS", // Immersive Engineering
            "TAZ_GUN", // Tacz
            "BALLISTIX_EXPLOSIVE", // Ballistix
            "QUANTUM_GENERATOR", // Quantum Generators
            "QUARRY" // Simply Quarries
        };
        
        for (String mat : testMaterials) {
            try {
                Material.valueOf(mat);
                // Identify mod from material name
                if (mat.contains("AE2") || mat.contains("ME_")) {
                    detectedMods.put("appliedenergistics2", true);
                } else if (mat.contains("IE_")) {
                    detectedMods.put("immersiveengineering", true);
                } else if (mat.contains("TAZ") || mat.contains("TACZ")) {
                    detectedMods.put("tacz", true);
                } else if (mat.contains("BALLISTIX")) {
                    detectedMods.put("ballistix", true);
                } else if (mat.contains("QUANTUM")) {
                    detectedMods.put("quantumgenerators", true);
                } else if (mat.contains("QUARRY")) {
                    detectedMods.put("simplyquarries", true);
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * Method 2: Try reflection-based detection (check for mod classes).
     */
    private void detectViaReflection() {
        Map<String, String> modClasses = new HashMap<>();
        modClasses.put("appliedenergistics2", "appeng.core.Api");
        modClasses.put("immersiveengineering", "blusunrize.immersiveengineering.common.IEContent");
        modClasses.put("tacz", "com.tacz.core.ModMain");
        modClasses.put("pointblank", "net.pointblank.PointBlank");
        modClasses.put("simplyquarries", "mod.simplyquarries.SimplyQuarries");
        modClasses.put("ballistix", "com.ballistix.BallistixMod");
        modClasses.put("industrialupgrade", "com.industrialupgrade.IndustrialUpgrade");
        modClasses.put("immersivevehicles", "com.immersivevehicles.ImmersiveVehicles");
        modClasses.put("ashvehicle", "com.ashvehicle.AshVehicle");
        modClasses.put("voicechat", "de.maxhenkel.voicechat.plugins.Plugin");
        
        for (Map.Entry<String, String> entry : modClasses.entrySet()) {
            try {
                Class.forName(entry.getValue());
                detectedMods.put(entry.getKey(), true);
            } catch (ClassNotFoundException ignored) {}
        }
    }
    
    /**
     * Method 3: Check for mod plugins (if available on Mohist).
     */
    private void detectViaServerPlugins() {
        // Some mods may register as plugins
        String[] pluginNames = {
            "VoiceChat", "voicechat",
            "XaerosMinimap", "xaerosminimap",
            "XaerosWorldMap", "xaerosworldmap"
        };
        
        for (String name : pluginNames) {
            if (Bukkit.getPluginManager().getPlugin(name) != null) {
                String modId = name.toLowerCase();
                detectedMods.put(modId, true);
                if (name.contains("Minimap") || name.contains("WorldMap")) {
                    mapMods.add(modId);
                }
                if (name.contains("VoiceChat") || name.contains("voicechat")) {
                    communicationMods.add(modId);
                }
            }
        }
    }
    
    /**
     * Runtime detection: Check if an item is from a mod.
     */
    public String detectModFromItem(ItemStack item) {
        if (item == null) return null;
        String itemType = item.getType().name().toUpperCase();
        
        for (Map.Entry<String, Set<String>> entry : modItemPatterns.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (itemType.contains(pattern)) {
                    return entry.getKey();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if a block is from a specific mod.
     */
    public String detectModFromBlock(Block block) {
        if (block == null) return null;
        String blockName = block.getType().name().toUpperCase();
        
        for (Map.Entry<String, Set<String>> entry : modBlockPatterns.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (blockName.contains(pattern)) {
                    return entry.getKey();
                }
            }
        }
        
        return null;
    }
    
    private void logDetectionResults() {
        StringBuilder sb = new StringBuilder("Mod Detection Results: ");
        int count = 0;
        for (Map.Entry<String, Boolean> e : detectedMods.entrySet()) {
            if (e.getValue()) {
                sb.append(e.getKey()).append(", ");
                count++;
            }
        }
        if (plugin != null) {
            plugin.getLogger().info("Detected " + count + " mods: " + sb.toString());
        }
    }
    
    // ========== GENERAL QUERIES ==========
    
    public boolean isModAvailable(String modId) {
        if (modId == null) return false;
        return detectedMods.getOrDefault(modId.toLowerCase(), false);
    }
    
    public Set<String> getDetectedMods() {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Boolean> e : detectedMods.entrySet()) {
            if (e.getValue()) result.add(e.getKey());
        }
        return result;
    }
    
    // ========== CATEGORY QUERIES ==========
    
    public boolean hasWarfareMods() { return !warfareMods.isEmpty(); }
    public boolean hasIndustrialMods() { return !industrialMods.isEmpty(); }
    public boolean hasLogisticsMods() { return !logisticsMods.isEmpty(); }
    public boolean hasEnergyMods() { return !energyMods.isEmpty(); }
    public boolean hasEquipmentMods() { return !equipmentMods.isEmpty(); }
    public boolean hasCommunicationMods() { return !communicationMods.isEmpty(); }
    public boolean hasMapMods() { return !mapMods.isEmpty(); }
    
    public Set<String> getWarfareMods() { return new HashSet<>(warfareMods); }
    public Set<String> getIndustrialMods() { return new HashSet<>(industrialMods); }
    public Set<String> getLogisticsMods() { return new HashSet<>(logisticsMods); }
    
    // ========== SPECIFIC MOD GETTERS ==========
    
    public boolean isTaczAvailable() { return isModAvailable("tacz"); }
    public boolean isIEAvailable() { return isModAvailable("immersiveengineering"); }
    public boolean isAE2Available() { return isModAvailable("appliedenergistics2"); }
    public boolean isBallistixAvailable() { return isModAvailable("ballistix"); }
    public boolean isSuperWarfareAvailable() { return isModAvailable("superwarfare"); }
    public boolean isPointBlankAvailable() { return isModAvailable("pointblank"); }
    public boolean isSimplyQuarriesAvailable() { return isModAvailable("simplyquarries"); }
    public boolean isIndustrialUpgradeAvailable() { return isModAvailable("industrialupgrade"); }
    public boolean isQuantumGeneratorsAvailable() { return isModAvailable("quantumgenerators"); }
    public boolean isVoiceChatAvailable() { return isModAvailable("voicechat"); }
    
    // ========== MOD-SPECIFIC INTEGRATION METHODS ==========
    
    /**
     * Warfare: Check if player has weapon from any warfare mod.
     */
    public boolean playerHasWeapon(Player player) {
        if (player == null) return false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String mod = detectModFromItem(item);
                if (mod != null && warfareMods.contains(mod)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Warfare: Check if block is explosive/artillery.
     */
    public boolean isExplosiveBlock(Block block) {
        String mod = detectModFromBlock(block);
        return "ballistix".equals(mod) || "superwarfare".equals(mod);
    }
    
    /**
     * Industrial: Check if block is industrial machine.
     */
    public boolean isIndustrialMachine(Block block) {
        String mod = detectModFromBlock(block);
        return mod != null && (industrialMods.contains(mod) || energyMods.contains(mod));
    }
    
    /**
     * Logistics: Check if block is logistics/network component.
     */
    public boolean isLogisticsBlock(Block block) {
        String mod = detectModFromBlock(block);
        return "appliedenergistics2".equals(mod) || logisticsMods.contains(mod);
    }
    
    /**
     * Energy: Check if block is energy generator/storage.
     */
    public boolean isEnergyBlock(Block block) {
        String mod = detectModFromBlock(block);
        return mod != null && energyMods.contains(mod);
    }
    
    /**
     * Resource: Check if block is quarry/resource extractor.
     */
    public boolean isResourceExtractor(Block block) {
        String mod = detectModFromBlock(block);
        return "simplyquarries".equals(mod);
    }
    
    /**
     * Register custom mod item/block for runtime detection.
     */
    public void registerModItem(String modId, String itemNamespace) {
        if (modId == null || itemNamespace == null) return;
        modItemPatterns.computeIfAbsent(modId, k -> new HashSet<>()).add(itemNamespace.toUpperCase());
    }
    
    public void registerModBlock(String modId, String blockNamespace) {
        if (modId == null || blockNamespace == null) return;
        modBlockPatterns.computeIfAbsent(modId, k -> new HashSet<>()).add(blockNamespace.toUpperCase());
    }
    
    /**
     * Get comprehensive mod integration statistics.
     */
    public Map<String, Object> getModIntegrationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalDetected", getDetectedMods().size());
        stats.put("warfareMods", warfareMods.size());
        stats.put("industrialMods", industrialMods.size());
        stats.put("logisticsMods", logisticsMods.size());
        stats.put("energyMods", energyMods.size());
        stats.put("equipmentMods", equipmentMods.size());
        stats.put("communicationMods", communicationMods.size());
        stats.put("mapMods", mapMods.size());
        
        stats.put("detectedModsList", new HashSet<>(getDetectedMods()));
        stats.put("hasWarfare", hasWarfareMods());
        stats.put("hasIndustrial", hasIndustrialMods());
        stats.put("hasLogistics", hasLogisticsMods());
        stats.put("hasEnergy", hasEnergyMods());
        
        return stats;
    }
    
    /**
     * Get mods by category.
     */
    public Set<String> getModsByCategory(String category) {
        if (category == null) return new HashSet<>();
        switch (category.toLowerCase()) {
            case "warfare":
                return getWarfareMods();
            case "industrial":
                return getIndustrialMods();
            case "logistics":
                return getLogisticsMods();
            case "energy":
                return new HashSet<>(energyMods);
            case "equipment":
                return new HashSet<>(equipmentMods);
            case "communication":
                return new HashSet<>(communicationMods);
            case "map":
                return new HashSet<>(mapMods);
            default:
                return new HashSet<>();
        }
    }
    
    /**
     * Check if any mod from a category is available.
     */
    public boolean hasAnyModFromCategory(String category) {
        Set<String> mods = getModsByCategory(category);
        return mods.stream().anyMatch(this::isModAvailable);
    }
    
    /**
     * Get mod compatibility level (for version checks in future).
     */
    public String getModCompatibilityLevel(String modId) {
        if (!isModAvailable(modId)) return "NOT_AVAILABLE";
        // Future: could check mod versions
        return "AVAILABLE";
    }
    
    /**
     * Get global mod integration statistics (alias for consistency).
     */
    public Map<String, Object> getGlobalModIntegrationStatistics() {
        return getModIntegrationStatistics();
    }
}
