package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.domain.service.state.NationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /axiom unclaim */
public class UnclaimCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final NationManager nationManager;

    public UnclaimCommand(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.nation.claim")) {
            sender.sendMessage("§cНедостаточно прав.");
            return true;
        }
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        if (args.length >= 1 && !args[0].equalsIgnoreCase("unclaim")) {
            sender.sendMessage("§cИспользование: /unclaim");
            return true;
        }
        Player p = (Player) sender;
        if (!AXIOM.getInstance().getDoubleClickService().shouldProceed(p.getUniqueId(), "unclaim")) {
            p.sendMessage("§7Нажмите ещё раз в течение 5 сек для подтверждения.");
            return true;
        }
        try {
            String msg = nationManager.unclaimChunk(p);
            p.sendMessage(msg);
        } catch (Exception e) { p.sendMessage("§cОшибка: " + e.getMessage()); }
        return true;
    }
}


