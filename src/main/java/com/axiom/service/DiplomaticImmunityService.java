package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Grants diplomatic immunity to embassy staff. */
public class DiplomaticImmunityService {
    private final AXIOM plugin;
    private final Map<UUID, String> immunePlayers = new HashMap<>(); // playerId -> missionId

    public DiplomaticImmunityService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized void grantImmunity(UUID playerId, String missionId) {
        immunePlayers.put(playerId, missionId);
    }

    public synchronized void revokeImmunity(UUID playerId) {
        immunePlayers.remove(playerId);
    }

    public synchronized boolean hasImmunity(UUID playerId) {
        return immunePlayers.containsKey(playerId);
    }

    public synchronized boolean canAttack(UUID attackerId, UUID targetId) {
        // Diplomats cannot be attacked, and cannot attack
        if (hasImmunity(attackerId) || hasImmunity(targetId)) {
            Player attacker = plugin.getServer().getPlayer(attackerId);
            if (attacker != null) {
                attacker.sendMessage("§cДипломатический иммунитет защищает от нападения.");
            }
            return false;
        }
        return true;
    }
    
    /**
     * Get comprehensive diplomatic immunity statistics.
     */
    public synchronized Map<String, Object> getImmunityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalImmunePlayers", immunePlayers.size());
        
        // Group by mission
        Map<String, Integer> byMission = new HashMap<>();
        for (String missionId : immunePlayers.values()) {
            byMission.put(missionId, byMission.getOrDefault(missionId, 0) + 1);
        }
        stats.put("byMission", byMission);
        
        // Player details
        List<Map<String, Object>> playersList = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : immunePlayers.entrySet()) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("playerId", entry.getKey().toString());
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());
            playerData.put("playerName", player.getName());
            playerData.put("missionId", entry.getValue());
            playerData.put("isOnline", player.isOnline());
            playersList.add(playerData);
        }
        stats.put("playersList", playersList);
        
        return stats;
    }
    
    /**
     * Get immunity for specific player.
     */
    public synchronized Map<String, Object> getPlayerImmunity(UUID playerId) {
        Map<String, Object> data = new HashMap<>();
        
        boolean hasImmunity = hasImmunity(playerId);
        data.put("hasImmunity", hasImmunity);
        
        if (hasImmunity) {
            String missionId = immunePlayers.get(playerId);
            data.put("missionId", missionId);
        }
        
        return data;
    }
    
    /**
     * Get all immune players.
     */
    public synchronized Set<UUID> getAllImmunePlayers() {
        return new HashSet<>(immunePlayers.keySet());
    }
    
    /**
     * Bulk revoke immunity for mission.
     */
    public synchronized int revokeMissionImmunity(String missionId) {
        int revoked = 0;
        Set<UUID> toRemove = new HashSet<>();
        
        for (Map.Entry<UUID, String> entry : immunePlayers.entrySet()) {
            if (entry.getValue().equals(missionId)) {
                toRemove.add(entry.getKey());
                revoked++;
            }
        }
        
        for (UUID playerId : toRemove) {
            immunePlayers.remove(playerId);
        }
        
        return revoked;
    }
    
    /**
     * Get global diplomatic immunity statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalImmunityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalImmunePlayers", immunePlayers.size());
        
        // Group by mission
        Map<String, Integer> byMission = new HashMap<>();
        for (String missionId : immunePlayers.values()) {
            byMission.put(missionId, byMission.getOrDefault(missionId, 0) + 1);
        }
        stats.put("byMission", byMission);
        
        // Online vs offline
        int online = 0, offline = 0;
        for (UUID playerId : immunePlayers.keySet()) {
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(playerId);
            if (player.isOnline()) online++;
            else offline++;
        }
        stats.put("onlineImmune", online);
        stats.put("offlineImmune", offline);
        
        // Total unique missions
        stats.put("totalMissions", byMission.size());
        
        // Average immune players per mission
        stats.put("averageImmunePerMission", byMission.size() > 0 ? 
            (double) immunePlayers.size() / byMission.size() : 0);
        
        // Top missions by immune players
        List<Map.Entry<String, Integer>> topByImmune = byMission.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByImmune", topByImmune);
        
        return stats;
    }
}

