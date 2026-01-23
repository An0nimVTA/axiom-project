package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages religions, tithes (5%) and holy sites. */
public class ReligionManager {
    private final AXIOM plugin;
    private final File religionsDir;
    private final Map<String, JsonObject> idToReligion = new HashMap<>();

    public ReligionManager(AXIOM plugin) {
        this.plugin = plugin;
        this.religionsDir = new File(plugin.getDataFolder(), "religions");
        this.religionsDir.mkdirs();
        loadAll();
    }

    public synchronized String foundReligion(UUID founder, String id, String name) throws IOException {
        if (founder == null) return "Неверный основатель.";
        if (isBlank(id)) return "Неверный идентификатор религии.";
        if (isBlank(name)) return "Название религии не может быть пустым.";
        if (idToReligion.containsKey(id)) return "Религия уже существует.";
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("name", name);
        o.addProperty("founder", founder.toString());
        o.addProperty("tithesCollected", 0);
        o.add("holySites", new JsonArray());
        idToReligion.put(id, o);
        save(o);
        
        // VISUAL EFFECTS: Celebrate religion founding
        org.bukkit.entity.Player founderPlayer = org.bukkit.Bukkit.getPlayer(founder);
        if (founderPlayer != null && founderPlayer.isOnline()) {
            if (plugin.getVisualEffectsService() != null) {
                plugin.getVisualEffectsService().playNationJoinEffect(founderPlayer); // Reuse celebration effect
                plugin.getVisualEffectsService().sendActionBar(founderPlayer, "§e✨ Религия '" + name + "' основана!");
            }
        }
        
        return "Религия основана.";
    }

    public synchronized void recordTithe(UUID player, String religionId, double amount) {
        if (isBlank(religionId)) return;
        if (!Double.isFinite(amount) || amount <= 0) return;
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return;
        double t = o.get("tithesCollected").getAsDouble();
        o.addProperty("tithesCollected", t + amount);
        try { save(o); } catch (Exception ignored) {}
    }

    public synchronized void addHolySite(String religionId, String chunkKey) throws IOException {
        if (isBlank(religionId) || isBlank(chunkKey)) throw new IOException("Некорректные параметры.");
        JsonObject o = idToReligion.get(religionId);
        if (o == null) throw new IOException("Религия не найдена.");
        JsonArray a = o.getAsJsonArray("holySites");
        if (a == null) throw new IOException("Данные религии повреждены.");
        
        // Check if already exists
        for (var e : a) {
            if (e.getAsString().equals(chunkKey)) {
                throw new IOException("Святое место уже существует на этом чанке.");
            }
        }
        
        a.add(chunkKey);
        save(o);
        
        // VISUAL EFFECTS: Notify online players in this religion near the site
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String[] parts = chunkKey.split(":");
            if (parts.length == 3) {
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
                if (world != null) {
                    try {
                        int chunkX = Integer.parseInt(parts[1]);
                        int chunkZ = Integer.parseInt(parts[2]);
                        org.bukkit.Location centerLoc = new org.bukkit.Location(world, chunkX * 16 + 8, 64, chunkZ * 16 + 8);
                        
                        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                            if (plugin.getPlayerDataManager() == null) continue;
                            String playerReligion = plugin.getPlayerDataManager().getReligion(player.getUniqueId());
                            if (playerReligion != null && playerReligion.equals(religionId)) {
                                if (player.getLocation().distanceSquared(centerLoc) < 32 * 32) { // Within 32 blocks
                                    if (plugin.getVisualEffectsService() != null) {
                                        plugin.getVisualEffectsService().playHolySiteEffect(player, centerLoc);
                                    }
                                    player.sendMessage("§e✨ Святое место создано! Regeneration I активен.");
                                }
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        });
    }

    public synchronized boolean isHolySite(String religionId, String chunkKey) {
        if (isBlank(religionId) || isBlank(chunkKey)) return false;
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return false;
        JsonArray a = o.getAsJsonArray("holySites");
        if (a == null) return false;
        for (var e : a) {
            if (e != null && chunkKey.equals(e.getAsString())) return true;
        }
        return false;
    }
    
    /**
     * Get religion data by ID.
     */
    public synchronized ReligionData getReligion(String religionId) {
        if (isBlank(religionId)) return null;
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return null;
        ReligionData data = new ReligionData();
        data.id = o.has("id") ? o.get("id").getAsString() : religionId;
        data.name = o.has("name") ? o.get("name").getAsString() : religionId;
        return data;
    }
    
    /**
     * Check if location is near a holy site (within same chunk).
     */
    public synchronized boolean isNearHolySite(String religionId, org.bukkit.Location loc) {
        if (religionId == null || loc == null) return false;
        if (loc.getWorld() == null) return false;
        String chunkKey = loc.getWorld().getName() + ":" + loc.getChunk().getX() + ":" + loc.getChunk().getZ();
        return isHolySite(religionId, chunkKey);
    }
    
    /**
     * Get all religions.
     */
    public synchronized List<ReligionData> getAllReligions() {
        List<ReligionData> result = new ArrayList<>();
        for (JsonObject o : idToReligion.values()) {
            if (o == null) continue;
            ReligionData data = new ReligionData();
            data.id = o.has("id") ? o.get("id").getAsString() : null;
            data.name = o.has("name") ? o.get("name").getAsString() : data.id;
            result.add(data);
        }
        return result;
    }
    
    /**
     * Get follower count for a religion.
     */
    public synchronized int getFollowerCount(String religionId) {
        if (isBlank(religionId) || plugin.getPlayerDataManager() == null) return 0;
        int count = 0;
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            String playerReligion = plugin.getPlayerDataManager().getReligion(player.getUniqueId());
            if (religionId.equals(playerReligion)) {
                count++;
            }
        }
        // Note: This only counts online players. Full count would require scanning all player data files.
        return count;
    }
    
    /**
     * Simple data class for religion information.
     */
    public static class ReligionData {
        public String id;
        public String name;
    }

    private void loadAll() {
        File[] files = religionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                idToReligion.put(o.get("id").getAsString(), o);
            } catch (Exception e) { plugin.getLogger().warning("Failed religion load: " + e.getMessage()); }
        }
    }

    private void save(JsonObject o) throws IOException {
        if (o == null || !o.has("id")) return;
        File f = new File(religionsDir, o.get("id").getAsString() + ".json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive religion statistics.
     */
    public synchronized Map<String, Object> getReligionStatistics(String religionId) {
        if (isBlank(religionId)) return Collections.emptyMap();
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return Collections.emptyMap();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("id", o.has("id") ? o.get("id").getAsString() : religionId);
        stats.put("name", o.has("name") ? o.get("name").getAsString() : religionId);
        stats.put("tithesCollected", o.has("tithesCollected") ? o.get("tithesCollected").getAsDouble() : 0.0);
        stats.put("holySites", o.has("holySites") && o.getAsJsonArray("holySites") != null
            ? o.getAsJsonArray("holySites").size() : 0);
        stats.put("followers", getFollowerCount(religionId));
        stats.put("founder", o.has("founder") ? o.get("founder").getAsString() : "Unknown");
        
        return stats;
    }
    
    /**
     * Remove holy site.
     */
    public synchronized String removeHolySite(String religionId, String chunkKey) throws IOException {
        if (isBlank(religionId) || isBlank(chunkKey)) return "Неверные параметры.";
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return "Религия не найдена.";
        
        JsonArray sites = o.getAsJsonArray("holySites");
        if (sites == null) return "Святые места не найдены.";
        boolean removed = false;
        for (int i = 0; i < sites.size(); i++) {
            if (sites.get(i).getAsString().equals(chunkKey)) {
                sites.remove(i);
                removed = true;
                break;
            }
        }
        
        if (!removed) return "Святое место не найдено.";
        
        save(o);
        return "Святое место удалено.";
    }
    
    /**
     * Get all holy sites for a religion.
     */
    public synchronized List<String> getHolySites(String religionId) {
        if (isBlank(religionId)) return Collections.emptyList();
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return Collections.emptyList();
        
        List<String> sites = new ArrayList<>();
        if (o.has("holySites")) {
            JsonArray arr = o.getAsJsonArray("holySites");
            if (arr != null) {
                for (var e : arr) {
                    sites.add(e.getAsString());
                }
            }
        }
        return sites;
    }
    
    /**
     * Get total tithes collected.
     */
    public synchronized double getTotalTithes(String religionId) {
        if (isBlank(religionId)) return 0.0;
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return 0.0;
        return o.has("tithesCollected") ? o.get("tithesCollected").getAsDouble() : 0.0;
    }
    
    /**
     * Get top religions by followers.
     */
    public synchronized List<Map.Entry<String, Integer>> getTopReligionsByFollowers(int limit) {
        if (limit <= 0) return Collections.emptyList();
        Map<String, Integer> followerCounts = new HashMap<>();
        for (String religionId : idToReligion.keySet()) {
            followerCounts.put(religionId, getFollowerCount(religionId));
        }
        
        return followerCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get top religions by tithes collected.
     */
    public synchronized List<Map.Entry<String, Double>> getTopReligionsByTithes(int limit) {
        if (limit <= 0) return Collections.emptyList();
        Map<String, Double> titheCounts = new HashMap<>();
        for (Map.Entry<String, JsonObject> entry : idToReligion.entrySet()) {
            JsonObject value = entry.getValue();
            double tithes = value != null && value.has("tithesCollected") 
                ? value.get("tithesCollected").getAsDouble() 
                : 0.0;
            titheCounts.put(entry.getKey(), tithes);
        }
        
        return titheCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Rename religion (only by founder).
     */
    public synchronized String renameReligion(String religionId, String newName, UUID actor) throws IOException {
        if (isBlank(religionId)) return "Неверный идентификатор религии.";
        if (isBlank(newName)) return "Новое имя не может быть пустым.";
        if (actor == null) return "Неверный игрок.";
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return "Религия не найдена.";
        
        String founder = o.has("founder") ? o.get("founder").getAsString() : null;
        if (founder == null || !founder.equals(actor.toString())) {
            return "Только основатель может переименовать религию.";
        }
        
        String oldName = o.has("name") ? o.get("name").getAsString() : religionId;
        o.addProperty("name", newName);
        save(o);
        
        // VISUAL EFFECTS
        org.bukkit.entity.Player founderPlayer = org.bukkit.Bukkit.getPlayer(actor);
        if (founderPlayer != null && founderPlayer.isOnline()) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                founderPlayer.sendMessage("§e✨ Религия '" + oldName + "' переименована в '" + newName + "'");
            });
        }
        
        return "Религия переименована.";
    }
    
    /**
     * Check if player is founder of religion.
     */
    public synchronized boolean isFounder(UUID playerId, String religionId) {
        if (playerId == null || isBlank(religionId)) return false;
        JsonObject o = idToReligion.get(religionId);
        if (o == null) return false;
        String founder = o.has("founder") ? o.get("founder").getAsString() : null;
        return founder != null && founder.equals(playerId.toString());
    }
    
    /**
     * Get global religion statistics.
     */
    public synchronized Map<String, Object> getGlobalReligionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalReligions", idToReligion.size());
        
        // Calculate total tithes
        double totalTithes = 0.0;
        int totalHolySites = 0;
        int totalFollowers = 0;
        
        for (Map.Entry<String, JsonObject> entry : idToReligion.entrySet()) {
            JsonObject o = entry.getValue();
            if (o == null) continue;
            totalTithes += o.has("tithesCollected") ? o.get("tithesCollected").getAsDouble() : 0.0;
            totalHolySites += o.has("holySites") && o.getAsJsonArray("holySites") != null
                ? o.getAsJsonArray("holySites").size() : 0;
            totalFollowers += getFollowerCount(entry.getKey());
        }
        
        stats.put("totalTithesCollected", totalTithes);
        stats.put("totalHolySites", totalHolySites);
        stats.put("totalFollowers", totalFollowers);
        stats.put("averageTithesPerReligion", idToReligion.size() > 0 ? totalTithes / idToReligion.size() : 0);
        stats.put("averageHolySitesPerReligion", idToReligion.size() > 0 ? (double) totalHolySites / idToReligion.size() : 0);
        
        // Top religions
        List<Map.Entry<String, Integer>> topByFollowers = getTopReligionsByFollowers(10);
        List<Map.Entry<String, Double>> topByTithes = getTopReligionsByTithes(10);
        
        stats.put("topReligionsByFollowers", topByFollowers);
        stats.put("topReligionsByTithes", topByTithes);
        
        return stats;
    }
    
    /**
     * Get religion leaderboard.
     */
    public synchronized Map<String, List<Map.Entry<String, Double>>> getReligionLeaderboards() {
        Map<String, List<Map.Entry<String, Double>>> leaderboards = new HashMap<>();
        
        // By followers (convert to Double for consistency)
        List<Map.Entry<String, Double>> followersLb = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : getTopReligionsByFollowers(10)) {
            followersLb.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), (double) entry.getValue()));
        }
        leaderboards.put("followers", followersLb);
        
        leaderboards.put("tithes", getTopReligionsByTithes(10));
        
        return leaderboards;
    }
    
    /**
     * Get all religions with their statistics.
     */
    public synchronized List<Map<String, Object>> getAllReligionsWithStatistics() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (String religionId : idToReligion.keySet()) {
            result.add(getReligionStatistics(religionId));
        }
        
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}


