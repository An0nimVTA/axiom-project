package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Visualizes nation borders with particles and markers. */
public class BorderVisualizationService {
    private final AXIOM plugin;
    private final Map<UUID, Boolean> playerVisualizationEnabled = new HashMap<>();

    public BorderVisualizationService(AXIOM plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::showBorders, 0, 20 * 2); // every 2 seconds
    }

    public synchronized void toggleVisualization(UUID playerId) {
        playerVisualizationEnabled.put(playerId, !playerVisualizationEnabled.getOrDefault(playerId, false));
    }

    public synchronized boolean isEnabled(UUID playerId) {
        return playerVisualizationEnabled.getOrDefault(playerId, false);
    }

    private void showBorders() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!isEnabled(p.getUniqueId())) continue;
            String nationId = plugin.getPlayerDataManager().getNation(p.getUniqueId());
            if (nationId == null) continue;
            showBordersForPlayer(p, nationId);
        }
    }

    private void showBordersForPlayer(Player p, String nationId) {
        // Show particles at chunk borders
        int chunkX = p.getLocation().getChunk().getX();
        int chunkZ = p.getLocation().getChunk().getZ();
        World w = p.getWorld();
        String currentChunk = w.getName() + ":" + chunkX + ":" + chunkZ;
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return;
        boolean isOwned = n.getClaimedChunkKeys().contains(currentChunk);
        // Check adjacent chunks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                String adjChunk = w.getName() + ":" + (chunkX + dx) + ":" + (chunkZ + dz);
                boolean adjOwned = n.getClaimedChunkKeys().contains(adjChunk);
                if (isOwned != adjOwned) {
                    // Border detected - show particle
                    int x = (chunkX + (dx > 0 ? 15 : 0)) * 16;
                    int z = (chunkZ + (dz > 0 ? 15 : 0)) * 16;
                    p.spawnParticle(isOwned ? Particle.VILLAGER_HAPPY : Particle.REDSTONE,
                        x, p.getLocation().getY(), z, 1, 0, 0, 0, 0);
                }
            }
        }
    }
    
    /**
     * Get comprehensive border visualization statistics.
     */
    public synchronized Map<String, Object> getBorderVisualizationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int enabledCount = 0;
        int disabledCount = 0;
        for (Boolean enabled : playerVisualizationEnabled.values()) {
            if (enabled) enabledCount++;
            else disabledCount++;
        }
        
        stats.put("enabledPlayers", enabledCount);
        stats.put("disabledPlayers", disabledCount);
        stats.put("totalPlayersTracked", playerVisualizationEnabled.size());
        
        // Online players with visualization
        int onlineEnabled = 0;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (isEnabled(p.getUniqueId())) {
                onlineEnabled++;
            }
        }
        stats.put("onlineEnabled", onlineEnabled);
        stats.put("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
        
        // Update interval
        stats.put("updateIntervalSeconds", 2);
        stats.put("particleType", "VILLAGER_HAPPY / REDSTONE");
        
        return stats;
    }
    
    /**
     * Get visualization status for player.
     */
    public synchronized Map<String, Object> getPlayerVisualizationStatus(UUID playerId) {
        Map<String, Object> status = new HashMap<>();
        
        boolean enabled = isEnabled(playerId);
        status.put("enabled", enabled);
        
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            status.put("isOnline", true);
            String nationId = plugin.getPlayerDataManager().getNation(playerId);
            status.put("hasNation", nationId != null);
            if (nationId != null) {
                status.put("nationId", nationId);
                com.axiom.model.Nation nation = plugin.getNationManager().getNationById(nationId);
                if (nation != null) {
                    status.put("nationName", nation.getName());
                    status.put("chunksCount", nation.getClaimedChunkKeys().size());
                }
            }
        } else {
            status.put("isOnline", false);
        }
        
        return status;
    }
    
    /**
     * Enable visualization for player.
     */
    public synchronized void enableVisualization(UUID playerId) {
        playerVisualizationEnabled.put(playerId, true);
    }
    
    /**
     * Disable visualization for player.
     */
    public synchronized void disableVisualization(UUID playerId) {
        playerVisualizationEnabled.put(playerId, false);
    }
    
    /**
     * Clear visualization data for player.
     */
    public synchronized void clearVisualization(UUID playerId) {
        playerVisualizationEnabled.remove(playerId);
    }
}

