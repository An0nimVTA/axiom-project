package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.api.ModIntegrationAPI;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for disaster system integration with mod events.
 * Manages natural and artificial disasters, their impacts, and recovery using mod interactions.
 */
public class DisasterService implements Listener {
    private final AXIOM plugin;
    private final ModIntegrationAPI modAPI;
    private final NationManager nationManager;
    private final EconomyService economyService;
    private final ResourceService resourceService;
    
    private final Map<String, DisasterEvent> activeDisasters = new HashMap<>();
    private final Map<String, List<DisasterEvent>> nationDisasters = new HashMap<>();
    private final Map<String, DisasterPreparedness> preparednessLevels = new HashMap<>();
    private final Map<String, List<DisasterAid>> internationalAid = new HashMap<>();
    private final Map<String, Long> lastDisasterTime = new HashMap<>();
    private final Map<String, Integer> disasterCount = new HashMap<>();
    private final Map<String, List<DisasterDamage>> disasterDamages = new HashMap<>();
    private final Map<String, List<DisasterRecovery>> disasterRecoveries = new HashMap<>();
    private final Random random = new Random();

    public DisasterService(AXIOM plugin) {
        this.plugin = plugin;
        this.modAPI = plugin.getModIntegrationAPI();
        this.nationManager = plugin.getNationManager();
        this.economyService = plugin.getEconomyService();
        this.resourceService = plugin.getResourceService();
        
        // Register as event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Schedule periodic disaster checking
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkForDisasters, 0, 20 * 60 * 15); // every 15 minutes
    }

    /**
     * Trigger a disaster event in a specific nation.
     */
    public String triggerDisaster(String nationId, String disasterType, String cause) {
        try {
            Nation nation = nationManager.getNationById(nationId);
            if (nation == null) {
                return "Нация не найдена";
            }

            // Validate disaster type
            if (!isValidDisasterType(disasterType)) {
                return "Неверный тип бедствия";
            }
            
            // Check if nation has had too many disasters recently
            if (hasTooManyRecentDisasters(nationId)) {
                return "Нация переживает слишком много бедствий за короткое время";
            }
            
            // Calculate disaster severity based on mod factors
            double severity = calculateDisasterSeverity(nationId, disasterType);
            
            // Calculate impacted territories
            List<String> impactedTerritories = getRandomTerritories(nation, (int)(severity * 0.3));
            
            // Create disaster event
            String disasterId = generateDisasterId(nationId, disasterType);
            DisasterEvent disaster = new DisasterEvent(
                disasterId,
                nationId,
                disasterType,
                cause,
                severity,
                impactedTerritories,
                System.currentTimeMillis(),
                estimateDisasterDuration(disasterType, severity)
            );
            
            activeDisasters.put(disasterId, disaster);
            nationDisasters.computeIfAbsent(nationId, k -> new ArrayList<>()).add(disaster);
            
            // Add disaster to history
            disasterCount.merge(nationId, 1, Integer::sum);
            lastDisasterTime.put(nationId, System.currentTimeMillis());
            
            // Apply immediate effects
            applyDisasterEffects(disaster);
            
            // Notify nation
            notifyNationOfDisaster(nationId, disaster);
            
            plugin.getLogger().info("Disaster triggered: " + disasterType + " in nation " + nationId + 
                                   " with severity: " + severity);
            
            return "✓ Бедствие \"" + disasterType + "\" начато в нации " + nationId + 
                   " с уровнем серьезности " + String.format("%.2f", severity);
        } catch (Exception e) {
            plugin.getLogger().severe("Error triggering disaster: " + e.getMessage());
            return "Ошибка при вызове бедствия: " + e.getMessage();
        }
    }

    /**
     * Simulate mod-based disaster caused by mod interactions.
     */
    public String simulateModDisaster(Location location, String modId, String disasterType) {
        try {
            // Determine affected nation based on location
            String nationId = getNationAtLocation(location);
            if (nationId == null) {
                return "Нет нации в данной локации";
            }
            
            // Check if this is really from a mod
            if (!modAPI.isModAvailable(modId)) {
                return "Мод не доступен на сервере";
            }
            
            // Calculate severity based on mod type and interaction
            double severity = calculateModDisasterSeverity(modId);
            
            // Determine affected territories near the location
            Nation nation = nationManager.getNationById(nationId);
            List<String> impactedTerritories = getTerritoriesNearLocation(nation, location, (int)(severity * 0.2));
            
            // Create mod-specific disaster
            String disasterId = generateModDisasterId(nationId, modId, disasterType);
            DisasterEvent disaster = new DisasterEvent(
                disasterId,
                nationId,
                disasterType + "_from_" + modId,
                "Мод-индуцированное бедствие от " + modId,
                severity,
                impactedTerritories,
                System.currentTimeMillis(),
                estimateDisasterDuration(disasterType, severity)
            );
            
            activeDisasters.put(disasterId, disaster);
            nationDisasters.computeIfAbsent(nationId, k -> new ArrayList<>()).add(disaster);
            
            // Apply mod-specific effects
            applyModSpecificDisasterEffects(disaster, modId);
            
            // Apply general effects
            applyDisasterEffects(disaster);
            
            // Notify players in the area
            notifyPlayersInArea(location, disaster);
            
            plugin.getLogger().info("Mod-induced disaster: " + disasterType + " from mod " + 
                                   modId + " in nation " + nationId + " (severity: " + severity + ")");
            
            return "✓ Мод-индуцированное бедствие \"" + disasterType + "\" от мода " + 
                   modId + " начато в нации " + nationId;
        } catch (Exception e) {
            plugin.getLogger().severe("Error simulating mod disaster: " + e.getMessage());
            return "Ошибка при симуляции мод-бедствия: " + e.getMessage();
        }
    }

    /**
     * Apply disaster effects to affected nations/entities.
     */
    private void applyDisasterEffects(DisasterEvent disaster) {
        String nationId = disaster.nationId;
        double severity = disaster.severity;
        
        // Apply economic damage
        double economicDamage = calculateEconomicDamage(severity);
        economyService.deposit(nationId, -economicDamage);
        
        // Apply resource damage
        double resourceDamage = calculateResourceDamage(severity);
        List<String> resourceTypes = Arrays.asList("food", "materials", "energy", "medicine");
        for (String resourceType : resourceTypes) {
            double reduction = resourceDamage * random.nextDouble();
            resourceService.removeResource(nationId, resourceType, reduction);
        }
        
        // Apply population effects
        applyPopulationEffects(disaster);
        
        // Add to damages tracking
        DisasterDamage damage = new DisasterDamage(
            disaster.disasterId,
            nationId,
            economicDamage,
            resourceDamage,
            (int)(severity * 10), // Estimated population loss
            System.currentTimeMillis()
        );
        
        disasterDamages.computeIfAbsent(nationId, k -> new ArrayList<>()).add(damage);
    }

    /**
     * Apply mod-specific disaster effects.
     */
    private void applyModSpecificDisasterEffects(DisasterEvent disaster, String modId) {
        switch (modId.toLowerCase()) {
            case "immersiveengineering":
                // Industrial accidents from IE machines
                applyIndustrialEffect(disaster);
                break;
            case "tacz":
            case "pointblank":
            case "ballistix":
                // Weapon-related disasters
                applyExplosionEffect(disaster);
                break;
            case "appliedenergistics2":
                // Network/grid failures
                applyNetworkFailureEffect(disaster);
                break;
            case "industrialupgrade":
                // Large-scale industrial failures
                applyLargeScaleIndustrialEffect(disaster);
                break;
            default:
                // Generic mod effect
                applyGenericModEffect(disaster);
        }
    }

    /**
     * Apply industrial accident effects (from Immersive Engineering).
     */
    private void applyIndustrialEffect(DisasterEvent disaster) {
        double severity = disaster.severity;
        
        // Reduce industrial capacity of the nation
        if (modAPI.hasIndustrialMods()) {
            // Apply additional industrial penalty
            double penalty = severity * 0.3;
            // In a real implementation, this would reduce industrial production
            
            plugin.getLogger().info("Applied industrial effects for disaster " + disaster.disasterId);
        }
    }

    /**
     * Apply explosion/weapon-related effects.
     */
    private void applyExplosionEffect(DisasterEvent disaster) {
        double severity = disaster.severity;
        
        // More severe effects for warfare mods
        double extraEconomicDamage = severity * 1000; // Extra damage from weapons
        economyService.deposit(disaster.nationId, -extraEconomicDamage);
        
        // Apply to players in affected areas
        for (String territory : disaster.impactedTerritories) {
            Location center = getCenterOfTerritory(territory);
            if (center != null) {
                applyPotionEffectsToNearbyPlayers(center, severity);
            }
        }
    }

    /**
     * Apply network/grid failure effects (from AE2).
     */
    private void applyNetworkFailureEffect(DisasterEvent disaster) {
        double severity = disaster.severity;
        
        // Disrupt logistics and automation for a period
        // In a real implementation, this would affect mod machines
        
        plugin.getLogger().info("Applied AE2 network failure effects for disaster " + disaster.disasterId);
    }

    /**
     * Apply large-scale industrial effects (from Industrial Upgrade).
     */
    private void applyLargeScaleIndustrialEffect(DisasterEvent disaster) {
        double severity = disaster.severity;
        
        // Apply broad industrial disruption
        // In a real implementation, this would affect all industrial processes
        
        plugin.getLogger().info("Applied Industrial Upgrade effects for disaster " + disaster.disasterId);
    }

    /**
     * Apply generic mod effects.
     */
    private void applyGenericModEffect(DisasterEvent disaster) {
        double severity = disaster.severity;
        
        // Apply moderate generic effects
        plugin.getLogger().info("Applied generic mod effects for disaster " + disaster.disasterId);
    }

    /**
     * Apply population effects based on disaster.
     */
    private void applyPopulationEffects(DisasterEvent disaster) {
        double severity = disaster.severity;
        int populationAffected = (int)(severity * 50); // Simplified calculation
        
        // In a real implementation, this would affect actual population numbers
        // For now, we'll just log that effects were applied
        plugin.getLogger().info("Applied population effects to " + populationAffected + 
                               " people for disaster " + disaster.disasterId);
    }

    /**
     * Apply potion effects to players near disaster location.
     */
    private void applyPotionEffectsToNearbyPlayers(Location center, double severity) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(center.getWorld()) && 
                player.getLocation().distance(center) < (severity * 50)) {
                
                // Apply negative effects based on severity
                int duration = (int)(severity * 20 * 20); // ticks (20 ticks = 1 sec)
                
                // Apply weakness, slowness, etc. based on severity
                if (severity > 2.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0));
                }
                if (severity > 3.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 0));
                }
                if (severity > 4.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));
                }
                
                player.sendMessage("§c[БЕДСТВИЕ] §fВы пострадали от бедствия! Длительность: §e" + (duration/20) + "§f секунд");
            }
        }
    }

    /**
     * Calculate disaster severity based on mod factors.
     */
    private double calculateDisasterSeverity(String nationId, String disasterType) {
        double baseSeverity = 1.0 + (random.nextDouble() * 4.0); // 1.0 to 5.0
        
        // Apply mod-related modifiers
        if (modAPI.hasWarfareMods()) {
            baseSeverity *= 1.2; // Warfare mods = more intense disasters
        }
        if (modAPI.hasIndustrialMods()) {
            baseSeverity *= 1.1; // Industrial mods = more industrial risks
        }
        if (modAPI.hasEnergyMods()) {
            // Energy mods = different kind of risk
            baseSeverity *= 0.95; // But also better infrastructure
        }
        
        // Apply disaster-type specific modifiers
        switch (disasterType.toLowerCase()) {
            case "earthquake":
            case "tsunami":
            case "volcano":
                baseSeverity *= 1.3; // Natural disasters from modded worldgen
                break;
            case "industrial_accident":
                baseSeverity *= modAPI.hasIndustrialMods() ? 1.5 : 0.8; // Mod-dependent
                break;
            case "nuclear_incident":
                baseSeverity *= modAPI.hasEnergyMods() ? 1.8 : 0.5; // Only possible with energy mods
                break;
            default:
                baseSeverity *= 1.0;
        }
        
        // Cap the severity
        return Math.min(10.0, baseSeverity);
    }

    /**
     * Calculate mod disaster severity based on mod type.
     */
    private double calculateModDisasterSeverity(String modId) {
        switch (modId.toLowerCase()) {
            case "ballistix":
                return 4.0 + (random.nextDouble() * 3.0); // Explosives are dangerous
            case "tacz":
            case "pointblank":
                return 3.0 + (random.nextDouble() * 2.0); // Small arms but can accumulate
            case "immersiveengineering":
                return 2.0 + (random.nextDouble() * 2.0); // Industrial accidents
            case "appliedenergistics2":
                return 2.5 + (random.nextDouble() * 1.5); // Network failures
            case "industrialupgrade":
                return 3.5 + (random.nextDouble() * 2.0); // Large industrial systems
            default:
                return 1.5 + (random.nextDouble() * 2.0); // Generic mod risk
        }
    }

    /**
     * Calculate economic damage from disaster.
     */
    private double calculateEconomicDamage(double severity) {
        // Base damage scaled by severity
        double baseDamage = severity * 1000;
        
        // Apply preparedness mitigation
        DisasterPreparedness preparedness = preparednessLevels.getOrDefault(
            getNationAtLocation(Bukkit.getWorlds().get(0).getSpawnLocation()), 
            new DisasterPreparedness(1.0, 1.0, 1.0, 1.0)
        );
        
        return baseDamage * (1.0 - preparedness.economicMitigation);
    }

    /**
     * Calculate resource damage from disaster.
     */
    private double calculateResourceDamage(double severity) {
        // Base resource damage
        return severity * 500;
    }

    /**
     * Notify nation of disaster.
     */
    private void notifyNationOfDisaster(String nationId, DisasterEvent disaster) {
        // Notify nation leaders/admins
        Nation nation = nationManager.getNationById(nationId);
        if (nation == null) return;
        
        // In a real implementation, this would notify nation members
        for (UUID citizenId : nation.getCitizens()) {
            Player player = Bukkit.getPlayer(citizenId);
            if (player != null) {
                player.sendMessage("§c[БЕДСТВИЕ] §fВ вашей нации произошло бедствие:");
                player.sendMessage("§e" + disaster.type + " §fс причиной: §7" + disaster.cause);
                player.sendMessage("§cСерьезность: §f" + String.format("%.2f", disaster.severity));
                player.sendMessage("§7Изменения могут затронуть экономику и ресурсы нации.");
            }
        }
    }

    /**
     * Notify players in the area of disaster.
     */
    private void notifyPlayersInArea(Location location, DisasterEvent disaster) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) < 100) { // Within 100 blocks
                player.sendMessage("§c[ЛОКАЛЬНОЕ БЕДСТВИЕ] §fВ вашем районе происходит: §e" + disaster.type);
                player.sendMessage("§7Опасайтесь возможных эффектов в этой области!");
            }
        }
    }

    /**
     * Get nation at specific location.
     */
    private String getNationAtLocation(Location location) {
        return nationManager.getNationAtLocation(location)
            .map(Nation::getId)
            .orElse(null);
    }

    /**
     * Get random territories from a nation.
     */
    private List<String> getRandomTerritories(Nation nation, int count) {
        List<String> territories = new ArrayList<>(nation.getClaimedChunkKeys());
        Collections.shuffle(territories, random);
        
        int actualCount = Math.min(count, territories.size());
        return territories.subList(0, actualCount);
    }

    /**
     * Get territories near a specific location.
     */
    private List<String> getTerritoriesNearLocation(Nation nation, Location location, int radius) {
        List<String> allTerritories = new ArrayList<>(nation.getClaimedChunkKeys());
        List<String> nearbyTerritories = new ArrayList<>();
        
        for (String territory : allTerritories) {
            // Simplified chunk distance calculation
            String[] coords = territory.split(":");
            if (coords.length >= 2) {
                try {
                    int chunkX = Integer.parseInt(coords[0]);
                    int chunkZ = Integer.parseInt(coords[1]);
                    
                    int playerChunkX = location.getChunk().getX();
                    int playerChunkZ = location.getChunk().getZ();
                    
                    int distance = Math.abs(chunkX - playerChunkX) + Math.abs(chunkZ - playerChunkZ);
                    if (distance <= radius) {
                        nearbyTerritories.add(territory);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid territory
                }
            }
        }
        
        return nearbyTerritories;
    }

    /**
     * Get center of a territory (chunk).
     */
    private Location getCenterOfTerritory(String territoryKey) {
        try {
            String[] parts = territoryKey.split(":");
            if (parts.length >= 2) {
                int chunkX = Integer.parseInt(parts[0]) * 16 + 8; // Center of chunk
                int chunkZ = Integer.parseInt(parts[1]) * 16 + 8;
                
                World world = Bukkit.getWorlds().get(0); // Default world
                return new Location(world, chunkX, 100, chunkZ); // Y at 100 for safety
            }
        } catch (NumberFormatException e) {
            // Invalid territory key
        }
        
        return null;
    }

    /**
     * Validate disaster type.
     */
    private boolean isValidDisasterType(String disasterType) {
        List<String> validTypes = Arrays.asList(
            "earthquake", "flood", "wildfire", "storm", "tsunami", "volcano",
            "drought", "pandemic", "industrial_accident", "nuclear_incident",
            "terrorist_attack", "cyber_attack", "economic_crash"
        );
        
        return validTypes.contains(disasterType.toLowerCase());
    }

    /**
     * Check if nation has too many recent disasters.
     */
    private boolean hasTooManyRecentDisasters(String nationId) {
        Long lastTime = lastDisasterTime.get(nationId);
        if (lastTime == null) return false;
        
        // Allow max 1 major disaster every 2 hours
        return (System.currentTimeMillis() - lastTime) < (2 * 60 * 60 * 1000);
    }

    /**
     * Estimate disaster duration based on type and severity.
     */
    private long estimateDisasterDuration(String disasterType, double severity) {
        // Base duration in milliseconds
        long baseDuration;
        
        switch (disasterType.toLowerCase()) {
            case "wildfire":
                baseDuration = 30 * 60 * 1000; // 30 minutes
                break;
            case "storm":
                baseDuration = 45 * 60 * 1000; // 45 minutes
                break;
            case "flood":
                baseDuration = 60 * 60 * 1000; // 1 hour
                break;
            case "earthquake":
                baseDuration = 15 * 60 * 1000; // 15 minutes
                break;
            case "industrial_accident":
                baseDuration = 120 * 60 * 1000; // 2 hours
                break;
            case "nuclear_incident":
                baseDuration = 24 * 60 * 60 * 1000; // 24 hours
                break;
            default:
                baseDuration = 60 * 60 * 1000; // 1 hour default
        }
        
        // Adjust by severity (longer for more severe disasters)
        return (long)(baseDuration * (1.0 + (severity / 10.0)));
    }

    /**
     * Generate unique disaster ID.
     */
    private String generateDisasterId(String nationId, String disasterType) {
        return "disaster_" + disasterType + "_" + nationId + "_" + System.currentTimeMillis();
    }

    /**
     * Generate unique mod disaster ID.
     */
    private String generateModDisasterId(String nationId, String modId, String disasterType) {
        return "mod_disaster_" + modId + "_" + disasterType + "_" + nationId + "_" + System.currentTimeMillis();
    }

    /**
     * Check for disasters periodically.
     */
    private void checkForDisasters() {
        long currentTime = System.currentTimeMillis();
        
        // Process active disasters
        List<String> completedDisasters = new ArrayList<>();
        for (Map.Entry<String, DisasterEvent> entry : activeDisasters.entrySet()) {
            DisasterEvent disaster = entry.getValue();
            if (currentTime > (disaster.startTime + disaster.duration)) {
                // Disaster has ended
                processDisasterEnd(disaster);
                completedDisasters.add(entry.getKey());
            }
        }
        
        // Remove completed disasters
        for (String disasterId : completedDisasters) {
            activeDisasters.remove(disasterId);
        }
    }

    /**
     * Process when a disaster ends.
     */
    private void processDisasterEnd(DisasterEvent disaster) {
        String nationId = disaster.nationId;
        
        plugin.getLogger().info("Disaster ending: " + disaster.type + " in " + nationId);
        
        // Calculate recovery needs
        double recoveryCost = estimateRecoveryCost(disaster);
        
        // Add recovery process
        DisasterRecovery recovery = new DisasterRecovery(
            "recovery_" + disaster.disasterId,
            disaster.disasterId,
            nationId,
            recoveryCost,
            disaster.severity,
            System.currentTimeMillis()
        );
        
        disasterRecoveries.computeIfAbsent(nationId, k -> new ArrayList<>()).add(recovery);
        
        // Apply recovery requirements to nation
        // In a real implementation, this would start the recovery process
    }

    /**
     * Estimate recovery cost for a disaster.
     */
    private double estimateRecoveryCost(DisasterEvent disaster) {
        // Recovery cost is proportional to damage caused
        double baseCost = disaster.severity * 5000;
        
        // Apply mod factors
        if (modAPI.hasIndustrialMods()) {
            baseCost *= 1.2; // More infrastructure to repair
        }
        if (modAPI.hasEnergyMods()) {
            baseCost *= 1.1; // More systems to restore
        }
        
        return baseCost;
    }

    /**
     * Assess disaster preparedness for a nation.
     */
    public DisasterPreparedness assessPreparedness(String nationId) {
        double baseEconomicMitigation = 0.1; // 10% base mitigation
        double baseResourceMitigation = 0.1;
        double basePopulationMitigation = 0.1;
        double baseInfrastructureMitigation = 0.1;
        
        // Apply mod bonuses
        if (modAPI.hasIndustrialMods()) {
            baseInfrastructureMitigation += 0.2; // Industrial mods = better infrastructure
        }
        if (modAPI.hasEnergyMods()) {
            baseInfrastructureMitigation += 0.15; // Energy mods = better utilities
        }
        if (modAPI.hasLogisticsMods()) {
            baseResourceMitigation += 0.2; // Logistics mods = better resource management
        }
        
        // Apply technology bonuses
        if (plugin.getTechnologyTreeService() != null) {
            baseEconomicMitigation += plugin.getTechnologyTreeService().getBonus(nationId, "disasterMitigation") * 0.1;
        }
        
        // Cap at 90% mitigation
        DisasterPreparedness preparedness = new DisasterPreparedness(
            Math.min(0.9, baseEconomicMitigation),
            Math.min(0.9, baseResourceMitigation),
            Math.min(0.9, basePopulationMitigation),
            Math.min(0.9, baseInfrastructureMitigation)
        );
        
        preparednessLevels.put(nationId, preparedness);
        return preparedness;
    }

    /**
     * Request international aid for disaster relief.
     */
    public String requestInternationalAid(String affectedNationId, String donorNationId, double amount, String resourceType) {
        try {
            Nation affectedNation = nationManager.getNationById(affectedNationId);
            Nation donorNation = nationManager.getNationById(donorNationId);
            
            if (affectedNation == null || donorNation == null) {
                return "Нация не найдена";
            }
            
            // Check diplomatic relations
            double relation = checkNationRelation(affectedNationId, donorNationId);
            if (relation < 10) { // Need somewhat positive relation
                return "Недостаточные дипломатические отношения";
            }
            
            // Check if donor has sufficient resources
            if (!hasSufficientAidResources(donorNationId, amount, resourceType)) {
                return "Донорская нация не имеет достаточных ресурсов";
            }
            
            // Process the aid
            String aidId = generateAidId(affectedNationId, donorNationId);
            
            // Transfer resources from donor to affected nation
            if ("money".equalsIgnoreCase(resourceType)) {
                economyService.deposit(donorNationId, -amount);
                economyService.deposit(affectedNationId, amount);
            } else {
                resourceService.removeResource(donorNationId, resourceType, amount);
                resourceService.addResource(affectedNationId, resourceType, amount);
            }
            
            DisasterAid aid = new DisasterAid(
                aidId,
                affectedNationId,
                donorNationId,
                amount,
                resourceType,
                System.currentTimeMillis()
            );
            
            internationalAid.computeIfAbsent(affectedNationId, k -> new ArrayList<>()).add(aid);
            
            plugin.getLogger().info("International aid requested: " + amount + " " + 
                                   resourceType + " from " + donorNationId + " to " + affectedNationId);
            
            return "✓ Международная помощь " + amount + " " + resourceType + " отправлена!";
        } catch (Exception e) {
            plugin.getLogger().severe("Error requesting international aid: " + e.getMessage());
            return "Ошибка при запросе помощи: " + e.getMessage();
        }
    }

    /**
     * Check if donor nation has sufficient resources for aid.
     */
    private boolean hasSufficientAidResources(String donorNationId, double amount, String resourceType) {
        if ("money".equalsIgnoreCase(resourceType)) {
            double treasury = economyService.getTreasury(donorNationId);
            return treasury >= (amount * 1.1); // Need 10% more than requested
        } else {
            double available = resourceService.getResource(donorNationId, resourceType);
            return available >= (amount * 1.1);
        }
    }

    /**
     * Check nation-to-nation relation (simplified).
     */
    private double checkNationRelation(String nation1Id, String nation2Id) {
        // Placeholder - in real implementation this would check diplomatic relations
        return 50.0; // Neutral relation
    }

    /**
     * Get disaster statistics for a nation.
     */
    public Map<String, Object> getDisasterStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic disaster counts
        int totalDisasters = disasterCount.getOrDefault(nationId, 0);
        stats.put("totalDisasters", totalDisasters);
        
        // Active disasters
        List<DisasterEvent> active = new ArrayList<>();
        for (DisasterEvent disaster : nationDisasters.getOrDefault(nationId, new ArrayList<>())) {
            if (System.currentTimeMillis() < (disaster.startTime + disaster.duration)) {
                active.add(disaster);
            }
        }
        stats.put("activeDisasters", active);
        
        // Disaster damages
        List<DisasterDamage> damages = disasterDamages.getOrDefault(nationId, new ArrayList<>());
        double totalEconomicDamage = damages.stream()
            .mapToDouble(d -> d.economicDamage)
            .sum();
        double totalResourceDamage = damages.stream()
            .mapToDouble(d -> d.resourceDamage)
            .sum();
        stats.put("totalEconomicDamage", totalEconomicDamage);
        stats.put("totalResourceDamage", totalResourceDamage);
        
        // Preparedness level
        DisasterPreparedness preparedness = assessPreparedness(nationId);
        stats.put("preparedness", preparedness);
        
        // Recovery efforts
        List<DisasterRecovery> recoveries = disasterRecoveries.getOrDefault(nationId, new ArrayList<>());
        stats.put("ongoingRecoveries", recoveries.size());
        
        // International aid received
        List<DisasterAid> receivedAid = internationalAid.getOrDefault(nationId, new ArrayList<>());
        stats.put("internationalAidReceived", receivedAid.size());
        stats.put("totalAidReceived", receivedAid.stream()
            .mapToDouble(a -> a.amount)
            .sum());
        
        // Mod-related disaster metrics
        int modDisasters = 0;
        for (DisasterEvent disaster : nationDisasters.getOrDefault(nationId, new ArrayList<>())) {
            if (disaster.type.contains("_from_")) {
                modDisasters++;
            }
        }
        stats.put("modInducedDisasters", modDisasters);
        stats.put("hasModDisasterResilience", modAPI.hasIndustrialMods() || modAPI.hasEnergyMods());
        
        // Risk assessment
        stats.put("disasterRiskLevel", calculateDisasterRisk(nationId));
        stats.put("recentDisasterCount", getRecentDisasterCount(nationId));
        
        return stats;
    }

    /**
     * Calculate disaster risk level for a nation.
     */
    private double calculateDisasterRisk(String nationId) {
        double baseRisk = 1.0;
        
        // Higher risk with more territory
        Nation nation = nationManager.getNationById(nationId);
        if (nation != null) {
            baseRisk += Math.log10(Math.max(1, nation.getClaimedChunkKeys().size()));
        }
        
        // Higher risk with more mod interactions
        if (modAPI.hasWarfareMods() || modAPI.hasIndustrialMods()) {
            baseRisk *= 2.0;
        }
        
        // Lower risk with better preparedness
        DisasterPreparedness prep = preparednessLevels.getOrDefault(nationId, assessPreparedness(nationId));
        baseRisk *= (1.0 - (prep.averageMitigation() * 0.5));
        
        return Math.min(10.0, baseRisk);
    }

    /**
     * Get count of recent disasters (last week).
     */
    private int getRecentDisasterCount(String nationId) {
        long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        
        int count = 0;
        for (DisasterEvent disaster : nationDisasters.getOrDefault(nationId, new ArrayList<>())) {
            if (disaster.startTime > weekAgo) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * Force a disaster based on external conditions.
     */
    public boolean forceDisasterByConditions(String nationId, double riskThreshold) {
        double disasterRisk = calculateDisasterRisk(nationId);
        
        if (disasterRisk > riskThreshold) {
            // Choose a likely disaster type based on mod situation
            String[] likelyDisasters = {"industrial_accident", "earthquake", "flood", "wildfire"};
            String disasterType = likelyDisasters[random.nextInt(likelyDisasters.length)];
            
            String cause = "Накопленные риски";
            if (modAPI.hasIndustrialMods()) cause = "Промышленная перегрузка";
            else if (modAPI.hasWarfareMods()) cause = "Милитаризованные риски";
            
            triggerDisaster(nationId, disasterType, cause);
            return true;
        }
        
        return false;
    }

    // Data classes for disaster system structures
    
    public static class DisasterEvent {
        public final String disasterId;
        public final String nationId;
        public final String type;
        public final String cause;
        public final double severity;
        public final List<String> impactedTerritories;
        public final long startTime;
        public final long duration;
        
        public DisasterEvent(String disasterId, String nationId, String type, String cause, 
                            double severity, List<String> impactedTerritories, 
                            long startTime, long duration) {
            this.disasterId = disasterId;
            this.nationId = nationId;
            this.type = type;
            this.cause = cause;
            this.severity = severity;
            this.impactedTerritories = impactedTerritories;
            this.startTime = startTime;
            this.duration = duration;
        }
    }
    
    public static class DisasterPreparedness {
        public final double economicMitigation;
        public final double resourceMitigation;
        public final double populationMitigation;
        public final double infrastructureMitigation;
        
        public DisasterPreparedness(double economicMitigation, double resourceMitigation, 
                                  double populationMitigation, double infrastructureMitigation) {
            this.economicMitigation = economicMitigation;
            this.resourceMitigation = resourceMitigation;
            this.populationMitigation = populationMitigation;
            this.infrastructureMitigation = infrastructureMitigation;
        }
        
        public double averageMitigation() {
            return (economicMitigation + resourceMitigation + populationMitigation + infrastructureMitigation) / 4.0;
        }
    }
    
    public static class DisasterDamage {
        public final String disasterId;
        public final String nationId;
        public final double economicDamage;
        public final double resourceDamage;
        public final int populationLoss;
        public final long timestamp;
        
        public DisasterDamage(String disasterId, String nationId, double economicDamage, 
                             double resourceDamage, int populationLoss, long timestamp) {
            this.disasterId = disasterId;
            this.nationId = nationId;
            this.economicDamage = economicDamage;
            this.resourceDamage = resourceDamage;
            this.populationLoss = populationLoss;
            this.timestamp = timestamp;
        }
    }
    
    public static class DisasterRecovery {
        public final String recoveryId;
        public final String disasterId;
        public final String nationId;
        public final double cost;
        public final double severity;
        public final long startTime;
        
        public DisasterRecovery(String recoveryId, String disasterId, String nationId, 
                               double cost, double severity, long startTime) {
            this.recoveryId = recoveryId;
            this.disasterId = disasterId;
            this.nationId = nationId;
            this.cost = cost;
            this.severity = severity;
            this.startTime = startTime;
        }
    }
    
    public static class DisasterAid {
        public final String aidId;
        public final String affectedNationId;
        public final String donorNationId;
        public final double amount;
        public final String resourceType;
        public final long timestamp;
        
        public DisasterAid(String aidId, String affectedNationId, String donorNationId, 
                          double amount, String resourceType, long timestamp) {
            this.aidId = aidId;
            this.affectedNationId = affectedNationId;
            this.donorNationId = donorNationId;
            this.amount = amount;
            this.resourceType = resourceType;
            this.timestamp = timestamp;
        }
    }
}