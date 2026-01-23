package com.axiom.command;

import com.axiom.AXIOM;
import com.axiom.service.DynamicModBalancerService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command for dynamic mod balancer management
 */
public class DynamicModBalancerCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final DynamicModBalancerService dynamicModBalancerService;

    public DynamicModBalancerCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.dynamicModBalancerService = plugin.getDynamicModBalancerService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
            case "overview":
                return handleStatusCommand(sender);
            case "player":
            case "playerstats":
                return handlePlayerStatsCommand(sender, args);
            case "nation":
            case "nations":
                return handleNationStatsCommand(sender, args);
            case "mod":
            case "modstats":
                return handleModStatsCommand(sender, args);
            case "global":
            case "server":
                return handleGlobalStatsCommand(sender);
            case "reset":
                return handleResetCommand(sender, args);
            case "adjust":
                return handleAdjustCommand(sender, args);
            case "enable":
                return handleEnableCommand(sender, args);
            case "disable":
                return handleDisableCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Dynamic Mod Balancer Commands" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/dmb status" + ChatColor.WHITE + " - Show overall balancing status");
        sender.sendMessage(ChatColor.YELLOW + "/dmb player [playerName]" + ChatColor.WHITE + " - Show player mod activity");
        sender.sendMessage(ChatColor.YELLOW + "/dmb nation [nationId]" + ChatColor.WHITE + " - Show nation mod activity");
        sender.sendMessage(ChatColor.YELLOW + "/dmb mod <modId>" + ChatColor.WHITE + " - Show mod-specific statistics");
        sender.sendMessage(ChatColor.YELLOW + "/dmb global" + ChatColor.WHITE + " - Show server-wide statistics");
        sender.sendMessage(ChatColor.YELLOW + "/dmb adjust <modId> <param> <value>" + ChatColor.WHITE + " - Adjust mod parameters");
        sender.sendMessage(ChatColor.YELLOW + "/dmb reset [all|modpack|players|nations]" + ChatColor.WHITE + " - Reset balancing data");
        sender.sendMessage(ChatColor.YELLOW + "/dmb reload" + ChatColor.WHITE + " - Reload balancing configuration");
    }

    private boolean handleStatusCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.dmb.status")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> stats = dynamicModBalancerService.getGlobalActivityStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Dynamic Balancer Status" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Tracked players: " + ChatColor.WHITE + stats.get("totalPlayersTracked"));
        sender.sendMessage(ChatColor.AQUA + "Tracked nations: " + ChatColor.WHITE + stats.get("totalNationsTracked"));
        sender.sendMessage(ChatColor.AQUA + "Avg player engagement: " + ChatColor.WHITE + 
            String.format("%.2f", (Double) stats.get("averagePlayerEngagement")) + "%");
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> globalModUsage = (Map<String, Integer>) stats.get("globalModUsage");
        if (globalModUsage != null) {
            sender.sendMessage(ChatColor.GRAY + "Top mod usage:");
            globalModUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue() + " interactions"));
        }
        
        // Nation engagement stats
        @SuppressWarnings("unchecked")
        Map<String, Object> nationEngagement = (Map<String, Object>) stats.get("nationEngagement");
        if (nationEngagement != null) {
            sender.sendMessage(ChatColor.GRAY + "Nation engagement:"); 
            sender.sendMessage(ChatColor.GRAY + "  Avg: " + String.format("%.2f", (Double) nationEngagement.get("averageNationEngagement")) + "%");
            sender.sendMessage(ChatColor.GRAY + "  Military: " + String.format("%.1f", (Double) nationEngagement.get("averageMilitaryEngagement")) + "%");
            sender.sendMessage(ChatColor.GRAY + "  Industrial: " + String.format("%.1f", (Double) nationEngagement.get("averageIndustrialEngagement")) + "%");
            sender.sendMessage(ChatColor.GRAY + "  Tech: " + String.format("%.1f", (Double) nationEngagement.get("averageTechEngagement")) + "%");
        }

        return true;
    }

    private boolean handlePlayerStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.dmb.player")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        String playerName = null;
        if (args.length > 1) {
            playerName = args[1];
        } else if (sender instanceof Player) {
            playerName = ((Player) sender).getName();
        }

        if (playerName == null) {
            sender.sendMessage(ChatColor.RED + "Specify a player name or be a player.");
            return true;
        }

        org.bukkit.entity.Player targetPlayer = org.bukkit.Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player not found online.");
            return true;
        }

        UUID playerId = targetPlayer.getUniqueId();
        var playerActivity = dynamicModBalancerService.getPlayerActivity(playerId);
        
        if (playerActivity == null) {
            sender.sendMessage(ChatColor.RED + "No activity data found for player " + playerName);
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Player Mod Activity: " + playerName + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total playtime: " + ChatColor.WHITE + playerActivity.totalPlayTimeMinutes + " minutes");
        sender.sendMessage(ChatColor.AQUA + "Last active: " + ChatColor.WHITE + 
            formatDuration(System.currentTimeMillis() - playerActivity.lastActive) + " ago");
        sender.sendMessage(ChatColor.AQUA + "Avg engagement: " + ChatColor.WHITE + 
            String.format("%.2f", playerActivity.averageModEngagement) + "%");
        
        // Mod interactions
        if (!playerActivity.modInteractionCount.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Mod interactions:");
            for (Map.Entry<String, Integer> entry : playerActivity.modInteractionCount.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue() + " interactions");
            }
        }

        return true;
    }

    private boolean handleNationStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.dmb.nation")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        String nationId = null;
        if (args.length > 1) {
            // Find by nation name
            for (var nation : plugin.getNationManager().getAllNations()) {
                if (nation.getName().equalsIgnoreCase(args[1])) {
                    nationId = nation.getId();
                    break;
                }
            }
        } else if (sender instanceof Player) {
            var nationOpt = plugin.getNationManager().getNationOfPlayer(((Player) sender).getUniqueId());
            if (nationOpt.isPresent()) {
                nationId = nationOpt.get().getId();
            }
        }

        if (nationId == null) {
            sender.sendMessage(ChatColor.RED + "Specify a nation name or be in a nation.");
            return true;
        }

        var nationActivity = dynamicModBalancerService.getNationActivity(nationId);
        if (nationActivity == null) {
            sender.sendMessage(ChatColor.RED + "No activity data found for nation.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Nation Mod Activity" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total members: " + ChatColor.WHITE + nationActivity.totalMembers);
        sender.sendMessage(ChatColor.AQUA + "Avg engagement: " + ChatColor.WHITE + 
            String.format("%.2f", nationActivity.averageModEngagement) + "%");
        sender.sendMessage(ChatColor.AQUA + "Peak players: " + ChatColor.WHITE + nationActivity.peakSimultaneousPlayers);
        
        // Mod usage by type
        sender.sendMessage(ChatColor.GRAY + "Engagement by type:");
        sender.sendMessage(ChatColor.GRAY + "  Military: " + String.format("%.1f", nationActivity.militaryEngagement) + "%");
        sender.sendMessage(ChatColor.GRAY + "  Industrial: " + String.format("%.1f", nationActivity.industrialEngagement) + "%");
        sender.sendMessage(ChatColor.GRAY + "  Tech: " + String.format("%.1f", nationActivity.techEngagement) + "%");

        return true;
    }

    private boolean handleModStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.dmb.modstats")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/dmb mod <modId>");
            return true;
        }

        String modId = args[1];
        Map<String, Object> modStats = dynamicModBalancerService.getModStatistics(modId);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Statistics: " + modId + ChatColor.GOLD + " ===");
        
        if (modStats.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Mod not found or no data available.");
            return true;
        }

        Double popularity = (Double) modStats.get("popularityScore");
        Integer totalUses = (Integer) modStats.get("totalUses");
        Integer activeUsers = (Integer) modStats.get("activeUsers");
        Integer dailyActive = (Integer) modStats.get("dailyActiveUsers");
        Double avgUsage = (Double) modStats.get("averageUsagePerPlayer");

        sender.sendMessage(ChatColor.AQUA + "Popularity: " + ChatColor.WHITE + String.format("%.2f%%", popularity));
        sender.sendMessage(ChatColor.AQUA + "Total uses: " + ChatColor.WHITE + totalUses);
        sender.sendMessage(ChatColor.AQUA + "Active users: " + ChatColor.WHITE + activeUsers);
        sender.sendMessage(ChatColor.AQUA + "Daily users: " + ChatColor.WHITE + dailyActive);
        sender.sendMessage(ChatColor.AQUA + "Avg usage/player: " + ChatColor.WHITE + String.format("%.2f", avgUsage));
        
        // Parameters if available
        if (modStats.containsKey("difficultyMultiplier")) {
            sender.sendMessage(ChatColor.GRAY + "Current parameters:");
            sender.sendMessage(ChatColor.GRAY + "  Difficulty: " + String.format("%.2f", (Double) modStats.get("difficultyMultiplier")));
            sender.sendMessage(ChatColor.GRAY + "  Resources: " + String.format("%.2f", (Double) modStats.get("resourceAbundance")));
            sender.sendMessage(ChatColor.GRAY + "  Crafting speed: " + String.format("%.2f", (Double) modStats.get("craftingTimeMultiplier")));
        }

        return true;
    }

    private boolean handleGlobalStatsCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.dmb.global")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> stats = dynamicModBalancerService.getGlobalActivityStatistics();
        Map<String, Object> balanceStats = dynamicModBalancerService.getBalanceStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Server-Wide Mod Statistics" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total tracked players: " + ChatColor.WHITE + stats.get("totalPlayersTracked"));
        sender.sendMessage(ChatColor.AQUA + "Total tracked nations: " + ChatColor.WHITE + stats.get("totalNationsTracked"));
        sender.sendMessage(ChatColor.AQUA + "Avg player engagement: " + ChatColor.WHITE + 
            String.format("%.2f%%", (Double) stats.get("averagePlayerEngagement")));
        
        // Balance-specific stats
        if (balanceStats != null) {
            sender.sendMessage(ChatColor.GRAY + "Balancing statistics:");
            // Add any balance-specific data from the service
        }

        return true;
    }

    private boolean handleAdjustCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.dmb.adjust")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "/dmb adjust <modId> <parameter> <value>");
            sender.sendMessage(ChatColor.GRAY + "Parameters: difficulty, resources, crafting, energy, combat");
            return true;
        }

        String modId = args[1];
        String param = args[2].toLowerCase();
        double value;
        
        try {
            value = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Value must be a number.");
            return true;
        }

        // Create parameters object and apply to specific mod
        DynamicModBalancerService.ModBalanceParameters currentParams = dynamicModBalancerService.getModBalanceParameters(modId);
        if (currentParams == null) {
            currentParams = new DynamicModBalancerService.ModBalanceParameters(modId);
        }

        switch (param) {
            case "difficulty":
                currentParams.difficultyMultiplier = value;
                break;
            case "resources":
                currentParams.resourceAbundance = value;
                break;
            case "crafting":
                currentParams.craftingTimeMultiplier = value;
                break;
            case "energy":
                currentParams.energyMultiplier = value;
                break;
            case "combat":
                currentParams.combatEffectiveness = value;
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown parameter. Use: difficulty, resources, crafting, energy, combat");
                return true;
        }

        // Apply the new parameters
        dynamicModBalancerService.setModParameters(modId, currentParams);
        
        sender.sendMessage(ChatColor.GREEN + "Parameter '" + param + "' for mod '" + modId + 
            "' adjusted to " + String.format("%.2f", value));
        sender.sendMessage(ChatColor.GRAY + "Changes applied dynamically to game systems.");

        return true;
    }

    private boolean handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.dmb.reset")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/dmb reset <category>");
            sender.sendMessage(ChatColor.GRAY + "Categories: all, modpacks, players, nations, balancing");
            return true;
        }

        String category = args[1].toLowerCase();
        
        switch (category) {
            case "all":
                dynamicModBalancerService.resetAllParameters();
                sender.sendMessage(ChatColor.GREEN + "All dynamic balancing parameters reset to defaults.");
                break;
            case "modpacks":
                // Reset modpack configurations only
                sender.sendMessage(ChatColor.YELLOW + "ModPack configurations reset (not implemented).");
                break;
            case "players":
                // Reset player activity records
                sender.sendMessage(ChatColor.YELLOW + "Player activity records reset (not implemented).");
                break;
            case "nations":
                // Reset nation activity records
                sender.sendMessage(ChatColor.YELLOW + "Nation activity records reset (not implemented).");
                break;
            case "balancing":
            case "balance":
                // Reset balance parameters
                dynamicModBalancerService.resetAllParameters();
                sender.sendMessage(ChatColor.GREEN + "All balancing parameters reset.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown category. Use: all, modpacks, players, nations, balancing");
                return true;
        }

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.dmb.reload")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        dynamicModBalancerService.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Dynamic mod balancer configuration reloaded.");
        return true;
    }

    /**
     * Format duration in milliseconds to human-readable string
     */
    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else {
            return minutes + "m " + (seconds % 60) + "s";
        }
    }
}