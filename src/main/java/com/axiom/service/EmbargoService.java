package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages trade embargoes between nations. */
public class EmbargoService {
    private final AXIOM plugin;
    private final File embargoesDir;
    private final Map<String, Set<String>> embargoes = new HashMap<>(); // embargor -> embargoed nations

    public EmbargoService(AXIOM plugin) {
        this.plugin = plugin;
        this.embargoesDir = new File(plugin.getDataFolder(), "embargoes");
        this.embargoesDir.mkdirs();
        loadAll();
    }

    public synchronized String imposeEmbargo(String embargorId, String targetId) throws IOException {
        Nation e = plugin.getNationManager().getNationById(embargorId);
        Nation t = plugin.getNationManager().getNationById(targetId);
        if (e == null || t == null) return "Нация не найдена.";
        embargoes.computeIfAbsent(embargorId, k -> new HashSet<>()).add(targetId);
        saveEmbargoes(embargorId);
        e.getHistory().add("Введено эмбарго против " + t.getName());
        t.getHistory().add("Эмбарго от " + e.getName());
        plugin.getNationManager().save(e);
        plugin.getNationManager().save(t);
        return "Эмбарго введено.";
    }

    public synchronized boolean hasEmbargo(String nationA, String nationB) {
        return embargoes.getOrDefault(nationA, Collections.emptySet()).contains(nationB) ||
               embargoes.getOrDefault(nationB, Collections.emptySet()).contains(nationA);
    }

    private void loadAll() {
        File[] files = embargoesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String embargorId = f.getName().replace(".json", "");
                Set<String> targets = new HashSet<>();
                if (o.has("targets")) {
                    JsonArray arr = o.getAsJsonArray("targets");
                    for (var e : arr) targets.add(e.getAsString());
                }
                embargoes.put(embargorId, targets);
            } catch (Exception ignored) {}
        }
    }

    private void saveEmbargoes(String embargorId) throws IOException {
        File f = new File(embargoesDir, embargorId + ".json");
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String t : embargoes.getOrDefault(embargorId, Collections.emptySet())) arr.add(t);
        o.add("targets", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive embargo statistics.
     */
    public synchronized Map<String, Object> getEmbargoStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> imposed = embargoes.get(nationId);
        if (imposed == null) imposed = Collections.emptySet();
        
        stats.put("imposedEmbargoes", imposed.size());
        stats.put("imposedOn", new ArrayList<>(imposed));
        
        // Count embargoes targeting this nation
        int targetedBy = 0;
        List<String> targetedByList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : embargoes.entrySet()) {
            if (entry.getValue().contains(nationId)) {
                targetedBy++;
                targetedByList.add(entry.getKey());
            }
        }
        stats.put("targetedBy", targetedBy);
        stats.put("targetedByList", targetedByList);
        
        // Calculate economic impact
        double economicImpact = -(targetedBy * 5.0); // -5% per embargo
        stats.put("economicImpact", economicImpact);
        
        return stats;
    }
    
    /**
     * Lift embargo.
     */
    public synchronized String liftEmbargo(String embargorId, String targetId) throws IOException {
        Set<String> targets = embargoes.get(embargorId);
        if (targets == null || !targets.contains(targetId)) {
            return "Эмбарго не найдено.";
        }
        
        targets.remove(targetId);
        if (targets.isEmpty()) {
            embargoes.remove(embargorId);
        }
        saveEmbargoes(embargorId);
        
        Nation e = plugin.getNationManager().getNationById(embargorId);
        Nation t = plugin.getNationManager().getNationById(targetId);
        if (e != null && t != null) {
            e.getHistory().add("Эмбарго против " + t.getName() + " снято");
            t.getHistory().add("Эмбарго от " + e.getName() + " снято");
            plugin.getNationManager().save(e);
            plugin.getNationManager().save(t);
        }
        
        return "Эмбарго снято.";
    }
    
    /**
     * Get all nations this nation has embargoed.
     */
    public synchronized List<String> getEmbargoedNations(String nationId) {
        Set<String> targets = embargoes.get(nationId);
        return targets != null ? new ArrayList<>(targets) : Collections.emptyList();
    }
    
    /**
     * Get all nations that have embargoed this nation.
     */
    public synchronized List<String> getEmbargoingNations(String nationId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : embargoes.entrySet()) {
            if (entry.getValue().contains(nationId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Get global embargo statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalEmbargoStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalEmbargoes = 0;
        int uniqueEmbargors = embargoes.size();
        Map<String, Integer> embargoesByEmbargor = new HashMap<>();
        Map<String, Integer> embargoedByTarget = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : embargoes.entrySet()) {
            int count = entry.getValue().size();
            totalEmbargoes += count;
            embargoesByEmbargor.put(entry.getKey(), count);
            
            for (String target : entry.getValue()) {
                embargoedByTarget.put(target, embargoedByTarget.getOrDefault(target, 0) + 1);
            }
        }
        
        stats.put("totalEmbargoes", totalEmbargoes);
        stats.put("uniqueEmbargors", uniqueEmbargors);
        stats.put("uniqueEmbargoed", embargoedByTarget.size());
        stats.put("averageEmbargoesPerEmbargor", uniqueEmbargors > 0 ? 
            (double) totalEmbargoes / uniqueEmbargors : 0);
        
        // Top embargors
        List<Map.Entry<String, Integer>> topEmbargors = embargoesByEmbargor.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topEmbargors", topEmbargors);
        
        // Most embargoed nations
        List<Map.Entry<String, Integer>> mostEmbargoed = embargoedByTarget.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostEmbargoed", mostEmbargoed);
        
        // Embargo distribution
        Map<Integer, Integer> distribution = new HashMap<>();
        for (int count : embargoesByEmbargor.values()) {
            distribution.put(count, distribution.getOrDefault(count, 0) + 1);
        }
        stats.put("embargoDistribution", distribution);
        
        return stats;
    }
}

