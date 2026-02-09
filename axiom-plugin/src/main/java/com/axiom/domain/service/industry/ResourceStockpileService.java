package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages strategic resource stockpiles for emergency situations. */
public class ResourceStockpileService {
    private static final double DECAY_INTERVAL_MINUTES = 10.0;
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File stockpilesDir;
    private final Map<String, Stockpile> nationStockpiles = new HashMap<>(); // nationId -> stockpile

    public static class Stockpile {
        Map<String, Double> resources = new HashMap<>(); // resource -> amount
        double capacity;
        double usedCapacity;
        long lastUpdated;
    }

    public ResourceStockpileService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.stockpilesDir = new File(plugin.getDataFolder(), "stockpiles");
        this.stockpilesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processStockpiles, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String addToStockpile(String nationId, String resourceType, double amount) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(resourceType)) return "Неверные параметры.";
        if (!Double.isFinite(amount) || amount <= 0) return "Некорректное количество.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        Stockpile stockpile = nationStockpiles.computeIfAbsent(nationId, k -> {
            Stockpile s = new Stockpile();
            s.capacity = 10000.0; // Base capacity
            s.usedCapacity = 0.0;
            s.lastUpdated = System.currentTimeMillis();
            return s;
        });
        if (stockpile.usedCapacity + amount > stockpile.capacity) {
            return "Недостаточно места в хранилище.";
        }
        if (plugin.getResourceService() == null) return "Сервис ресурсов недоступен.";
        if (!plugin.getResourceService().consumeResource(nationId, resourceType, amount)) {
            return "Недостаточно ресурсов.";
        }
        double current = stockpile.resources.getOrDefault(resourceType, 0.0);
        stockpile.resources.put(resourceType, current + amount);
        stockpile.usedCapacity += amount;
        stockpile.lastUpdated = System.currentTimeMillis();
        saveStockpile(nationId, stockpile);
        return "Ресурсы добавлены в хранилище: " + amount + " " + resourceType;
    }

    public synchronized String releaseFromStockpile(String nationId, String resourceType, double amount) {
        if (isBlank(nationId) || isBlank(resourceType)) return "Неверные параметры.";
        if (!Double.isFinite(amount) || amount <= 0) return "Некорректное количество.";
        Stockpile stockpile = nationStockpiles.get(nationId);
        if (stockpile == null) return "Хранилище не найдено.";
        double current = stockpile.resources.getOrDefault(resourceType, 0.0);
        if (current < amount) return "Недостаточно ресурсов в хранилище.";
        stockpile.resources.put(resourceType, current - amount);
        stockpile.usedCapacity -= amount;
        if (plugin.getResourceService() != null) {
            plugin.getResourceService().addResource(nationId, resourceType, amount);
        }
        saveStockpile(nationId, stockpile);
        return "Ресурсы выпущены из хранилища: " + amount + " " + resourceType;
    }

    private synchronized void processStockpiles() {
        // Stockpiles slowly decay over time (natural spoilage)
        for (Map.Entry<String, Stockpile> e : nationStockpiles.entrySet()) {
            Stockpile stockpile = e.getValue();
            if (stockpile == null) continue;
            for (String resource : getDecayResources()) {
                double current = stockpile.resources.getOrDefault(resource, 0.0);
                if (current > 0) {
                    double decayRate = getDecayPerHour(resource);
                    double decay = current * decayRate * (DECAY_INTERVAL_MINUTES / 60.0);
                    stockpile.resources.put(resource, Math.max(0, current - decay));
                    stockpile.usedCapacity = Math.max(0, stockpile.usedCapacity - decay);
                }
            }
            saveStockpile(e.getKey(), stockpile);
        }
    }

    private void loadAll() {
        File[] files = stockpilesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                Stockpile stockpile = new Stockpile();
                stockpile.capacity = o.has("capacity") ? o.get("capacity").getAsDouble() : 0.0;
                stockpile.usedCapacity = o.has("usedCapacity") ? o.get("usedCapacity").getAsDouble() : 0.0;
                stockpile.lastUpdated = o.has("lastUpdated") ? o.get("lastUpdated").getAsLong() : System.currentTimeMillis();
                if (o.has("resources")) {
                    JsonObject resources = o.getAsJsonObject("resources");
                    if (resources != null) {
                        for (var entry : resources.entrySet()) {
                            stockpile.resources.put(entry.getKey(), entry.getValue().getAsDouble());
                        }
                    }
                }
                if (!isBlank(nationId)) {
                    nationStockpiles.put(nationId, stockpile);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveStockpile(String nationId, Stockpile stockpile) {
        if (isBlank(nationId) || stockpile == null) return;
        File f = new File(stockpilesDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("capacity", stockpile.capacity);
        o.addProperty("usedCapacity", stockpile.usedCapacity);
        o.addProperty("lastUpdated", stockpile.lastUpdated);
        JsonObject resources = new JsonObject();
        for (var entry : stockpile.resources.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                resources.addProperty(entry.getKey(), entry.getValue());
            }
        }
        o.add("resources", resources);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive resource stockpile statistics for a nation.
     */
    public synchronized Map<String, Object> getResourceStockpileStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        Stockpile stockpile = nationStockpiles.get(nationId);
        if (stockpile == null) {
            stats.put("hasStockpile", false);
            return stats;
        }
        
        stats.put("hasStockpile", true);
        stats.put("capacity", stockpile.capacity);
        stats.put("usedCapacity", stockpile.usedCapacity);
        stats.put("availableCapacity", stockpile.capacity - stockpile.usedCapacity);
        stats.put("utilizationRate", stockpile.capacity > 0 ? stockpile.usedCapacity / stockpile.capacity : 0);
        stats.put("resources", new HashMap<>(stockpile.resources));
        stats.put("resourceCount", stockpile.resources.size());
        
        // Calculate total value (simplified)
        double totalValue = stockpile.resources.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("totalValue", totalValue);
        
        // Stockpile rating
        double utilization = stockpile.capacity > 0 ? stockpile.usedCapacity / stockpile.capacity : 0;
        String rating = "ПУСТОЕ";
        if (utilization >= 0.9) rating = "ПОЛНОЕ";
        else if (utilization >= 0.7) rating = "ЗАПОЛНЕННОЕ";
        else if (utilization >= 0.5) rating = "СРЕДНЕЕ";
        else if (utilization >= 0.2) rating = "ЧАСТИЧНОЕ";
        else if (utilization > 0) rating = "МИНИМАЛЬНОЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global resource stockpile statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalResourceStockpileStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalStockpiles = nationStockpiles.size();
        double totalCapacity = 0.0;
        double totalUsed = 0.0;
        Map<String, Double> capacityByNation = new HashMap<>();
        Map<String, Double> usedByNation = new HashMap<>();
        Map<String, Integer> resourceCountByNation = new HashMap<>();
        Map<String, Integer> resourcesByType = new HashMap<>();
        
        for (Map.Entry<String, Stockpile> entry : nationStockpiles.entrySet()) {
            String nationId = entry.getKey();
            Stockpile stockpile = entry.getValue();
            if (stockpile == null) continue;
            
            totalCapacity += stockpile.capacity;
            totalUsed += stockpile.usedCapacity;
            capacityByNation.put(nationId, stockpile.capacity);
            usedByNation.put(nationId, stockpile.usedCapacity);
            resourceCountByNation.put(nationId, stockpile.resources.size());
            
            for (String resource : stockpile.resources.keySet()) {
                if (resource != null) {
                    resourcesByType.put(resource, resourcesByType.getOrDefault(resource, 0) + 1);
                }
            }
        }
        
        stats.put("totalStockpiles", totalStockpiles);
        stats.put("totalCapacity", totalCapacity);
        stats.put("totalUsed", totalUsed);
        stats.put("globalUtilizationRate", totalCapacity > 0 ? totalUsed / totalCapacity : 0);
        stats.put("capacityByNation", capacityByNation);
        stats.put("usedByNation", usedByNation);
        stats.put("resourceCountByNation", resourceCountByNation);
        stats.put("resourcesByType", resourcesByType);
        
        // Average statistics
        stats.put("averageCapacity", totalStockpiles > 0 ? totalCapacity / totalStockpiles : 0);
        stats.put("averageUsed", totalStockpiles > 0 ? totalUsed / totalStockpiles : 0);
        stats.put("averageUtilizationRate", capacityByNation.size() > 0 ? 
            usedByNation.values().stream().mapToDouble(Double::doubleValue).sum() / 
            capacityByNation.values().stream().mapToDouble(Double::doubleValue).sum() : 0);
        
        // Top nations by capacity
        List<Map.Entry<String, Double>> topByCapacity = capacityByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCapacity", topByCapacity);
        
        // Top nations by utilization
        Map<String, Double> utilizationByNation = new HashMap<>();
        for (String nationId : capacityByNation.keySet()) {
            double capacity = capacityByNation.get(nationId);
            double used = usedByNation.get(nationId);
            utilizationByNation.put(nationId, capacity > 0 ? used / capacity : 0);
        }
        List<Map.Entry<String, Double>> topByUtilization = utilizationByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByUtilization", topByUtilization);
        
        // Most stocked resource types
        List<Map.Entry<String, Integer>> topByResourceType = resourcesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByResourceType", topByResourceType);
        
        return stats;
    }

    private Set<String> getDecayResources() {
        ResourceCatalogService catalog = plugin.getResourceCatalogService();
        if (catalog != null) {
            Set<String> resources = catalog.getDecayResources();
            if (!resources.isEmpty()) {
                return resources;
            }
        }
        return new HashSet<>(List.of("food", "wood"));
    }

    private double getDecayPerHour(String resource) {
        ResourceCatalogService catalog = plugin.getResourceCatalogService();
        return catalog != null ? catalog.getDecayPerHour(resource) : 0.006;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

