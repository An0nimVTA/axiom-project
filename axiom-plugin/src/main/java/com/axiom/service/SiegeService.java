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
import java.util.Objects;
import java.util.Set;

/** Manages siege warfare against cities. */
public class SiegeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File siegesDir;
    private final Map<String, Siege> activeSieges = new HashMap<>(); // cityId -> siege

    public static class Siege {
        String cityId;
        String attackerNationId;
        String defenderNationId;
        double progress; // 0-100%
        long startTime;
    }

    public SiegeService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.siegesDir = new File(plugin.getDataFolder(), "sieges");
        this.siegesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateSieges, 0, 20 * 60 * 5); // every 5 minutes
    }

    public synchronized String startSiege(String cityId, String attackerId, String defenderId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(cityId) || isBlank(attackerId) || isBlank(defenderId)) return "Неверные параметры.";
        if (attackerId.equals(defenderId)) return "Нельзя осаждать свою нацию.";
        if (activeSieges.containsKey(cityId)) return "Осада уже идёт.";
        if (plugin.getDiplomacySystem() == null) return "Дипломатическая система недоступна.";
        if (!plugin.getDiplomacySystem().isAtWar(attackerId, defenderId)) return "Не в состоянии войны.";
        Siege s = new Siege();
        s.cityId = cityId;
        s.attackerNationId = attackerId;
        s.defenderNationId = defenderId;
        s.progress = 0.0;
        s.startTime = System.currentTimeMillis();
        activeSieges.put(cityId, s);
        saveSiege(s);
        Nation attacker = nationManager.getNationById(attackerId);
        Nation defender = nationManager.getNationById(defenderId);
        if (defender != null) {
            if (defender.getHistory() != null) {
                defender.getHistory().add("Осада города " + cityId);
            }
            try { nationManager.save(defender); } catch (Exception ignored) {}
        }
        
        // VISUAL EFFECTS: Announce siege to both nations
        if (attacker != null && defender != null) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String msg1 = "§c⚔ Осада города начата! Прогресс: 0%";
                Set<UUID> attackerCitizens = attacker.getCitizens();
                if (attackerCitizens != null) {
                    for (UUID citizenId : attackerCitizens) {
                        org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                        if (citizen != null && citizen.isOnline()) {
                            citizen.sendTitle("§c§l[ОСАДА]", "§fГород '" + cityId + "' осаждён", 10, 80, 20);
                            if (plugin.getVisualEffectsService() != null) {
                                plugin.getVisualEffectsService().sendActionBar(citizen, msg1);
                            }
                            org.bukkit.Location loc = citizen.getLocation();
                            if (loc != null && loc.getWorld() != null) {
                                loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0,
                                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 2.0f));
                                citizen.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.9f);
                            }
                        }
                    }
                }
                String msg2 = "§c⚠ Город '" + cityId + "' осаждён!";
                Set<UUID> defenderCitizens = defender.getCitizens();
                if (defenderCitizens != null) {
                    for (UUID citizenId : defenderCitizens) {
                        org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                        if (citizen != null && citizen.isOnline()) {
                            citizen.sendTitle("§c§l[ОСАДА!]", "§fГород '" + cityId + "' под атакой!", 10, 100, 20);
                            if (plugin.getVisualEffectsService() != null) {
                                plugin.getVisualEffectsService().sendActionBar(citizen, msg2);
                            }
                            org.bukkit.Location loc = citizen.getLocation();
                            if (loc != null && loc.getWorld() != null) {
                                loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_LARGE, loc.add(0, 1, 0), 20, 1, 1, 1, 0.1);
                                citizen.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.7f);
                            }
                        }
                    }
                }
            });
        }
        
        return "Осада начата.";
    }

    private synchronized void updateSieges() {
        List<Siege> completed = new ArrayList<>();
        for (Siege s : activeSieges.values()) {
            if (s == null) continue;
            s.progress = Math.min(100, s.progress + 2.0); // 2% per 5 minutes
            if (s.progress >= 100) {
                completed.add(s);
            }
        }
        for (Siege siege : completed) {
            completeSiege(siege);
        }
    }

    private void completeSiege(Siege s) {
        if (s == null) return;
        // City falls to attacker
        com.axiom.model.City city = null;
        if (plugin.getCityGrowthEngine() != null && !isBlank(s.defenderNationId)) {
            List<com.axiom.model.City> cities = plugin.getCityGrowthEngine().getCitiesOf(s.defenderNationId);
            if (cities != null) {
                city = cities.stream()
                    .filter(c -> c != null && s.cityId.equals(c.getId()))
                    .findFirst()
                    .orElse(null);
            }
        }
        if (city != null) {
            // Transfer city
            city.setNationId(s.attackerNationId);
        }
        activeSieges.remove(s.cityId);
        Nation attacker = nationManager != null ? nationManager.getNationById(s.attackerNationId) : null;
        Nation defender = nationManager != null ? nationManager.getNationById(s.defenderNationId) : null;
        if (attacker != null && attacker.getHistory() != null) {
            attacker.getHistory().add("Город " + s.cityId + " захвачен!");
        }
        if (defender != null && defender.getHistory() != null) {
            defender.getHistory().add("Город " + s.cityId + " потерян!");
        }
        try {
            if (attacker != null) nationManager.save(attacker);
            if (defender != null) nationManager.save(defender);
        } catch (Exception ignored) {}
        
        // VISUAL EFFECTS: Celebrate/announce city capture
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (attacker != null) {
                String msg1 = "§a✓ Город '" + s.cityId + "' захвачен!";
                Set<UUID> attackerCitizens = attacker.getCitizens();
                if (attackerCitizens != null) {
                    for (UUID citizenId : attackerCitizens) {
                        org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                        if (citizen != null && citizen.isOnline()) {
                            citizen.sendTitle("§a§l[ПОБЕДА]", "§fГород '" + s.cityId + "' захвачен!", 10, 100, 20);
                            if (plugin.getVisualEffectsService() != null) {
                                plugin.getVisualEffectsService().sendActionBar(citizen, msg1);
                            }
                            org.bukkit.Location loc = citizen.getLocation();
                            if (loc != null && loc.getWorld() != null) {
                                for (int i = 0; i < 25; i++) {
                                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                                }
                                loc.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, loc.add(0, 2, 0), 30, 1, 1, 1, 0.05);
                                citizen.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                            }
                        }
                    }
                }
            }
            if (defender != null) {
                String msg2 = "§c⚠ Город '" + s.cityId + "' потерян!";
                Set<UUID> defenderCitizens = defender.getCitizens();
                if (defenderCitizens != null) {
                    for (UUID citizenId : defenderCitizens) {
                        org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                        if (citizen != null && citizen.isOnline()) {
                            citizen.sendTitle("§c§l[ПОРАЖЕНИЕ]", "§fГород '" + s.cityId + "' потерян", 10, 100, 20);
                            if (plugin.getVisualEffectsService() != null) {
                                plugin.getVisualEffectsService().sendActionBar(citizen, msg2);
                            }
                            org.bukkit.Location loc = citizen.getLocation();
                            if (loc != null && loc.getWorld() != null) {
                                loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_LARGE, loc.add(0, 1, 0), 30, 1, 1, 1, 0.15);
                                citizen.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_DEATH, 0.5f, 0.8f);
                            }
                        }
                    }
                }
            }
        });
    }

    private void loadAll() {
        File[] files = siegesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Siege s = new Siege();
                s.cityId = o.has("cityId") ? o.get("cityId").getAsString() : null;
                s.attackerNationId = o.has("attackerNationId") ? o.get("attackerNationId").getAsString() : null;
                s.defenderNationId = o.has("defenderNationId") ? o.get("defenderNationId").getAsString() : null;
                s.progress = o.has("progress") ? o.get("progress").getAsDouble() : 0.0;
                s.startTime = o.has("startTime") ? o.get("startTime").getAsLong() : System.currentTimeMillis();
                if (!isBlank(s.cityId) && s.progress < 100) activeSieges.put(s.cityId, s);
            } catch (Exception ignored) {}
        }
    }

    private void saveSiege(Siege s) {
        if (s == null || isBlank(s.cityId)) return;
        File f = new File(siegesDir, s.cityId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("cityId", s.cityId);
        o.addProperty("attackerNationId", s.attackerNationId);
        o.addProperty("defenderNationId", s.defenderNationId);
        o.addProperty("progress", s.progress);
        o.addProperty("startTime", s.startTime);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive siege statistics.
     */
    public synchronized Map<String, Object> getSiegeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        // As attacker
        int activeSiegesAsAttacker = 0;
        List<Map<String, Object>> attackingList = new ArrayList<>();
        
        for (Siege s : activeSieges.values()) {
            if (s != null && Objects.equals(s.attackerNationId, nationId)) {
                activeSiegesAsAttacker++;
                attackingList.add(java.util.Map.of(
                    "cityId", s.cityId,
                    "defender", s.defenderNationId,
                    "progress", s.progress,
                    "startTime", s.startTime,
                    "durationHours", (System.currentTimeMillis() - s.startTime) / 1000 / 60 / 60
                ));
            }
        }
        
        stats.put("activeSiegesAsAttacker", activeSiegesAsAttacker);
        stats.put("attackingList", attackingList);
        
        // As defender
        int activeSiegesAsDefender = 0;
        List<Map<String, Object>> defendingList = new ArrayList<>();
        
        for (Siege s : activeSieges.values()) {
            if (s != null && Objects.equals(s.defenderNationId, nationId)) {
                activeSiegesAsDefender++;
                defendingList.add(java.util.Map.of(
                    "cityId", s.cityId,
                    "attacker", s.attackerNationId,
                    "progress", s.progress,
                    "startTime", s.startTime,
                    "durationHours", (System.currentTimeMillis() - s.startTime) / 1000 / 60 / 60,
                    "timeRemaining", (100 - s.progress) / 2.0 * 5 // minutes remaining at 2% per 5 min
                ));
            }
        }
        
        stats.put("activeSiegesAsDefender", activeSiegesAsDefender);
        stats.put("defendingList", defendingList);
        
        // Siege rating
        String rating = "НЕТ ОСАД";
        if (activeSiegesAsAttacker >= 5) rating = "ОСАЖДАЮЩИЙ";
        else if (activeSiegesAsAttacker >= 3) rating = "АКТИВНЫЙ";
        else if (activeSiegesAsAttacker >= 1) rating = "НАЧАЛЬНЫЙ";
        else if (activeSiegesAsDefender >= 3) rating = "ОСАЖДЁННЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global siege statistics.
     */
    public synchronized Map<String, Object> getGlobalSiegeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeSieges", activeSieges.size());
        
        Map<String, Integer> siegesByAttacker = new HashMap<>();
        Map<String, Integer> siegesByDefender = new HashMap<>();
        double averageProgress = 0.0;
        
        for (Siege s : activeSieges.values()) {
            if (s == null) continue;
            if (s.attackerNationId != null) {
                siegesByAttacker.put(s.attackerNationId, siegesByAttacker.getOrDefault(s.attackerNationId, 0) + 1);
            }
            if (s.defenderNationId != null) {
                siegesByDefender.put(s.defenderNationId, siegesByDefender.getOrDefault(s.defenderNationId, 0) + 1);
            }
            averageProgress += s.progress;
        }
        
        stats.put("siegesByAttacker", siegesByAttacker);
        stats.put("siegesByDefender", siegesByDefender);
        stats.put("averageProgress", activeSieges.size() > 0 ? averageProgress / activeSieges.size() : 0);
        
        // Top attackers
        List<Map.Entry<String, Integer>> topAttackers = siegesByAttacker.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topAttackers", topAttackers);
        
        // Most besieged
        List<Map.Entry<String, Integer>> mostBesieged = siegesByDefender.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostBesieged", mostBesieged);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

