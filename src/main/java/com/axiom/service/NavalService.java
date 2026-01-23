package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manages naval power and maritime trade. */
public class NavalService {
    private final AXIOM plugin;
    private final File navalDir;
    private final Map<String, NavalPower> nationNaval = new HashMap<>(); // nationId -> power

    public static class NavalPower {
        int ships = 0;
        double tradeBonus = 1.0;
        double defenseBonus = 1.0;
    }

    public NavalService(AXIOM plugin) {
        this.plugin = plugin;
        this.navalDir = new File(plugin.getDataFolder(), "naval");
        this.navalDir.mkdirs();
        loadAll();
    }

    public synchronized String buildShip(String nationId, double cost) {
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        NavalPower power = nationNaval.computeIfAbsent(nationId, k -> new NavalPower());
        power.ships++;
        power.tradeBonus = 1.0 + (power.ships * 0.02); // 2% per ship
        power.defenseBonus = 1.0 + (power.ships * 0.01); // 1% per ship
        n.setTreasury(n.getTreasury() - cost);
        try {
            plugin.getNationManager().save(n);
            saveNaval(nationId, power);
        } catch (Exception ignored) {}
        return "Корабль построен. Всего: " + power.ships;
    }

    public synchronized NavalPower getNavalPower(String nationId) {
        return nationNaval.getOrDefault(nationId, new NavalPower());
    }

    private void loadAll() {
        File[] files = navalDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                NavalPower power = new NavalPower();
                power.ships = o.has("ships") ? o.get("ships").getAsInt() : 0;
                power.tradeBonus = o.has("tradeBonus") ? o.get("tradeBonus").getAsDouble() : 1.0;
                power.defenseBonus = o.has("defenseBonus") ? o.get("defenseBonus").getAsDouble() : 1.0;
                nationNaval.put(nationId, power);
            } catch (Exception ignored) {}
        }
    }

    private void saveNaval(String nationId, NavalPower power) {
        File f = new File(navalDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("ships", power.ships);
        o.addProperty("tradeBonus", power.tradeBonus);
        o.addProperty("defenseBonus", power.defenseBonus);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive naval statistics.
     */
    public synchronized Map<String, Object> getNavalStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        NavalPower power = getNavalPower(nationId);
        
        stats.put("ships", power.ships);
        stats.put("tradeBonus", power.tradeBonus);
        stats.put("defenseBonus", power.defenseBonus);
        
        // Naval rating
        String rating = "ОТСУТСТВУЕТ";
        if (power.ships >= 50) rating = "МОГУЧИЙ";
        else if (power.ships >= 30) rating = "СИЛЬНЫЙ";
        else if (power.ships >= 20) rating = "РАЗВИТЫЙ";
        else if (power.ships >= 10) rating = "СРЕДНИЙ";
        else if (power.ships >= 5) rating = "НАЧАЛЬНЫЙ";
        else if (power.ships > 0) rating = "СЛАБЫЙ";
        stats.put("rating", rating);
        
        // Calculate naval capacity
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n != null) {
            // Base capacity from territories (especially coastal ones)
            int capacity = n.getClaimedChunkKeys().size() / 10; // 1 ship per 10 chunks
            stats.put("capacity", capacity);
            stats.put("utilization", capacity > 0 ? (power.ships / (double) capacity) * 100 : 0);
        }
        
        // Naval maintenance cost
        double maintenanceCost = power.ships * 5.0; // Cost per hour
        stats.put("maintenanceCost", maintenanceCost);
        
        return stats;
    }
    
    /**
     * Destroy ship (loss in battle, etc.).
     */
    public synchronized String destroyShip(String nationId, int count) throws IOException {
        NavalPower power = nationNaval.get(nationId);
        if (power == null || power.ships < count) {
            return "Недостаточно кораблей.";
        }
        
        power.ships -= count;
        power.tradeBonus = 1.0 + (power.ships * 0.02);
        power.defenseBonus = 1.0 + (power.ships * 0.01);
        
        saveNaval(nationId, power);
        
        return "Потеряно кораблей: " + count + ". Осталось: " + power.ships;
    }
    
    /**
     * Upgrade ships (improve quality/efficiency).
     */
    public synchronized String upgradeShips(String nationId, double cost) throws IOException {
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        
        NavalPower power = nationNaval.get(nationId);
        if (power == null || power.ships == 0) return "Нет кораблей для улучшения.";
        
        // Improve bonuses
        power.tradeBonus *= 1.1; // +10% to trade bonus
        power.defenseBonus *= 1.05; // +5% to defense bonus
        
        n.setTreasury(n.getTreasury() - cost);
        
        plugin.getNationManager().save(n);
        saveNaval(nationId, power);
        
        return "Флот улучшен. Новый торговый бонус: +" + String.format("%.0f", (power.tradeBonus - 1.0) * 100) + "%";
    }
    
    /**
     * Calculate naval dominance score.
     */
    public synchronized double getNavalDominanceScore(String nationId) {
        NavalPower power = getNavalPower(nationId);
        
        // Score based on ship count and bonuses
        double score = power.ships * 10.0;
        score += (power.tradeBonus - 1.0) * 500.0;
        score += (power.defenseBonus - 1.0) * 300.0;
        
        return score;
    }
    
    /**
     * Get global naval statistics.
     */
    public synchronized Map<String, Object> getGlobalNavalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalShips = 0;
        int maxShips = 0;
        double totalTradeBonus = 0.0;
        double totalDefenseBonus = 0.0;
        
        for (NavalPower power : nationNaval.values()) {
            totalShips += power.ships;
            maxShips = Math.max(maxShips, power.ships);
            totalTradeBonus += (power.tradeBonus - 1.0);
            totalDefenseBonus += (power.defenseBonus - 1.0);
        }
        
        stats.put("totalShips", totalShips);
        stats.put("maxShips", maxShips);
        stats.put("averageShips", nationNaval.size() > 0 ? (double) totalShips / nationNaval.size() : 0);
        stats.put("totalTradeBonus", 1.0 + totalTradeBonus);
        stats.put("totalDefenseBonus", 1.0 + totalDefenseBonus);
        stats.put("nationsWithNaval", nationNaval.size());
        
        // Top navies
        List<Map.Entry<String, Integer>> topNavies = new ArrayList<>();
        for (Map.Entry<String, NavalPower> entry : nationNaval.entrySet()) {
            topNavies.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().ships));
        }
        topNavies.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        stats.put("topNavies", topNavies.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

