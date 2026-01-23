package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Manages energy production and consumption. */
public class EnergyService {
    private final AXIOM plugin;
    private final File energyDir;
    private final Map<String, EnergyData> nationEnergy = new HashMap<>(); // nationId -> data

    public static class EnergyData {
        double production; // per hour
        double consumption; // per hour
        double storage; // current stored energy
        double maxStorage;
        String source; // "coal", "solar", "nuclear", etc.
    }

    public EnergyService(AXIOM plugin) {
        this.plugin = plugin;
        this.energyDir = new File(plugin.getDataFolder(), "energy");
        this.energyDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processEnergy, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String buildPowerPlant(String nationId, String source, double cost, double production) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        EnergyData ed = nationEnergy.computeIfAbsent(nationId, k -> {
            EnergyData d = new EnergyData();
            d.production = 0;
            d.consumption = 100; // base consumption
            d.storage = 0;
            d.maxStorage = 1000;
            d.source = "none";
            return d;
        });
        ed.production += production;
        ed.source = source;
        n.setTreasury(n.getTreasury() - cost);
        try {
            plugin.getNationManager().save(n);
            saveEnergy(nationId, ed);
        } catch (Exception ignored) {}
        return "Электростанция построена: " + source + " (+" + production + "/час)";
    }

    private void processEnergy() {
        for (Map.Entry<String, EnergyData> e : nationEnergy.entrySet()) {
            EnergyData ed = e.getValue();
            double net = ed.production - ed.consumption;
            ed.storage = Math.min(ed.maxStorage, ed.storage + net * 10 / 60); // per 10 minutes
            if (ed.storage < 0) {
                // Energy deficit - penalties
                Nation n = plugin.getNationManager().getNationById(e.getKey());
                if (n != null) {
                    plugin.getHappinessService().modifyHappiness(e.getKey(), -5.0);
                }
                ed.storage = 0;
            }
            saveEnergy(e.getKey(), ed);
        }
    }

    private void loadAll() {
        File[] files = energyDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                EnergyData ed = new EnergyData();
                ed.production = o.get("production").getAsDouble();
                ed.consumption = o.get("consumption").getAsDouble();
                ed.storage = o.get("storage").getAsDouble();
                ed.maxStorage = o.get("maxStorage").getAsDouble();
                ed.source = o.get("source").getAsString();
                nationEnergy.put(nationId, ed);
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * Get comprehensive energy statistics for a nation.
     */
    public synchronized Map<String, Object> getEnergyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        EnergyData ed = nationEnergy.get(nationId);
        if (ed == null) {
            stats.put("hasData", false);
            return stats;
        }
        
        stats.put("hasData", true);
        stats.put("production", ed.production);
        stats.put("consumption", ed.consumption);
        stats.put("netProduction", ed.production - ed.consumption);
        stats.put("storage", ed.storage);
        stats.put("maxStorage", ed.maxStorage);
        stats.put("storageUtilization", ed.maxStorage > 0 ? ed.storage / ed.maxStorage : 0);
        stats.put("source", ed.source);
        
        // Energy status rating
        double net = ed.production - ed.consumption;
        String rating = "ДЕФИЦИТ";
        if (net >= 100) rating = "ИЗБЫТОК";
        else if (net >= 50) rating = "СБАЛАНСИРОВАН";
        else if (net >= 0) rating = "МИНИМАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global energy statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalEnergyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalNations = nationEnergy.size();
        double totalProduction = 0.0;
        double totalConsumption = 0.0;
        double totalStorage = 0.0;
        double totalMaxStorage = 0.0;
        Map<String, Double> productionByNation = new HashMap<>();
        Map<String, Double> consumptionByNation = new HashMap<>();
        Map<String, Double> netProductionByNation = new HashMap<>();
        Map<String, String> sourceByNation = new HashMap<>();
        int nationsWithDeficit = 0;
        
        for (Map.Entry<String, EnergyData> entry : nationEnergy.entrySet()) {
            String nationId = entry.getKey();
            EnergyData ed = entry.getValue();
            
            totalProduction += ed.production;
            totalConsumption += ed.consumption;
            totalStorage += ed.storage;
            totalMaxStorage += ed.maxStorage;
            
            productionByNation.put(nationId, ed.production);
            consumptionByNation.put(nationId, ed.consumption);
            double net = ed.production - ed.consumption;
            netProductionByNation.put(nationId, net);
            sourceByNation.put(nationId, ed.source);
            
            if (net < 0) {
                nationsWithDeficit++;
            }
        }
        
        stats.put("totalNations", totalNations);
        stats.put("totalProduction", totalProduction);
        stats.put("totalConsumption", totalConsumption);
        stats.put("globalNetProduction", totalProduction - totalConsumption);
        stats.put("totalStorage", totalStorage);
        stats.put("totalMaxStorage", totalMaxStorage);
        stats.put("globalStorageUtilization", totalMaxStorage > 0 ? totalStorage / totalMaxStorage : 0);
        stats.put("nationsWithDeficit", nationsWithDeficit);
        stats.put("productionByNation", productionByNation);
        stats.put("consumptionByNation", consumptionByNation);
        stats.put("netProductionByNation", netProductionByNation);
        stats.put("sourceByNation", sourceByNation);
        
        // Average statistics
        stats.put("averageProduction", totalNations > 0 ? totalProduction / totalNations : 0);
        stats.put("averageConsumption", totalNations > 0 ? totalConsumption / totalNations : 0);
        stats.put("averageNetProduction", totalNations > 0 ? (totalProduction - totalConsumption) / totalNations : 0);
        
        // Top nations by production
        List<Map.Entry<String, Double>> topByProduction = productionByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByProduction", topByProduction);
        
        // Top nations by net production
        List<Map.Entry<String, Double>> topByNet = netProductionByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByNet", topByNet);
        
        // Energy source distribution
        Map<String, Integer> sourceDistribution = new HashMap<>();
        for (String source : sourceByNation.values()) {
            sourceDistribution.put(source, sourceDistribution.getOrDefault(source, 0) + 1);
        }
        stats.put("sourceDistribution", sourceDistribution);
        
        return stats;
    }

    private void saveEnergy(String nationId, EnergyData ed) {
        File f = new File(energyDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("production", ed.production);
        o.addProperty("consumption", ed.consumption);
        o.addProperty("storage", ed.storage);
        o.addProperty("maxStorage", ed.maxStorage);
        o.addProperty("source", ed.source);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    public synchronized double getEnergyStorage(String nationId) {
        EnergyData ed = nationEnergy.get(nationId);
        return ed != null ? ed.storage : 0.0;
    }
}

