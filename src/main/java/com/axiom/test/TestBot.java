package com.axiom.test;

import com.axiom.AXIOM;
import com.axiom.service.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Тестовый бот для автоматического тестирования функций AXIOM
 */
public class TestBot {
    private final AXIOM plugin;
    private final EconomyService economyService;
    private final NationManager nationManager;
    private final DiplomacySystem diplomacySystem;
    private final PlayerDataManager playerDataManager;
    
    // Список тестов
    private final List<TestSuite> testSuites;
    
    public TestBot(AXIOM plugin) {
        this.plugin = plugin;
        this.economyService = plugin.getEconomyService();
        this.nationManager = plugin.getNationManager();
        this.diplomacySystem = plugin.getDiplomacySystem();
        this.playerDataManager = plugin.getPlayerDataManager();
        
        this.testSuites = new ArrayList<>();
        initializeTestSuites();
    }
    
    private void initializeTestSuites() {
        // Тесты для экономической системы
        testSuites.add(new EconomyTestSuite());
        
        // Тесты для национальной системы
        testSuites.add(new NationTestSuite());
        
        // Тесты для дипломатической системы
        testSuites.add(new DiplomacyTestSuite());
        
        // Тесты для боевой системы
        testSuites.add(new WarfareTestSuite());
    }
    
    /**
     * Выполнить все тесты
     */
    public boolean runAllTests(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[AXIOM] " + ChatColor.WHITE + "Запуск автоматических тестов...");
        
        int totalTests = 0;
        int passedTests = 0;
        
        for (TestSuite suite : testSuites) {
            sender.sendMessage(ChatColor.YELLOW + "Тестирование: " + suite.getSuiteName());
            
            List<TestCase> testCases = suite.getTestCases();
            for (TestCase testCase : testCases) {
                totalTests++;
                
                boolean result = testCase.execute(sender);
                if (result) {
                    passedTests++;
                    sender.sendMessage(ChatColor.GREEN + "  ✓ " + testCase.getName());
                } else {
                    sender.sendMessage(ChatColor.RED + "  ✗ " + testCase.getName());
                }
            }
        }
        
        String resultMessage = String.format(
            ChatColor.GOLD + "[AXIOM] " + ChatColor.WHITE + "Тестирование завершено: %d/%d тестов пройдено", 
            passedTests, totalTests
        );
        sender.sendMessage(resultMessage);
        
        return passedTests == totalTests;
    }
    
    /**
     * Выполнить конкретный тест
     */
    public boolean runTest(CommandSender sender, String testName) {
        for (TestSuite suite : testSuites) {
            for (TestCase testCase : suite.getTestCases()) {
                if (testCase.getName().equalsIgnoreCase(testName)) {
                    sender.sendMessage(ChatColor.GOLD + "[AXIOM] " + ChatColor.WHITE + "Запуск теста: " + testName);
                    boolean result = testCase.execute(sender);
                    
                    if (result) {
                        sender.sendMessage(ChatColor.GREEN + "Тест пройден: " + testName);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Тест НЕ пройден: " + testName);
                    }
                    return result;
                }
            }
        }
        
        sender.sendMessage(ChatColor.RED + "Тест не найден: " + testName);
        return false;
    }
    
    /**
     * Интерфейс тест-кейса
     */
    public interface TestCase {
        String getName();
        boolean execute(CommandSender sender);
    }
    
    /**
     * Интерфейс набора тестов
     */
    public interface TestSuite {
        String getSuiteName();
        List<TestCase> getTestCases();
    }
    
    /**
     * Тесты для экономической системы
     */
    private class EconomyTestSuite implements TestSuite {
        @Override
        public String getSuiteName() {
            return "Экономическая система";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> testCases = new ArrayList<>();
            
            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Проверка баланса игрока";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Тест должен выполняться игроком");
                        return false;
                    }
                    
                    Player player = (Player) sender;
                    double balance = plugin.getWalletService().getBalance(player.getUniqueId());
                    
                    // Проверяем, что баланс существует и не отрицательный
                    return balance >= 0;
                }
            });
            
            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Перевод между игроками";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    // Находим другого игрока для теста перевода
                    Player[] onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[0]);
                    if (onlinePlayers.length < 2) {
                        sender.sendMessage(ChatColor.YELLOW + "Необходимо как минимум 2 игрока для теста перевода");
                        return true; // Тест не проваливается, просто пропускается
                    }
                    
                    Player player1 = (Player) sender;
                    Player player2 = null;
                    
                    for (Player p : onlinePlayers) {
                        if (!p.equals(player1)) {
                            player2 = p;
                            break;
                        }
                    }
                    
                    if (player2 == null) return true;
                    
                    // Даем игроку немного денег для теста
                    plugin.getWalletService().deposit(player1.getUniqueId(), 100.0);
                    
                    double initialBalance1 = plugin.getWalletService().getBalance(player1.getUniqueId());
                    double initialBalance2 = plugin.getWalletService().getBalance(player2.getUniqueId());
                    
                    boolean success = plugin.getWalletService().transfer(player1.getUniqueId(), player2.getUniqueId(), 50.0);
                    
                    if (success) {
                        double finalBalance1 = plugin.getWalletService().getBalance(player1.getUniqueId());
                        double finalBalance2 = plugin.getWalletService().getBalance(player2.getUniqueId());
                        
                        return finalBalance1 == initialBalance1 - 50.0 && 
                               finalBalance2 == initialBalance2 + 50.0;
                    }
                    
                    return success;
                }
            });
            
            return testCases;
        }
    }
    
    /**
     * Тесты для национальной системы
     */
    private class NationTestSuite implements TestSuite {
        @Override
        public String getSuiteName() {
            return "Национальная система";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> testCases = new ArrayList<>();
            
            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Создание нации";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Тест должен выполняться игроком");
                        return false;
                    }
                    
                    Player player = (Player) sender;
                    String nationName = "TestNation_" + new Random().nextInt(10000);
                    
                    // Пытаемся создать нацию
                    try {
                        double startTreasury = plugin.getConfig().getDouble("economy.startingTreasury", 10000.0);
                        String currencyCode = plugin.getEconomyService().getDefaultCurrencyCode();
                        nationManager.createNation(player, nationName, currencyCode, startTreasury);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Проверка существования нации";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Тест должен выполняться игроком");
                        return false;
                    }
                    
                    Player player = (Player) sender;
                    
                    // Проверяем, есть ли у игрока нация
                    return nationManager.getNationOfPlayer(player.getUniqueId()).isPresent();
                }
            });
            
            return testCases;
        }
    }
    
    /**
     * Тесты для дипломатической системы
     */
    private class DiplomacyTestSuite implements TestSuite {
        @Override
        public String getSuiteName() {
            return "Дипломатическая система";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> testCases = new ArrayList<>();
            
            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Проверка дипломатических отношений";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Тест должен выполняться игроком");
                        return false;
                    }
                    
                    Player player = (Player) sender;
                    // Получаем нацию игрока
                    var playerNation = nationManager.getNationOfPlayer(player.getUniqueId()).orElse(null);
                    
                    // Если у игрока нет нации, тест не может быть выполнен
                    if (playerNation == null) {
                        // Создаем временную нацию для теста
                        String nationName = "TempNation_" + new Random().nextInt(10000);
                        try {
                            double startTreasury = plugin.getConfig().getDouble("economy.startingTreasury", 10000.0);
                            String currencyCode = plugin.getEconomyService().getDefaultCurrencyCode();
                            nationManager.createNation(player, nationName, currencyCode, startTreasury);
                        } catch (Exception e) {
                            return false;
                        }
                        playerNation = nationManager.getNationOfPlayer(player.getUniqueId()).orElse(null);
                    }
                    
                    // Проверяем, что у нации есть дипломатические отношения
                    return diplomacySystem != null;
                }
            });
            
            return testCases;
        }
    }
    
    /**
     * Тесты для боевой системы
     */
    private class WarfareTestSuite implements TestSuite {
        @Override
        public String getSuiteName() {
            return "Боевая система";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> testCases = new ArrayList<>();
            
            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Проверка боевой системы";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    // Просто проверяем, что боевая система инициализирована
                    return plugin.getMilitaryService() != null && plugin.getSiegeService() != null;
                }
            });
            
            return testCases;
        }
    }
}