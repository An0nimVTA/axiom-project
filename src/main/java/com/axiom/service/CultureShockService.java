package com.axiom.service;

import com.axiom.AXIOM;
import java.util.UUID;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** Manages cultural shock when players migrate between nations with different cultures. */
public class CultureShockService {
    private final AXIOM plugin;
    private final File shockDir;
    private final Map<String, Map<String, Double>> playerShock = new HashMap<>(); // playerUUID -> nationId -> shock level

    public CultureShockService(AXIOM plugin) {
        this.plugin = plugin;
        this.shockDir = new File(plugin.getDataFolder(), "cultureshock");
        this.shockDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processShock, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized void applyShock(UUID playerId, String oldNationId, String newNationId) {
        if (oldNationId == null || newNationId == null || oldNationId.equals(newNationId)) return;
        Nation oldN = plugin.getNationManager().getNationById(oldNationId);
        Nation newN = plugin.getNationManager().getNationById(newNationId);
        if (oldN == null || newN == null) return;
        // Calculate cultural difference
        double culturalDiff = Math.abs(plugin.getCultureService().getCulturalScore(oldNationId) - 
                                       plugin.getCultureService().getCulturalScore(newNationId));
        double shock = Math.min(100, culturalDiff * 0.5);
        Map<String, Double> shocks = playerShock.computeIfAbsent(playerId.toString(), k -> new HashMap<>());
        shocks.put(newNationId, shock);
        saveShock(playerId.toString(), shocks);
    }

    private void processShock() {
        // Gradually reduce shock over time
        for (Map<String, Double> shocks : playerShock.values()) {
            for (Map.Entry<String, Double> entry : shocks.entrySet()) {
                double newShock = Math.max(0, entry.getValue() - 0.5);
                shocks.put(entry.getKey(), newShock);
            }
        }
    }

    public synchronized double getShock(UUID playerId, String nationId) {
        Map<String, Double> shocks = playerShock.get(playerId.toString());
        return shocks != null ? shocks.getOrDefault(nationId, 0.0) : 0.0;
    }

    private void loadAll() {
        File[] files = shockDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String playerId = f.getName().replace(".json", "");
                Map<String, Double> shocks = new HashMap<>();
                for (var entry : o.entrySet()) {
                    shocks.put(entry.getKey(), entry.getValue().getAsDouble());
                }
                playerShock.put(playerId, shocks);
            } catch (Exception ignored) {}
        }
    }

    private void saveShock(String playerId, Map<String, Double> shocks) {
        File f = new File(shockDir, playerId + ".json");
        JsonObject o = new JsonObject();
        for (var entry : shocks.entrySet()) {
            o.addProperty(entry.getKey(), entry.getValue());
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
}

