package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages resource discovery events. */
public class ResourceDiscoveryService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File discoveriesDir;
    private final Map<String, List<ResourceDiscovery>> discoveries = new HashMap<>(); // nationId -> discoveries

    public static class ResourceDiscovery {
        String resourceType;
        double quantity;
        String location; // chunk key
        long discoveredAt;
        boolean exploited;
    }

    public ResourceDiscoveryService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.discoveriesDir = new File(plugin.getDataFolder(), "discoveries");
        this.discoveriesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::generateDiscoveries, 0, 20 * 60 * 30); // every 30 minutes
    }

    private void generateDiscoveries() {
        // Randomly discover resources in nation territories
        for (Nation n : nationManager.getAll()) {
            if (n.getClaimedChunkKeys().isEmpty()) continue;
            if (Math.random() < 0.1) { // 10% chance per nation
                String[] resources = {"oil", "gold", "iron", "coal", "rare_metals"};
                String resource = resources[(int)(Math.random() * resources.length)];
                String chunkKey = n.getClaimedChunkKeys().iterator().next(); // Random chunk
                ResourceDiscovery disc = new ResourceDiscovery();
                disc.resourceType = resource;
                disc.quantity = 100 + Math.random() * 900; // 100-1000
                disc.location = chunkKey;
                disc.discoveredAt = System.currentTimeMillis();
                disc.exploited = false;
                discoveries.computeIfAbsent(n.getId(), k -> new ArrayList<>()).add(disc);
                n.getHistory().add("Обнаружены ресурсы: " + resource + " (" + String.format("%.0f", disc.quantity) + ")");
                try {
                    nationManager.save(n);
                    saveDiscoveries(n.getId());
                } catch (Exception ignored) {}
            }
        }
    }

    public synchronized String exploitDiscovery(String nationId, String location) {
        List<ResourceDiscovery> discList = discoveries.get(nationId);
        if (discList == null) return "Открытий не найдено.";
        ResourceDiscovery disc = discList.stream()
            .filter(d -> d.location.equals(location) && !d.exploited)
            .findFirst()
            .orElse(null);
        if (disc == null) return "Открытие не найдено или уже использовано.";
        plugin.getResourceService().addResource(nationId, disc.resourceType, disc.quantity);
        disc.exploited = true;
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Ресурсы добыты: " + disc.resourceType);
            try { nationManager.save(n); } catch (Exception ignored) {}
        }
        saveDiscoveries(nationId);
        return "Ресурсы добыты: " + disc.quantity + " " + disc.resourceType;
    }

    private void loadAll() {
        File[] files = discoveriesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                List<ResourceDiscovery> discList = new ArrayList<>();
                if (o.has("discoveries")) {
                    for (var elem : o.getAsJsonArray("discoveries")) {
                        JsonObject dObj = elem.getAsJsonObject();
                        ResourceDiscovery disc = new ResourceDiscovery();
                        disc.resourceType = dObj.get("resourceType").getAsString();
                        disc.quantity = dObj.get("quantity").getAsDouble();
                        disc.location = dObj.get("location").getAsString();
                        disc.discoveredAt = dObj.get("discoveredAt").getAsLong();
                        disc.exploited = dObj.has("exploited") && dObj.get("exploited").getAsBoolean();
                        discList.add(disc);
                    }
                }
                discoveries.put(nationId, discList);
            } catch (Exception ignored) {}
        }
    }

    private void saveDiscoveries(String nationId) {
        File f = new File(discoveriesDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<ResourceDiscovery> discList = discoveries.get(nationId);
        if (discList != null) {
            for (ResourceDiscovery disc : discList) {
                JsonObject dObj = new JsonObject();
                dObj.addProperty("resourceType", disc.resourceType);
                dObj.addProperty("quantity", disc.quantity);
                dObj.addProperty("location", disc.location);
                dObj.addProperty("discoveredAt", disc.discoveredAt);
                dObj.addProperty("exploited", disc.exploited);
                arr.add(dObj);
            }
        }
        o.add("discoveries", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive resource discovery statistics for a nation.
     */
    public synchronized Map<String, Object> getResourceDiscoveryStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<ResourceDiscovery> discList = discoveries.get(nationId);
        if (discList == null || discList.isEmpty()) {
            stats.put("hasDiscoveries", false);
            stats.put("totalDiscoveries", 0);
            return stats;
        }
        
        int total = discList.size();
        int exploited = 0;
        int unexploited = 0;
        double totalQuantity = 0.0;
        Map<String, Integer> discoveriesByResource = new HashMap<>();
        Map<String, Double> quantityByResource = new HashMap<>();
        
        for (ResourceDiscovery disc : discList) {
            if (disc.exploited) exploited++;
            else unexploited++;
            
            totalQuantity += disc.quantity;
            discoveriesByResource.put(disc.resourceType,
                discoveriesByResource.getOrDefault(disc.resourceType, 0) + 1);
            quantityByResource.put(disc.resourceType,
                quantityByResource.getOrDefault(disc.resourceType, 0.0) + disc.quantity);
        }
        
        stats.put("hasDiscoveries", true);
        stats.put("totalDiscoveries", total);
        stats.put("exploited", exploited);
        stats.put("unexploited", unexploited);
        stats.put("exploitationRate", total > 0 ? (double) exploited / total : 0);
        stats.put("totalQuantity", totalQuantity);
        stats.put("averageQuantity", total > 0 ? totalQuantity / total : 0);
        stats.put("discoveriesByResource", discoveriesByResource);
        stats.put("quantityByResource", quantityByResource);
        
        // Discovery rating
        String rating = "НЕТ ОТКРЫТИЙ";
        if (total >= 10) rating = "БОГАТЫЙ";
        else if (total >= 5) rating = "РАЗВИТЫЙ";
        else if (total >= 3) rating = "УМЕРЕННЫЙ";
        else if (total >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global resource discovery statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalResourceDiscoveryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalDiscoveries = 0;
        int totalExploited = 0;
        int totalUnexploited = 0;
        double totalQuantity = 0.0;
        Map<String, Integer> discoveriesByNation = new HashMap<>();
        Map<String, Integer> discoveriesByResource = new HashMap<>();
        Map<String, Double> quantityByResource = new HashMap<>();
        
        for (Map.Entry<String, List<ResourceDiscovery>> entry : discoveries.entrySet()) {
            String nationId = entry.getKey();
            List<ResourceDiscovery> discList = entry.getValue();
            
            int nationTotal = discList.size();
            
            totalDiscoveries += nationTotal;
            discoveriesByNation.put(nationId, nationTotal);
            
            for (ResourceDiscovery disc : discList) {
                if (disc.exploited) {
                    totalExploited++;
                } else {
                    totalUnexploited++;
                }
                
                totalQuantity += disc.quantity;
                
                discoveriesByResource.put(disc.resourceType,
                    discoveriesByResource.getOrDefault(disc.resourceType, 0) + 1);
                quantityByResource.put(disc.resourceType,
                    quantityByResource.getOrDefault(disc.resourceType, 0.0) + disc.quantity);
            }
        }
        
        stats.put("totalDiscoveries", totalDiscoveries);
        stats.put("totalExploited", totalExploited);
        stats.put("totalUnexploited", totalUnexploited);
        stats.put("globalExploitationRate", totalDiscoveries > 0 ? (double) totalExploited / totalDiscoveries : 0);
        stats.put("totalQuantity", totalQuantity);
        stats.put("averageQuantity", totalDiscoveries > 0 ? totalQuantity / totalDiscoveries : 0);
        stats.put("discoveriesByNation", discoveriesByNation);
        stats.put("discoveriesByResource", discoveriesByResource);
        stats.put("quantityByResource", quantityByResource);
        stats.put("nationsWithDiscoveries", discoveriesByNation.size());
        
        // Average discoveries per nation
        stats.put("averageDiscoveriesPerNation", discoveriesByNation.size() > 0 ?
            (double) totalDiscoveries / discoveriesByNation.size() : 0);
        
        // Top nations by discoveries
        List<Map.Entry<String, Integer>> topByDiscoveries = discoveriesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByDiscoveries", topByDiscoveries);
        
        // Most discovered resource types
        List<Map.Entry<String, Integer>> topByResource = discoveriesByResource.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByResource", topByResource);
        
        return stats;
    }
}

