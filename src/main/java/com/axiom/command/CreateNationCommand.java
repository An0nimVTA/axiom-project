package com.axiom.command;

import com.axiom.model.Nation;
import com.axiom.service.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /axiom nation create <name> */
public class CreateNationCommand implements CommandExecutor {

    public CreateNationCommand() {
        // Constructor is now empty
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Игрок только.");
            return true;
        }
        if (args.length < 3 || !args[0].equalsIgnoreCase("nation") || !args[1].equalsIgnoreCase("create")) {
            sender.sendMessage("§cИспользование: /axiom nation create <name>");
            return true;
        }
        
        Player p = (Player) sender;

        PlayerDataManager playerDataManager = ServiceLocator.get(PlayerDataManager.class);
        BalancingService balancingService = ServiceLocator.get(BalancingService.class);
        EconomyService economyService = ServiceLocator.get(EconomyService.class);
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        VisualEffectsService visualEffectsService = ServiceLocator.get(VisualEffectsService.class);

        String existing = playerDataManager.getNation(p.getUniqueId());
        if (existing != null) {
            sender.sendMessage("§cВы уже состоите в нации.");
            return true;
        }
        
        // ANTI-GRIEF: Check cooldown
        if (balancingService != null) {
            if (!balancingService.canCreateNation(p.getUniqueId())) {
                sender.sendMessage("§cВы уже создавали нацию. Кулдаун: 7 дней.");
                return true;
            }
        }
        
        String name = args[2];
        try {
            double start = 10000.0; // Default value, consider moving to a config service
            String currency = economyService.getDefaultCurrencyCode();
            var nation = nationManager.createNation(p, name, currency, start);
            // Persist player affiliation
            playerDataManager.setNation(p.getUniqueId(), nation.getId(), Nation.Role.LEADER.name());
            
            // Record creation
            if (balancingService != null) {
                balancingService.recordNationCreation(p.getUniqueId());
            }
            
            sender.sendMessage("§aНация '" + name + "' создана! Вы — лидер.");
            
            // VISUAL EFFECTS: Celebrate nation creation
            visualEffectsService.playNationJoinEffect(p);
        } catch (Exception e) {
            sender.sendMessage("§cОшибка: " + e.getMessage());
        }
        return true;
    }
}


