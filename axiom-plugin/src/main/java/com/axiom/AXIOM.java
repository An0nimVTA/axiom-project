package com.axiom;

import com.axiom.app.command.AxiomCommand;
import com.axiom.app.command.CaptureCommand;
import com.axiom.app.command.ClaimCommand;
import com.axiom.app.command.EconomicIndicatorsCommand;
import com.axiom.app.command.MapBoundaryVisualizationCommand;
import com.axiom.app.command.ModBalanceCommand;
import com.axiom.app.command.ModIntegrationEnhancementCommand;
import com.axiom.app.command.ModPackManagerCommand;
import com.axiom.app.command.RecipeIntegrationCommand;
import com.axiom.app.command.ModPackCommand;
import com.axiom.app.command.TechnologyCommand;
import com.axiom.app.command.UnclaimCommand;
import com.axiom.app.command.TestBotCommand;
import com.axiom.app.command.TestCommand;
import com.axiom.app.gui.NationMainMenu;
import com.axiom.app.command.NationCommandAlias;
import com.axiom.domain.DomainServices;
import com.axiom.test.AutoTestBot;
import com.axiom.test.AutotestSupport;
import com.axiom.test.TestBot;
import com.axiom.app.gui.AdvancedFeaturesMenu;
import com.axiom.app.gui.AdvancedTechnologyTreeMenu;
import com.axiom.app.gui.BankingMenu;
import com.axiom.app.gui.CaptureSystemMenu;
import com.axiom.app.gui.CitizenshipMenu;
import com.axiom.app.gui.ConfirmMenu;
import com.axiom.app.gui.DiplomacyMenu;
import com.axiom.app.gui.EconomyMenu;
import com.axiom.app.gui.ElectionMenu;
import com.axiom.app.gui.HistoryMenu;
import com.axiom.app.gui.ModPackBuilderMenu;
import com.axiom.app.gui.ProfileMenu;
import com.axiom.app.gui.ReligionMenu;
import com.axiom.app.gui.TerritoryMenu;
import com.axiom.app.gui.CitiesMenu;
import com.axiom.app.gui.TechnologyMenu;
import com.axiom.app.gui.CorporationsMenu;
import com.axiom.app.listener.TerritoryProtectionListener;
import com.axiom.app.listener.WarMobilizationListener;
import com.axiom.app.listener.WarzoneVisualListener;
import com.axiom.app.listener.ReligionBuffListener;
import com.axiom.app.listener.UiAutotestStartListener;
import com.axiom.domain.service.state.CityGrowthEngine;
import com.axiom.domain.service.politics.DiplomacyRelationService;
import com.axiom.domain.service.politics.DiplomacySystem;
import com.axiom.domain.service.industry.EconomyService;
import com.axiom.domain.service.state.NationManager;
import com.axiom.domain.service.politics.ReligionManager;
import com.axiom.domain.service.military.PvpService;
import com.axiom.domain.service.state.PlayerDataManager;
import com.axiom.domain.service.infrastructure.ConfirmationService;
import com.axiom.domain.service.infrastructure.DoubleClickService;
import com.axiom.domain.service.industry.WalletService;
import com.axiom.domain.service.infrastructure.NewsService;
import com.axiom.domain.service.state.RolePermissionService;
import com.axiom.domain.service.infrastructure.EventGenerator;
import com.axiom.domain.service.infrastructure.ModBalancerService;
import com.axiom.domain.service.infrastructure.ModIntegrationService;
import com.axiom.domain.service.military.UnifiedEspionageService;
import com.axiom.domain.service.industry.TradeService;
import com.axiom.domain.service.infrastructure.MapBoundaryService;
import com.axiom.domain.service.industry.StockMarketService;
import com.axiom.domain.service.politics.ElectionService;
import com.axiom.domain.service.state.CrimeService;
import com.axiom.domain.service.state.EducationService;
import com.axiom.domain.service.infrastructure.AchievementService;
import com.axiom.domain.service.state.ClimateService;
import com.axiom.domain.service.politics.HolidayService;
import com.axiom.domain.service.state.HappinessService;
import com.axiom.domain.service.infrastructure.TransportService;
import com.axiom.domain.service.industry.ResourceCatalogService;
import com.axiom.domain.service.industry.ResourceService;
import com.axiom.domain.service.politics.TreatyService;
import com.axiom.domain.service.infrastructure.NotificationService;
import com.axiom.domain.service.infrastructure.StatisticsService;
import com.axiom.domain.service.industry.BankingService;
import com.axiom.domain.service.military.MobilizationService;
import com.axiom.domain.service.infrastructure.ChatService;
import com.axiom.domain.service.industry.TradingPostService;
import com.axiom.domain.service.state.PlayerReputationService;
import com.axiom.domain.service.technology.TechnologyTreeService;
import com.axiom.domain.service.politics.VassalService;
import com.axiom.domain.service.infrastructure.QuestService;
import com.axiom.domain.service.state.NationModifierService;
import com.axiom.domain.service.state.PollutionService;
import com.axiom.domain.service.politics.AllianceService;
import com.axiom.domain.service.state.CrisisResponseService;
import com.axiom.domain.service.politics.DynastyService;
import com.axiom.domain.service.politics.InfluenceService;
import com.axiom.domain.service.state.MigrationService;
import com.axiom.domain.service.politics.EmbargoService;
import com.axiom.domain.service.infrastructure.LegacyService;
import com.axiom.domain.service.politics.RitualService;
import com.axiom.domain.service.military.ConquestService;
import com.axiom.domain.service.politics.CultureService;
import com.axiom.domain.service.politics.PropagandaService;
import com.axiom.domain.service.state.DisasterService;
import com.axiom.domain.service.politics.FestivalService;
import com.axiom.domain.service.industry.SupplyChainService;
import com.axiom.domain.service.military.RevoltService;
import com.axiom.domain.service.politics.DiplomaticMissionService;
import com.axiom.domain.service.industry.CurrencyExchangeService;
import com.axiom.domain.service.military.NavalService;
import com.axiom.domain.service.state.SanitationService;
import com.axiom.domain.service.state.PrisonService;
import com.axiom.domain.service.industry.BlackMarketService;
import com.axiom.domain.service.military.ResistanceMovementService;
import com.axiom.domain.service.politics.MonumentService;
import com.axiom.domain.service.state.PandemicService;
import com.axiom.domain.service.infrastructure.InfrastructureService;
import com.axiom.domain.service.state.RefugeeService;
import com.axiom.domain.service.politics.GreatWorksService;
import com.axiom.domain.service.military.RaidService;
import com.axiom.domain.service.industry.TributeService;
import com.axiom.domain.service.state.PlagueService;
import com.axiom.domain.service.military.PartisanService;
import com.axiom.domain.service.military.ConscriptionService;
import com.axiom.domain.service.military.BlockadeService;
import com.axiom.domain.service.infrastructure.HarborService;
import com.axiom.domain.service.state.FamineService;
import com.axiom.domain.service.military.AssassinationService;
import com.axiom.domain.service.state.ColonizationService;
import com.axiom.domain.service.state.CensusService;
import com.axiom.domain.service.industry.ResourceDepletionService;
import com.axiom.domain.service.military.SiegeService;
import com.axiom.domain.service.military.FortificationService;
import com.axiom.domain.service.politics.CoupService;
import com.axiom.domain.service.industry.TradeAgreementService;
import com.axiom.domain.service.military.TerrorismService;
import com.axiom.domain.service.politics.CeasefireService;
import com.axiom.domain.service.military.MilitaryService;
import com.axiom.domain.service.military.ReligiousWarService;
import com.axiom.domain.service.military.AdvancedWarSystem;
import com.axiom.domain.service.industry.ImportExportService;
import com.axiom.domain.service.industry.EnergyService;
import com.axiom.domain.service.military.RebellionService;
import com.axiom.domain.service.politics.TreatyViolationService;
import com.axiom.domain.service.politics.SanctionService;
import com.axiom.domain.service.industry.ResourceScarcityService;
import com.axiom.domain.service.industry.TradeRouteService;
import com.axiom.domain.service.politics.DiplomaticRecognitionService;
import com.axiom.domain.service.politics.CultureShockService;
import com.axiom.domain.service.politics.BorderControlService;
import com.axiom.domain.service.industry.TaxEvasionService;
import com.axiom.domain.service.industry.MonetaryPolicyService;
import com.axiom.domain.service.technology.ResearchFundingService;
import com.axiom.domain.service.industry.CommodityMarketService;
import com.axiom.domain.service.state.ImmigrationControlService;
import com.axiom.domain.service.state.EmergencyService;
import com.axiom.domain.service.military.ArmsDealService;
import com.axiom.domain.service.politics.TreatyRenegotiationService;
import com.axiom.domain.service.industry.EconomicCrisisService;
import com.axiom.domain.service.politics.CorruptionService;
import com.axiom.domain.service.politics.DiplomaticImmunityService;
import com.axiom.domain.service.industry.ResourceDiscoveryService;
import com.axiom.domain.service.military.WarCrimeService;
import com.axiom.domain.service.politics.PeaceTreatyService;
import com.axiom.domain.service.politics.TradeEmbargoExpansionService;
import com.axiom.domain.service.politics.RaceService;
import com.axiom.domain.service.politics.RacialDiscriminationService;
import com.axiom.domain.service.industry.TradeNetworkService;
import com.axiom.domain.service.military.MilitaryExerciseService;
import com.axiom.domain.service.industry.ResourceProcessingService;
import com.axiom.domain.service.politics.PublicOpinionService;
import com.axiom.domain.service.infrastructure.InfrastructureMaintenanceService;
import com.axiom.domain.service.politics.DiplomaticProtocolService;
import com.axiom.domain.service.politics.PropagandaCampaignService;
import com.axiom.domain.service.industry.CurrencyManipulationService;
import com.axiom.domain.service.industry.ResourceStockpileService;
import com.axiom.domain.service.politics.CulturalExchangeService;
import com.axiom.domain.service.military.MilitaryIntelligenceService;
import com.axiom.domain.service.politics.EnvironmentalPolicyService;
import com.axiom.domain.service.industry.TradeDisputeService;
import com.axiom.domain.service.politics.SocialWelfareService;
import com.axiom.domain.service.military.NuclearWeaponsService;
import com.axiom.domain.service.military.ArmsControlService;
import com.axiom.domain.service.state.RefugeeResettlementService;
import com.axiom.domain.service.politics.InternationalCourtService;
import com.axiom.domain.service.politics.CulturalHeritageService;
import com.axiom.domain.service.industry.ResourceNationalizationService;
import com.axiom.domain.service.industry.CurrencyUnionService;
import com.axiom.domain.service.technology.ResearchCollaborationService;
import com.axiom.domain.service.technology.SpaceProgramService;
import com.axiom.domain.service.industry.TradeWarService;
import com.axiom.domain.service.military.MilitaryAllianceService;
import com.axiom.domain.service.industry.ResourceCartelService;
import com.axiom.domain.service.state.PopulationGrowthService;
import com.axiom.domain.service.politics.InternationalAidService;
import com.axiom.domain.service.politics.DiplomaticSummitService;
import com.axiom.domain.service.politics.CulturalRevolutionService;
import com.axiom.domain.service.infrastructure.ModResourceService;
import com.axiom.domain.service.infrastructure.ModWarfareService;
import com.axiom.domain.service.infrastructure.ModEnergyService;
import com.axiom.domain.service.infrastructure.BalancingService;
import com.axiom.domain.service.infrastructure.BackupService;
import com.axiom.domain.service.infrastructure.WebExportService;
import com.axiom.domain.service.infrastructure.TerritorySyncService;
import com.axiom.domain.service.infrastructure.TutorialService;
import com.axiom.domain.service.infrastructure.PerformanceMetricsService;
import com.axiom.domain.service.infrastructure.MultiWorldService;
import com.axiom.domain.service.infrastructure.PlayerDashboardService;
import com.axiom.domain.service.infrastructure.VisualEffectsService;
import com.axiom.domain.service.infrastructure.ModPackBuilderService;
import com.axiom.domain.service.military.CountryCaptureService;
import com.axiom.domain.service.infrastructure.MapBoundaryVisualizationService;
import com.axiom.domain.service.industry.EconomicIndicatorsService;
import com.axiom.domain.service.infrastructure.ModCompatibilityChecker;
import com.axiom.domain.service.state.TerritoryService;
import com.axiom.app.listener.ModIntegrationListener;
import com.axiom.app.listener.DashboardListener;
import com.axiom.app.listener.VisualEffectsListener;
import com.axiom.domain.service.infrastructure.RecipeIntegrationService;
import com.axiom.domain.service.infrastructure.ModIntegrationEnhancementService;
import com.axiom.domain.service.infrastructure.ModPackManagerService;
import com.axiom.util.CacheManager;
import com.axiom.app.controller.MilitaryController;
import com.axiom.domain.service.military.MilitaryServiceInterface;
import com.axiom.domain.service.industry.EconomyServiceInterface;
import com.axiom.kernel.AxiomKernel;
import com.axiom.kernel.modules.CoreStateModule;
import com.axiom.kernel.modules.PoliticsModule;
import com.axiom.kernel.modules.IndustryModule;
import com.axiom.kernel.modules.MilitaryModule;
import com.axiom.kernel.modules.TechnologyModule;
import com.axiom.kernel.modules.InfrastructureModule;
import com.axiom.api.ModIntegrationAPI;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private DiplomacyRelationService diplomacyRelationService;
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
    private DomainServices domainServices;
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
    private ResourceCatalogService resourceCatalogService;
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
    private TerritorySyncService territorySyncService;
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
    private boolean uiAutotestEnabled = false;
    private int uiAutotestDelayTicks = 200;
    private static final Gson GSON = new Gson();

    // New Architecture Components
    private AxiomKernel kernel;
    private MilitaryController militaryController;
    private final Map<Class<?>, Field> serviceFieldIndex = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        buildServiceFieldIndex();
        bootstrapKernel();

        this.nationMainMenu = null; // Будет инициализироваться при открытии меню
        this.confirmMenu = new ConfirmMenu(this);
        this.technologyMenu = new TechnologyMenu(this);
        this.corporationsMenu = new CorporationsMenu(this, getStockMarketService());
        
        // PlaceholderAPI integration (if available)
        // Note: PlaceholderAPI integration requires the plugin to be installed.
        // The expansion class will be loaded dynamically at runtime.
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                com.axiom.infra.integration.AxiomPlaceholderExpansion expansion = new com.axiom.infra.integration.AxiomPlaceholderExpansion(this);
                expansion.register();
                getLogger().info("PlaceholderAPI интеграция активирована!");
            } catch (Exception e) {
                getLogger().info("Ошибка регистрации PlaceholderAPI: " + e.getMessage());
            }
        } else {
            getLogger().info("PlaceholderAPI не найден. Интеграция пропущена.");
        }

        // Create controllers
        this.militaryController = new MilitaryController(
            this,
            kernel.services().require(MilitaryServiceInterface.class),
            kernel.services().require(EconomyServiceInterface.class),
            kernel.services().require(NationManager.class),
            kernel.services().require(CacheManager.class)
        );

        // Commands
        if (getCommand("axiom") != null) {
            // Single root dispatcher handles subcommands
            getCommand("axiom").setExecutor(new AxiomCommand(this));
        }
        if (getCommand("nation") != null) {
            getCommand("nation").setExecutor(new NationCommandAlias(this));
        }
        if (getCommand("military") != null) {
            getCommand("military").setExecutor(militaryController);
        }
        if (getCommand("testbot") != null) {
            getCommand("testbot").setExecutor(new TestBotCommand(this));
        }
        if (getCommand("claim") != null) {
            getCommand("claim").setExecutor(new ClaimCommand(this, getNationManager()));
        }
        if (getCommand("unclaim") != null) {
            getCommand("unclaim").setExecutor(new UnclaimCommand(this, getNationManager()));
        }
        if (getCommand("test") != null) {
            getCommand("test").setExecutor(new TestCommand(this));
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
        if (getCommand("mapvisual") != null) {
            getCommand("mapvisual").setExecutor(new MapBoundaryVisualizationCommand(this));
        }
        if (getCommand("modbalance") != null) {
            getCommand("modbalance").setExecutor(new ModBalanceCommand(this));
        }
        if (getCommand("modenhance") != null) {
            getCommand("modenhance").setExecutor(new ModIntegrationEnhancementCommand(this));
        }
        if (getCommand("modpackmanager") != null) {
            getCommand("modpackmanager").setExecutor(new ModPackManagerCommand(this));
        }
        if (getCommand("recipeintegration") != null) {
            getCommand("recipeintegration").setExecutor(new RecipeIntegrationCommand(this));
        }

        // Listeners
        Bukkit.getPluginManager().registerEvents(new TerritoryProtectionListener(this, getNationManager()), this);
        Bukkit.getPluginManager().registerEvents(new WarMobilizationListener(this, getNationManager(), getDiplomacySystem()), this);
        Bukkit.getPluginManager().registerEvents(new WarzoneVisualListener(this, getDiplomacySystem()), this);
        Bukkit.getPluginManager().registerEvents(new ReligionBuffListener(this, getReligionManager()), this);
        Bukkit.getPluginManager().registerEvents(new ModIntegrationListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.axiom.app.listener.LeaderActivityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DashboardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VisualEffectsListener(this, getVisualEffectsService()), this);

        // Register plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "axiom:ui");
        getServer().getMessenger().registerIncomingPluginChannel(this, "axiom:ui", new com.axiom.infra.network.ModCommunicationHandler(this));

        initUiAutotestConfig();
        if (uiAutotestEnabled) {
            Bukkit.getPluginManager().registerEvents(new UiAutotestStartListener(this), this);
        }

        // Autosave
        long autosaveTicks = getConfig().getLong("autosave.intervalSeconds", 300) * 20L;
        if (autosaveTicks > 0) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                try {
                    NationManager manager = getNationManager();
                    if (manager != null) {
                        manager.flush();
                    }
                } catch (Exception ignored) {}
            }, autosaveTicks, autosaveTicks);
        }

        maybeRunAutoTests();
        getLogger().info("AXIOM enabled with UI mod support.");
    }

    private void bootstrapKernel() {
        this.kernel = new AxiomKernel(getLogger(), this::bindService);
        kernel.registerModule(new CoreStateModule(this));
        kernel.registerModule(new PoliticsModule(this));
        kernel.registerModule(new IndustryModule(this));
        kernel.registerModule(new TechnologyModule(this));
        kernel.registerModule(new MilitaryModule(this));
        kernel.registerModule(new InfrastructureModule(this));
        kernel.start();
        bindKernelServices();
    }

    private void buildServiceFieldIndex() {
        serviceFieldIndex.clear();
        for (Field field : AXIOM.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }
            if (serviceFieldIndex.containsKey(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            serviceFieldIndex.put(field.getType(), field);
        }
    }

    private void bindService(Class<?> type, Object service) {
        Field field = serviceFieldIndex.get(type);
        if (field == null) {
            return;
        }
        try {
            field.set(this, service);
        } catch (IllegalAccessException e) {
            getLogger().warning("Failed to bind service: " + type.getName() + " -> " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void bindKernelServices() {
        if (kernel == null) {
            return;
        }
        for (Map.Entry<Class<?>, Field> entry : serviceFieldIndex.entrySet()) {
            Field field = entry.getValue();
            try {
                if (field.get(this) != null) {
                    continue;
                }
            } catch (IllegalAccessException ignored) {
            }
            kernel.services()
                .resolve((Class<Object>) entry.getKey())
                .ifPresent(service -> {
                    try {
                        field.set(this, service);
                    } catch (IllegalAccessException ignored) {
                    }
                });
        }
    }

    private <T> T resolveService(Class<T> type) {
        if (kernel == null) {
            return null;
        }
        return kernel.services().resolve(type).orElse(null);
    }

    private <T> T serviceOrField(T fallback, Class<T> type) {
        T resolved = resolveService(type);
        return resolved != null ? resolved : fallback;
    }

    private void maybeRunAutoTests() {
        String autoTest = System.getenv("AXIOM_AUTOTEST");
        if (autoTest == null || autoTest.trim().isEmpty() || "0".equals(autoTest)) {
            return;
        }

        AutotestSupport.maybeResetDataFolder(this);

        String delayRaw = System.getenv("AXIOM_AUTOTEST_DELAY_TICKS");
        int delayTicks = 200;
        if (delayRaw != null && !delayRaw.trim().isEmpty()) {
            try {
                delayTicks = Integer.parseInt(delayRaw.trim());
            } catch (NumberFormatException ignored) {
                delayTicks = 200;
            }
        }

        String shutdownRaw = System.getenv("AXIOM_AUTOTEST_SHUTDOWN");
        boolean shutdown = shutdownRaw != null && !shutdownRaw.trim().isEmpty() && !"0".equals(shutdownRaw);

        getLogger().info("AXIOM autotest enabled. Delay ticks: " + delayTicks + ", shutdown: " + shutdown);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("AXIOM autotest: starting AutoTestBot + TestBot");
            boolean autoOk = new AutoTestBot(this).runAllTests(Bukkit.getConsoleSender());
            boolean testbotOk = new TestBot(this).runAllTests(Bukkit.getConsoleSender());
            getLogger().info("AXIOM autotest: AutoTestBot=" + autoOk + ", TestBot=" + testbotOk);
            if (shutdown) {
                getLogger().info("AXIOM autotest: shutting down server");
                Bukkit.shutdown();
            }
        }, delayTicks);
    }

    private void initUiAutotestConfig() {
        uiAutotestEnabled = isEnvEnabled("AXIOM_UI_AUTOTEST") || isEnvEnabled("AXIOM_AUTOTEST");
        uiAutotestDelayTicks = readEnvInt("AXIOM_UI_AUTOTEST_DELAY_TICKS", 200);
    }

    private boolean isEnvEnabled(String name) {
        String raw = System.getenv(name);
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        return !"0".equals(raw.trim());
    }

    private int readEnvInt(String name, int fallback) {
        String raw = System.getenv(name);
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public boolean isUiAutotestEnabled() {
        return uiAutotestEnabled;
    }

    public int getUiAutotestDelayTicks() {
        return uiAutotestDelayTicks;
    }

    public void sendUiAutotestStart(Player player) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("enabled", true);
        payload.put("autoStartDelayTicks", 0);
        payload.put("stepDelayTicks", readEnvInt("AXIOM_UI_AUTOTEST_STEP_DELAY_TICKS", 5));
        payload.put("commandTimeoutTicks", readEnvInt("AXIOM_UI_AUTOTEST_COMMAND_TIMEOUT_TICKS", 200));
        payload.put("includeUiScreens", true);
        payload.put("commands", resolveUiAutotestCommands());
        payload.put("commandBlacklist", List.of());
        payload.put("actions", List.of());

        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(msgBytes);
            out.writeUTF("ui_autotest_start");
            out.writeUTF(GSON.toJson(payload));
            player.sendPluginMessage(this, "axiom:ui", msgBytes.toByteArray());
        } catch (IOException e) {
            getLogger().warning("Failed to send UI autotest start: " + e.getMessage());
        }
    }

    public void openUiMainMenu(Player player) {
        sendUiMessage(player, "open_main_menu", "");
    }

    private void sendUiMessage(Player player, String type, Object data) {
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(msgBytes);
            out.writeUTF(type);
            out.writeUTF(GSON.toJson(data));
            player.sendPluginMessage(this, "axiom:ui", msgBytes.toByteArray());
        } catch (IOException e) {
            getLogger().warning("Failed to send UI message '" + type + "': " + e.getMessage());
        }
    }

    private List<String> resolveUiAutotestCommands() {
        List<String> fallback = List.of("/test", "/testbot run all");
        String raw = System.getenv("AXIOM_UI_AUTOTEST_COMMANDS");
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = GSON.fromJson(trimmed, new TypeToken<List<String>>(){}.getType());
                if (parsed != null && !parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        String[] parts = trimmed.split(",");
        List<String> commands = new ArrayList<>();
        for (String part : parts) {
            String cmd = part.trim();
            if (!cmd.isEmpty()) {
                commands.add(cmd);
            }
        }
        return commands.isEmpty() ? fallback : commands;
    }

    @Override
    public void onDisable() {
        if (kernel != null) {
            try {
                kernel.stop();
            } catch (Exception e) {
                getLogger().warning("Kernel shutdown failed: " + e.getMessage());
            }
        }
        if (mapBoundaryService != null) {
            mapBoundaryService.shutdown();
        }
        TerritorySyncService syncService = getTerritorySyncService();
        if (syncService != null) {
            syncService.shutdown();
        }
        try {
            NationManager manager = getNationManager();
            if (manager != null) {
                manager.flush();
            }
        } catch (Exception e) {
            getLogger().severe("Failed to flush data: " + e.getMessage());
        }
        TerritoryService territoryService = getTerritoryService();
        if (territoryService != null) {
            territoryService.save();
        }
        getLogger().info("AXIOM disabled.");
    }

    /** Reloads config and propagates to systems. */
    public void reloadAxiomConfig() {
        reloadConfig();
        EconomyService service = getEconomyService();
        if (service != null) {
            service.reload();
        }
    }

    public NationManager getNationManager() { return serviceOrField(nationManager, NationManager.class); }
    public EconomyService getEconomyService() { return serviceOrField(economyService, EconomyService.class); }
    public DiplomacySystem getDiplomacySystem() { return serviceOrField(diplomacySystem, DiplomacySystem.class); }
    public DiplomacyRelationService getDiplomacyRelationService() { return serviceOrField(diplomacyRelationService, DiplomacyRelationService.class); }
    public ReligionManager getReligionManager() { return serviceOrField(religionManager, ReligionManager.class); }
    public CityGrowthEngine getCityGrowthEngine() { return serviceOrField(cityGrowthEngine, CityGrowthEngine.class); }
    public PvpService getPvpService() { return serviceOrField(pvpService, PvpService.class); }
    public PlayerDataManager getPlayerDataManager() { return serviceOrField(playerDataManager, PlayerDataManager.class); }
    public ModPackBuilderService getModPackBuilderService() { return serviceOrField(modPackBuilderService, ModPackBuilderService.class); }
    public ModCompatibilityChecker getModCompatibilityChecker() { return serviceOrField(modCompatibilityChecker, ModCompatibilityChecker.class); }
    public CountryCaptureService getCountryCaptureService() { return serviceOrField(countryCaptureService, CountryCaptureService.class); }
    public MapBoundaryVisualizationService getMapBoundaryVisualizationService() { return serviceOrField(mapBoundaryVisualizationService, MapBoundaryVisualizationService.class); }
    public EconomicIndicatorsService getEconomicIndicatorsService() { return serviceOrField(economicIndicatorsService, EconomicIndicatorsService.class); }
    public TerritoryService getTerritoryService() { return resolveService(TerritoryService.class); }
    public static AXIOM getInstance() { return instance; }
    public DomainServices domain() {
        if (domainServices == null) {
            domainServices = new DomainServices(this);
        }
        return domainServices;
    }
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
        new com.axiom.app.gui.EconomicIndicatorsMenu(this, player).open();
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
    public NewsService getNewsService() { return serviceOrField(newsService, NewsService.class); }
    public ElectionService getElectionService() { return serviceOrField(electionService, ElectionService.class); }
    public CrimeService getCrimeService() { return serviceOrField(crimeService, CrimeService.class); }
    public EducationService getEducationService() { return serviceOrField(educationService, EducationService.class); }
    public AchievementService getAchievementService() { return serviceOrField(achievementService, AchievementService.class); }
    public ResourceCatalogService getResourceCatalogService() { return serviceOrField(resourceCatalogService, ResourceCatalogService.class); }
    public ResourceService getResourceService() { return serviceOrField(resourceService, ResourceService.class); }
    public NationModifierService getNationModifierService() { return serviceOrField(nationModifierService, NationModifierService.class); }
    public NavalService getNavalService() { return serviceOrField(navalService, NavalService.class); }
    public HappinessService getHappinessService() { return serviceOrField(happinessService, HappinessService.class); }
    public MilitaryService getMilitaryService() { return serviceOrField(militaryService, MilitaryService.class); }
    public CultureService getCultureService() { return serviceOrField(cultureService, CultureService.class); }
    public RolePermissionService getRolePermissionService() { return serviceOrField(rolePermissionService, RolePermissionService.class); }
    public MigrationService getMigrationService() { return serviceOrField(migrationService, MigrationService.class); }
    public RaceService getRaceService() { return serviceOrField(raceService, RaceService.class); }
    public PublicOpinionService getPublicOpinionService() { return serviceOrField(publicOpinionService, PublicOpinionService.class); }
    public CorruptionService getCorruptionService() { return serviceOrField(corruptionService, CorruptionService.class); }
    public PollutionService getPollutionService() { return serviceOrField(pollutionService, PollutionService.class); }
    public NuclearWeaponsService getNuclearWeaponsService() { return serviceOrField(nuclearWeaponsService, NuclearWeaponsService.class); }
    public TechnologyTreeService getTechnologyTreeService() { return serviceOrField(technologyTreeService, TechnologyTreeService.class); }
    public ModIntegrationService getModIntegrationService() { return serviceOrField(modIntegrationService, ModIntegrationService.class); }
    public ModResourceService getModResourceService() { return serviceOrField(modResourceService, ModResourceService.class); }
    public ModWarfareService getModWarfareService() { return serviceOrField(modWarfareService, ModWarfareService.class); }
    public ModEnergyService getModEnergyService() { return serviceOrField(modEnergyService, ModEnergyService.class); }
    public CacheManager getCacheManager() { return resolveService(CacheManager.class); }
    public ModIntegrationAPI getModIntegrationAPI() { return serviceOrField(modIntegrationAPI, ModIntegrationAPI.class); }
    public StatisticsService getStatisticsService() { return serviceOrField(statisticsService, StatisticsService.class); }
    public BalancingService getBalancingService() { return serviceOrField(balancingService, BalancingService.class); }
    public CommodityMarketService getCommodityMarketService() { return serviceOrField(commodityMarketService, CommodityMarketService.class); }
    public ResourceProcessingService getResourceProcessingService() { return serviceOrField(resourceProcessingService, ResourceProcessingService.class); }
    public BackupService getBackupService() { return serviceOrField(backupService, BackupService.class); }
    public WebExportService getWebExportService() { return serviceOrField(webExportService, WebExportService.class); }
    public TerritorySyncService getTerritorySyncService() { return serviceOrField(territorySyncService, TerritorySyncService.class); }
    public TutorialService getTutorialService() { return serviceOrField(tutorialService, TutorialService.class); }
    public PerformanceMetricsService getPerformanceMetricsService() { return serviceOrField(performanceMetricsService, PerformanceMetricsService.class); }
    public MultiWorldService getMultiWorldService() { return serviceOrField(multiWorldService, MultiWorldService.class); }
    public PlayerDashboardService getPlayerDashboardService() { return serviceOrField(playerDashboardService, PlayerDashboardService.class); }
    public VisualEffectsService getVisualEffectsService() { return serviceOrField(visualEffectsService, VisualEffectsService.class); }
    public TreatyService getTreatyService() { return serviceOrField(treatyService, TreatyService.class); }
    public TradeService getTradeService() { return serviceOrField(tradeService, TradeService.class); }
    public TradingPostService getTradingPostService() { return serviceOrField(tradingPostService, TradingPostService.class); }
    public AdvancedWarSystem getAdvancedWarSystem() { return serviceOrField(advancedWarSystem, AdvancedWarSystem.class); }
    public MapBoundaryService getMapBoundaryService() { return serviceOrField(mapBoundaryService, MapBoundaryService.class); }
    public BankingService getBankingService() { return serviceOrField(bankingService, BankingService.class); }
    public StockMarketService getStockMarketService() { return serviceOrField(stockMarketService, StockMarketService.class); }
    public DiplomaticImmunityService getDiplomaticImmunityService() { return serviceOrField(diplomaticImmunityService, DiplomaticImmunityService.class); }
    
    // Additional getters for SuperTestBot
    public RaidService getRaidService() { return serviceOrField(raidService, RaidService.class); }
    public SiegeService getSiegeService() { return serviceOrField(siegeService, SiegeService.class); }
    public ConquestService getConquestService() { return serviceOrField(conquestService, ConquestService.class); }
    public MobilizationService getMobilizationService() { return serviceOrField(mobilizationService, MobilizationService.class); }
    public CurrencyUnionService getCurrencyUnionService() { return serviceOrField(currencyUnionService, CurrencyUnionService.class); }
    public ResourceCartelService getResourceCartelService() { return serviceOrField(resourceCartelService, ResourceCartelService.class); }
    public MilitaryAllianceService getMilitaryAllianceService() { return serviceOrField(militaryAllianceService, MilitaryAllianceService.class); }
    public RacialDiscriminationService getRacialDiscriminationService() { return serviceOrField(racialDiscriminationService, RacialDiscriminationService.class); }
    public CulturalExchangeService getCulturalExchangeService() { return serviceOrField(culturalExchangeService, CulturalExchangeService.class); }
    public CulturalHeritageService getCulturalHeritageService() { return serviceOrField(culturalHeritageService, CulturalHeritageService.class); }
    public PropagandaService getPropagandaService() { return serviceOrField(propagandaService, PropagandaService.class); }
    public SanctionService getSanctionService() { return serviceOrField(sanctionService, SanctionService.class); }
    public EmbargoService getEmbargoService() { return serviceOrField(embargoService, EmbargoService.class); }
    public DiplomaticRecognitionService getDiplomaticRecognitionService() { return serviceOrField(diplomaticRecognitionService, DiplomaticRecognitionService.class); }
    public CurrencyExchangeService getCurrencyExchangeService() { return serviceOrField(currencyExchangeService, CurrencyExchangeService.class); }
    public HarborService getHarborService() { return serviceOrField(harborService, HarborService.class); }
    public MonumentService getMonumentService() { return serviceOrField(monumentService, MonumentService.class); }
    public UnifiedEspionageService getUnifiedEspionageService() { return serviceOrField(unifiedEspionageService, UnifiedEspionageService.class); }
    public DisasterService getDisasterService() { return serviceOrField(disasterService, DisasterService.class); }
    public InfrastructureService getInfrastructureService() { return serviceOrField(infrastructureService, InfrastructureService.class); }
    public InfluenceService getInfluenceService() { return serviceOrField(influenceService, InfluenceService.class); }
    public TradeNetworkService getTradeNetworkService() { return serviceOrField(tradeNetworkService, TradeNetworkService.class); }
    public ModBalancerService getModBalancerService() { return serviceOrField(modBalancerService, ModBalancerService.class); }
    public PropagandaCampaignService getPropagandaCampaignService() { return serviceOrField(propagandaCampaignService, PropagandaCampaignService.class); }
    public RecipeIntegrationService getRecipeIntegrationService() { return serviceOrField(recipeIntegrationService, RecipeIntegrationService.class); }
    public ModIntegrationEnhancementService getModIntegrationEnhancementService() { return serviceOrField(modIntegrationEnhancementService, ModIntegrationEnhancementService.class); }
    public ModPackManagerService getModPackManagerService() { return serviceOrField(modPackManagerService, ModPackManagerService.class); }
}
