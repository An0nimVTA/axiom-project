package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Advanced service for cross-mod recipe integration with deep monitoring
 * Tracks usage of cross-mod recipes and improves compatibility
 */
public class RecipeIntegrationService implements Listener {
    private final AXIOM plugin;
    private final Map<String, List<IntegrationRecipe>> integrationRecipes = new ConcurrentHashMap<>();
    private final Set<String> enabledIntegrations = ConcurrentHashMap.newKeySet();
    
    // Resource compatibility map using real mod item names
    private final Map<String, Set<String>> resourceCompatibility = new ConcurrentHashMap<>();
    
    // Usage tracking for recipe effectiveness
    private final Map<String, Integer> recipeUsageCount = new ConcurrentHashMap<>();
    private final Map<String, Long> recipeLastUsed = new ConcurrentHashMap<>();
    
    // Player-based crafting tracking for individual nation benefits
    private final Map<UUID, Map<String, Integer>> playerRecipeUsage = new ConcurrentHashMap<>();
    
    // Config file
    private FileConfiguration recipeConfig;
    private File recipeFile;
    
    public static class IntegrationRecipe {
        String id; // Unique recipe ID
        String modOrigin; // source mod
        String modTarget; // target mod
        String inputItemId; // input item (mod-specific ID)
        String outputItemId; // output item (mod-specific ID)
        String recipeType; // shaped, shapeless, furnace, etc.
        List<String> pattern; // for shaped recipes
        Map<Character, List<String>> ingredients; // for shaped recipes - each char maps to list of compatible items
        List<String> inputIngredients; // for shapeless recipes - list of compatible input items
        float experience; // for furnace recipes
        int cookingTime; // for furnace recipes
        int outputAmount; // amount of output items
        String category; // recipe category for organization
        boolean enabled; // whether recipe is enabled
        double successRate; // success rate based on usage data
        
        public IntegrationRecipe(String id, String modOrigin, String modTarget, String inputItemId, String outputItemId) {
            this.id = id;
            this.modOrigin = modOrigin;
            this.modTarget = modTarget;
            this.inputItemId = inputItemId;
            this.outputItemId = outputItemId;
            this.ingredients = new HashMap<>();
            this.inputIngredients = new ArrayList<>();
            this.outputAmount = 1; // default amount
            this.enabled = true;
            this.successRate = 1.0; // default 100% success rate
            this.category = "general";
        }
    }
    
    public RecipeIntegrationService(AXIOM plugin) {
        this.plugin = plugin;
        initializeConfig();
        initializeResourceCompatibility();
        loadCustomRecipes();
        
        // Enable common integrations by default
        enabledIntegrations.add("steel_cross_mod");
        enabledIntegrations.add("bullet_cross_mod");
        enabledIntegrations.add("circuit_cross_mod");
        enabledIntegrations.add("energy_component_cross_mod");
        enabledIntegrations.add("construction_materials");
        enabledIntegrations.add("fuel_integration");
        
        // Register recipes
        Bukkit.getScheduler().runTask(plugin, this::registerIntegrationRecipes);
        
        // Register this as a listener to handle dynamic recipe checks
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start periodic cleanup task
        startMaintenanceTasks();
    }
    
    private void initializeConfig() {
        recipeFile = new File(plugin.getDataFolder(), "recipe-integration.yml");
        if (!recipeFile.exists()) {
            plugin.saveResource("recipe-integration.yml", false);
        }
        recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);
    }
    
    /**
     * Initialize resource compatibility between mods using real item IDs
     */
    private void initializeResourceCompatibility() {
        // Steel ingots compatibility - allowing cross-mod usage
        Set<String> steelItems = new HashSet<>();
        steelItems.add("immersiveengineering:ingot_steel");
        steelItems.add("industrialupgrade:steel_ingot");
        steelItems.add("mekanism:ingot_steel");
        steelItems.add("techguns:steel_ingot");
        steelItems.add("minecraft:iron_ingot"); // Vanilla iron as basic alternative
        resourceCompatibility.put("steel_ingots", steelItems);
        
        // Electronic components compatibility
        Set<String> electronicComponents = new HashSet<>();
        electronicComponents.add("appliedenergistics2:certus_quartz_crystal");
        electronicComponents.add("appliedenergistics2:fluix_crystal");
        electronicComponents.add("appliedenergistics2:calculation_processor");
        electronicComponents.add("appliedenergistics2:engineering_processor");
        electronicComponents.add("appliedenergistics2:logic_processor");
        electronicComponents.add("immersiveengineering:component_electronic");
        electronicComponents.add("immersiveengineering:component_electronic_adv");
        electronicComponents.add("industrialupgrade:electronic_component");
        electronicComponents.add("industrialupgrade:advanced_electronic_component");
        electronicComponents.add("minecraft:redstone");
        electronicComponents.add("minecraft:gold_ingot");
        electronicComponents.add("minecraft:quartz");
        resourceCompatibility.put("electronic_components", electronicComponents);
        
        // Ammunition materials - bullet compatibility
        Set<String> ammoMaterials = new HashSet<>();
        ammoMaterials.add("tacz:bullet");
        ammoMaterials.add("tacz:small_bullet");
        ammoMaterials.add("tacz:medium_bullet");
        ammoMaterials.add("tacz:large_bullet");
        ammoMaterials.add("pointblank:bullet");
        ammoMaterials.add("pointblank:shell");
        ammoMaterials.add("pointblank:rocket");
        ammoMaterials.add("minecraft:arrow");
        ammoMaterials.add("minecraft:spectral_arrow");
        ammoMaterials.add("minecraft:tipped_arrow");
        ammoMaterials.add("minecraft:gunpowder");
        ammoMaterials.add("minecraft:fire_charge");
        resourceCompatibility.put("ammunition", ammoMaterials);
        
        // Energy materials - wiring and power components
        Set<String> energyMaterials = new HashSet<>();
        energyMaterials.add("immersiveengineering:wire_copper");
        energyMaterials.add("immersiveengineering:wire_electrum");
        energyMaterials.add("immersiveengineering:wire_steel");
        energyMaterials.add("industrialupgrade:copper_cable");
        energyMaterials.add("industrialupgrade:electrum_cable");
        energyMaterials.add("industrialupgrade:signalum_cable");
        energyMaterials.add("mekanism:basic_universal_cable");
        energyMaterials.add("mekanism:advanced_universal_cable");
        energyMaterials.add("mekanism:ultimate_universal_cable");
        energyMaterials.add("thermal:energy_duct");
        energyMaterials.add("minecraft:redstone");
        energyMaterials.add("minecraft:glowstone_dust");
        resourceCompatibility.put("energy_materials", energyMaterials);
        
        // Construction materials
        Set<String> constructionMaterials = new HashSet<>();
        constructionMaterials.add("immersiveengineering:steel_wallmount");
        constructionMaterials.add("immersiveengineering:connector_structural");
        constructionMaterials.add("industrialupgrade:reinforced_plate");
        constructionMaterials.add("minecraft:iron_block");
        constructionMaterials.add("minecraft:obsidian");
        constructionMaterials.add("minecraft:end_stone");
        constructionMaterials.add("minecraft:redstone_block");
        resourceCompatibility.put("construction_materials", constructionMaterials);
        
        // Advanced materials - alloys and compounds
        Set<String> advancedMaterials = new HashSet<>();
        advancedMaterials.add("industrialupgrade:electrum_ingot");
        advancedMaterials.add("immersiveengineering:ingot_electrum");
        advancedMaterials.add("industrialupgrade:invar_ingot");
        advancedMaterials.add("immersiveengineering:ingot_invar");
        advancedMaterials.add("industrialupgrade:bronze_ingot");
        advancedMaterials.add("immersiveengineering:ingot_bronze");
        advancedMaterials.add("mekanism:ingot_refined_obsidian");
        advancedMaterials.add("mekanism:ingot_refined_glowstone");
        resourceCompatibility.put("advanced_materials", advancedMaterials);
    }
    
    /**
     * Load custom recipes from configuration
     */
    private void loadCustomRecipes() {
        // Load from config file first
        loadRecipesFromConfig();
        
        // Add default fallback recipes if needed
        if (integrationRecipes.isEmpty()) {
            // Universal bullet recipe using cross-mod materials
            IntegrationRecipe bulletRecipe = new IntegrationRecipe(
                "universal_bullet", "multi", "firearms", "minecraft:gunpowder", "minecraft:arrow"
            );
            bulletRecipe.recipeType = "shapeless";
            bulletRecipe.inputIngredients.addAll(Arrays.asList(
                "minecraft:feather", "minecraft:stick", "minecraft:gunpowder",
                "tacz:bullet_casing", "pointblank:shell_casing"
            ));
            bulletRecipe.outputItemId = "minecraft:arrow";
            bulletRecipe.outputAmount = 16;
            bulletRecipe.category = "ammunition";
            integrationRecipes.computeIfAbsent("ammunition", k -> new ArrayList<>()).add(bulletRecipe);
            
            // Electronic component recipe using cross-mod materials
            IntegrationRecipe circuitRecipe = new IntegrationRecipe(
                "basic_circuit", "multi", "electronics", "minecraft:redstone", "minecraft:repeater"
            );
            circuitRecipe.recipeType = "shaped";
            circuitRecipe.pattern = Arrays.asList("R R", "SSS", "T T");
            circuitRecipe.ingredients.put('R', Arrays.asList("minecraft:redstone", "appliedenergistics2:certus_quartz_crystal"));
            circuitRecipe.ingredients.put('S', Arrays.asList("minecraft:cobblestone", "minecraft:stone", "immersiveengineering:stone_decoration"));
            circuitRecipe.ingredients.put('T', Arrays.asList("minecraft:redstone_torch", "industrialupgrade:electronic_component"));
            circuitRecipe.outputItemId = "minecraft:repeater";
            circuitRecipe.category = "electronics";
            integrationRecipes.computeIfAbsent("electronics", k -> new ArrayList<>()).add(circuitRecipe);
            
            // Steel processing recipe with cross-mod inputs
            IntegrationRecipe steelRecipe = new IntegrationRecipe(
                "steel_processing", "immersiveengineering", "industrialupgrade", 
                "minecraft:iron_ingot", "minecraft:netherite_ingot"
            );
            steelRecipe.recipeType = "shapeless";
            steelRecipe.inputIngredients.addAll(Arrays.asList(
                "minecraft:iron_ingot", "minecraft:coal",
                "immersiveengineering:coal_coke", "industrialupgrade:processed_coal"
            ));
            steelRecipe.outputItemId = "minecraft:netherite_ingot";
            steelRecipe.outputAmount = 1;
            steelRecipe.category = "metallurgy";
            integrationRecipes.computeIfAbsent("metallurgy", k -> new ArrayList<>()).add(steelRecipe);
        }
    }
    
    /**
     * Load recipes from configuration file
     */
    private void loadRecipesFromConfig() {
        if (recipeConfig == null) return;
        
        // Load custom recipes from config
        if (recipeConfig.contains("recipeIntegration.customRecipes")) {
            Set<String> recipeKeys = recipeConfig.getConfigurationSection("recipeIntegration.customRecipes").getKeys(false);
            
            for (String recipeKey : recipeKeys) {
                String path = "recipeIntegration.customRecipes." + recipeKey;
                
                if (recipeConfig.getBoolean(path + ".enabled", true)) {
                    String type = recipeConfig.getString(path + ".type", "shapeless").toLowerCase();
                    String result = recipeConfig.getString(path + ".result", "minecraft:stone");
                    int outputAmount = recipeConfig.getInt(path + ".outputAmount", 1);
                    String category = recipeConfig.getString(path + ".category", "general");
                    
                    IntegrationRecipe recipe = new IntegrationRecipe(
                        recipeKey, "config", "integration", "unknown_input", result
                    );
                    recipe.recipeType = type;
                    recipe.outputAmount = outputAmount;
                    recipe.category = category;
                    
                    switch (type) {
                        case "shaped":
                            // Load pattern
                            List<String> patternList = recipeConfig.getStringList(path + ".pattern");
                            if (patternList.isEmpty()) {
                                // Fallback for string pattern
                                String patternStr = recipeConfig.getString(path + ".pattern");
                                if (patternStr != null) {
                                    patternList = Arrays.asList(patternStr.split(","));
                                }
                            }
                            recipe.pattern = patternList;
                            
                            // Load ingredients
                            if (recipeConfig.contains(path + ".ingredients")) {
                                Set<String> ingredientKeys = recipeConfig.getConfigurationSection(path + ".ingredients").getKeys(false);
                                for (String ingredientKey : ingredientKeys) {
                                    List<String> items = recipeConfig.getStringList(path + ".ingredients." + ingredientKey);
                                    if (items.isEmpty()) {
                                        String item = recipeConfig.getString(path + ".ingredients." + ingredientKey);
                                        if (item != null) {
                                            items = Arrays.asList(item);
                                        }
                                    }
                                    recipe.ingredients.put(ingredientKey.charAt(0), items);
                                }
                            }
                            break;
                            
                        case "shapeless":
                            // Load inputs
                            recipe.inputIngredients.addAll(recipeConfig.getStringList(path + ".inputs"));
                            if (recipe.inputIngredients.isEmpty()) {
                                // Fallback for single input
                                String input = recipeConfig.getString(path + ".inputs");
                                if (input != null) {
                                    recipe.inputIngredients.addAll(Arrays.asList(input.split(",")));
                                }
                            }
                            break;
                            
                        case "furnace":
                        case "smelting":
                            String input = recipeConfig.getString(path + ".input", "minecraft:iron_ore");
                            recipe.inputIngredients.add(input);
                            recipe.experience = (float) recipeConfig.getDouble(path + ".experience", 0.7f);
                            recipe.cookingTime = recipeConfig.getInt(path + ".cookingTime", 200);
                            break;
                    }
                    
                    // Add to the appropriate category
                    integrationRecipes.computeIfAbsent(category, k -> new ArrayList<>()).add(recipe);
                }
            }
        }
        
        // Load resource substitution rules
        if (recipeConfig.contains("recipeIntegration.resourceSubstitution.interchangeable")) {
            List<Map<?, ?>> interchangeables = (List<Map<?, ?>>) recipeConfig.getList("recipeIntegration.resourceSubstitution.interchangeable");
            if (interchangeables != null) {
                for (Map<?, ?> interchange : interchangeables) {
                    String groupName = (String) interchange.get("groupName");
                    List<String> items = (List<String>) interchange.get("items");
                    if (groupName != null && items != null) {
                        resourceCompatibility.put(groupName, new HashSet<>(items));
                    }
                }
            }
        }
    }
    
    /**
     * Register all integration recipes
     */
    public void registerIntegrationRecipes() {
        unregisterIntegrationRecipes();
        
        // Register all defined integration recipes
        int registeredCount = 0;
        for (List<IntegrationRecipe> recipes : integrationRecipes.values()) {
            for (IntegrationRecipe recipe : recipes) {
                String integrationId = recipe.category + "_" + recipe.modOrigin + "_to_" + recipe.modTarget;
                if (enabledIntegrations.contains(recipe.id) || 
                    enabledIntegrations.contains(integrationId) || 
                    enabledIntegrations.contains(recipe.category)) {
                    if (recipe.enabled) {
                        registerRecipe(recipe);
                        registeredCount++;
                    }
                }
            }
        }
        
        plugin.getLogger().info("Registered " + registeredCount + " integration recipes");
    }
    
    /**
     * Register a specific integration recipe
     */
    private void registerRecipe(IntegrationRecipe integrationRecipe) {
        try {
            // Create output item
            ItemStack outputItem = createItemStack(integrationRecipe.outputItemId, integrationRecipe.outputAmount);
            
            // Add integration info to item
            ItemMeta meta = outputItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("§7Cross-Mod Recipe");
                lore.add("§7Source: " + integrationRecipe.modOrigin);
                lore.add("§7Produced by: " + integrationRecipe.modTarget);
                meta.setLore(lore);
                outputItem.setItemMeta(meta);
            }
            
            switch (integrationRecipe.recipeType.toLowerCase()) {
                case "shaped":
                    registerShapedRecipe(integrationRecipe, outputItem);
                    break;
                case "shapeless":
                    registerShapelessRecipe(integrationRecipe, outputItem);
                    break;
                case "furnace":
                case "smelting":
                    registerFurnaceRecipe(integrationRecipe, outputItem);
                    break;
                default:
                    plugin.getLogger().warning("Unknown recipe type: " + integrationRecipe.recipeType);
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register recipe " + integrationRecipe.id + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Register a shaped recipe
     */
    private void registerShapedRecipe(IntegrationRecipe integrationRecipe, ItemStack outputItem) {
        NamespacedKey key = new NamespacedKey(plugin, integrationRecipe.id);
        ShapedRecipe recipe = new ShapedRecipe(key, outputItem);
        
        // Set the pattern
        if (integrationRecipe.pattern != null && !integrationRecipe.pattern.isEmpty()) {
            recipe.shape(integrationRecipe.pattern.toArray(new String[0]));
        }
        
        // Set the ingredients with cross-mod compatibility
        for (Map.Entry<Character, List<String>> entry : integrationRecipe.ingredients.entrySet()) {
            Character position = entry.getKey();
            List<String> itemIds = entry.getValue();
            
            if (!itemIds.isEmpty()) {
                // Convert to Bukkit materials with fallbacks
                Material mainMaterial = getItemMaterial(itemIds.get(0));
                if (mainMaterial != Material.AIR) {
                    recipe.setIngredient(position, mainMaterial);
                } else {
                    // Create choice from all compatible materials
                    List<Material> compatMats = new ArrayList<>();
                    for (String itemId : itemIds) {
                        Material mat = getItemMaterial(itemId);
                        if (mat != Material.AIR) {
                            compatMats.add(mat);
                        }
                    }
                    if (!compatMats.isEmpty()) {
                        recipe.setIngredient(position, new org.bukkit.inventory.RecipeChoice.MaterialChoice(compatMats));
                    }
                }
            }
        }
        
        Bukkit.addRecipe(recipe);
    }
    
    /**
     * Register a shapeless recipe
     */
    private void registerShapelessRecipe(IntegrationRecipe integrationRecipe, ItemStack outputItem) {
        NamespacedKey key = new NamespacedKey(plugin, integrationRecipe.id);
        ShapelessRecipe recipe = new ShapelessRecipe(key, outputItem);
        
        // Add all compatible ingredients
        for (String itemId : integrationRecipe.inputIngredients) {
            Material material = getItemMaterial(itemId);
            if (material != Material.AIR) {
                recipe.addIngredient(material);
            } else {
                // Add compatible alternatives
                List<Material> compatMats = getCompatibleMaterialsAsBukkit(itemId);
                if (!compatMats.isEmpty()) {
                    recipe.addIngredient(new org.bukkit.inventory.RecipeChoice.MaterialChoice(compatMats));
                }
            }
        }
        
        Bukkit.addRecipe(recipe);
    }
    
    /**
     * Register a furnace recipe
     */
    private void registerFurnaceRecipe(IntegrationRecipe integrationRecipe, ItemStack outputItem) {
        NamespacedKey key = new NamespacedKey(plugin, integrationRecipe.id);
        Material inputMaterial = getItemMaterial(integrationRecipe.inputItemId);
        
        if (inputMaterial != Material.AIR) {
            org.bukkit.inventory.FurnaceRecipe recipe = new org.bukkit.inventory.FurnaceRecipe(key, outputItem, 
                inputMaterial, integrationRecipe.experience, integrationRecipe.cookingTime);
            Bukkit.addRecipe(recipe);
        }
    }
    
    /**
     * Convert mod-specific item ID to Bukkit Material
     */
    private Material getItemMaterial(String itemId) {
        // Handle mod-specific material names
        if (itemId.contains(":")) {
            String[] parts = itemId.split(":", 2);
            String modName = parts[0];
            String itemName = parts[1];
            
            // Map common mod materials to vanilla equivalents
            if (modName.equals("minecraft")) {
                try {
                    return Material.valueOf(itemName.toUpperCase().replace('-', '_'));
                } catch (IllegalArgumentException e) {
                    return Material.AIR;
                }
            } else if (modName.equals("immersiveengineering")) {
                // Common IE materials mapped to vanilla equivalents
                switch (itemName.toLowerCase()) {
                    case "ingot_steel": return Material.IRON_INGOT;
                    case "ingot_copper": return Material.COPPER_INGOT;
                    case "ingot_lead": return Material.NETHERITE_INGOT; // approximation
                    case "ingot_silver": return Material.GOLD_INGOT; // approximation
                    case "ingot_nickel": return Material.IRON_INGOT; // approximation
                    default: return Material.AIR;
                }
            } else if (modName.equals("industrialupgrade")) {
                switch (itemName.toLowerCase()) {
                    case "steel_ingot": return Material.IRON_INGOT;
                    case "electrum_ingot": return Material.GOLD_INGOT;
                    case "invar_ingot": return Material.IRON_INGOT;
                    case "bronze_ingot": return Material.COPPER_INGOT;
                    default: return Material.AIR;
                }
            } else if (modName.equals("appliedenergistics2")) {
                switch (itemName.toLowerCase()) {
                    case "certus_quartz_crystal": return Material.QUARTZ;
                    case "fluix_crystal": return Material.QUARTZ;
                    default: return Material.AIR;
                }
            } else if (modName.equals("tacz") || modName.equals("pointblank")) {
                return Material.ARROW; // Ammunition mapped to arrows
            }
        }
        
        // Fallback for vanilla materials
        if (!itemId.contains(":")) {
            try {
                return Material.valueOf(itemId.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return Material.AIR;
            }
        }
        
        return Material.AIR;
    }
    
    /**
     * Create ItemStack from mod-specific item ID
     */
    private ItemStack createItemStack(String itemId, int amount) {
        Material material = getItemMaterial(itemId);
        if (material != Material.AIR) {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(formatItemName(itemId));
                item.setItemMeta(meta);
            }
            return item;
        }
        
        // Fallback: create stone with ID as name
        ItemStack fallback = new ItemStack(Material.STONE, amount);
        ItemMeta meta = fallback.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(formatItemName(itemId));
            List<String> lore = new ArrayList<>();
            lore.add("§7External Mod Item");
            lore.add("§7ID: " + itemId);
            meta.setLore(lore);
            fallback.setItemMeta(meta);
        }
        return fallback;
    }
    
    /**
     * Format item name nicely
     */
    private String formatItemName(String itemId) {
        if (itemId.contains(":")) {
            String[] parts = itemId.split(":", 2);
            String modName = parts[0];
            String itemName = parts[1];
            // Convert to readable format
            String readableName = Arrays.stream(itemName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
            return "§6[" + modName + "] §f" + readableName;
        }
        return "§f" + itemId.replace('_', ' ');
    }
    
    /**
     * Get compatible materials as Bukkit Materials
     */
    private List<Material> getCompatibleMaterialsAsBukkit(String baseItemId) {
        List<Material> compatible = new ArrayList<>();
        
        for (Set<String> group : resourceCompatibility.values()) {
            if (group.contains(baseItemId)) {
                for (String itemId : group) {
                    Material material = getItemMaterial(itemId);
                    if (material != Material.AIR && !compatible.contains(material)) {
                        compatible.add(material);
                    }
                }
                break; // Found the group
            }
        }
        
        // If no specific compatibility found, look for type-based compatibility
        if (compatible.isEmpty()) {
            for (Map.Entry<String, Set<String>> entry : resourceCompatibility.entrySet()) {
                for (String itemId : entry.getValue()) {
                    if (itemId.contains("ingot_") && baseItemId.contains("ingot_")) {
                        Set<String> ingotGroup = entry.getValue();
                        for (String ingotId : ingotGroup) {
                            Material mat = getItemMaterial(ingotId);
                            if (mat != Material.AIR && !compatible.contains(mat)) {
                                compatible.add(mat);
                            }
                        }
                        break;
                    } else if ((itemId.contains("bullet") || itemId.contains("ammo")) && 
                              (baseItemId.contains("bullet") || baseItemId.contains("ammo"))) {
                        Set<String> ammoGroup = entry.getValue();
                        for (String ammoId : ammoGroup) {
                            Material mat = getItemMaterial(ammoId);
                            if (mat != Material.AIR && !compatible.contains(mat)) {
                                compatible.add(mat);
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        return compatible;
    }
    
    /**
     * Unregister integration recipes
     */
    public void unregisterIntegrationRecipes() {
        // Remove integration recipes added by this service
        List<NamespacedKey> toRemove = new ArrayList<>();
        for (Iterator<Recipe> iterator = Bukkit.recipeIterator(); iterator.hasNext();) {
            Recipe recipe = iterator.next();
            if (recipe instanceof ShapedRecipe) {
                NamespacedKey key = ((ShapedRecipe) recipe).getKey();
                if (key.getNamespace().equals(plugin.getName())) {
                    toRemove.add(key);
                }
            } else if (recipe instanceof ShapelessRecipe) {
                NamespacedKey key = ((ShapelessRecipe) recipe).getKey();
                if (key.getNamespace().equals(plugin.getName())) {
                    toRemove.add(key);
                }
            } else if (recipe instanceof org.bukkit.inventory.FurnaceRecipe) {
                NamespacedKey key = ((org.bukkit.inventory.FurnaceRecipe) recipe).getKey();
                if (key.getNamespace().equals(plugin.getName())) {
                    toRemove.add(key);
                }
            }
        }
        
        for (NamespacedKey key : toRemove) {
            Bukkit.removeRecipe(key);
        }
    }
    
    /**
     * Event handler for when a player crafts an item
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        Player player = (Player) event.getView().getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Check if this is one of our integration recipes
        String recipeId = getRecipeId(event.getRecipe());
        if (recipeId != null && integrationRecipes.values().stream()
            .flatMap(List::stream)
            .anyMatch(recipe -> recipe.id.equals(recipeId))) {
            
            // Record recipe usage
            recipeUsageCount.merge(recipeId, 1, Integer::sum);
            recipeLastUsed.put(recipeId, System.currentTimeMillis());
            
            // Record for the player
            playerRecipeUsage.computeIfAbsent(playerId, k -> new HashMap<>())
                .merge(recipeId, 1, Integer::sum);
            
            // Apply benefits for cross-mod crafting
            applyCrossModBenefits(player, recipeId);
            
            plugin.getLogger().info("Player " + player.getName() + " crafted integration recipe: " + recipeId);
        }
    }
    
    /**
     * Event handler for preparing craft (to provide dynamic integration hints)
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        // Could be extended to provide real-time feedback about available cross-mod recipes
        // For now, this is just a placeholder for future enhancements
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
        } else if (bukkitRecipe instanceof org.bukkit.inventory.FurnaceRecipe) {
            NamespacedKey key = ((org.bukkit.inventory.FurnaceRecipe) bukkitRecipe).getKey();
            if (key.getNamespace().equals(plugin.getName())) {
                return key.getKey();
            }
        }
        return null;
    }
    
    /**
     * Apply benefits for successful cross-mod crafting
     */
    private void applyCrossModBenefits(Player player, String recipeId) {
        IntegrationRecipe recipe = getRecipeById(recipeId);
        if (recipe != null) {
            // Send feedback message to player
            player.sendMessage("§a§lCRAFTING SUCCESS! §7Used " + recipe.modOrigin + " → " + recipe.modTarget + " integration");
            
            // Potentially apply bonuses to nation or player
            // This would connect to the nation system
            plugin.getNationManager().getNationOfPlayer(player.getUniqueId()).ifPresent(nation -> {
                // Example: Increase nation's integration level or provide experience
                // Would need to be integrated with nation XP system
            });
        }
    }
    
    /**
     * Start maintenance tasks
     */
    private void startMaintenanceTasks() {
        // Periodic cleanup of old usage data
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up old data older than 24 hours
                long cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L; // 24 hours
                
                Iterator<Map.Entry<String, Long>> iterator = recipeLastUsed.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Long> entry = iterator.next();
                    if (entry.getValue() < cutoff) {
                        iterator.remove();
                        recipeUsageCount.remove(entry.getKey());
                    }
                }
                
                // Clean up player data for offline players
                for (Iterator<Map.Entry<UUID, Map<String, Integer>>> playerIter = playerRecipeUsage.entrySet().iterator(); 
                     playerIter.hasNext();) {
                    Map.Entry<UUID, Map<String, Integer>> entry = playerIter.next();
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) {
                        // Keep for a while for statistics, but could clean up if needed
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 12000L, 12000L); // Every 10 minutes (12000 ticks)
    }
    
    /**
     * Get count of registered recipes
     */
    private int getRecipeCount() {
        int count = 0;
        for (List<IntegrationRecipe> recipes : integrationRecipes.values()) {
            count += recipes.size();
        }
        return count;
    }
    
    /**
     * Enable a specific integration
     */
    public void enableIntegration(String integrationId) {
        enabledIntegrations.add(integrationId);
        registerIntegrationRecipes(); // Re-register to include new integration
    }
    
    /**
     * Disable a specific integration
     */
    public void disableIntegration(String integrationId) {
        enabledIntegrations.remove(integrationId);
        registerIntegrationRecipes(); // Re-register without disabled integration
    }
    
    /**
     * Check if an integration is enabled
     */
    public boolean isIntegrationEnabled(String integrationId) {
        return enabledIntegrations.contains(integrationId);
    }
    
    /**
     * Get available integrations
     */
    public Set<String> getAvailableIntegrations() {
        return new HashSet<>(enabledIntegrations);
    }
    
    /**
     * Get integration recipe by ID
     */
    public IntegrationRecipe getRecipeById(String recipeId) {
        for (List<IntegrationRecipe> recipes : integrationRecipes.values()) {
            for (IntegrationRecipe recipe : recipes) {
                if (recipe.id.equals(recipeId)) {
                    return recipe;
                }
            }
        }
        return null;
    }
    
    /**
     * Get all integration recipes
     */
    public Map<String, List<IntegrationRecipe>> getAllIntegrationRecipes() {
        return new HashMap<>(integrationRecipes);
    }
    
    /**
     * Get resource compatibility for a category
     */
    public Set<String> getResourceCompatibility(String category) {
        return new HashSet<>(resourceCompatibility.getOrDefault(category, new HashSet<>()));
    }
    
    /**
     * Check if items are compatible for substitution
     */
    public boolean areItemsCompatible(String item1, String item2) {
        // Check each compatibility category
        for (Set<String> compatibilityGroup : resourceCompatibility.values()) {
            if (compatibilityGroup.contains(item1) && compatibilityGroup.contains(item2)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get integration statistics
     */
    public Map<String, Object> getIntegrationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("enabledIntegrations", enabledIntegrations.size());
        stats.put("totalRecipeMappings", getRecipeCount());
        stats.put("totalRecipesRegistered", Bukkit.getRecipesFor(new ItemStack(Material.STONE)).size()); // Approximation
        stats.put("availableIntegrations", new ArrayList<>(enabledIntegrations));
        
        // Recipe usage statistics
        stats.put("totalRecipeUsage", recipeUsageCount.values().stream().mapToInt(Integer::intValue).sum());
        stats.put("mostUsedRecipes", getMostUsedRecipes(10));
        
        // Compatibility groups info
        Map<String, List<String>> compatibilityInfo = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : resourceCompatibility.entrySet()) {
            compatibilityInfo.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        stats.put("compatibilityGroups", compatibilityInfo);
        
        // Player usage stats
        stats.put("activePlayersUsingIntegration", playerRecipeUsage.size());
        stats.put("recipeUsageCount", new HashMap<>(recipeUsageCount));
        
        return stats;
    }
    
    /**
     * Get most used recipes
     */
    private List<Map.Entry<String, Integer>> getMostUsedRecipes(int limit) {
        return recipeUsageCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Reload integration recipes
     */
    public void reloadRecipes() {
        unregisterIntegrationRecipes();
        loadCustomRecipes(); // Reload from config
        registerIntegrationRecipes();
    }
    
    /**
     * Get compatible items for a given item
     */
    public List<String> getCompatibleItems(String baseItem) {
        List<String> compatible = new ArrayList<>();
        
        for (Set<String> group : resourceCompatibility.values()) {
            if (group.contains(baseItem)) {
                compatible.addAll(group);
                break; // Found the group, no need to continue
            }
        }
        
        return compatible;
    }
    
    /**
     * Get recipe usage statistics
     */
    public Map<String, Object> getRecipeUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalUsage", recipeUsageCount.values().stream().mapToInt(Integer::intValue).sum());
        stats.put("uniqueRecipesUsed", recipeUsageCount.size());
        stats.put("currentActiveRecipes", integrationRecipes.size());
        
        // Top used recipes
        List<Map.Entry<String, Integer>> topRecipes = getMostUsedRecipes(10);
        stats.put("topUsedRecipes", topRecipes);
        
        return stats;
    }
    
    /**
     * Get player craft statistics
     */
    public Map<String, Object> getPlayerCraftStats(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Integer> playerRecipes = playerRecipeUsage.get(playerId);
        if (playerRecipes != null) {
            stats.put("totalIntegrationCrafts", playerRecipes.values().stream().mapToInt(Integer::intValue).sum());
            stats.put("uniqueRecipesUsed", playerRecipes.size());
            
            // Top recipes used by this player
            List<Map.Entry<String, Integer>> topRecipes = playerRecipes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());
            stats.put("topRecipes", topRecipes);
        } else {
            stats.put("totalIntegrationCrafts", 0);
            stats.put("uniqueRecipesUsed", 0);
            stats.put("topRecipes", Collections.emptyList());
        }
        
        return stats;
    }
    
    /**
     * Get all recipe usage data for all players
     */
    public Map<UUID, Map<String, Integer>> getAllPlayerRecipeUsage() {
        return new HashMap<>(playerRecipeUsage);
    }
}