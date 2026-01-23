package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.api.ModIntegrationAPI;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for spy system using mod-based stealth mechanics.
 * Enables espionage operations using mod equipment and abilities.
 */
public class SpyService {
    private final AXIOM plugin;
    private final ModIntegrationAPI modAPI;
    private final NationManager nationManager;
    private final DiplomacySystem diplomacySystem;
    
    private final Map<UUID, SpyAgent> activeAgents = new HashMap<>();
    private final Map<String, List<SpyMission>> nationMissions = new HashMap<>();
    private final Map<UUID, Long> agentCooldowns = new HashMap<>();
    private final Map<String, List<SpyReport>> intelligenceReports = new HashMap<>();
    private final Map<UUID, String> agentInfiltrationTargets = new HashMap<>();

    public SpyService(AXIOM plugin) {
        this.plugin = plugin;
        this.modAPI = plugin.getModIntegrationAPI();
        this.nationManager = plugin.getNationManager();
        this.diplomacySystem = plugin.getDiplomacySystem();
        
        // Schedule periodic mission updates
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateMissions, 0, 20 * 30); // every 30 seconds
    }

    /**
     * Recruit a spy agent.
     */
    public String recruitAgent(Player player) {
        try {
            UUID playerId = player.getUniqueId();
            String nationId = plugin.getPlayerDataManager().getNation(playerId);
            
            if (nationId == null) {
                return "Вы не состоите в нации";
            }
            
            // Check if player is already an agent
            if (activeAgents.containsKey(playerId)) {
                return "Вы уже являетесь агентом";
            }
            
            // Check cooldown
            if (isOnCooldown(playerId)) {
                return "Подождите перед следующим набором агента";
            }
            
            // Check requirements - player should have some mod equipment
            boolean hasStealthEquipment = hasStealthEquipment(player);
            boolean hasSpecializedItems = hasSpecializedItems(player);
            
            if (!hasStealthEquipment && !hasSpecializedItems) {
                return "Для агентской деятельности требуются мод-предметы (камуфляж, тактическое снаряжение)";
            }
            
            // Create new spy agent
            SpyAgent agent = new SpyAgent(
                playerId,
                nationId,
                System.currentTimeMillis(),
                hasStealthEquipment ? 1.5 : 1.0, // Stealth bonus
                hasSpecializedItems ? 1.3 : 1.0  // Equipment bonus
            );
            
            activeAgents.put(playerId, agent);
            setCooldown(playerId, 60000); // 1 minute cooldown
            
            player.sendMessage("§a[Разведка] §fВы были завербованы как агент! Используйте §b/spy §fдля заданий");
            
            return "✓ Агент завербован! Используйте /spy для управления разведкой";
        } catch (Exception e) {
            plugin.getLogger().severe("Error recruiting spy agent: " + e.getMessage());
            return "Ошибка при вербовке агента: " + e.getMessage();
        }
    }

    /**
     * Start a spy mission using mod-based stealth.
     */
    public String startMission(Player player, String targetNationId, String missionType) {
        try {
            UUID playerId = player.getUniqueId();
            SpyAgent agent = activeAgents.get(playerId);
            
            if (agent == null) {
                return "Вы не являетесь агентом";
            }
            
            if (agentInfiltrationTargets.containsKey(playerId)) {
                return "Вы уже находитесь в режиме инфильтрации";
            }
            
            // Validate mission
            if (!isValidMissionType(missionType)) {
                return "Неверный тип задания";
            }
            
            // Check if player has required stealth equipment
            double stealthLevel = calculateStealthLevel(player);
            double missionDifficulty = getMissionDifficulty(missionType);
            
            if (stealthLevel < missionDifficulty) {
                return "Недостаточный уровень скрытности для этого задания";
            }
            
            // Create mission
            String missionId = generateMissionId(playerId, targetNationId);
            SpyMission mission = new SpyMission(
                missionId,
                playerId,
                agent.nationId,
                targetNationId,
                missionType,
                System.currentTimeMillis(),
                estimateMissionDuration(missionType),
                stealthLevel
            );
            
            // Add to nation missions
            nationMissions.computeIfAbsent(agent.nationId, k -> new ArrayList<>()).add(mission);
            
            // Activate stealth effects
            applyStealthEffects(player, missionType);
            
            // Mark player as infiltrating
            agentInfiltrationTargets.put(playerId, targetNationId);
            
            player.sendMessage("§a[Разведка] §fЗадание начато: §e" + missionType + " §fв нации §c" + targetNationId);
            player.sendMessage("§7Текущий уровень скрытности: §f" + String.format("%.2f", stealthLevel));
            
            plugin.getLogger().info("Agent " + player.getName() + " started mission " + missionType + 
                                   " in nation " + targetNationId);
            
            return "✓ Задание начато! Будьте осторожны...";
        } catch (Exception e) {
            plugin.getLogger().severe("Error starting spy mission: " + e.getMessage());
            return "Ошибка при начале задания: " + e.getMessage();
        }
    }

    /**
     * Perform spy action using mod equipment.
     */
    public String performSpyAction(Player player, String action) {
        try {
            UUID playerId = player.getUniqueId();
            String nationId = plugin.getPlayerDataManager().getNation(playerId);
            
            if (nationId == null || !activeAgents.containsKey(playerId)) {
                return "Вы не являетесь агентом";
            }
            
            // Check if using mod equipment for enhanced action
            boolean hasEnhancedEquipment = isUsingEnhancedEquipment(player);
            double effectiveness = hasEnhancedEquipment ? 1.5 : 1.0;
            
            // Apply action based on mod capabilities
            String result = executeAction(player, action, effectiveness);
            
            // If action involves detection risk, calculate detection chance
            double detectionChance = calculateDetectionChance(player, action);
            if (Math.random() < detectionChance) {
                handleDetection(player, action);
            }
            
            return result;
        } catch (Exception e) {
            plugin.getLogger().severe("Error performing spy action: " + e.getMessage());
            return "Ошибка при выполнении действия: " + e.getMessage();
        }
    }

    /**
     * Calculate player's stealth level based on mod equipment.
     */
    private double calculateStealthLevel(Player player) {
        double stealthLevel = 1.0; // Base stealth
        
        // Check armor for camouflage
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                String modId = modAPI.detectModFromItem(armor);
                if (modId != null) {
                    switch (modId.toLowerCase()) {
                        case "capsawims":
                        case "warium":
                            stealthLevel += 0.5; // Tactical camouflage
                            break;
                        case "curios":
                            // Some curios might provide stealth bonuses
                            stealthLevel += 0.2;
                            break;
                        default:
                            stealthLevel += 0.1; // General mod equipment
                    }
                }
            }
        }
        
        // Check for potion effects that enhance stealth
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.INVISIBILITY) ||
                effect.getType().equals(PotionEffectType.SLOW)) {
                stealthLevel += 0.3;
            }
        }
        
        // Check for specific stealth items in main inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String modId = modAPI.detectModFromItem(item);
                if (modId != null) {
                    // Specific mod items might provide stealth bonuses
                    stealthLevel += 0.1;
                }
            }
        }
        
        return Math.min(5.0, stealthLevel); // Cap at 5.0
    }

    /**
     * Apply stealth effects based on mission type.
     */
    private void applyStealthEffects(Player player, String missionType) {
        // In a real implementation, this would apply actual potion effects or visual changes
        // For now, we'll just send a message to the player
        
        switch (missionType) {
            case "recon":
                // Recon missions might provide night vision or other senses
                player.sendMessage("§7[Скрыто] §fВаши чувства обострены...");
                break;
            case "infiltration":
                // Infiltration might provide invisibility or camouflage
                player.sendMessage("§7[Скрыто] §fВы становитесь менее заметным...");
                break;
            case "extraction":
                // Extraction missions might provide speed boost
                player.sendMessage("§7[Скрыто] §fВы готовы к быстрому отходу...");
                break;
        }
    }

    /**
     * Execute specific spy action.
     */
    private String executeAction(Player player, String action, double effectiveness) {
        UUID playerId = player.getUniqueId();
        String playerNationId = plugin.getPlayerDataManager().getNation(playerId);
        
        switch (action.toLowerCase()) {
            case "gather_intel":
                // Gather intelligence from the area
                return gatherIntelligence(player, playerNationId, effectiveness);
            case "sabotage":
                // Attempt to sabotage something (would need specific targets)
                return "Попытка саботажа... (реализация требует больше контекста)";
            case "plant_device":
                // Plant surveillance device
                return "Установка наблюдательного устройства... (реализация требует мод-механик)";
            case "observe":
                // Observe the area for valuable information
                return observeArea(player, effectiveness);
            default:
                return "Неизвестное действие: " + action;
        }
    }

    /**
     * Gather intelligence from the area.
     */
    private String gatherIntelligence(Player player, String agentNationId, double effectiveness) {
        Location location = player.getLocation();
        String targetNationId = getNationAtLocation(location);
        
        if (targetNationId == null) {
            return "Вы не находитесь на территории чужой нации";
        }
        
        // Calculate intelligence gathered based on effectiveness and location
        double intelligenceValue = 10.0 * effectiveness;
        
        // Create spy report
        String reportId = "intel_" + agentNationId + "_" + targetNationId + "_" + System.currentTimeMillis();
        SpyReport report = new SpyReport(
            reportId,
            agentNationId,
            targetNationId,
            "Интеллектуальные данные",
            "Собранные данные о нации " + targetNationId + " вблизи " + location.getBlockX() + "," + location.getZ(),
            intelligenceValue,
            System.currentTimeMillis()
        );
        
        intelligenceReports.computeIfAbsent(agentNationId, k -> new ArrayList<>()).add(report);
        
        // Bonus for mod-based gathering tools
        if (isUsingEnhancedEquipment(player)) {
            player.sendMessage("§a[Разведка] §fСпециализированные мод-инструменты увеличили качество данных!");
        }
        
        player.sendMessage("§a[Разведка] §fСобраны данные о нации §c" + targetNationId + 
                          "§f. Качество: §e" + String.format("%.1f", intelligenceValue));
        
        return "✓ Интеллектуальные данные собраны";
    }

    /**
     * Observe the area for valuable information.
     */
    private String observeArea(Player player, double effectiveness) {
        String targetNationId = getNationAtLocation(player.getLocation());
        
        if (targetNationId == null) {
            return "Вы не находитесь на территории чужой нации";
        }
        
        // Check for nearby players to observe
        int observedPlayers = 0;
        double intelligenceValue = 0.0;
        
        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (player.getLocation().distance(nearbyPlayer.getLocation()) < 20) {
                String playerNation = plugin.getPlayerDataManager().getNation(nearbyPlayer.getUniqueId());
                if (playerNation != null && playerNation.equals(targetNationId)) {
                    observedPlayers++;
                    intelligenceValue += 5.0 * effectiveness;
                }
            }
        }
        
        String observationReport = "Наблюдение: обнаружено " + observedPlayers + " игроков из нации " + targetNationId;
        
        // Create observation report
        String agentNationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        String reportId = "obs_" + agentNationId + "_" + targetNationId + "_" + System.currentTimeMillis();
        SpyReport report = new SpyReport(
            reportId,
            agentNationId,
            targetNationId,
            "Наблюдательный отчет",
            observationReport,
            intelligenceValue,
            System.currentTimeMillis()
        );
        
        intelligenceReports.computeIfAbsent(agentNationId, k -> new ArrayList<>()).add(report);
        
        player.sendMessage("§a[Разведка] §fНаблюдение завершено. Обнаружено §c" + observedPlayers + 
                          " §fагентов врага. Информация: §e" + String.format("%.1f", intelligenceValue));
        
        return "✓ Наблюдение завершено";
    }

    /**
     * Check if player is using enhanced equipment from mods.
     */
    private boolean isUsingEnhancedEquipment(Player player) {
        // Check if player has any mod items that would enhance spy activities
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String modId = modAPI.detectModFromItem(item);
                if (modId != null) {
                    // Specific mods that would be beneficial for espionage
                    if (modId.equals("capsawims") || modId.equals("curios") || modId.equals("warium")) {
                        return true;
                    }
                }
            }
        }
        
        // Check armor as well
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                String modId = modAPI.detectModFromItem(armor);
                if (modId != null) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if player has stealth equipment.
     */
    private boolean hasStealthEquipment(Player player) {
        // Check for camouflage or stealth-enhancing equipment
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                String modId = modAPI.detectModFromItem(armor);
                if (modId != null) {
                    return true; // Any mod armor could be considered stealth equipment
                }
            }
        }
        return false;
    }

    /**
     * Check if player has specialized spy items.
     */
    private boolean hasSpecializedItems(Player player) {
        // For now, consider any mod item as specialized
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String modId = modAPI.detectModFromItem(item);
                if (modId != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calculate detection chance based on action and equipment.
     */
    private double calculateDetectionChance(Player player, String action) {
        double baseChance = 0.1; // 10% base chance
        
        // Higher chance for more risky actions
        switch (action.toLowerCase()) {
            case "sabotage":
                baseChance = 0.3; // 30% chance for sabotage
                break;
            case "plant_device":
                baseChance = 0.2; // 20% chance for planting
                break;
            case "gather_intel":
            case "observe":
            default:
                baseChance = 0.1; // 10% chance for observation
        }
        
        // Reduce chance based on stealth level
        double stealthLevel = calculateStealthLevel(player);
        double detectionChance = baseChance / stealthLevel;
        
        // Cap between 0 and 0.8 (80% max detection chance)
        return Math.min(0.8, Math.max(0, detectionChance));
    }

    /**
     * Handle when agent is detected.
     */
    private void handleDetection(Player player, String action) {
        UUID playerId = player.getUniqueId();
        String agentNationId = plugin.getPlayerDataManager().getNation(playerId);
        
        if (agentNationId == null) return;
        
        // Find the target nation to report to
        String targetNationId = agentInfiltrationTargets.get(playerId);
        if (targetNationId == null) return;
        
        // Send alert to target nation
        sendDetectionAlert(targetNationId, agentNationId, action);
        
        // Apply consequences
        applyDetectionConsequences(player, targetNationId);
        
        // End infiltration
        agentInfiltrationTargets.remove(playerId);
        
        plugin.getLogger().info("Agent " + player.getName() + " was detected while performing " + action);
    }

    /**
     * Send detection alert to target nation.
     */
    private void sendDetectionAlert(String targetNationId, String agentNationId, String action) {
        // In a real implementation, this would notify target nation leadership
        plugin.getLogger().info("Detection alert: Agent from " + agentNationId + 
                               " was detected performing " + action + " in " + targetNationId);
        
        // Update diplomatic relations
        double relationPenalty = -10.0;
        // In real implementation: diplomacySystem.adjustReputation(agentNationId, targetNationId, relationPenalty);
    }

    /**
     * Apply consequences of detection.
     */
    private void applyDetectionConsequences(Player player, String targetNationId) {
        // Teleport player away and potentially apply penalties
        player.teleport(findSafeLocation(player));
        player.sendMessage("§c[ВНИМАНИЕ] §fВы были замечены агентом нации §e" + targetNationId + "§c!");
        player.sendMessage("§cВаша инфильтрация провалена, и вы были отозваны.");
    }

    /**
     * Find a safe location to teleport detected agent.
     */
    private Location findSafeLocation(Player player) {
        // Return player to their nation's spawn or a safe location
        String playerNationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (playerNationId != null) {
            // In a real implementation, this would return to nation spawn
            return player.getWorld().getSpawnLocation();
        }
        return player.getWorld().getSpawnLocation();
    }

    /**
     * Get nation at specific location.
     */
    private String getNationAtLocation(Location location) {
        // In a real implementation, this would check the nation manager
        // For now, we'll return a placeholder
        return plugin.getNationManager().getNationAtLocation(location).map(Nation::getId).orElse(null);
    }

    /**
     * Check if mission type is valid.
     */
    private boolean isValidMissionType(String missionType) {
        return Arrays.asList("recon", "infiltration", "extraction", "sabotage", "observation").contains(missionType.toLowerCase());
    }

    /**
     * Get mission difficulty multiplier.
     */
    private double getMissionDifficulty(String missionType) {
        switch (missionType.toLowerCase()) {
            case "sabotage":
                return 2.5; // Most difficult
            case "infiltration":
                return 2.0;
            case "extraction":
                return 1.8;
            case "recon":
                return 1.0; // Easiest
            case "observation":
                return 1.2;
            default:
                return 1.5; // Default
        }
    }

    /**
     * Estimate mission duration based on type and difficulty.
     */
    private long estimateMissionDuration(String missionType) {
        switch (missionType.toLowerCase()) {
            case "recon":
                return 30000; // 30 seconds
            case "observation":
                return 45000; // 45 seconds
            case "infiltration":
                return 90000; // 90 seconds
            case "extraction":
                return 120000; // 120 seconds
            case "sabotage":
                return 150000; // 150 seconds
            default:
                return 60000; // 60 seconds default
        }
    }

    /**
     * Generate unique mission ID.
     */
    private String generateMissionId(UUID agentId, String targetNationId) {
        return "mission_" + agentId.toString().substring(0, 8) + "_" + targetNationId + "_" + System.currentTimeMillis();
    }

    /**
     * Check if player is on cooldown.
     */
    private boolean isOnCooldown(UUID playerId) {
        Long lastAction = agentCooldowns.get(playerId);
        return lastAction != null && (System.currentTimeMillis() - lastAction) < 30000; // 30 seconds
    }

    /**
     * Set cooldown for player.
     */
    private void setCooldown(UUID playerId, long duration) {
        agentCooldowns.put(playerId, System.currentTimeMillis() + duration);
    }

    /**
     * Update missions periodically.
     */
    private void updateMissions() {
        long currentTime = System.currentTimeMillis();
        
        // Check for completed missions
        List<String> completedMissions = new ArrayList<>();
        
        for (Map.Entry<String, List<SpyMission>> entry : nationMissions.entrySet()) {
            List<SpyMission> missions = entry.getValue();
            Iterator<SpyMission> iterator = missions.iterator();
            
            while (iterator.hasNext()) {
                SpyMission mission = iterator.next();
                if (currentTime > (mission.startTime + mission.duration)) {
                    completeMission(mission);
                    completedMissions.add(mission.missionId);
                    iterator.remove();
                }
            }
        }
        
        // Update cooldowns
        Iterator<Map.Entry<UUID, Long>> cooldownIterator = agentCooldowns.entrySet().iterator();
        while (cooldownIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = cooldownIterator.next();
            if (currentTime > entry.getValue()) {
                cooldownIterator.remove();
            }
        }
    }

    /**
     * Complete a mission and generate results.
     */
    private void completeMission(SpyMission mission) {
        Player agentPlayer = Bukkit.getPlayer(mission.agentId);
        if (agentPlayer == null) return;
        
        // Calculate success based on stealth level and mission difficulty
        double successChance = Math.min(1.0, mission.stealthLevel / getMissionDifficulty(mission.type));
        boolean successful = Math.random() < successChance;
        
        if (successful) {
            // Generate intelligence report
            String reportId = "mission_" + mission.missionId + "_" + System.currentTimeMillis();
            SpyReport report = new SpyReport(
                reportId,
                mission.originNationId,
                mission.targetNationId,
                "Отчет по миссии: " + mission.type,
                "Миссия " + mission.type + " в нации " + mission.targetNationId + " выполнена успешно",
                20.0, // Base intelligence value
                System.currentTimeMillis()
            );
            
            intelligenceReports.computeIfAbsent(mission.originNationId, k -> new ArrayList<>()).add(report);
            
            agentPlayer.sendMessage("§a[Разведка] §fМиссия §e" + mission.type + " §fуспешно выполнена!");
            agentPlayer.sendMessage("§7Получена ценная разведывательная информация!");
        } else {
            // Mission failed, may result in detection
            if (Math.random() < 0.3) { // 30% chance of detection upon failure
                handleDetection(agentPlayer, mission.type);
            } else {
                agentPlayer.sendMessage("§c[Разведка] §fМиссия §e" + mission.type + " §fпровалена!");
            }
        }
    }

    /**
     * Get active agent information.
     */
    public Map<String, Object> getAgentStatus(UUID playerId) {
        SpyAgent agent = activeAgents.get(playerId);
        if (agent == null) {
            return null;
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("playerId", agent.playerId);
        status.put("nationId", agent.nationId);
        status.put("recruitmentTime", agent.recruitmentTime);
        status.put("stealthBonus", agent.stealthBonus);
        status.put("equipmentBonus", agent.equipmentBonus);
        status.put("infiltrationTarget", agentInfiltrationTargets.get(playerId));
        status.put("isOnMission", agentInfiltrationTargets.containsKey(playerId));
        status.put("stealthLevel", calculateStealthLevel(Bukkit.getPlayer(playerId)));
        
        return status;
    }

    /**
     * Get intelligence reports for a nation.
     */
    public List<SpyReport> getIntelligenceReports(String nationId) {
        return intelligenceReports.getOrDefault(nationId, new ArrayList<>());
    }

    /**
     * Get active missions for a nation.
     */
    public List<SpyMission> getActiveMissions(String nationId) {
        List<SpyMission> missions = nationMissions.getOrDefault(nationId, new ArrayList<>());
        return new ArrayList<>(missions); // Return copy to prevent modification
    }

    // Data classes for spy system structures
    
    public static class SpyAgent {
        public final UUID playerId;
        public final String nationId;
        public final long recruitmentTime;
        public final double stealthBonus;
        public final double equipmentBonus;
        
        public SpyAgent(UUID playerId, String nationId, long recruitmentTime, 
                       double stealthBonus, double equipmentBonus) {
            this.playerId = playerId;
            this.nationId = nationId;
            this.recruitmentTime = recruitmentTime;
            this.stealthBonus = stealthBonus;
            this.equipmentBonus = equipmentBonus;
        }
    }
    
    public static class SpyMission {
        public final String missionId;
        public final UUID agentId;
        public final String originNationId;
        public final String targetNationId;
        public final String type;
        public final long startTime;
        public final long duration;
        public final double stealthLevel;
        
        public SpyMission(String missionId, UUID agentId, String originNationId, 
                         String targetNationId, String type, long startTime, 
                         long duration, double stealthLevel) {
            this.missionId = missionId;
            this.agentId = agentId;
            this.originNationId = originNationId;
            this.targetNationId = targetNationId;
            this.type = type;
            this.startTime = startTime;
            this.duration = duration;
            this.stealthLevel = stealthLevel;
        }
    }
    
    public static class SpyReport {
        public final String reportId;
        public final String originNationId;
        public final String targetNationId;
        public final String title;
        public final String content;
        public final double intelligenceValue;
        public final long timestamp;
        
        public SpyReport(String reportId, String originNationId, String targetNationId,
                        String title, String content, double intelligenceValue, long timestamp) {
            this.reportId = reportId;
            this.originNationId = originNationId;
            this.targetNationId = targetNationId;
            this.title = title;
            this.content = content;
            this.intelligenceValue = intelligenceValue;
            this.timestamp = timestamp;
        }
    }
}