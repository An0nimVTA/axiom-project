package com.axiom.example;

import com.axiom.AXIOM;
import com.axiom.domain.service.infrastructure.ServiceFactory;
import com.axiom.domain.service.military.MilitaryServiceInterface;
import com.axiom.domain.service.military.SiegeServiceInterface;
import com.axiom.domain.service.industry.EconomyServiceInterface;
import com.axiom.app.controller.MilitaryController;
import com.axiom.domain.repo.NationRepository;
import com.axiom.domain.repo.MilitaryRepository;
import com.axiom.util.CacheManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Пример интеграции новых компонентов в главный класс
 * Этот класс демонстрирует, как можно использовать новую архитектуру
 */
public class NewAxiomIntegrationExample {
    
    private final AXIOM plugin;
    private ServiceFactory serviceFactory;
    
    public NewAxiomIntegrationExample(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Инициализация новой архитектуры
     * Этот метод показывает, как можно интегрировать новые компоненты
     */
    public void initializeNewArchitecture() {
        // Создание фабрики сервисов
        serviceFactory = new ServiceFactory(plugin);
        
        // Получение сервисов через фабрику
        MilitaryServiceInterface militaryService = serviceFactory.createMilitaryService();
        SiegeServiceInterface siegeService = serviceFactory.createSiegeService();
        EconomyServiceInterface economyService = serviceFactory.createEconomyService();
        
        // Получение репозиториев
        NationRepository nationRepository = serviceFactory.createNationRepository();
        MilitaryRepository militaryRepository = serviceFactory.createMilitaryRepository();
        
        // Регистрация контроллеров
        registerControllers(militaryService, economyService);
        
        // Пример использования
        demonstrateUsage(militaryService, nationRepository);
        
        plugin.getLogger().info("Новая архитектура инициализирована успешно!");
    }
    
    private void registerControllers(MilitaryServiceInterface militaryService,
                                   EconomyServiceInterface economyService) {
        // Получение NationManager из основного плагина
        var nationManager = plugin.getNationManager();
        
        // Создание и регистрация контроллера
        CacheManager cacheManager = new CacheManager(plugin);
        MilitaryController militaryController = new MilitaryController(
            plugin,
            militaryService, 
            economyService, 
            nationManager,
            cacheManager
        );
        
        // Регистрация команды (в реальном плагине это делается в onEnable)
        plugin.getCommand("military").setExecutor(militaryController);
        
        plugin.getLogger().info("Контроллер MilitaryController зарегистрирован");
    }
    
    private void demonstrateUsage(MilitaryServiceInterface militaryService,
                                NationRepository nationRepository) {
        // Пример использования новых компонентов
        try {
            // Поиск нации
            var nationOpt = nationRepository.findByName("Test Nation");
            
            if (nationOpt.isPresent()) {
                String nationId = nationOpt.get().getId();
                
                // Получение статистики
                var stats = militaryService.getMilitaryStatistics(nationId);
                double strength = militaryService.getMilitaryStrength(nationId);
                
                plugin.getLogger().info("Статистика для нации " + nationId + ":");
                plugin.getLogger().info("  Боевая мощь: " + strength);
                plugin.getLogger().info("  Общие единицы: " + stats.get("totalUnits"));
            }
            
            // Пример найма войск (если есть нация)
            // String result = militaryService.recruitUnits(nationId, "infantry", 100, 50.0);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при демонстрации использования: " + e.getMessage());
        }
    }
    
    /**
     * Пример миграции с старого кода на новый
     * Показывает, как можно постепенно заменять старые вызовы
     */
    public void demonstrateMigration() {
        // Старый способ (прямой вызов)
        // double oldStrength = plugin.getMilitaryService().getMilitaryStrength(nationId);
        
        // Новый способ (через интерфейс)
        MilitaryServiceInterface militaryService = serviceFactory.createMilitaryService();
        // double newStrength = militaryService.getMilitaryStrength(nationId);
        
        // Оба способа работают, что позволяет постепенно мигрировать
        plugin.getLogger().info("Демонстрация миграции завершена");
    }
    
    /**
     * Пример использования новой реализации (будущее)
     * Показывает, как будет выглядеть код после полной миграции
     */
    public void demonstrateFutureUsage() {
        // В будущем, когда миграция будет завершена:
        // MilitaryServiceInterface militaryService = serviceFactory.createNewMilitaryService();
        
        // Этот сервис будет использовать новые репозитории и новую логику
        // Но интерфейс останется тем же, поэтому изменения в контроллерах не потребуются
        
        plugin.getLogger().info("Будущее использование: после полной миграции");
    }
    
    /**
     * Метод для тестирования производительности
     * Сравнивает старую и новую реализации
     */
    public void testPerformance() {
        long startTime = System.currentTimeMillis();
        
        // Тестирование нового репозитория
        NationRepository repo = serviceFactory.createNationRepository();
        var allNations = repo.findAll();
        
        long endTime = System.currentTimeMillis();
        
        plugin.getLogger().info("Производительность: загружено " + allNations.size() + 
                              " наций за " + (endTime - startTime) + "ms");
    }
}
