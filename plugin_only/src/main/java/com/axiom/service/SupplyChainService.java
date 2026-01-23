package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages supply chains and resource distribution between cities. */
public class SupplyChainService {
    private final AXIOM plugin;
    private final File chainsDir;
    private final Map<String, SupplyChain> chains = new HashMap<>(); // chainId -> chain

    public static class SupplyChain {
        String id;
        String sourceCityId;
        String targetCityId;
        String resourceType;
        double ratePerHour;
        boolean active;
    }

    public SupplyChainService(AXIOM plugin) {
        this.plugin = plugin;
        this.chainsDir = new File(plugin.getDataFolder(), "supplychains");
        this.chainsDir.mkdirs();
        loadAll();
    }

    public synchronized String createChain(String sourceCityId, String targetCityId, String resourceType, double ratePerHour) throws IOException {
        String id = sourceCityId + "_" + targetCityId + "_" + resourceType;
        if (chains.containsKey(id)) return "Цепочка уже существует.";
        SupplyChain sc = new SupplyChain();
        sc.id = id;
        sc.sourceCityId = sourceCityId;
        sc.targetCityId = targetCityId;
        sc.resourceType = resourceType;
        sc.ratePerHour = ratePerHour;
        sc.active = true;
        chains.put(id, sc);
        saveChain(sc);
        return "Цепочка поставок создана: " + resourceType + " (" + ratePerHour + "/час)";
    }

    public synchronized void processChains() {
        for (SupplyChain sc : chains.values()) {
            if (!sc.active) continue;
            String sourceNationId = plugin.getCityGrowthEngine().getCitiesOf("").stream()
                .filter(c -> c.getId().equals(sc.sourceCityId))
                .findFirst()
                .map(c -> c.getNationId())
                .orElse(null);
            if (sourceNationId == null) continue;
            double amount = sc.ratePerHour / 60.0; // per minute
            if (plugin.getResourceService().consumeResource(sourceNationId, sc.resourceType, amount)) {
                String targetNationId = plugin.getCityGrowthEngine().getCitiesOf("").stream()
                    .filter(c -> c.getId().equals(sc.targetCityId))
                    .findFirst()
                    .map(c -> c.getNationId())
                    .orElse(null);
                if (targetNationId != null) {
                    plugin.getResourceService().addResource(targetNationId, sc.resourceType, amount);
                }
            }
        }
    }

    private void loadAll() {
        File[] files = chainsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                SupplyChain sc = new SupplyChain();
                sc.id = o.get("id").getAsString();
                sc.sourceCityId = o.get("sourceCityId").getAsString();
                sc.targetCityId = o.get("targetCityId").getAsString();
                sc.resourceType = o.get("resourceType").getAsString();
                sc.ratePerHour = o.get("ratePerHour").getAsDouble();
                sc.active = o.get("active").getAsBoolean();
                chains.put(sc.id, sc);
            } catch (Exception ignored) {}
        }
    }

    private void saveChain(SupplyChain sc) throws IOException {
        File f = new File(chainsDir, sc.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", sc.id);
        o.addProperty("sourceCityId", sc.sourceCityId);
        o.addProperty("targetCityId", sc.targetCityId);
        o.addProperty("resourceType", sc.resourceType);
        o.addProperty("ratePerHour", sc.ratePerHour);
        o.addProperty("active", sc.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive supply chain statistics.
     */
    public synchronized Map<String, Object> getSupplyChainStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<SupplyChain> nationChains = new ArrayList<>();
        for (SupplyChain sc : chains.values()) {
            // Check if chain belongs to this nation
            com.axiom.model.City sourceCity = plugin.getCityGrowthEngine().getCity(sc.sourceCityId);
            com.axiom.model.City targetCity = plugin.getCityGrowthEngine().getCity(sc.targetCityId);
            if (sourceCity != null && sourceCity.getNationId().equals(nationId) ||
                targetCity != null && targetCity.getNationId().equals(nationId)) {
                nationChains.add(sc);
            }
        }
        
        stats.put("totalChains", nationChains.size());
        stats.put("activeChains", nationChains.stream().filter(sc -> sc.active).count());
        
        // Calculate total throughput
        double totalThroughput = nationChains.stream()
            .filter(sc -> sc.active)
            .mapToDouble(sc -> sc.ratePerHour)
            .sum();
        stats.put("totalThroughput", totalThroughput);
        
        // Chains by resource type
        Map<String, Integer> byResource = new HashMap<>();
        for (SupplyChain sc : nationChains) {
            byResource.put(sc.resourceType, byResource.getOrDefault(sc.resourceType, 0) + 1);
        }
        stats.put("byResource", byResource);
        
        return stats;
    }
    
    /**
     * Deactivate a supply chain.
     */
    public synchronized String deactivateChain(String chainId) throws IOException {
        SupplyChain sc = chains.get(chainId);
        if (sc == null) return "Цепочка не найдена.";
        
        sc.active = false;
        saveChain(sc);
        return "Цепочка поставок деактивирована.";
    }
    
    /**
     * Activate a supply chain.
     */
    public synchronized String activateChain(String chainId) throws IOException {
        SupplyChain sc = chains.get(chainId);
        if (sc == null) return "Цепочка не найдена.";
        
        sc.active = true;
        saveChain(sc);
        return "Цепочка поставок активирована.";
    }
    
    /**
     * Get all supply chains for a nation.
     */
    public synchronized List<SupplyChain> getNationChains(String nationId) {
        List<SupplyChain> result = new ArrayList<>();
        for (SupplyChain sc : chains.values()) {
            com.axiom.model.City sourceCity = plugin.getCityGrowthEngine().getCity(sc.sourceCityId);
            com.axiom.model.City targetCity = plugin.getCityGrowthEngine().getCity(sc.targetCityId);
            if (sourceCity != null && sourceCity.getNationId().equals(nationId) ||
                targetCity != null && targetCity.getNationId().equals(nationId)) {
                result.add(sc);
            }
        }
        return result;
    }
    
    /**
     * Update chain rate.
     */
    public synchronized String updateChainRate(String chainId, double newRate) throws IOException {
        SupplyChain sc = chains.get(chainId);
        if (sc == null) return "Цепочка не найдена.";
        
        sc.ratePerHour = newRate;
        saveChain(sc);
        return "Скорость цепочки обновлена: " + newRate + "/час";
    }
    
    /**
     * Get global supply chain statistics.
     */
    public synchronized Map<String, Object> getGlobalSupplyChainStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalChains", chains.size());
        stats.put("activeChains", chains.values().stream().filter(sc -> sc.active).count());
        
        // Chains by resource type
        Map<String, Integer> byResource = new HashMap<>();
        for (SupplyChain sc : chains.values()) {
            if (sc.active) {
                byResource.put(sc.resourceType, byResource.getOrDefault(sc.resourceType, 0) + 1);
            }
        }
        stats.put("chainsByResource", byResource);
        
        // Total throughput
        double totalThroughput = chains.values().stream()
            .filter(sc -> sc.active)
            .mapToDouble(sc -> sc.ratePerHour)
            .sum();
        stats.put("totalThroughput", totalThroughput);
        stats.put("averageThroughput", chains.size() > 0 ? totalThroughput / chains.size() : 0);
        
        return stats;
    }
}

