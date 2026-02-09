package com.axiom.api;

import com.axiom.AXIOM;
import com.axiom.domain.service.infrastructure.ModIntegrationService;
import com.axiom.domain.service.infrastructure.ModResourceService;
import com.axiom.domain.service.infrastructure.ModWarfareService;
import com.axiom.domain.service.infrastructure.ModEnergyService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * API interface for comprehensive mod integration.
 * Provides unified access to all mod-related services and functionality.
 */
public class ModIntegrationAPI {
    private final AXIOM plugin;
    private final ModIntegrationService modIntegrationService;
    private final ModResourceService modResourceService;
    private final ModWarfareService modWarfareService;
    private final ModEnergyService modEnergyService;

    public ModIntegrationAPI(AXIOM plugin) {
        this.plugin = plugin;
        this.modIntegrationService = plugin.getModIntegrationService();
        this.modResourceService = plugin.getModResourceService();
        this.modWarfareService = plugin.getModWarfareService();
        this.modEnergyService = plugin.getModEnergyService();
    }

    // ========== MOD DETECTION ==========

    /**
     * Check if a specific mod is available on the server.
     */
    public boolean isModAvailable(String modId) {
        return modIntegrationService.isModAvailable(modId);
    }

    /**
     * Get all detected mods.
     */
    public Set<String> getDetectedMods() {
        return modIntegrationService.getDetectedMods();
    }

    /**
     * Detect the mod ID from an item stack.
     */
    public String detectModFromItem(ItemStack item) {
        return modIntegrationService.detectModFromItem(item);
    }

    /**
     * Detect the mod ID from a block.
     */
    public String detectModFromBlock(Block block) {
        return modIntegrationService.detectModFromBlock(block);
    }

    // ========== MOD CATEGORIES ==========

    /**
     * Check if server has warfare mods.
     */
    public boolean hasWarfareMods() {
        return modIntegrationService.hasWarfareMods();
    }

    /**
     * Check if server has industrial mods.
     */
    public boolean hasIndustrialMods() {
        return modIntegrationService.hasIndustrialMods();
    }

    /**
     * Check if server has logistics mods.
     */
    public boolean hasLogisticsMods() {
        return modIntegrationService.hasLogisticsMods();
    }

    /**
     * Check if server has energy mods.
     */
    public boolean hasEnergyMods() {
        return modIntegrationService.hasEnergyMods();
    }

    /**
     * Get specific mod categories.
     */
    public Set<String> getWarfareMods() {
        return modIntegrationService.getWarfareMods();
    }

    public Set<String> getIndustrialMods() {
        return modIntegrationService.getIndustrialMods();
    }

    // ========== MOD RESOURCES ==========

    /**
     * Record mod item extraction for national resource tracking.
     */
    public void recordModItemExtraction(String nationId, ItemStack item) {
        modResourceService.recordModItemExtraction(nationId, item);
    }

    /**
     * Get resource statistics for a nation including mod resources.
     */
    public Map<String, Object> getResourceStatistics(String nationId) {
        return modResourceService.getResourceStatistics(nationId);
    }

    /**
     * Get global mod resource statistics across all nations.
     */
    public Map<String, Object> getGlobalModResourceStatistics() {
        return modResourceService.getGlobalModResourceStatistics();
    }

    /**
     * Check if a block is a protected mod resource.
     */
    public boolean isProtectedModResource(Block block, String nationId) {
        return modResourceService.isProtectedModResource(block, nationId);
    }

    // ========== MOD WARFARE ==========

    /**
     * Check if player is armed with mod weapons.
     */
    public boolean isPlayerArmed(Player player) {
        return modWarfareService.isPlayerArmed(player);
    }

    /**
     * Get the type of weapon a player is carrying.
     */
    public String getPlayerWeaponType(Player player) {
        return modWarfareService.getPlayerWeaponType(player);
    }

    /**
     * Get military bonus from mod equipment.
     */
    public double getModMilitaryBonus(Player player) {
        return modWarfareService.getModMilitaryBonus(player);
    }

    /**
     * Get warfare statistics for a nation.
     */
    public Map<String, Object> getWarfareStatistics(String nationId) {
        return modWarfareService.getWarfareStatistics(nationId);
    }

    /**
     * Get global mod warfare statistics across all nations.
     */
    public Map<String, Object> getGlobalModWarfareStatistics() {
        return modWarfareService.getGlobalModWarfareStatistics();
    }

    /**
     * Calculate warfare potential of a nation.
     */
    public double calculateWarfarePotential(String nationId) {
        return modWarfareService.calculateWarfarePotential(nationId);
    }

    // ========== MOD ENERGY ==========

    /**
     * Check if block is an energy generator.
     */
    public boolean isEnergyGenerator(Block block) {
        return modEnergyService.isEnergyGenerator(block);
    }

    /**
     * Get energy production for a nation.
     */
    public double getEnergyProduction(String nationId) {
        return modEnergyService.getEnergyProduction(nationId);
    }

    /**
     * Get energy statistics for a nation.
     */
    public Map<String, Object> getEnergyStatistics(String nationId) {
        return modEnergyService.getEnergyStatistics(nationId);
    }

    /**
     * Get global mod energy statistics across all nations.
     */
    public Map<String, Object> getGlobalModEnergyStatistics() {
        return modEnergyService.getGlobalModEnergyStatistics();
    }

    /**
     * Check if nation has sufficient energy.
     */
    public boolean hasSufficientEnergy(String nationId, double required) {
        return modEnergyService.hasSufficientEnergy(nationId, required);
    }

    /**
     * Calculate energy production potential (if all mods were fully utilized).
     */
    public double getMaximumEnergyPotential(String nationId) {
        return modEnergyService.getMaximumEnergyPotential(nationId);
    }

    /**
     * Check if nation can export energy to others.
     */

    // ========== ADVANCED INTEGRATION ==========

    /**
     * Calculate composite power index (combining resources, warfare, energy).
     */
    public double calculateCompositePowerIndex(String nationId) {
        double resourceFactor = 0.0;
        double warfareFactor = 0.0;
        double energyFactor = 0.0;

        // Calculate resource factor
        Map<String, Object> resourceStats = getResourceStatistics(nationId);
        Double totalResourceValue = (Double) resourceStats.getOrDefault("totalResourceValue", 0.0);
        resourceFactor = totalResourceValue / 1000.0; // Normalize

        // Calculate warfare factor
        double warfarePotential = calculateWarfarePotential(nationId);
        warfareFactor = warfarePotential / 100.0; // Normalize

        // Calculate energy factor
        double energyProduction = getEnergyProduction(nationId);
        energyFactor = energyProduction / 1000.0; // Normalize

        // Weighted composite score
        return (resourceFactor * 0.3) + (warfareFactor * 0.4) + (energyFactor * 0.3);
    }

    /**
     * Get composite statistics for a nation.
     */
    public Map<String, Object> getCompositeStatistics(String nationId) {
        Map<String, Object> stats = getResourceStatistics(nationId);
        stats.putAll(getWarfareStatistics(nationId));
        stats.putAll(getEnergyStatistics(nationId));

        stats.put("compositePowerIndex", calculateCompositePowerIndex(nationId));

        return stats;
    }

    /**
     * Get advanced metrics for economic indicators.
     */
    public Map<String, Object> getAdvancedEconomicMetrics(String nationId) {
        Map<String, Object> metrics = getEnergyStatistics(nationId);

        // Add resource efficiency
        double energyProduction = getEnergyProduction(nationId);
        double resourceValue = (Double) getResourceStatistics(nationId).getOrDefault("totalResourceValue", 0.0);
        double efficiency = resourceValue > 0 ? energyProduction / resourceValue : 0;
        metrics.put("resourceToEnergyEfficiency", efficiency);

        // Add industrial potential
        double industrialPotential = hasIndustrialMods() ? energyProduction * 1.2 : energyProduction;
        metrics.put("industrialCapacity", industrialPotential);

        // Add logistics efficiency
        double logisticsFactor = hasLogisticsMods() ? 1.1 : 1.0;
        metrics.put("logisticsEfficiency", logisticsFactor);

        return metrics;
    }

    /**
     * Register custom mod item/block patterns for runtime detection.
     */
    public void registerCustomModItem(String modId, String itemNamespace) {
        modIntegrationService.registerModItem(modId, itemNamespace);
    }

    public void registerCustomModBlock(String modId, String blockNamespace) {
        modIntegrationService.registerModBlock(modId, blockNamespace);
    }
}