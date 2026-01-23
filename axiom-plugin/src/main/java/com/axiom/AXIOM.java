package com.axiom;

import com.axiom.command.AxiomCommand;
import com.axiom.command.CaptureCommand;
import com.axiom.command.ClaimCommand;
import com.axiom.command.CreateNationCommand;
import com.axiom.command.EconomicIndicatorsCommand;
import com.axiom.command.ModPackCommand;
import com.axiom.command.TechnologyCommand;
import com.axiom.command.UnclaimCommand;
import com.axiom.command.TestBotCommand;
import com.axiom.gui.NationMainMenu;
import com.axiom.command.NationCommandAlias;
import com.axiom.gui.AdvancedFeaturesMenu;
import com.axiom.gui.AdvancedTechnologyTreeMenu;
import com.axiom.gui.BankingMenu;
import com.axiom.gui.CaptureSystemMenu;
import com.axiom.gui.CitizenshipMenu;
import com.axiom.gui.ConfirmMenu;
import com.axiom.gui.DiplomacyMenu;
import com.axiom.gui.EconomyMenu;
import com.axiom.gui.ElectionMenu;
import com.axiom.gui.HistoryMenu;
import com.axiom.gui.ModPackBuilderMenu;
import com.axiom.gui.ProfileMenu;
import com.axiom.gui.ReligionMenu;
import com.axiom.gui.TerritoryMenu;
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
import com.axiom.service.UnifiedEspionageService;
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
import com.axiom.service.FestivalService;
import com.axiom.service.SupplyChainService;
import com.axiom.service.RevoltService;
import com.axiom.service.DiplomaticMissionService;
import com.axiom.service.CurrencyExchangeService;
import com.axiom.service.NavalService;
import com.axiom.service.SanitationService;
import com.axiom.service.PrisonService;
import com.axiom.service.BlackMarketService;
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
import com.axiom.service.DiplomaticImmunityService;
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
import com.axiom.service.ModPackBuilderService;
import com.axiom.service.CountryCaptureService;
import com.axiom.service.MapBoundaryVisualizationService;
import com.axiom.service.EconomicIndicatorsService;
import com.axiom.service.ModBalancerService;
import com.axiom.service.ModCompatibilityChecker;
import com.axiom.listener.ModIntegrationListener;
import com.axiom.listener.DashboardListener;
import com.axiom.listener.VisualEffectsListener;
import com.axiom.service.RecipeIntegrationService;
import com.axiom.service.ModIntegrationEnhancementService;
import com.axiom.service.ModPackManagerService;
import com.axiom.service.ModBalancerService;
import com.axiom.service.PropagandaCampaignService;
import com.axiom.service.ServiceFactory;
import com.axiom.util.CacheManager;
import com.axiom.controller.MilitaryController;
import com.axiom.service.MilitaryServiceInterface;
import com.axiom.service.EconomyServiceInterface;
import com.axiom.api.ModIntegrationAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AXIOM Geopolitical Engine
 * <p>
 * Main plugin bootstrap class. Wires services, registers commands and listeners,
 * manages configuration lifecycle.
 */
public final class AXIOM extends JavaPlugin {

    private NationManager nationManager;
    private EconomyService economyService;
    private DiplomacySystem diplomacySystem;
    private ReligionManager religionManager;
    private CityGrowthEngine cityGrowthEngine;
    private PvpService pvpService;
    private PlayerDataManager playerDataManager;
    private ModPackBuilderService modPackBuilderService;
    private ModCompatibilityChecker modCompatibilityChecker;
    private CountryCaptureService countryCaptureService;
    private MapBoundaryVisualizationService mapBoundaryVisualizationService;
    private EconomicIndicatorsService economicIndicatorsService;
    private ModBalancerService modBalancerService;
    private static AXIOM instance;
    private NationMainMenu nationMainMenu;
    private ConfirmMenu confirmMenu;
    private TechnologyMenu technologyMenu;
    private ReligionMenu religionMain;
    private CitiesMenu citiesMenu;
    private CorporationsMenu corporationsMenu;
    private ConfirmationService confirmationService;
    private DoubleClickService doubleClickService;
    private WalletService walletService;
    private NewsService newsService;
    private RolePermissionService rolePermissionService;
    private EventGenerator eventGenerator;
    private ModIntegrationService modIntegrationService;
    private UnifiedEspionageService unifiedEspionageService;
    private TradeService tradeService;
    private StockMarketService stockMarketService;
    private ElectionService electionService;
    private CrimeService crimeService;
    private EducationService educationService;
    private AchievementService achievementService;
    private ClimateService climateService;
    private HolidayService holidayService;
    private HappinessService happinessService;
    private TransportService transportService;
    private ResourceService resourceService;
    private TreatyService treatyService;
    private NotificationService notificationService;
    private StatisticsService statisticsService;
    private BankingService bankingService;
    private MobilizationService mobilizationService;
    private ChatService chatService;
    private TradingPostService tradingPostService;
    private PlayerReputationService playerReputationService;
    private TechnologyTreeService technologyTreeService;
    private VassalService vassalService;
    private QuestService questService;
    private NationModifierService nationModifierService;
    private PollutionService pollutionService;
    private AllianceService allianceService;
    private CrisisResponseService crisisResponseService;
    private DynastyService dynastyService;
    private InfluenceService influenceService;
    private MigrationService migrationService;
    private EmbargoService embargoService;
    private LegacyService legacyService;
    private RitualService ritualService;
    private ConquestService conquestService;
    private CultureService cultureService;
    private PropagandaService propagandaService;
    private DisasterService disasterService;
    private FestivalService festivalService;
    private SupplyChainService supplyChainService;
    private RevoltService revoltService;
    private DiplomaticMissionService diplomaticMissionService;
    private CurrencyExchangeService currencyExchangeService;
    private NavalService navalService;
    private SanitationService sanitationService;
    private PrisonService prisonService;
    private BlackMarketService blackMarketService;
    private ResistanceMovementService resistanceMovementService;
    private MonumentService monumentService;
    private PandemicService pandemicService;
    private InfrastructureService infrastructureService;
    private RefugeeService refugeeService;
    private GreatWorksService greatWorksService;
    private RaidService raidService;
    private TributeService tributeService;
    private PlagueService plagueService;
    private PartisanService partisanService;
    private ConscriptionService conscriptionService;
    private BlockadeService blockadeService;
    private HarborService harborService;
    private DiplomaticImmunityService diplomaticImmunityService;
    private FamineService famineService;
    private AssassinationService assassinationService;
    private ColonizationService colonizationService;
    private CensusService censusService;
    private ResourceDepletionService resourceDepletionService;
    private SiegeService siegeService;
    private FortificationService fortificationService;
    private CoupService coupService;
    private TradeAgreementService tradeAgreementService;
    private TerrorismService terrorismService;
    private CeasefireService ceasefireService;
    private MilitaryService militaryService;
    private ReligiousWarService religiousWarService;
    private ImportExportService importExportService;
    private EnergyService energyService;
    private RebellionService rebellionService;
    private TreatyViolationService treatyViolationService;
    private SanctionService sanctionService;
    private ResourceScarcityService resourceScarcityService;
    private TradeRouteService tradeRouteService;
    private DiplomaticRecognitionService diplomaticRecognitionService;
    private CultureShockService cultureShockService;
    private BorderControlService borderControlService;
    private TaxEvasionService taxEvasionService;
    private MonetaryPolicyService monetaryPolicyService;
    private ResearchFundingService researchFundingService;
    private CommodityMarketService commodityMarketService;
    private ImmigrationControlService immigrationControlService;
    private EmergencyService emergencyService;
    private ArmsDealService armsDealService;
    private TreatyRenegotiationService treatyRenegotiationService;
    private EconomicCrisisService economicCrisisService;
    private CorruptionService corruptionService;
    private ResourceDiscoveryService resourceDiscoveryService;
    private WarCrimeService warCrimeService;
    private PeaceTreatyService peaceTreatyService;
    private TradeEmbargoExpansionService tradeEmbargoExpansionService;
    private RaceService raceService;
    private RacialDiscriminationService racialDiscriminationService;
    private TradeNetworkService tradeNetworkService;
    private MilitaryExerciseService militaryExerciseService;
    private ResourceProcessingService resourceProcessingService;
    private PublicOpinionService publicOpinionService;
    private InfrastructureMaintenanceService infrastructureMaintenanceService;
    private DiplomaticProtocolService diplomaticProtocolService;
    private PropagandaCampaignService propagandaCampaignService;
    private CurrencyManipulationService currencyManipulationService;
    private ResourceStockpileService resourceStockpileService;
    private CulturalExchangeService culturalExchangeService;
    private MilitaryIntelligenceService militaryIntelligenceService;
    private EnvironmentalPolicyService environmentalPolicyService;
    private TradeDisputeService tradeDisputeService;
    private SocialWelfareService socialWelfareService;
    private NuclearWeaponsService nuclearWeaponsService;
    private ArmsControlService armsControlService;
    private RefugeeResettlementService refugeeResettlementService;
    private InternationalCourtService internationalCourtService;
    private CulturalHeritageService culturalHeritageService;
    private ResourceNationalizationService resourceNationalizationService;
    private CurrencyUnionService currencyUnionService;
    private ResearchCollaborationService researchCollaborationService;
    private SpaceProgramService spaceProgramService;
    private TradeWarService tradeWarService;
    private MilitaryAllianceService militaryAllianceService;
    private ResourceCartelService resourceCartelService;
    private PopulationGrowthService populationGrowthService;
    private InternationalAidService internationalAidService;
    private DiplomaticSummitService diplomaticSummitService;
    private CulturalRevolutionService culturalRevolutionService;
    private ModResourceService modResourceService;
    private ModWarfareService modWarfareService;
    private ModEnergyService modEnergyService;
    private ModIntegrationAPI modIntegrationAPI;
    private BalancingService balancingService;
    private BackupService backupService;
    private WebExportService webExportService;
    private TutorialService tutorialService;
    private PerformanceMetricsService performanceMetricsService;
    private MultiWorldService multiWorldService;
    private PlayerDashboardService playerDashboardService;
    private VisualEffectsService visualEffectsService;
    private MapBoundaryService mapBoundaryService;
    private AdvancedWarSystem advancedWarSystem;
    
    private ModPackManagerService modPackManagerService;
    private RecipeIntegrationService recipeIntegrationService;
    private ModIntegrationEnhancementService modIntegrationEnhancementService;

    // New Architecture Components
    private ServiceFactory serviceFactory;
    private CacheManager cacheManager;
    private MilitaryController militaryController;
    private MilitaryServiceInterface militaryServiceInterface;
    private EconomyServiceInterface economyServiceInterface;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.nationManager = new NationManager(this);
        this.economyService = new EconomyService(this, nationManager);
        this.diplomacySystem = new DiplomacySystem(this, nationManager);
        this.religionManager = new ReligionManager(this);
        this.cityGrowthEngine = new CityGrowthEngine(this, nationManager);
        this.pvpService = new PvpService(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.nationMainMenu = null; // Будет инициализироваться при открытии меню
        this.confirmMenu = new ConfirmMenu(this);
        this.technologyMenu = new TechnologyMenu(this);
        this.confirmationService = new ConfirmationService();
        this.doubleClickService = new DoubleClickService();
        this.walletService = new WalletService(this, playerDataManager);
        // this.religionMain = new ReligionMenu(this); // Per-player menu
        // this.citiesMenu = new CitiesMenu(this, nationManager, cityGrowthEngine); // Per-player menu
        this.stockMarketService = new StockMarketService(this);
        this.corporationsMenu = new CorporationsMenu(this, stockMarketService);
        this.newsService = new NewsService(this, nationManager);
        this.rolePermissionService = new RolePermissionService(this);
        this.eventGenerator = new EventGenerator(this, nationManager);
        this.modIntegrationService = new ModIntegrationService(this);
        this.mapBoundaryService = new MapBoundaryService(this, nationManager, cityGrowthEngine, modIntegrationService);
        this.unifiedEspionageService = new UnifiedEspionageService(this, nationManager);
        this.modPackBuilderService = new ModPackBuilderService(this);
        this.modCompatibilityChecker = new ModCompatibilityChecker(this);
        this.visualEffectsService = new VisualEffectsService(this);
        this.countryCaptureService = new CountryCaptureService(this);
        this.mapBoundaryVisualizationService = new MapBoundaryVisualizationService(this, nationManager, cityGrowthEngine, modIntegrationService, visualEffectsService);
        this.tradeService = new TradeService(this, nationManager);
        this.electionService = new ElectionService(this, nationManager);
        this.crimeService = new CrimeService(this, nationManager);
        this.educationService = new EducationService(this, nationManager);
        this.achievementService = new AchievementService(this);
        this.climateService = new ClimateService(this);
        this.holidayService = new HolidayService(this, religionManager);
        this.happinessService = new HappinessService(this, nationManager, cityGrowthEngine, crimeService, educationService);
        this.transportService = new TransportService(this);
        this.resourceService = new ResourceService(this);
        this.treatyService = new TreatyService(this, nationManager);
        this.notificationService = new NotificationService(this);
        this.statisticsService = new StatisticsService(this);
        this.bankingService = new BankingService(this, nationManager);
        this.mobilizationService = new MobilizationService(this, nationManager, diplomacySystem);
        this.chatService = new ChatService(this);
        this.tradingPostService = new TradingPostService(this);
        this.playerReputationService = new PlayerReputationService(this);
        this.technologyTreeService = new TechnologyTreeService(this);
        this.vassalService = new VassalService(this, nationManager);
        this.questService = new QuestService(this);
        this.nationModifierService = new NationModifierService(this);
        this.pollutionService = new PollutionService(this, nationManager);
        this.allianceService = new AllianceService(this, diplomacySystem);
        this.crisisResponseService = new CrisisResponseService(this, nationManager);
        this.dynastyService = new DynastyService(this);
        this.influenceService = new InfluenceService(this);
        this.migrationService = new MigrationService(this);
        this.embargoService = new EmbargoService(this);
        this.legacyService = new LegacyService(this);
        this.ritualService = new RitualService(this);
        this.conquestService = new ConquestService(this, nationManager);
        this.cultureService = new CultureService(this);
        this.propagandaService = new PropagandaService(this, nationManager);
        this.disasterService = new DisasterService(this, nationManager);
        this.festivalService = new FestivalService(this);
        this.supplyChainService = new SupplyChainService(this);
        this.revoltService = new RevoltService(this, nationManager, happinessService);
        this.diplomaticMissionService = new DiplomaticMissionService(this);
        this.currencyExchangeService = new CurrencyExchangeService(this, nationManager, economyService);
        this.navalService = new NavalService(this);
        this.sanitationService = new SanitationService(this, nationManager);
        this.prisonService = new PrisonService(this);
        this.blackMarketService = new BlackMarketService(this);
        this.resistanceMovementService = new ResistanceMovementService(this);
        this.monumentService = new MonumentService(this);
        this.pandemicService = new PandemicService(this, nationManager);
        this.infrastructureService = new InfrastructureService(this);
        this.refugeeService = new RefugeeService(this, nationManager);
        this.greatWorksService = new GreatWorksService(this);
        this.raidService = new RaidService(this, nationManager);
        this.tributeService = new TributeService(this, nationManager);
        this.plagueService = new PlagueService(this, nationManager);
        this.partisanService = new PartisanService(this);
        this.conscriptionService = new ConscriptionService(this, nationManager);
        this.blockadeService = new BlockadeService(this);
        this.harborService = new HarborService(this);
        this.diplomaticImmunityService = new DiplomaticImmunityService(this, nationManager);
        this.famineService = new FamineService(this, nationManager);
        this.assassinationService = new AssassinationService(this, nationManager);
        this.colonizationService = new ColonizationService(this, nationManager);
        this.censusService = new CensusService(this, nationManager);
        this.resourceDepletionService = new ResourceDepletionService(this);
        this.siegeService = new SiegeService(this, nationManager);
        this.militaryService = new MilitaryService(this);
        
        // Advanced War System (requires Military, Conquest, Raid, Siege services)
        this.advancedWarSystem = new AdvancedWarSystem(this, nationManager, diplomacySystem,
            militaryService, conquestService, raidService, siegeService);
        
        this.fortificationService = new FortificationService(this);
        this.coupService = new CoupService(this, nationManager);
        this.tradeAgreementService = new TradeAgreementService(this);
        this.terrorismService = new TerrorismService(this, nationManager);
        this.ceasefireService = new CeasefireService(this, nationManager);
        this.religiousWarService = new ReligiousWarService(this, nationManager);
        this.importExportService = new ImportExportService(this);
        this.energyService = new EnergyService(this);
        this.rebellionService = new RebellionService(this, nationManager);
        this.treatyViolationService = new TreatyViolationService(this, nationManager);
        this.sanctionService = new SanctionService(this, nationManager);
        this.resourceScarcityService = new ResourceScarcityService(this);
        this.tradeRouteService = new TradeRouteService(this);
        this.diplomaticRecognitionService = new DiplomaticRecognitionService(this, nationManager);
        this.cultureShockService = new CultureShockService(this);
        this.borderControlService = new BorderControlService(this);
        this.taxEvasionService = new TaxEvasionService(this);
        this.monetaryPolicyService = new MonetaryPolicyService(this);
        this.researchFundingService = new ResearchFundingService(this);
        this.commodityMarketService = new CommodityMarketService(this);
        this.immigrationControlService = new ImmigrationControlService(this);
        this.emergencyService = new EmergencyService(this, nationManager);
        this.armsDealService = new ArmsDealService(this, nationManager);
        this.treatyRenegotiationService = new TreatyRenegotiationService(this, nationManager);
        this.economicCrisisService = new EconomicCrisisService(this, nationManager);
        this.corruptionService = new CorruptionService(this, nationManager);
        this.resourceDiscoveryService = new ResourceDiscoveryService(this, nationManager);
        this.warCrimeService = new WarCrimeService(this, nationManager);
        this.peaceTreatyService = new PeaceTreatyService(this, nationManager);
        this.tradeEmbargoExpansionService = new TradeEmbargoExpansionService(this);
        this.raceService = new RaceService(this);
        this.racialDiscriminationService = new RacialDiscriminationService(this, nationManager);
        this.tradeNetworkService = new TradeNetworkService(this, nationManager);
        this.militaryExerciseService = new MilitaryExerciseService(this, nationManager);
        this.resourceProcessingService = new ResourceProcessingService(this);
        this.publicOpinionService = new PublicOpinionService(this, nationManager);
        this.infrastructureMaintenanceService = new InfrastructureMaintenanceService(this, nationManager);
        this.diplomaticProtocolService = new DiplomaticProtocolService(this);
        this.propagandaCampaignService = new PropagandaCampaignService(this, nationManager);
        this.currencyManipulationService = new CurrencyManipulationService(this);
        this.resourceStockpileService = new ResourceStockpileService(this, nationManager);
        this.culturalExchangeService = new CulturalExchangeService(this, nationManager);
        this.militaryIntelligenceService = new MilitaryIntelligenceService(this, nationManager);
        this.environmentalPolicyService = new EnvironmentalPolicyService(this, nationManager);
        this.tradeDisputeService = new TradeDisputeService(this, nationManager);
        this.socialWelfareService = new SocialWelfareService(this, nationManager);
        this.nuclearWeaponsService = new NuclearWeaponsService(this, nationManager);
        this.armsControlService = new ArmsControlService(this, nationManager);
        this.refugeeResettlementService = new RefugeeResettlementService(this, nationManager);
        this.internationalCourtService = new InternationalCourtService(this, nationManager);
        this.culturalHeritageService = new CulturalHeritageService(this, nationManager);
        this.resourceNationalizationService = new ResourceNationalizationService(this);
        this.currencyUnionService = new CurrencyUnionService(this, nationManager);
        this.researchCollaborationService = new ResearchCollaborationService(this, nationManager);
        this.spaceProgramService = new SpaceProgramService(this, nationManager);
        this.tradeWarService = new TradeWarService(this, nationManager);
        this.militaryAllianceService = new MilitaryAllianceService(this, nationManager);
        this.resourceCartelService = new ResourceCartelService(this, nationManager);
        this.populationGrowthService = new PopulationGrowthService(this, nationManager);
        this.internationalAidService = new InternationalAidService(this, nationManager);
        this.diplomaticSummitService = new DiplomaticSummitService(this, nationManager);
        this.culturalRevolutionService = new CulturalRevolutionService(this, nationManager);
        
        // Mod integration services (initialized after modIntegration)
        this.modResourceService = new ModResourceService(this, modIntegrationService, resourceService);
        this.modWarfareService = new ModWarfareService(this, modIntegrationService);
        this.modEnergyService = new ModEnergyService(this, modIntegrationService);
        this.modIntegrationAPI = new ModIntegrationAPI(this);
        
        // Advanced Mod Integration
        this.recipeIntegrationService = new RecipeIntegrationService(this);
        this.modIntegrationEnhancementService = new ModIntegrationEnhancementService(this);
        this.modPackManagerService = new ModPackManagerService(this);

        // New essential services
        this.economicIndicatorsService = new EconomicIndicatorsService(this);
        this.balancingService = new BalancingService(this, nationManager, playerDataManager);
        this.backupService = new BackupService(this);
        this.webExportService = new WebExportService(this, nationManager);
        this.tutorialService = new TutorialService(this);
        this.performanceMetricsService = new PerformanceMetricsService(this);
        this.multiWorldService = new MultiWorldService(this, nationManager);
        this.playerDashboardService = new PlayerDashboardService(this);
        this.visualEffectsService = new VisualEffectsService(this);
        this.modBalancerService = new ModBalancerService(this, nationManager, modIntegrationService);
        
        // PlaceholderAPI integration (if available)
        // Note: PlaceholderAPI integration requires the plugin to be installed.
        // The expansion class will be loaded dynamically at runtime.
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

        // Initialize New Architecture Components
        this.cacheManager = new CacheManager(this);
        this.serviceFactory = new ServiceFactory(this);
        
        // Create services via factory
        this.militaryServiceInterface = serviceFactory.createMilitaryService();
        this.economyServiceInterface = serviceFactory.createEconomyService();
        
        // Create controllers
        this.militaryController = new MilitaryController(
            this,
            militaryServiceInterface,
            economyServiceInterface,
            nationManager,
            cacheManager
        );

        // Commands
        if (getCommand("axiom") != null) {
            // Single root dispatcher handles subcommands
            getCommand("axiom").setExecutor(new AxiomCommand(this));
        }
        if (getCommand("military") != null) {
            getCommand("military").setExecutor(militaryController);
        }
        if (getCommand("testbot") != null) {
            getCommand("testbot").setExecutor(new TestBotCommand(this));
        }
        if (getCommand("claim") != null) {
            getCommand("claim").setExecutor(new ClaimCommand(this, nationManager));
        }
        if (getCommand("unclaim") != null) {
            getCommand("unclaim").setExecutor(new UnclaimCommand(this, nationManager));
        }
        if (getCommand("create") != null) {
            getCommand("create").setExecutor(new CreateNationCommand(this, nationManager, playerDataManager));
        }
        if (getCommand("modpack") != null) {
            getCommand("modpack").setExecutor(new ModPackCommand(this));
        }
        if (getCommand("capture") != null) {
            getCommand("capture").setExecutor(new CaptureCommand(this));
        }
        if (getCommand("technology") != null) {
            getCommand("technology").setExecutor(new TechnologyCommand(this));
        }
        if (getCommand("tech") != null) {
            getCommand("tech").setExecutor(new TechnologyCommand(this));
        }
        if (getCommand("econom") != null) {
            getCommand("econom").setExecutor(new EconomicIndicatorsCommand(this));
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

    @Override
    public void onDisable() {
        if (mapBoundaryService != null) {
            mapBoundaryService.shutdown();
        }
        try {
            nationManager.flush();
        } catch (Exception e) {
            getLogger().severe("Failed to flush data: " + e.getMessage());
        }
        getLogger().info("AXIOM disabled.");
    }

    /** Reloads config and propagates to systems. */
    public void reloadAxiomConfig() {
        reloadConfig();
        economyService.reload();
    }

    public NationManager getNationManager() { return nationManager; }
    public EconomyService getEconomyService() { return economyService; }
    public DiplomacySystem getDiplomacySystem() { return diplomacySystem; }
    public ReligionManager getReligionManager() { return religionManager; }
    public CityGrowthEngine getCityGrowthEngine() { return cityGrowthEngine; }
    public PvpService getPvpService() { return pvpService; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ModPackBuilderService getModPackBuilderService() { return modPackBuilderService; }
    public ModCompatibilityChecker getModCompatibilityChecker() { return modCompatibilityChecker; }
    public CountryCaptureService getCountryCaptureService() { return countryCaptureService; }
    public MapBoundaryVisualizationService getMapBoundaryVisualizationService() { return mapBoundaryVisualizationService; }
    public EconomicIndicatorsService getEconomicIndicatorsService() { return economicIndicatorsService; }
    public static AXIOM getInstance() { return instance; }
    public void openNationMainMenu(Player player) {
        new NationMainMenu(this, player).open();
    }
    
    // Устаревший метод, но сохраняем для совместимости
    @Deprecated
    public NationMainMenu getNationMainMenu() { 
        return null; // В новой системе меню создается при открытии
    }
    
    /**
     * Открытие меню экономической системы
     */
    public void openEconomyMenu(Player player) {
        new EconomyMenu(this, player).open();
    }
    
    /**
     * Открытие меню банковской системы
     */
    public void openBankingMenu(Player player) {
        new BankingMenu(this, player).open();
    }
    
    /**
     * Открытие меню дипломатической системы
     */
    public void openDiplomacyMenu(Player player) {
        new DiplomacyMenu(this, player).open();
    }
    
    /**
     * Открытие меню религиозной системы
     */
    public void openReligionMenu(Player player) {
        new ReligionMenu(this, player).open();
    }
    
    /**
     * Открытие меню городской системы
     */
    public void openCitiesMenu(Player player) {
        new CitiesMenu(this, player).open();
    }
    
    /**
     * Открытие меню профиля игрока
     */
    public void openProfileMenu(Player player) {
        new ProfileMenu(this, player).open();
    }
    
    /**
     * Открытие меню гражданства
     */
    public void openCitizenshipMenu(Player player) {
        new CitizenshipMenu(this, player).open();
    }
    
    /**
     * Открытие меню управления территорией
     */
    public void openTerritoryMenu(Player player) {
        new TerritoryMenu(this, player).open();
    }
    
    /**
     * Открытие меню истории
     */
    public void openHistoryMenu(Player player) {
        new HistoryMenu(this, player).open();
    }
    
    /**
     * Открытие меню продвинутых функций
     */
    public void openAdvancedFeaturesMenu(Player player) {
        new AdvancedFeaturesMenu(this, player).open();
    }
    
    /**
     * Открытие меню выборов
     */
    public void openElectionMenu(Player player) {
        new ElectionMenu(this, player).open();
    }
    
    /**
     * Открытие расширенного меню технологического древа
     */
    public void openAdvancedTechnologyTreeMenu(Player player) {
        new AdvancedTechnologyTreeMenu(this, player).open();
    }
    
    /**
     * Открытие меню экономических индикаторов
     */
    public void openEconomicIndicatorsMenu(Player player) {
        new com.axiom.gui.EconomicIndicatorsMenu(this, player).open();
    }
    
    /**
     * Открытие меню конструктора модпаков
     */
    public void openModPackBuilderMenu(Player player) {
        new ModPackBuilderMenu(this, player).open();
    }
    
    /**
     * Открытие меню системы захвата
     */
    public void openCaptureSystemMenu(Player player) {
        new CaptureSystemMenu(this, player).open();
    }
    public ConfirmMenu getConfirmMenu() { return confirmMenu; }
    public TechnologyMenu getTechnologyMenu() { return technologyMenu; }
    public ConfirmationService getConfirmationService() { return confirmationService; }
    public DoubleClickService getDoubleClickService() { return doubleClickService; }
    public WalletService getWalletService() { return walletService; }
    @Deprecated
    public ReligionMenu getReligionMain() { return null; } // Устаревшее, используйте openReligionMenu()
    public CitiesMenu getCitiesMenu() { 
        return null; // Устаревшее, используйте openCitiesMenu()
    }
    public CorporationsMenu getCorporationsMenu() { return corporationsMenu; }
    public NewsService getNewsService() { return newsService; }
    public ElectionService getElectionService() { return electionService; }
    public CrimeService getCrimeService() { return crimeService; }
    public EducationService getEducationService() { return educationService; }
    public AchievementService getAchievementService() { return achievementService; }
    public ResourceService getResourceService() { return resourceService; }
    public NationModifierService getNationModifierService() { return nationModifierService; }
    public NavalService getNavalService() { return navalService; }
    public HappinessService getHappinessService() { return happinessService; }
    public MilitaryService getMilitaryService() { return militaryService; }
    public CultureService getCultureService() { return cultureService; }
    public RolePermissionService getRolePermissionService() { return rolePermissionService; }
    public MigrationService getMigrationService() { return migrationService; }
    public RaceService getRaceService() { return raceService; }
    public PublicOpinionService getPublicOpinionService() { return publicOpinionService; }
    public CorruptionService getCorruptionService() { return corruptionService; }
    public PollutionService getPollutionService() { return pollutionService; }
    public NuclearWeaponsService getNuclearWeaponsService() { return nuclearWeaponsService; }
    public TechnologyTreeService getTechnologyTreeService() { return technologyTreeService; }
    public ModIntegrationService getModIntegrationService() { return modIntegrationService; }
    public ModResourceService getModResourceService() { return modResourceService; }
    public ModWarfareService getModWarfareService() { return modWarfareService; }
    public ModEnergyService getModEnergyService() { return modEnergyService; }
    public ModIntegrationAPI getModIntegrationAPI() { return modIntegrationAPI; }
    public StatisticsService getStatisticsService() { return statisticsService; }
    public BalancingService getBalancingService() { return balancingService; }
    public BackupService getBackupService() { return backupService; }
    public WebExportService getWebExportService() { return webExportService; }
    public TutorialService getTutorialService() { return tutorialService; }
    public PerformanceMetricsService getPerformanceMetricsService() { return performanceMetricsService; }
    public MultiWorldService getMultiWorldService() { return multiWorldService; }
    public PlayerDashboardService getPlayerDashboardService() { return playerDashboardService; }
    public VisualEffectsService getVisualEffectsService() { return visualEffectsService; }
    public TreatyService getTreatyService() { return treatyService; }
    public TradeService getTradeService() { return tradeService; }
    public TradingPostService getTradingPostService() { return tradingPostService; }
    public AdvancedWarSystem getAdvancedWarSystem() { return advancedWarSystem; }
    public MapBoundaryService getMapBoundaryService() { return mapBoundaryService; }
    public BankingService getBankingService() { return bankingService; }
    public StockMarketService getStockMarketService() { return stockMarketService; }
    public DiplomaticImmunityService getDiplomaticImmunityService() { return diplomaticImmunityService; }
    
    // Additional getters for SuperTestBot
    public RaidService getRaidService() { return raidService; }
    public SiegeService getSiegeService() { return siegeService; }
    public ConquestService getConquestService() { return conquestService; }
    public MobilizationService getMobilizationService() { return mobilizationService; }
    public CurrencyUnionService getCurrencyUnionService() { return currencyUnionService; }
    public ResourceCartelService getResourceCartelService() { return resourceCartelService; }
    public MilitaryAllianceService getMilitaryAllianceService() { return militaryAllianceService; }
    public RacialDiscriminationService getRacialDiscriminationService() { return racialDiscriminationService; }
    public CulturalExchangeService getCulturalExchangeService() { return culturalExchangeService; }
    public CulturalHeritageService getCulturalHeritageService() { return culturalHeritageService; }
    public PropagandaService getPropagandaService() { return propagandaService; }
    public SanctionService getSanctionService() { return sanctionService; }
    public EmbargoService getEmbargoService() { return embargoService; }
    public DiplomaticRecognitionService getDiplomaticRecognitionService() { return diplomaticRecognitionService; }
    public CurrencyExchangeService getCurrencyExchangeService() { return currencyExchangeService; }
    public HarborService getHarborService() { return harborService; }
    public MonumentService getMonumentService() { return monumentService; }
    public UnifiedEspionageService getUnifiedEspionageService() { return unifiedEspionageService; }
    public DisasterService getDisasterService() { return disasterService; }
    public InfrastructureService getInfrastructureService() { return infrastructureService; }
    public InfluenceService getInfluenceService() { return influenceService; }
    public TradeNetworkService getTradeNetworkService() { return tradeNetworkService; }
    public ModBalancerService getModBalancerService() { return modBalancerService; }
    public PropagandaCampaignService getPropagandaCampaignService() { return propagandaCampaignService; }
    public RecipeIntegrationService getRecipeIntegrationService() { return recipeIntegrationService; }
    public ModIntegrationEnhancementService getModIntegrationEnhancementService() { return modIntegrationEnhancementService; }
    public ModPackManagerService getModPackManagerService() { return modPackManagerService; }
}
