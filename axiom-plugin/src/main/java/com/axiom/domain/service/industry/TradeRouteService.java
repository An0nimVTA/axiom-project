package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages trade routes between cities and nations. */
public class TradeRouteService {
    private final AXIOM plugin;
    private final File routesDir;
    private final Map<String, TradeRoute> routes = new HashMap<>(); // routeId -> route

    public static class TradeRoute {
        String id;
        String cityA;
        String cityB;
        String nationA;
        String nationB;
        double capacity; // per hour
        boolean active;
        long establishedAt;
    }

    public TradeRouteService(AXIOM plugin) {
        this.plugin = plugin;
        this.routesDir = new File(plugin.getDataFolder(), "traderoutes");
        this.routesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processRoutes, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String establishRoute(String cityA, String cityB, String nationA, String nationB, double cost) {
        if (isBlank(cityA) || isBlank(cityB) || isBlank(nationA) || isBlank(nationB)) return "Неверные параметры.";
        if (cityA.equals(cityB)) return "Города должны различаться.";
        if (nationA.equals(nationB)) return "Нельзя установить путь внутри одной нации.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        Nation a = plugin.getNationManager().getNationById(nationA);
        Nation b = plugin.getNationManager().getNationById(nationB);
        if (a == null || b == null) return "Нация не найдена.";
        if (a.getTreasury() < cost || b.getTreasury() < cost) return "Недостаточно средств.";
        String routeId = UUID.randomUUID().toString().substring(0, 8);
        TradeRoute tr = new TradeRoute();
        tr.id = routeId;
        tr.cityA = cityA;
        tr.cityB = cityB;
        tr.nationA = nationA;
        tr.nationB = nationB;
        tr.capacity = 100.0;
        tr.active = true;
        tr.establishedAt = System.currentTimeMillis();
        routes.put(routeId, tr);
        a.setTreasury(a.getTreasury() - cost);
        b.setTreasury(b.getTreasury() - cost);
        try {
            plugin.getNationManager().save(a);
            plugin.getNationManager().save(b);
            saveRoute(tr);
        } catch (Exception ignored) {}
        return "Торговый путь установлен.";
    }

    private synchronized void processRoutes() {
        if (plugin.getNationManager() == null || plugin.getResourceService() == null) return;
        for (TradeRoute tr : routes.values()) {
            if (tr == null || !tr.active) continue;
            if (isBlank(tr.nationA) || isBlank(tr.nationB)) continue;
            if (!Double.isFinite(tr.capacity) || tr.capacity <= 0) continue;
            // Exchange small amounts of resources
            plugin.getResourceService().addResource(tr.nationA, "food", tr.capacity * 0.1);
            plugin.getResourceService().addResource(tr.nationB, "food", tr.capacity * 0.1);
            // Both nations get economic benefit
            Nation a = plugin.getNationManager().getNationById(tr.nationA);
            Nation b = plugin.getNationManager().getNationById(tr.nationB);
            if (a != null && b != null) {
                a.setTreasury(a.getTreasury() + tr.capacity * 0.5);
                b.setTreasury(b.getTreasury() + tr.capacity * 0.5);
                try {
                    plugin.getNationManager().save(a);
                    plugin.getNationManager().save(b);
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadAll() {
        File[] files = routesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                TradeRoute tr = new TradeRoute();
                tr.id = o.has("id") ? o.get("id").getAsString() : null;
                tr.cityA = o.has("cityA") ? o.get("cityA").getAsString() : null;
                tr.cityB = o.has("cityB") ? o.get("cityB").getAsString() : null;
                tr.nationA = o.has("nationA") ? o.get("nationA").getAsString() : null;
                tr.nationB = o.has("nationB") ? o.get("nationB").getAsString() : null;
                tr.capacity = o.has("capacity") ? o.get("capacity").getAsDouble() : 0.0;
                tr.active = o.has("active") && o.get("active").getAsBoolean();
                tr.establishedAt = o.has("establishedAt") ? o.get("establishedAt").getAsLong() : System.currentTimeMillis();
                if (!isBlank(tr.id)) {
                    routes.put(tr.id, tr);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveRoute(TradeRoute tr) {
        if (tr == null || isBlank(tr.id)) return;
        File f = new File(routesDir, tr.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", tr.id);
        o.addProperty("cityA", tr.cityA);
        o.addProperty("cityB", tr.cityB);
        o.addProperty("nationA", tr.nationA);
        o.addProperty("nationB", tr.nationB);
        o.addProperty("capacity", tr.capacity);
        o.addProperty("active", tr.active);
        o.addProperty("establishedAt", tr.establishedAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

