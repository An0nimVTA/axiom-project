package com.axiom.command;

import com.axiom.AXIOM;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /nation -> open GUI */
public class NationCommandAlias implements CommandExecutor {
    private final AXIOM plugin;
    public NationCommandAlias(AXIOM plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Игрок только."); return true; }
        plugin.openNationMainMenu((Player) sender);
        return true;
    }
}


