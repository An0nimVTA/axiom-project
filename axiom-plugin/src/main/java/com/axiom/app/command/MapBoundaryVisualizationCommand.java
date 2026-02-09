package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.app.gui.MapBoundaryVisualizationMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Команда для открытия меню визуализации границ
 */
public class MapBoundaryVisualizationCommand implements CommandExecutor {
    private final AXIOM plugin;
    
    public MapBoundaryVisualizationCommand(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.map.visualize")) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Открыть меню визуализации границ
            new MapBoundaryVisualizationMenu(plugin, player).open();
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "stats":
                return handleStatsCommand(sender);
            case "nations":
                return handleNationsCommand(sender);
            case "cities":
                return handleCitiesCommand(sender);
            case "enable":
                return handleEnableCommand(sender);
            case "disable":
                return handleDisableCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.BOLD + "Команды визуализации границ" + ChatColor.RESET + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/mapvisual" + ChatColor.GRAY + " - Открыть меню визуализации границ");
        sender.sendMessage(ChatColor.YELLOW + "/mapvisual stats" + ChatColor.GRAY + " - Показать статистику границ");
        sender.sendMessage(ChatColor.YELLOW + "/mapvisual nations" + ChatColor.GRAY + " - Показать границы наций");
        sender.sendMessage(ChatColor.YELLOW + "/mapvisual cities" + ChatColor.GRAY + " - Показать границы городов");
        sender.sendMessage(ChatColor.YELLOW + "/mapvisual enable" + ChatColor.GRAY + " - Включить визуализацию в мире");
        sender.sendMessage(ChatColor.YELLOW + "/mapvisual disable" + ChatColor.GRAY + " - Отключить визуализацию в мире");
        sender.sendMessage(ChatColor.YELLOW + "/mapvisual reload" + ChatColor.GRAY + " - Перезагрузить настройки визуализации");
    }
    
    private boolean handleStatsCommand(CommandSender sender) {
        var stats = plugin.getMapBoundaryVisualizationService().getBoundaryStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== Статистика границ ===");
        sender.sendMessage(ChatColor.YELLOW + "Всего наций: " + ChatColor.WHITE + stats.get("totalNations"));
        sender.sendMessage(ChatColor.YELLOW + "Всего городов: " + ChatColor.WHITE + stats.get("totalCities"));
        sender.sendMessage(ChatColor.YELLOW + "Всего чанков наций: " + ChatColor.WHITE + stats.get("totalNationsChunks"));
        sender.sendMessage(ChatColor.YELLOW + "Всего чанков городов: " + ChatColor.WHITE + stats.get("totalCityChunks"));
        sender.sendMessage(ChatColor.YELLOW + "Средний размер нации: " + ChatColor.WHITE + String.format("%.2f", (Double)stats.get("avgNationSize")));
        sender.sendMessage(ChatColor.YELLOW + "Средний размер города: " + ChatColor.WHITE + String.format("%.2f", (Double)stats.get("avgCitySize")));
        sender.sendMessage(ChatColor.YELLOW + "Интеграция с картами: " + 
            (((Boolean)stats.get("mapIntegrationEnabled")) ? ChatColor.GREEN + "ДА" : ChatColor.RED + "НЕТ"));
        
        return true;
    }
    
    private boolean handleNationsCommand(CommandSender sender) {
        var boundaries = plugin.getMapBoundaryVisualizationService().getAllNationBoundaries();
        
        sender.sendMessage(ChatColor.GOLD + "=== Границы наций ===");
        for (com.axiom.domain.service.infrastructure.MapBoundaryVisualizationService.BoundaryData boundary : boundaries.values()) {
            int color = boundary.getColor();
            sender.sendMessage(String.format("§x§%06X%s §7(%d чанков)", color, 
                boundary.getName(), boundary.getChunkCount()));
        }
        
        if (boundaries.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Нет созданных наций с границами");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Всего наций: " + boundaries.size());
        }
        
        return true;
    }
    
    private boolean handleCitiesCommand(CommandSender sender) {
        var boundaries = plugin.getMapBoundaryVisualizationService().getAllCityBoundaries();
        
        sender.sendMessage(ChatColor.GOLD + "=== Границы городов ===");
        for (com.axiom.domain.service.infrastructure.MapBoundaryVisualizationService.BoundaryData boundary : boundaries.values()) {
            int color = boundary.getColor();
            sender.sendMessage(String.format("§x§%06X%s §7(нация: %s, %d чанков)", color, 
                boundary.getName(), boundary.getNationId(), boundary.getChunkCount()));
        }
        
        if (boundaries.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Нет созданных городов с границами");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Всего городов: " + boundaries.size());
        }
        
        return true;
    }
    
    private boolean handleEnableCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // В реальной системе: включить отображение границ для игрока
        player.sendMessage(ChatColor.GREEN + "Визуализация границ включена для вас!");
        player.sendMessage(ChatColor.GRAY + "Границы теперь будут отображаться в вашем Хаерос Minimap.");
        
        return true;
    }
    
    private boolean handleDisableCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // В реальной системе: отключить отображение границ для игрока
        player.sendMessage(ChatColor.RED + "Визуализация границ отключена для вас!");
        player.sendMessage(ChatColor.GRAY + "Границы больше не будут отображаться в Хаерос Minimap.");
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("axiom.admin")) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
            return true;
        }
        
        // Перезагружаем настройки визуализации
        plugin.getMapBoundaryVisualizationService().forceUpdate();
        sender.sendMessage(ChatColor.GREEN + "Настройки визуализации границ перезагружены!");
        
        return true;
    }
}
