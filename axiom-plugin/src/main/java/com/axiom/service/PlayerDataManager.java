package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.UUID;

/**
 * Manages per-player data under plugins/AXIOM/players/{uuid}.json
 */
public class PlayerDataManager {
    private final AXIOM plugin;
    private final File playersDir;

    public PlayerDataManager(AXIOM plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        this.playersDir.mkdirs();
    }

    public synchronized void setNation(UUID uuid, String nationId, String role) {
        JsonObject o = load(uuid);
        o.addProperty("uuid", uuid.toString());
        o.addProperty("nation", nationId);
        if (role != null) o.addProperty("role", role);
        save(uuid, o);
    }

    public synchronized void clearNation(UUID uuid) {
        JsonObject o = load(uuid);
        o.remove("nation");
        o.remove("role");
        save(uuid, o);
    }

    public synchronized String getNation(UUID uuid) {
        JsonObject o = load(uuid);
        return o.has("nation") ? o.get("nation").getAsString() : null;
    }

    public synchronized String getRole(UUID uuid) {
        JsonObject o = load(uuid);
        return o.has("role") ? o.get("role").getAsString() : null;
    }

    public synchronized String getReligion(UUID uuid) {
        return getField(uuid, "religion");
    }

    public synchronized String getField(UUID uuid, String key) {
        JsonObject o = load(uuid);
        return o.has(key) ? o.get(key).getAsString() : null;
    }

    public synchronized void setField(UUID uuid, String key, String value) {
        JsonObject o = load(uuid);
        String oldValue = o.has(key) ? o.get(key).getAsString() : null;
        o.addProperty(key, value);
        save(uuid, o);
        
        // VISUAL EFFECTS: Special handling for religion join
        if (key.equals("religion") && value != null && !value.equals(oldValue)) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    ReligionManager religionManager = plugin.getReligionManager();
                    VisualEffectsService effectsService = plugin.getVisualEffectsService();
                    var religionData = religionManager != null ? religionManager.getReligion(value) : null;
                    String religionName = religionData != null ? religionData.name : value;
                    player.sendTitle("§e§l[РЕЛИГИЯ]", "§fВы присоединились к '" + religionName + "'", 10, 80, 20);
                    if (effectsService != null) {
                        effectsService.sendActionBar(player, "§e✨ Религия: '" + religionName + "'. Десятина: 5%, Бонусы активны");
                    }
                    // Gold/white particles
                    org.bukkit.Location loc = player.getLocation();
                    for (int i = 0; i < 15; i++) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.05);
                    }
                    player.playSound(loc, org.bukkit.Sound.BLOCK_BELL_USE, 0.8f, 1.2f);
                }
            });
        }
    }

    private JsonObject load(UUID uuid) {
        File f = new File(playersDir, uuid.toString() + ".json");
        if (!f.exists()) return new JsonObject();
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read player file: " + e.getMessage());
            return new JsonObject();
        }
    }

    private void save(UUID uuid, JsonObject o) {
        File f = new File(playersDir, uuid.toString() + ".json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to write player file: " + e.getMessage());
        }
    }
    
    /**
     * Get comprehensive player statistics.
     */
    public synchronized Map<String, Object> getPlayerStatistics(UUID uuid) {
        Map<String, Object> stats = new HashMap<>();
        JsonObject data = load(uuid);
        
        stats.put("uuid", uuid.toString());
        stats.put("nation", data.has("nation") ? data.get("nation").getAsString() : null);
        stats.put("role", data.has("role") ? data.get("role").getAsString() : null);
        stats.put("religion", data.has("religion") ? data.get("religion").getAsString() : null);
        
        // Extended fields
        if (data.has("balance")) stats.put("balance", data.get("balance").getAsDouble());
        if (data.has("totalIncome")) stats.put("totalIncome", data.get("totalIncome").getAsDouble());
        if (data.has("totalTaxes")) stats.put("totalTaxes", data.get("totalTaxes").getAsDouble());
        if (data.has("playTime")) stats.put("playTime", data.get("playTime").getAsLong());
        if (data.has("achievements")) {
            stats.put("achievements", data.get("achievements").getAsJsonArray().size());
        }
        
        // Nation-based stats
        String nationId = getNation(uuid);
        if (nationId != null) {
            stats.put("inNation", true);
            if (plugin.getNationManager() != null) {
                Nation nation = plugin.getNationManager().getNationById(nationId);
                if (nation != null) {
                    stats.put("nationName", nation.getName());
                    stats.put("nationTreasury", nation.getTreasury());
                }
            }
        } else {
            stats.put("inNation", false);
        }
        
        return stats;
    }
    
    /**
     * Set numeric field.
     */
    public synchronized void setNumericField(UUID uuid, String key, double value) {
        JsonObject o = load(uuid);
        o.addProperty(key, value);
        save(uuid, o);
    }
    
    /**
     * Get numeric field.
     */
    public synchronized double getNumericField(UUID uuid, String key) {
        JsonObject o = load(uuid);
        return o.has(key) ? o.get(key).getAsDouble() : 0.0;
    }
    
    /**
     * Increment numeric field.
     */
    public synchronized double incrementNumericField(UUID uuid, String key, double amount) {
        double current = getNumericField(uuid, key);
        double newValue = current + amount;
        setNumericField(uuid, key, newValue);
        return newValue;
    }
    
    /**
     * Record income for player.
     */
    public synchronized void recordIncome(UUID uuid, double amount) {
        incrementNumericField(uuid, "totalIncome", amount);
        // Track daily income
        incrementNumericField(uuid, "dailyIncome", amount);
    }
    
    /**
     * Record taxes paid.
     */
    public synchronized void recordTaxes(UUID uuid, double amount) {
        incrementNumericField(uuid, "totalTaxes", amount);
    }
    
    /**
     * Get all player data as map.
     */
    public synchronized Map<String, Object> getAllPlayerData(UUID uuid) {
        JsonObject data = load(uuid);
        Map<String, Object> result = new HashMap<>();
        
        for (String key : data.keySet()) {
            var element = data.get(key);
            if (element.isJsonPrimitive()) {
                var prim = element.getAsJsonPrimitive();
                if (prim.isNumber()) {
                    result.put(key, prim.getAsDouble());
                } else if (prim.isBoolean()) {
                    result.put(key, prim.getAsBoolean());
                } else {
                    result.put(key, prim.getAsString());
                }
            } else if (element.isJsonArray()) {
                result.put(key, element.getAsJsonArray().size());
            }
        }
        
        return result;
    }
    
    /**
     * Clear player data (for leaving nation/religion).
     */
    /**
     * Get global player data statistics across all players.
     */
    public synchronized Map<String, Object> getGlobalPlayerDataStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        File[] files = playersDir.listFiles((d, n) -> n.endsWith(".json"));
        int totalPlayers = files != null ? files.length : 0;
        stats.put("totalPlayers", totalPlayers);
        
        if (totalPlayers == 0) {
            return stats;
        }
        
        // Player statistics
        int playersInNations = 0;
        int playersWithoutNations = 0;
        Map<String, Integer> roleDistribution = new HashMap<>();
        Map<String, Integer> religionDistribution = new HashMap<>();
        Map<String, Integer> nationDistribution = new HashMap<>();
        double totalBalance = 0.0;
        double totalIncome = 0.0;
        double totalTaxes = 0.0;
        long totalPlayTime = 0L;
        int playersWithBalance = 0;
        int playersWithIncome = 0;
        int playersWithTaxes = 0;
        int playersWithPlayTime = 0;
        
        for (File file : files) {
            try {
                String uuidStr = file.getName().replace(".json", "");
                UUID uuid = UUID.fromString(uuidStr);
                JsonObject data = load(uuid);
                
                // Nation stats
                if (data.has("nation")) {
                    playersInNations++;
                    String nationId = data.get("nation").getAsString();
                    nationDistribution.put(nationId, nationDistribution.getOrDefault(nationId, 0) + 1);
                    
                    // Role stats
                    if (data.has("role")) {
                        String role = data.get("role").getAsString();
                        roleDistribution.put(role, roleDistribution.getOrDefault(role, 0) + 1);
                    }
                } else {
                    playersWithoutNations++;
                }
                
                // Religion stats
                if (data.has("religion")) {
                    String religion = data.get("religion").getAsString();
                    religionDistribution.put(religion, religionDistribution.getOrDefault(religion, 0) + 1);
                }
                
                // Economic stats
                if (data.has("balance")) {
                    double balance = data.get("balance").getAsDouble();
                    totalBalance += balance;
                    playersWithBalance++;
                }
                if (data.has("totalIncome")) {
                    double income = data.get("totalIncome").getAsDouble();
                    totalIncome += income;
                    playersWithIncome++;
                }
                if (data.has("totalTaxes")) {
                    double taxes = data.get("totalTaxes").getAsDouble();
                    totalTaxes += taxes;
                    playersWithTaxes++;
                }
                if (data.has("playTime")) {
                    long playTime = data.get("playTime").getAsLong();
                    totalPlayTime += playTime;
                    playersWithPlayTime++;
                }
            } catch (Exception e) {
                // Skip invalid files
            }
        }
        
        stats.put("playersInNations", playersInNations);
        stats.put("playersWithoutNations", playersWithoutNations);
        stats.put("roleDistribution", roleDistribution);
        stats.put("religionDistribution", religionDistribution);
        stats.put("nationDistribution", nationDistribution);
        stats.put("totalBalance", totalBalance);
        stats.put("averageBalance", playersWithBalance > 0 ? totalBalance / playersWithBalance : 0);
        stats.put("totalIncome", totalIncome);
        stats.put("averageIncome", playersWithIncome > 0 ? totalIncome / playersWithIncome : 0);
        stats.put("totalTaxes", totalTaxes);
        stats.put("averageTaxes", playersWithTaxes > 0 ? totalTaxes / playersWithTaxes : 0);
        stats.put("totalPlayTime", totalPlayTime);
        stats.put("averagePlayTime", playersWithPlayTime > 0 ? totalPlayTime / playersWithPlayTime : 0);
        stats.put("playersWithBalance", playersWithBalance);
        stats.put("playersWithIncome", playersWithIncome);
        stats.put("playersWithTaxes", playersWithTaxes);
        stats.put("playersWithPlayTime", playersWithPlayTime);
        
        // Top nations by player count
        List<Map.Entry<String, Integer>> topByPlayers = nationDistribution.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topNationsByPlayers", topByPlayers);
        
        // Most common roles
        List<Map.Entry<String, Integer>> topRoles = roleDistribution.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topRoles", topRoles);
        
        // Most common religions
        List<Map.Entry<String, Integer>> topReligions = religionDistribution.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topReligions", topReligions);
        
        return stats;
    }
    
    public synchronized void clearPlayerData(UUID uuid) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", uuid.toString());
        save(uuid, o);
    }
    
    /**
     * Set multiple fields at once.
     */
    public synchronized void setFields(UUID uuid, Map<String, String> fields) {
        JsonObject o = load(uuid);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            o.addProperty(entry.getKey(), entry.getValue());
        }
        save(uuid, o);
    }
    
    /**
     * Check if player has field.
     */
    public synchronized boolean hasField(UUID uuid, String key) {
        JsonObject o = load(uuid);
        return o.has(key);
    }
    
    /**
     * Remove field.
     */
    public synchronized void removeField(UUID uuid, String key) {
        JsonObject o = load(uuid);
        o.remove(key);
        save(uuid, o);
    }
    
    /**
     * Get player balance (from wallet or data).
     */
    public synchronized double getPlayerBalance(UUID uuid) {
        if (plugin.getWalletService() != null) {
            return plugin.getWalletService().getBalance(uuid);
        }
        return getNumericField(uuid, "balance");
    }
    
    /**
     * Set player balance.
     */
    public synchronized void setPlayerBalance(UUID uuid, double balance) {
        if (plugin.getWalletService() != null) {
            // Use wallet service if available
            plugin.getWalletService().setBalance(uuid, balance);
        } else {
            setNumericField(uuid, "balance", balance);
        }
    }
    
    /**
     * Add to player balance.
     */
    public synchronized double addToBalance(UUID uuid, double amount) {
        double current = getPlayerBalance(uuid);
        double newBalance = current + amount;
        setPlayerBalance(uuid, newBalance);
        return newBalance;
    }
}


