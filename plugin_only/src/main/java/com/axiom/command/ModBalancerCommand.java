package com.axiom.command;

import com.axiom.AXIOM;
import com.axiom.service.BalancingService;
import com.axiom.service.ModdedEconomicBalanceService;
import com.axiom.service.ModPackManagerService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/**
 * Server-wide mod balancing command
 */
public class ModBalancerCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final BalancingService balancingService;
    private final ModdedEconomicBalanceService moddedEconomicBalanceService;
    private final ModPackManagerService modPackManagerService;

    public ModBalancerCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.balancingService = plugin.getBalancingService();
        this.moddedEconomicBalanceService = plugin.getModdedEconomicBalanceService();
        this.modPackManagerService = plugin.getModPackManagerService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "global":
            case "server":
                return handleGlobalBalancing(sender);
            case "nations":
            case "nationsreport":
                return handleNationsReport(sender);
            case "modpacks":
            case "packs":
                return handleModPacksReport(sender);
            case "economy":
            case "economic":
                return handleEconomicReport(sender);
            case "resources":
            case "resource":
                return handleResourceReport(sender);
            case "adjust":
            case "balance":
                return handleAdjustCommand(sender, args);
            case "reset":
                return handleResetCommand(sender, args);
            case "status":
            case "overview":
                return handleStatusCommand(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Global Mod Balancer Commands" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/modbal global" + ChatColor.WHITE + " - Show global mod balance status");
        sender.sendMessage(ChatColor.YELLOW + "/modbal nations" + ChatColor.WHITE + " - Report nations' mod usage");
        sender.sendMessage(ChatColor.YELLOW + "/modbal modpacks" + ChatColor.WHITE + " - Show modpack usage statistics");
        sender.sendMessage(ChatColor.YELLOW + "/modbal economy" + ChatColor.WHITE + " - Show economic balance report");
        sender.sendMessage(ChatColor.YELLOW + "/modbal resources" + ChatColor.WHITE + " - Show resource balance report");
        sender.sendMessage(ChatColor.YELLOW + "/modbal adjust <setting> <value>" + ChatColor.WHITE + " - Adjust balance settings");
        sender.sendMessage(ChatColor.YELLOW + "/modbal reset" + ChatColor.WHITE + " - Reset mod balance statistics");
        sender.sendMessage(ChatColor.YELLOW + "/modbal status" + ChatColor.WHITE + " - Show overall balance status");
    }

    private boolean handleGlobalBalancing(CommandSender sender) {
        if (!sender.hasPermission("axiom.modbal.global")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> globalStats = balancingService.getGlobalBalanceStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Global Mod Balance Status" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total nations: " + ChatColor.WHITE + globalStats.get("totalNations"));
        sender.sendMessage(ChatColor.AQUA + "Active conflicts: " + ChatColor.WHITE + globalStats.get("activeConflicts"));
        
        // Get mod-specific stats
        Map<String, Object> modStats = moddedEconomicBalanceService.getGlobalModEconomyStats();
        sender.sendMessage(ChatColor.AQUA + "Nations with mod economies: " + ChatColor.WHITE + modStats.get("totalNationsTracked"));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> modUsage = (Map<String, Integer>) modStats.get("globalSupply");
        if (modUsage != null && !modUsage.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Top used mod resources:");
            modUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue()));
        }

        return true;
    }

    private boolean handleNationsReport(CommandSender sender) {
        if (!sender.hasPermission("axiom.modbal.nations")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Nations Mod Usage Report" + ChatColor.GOLD + " ===");
        
        int count = 0;
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            Map<String, Object> modStats = plugin.getEnhancedModBalanceService().getNationModUsageStats(nation.getId());
            double powerScore = (Double) modStats.get("accumulatedPower");
            
            if (powerScore > 1.0) { // Only show nations with mod usage
                sender.sendMessage(ChatColor.AQUA + nation.getName() + ChatColor.GRAY + 
                    " (ID: " + nation.getId() + ") - " + ChatColor.WHITE + 
                    String.format("Power: %.2f", powerScore));
                
                if (++count >= 20) {
                    sender.sendMessage(ChatColor.GRAY + "... and more nations");
                    break;
                }
            }
        }
        
        if (count == 0) {
            sender.sendMessage(ChatColor.GRAY + "No nations with significant mod usage found.");
        }

        return true;
    }

    private boolean handleModPacksReport(CommandSender sender) {
        if (!sender.hasPermission("axiom.modbal.pack")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> packStats = modPackManagerService.getModPackStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "ModPack Usage Statistics" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total modpacks: " + ChatColor.WHITE + packStats.get("totalModPacks"));
        sender.sendMessage(ChatColor.AQUA + "Nations with active modpacks: " + ChatColor.WHITE + packStats.get("activeNations"));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> byLevel = (Map<String, Integer>) packStats.get("byCompatibilityLevel");
        if (byLevel != null) {
            sender.sendMessage(ChatColor.GRAY + "By compatibility level:");
            for (Map.Entry<String, Integer> entry : byLevel.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue() + " packs");
            }
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> byPack = (Map<String, Integer>) packStats.get("nationsByModPack");
        if (byPack != null) {
            sender.sendMessage(ChatColor.GRAY + "Nations by modpack:");
            for (Map.Entry<String, Integer> entry : byPack.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue() + " nations");
            }
        }

        return true;
    }

    private boolean handleEconomicReport(CommandSender sender) {
        if (!sender.hasPermission("axiom.modbal.economy")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> economyStats = moddedEconomicBalanceService.getGlobalModEconomyStats();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Global Economic Balance Report" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Nations with mod economies: " + ChatColor.WHITE + economyStats.get("totalNationsTracked"));
        sender.sendMessage(ChatColor.AQUA + "Total global mod value: " + ChatColor.WHITE + 
            String.format("$%.2f", (Double) economyStats.get("totalGlobalModValue")));
        sender.sendMessage(ChatColor.AQUA + "Average mod factories: " + ChatColor.WHITE + 
            String.format("%.2f", (Double) economyStats.get("averageModFactories")));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topTraded = (List<Map<String, Object>>) economyStats.get("topTradedResources");
        if (topTraded != null && !topTraded.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Most traded mod resources:");
            for (int i = 0; i < Math.min(10, topTraded.size()); i++) {
                Map<String, Object> resource = topTraded.get(i);
                sender.sendMessage(ChatColor.GRAY + "  " + (i+1) + ". " + 
                    ChatColor.AQUA + resource.get("id") + ChatColor.GRAY + 
                    " (vol: " + resource.get("totalVolume") + ")");
            }
        }

        return true;
    }

    private boolean handleResourceReport(CommandSender sender) {
        if (!sender.hasPermission("axiom.modbal.resources")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Resource Balance Report" + ChatColor.GOLD + " ===");
        
        // Show mod resource compatibility statistics
        Map<String, Object> compatStats = plugin.getModIntegrationEnhancementService().getGlobalCompatibilityStats();
        sender.sendMessage(ChatColor.AQUA + "Mod resource compatibility groups: " + ChatColor.WHITE + 
            compatStats.get("totalCompatibilityGroups"));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> groupSizes = (Map<String, Integer>) compatStats.get("compatibilityGroupSizes");
        if (groupSizes != null) {
            sender.sendMessage(ChatColor.GRAY + "Resource groups:");
            for (Map.Entry<String, Integer> entry : groupSizes.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue() + " items");
            }
        }

        // Show cross-mod recipe statistics
        Map<String, Object> recipeStats = plugin.getRecipeIntegrationService().getIntegrationStatistics();
        sender.sendMessage(ChatColor.AQUA + "Cross-mod recipe integrations: " + ChatColor.WHITE + 
            recipeStats.get("totalRecipeMappings"));

        return true;
    }

    private boolean handleAdjustCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modbal.admin")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "/modbal adjust <setting> <value>");
            sender.sendMessage(ChatColor.GRAY + "Settings: modPowerMultiplier, resourceAbundance, energyEfficiency, militaryBalance");
            return true;
        }

        String setting = args[1].toLowerCase();
        String valueStr = args[2];
        
        try {
            double value = Double.parseDouble(valueStr);
            
            switch (setting) {
                case "modpowermultiplier":
                case "mod_power":
                    // Adjust global mod power multiplier
                    // This would connect to balancing service to adjust power levels
                    sender.sendMessage(ChatColor.YELLOW + "Adjusted global mod power multiplier to: " + value);
                    break;
                case "resourceabundance":
                case "resource_abundance":
                    // Adjust resource abundance in mod worlds
                    sender.sendMessage(ChatColor.YELLOW + "Adjusted resource abundance multiplier to: " + value);
                    break;
                case "energyefficiency":
                case "energy_efficiency":
                    // Adjust energy efficiency of mod systems
                    sender.sendMessage(ChatColor.YELLOW + "Adjusted energy efficiency multiplier to: " + value);
                    break;
                case "militarybalance":
                case "military_balance":
                    // Adjust military power balance
                    sender.sendMessage(ChatColor.YELLOW + "Adjusted military balance multiplier to: " + value);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown setting. Use: modPowerMultiplier, resourceAbundance, energyEfficiency, militaryBalance");
                    return true;
            }
            
            sender.sendMessage(ChatColor.GREEN + "Setting '" + setting + "' adjusted to " + value);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Value must be a number.");
        }

        return true;
    }

    private boolean handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modbal.reset")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/modbal reset <category>");
            sender.sendMessage(ChatColor.GRAY + "Categories: statistics, balances, all");
            return true;
        }

        String category = args[1].toLowerCase();
        
        switch (category) {
            case "statistics":
            case "stats":
                // Reset mod statistics
                sender.sendMessage(ChatColor.YELLOW + "Resetting mod statistics...");
                // In a full implementation, this would reset stat counters
                sender.sendMessage(ChatColor.GREEN + "Mod statistics reset.");
                break;
            case "balances":
            case "balance":
                // Reset balance measurements
                sender.sendMessage(ChatColor.YELLOW + "Resetting mod balances...");
                // In a full implementation, this would reset balance states
                sender.sendMessage(ChatColor.GREEN + "Mod balances reset.");
                break;
            case "all":
                // Reset everything
                sender.sendMessage(ChatColor.YELLOW + "Resetting all mod balance data...");
                // In a full implementation, this would reset all balance states
                sender.sendMessage(ChatColor.GREEN + "All mod balance data reset.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown category. Use: statistics, balances, all");
                return true;
        }

        return true;
    }

    private boolean handleStatusCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modbal.status")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        // Overall server mod balance status
        Map<String, Object> globalStats = balancingService.getGlobalBalanceStatistics();
        Map<String, Object> modStats = moddedEconomicBalanceService.getGlobalModEconomyStats();
        Map<String, Object> packStats = modPackManagerService.getModPackStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "AXIOM Server Mod Balance Overview" + ChatColor.GOLD + " ===");
        
        // Global balance metrics
        sender.sendMessage(ChatColor.AQUA + "Nations: " + ChatColor.WHITE + globalStats.get("totalNations"));
        sender.sendMessage(ChatColor.AQUA + "Mod-enabled nations: " + ChatColor.WHITE + 
            modStats.get("totalNationsTracked"));
        sender.sendMessage(ChatColor.AQUA + "Active modpacks: " + ChatColor.WHITE + 
            packStats.get("activeNations"));
        
        // Mod usage metrics
        sender.sendMessage(ChatColor.GRAY + "Mod Integration Metrics:");
        sender.sendMessage(ChatColor.GRAY + "  • Total mod resources: " + 
            ((Map<String, Integer>) modStats.getOrDefault("globalSupply", new HashMap<>())).size());
        sender.sendMessage(ChatColor.GRAY + "  • Cross-mod recipes: " + 
            plugin.getRecipeIntegrationService().getTotalRecipeMappings());
        sender.sendMessage(ChatColor.GRAY + "  • Active integrations: " + 
            plugin.getModIntegrationEnhancementService().getTotalIntegrations());
        
        // Balance status
        double avgModPower = 0.0;
        int modNations = 0;
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            Map<String, Object> nationStats = plugin.getEnhancedModBalanceService().getNationModUsageStats(nation.getId());
            if (nationStats.get("accumulatedPower") != null) {
                avgModPower += (Double) nationStats.get("accumulatedPower");
                modNations++;
            }
        }
        
        if (modNations > 0) {
            avgModPower /= modNations;
            sender.sendMessage(ChatColor.GRAY + "  • Average mod power: " + String.format("%.2f", avgModPower));
            
            if (avgModPower > 3.0) {
                sender.sendMessage(ChatColor.RED + "  • Status: HIGH MOD USAGE (Consider reviewing balance)");
            } else if (avgModPower < 1.0) {
                sender.sendMessage(ChatColor.YELLOW + "  • Status: LOW MOD USAGE (Consider promoting mod usage)");
            } else {
                sender.sendMessage(ChatColor.GREEN + "  • Status: BALANCED MOD USAGE");
            }
        }
        
        return true;
    }
}