package com.axiom.controller;

import com.axiom.AXIOM;
import com.axiom.service.MilitaryServiceInterface;
import com.axiom.service.EconomyServiceInterface;
import com.axiom.service.NationManager;
import com.axiom.util.DataValidator;
import com.axiom.util.CacheManager;
import com.axiom.exception.ValidationException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.UUID;
import java.util.Optional;
import java.util.Map;

/**
 * Контроллер для обработки военных команд
 */
public class MilitaryController implements CommandExecutor {
    
    private final AXIOM plugin;
    private final MilitaryServiceInterface militaryService;
    private final EconomyServiceInterface economyService;
    private final NationManager nationManager;
    private final CacheManager cacheManager;
    private CacheManager.Cache<String, Map<String, Object>> statsCache;
    
    public MilitaryController(AXIOM plugin,
                           MilitaryServiceInterface militaryService,
                           EconomyServiceInterface economyService,
                           NationManager nationManager,
                           CacheManager cacheManager) {
        this.plugin = plugin;
        this.militaryService = militaryService;
        this.economyService = economyService;
        this.nationManager = nationManager;
        this.cacheManager = cacheManager;
        
        // Инициализация кэша для статистики (5 минут, max 100 записей)
        this.statsCache = cacheManager.createCache("military_stats", 300, 100);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(DataValidator.formatErrorMessage("Эту команду может выполнить только игрок"));
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        try {
            // Получение нации игрока
            String nationId = nationManager.getPlayerNationId(playerId);
            if (nationId == null) {
                player.sendMessage(DataValidator.formatErrorMessage("У вас нет нации"));
                return true;
            }
            
            // Валидация ID нации
            DataValidator.validateNationId(nationId);
            
            if (args.length == 0) {
                showHelp(player);
                return true;
            }
            
            switch (args[0].toLowerCase()) {
                case "recruit":
                    return handleRecruit(player, nationId, args);
                case "stats":
                    return handleStats(player, nationId);
                case "disband":
                    return handleDisband(player, nationId, args);
                case "upgrade":
                    return handleUpgrade(player, nationId, args);
                default:
                    showHelp(player);
                    return true;
            }
        } catch (ValidationException e) {
            player.sendMessage(DataValidator.formatErrorMessage(e.getMessage()));
            return true;
        } catch (Exception e) {
            player.sendMessage(DataValidator.formatErrorMessage("Произошла ошибка: " + e.getMessage()));
            plugin.getLogger().warning("Error in MilitaryController: " + e.getMessage());
            return true;
        }
    }
    
    private boolean handleRecruit(Player player, String nationId, String[] args) {
        try {
            if (args.length < 4) {
                player.sendMessage(DataValidator.formatErrorMessage("Использование: /military recruit <type> <count> <cost>"));
                return true;
            }
            
            // Валидация типа войск
            String unitType = args[1];
            DataValidator.validateUnitType(unitType);
            
            // Валидация и парсинг количества
            int count;
            try {
                count = Integer.parseInt(args[2]);
                DataValidator.validatePositiveInteger(count, "Count");
            } catch (NumberFormatException e) {
                player.sendMessage(DataValidator.formatErrorMessage("Неверный формат количества"));
                return true;
            }
            
            // Валидация и парсинг стоимости
            double cost;
            try {
                cost = Double.parseDouble(args[3]);
                DataValidator.validatePositiveNumber(cost, "Cost");
            } catch (NumberFormatException e) {
                player.sendMessage(DataValidator.formatErrorMessage("Неверный формат стоимости"));
                return true;
            }
            
            // Проверка бюджета
            double totalCost = count * cost;
            double balance = economyService.getGDP(nationId); // Временное решение
            
            if (balance < totalCost) {
                player.sendMessage(DataValidator.formatErrorMessage("Недостаточно средств. Требуется: " + totalCost));
                return true;
            }
            
            // Наем войск
            String result = militaryService.recruitUnits(nationId, unitType, count, cost);
            player.sendMessage(DataValidator.formatSuccessMessage(result));
            
            // Очистка кэша статистики
            statsCache.remove(nationId);
            
            // Показать обновленную статистику
            showStats(player, nationId);
            
            return true;
        } catch (ValidationException e) {
            player.sendMessage(DataValidator.formatErrorMessage(e.getMessage()));
            return true;
        } catch (Exception e) {
            player.sendMessage(DataValidator.formatErrorMessage("Ошибка при найме войск: " + e.getMessage()));
            return true;
        }
    }
    
    private boolean handleStats(Player player, String nationId) {
        try {
            // Проверка кэша
            Optional<Map<String, Object>> cachedStats = statsCache.get(nationId);
            Map<String, Object> stats;
            
            if (cachedStats.isPresent()) {
                stats = cachedStats.get();
                player.sendMessage(DataValidator.formatInfoMessage("Статистика из кэша"));
            } else {
                // Загрузка статистики
                stats = militaryService.getMilitaryStatistics(nationId);
                // Сохранение в кэш
                statsCache.put(nationId, stats);
                player.sendMessage(DataValidator.formatInfoMessage("Статистика загружена"));
            }
            
            player.sendMessage(ChatColor.GOLD + "=== Военная статистика ===");
            player.sendMessage(ChatColor.YELLOW + "Общие единицы: " + stats.get("totalUnits"));
            player.sendMessage(ChatColor.YELLOW + "Боевая мощь: " + stats.get("strength"));
            player.sendMessage(ChatColor.YELLOW + "Пехота: " + stats.get("infantry"));
            player.sendMessage(ChatColor.YELLOW + "Кавалерия: " + stats.get("cavalry"));
            player.sendMessage(ChatColor.YELLOW + "Артиллерия: " + stats.get("artillery"));
            player.sendMessage(ChatColor.YELLOW + "Флот: " + stats.get("navy"));
            player.sendMessage(ChatColor.YELLOW + "Авиация: " + stats.get("airForce"));
            
            return true;
        } catch (Exception e) {
            player.sendMessage(DataValidator.formatErrorMessage("Ошибка при получении статистики: " + e.getMessage()));
            return true;
        }
    }
    
    private boolean handleDisband(Player player, String nationId, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Использование: /military disband <type> <count>");
            return true;
        }
        
        String unitType = args[1];
        int count;
        
        try {
            count = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Неверный формат числа");
            return true;
        }
        
        try {
            String result = militaryService.disbandUnits(nationId, unitType, count);
            player.sendMessage(ChatColor.GREEN + result);
            showStats(player, nationId);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Ошибка: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleUpgrade(Player player, String nationId, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Использование: /military upgrade <type> <count> <cost>");
            return true;
        }
        
        String unitType = args[1];
        int count;
        double cost;
        
        try {
            count = Integer.parseInt(args[2]);
            cost = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Неверный формат числа");
            return true;
        }
        
        try {
            String result = militaryService.upgradeUnits(nationId, unitType, count, cost);
            player.sendMessage(ChatColor.GREEN + result);
            showStats(player, nationId);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Ошибка: " + e.getMessage());
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Военные команды ===");
        player.sendMessage(ChatColor.YELLOW + "/military recruit <type> <count> <cost>" + ChatColor.GRAY + " - Нанять войска");
        player.sendMessage(ChatColor.YELLOW + "/military stats" + ChatColor.GRAY + " - Показать статистику");
        player.sendMessage(ChatColor.YELLOW + "/military disband <type> <count>" + ChatColor.GRAY + " - Расформировать войска");
        player.sendMessage(ChatColor.YELLOW + "/military upgrade <type> <count> <cost>" + ChatColor.GRAY + " - Улучшить войска");
        player.sendMessage(ChatColor.GRAY + "Типы войск: infantry, cavalry, artillery, navy, airforce");
    }
    
    private void showStats(Player player, String nationId) {
        Map<String, Object> stats = militaryService.getMilitaryStatistics(nationId);
        double strength = (double) stats.get("strength");
        int capacity = militaryService.getMilitaryCapacity(nationId);
        int totalUnits = (int) stats.get("totalUnits");
        
        player.sendMessage(ChatColor.GOLD + "=== Текущее состояние ===");
        player.sendMessage(ChatColor.YELLOW + "Боевая мощь: " + String.format("%.1f", strength));
        player.sendMessage(ChatColor.YELLOW + "Единицы: " + totalUnits + "/" + capacity);
        player.sendMessage(ChatColor.YELLOW + "Содержание: " + 
                         String.format("%.2f", militaryService.calculateMaintenanceCost(nationId)) + " в час");
    }
}
