package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.domain.service.infrastructure.ModIntegrationEnhancementService;
import com.axiom.domain.service.infrastructure.RecipeIntegrationService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command for mod integration enhancement management
 */
public class ModIntegrationEnhancementCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final ModIntegrationEnhancementService modIntegrationEnhancementService;
    private final RecipeIntegrationService recipeIntegrationService;

    public ModIntegrationEnhancementCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.modIntegrationEnhancementService = plugin.getModIntegrationEnhancementService();
        this.recipeIntegrationService = plugin.getRecipeIntegrationService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.modenhance.check")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "mappings":
                return handleMappingsCommand(sender);
            case "compat":
            case "compatibility":
                return handleCompatibilityCommand(sender, args);
            case "check":
                return handleCheckCommand(sender, args);
            case "stats":
                return handleStatsCommand(sender);
            case "list":
                return handleListCommand(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Integration Enhancement Commands" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/modenhance mappings" + ChatColor.WHITE + " - Show material mappings");
        sender.sendMessage(ChatColor.YELLOW + "/modenhance compat <material1> <material2>" + ChatColor.WHITE + " - Check material compatibility");
        sender.sendMessage(ChatColor.YELLOW + "/modenhance check <itemId>" + ChatColor.WHITE + " - Check if item is from known mod");
        sender.sendMessage(ChatColor.YELLOW + "/modenhance stats" + ChatColor.WHITE + " - Show integration statistics");
        sender.sendMessage(ChatColor.YELLOW + "/modenhance list" + ChatColor.WHITE + " - List known mod items");
    }

    private boolean handleMappingsCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modenhance.mappings")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> stats = modIntegrationEnhancementService.getMaterialMappingStats();
        
        @SuppressWarnings("unchecked")
        Map<String, org.bukkit.Material> mappings = (Map<String, org.bukkit.Material>) stats.get("mappingDetails");
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Material Mappings" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total mappings: " + ChatColor.WHITE + stats.get("totalModMaterialMappings"));
        
        if (mappings != null) {
            int count = 0;
            for (Map.Entry<String, org.bukkit.Material> entry : mappings.entrySet()) {
                if (count++ < 20) { // Limit output
                    sender.sendMessage(ChatColor.YELLOW + entry.getKey() + ChatColor.WHITE + " → " + 
                        ChatColor.AQUA + entry.getValue().name());
                }
            }
            if (mappings.size() > 20) {
                sender.sendMessage(ChatColor.GRAY + "(Showing first 20 of " + mappings.size() + " mappings)");
            }
        }

        return true;
    }

    private boolean handleCompatibilityCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modenhance.compat")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "/modenhance compat <material1> <material2>");
            return true;
        }

        String material1Name = args[1].toUpperCase().replace('-', '_');
        String material2Name = args[2].toUpperCase().replace('-', '_');

        org.bukkit.Material mat1, mat2;

        try {
            // First try parsing as vanilla material
            mat1 = org.bukkit.Material.valueOf(material1Name);
        } catch (IllegalArgumentException e) {
            // If not vanilla, try as mod item ID
            mat1 = modIntegrationEnhancementService.getMaterialFromModItem(args[1]);
            if (mat1 == org.bukkit.Material.AIR) {
                sender.sendMessage(ChatColor.RED + "Unknown material: " + args[1]);
                return true;
            }
        }

        try {
            mat2 = org.bukkit.Material.valueOf(material2Name);
        } catch (IllegalArgumentException e) {
            mat2 = modIntegrationEnhancementService.getMaterialFromModItem(args[2]);
            if (mat2 == org.bukkit.Material.AIR) {
                sender.sendMessage(ChatColor.RED + "Unknown material: " + args[2]);
                return true;
            }
        }

        boolean compatible = modIntegrationEnhancementService.areMaterialsCrossModCompatible(mat1, mat2);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Material Compatibility" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + mat1.name() + ChatColor.WHITE + " and " + ChatColor.AQUA + mat2.name() + 
            ChatColor.WHITE + ": " + (compatible ? ChatColor.GREEN + "COMPATIBLE" : ChatColor.RED + "INCOMPATIBLE"));

        // Show compatible alternatives
        if (!compatible) {
            Set<org.bukkit.Material> alternatives = modIntegrationEnhancementService.getCompatibleMaterials(mat1);
            if (!alternatives.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Compatible alternatives for " + mat1.name() + ":");
                for (org.bukkit.Material alt : alternatives) {
                    if (alt != mat1) {
                        boolean altCompatible = modIntegrationEnhancementService.areMaterialsCrossModCompatible(alt, mat2);
                        String color = (altCompatible ? ChatColor.GREEN : ChatColor.GRAY).toString();
                        sender.sendMessage(color + "  • " + alt.name() + (altCompatible ? " ✓" : ""));
                    }
                }
            }
        }

        return true;
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modenhance.check")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/modenhance check <itemId>");
            return true;
        }

        String itemId = args[1];
        boolean isKnown = modIntegrationEnhancementService.isKnownModItem(itemId);
        String modName = modIntegrationEnhancementService.getModName(itemId);

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Item Check: " + itemId + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Known mod item: " + ChatColor.WHITE + 
            (isKnown ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"));
        sender.sendMessage(ChatColor.AQUA + "Source mod: " + ChatColor.WHITE + modName);
        
        // Show material mapping if available
        org.bukkit.Material mappedMat = modIntegrationEnhancementService.getMaterialFromModItem(itemId);
        if (mappedMat != org.bukkit.Material.AIR) {
            sender.sendMessage(ChatColor.AQUA + "Mapped to: " + ChatColor.WHITE + mappedMat.name());
        }

        return true;
    }

    private boolean handleStatsCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modenhance.stats")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> mappingStats = modIntegrationEnhancementService.getMaterialMappingStats();
        Map<String, Object> recipeStats = recipeIntegrationService.getIntegrationStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Integration Enhancement Statistics" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Mod material mappings: " + ChatColor.WHITE + 
            mappingStats.get("totalModMaterialMappings"));
        sender.sendMessage(ChatColor.AQUA + "Cross-mod compatibility checks: " + ChatColor.WHITE + 
            mappingStats.get("crossModCompatibilityChecks"));
        sender.sendMessage(ChatColor.AQUA + "Integration recipes: " + ChatColor.WHITE + 
            recipeStats.get("totalRecipeMappings"));
        sender.sendMessage(ChatColor.AQUA + "Enabled integrations: " + ChatColor.WHITE + 
            recipeStats.get("enabledIntegrations"));

        return true;
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modenhance.list")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Set<String> knownItems = modIntegrationEnhancementService.getRegisteredModItemIds();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Known Mod Items (" + knownItems.size() + ")" + ChatColor.GOLD + " ===");

        int displayed = 0;
        for (String itemId : knownItems) {
            if (displayed++ < 30) { // Limit output
                String modName = modIntegrationEnhancementService.getModName(itemId);
                sender.sendMessage(ChatColor.YELLOW + modName + ChatColor.GRAY + ":" + ChatColor.WHITE + 
                    itemId.substring(itemId.indexOf(':') + 1));
            }
        }
        
        if (knownItems.size() > 30) {
            sender.sendMessage(ChatColor.GRAY + "(Showing first 30 of " + knownItems.size() + " items)");
        }

        return true;
    }
}
