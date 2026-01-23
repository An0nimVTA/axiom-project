package com.axiom.command;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.NationManager;
import com.axiom.service.PlayerDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /axiom nation create <name> */
public class CreateNationCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final PlayerDataManager playerDataManager;

    public CreateNationCommand(AXIOM plugin, NationManager nationManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        if (args.length < 3 || !args[0].equalsIgnoreCase("nation") || !args[1].equalsIgnoreCase("create")) {
            sender.sendMessage("§cИспользование: /axiom nation create <name>");
            return true;
        }
        Player p = (Player) sender;
        String existing = playerDataManager.getNation(p.getUniqueId());
        if (existing != null) { sender.sendMessage("§cВы уже состоите в нации."); return true; }
        
        // ANTI-GRIEF: Check cooldown
        if (plugin.getBalancingService() != null) {
            if (!plugin.getBalancingService().canCreateNation(p.getUniqueId())) {
                sender.sendMessage("§cВы уже создавали нацию. Кулдаун: 7 дней.");
                return true;
            }
        }
        
        String name = args[2];
        try {
            double start = plugin.getConfig().getDouble("economy.startingTreasury", 10000.0);
            String currency = plugin.getEconomyService().getDefaultCurrencyCode();
            var nation = nationManager.createNation(p, name, currency, start);
            // Persist player affiliation
            playerDataManager.setNation(p.getUniqueId(), nation.getId(), Nation.Role.LEADER.name());
            
            // Record creation
            if (plugin.getBalancingService() != null) {
                plugin.getBalancingService().recordNationCreation(p.getUniqueId());
            }
            
            sender.sendMessage("§aНация '" + name + "' создана! Вы — лидер.");
            
            // VISUAL EFFECTS: Celebrate nation creation
            plugin.getVisualEffectsService().playNationJoinEffect(p);
        } catch (Exception e) {
            sender.sendMessage("§cОшибка: " + e.getMessage());
        }
        return true;
    }
}


