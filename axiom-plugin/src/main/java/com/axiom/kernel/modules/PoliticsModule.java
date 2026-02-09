package com.axiom.kernel.modules;

import com.axiom.AXIOM;
import com.axiom.kernel.KernelModule;
import com.axiom.kernel.ModuleIds;
import com.axiom.kernel.ServiceRegistry;
import com.axiom.domain.service.politics.*;
import com.axiom.domain.service.state.NationManager;

import java.util.Set;

public class PoliticsModule implements KernelModule {
    private final AXIOM plugin;

    public PoliticsModule(AXIOM plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return ModuleIds.POLITICS;
    }

    @Override
    public Set<String> dependencies() {
        return Set.of(ModuleIds.STATE);
    }

    @Override
    public void register(ServiceRegistry services) {
        NationManager nationManager = services.require(NationManager.class);

        ReligionManager religionManager = new ReligionManager(plugin);
        services.register(ReligionManager.class, religionManager);

        HolidayService holidayService = new HolidayService(plugin, religionManager);
        services.register(HolidayService.class, holidayService);

        DynastyService dynastyService = new DynastyService(plugin);
        services.register(DynastyService.class, dynastyService);

        RitualService ritualService = new RitualService(plugin);
        services.register(RitualService.class, ritualService);

        CultureService cultureService = new CultureService(plugin);
        services.register(CultureService.class, cultureService);

        FestivalService festivalService = new FestivalService(plugin);
        services.register(FestivalService.class, festivalService);

        MonumentService monumentService = new MonumentService(plugin);
        services.register(MonumentService.class, monumentService);

        GreatWorksService greatWorksService = new GreatWorksService(plugin);
        services.register(GreatWorksService.class, greatWorksService);

        RaceService raceService = new RaceService(plugin);
        services.register(RaceService.class, raceService);

        RacialDiscriminationService racialDiscriminationService = new RacialDiscriminationService(plugin, nationManager);
        services.register(RacialDiscriminationService.class, racialDiscriminationService);

        CulturalExchangeService culturalExchangeService = new CulturalExchangeService(plugin, nationManager);
        services.register(CulturalExchangeService.class, culturalExchangeService);

        CulturalHeritageService culturalHeritageService = new CulturalHeritageService(plugin, nationManager);
        services.register(CulturalHeritageService.class, culturalHeritageService);

        CulturalRevolutionService culturalRevolutionService = new CulturalRevolutionService(plugin, nationManager);
        services.register(CulturalRevolutionService.class, culturalRevolutionService);

        SocialWelfareService socialWelfareService = new SocialWelfareService(plugin, nationManager);
        services.register(SocialWelfareService.class, socialWelfareService);

        EnvironmentalPolicyService environmentalPolicyService = new EnvironmentalPolicyService(plugin, nationManager);
        services.register(EnvironmentalPolicyService.class, environmentalPolicyService);

        CorruptionService corruptionService = new CorruptionService(plugin, nationManager);
        services.register(CorruptionService.class, corruptionService);

        TradeEmbargoExpansionService tradeEmbargoExpansionService = new TradeEmbargoExpansionService(plugin);
        services.register(TradeEmbargoExpansionService.class, tradeEmbargoExpansionService);

        DiplomacyRelationService diplomacyRelationService = new DiplomacyRelationService(plugin, nationManager);
        services.register(DiplomacyRelationService.class, diplomacyRelationService);

        DiplomacySystem diplomacySystem = new DiplomacySystem(plugin, nationManager, diplomacyRelationService);
        services.register(DiplomacySystem.class, diplomacySystem);

        TreatyService treatyService = new TreatyService(plugin, nationManager);
        services.register(TreatyService.class, treatyService);

        ElectionService electionService = new ElectionService(plugin, nationManager);
        services.register(ElectionService.class, electionService);

        AllianceService allianceService = new AllianceService(plugin, diplomacySystem);
        services.register(AllianceService.class, allianceService);

        VassalService vassalService = new VassalService(plugin, nationManager);
        services.register(VassalService.class, vassalService);

        InfluenceService influenceService = new InfluenceService(plugin);
        services.register(InfluenceService.class, influenceService);

        EmbargoService embargoService = new EmbargoService(plugin);
        services.register(EmbargoService.class, embargoService);

        SanctionService sanctionService = new SanctionService(plugin, nationManager);
        services.register(SanctionService.class, sanctionService);

        DiplomaticRecognitionService diplomaticRecognitionService = new DiplomaticRecognitionService(plugin, nationManager);
        services.register(DiplomaticRecognitionService.class, diplomaticRecognitionService);

        DiplomaticMissionService diplomaticMissionService = new DiplomaticMissionService(plugin);
        services.register(DiplomaticMissionService.class, diplomaticMissionService);

        DiplomaticImmunityService diplomaticImmunityService = new DiplomaticImmunityService(plugin, nationManager);
        services.register(DiplomaticImmunityService.class, diplomaticImmunityService);

        DiplomaticProtocolService diplomaticProtocolService = new DiplomaticProtocolService(plugin);
        services.register(DiplomaticProtocolService.class, diplomaticProtocolService);

        TreatyViolationService treatyViolationService = new TreatyViolationService(plugin, nationManager);
        services.register(TreatyViolationService.class, treatyViolationService);

        TreatyRenegotiationService treatyRenegotiationService = new TreatyRenegotiationService(plugin, nationManager);
        services.register(TreatyRenegotiationService.class, treatyRenegotiationService);

        CeasefireService ceasefireService = new CeasefireService(plugin, nationManager);
        services.register(CeasefireService.class, ceasefireService);

        PeaceTreatyService peaceTreatyService = new PeaceTreatyService(plugin, nationManager);
        services.register(PeaceTreatyService.class, peaceTreatyService);

        InternationalCourtService internationalCourtService = new InternationalCourtService(plugin, nationManager);
        services.register(InternationalCourtService.class, internationalCourtService);

        InternationalAidService internationalAidService = new InternationalAidService(plugin, nationManager);
        services.register(InternationalAidService.class, internationalAidService);

        DiplomaticSummitService diplomaticSummitService = new DiplomaticSummitService(plugin, nationManager);
        services.register(DiplomaticSummitService.class, diplomaticSummitService);

        PropagandaService propagandaService = new PropagandaService(plugin, nationManager);
        services.register(PropagandaService.class, propagandaService);

        PropagandaCampaignService propagandaCampaignService = new PropagandaCampaignService(plugin, nationManager);
        services.register(PropagandaCampaignService.class, propagandaCampaignService);

        PublicOpinionService publicOpinionService = new PublicOpinionService(plugin, nationManager);
        services.register(PublicOpinionService.class, publicOpinionService);

        BorderControlService borderControlService = new BorderControlService(plugin);
        services.register(BorderControlService.class, borderControlService);

        CultureShockService cultureShockService = new CultureShockService(plugin);
        services.register(CultureShockService.class, cultureShockService);

        CoupService coupService = new CoupService(plugin, nationManager);
        services.register(CoupService.class, coupService);
    }
}
