package com.axiom.app.command;

import com.axiom.AXIOM;
import com.axiom.test.TestBot;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Команда для управления автотест ботом
 */
public class TestBotCommand implements CommandExecutor {
    private final AXIOM plugin;
    private final TestBot testBot;
    
    public TestBotCommand(AXIOM plugin) {
        this.plugin = plugin;
        this.testBot = new TestBot(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axiom.testbot.use")) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
            return true;
        }
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "run":
                return handleRunCommand(sender, args);
            case "list":
                return handleListCommand(sender);
            case "help":
                showHelp(sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Неизвестная подкоманда: " + subCommand);
                showHelp(sender);
                return true;
        }
    }
    
    private boolean handleRunCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Запустить все тесты
            testBot.runAllTests(sender);
            return true;
        } else if (args.length == 2) {
            // Запустить конкретный тест
            String testName = args[1];
            if ("all".equalsIgnoreCase(testName)) {
                testBot.runAllTests(sender);
                return true;
            }
            testBot.runTest(sender, testName);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Использование: /testbot run [название_теста]");
            return true;
        }
    }
    
    private boolean handleListCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[AXIOM] " + ChatColor.WHITE + "Доступные тесты:");
        int suiteIndex = 1;
        for (TestBot.TestSuite suite : testBot.getTestSuites()) {
            var tests = suite.getTestCases();
            sender.sendMessage(ChatColor.AQUA + "  " + suiteIndex + ". " + suite.getSuiteName() +
                ChatColor.GRAY + " (" + tests.size() + ")");
            int testIndex = 1;
            for (TestBot.TestCase test : tests) {
                sender.sendMessage(ChatColor.YELLOW + "     - " + testIndex + ") " + test.getName());
                testIndex++;
            }
            suiteIndex++;
        }
        return true;
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "AXIOM TestBot Commands" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/testbot run" + ChatColor.WHITE + " - Запустить все тесты");
        sender.sendMessage(ChatColor.YELLOW + "/testbot run <тест>" + ChatColor.WHITE + " - Запустить конкретный тест");
        sender.sendMessage(ChatColor.YELLOW + "/testbot list" + ChatColor.WHITE + " - Показать список тестов");
        sender.sendMessage(ChatColor.YELLOW + "/testbot help" + ChatColor.WHITE + " - Показать эту справку");
    }
}
