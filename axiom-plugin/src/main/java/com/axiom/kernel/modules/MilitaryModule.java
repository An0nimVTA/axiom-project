package com.axiom.kernel.modules;

import com.axiom.AXIOM;
import com.axiom.kernel.KernelModule;
import com.axiom.kernel.ModuleIds;
import com.axiom.kernel.ServiceRegistry;
import com.axiom.domain.service.military.*;
import com.axiom.domain.service.politics.DiplomacySystem;
import com.axiom.domain.service.state.HappinessService;
import com.axiom.domain.service.state.NationManager;

import java.util.Set;

public class MilitaryModule implements KernelModule {
    private final AXIOM plugin;

    public MilitaryModule(AXIOM plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return ModuleIds.MILITARY;
    }

    @Override
    public Set<String> dependencies() {
        return Set.of(ModuleIds.STATE, ModuleIds.POLITICS, ModuleIds.INDUSTRY);
    }

    @Override
    public void register(ServiceRegistry services) {
        NationManager nationManager = services.require(NationManager.class);
        DiplomacySystem diplomacySystem = services.require(DiplomacySystem.class);
        HappinessService happinessService = services.require(HappinessService.class);

        PvpService pvpService = new PvpService(plugin);
        services.register(PvpService.class, pvpService);

        MilitaryService militaryService = new MilitaryService(plugin);
        services.register(MilitaryService.class, militaryService);

        MobilizationService mobilizationService = new MobilizationService(plugin, nationManager, diplomacySystem);
        services.register(MobilizationService.class, mobilizationService);

        ConquestService conquestService = new ConquestService(plugin, nationManager);
        services.register(ConquestService.class, conquestService);

        RaidService raidService = new RaidService(plugin, nationManager);
        services.register(RaidService.class, raidService);

        SiegeService siegeService = new SiegeService(plugin, nationManager);
        services.register(SiegeService.class, siegeService);

        RevoltService revoltService = new RevoltService(plugin, nationManager, happinessService);
        services.register(RevoltService.class, revoltService);

        AdvancedWarSystem advancedWarSystem = new AdvancedWarSystem(
            plugin,
            nationManager,
            diplomacySystem,
            militaryService,
            conquestService,
            raidService,
            siegeService
        );
        services.register(AdvancedWarSystem.class, advancedWarSystem);

        FortificationService fortificationService = new FortificationService(plugin);
        services.register(FortificationService.class, fortificationService);

        TerrorismService terrorismService = new TerrorismService(plugin, nationManager);
        services.register(TerrorismService.class, terrorismService);

        ReligiousWarService religiousWarService = new ReligiousWarService(plugin, nationManager);
        services.register(ReligiousWarService.class, religiousWarService);

        RebellionService rebellionService = new RebellionService(plugin, nationManager);
        services.register(RebellionService.class, rebellionService);

        ConscriptionService conscriptionService = new ConscriptionService(plugin, nationManager);
        services.register(ConscriptionService.class, conscriptionService);

        NavalService navalService = new NavalService(plugin);
        services.register(NavalService.class, navalService);

        BlockadeService blockadeService = new BlockadeService(plugin);
        services.register(BlockadeService.class, blockadeService);

        MilitaryExerciseService militaryExerciseService = new MilitaryExerciseService(plugin, nationManager);
        services.register(MilitaryExerciseService.class, militaryExerciseService);

        MilitaryIntelligenceService militaryIntelligenceService = new MilitaryIntelligenceService(plugin, nationManager);
        services.register(MilitaryIntelligenceService.class, militaryIntelligenceService);

        ResistanceMovementService resistanceMovementService = new ResistanceMovementService(plugin);
        services.register(ResistanceMovementService.class, resistanceMovementService);

        WarCrimeService warCrimeService = new WarCrimeService(plugin, nationManager);
        services.register(WarCrimeService.class, warCrimeService);

        ArmsDealService armsDealService = new ArmsDealService(plugin, nationManager);
        services.register(ArmsDealService.class, armsDealService);

        ArmsControlService armsControlService = new ArmsControlService(plugin, nationManager);
        services.register(ArmsControlService.class, armsControlService);

        NuclearWeaponsService nuclearWeaponsService = new NuclearWeaponsService(plugin, nationManager);
        services.register(NuclearWeaponsService.class, nuclearWeaponsService);

        PartisanService partisanService = new PartisanService(plugin);
        services.register(PartisanService.class, partisanService);

        AssassinationService assassinationService = new AssassinationService(plugin, nationManager);
        services.register(AssassinationService.class, assassinationService);

        MilitaryAllianceService militaryAllianceService = new MilitaryAllianceService(plugin, nationManager);
        services.register(MilitaryAllianceService.class, militaryAllianceService);

        UnifiedEspionageService unifiedEspionageService = new UnifiedEspionageService(plugin, nationManager);
        services.register(UnifiedEspionageService.class, unifiedEspionageService);

        CountryCaptureService countryCaptureService = new CountryCaptureService(plugin);
        services.register(CountryCaptureService.class, countryCaptureService);
    }
}
