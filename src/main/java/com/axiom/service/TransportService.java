package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages trade routes and transport networks between cities/nations. */
public class TransportService {
    private final AXIOM plugin;
    private final File routesDir;
    private final Map<String, TradeRoute> routes = new HashMap<>(); // routeId -> route

    public static class TradeRoute {
        String id;
        String fromCityId;
        String toCityId;
        String fromNationId;
        String toNationId;
        double tradeBonus; // multiplier (1.0 = no bonus)
        boolean active;
    }

    public TransportService(AXIOM plugin) {
        this.plugin = plugin;
        this.routesDir = new File(plugin.getDataFolder(), "routes");
        this.routesDir.mkdirs();
        loadAll();
    }

    public synchronized String createRoute(String fromCityId, String toCityId, String fromNationId, String toNationId) throws IOException {
        String id = fromCityId + "_to_" + toCityId;
        if (routes.containsKey(id)) return "Маршрут уже существует.";
        TradeRoute r = new TradeRoute();
        r.id = id;
        r.fromCityId = fromCityId;
        r.toCityId = toCityId;
        r.fromNationId = fromNationId;
        r.toNationId = toNationId;
        r.tradeBonus = 1.1; // 10% bonus
        r.active = true;
        routes.put(id, r);
        saveRoute(r);
        return "Торговый маршрут создан: " + fromCityId + " → " + toCityId;
    }

    public synchronized double getTradeBonus(String fromNationId, String toNationId) {
        double bonus = 1.0;
        for (TradeRoute r : routes.values()) {
            if (r.active && ((r.fromNationId.equals(fromNationId) && r.toNationId.equals(toNationId)) ||
                (r.fromNationId.equals(toNationId) && r.toNationId.equals(fromNationId)))) {
                bonus *= r.tradeBonus;
            }
        }
        return bonus;
    }

    private void loadAll() {
        File[] files = routesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                TradeRoute route = new TradeRoute();
                route.id = o.get("id").getAsString();
                route.fromCityId = o.get("fromCityId").getAsString();
                route.toCityId = o.get("toCityId").getAsString();
                route.fromNationId = o.get("fromNationId").getAsString();
                route.toNationId = o.get("toNationId").getAsString();
                route.tradeBonus = o.get("tradeBonus").getAsDouble();
                route.active = o.get("active").getAsBoolean();
                routes.put(route.id, route);
            } catch (Exception ignored) {}
        }
    }

    private void saveRoute(TradeRoute r) throws IOException {
        File f = new File(routesDir, r.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", r.id);
        o.addProperty("fromCityId", r.fromCityId);
        o.addProperty("toCityId", r.toCityId);
        o.addProperty("fromNationId", r.fromNationId);
        o.addProperty("toNationId", r.toNationId);
        o.addProperty("tradeBonus", r.tradeBonus);
        o.addProperty("active", r.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive transport statistics.
     */
    public synchronized Map<String, Object> getTransportStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<TradeRoute> nationRoutes = new ArrayList<>();
        for (TradeRoute r : routes.values()) {
            if (r.fromNationId.equals(nationId) || r.toNationId.equals(nationId)) {
                nationRoutes.add(r);
            }
        }
        
        stats.put("totalRoutes", nationRoutes.size());
        stats.put("activeRoutes", nationRoutes.stream().filter(r -> r.active).count());
        
        // Calculate total trade bonus from routes
        double totalBonus = nationRoutes.stream()
            .filter(r -> r.active)
            .mapToDouble(r -> r.tradeBonus - 1.0)
            .sum();
        stats.put("totalTradeBonus", totalBonus * 100); // Convert to percentage
        
        // Routes by destination
        Map<String, Integer> byDestination = new HashMap<>();
        for (TradeRoute r : nationRoutes) {
            String dest = r.fromNationId.equals(nationId) ? r.toNationId : r.fromNationId;
            byDestination.put(dest, byDestination.getOrDefault(dest, 0) + 1);
        }
        stats.put("byDestination", byDestination);
        
        return stats;
    }
    
    /**
     * Deactivate a route (temporary closure).
     */
    public synchronized String deactivateRoute(String routeId) throws IOException {
        TradeRoute r = routes.get(routeId);
        if (r == null) return "Маршрут не найден.";
        
        r.active = false;
        saveRoute(r);
        return "Маршрут деактивирован.";
    }
    
    /**
     * Activate a route.
     */
    public synchronized String activateRoute(String routeId) throws IOException {
        TradeRoute r = routes.get(routeId);
        if (r == null) return "Маршрут не найден.";
        
        r.active = true;
        saveRoute(r);
        return "Маршрут активирован.";
    }
    
    /**
     * Upgrade route (increase trade bonus).
     */
    public synchronized String upgradeRoute(String routeId, double newBonus) throws IOException {
        TradeRoute r = routes.get(routeId);
        if (r == null) return "Маршрут не найден.";
        
        r.tradeBonus = newBonus;
        saveRoute(r);
        return "Маршрут улучшен. Бонус: +" + String.format("%.0f", (newBonus - 1.0) * 100) + "%";
    }
    
    /**
     * Get all routes for a nation.
     */
    public synchronized List<TradeRoute> getNationRoutes(String nationId) {
        List<TradeRoute> result = new ArrayList<>();
        for (TradeRoute r : routes.values()) {
            if (r.fromNationId.equals(nationId) || r.toNationId.equals(nationId)) {
                result.add(r);
            }
        }
        return result;
    }
    
    /**
     * Calculate total trade bonus from all active routes.
     */
    public synchronized double calculateTotalTradeBonus(String nationId) {
        return getNationRoutes(nationId).stream()
            .filter(r -> r.active)
            .mapToDouble(r -> r.tradeBonus)
            .reduce(1.0, (a, b) -> a * b);
    }
    
    /**
     * Get global transport statistics.
     */
    public synchronized Map<String, Object> getGlobalTransportStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRoutes", routes.size());
        stats.put("activeRoutes", routes.values().stream().filter(r -> r.active).count());
        
        // Routes by nation
        Map<String, Integer> routesByNation = new HashMap<>();
        for (TradeRoute r : routes.values()) {
            routesByNation.put(r.fromNationId, routesByNation.getOrDefault(r.fromNationId, 0) + 1);
            routesByNation.put(r.toNationId, routesByNation.getOrDefault(r.toNationId, 0) + 1);
        }
        stats.put("routesByNation", routesByNation);
        
        // Top nations by routes
        List<Map.Entry<String, Integer>> topByRoutes = routesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRoutes", topByRoutes);
        
        // Average trade bonus
        double avgBonus = routes.values().stream()
            .filter(r -> r.active)
            .mapToDouble(r -> r.tradeBonus)
            .average()
            .orElse(1.0);
        stats.put("averageTradeBonus", avgBonus);
        
        return stats;
    }
}

