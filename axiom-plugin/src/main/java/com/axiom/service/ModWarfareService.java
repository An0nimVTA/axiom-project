package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages mod-based warfare integration.
 * Tracks weapons, explosives, military vehicles, and their use in conflicts.
 */
public class ModWarfareService {
    private final AXIOM plugin;
    private final ModIntegrationService modIntegration;
    
    public ModWarfareService(AXIOM plugin, ModIntegrationService modIntegration) {
        this.plugin = plugin;
        this.modIntegration = modIntegration;
    }
    
    /**
     * Check if player is armed (has weapon from any warfare mod).
     */
    public boolean isPlayerArmed(Player player) {
        if (modIntegration == null) return false;
        return modIntegration.playerHasWeapon(player);
    }
    
    /**
     * Get weapon type from player's inventory.
     * Returns mod ID or null.
     */
    public String getPlayerWeaponType(Player player) {
        if (modIntegration == null) return null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String mod = modIntegration.detectModFromItem(item);
                if (mod != null && modIntegration.getWarfareMods().contains(mod)) {
                    return mod;
                }
            }
        }
        return null;
    }
    
    /**
     * Check if block is explosive/artillery (can damage in warzones).
     */
    public boolean isExplosive(Block block) {
        if (modIntegration == null) return false;
        return modIntegration.isExplosiveBlock(block);
    }
    
    /**
     * Calculate military strength bonus from mod equipment.
     * Returns multiplier (1.0 = no bonus, 2.0 = double strength).
     */
    public double getModMilitaryBonus(Player player) {
        if (modIntegration == null) return 1.0;
        double bonus = 1.0; // Start with base multiplier
        String weaponType = getPlayerWeaponType(player);
        
        if (weaponType != null) {
            // Different weapons provide different bonuses
            switch (weaponType) {
                case "tacz":
                case "pointblank":
                    bonus += 0.15; // +15% for firearms
                    break;
                case "superwarfare":
                    bonus += 0.25; // +25% for military vehicles/equipment
                    break;
                case "ballistix":
                    bonus += 0.20; // +20% for artillery
                    break;
            }
        }
        
        // Check for tactical equipment (CAPS Awims, Warium) in inventory
        boolean hasTacticalGear = false;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null) {
                String mod = modIntegration.detectModFromItem(item);
                if ("capsawims".equals(mod) || "warium".equals(mod)) {
                    hasTacticalGear = true;
                    break;
                }
            }
        }
        if (hasTacticalGear) {
            bonus += 0.10; // +10% for tactical armor
        }
        
        // Check nation technologies that enhance mod bonuses
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        String nationId = playerDataManager != null ? playerDataManager.getNation(player.getUniqueId()) : null;
        if (nationId != null) {
            TechnologyTreeService techService = plugin.getTechnologyTreeService();
            double techBonus = techService != null ? techService.getBonus(nationId, "warStrength") : 1.0;
            bonus *= techBonus; // Multiply by technology bonuses
        }
        
        return bonus;
    }
    
    /**
     * Get available warfare mods count for a nation.
     */
    public int getAvailableWarfareModsCount() {
        if (modIntegration == null) return 0;
        int count = 0;
        if (modIntegration.isTaczAvailable()) count++;
        if (modIntegration.isPointBlankAvailable()) count++;
        if (modIntegration.isSuperWarfareAvailable()) count++;
        if (modIntegration.isBallistixAvailable()) count++;
        return count;
    }
    
    /**
     * Check if player can use weapon in current context (warzone, PvP enabled, etc.).
     */
    public boolean canUseWeapon(Player player, String targetNationId) {
        // PvP completely allowed - no restrictions
        return true;
    }
    
    /**
     * Record weapon usage for statistics.
     */
    public void recordWeaponUsage(Player player, String weaponMod) {
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (nationId == null) return;
        
        // Track weapon usage (could be extended to StatisticsService later)
        plugin.getLogger().info("Weapon usage: " + weaponMod + " by nation " + nationId);
    }
    
    /**
     * Get comprehensive warfare mod statistics for a nation.
     */
    public Map<String, Object> getWarfareStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("availableMods", getAvailableWarfareModsCount());
        stats.put("hasTacz", modIntegration != null && modIntegration.isTaczAvailable());
        stats.put("hasPointBlank", modIntegration != null && modIntegration.isPointBlankAvailable());
        stats.put("hasSuperWarfare", modIntegration != null && modIntegration.isSuperWarfareAvailable());
        stats.put("hasBallistix", modIntegration != null && modIntegration.isBallistixAvailable());
        
        // Count armed players
        int armedCount = 0;
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            String playerNation = playerDataManager != null ? playerDataManager.getNation(player.getUniqueId()) : null;
            if (playerNation != null && playerNation.equals(nationId) && isPlayerArmed(player)) {
                armedCount++;
            }
        }
        stats.put("armedPlayers", armedCount);
        
        return stats;
    }
    
    /**
     * Get average military bonus for a nation (from all online players).
     */
    public double getAverageMilitaryBonus(String nationId) {
        double totalBonus = 0.0;
        int count = 0;
        
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            String playerNation = playerDataManager != null ? playerDataManager.getNation(player.getUniqueId()) : null;
            if (playerNation != null && playerNation.equals(nationId)) {
                totalBonus += getModMilitaryBonus(player);
                count++;
            }
        }
        
        return count > 0 ? totalBonus / count : 1.0;
    }
    
    /**
     * Calculate total warfare potential of a nation.
     */
    public double calculateWarfarePotential(String nationId) {
        double potential = 0.0;
        
        // Base potential from available mods
        potential += getAvailableWarfareModsCount() * 10.0;
        
        // Average military bonus
        potential += (getAverageMilitaryBonus(nationId) - 1.0) * 50.0;
        
        // Technology bonuses
        if (plugin.getTechnologyTreeService() != null) {
            double warTech = plugin.getTechnologyTreeService().getBonus(nationId, "warStrength");
            potential *= warTech;
        }
        
        return potential;
    }
    
    /**
     * Check if nation has advanced warfare capabilities.
     */
    public boolean hasAdvancedWarfare(String nationId) {
        return getAvailableWarfareModsCount() >= 2 && getAverageMilitaryBonus(nationId) >= 1.2;
    }
    
    /**
     * Get global mod warfare statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalModWarfareStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("availableMods", getAvailableWarfareModsCount());
        stats.put("hasTacz", modIntegration != null && modIntegration.isTaczAvailable());
        stats.put("hasPointBlank", modIntegration != null && modIntegration.isPointBlankAvailable());
        stats.put("hasSuperWarfare", modIntegration != null && modIntegration.isSuperWarfareAvailable());
        stats.put("hasBallistix", modIntegration != null && modIntegration.isBallistixAvailable());
        
        // Global armed players count
        int totalArmedPlayers = 0;
        Map<String, Integer> armedByNation = new HashMap<>();
        Map<String, Double> warfarePotentialByNation = new HashMap<>();
        Map<String, Double> averageBonusByNation = new HashMap<>();
        
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            String nationId = playerDataManager != null ? playerDataManager.getNation(player.getUniqueId()) : null;
            if (nationId != null && isPlayerArmed(player)) {
                totalArmedPlayers++;
                armedByNation.put(nationId, armedByNation.getOrDefault(nationId, 0) + 1);
            }
        }
        
        stats.put("totalArmedPlayers", totalArmedPlayers);
        stats.put("nationsWithArmedPlayers", armedByNation.size());
        stats.put("armedByNation", armedByNation);
        
        // Calculate warfare potential and bonuses for all nations
        for (com.axiom.model.Nation n : plugin.getNationManager().getAll()) {
            double potential = calculateWarfarePotential(n.getId());
            double avgBonus = getAverageMilitaryBonus(n.getId());
            warfarePotentialByNation.put(n.getId(), potential);
            averageBonusByNation.put(n.getId(), avgBonus);
        }
        
        stats.put("warfarePotentialByNation", warfarePotentialByNation);
        stats.put("averageBonusByNation", averageBonusByNation);
        
        // Top nations by warfare potential
        List<Map.Entry<String, Double>> topByPotential = warfarePotentialByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPotential", topByPotential);
        
        // Top nations by armed players
        List<Map.Entry<String, Integer>> topByArmed = armedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByArmed", topByArmed);
        
        // Nations with advanced warfare
        int nationsWithAdvanced = 0;
        for (com.axiom.model.Nation n : plugin.getNationManager().getAll()) {
            if (hasAdvancedWarfare(n.getId())) {
                nationsWithAdvanced++;
            }
        }
        stats.put("nationsWithAdvancedWarfare", nationsWithAdvanced);
        
        // Average warfare potential
        double totalPotential = warfarePotentialByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("averageWarfarePotential", warfarePotentialByNation.size() > 0 ? 
            totalPotential / warfarePotentialByNation.size() : 0);
        
        // Average military bonus
        double totalBonus = averageBonusByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("averageMilitaryBonus", averageBonusByNation.size() > 0 ? 
            totalBonus / averageBonusByNation.size() : 0);
        
        return stats;
    }
}

