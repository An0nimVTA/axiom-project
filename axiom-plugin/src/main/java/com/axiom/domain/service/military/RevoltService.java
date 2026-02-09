package com.axiom.domain.service.military;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;
import com.axiom.domain.service.state.HappinessService;
import com.axiom.domain.service.state.NationManager;

/** Manages rebellions and revolts in nations. */
public class RevoltService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final HappinessService happinessService;
    private final Map<String, Revolt> activeRevolts = new HashMap<>(); // nationId -> revolt

    public static class Revolt {
        String nationId;
        double supportLevel; // 0-100%
        long startTime;
        String leaderName;
    }

    public RevoltService(AXIOM plugin, NationManager nationManager, HappinessService happinessService) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.happinessService = happinessService;
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkRevoltConditions, 0, 20 * 60 * 10); // every 10 minutes
    }

    private synchronized void checkRevoltConditions() {
        if (nationManager == null || happinessService == null) return;
        Collection<Nation> nations = nationManager.getAll();
        if (nations == null) return;
        for (Nation n : nations) {
            if (n == null || isBlank(n.getId())) continue;
            if (activeRevolts.containsKey(n.getId())) continue;
            double happiness = happinessService.getNationHappiness(n.getId());
            if (happiness < 20 && Math.random() < 0.1) {
                startRevolt(n.getId());
            }
        }
    }

    private void startRevolt(String nationId) {
        if (nationManager == null || isBlank(nationId)) return;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return;
        Revolt r = new Revolt();
        r.nationId = nationId;
        r.supportLevel = 20.0;
        r.startTime = System.currentTimeMillis();
        r.leaderName = "Лидер повстанцев";
        activeRevolts.put(nationId, r);
        if (n.getHistory() != null) {
            n.getHistory().add("Началось восстание! Уровень поддержки: " + r.supportLevel + "%");
        }
        try { nationManager.save(n); } catch (Exception ignored) {}
        // Apply penalties
        if (plugin.getNationModifierService() != null) {
            plugin.getNationModifierService().addModifier(nationId, "economy", "penalty", 0.15, 1440);
        }
        
        // VISUAL EFFECTS: Announce revolt
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = "§c⚠ ВОССТАНИЕ! Уровень поддержки: §f" + String.format("%.1f", r.supportLevel) + "%";
            Collection<UUID> citizens = n.getCitizens();
            if (citizens == null) return;
            for (UUID citizenId : citizens) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§c§l[ВОССТАНИЕ!]", "§fУровень поддержки: " + String.format("%.1f", r.supportLevel) + "%", 10, 100, 20);
                    if (plugin.getVisualEffectsService() != null) {
                        plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                    }
                    org.bukkit.Location loc = citizen.getLocation();
                    if (loc != null && loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_LARGE, loc.add(0, 1, 0), 30, 1, 1, 1, 0.15);
                        loc.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc, 15, 0.5, 0.5, 0.5, 0.05);
                        citizen.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.7f);
                    }
                }
            }
        });
    }

    public synchronized String suppressRevolt(String nationId, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        Revolt r = activeRevolts.get(nationId);
        if (r == null) return "Восстания нет.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        activeRevolts.remove(nationId);
        n.setTreasury(n.getTreasury() - cost);
        if (n.getHistory() != null) {
            n.getHistory().add("Восстание подавлено за " + cost + " средств.");
        }
        try { nationManager.save(n); } catch (Exception ignored) {}
        
        // VISUAL EFFECTS: Announce revolt suppression
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = "§a✓ Восстание подавлено! Потрачено: §f" + String.format("%.0f", cost);
            Collection<UUID> citizens = n.getCitizens();
            if (citizens == null) return;
            for (UUID citizenId : citizens) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[ВОССТАНИЕ ПОДАВЛЕНО]", "§fПорядок восстановлен", 10, 80, 20);
                    if (plugin.getVisualEffectsService() != null) {
                        plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                    }
                    org.bukkit.Location loc = citizen.getLocation();
                    if (loc != null && loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                        citizen.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                    }
                }
            }
        });
        
        return "Восстание подавлено.";
    }

    public synchronized boolean isInRevolt(String nationId) {
        if (isBlank(nationId)) return false;
        return activeRevolts.containsKey(nationId);
    }
    
    /**
     * Get comprehensive revolt statistics.
     */
    public synchronized Map<String, Object> getRevoltStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        Revolt revolt = activeRevolts.get(nationId);
        if (revolt != null) {
            stats.put("isActive", true);
            stats.put("supportLevel", revolt.supportLevel);
            stats.put("leaderName", revolt.leaderName);
            stats.put("startTime", revolt.startTime);
            stats.put("duration", (System.currentTimeMillis() - revolt.startTime) / 1000 / 60); // minutes
            
            // Calculate revolt risk factors
            double happiness = happinessService != null ? happinessService.getNationHappiness(nationId) : 50.0;
            stats.put("happiness", happiness);
            
            // Revolt severity rating
            String rating = "НИЗКИЙ";
            if (revolt.supportLevel >= 70) rating = "КРИТИЧЕСКИЙ";
            else if (revolt.supportLevel >= 50) rating = "ВЫСОКИЙ";
            else if (revolt.supportLevel >= 30) rating = "СРЕДНИЙ";
            stats.put("severityRating", rating);
            
            // Economic impact estimate
            double economicImpact = -(revolt.supportLevel * 0.01); // -0.01% per 1% support
            stats.put("economicImpact", economicImpact);
        } else {
            stats.put("isActive", false);
            
            // Calculate revolt risk
            double happiness = happinessService != null ? happinessService.getNationHappiness(nationId) : 50.0;
            double revoltRisk = happiness < 30 ? (30 - happiness) * 2.0 : 0.0; // Risk percentage
            stats.put("revoltRisk", Math.min(100, revoltRisk));
            
            String riskRating = "ОТСУТСТВУЕТ";
            if (revoltRisk >= 50) riskRating = "ОЧЕНЬ ВЫСОКИЙ";
            else if (revoltRisk >= 30) riskRating = "ВЫСОКИЙ";
            else if (revoltRisk >= 15) riskRating = "СРЕДНИЙ";
            else if (revoltRisk > 0) riskRating = "НИЗКИЙ";
            stats.put("riskRating", riskRating);
        }
        
        return stats;
    }
    
    /**
     * Increase revolt support (through events, etc.).
     */
    public synchronized String increaseRevoltSupport(String nationId, double amount) {
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (!Double.isFinite(amount) || amount <= 0) return "Некорректное значение.";
        Revolt revolt = activeRevolts.get(nationId);
        if (revolt == null) return "Восстания нет.";
        
        revolt.supportLevel = Math.min(100, revolt.supportLevel + amount);
        
        Nation n = nationManager != null ? nationManager.getNationById(nationId) : null;
        if (n != null) {
            if (n.getHistory() != null) {
                n.getHistory().add("Поддержка восстания выросла до " + String.format("%.1f", revolt.supportLevel) + "%");
            }
            try { nationManager.save(n); } catch (Exception ignored) {}
        }
        
        return "Поддержка восстания: " + String.format("%.1f", revolt.supportLevel) + "%";
    }
    
    /**
     * Reduce revolt support (through concessions, etc.).
     */
    public synchronized String reduceRevoltSupport(String nationId, double amount, double cost) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (!Double.isFinite(amount) || amount <= 0) return "Некорректное значение.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        Revolt revolt = activeRevolts.get(nationId);
        if (revolt == null) return "Восстания нет.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        revolt.supportLevel = Math.max(0, revolt.supportLevel - amount);
        n.setTreasury(n.getTreasury() - cost);
        
        if (revolt.supportLevel <= 0) {
            activeRevolts.remove(nationId);
            if (n.getHistory() != null) {
                n.getHistory().add("Восстание прекращено через уступки");
            }
        } else {
            if (n.getHistory() != null) {
                n.getHistory().add("Поддержка восстания снижена до " + String.format("%.1f", revolt.supportLevel) + "%");
            }
        }
        
        nationManager.save(n);
        
        return "Поддержка восстания: " + String.format("%.1f", revolt.supportLevel) + "%";
    }
    
    /**
     * Get revolt support level.
     */
    public synchronized double getRevoltSupport(String nationId) {
        if (isBlank(nationId)) return 0.0;
        Revolt revolt = activeRevolts.get(nationId);
        return revolt != null ? revolt.supportLevel : 0.0;
    }
    
    /**
     * Calculate suppression cost based on support level.
     */
    public synchronized double calculateSuppressionCost(String nationId) {
        if (isBlank(nationId)) return 0.0;
        Revolt revolt = activeRevolts.get(nationId);
        if (revolt == null) return 0.0;
        
        // Base cost scales with support level
        return revolt.supportLevel * 100.0;
    }
    
    /**
     * Get global revolt statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalRevoltStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeRevolts", activeRevolts.size());
        
        double totalSupportLevel = 0.0;
        double maxSupportLevel = 0.0;
        Map<String, Double> revoltsByNation = new HashMap<>();
        
        for (Map.Entry<String, Revolt> entry : activeRevolts.entrySet()) {
            Revolt revolt = entry.getValue();
            if (revolt == null) continue;
            double support = revolt.supportLevel;
            totalSupportLevel += support;
            maxSupportLevel = Math.max(maxSupportLevel, support);
            revoltsByNation.put(entry.getKey(), support);
        }
        
        stats.put("totalActiveRevolts", activeRevolts.size());
        stats.put("averageSupportLevel", activeRevolts.size() > 0 ? totalSupportLevel / activeRevolts.size() : 0);
        stats.put("maxSupportLevel", maxSupportLevel);
        
        // Revolt severity distribution
        int critical = 0, high = 0, medium = 0, low = 0;
        for (Revolt r : activeRevolts.values()) {
            if (r == null) continue;
            if (r.supportLevel >= 70) critical++;
            else if (r.supportLevel >= 50) high++;
            else if (r.supportLevel >= 30) medium++;
            else low++;
        }
        
        Map<String, Integer> severityDistribution = new HashMap<>();
        severityDistribution.put("critical", critical);
        severityDistribution.put("high", high);
        severityDistribution.put("medium", medium);
        severityDistribution.put("low", low);
        stats.put("severityDistribution", severityDistribution);
        
        // Top nations by revolt support (worst)
        List<Map.Entry<String, Double>> topBySupport = revoltsByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySupport", topBySupport);
        
        // Calculate total revolt risk across all nations
        int nationsAtRisk = 0;
        double totalRisk = 0.0;
        if (nationManager == null || happinessService == null) {
            stats.put("nationsAtRisk", 0);
            stats.put("averageRisk", 0);
            return stats;
        }
        List<Nation> nations = new ArrayList<>(nationManager.getAll());
        if (nations == null) {
            stats.put("nationsAtRisk", 0);
            stats.put("averageRisk", 0);
            return stats;
        }
        for (Nation n : nations) {
            if (n == null || isBlank(n.getId())) continue;
            if (!activeRevolts.containsKey(n.getId())) {
                double happiness = happinessService.getNationHappiness(n.getId());
                double revoltRisk = happiness < 30 ? (30 - happiness) * 2.0 : 0.0;
                if (revoltRisk > 0) {
                    nationsAtRisk++;
                    totalRisk += revoltRisk;
                }
            }
        }
        stats.put("nationsAtRisk", nationsAtRisk);
        stats.put("averageRisk", nationsAtRisk > 0 ? totalRisk / nationsAtRisk : 0);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

