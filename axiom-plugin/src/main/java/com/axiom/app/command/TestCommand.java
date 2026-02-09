package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.test.AutoTestBot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

/**
 * Команда для запуска автоматического тестирования
 */
public class TestCommand implements CommandExecutor {
    
    private final AXIOM plugin;
    private AutoTestBot testBot;
    
    public TestCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.testBot = new AutoTestBot(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.test")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды");
            return true;
        }
        
        if (args.length == 0) {
            // Запуск всех тестов
            sender.sendMessage(ChatColor.GOLD + "Запуск всех автоматических тестов...");
            boolean success = testBot.runAllTests(sender);
            
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Все тесты пройдены успешно!");
            } else {
                sender.sendMessage(ChatColor.RED + "Некоторые тесты не пройдены. Смотрите логи выше.");
            }
            
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "suite":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /test suite <suiteName>");
                    return true;
                }
                
                String suiteName = args[1];
                sender.sendMessage(ChatColor.YELLOW + "Запуск тестового сюита: " + suiteName);
                testBot.runSuite(sender, suiteName);
                break;
                
            case "test":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /test test <testName>");
                    return true;
                }
                
                String testName = args[1];
                sender.sendMessage(ChatColor.YELLOW + "Запуск теста: " + testName);
                testBot.runTest(sender, testName);
                break;
                
            case "list":
                listAvailableTests(sender);
                break;
                
            default:
                showHelp(sender);
        }
        
        return true;
    }
    
    private void listAvailableTests(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Доступные тестовые сюиты ===");
        int suiteIndex = 1;
        for (AutoTestBot.TestSuite suite : testBot.getTestSuites()) {
            sender.sendMessage(ChatColor.YELLOW + "" + suiteIndex + ". " + suite.getName() +
                ChatColor.GRAY + " (" + suite.getTestCases().size() + ")");
            suiteIndex++;
        }

        sender.sendMessage(ChatColor.GOLD + "\nИспользование:");
        sender.sendMessage(ChatColor.YELLOW + "/test suite <suiteName>" + ChatColor.GRAY + " - Запустить конкретный сюит");
        sender.sendMessage(ChatColor.YELLOW + "/test test <testName>" + ChatColor.GRAY + " - Запустить конкретный тест");
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Автоматическое тестирование ===");
        sender.sendMessage(ChatColor.YELLOW + "/test" + ChatColor.GRAY + " - Запустить все тесты");
        sender.sendMessage(ChatColor.YELLOW + "/test suite <suiteName>" + ChatColor.GRAY + " - Запустить конкретный сюит");
        sender.sendMessage(ChatColor.YELLOW + "/test test <testName>" + ChatColor.GRAY + " - Запустить конкретный тест");
        sender.sendMessage(ChatColor.YELLOW + "/test list" + ChatColor.GRAY + " - Показать доступные тесты");
    }
}
