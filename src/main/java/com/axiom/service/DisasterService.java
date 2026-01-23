package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;

/** Generates and manages natural disasters affecting nations. */
public class DisasterService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, ActiveDisaster> activeDisasters = new HashMap<>(); // nationId -> disaster

    public static class ActiveDisaster {
        String nationId;
        String type; // "earthquake", "flood", "drought", "storm"
        double severity; // 0-100
        long startTime;
        long durationMinutes;
    }

    public DisasterService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::generateDisasters, 0, 20 * 60 * 30); // every 30 minutes
    }

    public synchronized void triggerDisaster(String nationId, String type, double severity, long durationMinutes) {
        ActiveDisaster d = new ActiveDisaster();
        d.nationId = nationId;
        d.type = type;
        d.severity = severity;
        d.startTime = System.currentTimeMillis();
        d.durationMinutes = durationMinutes;
        activeDisasters.put(nationId, d);
        applyDisasterEffects(d);
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Природная катастрофа: " + getDisasterName(type) + " (уровень: " + severity + "%)");
        }
    }

    private String getDisasterName(String type) {
        return switch (type) {
            case "earthquake" -> "Землетрясение";
            case "flood" -> "Наводнение";
            case "drought" -> "Засуха";
            case "storm" -> "Шторм";
            default -> type;
        };
    }

    private void applyDisasterEffects(ActiveDisaster d) {
        Nation n = nationManager.getNationById(d.nationId);
        if (n == null) return;
        // Reduce economy (happiness penalty is applied via NationModifierService indirectly)
        plugin.getNationModifierService().addModifier(d.nationId, "economy", "penalty", d.severity / 100.0, d.durationMinutes);
        
        // VISUAL EFFECTS: Broadcast disaster with effects
        String disasterName = getDisasterName(d.type);
        String msg = "§c[КАТАСТРОФА] §f" + disasterName + " затронула нацию " + n.getName() + "!";
        
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID citizen : n.getCitizens()) {
                org.bukkit.entity.Player p = Bukkit.getPlayer(citizen);
                if (p != null && p.isOnline()) {
                    // Title announcement
                    p.sendTitle("§c§l[КАТАСТРОФА]", "§f" + disasterName + " в вашей нации!", 10, 100, 20);
                    
                    // Actionbar
                    plugin.getVisualEffectsService().sendActionBar(p, msg + " §7(Уровень: " + String.format("%.0f", d.severity) + "%)");
                    
                    // Visual effects based on disaster type
                    org.bukkit.Location loc = p.getLocation();
                    switch (d.type) {
                        case "earthquake":
                            // Ground shake particles
                            loc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK_CRACK, loc, 30, 3, 0, 3, 0.1, org.bukkit.Material.STONE.createBlockData());
                            p.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.5f);
                            break;
                        case "flood":
                            // Water particles
                            loc.getWorld().spawnParticle(org.bukkit.Particle.WATER_SPLASH, loc, 40, 5, 0.5, 5, 0.2);
                            p.playSound(loc, org.bukkit.Sound.BLOCK_WATER_AMBIENT, 0.8f, 0.7f);
                            break;
                        case "drought":
                            // Fire/smoke particles
                            loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_LARGE, loc, 30, 5, 2, 5, 0.15);
                            p.playSound(loc, org.bukkit.Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.8f);
                            break;
                        case "storm":
                            // Lightning-like particles
                            loc.getWorld().spawnParticle(org.bukkit.Particle.CRIT, loc, 50, 5, 3, 5, 0.3);
                            loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc, 20, 3, 2, 3, 0, 
                                new org.bukkit.Particle.DustOptions(org.bukkit.Color.YELLOW, 2.0f));
                            p.playSound(loc, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 0.8f);
                            break;
                    }
                }
            }
        });
    }

    private void generateDisasters() {
        // Random disaster generation
        if (Math.random() < 0.1) { // 10% chance
            List<Nation> nations = new ArrayList<>(nationManager.getAll());
            if (nations.isEmpty()) return;
            Nation target = nations.get((int)(Math.random() * nations.size()));
            String[] types = {"earthquake", "flood", "drought", "storm"};
            String type = types[(int)(Math.random() * types.length)];
            double severity = 20 + Math.random() * 60; // 20-80
            triggerDisaster(target.getId(), type, severity, 60); // 1 hour
        }
    }
    
    /**
     * Get comprehensive disaster statistics.
     */
    public synchronized Map<String, Object> getDisasterStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        ActiveDisaster active = activeDisasters.get(nationId);
        if (active != null) {
            Map<String, Object> activeInfo = new HashMap<>();
            activeInfo.put("type", active.type);
            activeInfo.put("typeName", getDisasterName(active.type));
            activeInfo.put("severity", active.severity);
            activeInfo.put("startTime", active.startTime);
            activeInfo.put("durationMinutes", active.durationMinutes);
            activeInfo.put("timeRemaining", Math.max(0, (active.startTime + active.durationMinutes * 60_000L - System.currentTimeMillis()) / 1000 / 60));
            stats.put("activeDisaster", activeInfo);
        } else {
            stats.put("activeDisaster", null);
        }
        
        // Get historical disasters from nation history
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            long disasterCount = n.getHistory().stream()
                .filter(h -> h.contains("катастрофа") || h.contains("Катастрофа"))
                .count();
            stats.put("historicalDisasters", disasterCount);
        }
        
        return stats;
    }
    
    /**
     * End an active disaster early (through relief efforts).
     */
    public synchronized String endDisaster(String nationId, double cost) throws IOException {
        ActiveDisaster d = activeDisasters.remove(nationId);
        if (d == null) return "Нет активных катастроф.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств для помощи пострадавшим.";
        
        n.setTreasury(n.getTreasury() - cost);
        nationManager.save(n);
        
        // VISUAL EFFECTS: Relief effort success
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = "§a✓ Катастрофа устранена благодаря усилиям по оказанию помощи!";
            for (UUID citizen : n.getCitizens()) {
                org.bukkit.entity.Player p = Bukkit.getPlayer(citizen);
                if (p != null && p.isOnline()) {
                    plugin.getVisualEffectsService().sendActionBar(p, msg);
                    org.bukkit.Location loc = p.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                }
            }
        });
        
        return "Катастрофа устранена. Затрачено: " + cost;
    }
    
    /**
     * Get disaster risk level for a nation.
     */
    public synchronized double getDisasterRisk(String nationId) {
        // Base risk: 10%
        double risk = 10.0;
        
        // Infrastructure reduces risk (simplified - infrastructure check removed as service may not be initialized)
        // This can be enhanced when InfrastructureService is fully integrated
        
        // Economic instability increases risk
        Nation n = nationManager.getNationById(nationId);
        if (n != null && n.getTreasury() < 1000) {
            risk += 10.0; // +10% if poor
        }
        
        return Math.max(0, Math.min(100, risk));
    }
    
    /**
     * Get global disaster statistics.
     */
    public synchronized Map<String, Object> getGlobalDisasterStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeDisasters", activeDisasters.size());
        
        Map<String, Integer> byType = new HashMap<>();
        double totalSeverity = 0.0;
        
        for (ActiveDisaster d : activeDisasters.values()) {
            byType.put(d.type, byType.getOrDefault(d.type, 0) + 1);
            totalSeverity += d.severity;
        }
        
        stats.put("disastersByType", byType);
        stats.put("averageSeverity", activeDisasters.size() > 0 ? totalSeverity / activeDisasters.size() : 0);
        
        // Nations with disasters
        stats.put("nationsWithDisasters", activeDisasters.size());
        
        return stats;
    }
}

