package com.axiom.kernel.modules;

import com.axiom.AXIOM;
import com.axiom.kernel.KernelModule;
import com.axiom.kernel.ModuleIds;
import com.axiom.kernel.ServiceRegistry;
import com.axiom.domain.service.state.NationManager;
import com.axiom.domain.service.technology.ResearchCollaborationService;
import com.axiom.domain.service.technology.ResearchFundingService;
import com.axiom.domain.service.technology.SpaceProgramService;
import com.axiom.domain.service.technology.TechnologyTreeService;

import java.util.Set;

public class TechnologyModule implements KernelModule {
    private final AXIOM plugin;

    public TechnologyModule(AXIOM plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return ModuleIds.TECHNOLOGY;
    }

    @Override
    public Set<String> dependencies() {
        return Set.of(ModuleIds.STATE, ModuleIds.INDUSTRY);
    }

    @Override
    public void register(ServiceRegistry services) {
        NationManager nationManager = services.require(NationManager.class);

        TechnologyTreeService technologyTreeService = new TechnologyTreeService(plugin);
        services.register(TechnologyTreeService.class, technologyTreeService);

        ResearchFundingService researchFundingService = new ResearchFundingService(plugin);
        services.register(ResearchFundingService.class, researchFundingService);

        ResearchCollaborationService researchCollaborationService = new ResearchCollaborationService(plugin, nationManager);
        services.register(ResearchCollaborationService.class, researchCollaborationService);

        SpaceProgramService spaceProgramService = new SpaceProgramService(plugin, nationManager);
        services.register(SpaceProgramService.class, spaceProgramService);
    }
}
