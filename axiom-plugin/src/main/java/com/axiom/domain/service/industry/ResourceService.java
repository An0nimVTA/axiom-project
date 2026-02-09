package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages nation resource stockpiles (iron, coal, food, etc.). */
public class ResourceService {
    private final AXIOM plugin;
    private final File resourcesDir;
    private final Map<String, ResourceStock> nationResources = new HashMap<>(); // nationId -> stock

    public static class ResourceStock {
        Map<String, Double> resources = new HashMap<>(); // resource name -> amount
    }

    public ResourceService(AXIOM plugin) {
        this.plugin = plugin;
        this.resourcesDir = new File(plugin.getDataFolder(), "resources");
        this.resourcesDir.mkdirs();
        loadAll();
    }

    public synchronized void addResource(String nationId, String resourceName, double amount) {
        if (isBlank(nationId) || isBlank(resourceName)) return;
        if (!Double.isFinite(amount) || amount <= 0) return;
        String normalized = normalizeResourceId(resourceName);
        ResourceStock stock = nationResources.computeIfAbsent(nationId, k -> new ResourceStock());
        stock.resources.put(normalized, stock.resources.getOrDefault(normalized, 0.0) + amount);
        saveStock(nationId, stock);
    }

    public synchronized boolean consumeResource(String nationId, String resourceName, double amount) {
        if (isBlank(nationId) || isBlank(resourceName)) return false;
        if (!Double.isFinite(amount) || amount <= 0) return false;
        String normalized = normalizeResourceId(resourceName);
        ResourceStock stock = nationResources.get(nationId);
        if (stock == null) return false;
        double current = stock.resources.getOrDefault(normalized, 0.0);
        if (current < amount) return false;
        stock.resources.put(normalized, current - amount);
        saveStock(nationId, stock);
        return true;
    }

    public synchronized double getResource(String nationId, String resourceName) {
        if (isBlank(nationId) || isBlank(resourceName)) return 0.0;
        String normalized = normalizeResourceId(resourceName);
        ResourceStock stock = nationResources.get(nationId);
        return stock != null ? stock.resources.getOrDefault(normalized, 0.0) : 0.0;
    }

    public synchronized double getResourceStockpile(String nationId, String resourceName) {
        return getResource(nationId, resourceName);
    }
    
    /**
     * Get all resources for a nation.
     */
    public synchronized Map<String, Double> getNationResources(String nationId) {
        if (isBlank(nationId)) return new HashMap<>();
        ResourceStock stock = nationResources.get(nationId);
        if (stock == null) return new HashMap<>();
        return new HashMap<>(stock.resources);
    }
    
    /**
     * Get comprehensive resource statistics.
     */
    public synchronized Map<String, Object> getResourceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        if (isBlank(nationId)) return stats;
        ResourceStock stock = nationResources.get(nationId);
        
        if (stock == null) {
            stats.put("totalResources", 0);
            stats.put("resourceTypes", 0);
            stats.put("resources", Collections.emptyMap());
            return stats;
        }
        
        Map<String, Double> resources = stock.resources;
        double total = resources.values().stream().mapToDouble(Double::doubleValue).sum();
        
        stats.put("totalResources", total);
        stats.put("resourceTypes", resources.size());
        stats.put("resources", new HashMap<>(resources));
        stats.put("totalValue", calculateResourceValue(nationId));
        
        // Top resources
        List<Map.Entry<String, Double>> topResources = resources.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topResources", topResources);
        
        return stats;
    }
    
    /**
     * Transfer resources between nations (trade, tribute, etc.).
     */
    public synchronized String transferResource(String fromNationId, String toNationId, String resourceName, double amount) {
        if (isBlank(fromNationId) || isBlank(toNationId) || isBlank(resourceName)) {
            return "Неверные параметры.";
        }
        if (!Double.isFinite(amount) || amount <= 0) return "Некорректное количество.";
        if (fromNationId.equals(toNationId)) return "Нельзя перевести ресурсы самому себе.";
        if (!consumeResource(fromNationId, resourceName, amount)) {
            return "Недостаточно ресурса у отправителя.";
        }
        addResource(toNationId, resourceName, amount);
        return "Переведено " + amount + " " + resourceName + " от '" + fromNationId + "' к '" + toNationId + "'";
    }
    
    /**
     * Calculate total resource value (for economy integration).
     */
    public synchronized double calculateResourceValue(String nationId) {
        if (isBlank(nationId)) return 0.0;
        ResourceStock stock = nationResources.get(nationId);
        if (stock == null) return 0.0;
        CommodityMarketService marketService = plugin.getCommodityMarketService();
        ResourceCatalogService catalogService = plugin.getResourceCatalogService();
        double total = 0.0;
        for (Map.Entry<String, Double> entry : stock.resources.entrySet()) {
            String resource = entry.getKey();
            double amount = entry.getValue() != null ? entry.getValue() : 0.0;
            if (amount <= 0) continue;
            double price = 10.0;
            if (marketService != null && marketService.hasPrice(resource)) {
                price = marketService.getPrice(resource);
            } else if (catalogService != null) {
                price = catalogService.getBasePrice(resource);
            }
            total += amount * price;
        }
        return total;
    }

    private void loadAll() {
        File[] files = resourcesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                ResourceStock stock = new ResourceStock();
                for (var entry : o.entrySet()) {
                    stock.resources.put(entry.getKey(), entry.getValue().getAsDouble());
                }
                String nationId = f.getName().replace(".json", "");
                if (!isBlank(nationId)) {
                    nationResources.put(nationId, stock);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveStock(String nationId, ResourceStock stock) {
        if (isBlank(nationId) || stock == null) return;
        File f = new File(resourcesDir, nationId + ".json");
        JsonObject o = new JsonObject();
        for (var entry : stock.resources.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                o.addProperty(entry.getKey(), entry.getValue());
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get global resource statistics.
     */
    public synchronized Map<String, Object> getGlobalResourceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Double> globalResources = new HashMap<>();
        double totalValue = 0.0;
        
        for (Map.Entry<String, ResourceStock> entry : nationResources.entrySet()) {
            ResourceStock stock = entry.getValue();
            if (stock == null) continue;
            for (Map.Entry<String, Double> resourceEntry : stock.resources.entrySet()) {
                if (resourceEntry.getKey() == null || resourceEntry.getValue() == null) continue;
                globalResources.put(resourceEntry.getKey(),
                    globalResources.getOrDefault(resourceEntry.getKey(), 0.0) + resourceEntry.getValue());
            }
            totalValue += calculateResourceValue(entry.getKey());
        }
        
        stats.put("totalResourceTypes", globalResources.size());
        stats.put("totalResourceValue", totalValue);
        stats.put("globalResources", globalResources);
        
        // Top resources by total amount
        List<Map.Entry<String, Double>> topResources = globalResources.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topResources", topResources);
        
        // Nations with resources
        stats.put("nationsWithResources", nationResources.size());
        
        return stats;
    }
    
    private String getNationIdFromStock(ResourceStock stock) {
        for (Map.Entry<String, ResourceStock> entry : nationResources.entrySet()) {
            if (entry.getValue() == stock) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeResourceId(String resourceName) {
        ResourceCatalogService catalog = plugin.getResourceCatalogService();
        return catalog != null ? catalog.resolveResourceId(resourceName) : resourceName;
    }
}

