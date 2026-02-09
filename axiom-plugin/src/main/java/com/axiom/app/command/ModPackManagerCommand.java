package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.domain.service.infrastructure.ModPackManagerService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Command for modpack management
 */
public class ModPackManagerCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final ModPackManagerService modPackManagerService;

    public ModPackManagerCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.modPackManagerService = plugin.getModPackManagerService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.modpackmanager.admin")) {
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
            case "info":
            case "view":
                return handleInfoCommand(sender, args);
            case "enable":
            case "activate":
                return handleEnableCommand(sender, args);
            case "disable":
            case "deactivate":
                return handleDisableCommand(sender, args);
            case "stats":
            case "status":
                return handleStatsCommand(sender);
            case "available":
                return handleAvailableCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Pack Manager Commands" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/modpackmanager list" + ChatColor.WHITE + " - List available mod packs");
        sender.sendMessage(ChatColor.YELLOW + "/modpackmanager info <packId>" + ChatColor.WHITE + " - View mod pack details");
        sender.sendMessage(ChatColor.YELLOW + "/modpackmanager enable <packId>" + ChatColor.WHITE + " - Enable a mod pack");
        sender.sendMessage(ChatColor.YELLOW + "/modpackmanager disable <packId>" + ChatColor.WHITE + " - Disable a mod pack");
        sender.sendMessage(ChatColor.YELLOW + "/modpackmanager available" + ChatColor.WHITE + " - List available mod packs for your nation");
        sender.sendMessage(ChatColor.YELLOW + "/modpackmanager stats" + ChatColor.WHITE + " - Show mod pack statistics");
        sender.sendMessage(ChatColor.YELLOW + "/modpackmanager reload" + ChatColor.WHITE + " - Reload mod pack configuration");
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modpackmanager.list")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        List<ModPackManagerService.ModPack> modPacks = (List<ModPackManagerService.ModPack>) modPackManagerService.getAvailableModPacks();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Available Mod Packs" + ChatColor.GOLD + " ===");
        
        for (ModPackManagerService.ModPack modPack : modPacks) {
            String status = sender instanceof Player ? 
                (modPackManagerService.getActiveModPackForNation(getPlayerNationId((Player)sender)).equals(modPack.id) ? 
                 ChatColor.GREEN + "[ACTIVE]" : ChatColor.GRAY + "[INACTIVE]") : 
                 ChatColor.GRAY + "[SERVER]";
            
            sender.sendMessage(ChatColor.AQUA + modPack.name + " " + status);
            sender.sendMessage(ChatColor.GRAY + "  ID: " + ChatColor.WHITE + modPack.id);
            sender.sendMessage(ChatColor.GRAY + "  " + modPack.description);
            sender.sendMessage(ChatColor.GRAY + "  Required: " + ChatColor.WHITE + String.join(", ", modPack.requiredMods));
            sender.sendMessage(ChatColor.GRAY + "  Optional: " + ChatColor.WHITE + String.join(", ", modPack.optionalMods));
            sender.sendMessage("");
        }

        return true;
    }

    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modpackmanager.info")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/modpackmanager info <packId>");
            return true;
        }

        String modPackId = args[1];
        ModPackManagerService.ModPack modPack = modPackManagerService.getModPack(modPackId);
        
        if (modPack == null) {
            sender.sendMessage(ChatColor.RED + "Mod pack '" + modPackId + "' not found.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Pack Info: " + modPack.name + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "ID: " + ChatColor.WHITE + modPack.id);
        sender.sendMessage(ChatColor.AQUA + "Description: " + ChatColor.WHITE + modPack.description);
        sender.sendMessage(ChatColor.AQUA + "Compatibility Level: " + ChatColor.WHITE + modPack.compatibilityLevel);
        sender.sendMessage(ChatColor.AQUA + "Recommended Players: " + ChatColor.WHITE + modPack.recommendedPlayers);
        sender.sendMessage(ChatColor.AQUA + "Enabled: " + ChatColor.WHITE + (modPack.enabled ? "Yes" : "No"));
        
        sender.sendMessage(ChatColor.GRAY + "Required Mods:");
        for (String mod : modPack.requiredMods) {
            sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + mod);
        }
        
        sender.sendMessage(ChatColor.GRAY + "Optional Mods:");
        for (String mod : modPack.optionalMods) {
            sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + mod);
        }
        
        sender.sendMessage(ChatColor.GRAY + "Integration Rules: " + modPack.integrationRules.size());
        for (ModPackManagerService.ModIntegrationRule rule : modPack.integrationRules) {
            sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.AQUA + rule.sourceMod + 
                ChatColor.GRAY + " → " + ChatColor.AQUA + rule.targetMod + 
                ChatColor.GRAY + " (" + rule.conversionType + ")" + 
                (rule.enabled ? "" : ChatColor.RED + " [DISABLED]"));
        }

        return true;
    }

    private boolean handleEnableCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modpackmanager.modify")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players (for nation-specific modpacks).");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/modpackmanager enable <packId>");
            return true;
        }

        Player player = (Player) sender;
        String nationId = getPlayerNationId(player);
        if (nationId == null) {
            sender.sendMessage(ChatColor.RED + "You must be in a nation to use this command.");
            return true;
        }

        String modPackId = args[1];
        
        if (!modPackManagerService.isModPackAvailableForNation(nationId, modPackId)) {
            sender.sendMessage(ChatColor.RED + "Mod pack '" + modPackId + "' is not compatible with your nation's mods.");
            return true;
        }

        boolean success = modPackManagerService.enableModPackForNation(nationId, modPackId);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Mod pack '" + modPackId + "' enabled for your nation.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to enable mod pack '" + modPackId + "'.");
        }

        return true;
    }

    private boolean handleDisableCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("axiom.modpackmanager.modify")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players (for nation-specific modpacks).");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/modpackmanager disable <packId>");
            return true;
        }

        Player player = (Player) sender;
        String nationId = getPlayerNationId(player);
        if (nationId == null) {
            sender.sendMessage(ChatColor.RED + "You must be in a nation to use this command.");
            return true;
        }

        String modPackId = args[1];
        String activeModPack = modPackManagerService.getActiveModPackForNation(nationId);
        
        if (activeModPack == null || !activeModPack.equals(modPackId)) {
            sender.sendMessage(ChatColor.RED + "Mod pack '" + modPackId + "' is not currently active for your nation.");
            return true;
        }

        boolean success = modPackManagerService.disableModPackForNation(nationId, modPackId);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Mod pack '" + modPackId + "' disabled for your nation.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to disable mod pack '" + modPackId + "'.");
        }

        return true;
    }

    private boolean handleStatsCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modpackmanager.stats")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        Map<String, Object> stats = modPackManagerService.getModPackStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Mod Pack Statistics" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.AQUA + "Total mod packs: " + ChatColor.WHITE + stats.get("totalModPacks"));
        sender.sendMessage(ChatColor.AQUA + "Nations with active mod packs: " + ChatColor.WHITE + stats.get("activeNations"));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> byLevel = (Map<String, Integer>) stats.get("byCompatibilityLevel");
        if (byLevel != null) {
            sender.sendMessage(ChatColor.GRAY + "By compatibility level:");
            for (Map.Entry<String, Integer> entry : byLevel.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> nationsByModPack = (Map<String, Integer>) stats.get("nationsByModPack");
        if (nationsByModPack != null) {
            sender.sendMessage(ChatColor.GRAY + "Nations by mod pack:");
            for (Map.Entry<String, Integer> entry : nationsByModPack.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue() + " nations");
            }
        }

        // If player, show nation-specific stats
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String nationId = getPlayerNationId(player);
            if (nationId != null) {
                Map<String, Object> nationStats = modPackManagerService.getNationModPackStats(nationId);
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Your Nation's Mod Pack" + ChatColor.GOLD + " ===");
                sender.sendMessage(ChatColor.AQUA + "Active mod pack: " + ChatColor.WHITE + 
                    nationStats.get("activeModPack"));
                
                if (nationStats.get("activeModPackId") != null) {
                    sender.sendMessage(ChatColor.AQUA + "Integration rules: " + ChatColor.WHITE + 
                        nationStats.get("activeIntegrationRules") + "/" + 
                        nationStats.get("totalIntegrationRules"));
                }
            }
        }

        return true;
    }

    private boolean handleAvailableCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modpackmanager.list")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        List<ModPackManagerService.ModPack> available;
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String nationId = getPlayerNationId(player);
            if (nationId == null) {
                sender.sendMessage(ChatColor.RED + "You must be in a nation to use this command.");
                return true;
            }
            available = modPackManagerService.getCompatibleModPacks();
        } else {
            available = (List<ModPackManagerService.ModPack>) modPackManagerService.getAvailableModPacks();
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Available Mod Packs" + ChatColor.GOLD + " ===");
        
        for (ModPackManagerService.ModPack modPack : available) {
            sender.sendMessage(ChatColor.AQUA + modPack.name);
            sender.sendMessage(ChatColor.GRAY + "  " + modPack.description);
            if (sender instanceof Player) {
                String isActive = modPackManagerService.getActiveModPackForNation(getPlayerNationId((Player)sender))
                    .equals(modPack.id) ? ChatColor.GREEN + " [ACTIVE]" : "";
                sender.sendMessage(ChatColor.GRAY + "  Status: " + (modPack.enabled ? ChatColor.WHITE + "Enabled" : ChatColor.RED + "Disabled") + isActive);
            }
            sender.sendMessage("");
        }

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.modpackmanager.admin")) {
            sender.sendMessage(ChatColor.RED + "Insufficient permissions.");
            return true;
        }

        modPackManagerService.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Mod pack configuration reloaded.");
        return true;
    }

    /**
     * Helper method to get player's nation ID
     */
    private String getPlayerNationId(Player player) {
        return plugin.getNationManager().getNationOfPlayer(player.getUniqueId())
            .map(n -> n.getId())
            .orElse(null);
    }
}
