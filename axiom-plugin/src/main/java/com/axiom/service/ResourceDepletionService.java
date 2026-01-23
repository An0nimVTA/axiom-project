package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** Tracks resource depletion and regeneration rates. */
public class ResourceDepletionService {
    private final AXIOM plugin;
    private final File depletionDir;
    private final Map<String, Map<String, Double>> depletionRates = new HashMap<>(); // nationId -> resource -> depletion

    public ResourceDepletionService(AXIOM plugin) {
        this.plugin = plugin;
        this.depletionDir = new File(plugin.getDataFolder(), "depletion");
        this.depletionDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processDepletion, 0, 20 * 60 * 5); // every 5 minutes
    }

    private synchronized void processDepletion() {
        // Natural resource regeneration and depletion
        if (plugin.getNationManager() == null || plugin.getResourceService() == null) return;
        java.util.Collection<com.axiom.model.Nation> nations = plugin.getNationManager().getAll();
        if (nations == null) return;
        for (com.axiom.model.Nation n : nations) {
            if (n == null || n.getId() == null) continue;
            // Regenerate some resources naturally
            plugin.getResourceService().addResource(n.getId(), "food", 10.0);
            plugin.getResourceService().addResource(n.getId(), "wood", 5.0);
            // Deplete based on usage
            java.util.Collection<java.util.UUID> citizens = n.getCitizens();
            double consumption = (citizens != null ? citizens.size() : 0) * 0.1;
            plugin.getResourceService().consumeResource(n.getId(), "food", consumption);
        }
    }

    public synchronized void setDepletionRate(String nationId, String resource, double rate) {
        if (isBlank(nationId) || isBlank(resource)) return;
        if (!Double.isFinite(rate)) return;
        Map<String, Double> rates = depletionRates.computeIfAbsent(nationId, k -> new HashMap<>());
        rates.put(resource, rate);
        saveDepletion(nationId);
    }

    private void loadAll() {
        File[] files = depletionDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                Map<String, Double> rates = new HashMap<>();
                for (var entry : o.entrySet()) {
                    rates.put(entry.getKey(), entry.getValue().getAsDouble());
                }
                depletionRates.put(nationId, rates);
            } catch (Exception ignored) {}
        }
    }

    private void saveDepletion(String nationId) {
        if (isBlank(nationId)) return;
        File f = new File(depletionDir, nationId + ".json");
        JsonObject o = new JsonObject();
        Map<String, Double> rates = depletionRates.get(nationId);
        if (rates != null) {
            for (var entry : rates.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    o.addProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

