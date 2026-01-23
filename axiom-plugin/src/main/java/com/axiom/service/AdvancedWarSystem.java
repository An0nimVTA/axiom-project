package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced war system with battles, fronts, occupation, and military operations.
 * Integrates all war-related services into a comprehensive warfare engine.
 */
public class AdvancedWarSystem {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final DiplomacySystem diplomacySystem;
    private final MilitaryService militaryService;
    private final ConquestService conquestService;
    private final RaidService raidService;
    private final SiegeService siegeService;
    
    private final File warsDir;
    
    // War types
    public enum WarType {
        TERRITORIAL,     // Standard conquest war
        ECONOMIC,        // Economic warfare
        TOTAL,           // Total war
        COLONIAL,        // Colonial expansion
        CIVIL,           // Civil war
        RELIGIOUS,       // Religious war
        PROXY,           // Proxy war
        DEFENSIVE        // Defensive alliance
    }
    
    // War status
    public enum WarStatus {
        DECLARED,           // Just declared
        ACTIVE,            // Active combat
        STALEMATE,         // Stalemate
        ATTACKER_WINNING,  // Attacker advancing
        DEFENDER_WINNING,  // Defender holding
        NEAR_PEACE,        // Near peace
        ENDED              // Ended
    }
    
    // War data structure
    public static class War {
        public String id;                              // Unique war ID
        public String attackerId;                      // Attacking nation
        public String defenderId;                      // Defending nation
        public WarType type;                           // Type of war
        public WarStatus status;                       // Current status
        public long startTime;                         // When war started
        public long endTime;                          // When war ended (0 if active)
        
        // Battle statistics
        public int battlesFought;                      // Total battles
        public int attackerWins;                       // Attacker victories
        public int defenderWins;                       // Defender victories
        
        // Territory changes
        public int territoriesCaptured;                // Territories captured by attacker
        public int territoriesLost;                   // Territories lost by defender
        public Set<String> occupiedChunks;             // Currently occupied chunks
        
        // Economic impact
        public double attackerCost;                    // Total cost for attacker
        public double defenderCost;                    // Total cost for defender
        public double damagesDealt;                    // Economic damages
        
        // Military losses
        public int attackerCasualties;                 // Attacker losses
        public int defenderCasualties;                 // Defender losses
        
        // Front lines
        public Map<String, Front> fronts;              // Front name -> front data
        
        // War goals
        public List<String> attackerGoals;            // War goals of attacker
        public List<String> defenderGoals;            // Defensive goals
        public Map<String, Integer> goalProgress;     // Goal -> progress %
    }
    
    // Front line data
    public static class Front {
        String name;                           // Front name (e.g., "Northern Front")
        String region;                         // World region
        public double attackerProgress;        // 0-100% attacker advancement
        public double defenderDefense;         // 0-100% defender resistance
        Set<String> contestedChunks;           // Chunks in this front
        long lastBattleTime;                   // Last battle on this front
        int battlesOnFront;                     // Battles fought here
    }
    
    // Battle result
    public static class BattleResult {
        boolean attackerVictory;                // Who won
        int attackerCasualties;                // Attacker losses
        int defenderCasualties;                 // Defender losses
        double damageToInfrastructure;          // Infrastructure damage
        int territoriesGained;                  // Territories captured
        String frontName;                      // Which front
    }
    
    // Active wars
    private final Map<String, War> activeWars = new ConcurrentHashMap<>(); // warId -> war
    private final Map<String, Set<String>> nationWars = new ConcurrentHashMap<>(); // nationId -> set of warIds
    
    public AdvancedWarSystem(AXIOM plugin, NationManager nationManager, DiplomacySystem diplomacySystem,
                           MilitaryService militaryService, ConquestService conquestService,
                           RaidService raidService, SiegeService siegeService) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.diplomacySystem = diplomacySystem;
        this.militaryService = militaryService;
        this.conquestService = conquestService;
        this.raidService = raidService;
        this.siegeService = siegeService;
        
        this.warsDir = new File(plugin.getDataFolder(), "wars");
        this.warsDir.mkdirs();
        
        loadAllWars();
        
        // Process wars every 2 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processWars, 0, 20 * 60 * 2);
        
        // Update fronts every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateFronts, 0, 20 * 60 * 5);
        
        plugin.getLogger().info("AdvancedWarSystem initialized with " + activeWars.size() + " active wars");
    }
    
    /**
     * Declare advanced war with type and goals.
     */
    public synchronized String declareAdvancedWar(Nation attacker, Nation defender, WarType type, List<String> goals) throws IOException {
        if (diplomacySystem.isAtWar(attacker.getId(), defender.getId())) {
            return "Война уже активна.";
        }
        
        String warId = UUID.randomUUID().toString().substring(0, 8);
        War war = new War();
        war.id = warId;
        war.attackerId = attacker.getId();
        war.defenderId = defender.getId();
        war.type = type;
        war.status = WarStatus.DECLARED;
        war.startTime = System.currentTimeMillis();
        war.endTime = 0;
        war.battlesFought = 0;
        war.attackerWins = 0;
        war.defenderWins = 0;
        war.territoriesCaptured = 0;
        war.territoriesLost = 0;
        war.occupiedChunks = new HashSet<>();
        war.attackerCost = 0;
        war.defenderCost = 0;
        war.damagesDealt = 0;
        war.attackerCasualties = 0;
        war.defenderCasualties = 0;
        war.fronts = new HashMap<>();
        war.attackerGoals = new ArrayList<>(goals);
        war.defenderGoals = new ArrayList<>(Arrays.asList("Защитить территорию", "Отразить атаку"));
        war.goalProgress = new HashMap<>();
        
        // Initialize fronts based on war type
        initializeFronts(war);
        
        // Calculate war cost based on type
        double cost = calculateWarCost(war);
        if (attacker.getTreasury() < cost) {
            return "Недостаточно средств. Требуется: " + String.format("%.0f", cost);
        }
        
        attacker.setTreasury(attacker.getTreasury() - cost);
        war.attackerCost = cost;
        
        activeWars.put(warId, war);
        nationWars.computeIfAbsent(attacker.getId(), k -> new HashSet<>()).add(warId);
        nationWars.computeIfAbsent(defender.getId(), k -> new HashSet<>()).add(warId);
        
        // Also register with basic diplomacy system
        diplomacySystem.declareWar(attacker, defender);
        
        saveWar(war);
        nationManager.save(attacker);
        nationManager.save(defender);
        
        // VISUAL EFFECTS: Enhanced war declaration
        announceWarDeclaration(war);
        
        return String.format("Война типа '%s' объявлена. Цели: %s", type, String.join(", ", goals));
    }
    
    /**
     * Calculate war cost based on type and nation strength.
     */
    private double calculateWarCost(War war) {
        double baseCost = 5000.0;
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        if (attacker == null || defender == null) return baseCost;
        
        double attackerStrength = militaryService != null ? militaryService.getMilitaryStrength(war.attackerId) : 1.0;
        double defenderStrength = militaryService != null ? militaryService.getMilitaryStrength(war.defenderId) : 1.0;
        
        // War type multipliers
        double typeMultiplier = 1.0;
        switch (war.type) {
            case TOTAL: typeMultiplier = 3.0; break;
            case ECONOMIC: typeMultiplier = 2.0; break;
            case COLONIAL: typeMultiplier = 1.5; break;
            case RELIGIOUS: typeMultiplier = 1.2; break;
            default: typeMultiplier = 1.0;
        }
        
        // Strength-based cost (stronger enemy = more expensive)
        double strengthRatio = defenderStrength / Math.max(1, attackerStrength);
        double strengthMultiplier = 1.0 + (strengthRatio * 0.5);
        
        return baseCost * typeMultiplier * strengthMultiplier;
    }
    
    /**
     * Initialize fronts for the war.
     */
    private void initializeFronts(War war) {
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        if (attacker == null || defender == null) return;
        
        // Create fronts based on shared borders
        Map<String, Set<String>> sharedBorders = findSharedBorders(attacker, defender);
        
        int frontNumber = 1;
        for (Map.Entry<String, Set<String>> entry : sharedBorders.entrySet()) {
            String world = entry.getKey();
            Set<String> borderChunks = entry.getValue();
            
            if (!borderChunks.isEmpty()) {
                Front front = new Front();
                front.name = "Фронт " + frontNumber + " (" + world + ")";
                front.region = world;
                front.attackerProgress = 0.0;
                front.defenderDefense = 50.0; // Initial defense
                front.contestedChunks = new HashSet<>(borderChunks);
                front.lastBattleTime = System.currentTimeMillis();
                front.battlesOnFront = 0;
                
                war.fronts.put(front.name, front);
                frontNumber++;
            }
        }
        
        // If no shared borders, create a general front
        if (war.fronts.isEmpty()) {
            Front front = new Front();
            front.name = "Главный фронт";
            front.region = "all";
            front.attackerProgress = 0.0;
            front.defenderDefense = 50.0;
            front.contestedChunks = new HashSet<>();
            front.lastBattleTime = System.currentTimeMillis();
            front.battlesOnFront = 0;
            
            war.fronts.put(front.name, front);
        }
    }
    
    /**
     * Find shared borders between two nations.
     */
    private Map<String, Set<String>> findSharedBorders(Nation attacker, Nation defender) {
        Map<String, Set<String>> borders = new HashMap<>();
        
        Set<String> attackerChunks = attacker.getClaimedChunkKeys();
        Set<String> defenderChunks = defender.getClaimedChunkKeys();
        if (attackerChunks == null || defenderChunks == null || attackerChunks.isEmpty() || defenderChunks.isEmpty()) {
            return borders;
        }
        
        // Find chunks that are adjacent
        for (String attackerChunk : attackerChunks) {
            String[] parts = attackerChunk.split(":");
            if (parts.length != 3) continue;
            
            String world = parts[0];
            int chunkX;
            int chunkZ;
            try {
                chunkX = Integer.parseInt(parts[1]);
                chunkZ = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            
            // Check adjacent chunks
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    String adjacent = world + ":" + (chunkX + dx) + ":" + (chunkZ + dz);
                    if (defenderChunks.contains(adjacent)) {
                        borders.computeIfAbsent(world, k -> new HashSet<>()).add(attackerChunk);
                        break;
                    }
                }
            }
        }
        
        return borders;
    }
    
    /**
     * Process all active wars.
     */
    private synchronized void processWars() {
        long now = System.currentTimeMillis();
        List<String> warsToEnd = new ArrayList<>();
        
        for (War war : activeWars.values()) {
            if (war.status == WarStatus.ENDED) {
                warsToEnd.add(war.id);
                continue;
            }
            
            // Update war status based on progress
            updateWarStatus(war);
            
            // Process battles on fronts
            for (Front front : war.fronts.values()) {
                // Chance for battle every 10 minutes per front
                if (now - front.lastBattleTime > 10 * 60 * 1000L && Math.random() < 0.3) {
                    BattleResult result = simulateBattle(war, front);
                    if (result != null) {
                        applyBattleResults(war, front, result);
                    }
                }
            }
            
            // Calculate ongoing costs
            double hourlyCost = calculateHourlyCost(war);
            Nation attacker = nationManager.getNationById(war.attackerId);
            Nation defender = nationManager.getNationById(war.defenderId);
            
            if (attacker != null && attacker.getTreasury() >= hourlyCost) {
                attacker.setTreasury(attacker.getTreasury() - hourlyCost);
                war.attackerCost += hourlyCost;
                try { nationManager.save(attacker); } catch (Exception ignored) {}
            }
            
            if (defender != null && defender.getTreasury() >= hourlyCost) {
                defender.setTreasury(defender.getTreasury() - hourlyCost);
                war.defenderCost += hourlyCost;
                try { nationManager.save(defender); } catch (Exception ignored) {}
            }
            
            saveWar(war);
        }
        
        // End expired wars
        for (String warId : warsToEnd) {
            endWar(warId);
        }
    }
    
    /**
     * Simulate a battle on a front.
     */
    private BattleResult simulateBattle(War war, Front front) {
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        
        if (attacker == null || defender == null) return null;
        
        // Get military strengths
        double attackerStrength = militaryService != null ? militaryService.getMilitaryStrength(war.attackerId) : 1.0;
        double defenderStrength = militaryService != null ? militaryService.getMilitaryStrength(war.defenderId) : 1.0;
        
        // Apply front-specific bonuses
        double attackerBonus = 1.0 + (front.attackerProgress / 100.0) * 0.1; // Up to +10% if advancing
        double defenderBonus = 1.0 + (front.defenderDefense / 100.0) * 0.1; // Up to +10% if defending well
        
        // Apply mod bonuses
        double attackerModBonus = getWarModBonus(war.attackerId);
        double defenderModBonus = getWarModBonus(war.defenderId);
        
        attackerStrength *= attackerBonus * attackerModBonus;
        defenderStrength *= defenderBonus * defenderModBonus;
        
        // Apply technology bonuses
        TechnologyTreeService techService = plugin.getTechnologyTreeService();
        double attackerTechBonus = techService != null ? techService.getBonus(war.attackerId, "warStrength") : 1.0;
        double defenderTechBonus = techService != null ? techService.getBonus(war.defenderId, "warStrength") : 1.0;
        
        attackerStrength *= attackerTechBonus;
        defenderStrength *= defenderTechBonus;
        
        // Calculate battle outcome
        double totalStrength = attackerStrength + defenderStrength;
        double attackerChance = attackerStrength / totalStrength;
        boolean attackerVictory = Math.random() < attackerChance;
        
        BattleResult result = new BattleResult();
        result.attackerVictory = attackerVictory;
        
        // Calculate casualties (based on opponent strength)
        if (attackerVictory) {
            result.attackerCasualties = (int) (defenderStrength * 0.05); // 5% of defender strength
            result.defenderCasualties = (int) (defenderStrength * 0.15); // 15% of defender strength
            result.damageToInfrastructure = defenderStrength * 0.02;
            result.territoriesGained = Math.max(1, (int) (defender.getClaimedChunkKeys().size() * 0.01)); // 1% of territory
        } else {
            result.attackerCasualties = (int) (attackerStrength * 0.15); // 15% of attacker strength
            result.defenderCasualties = (int) (attackerStrength * 0.05); // 5% of attacker strength
            result.damageToInfrastructure = attackerStrength * 0.01;
            result.territoriesGained = 0;
        }
        
        result.frontName = front.name;
        
        return result;
    }
    
    /**
     * Get war mod bonus for a nation.
     */
    private double getWarModBonus(String nationId) {
        double bonus = 1.0;
        
        // Check for warfare mods
        ModWarfareService modWarfareService = plugin.getModWarfareService();
        if (modWarfareService != null) {
            int warfareMods = modWarfareService.getAvailableWarfareModsCount();
            if (warfareMods > 0) {
                bonus += warfareMods * 0.05; // +5% per warfare mod
            }
        }
        
        // Check for industrial mods (production support)
        ModIntegrationService modIntegrationService = plugin.getModIntegrationService();
        if (modIntegrationService != null) {
            if (modIntegrationService.hasIndustrialMods()) {
                bonus += 0.03; // +3% for industrial support
            }
            
            // Check for logistics mods (supply)
            if (modIntegrationService.hasLogisticsMods()) {
                bonus += 0.02; // +2% for logistics
            }
        }
        
        return bonus;
    }
    
    /**
     * Apply battle results to war and fronts.
     */
    private synchronized void applyBattleResults(War war, Front front, BattleResult result) {
        war.battlesFought++;
        if (result.attackerVictory) {
            war.attackerWins++;
            front.attackerProgress = Math.min(100, front.attackerProgress + 5.0);
            front.defenderDefense = Math.max(0, front.defenderDefense - 5.0);
            
            // Capture territories
            if (result.territoriesGained > 0) {
                captureTerritories(war, result.territoriesGained);
            }
        } else {
            war.defenderWins++;
            front.attackerProgress = Math.max(0, front.attackerProgress - 3.0);
            front.defenderDefense = Math.min(100, front.defenderDefense + 3.0);
        }
        
        war.attackerCasualties += result.attackerCasualties;
        war.defenderCasualties += result.defenderCasualties;
        war.damagesDealt += result.damageToInfrastructure;
        front.lastBattleTime = System.currentTimeMillis();
        front.battlesOnFront++;
        
        // Apply economic damages
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        
        if (attacker != null && result.attackerCasualties > 0) {
            // Casualties cost money
            double casualtyCost = result.attackerCasualties * 100.0;
            attacker.setTreasury(Math.max(0, attacker.getTreasury() - casualtyCost));
            try { nationManager.save(attacker); } catch (Exception ignored) {}
        }
        
        if (defender != null && result.defenderCasualties > 0) {
            double casualtyCost = result.defenderCasualties * 100.0;
            defender.setTreasury(Math.max(0, defender.getTreasury() - casualtyCost));
            try { nationManager.save(defender); } catch (Exception ignored) {}
        }
        
        // VISUAL EFFECTS: Announce battle
        announceBattle(war, front, result);
    }
    
    /**
     * Capture territories during war.
     */
    private synchronized void captureTerritories(War war, int count) {
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        
        if (attacker == null || defender == null) return;
        if (attacker.getClaimedChunkKeys() == null || defender.getClaimedChunkKeys() == null) return;
        if (war.occupiedChunks == null) war.occupiedChunks = new HashSet<>();
        
        // Capture random chunks from defender
        List<String> defenderChunks = new ArrayList<>(defender.getClaimedChunkKeys());
        Collections.shuffle(defenderChunks);
        
        int captured = 0;
        for (String chunk : defenderChunks) {
            if (captured >= count) break;
            defender.getClaimedChunkKeys().remove(chunk);
            attacker.getClaimedChunkKeys().add(chunk);
            war.occupiedChunks.add(chunk);
            captured++;
        }
        
        war.territoriesCaptured += captured;
        war.territoriesLost += captured;
        
        try {
            nationManager.save(attacker);
            nationManager.save(defender);
            
            // Update map boundaries if service exists
            com.axiom.service.MapBoundaryService mapService = plugin.getMapBoundaryService();
            if (mapService != null) {
                mapService.forceUpdate();
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Update war status based on progress.
     */
    private void updateWarStatus(War war) {
        if (war.battlesFought == 0) {
            war.status = WarStatus.DECLARED;
            return;
        }
        
        double winRatio = (double) war.attackerWins / war.battlesFought;
        double avgProgress = war.fronts.values().stream()
            .mapToDouble(f -> f.attackerProgress)
            .average()
            .orElse(0);
        
        if (winRatio >= 0.7 && avgProgress >= 70) {
            war.status = WarStatus.ATTACKER_WINNING;
        } else if (winRatio <= 0.3 && avgProgress <= 30) {
            war.status = WarStatus.DEFENDER_WINNING;
        } else if (winRatio >= 0.4 && winRatio <= 0.6 && avgProgress >= 40 && avgProgress <= 60) {
            war.status = WarStatus.STALEMATE;
        } else {
            war.status = WarStatus.ACTIVE;
        }
        
        // Check for victory conditions
        Nation defender = nationManager.getNationById(war.defenderId);
        if (defender != null) {
            int defenderCurrent = defender.getClaimedChunkKeys() != null ? defender.getClaimedChunkKeys().size() : 0;
            int defenderOriginal = defenderCurrent + war.territoriesCaptured;
            boolean territoryVictory = defenderOriginal > 0 && war.territoriesCaptured >= defenderOriginal * 0.5;
            if (avgProgress >= 90 || territoryVictory) {
                endWarVictory(war, true); // Attacker victory
            } else if (avgProgress <= 10 && war.defenderWins > war.attackerWins * 2) {
                endWarVictory(war, false); // Defender victory
            }
        }
    }
    
    /**
     * End war with victory.
     */
    private synchronized void endWarVictory(War war, boolean attackerWon) {
        war.status = WarStatus.ENDED;
        war.endTime = System.currentTimeMillis();
        
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        
        if (attacker == null || defender == null) return;
        
        // Apply victory rewards/penalties
        if (attackerWon) {
            // Attacker gets reparations
            double reparations = war.defenderCost * 0.3;
            attacker.setTreasury(attacker.getTreasury() + reparations);
            defender.setTreasury(Math.max(0, defender.getTreasury() - reparations));
            
            // Update reputation
            try {
                diplomacySystem.setReputation(attacker, defender, -20);
                diplomacySystem.setReputation(defender, attacker, -30);
            } catch (Exception ignored) {}
        } else {
            // Defender gets reparations
            double reparations = war.attackerCost * 0.2;
            defender.setTreasury(defender.getTreasury() + reparations);
            attacker.setTreasury(Math.max(0, attacker.getTreasury() - reparations));
            
            try {
                diplomacySystem.setReputation(defender, attacker, +10);
                diplomacySystem.setReputation(attacker, defender, -20);
            } catch (Exception ignored) {}
        }
        
        try {
            nationManager.save(attacker);
            nationManager.save(defender);
        } catch (Exception ignored) {}
        
        // VISUAL EFFECTS: Victory/defeat announcement
        announceWarEnd(war, attackerWon);
        
        // End in diplomacy system
        try {
            diplomacySystem.declarePeace(war.attackerId, war.defenderId);
        } catch (Exception ignored) {}
    }
    
    /**
     * Calculate hourly war cost.
     */
    private double calculateHourlyCost(War war) {
        double baseCost = 100.0;
        double typeMultiplier = 1.0;
        
        switch (war.type) {
            case TOTAL: typeMultiplier = 3.0; break;
            case ECONOMIC: typeMultiplier = 2.5; break;
            case COLONIAL: typeMultiplier = 1.5; break;
            default: typeMultiplier = 1.0;
        }
        
        return baseCost * typeMultiplier;
    }
    
    /**
     * Update front lines.
     */
    private synchronized void updateFronts() {
        for (War war : activeWars.values()) {
            if (war.status == WarStatus.ENDED) continue;
            
            for (Front front : war.fronts.values()) {
                // Front naturally shifts based on battle outcomes
                // If no battles, slight decay
                long timeSinceBattle = System.currentTimeMillis() - front.lastBattleTime;
                if (timeSinceBattle > 30 * 60 * 1000L) { // 30 minutes
                    front.attackerProgress = Math.max(0, front.attackerProgress - 1.0);
                    front.defenderDefense = Math.min(100, front.defenderDefense + 1.0);
                }
            }
            
            saveWar(war);
        }
    }
    
    /**
     * End war manually (peace treaty).
     */
    public synchronized String declarePeace(String warId) throws IOException {
        War war = activeWars.get(warId);
        if (war == null) return "Война не найдена.";
        if (war.status == WarStatus.ENDED) return "Война уже завершена.";
        
        war.status = WarStatus.ENDED;
        war.endTime = System.currentTimeMillis();
        
        endWar(warId);
        
        return "Мир объявлен.";
    }
    
    /**
     * End war and cleanup.
     */
    private synchronized void endWar(String warId) {
        War war = activeWars.remove(warId);
        if (war == null) return;
        
        nationWars.computeIfPresent(war.attackerId, (k, v) -> {
            v.remove(warId);
            return v.isEmpty() ? null : v;
        });
        nationWars.computeIfPresent(war.defenderId, (k, v) -> {
            v.remove(warId);
            return v.isEmpty() ? null : v;
        });
        
        // Delete war file
        File warFile = new File(warsDir, warId + ".json");
        if (warFile.exists()) {
            warFile.delete();
        }
    }
    
    /**
     * Get active war between two nations.
     */
    public synchronized War getActiveWar(String nationA, String nationB) {
        Set<String> warsA = nationWars.getOrDefault(nationA, Collections.emptySet());
        Set<String> warsB = nationWars.getOrDefault(nationB, Collections.emptySet());
        
        for (String warId : warsA) {
            if (warsB.contains(warId)) {
                War war = activeWars.get(warId);
                if (war != null && war.status != WarStatus.ENDED) {
                    return war;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get all wars for a nation.
     */
    public synchronized List<War> getNationWars(String nationId) {
        List<War> result = new ArrayList<>();
        Set<String> warIds = nationWars.getOrDefault(nationId, Collections.emptySet());
        for (String warId : warIds) {
            War war = activeWars.get(warId);
            if (war != null && war.status != WarStatus.ENDED) {
                result.add(war);
            }
        }
        return result;
    }
    
    /**
     * Get war statistics.
     */
    public synchronized Map<String, Object> getWarStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<War> wars = getNationWars(nationId);
        int totalWars = wars.size();
        int victories = 0;
        int defeats = 0;
        int totalBattles = 0;
        int totalCasualties = 0;
        double totalCost = 0;
        
        for (War war : wars) {
            boolean isAttacker = war.attackerId.equals(nationId);
            totalBattles += war.battlesFought;
            totalCasualties += isAttacker ? war.attackerCasualties : war.defenderCasualties;
            totalCost += isAttacker ? war.attackerCost : war.defenderCost;
            
            if (war.status == WarStatus.ATTACKER_WINNING) {
                if (isAttacker) victories++;
                else defeats++;
            } else if (war.status == WarStatus.DEFENDER_WINNING) {
                if (isAttacker) defeats++;
                else victories++;
            }
        }
        
        stats.put("totalWars", totalWars);
        stats.put("victories", victories);
        stats.put("defeats", defeats);
        stats.put("totalBattles", totalBattles);
        stats.put("totalCasualties", totalCasualties);
        stats.put("totalCost", totalCost);
        
        return stats;
    }
    
    // VISUAL EFFECTS METHODS
    
    private void announceWarDeclaration(War war) {
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        if (attacker == null || defender == null) return;
        VisualEffectsService effectsService = plugin.getVisualEffectsService();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            String title = String.format("§c§l[%s ВОЙНА]", war.type);
            String subtitle = String.format("§f'%s' против '%s'", attacker.getName(), defender.getName());
            
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(title, subtitle, 10, 100, 20);
                if (effectsService != null) {
                    effectsService.sendActionBar(player, 
                        String.format("§c⚔ %s война началась!", war.type));
                }
            }
        });
    }
    
    private void announceBattle(War war, Front front, BattleResult result) {
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        if (attacker == null || defender == null) return;
        VisualEffectsService effectsService = plugin.getVisualEffectsService();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            String winnerName = result.attackerVictory ? attacker.getName() : defender.getName();
            
            String msg = String.format("§6⚔ Битва на %s: §a%s §7победила! Потери: §c%s §7| §c%s",
                front.name, winnerName, result.attackerCasualties, result.defenderCasualties);
            
            for (UUID citizenId : attacker.getCitizens()) {
                org.bukkit.entity.Player citizen = Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    if (effectsService != null) {
                        effectsService.sendActionBar(citizen, msg);
                    }
                }
            }
            for (UUID citizenId : defender.getCitizens()) {
                org.bukkit.entity.Player citizen = Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    if (effectsService != null) {
                        effectsService.sendActionBar(citizen, msg);
                    }
                }
            }
        });
    }
    
    private void announceWarEnd(War war, boolean attackerWon) {
        Nation attacker = nationManager.getNationById(war.attackerId);
        Nation defender = nationManager.getNationById(war.defenderId);
        if (attacker == null || defender == null) return;
        VisualEffectsService effectsService = plugin.getVisualEffectsService();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            String winnerName = attackerWon ? attacker.getName() : defender.getName();
            
            for (UUID citizenId : attacker.getCitizens()) {
                org.bukkit.entity.Player citizen = Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    boolean won = attackerWon;
                    citizen.sendTitle(
                        won ? "§a§l[ПОБЕДА!]" : "§c§l[ПОРАЖЕНИЕ]",
                        won ? "Война выиграна!" : "Война проиграна",
                        10, 100, 20);
                    if (effectsService != null) {
                        effectsService.sendActionBar(citizen,
                            String.format("§6⚔ Война завершена. %s победила!", winnerName));
                    }
                }
            }
            for (UUID citizenId : defender.getCitizens()) {
                org.bukkit.entity.Player citizen = Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    boolean won = !attackerWon;
                    citizen.sendTitle(
                        won ? "§a§l[ПОБЕДА!]" : "§c§l[ПОРАЖЕНИЕ]",
                        won ? "Война выиграна!" : "Война проиграна",
                        10, 100, 20);
                    if (effectsService != null) {
                        effectsService.sendActionBar(citizen,
                            String.format("§6⚔ Война завершена. %s победила!", winnerName));
                    }
                }
            }
        });
    }
    
    // PERSISTENCE
    
    private void loadAllWars() {
        File[] files = warsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                War war = deserializeWar(o);
                if (war != null && war.status != WarStatus.ENDED) {
                    activeWars.put(war.id, war);
                    nationWars.computeIfAbsent(war.attackerId, k -> new HashSet<>()).add(war.id);
                    nationWars.computeIfAbsent(war.defenderId, k -> new HashSet<>()).add(war.id);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load war: " + f.getName() + " - " + e.getMessage());
            }
        }
    }
    
    private void saveWar(War war) {
        File f = new File(warsDir, war.id + ".json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(serializeWar(war).toString());
        } catch (Exception ignored) {}
    }
    
    private JsonObject serializeWar(War war) {
        JsonObject o = new JsonObject();
        o.addProperty("id", war.id);
        o.addProperty("attackerId", war.attackerId);
        o.addProperty("defenderId", war.defenderId);
        o.addProperty("type", war.type.name());
        o.addProperty("status", war.status.name());
        o.addProperty("startTime", war.startTime);
        o.addProperty("endTime", war.endTime);
        o.addProperty("battlesFought", war.battlesFought);
        o.addProperty("attackerWins", war.attackerWins);
        o.addProperty("defenderWins", war.defenderWins);
        o.addProperty("territoriesCaptured", war.territoriesCaptured);
        o.addProperty("territoriesLost", war.territoriesLost);
        o.addProperty("attackerCost", war.attackerCost);
        o.addProperty("defenderCost", war.defenderCost);
        o.addProperty("damagesDealt", war.damagesDealt);
        o.addProperty("attackerCasualties", war.attackerCasualties);
        o.addProperty("defenderCasualties", war.defenderCasualties);
        
        // Serialize occupied chunks
        com.google.gson.JsonArray occupiedArr = new com.google.gson.JsonArray();
        if (war.occupiedChunks != null) {
            for (String chunk : war.occupiedChunks) occupiedArr.add(chunk);
        }
        o.add("occupiedChunks", occupiedArr);
        
        // Serialize fronts
        JsonObject frontsObj = new JsonObject();
        for (Map.Entry<String, Front> entry : war.fronts.entrySet()) {
            JsonObject frontObj = new JsonObject();
            frontObj.addProperty("name", entry.getValue().name);
            frontObj.addProperty("region", entry.getValue().region);
            frontObj.addProperty("attackerProgress", entry.getValue().attackerProgress);
            frontObj.addProperty("defenderDefense", entry.getValue().defenderDefense);
            frontObj.addProperty("lastBattleTime", entry.getValue().lastBattleTime);
            frontObj.addProperty("battlesOnFront", entry.getValue().battlesOnFront);
            com.google.gson.JsonArray contestedArr = new com.google.gson.JsonArray();
            if (entry.getValue().contestedChunks != null) {
                for (String chunk : entry.getValue().contestedChunks) contestedArr.add(chunk);
            }
            frontObj.add("contestedChunks", contestedArr);
            frontsObj.add(entry.getKey(), frontObj);
        }
        o.add("fronts", frontsObj);
        
        // Serialize goals
        com.google.gson.JsonArray goalsArr = new com.google.gson.JsonArray();
        for (String goal : war.attackerGoals) goalsArr.add(goal);
        o.add("attackerGoals", goalsArr);
        
        com.google.gson.JsonArray defenderGoalsArr = new com.google.gson.JsonArray();
        for (String goal : war.defenderGoals) defenderGoalsArr.add(goal);
        o.add("defenderGoals", defenderGoalsArr);
        
        JsonObject goalProgressObj = new JsonObject();
        for (Map.Entry<String, Integer> entry : war.goalProgress.entrySet()) {
            goalProgressObj.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("goalProgress", goalProgressObj);
        
        return o;
    }
    
    private War deserializeWar(JsonObject o) {
        War war = new War();
        war.id = o.get("id").getAsString();
        war.attackerId = o.get("attackerId").getAsString();
        war.defenderId = o.get("defenderId").getAsString();
        war.type = WarType.valueOf(o.get("type").getAsString());
        war.status = WarStatus.valueOf(o.get("status").getAsString());
        war.startTime = o.get("startTime").getAsLong();
        war.endTime = o.has("endTime") ? o.get("endTime").getAsLong() : 0;
        war.battlesFought = o.has("battlesFought") ? o.get("battlesFought").getAsInt() : 0;
        war.attackerWins = o.has("attackerWins") ? o.get("attackerWins").getAsInt() : 0;
        war.defenderWins = o.has("defenderWins") ? o.get("defenderWins").getAsInt() : 0;
        war.territoriesCaptured = o.has("territoriesCaptured") ? o.get("territoriesCaptured").getAsInt() : 0;
        war.territoriesLost = o.has("territoriesLost") ? o.get("territoriesLost").getAsInt() : 0;
        war.occupiedChunks = new HashSet<>();
        if (o.has("occupiedChunks")) {
            for (com.google.gson.JsonElement e : o.getAsJsonArray("occupiedChunks")) {
                war.occupiedChunks.add(e.getAsString());
            }
        }
        war.attackerCost = o.has("attackerCost") ? o.get("attackerCost").getAsDouble() : 0;
        war.defenderCost = o.has("defenderCost") ? o.get("defenderCost").getAsDouble() : 0;
        war.damagesDealt = o.has("damagesDealt") ? o.get("damagesDealt").getAsDouble() : 0;
        war.attackerCasualties = o.has("attackerCasualties") ? o.get("attackerCasualties").getAsInt() : 0;
        war.defenderCasualties = o.has("defenderCasualties") ? o.get("defenderCasualties").getAsInt() : 0;
        war.fronts = new HashMap<>();
        war.attackerGoals = new ArrayList<>();
        war.defenderGoals = new ArrayList<>();
        war.goalProgress = new HashMap<>();
        
        // Deserialize fronts
        if (o.has("fronts")) {
            JsonObject frontsObj = o.getAsJsonObject("fronts");
            for (Map.Entry<String, com.google.gson.JsonElement> entry : frontsObj.entrySet()) {
                JsonObject frontObj = entry.getValue().getAsJsonObject();
                Front front = new Front();
                front.name = frontObj.get("name").getAsString();
                front.region = frontObj.get("region").getAsString();
                front.attackerProgress = frontObj.get("attackerProgress").getAsDouble();
                front.defenderDefense = frontObj.get("defenderDefense").getAsDouble();
                front.lastBattleTime = frontObj.get("lastBattleTime").getAsLong();
                front.battlesOnFront = frontObj.get("battlesOnFront").getAsInt();
                front.contestedChunks = new HashSet<>();
                if (frontObj.has("contestedChunks")) {
                    for (com.google.gson.JsonElement e : frontObj.getAsJsonArray("contestedChunks")) {
                        front.contestedChunks.add(e.getAsString());
                    }
                }
                war.fronts.put(entry.getKey(), front);
            }
        }
        
        // Deserialize goals
        if (o.has("attackerGoals")) {
            com.google.gson.JsonArray goalsArr = o.getAsJsonArray("attackerGoals");
            for (com.google.gson.JsonElement e : goalsArr) {
                war.attackerGoals.add(e.getAsString());
            }
        }
        if (o.has("defenderGoals")) {
            com.google.gson.JsonArray goalsArr = o.getAsJsonArray("defenderGoals");
            for (com.google.gson.JsonElement e : goalsArr) {
                war.defenderGoals.add(e.getAsString());
            }
        }
        if (o.has("goalProgress")) {
            JsonObject progressObj = o.getAsJsonObject("goalProgress");
            for (Map.Entry<String, com.google.gson.JsonElement> entry : progressObj.entrySet()) {
                war.goalProgress.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        
        return war;
    }
    
    /**
     * Get war by ID.
     */
    public synchronized War getWar(String warId) {
        return activeWars.get(warId);
    }
    
    /**
     * Get all active wars.
     */
    public synchronized Collection<War> getAllActiveWars() {
        return new ArrayList<>(activeWars.values());
    }
    
    /**
     * Get global war statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalWarStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveWars", activeWars.size());
        
        Map<WarType, Integer> warsByType = new HashMap<>();
        Map<WarStatus, Integer> warsByStatus = new HashMap<>();
        int totalBattles = 0;
        int totalCasualties = 0;
        double totalWarCost = 0.0;
        Map<String, Integer> warsByNation = new HashMap<>();
        Set<String> nationsAtWar = new HashSet<>();
        
        for (War war : activeWars.values()) {
            warsByType.put(war.type, warsByType.getOrDefault(war.type, 0) + 1);
            warsByStatus.put(war.status, warsByStatus.getOrDefault(war.status, 0) + 1);
            totalBattles += war.battlesFought;
            totalCasualties += (war.attackerCasualties + war.defenderCasualties);
            totalWarCost += (war.attackerCost + war.defenderCost);
            warsByNation.put(war.attackerId, warsByNation.getOrDefault(war.attackerId, 0) + 1);
            warsByNation.put(war.defenderId, warsByNation.getOrDefault(war.defenderId, 0) + 1);
            nationsAtWar.add(war.attackerId);
            nationsAtWar.add(war.defenderId);
        }
        
        stats.put("warsByType", warsByType);
        stats.put("warsByStatus", warsByStatus);
        stats.put("totalBattles", totalBattles);
        stats.put("totalCasualties", totalCasualties);
        stats.put("totalWarCost", totalWarCost);
        stats.put("averageBattlesPerWar", activeWars.size() > 0 ? (double) totalBattles / activeWars.size() : 0);
        stats.put("averageCasualtiesPerWar", activeWars.size() > 0 ? (double) totalCasualties / activeWars.size() : 0);
        stats.put("nationsAtWar", nationsAtWar.size());
        stats.put("warsByNation", warsByNation);
        
        // Top nations by war count
        List<Map.Entry<String, Integer>> topByWars = warsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByWars", topByWars);
        
        // Most common war types
        List<Map.Entry<WarType, Integer>> mostCommonTypes = warsByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTypes", mostCommonTypes);
        
        // Total fronts
        int totalFronts = activeWars.values().stream()
            .mapToInt(war -> war.fronts.size())
            .sum();
        stats.put("totalFronts", totalFronts);
        stats.put("averageFrontsPerWar", activeWars.size() > 0 ? (double) totalFronts / activeWars.size() : 0);
        
        // Total occupied territories
        int totalOccupied = activeWars.values().stream()
            .mapToInt(war -> war.occupiedChunks.size())
            .sum();
        stats.put("totalOccupiedTerritories", totalOccupied);
        
        return stats;
    }
}

