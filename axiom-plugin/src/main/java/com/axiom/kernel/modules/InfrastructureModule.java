package com.axiom.kernel.modules;

import com.axiom.AXIOM;
import com.axiom.api.ModIntegrationAPI;
import com.axiom.kernel.KernelModule;
import com.axiom.kernel.ModuleIds;
import com.axiom.kernel.ServiceRegistry;
import com.axiom.domain.service.infrastructure.*;
import com.axiom.domain.service.industry.EconomyService;
import com.axiom.domain.service.industry.EconomyServiceInterface;
import com.axiom.domain.service.industry.EconomicIndicatorsService;
import com.axiom.domain.service.infrastructure.ModEnergyService;
import com.axiom.domain.service.infrastructure.ModResourceService;
import com.axiom.domain.service.industry.ResourceService;
import com.axiom.domain.service.military.MilitaryServiceInterface;
import com.axiom.domain.service.infrastructure.ModWarfareService;
import com.axiom.domain.service.state.CityGrowthEngine;
import com.axiom.domain.service.state.NationManager;
import com.axiom.domain.service.infrastructure.PlayerDashboardService;
import com.axiom.domain.service.state.PlayerDataManager;
import com.axiom.domain.service.state.TerritoryService;
import com.axiom.domain.service.infrastructure.TerritorySyncService;
import com.axiom.util.CacheManager;

import java.util.Set;

public class InfrastructureModule implements KernelModule {
    private final AXIOM plugin;

    public InfrastructureModule(AXIOM plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return ModuleIds.INFRASTRUCTURE;
    }

    @Override
    public Set<String> dependencies() {
        return Set.of(
            ModuleIds.STATE,
            ModuleIds.POLITICS,
            ModuleIds.INDUSTRY,
            ModuleIds.TECHNOLOGY,
            ModuleIds.MILITARY
        );
    }

    @Override
    public void register(ServiceRegistry services) {
        NationManager nationManager = services.require(NationManager.class);
        PlayerDataManager playerDataManager = services.require(PlayerDataManager.class);
        CityGrowthEngine cityGrowthEngine = services.require(CityGrowthEngine.class);
        ResourceService resourceService = services.require(ResourceService.class);
        EconomyService economyService = services.require(EconomyService.class);
        TerritoryService territoryService = services.require(TerritoryService.class);

        ConfirmationService confirmationService = new ConfirmationService();
        services.register(ConfirmationService.class, confirmationService);

        DoubleClickService doubleClickService = new DoubleClickService();
        services.register(DoubleClickService.class, doubleClickService);

        AchievementService achievementService = new AchievementService(plugin);
        services.register(AchievementService.class, achievementService);

        NewsService newsService = new NewsService(plugin, nationManager);
        services.register(NewsService.class, newsService);

        EventGenerator eventGenerator = new EventGenerator(plugin, nationManager);
        services.register(EventGenerator.class, eventGenerator);

        TransportService transportService = new TransportService(plugin);
        services.register(TransportService.class, transportService);

        HarborService harborService = new HarborService(plugin);
        services.register(HarborService.class, harborService);

        QuestService questService = new QuestService(plugin);
        services.register(QuestService.class, questService);

        LegacyService legacyService = new LegacyService(plugin);
        services.register(LegacyService.class, legacyService);

        MultiWorldService multiWorldService = new MultiWorldService(plugin, nationManager);
        services.register(MultiWorldService.class, multiWorldService);

        InfrastructureService infrastructureService = new InfrastructureService(plugin);
        services.register(InfrastructureService.class, infrastructureService);

        InfrastructureMaintenanceService infrastructureMaintenanceService = new InfrastructureMaintenanceService(plugin, nationManager);
        services.register(InfrastructureMaintenanceService.class, infrastructureMaintenanceService);

        ModIntegrationService modIntegrationService = new ModIntegrationService(plugin);
        services.register(ModIntegrationService.class, modIntegrationService);

        ModResourceService modResourceService = new ModResourceService(plugin, modIntegrationService, resourceService);
        services.register(ModResourceService.class, modResourceService);

        ModWarfareService modWarfareService = new ModWarfareService(plugin, modIntegrationService);
        services.register(ModWarfareService.class, modWarfareService);

        ModEnergyService modEnergyService = new ModEnergyService(plugin, modIntegrationService);
        services.register(ModEnergyService.class, modEnergyService);

        ModIntegrationAPI modIntegrationAPI = new ModIntegrationAPI(plugin);
        services.register(ModIntegrationAPI.class, modIntegrationAPI);

        RecipeIntegrationService recipeIntegrationService = new RecipeIntegrationService(plugin);
        services.register(RecipeIntegrationService.class, recipeIntegrationService);

        ModIntegrationEnhancementService modIntegrationEnhancementService = new ModIntegrationEnhancementService(plugin);
        services.register(ModIntegrationEnhancementService.class, modIntegrationEnhancementService);

        ModPackBuilderService modPackBuilderService = new ModPackBuilderService(plugin);
        services.register(ModPackBuilderService.class, modPackBuilderService);

        ModPackManagerService modPackManagerService = new ModPackManagerService(plugin);
        services.register(ModPackManagerService.class, modPackManagerService);

        ModCompatibilityChecker modCompatibilityChecker = new ModCompatibilityChecker(plugin);
        services.register(ModCompatibilityChecker.class, modCompatibilityChecker);

        ModBalancerService modBalancerService = new ModBalancerService(plugin, nationManager, modIntegrationService);
        services.register(ModBalancerService.class, modBalancerService);

        BalancingService balancingService = new BalancingService(plugin, nationManager, playerDataManager);
        services.register(BalancingService.class, balancingService);

        BackupService backupService = new BackupService(plugin);
        services.register(BackupService.class, backupService);

        WebExportService webExportService = new WebExportService(plugin, nationManager, territoryService);
        services.register(WebExportService.class, webExportService);

        TerritorySyncService territorySyncService = new TerritorySyncService(plugin, territoryService);
        services.register(TerritorySyncService.class, territorySyncService);

        TutorialService tutorialService = new TutorialService(plugin);
        services.register(TutorialService.class, tutorialService);

        PerformanceMetricsService performanceMetricsService = new PerformanceMetricsService(plugin);
        services.register(PerformanceMetricsService.class, performanceMetricsService);

        PlayerDashboardService playerDashboardService = new PlayerDashboardService(plugin);
        services.register(PlayerDashboardService.class, playerDashboardService);

        VisualEffectsService visualEffectsService = new VisualEffectsService(plugin);
        services.register(VisualEffectsService.class, visualEffectsService);

        StatisticsService statisticsService = new StatisticsService(plugin);
        services.register(StatisticsService.class, statisticsService);

        NotificationService notificationService = new NotificationService(plugin);
        services.register(NotificationService.class, notificationService);

        ChatService chatService = new ChatService(plugin);
        services.register(ChatService.class, chatService);

        MapBoundaryService mapBoundaryService = new MapBoundaryService(
            plugin,
            nationManager,
            cityGrowthEngine,
            modIntegrationService,
            territoryService
        );
        services.register(MapBoundaryService.class, mapBoundaryService);

        MapBoundaryVisualizationService mapBoundaryVisualizationService = new MapBoundaryVisualizationService(
            plugin,
            nationManager,
            cityGrowthEngine,
            modIntegrationService,
            visualEffectsService,
            territoryService
        );
        services.register(MapBoundaryVisualizationService.class, mapBoundaryVisualizationService);

        EconomicIndicatorsService economicIndicatorsService = new EconomicIndicatorsService(plugin);
        services.register(EconomicIndicatorsService.class, economicIndicatorsService);

        ServiceFactory serviceFactory = new ServiceFactory(plugin);
        services.register(ServiceFactory.class, serviceFactory);

        CacheManager cacheManager = new CacheManager(plugin);
        services.register(CacheManager.class, cacheManager);

        MilitaryServiceInterface militaryServiceInterface = serviceFactory.createMilitaryService();
        services.register(MilitaryServiceInterface.class, militaryServiceInterface);

        EconomyServiceInterface economyServiceInterface = serviceFactory.createEconomyService();
        services.register(EconomyServiceInterface.class, economyServiceInterface);
    }
}
