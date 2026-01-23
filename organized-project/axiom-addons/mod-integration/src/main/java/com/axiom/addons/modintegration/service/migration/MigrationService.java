package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.api.ModIntegrationAPI;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for migration system using mod entities.
 * Manages population movement, migration policies, and demographic changes using mod entities.
 */
public class MigrationService {
    private final AXIOM plugin;
    private final ModIntegrationAPI modAPI;
    private final NationManager nationManager;
    private final EconomyService economyService;
    
    private final Map<UUID, MigrationRecord> activeMigrations = new HashMap<>();
    private final Map<String, List<MigrationRecord>> nationMigrations = new HashMap<>();
    private final Map<String, MigrationPolicy> migrationPolicies = new HashMap<>();
    private final Map<String, Double> migrationFactors = new HashMap<>();
    private final Map<UUID, Long> migrationCooldowns = new HashMap<>();
    private final Set<UUID> migratingEntities = new HashSet<>();
    private final Map<String, List<PopulationChangeEvent>> populationHistory = new HashMap<>();

    public MigrationService(AXIOM plugin) {
        this.plugin = plugin;
        this.modAPI = plugin.getModIntegrationAPI();
        this.nationManager = plugin.getNationManager();
        this.economyService = plugin.getEconomyService();
        
        // Schedule periodic migration processing
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processMigrations, 0, 20 * 60 * 10); // every 10 minutes
    }

    /**
     * Register a migration attempt by a player.
     */
    public String registerMigrationAttempt(UUID playerId, String sourceNationId, String targetNationId, String reason) {
        try {
            // Check if player is in source nation
            String currentPlayerNationId = plugin.getPlayerDataManager().getNation(playerId);
            if (!sourceNationId.equals(currentPlayerNationId)) {
                return "Вы не состоите в отправляющей нации";
            }
            
            // Check if source and target are different
            if (sourceNationId.equals(targetNationId)) {
                return "Невозможно мигрировать в ту же нацию";
            }
            
            // Check cooldown
            if (isOnMigrationCooldown(playerId)) {
                return "Подождите перед следующей попыткой миграции";
            }
            
            // Check migration policy of target nation
            MigrationPolicy targetPolicy = getOrCreateMigrationPolicy(targetNationId);
            if (!targetPolicy.isOpenToMigration && !targetPolicy.allowedExceptionceptions.contains(sourceNationId)) {
                return "Целевая нация не принимает мигрантов";
            }
            
            // Check relations between nations
            double relation = checkNationRelation(sourceNationId, targetNationId);
            if (relation < targetPolicy.minimumRelationThreshold) {
                return "Недостаточные дипломатические отношения для миграции";
            }
            
            // Check if player has valid reasons/resources for migration
            double migrationCost = calculateMigrationCost(sourceNationId, targetNationId, reason);
            
            // Check if player has sufficient funds/resources
            if (!hasSufficientResources(playerId, migrationCost)) {
                return "Недостаточно средств для миграции";
            }
            
            // Create migration record
            String migrationId = generateMigrationId(playerId, sourceNationId, targetNationId);
            MigrationRecord migration = new MigrationRecord(
                migrationId,
                playerId,
                sourceNationId,
                targetNationId,
                reason,
                migrationCost,
                System.currentTimeMillis(),
                targetPolicy.processingTime,
                MigrationStatus.PENDING
            );
            
            activeMigrations.put(playerId, migration);
            nationMigrations.computeIfAbsent(sourceNationId, k -> new ArrayList<>()).add(migration);
            nationMigrations.computeIfAbsent(targetNationId, k -> new ArrayList<>()).add(migration);
            
            // Apply migration cooldown
            setMigrationCooldown(playerId, 60 * 60 * 1000); // 1 hour cooldown
            
            plugin.getLogger().info("Migration attempt registered: " + playerId + 
                                   " from " + sourceNationId + " to " + targetNationId);
            
            return "✓ Заявка на миграцию создана! Стоимость: " + migrationCost + 
                   ". Ожидайте обработки заявки...";
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering migration attempt: " + e.getMessage());
            return "Ошибка при регистрации миграции: " + e.getMessage();
        }
    }

    /**
     * Process migration using mod entities (like villagers from modded villages).
     */
    public String processEntityMigration(EntityType entityType, String sourceNationId, String targetNationId, int count) {
        try {
            // Check if entity type is supported by mod entities
            if (!isModEntityType(entityType)) {
                return "Тип сущности не поддерживается миграцией";
            }
            
            Nation sourceNation = nationManager.getNationById(sourceNationId);
            Nation targetNation = nationManager.getNationById(targetNationId);
            
            if (sourceNation == null || targetNation == null) {
                return "Нация не найдена";
            }
            
            // Get migration policy for target nation
            MigrationPolicy targetPolicy = getOrCreateMigrationPolicy(targetNationId);
            if (!targetPolicy.allowModEntityMigration) {
                return "Целевая нация не принимает мод-мигрантов";
            }
            
            // Calculate migration cost for entities
            double migrationCost = count * targetPolicy.entityMigrationBaseCost;
            
            // Check nation's ability to absorb new population
            if (!canAbsorbPopulation(targetNationId, count)) {
                return "Целевая нация не может вместить такое количество мигрантов";
            }
            
            // Perform the migration
            String migrationId = generateEntityMigrationId(sourceNationId, targetNationId, entityType);
            EntityMigrationRecord entityMigration = new EntityMigrationRecord(
                migrationId,
                entityType,
                sourceNationId,
                targetNationId,
                count,
                migrationCost,
                System.currentTimeMillis(),
                MigrationStatus.COMPLETED
            );
            
            // Update nation populations
            updateNationPopulation(targetNationId, count, MigrationDirection.ARRIVAL);
            updateNationPopulation(sourceNationId, count, MigrationDirection.DEPARTURE);
            
            // Add to history
            recordPopulationChange(targetNationId, count, PopulationChangeType.MIGRATION_ARRIVAL, "Мод-мигранты: " + entityType.name());
            recordPopulationChange(sourceNationId, -count, PopulationChangeType.MIGRATION_DEPARTURE, "Мод-мигранты: " + entityType.name());
            
            plugin.getLogger().info("Entity migration processed: " + count + " " + entityType.name() + 
                                   " from " + sourceNationId + " to " + targetNationId);
            
            return "✓ " + count + " " + entityType.name() + " успешно мигрировали!";
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing entity migration: " + e.getMessage());
            return "Ошибка при обработке миграции сущностей: " + e.getMessage();
        }
    }

    /**
     * Set migration policy for a nation.
     */
    public String setMigrationPolicy(String nationId, MigrationPolicy policy) {
        try {
            Nation nation = nationManager.getNationById(nationId);
            if (nation == null) {
                return "Нация не найдена";
            }
            
            migrationPolicies.put(nationId, policy);
            
            plugin.getLogger().info("Migration policy set for nation " + nationId + ": " + policy.isOpenToMigration);
            
            return "✓ Политика миграции обновлена!";
        } catch (Exception e) {
            plugin.getLogger().severe("Error setting migration policy: " + e.getMessage());
            return "Ошибка при установке политики: " + e.getMessage();
        }
    }

    /**
     * Get migration statistics for a nation.
     */
    public Map<String, Object> getMigrationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get all migrations related to this nation
        List<MigrationRecord> migrations = nationMigrations.getOrDefault(nationId, new ArrayList<>());
        
        // Count arrivals and departures
        int arrivals = 0;
        int departures = 0;
        double totalCost = 0.0;
        
        for (MigrationRecord migration : migrations) {
            if (migration.targetNationId.equals(nationId)) {
                arrivals++;
                if (migration.status == MigrationStatus.COMPLETED) {
                    totalCost += migration.cost;
                }
            } else if (migration.sourceNationId.equals(nationId)) {
                departures++;
            }
        }
        
        // Get nation's population change history
        List<PopulationChangeEvent> history = populationHistory.getOrDefault(nationId, new ArrayList<>());
        
        // Calculate migration factors
        double attractionFactor = calculateAttractionFactor(nationId);
        double pushFactor = calculatePushFactor(nationId);
        
        stats.put("migrationsArriving", arrivals);
        stats.put("migrationsDeparting", departures);
        stats.put("totalMigrationCost", totalCost);
        stats.put("migrationPolicy", migrationPolicies.get(nationId));
        stats.put("populationChangeHistory", history);
        stats.put("attractionFactor", attractionFactor);
        stats.put("pushFactor", pushFactor);
        stats.put("netMigration", arrivals - departures);
        stats.put("hasModEntityMigration", modAPI.hasModAvailable("immersivevehicles") || modAPI.hasModAvailable("mobs")); // Generic mod check
        stats.put("migrationTrend", calculateMigrationTrend(nationId));
        
        return stats;
    }

    /**
     * Calculate attraction factor for a nation (how attractive it is for migration).
     */
    private double calculateAttractionFactor(String nationId) {
        double baseFactor = 1.0;
        
        // Economic factors
        double treasury = economyService.getTreasury(nationId);
        baseFactor += Math.min(2.0, Math.log10(Math.max(1, treasury / 10000))); // More treasury = more attractive
        
        // Mod-based factors
        if (modAPI.hasIndustrialMods()) {
            baseFactor += 0.5; // Industrial mods indicate prosperity
        }
        if (modAPI.hasEnergyMods()) {
            baseFactor += 0.3; // Energy indicates stability
        }
        if (modAPI.hasLogisticsMods()) {
            baseFactor += 0.2; // Good logistics = good infrastructure
        }
        
        // Territory size affects capacity
        Nation nation = nationManager.getNationById(nationId);
        if (nation != null) {
            baseFactor += Math.min(2.0, nation.getClaimedChunkKeys().size() * 0.01); // More territory = more space
        }
        
        return baseFactor;
    }

    /**
     * Calculate push factor for a nation (what drives people away).
     */
    private double calculatePushFactor(String nationId) {
        double pushFactor = 0.5; // Base push factor
        
        // Negative economic factors
        double treasury = economyService.getTreasury(nationId);
        if (treasury < 1000) {
            pushFactor += 0.5; // Poor nation
        }
        
        // Check for warfare (which drives people away)
        if (modAPI.hasWarfareMods()) {
            // In a real implementation, this would check for active wars
            pushFactor += 0.3; // Warfare creates instability
        }
        
        // Check for natural disasters or negative events
        // (Would be implemented in a full system)
        
        return pushFactor;
    }

    /**
     * Check if a player has sufficient resources for migration.
     */
    private boolean hasSufficientResources(UUID playerId, double requiredCost) {
        String nationId = plugin.getPlayerDataManager().getNation(playerId);
        if (nationId == null) return false;
        
        double availableFunds = economyService.getTreasury(nationId);
        
        return availableFunds >= requiredCost;
    }

    /**
     * Calculate migration cost based on distance, reason, and nations.
     */
    private double calculateMigrationCost(String sourceNationId, String targetNationId, String reason) {
        // Base cost
        double baseCost = 100.0;
        
        // Distance factor (in a real implementation, this would use actual distances)
        baseCost *= 1.0; // Placeholder - would use real distance
        
        // Relation modifier
        double relation = checkNationRelation(sourceNationId, targetNationId);
        baseCost *= (2.0 - Math.min(1.0, Math.max(0.0, relation / 100.0))); // Better relations = lower cost
        
        // Reason modifier
        switch (reason.toLowerCase()) {
            case "economy":
                baseCost *= 1.2; // High expectations cost more
                break;
            case "safety":
                baseCost *= 0.8; // Desperate migration, lower cost
                break;
            case "opportunity":
                baseCost *= 1.1; // Opportunity-seeking costs more
                break;
            case "family":
                baseCost *= 0.9; // Family reunification favored
                break;
            default:
                baseCost *= 1.0;
        }
        
        // Target nation's migration policy cost modifier
        MigrationPolicy policy = migrationPolicies.get(targetNationId);
        if (policy != null) {
            baseCost *= policy.costMultiplier;
        }
        
        return Math.max(50.0, baseCost); // Minimum cost of 50
    }

    /**
     * Check diplomatic relation between nations.
     */
    private double checkNationRelation(String nation1Id, String nation2Id) {
        // In a real implementation, this would check actual diplomatic relations
        // For now, return a placeholder value
        return 50.0; // Neutral relation
    }

    /**
     * Check if nation can absorb more population.
     */
    private boolean canAbsorbPopulation(String nationId, int populationCount) {
        Nation nation = nationManager.getNationById(nationId);
        if (nation == null) return false;
        
        // Check territory size vs population
        long territorySize = nation.getClaimedChunkKeys().size();
        long currentPopulation = nation.getCitizens().size();
        
        // Crude calculation: 1 citizen per 2 chunks max
        return (currentPopulation + populationCount) < (territorySize * 2);
    }

    /**
     * Update nation population for migration.
     */
    private void updateNationPopulation(String nationId, int populationChange, MigrationDirection direction) {
        Nation nation = nationManager.getNationById(nationId);
        if (nation == null) return;
        
        // In a real implementation, this would update actual population data
        // For now, we'll just log the change
        
        String changeType = (direction == MigrationDirection.ARRIVAL) ? " ARRIVAL " : " DEPARTURE ";
        plugin.getLogger().info("Population change in " + nationId + ": " + changeType + populationChange);
    }

    /**
     * Record population change.
     */
    private void recordPopulationChange(String nationId, int changeAmount, PopulationChangeType changeType, String reason) {
        PopulationChangeEvent event = new PopulationChangeEvent(
            "popchange_" + System.currentTimeMillis(),
            nationId,
            changeAmount,
            changeType,
            reason,
            System.currentTimeMillis()
        );
        
        populationHistory.computeIfAbsent(nationId, k -> new ArrayList<>()).add(event);
    }

    /**
     * Check if entity type is from mods.
     */
    private boolean isModEntityType(EntityType entityType) {
        // In a real implementation, this would check if the entity type is from a mod
        // For now, we'll consider some common entity types as potentially modded
        switch (entityType) {
            case VILLAGER:
                // Villagers can be from mod villages
                return true;
            case ZOMBIE_VILLAGER:
                return true;
            case ILLUSIONER:
                return true;
            case RAVAGER:
                return true;
            case PANDA:
                return true;
            default:
                // Check if there's a mod that could add this entity type
                return modAPI.hasModAvailable("mobs") || modAPI.hasModAvailable("animals") || modAPI.hasModAvailable("moo");
        }
    }

    /**
     * Calculate migration trend for a nation.
     */
    private String calculateMigrationTrend(String nationId) {
        List<MigrationRecord> migrations = nationMigrations.getOrDefault(nationId, new ArrayList<>());
        
        if (migrations.isEmpty()) {
            return "STABLE";
        }
        
        int arrivalCount = 0;
        int departureCount = 0;
        
        long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        
        for (MigrationRecord migration : migrations) {
            if (migration.timestamp > weekAgo) {
                if (migration.targetNationId.equals(nationId)) {
                    arrivalCount++;
                } else if (migration.sourceNationId.equals(nationId)) {
                    departureCount++;
                }
            }
        }
        
        int netChange = arrivalCount - departureCount;
        
        if (netChange > 5) return "STRONG_INFLUX";
        if (netChange > 1) return "MODERATE_INFLUX";
        if (netChange < -5) return "STRONG_OUTFLUX";
        if (netChange < -1) return "MODERATE_OUTFLUX";
        return "STABLE";
    }

    /**
     * Apply immigration policy to a migration request.
     */
    public String applyMigrationPolicy(String migrationId, boolean approve) {
        try {
            MigrationRecord migration = null;
            for (MigrationRecord rec : activeMigrations.values()) {
                if (rec.migrationId.equals(migrationId)) {
                    migration = rec;
                    break;
                }
            }
            
            if (migration == null) {
                return "Миграция не найдена";
            }
            
            if (approve) {
                // Process the migration
                migration.status = MigrationStatus.APPROVED;
                
                // Move player to new nation
                Player player = Bukkit.getPlayer(migration.playerId);
                if (player != null) {
                    // Remove from old nation
                    // Add to new nation
                    plugin.getPlayerDataManager().setNation(migration.playerId, migration.targetNationId, "CITIZEN");
                    
                    player.sendMessage("§a[Миграция] §fВаша заявка на миграцию одобрена!");
                    player.sendMessage("§7Вы теперь являетесь гражданином нации §e" + migration.targetNationId + "§f.");
                    
                    // Apply migration cost
                    String sourceNationId = plugin.getPlayerDataManager().getNation(migration.playerId);
                    economyService.deposit(sourceNationId, -migration.cost);
                    
                    // Add to population history
                    recordPopulationChange(migration.targetNationId, 1, PopulationChangeType.MIGRATION_ARRIVAL, "Гражданская миграция");
                    recordPopulationChange(migration.sourceNationId, -1, PopulationChangeType.MIGRATION_DEPARTURE, "Гражданская миграция");
                }
            } else {
                migration.status = MigrationStatus.REJECTED;
                
                Player player = Bukkit.getPlayer(migration.playerId);
                if (player != null) {
                    player.sendMessage("§c[Миграция] §fВаша заявка на миграцию отклонена.");
                    player.sendMessage("§7Причина: §e" + migration.reason);
                }
            }
            
            return approve ? "✓ Миграция одобрена!" : "✓ Миграция отклонена!";
        } catch (Exception e) {
            plugin.getLogger().severe("Error applying migration policy: " + e.getMessage());
            return "Ошибка при обработке миграции: " + e.getMessage();
        }
    }

    /**
     * Process pending migrations periodically.
     */
    private void processMigrations() {
        long currentTime = System.currentTimeMillis();
        
        for (Iterator<Map.Entry<UUID, MigrationRecord>> it = activeMigrations.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, MigrationRecord> entry = it.next();
            MigrationRecord migration = entry.getValue();
            
            if (migration.status == MigrationStatus.PENDING && 
                (currentTime - migration.timestamp) > migration.processingTime) {
                
                // Automatically approve or reject based on policy
                MigrationPolicy policy = getOrCreateMigrationPolicy(migration.targetNationId);
                
                // Simulate approval decision based on factors
                boolean approve = shouldAutoApprove(migration, policy);
                
                if (approve) {
                    migration.status = MigrationStatus.APPROVED;
                    
                    // Process the migration
                    Player player = Bukkit.getPlayer(migration.playerId);
                    if (player != null) {
                        plugin.getPlayerDataManager().setNation(migration.playerId, migration.targetNationId, "CITIZEN");
                        
                        player.sendMessage("§a[Миграция] §fВаша заявка на миграцию автоматически одобрена!");
                        player.sendMessage("§7Вы теперь являетесь гражданином нации §e" + migration.targetNationId + "§f.");
                        
                        // Apply migration cost
                        String sourceNationId = plugin.getPlayerDataManager().getNation(migration.playerId);
                        if (sourceNationId != null) {
                            economyService.deposit(sourceNationId, -migration.cost);
                            
                            // Update population
                            recordPopulationChange(migration.targetNationId, 1, PopulationChangeType.MIGRATION_ARRIVAL, "Гражданская миграция");
                            recordPopulationChange(migration.sourceNationId, -1, PopulationChangeType.MIGRATION_DEPARTURE, "Гражданская миграция");
                        }
                    }
                } else {
                    migration.status = MigrationStatus.REJECTED;
                    
                    Player player = Bukkit.getPlayer(migration.playerId);
                    if (player != null) {
                        player.sendMessage("§c[Миграция] §fВаша заявка на миграция автоматически отклонена.");
                    }
                }
                
                // Remove processed migration
                it.remove();
            }
        }
        
        // Clean up old migration records from nation records
        for (List<MigrationRecord> migrations : nationMigrations.values()) {
            migrations.removeIf(migration -> 
                migration.status != MigrationStatus.PENDING || 
                (currentTime - migration.timestamp) > (24 * 60 * 60 * 1000) // Keep for 24 hours
            );
        }
    }

    /**
     * Determine if a migration should be auto-approved based on policy and factors.
     */
    private boolean shouldAutoApprove(MigrationRecord migration, MigrationPolicy policy) {
        // Check if policy allows auto-approval
        if (!policy.autoApproveEnabled) {
            return false; // Manual approval required
        }
        
        // Check relation threshold
        double relation = checkNationRelation(migration.sourceNationId, migration.targetNationId);
        if (relation < policy.minimumRelationThreshold) {
            return false;
        }
        
        // Check if it's a special exception
        if (policy.allowedExceptionceptions.contains(migration.sourceNationId)) {
            return true;
        }
        
        // Apply random factor for realism
        return Math.random() < policy.approvalRate;
    }

    /**
     * Check if player is on migration cooldown.
     */
    private boolean isOnMigrationCooldown(UUID playerId) {
        Long lastMigration = migrationCooldowns.get(playerId);
        if (lastMigration == null) return false;
        
        return (System.currentTimeMillis() - lastMigration) < getMigrationCooldownPeriod();
    }

    /**
     * Set migration cooldown for a player.
     */
    private void setMigrationCooldown(UUID playerId, long period) {
        migrationCooldowns.put(playerId, System.currentTimeMillis() + period);
    }

    /**
     * Get migration cooldown period.
     */
    private long getMigrationCooldownPeriod() {
        return 60 * 60 * 1000; // 1 hour in milliseconds
    }

    /**
     * Get or create migration policy for a nation.
     */
    private MigrationPolicy getOrCreateMigrationPolicy(String nationId) {
        return migrationPolicies.computeIfAbsent(nationId, k -> 
            new MigrationPolicy(
                true, // Open to migration by default
                0.7,  // 70% approval rate
                1.0,  // Cost multiplier
                0,    // No minimum relation threshold
                new ArrayList<>(), // No exceptions
                true, // Auto approval enabled
                1000, // Base cost for entities
                false // Don't allow mod entity migration by default
            ));
    }

    /**
     * Generate unique migration ID.
     */
    private String generateMigrationId(UUID playerId, String sourceId, String targetId) {
        return "migration_" + playerId.toString().substring(0, 8) + 
               "_from_" + sourceId + "_to_" + targetId + "_" + System.currentTimeMillis();
    }

    /**
     * Generate unique entity migration ID.
     */
    private String generateEntityMigrationId(String sourceId, String targetId, EntityType entityType) {
        return "entity_migration_" + entityType.name() + 
               "_from_" + sourceId + "_to_" + targetId + "_" + System.currentTimeMillis();
    }

    /**
     * Get migration status for a player.
     */
    public MigrationRecord getPlayerMigrationStatus(UUID playerId) {
        return activeMigrations.get(playerId);
    }

    /**
     * Get pending migrations for a nation.
     */
    public List<MigrationRecord> getPendingMigrations(String nationId) {
        List<MigrationRecord> pending = new ArrayList<>();
        
        for (MigrationRecord migration : nationMigrations.getOrDefault(nationId, new ArrayList<>())) {
            if (migration.status == MigrationStatus.PENDING && migration.targetNationId.equals(nationId)) {
                pending.add(migration);
            }
        }
        
        return pending;
    }

    // Data classes for migration system structures
    
    public enum MigrationStatus {
        PENDING, APPROVED, REJECTED, COMPLETED, FAILED
    }
    
    public enum MigrationDirection {
        ARRIVAL, DEPARTURE
    }
    
    public enum PopulationChangeType {
        MIGRATION_ARRIVAL, MIGRATION_DEPARTURE, BIRTH, DEATH, NATURAL_GROWTH
    }
    
    public static class MigrationRecord {
        public final String migrationId;
        public final UUID playerId;
        public final String sourceNationId;
        public final String targetNationId;
        public final String reason;
        public final double cost;
        public final long timestamp;
        public final long processingTime;
        public MigrationStatus status;
        
        public MigrationRecord(String migrationId, UUID playerId, String sourceNationId, 
                              String targetNationId, String reason, double cost, 
                              long timestamp, long processingTime, MigrationStatus status) {
            this.migrationId = migrationId;
            this.playerId = playerId;
            this.sourceNationId = sourceNationId;
            this.targetNationId = targetNationId;
            this.reason = reason;
            this.cost = cost;
            this.timestamp = timestamp;
            this.processingTime = processingTime;
            this.status = status;
        }
    }
    
    public static class EntityMigrationRecord {
        public final String migrationId;
        public final EntityType entityType;
        public final String sourceNationId;
        public final String targetNationId;
        public final int count;
        public final double cost;
        public final long timestamp;
        public final MigrationStatus status;
        
        public EntityMigrationRecord(String migrationId, EntityType entityType, String sourceNationId, 
                                   String targetNationId, int count, double cost, 
                                   long timestamp, MigrationStatus status) {
            this.migrationId = migrationId;
            this.entityType = entityType;
            this.sourceNationId = sourceNationId;
            this.targetNationId = targetNationId;
            this.count = count;
            this.cost = cost;
            this.timestamp = timestamp;
            this.status = status;
        }
    }
    
    public static class MigrationPolicy {
        public boolean isOpenToMigration;
        public double approvalRate;
        public double costMultiplier;
        public double minimumRelationThreshold;
        public List<String> allowedExceptionceptions;
        public boolean autoApproveEnabled;
        public double entityMigrationBaseCost;
        public boolean allowModEntityMigration;
        
        public MigrationPolicy(boolean isOpenToMigration, double approvalRate, 
                              double costMultiplier, double minimumRelationThreshold,
                              List<String> allowedExceptionceptions, boolean autoApproveEnabled,
                              double entityMigrationBaseCost, boolean allowModEntityMigration) {
            this.isOpenToMigration = isOpenToMigration;
            this.approvalRate = approvalRate;
            this.costMultiplier = costMultiplier;
            this.minimumRelationThreshold = minimumRelationThreshold;
            this.allowedExceptionceptions = allowedExceptionceptions != null ? allowedExceptionceptions : new ArrayList<>();
            this.autoApproveEnabled = autoApproveEnabled;
            this.entityMigrationBaseCost = entityMigrationBaseCost;
            this.allowModEntityMigration = allowModEntityMigration;
        }
    }
    
    public static class PopulationChangeEvent {
        public final String eventId;
        public final String nationId;
        public final int changeAmount;
        public final PopulationChangeType changeType;
        public final String reason;
        public final long timestamp;
        
        public PopulationChangeEvent(String eventId, String nationId, int changeAmount, 
                                   PopulationChangeType changeType, String reason, long timestamp) {
            this.eventId = eventId;
            this.nationId = nationId;
            this.changeAmount = changeAmount;
            this.changeType = changeType;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
}