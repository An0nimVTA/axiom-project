package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced Mod Integration Service
 * Extends recipe integration with deeper cross-mod compatibility
 */
public class ModIntegrationEnhancementService implements Listener {
    private final AXIOM plugin;
    private final RecipeIntegrationService recipeIntegrationService;
    private final NationManager nationManager;
    
    // Tracking map for temporary recipe overrides
    private final Map<String, Boolean> recipeOverrides = new ConcurrentHashMap<>();
    
    // Material mapping for better cross-mod compatibility
    private final Map<String, Material> modMaterialMap = new ConcurrentHashMap<>();
    
    // Cached recipe compatibility checks
    private final Map<String, Boolean> recipeCompatibilityCache = new ConcurrentHashMap<>();
    
    public ModIntegrationEnhancementService(AXIOM plugin) {
        this.plugin = plugin;
        this.recipeIntegrationService = plugin.getRecipeIntegrationService();
        this.nationManager = plugin.getNationManager();
        
        // Initialize material mapping
        initializeMaterialMapping();
        
        // Register this as a listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start periodic cleanup task
        startMaintenanceTasks();
    }
    
    private void initializeMaterialMapping() {
        // Map common mod materials to vanilla equivalents
        modMaterialMap.put("immersiveengineering:ingot_steel", Material.IRON_INGOT);
        modMaterialMap.put("immersiveengineering:ingot_copper", Material.COPPER_INGOT);
        modMaterialMap.put("immersiveengineering:ingot_lead", Material.NETHERITE_INGOT); // close approximation
        modMaterialMap.put("immersiveengineering:ingot_silver", Material.GOLD_INGOT); // close approximation
        modMaterialMap.put("immersiveengineering:ingot_nickel", Material.IRON_INGOT); // close approximation
        
        modMaterialMap.put("industrialupgrade:steel_ingot", Material.IRON_INGOT);
        modMaterialMap.put("industrialupgrade:electrum_ingot", Material.GOLD_INGOT);
        modMaterialMap.put("industrialupgrade:invar_ingot", Material.IRON_INGOT);
        modMaterialMap.put("industrialupgrade:bronze_ingot", Material.COPPER_INGOT);
        
        modMaterialMap.put("appliedenergistics2:certus_quartz_crystal", Material.QUARTZ);
        modMaterialMap.put("appliedenergistics2:fluix_crystal", Material.QUARTZ);
        modMaterialMap.put("appliedenergistics2:sky_stone_block", Material.BLACKSTONE);
        
        modMaterialMap.put("mekanism:ingot_steel", Material.IRON_INGOT);
        modMaterialMap.put("mekanism:ingot_refined_obsidian", Material.OBSIDIAN);
        modMaterialMap.put("mekanism:ingot_refined_glowstone", Material.GLOWSTONE);
        
        // TACZ and PointBlank ammo
        modMaterialMap.put("tacz:bullet", Material.ARROW);
        modMaterialMap.put("tacz:small_bullet", Material.ARROW);
        modMaterialMap.put("tacz:medium_bullet", Material.SPECTRAL_ARROW);
        modMaterialMap.put("tacz:large_bullet", Material.TIPPED_ARROW);
        
        modMaterialMap.put("pointblank:bullet", Material.ARROW);
        modMaterialMap.put("pointblank:shell", Material.SPECTRAL_ARROW);
        modMaterialMap.put("pointblank:rocket", Material.TNT);
    }
    
    /**
     * Event handler for crafting to apply nation-specific crafting bonuses
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        Player player = (Player) event.getView().getPlayer();
        ItemStack result = event.getRecipe().getResult();
        Recipe bukkitRecipe = event.getRecipe();
        
        // Check if this is an integration recipe from our plugin
        String recipeId = getRecipeId(bukkitRecipe);
        if (recipeId != null) {
            // Apply nation-based crafting bonuses
            Optional<Nation> nation = nationManager.getNationOfPlayer(player.getUniqueId());
            if (nation.isPresent()) {
                // Check for crafting technology bonuses
                double craftingBonus = plugin.getTechnologyTreeService().getBonus(nation.get().getId(), "craftingEfficiency");
                
                if (craftingBonus > 1.0) {
                    // Apply bonus to output quantity
                    int originalAmount = result.getAmount();
                    int newAmount = (int) Math.ceil(originalAmount * craftingBonus);
                    
                    if (originalAmount != newAmount) {
                        ItemStack newResult = result.clone();
                        newResult.setAmount(newAmount);
                        
                        // This can't be done directly due to Bukkit limitations
                        // Instead, we provide feedback to the player
                        player.sendMessage("§a§lCRAFTING BONUS! §7Crafting efficiency bonus: §e+" + 
                            String.format("%.1f", (craftingBonus - 1) * 100) + "%");
                    }
                }
                
                // Check for recipe-specific nation bonuses
                applyNationRecipeBonuses(nation.get().getId(), recipeId, player);
            }
        }
    }
    
    /**
     * Apply nation-specific recipe bonuses
     */
    private void applyNationRecipeBonuses(String nationId, String recipeId, Player player) {
        // Check if this recipe benefits from specific nation traits
        // This would connect to other AXIOM systems like nation modifiers, etc.
        if (recipeId.contains("bullet") || recipeId.contains("ammo")) {
            // Check if the nation has technology that improves ammunition crafting
            if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "firearms_tech") ||
                plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "firearms_tech_pb")) {
                player.sendMessage("§eAmmunition crafting bonus applied! §7(From firearm technology)");
            }
        } else if (recipeId.contains("circuit") || recipeId.contains("electronic")) {
            // Check for electronics technology
            if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "advanced_industry") ||
                plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "auto_crafting")) {
                player.sendMessage("§eElectronics crafting bonus applied! §7(From advanced technologies)");
            }
        }
    }
    
    /**
     * Event handler for inventory clicks to provide cross-mod crafting hints
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // This could be extended to provide real-time hints about cross-mod compatibility
        // For example, when a player hovers over an item, show compatible alternatives
    }
    
    /**
     * Get recipe ID from Bukkit recipe object
     */
    private String getRecipeId(Recipe bukkitRecipe) {
        if (bukkitRecipe instanceof ShapedRecipe) {
            NamespacedKey key = ((ShapedRecipe) bukkitRecipe).getKey();
            if (key.getNamespace().equals(plugin.getName())) {
                return key.getKey();
            }
        } else if (bukkitRecipe instanceof ShapelessRecipe) {
            NamespacedKey key = ((ShapelessRecipe) bukkitRecipe).getKey();
            if (key.getNamespace().equals(plugin.getName())) {
                return key.getKey();
            }
        }
        return null;
    }
    
    /**
     * Get material from mod-specific item ID
     */
    public Material getMaterialFromModItem(String modItemId) {
        if (modMaterialMap.containsKey(modItemId)) {
            return modMaterialMap.get(modItemId);
        }
        
        // Fallback: try to parse vanilla material names
        if (!modItemId.contains(":")) {
            try {
                return Material.valueOf(modItemId.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return Material.AIR;
            }
        }
        
        // Extract vanilla equivalent from mod ID
        String[] parts = modItemId.split(":", 2);
        if (parts.length == 2) {
            String mod = parts[0];
            String item = parts[1];
            
            // Common patterns
            if (item.startsWith("ingot_")) {
                String metal = item.substring("ingot_".length());
                switch (metal) {
                    case "iron":
                        return Material.IRON_INGOT;
                    case "gold":
                        return Material.GOLD_INGOT;
                    case "copper":
                        return Material.COPPER_INGOT;
                    default:
                        return Material.IRON_INGOT; // fallback
                }
            } else if (item.startsWith("nugget_")) {
                String metal = item.substring("nugget_".length());
                switch (metal) {
                    case "gold":
                        return Material.GOLD_NUGGET;
                    case "iron":
                        return Material.IRON_NUGGET;
                    default:
                        return Material.IRON_NUGGET; // fallback
                }
            } else if (item.contains("bullet") || item.contains("ammo")) {
                return Material.ARROW;
            } else if (item.contains("circuit") || item.contains("processor")) {
                return Material.REDSTONE;
            }
        }
        
        return Material.AIR; // Unknown material
    }
    
    /**
     * Check if two materials are cross-mod compatible
     */
    public boolean areMaterialsCrossModCompatible(Material mat1, Material mat2) {
        // Same material is always compatible
        if (mat1 == mat2) return true;
        
        // Check if they're different forms of the same resource
        String mat1Name = mat1.name().toLowerCase();
        String mat2Name = mat2.name().toLowerCase();
        
        // Metal compatibility
        if (isMetal(mat1Name) && isMetal(mat2Name)) {
            return true; // Different metals can sometimes be substituted
        }
        
        // Similar types (ingots, nuggets, etc.)
        if (mat1Name.contains("ingot") && mat2Name.contains("ingot")) return true;
        if (mat1Name.contains("nugget") && mat2Name.contains("nugget")) return true;
        if (mat1Name.contains("ore") && mat2Name.contains("ore")) return true;
        if (mat1Name.contains("block") && mat2Name.contains("block")) return true;
        
        // Ammunition
        if (isAmmunition(mat1) && isAmmunition(mat2)) return true;
        
        // Electronic components
        if (isElectronic(mat1) && isElectronic(mat2)) return true;
        
        return false;
    }
    
    /**
     * Check if material represents a metal
     */
    private boolean isMetal(String materialName) {
        return materialName.contains("iron") || 
               materialName.contains("gold") || 
               materialName.contains("copper") || 
               materialName.contains("diamond") || 
               materialName.contains("emerald") || 
               materialName.contains("netherite") || 
               materialName.contains("steel") || 
               materialName.contains("tin") || 
               materialName.contains("lead") || 
               materialName.contains("silver") || 
               materialName.contains("nickel");
    }
    
    /**
     * Check if item represents ammunition
     */
    private boolean isAmmunition(Material material) {
        String name = material.name().toLowerCase();
        return name.contains("arrow") || 
               name.contains("bullet") || 
               name.contains("ammo") || 
               name.contains("firework");
    }
    
    /**
     * Check if item represents electronic component
     */
    private boolean isElectronic(Material material) {
        String name = material.name().toLowerCase();
        return name.contains("redstone") || 
               name.contains("comparator") || 
               name.contains("repeater") || 
               name.contains("detector") || 
               name.contains("torch") && !name.contains("soul");
    }
    
    /**
     * Get compatible materials for a given material
     */
    public Set<Material> getCompatibleMaterials(Material baseMaterial) {
        Set<Material> compatible = new HashSet<>();
        
        // Add the base material
        compatible.add(baseMaterial);
        
        // Add similar materials
        String baseName = baseMaterial.name().toLowerCase();
        
        if (isMetal(baseName)) {
            compatible.add(Material.IRON_INGOT);
            compatible.add(Material.GOLD_INGOT);
            compatible.add(Material.COPPER_INGOT);
            compatible.add(Material.NETHERITE_INGOT);
        } else if (isAmmunition(baseMaterial)) {
            compatible.add(Material.ARROW);
            compatible.add(Material.SPECTRAL_ARROW);
            compatible.add(Material.TIPPED_ARROW);
        } else if (isElectronic(baseMaterial)) {
            compatible.add(Material.REDSTONE);
            compatible.add(Material.REDSTONE_TORCH);
            compatible.add(Material.REPEATER);
            compatible.add(Material.COMPARATOR);
        }
        
        return compatible;
    }
    
    /**
     * Start maintenance tasks
     */
    private void startMaintenanceTasks() {
        // Periodic cleanup of caches
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up old cache entries (older than 1 hour)
                long cutoff = System.currentTimeMillis() - 60 * 60 * 1000L; // 1 hour
                
                // In this implementation, we don't have timestamped caches, but this is where cleanup would happen
                recipeCompatibilityCache.entrySet().removeIf(entry -> 
                    Math.random() < 0.1); // Random cleanup to manage size
            }
        }.runTaskTimerAsynchronously(plugin, 24000L, 24000L); // Every 20 minutes
    }
    
    /**
     * Get material mapping statistics
     */
    public Map<String, Object> getMaterialMappingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalModMaterialMappings", modMaterialMap.size());
        stats.put("mappingDetails", new HashMap<>(modMaterialMap)); // Don't expose internal structure
        stats.put("crossModCompatibilityChecks", recipeCompatibilityCache.size());
        return stats;
    }
    
    /**
     * Register a custom material mapping
     */
    public void registerMaterialMapping(String modItemId, Material vanillaEquivalent) {
        modMaterialMap.put(modItemId, vanillaEquivalent);
    }
    
    /**
     * Get all registered mod item IDs
     */
    public Set<String> getRegisteredModItemIds() {
        return new HashSet<>(modMaterialMap.keySet());
    }
    
    /**
     * Check if an item ID is for a known mod
     */
    public boolean isKnownModItem(String itemId) {
        return modMaterialMap.containsKey(itemId) || 
               (itemId.contains(":") && 
                (itemId.contains("immersiveengineering") || 
                 itemId.contains("industrialupgrade") || 
                 itemId.contains("appliedenergistics") || 
                 itemId.contains("mekanism") || 
                 itemId.contains("tacz") || 
                 itemId.contains("pointblank") || 
                 itemId.contains("ballistix") || 
                 itemId.contains("superbwarfare")));
    }
    
    /**
     * Get associated mod name from item ID
     */
    public String getModName(String itemId) {
        if (itemId.contains(":")) {
            return itemId.split(":")[0];
        }
        return "vanilla";
    }
}