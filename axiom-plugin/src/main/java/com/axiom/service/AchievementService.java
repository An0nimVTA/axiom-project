package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages player achievements and titles. */
public class AchievementService {
    private final AXIOM plugin;
    private final File achievementsDir;
    private final Map<UUID, Set<String>> playerAchievements = new HashMap<>(); // uuid -> achievement IDs

    public AchievementService(AXIOM plugin) {
        this.plugin = plugin;
        this.achievementsDir = new File(plugin.getDataFolder(), "achievements");
        this.achievementsDir.mkdirs();
        loadAll();
    }

    public synchronized void grantAchievement(UUID uuid, String achievementId) {
        boolean wasNew = !playerAchievements.computeIfAbsent(uuid, k -> new HashSet<>()).contains(achievementId);
        playerAchievements.get(uuid).add(achievementId);
        saveAchievements(uuid);
        
        // VISUAL EFFECTS: Celebrate achievement unlock with rarity effects
        if (wasNew) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    String achievementName = getAchievementDisplayName(achievementId);
                    String rarity = getAchievementRarity(achievementId);
                    VisualEffectsService effectsService = plugin.getVisualEffectsService();
                    
                    // Special effects for rare achievements
                    if (rarity.equals("LEGENDARY") || rarity.equals("MYTHIC")) {
                        if (effectsService != null) {
                            effectsService.playLegendaryAchievementEffect(player, achievementName, rarity);
                        }
                    } else {
                        // Standard achievement effect
                        player.sendTitle("§6§l[ДОСТИЖЕНИЕ]", "§e" + achievementName, 10, 80, 20);
                        if (effectsService != null) {
                            effectsService.sendActionBar(player, "§6🏆 Достижение разблокировано: §e" + achievementName);
                        }
                        
                        org.bukkit.Location loc = player.getLocation();
                        for (int i = 0; i < 25; i++) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                        }
                        loc.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, loc.add(0, 2, 0), 30, 1, 1, 1, 0.05);
                        player.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                }
            });
        }
    }
    
    private String getAchievementDisplayName(String achievementId) {
        switch (achievementId.toLowerCase()) {
            case "founder": return "Основатель империи";
            case "conqueror": return "Завоеватель";
            case "merchant": return "Великий торговец";
            case "diplomat": return "Дипломат";
            case "warrior": return "Воин";
            case "builder": return "Строитель";
            case "trader": return "Торговец";
            case "explorer": return "Исследователь";
            case "scholar": return "Учёный";
            case "leader": return "Лидер";
            case "tycoon": return "Магнат";
            case "peacemaker": return "Миротворец";
            case "genius": return "Гений";
            case "legend": return "Легенда";
            case "immortal": return "Бессмертный";
            case "god": return "Бог";
            default: return achievementId;
        }
    }

    public synchronized boolean hasAchievement(UUID uuid, String achievementId) {
        return playerAchievements.getOrDefault(uuid, Collections.emptySet()).contains(achievementId);
    }

    public synchronized Set<String> getAchievements(UUID uuid) {
        return new HashSet<>(playerAchievements.getOrDefault(uuid, Collections.emptySet()));
    }

    public synchronized String getTitle(UUID uuid) {
        Set<String> achievements = getAchievements(uuid);
        if (achievements.contains("founder")) return "Основатель империи";
        if (achievements.contains("conqueror")) return "Завоеватель";
        if (achievements.contains("merchant")) return "Великий торговец";
        if (achievements.contains("diplomat")) return "Дипломат";
        return "Гражданин";
    }
    
    /**
     * Get comprehensive achievement statistics.
     */
    public synchronized Map<String, Object> getAchievementStatistics(UUID uuid) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> achievements = getAchievements(uuid);
        stats.put("totalAchievements", achievements.size());
        stats.put("achievements", new ArrayList<>(achievements));
        stats.put("title", getTitle(uuid));
        
        // Achievement progress (percentages)
        Map<String, Integer> byCategory = new HashMap<>();
        for (String ach : achievements) {
            String category = getAchievementCategory(ach);
            byCategory.put(category, byCategory.getOrDefault(category, 0) + 1);
        }
        stats.put("byCategory", byCategory);
        
        // Achievement score
        int score = 0;
        for (String ach : achievements) {
            score += getAchievementPoints(ach);
        }
        stats.put("achievementScore", score);
        
        // Rating
        String rating = "НОВИЧОК";
        if (score >= 1000) rating = "ЛЕГЕНДАРНЫЙ";
        else if (score >= 500) rating = "ВЕЛИКИЙ";
        else if (score >= 250) rating = "ОПЫТНЫЙ";
        else if (score >= 100) rating = "ЗНАЮЩИЙ";
        else if (score >= 50) rating = "НАЧИНАЮЩИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get all available achievements (for reference).
     */
    public synchronized List<Map<String, Object>> getAllAvailableAchievements() {
        List<Map<String, Object>> all = new ArrayList<>();
        
        String[] achievementIds = {"founder", "conqueror", "merchant", "diplomat", "warrior", 
                                   "builder", "trader", "explorer", "scholar", "leader"};
        
        for (String id : achievementIds) {
            Map<String, Object> ach = new HashMap<>();
            ach.put("id", id);
            ach.put("name", getAchievementDisplayName(id));
            ach.put("category", getAchievementCategory(id));
            ach.put("points", getAchievementPoints(id));
            all.add(ach);
        }
        
        return all;
    }
    
    /**
     * Check if player has completed achievement category.
     */
    public synchronized boolean hasCategoryCompleted(UUID uuid, String category) {
        Set<String> achievements = getAchievements(uuid);
        // This would check if player has all achievements in a category
        // Simplified version - check if has at least one in category
        for (String ach : achievements) {
            if (getAchievementCategory(ach).equals(category)) {
                return true;
            }
        }
        return false;
    }
    
    private String getAchievementCategory(String achievementId) {
        switch (achievementId.toLowerCase()) {
            case "founder": case "builder": return "НАЦИЯ";
            case "conqueror": case "warrior": return "ВОЙНА";
            case "merchant": case "trader": return "ТОРГОВЛЯ";
            case "diplomat": return "ДИПЛОМАТИЯ";
            case "explorer": return "ИССЛЕДОВАНИЕ";
            case "scholar": return "ОБРАЗОВАНИЕ";
            case "leader": return "ЛИДЕРСТВО";
            default: return "ДРУГОЕ";
        }
    }
    
    private int getAchievementPoints(String achievementId) {
        switch (achievementId.toLowerCase()) {
            case "founder": return 200;
            case "conqueror": return 300;
            case "merchant": return 150;
            case "diplomat": return 150;
            case "warrior": return 100;
            case "builder": return 100;
            case "trader": return 75;
            case "explorer": return 75;
            case "scholar": return 100;
            case "leader": return 200;
            case "tycoon": return 500;
            case "peacemaker": return 400;
            case "genius": return 600;
            case "legend": return 800;
            case "immortal": return 1000;
            case "god": return 1500;
            default: return 50;
        }
    }
    
    /**
     * Get achievement rarity tier.
     */
    private String getAchievementRarity(String achievementId) {
        switch (achievementId.toLowerCase()) {
            case "founder": case "conqueror": return "EPIC";
            case "leader": return "EPIC";
            case "tycoon": case "peacemaker": return "LEGENDARY";
            case "genius": return "LEGENDARY";
            case "legend": case "immortal": return "MYTHIC";
            case "god": return "MYTHIC";
            default: return "COMMON";
        }
    }

    private void loadAll() {
        File[] files = achievementsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                UUID uuid = UUID.fromString(f.getName().replace(".json", ""));
                Set<String> achievements = new HashSet<>();
                if (o.has("achievements")) {
                    JsonArray arr = o.getAsJsonArray("achievements");
                    for (var e : arr) achievements.add(e.getAsString());
                }
                playerAchievements.put(uuid, achievements);
            } catch (Exception ignored) {}
        }
    }

    private void saveAchievements(UUID uuid) {
        File f = new File(achievementsDir, uuid.toString() + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("uuid", uuid.toString());
        JsonArray arr = new JsonArray();
        for (String a : playerAchievements.getOrDefault(uuid, Collections.emptySet())) arr.add(a);
        o.add("achievements", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get global achievement statistics.
     */
    public synchronized Map<String, Object> getGlobalAchievementStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPlayers = playerAchievements.size();
        int totalAchievements = 0;
        Map<String, Integer> byAchievement = new HashMap<>();
        Map<String, Integer> byCategory = new HashMap<>();
        int totalScore = 0;
        int maxScore = 0;
        
        for (Map.Entry<UUID, Set<String>> entry : playerAchievements.entrySet()) {
            Set<String> achievements = entry.getValue();
            totalAchievements += achievements.size();
            
            int playerScore = 0;
            for (String ach : achievements) {
                byAchievement.put(ach, byAchievement.getOrDefault(ach, 0) + 1);
                String category = getAchievementCategory(ach);
                byCategory.put(category, byCategory.getOrDefault(category, 0) + 1);
                playerScore += getAchievementPoints(ach);
            }
            
            totalScore += playerScore;
            maxScore = Math.max(maxScore, playerScore);
        }
        
        stats.put("totalPlayers", totalPlayers);
        stats.put("totalAchievementsUnlocked", totalAchievements);
        stats.put("averageAchievementsPerPlayer", totalPlayers > 0 ? (double) totalAchievements / totalPlayers : 0);
        stats.put("totalScore", totalScore);
        stats.put("maxScore", maxScore);
        stats.put("averageScore", totalPlayers > 0 ? (double) totalScore / totalPlayers : 0);
        stats.put("achievementsByType", byAchievement);
        stats.put("achievementsByCategory", byCategory);
        
        // Top players by achievement score
        List<Map.Entry<UUID, Integer>> topByScore = new ArrayList<>();
        for (Map.Entry<UUID, Set<String>> entry : playerAchievements.entrySet()) {
            int score = 0;
            for (String ach : entry.getValue()) {
                score += getAchievementPoints(ach);
            }
            if (score > 0) {
                topByScore.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), score));
            }
        }
        topByScore.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<Map<String, Object>> topPlayersData = new ArrayList<>();
        for (int i = 0; i < Math.min(10, topByScore.size()); i++) {
            Map.Entry<UUID, Integer> entry = topByScore.get(i);
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("playerId", entry.getKey().toString());
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());
            playerData.put("playerName", player.getName());
            playerData.put("score", entry.getValue());
            playerData.put("achievements", playerAchievements.get(entry.getKey()).size());
            topPlayersData.add(playerData);
        }
        stats.put("topPlayers", topPlayersData);
        
        // Most common achievements
        List<Map.Entry<String, Integer>> mostCommon = byAchievement.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonAchievements", mostCommon);
        
        return stats;
    }
}

