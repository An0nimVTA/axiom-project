package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.api.ModIntegrationAPI;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for diplomatic system integration with mod resources.
 * Manages diplomatic relations, treaties, and agreements enhanced by mod capabilities.
 */
public class DiplomaticIntegrationService {
    private final AXIOM plugin;
    private final ModIntegrationAPI modAPI;
    private final DiplomacySystem diplomacySystem;
    private final EconomyService economyService;
    private final Map<String, TreatyDetails> activeTreaties = new HashMap<>();
    private final Map<String, List<TradeAgreement>> tradeAgreements = new HashMap<>();
    private final Map<String, AllianceDetails> alliances = new HashMap<>();

    public DiplomaticIntegrationService(AXIOM plugin) {
        this.plugin = plugin;
        this.modAPI = plugin.getModIntegrationAPI();
        this.diplomacySystem = plugin.getDiplomacySystem();
        this.economyService = plugin.getEconomyService();
        
        // Schedule periodic treaty checks
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkTreatyCompliance, 0, 20 * 60 * 10); // every 10 minutes
    }

    /**
     * Create a resource-based treaty between two nations.
     */
    public String createResourceTreaty(String initiatorId, String targetId, Map<String, Object> terms) {
        try {
            // Check if both nations exist
            Nation initiator = plugin.getNationManager().getNationById(initiatorId);
            Nation target = plugin.getNationManager().getNationById(targetId);
            
            if (initiator == null || target == null) {
                return "Нация не найдена";
            }

            // Validate treaty terms
            if (!validateTreatyTerms(initiatorId, targetId, terms)) {
                return "Неверные условия договора";
            }

            // Calculate treaty benefits based on mod resources
            double modBonus = calculateModDiplomaticBonus(initiatorId, targetId);
            
            String treatyId = generateTreatyId(initiatorId, targetId);
            TreatyDetails treaty = new TreatyDetails(
                treatyId, 
                initiatorId, 
                targetId, 
                (String) terms.get("type"), 
                terms, 
                System.currentTimeMillis(), 
                (Long) terms.getOrDefault("duration", 86400000L) // 1 day default
            );
            
            // Apply mod bonuses to treaty effectiveness
            treaty.modBonus = modBonus;
            activeTreaties.put(treatyId, treaty);
            
            // Update diplomatic relations
            double baseRelation = (Double) terms.getOrDefault("relationImpact", 0.0);
            double totalImpact = baseRelation * (1.0 + modBonus);
            
            // In a real implementation, this would update diplomatic relations
            // For now, we'll just simulate the effect
            plugin.getLogger().info("Created treaty between " + initiatorId + " and " + targetId + 
                                   " with mod bonus: " + modBonus + " and total impact: " + totalImpact);

            return "✓ Договор заключен успешно! Мод-бонус: " + (modBonus * 100) + "%";
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating resource treaty: " + e.getMessage());
            return "Ошибка при создании договора: " + e.getMessage();
        }
    }

    /**
     * Validate treaty terms for mod compatibility.
     */
    public boolean validateTreatyTerms(String initiatorId, String targetId, Map<String, Object> terms) {
        String treatyType = (String) terms.get("type");
        Map<String, Object> requiredResources = (Map<String, Object>) terms.get("requiredResources");
        
        if (requiredResources == null) return true; // No resource requirements
        
        // Check if initiator has required resources
        for (Map.Entry<String, Object> entry : requiredResources.entrySet()) {
            String resourceType = entry.getKey();
            double requiredAmount = ((Number) entry.getValue()).doubleValue();
            
            // In a real implementation, this would check actual resource availability
            // For now, we'll just return true
        }
        
        return true;
    }

    /**
     * Create a trade agreement enhanced by logistics mods.
     */
    public String createTradeAgreement(String nation1Id, String nation2Id, Map<String, Object> tradeTerms) {
        try {
            // Check if logistics mods are available for better trade efficiency
            boolean hasLogisticsMods = modAPI.hasLogisticsMods();
            double logisticsBonus = hasLogisticsMods ? 0.2 : 0.0; // 20% bonus for logistics mods
            
            String agreementId = generateAgreementId(nation1Id, nation2Id);
            TradeAgreement agreement = new TradeAgreement(
                agreementId,
                nation1Id,
                nation2Id,
                tradeTerms,
                System.currentTimeMillis(),
                (Long) tradeTerms.getOrDefault("duration", 604800000L) // 7 days default
            );
            
            // Apply logistics bonus to trade efficiency
            agreement.logisticsBonus = logisticsBonus;
            
            // Add to both nations' trade agreements
            tradeAgreements.computeIfAbsent(nation1Id, k -> new ArrayList<>()).add(agreement);
            tradeAgreements.computeIfAbsent(nation2Id, k -> new ArrayList<>()).add(agreement);
            
            // Calculate enhanced trade value based on logistics
            double baseValue = calculateTradeValue(tradeTerms);
            double enhancedValue = baseValue * (1.0 + logisticsBonus);
            
            plugin.getLogger().info("Created trade agreement between " + nation1Id + " and " + nation2Id + 
                                   " with logistics bonus: " + logisticsBonus + 
                                   " and enhanced value: " + enhancedValue);

            return "✓ Торговое соглашение заключено! Логистический бонус: " + (logisticsBonus * 100) + "%";
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating trade agreement: " + e.getMessage());
            return "Ошибка при создании торгового соглашения: " + e.getMessage();
        }
    }

    /**
     * Create an alliance with mod-enhanced capabilities.
     */
    public String createAlliance(String nation1Id, String nation2Id, Map<String, Object> allianceTerms) {
        try {
            // Calculate alliance strength based on mod capabilities
            double warfareBonus = 0.0;
            double resourceBonus = 0.0;
            double technologyBonus = 0.0;
            
            if (modAPI.hasWarfareMods()) {
                warfareBonus = 0.15; // 15% military cooperation bonus
            }
            if (modAPI.hasIndustrialMods()) {
                resourceBonus = 0.1; // 10% resource sharing bonus
            }
            // Add more mod bonuses as needed
            
            String allianceId = generateAllianceId(nation1Id, nation2Id);
            AllianceDetails alliance = new AllianceDetails(
                allianceId,
                Arrays.asList(nation1Id, nation2Id),
                allianceTerms,
                warfareBonus,
                resourceBonus,
                technologyBonus
            );
            
            alliances.put(allianceId, alliance);
            
            plugin.getLogger().info("Created alliance between " + nation1Id + " and " + nation2Id + 
                                   " with combined mod bonuses: warfare=" + warfareBonus + 
                                   ", resources=" + resourceBonus + 
                                   ", tech=" + technologyBonus);

            return "✓ Альянс создан! Военный бонус: " + (warfareBonus * 100) + "%, Ресурсный бонус: " + (resourceBonus * 100) + "%";
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating alliance: " + e.getMessage());
            return "Ошибка при создании альянса: " + e.getMessage();
        }
    }

    /**
     * Calculate mod diplomatic bonus between two nations.
     */
    public double calculateModDiplomaticBonus(String nation1Id, String nation2Id) {
        double bonus = 0.0;
        
        // Check for shared mod interests
        Map<String, Object> nation1Resources = modAPI.getResourceStatistics(nation1Id);
        Map<String, Object> nation2Resources = modAPI.getResourceStatistics(nation2Id);
        
        // If both nations have similar high-value mod resources, increase diplomatic ties
        long nation1Diversity = (Long) nation1Resources.getOrDefault("resourceDiversity", 0L);
        long nation2Diversity = (Long) nation2Resources.getOrDefault("resourceDiversity", 0L);
        
        // Bonus for resource cooperation
        if (nation1Diversity > 3 && nation2Diversity > 3) {
            bonus += 0.1; // 10% bonus for developed resource economies
        }
        
        // Check energy cooperation
        double nation1Energy = modAPI.getEnergyProduction(nation1Id);
        double nation2Energy = modAPI.getEnergyProduction(nation2Id);
        
        if (nation1Energy > 5000 && nation2Energy > 5000) {
            bonus += 0.05; // 5% bonus for high energy nations
        }
        
        // Check for mod compatibility
        if (modAPI.hasLogisticsMods()) {
            bonus += 0.05; // 5% bonus for logistics capability
        }
        
        // Cap the bonus at 50%
        return Math.min(0.5, bonus);
    }

    /**
     * Get enhanced diplomatic statistics for a nation.
     */
    public Map<String, Object> getEnhancedDiplomaticStats(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic diplomatic stats
        stats.put("allies", getAllies(nationId).size());
        stats.put("trade_partners", getTradePartners(nationId).size());
        stats.put("treaties_active", getActiveTreaties(nationId).size());
        
        // Mod-enhanced stats
        double resourceDiplomacy = calculateResourceDiplomacyScore(nationId);
        stats.put("resource_diplomacy_score", resourceDiplomacy);
        
        double logisticsDiplomacy = calculateLogisticsDiplomacyScore(nationId);
        stats.put("logistics_diplomacy_score", logisticsDiplomacy);
        
        double warfareCooperation = calculateWarfareCooperationScore(nationId);
        stats.put("warfare_cooperation_score", warfareCooperation);
        
        // Composite diplomatic strength
        double compositeScore = (resourceDiplomacy * 0.4) + (logisticsDiplomacy * 0.3) + (warfareCooperation * 0.3);
        stats.put("composite_diplomatic_strength", compositeScore);
        
        // Potential for new agreements based on mod capabilities
        stats.put("diplomatic_potential", calculateDiplomaticPotential(nationId));
        
        return stats;
    }

    /**
     * Calculate resource diplomacy score.
     */
    public double calculateResourceDiplomacyScore(String nationId) {
        Map<String, Object> resourceStats = modAPI.getResourceStatistics(nationId);
        long diversity = (Long) resourceStats.getOrDefault("resourceDiversity", 0L);
        Double totalValue = (Double) resourceStats.getOrDefault("totalResourceValue", 0.0);
        
        // Score based on resource diversity and value
        double diversityScore = Math.min(50, diversity * 5); // Max 50 for diversity
        double valueScore = Math.min(50, Math.log10(Math.max(1, totalValue / 1000)) * 10); // Logarithmic scale for value
        
        return (diversityScore + valueScore) / 2; // Average score 0-50
    }

    /**
     * Calculate logistics diplomacy score.
     */
    public double calculateLogisticsDiplomacyScore(String nationId) {
        if (!modAPI.hasLogisticsMods()) {
            return 0;
        }
        
        // Score based on logistics capabilities
        double baseScore = 30; // Base for having logistics mods
        
        // Bonus for energy production (logistics need power)
        double energyFactor = Math.min(20, modAPI.getEnergyProduction(nationId) / 1000);
        return baseScore + energyFactor;
    }

    /**
     * Calculate warfare cooperation score.
     */
    public double calculateWarfareCooperationScore(String nationId) {
        if (!modAPI.hasWarfareMods()) {
            return 0;
        }
        
        double baseScore = 25; // Base for having warfare mods
        
        // Bonus for military potential
        double militaryPotential = modAPI.calculateWarfarePotential(nationId);
        double potentialBonus = Math.min(25, militaryPotential / 50); // Cap at 25
        
        return baseScore + potentialBonus;
    }

    /**
     * Calculate diplomatic potential.
     */
    public double calculateDiplomaticPotential(String nationId) {
        double potential = 10; // Base potential
        
        if (modAPI.hasLogisticsMods()) potential += 20;
        if (modAPI.hasWarfareMods()) potential += 15;
        if (modAPI.hasIndustrialMods()) potential += 15;
        
        // Factor in resource wealth
        Map<String, Object> resourceStats = modAPI.getResourceStatistics(nationId);
        Double totalValue = (Double) resourceStats.getOrDefault("totalResourceValue", 0.0);
        potential += Math.min(20, Math.log10(Math.max(1, totalValue / 10000)) * 5);
        
        return potential;
    }

    /**
     * Get allies of a nation.
     */
    public List<String> getAllies(String nationId) {
        List<String> alliesList = new ArrayList<>();
        
        for (AllianceDetails alliance : alliances.values()) {
            if (alliance.nations.contains(nationId)) {
                for (String nation : alliance.nations) {
                    if (!nation.equals(nationId)) {
                        alliesList.add(nation);
                    }
                }
            }
        }
        
        return alliesList;
    }

    /**
     * Get trade partners of a nation.
     */
    public List<String> getTradePartners(String nationId) {
        Set<String> partners = new HashSet<>();
        
        List<TradeAgreement> agreements = tradeAgreements.getOrDefault(nationId, new ArrayList<>());
        for (TradeAgreement agreement : agreements) {
            if (agreement.nation1Id.equals(nationId)) {
                partners.add(agreement.nation2Id);
            } else if (agreement.nation2Id.equals(nationId)) {
                partners.add(agreement.nation1Id);
            }
        }
        
        return new ArrayList<>(partners);
    }

    /**
     * Get active treaties for a nation.
     */
    public List<String> getActiveTreaties(String nationId) {
        List<String> treatyIds = new ArrayList<>();
        
        for (Map.Entry<String, TreatyDetails> entry : activeTreaties.entrySet()) {
            TreatyDetails treaty = entry.getValue();
            if (treaty.nation1Id.equals(nationId) || treaty.nation2Id.equals(nationId)) {
                treatyIds.add(entry.getKey());
            }
        }
        
        return treatyIds;
    }

    /**
     * Check if nations can form a beneficial alliance based on mod capabilities.
     */
    public boolean canFormBeneficialAlliance(String nation1Id, String nation2Id) {
        // Check if the combination would provide mutual benefits
        double nation1Warfare = modAPI.calculateWarfarePotential(nation1Id);
        double nation2Warfare = modAPI.calculateWarfarePotential(nation2Id);
        double combinedWarfare = (nation1Warfare + nation2Warfare) * 1.1; // 10% alliance bonus
        
        double nation1Resources = modAPI.getResourceStatistics(nation1Id).size(); // Simplified
        double nation2Resources = modAPI.getResourceStatistics(nation2Id).size(); // Simplified
        double combinedResources = (nation1Resources + nation2Resources) * 1.1; // 10% alliance bonus
        
        // Calculate benefit threshold
        double benefitThreshold = (nation1Warfare + nation2Warfare + nation1Resources + nation2Resources) * 0.15; // 15% improvement needed
        
        return (combinedWarfare + combinedResources) > 
               (nation1Warfare + nation2Warfare + nation1Resources + nation2Resources + benefitThreshold);
    }

    /**
     * Calculate trade value based on terms.
     */
    private double calculateTradeValue(Map<String, Object> tradeTerms) {
        // Simplified calculation - in reality would be more complex
        return 1000.0; // Placeholder value
    }

    /**
     * Generate unique treaty ID.
     */
    private String generateTreatyId(String nation1Id, String nation2Id) {
        return "treaty_" + nation1Id + "_" + nation2Id + "_" + System.currentTimeMillis();
    }

    /**
     * Generate unique agreement ID.
     */
    private String generateAgreementId(String nation1Id, String nation2Id) {
        return "trade_" + nation1Id + "_" + nation2Id + "_" + System.currentTimeMillis();
    }

    /**
     * Generate unique alliance ID.
     */
    private String generateAllianceId(String nation1Id, String nation2Id) {
        return "alliance_" + nation1Id + "_" + nation2Id + "_" + System.currentTimeMillis();
    }

    /**
     * Check treaty compliance and update statuses.
     */
    private void checkTreatyCompliance() {
        long now = System.currentTimeMillis();
        List<String> expiredTreaties = new ArrayList<>();
        
        for (Map.Entry<String, TreatyDetails> entry : activeTreaties.entrySet()) {
            TreatyDetails treaty = entry.getValue();
            if (now > (treaty.startTime + treaty.duration)) {
                expiredTreaties.add(entry.getKey());
                
                // In a real implementation, this would trigger expiration events
                plugin.getLogger().info("Treaty expired: " + entry.getKey());
            }
        }
        
        // Remove expired treaties
        for (String treatyId : expiredTreaties) {
            activeTreaties.remove(treatyId);
        }
        
        // Check trade agreements as well
        List<String> expiredAgreements = new ArrayList<>();
        for (Map.Entry<String, List<TradeAgreement>> entry : tradeAgreements.entrySet()) {
            List<TradeAgreement> agreements = entry.getValue();
            agreements.removeIf(agreement -> 
                now > (agreement.startTime + agreement.duration)
            );
        }
    }

    // Data classes for diplomatic structures
    
    public static class TreatyDetails {
        public final String treatyId;
        public final String nation1Id;
        public final String nation2Id;
        public final String type;
        public final Map<String, Object> terms;
        public final long startTime;
        public final long duration;
        public double modBonus = 0.0;
        
        public TreatyDetails(String treatyId, String nation1Id, String nation2Id, String type, 
                           Map<String, Object> terms, long startTime, long duration) {
            this.treatyId = treatyId;
            this.nation1Id = nation1Id;
            this.nation2Id = nation2Id;
            this.type = type;
            this.terms = terms;
            this.startTime = startTime;
            this.duration = duration;
        }
    }
    
    public static class TradeAgreement {
        public final String agreementId;
        public final String nation1Id;
        public final String nation2Id;
        public final Map<String, Object> terms;
        public final long startTime;
        public final long duration;
        public double logisticsBonus = 0.0;
        
        public TradeAgreement(String agreementId, String nation1Id, String nation2Id, 
                            Map<String, Object> terms, long startTime, long duration) {
            this.agreementId = agreementId;
            this.nation1Id = nation1Id;
            this.nation2Id = nation2Id;
            this.terms = terms;
            this.startTime = startTime;
            this.duration = duration;
        }
    }
    
    public static class AllianceDetails {
        public final String allianceId;
        public final List<String> nations;
        public final Map<String, Object> terms;
        public final double warfareBonus;
        public final double resourceBonus;
        public final double technologyBonus;
        
        public AllianceDetails(String allianceId, List<String> nations, Map<String, Object> terms,
                             double warfareBonus, double resourceBonus, double technologyBonus) {
            this.allianceId = allianceId;
            this.nations = nations;
            this.terms = terms;
            this.warfareBonus = warfareBonus;
            this.resourceBonus = resourceBonus;
            this.technologyBonus = technologyBonus;
        }
    }
}