package com.axiom.test;

import com.axiom.AXIOM;
import com.axiom.domain.service.state.*;
import com.axiom.domain.service.politics.*;
import com.axiom.domain.service.industry.*;
import com.axiom.domain.service.military.*;
import com.axiom.domain.service.technology.*;
import com.axiom.domain.service.infrastructure.*;
import com.axiom.service.adapter.*;
import com.axiom.domain.repo.*;
import com.axiom.infra.persistence.*;
import com.axiom.util.*;
import com.axiom.domain.model.*;
import com.axiom.exception.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Автоматический тестовый бот
 * Проводит комплексное тестирование всех компонентов проекта
 */
public class AutoTestBot {
    
    private final AXIOM plugin;
    private final TestReporter reporter;
    private final List<TestSuite> testSuites = new ArrayList<>();
    
    public AutoTestBot(AXIOM plugin) {
        this.plugin = plugin;
        this.reporter = new TestReporter(plugin);
        initializeTestSuites();
    }
    
    /**
     * Инициализация всех тестовых сюитов
     */
    private void initializeTestSuites() {
        boolean safeMode = AutotestSupport.isSafeMode();
        testSuites.add(new ServiceTestSuite());
        testSuites.add(new RepositoryTestSuite());
        testSuites.add(new CacheTestSuite());
        testSuites.add(new ValidationTestSuite());
        testSuites.add(new IntegrationTestSuite());
        if (!safeMode) {
            testSuites.add(new PerformanceTestSuite());
        } else {
            plugin.getLogger().info("AutoTestBot: safe mode enabled, skipping PerformanceTestSuite");
        }
    }

    public List<TestSuite> getTestSuites() {
        return Collections.unmodifiableList(testSuites);
    }
    
    /**
     * Запустить все тесты
     * @param sender отправитель команд (для вывода результатов)
     * @return true если все тесты пройдены успешно
     */
    public boolean runAllTests(CommandSender sender) {
        reporter.startReport(sender);
        
        AtomicInteger totalTests = new AtomicInteger();
        AtomicInteger passedTests = new AtomicInteger();
        
        for (TestSuite suite : testSuites) {
            reporter.startSuite(sender, suite.getName());
            
            List<TestCase> testCases = suite.getTestCases();
            for (TestCase testCase : testCases) {
                totalTests.incrementAndGet();
                long startedAt = System.currentTimeMillis();
                try {
                    boolean result = testCase.execute(sender);
                    long durationMs = System.currentTimeMillis() - startedAt;
                    if (result) {
                        passedTests.incrementAndGet();
                        reporter.testPassed(sender, testCase.getName(), durationMs);
                    } else {
                        reporter.testFailed(sender, testCase.getName(), "Тест вернул false", durationMs);
                    }
                } catch (Exception e) {
                    long durationMs = System.currentTimeMillis() - startedAt;
                    reporter.testFailed(sender, testCase.getName(), e.getMessage(), durationMs);
                }
            }
            
            reporter.endSuite(sender, suite.getName());
        }
        
        return reporter.endReport(sender, totalTests.get(), passedTests.get());
    }
    
    /**
     * Запустить конкретный тест
     * @param sender отправитель команд
     * @param testName название теста
     * @return true если тест пройден успешно
     */
    public boolean runTest(CommandSender sender, String testName) {
        for (TestSuite suite : testSuites) {
            for (TestCase testCase : suite.getTestCases()) {
                if (testCase.getName().equalsIgnoreCase(testName)) {
                    reporter.startSingleTest(sender, testName);
                    long startedAt = System.currentTimeMillis();
                    try {
                        boolean result = testCase.execute(sender);
                        long durationMs = System.currentTimeMillis() - startedAt;
                        if (result) {
                            reporter.testPassed(sender, testName, durationMs);
                            return true;
                        } else {
                            reporter.testFailed(sender, testName, "Тест вернул false", durationMs);
                            return false;
                        }
                    } catch (Exception e) {
                        long durationMs = System.currentTimeMillis() - startedAt;
                        reporter.testFailed(sender, testName, e.getMessage(), durationMs);
                        return false;
                    }
                }
            }
        }
        
        sender.sendMessage(ChatColor.RED + "Тест не найден: " + testName);
        return false;
    }
    
    /**
     * Запустить тесты для конкретного сюита
     * @param sender отправитель команд
     * @param suiteName название сюита
     * @return true если все тесты сюита пройдены успешно
     */
    public boolean runSuite(CommandSender sender, String suiteName) {
        for (TestSuite suite : testSuites) {
            if (suite.getName().equalsIgnoreCase(suiteName)) {
                reporter.startSuite(sender, suite.getName());
                
                AtomicInteger total = new AtomicInteger();
                AtomicInteger passed = new AtomicInteger();
                
                for (TestCase testCase : suite.getTestCases()) {
                    total.incrementAndGet();
                    
                    long startedAt = System.currentTimeMillis();
                    try {
                        boolean result = testCase.execute(sender);
                        long durationMs = System.currentTimeMillis() - startedAt;
                        if (result) {
                            passed.incrementAndGet();
                            reporter.testPassed(sender, testCase.getName(), durationMs);
                        } else {
                            reporter.testFailed(sender, testCase.getName(), "Тест вернул false", durationMs);
                        }
                    } catch (Exception e) {
                        long durationMs = System.currentTimeMillis() - startedAt;
                        reporter.testFailed(sender, testCase.getName(), e.getMessage(), durationMs);
                    }
                }
                
                return reporter.endSuite(sender, suite.getName());
            }
        }
        
        sender.sendMessage(ChatColor.RED + "Тестовый сюит не найден: " + suiteName);
        return false;
    }
    
    /**
     * Интерфейс тестового сюита
     */
    public interface TestSuite {
        String getName();
        List<TestCase> getTestCases();
    }
    
    /**
     * Интерфейс тестового кейса
     */
    public interface TestCase {
        String getName();
        boolean execute(CommandSender sender) throws Exception;
    }
    
    /**
     * Тесты сервисов
     */
    private class ServiceTestSuite implements TestSuite {
        @Override
        public String getName() {
            return "Service Tests";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> tests = new ArrayList<>();
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "MilitaryService Interface Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    MilitaryServiceInterface service = new MilitaryServiceAdapter(plugin);
                    return service != null;
                }
            });
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "SiegeService Interface Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    SiegeServiceInterface service = new SiegeServiceAdapter(plugin);
                    return service != null;
                }
            });
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "EconomyService Interface Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    EconomyServiceInterface service = new EconomyServiceAdapter(plugin);
                    return service != null;
                }
            });
            
            return tests;
        }
    }
    
    /**
     * Тесты репозиториев
     */
    private class RepositoryTestSuite implements TestSuite {
        @Override
        public String getName() {
            return "Repository Tests";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> tests = new ArrayList<>();
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "NationRepository CRUD Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) throws Exception {
                    NationRepository repo = new JsonNationRepository(plugin);
                    
                    // Create
                    Nation testNation = new Nation("test_repo_", "Test Nation", UUID.randomUUID(), "AXC", 1000.0);
                    repo.save(testNation);
                    
                    // Read
                    Optional<Nation> found = repo.findById("test_repo_");
                    if (!found.isPresent()) return false;
                    
                    // Update
                    testNation.setName("Updated Nation");
                    repo.save(testNation);
                    
                    // Delete
                    repo.delete("test_repo_");
                    
                    return !repo.findById("test_repo_").isPresent();
                }
            });
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "MilitaryRepository CRUD Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) throws Exception {
                    MilitaryRepository repo = new JsonMilitaryRepository(plugin);
                    
                    // Create
                    MilitaryData testData = new MilitaryData("test_military_");
                    testData.setInfantry(1000);
                    repo.save(testData);
                    
                    // Read
                    Optional<MilitaryData> found = repo.findByNationId("test_military_");
                    if (!found.isPresent()) return false;
                    
                    // Update
                    testData.setInfantry(2000);
                    repo.save(testData);
                    
                    // Delete
                    repo.delete("test_military_");
                    
                    return !repo.findByNationId("test_military_").isPresent();
                }
            });
            
            return tests;
        }
    }
    
    /**
     * Тесты кэширования
     */
    private class CacheTestSuite implements TestSuite {
        @Override
        public String getName() {
            return "Cache Tests";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> tests = new ArrayList<>();
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "CacheManager Basic Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    CacheManager cacheManager = new CacheManager(plugin);
                    CacheManager.Cache<String, String> cache = cacheManager.createCache("test", 60, 10);
                    
                    // Test put and get
                    cache.put("key1", "value1");
                    Optional<String> result = cache.get("key1");
                    
                    return result.isPresent() && result.get().equals("value1");
                }
            });
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "Cache TTL Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) throws InterruptedException {
                    CacheManager cacheManager = new CacheManager(plugin);
                    CacheManager.Cache<String, String> cache = cacheManager.createCache("test_ttl", 1, 10); // 1 second TTL
                    
                    cache.put("key1", "value1");
                    Thread.sleep(1500); // Wait for TTL to expire
                    
                    return !cache.get("key1").isPresent();
                }
            });
            
            return tests;
        }
    }
    
    /**
     * Тесты валидации
     */
    private class ValidationTestSuite implements TestSuite {
        @Override
        public String getName() {
            return "Validation Tests";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> tests = new ArrayList<>();
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "Nation ID Validation Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    try {
                        DataValidator.validateNationId("valid_nation_123");
                        DataValidator.validateNationId("another-valid-id");
                        
                        // Should throw exception
                        try {
                            DataValidator.validateNationId("invalid id with spaces");
                            return false;
                        } catch (ValidationException e) {
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "Unit Type Validation Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    try {
                        DataValidator.validateUnitType("infantry");
                        DataValidator.validateUnitType("CAVALRY");
                        
                        // Should throw exception
                        try {
                            DataValidator.validateUnitType("invalid_type");
                            return false;
                        } catch (ValidationException e) {
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            return tests;
        }
    }
    
    /**
     * Интеграционные тесты
     */
    private class IntegrationTestSuite implements TestSuite {
        @Override
        public String getName() {
            return "Integration Tests";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> tests = new ArrayList<>();
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "Military Service Integration Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    try {
                        // Create components
                        NationRepository nationRepo = new JsonNationRepository(plugin);
                        MilitaryRepository militaryRepo = new JsonMilitaryRepository(plugin);
                        MilitaryServiceInterface service = new MilitaryServiceAdapter(plugin);
                        
                        // Test integration
                        double strength = service.getMilitaryStrength("test_nation");
                        Map<String, Object> stats = service.getMilitaryStatistics("test_nation");
                        
                        return stats != null;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            return tests;
        }
    }
    
    /**
     * Тесты производительности
     */
    private class PerformanceTestSuite implements TestSuite {
        @Override
        public String getName() {
            return "Performance Tests";
        }
        
        @Override
        public List<TestCase> getTestCases() {
            List<TestCase> tests = new ArrayList<>();
            
            tests.add(new TestCase() {
                @Override
                public String getName() {
                    return "Repository Performance Test";
                }
                
                @Override
                public boolean execute(CommandSender sender) {
                    try {
                        NationRepository repo = new JsonNationRepository(plugin);
                        
                        long startTime = System.currentTimeMillis();
                        List<Nation> nations = repo.findAll();
                        long endTime = System.currentTimeMillis();
                        
                        sender.sendMessage(ChatColor.YELLOW + "Loaded " + nations.size() + 
                                         " nations in " + (endTime - startTime) + "ms");
                        
                        return (endTime - startTime) < 1000; // Should be less than 1 second
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            return tests;
        }
    }
    
    /**
     * Отчет о тестировании
     */
    private static class TestReporter {
        private final AXIOM plugin;
        private long startTime;
        private final List<TestRecord> records = new ArrayList<>();
        private String currentSuite = "Unknown";
        
        public TestReporter(AXIOM plugin) {
            this.plugin = plugin;
        }
        
        public void startReport(CommandSender sender) {
            startTime = System.currentTimeMillis();
            records.clear();
            currentSuite = "Unknown";
            sender.sendMessage(ChatColor.GOLD + "=== AUTO TEST BOT ===");
            sender.sendMessage(ChatColor.YELLOW + "Starting comprehensive testing...");
        }
        
        public void startSuite(CommandSender sender, String suiteName) {
            currentSuite = suiteName;
            sender.sendMessage(ChatColor.BLUE + "Running suite: " + suiteName);
        }
        
        public void testPassed(CommandSender sender, String testName) {
            testPassed(sender, testName, 0);
        }

        public void testPassed(CommandSender sender, String testName, long durationMs) {
            records.add(new TestRecord(currentSuite, testName, true, "", durationMs));
            sender.sendMessage(ChatColor.GREEN + "  ✓ " + testName);
        }
        
        public void testFailed(CommandSender sender, String testName, String error) {
            testFailed(sender, testName, error, 0);
        }

        public void testFailed(CommandSender sender, String testName, String error, long durationMs) {
            String reason = error == null ? "" : error;
            records.add(new TestRecord(currentSuite, testName, false, reason, durationMs));
            sender.sendMessage(ChatColor.RED + "  ✗ " + testName + ": " + error);
        }
        
        public boolean endSuite(CommandSender sender, String suiteName) {
            sender.sendMessage(ChatColor.BLUE + "Completed suite: " + suiteName);
            return true;
        }
        
        public void startSingleTest(CommandSender sender, String testName) {
            sender.sendMessage(ChatColor.YELLOW + "Running test: " + testName);
        }
        
        public boolean endReport(CommandSender sender, int totalTests, int passedTests) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            sender.sendMessage(ChatColor.GOLD + "=== TEST RESULTS ===");
            sender.sendMessage(ChatColor.YELLOW + "Total tests: " + totalTests);
            sender.sendMessage(ChatColor.YELLOW + "Passed: " + passedTests);
            sender.sendMessage(ChatColor.YELLOW + "Failed: " + (totalTests - passedTests));
            sender.sendMessage(ChatColor.YELLOW + "Duration: " + duration + "ms");
            
            if (passedTests == totalTests) {
                sender.sendMessage(ChatColor.GREEN + "All tests passed! 🎉");
                saveReport(totalTests, passedTests, duration);
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Some tests failed! ❌");
                saveReport(totalTests, passedTests, duration);
                return false;
            }
        }

        private void saveReport(int totalTests, int passedTests, long durationMs) {
            try {
                File reportsDir = new File(plugin.getDataFolder(), "test-reports");
                reportsDir.mkdirs();

                SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String timestamp = fileFormat.format(new Date());
                File reportFile = new File(reportsDir, "autotest-report_" + timestamp + ".json");

                Map<String, Object> report = new LinkedHashMap<>();
                report.put("type", "auto-testbot");
                report.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                report.put("axiomVersion", plugin.getDescription().getVersion());
                report.put("totalTests", totalTests);
                report.put("passedTests", passedTests);
                report.put("failedTests", totalTests - passedTests);
                report.put("durationMs", durationMs);

                Map<String, List<TestRecord>> bySuite = new LinkedHashMap<>();
                for (TestRecord record : records) {
                    bySuite.computeIfAbsent(record.suite, k -> new ArrayList<>()).add(record);
                }

                List<Map<String, Object>> suites = new ArrayList<>();
                for (Map.Entry<String, List<TestRecord>> entry : bySuite.entrySet()) {
                    Map<String, Object> suiteMap = new LinkedHashMap<>();
                    suiteMap.put("name", entry.getKey());
                    List<Map<String, Object>> tests = new ArrayList<>();
                    int suiteTotal = 0;
                    int suitePassed = 0;
                    long suiteDuration = 0;
                    for (TestRecord record : entry.getValue()) {
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
                plugin.getLogger().info("AutoTestBot report saved: " + reportFile.getAbsolutePath());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save AutoTestBot report: " + e.getMessage());
            }
        }

        private static class TestRecord {
            private final String suite;
            private final String name;
            private final boolean success;
            private final String error;
            private final long durationMs;
            private final String id;

            private TestRecord(String suite, String name, boolean success, String error, long durationMs) {
                this.suite = suite;
                this.name = name;
                this.success = success;
                this.error = error == null ? "" : error;
                this.durationMs = durationMs;
                this.id = suite + "::" + name;
            }
        }
    }
}
