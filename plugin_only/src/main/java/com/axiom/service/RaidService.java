package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Manages raiding mechanics during war. */
public class RaidService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File raidsDir;
    private final Map<String, Long> lastRaidTime = new HashMap<>(); // raiderId_targetId -> timestamp

    public RaidService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.raidsDir = new File(plugin.getDataFolder(), "raids");
        this.raidsDir.mkdirs();
        loadAll();
    }

    public synchronized String performRaid(String raiderId, String targetId, double stolenAmount) {
        Nation raider = nationManager.getNationById(raiderId);
        Nation target = nationManager.getNationById(targetId);
        if (raider == null || target == null) return "Нация не найдена.";
        if (!plugin.getDiplomacySystem().isAtWar(raiderId, targetId)) return "Не в состоянии войны.";
        String key = raiderId + "_" + targetId;
        Long lastRaid = lastRaidTime.get(key);
        if (lastRaid != null && System.currentTimeMillis() - lastRaid < 60 * 60_000L) {
            return "Рейд на перезарядке (1 час).";
        }
        if (target.getTreasury() < stolenAmount) stolenAmount = target.getTreasury();
        final double finalStolenAmount = stolenAmount;
        target.setTreasury(target.getTreasury() - finalStolenAmount);
        raider.setTreasury(raider.getTreasury() + finalStolenAmount * 0.8); // 80% of stolen, 20% lost
        lastRaidTime.put(key, System.currentTimeMillis());
        target.getHistory().add("Рейд! Украдено " + finalStolenAmount + " из казны " + raider.getName());
        raider.getHistory().add("Рейд на " + target.getName() + ": получено " + (finalStolenAmount * 0.8));
        try {
            nationManager.save(raider);
            nationManager.save(target);
            saveRaid(key);
        } catch (Exception ignored) {}
        
        // VISUAL EFFECTS: Announce raid to both nations
        final double received = finalStolenAmount * 0.8;
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String msg1 = "§6⚔ Рейд успешен! Получено: §f" + String.format("%.0f", received);
            for (UUID citizenId : raider.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§6§l[РЕЙД]", "§fПолучено " + String.format("%.0f", received), 10, 60, 10);
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg1);
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                    citizen.playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                }
            }
            String msg2 = "§c⚠ Рейд! Украдено: §f" + String.format("%.0f", finalStolenAmount);
            for (UUID citizenId : target.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§c§l[РЕЙД!]", "§fУкрадено " + String.format("%.0f", finalStolenAmount), 10, 80, 20);
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg2);
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                    citizen.playSound(loc, org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.7f, 0.8f);
                }
            }
        });
        
        return "Рейд выполнен. Получено: " + (stolenAmount * 0.8);
    }

    private void loadAll() {
        File f = new File(raidsDir, "timestamps.json");
        if (!f.exists()) return;
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            for (var entry : o.entrySet()) {
                lastRaidTime.put(entry.getKey(), entry.getValue().getAsLong());
            }
        } catch (Exception ignored) {}
    }

    private void saveRaid(String key) {
        File f = new File(raidsDir, "timestamps.json");
        JsonObject o = new JsonObject();
        for (var entry : lastRaidTime.entrySet()) {
            o.addProperty(entry.getKey(), entry.getValue());
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive raid statistics.
     */
    public synchronized Map<String, Object> getRaidStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        int raidsPerformed = 0;
        int raidsReceived = 0;
        List<Map<String, Object>> recentRaids = new ArrayList<>();
        
        long now = System.currentTimeMillis();
        long oneDayAgo = now - (24L * 60 * 60 * 1000);
        
        for (Map.Entry<String, Long> entry : lastRaidTime.entrySet()) {
            String[] parts = entry.getKey().split("_");
            if (parts.length >= 2) {
                String raiderId = parts[0];
                String targetId = parts[1];
                
                if (raiderId.equals(nationId)) {
                    raidsPerformed++;
                    if (entry.getValue() >= oneDayAgo) {
                        recentRaids.add(java.util.Map.of(
                            "target", targetId,
                            "timestamp", entry.getValue(),
                            "cooldownRemaining", Math.max(0, (entry.getValue() + 60 * 60_000L - now) / 1000 / 60)
                        ));
                    }
                }
                if (targetId.equals(nationId)) {
                    raidsReceived++;
                }
            }
        }
        
        stats.put("raidsPerformed", raidsPerformed);
        stats.put("raidsReceived", raidsReceived);
        stats.put("totalRaids", raidsPerformed + raidsReceived);
        stats.put("recentRaids", recentRaids);
        
        // Check cooldowns
        Map<String, Long> cooldowns = new HashMap<>();
        for (Map.Entry<String, Long> entry : lastRaidTime.entrySet()) {
            if (entry.getKey().startsWith(nationId + "_")) {
                String targetId = entry.getKey().substring(nationId.length() + 1);
                long cooldownRemaining = Math.max(0, (entry.getValue() + 60 * 60_000L - now) / 1000 / 60);
                if (cooldownRemaining > 0) {
                    cooldowns.put(targetId, cooldownRemaining);
                }
            }
        }
        stats.put("activeCooldowns", cooldowns);
        
        // Raid rating
        String rating = "НЕТ РЕЙДОВ";
        if (raidsPerformed >= 20) rating = "АГРЕССОР";
        else if (raidsPerformed >= 10) rating = "АКТИВНЫЙ";
        else if (raidsPerformed >= 5) rating = "РЕГУЛЯРНЫЙ";
        else if (raidsPerformed >= 1) rating = "НАЧИНАЮЩИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global raid statistics.
     */
    public synchronized Map<String, Object> getGlobalRaidStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalRaids = lastRaidTime.size();
        Map<String, Integer> raidsByNation = new HashMap<>();
        
        for (String key : lastRaidTime.keySet()) {
            String[] parts = key.split("_");
            if (parts.length >= 2) {
                String raiderId = parts[0];
                raidsByNation.put(raiderId, raidsByNation.getOrDefault(raiderId, 0) + 1);
            }
        }
        
        stats.put("totalRaids", totalRaids);
        stats.put("raidsByNation", raidsByNation);
        
        // Top raiders
        List<Map.Entry<String, Integer>> topRaiders = raidsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topRaiders", topRaiders);
        
        return stats;
    }
}

