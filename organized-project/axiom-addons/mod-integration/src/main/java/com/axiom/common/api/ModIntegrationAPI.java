package com.axiom.common.api;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * Common API interface for mod integration across AXIOM components.
 * Defines the contract for mod interaction without direct dependency on main AXIOM plugin.
 */
public interface ModIntegrationAPI {
    // Mod detection methods
    boolean isModAvailable(String modId);
    Set<String> getDetectedMods();
    String detectModFromItem(ItemStack item);
    String detectModFromBlock(Block block);

    // Mod category queries
    boolean hasWarfareMods();
    boolean hasIndustrialMods();
    boolean hasLogisticsMods();
    boolean hasEnergyMods();
    boolean hasCommunicationMods();
    boolean hasComputerMods();

    // Resource tracking
    void recordModItemExtraction(String nationId, ItemStack item);
    Map<String, Object> getResourceStatistics(String nationId);
    Map<String, Object> getGlobalModResourceStatistics();

    // Warfare integration
    boolean isPlayerArmed(Player player);
    String getPlayerWeaponType(Player player);
    double getModMilitaryBonus(Player player);
    Map<String, Object> getWarfareStatistics(String nationId);
    Map<String, Object> getGlobalModWarfareStatistics();
    double calculateWarfarePotential(String nationId);

    // Energy integration
    boolean isEnergyGenerator(Block block);
    double getEnergyProduction(String nationId);
    Map<String, Object> getEnergyStatistics(String nationId);
    Map<String, Object> getGlobalModEnergyStatistics();
    boolean hasSufficientEnergy(String nationId, double required);

    // Composite metrics
    double calculateCompositePowerIndex(String nationId);
    Map<String, Object> getCompositeStatistics(String nationId);
    Map<String, Object> getAdvancedEconomicMetrics(String nationId);

    // Custom integration
    void registerCustomModItem(String modId, String itemNamespace);
    void registerCustomModBlock(String modId, String blockNamespace);
}