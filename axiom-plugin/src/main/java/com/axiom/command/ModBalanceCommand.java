package com.axiom.command;

import com.axiom.AXIOM;
import com.axiom.service.ModBalancerService;
import com.axiom.service.NationManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Command for mod balancing and integration management
 */
public class ModBalanceCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final ModBalancerService modBalancerService;
    private final NationManager nationManager;

    public ModBalanceCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.modBalancerService = plugin.getModBalancerService();
        this.nationManager = plugin.getNationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "check":
                return handleCheckCommand(sender, args);
            case "config":
            case "balance":
                return handleConfigCommand(sender, args);
            case "enable":
                return handleEnableCommand(sender, args);
            case "disable":
                return handleDisableCommand(sender, args);
            case "stats":
                return handleStatsCommand(sender, args);
            case "list":
                return handleListCommand(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Balance Commands" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/modbalance check" + ChatColor.WHITE + " - Check mod compatibility for your nation");
        sender.sendMessage(ChatColor.YELLOW + "/modbalance stats" + ChatColor.WHITE + " - Show mod balance statistics");
        sender.sendMessage(ChatColor.YELLOW + "/modbalance list" + ChatColor.WHITE + " - List all available mods");
        sender.sendMessage(ChatColor.YELLOW + "/modbalance enable <modId>" + ChatColor.WHITE + " - Enable mod for your nation (admin)");
        sender.sendMessage(ChatColor.YELLOW + "/modbalance disable <modId>" + ChatColor.WHITE + " - Disable mod for your nation (admin)");
        sender.sendMessage(ChatColor.YELLOW + "/modbalance config" + ChatColor.WHITE + " - Show configuration options (admin)");
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        String nationId = nationManager.getNationOfPlayer(player.getUniqueId())
            .map(nation -> nation.getId())
            .orElse(null);

        if (nationId == null) {
            sender.sendMessage(ChatColor.RED + "You must be in a nation to use this command.");
            return true;
        }

        // Get mod balance stats for the nation
        Map<String, Object> stats = modBalancerService.getNationModBalanceStats(nationId);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Balance Check" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Active mods: " + ChatColor.WHITE + 
            stats.getOrDefault("activeMods", "Unknown"));
        sender.sendMessage(ChatColor.AQUA + "Mod synergy: " + ChatColor.WHITE + 
            String.format("%.2f", (Double) stats.getOrDefault("modSynergy", 1.0)));
        sender.sendMessage(ChatColor.AQUA + "Military balance: " + ChatColor.WHITE + 
            stats.getOrDefault("militaryBalance", "Unknown"));
        sender.sendMessage(ChatColor.AQUA + "Energy balance: " + ChatColor.WHITE + 
            stats.getOrDefault("energyBalance", "Unknown"));

        return true;
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modbalance.check")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        // Get global mod balancer statistics
        Map<String, Object> stats = modBalancerService.getModBalancerStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Global Mod Balance Stats" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total mods tracked: " + ChatColor.WHITE + 
            stats.getOrDefault("totalMods", 0));
        sender.sendMessage(ChatColor.AQUA + "Nations with mod settings: " + ChatColor.WHITE + 
            stats.getOrDefault("trackedNations", 0));
        sender.sendMessage(ChatColor.AQUA + "Top used mods: " + ChatColor.WHITE + 
            stats.getOrDefault("topUsedMods", "None"));

        return true;
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modbalance.use")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Available Mods" + ChatColor.GOLD + " ===");
        
        for (var modInfo : modBalancerService.getAvailableMods()) {
            String status = modInfo.enabled ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗";
            sender.sendMessage(status + " " + ChatColor.AQUA + modInfo.modId + 
                ChatColor.WHITE + " - " + modInfo.name + 
                ChatColor.GRAY + " (compat: " + String.format("%.1f", modInfo.compatibilityScore) + ")");
        }

        return true;
    }

    private boolean handleEnableCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modbalance.admin")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/modbalance enable <modId> [nationId]");
            return true;
        }

        String modId = args[1];
        String nationId = null;

        if (args.length >= 3) {
            // Admin can specify nation
            nationId = args[2];
        } else if (sender instanceof Player) {
            // Player's nation
            nationId = nationManager.getNationOfPlayer(((Player) sender).getUniqueId())
                .map(n -> n.getId())
                .orElse(null);
        }

        if (nationId == null) {
            sender.sendMessage(ChatColor.RED + "No nation specified and you're not in a nation.");
            return true;
        }

        modBalancerService.setModEnabledForNation(nationId, modId, true);
        sender.sendMessage(ChatColor.GREEN + "Mod " + modId + " enabled for nation " + nationId);
        return true;
    }

    private boolean handleDisableCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modbalance.admin")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/modbalance disable <modId> [nationId]");
            return true;
        }

        String modId = args[1];
        String nationId = null;

        if (args.length >= 3) {
            // Admin can specify nation
            nationId = args[2];
        } else if (sender instanceof Player) {
            // Player's nation
            nationId = nationManager.getNationOfPlayer(((Player) sender).getUniqueId())
                .map(n -> n.getId())
                .orElse(null);
        }

        if (nationId == null) {
            sender.sendMessage(ChatColor.RED + "No nation specified and you're not in a nation.");
            return true;
        }

        modBalancerService.setModEnabledForNation(nationId, modId, false);
        sender.sendMessage(ChatColor.GREEN + "Mod " + modId + " disabled for nation " + nationId);
        return true;
    }

    private boolean handleConfigCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modbalance.config")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Balance Configuration" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Energy conversion allowed: " + ChatColor.WHITE + "Yes");
        sender.sendMessage(ChatColor.AQUA + "Mod integration enabled: " + ChatColor.WHITE + "Yes");
        sender.sendMessage(ChatColor.AQUA + "Military power balancing: " + ChatColor.WHITE + "Active");
        sender.sendMessage(ChatColor.GRAY + "Use /modbalance config <setting> <value> to change settings");

        return true;
    }
}