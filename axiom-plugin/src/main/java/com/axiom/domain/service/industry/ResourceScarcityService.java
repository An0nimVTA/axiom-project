package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.axiom.domain.service.state.HappinessService;

/** Tracks resource scarcity and its economic impact. */
public class ResourceScarcityService {
    private final AXIOM plugin;
    private final File scarcityDir;
    private final Map<String, Map<String, Double>> scarcityLevels = new HashMap<>(); // nationId -> resource -> scarcity (0-100)

    public ResourceScarcityService(AXIOM plugin) {
        this.plugin = plugin;
        this.scarcityDir = new File(plugin.getDataFolder(), "scarcity");
        this.scarcityDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateScarcity, 0, 20 * 60 * 10); // every 10 minutes
    }

    private void updateScarcity() {
        if (plugin.getResourceService() == null) return;
        HappinessService happinessService = plugin.getHappinessService();
        for (Nation n : plugin.getNationManager().getAll()) {
            Map<String, Double> levels = scarcityLevels.computeIfAbsent(n.getId(), k -> new HashMap<>());
            // Check each resource
            for (String resource : getScarcityResources()) {
                double stockpile = plugin.getResourceService().getResourceStockpile(n.getId(), resource);
                double population = n.getCitizens().size();
                double perCitizenNeed = getPerCitizenNeed(resource);
                double scarcity = Math.max(0, 100 - (stockpile / Math.max(1, population * perCitizenNeed)) * 100);
                levels.put(resource, scarcity);
                if (scarcity > 70) {
                    // Severe scarcity - apply penalties
                    if (happinessService != null) {
                        happinessService.modifyHappiness(n.getId(), -5.0);
                    }
                    n.getHistory().add("Критическая нехватка " + resource);
                    try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
                }
            }
            saveScarcity(n.getId(), levels);
        }
    }

    public synchronized double getScarcity(String nationId, String resource) {
        Map<String, Double> levels = scarcityLevels.get(nationId);
        return levels != null && levels.containsKey(resource) ? levels.get(resource) : 0.0;
    }

    private void loadAll() {
        File[] files = scarcityDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                Map<String, Double> levels = new HashMap<>();
                for (var entry : o.entrySet()) {
                    levels.put(entry.getKey(), entry.getValue().getAsDouble());
                }
                scarcityLevels.put(nationId, levels);
            } catch (Exception ignored) {}
        }
    }

    private void saveScarcity(String nationId, Map<String, Double> levels) {
        File f = new File(scarcityDir, nationId + ".json");
        JsonObject o = new JsonObject();
        for (var entry : levels.entrySet()) {
            o.addProperty(entry.getKey(), entry.getValue());
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    private Set<String> getScarcityResources() {
        ResourceCatalogService catalog = plugin.getResourceCatalogService();
        if (catalog != null) {
            Set<String> resources = catalog.getScarcityResources();
            if (!resources.isEmpty()) {
                return resources;
            }
        }
        return new HashSet<>(java.util.List.of("food", "wood", "stone", "iron", "gold"));
    }

    private double getPerCitizenNeed(String resource) {
        ResourceCatalogService catalog = plugin.getResourceCatalogService();
        return catalog != null ? catalog.getPerCitizenNeed(resource) : 10.0;
    }
}

