package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.app.gui.TechnologyTreeMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Команда для открытия GUI-меню технологического древа
 */
public class TechnologyCommand implements CommandExecutor {
    private final AXIOM plugin;
    
    public TechnologyCommand(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.technology.research")) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игроки могут использовать эту команду!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Проверяем, состоит ли игрок в нации
        String nationId = plugin.getNationManager().getPlayerNationId(player.getUniqueId());
        if (nationId == null) {
            player.sendMessage(ChatColor.RED + "Вы должны состоять в нации, чтобы использовать технологическое древо!");
            return true;
        }
        
        // Открываем GUI-меню технологического древа
        TechnologyTreeMenu techMenu = new TechnologyTreeMenu(plugin, player);
        techMenu.open();
        
        return true;
    }
}
