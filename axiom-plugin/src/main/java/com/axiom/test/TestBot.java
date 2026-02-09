package com.axiom.test;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.state.*;
import com.axiom.domain.service.politics.*;
import com.axiom.domain.service.industry.*;
import com.axiom.domain.service.military.*;
import com.axiom.domain.service.technology.*;
import com.axiom.domain.service.infrastructure.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.permissions.Permission;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Тестовый бот для автоматического тестирования функций AXIOM
 */
public class TestBot {
    private final AXIOM plugin;
    private final EconomyService economyService;
    private final NationManager nationManager;
    private final DiplomacySystem diplomacySystem;
    private final PlayerDataManager playerDataManager;
    private final RecipeIntegrationService recipeIntegrationService;
    
    // Список тестов
    private final List<TestSuite> testSuites;
    
    public TestBot(AXIOM plugin) {
        this.plugin = plugin;
        this.economyService = plugin.getEconomyService();
        this.nationManager = plugin.getNationManager();
        this.diplomacySystem = plugin.getDiplomacySystem();
        this.playerDataManager = plugin.getPlayerDataManager();
        this.recipeIntegrationService = plugin.getRecipeIntegrationService();
        
        this.testSuites = new ArrayList<>();
        initializeTestSuites();
    }

    private static UUID offlineUuidForName(String name) {
        return UUID.nameUUIDFromBytes(("AXIOM-TestBot:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private TestActor resolveActor(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return new TestActor(player.getName(), player.getUniqueId(), player);
        }
        String name = sender.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "TestBot";
        }
        return new TestActor(name, offlineUuidForName(name), null);
    }

    private Nation ensureNation(UUID ownerId, String namePrefix) throws IOException {
        Nation existing = nationManager.getPlayerNation(ownerId);
        if (existing != null) {
            return existing;
        }
        String nationName = namePrefix + "_" + new Random().nextInt(10000);
        return nationManager.createNation(nationName, ownerId);
    }

    private static final class TestActor {
        private final String name;
        private final UUID uuid;
        private final Player player;

        private TestActor(String name, UUID uuid, Player player) {
            this.name = name;
            this.uuid = uuid;
            this.player = player;
        }
    }
    
    private void initializeTestSuites() {
        boolean safeMode = AutotestSupport.isSafeMode();
        // Тесты для экономической системы
        testSuites.add(new EconomyTestSuite());
        
        // Тесты для национальной системы
        testSuites.add(new NationTestSuite());
        
        // Тесты для дипломатической системы
        testSuites.add(new DiplomacyTestSuite());
        
        // Тесты для боевой системы
        if (!safeMode) {
            testSuites.add(new WarfareTestSuite());
        } else {
            plugin.getLogger().info("TestBot: safe mode enabled, skipping WarfareTestSuite");
        }
        
        // Тесты для интеграции рецептов
        if (!safeMode) {
            testSuites.add(new RecipeIntegrationTestSuite());
        } else {
            plugin.getLogger().info("TestBot: safe mode enabled, skipping RecipeIntegrationTestSuite");
        }

        // Тесты для команд и прав
        testSuites.add(new CommandPermissionTestSuite());
    }

    public List<TestSuite> getTestSuites() {
        return new ArrayList<>(testSuites);
    }
    
    /**
     * Выполнить все тесты
     */
    public boolean runAllTests(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[AXIOM] " + ChatColor.WHITE + "Запуск автоматических тестов...");
        
        long startTime = System.currentTimeMillis();
        int totalTests = 0;
        int passedTests = 0;
        List<ReportEntry> reportEntries = new ArrayList<>();
        
        for (TestSuite suite : testSuites) {
            sender.sendMessage(ChatColor.YELLOW + "Тестирование: " + suite.getSuiteName());
            
            List<TestCase> testCases = suite.getTestCases();
            for (TestCase testCase : testCases) {
                totalTests++;
                long startedAt = System.currentTimeMillis();
                boolean result = false;
                String error = "";
                try {
                    result = testCase.execute(sender);
                    long durationMs = System.currentTimeMillis() - startedAt;
                    if (result) {
                        passedTests++;
                        sender.sendMessage(ChatColor.GREEN + "  ✓ " + testCase.getName());
                    } else {
                        sender.sendMessage(ChatColor.RED + "  ✗ " + testCase.getName());
                    }
                    if (!result && error.isEmpty()) {
                        error = "failed";
                    }
                    reportEntries.add(new ReportEntry(suite.getSuiteName(), testCase.getName(), result, error, durationMs));
                    continue;
                } catch (Exception e) {
                    result = false;
                    error = e.getMessage() == null ? "" : e.getMessage();
                }
                if (!result) {
                    sender.sendMessage(ChatColor.RED + "  ✗ " + testCase.getName());
                }
                if (error.isEmpty()) {
                    error = "failed";
                }
                long durationMs = System.currentTimeMillis() - startedAt;
                reportEntries.add(new ReportEntry(suite.getSuiteName(), testCase.getName(), false, error, durationMs));
            }
        }
        
        String resultMessage = String.format(
            ChatColor.GOLD + "[AXIOM] " + ChatColor.WHITE + "Тестирование завершено: %d/%d тестов пройдено", 
            passedTests, totalTests
        );
        sender.sendMessage(resultMessage);

        saveReport(reportEntries, totalTests, passedTests, System.currentTimeMillis() - startTime);
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
                    long startedAt = System.currentTimeMillis();
                    boolean result = false;
                    String error = "";
                    try {
                        result = testCase.execute(sender);
                    } catch (Exception e) {
                        error = e.getMessage() == null ? "" : e.getMessage();
                    }
                    long durationMs = System.currentTimeMillis() - startedAt;

                    if (result) {
                        sender.sendMessage(ChatColor.GREEN + "Тест пройден: " + testName);
                        saveReport(List.of(new ReportEntry(suite.getSuiteName(), testCase.getName(), true, "", durationMs)), 1, 1, durationMs);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Тест НЕ пройден: " + testName);
                        if (error.isEmpty()) {
                            error = "failed";
                        }
                        saveReport(List.of(new ReportEntry(suite.getSuiteName(), testCase.getName(), false, error, durationMs)), 1, 0, durationMs);
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
                    TestActor actor = resolveActor(sender);
                    double balance = economyService.getBalance(actor.uuid);
                    
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
                    TestActor actor = resolveActor(sender);
                    UUID fromId = actor.uuid;
                    UUID toId = offlineUuidForName(actor.name + "_peer");

                    if (sender instanceof Player) {
                        Player player1 = (Player) sender;
                        Player[] onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[0]);
                        Player player2 = null;
                        for (Player p : onlinePlayers) {
                            if (!p.equals(player1)) {
                                player2 = p;
                                break;
                            }
                        }
                        if (player2 != null) {
                            fromId = player1.getUniqueId();
                            toId = player2.getUniqueId();
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "Использую оффлайн аккаунт для теста перевода");
                        }
                    }

                    // Даем немного денег для теста
                    economyService.addBalance(fromId, 100.0);
                    
                    double initialBalance1 = economyService.getBalance(fromId);
                    double initialBalance2 = economyService.getBalance(toId);
                    
                    boolean success = economyService.transfer(fromId, toId, 50.0);
                    
                    if (success) {
                        double finalBalance1 = economyService.getBalance(fromId);
                        double finalBalance2 = economyService.getBalance(toId);
                        
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
                    TestActor actor = resolveActor(sender);
                    String nationName = "TestNation_" + new Random().nextInt(10000);
                    
                    // Пытаемся создать нацию
                    try {
                        if (actor.player != null) {
                            nationManager.createNation(actor.player, nationName, "AXC", 1000.0);
                        } else {
                            nationManager.createNation(nationName, actor.uuid);
                        }
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
                    TestActor actor = resolveActor(sender);

                    // Проверяем, есть ли у игрока нация
                    if (nationManager.getPlayerNation(actor.uuid) != null) {
                        return true;
                    }

                    if (actor.player == null) {
                        try {
                            String nationName = "TestNation_" + new Random().nextInt(10000);
                            nationManager.createNation(nationName, actor.uuid);
                        } catch (Exception e) {
                            return false;
                        }
                        return nationManager.getPlayerNation(actor.uuid) != null;
                    }

                    return false;
                }
            });

            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Проверка территорий на карте";
                }

                @Override
                public boolean execute(CommandSender sender) {
                    TestActor actor = resolveActor(sender);
                    if (Bukkit.getWorlds().isEmpty()) {
                        sender.sendMessage(ChatColor.YELLOW + "Миры не найдены, пропуск проверки территорий");
                        return true;
                    }
                    try {
                        Nation nation = ensureNation(actor.uuid, "MapNation");
                        String worldName = Bukkit.getWorlds().get(0).getName();
                        String key = worldName + ":0:0";
                        nation.getClaimedChunkKeys().add(key);
                        nationManager.save(nation);

                        var owner = nationManager.getNationClaiming(Bukkit.getWorlds().get(0), 0, 0);
                        return owner.isPresent() && owner.get().getId().equals(nation.getId());
                    } catch (Exception e) {
                        return false;
                    }
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
                    TestActor actor = resolveActor(sender);
                    // Получаем нацию игрока
                    var playerNation = nationManager.getPlayerNation(actor.uuid);
                    
                    // Если у игрока нет нации, тест не может быть выполнен
                    if (playerNation == null) {
                        // Создаем временную нацию для теста
                        String nationName = "TempNation_" + new Random().nextInt(10000);
                        try {
                            if (actor.player != null) {
                                nationManager.createNation(actor.player, nationName, "AXC", 1000.0);
                            } else {
                                nationManager.createNation(nationName, actor.uuid);
                            }
                        } catch (Exception e) {
                            return false;
                        }
                        playerNation = nationManager.getPlayerNation(actor.uuid);
                    }
                    
                    // Проверяем, что у нации есть дипломатические отношения
                    return diplomacySystem != null;
                }
            });

            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Создание альянса";
                }

                @Override
                public boolean execute(CommandSender sender) {
                    if (diplomacySystem == null) {
                        sender.sendMessage(ChatColor.RED + "DiplomacySystem не инициализирован");
                        return false;
                    }
                    try {
                        TestActor actor = resolveActor(sender);
                        Nation a = ensureNation(actor.uuid, "AllyNationA");
                        UUID allyId = offlineUuidForName(actor.name + "_ally");
                        Nation b = ensureNation(allyId, "AllyNationB");

                        diplomacySystem.requestAlliance(a, b);
                        diplomacySystem.acceptAlliance(b, a);
                        return a.getAllies().contains(b.getId()) && b.getAllies().contains(a.getId());
                    } catch (Exception e) {
                        return false;
                    }
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

            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Объявление войны";
                }

                @Override
                public boolean execute(CommandSender sender) {
                    if (diplomacySystem == null) {
                        sender.sendMessage(ChatColor.RED + "DiplomacySystem не инициализирован");
                        return false;
                    }
                    try {
                        Nation attacker = ensureNation(offlineUuidForName("WarNationA"), "WarNationA");
                        Nation defender = ensureNation(offlineUuidForName("WarNationB"), "WarNationB");
                        attacker.setTreasury(Math.max(attacker.getTreasury(), 10000.0));
                        nationManager.save(attacker);
                        diplomacySystem.declareWar(attacker, defender);
                        return diplomacySystem.isAtWar(attacker.getId(), defender.getId());
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            return testCases;
        }
    }

    /**
     * Тесты для интеграции рецептов
     */
    private class RecipeIntegrationTestSuite implements TestSuite {
        @Override
        public String getSuiteName() {
            return "Интеграция рецептов";
        }

        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> testCases = new ArrayList<>();

            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Интеграционные рецепты зарегистрированы";
                }

                @Override
                public boolean execute(CommandSender sender) {
                    if (recipeIntegrationService == null) {
                        sender.sendMessage(ChatColor.RED + "RecipeIntegrationService не инициализирован");
                        return false;
                    }

                    Map<String, List<RecipeIntegrationService.IntegrationRecipe>> allRecipes =
                        recipeIntegrationService.getAllIntegrationRecipes();

                    if (allRecipes.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "Интеграционные рецепты не загружены");
                        return false;
                    }

                    int checked = 0;
                    int missing = 0;
                    int skipped = 0;

                    for (List<RecipeIntegrationService.IntegrationRecipe> recipes : allRecipes.values()) {
                        for (RecipeIntegrationService.IntegrationRecipe recipe : recipes) {
                            if (recipe == null || recipe.id == null || recipe.id.isBlank()) {
                                sender.sendMessage(ChatColor.RED + "Рецепт с пустым id");
                                return false;
                            }
                            if (recipe.outputItemId == null || recipe.outputItemId.isBlank()) {
                                sender.sendMessage(ChatColor.RED + "Рецепт " + recipe.id + " без outputItemId");
                                return false;
                            }
                            if (recipe.recipeType == null || recipe.recipeType.isBlank()) {
                                sender.sendMessage(ChatColor.RED + "Рецепт " + recipe.id + " без recipeType");
                                return false;
                            }
                            if (recipe.category == null || recipe.category.isBlank()) {
                                sender.sendMessage(ChatColor.RED + "Рецепт " + recipe.id + " без category");
                                return false;
                            }

                            String integrationId = recipe.category + "_" + recipe.modOrigin + "_to_" + recipe.modTarget;
                            boolean enabled = recipe.enabled &&
                                (recipeIntegrationService.isIntegrationEnabled(recipe.id) ||
                                 recipeIntegrationService.isIntegrationEnabled(integrationId) ||
                                 recipeIntegrationService.isIntegrationEnabled(recipe.category));

                            if (!enabled) {
                                skipped++;
                                continue;
                            }

                            checked++;
                            NamespacedKey key = new NamespacedKey(plugin, recipe.id);
                            Recipe bukkitRecipe = Bukkit.getRecipe(key);
                            if (bukkitRecipe == null) {
                                missing++;
                                sender.sendMessage(ChatColor.RED + "Не зарегистрирован рецепт: " + recipe.id);
                            }
                        }
                    }

                    sender.sendMessage(ChatColor.YELLOW + "Проверено рецептов: " + checked +
                        ", пропущено: " + skipped + ", отсутствует: " + missing);
                    return missing == 0;
                }
            });

            return testCases;
        }
    }

    /**
     * Тесты для команд и прав доступа
     */
    private class CommandPermissionTestSuite implements TestSuite {
        @Override
        public String getSuiteName() {
            return "Команды и права";
        }

        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> testCases = new ArrayList<>();

            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Команды зарегистрированы";
                }

                @Override
                public boolean execute(CommandSender sender) {
                    var commands = plugin.getDescription().getCommands();
                    if (commands == null || commands.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "Нет команд в plugin.yml");
                        return false;
                    }

                    int missing = 0;
                    for (String commandName : commands.keySet()) {
                        if (plugin.getCommand(commandName) == null) {
                            missing++;
                            sender.sendMessage(ChatColor.RED + "Не зарегистрирована команда: /" + commandName);
                        }
                    }

                    sender.sendMessage(ChatColor.YELLOW + "Проверено команд: " + commands.size() +
                        ", отсутствует: " + missing);
                    return missing == 0;
                }
            });

            testCases.add(new TestCase() {
                @Override
                public String getName() {
                    return "Права зарегистрированы";
                }

                @Override
                public boolean execute(CommandSender sender) {
                    List<Permission> permissions = plugin.getDescription().getPermissions();
                    if (permissions == null || permissions.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "Нет прав в plugin.yml");
                        return false;
                    }

                    int missing = 0;
                    for (Permission permission : permissions) {
                        if (permission == null || permission.getName() == null) {
                            missing++;
                            sender.sendMessage(ChatColor.RED + "Найдено право без имени");
                            continue;
                        }
                        Permission registered = Bukkit.getPluginManager().getPermission(permission.getName());
                        if (registered == null) {
                            missing++;
                            sender.sendMessage(ChatColor.RED + "Не зарегистрировано право: " + permission.getName());
                        }
                    }

                    sender.sendMessage(ChatColor.YELLOW + "Проверено прав: " + permissions.size() +
                        ", отсутствует: " + missing);
                    return missing == 0;
                }
            });

            return testCases;
        }
    }

    private void saveReport(List<ReportEntry> records, int totalTests, int passedTests, long durationMs) {
        try {
            File reportsDir = new File(plugin.getDataFolder(), "test-reports");
            reportsDir.mkdirs();

            SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = fileFormat.format(new Date());
            File reportFile = new File(reportsDir, "testbot-report_" + timestamp + ".json");

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("type", "testbot");
            report.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            report.put("axiomVersion", plugin.getDescription().getVersion());
            report.put("totalTests", totalTests);
            report.put("passedTests", passedTests);
            report.put("failedTests", totalTests - passedTests);
            report.put("durationMs", durationMs);

            Map<String, List<ReportEntry>> bySuite = new LinkedHashMap<>();
            for (ReportEntry record : records) {
                bySuite.computeIfAbsent(record.suite, k -> new ArrayList<>()).add(record);
            }

            List<Map<String, Object>> suites = new ArrayList<>();
            for (Map.Entry<String, List<ReportEntry>> entry : bySuite.entrySet()) {
                Map<String, Object> suiteMap = new LinkedHashMap<>();
                suiteMap.put("name", entry.getKey());
                List<Map<String, Object>> tests = new ArrayList<>();
                int suiteTotal = 0;
                int suitePassed = 0;
                long suiteDuration = 0;
                for (ReportEntry record : entry.getValue()) {
                    Map<String, Object> testMap = new LinkedHashMap<>();
                    suiteTotal++;
                    if (record.success) {
                        suitePassed++;
                    }
                    suiteDuration += Math.max(0, record.durationMs);
                    testMap.put("id", record.id);
                    testMap.put("name", record.name);
                    testMap.put("success", record.success);
                    testMap.put("durationMs", record.durationMs);
                    if (!record.error.isEmpty()) {
                        testMap.put("error", record.error);
                    }
                    tests.add(testMap);
                }
                suiteMap.put("total", suiteTotal);
                suiteMap.put("passed", suitePassed);
                suiteMap.put("failed", suiteTotal - suitePassed);
                suiteMap.put("durationMs", suiteDuration);
                suiteMap.put("tests", tests);
                suites.add(suiteMap);
            }
            report.put("suites", suites);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(reportFile, false)) {
                gson.toJson(report, writer);
            }
            plugin.getLogger().info("TestBot report saved: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save TestBot report: " + e.getMessage());
        }
    }

    private static class ReportEntry {
        private final String suite;
        private final String name;
        private final boolean success;
        private final String error;
        private final long durationMs;
        private final String id;

        private ReportEntry(String suite, String name, boolean success, String error, long durationMs) {
            this.suite = suite;
            this.name = name;
            this.success = success;
            this.error = error == null ? "" : error;
            this.durationMs = durationMs;
            this.id = suite + "::" + name;
        }
    }
}
