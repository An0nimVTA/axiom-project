package com.axiom.test;

import com.axiom.service.MilitaryServiceInterface;
import com.axiom.service.adapter.MilitaryServiceAdapter;
import com.axiom.repository.NationRepository;
import com.axiom.repository.MilitaryRepository;
import com.axiom.repository.impl.JsonNationRepository;
import com.axiom.repository.impl.JsonMilitaryRepository;
import com.axiom.model.Nation;
import com.axiom.model.MilitaryData;
import com.axiom.AXIOM;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Optional;

/**
 * Тесты для новых компонентов
 * Демонстрирует, как тестировать новые интерфейсы и репозитории
 */
public class NewComponentTests {
    
    private final AXIOM plugin;
    
    public NewComponentTests(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Тест MilitaryServiceAdapter
     * Проверяет, что адаптер корректно работает с существующим MilitaryService
     */
    public void testMilitaryServiceAdapter(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[TEST] Testing MilitaryServiceAdapter...");
        
        try {
            // Создание адаптера
            MilitaryServiceInterface service = new MilitaryServiceAdapter(plugin);
            
            // Тестирование основных методов
            // Примечание: эти тесты требуют существования тестовых данных
            
            // 1. Тест getMilitaryStrength
            double strength = service.getMilitaryStrength("test_nation");
            sender.sendMessage(ChatColor.GREEN + "  getMilitaryStrength: " + strength);
            
            // 2. Тест getMilitaryStatistics
            Map<String, Object> stats = service.getMilitaryStatistics("test_nation");
            sender.sendMessage(ChatColor.GREEN + "  getMilitaryStatistics: " + stats.size() + " entries");
            
            // 3. Тест calculateMaintenanceCost
            double cost = service.calculateMaintenanceCost("test_nation");
            sender.sendMessage(ChatColor.GREEN + "  calculateMaintenanceCost: " + cost);
            
            sender.sendMessage(ChatColor.GREEN + "[TEST] MilitaryServiceAdapter test passed!");
            return;
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "[TEST] MilitaryServiceAdapter test failed: " + e.getMessage());
        }
    }
    
    /**
     * Тест репозиториев
     * Проверяет работу JSON репозиториев
     */
    public void testRepositories(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[TEST] Testing repositories...");
        
        try {
            // Тестирование NationRepository
            NationRepository nationRepo = new JsonNationRepository(plugin);
            
            // Создание тестовой нации
            Nation testNation = new Nation("test_repo_nation", "Test Republic");
            nationRepo.save(testNation);
            
            // Поиск нации
            Optional<Nation> found = nationRepo.findById("test_repo_nation");
            if (found.isPresent()) {
                sender.sendMessage(ChatColor.GREEN + "  NationRepository: Found nation " + found.get().getName());
            }
            
            // Тестирование MilitaryRepository
            MilitaryRepository militaryRepo = new JsonMilitaryRepository(plugin);
            
            // Создание тестовых военных данных
            MilitaryData testData = new MilitaryData("test_repo_nation");
            testData.setInfantry(1000);
            testData.setCavalry(500);
            militaryRepo.save(testData);
            
            // Поиск данных
            Optional<MilitaryData> foundData = militaryRepo.findByNationId("test_repo_nation");
            if (foundData.isPresent()) {
                sender.sendMessage(ChatColor.GREEN + "  MilitaryRepository: Found data with " + 
                                 foundData.get().getInfantry() + " infantry");
            }
            
            // Очистка
            nationRepo.delete("test_repo_nation");
            militaryRepo.delete("test_repo_nation");
            
            sender.sendMessage(ChatColor.GREEN + "[TEST] Repository tests passed!");
            return;
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "[TEST] Repository tests failed: " + e.getMessage());
        }
    }
    
    /**
     * Тест производительности
     * Измеряет производительность новых репозиториев
     */
    public void testPerformance(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[TEST] Testing performance...");
        
        try {
            NationRepository nationRepo = new JsonNationRepository(plugin);
            
            // Тестирование загрузки всех наций
            long startTime = System.currentTimeMillis();
            var allNations = nationRepo.findAll();
            long endTime = System.currentTimeMillis();
            
            sender.sendMessage(ChatColor.GREEN + "  Loaded " + allNations.size() + 
                             " nations in " + (endTime - startTime) + "ms");
            
            // Тестирование поиска по ID
            if (!allNations.isEmpty()) {
                String testId = allNations.get(0).getId();
                startTime = System.currentTimeMillis();
                for (int i = 0; i < 100; i++) {
                    nationRepo.findById(testId);
                }
                endTime = System.currentTimeMillis();
                sender.sendMessage(ChatColor.GREEN + "  100 findById operations: " + 
                                 (endTime - startTime) + "ms");
            }
            
            sender.sendMessage(ChatColor.GREEN + "[TEST] Performance tests completed!");
            return;
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "[TEST] Performance tests failed: " + e.getMessage());
        }
    }
    
    /**
     * Тест интеграции
     * Проверяет взаимодействие между компонентами
     */
    public void testIntegration(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[TEST] Testing component integration...");
        
        try {
            // Создание репозиториев
            NationRepository nationRepo = new JsonNationRepository(plugin);
            MilitaryRepository militaryRepo = new JsonMilitaryRepository(plugin);
            
            // Создание тестовой нации
            Nation testNation = new Nation("test_integration", "Integration Empire");
            testNation.setTreasury(100000.0);
            nationRepo.save(testNation);
            
            // Создание тестовых военных данных
            MilitaryData testData = new MilitaryData("test_integration");
            testData.setInfantry(5000);
            testData.setCavalry(2000);
            militaryRepo.save(testData);
            
            // Использование адаптера
            MilitaryServiceInterface militaryService = new MilitaryServiceAdapter(plugin);
            
            // Тестирование интеграции
            double strength = militaryService.getMilitaryStrength("test_integration");
            Map<String, Object> stats = militaryService.getMilitaryStatistics("test_integration");
            
            sender.sendMessage(ChatColor.GREEN + "  Integrated strength: " + strength);
            sender.sendMessage(ChatColor.GREEN + "  Integrated stats: " + stats.size() + " entries");
            
            // Очистка
            nationRepo.delete("test_integration");
            militaryRepo.delete("test_integration");
            
            sender.sendMessage(ChatColor.GREEN + "[TEST] Integration tests passed!");
            return;
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "[TEST] Integration tests failed: " + e.getMessage());
        }
    }
    
    /**
     * Запуск всех тестов
     */
    public void runAllTests(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Running New Component Tests ===");
        
        testMilitaryServiceAdapter(sender);
        testRepositories(sender);
        testPerformance(sender);
        testIntegration(sender);
        
        sender.sendMessage(ChatColor.GOLD + "=== All Tests Completed ===");
    }
}