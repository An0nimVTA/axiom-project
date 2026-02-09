package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.domain.service.infrastructure.RecipeIntegrationService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Advanced command for recipe integration management
 */
public class RecipeIntegrationCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final RecipeIntegrationService recipeIntegrationService;

    public RecipeIntegrationCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.recipeIntegrationService = plugin.getRecipeIntegrationService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.recipeintegration.use")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleListCommand(sender);
            case "enable":
                return handleEnableCommand(sender, args);
            case "disable":
                return handleDisableCommand(sender, args);
            case "stats":
                return handleStatsCommand(sender);
            case "usage":
                return handleUsageStatsCommand(sender);
            case "player":
                return handlePlayerStatsCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            case "check":
                return handleCheckCommand(sender, args);
            case "search":
                return handleSearchCommand(sender, args);
            case "compat":
                return handleCompatibilityCommand(sender, args);
            case "recipes":
                return handleRecipesCommand(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Recipe Integration Commands" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration list" + ChatColor.WHITE + " - List available integrations");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration recipes [category]" + ChatColor.WHITE + " - List integration recipes");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration enable <integration>" + ChatColor.WHITE + " - Enable an integration");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration disable <integration>" + ChatColor.WHITE + " - Disable an integration");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration stats" + ChatColor.WHITE + " - Show integration statistics");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration usage" + ChatColor.WHITE + " - Show recipe usage statistics");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration player [playerName]" + ChatColor.WHITE + " - Show player crafting statistics");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration reload" + ChatColor.WHITE + " - Reload integration recipes");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration check <item1> <item2>" + ChatColor.WHITE + " - Check compatibility between items");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration compat <item>" + ChatColor.WHITE + " - Show compatible items for an item");
        sender.sendMessage(ChatColor.YELLOW + "/recipeintegration search <keyword>" + ChatColor.WHITE + " - Search for recipes by keyword");
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.recipeint.list")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, List<RecipeIntegrationService.IntegrationRecipe>> allRecipes = recipeIntegrationService.getAllIntegrationRecipes();
        Set<String> enabledIntegrations = recipeIntegrationService.getAvailableIntegrations();

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Recipe Integrations" + ChatColor.GOLD + " ===");
        
        for (Map.Entry<String, List<RecipeIntegrationService.IntegrationRecipe>> entry : allRecipes.entrySet()) {
            String category = entry.getKey();
            List<RecipeIntegrationService.IntegrationRecipe> recipes = entry.getValue();
            String status = enabledIntegrations.contains(category) ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
            sender.sendMessage(ChatColor.AQUA + category + ChatColor.GRAY + " - " + 
                ChatColor.WHITE + recipes.size() + " recipes - " + status);
        }

        return true;
    }

    private boolean handleRecipesCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.recipeint.list")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, List<RecipeIntegrationService.IntegrationRecipe>> allRecipes = recipeIntegrationService.getAllIntegrationRecipes();
        
        if (args.length > 1) {
            // Show recipes for specific category
            String category = args[1].toLowerCase();
            List<RecipeIntegrationService.IntegrationRecipe> recipes = allRecipes.get(category);
            
            if (recipes == null || recipes.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No recipes found for category: " + category);
                return true;
            }
            
            sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Recipes for " + category + ChatColor.GOLD + " ===");
            for (RecipeIntegrationService.IntegrationRecipe recipe : recipes) {
                sender.sendMessage(ChatColor.YELLOW + recipe.id + ChatColor.GRAY + " (" + recipe.recipeType + ")" + 
                    ChatColor.WHITE + ": " + recipe.inputItemId + " → " + recipe.outputItemId);
            }
        } else {
            // Show all categories
            sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Recipe Categories" + ChatColor.GOLD + " ===");
            for (Map.Entry<String, List<RecipeIntegrationService.IntegrationRecipe>> entry : allRecipes.entrySet()) {
                sender.sendMessage(ChatColor.AQUA + entry.getKey() + ChatColor.WHITE + " - " + 
                    entry.getValue().size() + " recipes");
            }
        }

        return true;
    }

    private boolean handleEnableCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.recipeint.admin")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/recipeintegration enable <integration|category|recipe_id>");
            return true;
        }

        String integrationId = args[1];
        
        // Try to enable as integration category first
        if (recipeIntegrationService.getAvailableIntegrations().contains(integrationId)) {
            recipeIntegrationService.enableIntegration(integrationId);
            sender.sendMessage(ChatColor.GREEN + "Integration category '" + integrationId + "' enabled.");
        } else {
            // Try to enable as specific recipe
            RecipeIntegrationService.IntegrationRecipe recipe = recipeIntegrationService.getRecipeById(integrationId);
            if (recipe != null) {
                // This would require a different enable mechanism - for now just notify
                sender.sendMessage(ChatColor.YELLOW + "Specific recipe '" + integrationId + "' exists but individual enabling not implemented.");
                sender.sendMessage(ChatColor.GRAY + "Enable the recipe's category instead.");
            } else {
                sender.sendMessage(ChatColor.RED + "Integration '" + integrationId + "' not found.");
                return true;
            }
        }
        
        return true;
    }

    private boolean handleDisableCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.recipeint.admin")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/recipeintegration disable <integration>");
            return true;
        }

        String integrationId = args[1];
        recipeIntegrationService.disableIntegration(integrationId);
        
        sender.sendMessage(ChatColor.GREEN + "Integration '" + integrationId + "' disabled.");
        return true;
    }

    private boolean handleStatsCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.recipeint.stats")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> stats = recipeIntegrationService.getIntegrationStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Integration Statistics" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Enabled integrations: " + ChatColor.WHITE + 
            stats.get("enabledIntegrations"));
        sender.sendMessage(ChatColor.AQUA + "Total recipe mappings: " + ChatColor.WHITE + 
            stats.get("totalRecipeMappings"));
        sender.sendMessage(ChatColor.AQUA + "Total recipes registered: " + ChatColor.WHITE + 
            stats.get("totalRecipesRegistered"));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> recipesByCategory = (Map<String, Integer>) stats.get("recipesByCategory");
        if (recipesByCategory != null) {
            sender.sendMessage(ChatColor.GRAY + "Recipes by category:");
            for (Map.Entry<String, Integer> entry : recipesByCategory.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        @SuppressWarnings("unchecked")
        Map<String, List<String>> compatibilityGroups = (Map<String, List<String>>) stats.get("compatibilityGroups");
        if (compatibilityGroups != null) {
            sender.sendMessage(ChatColor.GRAY + "Compatibility groups:");
            for (Map.Entry<String, List<String>> entry : compatibilityGroups.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + 
                    entry.getValue().size() + " items");
            }
        }
        
        // Add usage statistics
        sender.sendMessage(ChatColor.GRAY + "Usage statistics:");
        sender.sendMessage(ChatColor.GRAY + "  Total recipe usage: " + stats.get("totalRecipeUsage"));
        sender.sendMessage(ChatColor.GRAY + "  Active players: " + stats.get("activePlayersUsingIntegration"));
        
        @SuppressWarnings("unchecked")
        List<Map.Entry<String, Integer>> topRecipes = (List<Map.Entry<String, Integer>>) stats.get("mostUsedRecipes");
        if (topRecipes != null && !topRecipes.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  Top used recipes:");
            for (int i = 0; i < Math.min(5, topRecipes.size()); i++) {
                Map.Entry<String, Integer> entry = topRecipes.get(i);
                sender.sendMessage(ChatColor.GRAY + "    " + (i+1) + ". " + entry.getKey() + ": " + entry.getValue() + " times");
            }
        }

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.recipeint.admin")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        recipeIntegrationService.reloadRecipes();
        sender.sendMessage(ChatColor.GREEN + "Recipe integrations reloaded.");
        return true;
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.recipeint.check")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "/recipeintegration check <item1> <item2>");
            return true;
        }

        String item1 = args[1];
        String item2 = args[2];
        boolean compatible = recipeIntegrationService.areItemsCompatible(item1, item2);

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Item Compatibility Check" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + item1 + ChatColor.WHITE + " and " + ChatColor.AQUA + item2 + 
            ChatColor.WHITE + ": " + (compatible ? ChatColor.GREEN + "COMPATIBLE" : ChatColor.RED + "INCOMPATIBLE"));

        return true;
    }

    private boolean handleSearchCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.recipeint.list")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/recipeintegration search <keyword>");
            return true;
        }

        String keyword = args[1].toLowerCase();
        Map<String, List<RecipeIntegrationService.IntegrationRecipe>> allRecipes = recipeIntegrationService.getAllIntegrationRecipes();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Search Results for '" + keyword + "'" + ChatColor.GOLD + " ===");
        
        boolean foundAny = false;
        for (Map.Entry<String, List<RecipeIntegrationService.IntegrationRecipe>> entry : allRecipes.entrySet()) {
            String category = entry.getKey();
            List<RecipeIntegrationService.IntegrationRecipe> recipes = entry.getValue();
            
            for (RecipeIntegrationService.IntegrationRecipe recipe : recipes) {
                if (recipe.id.toLowerCase().contains(keyword) || 
                    recipe.inputItemId.toLowerCase().contains(keyword) ||
                    recipe.outputItemId.toLowerCase().contains(keyword) ||
                    recipe.recipeType.toLowerCase().contains(keyword)) {
                    
                    sender.sendMessage(ChatColor.YELLOW + recipe.id + ChatColor.GRAY + " (in " + category + ")" + 
                        ChatColor.WHITE + ": " + recipe.inputItemId + " → " + recipe.outputItemId + 
                        ChatColor.GRAY + " (" + recipe.recipeType + ")");
                    foundAny = true;
                }
            }
        }
        
        if (!foundAny) {
            sender.sendMessage(ChatColor.GRAY + "No recipes found containing '" + keyword + "'");
        }

        return true;
    }

    private boolean handleCompatibilityCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.recipeint.check")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/recipeintegration compat <item>");
            return true;
        }

        String item = args[1];
        List<String> compatibleItems = recipeIntegrationService.getCompatibleItems(item);

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Compatible Items for '" + item + "'" + ChatColor.GOLD + " ===");
        
        if (compatibleItems.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No compatible items found for '" + item + "'");
        } else {
            for (String compItem : compatibleItems) {
                sender.sendMessage(ChatColor.AQUA + "  • " + compItem);
            }
        }

        return true;
    }
    
    private boolean handleUsageStatsCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.recipeint.stats")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> stats = recipeIntegrationService.getRecipeUsageStats();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Recipe Usage Statistics" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total recipe usage: " + ChatColor.WHITE + 
            stats.get("totalUsage"));
        sender.sendMessage(ChatColor.AQUA + "Unique recipes used: " + ChatColor.WHITE + 
            stats.get("uniqueRecipesUsed"));
        sender.sendMessage(ChatColor.AQUA + "Currently active recipes: " + ChatColor.WHITE + 
            stats.get("currentActiveRecipes"));
        
        @SuppressWarnings("unchecked")
        List<Map.Entry<String, Integer>> topRecipes = (List<Map.Entry<String, Integer>>) stats.get("topUsedRecipes");
        if (topRecipes != null && !topRecipes.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Top 10 most used recipes:");
            for (int i = 0; i < Math.min(10, topRecipes.size()); i++) {
                Map.Entry<String, Integer> entry = topRecipes.get(i);
                sender.sendMessage(ChatColor.YELLOW + String.format("%2d. ", (i+1)) + 
                    ChatColor.AQUA + entry.getKey() + ChatColor.WHITE + " - " + 
                    ChatColor.GOLD + entry.getValue() + " times");
            }
        }

        return true;
    }
    
    private boolean handlePlayerStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.recipeint.stats")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Please specify a player name: /recipeintegration player <username>");
                return true;
            }
            // Show current player's stats
            Player player = (Player) sender;
            return showPlayerStats(sender, player.getUniqueId(), player.getName());
        }

        String playerName = args[1];
        org.bukkit.entity.Player targetPlayer = org.bukkit.Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' is not online.");
            // In a full implementation, we would look up their data from storage
            sender.sendMessage(ChatColor.GRAY + "(Currently showing online player stats only)");
            return true;
        }

        return showPlayerStats(sender, targetPlayer.getUniqueId(), targetPlayer.getName());
    }
    
    private boolean showPlayerStats(CommandSender sender, java.util.UUID playerId, String playerName) {
        Map<String, Object> playerStats = recipeIntegrationService.getPlayerCraftStats(playerId);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Crafting Stats for " + playerName + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total integration crafts: " + ChatColor.WHITE + 
            playerStats.get("totalIntegrationCrafts"));
        sender.sendMessage(ChatColor.AQUA + "Unique recipes used: " + ChatColor.WHITE + 
            playerStats.get("uniqueRecipesUsed"));
        
        @SuppressWarnings("unchecked")
        List<Map.Entry<String, Integer>> topRecipes = (List<Map.Entry<String, Integer>>) playerStats.get("topRecipes");
        if (topRecipes != null && !topRecipes.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Top recipes used by player:");
            for (int i = 0; i < Math.min(5, topRecipes.size()); i++) {
                Map.Entry<String, Integer> entry = topRecipes.get(i);
                sender.sendMessage(ChatColor.YELLOW + String.format("%d. ", (i+1)) + 
                    ChatColor.AQUA + entry.getKey() + ChatColor.WHITE + " - " + 
                    ChatColor.GOLD + entry.getValue() + " times");
            }
        }

        return true;
    }
}
