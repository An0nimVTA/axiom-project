package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages interconnections between all AXIOM services to ensure balanced gameplay.
 * This service creates feedback loops and dependencies between different systems.
 */
public class ServiceInterconnectionService {
    private final AXIOM plugin;
    
    // Track inter-service dependencies and feedback
    private final Map<String, Map<String, Double>> serviceDependencies = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> serviceFeedback = new ConcurrentHashMap<>();
    
    // Nation-specific interconnection data
    private final Map<String, InterconnectionData> nationInterconnections = new ConcurrentHashMap<>();
    
    // Track resource flows between systems
    private final Map<String, ResourceFlow> resourceFlows = new ConcurrentHashMap<>();
    
    public static class InterconnectionData {
        double energyToEconomyFactor = 1.0;      // How energy affects economy
        double resourcesToTechnologyFactor = 1.0; // How resources affect technology
        double happinessToProductionFactor = 1.0; // How happiness affects production
        double militaryToInfrastructureFactor = 1.0; // How military affects infrastructure
        double educationToInnovationFactor = 1.0; // How education affects innovation
        double populationToConsumptionFactor = 1.0; // How population affects resource consumption
        double tradeToGrowthFactor = 1.0; // How trade affects nation growth
        double diplomacyToStabilityFactor = 1.0; // How diplomacy affects stability
        long lastUpdated = System.currentTimeMillis();
    }
    
    public static class ResourceFlow {
        double energyToProduction = 0.0;     // Energy flowing to production systems
        double resourcesToTechnology = 0.0;  // Resources flowing to technology
        double economyToInfrastructure = 0.0; // Economy flowing to infrastructure
        double militaryToResearch = 0.0;     // Military spending to research
        double educationToEconomy = 0.0;     // Education investment to economy
        double tradeToResources = 0.0;       // Trade affecting resource availability
        long lastUpdated = System.currentTimeMillis();
    }

    public ServiceInterconnectionService(AXIOM plugin) {
        this.plugin = plugin;
        initializeDependencies();
        initializeFeedbackLoops();
        
        // Run interconnection updates every 5 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateInterconnections, 0, 20 * 60 * 5);
    }

    /**
     * Initialize service dependencies - which services affect others.
     */
    private void initializeDependencies() {
        // Energy affects Economy
        addDependency("energy", "economy", 0.15); // 15% of energy affects economy
        
        // Resources affect Technology
        addDependency("resources", "technology", 0.10); // 10% of resources affect research
        
        // Happiness affects Production
        addDependency("happiness", "production", 0.20); // 20% of happiness affects production
        
        // Military affects Infrastructure
        addDependency("military", "infrastructure", 0.05); // 5% of military spending affects infrastructure
        
        // Education affects Innovation
        addDependency("education", "innovation", 0.25); // 25% of education affects innovation
        
        // Population affects Consumption
        addDependency("population", "consumption", 0.30); // 30% of population affects consumption
        
        // Trade affects Growth
        addDependency("trade", "growth", 0.12); // 12% of trade affects growth
        
        // Diplomacy affects Stability
        addDependency("diplomacy", "stability", 0.18); // 18% of diplomacy affects stability
    }

    /**
     * Initialize feedback loops - how services affect each other in cycles.
     */
    private void initializeFeedbackLoops() {
        // Economy -> Resources -> Technology -> Economy (positive feedback)
        addFeedback("economy", "resources", 0.08);
        addFeedback("resources", "technology", 0.12);
        addFeedback("technology", "economy", 0.10);
        
        // Military -> Infrastructure -> Production -> Military (positive feedback)
        addFeedback("military", "infrastructure", 0.05);
        addFeedback("infrastructure", "production", 0.15);
        addFeedback("production", "military", 0.07);
        
        // Education -> Innovation -> Economy -> Education (positive feedback)
        addFeedback("education", "innovation", 0.20);
        addFeedback("innovation", "economy", 0.15);
        addFeedback("economy", "education", 0.08);
    }

    /**
     * Add a dependency between services.
     */
    private void addDependency(String fromService, String toService, double factor) {
        serviceDependencies.computeIfAbsent(fromService, k -> new HashMap<>())
            .put(toService, factor);
    }

    /**
     * Add a feedback loop between services.
     */
    private void addFeedback(String fromService, String toService, double factor) {
        serviceFeedback.computeIfAbsent(fromService, k -> new HashMap<>())
            .put(toService, factor);
    }

    /**
     * Update all interconnections for all nations.
     */
    private void updateInterconnections() {
        for (Nation nation : plugin.getNationManager().getAll()) {
            updateNationInterconnections(nation.getId());
        }
    }

    /**
     * Update interconnections for a specific nation.
     */
    public synchronized void updateNationInterconnections(String nationId) {
        InterconnectionData data = nationInterconnections.computeIfAbsent(nationId, k -> new InterconnectionData());
        ResourceFlow flow = resourceFlows.computeIfAbsent(nationId, k -> new ResourceFlow());
        
        // Calculate time-based updates
        long now = System.currentTimeMillis();
        long timeDiff = now - data.lastUpdated;
        double timeInHours = timeDiff / (1000.0 * 60 * 60);
        data.lastUpdated = now;
        flow.lastUpdated = now;
        
        // Update interconnection factors based on current nation state
        updateInterconnectionFactors(nationId, data);
        
        // Apply resource flows and dependencies
        applyResourceFlows(nationId, flow, data, timeInHours);
        
        // Apply feedback loops
        applyFeedbackLoops(nationId, data, timeInHours);
    }

    /**
     * Update interconnection factors based on the nation's current state.
     */
    private void updateInterconnectionFactors(String nationId, InterconnectionData data) {
        // Energy to Economy factor: based on energy availability and infrastructure
        double energyLevel = plugin.getUnifiedEnergyService().getCurrentEnergy(nationId) / 
                            Math.max(1, plugin.getUnifiedEnergyService().getMaxEnergy(nationId));
        double infraLevel = plugin.getInfrastructureService().getInfrastructureLevel(nationId, "utilities") / 100.0;
        data.energyToEconomyFactor = 0.8 + (energyLevel * 0.4) + (infraLevel * 0.3);
        
        // Resources to Technology factor: based on resource availability and education
        double resourceLevel = plugin.getResourceService().getResource(nationId, "iron") / 1000.0; // Normalize
        double educationLevel = plugin.getEducationService().getEducationLevel(nationId) / 100.0;
        data.resourcesToTechnologyFactor = 0.7 + (Math.min(resourceLevel, 1.0) * 0.3) + (educationLevel * 0.2);
        
        // Happiness to Production factor: based on happiness and infrastructure
        double happinessLevel = plugin.getHappinessService().getNationHappiness(nationId) / 100.0;
        data.happinessToProductionFactor = 0.6 + (happinessLevel * 0.4);
        
        // Military to Infrastructure factor: based on military spending and infrastructure
        double militaryStrength = plugin.getMilitaryService().getMilitaryStrength(nationId);
        data.militaryToInfrastructureFactor = 0.5 + (Math.min(militaryStrength / 10000.0, 0.5)); // Cap at 0.5
        
        // Education to Innovation factor: based on education and technology
        double techLevel = (Double) plugin.getTechnologyTreeService().getTechnologyStatistics(nationId).getOrDefault("progressPercentage", 0.0);
        data.educationToInnovationFactor = 0.7 + (educationLevel * 0.2) + (techLevel / 100.0 * 0.1);
        
        // Population to Consumption factor: based on city population and infrastructure
        int totalPopulation = plugin.getCityGrowthEngine().getTotalPopulation(nationId);
        data.populationToConsumptionFactor = 0.9 + (Math.min(totalPopulation / 10000.0, 0.3)); // Cap at 0.3
        
        // Trade to Growth factor: based on trade activity and diplomatic relations
        Map<String, Object> tradeStats = plugin.getTradeService().getTradeStatistics(nationId);
        double tradeActivity = (Double) tradeStats.getOrDefault("totalTradeVolume", 0.0);
        data.tradeToGrowthFactor = 0.8 + (Math.min(tradeActivity / 10000.0, 0.4)); // Cap at 0.4
        
        // Diplomacy to Stability factor: based on diplomatic relations and peace
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation != null) {
            double peaceFactor = nation.getEnemies().isEmpty() ? 1.0 : 0.5;
            data.diplomacyToStabilityFactor = 0.7 + (peaceFactor * 0.3);
        }
    }

    /**
     * Apply resource flows between different systems.
     */
    private void applyResourceFlows(String nationId, ResourceFlow flow, InterconnectionData data, double timeInHours) {
        // Energy to Production flow: energy powers production
        double energyAvailable = plugin.getUnifiedEnergyService().getCurrentEnergy(nationId);
        flow.energyToProduction = Math.min(energyAvailable * 0.1, 1000) * data.energyToEconomyFactor * timeInHours;
        
        // Resources to Technology flow: resources fund research
        double totalResources = plugin.getResourceService().calculateResourceValue(nationId);
        flow.resourcesToTechnology = Math.min(totalResources * 0.05, 500) * data.resourcesToTechnologyFactor * timeInHours;
        
        // Economy to Infrastructure flow: economy funds infrastructure
        double treasury = plugin.getNationManager().getNationById(nationId).getTreasury();
        flow.economyToInfrastructure = Math.min(treasury * 0.02, 1000) * timeInHours;
        
        // Military to Research flow: military spending funds advanced research
        double militarySpending = plugin.getMilitaryService().getMilitaryStrength(nationId) * 0.01;
        flow.militaryToResearch = Math.min(militarySpending, 500) * timeInHours;
        
        // Education to Economy flow: education investment improves economy
        double educationInvestment = plugin.getEducationService().getEducationLevel(nationId) * 10;
        flow.educationToEconomy = educationInvestment * data.educationToInnovationFactor * timeInHours;
        
        // Trade to Resources flow: trade activity brings in resources
        Map<String, Object> tradeStats = plugin.getTradeService().getTradeStatistics(nationId);
        double tradeVolume = (Double) tradeStats.getOrDefault("totalTradeVolume", 0.0);
        flow.tradeToResources = tradeVolume * 0.05 * timeInHours;
        
        // Apply the flows to the respective services
        applyFlowEffects(nationId, flow);
    }

    /**
     * Apply flow effects to the respective services.
     */
    private void applyFlowEffects(String nationId, ResourceFlow flow) {
        // Apply energy to production (affects manufacturing/output)
        if (flow.energyToProduction > 0) {
            // This could affect industrial production rates
            plugin.getTechnologyTreeService().addResearchPoints(nationId, "production", flow.energyToProduction * 0.01);
        }
        
        // Apply resources to technology (affects research speed)
        if (flow.resourcesToTechnology > 0) {
            // This could speed up technology research
            plugin.getTechnologyTreeService().addResearchPoints(nationId, "science", flow.resourcesToTechnology * 0.02);
        }
        
        // Apply economy to infrastructure (affects infrastructure development)
        if (flow.economyToInfrastructure > 0) {
            // This could fund infrastructure projects
        }
        
        // Apply military to research (affects military technology)
        if (flow.militaryToResearch > 0) {
            // This could fund military research
            plugin.getTechnologyTreeService().addResearchPoints(nationId, "military", flow.militaryToResearch * 0.015);
        }
        
        // Apply education to economy (affects economic growth)
        if (flow.educationToEconomy > 0) {
            // This could improve economic efficiency
        }
        
        // Apply trade to resources (affects resource availability)
        if (flow.tradeToResources > 0) {
            // This could improve resource acquisition
            plugin.getResourceService().addResource(nationId, "materials", flow.tradeToResources * 0.1); // Generic materials
        }
    }

    /**
     * Apply feedback loops between services.
     */
    private void applyFeedbackLoops(String nationId, InterconnectionData data, double timeInHours) {
        // Economy -> Resources -> Technology -> Economy loop
        double economyBoost = plugin.getEconomyService().getEconomicHealth(nationId) / 100.0;
        if (economyBoost > 0.5) {
            // When economy is healthy, more resources can be acquired
            double resourceBoost = economyBoost * serviceFeedback.getOrDefault("economy", new HashMap<>()).getOrDefault("resources", 0.0);
            plugin.getResourceService().addResource(nationId, "materials", resourceBoost * 50 * timeInHours);
            
            // When more resources are acquired, technology research speeds up
            double techBoost = resourceBoost * serviceFeedback.getOrDefault("resources", new HashMap<>()).getOrDefault("technology", 0.0);
            plugin.getTechnologyTreeService().addResearchPoints(nationId, "science", techBoost * 100 * timeInHours);
            
            // When technology advances, economy improves further
            double economyFeedback = techBoost * serviceFeedback.getOrDefault("technology", new HashMap<>()).getOrDefault("economy", 0.0);
            // Apply to nation treasury or economic growth
            Nation nation = plugin.getNationManager().getNationById(nationId);
            if (nation != null) {
                nation.setTreasury(nation.getTreasury() + economyFeedback * 10 * timeInHours);
                try {
                    plugin.getNationManager().save(nation);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to update nation treasury from feedback: " + e.getMessage());
                }
            }
        }
        
        // Military -> Infrastructure -> Production -> Military loop
        double militaryStrength = plugin.getMilitaryService().getMilitaryStrength(nationId) / 10000.0;
        if (militaryStrength > 0.1) { // Only trigger if military is somewhat developed
            double infraBoost = militaryStrength * serviceFeedback.getOrDefault("military", new HashMap<>()).getOrDefault("infrastructure", 0.0);
            // This could fund additional infrastructure development
            
            double productionBoost = infraBoost * serviceFeedback.getOrDefault("infrastructure", new HashMap<>()).getOrDefault("production", 0.0);
            // This could improve production rates
            
            double militaryFeedback = productionBoost * serviceFeedback.getOrDefault("production", new HashMap<>()).getOrDefault("military", 0.0);
            // This could improve military capabilities
        }
        
        // Education -> Innovation -> Economy -> Education loop
        double educationLevel = plugin.getEducationService().getEducationLevel(nationId) / 100.0;
        if (educationLevel > 0.2) { // Only trigger if education is somewhat developed
            double innovationBoost = educationLevel * serviceFeedback.getOrDefault("education", new HashMap<>()).getOrDefault("innovation", 0.0);
            // This could fund innovation projects
            
            double economyBoost2 = innovationBoost * serviceFeedback.getOrDefault("innovation", new HashMap<>()).getOrDefault("economy", 0.0);
            // This could boost the economy
            
            double educationFeedback = economyBoost2 * serviceFeedback.getOrDefault("economy", new HashMap<>()).getOrDefault("education", 0.0);
            // This could fund further education
        }
    }

    /**
     * Get interconnection statistics for a nation.
     */
    public synchronized Map<String, Object> getInterconnectionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        InterconnectionData data = nationInterconnections.get(nationId);
        ResourceFlow flow = resourceFlows.get(nationId);
        
        if (data != null) {
            stats.put("energyToEconomyFactor", data.energyToEconomyFactor);
            stats.put("resourcesToTechnologyFactor", data.resourcesToTechnologyFactor);
            stats.put("happinessToProductionFactor", data.happinessToProductionFactor);
            stats.put("militaryToInfrastructureFactor", data.militaryToInfrastructureFactor);
            stats.put("educationToInnovationFactor", data.educationToInnovationFactor);
            stats.put("populationToConsumptionFactor", data.populationToConsumptionFactor);
            stats.put("tradeToGrowthFactor", data.tradeToGrowthFactor);
            stats.put("diplomacyToStabilityFactor", data.diplomacyToStabilityFactor);
        }
        
        if (flow != null) {
            stats.put("energyToProductionFlow", flow.energyToProduction);
            stats.put("resourcesToTechnologyFlow", flow.resourcesToTechnology);
            stats.put("economyToInfrastructureFlow", flow.economyToInfrastructure);
            stats.put("militaryToResearchFlow", flow.militaryToResearch);
            stats.put("educationToEconomyFlow", flow.educationToEconomy);
            stats.put("tradeToResourcesFlow", flow.tradeToResources);
        }
        
        // Calculate interconnection health score
        double healthScore = 0.0;
        if (data != null) {
            healthScore = (data.energyToEconomyFactor + data.resourcesToTechnologyFactor + 
                          data.happinessToProductionFactor + data.educationToInnovationFactor) / 4.0;
        }
        stats.put("interconnectionHealth", healthScore);
        
        // Determine interconnection rating
        String rating = "BALANCED";
        if (healthScore > 1.5) rating = "STRONG";
        else if (healthScore > 1.2) rating = "GOOD";
        else if (healthScore < 0.8) rating = "WEAK";
        stats.put("rating", rating);
        
        return stats;
    }

    /**
     * Get global interconnection statistics.
     */
    public synchronized Map<String, Object> getGlobalInterconnectionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int nationsWithInterconnections = 0;
        double totalEnergyFactor = 0.0;
        double totalResourceFactor = 0.0;
        double totalHappinessFactor = 0.0;
        double totalEducationFactor = 0.0;
        double totalInterconnectionHealth = 0.0;
        
        for (Map.Entry<String, InterconnectionData> entry : nationInterconnections.entrySet()) {
            nationsWithInterconnections++;
            InterconnectionData data = entry.getValue();
            totalEnergyFactor += data.energyToEconomyFactor;
            totalResourceFactor += data.resourcesToTechnologyFactor;
            totalHappinessFactor += data.happinessToProductionFactor;
            totalEducationFactor += data.educationToInnovationFactor;
            
            // Calculate health score for this nation
            double healthScore = (data.energyToEconomyFactor + data.resourcesToTechnologyFactor + 
                                 data.happinessToProductionFactor + data.educationToInnovationFactor) / 4.0;
            totalInterconnectionHealth += healthScore;
        }
        
        stats.put("nationsWithInterconnections", nationsWithInterconnections);
        
        if (nationsWithInterconnections > 0) {
            stats.put("averageEnergyToEconomyFactor", totalEnergyFactor / nationsWithInterconnections);
            stats.put("averageResourcesToTechnologyFactor", totalResourceFactor / nationsWithInterconnections);
            stats.put("averageHappinessToProductionFactor", totalHappinessFactor / nationsWithInterconnections);
            stats.put("averageEducationToInnovationFactor", totalEducationFactor / nationsWithInterconnections);
            stats.put("averageInterconnectionHealth", totalInterconnectionHealth / nationsWithInterconnections);
        }
        
        // Top nations by interconnection health
        List<Map.Entry<String, Double>> topByHealth = new ArrayList<>();
        for (Map.Entry<String, InterconnectionData> entry : nationInterconnections.entrySet()) {
            InterconnectionData data = entry.getValue();
            double healthScore = (data.energyToEconomyFactor + data.resourcesToTechnologyFactor + 
                                 data.happinessToProductionFactor + data.educationToInnovationFactor) / 4.0;
            topByHealth.add(new AbstractMap.SimpleEntry<>(entry.getKey(), healthScore));
        }
        topByHealth.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByInterconnectionHealth", topByHealth.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        // Interconnection health distribution
        Map<String, Integer> healthDistribution = new HashMap<>();
        int veryStrong = 0, strong = 0, balanced = 0, weak = 0, veryWeak = 0;
        for (InterconnectionData data : nationInterconnections.values()) {
            double healthScore = (data.energyToEconomyFactor + data.resourcesToTechnologyFactor + 
                                 data.happinessToProductionFactor + data.educationToInnovationFactor) / 4.0;
            if (healthScore >= 1.6) veryStrong++;
            else if (healthScore >= 1.3) strong++;
            else if (healthScore >= 0.9) balanced++;
            else if (healthScore >= 0.6) weak++;
            else veryWeak++;
        }
        healthDistribution.put("veryStrong", veryStrong);
        healthDistribution.put("strong", strong);
        healthDistribution.put("balanced", balanced);
        healthDistribution.put("weak", weak);
        healthDistribution.put("veryWeak", veryWeak);
        stats.put("healthDistribution", healthDistribution);
        
        return stats;
    }

    /**
     * Get dependency map for analysis.
     */
    public Map<String, Map<String, Double>> getDependencies() {
        return new HashMap<>(serviceDependencies);
    }

    /**
     * Get feedback loop map for analysis.
     */
    public Map<String, Map<String, Double>> getFeedbackLoops() {
        return new HashMap<>(serviceFeedback);
    }

    /**
     * Get all nation interconnection data.
     */
    public Map<String, InterconnectionData> getNationInterconnections() {
        return new HashMap<>(nationInterconnections);
    }

    /**
     * Get all resource flow data.
     */
    public Map<String, ResourceFlow> getResourceFlows() {
        return new HashMap<>(resourceFlows);
    }
}