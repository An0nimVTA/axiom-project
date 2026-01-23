package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages colonization of unclaimed territories. */
public class ColonizationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File coloniesDir;
    private final Map<String, List<Colony>> nationColonies = new HashMap<>(); // nationId -> colonies

    public static class Colony {
        String id;
        String nationId;
        String territoryKey;
        long establishedAt;
        int population;
    }

    public ColonizationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.coloniesDir = new File(plugin.getDataFolder(), "colonies");
        this.coloniesDir.mkdirs();
        loadAll();
    }

    public synchronized String establishColony(String nationId, String territoryKey, double cost) throws IOException {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        String id = UUID.randomUUID().toString();
        Colony c = new Colony();
        c.id = id;
        c.nationId = nationId;
        c.territoryKey = territoryKey;
        c.establishedAt = System.currentTimeMillis();
        c.population = 10;
        n.setTreasury(n.getTreasury() - cost);
        nationColonies.computeIfAbsent(nationId, k -> new ArrayList<>()).add(c);
        n.getHistory().add("Колония основана в " + territoryKey);
        nationManager.save(n);
        saveColony(c);
        return "Колония основана: " + territoryKey;
    }

    public synchronized List<Colony> getColonies(String nationId) {
        return nationColonies.getOrDefault(nationId, Collections.emptyList());
    }

    private void loadAll() {
        File[] files = coloniesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Colony c = new Colony();
                c.id = o.get("id").getAsString();
                c.nationId = o.get("nationId").getAsString();
                c.territoryKey = o.get("territoryKey").getAsString();
                c.establishedAt = o.get("establishedAt").getAsLong();
                c.population = o.has("population") ? o.get("population").getAsInt() : 10;
                nationColonies.computeIfAbsent(c.nationId, k -> new ArrayList<>()).add(c);
            } catch (Exception ignored) {}
        }
    }

    private void saveColony(Colony c) throws IOException {
        File f = new File(coloniesDir, c.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", c.id);
        o.addProperty("nationId", c.nationId);
        o.addProperty("territoryKey", c.territoryKey);
        o.addProperty("establishedAt", c.establishedAt);
        o.addProperty("population", c.population);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive colonization statistics.
     */
    public synchronized Map<String, Object> getColonizationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Colony> colonies = nationColonies.getOrDefault(nationId, Collections.emptyList());
        
        stats.put("totalColonies", colonies.size());
        stats.put("totalColonyPopulation", colonies.stream().mapToInt(c -> c.population).sum());
        
        // Colony details
        List<Map<String, Object>> coloniesList = new ArrayList<>();
        for (Colony c : colonies) {
            Map<String, Object> colonyData = new HashMap<>();
            colonyData.put("id", c.id);
            colonyData.put("territoryKey", c.territoryKey);
            colonyData.put("population", c.population);
            colonyData.put("establishedAt", c.establishedAt);
            colonyData.put("age", (System.currentTimeMillis() - c.establishedAt) / 1000 / 60 / 60 / 24); // days
            coloniesList.add(colonyData);
        }
        stats.put("coloniesList", coloniesList);
        
        // Average colony age
        double avgAge = colonies.stream()
            .mapToDouble(c -> (System.currentTimeMillis() - c.establishedAt) / 1000.0 / 60 / 60 / 24)
            .average()
            .orElse(0.0);
        stats.put("averageColonyAge", avgAge);
        
        // Colonization rating
        String rating = "НЕТ КОЛОНИЙ";
        if (colonies.size() >= 20) rating = "КОЛОНИАЛЬНАЯ ИМПЕРИЯ";
        else if (colonies.size() >= 10) rating = "ОБШИРНАЯ КОЛОНИЗАЦИЯ";
        else if (colonies.size() >= 5) rating = "АКТИВНАЯ КОЛОНИЗАЦИЯ";
        else if (colonies.size() >= 3) rating = "РАЗВИТАЯ";
        else if (colonies.size() >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Abandon colony.
     */
    public synchronized String abandonColony(String colonyId, String nationId) throws IOException {
        List<Colony> colonies = nationColonies.get(nationId);
        if (colonies == null) return "Колония не найдена.";
        
        Colony c = colonies.stream()
            .filter(col -> col.id.equals(colonyId))
            .findFirst()
            .orElse(null);
        
        if (c == null) return "Колония не найдена.";
        if (!c.nationId.equals(nationId)) return "Это не ваша колония.";
        
        colonies.remove(c);
        File f = new File(coloniesDir, colonyId + ".json");
        if (f.exists()) f.delete();
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Колония покинута: " + c.territoryKey);
            nationManager.save(n);
        }
        
        return "Колония покинута.";
    }
    
    /**
     * Upgrade colony (increase population).
     */
    public synchronized String upgradeColony(String colonyId, String nationId, double cost) throws IOException {
        List<Colony> colonies = nationColonies.get(nationId);
        if (colonies == null) return "Колония не найдена.";
        
        Colony c = colonies.stream()
            .filter(col -> col.id.equals(colonyId))
            .findFirst()
            .orElse(null);
        
        if (c == null) return "Колония не найдена.";
        if (!c.nationId.equals(nationId)) return "Это не ваша колония.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        c.population += 20;
        n.setTreasury(n.getTreasury() - cost);
        nationManager.save(n);
        saveColony(c);
        
        return "Колония улучшена. Население: " + c.population;
    }
    
    /**
     * Calculate colonization bonus.
     */
    public synchronized double getColonizationBonus(String nationId) {
        int colonyCount = getColonies(nationId).size();
        // +1% economy per 2 colonies (capped at +25%)
        return 1.0 + Math.min(0.25, (colonyCount / 2.0) * 0.01);
    }
    
    /**
     * Get global colonization statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalColonizationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalColonies = 0;
        int totalColonyPopulation = 0;
        Map<String, Integer> coloniesByNation = new HashMap<>();
        Map<String, Integer> populationByNation = new HashMap<>();
        Map<String, Double> bonusByNation = new HashMap<>();
        
        for (Nation n : nationManager.getAll()) {
            String nationId = n.getId();
            List<Colony> colonies = getColonies(nationId);
            int count = colonies.size();
            int pop = colonies.stream().mapToInt(c -> c.population).sum();
            double bonus = getColonizationBonus(nationId);
            
            totalColonies += count;
            totalColonyPopulation += pop;
            coloniesByNation.put(nationId, count);
            populationByNation.put(nationId, pop);
            bonusByNation.put(nationId, bonus);
        }
        
        stats.put("totalColonies", totalColonies);
        stats.put("totalColonyPopulation", totalColonyPopulation);
        stats.put("coloniesByNation", coloniesByNation);
        stats.put("populationByNation", populationByNation);
        stats.put("bonusByNation", bonusByNation);
        stats.put("nationsWithColonies", coloniesByNation.values().stream().mapToInt(i -> i > 0 ? 1 : 0).sum());
        
        // Average colonies per nation
        stats.put("averageColoniesPerNation", coloniesByNation.size() > 0 ? 
            (double) totalColonies / coloniesByNation.size() : 0);
        
        // Average colony population
        stats.put("averageColonyPopulation", totalColonies > 0 ? 
            (double) totalColonyPopulation / totalColonies : 0);
        
        // Top nations by colonies
        List<Map.Entry<String, Integer>> topByColonies = coloniesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByColonies", topByColonies);
        
        // Top nations by colony population
        List<Map.Entry<String, Integer>> topByPopulation = populationByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPopulation", topByPopulation);
        
        // Average colony age
        long now = System.currentTimeMillis();
        long totalAge = 0;
        int coloniesWithAge = 0;
        for (List<Colony> colonies : nationColonies.values()) {
            for (Colony c : colonies) {
                totalAge += (now - c.establishedAt) / 1000 / 60 / 60 / 24; // days
                coloniesWithAge++;
            }
        }
        stats.put("averageColonyAgeDays", coloniesWithAge > 0 ? (double) totalAge / coloniesWithAge : 0);
        
        // Colonization rating distribution
        int empire = 0, extensive = 0, active = 0, developed = 0, initial = 0, none = 0;
        for (Integer count : coloniesByNation.values()) {
            if (count >= 20) empire++;
            else if (count >= 10) extensive++;
            else if (count >= 5) active++;
            else if (count >= 3) developed++;
            else if (count >= 1) initial++;
            else none++;
        }
        
        Map<String, Integer> ratingDistribution = new HashMap<>();
        ratingDistribution.put("empire", empire);
        ratingDistribution.put("extensive", extensive);
        ratingDistribution.put("active", active);
        ratingDistribution.put("developed", developed);
        ratingDistribution.put("initial", initial);
        ratingDistribution.put("none", none);
        stats.put("ratingDistribution", ratingDistribution);
        
        return stats;
    }
}

