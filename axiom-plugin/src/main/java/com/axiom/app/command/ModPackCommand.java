package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.app.gui.ModPackBuilderMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Команда для открытия меню конструктора модпаков
 */
public class ModPackCommand implements CommandExecutor {
    private final AXIOM plugin;
    
    public ModPackCommand(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.modpack.builder")) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игроки могут использовать эту команду!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Открываем GUI меню
            ModPackBuilderMenu modPackMenu = new ModPackBuilderMenu(plugin, player);
            modPackMenu.open();
            return true;
        }
        
        // Обработка аргументов
        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {
                case "create":
                    return handleCreateCommand(player, args);
                case "list":
                    return handleListCommand(player);
                case "info":
                    return handleInfoCommand(player, args);
                case "enable":
                    return handleEnableCommand(player, args);
                case "disable":
                    return handleDisableCommand(player, args);
                case "build":
                    return handleBuildCommand(player, args);
                case "download":
                    return handleDownloadCommand(player, args);
                default:
                    player.sendMessage(ChatColor.RED + "Использование: /modpack [create|list|info|enable|disable|build|download]");
                    return true;
            }
        }
        
        return true;
    }
    
    private boolean handleCreateCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Использование: /modpack create <id> <название> [описание]");
            return true;
        }
        
        String id = args[1];
        String name = args[2];
        String description = args.length > 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : "Новый модпак";
        
        boolean success = plugin.getModPackBuilderService().createModPack(
            player, id, name, description, "1.0.0", "modern" // стандартный тип - современный
        );
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Модпак '" + name + "' успешно создан!");
        } else {
            player.sendMessage(ChatColor.RED + "Ошибка при создании модпака! Возможно ID уже занят.");
        }
        
        return true;
    }
    
    private boolean handleListCommand(Player player) {
        var allModPacks = plugin.getModPackBuilderService().getAllModPacks();
        
        player.sendMessage(ChatColor.GOLD + "=== Доступные модпаки ===");
        for (var modPack : allModPacks) {
            ChatColor color = modPack.getCreatedBy() != null && modPack.getCreatedBy().equals(player.getName()) ? 
                ChatColor.AQUA : ChatColor.WHITE;
            player.sendMessage(color + "- " + ChatColor.YELLOW + modPack.getName() + 
                             ChatColor.GRAY + " (" + modPack.getId() + ") " +
                             ChatColor.DARK_GRAY + "[" + modPack.getServerType() + "]");
        }
        
        return true;
    }
    
    private boolean handleInfoCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /modpack info <id>");
            return true;
        }
        
        String id = args[1];
        var modPack = plugin.getModPackBuilderService().getModPack(id);
        
        if (modPack == null) {
            player.sendMessage(ChatColor.RED + "Модпак с ID '" + id + "' не найден!");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Информация о модпаке ===");
        player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + modPack.getId());
        player.sendMessage(ChatColor.YELLOW + "Название: " + ChatColor.WHITE + modPack.getName());
        player.sendMessage(ChatColor.YELLOW + "Описание: " + ChatColor.WHITE + modPack.getDescription());
        player.sendMessage(ChatColor.YELLOW + "Версия: " + ChatColor.WHITE + modPack.getVersion());
        player.sendMessage(ChatColor.YELLOW + "Тип сервера: " + ChatColor.WHITE + modPack.getServerType());
        player.sendMessage(ChatColor.YELLOW + "Создатель: " + ChatColor.WHITE + modPack.getCreatedBy());
        player.sendMessage(ChatColor.YELLOW + "Создан: " + ChatColor.WHITE + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(modPack.getCreatedAt())));
        player.sendMessage(ChatColor.YELLOW + "Количество модов: " + ChatColor.WHITE + modPack.getModConfig().size());
        
        // Показываем включенные моды
        int enabledCount = 0;
        for (String mod : modPack.getModConfig().keySet()) {
            if (modPack.isModEnabled(mod)) {
                enabledCount++;
            }
        }
        player.sendMessage(ChatColor.YELLOW + "Включенные моды: " + ChatColor.WHITE + enabledCount);
        
        return true;
    }
    
    private boolean handleEnableCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /modpack enable <мод>");
            return true;
        }
        
        // Для простоты, предположим, что игрок имеет активный модпак
        // В реальности это можно реализовать через сессию игрока
        player.sendMessage(ChatColor.GREEN + "Для включения модов используйте GUI меню!");
        player.sendMessage(ChatColor.GOLD + "Используйте: /modpack чтобы открыть конструктор модпаков");
        
        return true;
    }
    
    private boolean handleDisableCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /modpack disable <мод>");
            return true;
        }
        
        player.sendMessage(ChatColor.GREEN + "Для выключения модов используйте GUI меню!");
        player.sendMessage(ChatColor.GOLD + "Используйте: /modpack чтобы открыть конструктор модпаков");
        
        return true;
    }
    
    private boolean handleBuildCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /modpack build <id>");
            return true;
        }
        
        String id = args[1];
        boolean success = plugin.getModPackBuilderService().buildModPack(id, player);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Модпак '" + id + "' успешно собран!");
            player.sendMessage(ChatColor.GOLD + "Скачать можно по команде: /modpack download " + id);
        } else {
            player.sendMessage(ChatColor.RED + "Ошибка при сборке модпака!");
        }
        
        return true;
    }
    
    private boolean handleDownloadCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /modpack download <id>");
            return true;
        }
        
        String id = args[1];
        // Логика скачивания модпака (в реальности это будет файл)
        player.sendMessage(ChatColor.GREEN + "Модпак '" + id + "' доступен для скачивания!");
        player.sendMessage(ChatColor.GOLD + "В реальности модпак был бы доступен по ссылке: https://cdn.axiom.dev/modpacks/" + id + ".zip");
        
        return true;
    }
}
