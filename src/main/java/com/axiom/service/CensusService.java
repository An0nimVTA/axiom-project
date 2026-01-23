package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Conducts periodic censuses to track population and demographics. */
public class CensusService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File censusDir;
    private final Map<String, CensusData> lastCensus = new HashMap<>(); // nationId -> data

    public static class CensusData {
        int totalPopulation;
        int activePlayers;
        int cities;
        long conductedAt;
    }

    public CensusService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.censusDir = new File(plugin.getDataFolder(), "census");
        this.censusDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::conductCensus, 0, 20 * 60 * 60); // hourly
    }

    private void conductCensus() {
        for (Nation n : nationManager.getAll()) {
            CensusData data = new CensusData();
            data.totalPopulation = n.getCitizens().size();
            data.activePlayers = (int) n.getCitizens().stream()
                .filter(uuid -> plugin.getServer().getPlayer(uuid) != null && plugin.getServer().getPlayer(uuid).isOnline())
                .count();
            data.cities = plugin.getCityGrowthEngine().getCitiesOf(n.getId()).size();
            data.conductedAt = System.currentTimeMillis();
            lastCensus.put(n.getId(), data);
            saveCensus(n.getId(), data);
        }
    }

    public synchronized CensusData getLastCensus(String nationId) {
        return lastCensus.get(nationId);
    }

    private void loadAll() {
        File[] files = censusDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                CensusData data = new CensusData();
                data.totalPopulation = o.get("totalPopulation").getAsInt();
                data.activePlayers = o.get("activePlayers").getAsInt();
                data.cities = o.get("cities").getAsInt();
                data.conductedAt = o.get("conductedAt").getAsLong();
                lastCensus.put(nationId, data);
            } catch (Exception ignored) {}
        }
    }

    private void saveCensus(String nationId, CensusData data) {
        File f = new File(censusDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("totalPopulation", data.totalPopulation);
        o.addProperty("activePlayers", data.activePlayers);
        o.addProperty("cities", data.cities);
        o.addProperty("conductedAt", data.conductedAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive census statistics.
     */
    public synchronized Map<String, Object> getCensusStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        CensusData data = lastCensus.get(nationId);
        if (data == null) {
            stats.put("hasCensus", false);
            return stats;
        }
        
        stats.put("hasCensus", true);
        stats.put("totalPopulation", data.totalPopulation);
        stats.put("activePlayers", data.activePlayers);
        stats.put("cities", data.cities);
        stats.put("conductedAt", data.conductedAt);
        stats.put("conductedHoursAgo", (System.currentTimeMillis() - data.conductedAt) / 1000 / 60 / 60);
        
        // Activity rate
        double activityRate = data.totalPopulation > 0 ? ((double) data.activePlayers / data.totalPopulation) * 100 : 0;
        stats.put("activityRate", activityRate);
        
        // Population per city
        double popPerCity = data.cities > 0 ? (double) data.totalPopulation / data.cities : 0;
        stats.put("populationPerCity", popPerCity);
        
        // Compare with current state
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            int currentPopulation = n.getCitizens().size();
            int populationChange = currentPopulation - data.totalPopulation;
            stats.put("currentPopulation", currentPopulation);
            stats.put("populationChange", populationChange);
            stats.put("populationGrowthRate", data.totalPopulation > 0 ? ((double) populationChange / data.totalPopulation) * 100 : 0);
        }
        
        // Census rating
        String rating = "МАЛОЛЮДНАЯ";
        if (data.totalPopulation >= 100) rating = "ОГРОМНАЯ";
        else if (data.totalPopulation >= 50) rating = "БОЛЬШАЯ";
        else if (data.totalPopulation >= 30) rating = "СРЕДНЯЯ";
        else if (data.totalPopulation >= 15) rating = "РАЗВИТАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Force conduct census for nation.
     */
    public synchronized String conductCensusNow(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        
        CensusData data = new CensusData();
        data.totalPopulation = n.getCitizens().size();
        data.activePlayers = (int) n.getCitizens().stream()
            .filter(uuid -> plugin.getServer().getPlayer(uuid) != null && plugin.getServer().getPlayer(uuid).isOnline())
            .count();
        data.cities = plugin.getCityGrowthEngine().getCitiesOf(nationId).size();
        data.conductedAt = System.currentTimeMillis();
        
        lastCensus.put(nationId, data);
        saveCensus(nationId, data);
        
        return "Перепись проведена. Население: " + data.totalPopulation + ", Активных: " + data.activePlayers + ", Городов: " + data.cities;
    }
    
    /**
     * Get census history (simplified - just last census).
     */
    public synchronized CensusData getCensusHistory(String nationId) {
        return lastCensus.get(nationId);
    }
    
    /**
     * Calculate population growth rate.
     */
    public synchronized double getPopulationGrowthRate(String nationId) {
        CensusData data = lastCensus.get(nationId);
        if (data == null) return 0.0;
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 0.0;
        
        int currentPopulation = n.getCitizens().size();
        if (data.totalPopulation == 0) return currentPopulation > 0 ? 100.0 : 0.0;
        
        long hoursSince = (System.currentTimeMillis() - data.conductedAt) / 1000 / 60 / 60;
        if (hoursSince == 0) hoursSince = 1;
        
        int change = currentPopulation - data.totalPopulation;
        return ((double) change / data.totalPopulation) / hoursSince * 100; // % per hour
    }
    
    /**
     * Get global census statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCensusStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalNations = 0;
        int nationsWithCensus = 0;
        long totalPopulation = 0;
        int totalActivePlayers = 0;
        int totalCities = 0;
        Map<String, Integer> populationByNation = new HashMap<>();
        Map<String, Integer> activePlayersByNation = new HashMap<>();
        Map<String, Integer> citiesByNation = new HashMap<>();
        Map<String, Double> growthRateByNation = new HashMap<>();
        
        for (Nation n : nationManager.getAll()) {
            String nationId = n.getId();
            totalNations++;
            
            CensusData data = lastCensus.get(nationId);
            if (data != null) {
                nationsWithCensus++;
                totalPopulation += data.totalPopulation;
                totalActivePlayers += data.activePlayers;
                totalCities += data.cities;
                
                populationByNation.put(nationId, data.totalPopulation);
                activePlayersByNation.put(nationId, data.activePlayers);
                citiesByNation.put(nationId, data.cities);
                growthRateByNation.put(nationId, getPopulationGrowthRate(nationId));
            } else {
                // Use current data if no census
                int currentPop = n.getCitizens().size();
                int currentActive = (int) n.getCitizens().stream()
                    .filter(uuid -> plugin.getServer().getPlayer(uuid) != null && plugin.getServer().getPlayer(uuid).isOnline())
                    .count();
                int currentCities = plugin.getCityGrowthEngine().getCitiesOf(nationId).size();
                
                totalPopulation += currentPop;
                totalActivePlayers += currentActive;
                totalCities += currentCities;
                
                populationByNation.put(nationId, currentPop);
                activePlayersByNation.put(nationId, currentActive);
                citiesByNation.put(nationId, currentCities);
            }
        }
        
        stats.put("totalNations", totalNations);
        stats.put("nationsWithCensus", nationsWithCensus);
        stats.put("totalPopulation", totalPopulation);
        stats.put("totalActivePlayers", totalActivePlayers);
        stats.put("totalCities", totalCities);
        stats.put("populationByNation", populationByNation);
        stats.put("activePlayersByNation", activePlayersByNation);
        stats.put("citiesByNation", citiesByNation);
        stats.put("growthRateByNation", growthRateByNation);
        
        // Average statistics
        stats.put("averagePopulation", totalNations > 0 ? (double) totalPopulation / totalNations : 0);
        stats.put("averageActivePlayers", totalNations > 0 ? (double) totalActivePlayers / totalNations : 0);
        stats.put("averageCities", totalNations > 0 ? (double) totalCities / totalNations : 0);
        stats.put("averageGrowthRate", growthRateByNation.size() > 0 ?
            growthRateByNation.values().stream().mapToDouble(Double::doubleValue).sum() / growthRateByNation.size() : 0);
        
        // Activity rate
        double globalActivityRate = totalPopulation > 0 ? ((double) totalActivePlayers / totalPopulation) * 100 : 0;
        stats.put("globalActivityRate", globalActivityRate);
        
        // Top nations by population
        List<Map.Entry<String, Integer>> topByPopulation = populationByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPopulation", topByPopulation);
        
        // Top nations by growth rate
        List<Map.Entry<String, Double>> topByGrowth = growthRateByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByGrowth", topByGrowth);
        
        // Population distribution
        int huge = 0, large = 0, medium = 0, developed = 0, small = 0;
        for (Integer pop : populationByNation.values()) {
            if (pop >= 100) huge++;
            else if (pop >= 50) large++;
            else if (pop >= 30) medium++;
            else if (pop >= 15) developed++;
            else small++;
        }
        
        Map<String, Integer> populationDistribution = new HashMap<>();
        populationDistribution.put("huge", huge);
        populationDistribution.put("large", large);
        populationDistribution.put("medium", medium);
        populationDistribution.put("developed", developed);
        populationDistribution.put("small", small);
        stats.put("populationDistribution", populationDistribution);
        
        return stats;
    }
}

