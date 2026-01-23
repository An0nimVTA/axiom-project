package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anti-grief and balancing service.
 * Prevents economic abuse, territory spam, and inactive nations.
 */
public class BalancingService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final PlayerDataManager playerDataManager;
    
    // Track daily money printing per nation
    private final Map<String, DailyPrint> dailyPrints = new HashMap<>(); // nationId -> data
    private final Map<String, Long> lastPrintTime = new HashMap<>(); // nationId -> timestamp
    
    // Track nation creation per player
    private final Map<UUID, Long> nationCreationTime = new HashMap<>(); // playerId -> creation time
    
    // Track leader activity
    private final Map<String, Long> leaderLastSeen = new HashMap<>(); // nationId -> last seen
    
    public static class DailyPrint {
        double amount = 0.0;
        long resetTime; // when to reset counter
    }
    
    public BalancingService(AXIOM plugin, NationManager nationManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.playerDataManager = playerDataManager;
        
        // Reset daily counters every 24 hours
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::resetDailyCounters, 0, 20 * 60 * 60 * 24);
        
        // Check inactive nations every hour
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkInactiveNations, 0, 20 * 60 * 60);
        
        // Track player logins via listener registration in AXIOM.java
    }
    
    /**
     * Check if player can print money (anti-inflation grief).
     * Limit: 20% of treasury per day.
     */
    public synchronized boolean canPrintMoney(String nationId, double amount) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return false;
        
        DailyPrint dp = dailyPrints.computeIfAbsent(nationId, k -> {
            DailyPrint d = new DailyPrint();
            d.resetTime = System.currentTimeMillis() + 24 * 60 * 60_000L;
            return d;
        });
        
        // Reset if expired
        if (System.currentTimeMillis() > dp.resetTime) {
            dp.amount = 0.0;
            dp.resetTime = System.currentTimeMillis() + 24 * 60 * 60_000L;
        }
        
        double maxDaily = n.getTreasury() * 0.2; // 20% of treasury per day
        if (dp.amount + amount > maxDaily) {
            return false; // Exceeds daily limit
        }
        
        // Check cooldown between prints (minimum 1 minute)
        Long lastPrint = lastPrintTime.get(nationId);
        if (lastPrint != null && System.currentTimeMillis() - lastPrint < 60_000) {
            return false; // Too frequent
        }
        
        dp.amount += amount;
        lastPrintTime.put(nationId, System.currentTimeMillis());
        return true;
    }
    
    /**
     * Check if player can create a nation (one per account, or 7 days cooldown).
     */
    public synchronized boolean canCreateNation(UUID playerId) {
        Long lastCreation = nationCreationTime.get(playerId);
        if (lastCreation == null) {
            return true; // First nation creation
        }
        
        long cooldown = 7L * 24 * 60 * 60_000L; // 7 days
        if (System.currentTimeMillis() - lastCreation < cooldown) {
            return false; // Still on cooldown
        }
        
        return true;
    }
    
    public synchronized void recordNationCreation(UUID playerId) {
        nationCreationTime.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Check if can claim chunk (anti-spam: must be within 500 blocks of capital or connected).
     */
    public synchronized boolean canClaimChunk(String nationId, String world, int chunkX, int chunkZ) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return false;
        
        // First 5 chunks are free (already handled in NationManager)
        if (n.getClaimedChunkKeys().size() < 5) {
            return true;
        }
        
        // Must have capital
        String capitalKey = n.getCapitalChunkStr();
        if (capitalKey == null || capitalKey.isEmpty()) {
            return false; // No capital set
        }
        
        // Parse capital
        String[] capitalParts = capitalKey.split(":");
        if (capitalParts.length != 3) return false;
        
        String capitalWorld = capitalParts[0];
        int capitalX = Integer.parseInt(capitalParts[1]);
        int capitalZ = Integer.parseInt(capitalParts[2]);
        
        // Same world check
        if (!capitalWorld.equals(world)) {
            // Different world requires special infrastructure
            return false; // For now, only same world
        }
        
        // Distance check: 500 blocks = ~31 chunks
        int chunkDistance = Math.max(Math.abs(chunkX - capitalX), Math.abs(chunkZ - capitalZ));
        if (chunkDistance <= 31) {
            return true; // Within range
        }
        
        // Check for road infrastructure (simplified: check if there's a claimed path)
        // For now, allow if within 100 chunks (future: implement road system)
        return chunkDistance <= 100;
    }
    
    /**
     * Check inactive nations (leader hasn't been online for 14 days).
     */
    private void checkInactiveNations() {
        long inactiveThreshold = 14L * 24 * 60 * 60_000L; // 14 days
        long now = System.currentTimeMillis();
        
        for (Nation n : nationManager.getAll()) {
            Long lastSeen = leaderLastSeen.get(n.getId());
            if (lastSeen == null) {
                // Set initial if leader is online
                if (n.getLeader() != null) {
                    Player leader = Bukkit.getPlayer(n.getLeader());
                    if (leader != null && leader.isOnline()) {
                        leaderLastSeen.put(n.getId(), now);
                        continue;
                    }
                }
                // If leader never seen, mark as potentially inactive
                continue;
            }
            
            if (now - lastSeen > inactiveThreshold) {
                // Nation is inactive
                handleInactiveNation(n);
            }
        }
    }
    
    private void handleInactiveNation(Nation n) {
        // Option 1: Enter regency (transfer leadership to most active member)
        // Option 2: Dissolve nation
        // For now: Log warning and allow manual intervention
        
        plugin.getLogger().warning("Нация " + n.getName() + " неактивна (лидер не заходил 14+ дней).");
        
        // Try to transfer leadership to active member
        UUID newLeader = findMostActiveMember(n);
        if (newLeader != null && !newLeader.equals(n.getLeader())) {
            n.setLeader(newLeader);
            n.getHistory().add("Регентство: лидер изменён из-за неактивности");
            try {
                nationManager.save(n);
                plugin.getLogger().info("Лидерство передано игроку " + Bukkit.getOfflinePlayer(newLeader).getName());
            } catch (Exception ignored) {}
        }
    }
    
    private UUID findMostActiveMember(Nation n) {
        UUID mostActive = null;
        
        for (UUID memberId : n.getCitizens()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null && p.isOnline()) {
                return memberId; // Prefer online player
            }
            
            // Check last seen (would need to track this separately)
            // For now, return first citizen
            if (mostActive == null) {
                mostActive = memberId;
            }
        }
        
        return mostActive != null ? mostActive : n.getLeader();
    }
    
    public void updateLeaderActivity(Player player) {
        String nationId = playerDataManager.getNation(player.getUniqueId());
        if (nationId != null) {
            Nation n = nationManager.getNationById(nationId);
            if (n != null && player.getUniqueId().equals(n.getLeader())) {
                leaderLastSeen.put(nationId, System.currentTimeMillis());
            }
        }
    }
    
    private void resetDailyCounters() {
        long now = System.currentTimeMillis();
        dailyPrints.entrySet().removeIf(e -> now > e.getValue().resetTime);
    }
    
    /**
     * Get remaining daily print limit.
     */
    public synchronized double getRemainingDailyPrint(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 0.0;
        
        DailyPrint dp = dailyPrints.get(nationId);
        if (dp == null) {
            return n.getTreasury() * 0.2; // Full daily limit
        }
        
        double maxDaily = n.getTreasury() * 0.2;
        return Math.max(0, maxDaily - dp.amount);
    }
    
    /**
     * Get comprehensive balancing statistics.
     */
    public synchronized Map<String, Object> getBalancingStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null) {
            stats.put("error", "Нация не найдена.");
            return stats;
        }
        
        // Money printing stats
        DailyPrint dp = dailyPrints.get(nationId);
        double maxDaily = n.getTreasury() * 0.2;
        double used = dp != null ? dp.amount : 0.0;
        double remaining = maxDaily - used;
        
        stats.put("dailyPrintLimit", maxDaily);
        stats.put("dailyPrintUsed", used);
        stats.put("dailyPrintRemaining", remaining);
        stats.put("canPrintMoney", remaining > 0);
        
        // Territory stats
        stats.put("claimedChunks", n.getClaimedChunkKeys().size());
        stats.put("capitalChunk", n.getCapitalChunkStr());
        
        // Leader activity
        Long lastSeen = leaderLastSeen.get(nationId);
        if (lastSeen != null) {
            long inactiveHours = (System.currentTimeMillis() - lastSeen) / 1000 / 60 / 60;
            stats.put("leaderLastSeen", lastSeen);
            stats.put("leaderInactiveHours", inactiveHours);
            stats.put("isLeaderActive", inactiveHours < 336); // 14 days
        } else {
            // Check if leader is online
            if (n.getLeader() != null) {
                Player leader = org.bukkit.Bukkit.getPlayer(n.getLeader());
                stats.put("isLeaderOnline", leader != null && leader.isOnline());
            }
        }
        
        // Nation creation cooldown
        if (n.getLeader() != null) {
            Long lastCreation = nationCreationTime.get(n.getLeader());
            if (lastCreation != null) {
                long cooldownRemaining = (7L * 24 * 60 * 60_000L) - (System.currentTimeMillis() - lastCreation);
                stats.put("canCreateNation", cooldownRemaining <= 0);
                stats.put("nationCreationCooldownHours", cooldownRemaining > 0 ? cooldownRemaining / 1000 / 60 / 60 : 0);
            } else {
                stats.put("canCreateNation", true);
            }
        }
        
        // Balancing rating
        String rating = "БАЛАНСИРОВАННО";
        if (used > maxDaily * 0.9) rating = "ПРИБЛИЖАЕТСЯ К ЛИМИТУ";
        if (n.getClaimedChunkKeys().size() > 100) rating = "ОБШИРНАЯ ТЕРРИТОРИЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get all balancing violations.
     */
    public synchronized List<String> getBalancingViolations(String nationId) {
        List<String> violations = new ArrayList<>();
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return violations;
        
        // Check money printing
        DailyPrint dp = dailyPrints.get(nationId);
        if (dp != null) {
            double maxDaily = n.getTreasury() * 0.2;
            if (dp.amount > maxDaily) {
                violations.add("Превышен дневной лимит печати денег");
            }
        }
        
        // Check leader activity
        Long lastSeen = leaderLastSeen.get(nationId);
        if (lastSeen != null) {
            long inactiveDays = (System.currentTimeMillis() - lastSeen) / 1000 / 60 / 60 / 24;
            if (inactiveDays > 14) {
                violations.add("Лидер неактивен более 14 дней");
            }
        }
        
        return violations;
    }
    
    /**
     * Force reset daily print counter.
     */
    public synchronized void resetDailyPrint(String nationId) {
        dailyPrints.remove(nationId);
        lastPrintTime.remove(nationId);
    }
    
    /**
     * Get nation creation cooldown remaining.
     */
    public synchronized long getNationCreationCooldown(UUID playerId) {
        Long lastCreation = nationCreationTime.get(playerId);
        if (lastCreation == null) return 0;
        
        long cooldown = 7L * 24 * 60 * 60_000L; // 7 days
        long remaining = cooldown - (System.currentTimeMillis() - lastCreation);
        return Math.max(0, remaining);
    }
    
    /**
     * Get global balancing statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalBalancingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalNations = nationManager.getAll().size();
        int nationsWithDailyPrint = dailyPrints.size();
        double totalDailyPrintUsed = 0.0;
        double totalDailyPrintLimit = 0.0;
        
        int nationsWithInactiveLeaders = 0;
        int totalClaimedChunks = 0;
        Map<String, Double> printUsageByNation = new HashMap<>();
        
        for (Nation n : nationManager.getAll()) {
            String nationId = n.getId();
            
            // Daily print stats
            DailyPrint dp = dailyPrints.get(nationId);
            double maxDaily = n.getTreasury() * 0.2;
            double used = dp != null ? dp.amount : 0.0;
            totalDailyPrintLimit += maxDaily;
            totalDailyPrintUsed += used;
            
            if (dp != null) {
                printUsageByNation.put(nationId, used);
            }
            
            // Leader activity
            Long lastSeen = leaderLastSeen.get(nationId);
            if (lastSeen != null) {
                long inactiveDays = (System.currentTimeMillis() - lastSeen) / 1000 / 60 / 60 / 24;
                if (inactiveDays > 14) {
                    nationsWithInactiveLeaders++;
                }
            }
            
            // Territory
            totalClaimedChunks += n.getClaimedChunkKeys().size();
        }
        
        stats.put("totalNations", totalNations);
        stats.put("nationsWithDailyPrint", nationsWithDailyPrint);
        stats.put("totalDailyPrintUsed", totalDailyPrintUsed);
        stats.put("totalDailyPrintLimit", totalDailyPrintLimit);
        stats.put("globalPrintUsageRate", totalDailyPrintLimit > 0 ? 
            (totalDailyPrintUsed / totalDailyPrintLimit) * 100 : 0);
        stats.put("nationsWithInactiveLeaders", nationsWithInactiveLeaders);
        stats.put("totalClaimedChunks", totalClaimedChunks);
        stats.put("averageChunksPerNation", totalNations > 0 ? 
            (double) totalClaimedChunks / totalNations : 0);
        
        // Top nations by daily print usage
        List<Map.Entry<String, Double>> topByPrintUsage = printUsageByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPrintUsage", topByPrintUsage);
        
        // Nations on cooldown for creation
        int nationsOnCooldown = 0;
        long now = System.currentTimeMillis();
        for (Long creationTime : nationCreationTime.values()) {
            if (now - creationTime < 7L * 24 * 60 * 60_000L) {
                nationsOnCooldown++;
            }
        }
        stats.put("nationsOnCreationCooldown", nationsOnCooldown);
        
        // Violations summary
        int totalViolations = 0;
        for (Nation n : nationManager.getAll()) {
            List<String> violations = getBalancingViolations(n.getId());
            totalViolations += violations.size();
        }
        stats.put("totalViolations", totalViolations);
        stats.put("nationsWithViolations", totalViolations > 0 ? 
            (long) nationManager.getAll().stream()
                .filter(n -> !getBalancingViolations(n.getId()).isEmpty())
                .count() : 0);
        
        return stats;
    }
    
    /**
     * Update mod usage balances for all nations.
     * Called periodically to analyze mod usage and apply balancing.
     */
    private void updateModBalances() {
        for (Nation n : nationManager.getAll()) {
            // Update mod usage tracker for this nation
            ModUsageTracker tracker = modUsage.computeIfAbsent(n.getId(), k -> new ModUsageTracker());
            
            // This would normally iterate through nation's blocks/items to count mod usage
            // For now, we'll simulate balance based on unlocked technologies
            updateEnergyBalance(n.getId());
            updateMilitaryBalance(n.getId());
        }
    }
    
    /**
     * Update energy balance for a nation based on mod integration.
     */
    private void updateEnergyBalance(String nationId) {
        EnergyBalance balance = energyBalances.computeIfAbsent(nationId, k -> new EnergyBalance());
        
        // Calculate energy production and consumption based on mod technologies
        double baseProduction = 1000.0; // Base energy production
        double baseConsumption = 500.0; // Base energy consumption
        
        // Boost based on technologies (Industrial Upgrade, IE, etc.)
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "energy_generation")) {
            baseProduction *= 2.0;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "advanced_energy")) {
            baseProduction *= 3.0;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "nuclear_energy")) {
            baseProduction *= 5.0;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "quantum_energy")) {
            baseProduction *= 8.0;
        }
        
        // Calculate efficiency based on mod integration
        double efficiency = 1.0;
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "industrial_engineering") ||
            plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "simple_machines")) {
            efficiency *= 1.2;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "advanced_industry")) {
            efficiency *= 1.5;
        }
        
        balance.totalProduction = baseProduction;
        balance.totalConsumption = baseConsumption;
        balance.efficiencyRating = efficiency;
        balance.lastUpdate = System.currentTimeMillis();
    }
    
    /**
     * Update military balance for a nation based on mod integration.
     */
    private void updateMilitaryBalance(String nationId) {
        MilitaryBalance balance = militaryBalances.computeIfAbsent(nationId, k -> new MilitaryBalance());
        
        // Calculate military ratings based on unlocked technologies
        double offense = 1.0;
        double defense = 1.0;
        int units = 0;
        
        // Tier 1-2 military technologies
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "basic_weapons")) {
            offense *= 1.1;
            defense *= 1.05;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "metal_weapons")) {
            offense *= 1.3;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "basic_fortifications")) {
            defense *= 1.25;
        }
        
        // Tier 3 military technologies
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "firearms_tech") ||
            plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "firearms_tech_pb")) {
            offense *= 1.8;
            units += 50; // Simulate military units
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "tactical_equipment")) {
            defense *= 1.5;
            offense *= 1.1;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "ae2_basic")) {
            // Logistics improve military effectiveness
            offense *= 1.1;
            defense *= 1.1;
        }
        
        // Tier 4 military technologies
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "elite_warfare")) {
            offense *= 2.2;
            units += 100;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "advanced_artillery")) {
            offense *= 2.0;
            defense *= 1.2;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "warium_weapons")) {
            offense *= 2.5;
            units += 75;
        }
        
        // Tier 5 military technologies
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "nuclear_weapons")) {
            offense *= 4.0;
            units += 200; // Significant military increase with nukes
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "military_vehicles")) {
            offense *= 2.0;
            defense *= 2.0;
            units += 150;
        }
        if (plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, "ballistic_missiles")) {
            offense *= 3.5;
        }
        
        balance.offenseRating = offense;
        balance.defenseRating = defense;
        balance.totalUnits = units;
        balance.lastUpdate = System.currentTimeMillis();
    }
    
    /**
     * Check if a nation's military power is balanced (not too OP).
     */
    public boolean isMilitaryPowerBalanced(String nationId) {
        MilitaryBalance balance = militaryBalances.get(nationId);
        if (balance == null) {
            updateMilitaryBalance(nationId);
            balance = militaryBalances.get(nationId);
        }
        
        if (balance == null) return true;
        
        // Limit how much military power one nation can have compared to average
        double avgOffense = getAverageMilitaryOffense();
        double avgDefense = getAverageMilitaryDefense();
        
        // Allow up to 3x average power before considering unbalanced
        return balance.offenseRating <= avgOffense * 3.0 && balance.defenseRating <= avgDefense * 3.0;
    }
    
    /**
     * Get average military offense across all nations.
     */
    private double getAverageMilitaryOffense() {
        if (militaryBalances.isEmpty()) return 1.0;
        
        double total = 0;
        for (MilitaryBalance balance : militaryBalances.values()) {
            total += balance.offenseRating;
        }
        return total / militaryBalances.size();
    }
    
    /**
     * Get average military defense across all nations.
     */
    private double getAverageMilitaryDefense() {
        if (militaryBalances.isEmpty()) return 1.0;
        
        double total = 0;
        for (MilitaryBalance balance : militaryBalances.values()) {
            total += balance.defenseRating;
        }
        return total / militaryBalances.size();
    }
    
    /**
     * Get energy efficiency for a nation (for performance balancing).
     */
    public double getEnergyEfficiency(String nationId) {
        EnergyBalance balance = energyBalances.get(nationId);
        if (balance == null) {
            updateEnergyBalance(nationId);
            balance = energyBalances.get(nationId);
        }
        
        return balance != null ? balance.efficiencyRating : 1.0;
    }
    
    /**
     * Check if energy production is balanced with consumption.
     */
    public boolean isEnergyBalanced(String nationId) {
        EnergyBalance balance = energyBalances.get(nationId);
        if (balance == null) {
            updateEnergyBalance(nationId);
            balance = energyBalances.get(nationId);
        }
        
        if (balance == null) return true;
        
        // Energy consumption shouldn't exceed production by more than 50%
        return balance.totalConsumption <= balance.totalProduction * 1.5;
    }
    
    /**
     * Get military balance statistics for a nation.
     */
    public Map<String, Object> getMilitaryBalanceStats(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        MilitaryBalance balance = militaryBalances.get(nationId);
        if (balance == null) {
            updateMilitaryBalance(nationId);
            balance = militaryBalances.get(nationId);
        }
        
        if (balance != null) {
            stats.put("offenseRating", balance.offenseRating);
            stats.put("defenseRating", balance.defenseRating);
            stats.put("totalUnits", balance.totalUnits);
            stats.put("isBalanced", isMilitaryPowerBalanced(nationId));
            stats.put("averageOffense", getAverageMilitaryOffense());
            stats.put("averageDefense", getAverageMilitaryDefense());
        }
        
        return stats;
    }
    
    /**
     * Get energy balance statistics for a nation.
     */
    public Map<String, Object> getEnergyBalanceStats(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        EnergyBalance balance = energyBalances.get(nationId);
        if (balance == null) {
            updateEnergyBalance(nationId);
            balance = energyBalances.get(nationId);
        }
        
        if (balance != null) {
            stats.put("production", balance.totalProduction);
            stats.put("consumption", balance.totalConsumption);
            stats.put("efficiency", balance.efficiencyRating);
            stats.put("isBalanced", isEnergyBalanced(nationId));
        }
        
        return stats;
    }
}

