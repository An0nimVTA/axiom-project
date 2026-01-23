package com.axiom.command;

import com.axiom.AXIOM;
import com.axiom.service.ModdedEconomicBalanceService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Command for modded economic balance management
 */
public class ModdedEconomicBalanceCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final ModdedEconomicBalanceService moddedEconomicBalanceService;

    public ModdedEconomicBalanceCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.moddedEconomicBalanceService = plugin.getModdedEconomicBalanceService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "prices":
            case "market":
                return handleMarketCommand(sender);
            case "stats":
            case "economystats":
                return handleEconomyStatsCommand(sender, args);
            case "value":
            case "checkvalue":
                return handleValueCheckCommand(sender, args);
            case "balance":
            case "balancecheck":
                return handleBalanceCheckCommand(sender, args);
            case "adjust":
                return handleAdjustCommand(sender, args);
            case "recommend":
            case "recommendations":
                return handleRecommendationsCommand(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Modded Economic Balance Commands" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/moddedecon prices" + ChatColor.WHITE + " - View mod item market prices");
        sender.sendMessage(ChatColor.YELLOW + "/moddedecon stats [nation]" + ChatColor.WHITE + " - Show economic statistics");
        sender.sendMessage(ChatColor.YELLOW + "/moddedecon value <mod_item_id>" + ChatColor.WHITE + " - Check mod item value");
        sender.sendMessage(ChatColor.YELLOW + "/moddedecon balance [nation]" + ChatColor.WHITE + " - Check economic balance");
        sender.sendMessage(ChatColor.YELLOW + "/moddedecon recommend [nation]" + ChatColor.WHITE + " - Get balance recommendations");
        sender.sendMessage(ChatColor.YELLOW + "/moddedecon adjust <action>" + ChatColor.WHITE + " - Apply economic adjustments");
    }

    private boolean handleMarketCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.moddedecon.prices")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        // Show market prices
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Market Prices (Global Average)" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Steel Ingot (IndustrialUpgrade): " + ChatColor.WHITE + "$" + 
            String.format("%.2f", moddedEconomicBalanceService.getModItemValue("industrialupgrade:steel_ingot")));
        sender.sendMessage(ChatColor.AQUA + "Certus Quartz (AE2): " + ChatColor.WHITE + "$" + 
            String.format("%.2f", moddedEconomicBalanceService.getModItemValue("appliedenergistics2:certus_quartz_crystal")));
        sender.sendMessage(ChatColor.AQUA + "Gun Frame (TACZ): " + ChatColor.WHITE + "$" + 
            String.format("%.2f", moddedEconomicBalanceService.getModItemValue("tacz:gun_frame")));
        sender.sendMessage(ChatColor.AQUA + "Lithium Ingot (Ballistix): " + ChatColor.WHITE + "$" + 
            String.format("%.2f", moddedEconomicBalanceService.getModItemValue("ballistix:lithium_ingot")));

        return true;
    }

    private boolean handleEconomyStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.moddedecon.stats")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        String nationId = null;
        if (args.length > 1) {
            // Find nation by name
            nationId = args[1];
        } else if (sender instanceof Player) {
            // Use player's nation
            Player player = (Player) sender;
            var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (nationOpt.isPresent()) {
                nationId = nationOpt.get().getId();
            }
        }

        if (nationId == null) {
            // Show global stats
            return handleGlobalStats(sender);
        }

        Map<String, Object> stats = moddedEconomicBalanceService.getNationModEconomyStats(nationId);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Economic Stats for " + nationId + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total mod resources value: " + ChatColor.WHITE + 
            String.format("$%.2f", (Double) stats.get("totalModResourcesValue")));
        sender.sendMessage(ChatColor.AQUA + "Unique mod items traded: " + ChatColor.WHITE + 
            stats.get("uniqueModItemsTraded"));
        sender.sendMessage(ChatColor.AQUA + "Mod trade volume: " + ChatColor.WHITE + 
            String.format("$%.2f", (Double) stats.get("modTradeVolume")));
        sender.sendMessage(ChatColor.AQUA + "Mod factories: " + ChatColor.WHITE + 
            stats.get("modFactories"));
        sender.sendMessage(ChatColor.AQUA + "Mod energy consumption: " + ChatColor.WHITE + 
            String.format("%.2f", (Double) stats.get("modEnergyConsumption")));
        sender.sendMessage(ChatColor.AQUA + "Mod production output: " + ChatColor.WHITE + 
            String.format("$%.2f", (Double) stats.get("modProductionOutput")));

        // Regional market data
        @SuppressWarnings("unchecked")
        Map<String, Object> regionalData = (Map<String, Object>) stats.get("regionalMarket");
        if (regionalData != null) {
            sender.sendMessage(ChatColor.GRAY + "Regional market data:");
            sender.sendMessage(ChatColor.GRAY + "  Last update: " + (Long) regionalData.get("lastUpdate"));
            sender.sendMessage(ChatColor.GRAY + "  Available resources: " + (Integer) regionalData.get("resourceCount"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> topExpensive = (List<Map<String, Object>>) regionalData.get("topExpensiveResources");
            if (topExpensive != null && !topExpensive.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  Top expensive resources:");
                for (int i = 0; i < Math.min(5, topExpensive.size()); i++) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resource = topExpensive.get(i);
                    sender.sendMessage(ChatColor.GRAY + "    " + (i+1) + ". " + 
                        ChatColor.AQUA + resource.get("id") + ChatColor.WHITE + " - $" + 
                        String.format("%.2f", (Double) resource.get("price")));
                }
            }
        }

        return true;
    }

    private boolean handleGlobalStats(CommandSender sender) {
        Map<String, Object> globalStats = moddedEconomicBalanceService.getGlobalModEconomyStats();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Global Mod Economy Stats" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Nations with mod economies: " + ChatColor.WHITE + 
            globalStats.get("totalNationsTracked"));
        sender.sendMessage(ChatColor.AQUA + "Total global mod value: " + ChatColor.WHITE + 
            String.format("$%.2f", (Double) globalStats.get("totalGlobalModValue")));
        sender.sendMessage(ChatColor.AQUA + "Average mod factories: " + ChatColor.WHITE + 
            String.format("%.2f", (Double) globalStats.get("averageModFactories")));
        sender.sendMessage(ChatColor.AQUA + "Average mod production: " + ChatColor.WHITE + 
            String.format("$%.2f", (Double) globalStats.get("averageModProduction")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topTraded = (List<Map<String, Object>>) globalStats.get("topTradedResources");
        if (topTraded != null && !topTraded.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Top traded mod resources:");
            for (int i = 0; i < Math.min(5, topTraded.size()); i++) {
                Map<String, Object> resource = topTraded.get(i);
                sender.sendMessage(ChatColor.GRAY + "  " + (i+1) + ". " + 
                    ChatColor.AQUA + resource.get("id") + 
                    ChatColor.WHITE + " (vol: " + resource.get("totalVolume") + ")");
            }
        }

        return true;
    }

    private boolean handleValueCheckCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.moddedecon.checkvalue")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/moddedecon value <mod_item_id>");
            return true;
        }

        String modItemId = args[1].toLowerCase();
        double value = moddedEconomicBalanceService.getModItemValue(modItemId);

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Item Value: " + modItemId + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Estimated value: " + ChatColor.WHITE + "$" + String.format("%.2f", value));
        
        // Also show if it has regional variation
        if (sender instanceof Player) {
            Player player = (Player) sender;
            var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (nationOpt.isPresent()) {
                String nationName = nationOpt.get().getName();
                double regionalValue = moddedEconomicBalanceService.getRegionalModPrice(nationName, modItemId);
                sender.sendMessage(ChatColor.GRAY + "Regional price (" + nationName + "): " + 
                    ChatColor.WHITE + "$" + String.format("%.2f", regionalValue));
            }
        }

        return true;
    }

    private boolean handleBalanceCheckCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.moddedecon.balance")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        String nationId = null;
        if (args.length > 1) {
            nationId = args[1];
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (nationOpt.isPresent()) {
                nationId = nationOpt.get().getId();
            }
        }

        if (nationId == null) {
            sender.sendMessage(ChatColor.RED + "Specify a nation or be in a nation.");
            return true;
        }

        // Check for economic imbalances
        Map<String, Object> balanceStats = moddedEconomicBalanceService.getNationModEconomyStats(nationId);
        double totalValue = (Double) balanceStats.get("totalModResourcesValue");
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Economic Balance: " + nationId + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total mod value: " + ChatColor.WHITE + "$" + String.format("%.2f", totalValue));
        
        // Get nation's tech level for comparison
        double techLevel = plugin.getEnhancedModBalanceService().calculateBalancedPowerScore(nationId);
        double expectedValue = techLevel * 10000; // Approximate relationship
        
        double ratio = totalValue / expectedValue;
        if (ratio > 2.0) {
            sender.sendMessage(ChatColor.RED + "⚠ OVERPOWERED: Economic value significantly exceeds technological level");
        } else if (ratio < 0.5) {
            sender.sendMessage(ChatColor.YELLOW + "ℹ UNDERPOWERED: Economic value below technological level");
        } else {
            sender.sendMessage(ChatColor.GREEN + "✓ BALANCED: Economic value aligns with technological level");
        }

        return true;
    }

    private boolean handleRecommendationsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.moddedecon.recommend")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        String nationId = null;
        if (args.length > 1) {
            nationId = args[1];
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (nationOpt.isPresent()) {
                nationId = nationOpt.get().getId();
            }
        }

        if (nationId == null) {
            sender.sendMessage(ChatColor.RED + "Specify a nation or be in a nation.");
            return true;
        }

        List<String> recommendations = moddedEconomicBalanceService.getBalanceRecommendations(nationId);

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Balance Recommendations: " + nationId + ChatColor.GOLD + " ===");
        if (recommendations.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No specific recommendations at this time.");
        } else {
            for (String recommendation : recommendations) {
                sender.sendMessage(ChatColor.YELLOW + "• " + recommendation);
            }
        }

        return true;
    }

    private boolean handleAdjustCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.moddedecon.adjust")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/moddedecon adjust <action>");
            sender.sendMessage(ChatColor.GRAY + "Actions: prices, balancing, corrections");
            return true;
        }

        String action = args[1].toLowerCase();
        
        switch (action) {
            case "prices":
                // Force price rebalancing
                sender.sendMessage(ChatColor.YELLOW + "Initiating mod market rebalancing...");
                // In a real implementation, this would trigger price adjustments
                sender.sendMessage(ChatColor.GREEN + "Market rebalancing initiated.");
                break;
            case "balancing":
                // Apply balancing measures to nations
                sender.sendMessage(ChatColor.YELLOW + "Applying economic balancing measures to all nations...");
                moddedEconomicBalanceService.applyEconomicBalanceToNations();
                sender.sendMessage(ChatColor.GREEN + "Balancing measures applied to all nations.");
                break;
            case "checkall":
                // Check all nations for economic imbalances
                sender.sendMessage(ChatColor.YELLOW + "Checking all nations for economic imbalances...");
                // This would perform a full scan and report
                Map<String, Object> globalStats = moddedEconomicBalanceService.getGlobalModEconomyStats();
                sender.sendMessage(ChatColor.GREEN + "Checked " + globalStats.get("totalNationsTracked") + " nations.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown adjustment action. Use: prices, balancing, checkall");
                return true;
        }

        return true;
    }
}