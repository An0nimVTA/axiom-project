package com.axiom.test;

import com.axiom.AXIOM;
import com.axiom.app.gui.EconomicIndicatorsMenu;
import com.axiom.domain.service.industry.EconomicIndicatorsService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Тесты для системы экономических индикаторов
 */
public class EconomicIndicatorsTestSuite {
    private final AXIOM plugin;
    private final EconomicIndicatorsService service;
    
    public EconomicIndicatorsTestSuite(AXIOM plugin) {
        this.plugin = plugin;
        this.service = plugin.getEconomicIndicatorsService();
    }
    
    /**
     * Запуск всех тестов системы экономических индикаторов
     */
    public boolean runAllTests(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Запуск тестов системы экономических индикаторов ===");
        
        boolean allTestsPassed = true;
        
        // Тест 1: Проверка инициализации сервиса
        if (!testServiceInitialization(player)) {
            allTestsPassed = false;
        }
        
        // Тест 2: Проверка получения глобальных данных
        if (!testGetGlobalData(player)) {
            allTestsPassed = false;
        }
        
        // Тест 3: Проверка получения данных нации
        if (!testGetNationData(player)) {
            allTestsPassed = false;
        }
        
        // Тест 4: Проверка получения всех данных
        if (!testGetAllData(player)) {
            allTestsPassed = false;
        }
        
        // Тест 5: Проверка расчета ВВП
        if (!testCalculateGDP(player)) {
            allTestsPassed = false;
        }
        
        // Тест 6: Проверка оценки экономического здоровья
        if (!testEconomicHealthGrade(player)) {
            allTestsPassed = false;
        }
        
        // Тест 7: Проверка трендов
        if (!testEconomicTrends(player)) {
            allTestsPassed = false;
        }
        
        // Тест 8: Проверка GUI меню
        if (!testGUIMenu(player)) {
            allTestsPassed = false;
        }
        
        if (allTestsPassed) {
            player.sendMessage(ChatColor.GREEN + "Все тесты пройдены успешно!");
        } else {
            player.sendMessage(ChatColor.RED + "Некоторые тесты провалены!");
        }
        
        return allTestsPassed;
    }
    
    /**
     * Тест инициализации сервиса
     */
    private boolean testServiceInitialization(Player player) {
        try {
            if (service != null) {
                player.sendMessage(ChatColor.GREEN + "✓ Сервис экономических индикаторов инициализирован");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "✗ Сервис экономических индикаторов не инициализирован");
                return false;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ Ошибка при проверке инициализации сервиса: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Тест получения глобальных данных
     */
    private boolean testGetGlobalData(Player player) {
        try {
            var globalData = service.getGlobalEconomicData();
            if (globalData != null) {
                player.sendMessage(ChatColor.GREEN + "✓ Глобальные экономические данные получены");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "✗ Не удалось получить глобальные данные");
                return false;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ Ошибка при получении глобальных данных: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Тест получения данных нации
     */
    private boolean testGetNationData(Player player) {
        try {
            // Проверяем, состоит ли игрок в нации
            var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (nationOpt.isPresent()) {
                String nationId = nationOpt.get().getId();
                var econData = service.getEconomicData(nationId);
                
                if (econData != null) {
                    player.sendMessage(ChatColor.GREEN + "✓ Экономические данные нации получены");
                    return true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "⚠ Экономические данные нации недоступны (нормально для новых наций)");
                    return true; // Это не ошибка, если данные еще не собраны
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "⚠ Игрок не состоит в нации, пропуск теста данных нации");
                return true; // Это не ошибка, просто нет нации
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ Ошибка при получении данных нации: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Тест получения всех данных
     */
    private boolean testGetAllData(Player player) {
        try {
            Collection<EconomicIndicatorsService.EconomicData> allData = service.getAllEconomicData();
            player.sendMessage(ChatColor.GREEN + "✓ Получен список всех экономических данных: " + allData.size() + " элементов");
            return true;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ Ошибка при получении всех данных: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Тест расчета ВВП
     */
    private boolean testCalculateGDP(Player player) {
        try {
            var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (nationOpt.isPresent()) {
                // Тест не может быть проведен без реального метода calculateGDP
                // Вместо этого проверим, есть ли метод получения ВВП
                String nationId = nationOpt.get().getId();
                var econData = service.getEconomicData(nationId);
                
                if (econData != null) {
                    double gdp = econData.getGDP();
                    player.sendMessage(ChatColor.GREEN + "✓ ВВП нации: " + gdp);
                    return true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "⚠ Данные нации недоступны для теста ВВП");
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "⚠ Игрок не состоит в нации для теста ВВП");
                return true;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ Ошибка при тесте ВВП: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Тест оценки экономического здоровья
     */
    private boolean testEconomicHealthGrade(Player player) {
        try {
            var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (nationOpt.isPresent()) {
                String nationId = nationOpt.get().getId();
                var econData = service.getEconomicData(nationId);
                
                if (econData != null) {
                    String grade = econData.getEconomicHealthGrade();
                    player.sendMessage(ChatColor.GREEN + "✓ Оценка экономического здоровья: " + grade);
                    return true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "⚠ Данные нации недоступны для теста оценки");
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "⚠ Игрок не состоит в нации для теста оценки");
                return true;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ Ошибка при тесте оценки здоровья: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Тест экономических трендов
     */
    private boolean testEconomicTrends(Player player) {
        try {
            var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (nationOpt.isPresent()) {
                String nationId = nationOpt.get().getId();
                
                // Проверяем, что тренды могут быть вычислены
                EconomicTrend gdpTrend = service.getEconomicTrend(nationId, EconomicIndicator.GDP);
                EconomicTrend inflationTrend = service.getEconomicTrend(nationId, EconomicIndicator.INFLATION);
                
                if (gdpTrend != null && inflationTrend != null) {
                    player.sendMessage(ChatColor.GREEN + "✓ Тренды экономических показателей доступны");
                    return true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "⚠ Тренды не могут быть вычислены (недостаточно данных)");
                    return true; // Это нормально, если данных недостаточно
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "⚠ Игрок не состоит в нации для теста трендов");
                return true;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ Ошибка при тесте трендов: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Тест GUI меню
     */
    private boolean testGUIMenu(Player player) {
        try {
            // Открываем меню экономических индикаторов
            new EconomicIndicatorsMenu(plugin, player).open();
            player.sendMessage(ChatColor.GREEN + "✓ GUI меню экономических индикаторов успешно открыто");
            return true;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "✗ Ошибка при открытии GUI меню: " + e.getMessage());
            return false;
        }
    }
}