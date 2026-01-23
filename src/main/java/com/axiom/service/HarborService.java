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

/** Manages harbors and ports for maritime trade. */
public class HarborService {
    private final AXIOM plugin;
    private final File harborsDir;
    private final Map<String, Harbor> cityHarbors = new HashMap<>(); // cityId -> harbor

    public static class Harbor {
        String cityId;
        String nationId;
        int capacity;
        double tradeBonus;
        boolean active;
    }

    public HarborService(AXIOM plugin) {
        this.plugin = plugin;
        this.harborsDir = new File(plugin.getDataFolder(), "harbors");
        this.harborsDir.mkdirs();
        loadAll();
    }

    public synchronized String buildHarbor(String cityId, String nationId, double cost) {
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        Harbor h = new Harbor();
        h.cityId = cityId;
        h.nationId = nationId;
        h.capacity = 10;
        h.tradeBonus = 1.15; // 15% trade bonus
        h.active = true;
        n.setTreasury(n.getTreasury() - cost);
        cityHarbors.put(cityId, h);
        try {
            plugin.getNationManager().save(n);
            saveHarbor(h);
        } catch (Exception ignored) {}
        return "Порт построен в городе. Бонус торговли: +15%";
    }

    public synchronized Harbor getHarbor(String cityId) {
        return cityHarbors.get(cityId);
    }

    private void loadAll() {
        File[] files = harborsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Harbor h = new Harbor();
                h.cityId = o.get("cityId").getAsString();
                h.nationId = o.get("nationId").getAsString();
                h.capacity = o.get("capacity").getAsInt();
                h.tradeBonus = o.get("tradeBonus").getAsDouble();
                h.active = o.get("active").getAsBoolean();
                cityHarbors.put(h.cityId, h);
            } catch (Exception ignored) {}
        }
    }

    private void saveHarbor(Harbor h) {
        File f = new File(harborsDir, h.cityId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("cityId", h.cityId);
        o.addProperty("nationId", h.nationId);
        o.addProperty("capacity", h.capacity);
        o.addProperty("tradeBonus", h.tradeBonus);
        o.addProperty("active", h.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive harbor statistics.
     */
    public synchronized Map<String, Object> getHarborStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Harbors for this nation
        List<Harbor> nationHarbors = new ArrayList<>();
        double totalTradeBonus = 0.0;
        int totalCapacity = 0;
        
        for (Harbor h : cityHarbors.values()) {
            if (h.nationId.equals(nationId) && h.active) {
                nationHarbors.add(h);
                totalTradeBonus += (h.tradeBonus - 1.0); // Bonus percentage
                totalCapacity += h.capacity;
            }
        }
        
        stats.put("totalHarbors", nationHarbors.size());
        stats.put("activeHarbors", nationHarbors.size());
        stats.put("totalTradeBonus", 1.0 + totalTradeBonus); // Combined multiplier
        stats.put("totalCapacity", totalCapacity);
        
        // Harbor details
        List<Map<String, Object>> harborsList = new ArrayList<>();
        for (Harbor h : nationHarbors) {
            Map<String, Object> harborData = new HashMap<>();
            harborData.put("cityId", h.cityId);
            harborData.put("capacity", h.capacity);
            harborData.put("tradeBonus", h.tradeBonus);
            harborData.put("tradeBonusPercent", (h.tradeBonus - 1.0) * 100);
            harborsList.add(harborData);
        }
        stats.put("harborsList", harborsList);
        
        // Average capacity
        double avgCapacity = nationHarbors.stream()
            .mapToInt(h -> h.capacity)
            .average()
            .orElse(0.0);
        stats.put("averageCapacity", avgCapacity);
        
        // Harbor rating
        String rating = "НЕТ ПОРТОВ";
        if (nationHarbors.size() >= 10) rating = "МОРСКАЯ ИМПЕРИЯ";
        else if (nationHarbors.size() >= 5) rating = "МОРСКАЯ ДЕРЖАВА";
        else if (nationHarbors.size() >= 3) rating = "РАЗВИТАЯ ТОРГОВЛЯ";
        else if (nationHarbors.size() >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Upgrade harbor.
     */
    public synchronized String upgradeHarbor(String cityId, String nationId, double cost) throws IOException {
        Harbor h = cityHarbors.get(cityId);
        if (h == null) return "Порт не найден.";
        if (!h.nationId.equals(nationId)) return "Порт принадлежит другой нации.";
        
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        h.capacity += 5;
        h.tradeBonus += 0.05; // +5% trade bonus per upgrade
        n.setTreasury(n.getTreasury() - cost);
        
        plugin.getNationManager().save(n);
        saveHarbor(h);
        
        return "Порт улучшен. Вместимость: " + h.capacity + ", Бонус: " + String.format("%.1f", (h.tradeBonus - 1.0) * 100) + "%";
    }
    
    /**
     * Destroy harbor.
     */
    public synchronized String destroyHarbor(String cityId, String nationId) throws IOException {
        Harbor h = cityHarbors.get(cityId);
        if (h == null) return "Порт не найден.";
        if (!h.nationId.equals(nationId)) return "Порт принадлежит другой нации.";
        
        h.active = false;
        cityHarbors.remove(cityId);
        File f = new File(harborsDir, cityId + ".json");
        if (f.exists()) f.delete();
        
        return "Порт уничтожен.";
    }
    
    /**
     * Get all harbors for nation.
     */
    public synchronized List<Harbor> getNationHarbors(String nationId) {
        return cityHarbors.values().stream()
            .filter(h -> h.nationId.equals(nationId) && h.active)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate total trade bonus from harbors.
     */
    public synchronized double getTotalTradeBonus(String nationId) {
        double totalBonus = 1.0;
        for (Harbor h : cityHarbors.values()) {
            if (h.nationId.equals(nationId) && h.active) {
                totalBonus *= h.tradeBonus;
            }
        }
        // Cap at +100%
        return Math.min(2.0, totalBonus);
    }
    
    /**
     * Get global harbor statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalHarborStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalHarbors = 0;
        int activeHarbors = 0;
        Map<String, Integer> harborsByNation = new HashMap<>();
        int totalCapacity = 0;
        double totalTradeBonus = 0.0;
        Map<String, Integer> capacityByNation = new HashMap<>();
        
        for (Harbor h : cityHarbors.values()) {
            totalHarbors++;
            if (h.active) {
                activeHarbors++;
                harborsByNation.put(h.nationId, harborsByNation.getOrDefault(h.nationId, 0) + 1);
                totalCapacity += h.capacity;
                totalTradeBonus += (h.tradeBonus - 1.0); // Bonus percentage
                capacityByNation.put(h.nationId, 
                    capacityByNation.getOrDefault(h.nationId, 0) + h.capacity);
            }
        }
        
        stats.put("totalHarbors", totalHarbors);
        stats.put("activeHarbors", activeHarbors);
        stats.put("harborsByNation", harborsByNation);
        stats.put("totalCapacity", totalCapacity);
        stats.put("averageCapacity", activeHarbors > 0 ? (double) totalCapacity / activeHarbors : 0);
        stats.put("averageTradeBonus", activeHarbors > 0 ? (totalTradeBonus / activeHarbors) * 100 : 0);
        stats.put("nationsWithHarbors", harborsByNation.size());
        
        // Top nations by harbors
        List<Map.Entry<String, Integer>> topByHarbors = harborsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByHarbors", topByHarbors);
        
        // Top nations by capacity
        List<Map.Entry<String, Integer>> topByCapacity = capacityByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCapacity", topByCapacity);
        
        // Average harbors per nation
        stats.put("averageHarborsPerNation", harborsByNation.size() > 0 ? 
            (double) activeHarbors / harborsByNation.size() : 0);
        
        return stats;
    }
}

