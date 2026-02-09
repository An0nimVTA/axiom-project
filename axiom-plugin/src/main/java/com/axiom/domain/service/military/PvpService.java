package com.axiom.domain.service.military;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.UUID;

/** Stores player PvP opt-in flags. */
public class PvpService {
    private final AXIOM plugin;
    private final Set<UUID> enabled = new HashSet<>();

    public PvpService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean isEnabled(UUID uuid) { return uuid != null && enabled.contains(uuid); }
    public synchronized void set(UUID uuid, boolean on) {
        if (uuid == null) return;
        boolean wasEnabled = enabled.contains(uuid);
        if (on) {
            enabled.add(uuid);
            if (!wasEnabled) {
                // VISUAL EFFECTS: PvP enabled
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!p.isOnline()) return;
                        p.sendTitle("§c§l[PvP ВКЛЮЧЕНО]", "§fТеперь вы можете атаковать других игроков", 10, 60, 10);
                        if (plugin.getVisualEffectsService() != null) {
                            plugin.getVisualEffectsService().sendActionBar(p, "§c⚔ PvP включено!");
                        }
                        org.bukkit.Location loc = p.getLocation();
                        if (loc != null && loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0,
                                new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
                            p.playSound(loc, org.bukkit.Sound.ITEM_ARMOR_EQUIP_IRON, 0.7f, 0.7f);
                        }
                    });
                }
            }
        } else {
            enabled.remove(uuid);
            if (wasEnabled) {
                // VISUAL EFFECTS: PvP disabled
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!p.isOnline()) return;
                        p.sendTitle("§a§l[PvP ВЫКЛЮЧЕНО]", "§fВы больше не можете атаковать игроков", 10, 60, 10);
                        if (plugin.getVisualEffectsService() != null) {
                            plugin.getVisualEffectsService().sendActionBar(p, "§a✓ PvP выключено");
                        }
                        org.bukkit.Location loc = p.getLocation();
                        if (loc != null && loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.1);
                            p.playSound(loc, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
                        }
                    });
                }
            }
        }
    }
    
    /**
     * Get comprehensive PvP statistics.
     */
    public synchronized Map<String, Object> getPvpStatistics(UUID uuid) {
        Map<String, Object> stats = new HashMap<>();
        
        boolean enabled = isEnabled(uuid);
        stats.put("enabled", enabled);
        stats.put("status", enabled ? "ВКЛЮЧЕНО" : "ВЫКЛЮЧЕНО");
        
        // Check if player can PvP (not in same nation, etc.)
        String playerNationId = plugin.getPlayerDataManager() != null ? plugin.getPlayerDataManager().getNation(uuid) : null;
        if (playerNationId != null) {
            stats.put("nationId", playerNationId);
            
            // PvP restrictions based on nation
            boolean canPvp = true;
            // Add restrictions logic here if needed
            stats.put("canPvp", canPvp);
        }
        
        return stats;
    }
    
    /**
     * Get count of enabled PvP players.
     */
    public synchronized int getEnabledPvpCount() {
        return enabled.size();
    }
    
    /**
     * Get percentage of players with PvP enabled.
     */
    public synchronized double getPvpEnabledPercentage() {
        int totalOnline = org.bukkit.Bukkit.getOnlinePlayers().size();
        if (totalOnline == 0) return 0.0;
        return (enabled.size() / (double) totalOnline) * 100.0;
    }
    
    /**
     * Get global PvP statistics.
     */
    public synchronized Map<String, Object> getGlobalPvpStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int enabledCount = enabled.size();
        int totalOnline = org.bukkit.Bukkit.getOnlinePlayers().size();
        File playersDir = new File(plugin.getDataFolder(), "players");
        int totalPlayers = playersDir.exists() && playersDir.listFiles() != null ? 
            playersDir.listFiles((d, n) -> n.endsWith(".json")).length : 0;
        
        stats.put("enabledCount", enabledCount);
        stats.put("totalOnline", totalOnline);
        stats.put("totalPlayers", totalPlayers);
        stats.put("enabledPercentage", totalOnline > 0 ? (enabledCount / (double) totalOnline) * 100.0 : 0);
        
        // PvP status by nation
        Map<String, Integer> byNation = new HashMap<>();
        for (UUID uuid : enabled) {
            String nationId = plugin.getPlayerDataManager() != null ? 
                plugin.getPlayerDataManager().getNation(uuid) : null;
            if (nationId != null) {
                byNation.put(nationId, byNation.getOrDefault(nationId, 0) + 1);
            }
        }
        stats.put("pvpByNation", byNation);
        
        // Online vs offline PvP enabled
        int onlineEnabled = 0;
        for (UUID uuid : enabled) {
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            if (player.isOnline()) {
                onlineEnabled++;
            }
        }
        stats.put("onlineEnabled", onlineEnabled);
        stats.put("offlineEnabled", enabledCount - onlineEnabled);
        
        return stats;
    }
}


