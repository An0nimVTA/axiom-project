package com.axiom.kernel.modules;

import com.axiom.AXIOM;
import com.axiom.kernel.KernelModule;
import com.axiom.kernel.ModuleIds;
import com.axiom.kernel.ServiceRegistry;
import com.axiom.domain.service.state.*;

public class CoreStateModule implements KernelModule {
    private final AXIOM plugin;

    public CoreStateModule(AXIOM plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return ModuleIds.STATE;
    }

    @Override
    public void register(ServiceRegistry services) {
        NationManager nationManager = new NationManager(plugin);
        services.register(NationManager.class, nationManager);

        PlayerDataManager playerDataManager = new PlayerDataManager(plugin);
        services.register(PlayerDataManager.class, playerDataManager);

        RolePermissionService rolePermissionService = new RolePermissionService(plugin);
        services.register(RolePermissionService.class, rolePermissionService);

        CityGrowthEngine cityGrowthEngine = new CityGrowthEngine(plugin, nationManager);
        services.register(CityGrowthEngine.class, cityGrowthEngine);

        CrimeService crimeService = new CrimeService(plugin, nationManager);
        services.register(CrimeService.class, crimeService);

        EducationService educationService = new EducationService(plugin, nationManager);
        services.register(EducationService.class, educationService);

        ClimateService climateService = new ClimateService(plugin);
        services.register(ClimateService.class, climateService);

        HappinessService happinessService = new HappinessService(
            plugin,
            nationManager,
            cityGrowthEngine,
            crimeService,
            educationService
        );
        services.register(HappinessService.class, happinessService);

        PlayerReputationService playerReputationService = new PlayerReputationService(plugin);
        services.register(PlayerReputationService.class, playerReputationService);

        NationModifierService nationModifierService = new NationModifierService(plugin);
        services.register(NationModifierService.class, nationModifierService);

        PollutionService pollutionService = new PollutionService(plugin, nationManager);
        services.register(PollutionService.class, pollutionService);

        CrisisResponseService crisisResponseService = new CrisisResponseService(plugin, nationManager);
        services.register(CrisisResponseService.class, crisisResponseService);

        MigrationService migrationService = new MigrationService(plugin);
        services.register(MigrationService.class, migrationService);

        DisasterService disasterService = new DisasterService(plugin, nationManager);
        services.register(DisasterService.class, disasterService);

        SanitationService sanitationService = new SanitationService(plugin, nationManager);
        services.register(SanitationService.class, sanitationService);

        PrisonService prisonService = new PrisonService(plugin);
        services.register(PrisonService.class, prisonService);

        PandemicService pandemicService = new PandemicService(plugin, nationManager);
        services.register(PandemicService.class, pandemicService);

        RefugeeService refugeeService = new RefugeeService(plugin, nationManager);
        services.register(RefugeeService.class, refugeeService);

        PlagueService plagueService = new PlagueService(plugin, nationManager);
        services.register(PlagueService.class, plagueService);

        FamineService famineService = new FamineService(plugin, nationManager);
        services.register(FamineService.class, famineService);

        ColonizationService colonizationService = new ColonizationService(plugin, nationManager);
        services.register(ColonizationService.class, colonizationService);

        CensusService censusService = new CensusService(plugin, nationManager);
        services.register(CensusService.class, censusService);

        PopulationGrowthService populationGrowthService = new PopulationGrowthService(plugin, nationManager);
        services.register(PopulationGrowthService.class, populationGrowthService);

        RefugeeResettlementService refugeeResettlementService = new RefugeeResettlementService(plugin, nationManager);
        services.register(RefugeeResettlementService.class, refugeeResettlementService);

        ImmigrationControlService immigrationControlService = new ImmigrationControlService(plugin);
        services.register(ImmigrationControlService.class, immigrationControlService);
        
        EmergencyService emergencyService = new EmergencyService(plugin, nationManager);
        services.register(EmergencyService.class, emergencyService);

        TerritoryService territoryService = new TerritoryService(plugin, nationManager);
        services.register(TerritoryService.class, territoryService);
    }
}
