package com.axiom.domain.service.industry;

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
        if (isBlank(sourceCityId) || isBlank(targetCityId) || isBlank(resourceType)) return "Неверные параметры.";
        if (sourceCityId.equals(targetCityId)) return "Источник и цель не могут совпадать.";
        if (!Double.isFinite(ratePerHour) || ratePerHour <= 0) return "Некорректная скорость.";
        ResourceCatalogService catalog = plugin.getResourceCatalogService();
        if (catalog != null && !catalog.isKnownResource(resourceType)) {
            return "Неизвестный ресурс: " + resourceType;
        }
        if (plugin.getCityGrowthEngine() == null) return "Сервис городов недоступен.";
        com.axiom.domain.model.City sourceCity = plugin.getCityGrowthEngine().getCity(sourceCityId);
        com.axiom.domain.model.City targetCity = plugin.getCityGrowthEngine().getCity(targetCityId);
        if (sourceCity == null || targetCity == null) return "Город не найден.";
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
        if (plugin.getCityGrowthEngine() == null || plugin.getResourceService() == null) return;
        for (SupplyChain sc : chains.values()) {
            if (sc == null || !sc.active) continue;
            com.axiom.domain.model.City sourceCity = plugin.getCityGrowthEngine().getCity(sc.sourceCityId);
            com.axiom.domain.model.City targetCity = plugin.getCityGrowthEngine().getCity(sc.targetCityId);
            String sourceNationId = sourceCity != null ? sourceCity.getNationId() : null;
            if (sourceNationId == null) continue;
            String targetNationId = targetCity != null ? targetCity.getNationId() : null;
            if (targetNationId == null) continue;
            double amount = sc.ratePerHour / 60.0; // per minute
            if (!Double.isFinite(amount) || amount <= 0) continue;
            if (plugin.getResourceService().consumeResource(sourceNationId, sc.resourceType, amount)) {
                plugin.getResourceService().addResource(targetNationId, sc.resourceType, amount);
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
                sc.id = o.has("id") ? o.get("id").getAsString() : null;
                sc.sourceCityId = o.has("sourceCityId") ? o.get("sourceCityId").getAsString() : null;
                sc.targetCityId = o.has("targetCityId") ? o.get("targetCityId").getAsString() : null;
                sc.resourceType = o.has("resourceType") ? o.get("resourceType").getAsString() : null;
                sc.ratePerHour = o.has("ratePerHour") ? o.get("ratePerHour").getAsDouble() : 0.0;
                sc.active = o.has("active") && o.get("active").getAsBoolean();
                if (!isBlank(sc.id)) {
                    chains.put(sc.id, sc);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveChain(SupplyChain sc) throws IOException {
        if (sc == null || isBlank(sc.id)) return;
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
        
        if (isBlank(nationId)) return stats;
        List<SupplyChain> nationChains = new ArrayList<>();
        if (plugin.getCityGrowthEngine() == null) {
            stats.put("totalChains", 0);
            stats.put("activeChains", 0);
            stats.put("totalThroughput", 0.0);
            stats.put("byResource", Collections.emptyMap());
            return stats;
        }
        for (SupplyChain sc : chains.values()) {
            if (sc == null) continue;
            // Check if chain belongs to this nation
            com.axiom.domain.model.City sourceCity = plugin.getCityGrowthEngine().getCity(sc.sourceCityId);
            com.axiom.domain.model.City targetCity = plugin.getCityGrowthEngine().getCity(sc.targetCityId);
            if ((sourceCity != null && nationId.equals(sourceCity.getNationId())) ||
                (targetCity != null && nationId.equals(targetCity.getNationId()))) {
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
            if (sc.resourceType != null) {
                byResource.put(sc.resourceType, byResource.getOrDefault(sc.resourceType, 0) + 1);
            }
        }
        stats.put("byResource", byResource);
        
        return stats;
    }
    
    /**
     * Deactivate a supply chain.
     */
    public synchronized String deactivateChain(String chainId) throws IOException {
        if (isBlank(chainId)) return "Неверные параметры.";
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
        if (isBlank(chainId)) return "Неверные параметры.";
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
        if (isBlank(nationId)) return Collections.emptyList();
        List<SupplyChain> result = new ArrayList<>();
        if (plugin.getCityGrowthEngine() == null) return result;
        for (SupplyChain sc : chains.values()) {
            if (sc == null) continue;
            com.axiom.domain.model.City sourceCity = plugin.getCityGrowthEngine().getCity(sc.sourceCityId);
            com.axiom.domain.model.City targetCity = plugin.getCityGrowthEngine().getCity(sc.targetCityId);
            if ((sourceCity != null && nationId.equals(sourceCity.getNationId())) ||
                (targetCity != null && nationId.equals(targetCity.getNationId()))) {
                result.add(sc);
            }
        }
        return result;
    }
    
    /**
     * Update chain rate.
     */
    public synchronized String updateChainRate(String chainId, double newRate) throws IOException {
        if (isBlank(chainId)) return "Неверные параметры.";
        if (!Double.isFinite(newRate) || newRate <= 0) return "Некорректная скорость.";
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
            if (sc != null && sc.active && sc.resourceType != null) {
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

