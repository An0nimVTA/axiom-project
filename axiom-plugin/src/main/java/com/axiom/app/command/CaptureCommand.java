package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.domain.service.military.CountryCaptureService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Команда для управления системой захвата стран и городов
 */
public class CaptureCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final CountryCaptureService captureService;
    
    public CaptureCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.captureService = plugin.getCountryCaptureService();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.capture.use")) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игроки могут использовать эту команду!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "begin_siege":
                return handleBeginSiegeCommand(player, args);
            case "damage_core":
                return handleDamageCoreCommand(player, args);
            case "capture_chunk":
                return handleCaptureChunkCommand(player);
            case "info":
                return handleInfoCommand(player, args);
            case "trenches":
                return handleTrenchesCommand(player);
            case "list_cores":
                return handleListCoresCommand(player);
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Система захвата ядер ===");
        player.sendMessage(ChatColor.YELLOW + "/capture begin_siege <нация> - начать осаду страны");
        player.sendMessage(ChatColor.YELLOW + "/capture damage_core <типа> <id> <урон> - нанести урон ядру (country/city)");
        player.sendMessage(ChatColor.YELLOW + "/capture capture_chunk - захватить чанк для окопов");
        player.sendMessage(ChatColor.YELLOW + "/capture info <типа> <id> - информация о ядре (country/city)");
        player.sendMessage(ChatColor.YELLOW + "/capture trenches - информация о ваших укреплениях");
        player.sendMessage(ChatColor.YELLOW + "/capture list_cores - список всех ядер");
    }
    
    private boolean handleBeginSiegeCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /capture begin_siege <нация>");
            return true;
        }
        
        String attackerNationId = plugin.getNationManager().getNationIdOfPlayer(player.getUniqueId());
        if (attackerNationId == null) {
            player.sendMessage(ChatColor.RED + "Вы должны состоять в нации для начала осады!");
            return true;
        }
        
        String defenderNationId = args[1];
        
        boolean success = captureService.beginSiege(attackerNationId, defenderNationId);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Вы начали осаду страны " + defenderNationId + "!");
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось начать осаду этой страны!");
        }
        
        return true;
    }
    
    private boolean handleDamageCoreCommand(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Использование: /capture damage_core <типа> <id> <урон>");
            player.sendMessage(ChatColor.RED + "Типа: country, city");
            return true;
        }
        
        String type = args[1].toLowerCase();
        String coreId = args[2];
        int damage;
        
        try {
            damage = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Урон должен быть числом!");
            return true;
        }
        
        if (type.equals("country")) {
            boolean destroyed = captureService.damageCountryCore(coreId, damage);
            if (destroyed) {
                player.sendMessage(ChatColor.RED + "Ядро страны " + coreId + " РАЗРУШЕНО!");
            } else {
                player.sendMessage(ChatColor.GREEN + "Нанесен урон ядру страны " + coreId + ": " + damage);
            }
        } else if (type.equals("city")) {
            boolean destroyed = captureService.damageCityCore(coreId, damage);
            if (destroyed) {
                player.sendMessage(ChatColor.RED + "Ядро города " + coreId + " РАЗРУШЕНО!");
            } else {
                player.sendMessage(ChatColor.GREEN + "Нанесен урон ядру города " + coreId + ": " + damage);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Неверный тип! Используйте: country или city");
        }
        
        return true;
    }
    
    private boolean handleCaptureChunkCommand(Player player) {
        org.bukkit.Chunk chunk = player.getLocation().getChunk();
        String nationId = plugin.getNationManager().getNationIdOfPlayer(player.getUniqueId());
        
        if (nationId == null) {
            player.sendMessage(ChatColor.RED + "Вы должны состоять в нации!");
            return true;
        }
        
        boolean success = captureService.captureChunkForTrenches(chunk, nationId, player);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Чанк успешно захвачен для укреплений!");
        }
        
        return true;
    }
    
    private boolean handleInfoCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Использование: /capture info <типа> <id>");
            player.sendMessage(ChatColor.RED + "Типа: country, city");
            return true;
        }
        
        String type = args[1].toLowerCase();
        String coreId = args[2];
        
        if (type.equals("country")) {
            var core = captureService.getCountryCore(coreId);
            if (core != null) {
                player.sendMessage(ChatColor.GOLD + "=== Информация о ядре страны ===");
                player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + core.getId());
                player.sendMessage(ChatColor.YELLOW + "Название: " + ChatColor.WHITE + core.getNationName());
                player.sendMessage(ChatColor.YELLOW + "Стабильность: " + ChatColor.WHITE + core.getStability() + "/" + core.getMaxStability());
                player.sendMessage(ChatColor.YELLOW + "Военная сила: " + ChatColor.WHITE + core.getMilitaryStrength());
                player.sendMessage(ChatColor.YELLOW + "Здоровье правительственных построек: " + 
                    ChatColor.WHITE + core.getGovernmentBuildingHealth() + "/" + core.getMaxGovernmentBuildingHealth());
                player.sendMessage(ChatColor.YELLOW + "Под угрозой: " + 
                    (core.isUnderThreat() ? ChatColor.RED + "ДА" : ChatColor.GREEN + "НЕТ"));
            } else {
                player.sendMessage(ChatColor.RED + "Ядро страны с ID " + coreId + " не найдено!");
            }
        } else if (type.equals("city")) {
            var core = captureService.getCityCore(coreId);
            if (core != null) {
                player.sendMessage(ChatColor.GOLD + "=== Информация о ядре города ===");
                player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + core.getId());
                player.sendMessage(ChatColor.YELLOW + "Город: " + ChatColor.WHITE + core.getCityName());
                player.sendMessage(ChatColor.YELLOW + "Нация: " + ChatColor.WHITE + core.getNationId());
                player.sendMessage(ChatColor.YELLOW + "Уровень: " + ChatColor.WHITE + core.getLevel());
                player.sendMessage(ChatColor.YELLOW + "Здоровье: " + ChatColor.WHITE + core.getHealth() + "/" + core.getMaxHealth());
            } else {
                player.sendMessage(ChatColor.RED + "Ядро города с ID " + coreId + " не найдено!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Неверный тип! Используйте: country или city");
        }
        
        return true;
    }
    
    private boolean handleTrenchesCommand(Player player) {
        String nationId = plugin.getNationManager().getNationIdOfPlayer(player.getUniqueId());
        if (nationId == null) {
            player.sendMessage(ChatColor.RED + "Вы должны состоять в нации!");
            return true;
        }
        
        var trenches = captureService.getTrenchChunksForNation(nationId);
        player.sendMessage(ChatColor.GOLD + "=== Ваши укрепления ===");
        player.sendMessage(ChatColor.YELLOW + "Количество укрепленных чанков: " + ChatColor.WHITE + trenches.size());
        
        if (!trenches.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "Позиции укреплений:");
            int count = 0;
            for (org.bukkit.Chunk chunk : trenches) {
                if (count++ < 10) { // Показываем только первые 10
                    player.sendMessage(ChatColor.AQUA + "  Чанк: " + chunk.getX() + ", " + chunk.getZ());
                }
            }
            if (trenches.size() > 10) {
                player.sendMessage(ChatColor.GRAY + "  ... и еще " + (trenches.size() - 10) + " чанков");
            }
        }
        
        return true;
    }
    
    private boolean handleListCoresCommand(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Список всех ядер ===");
        
        player.sendMessage(ChatColor.YELLOW + "Ядра стран:");
        for (var core : captureService.getAllCountryCores()) {
            player.sendMessage(ChatColor.AQUA + "- " + core.getNationName() + " (" + core.getId() + ")");
        }
        
        player.sendMessage(ChatColor.YELLOW + "Ядра городов:");
        for (var core : captureService.getAllCityCores()) {
            player.sendMessage(ChatColor.GREEN + "- " + core.getCityName() + " (" + core.getId() + 
                             ", нация: " + core.getNationId() + ")");
        }
        
        return true;
    }
}
