package com.axiom.domain;

import com.axiom.AXIOM;
import com.axiom.domain.service.industry.EconomyService;
import com.axiom.domain.service.industry.ResourceCatalogService;
import com.axiom.domain.service.industry.ResourceService;
import com.axiom.domain.service.industry.TradeService;
import com.axiom.domain.service.industry.WalletService;
import com.axiom.domain.service.infrastructure.MapBoundaryService;
import com.axiom.domain.service.infrastructure.ModIntegrationService;
import com.axiom.domain.service.infrastructure.PlayerDashboardService;
import com.axiom.domain.service.infrastructure.TerritorySyncService;
import com.axiom.domain.service.infrastructure.WebExportService;
import com.axiom.domain.service.military.AdvancedWarSystem;
import com.axiom.domain.service.military.ConquestService;
import com.axiom.domain.service.military.MilitaryService;
import com.axiom.domain.service.politics.DiplomacyRelationService;
import com.axiom.domain.service.politics.DiplomacySystem;
import com.axiom.domain.service.politics.TreatyService;
import com.axiom.domain.service.state.CityGrowthEngine;
import com.axiom.domain.service.state.EducationService;
import com.axiom.domain.service.state.HappinessService;
import com.axiom.domain.service.state.NationManager;
import com.axiom.domain.service.state.PlayerDataManager;
import com.axiom.domain.service.state.TerritoryService;
import com.axiom.domain.service.technology.TechnologyTreeService;
import com.axiom.util.CacheManager;

/**
 * Minimal public facade for core domain services.
 *
 * Use this as a stable index instead of importing deep package trees.
 */
public final class DomainServices {
    private final AXIOM plugin;
    private final State state;
    private final Politics politics;
    private final Industry industry;
    private final Technology technology;
    private final Military military;
    private final Infrastructure infrastructure;

    public DomainServices(AXIOM plugin) {
        this.plugin = plugin;
        this.state = new State(plugin);
        this.politics = new Politics(plugin);
        this.industry = new Industry(plugin);
        this.technology = new Technology(plugin);
        this.military = new Military(plugin);
        this.infrastructure = new Infrastructure(plugin);
    }

    public State state() {
        return state;
    }

    public Politics politics() {
        return politics;
    }

    public Industry industry() {
        return industry;
    }

    public Technology technology() {
        return technology;
    }

    public Military military() {
        return military;
    }

    public Infrastructure infrastructure() {
        return infrastructure;
    }

    public static final class State {
        private final AXIOM plugin;

        private State(AXIOM plugin) {
            this.plugin = plugin;
        }

        public NationManager nationManager() {
            return require(plugin.getNationManager(), "NationManager");
        }

        public PlayerDataManager playerDataManager() {
            return require(plugin.getPlayerDataManager(), "PlayerDataManager");
        }

        public TerritoryService territoryService() {
            return require(plugin.getTerritoryService(), "TerritoryService");
        }

        public CityGrowthEngine cityGrowthEngine() {
            return require(plugin.getCityGrowthEngine(), "CityGrowthEngine");
        }

        public EducationService educationService() {
            return require(plugin.getEducationService(), "EducationService");
        }

        public HappinessService happinessService() {
            return require(plugin.getHappinessService(), "HappinessService");
        }
    }

    public static final class Politics {
        private final AXIOM plugin;

        private Politics(AXIOM plugin) {
            this.plugin = plugin;
        }

        public DiplomacyRelationService diplomacyRelations() {
            return require(plugin.getDiplomacyRelationService(), "DiplomacyRelationService");
        }

        public DiplomacySystem diplomacySystem() {
            return require(plugin.getDiplomacySystem(), "DiplomacySystem");
        }

        public TreatyService treatyService() {
            return require(plugin.getTreatyService(), "TreatyService");
        }
    }

    public static final class Industry {
        private final AXIOM plugin;

        private Industry(AXIOM plugin) {
            this.plugin = plugin;
        }

        public EconomyService economy() {
            return require(plugin.getEconomyService(), "EconomyService");
        }

        public ResourceCatalogService resourceCatalog() {
            return require(plugin.getResourceCatalogService(), "ResourceCatalogService");
        }

        public ResourceService resources() {
            return require(plugin.getResourceService(), "ResourceService");
        }

        public TradeService trade() {
            return require(plugin.getTradeService(), "TradeService");
        }

        public WalletService wallet() {
            return require(plugin.getWalletService(), "WalletService");
        }
    }

    public static final class Technology {
        private final AXIOM plugin;

        private Technology(AXIOM plugin) {
            this.plugin = plugin;
        }

        public TechnologyTreeService techTree() {
            return require(plugin.getTechnologyTreeService(), "TechnologyTreeService");
        }
    }

    public static final class Military {
        private final AXIOM plugin;

        private Military(AXIOM plugin) {
            this.plugin = plugin;
        }

        public AdvancedWarSystem warSystem() {
            return require(plugin.getAdvancedWarSystem(), "AdvancedWarSystem");
        }

        public MilitaryService militaryService() {
            return require(plugin.getMilitaryService(), "MilitaryService");
        }

        public ConquestService conquestService() {
            return require(plugin.getConquestService(), "ConquestService");
        }
    }

    public static final class Infrastructure {
        private final AXIOM plugin;

        private Infrastructure(AXIOM plugin) {
            this.plugin = plugin;
        }

        public ModIntegrationService modIntegration() {
            return require(plugin.getModIntegrationService(), "ModIntegrationService");
        }

        public MapBoundaryService mapBoundaries() {
            return require(plugin.getMapBoundaryService(), "MapBoundaryService");
        }

        public WebExportService webExport() {
            return require(plugin.getWebExportService(), "WebExportService");
        }

        public TerritorySyncService territorySync() {
            return require(plugin.getTerritorySyncService(), "TerritorySyncService");
        }

        public PlayerDashboardService playerDashboard() {
            return require(plugin.getPlayerDashboardService(), "PlayerDashboardService");
        }

        public CacheManager cache() {
            return require(plugin.getCacheManager(), "CacheManager");
        }
    }

    private static <T> T require(T service, String name) {
        if (service == null) {
            throw new IllegalStateException("Service not available: " + name);
        }
        return service;
    }
}
