package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages expanded diplomatic immunity (beyond basic immunity). */
public class DiplomaticImmunityService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File immunityDir;
    private final Map<UUID, ExpandedImmunity> playerImmunities = new HashMap<>(); // playerId -> immunity

    public static class ExpandedImmunity {
        UUID playerId;
        String grantingNationId;
        Set<String> applicableNations = new HashSet<>(); // nations where immunity applies
        String level; // "standard", "extended", "full"
        long grantedAt;
        long expiresAt;
        boolean active;
    }

    public DiplomaticImmunityService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.immunityDir = new File(plugin.getDataFolder(), "expandedimmunity");
        this.immunityDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processImmunities, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String grantImmunity(String grantingNationId, UUID targetPlayerId, String level, int durationHours, Set<String> applicableNations) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (grantingNationId == null || targetPlayerId == null) return "Неверные параметры.";
        if (level == null || level.trim().isEmpty()) return "Уровень иммунитета не указан.";
        if (durationHours <= 0) return "Длительность должна быть больше 0.";
        Nation granting = nationManager.getNationById(grantingNationId);
        if (granting == null) return "Нация не найдена.";
        ExpandedImmunity immunity = new ExpandedImmunity();
        immunity.playerId = targetPlayerId;
        immunity.grantingNationId = grantingNationId;
        immunity.applicableNations = applicableNations != null ? new HashSet<>(applicableNations) : new HashSet<>();
        immunity.level = level;
        immunity.grantedAt = System.currentTimeMillis();
        immunity.expiresAt = System.currentTimeMillis() + durationHours * 60 * 60_000L;
        immunity.active = true;
        playerImmunities.put(targetPlayerId, immunity);
        granting.getHistory().add("Расширенный дипломатический иммунитет предоставлен");
        try {
            nationManager.save(granting);
            saveImmunity(immunity);
        } catch (Exception ignored) {}
        return "Дипломатический иммунитет предоставлен (уровень: " + level + ")";
    }

    public synchronized boolean hasExpandedImmunity(UUID playerId, String nationId) {
        if (playerId == null || nationId == null) return false;
        ExpandedImmunity immunity = playerImmunities.get(playerId);
        if (immunity == null || !immunity.active) return false;
        if (System.currentTimeMillis() > immunity.expiresAt) {
            immunity.active = false;
            saveImmunity(immunity);
            return false;
        }
        return immunity.applicableNations.contains(nationId) || "full".equalsIgnoreCase(immunity.level);
    }

    private synchronized void processImmunities() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, ExpandedImmunity>> it = playerImmunities.entrySet().iterator();
        while (it.hasNext()) {
            ExpandedImmunity immunity = it.next().getValue();
            if (!immunity.active || now > immunity.expiresAt) {
                immunity.active = false;
                saveImmunity(immunity);
                it.remove();
            }
        }
    }

    private void loadAll() {
        File[] files = immunityDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                UUID playerId = UUID.fromString(f.getName().replace(".json", ""));
                ExpandedImmunity immunity = new ExpandedImmunity();
                immunity.playerId = playerId;
                immunity.grantingNationId = o.get("grantingNationId").getAsString();
                immunity.level = o.get("level").getAsString();
                immunity.grantedAt = o.get("grantedAt").getAsLong();
                immunity.expiresAt = o.get("expiresAt").getAsLong();
                immunity.active = o.has("active") && o.get("active").getAsBoolean();
                if (o.has("applicableNations")) {
                    for (var elem : o.getAsJsonArray("applicableNations")) {
                        immunity.applicableNations.add(elem.getAsString());
                    }
                }
                if (immunity.active && System.currentTimeMillis() < immunity.expiresAt) {
                    playerImmunities.put(playerId, immunity);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveImmunity(ExpandedImmunity immunity) {
        File f = new File(immunityDir, immunity.playerId.toString() + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("playerId", immunity.playerId.toString());
        o.addProperty("grantingNationId", immunity.grantingNationId);
        o.addProperty("level", immunity.level);
        o.addProperty("grantedAt", immunity.grantedAt);
        o.addProperty("expiresAt", immunity.expiresAt);
        o.addProperty("active", immunity.active);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String nationId : immunity.applicableNations) {
            arr.add(nationId);
        }
        o.add("applicableNations", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive expanded immunity statistics.
     */
    public synchronized Map<String, Object> getExpandedImmunityStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        // Immunities granted by this nation
        List<ExpandedImmunity> granted = new ArrayList<>();
        // Immunities for this nation's territory
        List<ExpandedImmunity> applicable = new ArrayList<>();
        
        long now = System.currentTimeMillis();
        for (ExpandedImmunity immunity : playerImmunities.values()) {
            if (immunity.grantingNationId.equals(nationId) && immunity.active && now < immunity.expiresAt) {
                granted.add(immunity);
            }
            if ((immunity.applicableNations.contains(nationId) || "full".equalsIgnoreCase(immunity.level)) 
                && immunity.active && now < immunity.expiresAt) {
                applicable.add(immunity);
            }
        }
        
        stats.put("grantedImmunities", granted.size());
        stats.put("applicableImmunities", applicable.size());
        
        // Immunity details
        List<Map<String, Object>> immunitiesList = new ArrayList<>();
        for (ExpandedImmunity immunity : granted) {
            Map<String, Object> immunityData = new HashMap<>();
            immunityData.put("playerId", immunity.playerId.toString());
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(immunity.playerId);
            immunityData.put("playerName", player.getName());
            immunityData.put("level", immunity.level);
            immunityData.put("grantedAt", immunity.grantedAt);
            immunityData.put("expiresAt", immunity.expiresAt);
            long remaining = immunity.expiresAt - now;
            immunityData.put("remainingHours", remaining > 0 ? remaining / 1000 / 60 / 60 : 0);
            immunityData.put("applicableNations", immunity.applicableNations.size());
            immunitiesList.add(immunityData);
        }
        stats.put("immunitiesList", immunitiesList);
        
        // Immunity by level
        Map<String, Integer> byLevel = new HashMap<>();
        for (ExpandedImmunity immunity : granted) {
            byLevel.put(immunity.level, byLevel.getOrDefault(immunity.level, 0) + 1);
        }
        stats.put("byLevel", byLevel);
        
        // Immunity rating
        String rating = "НЕТ ИММУНИТЕТОВ";
        if (granted.size() >= 20) rating = "ОБШИРНЫЕ";
        else if (granted.size() >= 10) rating = "РАСШИРЕННЫЕ";
        else if (granted.size() >= 5) rating = "АКТИВНЫЕ";
        else if (granted.size() >= 1) rating = "НАЧАЛЬНЫЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Revoke expanded immunity.
     */
    public synchronized String revokeImmunity(UUID playerId, String nationId) throws IOException {
        ExpandedImmunity immunity = playerImmunities.get(playerId);
        if (immunity == null) return "Иммунитет не найден.";
        if (!immunity.grantingNationId.equals(nationId)) return "Вы не предоставляли этот иммунитет.";
        
        immunity.active = false;
        saveImmunity(immunity);
        playerImmunities.remove(playerId);
        
        return "Иммунитет отозван.";
    }
    
    /**
     * Extend immunity duration.
     */
    public synchronized String extendImmunity(UUID playerId, String nationId, int additionalHours) throws IOException {
        ExpandedImmunity immunity = playerImmunities.get(playerId);
        if (immunity == null) return "Иммунитет не найден.";
        if (!immunity.grantingNationId.equals(nationId)) return "Вы не предоставляли этот иммунитет.";
        if (additionalHours <= 0) return "Длительность должна быть больше 0.";
        
        immunity.expiresAt += additionalHours * 60 * 60_000L;
        saveImmunity(immunity);
        
        return "Иммунитет продлён на " + additionalHours + " часов.";
    }
    
    /**
     * Get all active immunities.
     */
    public synchronized List<ExpandedImmunity> getActiveImmunities() {
        long now = System.currentTimeMillis();
        return playerImmunities.values().stream()
            .filter(i -> i.active && now < i.expiresAt)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get immunity for player.
     */
    public synchronized ExpandedImmunity getPlayerImmunity(UUID playerId) {
        ExpandedImmunity immunity = playerImmunities.get(playerId);
        if (immunity != null && immunity.active) {
            if (System.currentTimeMillis() > immunity.expiresAt) {
                immunity.active = false;
                saveImmunity(immunity);
                playerImmunities.remove(playerId);
                return null;
            }
            return immunity;
        }
        return null;
    }
    
    /**
     * Get global expanded immunity statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalExpandedImmunityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        long now = System.currentTimeMillis();
        int totalActive = 0;
        int totalExpired = 0;
        Map<String, Integer> grantedByNation = new HashMap<>();
        Map<String, Integer> immunitiesByLevel = new HashMap<>();
        Map<String, Integer> immunitiesByNation = new HashMap<>(); // applicable nations
        
        for (ExpandedImmunity immunity : playerImmunities.values()) {
            if (immunity.active && now < immunity.expiresAt) {
                totalActive++;
                grantedByNation.put(immunity.grantingNationId, 
                    grantedByNation.getOrDefault(immunity.grantingNationId, 0) + 1);
                immunitiesByLevel.put(immunity.level, 
                    immunitiesByLevel.getOrDefault(immunity.level, 0) + 1);
                
                // Track applicable nations
                if (immunity.level.equals("full")) {
                    for (Nation n : nationManager.getAll()) {
                        immunitiesByNation.put(n.getId(), 
                            immunitiesByNation.getOrDefault(n.getId(), 0) + 1);
                    }
                } else {
                    for (String nationId : immunity.applicableNations) {
                        immunitiesByNation.put(nationId, 
                            immunitiesByNation.getOrDefault(nationId, 0) + 1);
                    }
                }
            } else {
                totalExpired++;
            }
        }
        
        stats.put("totalActive", totalActive);
        stats.put("totalExpired", totalExpired);
        stats.put("grantedByNation", grantedByNation);
        stats.put("immunitiesByLevel", immunitiesByLevel);
        stats.put("immunitiesByNation", immunitiesByNation);
        stats.put("nationsGranting", grantedByNation.size());
        stats.put("nationsWithImmunities", immunitiesByNation.size());
        
        // Average immunities per granting nation
        stats.put("averageImmunitiesPerNation", grantedByNation.size() > 0 ? 
            (double) totalActive / grantedByNation.size() : 0);
        
        // Top nations by granted immunities
        List<Map.Entry<String, Integer>> topByGranted = grantedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByGranted", topByGranted);
        
        // Level distribution
        stats.put("levelDistribution", immunitiesByLevel);
        
        // Average applicable nations per immunity
        double totalApplicable = 0;
        int immunityCount = 0;
        for (ExpandedImmunity immunity : playerImmunities.values()) {
            if (immunity.active && now < immunity.expiresAt) {
                if (immunity.level.equals("full")) {
                    totalApplicable += nationManager.getAll().size();
                } else {
                    totalApplicable += immunity.applicableNations.size();
                }
                immunityCount++;
            }
        }
        stats.put("averageApplicableNations", immunityCount > 0 ? totalApplicable / immunityCount : 0);
        
        // Average time remaining
        long totalRemaining = 0;
        int activeCount = 0;
        for (ExpandedImmunity immunity : playerImmunities.values()) {
            if (immunity.active && now < immunity.expiresAt) {
                totalRemaining += (immunity.expiresAt - now);
                activeCount++;
            }
        }
        stats.put("averageRemainingHours", activeCount > 0 ? 
            (totalRemaining / 1000.0 / 60.0 / 60.0) / activeCount : 0);
        
        return stats;
    }
}

