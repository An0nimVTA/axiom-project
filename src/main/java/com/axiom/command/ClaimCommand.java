package com.axiom.command;

import com.axiom.service.DoubleClickService;
import com.axiom.service.NationManager;
import com.axiom.service.ServiceLocator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /axiom claim */
public class ClaimCommand implements CommandExecutor {

    public ClaimCommand() {
        // Constructor is now empty
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Игрок только.");
            return true;
        }
        // args[0] is "claim" from AxiomCommand dispatcher, so we don't check it here.
        // This command is now expected to be called from a dispatcher.
        // For simplicity in refactoring, we'll assume it's still direct for now.
        
        Player p = (Player) sender;

        DoubleClickService doubleClickService = ServiceLocator.get(DoubleClickService.class);
        NationManager nationManager = ServiceLocator.get(NationManager.class);

        if (!doubleClickService.shouldProceed(p.getUniqueId(), "claim")) {
            p.sendMessage("§7Нажмите ещё раз в течение 5 сек для подтверждения.");
            return true;
        }
        try {
            String msg = nationManager.claimChunk(p);
            p.sendMessage(msg);
        } catch (Exception e) {
            p.sendMessage("§cОшибка: " + e.getMessage());
        }
        return true;
    }
}


