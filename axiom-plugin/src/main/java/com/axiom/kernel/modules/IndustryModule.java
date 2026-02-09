package com.axiom.kernel.modules;

import com.axiom.AXIOM;
import com.axiom.kernel.KernelModule;
import com.axiom.kernel.ModuleIds;
import com.axiom.kernel.ServiceRegistry;
import com.axiom.domain.service.industry.*;
import com.axiom.domain.service.state.NationManager;
import com.axiom.domain.service.state.PlayerDataManager;

import java.util.Set;

public class IndustryModule implements KernelModule {
    private final AXIOM plugin;

    public IndustryModule(AXIOM plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return ModuleIds.INDUSTRY;
    }

    @Override
    public Set<String> dependencies() {
        return Set.of(ModuleIds.STATE);
    }

    @Override
    public void register(ServiceRegistry services) {
        NationManager nationManager = services.require(NationManager.class);
        PlayerDataManager playerDataManager = services.require(PlayerDataManager.class);

        ResourceCatalogService resourceCatalogService = new ResourceCatalogService(plugin);
        services.register(ResourceCatalogService.class, resourceCatalogService);

        EconomyService economyService = new EconomyService(plugin, nationManager);
        services.register(EconomyService.class, economyService);

        WalletService walletService = new WalletService(plugin, playerDataManager);
        services.register(WalletService.class, walletService);

        BankingService bankingService = new BankingService(plugin, nationManager);
        services.register(BankingService.class, bankingService);

        StockMarketService stockMarketService = new StockMarketService(plugin);
        services.register(StockMarketService.class, stockMarketService);

        TradeService tradeService = new TradeService(plugin, nationManager);
        services.register(TradeService.class, tradeService);

        TradingPostService tradingPostService = new TradingPostService(plugin);
        services.register(TradingPostService.class, tradingPostService);

        ResourceService resourceService = new ResourceService(plugin);
        services.register(ResourceService.class, resourceService);

        SupplyChainService supplyChainService = new SupplyChainService(plugin);
        services.register(SupplyChainService.class, supplyChainService);

        ResourceProcessingService resourceProcessingService = new ResourceProcessingService(plugin);
        services.register(ResourceProcessingService.class, resourceProcessingService);

        ResourceStockpileService resourceStockpileService = new ResourceStockpileService(plugin, nationManager);
        services.register(ResourceStockpileService.class, resourceStockpileService);

        ResourceCartelService resourceCartelService = new ResourceCartelService(plugin, nationManager);
        services.register(ResourceCartelService.class, resourceCartelService);

        ResourceNationalizationService resourceNationalizationService = new ResourceNationalizationService(plugin);
        services.register(ResourceNationalizationService.class, resourceNationalizationService);

        ResourceDiscoveryService resourceDiscoveryService = new ResourceDiscoveryService(plugin, nationManager);
        services.register(ResourceDiscoveryService.class, resourceDiscoveryService);

        ResourceDepletionService resourceDepletionService = new ResourceDepletionService(plugin);
        services.register(ResourceDepletionService.class, resourceDepletionService);

        ResourceScarcityService resourceScarcityService = new ResourceScarcityService(plugin);
        services.register(ResourceScarcityService.class, resourceScarcityService);

        CommodityMarketService commodityMarketService = new CommodityMarketService(plugin);
        services.register(CommodityMarketService.class, commodityMarketService);

        CurrencyExchangeService currencyExchangeService = new CurrencyExchangeService(plugin, nationManager, economyService);
        services.register(CurrencyExchangeService.class, currencyExchangeService);

        CurrencyManipulationService currencyManipulationService = new CurrencyManipulationService(plugin);
        services.register(CurrencyManipulationService.class, currencyManipulationService);

        MonetaryPolicyService monetaryPolicyService = new MonetaryPolicyService(plugin);
        services.register(MonetaryPolicyService.class, monetaryPolicyService);

        TaxEvasionService taxEvasionService = new TaxEvasionService(plugin);
        services.register(TaxEvasionService.class, taxEvasionService);

        EconomicCrisisService economicCrisisService = new EconomicCrisisService(plugin, nationManager);
        services.register(EconomicCrisisService.class, economicCrisisService);

        TradeNetworkService tradeNetworkService = new TradeNetworkService(plugin, nationManager);
        services.register(TradeNetworkService.class, tradeNetworkService);

        TradeRouteService tradeRouteService = new TradeRouteService(plugin);
        services.register(TradeRouteService.class, tradeRouteService);

        TradeDisputeService tradeDisputeService = new TradeDisputeService(plugin, nationManager);
        services.register(TradeDisputeService.class, tradeDisputeService);

        TradeWarService tradeWarService = new TradeWarService(plugin, nationManager);
        services.register(TradeWarService.class, tradeWarService);

        TradeAgreementService tradeAgreementService = new TradeAgreementService(plugin);
        services.register(TradeAgreementService.class, tradeAgreementService);

        ImportExportService importExportService = new ImportExportService(plugin);
        services.register(ImportExportService.class, importExportService);

        EnergyService energyService = new EnergyService(plugin);
        services.register(EnergyService.class, energyService);

        CurrencyUnionService currencyUnionService = new CurrencyUnionService(plugin, nationManager);
        services.register(CurrencyUnionService.class, currencyUnionService);

        BlackMarketService blackMarketService = new BlackMarketService(plugin);
        services.register(BlackMarketService.class, blackMarketService);

        TributeService tributeService = new TributeService(plugin, nationManager);
        services.register(TributeService.class, tributeService);
    }
}
