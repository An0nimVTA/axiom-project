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

/** Manages cultural exchange programs between nations. */
public class CulturalExchangeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File exchangesDir;
    private final Map<String, CulturalExchange> activeExchanges = new HashMap<>(); // exchangeId -> exchange

    public static class CulturalExchange {
        String id;
        String nationA;
        String nationB;
        String type; // "students", "artists", "scholars", "athletes"
        double culturalBonus;
        long startTime;
        long endTime;
        boolean active;
    }

    public CulturalExchangeService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.exchangesDir = new File(plugin.getDataFolder(), "culturalexchanges");
        this.exchangesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processExchanges, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String startExchange(String nationA, String nationB, String type, int durationDays, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (nationA == null || nationB == null) return "Неверные параметры.";
        if (nationA.equals(nationB)) return "Нельзя начать обмен с самим собой.";
        if (type == null || type.trim().isEmpty()) return "Тип обмена не указан.";
        if (durationDays <= 0) return "Длительность должна быть больше 0.";
        if (cost <= 0) return "Стоимость должна быть больше 0.";
        Nation a = nationManager.getNationById(nationA);
        Nation b = nationManager.getNationById(nationB);
        if (a == null || b == null) return "Нация не найдена.";
        if (a.getTreasury() < cost || b.getTreasury() < cost) return "Недостаточно средств.";
        String exchangeId = UUID.randomUUID().toString().substring(0, 8);
        CulturalExchange exchange = new CulturalExchange();
        exchange.id = exchangeId;
        exchange.nationA = nationA;
        exchange.nationB = nationB;
        exchange.type = type;
        exchange.culturalBonus = 5.0; // Base bonus
        exchange.startTime = System.currentTimeMillis();
        exchange.endTime = System.currentTimeMillis() + durationDays * 24 * 60 * 60_000L;
        exchange.active = true;
        activeExchanges.put(exchangeId, exchange);
        a.setTreasury(a.getTreasury() - cost);
        b.setTreasury(b.getTreasury() - cost);
        a.getHistory().add("Культурный обмен с " + b.getName());
        b.getHistory().add("Культурный обмен с " + a.getName());
        try {
            nationManager.save(a);
            nationManager.save(b);
            saveExchange(exchange);
        } catch (Exception ignored) {}
        return "Культурный обмен начат: " + type;
    }

    private synchronized void processExchanges() {
        long now = System.currentTimeMillis();
        List<String> completed = new ArrayList<>();
        CultureService cultureService = plugin.getCultureService();
        DiplomacySystem diplomacySystem = plugin.getDiplomacySystem();
        for (Map.Entry<String, CulturalExchange> e : activeExchanges.entrySet()) {
            CulturalExchange exchange = e.getValue();
            if (now >= exchange.endTime) {
                exchange.active = false;
                saveExchange(exchange);
                completed.add(e.getKey());
            } else {
                // Increase cultural bonus over time
                exchange.culturalBonus = Math.min(20, exchange.culturalBonus + 0.5);
                // Apply bonuses
                if (cultureService != null) {
                    cultureService.developCulture(exchange.nationA, exchange.culturalBonus * 0.1);
                    cultureService.developCulture(exchange.nationB, exchange.culturalBonus * 0.1);
                }
                // Improve reputation
                try {
                    if (nationManager != null && diplomacySystem != null) {
                        Nation a = nationManager.getNationById(exchange.nationA);
                        Nation b = nationManager.getNationById(exchange.nationB);
                        if (a != null && b != null) {
                            diplomacySystem.setReputation(a, b, 1);
                            diplomacySystem.setReputation(b, a, 1);
                        }
                    }
                } catch (Exception ignored) {}
                saveExchange(exchange);
            }
        }
        for (String exchangeId : completed) {
            activeExchanges.remove(exchangeId);
        }
    }

    private void loadAll() {
        File[] files = exchangesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                CulturalExchange exchange = new CulturalExchange();
                exchange.id = o.get("id").getAsString();
                exchange.nationA = o.get("nationA").getAsString();
                exchange.nationB = o.get("nationB").getAsString();
                exchange.type = o.get("type").getAsString();
                exchange.culturalBonus = o.get("culturalBonus").getAsDouble();
                exchange.startTime = o.get("startTime").getAsLong();
                exchange.endTime = o.get("endTime").getAsLong();
                exchange.active = o.has("active") && o.get("active").getAsBoolean() && exchange.endTime > System.currentTimeMillis();
                if (exchange.active) activeExchanges.put(exchange.id, exchange);
            } catch (Exception ignored) {}
        }
    }

    private void saveExchange(CulturalExchange exchange) {
        File f = new File(exchangesDir, exchange.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", exchange.id);
        o.addProperty("nationA", exchange.nationA);
        o.addProperty("nationB", exchange.nationB);
        o.addProperty("type", exchange.type);
        o.addProperty("culturalBonus", exchange.culturalBonus);
        o.addProperty("startTime", exchange.startTime);
        o.addProperty("endTime", exchange.endTime);
        o.addProperty("active", exchange.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive cultural exchange statistics for a nation.
     */
    public synchronized Map<String, Object> getCulturalExchangeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        int activeCount = 0;
        Map<String, Integer> exchangesByType = new HashMap<>();
        double totalCulturalBonus = 0.0;
        List<String> partnerNations = new ArrayList<>();
        
        for (CulturalExchange exchange : activeExchanges.values()) {
            if (exchange.nationA.equals(nationId) || exchange.nationB.equals(nationId)) {
                if (exchange.active) {
                    activeCount++;
                    exchangesByType.put(exchange.type, exchangesByType.getOrDefault(exchange.type, 0) + 1);
                    totalCulturalBonus += exchange.culturalBonus;
                    if (exchange.nationA.equals(nationId)) {
                        if (!partnerNations.contains(exchange.nationB)) partnerNations.add(exchange.nationB);
                    } else {
                        if (!partnerNations.contains(exchange.nationA)) partnerNations.add(exchange.nationA);
                    }
                }
            }
        }
        
        stats.put("activeExchanges", activeCount);
        stats.put("exchangesByType", exchangesByType);
        stats.put("totalCulturalBonus", totalCulturalBonus);
        stats.put("averageCulturalBonus", activeCount > 0 ? totalCulturalBonus / activeCount : 0);
        stats.put("partnerNations", partnerNations);
        stats.put("uniquePartners", partnerNations.size());
        
        // Exchange rating
        String rating = "НЕТ ОБМЕНОВ";
        if (activeCount >= 10) rating = "ОБШИРНЫЙ";
        else if (activeCount >= 5) rating = "АКТИВНЫЙ";
        else if (activeCount >= 3) rating = "РАЗВИТЫЙ";
        else if (activeCount >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global cultural exchange statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCulturalExchangeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveExchanges", activeExchanges.size());
        
        Map<String, Integer> exchangesByNation = new HashMap<>();
        Map<String, Integer> exchangesByType = new HashMap<>();
        double totalCulturalBonus = 0.0;
        Set<String> allPartners = new HashSet<>();
        
        for (CulturalExchange exchange : activeExchanges.values()) {
            if (exchange.active) {
                exchangesByNation.put(exchange.nationA, exchangesByNation.getOrDefault(exchange.nationA, 0) + 1);
                exchangesByNation.put(exchange.nationB, exchangesByNation.getOrDefault(exchange.nationB, 0) + 1);
                exchangesByType.put(exchange.type, exchangesByType.getOrDefault(exchange.type, 0) + 1);
                totalCulturalBonus += exchange.culturalBonus;
                allPartners.add(exchange.nationA);
                allPartners.add(exchange.nationB);
            }
        }
        
        stats.put("exchangesByNation", exchangesByNation);
        stats.put("exchangesByType", exchangesByType);
        stats.put("totalCulturalBonus", totalCulturalBonus);
        stats.put("averageCulturalBonus", activeExchanges.size() > 0 ? totalCulturalBonus / activeExchanges.size() : 0);
        stats.put("nationsInExchanges", allPartners.size());
        
        // Top nations by exchanges
        List<Map.Entry<String, Integer>> topByExchanges = exchangesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByExchanges", topByExchanges);
        
        // Most common exchange types
        List<Map.Entry<String, Integer>> topByType = exchangesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByType", topByType);
        
        return stats;
    }
}

