package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tracks conquered territories and conquest mechanics. */
public class ConquestService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File conquestsDir;
    private final Map<String, Map<String, Integer>> conquestProgress = new HashMap<>(); // attacker -> target -> progress %

    public ConquestService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.conquestsDir = new File(plugin.getDataFolder(), "conquests");
        this.conquestsDir.mkdirs();
        loadAll();
    }

    public synchronized void updateConquestProgress(String attackerId, String defenderId, int progress) {
        if (attackerId == null || defenderId == null) {
            return;
        }
        Map<String, Integer> targets = conquestProgress.computeIfAbsent(attackerId, k -> new HashMap<>());
        int current = targets.getOrDefault(defenderId, 0);
        int next = Math.max(0, Math.min(100, current + progress));
        targets.put(defenderId, next);
        saveConquest(attackerId);
        if (next >= 100) {
            completeConquest(attackerId, defenderId);
        }
    }

    private void completeConquest(String attackerId, String defenderId) {
        if (nationManager == null) return;
        Nation attacker = nationManager.getNationById(attackerId);
        Nation defender = nationManager.getNationById(defenderId);
        if (attacker == null || defender == null) return;
        // Transfer some territories
        int transferCount = Math.min(5, defender.getClaimedChunkKeys().size() / 4);
        int transferred = 0;
        for (String chunk : new java.util.ArrayList<>(defender.getClaimedChunkKeys())) {
            if (transferred >= transferCount) break;
            defender.getClaimedChunkKeys().remove(chunk);
            attacker.getClaimedChunkKeys().add(chunk);
            transferred++;
        }
        Map<String, Integer> targets = conquestProgress.get(attackerId);
        if (targets != null) {
            targets.remove(defenderId);
        }
        saveConquest(attackerId);
        try {
            nationManager.save(attacker);
            nationManager.save(defender);
        } catch (Exception ignored) {}
        
        // VISUAL EFFECTS: Announce conquest completion
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            VisualEffectsService effects = plugin.getVisualEffectsService();
            String msg1 = "§a✓ Завоевание завершено! Получено территорий: " + transferCount;
            Collection<UUID> attackerCitizens = attacker.getCitizens() != null ? attacker.getCitizens() : Collections.emptyList();
            for (UUID citizenId : attackerCitizens) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[ЗАВОЕВАНИЕ]", "§fТерритории '" + defender.getName() + "' захвачены", 10, 100, 20);
                    if (effects != null) {
                        effects.sendActionBar(citizen, msg1);
                    }
                    org.bukkit.Location loc = citizen.getLocation();
                    for (int i = 0; i < 20; i++) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                    }
                    citizen.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                }
            }
            String msg2 = "§c⚠ Потеряно территорий: " + transferCount;
            Collection<UUID> defenderCitizens = defender.getCitizens() != null ? defender.getCitizens() : Collections.emptyList();
            for (UUID citizenId : defenderCitizens) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§c§l[ПОТЕРЯ ТЕРРИТОРИЙ]", "§f" + transferCount + " территорий потеряно", 10, 100, 20);
                    if (effects != null) {
                        effects.sendActionBar(citizen, msg2);
                    }
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_LARGE, loc.add(0, 1, 0), 20, 1, 1, 1, 0.1);
                    citizen.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.8f);
                }
            }
        });
    }

    public synchronized int getConquestProgress(String attackerId, String defenderId) {
        Map<String, Integer> targets = conquestProgress.get(attackerId);
        return targets != null ? targets.getOrDefault(defenderId, 0) : 0;
    }

    private void loadAll() {
        File[] files = conquestsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String attackerId = f.getName().replace(".json", "");
                Map<String, Integer> targets = new HashMap<>();
                for (var entry : o.entrySet()) {
                    targets.put(entry.getKey(), entry.getValue().getAsInt());
                }
                conquestProgress.put(attackerId, targets);
            } catch (Exception ignored) {}
        }
    }

    private void saveConquest(String attackerId) {
        File f = new File(conquestsDir, attackerId + ".json");
        JsonObject o = new JsonObject();
        Map<String, Integer> targets = conquestProgress.get(attackerId);
        if (targets != null) {
            for (var entry : targets.entrySet()) {
                o.addProperty(entry.getKey(), entry.getValue());
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive conquest statistics.
     */
    public synchronized Map<String, Object> getConquestStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // As attacker
        Map<String, Integer> attacking = conquestProgress.get(nationId);
        if (attacking == null) attacking = new HashMap<>();
        
        int totalTargets = attacking.size();
        int completedConquests = 0;
        int activeConquests = 0;
        double averageProgress = 0.0;
        
        List<Map<String, Object>> activeList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : attacking.entrySet()) {
            if (entry.getValue() >= 100) {
                completedConquests++;
            } else {
                activeConquests++;
                averageProgress += entry.getValue();
                activeList.add(java.util.Map.of(
                    "target", entry.getKey(),
                    "progress", entry.getValue()
                ));
            }
        }
        
        stats.put("totalTargets", totalTargets);
        stats.put("completedConquests", completedConquests);
        stats.put("activeConquests", activeConquests);
        stats.put("averageProgress", activeConquests > 0 ? averageProgress / activeConquests : 0);
        stats.put("activeConquestsList", activeList);
        
        // As defender
        int beingConquered = 0;
        List<Map<String, Object>> defendingList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : conquestProgress.entrySet()) {
            if (!entry.getKey().equals(nationId)) {
                Integer progress = entry.getValue().get(nationId);
                if (progress != null) {
                    beingConquered++;
                    defendingList.add(java.util.Map.of(
                        "attacker", entry.getKey(),
                        "progress", progress
                    ));
                }
            }
        }
        
        stats.put("beingConquered", beingConquered);
        stats.put("defendingList", defendingList);
        
        // Conquest rating
        String rating = "НЕТ ЗАВОЕВАНИЙ";
        if (completedConquests >= 10) rating = "ЗАВОЕВАТЕЛЬ";
        else if (completedConquests >= 5) rating = "ЭКСПАНСИОНИСТ";
        else if (completedConquests >= 3) rating = "АКТИВНЫЙ";
        else if (activeConquests >= 5) rating = "АГРЕССИВНЫЙ";
        else if (activeConquests >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global conquest statistics.
     */
    public synchronized Map<String, Object> getGlobalConquestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalActiveConquests = 0;
        int totalCompleted = 0;
        Map<String, Integer> conquestsByNation = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Integer>> entry : conquestProgress.entrySet()) {
            for (Integer progress : entry.getValue().values()) {
                totalActiveConquests++;
                if (progress >= 100) {
                    totalCompleted++;
                }
                conquestsByNation.put(entry.getKey(), conquestsByNation.getOrDefault(entry.getKey(), 0) + 1);
            }
        }
        
        stats.put("totalActiveConquests", totalActiveConquests);
        stats.put("totalCompleted", totalCompleted);
        stats.put("conquestsByNation", conquestsByNation);
        
        // Top conquerors
        List<Map.Entry<String, Integer>> topConquerors = conquestsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topConquerors", topConquerors);
        
        return stats;
    }
}

