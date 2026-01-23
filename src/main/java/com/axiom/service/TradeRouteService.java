package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages trade routes between cities and nations. */
public class TradeRouteService {
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
        this.routesDir = new File(plugin.getDataFolder(), "traderoutes");
        this.routesDir.mkdirs();
        loadAll();
        Bukkit.getScheduler().runTaskTimer(plugin, this::processRoutes, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String establishRoute(String cityA, String cityB, String nationA, String nationB, double cost) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        Nation a = nationManager.getNationById(nationA);
        Nation b = nationManager.getNationById(nationB);
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
            nationManager.save(a);
            nationManager.save(b);
            saveRoute(tr);
        } catch (Exception ignored) {}
        return "Торговый путь установлен.";
    }

    private void processRoutes() {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        ResourceService resourceService = ServiceLocator.get(ResourceService.class);
        for (TradeRoute tr : routes.values()) {
            if (!tr.active) continue;
            resourceService.addResource(tr.nationA, "food", tr.capacity * 0.1);
            resourceService.addResource(tr.nationB, "food", tr.capacity * 0.1);
            Nation a = nationManager.getNationById(tr.nationA);
            Nation b = nationManager.getNationById(tr.nationB);
            if (a != null && b != null) {
                a.setTreasury(a.getTreasury() + tr.capacity * 0.5);
                b.setTreasury(b.getTreasury() + tr.capacity * 0.5);
                try {
                    nationManager.save(a);
                    nationManager.save(b);
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
                tr.id = o.get("id").getAsString();
                tr.cityA = o.get("cityA").getAsString();
                tr.cityB = o.get("cityB").getAsString();
                tr.nationA = o.get("nationA").getAsString();
                tr.nationB = o.get("nationB").getAsString();
                tr.capacity = o.get("capacity").getAsDouble();
                tr.active = o.has("active") && o.get("active").getAsBoolean();
                tr.establishedAt = o.get("establishedAt").getAsLong();
                routes.put(tr.id, tr);
            } catch (Exception ignored) {}
        }
    }

    private void saveRoute(TradeRoute tr) {
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
}

