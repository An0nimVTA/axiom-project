package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manages infrastructure development (roads, bridges, utilities). */
public class InfrastructureService {
    private final AXIOM plugin;
    private final File infrastructureDir;
    private final Map<String, InfrastructureData> nationInfrastructure = new HashMap<>(); // nationId -> data

    public static class InfrastructureData {
        double roadNetwork = 0.0; // 0-100
        double utilities = 0.0; // 0-100
        double bridges = 0.0; // 0-100
    }

    public InfrastructureService(AXIOM plugin) {
        this.plugin = plugin;
        this.infrastructureDir = new File(plugin.getDataFolder(), "infrastructure");
        this.infrastructureDir.mkdirs();
        loadAll();
    }

    public synchronized String buildInfrastructure(String nationId, String type, double cost) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        InfrastructureData data = nationInfrastructure.computeIfAbsent(nationId, k -> new InfrastructureData());
        double improvement = cost / 1000.0;
        switch (type.toLowerCase()) {
            case "road": data.roadNetwork = Math.min(100, data.roadNetwork + improvement); break;
            case "utility": data.utilities = Math.min(100, data.utilities + improvement); break;
            case "bridge": data.bridges = Math.min(100, data.bridges + improvement); break;
        }
        n.setTreasury(n.getTreasury() - cost);
        try {
            plugin.getNationManager().save(n);
            saveInfrastructure(nationId, data);
        } catch (Exception ignored) {}
        return "Инфраструктура улучшена: " + type;
    }

    public synchronized double getInfrastructureBonus(String nationId) {
        InfrastructureData data = nationInfrastructure.get(nationId);
        if (data == null) return 1.0;
        double avg = (data.roadNetwork + data.utilities + data.bridges) / 3.0;
        return 1.0 + (avg / 200.0); // Up to 1.5x bonus
    }
    
    /**
     * Get comprehensive infrastructure statistics.
     */
    public synchronized Map<String, Object> getInfrastructureStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        InfrastructureData data = nationInfrastructure.get(nationId);
        
        if (data == null) {
            stats.put("roadNetwork", 0.0);
            stats.put("utilities", 0.0);
            stats.put("bridges", 0.0);
            stats.put("totalInfrastructure", 0.0);
            stats.put("bonus", 1.0);
            return stats;
        }
        
        stats.put("roadNetwork", data.roadNetwork);
        stats.put("utilities", data.utilities);
        stats.put("bridges", data.bridges);
        
        double total = (data.roadNetwork + data.utilities + data.bridges) / 3.0;
        stats.put("totalInfrastructure", total);
        stats.put("bonus", getInfrastructureBonus(nationId));
        
        // Infrastructure rating
        String rating = "ОТЛИЧНО";
        if (total < 70) rating = "ХОРОШО";
        if (total < 50) rating = "СРЕДНЕ";
        if (total < 30) rating = "НИЗКО";
        if (total < 20) rating = "ОЧЕНЬ_НИЗКО";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Calculate maintenance cost for infrastructure.
     */
    public synchronized double calculateMaintenanceCost(String nationId) {
        InfrastructureData data = nationInfrastructure.get(nationId);
        if (data == null) return 0.0;
        
        // Base maintenance cost scales with infrastructure level
        double total = (data.roadNetwork + data.utilities + data.bridges) / 3.0;
        return total * 10.0; // Cost per hour
    }
    
    /**
     * Get infrastructure level for a specific type.
     */
    public synchronized double getInfrastructureLevel(String nationId, String type) {
        InfrastructureData data = nationInfrastructure.get(nationId);
        if (data == null) return 0.0;
        
        switch (type.toLowerCase()) {
            case "road": return data.roadNetwork;
            case "utility": return data.utilities;
            case "bridge": return data.bridges;
            default: return 0.0;
        }
    }
    
    /**
     * Calculate transport efficiency bonus from infrastructure.
     */
    public synchronized double getTransportEfficiency(String nationId) {
        InfrastructureData data = nationInfrastructure.get(nationId);
        if (data == null) return 1.0;
        
        // Road network directly affects transport efficiency
        return 1.0 + (data.roadNetwork / 200.0); // Up to 1.5x
    }

    private void loadAll() {
        File[] files = infrastructureDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                InfrastructureData data = new InfrastructureData();
                data.roadNetwork = o.has("roadNetwork") ? o.get("roadNetwork").getAsDouble() : 0.0;
                data.utilities = o.has("utilities") ? o.get("utilities").getAsDouble() : 0.0;
                data.bridges = o.has("bridges") ? o.get("bridges").getAsDouble() : 0.0;
                nationInfrastructure.put(nationId, data);
            } catch (Exception ignored) {}
        }
    }

    private void saveInfrastructure(String nationId, InfrastructureData data) {
        File f = new File(infrastructureDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("roadNetwork", data.roadNetwork);
        o.addProperty("utilities", data.utilities);
        o.addProperty("bridges", data.bridges);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get global infrastructure statistics.
     */
    public synchronized Map<String, Object> getGlobalInfrastructureStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalRoadNetwork = 0.0;
        double totalUtilities = 0.0;
        double totalBridges = 0.0;
        int nationsWithInfrastructure = 0;
        
        for (InfrastructureData data : nationInfrastructure.values()) {
            nationsWithInfrastructure++;
            totalRoadNetwork += data.roadNetwork;
            totalUtilities += data.utilities;
            totalBridges += data.bridges;
        }
        
        stats.put("nationsWithInfrastructure", nationsWithInfrastructure);
        stats.put("averageRoadNetwork", nationsWithInfrastructure > 0 ? totalRoadNetwork / nationsWithInfrastructure : 0);
        stats.put("averageUtilities", nationsWithInfrastructure > 0 ? totalUtilities / nationsWithInfrastructure : 0);
        stats.put("averageBridges", nationsWithInfrastructure > 0 ? totalBridges / nationsWithInfrastructure : 0);
        stats.put("averageTotalInfrastructure", nationsWithInfrastructure > 0 ? 
            (totalRoadNetwork + totalUtilities + totalBridges) / (nationsWithInfrastructure * 3) : 0);
        
        // Top nations by infrastructure
        List<Map.Entry<String, Double>> topByInfrastructure = new ArrayList<>();
        for (Map.Entry<String, InfrastructureData> entry : nationInfrastructure.entrySet()) {
            double total = (entry.getValue().roadNetwork + entry.getValue().utilities + entry.getValue().bridges) / 3.0;
            topByInfrastructure.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), total));
        }
        topByInfrastructure.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByInfrastructure", topByInfrastructure.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

