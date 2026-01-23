package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.api.ModIntegrationAPI;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for cyber security integration with computer mods (CC/OC).
 * Manages digital security, network protection, and cyber warfare using mod computers.
 */
public class CyberSecurityService {
    private final AXIOM plugin;
    private final ModIntegrationAPI modAPI;
    private final NationManager nationManager;
    
    private final Map<String, NetworkNode> networkNodes = new HashMap<>();
    private final Map<String, CyberSecurityProfile> nationSecurityProfiles = new HashMap<>();
    private final Map<String, List<CyberAttack>> cyberAttacks = new HashMap<>();
    private final Map<String, List<CyberDefense>> cyberDefenses = new HashMap<>();
    private final Map<String, Long> lastSecurityUpdate = new HashMap<>();
    private final Map<String, List<SecurityLogEntry>> securityLogs = new HashMap<>();

    public CyberSecurityService(AXIOM plugin) {
        this.plugin = plugin;
        this.modAPI = plugin.getModIntegrationAPI();
        this.nationManager = plugin.getNationManager();
        
        // Schedule periodic security updates
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateSecurity, 0, 20 * 60 * 5); // every 5 minutes
    }

    /**
     * Register a cyber security node (computer/terminal) in a nation's network.
     */
    public String registerNetworkNode(String nationId, Block block, String nodeType, String description) {
        try {
            Nation nation = nationManager.getNationById(nationId);
            if (nation == null) {
                return "Нация не найдена";
            }
            
            // Verify the block is a computer from CC or OC
            String modId = modAPI.detectModFromBlock(block);
            if (modId == null || 
                (!modId.equalsIgnoreCase("computer") && 
                 !modId.equalsIgnoreCase("oc") && 
                 !modId.equalsIgnoreCase("computercraft") && 
                 !modId.equalsIgnoreCase("opencomputers"))) {
                return "Блок не является компьютером из поддерживаемых модов";
            }
            
            String nodeId = generateNodeId(nationId, block.getLocation());
            NetworkNode node = new NetworkNode(
                nodeId,
                nationId,
                block.getLocation(),
                nodeType,
                description,
                System.currentTimeMillis(),
                1.0 // Base security level
            );
            
            networkNodes.put(nodeId, node);
            
            // Update nation's security profile
            CyberSecurityProfile profile = getOrCreateSecurityProfile(nationId);
            profile.nodesCount++;
            updateSecurityProfile(nationId, profile);
            
            plugin.getLogger().info("Registered cyber security node: " + nodeId + " for nation " + nationId);
            
            return "✓ Узел сети зарегистрирован: " + nodeId;
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering network node: " + e.getMessage());
            return "Ошибка при регистрации узла: " + e.getMessage();
        }
    }

    /**
     * Perform a cyber security scan of a nation's network.
     */
    public Map<String, Object> performSecurityScan(String nationId) {
        Map<String, Object> scanResults = new HashMap<>();
        
        CyberSecurityProfile profile = nationSecurityProfiles.get(nationId);
        if (profile == null) {
            scanResults.put("error", "У профиля безопасности не существует");
            return scanResults;
        }
        
        // Calculate security metrics
        scanResults.put("securityLevel", profile.securityLevel);
        scanResults.put("nodesCount", profile.nodesCount);
        scanResults.put("vulnerabilityScore", calculateVulnerabilityScore(nationId));
        scanResults.put("recentAttacks", getRecentAttacks(nationId).size());
        scanResults.put("activeDefenses", getActiveDefenses(nationId).size());
        scanResults.put("threatLevel", calculateThreatLevel(nationId));
        
        // Check for mod-specific security features
        scanResults.put("hasAdvancedSecurity", hasAdvancedSecurityMods(nationId));
        scanResults.put("networkRedundancy", calculateNetworkRedundancy(nationId));
        scanResults.put("encryptionLevel", calculateEncryptionLevel(nationId));
        
        return scanResults;
    }

    /**
     * Launch a cyber attack against a target nation.
     */
    public String launchCyberAttack(Player attacker, String targetNationId, String attackType, String targetNode) {
        try {
            UUID attackerId = attacker.getUniqueId();
            String attackerNationId = plugin.getPlayerDataManager().getNation(attackerId);
            
            if (attackerNationId == null) {
                return "Вы не состоите в нации";
            }
            
            if (attackerNationId.equals(targetNationId)) {
                return "Нельзя атаковать свою нацию";
            }
            
            // Check if attacker has required computer mods
            if (!modAPI.isModAvailable("computercraft") && !modAPI.isModAvailable("opencomputers")) {
                return "Требуется мод ComputerCraft или OpenComputers для кибератак";
            }
            
            // Check attack type validity
            if (!isValidAttackType(attackType)) {
                return "Неверный тип атаки";
            }
            
            // Calculate attack strength based on mod equipment
            double attackStrength = calculateAttackStrength(attacker, attackType);
            
            String attackId = generateAttackId(attackerNationId, targetNationId);
            
            CyberAttack attack = new CyberAttack(
                attackId,
                attackerId,
                attackerNationId,
                targetNationId,
                attackType,
                targetNode,
                attackStrength,
                System.currentTimeMillis(),
                0 // Initial defense counter
            );
            
            // Add to attacks list
            cyberAttacks.computeIfAbsent(attackerNationId, k -> new ArrayList<>()).add(attack);
            
            // Log the attack
            logSecurityEvent(attackerNationId, "CYBER_ATTACK", 
                           "Attack launched on " + targetNationId + " with strength " + attackStrength);
            
            plugin.getLogger().info("Cyber attack launched by " + attacker.getName() + 
                                   " (" + attackerNationId + ") against " + targetNationId + 
                                   " with strength: " + attackStrength);
            
            return "✓ Кибератака запущена! Сила атаки: " + attackStrength;
        } catch (Exception e) {
            plugin.getLogger().severe("Error launching cyber attack: " + e.getMessage());
            return "Ошибка при запуске атаки: " + e.getMessage();
        }
    }

    /**
     * Deploy cyber defense against incoming attacks.
     */
    public String deployCyberDefense(String nationId, String defenseType, double resourceAllocation) {
        try {
            // Check if nation has computer infrastructure
            long nationNodes = getNationNodes(nationId).size();
            if (nationNodes == 0) {
                return "У нации нет киберинфраструктуры для защиты";
            }
            
            // Validate defense type
            if (!isValidDefenseType(defenseType)) {
                return "Неверный тип защиты";
            }
            
            // Calculate defense effectiveness
            double defenseEffectiveness = calculateDefenseEffectiveness(nationId, defenseType, resourceAllocation);
            
            String defenseId = generateDefenseId(nationId, defenseType);
            
            CyberDefense defense = new CyberDefense(
                defenseId,
                nationId,
                defenseType,
                defenseEffectiveness,
                System.currentTimeMillis(),
                resourceAllocation
            );
            
            // Add to defenses list
            cyberDefenses.computeIfAbsent(nationId, k -> new ArrayList<>()).add(defense);
            
            // Update nation's security profile
            CyberSecurityProfile profile = getOrCreateSecurityProfile(nationId);
            profile.securityLevel += (defenseEffectiveness * 0.1);
            updateSecurityProfile(nationId, profile);
            
            // Log the defense
            logSecurityEvent(nationId, "CYBER_DEFENSE", 
                           "Defense deployed with effectiveness " + defenseEffectiveness);
            
            plugin.getLogger().info("Cyber defense deployed by " + nationId + 
                                   " with effectiveness: " + defenseEffectiveness);
            
            return "✓ Киберзащита развернута! Эффективность: " + defenseEffectiveness;
        } catch (Exception e) {
            plugin.getLogger().severe("Error deploying cyber defense: " + e.getMessage());
            return "Ошибка при развертывании защиты: " + e.getMessage();
        }
    }

    /**
     * Calculate attack strength based on mod equipment.
     */
    private double calculateAttackStrength(Player player, String attackType) {
        double baseStrength = 1.0;
        
        // Check if player has mod-based computer equipment
        if (modAPI.isModAvailable("computercraft")) {
            baseStrength *= 1.2; // ComputerCraft provides attack boost
        }
        if (modAPI.isModAvailable("opencomputers")) {
            baseStrength *= 1.3; // OpenComputers provides higher boost
        }
        
        // Different attack types have different multipliers
        switch (attackType.toLowerCase()) {
            case "ddos":
                baseStrength *= 1.1;
                break;
            case "data_breach":
                baseStrength *= 1.4;
                break;
            case "system_infiltration":
                baseStrength *= 1.3;
                break;
            case "network_sabotage":
                baseStrength *= 1.2;
                break;
            default:
                baseStrength *= 1.0;
        }
        
        // Check for specialized computer tools in inventory
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String modId = modAPI.detectModFromItem(item);
                if (modId != null && (modId.contains("computer") || modId.contains("oc") || modId.contains("cc"))) {
                    baseStrength *= 1.1; // Boost for computer tools
                }
            }
        }
        
        return Math.min(10.0, baseStrength); // Cap at 10.0
    }

    /**
     * Calculate defense effectiveness based on nation's infrastructure.
     */
    private double calculateDefenseEffectiveness(String nationId, String defenseType, double resourceAllocation) {
        CyberSecurityProfile profile = getOrCreateSecurityProfile(nationId);
        
        double baseEffectiveness = 1.0 + (profile.nodesCount * 0.05); // More nodes = better defense potential
        
        // Apply mod-based bonuses
        if (modAPI.hasIndustrialMods()) {
            baseEffectiveness *= 1.1; // Industrial mods help with infrastructure
        }
        
        // Defense type specific multipliers
        switch (defenseType.toLowerCase()) {
            case "firewall":
                baseEffectiveness *= 1.2;
                break;
            case "intrusion_detection":
                baseEffectiveness *= 1.1;
                break;
            case "data_encryption":
                baseEffectiveness *= 1.3;
                break;
            case "backup_systems":
                baseEffectiveness *= 1.15;
                break;
            default:
                baseEffectiveness *= 1.0;
        }
        
        // Apply resource allocation multiplier
        baseEffectiveness *= (1.0 + (resourceAllocation * 0.5));
        
        return Math.min(5.0, baseEffectiveness); // Cap at 5.0
    }

    /**
     * Calculate vulnerability score for a nation.
     */
    private double calculateVulnerabilityScore(String nationId) {
        CyberSecurityProfile profile = nationSecurityProfiles.get(nationId);
        if (profile == null) {
            return 5.0; // Default vulnerability if no profile
        }
        
        double baseVulnerability = 5.0;
        
        // Lower vulnerability with better security level
        baseVulnerability -= (profile.securityLevel * 0.5);
        
        // Higher vulnerability with more nodes (surface area)
        baseVulnerability += (profile.nodesCount * 0.1);
        
        // Factor in recent attacks
        List<CyberAttack> recentAttacks = getRecentAttacks(nationId);
        baseVulnerability += (recentAttacks.size() * 0.5);
        
        return Math.max(0.0, Math.min(10.0, baseVulnerability));
    }

    /**
     * Calculate threat level for a nation.
     */
    private double calculateThreatLevel(String nationId) {
        // Consider all attacking nations and their attack potential
        double totalThreat = 0.0;
        
        for (List<CyberAttack> attacks : cyberAttacks.values()) {
            for (CyberAttack attack : attacks) {
                if (attack.targetNationId.equals(nationId)) {
                    totalThreat += attack.strength;
                }
            }
        }
        
        return totalThreat;
    }

    /**
     * Check if nation has advanced security mods.
     */
    private boolean hasAdvancedSecurityMods(String nationId) {
        // In a real implementation, this would check for specific security-oriented mods
        // For now, we'll return true if they have any computer mods
        return modAPI.isModAvailable("computercraft") || modAPI.isModAvailable("opencomputers");
    }

    /**
     * Calculate network redundancy.
     */
    private double calculateNetworkRedundancy(String nationId) {
        long nodeCount = getNationNodes(nationId).size();
        // More nodes generally mean better redundancy
        return Math.min(5.0, nodeCount * 0.5); // Max 5.0 for redundancy
    }

    /**
     * Calculate encryption level.
     */
    private double calculateEncryptionLevel(String nationId) {
        // In a real implementation, this would be based on specific encryption mods
        // For now, estimate based on security profile
        CyberSecurityProfile profile = nationSecurityProfiles.get(nationId);
        return profile != null ? Math.min(5.0, profile.securityLevel * 0.5) : 1.0;
    }

    /**
     * Get or create security profile for a nation.
     */
    private CyberSecurityProfile getOrCreateSecurityProfile(String nationId) {
        return nationSecurityProfiles.computeIfAbsent(nationId, k -> 
            new CyberSecurityProfile(nationId, 1.0, 0, System.currentTimeMillis()));
    }

    /**
     * Update security profile for a nation.
     */
    private void updateSecurityProfile(String nationId, CyberSecurityProfile profile) {
        nationSecurityProfiles.put(nationId, profile);
        lastSecurityUpdate.put(nationId, System.currentTimeMillis());
    }

    /**
     * Get network nodes for a nation.
     */
    private List<NetworkNode> getNationNodes(String nationId) {
        List<NetworkNode> nodes = new ArrayList<>();
        for (NetworkNode node : networkNodes.values()) {
            if (node.nationId.equals(nationId)) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * Get recent attacks against a nation (past hour).
     */
    private List<CyberAttack> getRecentAttacks(String nationId) {
        List<CyberAttack> recent = new ArrayList<>();
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        
        for (List<CyberAttack> attacks : cyberAttacks.values()) {
            for (CyberAttack attack : attacks) {
                if (attack.targetNationId.equals(nationId) && attack.timestamp > oneHourAgo) {
                    recent.add(attack);
                }
            }
        }
        
        return recent;
    }

    /**
     * Get active defenses for a nation.
     */
    private List<CyberDefense> getActiveDefenses(String nationId) {
        return new ArrayList<>(cyberDefenses.getOrDefault(nationId, new ArrayList<>()));
    }

    /**
     * Validate attack type.
     */
    private boolean isValidAttackType(String attackType) {
        return Arrays.asList(
            "ddos", "data_breach", "system_infiltration", 
            "network_sabotage", "virus_infection", "malware_attack"
        ).contains(attackType.toLowerCase());
    }

    /**
     * Validate defense type.
     */
    private boolean isValidDefenseType(String defenseType) {
        return Arrays.asList(
            "firewall", "intrusion_detection", "data_encryption", 
            "backup_systems", "access_control", "monitoring_system"
        ).contains(defenseType.toLowerCase());
    }

    /**
     * Generate unique node ID.
     */
    private String generateNodeId(String nationId, Location location) {
        return "node_" + nationId + "_" + 
               location.getWorld().getName() + "_" + 
               location.getBlockX() + "_" + 
               location.getBlockY() + "_" + 
               location.getBlockZ() + "_" + 
               System.currentTimeMillis();
    }

    /**
     * Generate unique attack ID.
     */
    private String generateAttackId(String originId, String targetId) {
        return "attack_" + originId + "_to_" + targetId + "_" + System.currentTimeMillis();
    }

    /**
     * Generate unique defense ID.
     */
    private String generateDefenseId(String nationId, String defenseType) {
        return "defense_" + nationId + "_" + defenseType + "_" + System.currentTimeMillis();
    }

    /**
     * Log security event.
     */
    private void logSecurityEvent(String nationId, String eventType, String details) {
        SecurityLogEntry logEntry = new SecurityLogEntry(
            "log_" + System.currentTimeMillis(),
            nationId,
            eventType,
            details,
            System.currentTimeMillis()
        );
        
        securityLogs.computeIfAbsent(nationId, k -> new ArrayList<>()).add(logEntry);
    }

    /**
     * Update security metrics periodically.
     */
    private void updateSecurity() {
        long currentTime = System.currentTimeMillis();
        
        // Process cyber attacks
        processPendingAttacks();
        
        // Update security profiles based on activity
        for (String nationId : nationSecurityProfiles.keySet()) {
            CyberSecurityProfile profile = nationSecurityProfiles.get(nationId);
            if (profile != null) {
                // Gradually reduce security level if no activity
                profile.securityLevel = Math.max(1.0, profile.securityLevel * 0.999);
                profile.lastUpdated = currentTime;
            }
        }
    }

    /**
     * Process pending cyber attacks.
     */
    private void processPendingAttacks() {
        // In a real implementation, this would match attacks with defenses and calculate results
        // For now, we'll just increment defense counters as a simulation
        
        for (List<CyberAttack> attacks : cyberAttacks.values()) {
            for (CyberAttack attack : attacks) {
                // Simulate defense attempts
                List<CyberDefense> defenses = cyberDefenses.get(attack.targetNationId);
                if (defenses != null && !defenses.isEmpty()) {
                    // Count successful defenses against this attack
                    for (CyberDefense defense : defenses) {
                        if (Math.random() < 0.3) { // 30% chance of successful defense
                            attack.defenseCounter++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Get cyber security statistics for a nation.
     */
    public Map<String, Object> getSecurityStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic network information
        long nodeCount = getNationNodes(nationId).size();
        stats.put("networkNodes", nodeCount);
        
        // Security metrics
        CyberSecurityProfile profile = nationSecurityProfiles.get(nationId);
        if (profile != null) {
            stats.put("securityLevel", profile.securityLevel);
            stats.put("vulnerabilityScore", calculateVulnerabilityScore(nationId));
            stats.put("lastUpdated", profile.lastUpdated);
        }
        
        // Attack/defense statistics
        stats.put("attacksLaunched", cyberAttacks.getOrDefault(nationId, new ArrayList<>()).size());
        stats.put("attacksReceived", getAttacksReceived(nationId).size());
        stats.put("activeDefenses", getActiveDefenses(nationId).size());
        
        // Mod integration stats
        stats.put("hasComputerCraft", modAPI.isModAvailable("computercraft"));
        stats.put("hasOpenComputers", modAPI.isModAvailable("opencomputers"));
        stats.put("hasAdvancedSecurity", hasAdvancedSecurityMods(nationId));
        
        // Threat assessment
        stats.put("threatLevel", calculateThreatLevel(nationId));
        stats.put("networkRedundancy", calculateNetworkRedundancy(nationId));
        stats.put("encryptionLevel", calculateEncryptionLevel(nationId));
        
        return stats;
    }

    /**
     * Get attacks received by a nation.
     */
    private List<CyberAttack> getAttacksReceived(String nationId) {
        List<CyberAttack> received = new ArrayList<>();
        for (List<CyberAttack> attacks : cyberAttacks.values()) {
            for (CyberAttack attack : attacks) {
                if (attack.targetNationId.equals(nationId)) {
                    received.add(attack);
                }
            }
        }
        return received;
    }

    /**
     * Get security logs for a nation.
     */
    public List<SecurityLogEntry> getSecurityLogs(String nationId) {
        return new ArrayList<>(securityLogs.getOrDefault(nationId, new ArrayList<>()));
    }

    /**
     * Check if nation has active cyber security.
     */
    public boolean hasActiveSecurity(String nationId) {
        return nationSecurityProfiles.containsKey(nationId) && 
               nationSecurityProfiles.get(nationId).securityLevel > 1.0;
    }

    // Data classes for cyber security structures
    
    public static class NetworkNode {
        public final String nodeId;
        public final String nationId;
        public final Location location;
        public final String type;
        public final String description;
        public final long creationTime;
        public double securityLevel;
        
        public NetworkNode(String nodeId, String nationId, Location location, 
                          String type, String description, long creationTime, 
                          double securityLevel) {
            this.nodeId = nodeId;
            this.nationId = nationId;
            this.location = location;
            this.type = type;
            this.description = description;
            this.creationTime = creationTime;
            this.securityLevel = securityLevel;
        }
    }
    
    public static class CyberSecurityProfile {
        public String nationId;
        public double securityLevel;
        public int nodesCount;
        public long lastUpdated;
        
        public CyberSecurityProfile(String nationId, double securityLevel, 
                                   int nodesCount, long lastUpdated) {
            this.nationId = nationId;
            this.securityLevel = securityLevel;
            this.nodesCount = nodesCount;
            this.lastUpdated = lastUpdated;
        }
    }
    
    public static class CyberAttack {
        public final String attackId;
        public final UUID attackerId;
        public final String originNationId;
        public final String targetNationId;
        public final String type;
        public final String targetNode;
        public final double strength;
        public final long timestamp;
        public int defenseCounter; // Number of successful defenses against this attack
        
        public CyberAttack(String attackId, UUID attackerId, String originNationId, 
                          String targetNationId, String type, String targetNode, 
                          double strength, long timestamp, int defenseCounter) {
            this.attackId = attackId;
            this.attackerId = attackerId;
            this.originNationId = originNationId;
            this.targetNationId = targetNationId;
            this.type = type;
            this.targetNode = targetNode;
            this.strength = strength;
            this.timestamp = timestamp;
            this.defenseCounter = defenseCounter;
        }
    }
    
    public static class CyberDefense {
        public final String defenseId;
        public final String nationId;
        public final String type;
        public final double effectiveness;
        public final long timestamp;
        public final double resourceAllocation;
        
        public CyberDefense(String defenseId, String nationId, String type, 
                           double effectiveness, long timestamp, 
                           double resourceAllocation) {
            this.defenseId = defenseId;
            this.nationId = nationId;
            this.type = type;
            this.effectiveness = effectiveness;
            this.timestamp = timestamp;
            this.resourceAllocation = resourceAllocation;
        }
    }
    
    public static class SecurityLogEntry {
        public final String logId;
        public final String nationId;
        public final String eventType;
        public final String details;
        public final long timestamp;
        
        public SecurityLogEntry(String logId, String nationId, String eventType, 
                               String details, long timestamp) {
            this.logId = logId;
            this.nationId = nationId;
            this.eventType = eventType;
            this.details = details;
            this.timestamp = timestamp;
        }
    }
}