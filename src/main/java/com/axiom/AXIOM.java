package com.axiom;

import com.axiom.command.AxiomCommand;
import com.axiom.command.ClaimCommand;
import com.axiom.command.CreateNationCommand;
import com.axiom.command.UnclaimCommand;
import com.axiom.command.TestBotCommand;
import com.axiom.gui.NationMainMenu;
import com.axiom.command.NationCommandAlias;
import com.axiom.gui.ConfirmMenu;
import com.axiom.gui.ReligionMenu;
import com.axiom.gui.CitiesMenu;
import com.axiom.gui.TechnologyMenu;
import com.axiom.gui.CorporationsMenu;
import com.axiom.listener.TerritoryProtectionListener;
import com.axiom.listener.WarMobilizationListener;
import com.axiom.listener.WarzoneVisualListener;
import com.axiom.listener.ReligionBuffListener;
import com.axiom.service.CityGrowthEngine;
import com.axiom.service.DiplomacySystem;
import com.axiom.service.EconomyService;
import com.axiom.service.NationManager;
import com.axiom.service.ReligionManager;
import com.axiom.service.PvpService;
import com.axiom.service.PlayerDataManager;
import com.axiom.service.ConfirmationService;
import com.axiom.service.DoubleClickService;
import com.axiom.service.WalletService;
import com.axiom.service.NewsService;
import com.axiom.service.RolePermissionService;
import com.axiom.service.EventGenerator;
import com.axiom.service.ModIntegrationService;
import com.axiom.service.EspionageService;
import com.axiom.service.TradeService;
import com.axiom.service.MapBoundaryService;
import com.axiom.service.StockMarketService;
import com.axiom.service.ElectionService;
import com.axiom.service.CrimeService;
import com.axiom.service.EducationService;
import com.axiom.service.AchievementService;
import com.axiom.service.ClimateService;
import com.axiom.service.HolidayService;
import com.axiom.service.HappinessService;
import com.axiom.service.TransportService;
import com.axiom.service.ResourceService;
import com.axiom.service.TreatyService;
import com.axiom.service.NotificationService;
import com.axiom.service.StatisticsService;
import com.axiom.service.BankingService;
import com.axiom.service.MobilizationService;
import com.axiom.service.ChatService;
import com.axiom.service.TradingPostService;
import com.axiom.service.PlayerReputationService;
import com.axiom.service.TechnologyTreeService;
import com.axiom.service.VassalService;
import com.axiom.service.QuestService;
import com.axiom.service.NationModifierService;
import com.axiom.service.PollutionService;
import com.axiom.service.BorderVisualizationService;
import com.axiom.service.AllianceService;
import com.axiom.service.CrisisResponseService;
import com.axiom.service.DynastyService;
import com.axiom.service.InfluenceService;
import com.axiom.service.MigrationService;
import com.axiom.service.EmbargoService;
import com.axiom.service.LegacyService;
import com.axiom.service.RitualService;
import com.axiom.service.ConquestService;
import com.axiom.service.CultureService;
import com.axiom.service.PropagandaService;
import com.axiom.service.DisasterService;
import com.axiom.service.IntelligenceService;
import com.axiom.service.FestivalService;
import com.axiom.service.SupplyChainService;
import com.axiom.service.RevoltService;
import com.axiom.service.DiplomaticMissionService;
import com.axiom.service.CurrencyExchangeService;
import com.axiom.service.NavalService;
import com.axiom.service.SanitationService;
import com.axiom.service.PrisonService;
import com.axiom.service.BlackMarketService;
import com.axiom.service.ClientUiService;
import com.axiom.service.ResistanceMovementService;
import com.axiom.service.MonumentService;
import com.axiom.service.PandemicService;
import com.axiom.service.InfrastructureService;
import com.axiom.service.RefugeeService;
import com.axiom.service.GreatWorksService;
import com.axiom.service.RaidService;
import com.axiom.service.TributeService;
import com.axiom.service.PlagueService;
import com.axiom.service.PartisanService;
import com.axiom.service.ConscriptionService;
import com.axiom.service.BlockadeService;
import com.axiom.service.HarborService;
import com.axiom.service.DiplomaticImmunityService;
import com.axiom.service.FamineService;
import com.axiom.service.AssassinationService;
import com.axiom.service.ColonizationService;
import com.axiom.service.CensusService;
import com.axiom.service.ResourceDepletionService;
import com.axiom.service.SiegeService;
import com.axiom.service.FortificationService;
import com.axiom.service.CoupService;
import com.axiom.service.TradeAgreementService;
import com.axiom.service.TerrorismService;
import com.axiom.service.CeasefireService;
import com.axiom.service.MilitaryService;
import com.axiom.service.SpyService;
import com.axiom.service.ReligiousWarService;
import com.axiom.service.AdvancedWarSystem;
import com.axiom.service.ImportExportService;
import com.axiom.service.EnergyService;
import com.axiom.service.RebellionService;
import com.axiom.service.TreatyViolationService;
import com.axiom.service.SanctionService;
import com.axiom.service.ResourceScarcityService;
import com.axiom.service.TradeRouteService;
import com.axiom.service.DiplomaticRecognitionService;
import com.axiom.service.CultureShockService;
import com.axiom.service.BorderControlService;
import com.axiom.service.TaxEvasionService;
import com.axiom.service.MonetaryPolicyService;
import com.axiom.service.ResearchFundingService;
import com.axiom.service.CommodityMarketService;
import com.axiom.service.ImmigrationControlService;
import com.axiom.service.EmergencyService;
import com.axiom.service.ArmsDealService;
import com.axiom.service.TreatyRenegotiationService;
import com.axiom.service.EconomicCrisisService;
import com.axiom.service.CorruptionService;
import com.axiom.service.DiplomaticImmunityExpansionService;
import com.axiom.service.ResourceDiscoveryService;
import com.axiom.service.WarCrimeService;
import com.axiom.service.PeaceTreatyService;
import com.axiom.service.TradeEmbargoExpansionService;
import com.axiom.service.RaceService;
import com.axiom.service.RacialDiscriminationService;
import com.axiom.service.TradeNetworkService;
import com.axiom.service.MilitaryExerciseService;
import com.axiom.service.ResourceProcessingService;
import com.axiom.service.PublicOpinionService;
import com.axiom.service.InfrastructureMaintenanceService;
import com.axiom.service.DiplomaticProtocolService;
import com.axiom.service.PropagandaCampaignService;
import com.axiom.service.CurrencyManipulationService;
import com.axiom.service.ResourceStockpileService;
import com.axiom.service.CulturalExchangeService;
import com.axiom.service.MilitaryIntelligenceService;
import com.axiom.service.EnvironmentalPolicyService;
import com.axiom.service.TradeDisputeService;
import com.axiom.service.SocialWelfareService;
import com.axiom.service.NuclearWeaponsService;
import com.axiom.service.ArmsControlService;
import com.axiom.service.RefugeeResettlementService;
import com.axiom.service.InternationalCourtService;
import com.axiom.service.CulturalHeritageService;
import com.axiom.service.ResourceNationalizationService;
import com.axiom.service.CurrencyUnionService;
import com.axiom.service.ResearchCollaborationService;
import com.axiom.service.SpaceProgramService;
import com.axiom.service.TradeWarService;
import com.axiom.service.MilitaryAllianceService;
import com.axiom.service.ResourceCartelService;
import com.axiom.service.PopulationGrowthService;
import com.axiom.service.InternationalAidService;
import com.axiom.service.DiplomaticSummitService;
import com.axiom.service.CulturalRevolutionService;
import com.axiom.service.ModResourceService;
import com.axiom.service.ModWarfareService;
import com.axiom.service.ModEnergyService;
import com.axiom.service.BalancingService;
import com.axiom.service.BackupService;
import com.axiom.service.WebExportService;
import com.axiom.service.TutorialService;
import com.axiom.service.PerformanceMetricsService;
import com.axiom.service.MultiWorldService;
import com.axiom.service.PlayerDashboardService;
import com.axiom.service.VisualEffectsService;
import com.axiom.service.ServiceInterconnectionService;
import com.axiom.service.UnifiedEnergyService;
import com.axiom.service.FuelService;
import com.axiom.service.ServiceLocator;
import com.axiom.listener.ModIntegrationListener;
import com.axiom.listener.DashboardListener;
import com.axiom.listener.VisualEffectsListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
// Dynmap integration disabled - optional dependency
// import org.dynmap.DynmapAPI;

/**
 * AXIOM Geopolitical Engine
 * <p>
 * Main plugin bootstrap class. Wires services, registers commands and listeners,
 * manages configuration lifecycle.
 */
public final class AXIOM extends JavaPlugin {

    private static AXIOM instance;
    // private DynmapAPI dynmap; // Dynmap disabled

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ServiceLocator.register(AXIOM.class, this);

        // Initialize and register all services
        NationManager nationManager = new NationManager(this);
        ServiceLocator.register(NationManager.class, nationManager);

        EconomyService economyService = new EconomyService(this, nationManager);
        ServiceLocator.register(EconomyService.class, economyService);

        DiplomacySystem diplomacySystem = new DiplomacySystem(this, nationManager);
        ServiceLocator.register(DiplomacySystem.class, diplomacySystem);

        ReligionManager religionManager = new ReligionManager(this);
        ServiceLocator.register(ReligionManager.class, religionManager);

        CityGrowthEngine cityGrowthEngine = new CityGrowthEngine(this, nationManager);
        ServiceLocator.register(CityGrowthEngine.class, cityGrowthEngine);

        PvpService pvpService = new PvpService(this);
        ServiceLocator.register(PvpService.class, pvpService);

        PlayerDataManager playerDataManager = new PlayerDataManager(this);
        ServiceLocator.register(PlayerDataManager.class, playerDataManager);

        ConfirmationService confirmationService = new ConfirmationService();
        ServiceLocator.register(ConfirmationService.class, confirmationService);

        DoubleClickService doubleClickService = new DoubleClickService();
        ServiceLocator.register(DoubleClickService.class, doubleClickService);

        WalletService walletService = new WalletService(this, playerDataManager);
        ServiceLocator.register(WalletService.class, walletService);

        NewsService newsService = new NewsService(this, nationManager);
        ServiceLocator.register(NewsService.class, newsService);

        RolePermissionService rolePermissionService = new RolePermissionService(this);
        ServiceLocator.register(RolePermissionService.class, rolePermissionService);

        EventGenerator eventGenerator = new EventGenerator(this, nationManager);
        ServiceLocator.register(EventGenerator.class, eventGenerator);

        ModIntegrationService modIntegrationService = new ModIntegrationService(this);
        ServiceLocator.register(ModIntegrationService.class, modIntegrationService);

        EspionageService espionageService = new EspionageService(this, nationManager);
        ServiceLocator.register(EspionageService.class, espionageService);

        TradeService tradeService = new TradeService(this, nationManager);
        ServiceLocator.register(TradeService.class, tradeService);

        StockMarketService stockMarketService = new StockMarketService(this);
        ServiceLocator.register(StockMarketService.class, stockMarketService);

        ElectionService electionService = new ElectionService(this, nationManager);
        ServiceLocator.register(ElectionService.class, electionService);

        CrimeService crimeService = new CrimeService(this, nationManager);
        ServiceLocator.register(CrimeService.class, crimeService);

        EducationService educationService = new EducationService(this, nationManager);
        ServiceLocator.register(EducationService.class, educationService);

        AchievementService achievementService = new AchievementService(this);
        ServiceLocator.register(AchievementService.class, achievementService);

        ClimateService climateService = new ClimateService(this);
        ServiceLocator.register(ClimateService.class, climateService);

        HolidayService holidayService = new HolidayService(this, religionManager);
        ServiceLocator.register(HolidayService.class, holidayService);

        HappinessService happinessService = new HappinessService(this, nationManager, cityGrowthEngine, crimeService, educationService);
        ServiceLocator.register(HappinessService.class, happinessService);

        TransportService transportService = new TransportService(this);
        ServiceLocator.register(TransportService.class, transportService);

        ResourceService resourceService = new ResourceService(this);
        ServiceLocator.register(ResourceService.class, resourceService);

        TreatyService treatyService = new TreatyService(this, nationManager);
        ServiceLocator.register(TreatyService.class, treatyService);

        NotificationService notificationService = new NotificationService(this);
        ServiceLocator.register(NotificationService.class, notificationService);

        StatisticsService statisticsService = new StatisticsService(this);
        ServiceLocator.register(StatisticsService.class, statisticsService);

        BankingService bankingService = new BankingService(this, nationManager);
        ServiceLocator.register(BankingService.class, bankingService);

        MobilizationService mobilizationService = new MobilizationService(this, nationManager, diplomacySystem);
        ServiceLocator.register(MobilizationService.class, mobilizationService);

        ChatService chatService = new ChatService(this);
        ServiceLocator.register(ChatService.class, chatService);

        TradingPostService tradingPostService = new TradingPostService(this);
        ServiceLocator.register(TradingPostService.class, tradingPostService);

        PlayerReputationService playerReputationService = new PlayerReputationService(this);
        ServiceLocator.register(PlayerReputationService.class, playerReputationService);

        TechnologyTreeService technologyTreeService = new TechnologyTreeService(this);
        ServiceLocator.register(TechnologyTreeService.class, technologyTreeService);

        VassalService vassalService = new VassalService(this, nationManager);
        ServiceLocator.register(VassalService.class, vassalService);

        QuestService questService = new QuestService(this);
        ServiceLocator.register(QuestService.class, questService);

        NationModifierService nationModifierService = new NationModifierService(this);
        ServiceLocator.register(NationModifierService.class, nationModifierService);

        PollutionService pollutionService = new PollutionService(this, nationManager);
        ServiceLocator.register(PollutionService.class, pollutionService);

        BorderVisualizationService borderVisualizationService = new BorderVisualizationService(this);
        ServiceLocator.register(BorderVisualizationService.class, borderVisualizationService);

        AllianceService allianceService = new AllianceService(this, diplomacySystem);
        ServiceLocator.register(AllianceService.class, allianceService);

        CrisisResponseService crisisResponseService = new CrisisResponseService(this, nationManager);
        ServiceLocator.register(CrisisResponseService.class, crisisResponseService);

        DynastyService dynastyService = new DynastyService(this);
        ServiceLocator.register(DynastyService.class, dynastyService);

        InfluenceService influenceService = new InfluenceService(this);
        ServiceLocator.register(InfluenceService.class, influenceService);

        MigrationService migrationService = new MigrationService(this);
        ServiceLocator.register(MigrationService.class, migrationService);

        EmbargoService embargoService = new EmbargoService(this);
        ServiceLocator.register(EmbargoService.class, embargoService);

        LegacyService legacyService = new LegacyService(this);
        ServiceLocator.register(LegacyService.class, legacyService);

        RitualService ritualService = new RitualService(this);
        ServiceLocator.register(RitualService.class, ritualService);

        ConquestService conquestService = new ConquestService(this, nationManager);
        ServiceLocator.register(ConquestService.class, conquestService);

        CultureService cultureService = new CultureService(this);
        ServiceLocator.register(CultureService.class, cultureService);

        PropagandaService propagandaService = new PropagandaService(this, nationManager);
        ServiceLocator.register(PropagandaService.class, propagandaService);

        DisasterService disasterService = new DisasterService(this, nationManager);
        ServiceLocator.register(DisasterService.class, disasterService);

        IntelligenceService intelligenceService = new IntelligenceService(this);
        ServiceLocator.register(IntelligenceService.class, intelligenceService);

        FestivalService festivalService = new FestivalService(this);
        ServiceLocator.register(FestivalService.class, festivalService);

        SupplyChainService supplyChainService = new SupplyChainService(this);
        ServiceLocator.register(SupplyChainService.class, supplyChainService);

        RevoltService revoltService = new RevoltService(this, nationManager, happinessService);
        ServiceLocator.register(RevoltService.class, revoltService);

        DiplomaticMissionService diplomaticMissionService = new DiplomaticMissionService(this);
        ServiceLocator.register(DiplomaticMissionService.class, diplomaticMissionService);

        CurrencyExchangeService currencyExchangeService = new CurrencyExchangeService(this, nationManager, economyService);
        ServiceLocator.register(CurrencyExchangeService.class, currencyExchangeService);

        NavalService navalService = new NavalService(this);
        ServiceLocator.register(NavalService.class, navalService);

        SanitationService sanitationService = new SanitationService(this, nationManager);
        ServiceLocator.register(SanitationService.class, sanitationService);

        PrisonService prisonService = new PrisonService(this);
        ServiceLocator.register(PrisonService.class, prisonService);

        BlackMarketService blackMarketService = new BlackMarketService(this);
        ServiceLocator.register(BlackMarketService.class, blackMarketService);

        ResistanceMovementService resistanceMovementService = new ResistanceMovementService(this);
        ServiceLocator.register(ResistanceMovementService.class, resistanceMovementService);

        MonumentService monumentService = new MonumentService(this);
        ServiceLocator.register(MonumentService.class, monumentService);

        PandemicService pandemicService = new PandemicService(this, nationManager);
        ServiceLocator.register(PandemicService.class, pandemicService);

        InfrastructureService infrastructureService = new InfrastructureService(this);
        ServiceLocator.register(InfrastructureService.class, infrastructureService);

        RefugeeService refugeeService = new RefugeeService(this, nationManager);
        ServiceLocator.register(RefugeeService.class, refugeeService);

        GreatWorksService greatWorksService = new GreatWorksService(this);
        ServiceLocator.register(GreatWorksService.class, greatWorksService);

        RaidService raidService = new RaidService(this, nationManager);
        ServiceLocator.register(RaidService.class, raidService);

        TributeService tributeService = new TributeService(this, nationManager);
        ServiceLocator.register(TributeService.class, tributeService);

        PlagueService plagueService = new PlagueService(this, nationManager);
        ServiceLocator.register(PlagueService.class, plagueService);

        PartisanService partisanService = new PartisanService(this);
        ServiceLocator.register(PartisanService.class, partisanService);

        ConscriptionService conscriptionService = new ConscriptionService(this, nationManager);
        ServiceLocator.register(ConscriptionService.class, conscriptionService);

        BlockadeService blockadeService = new BlockadeService(this);
        ServiceLocator.register(BlockadeService.class, blockadeService);

        HarborService harborService = new HarborService(this);
        ServiceLocator.register(HarborService.class, harborService);

        DiplomaticImmunityService diplomaticImmunityService = new DiplomaticImmunityService(this);
        ServiceLocator.register(DiplomaticImmunityService.class, diplomaticImmunityService);

        FamineService famineService = new FamineService(this, nationManager);
        ServiceLocator.register(FamineService.class, famineService);

        AssassinationService assassinationService = new AssassinationService(this, nationManager);
        ServiceLocator.register(AssassinationService.class, assassinationService);

        ColonizationService colonizationService = new ColonizationService(this, nationManager);
        ServiceLocator.register(ColonizationService.class, colonizationService);

        CensusService censusService = new CensusService(this, nationManager);
        ServiceLocator.register(CensusService.class, censusService);

        ResourceDepletionService resourceDepletionService = new ResourceDepletionService(this);
        ServiceLocator.register(ResourceDepletionService.class, resourceDepletionService);

        SiegeService siegeService = new SiegeService(this, nationManager);
        ServiceLocator.register(SiegeService.class, siegeService);

        MilitaryService militaryService = new MilitaryService(this);
        ServiceLocator.register(MilitaryService.class, militaryService);

        AdvancedWarSystem advancedWarSystem = new AdvancedWarSystem(this, nationManager, diplomacySystem,
            militaryService, conquestService, raidService, siegeService);
        ServiceLocator.register(AdvancedWarSystem.class, advancedWarSystem);
        
        FortificationService fortificationService = new FortificationService(this);
        ServiceLocator.register(FortificationService.class, fortificationService);

        CoupService coupService = new CoupService(this, nationManager);
        ServiceLocator.register(CoupService.class, coupService);

        TradeAgreementService tradeAgreementService = new TradeAgreementService(this);
        ServiceLocator.register(TradeAgreementService.class, tradeAgreementService);

        TerrorismService terrorismService = new TerrorismService(this, nationManager);
        ServiceLocator.register(TerrorismService.class, terrorismService);

        CeasefireService ceasefireService = new CeasefireService(this, nationManager);
        ServiceLocator.register(CeasefireService.class, ceasefireService);

        SpyService spyService = new SpyService(this, nationManager);
        ServiceLocator.register(SpyService.class, spyService);

        ReligiousWarService religiousWarService = new ReligiousWarService(this, nationManager);
        ServiceLocator.register(ReligiousWarService.class, religiousWarService);

        ImportExportService importExportService = new ImportExportService(this);
        ServiceLocator.register(ImportExportService.class, importExportService);

        EnergyService energyService = new EnergyService(this);
        ServiceLocator.register(EnergyService.class, energyService);

        UnifiedEnergyService unifiedEnergyService = new UnifiedEnergyService(this);
        ServiceLocator.register(UnifiedEnergyService.class, unifiedEnergyService);

        ServiceInterconnectionService serviceInterconnectionService = new ServiceInterconnectionService(this);
        ServiceLocator.register(ServiceInterconnectionService.class, serviceInterconnectionService);

        FuelService fuelService = new FuelService(this);
        ServiceLocator.register(FuelService.class, fuelService);

        RebellionService rebellionService = new RebellionService(this, nationManager);
        ServiceLocator.register(RebellionService.class, rebellionService);

        TreatyViolationService treatyViolationService = new TreatyViolationService(this, nationManager);
        ServiceLocator.register(TreatyViolationService.class, treatyViolationService);

        SanctionService sanctionService = new SanctionService(this, nationManager);
        ServiceLocator.register(SanctionService.class, sanctionService);

        ResourceScarcityService resourceScarcityService = new ResourceScarcityService(this);
        ServiceLocator.register(ResourceScarcityService.class, resourceScarcityService);

        DiplomaticRecognitionService diplomaticRecognitionService = new DiplomaticRecognitionService(this, nationManager);
        ServiceLocator.register(DiplomaticRecognitionService.class, diplomaticRecognitionService);

        CultureShockService cultureShockService = new CultureShockService(this);
        ServiceLocator.register(CultureShockService.class, cultureShockService);

        BorderControlService borderControlService = new BorderControlService(this);
        ServiceLocator.register(BorderControlService.class, borderControlService);

        TaxEvasionService taxEvasionService = new TaxEvasionService(this);
        ServiceLocator.register(TaxEvasionService.class, taxEvasionService);

        MonetaryPolicyService monetaryPolicyService = new MonetaryPolicyService(this);
        ServiceLocator.register(MonetaryPolicyService.class, monetaryPolicyService);

        ResearchFundingService researchFundingService = new ResearchFundingService(this);
        ServiceLocator.register(ResearchFundingService.class, researchFundingService);

        CommodityMarketService commodityMarketService = new CommodityMarketService(this);
        ServiceLocator.register(CommodityMarketService.class, commodityMarketService);

        ImmigrationControlService immigrationControlService = new ImmigrationControlService(this);
        ServiceLocator.register(ImmigrationControlService.class, immigrationControlService);

        EmergencyService emergencyService = new EmergencyService(this, nationManager);
        ServiceLocator.register(EmergencyService.class, emergencyService);

        ArmsDealService armsDealService = new ArmsDealService(this, nationManager);
        ServiceLocator.register(ArmsDealService.class, armsDealService);

        TreatyRenegotiationService treatyRenegotiationService = new TreatyRenegotiationService(this, nationManager);
        ServiceLocator.register(TreatyRenegotiationService.class, treatyRenegotiationService);

        EconomicCrisisService economicCrisisService = new EconomicCrisisService(this, nationManager);
        ServiceLocator.register(EconomicCrisisService.class, economicCrisisService);

        CorruptionService corruptionService = new CorruptionService(this, nationManager);
        ServiceLocator.register(CorruptionService.class, corruptionService);

        DiplomaticImmunityExpansionService diplomaticImmunityExpansionService = new DiplomaticImmunityExpansionService(this, nationManager);
        ServiceLocator.register(DiplomaticImmunityExpansionService.class, diplomaticImmunityExpansionService);

        ResourceDiscoveryService resourceDiscoveryService = new ResourceDiscoveryService(this, nationManager);
        ServiceLocator.register(ResourceDiscoveryService.class, resourceDiscoveryService);

        WarCrimeService warCrimeService = new WarCrimeService(this, nationManager);
        ServiceLocator.register(WarCrimeService.class, warCrimeService);

        PeaceTreatyService peaceTreatyService = new PeaceTreatyService(this, nationManager);
        ServiceLocator.register(PeaceTreatyService.class, peaceTreatyService);

        TradeEmbargoExpansionService tradeEmbargoExpansionService = new TradeEmbargoExpansionService(this);
        ServiceLocator.register(TradeEmbargoExpansionService.class, tradeEmbargoExpansionService);

        RaceService raceService = new RaceService(this);
        ServiceLocator.register(RaceService.class, raceService);

        RacialDiscriminationService racialDiscriminationService = new RacialDiscriminationService(this, nationManager);
        ServiceLocator.register(RacialDiscriminationService.class, racialDiscriminationService);

        TradeNetworkService tradeNetworkService = new TradeNetworkService(this, nationManager);
        ServiceLocator.register(TradeNetworkService.class, tradeNetworkService);

        MilitaryExerciseService militaryExerciseService = new MilitaryExerciseService(this, nationManager);
        ServiceLocator.register(MilitaryExerciseService.class, militaryExerciseService);

        ResourceProcessingService resourceProcessingService = new ResourceProcessingService(this);
        ServiceLocator.register(ResourceProcessingService.class, resourceProcessingService);

        PublicOpinionService publicOpinionService = new PublicOpinionService(this, nationManager);
        ServiceLocator.register(PublicOpinionService.class, publicOpinionService);

        InfrastructureMaintenanceService infrastructureMaintenanceService = new InfrastructureMaintenanceService(this, nationManager);
        ServiceLocator.register(InfrastructureMaintenanceService.class, infrastructureMaintenanceService);

        DiplomaticProtocolService diplomaticProtocolService = new DiplomaticProtocolService(this);
        ServiceLocator.register(DiplomaticProtocolService.class, diplomaticProtocolService);

        PropagandaCampaignService propagandaCampaignService = new PropagandaCampaignService(this, nationManager);
        ServiceLocator.register(PropagandaCampaignService.class, propagandaCampaignService);

        CurrencyManipulationService currencyManipulationService = new CurrencyManipulationService(this);
        ServiceLocator.register(CurrencyManipulationService.class, currencyManipulationService);

        ResourceStockpileService resourceStockpileService = new ResourceStockpileService(this, nationManager);
        ServiceLocator.register(ResourceStockpileService.class, resourceStockpileService);

        CulturalExchangeService culturalExchangeService = new CulturalExchangeService(this, nationManager);
        ServiceLocator.register(CulturalExchangeService.class, culturalExchangeService);

        MilitaryIntelligenceService militaryIntelligenceService = new MilitaryIntelligenceService(this, nationManager);
        ServiceLocator.register(MilitaryIntelligenceService.class, militaryIntelligenceService);

        EnvironmentalPolicyService environmentalPolicyService = new EnvironmentalPolicyService(this, nationManager);
        ServiceLocator.register(EnvironmentalPolicyService.class, environmentalPolicyService);

        TradeDisputeService tradeDisputeService = new TradeDisputeService(this, nationManager);
        ServiceLocator.register(TradeDisputeService.class, tradeDisputeService);

        SocialWelfareService socialWelfareService = new SocialWelfareService(this, nationManager);
        ServiceLocator.register(SocialWelfareService.class, socialWelfareService);

        NuclearWeaponsService nuclearWeaponsService = new NuclearWeaponsService(this, nationManager);
        ServiceLocator.register(NuclearWeaponsService.class, nuclearWeaponsService);

        ArmsControlService armsControlService = new ArmsControlService(this, nationManager);
        ServiceLocator.register(ArmsControlService.class, armsControlService);

        RefugeeResettlementService refugeeResettlementService = new RefugeeResettlementService(this, nationManager);
        ServiceLocator.register(RefugeeResettlementService.class, refugeeResettlementService);

        InternationalCourtService internationalCourtService = new InternationalCourtService(this, nationManager);
        ServiceLocator.register(InternationalCourtService.class, internationalCourtService);

        CulturalHeritageService culturalHeritageService = new CulturalHeritageService(this, nationManager);
        ServiceLocator.register(CulturalHeritageService.class, culturalHeritageService);

        ResourceNationalizationService resourceNationalizationService = new ResourceNationalizationService(this);
        ServiceLocator.register(ResourceNationalizationService.class, resourceNationalizationService);

        CurrencyUnionService currencyUnionService = new CurrencyUnionService(this, nationManager);
        ServiceLocator.register(CurrencyUnionService.class, currencyUnionService);

        ResearchCollaborationService researchCollaborationService = new ResearchCollaborationService(this, nationManager);
        ServiceLocator.register(ResearchCollaborationService.class, researchCollaborationService);

        SpaceProgramService spaceProgramService = new SpaceProgramService(this, nationManager);
        ServiceLocator.register(SpaceProgramService.class, spaceProgramService);

        TradeWarService tradeWarService = new TradeWarService(this, nationManager);
        ServiceLocator.register(TradeWarService.class, tradeWarService);

        MilitaryAllianceService militaryAllianceService = new MilitaryAllianceService(this, nationManager);
        ServiceLocator.register(MilitaryAllianceService.class, militaryAllianceService);

        ResourceCartelService resourceCartelService = new ResourceCartelService(this, nationManager);
        ServiceLocator.register(ResourceCartelService.class, resourceCartelService);

        PopulationGrowthService populationGrowthService = new PopulationGrowthService(this, nationManager);
        ServiceLocator.register(PopulationGrowthService.class, populationGrowthService);

        InternationalAidService internationalAidService = new InternationalAidService(this, nationManager);
        ServiceLocator.register(InternationalAidService.class, internationalAidService);

        DiplomaticSummitService diplomaticSummitService = new DiplomaticSummitService(this, nationManager);
        ServiceLocator.register(DiplomaticSummitService.class, diplomaticSummitService);

        CulturalRevolutionService culturalRevolutionService = new CulturalRevolutionService(this, nationManager);
        ServiceLocator.register(CulturalRevolutionService.class, culturalRevolutionService);

        ModResourceService modResourceService = new ModResourceService(this, modIntegrationService, resourceService);
        ServiceLocator.register(ModResourceService.class, modResourceService);

        ModWarfareService modWarfareService = new ModWarfareService(this, modIntegrationService);
        ServiceLocator.register(ModWarfareService.class, modWarfareService);

        ModEnergyService modEnergyService = new ModEnergyService(this, modIntegrationService);
        ServiceLocator.register(ModEnergyService.class, modEnergyService);

        BalancingService balancingService = new BalancingService(this, nationManager, playerDataManager);
        ServiceLocator.register(BalancingService.class, balancingService);

        BackupService backupService = new BackupService(this);
        ServiceLocator.register(BackupService.class, backupService);

        WebExportService webExportService = new WebExportService(this, nationManager);
        ServiceLocator.register(WebExportService.class, webExportService);

        TutorialService tutorialService = new TutorialService(this);
        ServiceLocator.register(TutorialService.class, tutorialService);

        PerformanceMetricsService performanceMetricsService = new PerformanceMetricsService(this);
        ServiceLocator.register(PerformanceMetricsService.class, performanceMetricsService);

        MultiWorldService multiWorldService = new MultiWorldService(this, nationManager);
        ServiceLocator.register(MultiWorldService.class, multiWorldService);

        PlayerDashboardService playerDashboardService = new PlayerDashboardService(this);
        ServiceLocator.register(PlayerDashboardService.class, playerDashboardService);

        VisualEffectsService visualEffectsService = new VisualEffectsService(this);
        ServiceLocator.register(VisualEffectsService.class, visualEffectsService);

        ClientUiService clientUiService = new ClientUiService(this);
        ServiceLocator.register(ClientUiService.class, clientUiService);

        // Initialize GUI components after services
        NationMainMenu nationMainMenu = new NationMainMenu();
        Bukkit.getPluginManager().registerEvents(nationMainMenu, this);
        ConfirmMenu confirmMenu = new ConfirmMenu();
        Bukkit.getPluginManager().registerEvents(confirmMenu, this);
        CorporationsMenu corporationsMenu = new CorporationsMenu(this, stockMarketService);
        
        // PlaceholderAPI integration (if available)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                com.axiom.expansion.AxiomPlaceholderExpansion expansion = new com.axiom.expansion.AxiomPlaceholderExpansion(this);
                expansion.register();
                getLogger().info("PlaceholderAPI интеграция активирована!");
            } catch (Exception e) {
                getLogger().info("Ошибка регистрации PlaceholderAPI: " + e.getMessage());
            }
        } else {
            getLogger().info("PlaceholderAPI не найден. Интеграция пропущена.");
        }

        // Dynmap Integration
        setupDynmap();

        // Commands
        if (getCommand("axiom") != null) {
            getCommand("axiom").setExecutor(new AxiomCommand(this));
        }
        if (getCommand("nation") != null) {
            getCommand("nation").setExecutor(new NationCommandAlias(this));
        }
        if (getCommand("testbot") != null) {
            getCommand("testbot").setExecutor(new TestBotCommand(this));
        }
        if (getCommand("claim") != null) {
            getCommand("claim").setExecutor(new ClaimCommand());
        }
        if (getCommand("unclaim") != null) {
            getCommand("unclaim").setExecutor(new UnclaimCommand());
        }
        if (getCommand("create") != null) {
            getCommand("create").setExecutor(new CreateNationCommand());
        }

        // Listeners
        Bukkit.getPluginManager().registerEvents(new TerritoryProtectionListener(this, nationManager), this);
        Bukkit.getPluginManager().registerEvents(new WarMobilizationListener(this, nationManager, diplomacySystem), this);
        Bukkit.getPluginManager().registerEvents(new WarzoneVisualListener(this, diplomacySystem), this);
        Bukkit.getPluginManager().registerEvents(new ReligionBuffListener(this, religionManager), this);
        Bukkit.getPluginManager().registerEvents(new ModIntegrationListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.axiom.listener.LeaderActivityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DashboardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VisualEffectsListener(this, visualEffectsService), this);

        // Autosave
        long autosaveTicks = getConfig().getLong("autosave.intervalSeconds", 300) * 20L;
        if (autosaveTicks > 0) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                try { nationManager.flush(); } catch (Exception ignored) {}
            }, autosaveTicks, autosaveTicks);
        }

        getLogger().info("AXIOM enabled.");
    }

    private void setupDynmap() {
        // Dynmap integration disabled - optional dependency
        getLogger().info("Dynmap integration disabled.");
    }

    @Override
    public void onDisable() {
        ServiceLocator.clear();
        MapBoundaryService mapBoundaryService = ServiceLocator.get(MapBoundaryService.class);
        if (mapBoundaryService != null) {
            mapBoundaryService.shutdown();
        }
        try {
            ServiceLocator.get(NationManager.class).flush();
        } catch (Exception e) {
            getLogger().severe("Failed to flush data: " + e.getMessage());
        }
        getLogger().info("AXIOM disabled.");
    }

    /** Reloads config and propagates to systems. */
    public static AXIOM getInstance() { return instance; }

    // Service getters - delegate to ServiceLocator
    public NationManager getNationManager() { return ServiceLocator.get(NationManager.class); }
    public EconomyService getEconomyService() { return ServiceLocator.get(EconomyService.class); }
    public DiplomacySystem getDiplomacySystem() { return ServiceLocator.get(DiplomacySystem.class); }
    public ReligionManager getReligionManager() { return ServiceLocator.get(ReligionManager.class); }
    public CityGrowthEngine getCityGrowthEngine() { return ServiceLocator.get(CityGrowthEngine.class); }
    public PvpService getPvpService() { return ServiceLocator.get(PvpService.class); }
    public PlayerDataManager getPlayerDataManager() { return ServiceLocator.get(PlayerDataManager.class); }
    public ConfirmationService getConfirmationService() { return ServiceLocator.get(ConfirmationService.class); }
    public DoubleClickService getDoubleClickService() { return ServiceLocator.get(DoubleClickService.class); }
    public WalletService getWalletService() { return ServiceLocator.get(WalletService.class); }
    public NewsService getNewsService() { return ServiceLocator.get(NewsService.class); }
    public RolePermissionService getRolePermissionService() { return ServiceLocator.get(RolePermissionService.class); }
    public EventGenerator getEventGenerator() { return ServiceLocator.get(EventGenerator.class); }
    public ModIntegrationService getModIntegrationService() { return ServiceLocator.get(ModIntegrationService.class); }
    public EspionageService getEspionageService() { return ServiceLocator.get(EspionageService.class); }
    public TradeService getTradeService() { return ServiceLocator.get(TradeService.class); }
    public MapBoundaryService getMapBoundaryService() { return ServiceLocator.get(MapBoundaryService.class); }
    public StockMarketService getStockMarketService() { return ServiceLocator.get(StockMarketService.class); }
    public ElectionService getElectionService() { return ServiceLocator.get(ElectionService.class); }
    public CrimeService getCrimeService() { return ServiceLocator.get(CrimeService.class); }
    public EducationService getEducationService() { return ServiceLocator.get(EducationService.class); }
    public AchievementService getAchievementService() { return ServiceLocator.get(AchievementService.class); }
    public ClimateService getClimateService() { return ServiceLocator.get(ClimateService.class); }
    public HolidayService getHolidayService() { return ServiceLocator.get(HolidayService.class); }
    public HappinessService getHappinessService() { return ServiceLocator.get(HappinessService.class); }
    public TransportService getTransportService() { return ServiceLocator.get(TransportService.class); }
    public ResourceService getResourceService() { return ServiceLocator.get(ResourceService.class); }
    public TreatyService getTreatyService() { return ServiceLocator.get(TreatyService.class); }
    public NotificationService getNotificationService() { return ServiceLocator.get(NotificationService.class); }
    public StatisticsService getStatisticsService() { return ServiceLocator.get(StatisticsService.class); }
    public BankingService getBankingService() { return ServiceLocator.get(BankingService.class); }
    public MobilizationService getMobilizationService() { return ServiceLocator.get(MobilizationService.class); }
    public TechnologyTreeService getTechnologyTreeService() { return ServiceLocator.get(TechnologyTreeService.class); }
    public MilitaryService getMilitaryService() { return ServiceLocator.get(MilitaryService.class); }
    public VisualEffectsService getVisualEffectsService() { return ServiceLocator.get(VisualEffectsService.class); }
    public CultureService getCultureService() { return ServiceLocator.get(CultureService.class); }
    public PublicOpinionService getPublicOpinionService() { return ServiceLocator.get(PublicOpinionService.class); }
    public ModWarfareService getModWarfareService() { return ServiceLocator.get(ModWarfareService.class); }
    public AdvancedWarSystem getAdvancedWarSystem() { return ServiceLocator.get(AdvancedWarSystem.class); }
    public SiegeService getSiegeService() { return ServiceLocator.get(SiegeService.class); }
    public ConquestService getConquestService() { return ServiceLocator.get(ConquestService.class); }
    public RaidService getRaidService() { return ServiceLocator.get(RaidService.class); }
    public NavalService getNavalService() { return ServiceLocator.get(NavalService.class); }
    public NuclearWeaponsService getNuclearWeaponsService() { return ServiceLocator.get(NuclearWeaponsService.class); }
    public MilitaryAllianceService getMilitaryAllianceService() { return ServiceLocator.get(MilitaryAllianceService.class); }
    public IntelligenceService getIntelligenceService() { return ServiceLocator.get(IntelligenceService.class); }
    public PropagandaService getPropagandaService() { return ServiceLocator.get(PropagandaService.class); }
    public SanctionService getSanctionService() { return ServiceLocator.get(SanctionService.class); }
    public EmbargoService getEmbargoService() { return ServiceLocator.get(EmbargoService.class); }
    public DiplomaticRecognitionService getDiplomaticRecognitionService() { return ServiceLocator.get(DiplomaticRecognitionService.class); }
    public CurrencyExchangeService getCurrencyExchangeService() { return ServiceLocator.get(CurrencyExchangeService.class); }
    public CurrencyUnionService getCurrencyUnionService() { return ServiceLocator.get(CurrencyUnionService.class); }
    public TradeNetworkService getTradeNetworkService() { return ServiceLocator.get(TradeNetworkService.class); }
    public ResourceCartelService getResourceCartelService() { return ServiceLocator.get(ResourceCartelService.class); }
    public InfluenceService getInfluenceService() { return ServiceLocator.get(InfluenceService.class); }
    public CulturalExchangeService getCulturalExchangeService() { return ServiceLocator.get(CulturalExchangeService.class); }
    public CulturalHeritageService getCulturalHeritageService() { return ServiceLocator.get(CulturalHeritageService.class); }
    public MigrationService getMigrationService() { return ServiceLocator.get(MigrationService.class); }
    public RaceService getRaceService() { return ServiceLocator.get(RaceService.class); }
    public RacialDiscriminationService getRacialDiscriminationService() { return ServiceLocator.get(RacialDiscriminationService.class); }
    public DisasterService getDisasterService() { return ServiceLocator.get(DisasterService.class); }
    public PollutionService getPollutionService() { return ServiceLocator.get(PollutionService.class); }
    public InfrastructureService getInfrastructureService() { return ServiceLocator.get(InfrastructureService.class); }
    public HarborService getHarborService() { return ServiceLocator.get(HarborService.class); }
    public CorruptionService getCorruptionService() { return ServiceLocator.get(CorruptionService.class); }
    public BalancingService getBalancingService() { return ServiceLocator.get(BalancingService.class); }
    public NationModifierService getNationModifierService() { return ServiceLocator.get(NationModifierService.class); }
    public MonumentService getMonumentService() { return ServiceLocator.get(MonumentService.class); }
    public UnifiedEnergyService getUnifiedEnergyService() { return ServiceLocator.get(UnifiedEnergyService.class); }
    public ModEnergyService getModEnergyService() { return ServiceLocator.get(ModEnergyService.class); }
    public BackupService getBackupService() { return ServiceLocator.get(BackupService.class); }
    public TutorialService getTutorialService() { return ServiceLocator.get(TutorialService.class); }
    public WebExportService getWebExportService() { return ServiceLocator.get(WebExportService.class); }
    public PerformanceMetricsService getPerformanceMetricsService() { return ServiceLocator.get(PerformanceMetricsService.class); }
    public PlayerDashboardService getPlayerDashboardService() { return ServiceLocator.get(PlayerDashboardService.class); }
    public ModResourceService getModResourceService() { return ServiceLocator.get(ModResourceService.class); }
    
    // GUI getters
    public NationMainMenu getNationMainMenu() { return ServiceLocator.get(NationMainMenu.class); }
    public ConfirmMenu getConfirmMenu() { return ServiceLocator.get(ConfirmMenu.class); }
    public ReligionMenu getReligionMain() { return ServiceLocator.get(ReligionMenu.class); }
    public CitiesMenu getCitiesMenu() { return ServiceLocator.get(CitiesMenu.class); }
    public TechnologyMenu getTechnologyMenu() { return ServiceLocator.get(TechnologyMenu.class); }
}
