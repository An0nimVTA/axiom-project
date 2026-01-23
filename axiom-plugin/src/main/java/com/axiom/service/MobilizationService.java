package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/** Manages mobilization and conscription during wartime. */
public class MobilizationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final DiplomacySystem diplomacySystem;
    private final Map<String, Set<UUID>> mobilizedPlayers = new HashMap<>(); // nationId -> players

    public MobilizationService(AXIOM plugin, NationManager nationManager, DiplomacySystem diplomacySystem) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.diplomacySystem = diplomacySystem;
        Bukkit.getScheduler().runTaskTimer(plugin, this::applyMobilizationBuffs, 0, 20 * 60); // every minute
    }

    public synchronized void mobilize(String nationId) {
        if (nationManager == null || nationId == null || nationId.trim().isEmpty()) return;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return;
        Set<UUID> mobilized = mobilizedPlayers.computeIfAbsent(nationId, k -> new HashSet<>());
        if (n.getCitizens() != null) {
            mobilized.addAll(n.getCitizens());
        }
        
        // VISUAL EFFECTS: Announce mobilization to all citizens
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (n.getCitizens() == null) {
                return;
            }
            for (UUID uuid : n.getCitizens()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    // Apply buffs
                    applyBuffs(p);
                    
                    // Visual effects
                    if (plugin.getVisualEffectsService() != null) {
                        plugin.getVisualEffectsService().sendActionBar(p, "§c⚔ МОБИЛИЗАЦИЯ! Strength I + Resistance I");
                    }
                    p.sendTitle("§c§l[МОБИЛИЗАЦИЯ]", "§fНация мобилизована!", 10, 60, 10);
                    
                    // Orange particles
                    org.bukkit.Location loc = p.getLocation();
                    if (loc.getWorld() != null) {
                        for (int i = 0; i < 20; i++) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0,
                                new org.bukkit.Particle.DustOptions(org.bukkit.Color.ORANGE, 2.0f));
                        }
                    }
                    
                    // Sound
                    p.playSound(loc, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);
                }
            }
        });
    }

    public synchronized void demobilize(String nationId) {
        mobilizedPlayers.remove(nationId);
    }

    private synchronized void applyMobilizationBuffs() {
        Iterator<Map.Entry<String, Set<UUID>>> iterator = mobilizedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Set<UUID>> entry = iterator.next();
            String nationId = entry.getKey();
            Nation n = nationManager.getNationById(nationId);
            if (n == null) {
                iterator.remove();
                continue;
            }
            // Check if still at war
            boolean atWar = false;
            if (diplomacySystem != null && n.getEnemies() != null) {
                for (String enemyId : n.getEnemies()) {
                    if (diplomacySystem.isAtWar(nationId, enemyId)) {
                        atWar = true;
                        break;
                    }
                }
            }
            if (!atWar) {
                iterator.remove();
                continue;
            }
            // Apply buffs
            for (UUID uuid : entry.getValue()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    applyBuffs(p);
                }
            }
        }
    }

    private void applyBuffs(Player p) {
        if (p == null) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 120, 0, false, false)); // Strength I, 2 min
        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 120, 0, false, false)); // Resistance I, 2 min
        
        // Occasional visual feedback (orange particles)
        if (Math.random() < 0.05) {
            org.bukkit.Location loc = p.getLocation();
            if (loc.getWorld() != null) {
                loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc.add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.ORANGE, 1.5f));
            }
        }
    }

    public synchronized boolean isMobilized(UUID playerId) {
        if (playerId == null) return false;
        for (Set<UUID> mobilized : mobilizedPlayers.values()) {
            if (mobilized.contains(playerId)) return true;
        }
        return false;
    }
    
    /**
     * Get comprehensive mobilization statistics.
     */
    public synchronized Map<String, Object> getMobilizationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null || nationId == null || nationId.trim().isEmpty()) {
            stats.put("isMobilized", false);
            stats.put("mobilizedCount", 0);
            stats.put("totalCitizens", 0);
            stats.put("mobilizationRate", 0);
            stats.put("isAtWar", false);
            stats.put("onlineMobilized", 0);
            stats.put("status", "НЕ МОБИЛИЗОВАНА");
            return stats;
        }
        Set<UUID> mobilized = mobilizedPlayers.get(nationId);
        if (mobilized == null) mobilized = Collections.emptySet();
        
        int mobilizedCount = mobilized.size();
        int totalCitizens = 0;
        boolean isMobilized = !mobilized.isEmpty();
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            totalCitizens = n.getCitizens() != null ? n.getCitizens().size() : 0;
        }
        
        stats.put("isMobilized", isMobilized);
        stats.put("mobilizedCount", mobilizedCount);
        stats.put("totalCitizens", totalCitizens);
        stats.put("mobilizationRate", totalCitizens > 0 ? ((double) mobilizedCount / totalCitizens) * 100 : 0);
        
        // Check if at war
        boolean atWar = false;
        if (n != null && diplomacySystem != null && n.getEnemies() != null) {
            for (String enemyId : n.getEnemies()) {
                if (diplomacySystem.isAtWar(nationId, enemyId)) {
                    atWar = true;
                    break;
                }
            }
        }
        stats.put("isAtWar", atWar);
        
        // Online mobilized players
        int onlineMobilized = 0;
        for (UUID uuid : mobilized) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                onlineMobilized++;
            }
        }
        stats.put("onlineMobilized", onlineMobilized);
        
        // Mobilization status
        String status = "НЕ МОБИЛИЗОВАНА";
        if (isMobilized && atWar) status = "АКТИВНО МОБИЛИЗОВАНА";
        else if (isMobilized) status = "МОБИЛИЗОВАНА (НО НЕ В ВОЙНЕ)";
        else if (atWar) status = "В ВОЙНЕ (НЕ МОБИЛИЗОВАНА)";
        stats.put("status", status);
        
        return stats;
    }
    
    /**
     * Get global mobilization statistics.
     */
    public synchronized Map<String, Object> getGlobalMobilizationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalMobilizedNations = mobilizedPlayers.size();
        int totalMobilizedPlayers = mobilizedPlayers.values().stream()
            .mapToInt(Set::size)
            .sum();
        
        stats.put("totalMobilizedNations", totalMobilizedNations);
        stats.put("totalMobilizedPlayers", totalMobilizedPlayers);
        
        // Mobilization by nation
        Map<String, Integer> byNation = new HashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : mobilizedPlayers.entrySet()) {
            byNation.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("mobilizationByNation", byNation);
        
        // Top mobilized nations
        List<Map.Entry<String, Integer>> topMobilized = byNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topMobilized", topMobilized);
        
        // Online mobilized
        int onlineCount = 0;
        for (Set<UUID> mobilized : mobilizedPlayers.values()) {
            for (UUID uuid : mobilized) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    onlineCount++;
                }
            }
        }
        stats.put("onlineMobilized", onlineCount);
        stats.put("offlineMobilized", totalMobilizedPlayers - onlineCount);
        
        return stats;
    }
}

