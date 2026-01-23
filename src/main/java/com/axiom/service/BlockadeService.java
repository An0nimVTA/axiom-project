package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages naval blockades preventing trade. */
public class BlockadeService {
    private final AXIOM plugin;
    private final File blockadesDir;
    private final Map<String, Set<String>> blockadedNations = new HashMap<>(); // blockader -> blockaded

    public BlockadeService(AXIOM plugin) {
        this.plugin = plugin;
        this.blockadesDir = new File(plugin.getDataFolder(), "blockades");
        this.blockadesDir.mkdirs();
        loadAll();
    }

    public synchronized String establishBlockade(String blockaderId, String targetId) {
        Nation blockader = plugin.getNationManager().getNationById(blockaderId);
        Nation target = plugin.getNationManager().getNationById(targetId);
        if (blockader == null || target == null) return "Нация не найдена.";
        // Need naval power
        int ships = plugin.getNavalService().getNavalPower(blockaderId).ships;
        if (ships < 5) return "Недостаточно кораблей (нужно минимум 5).";
        blockadedNations.computeIfAbsent(blockaderId, k -> new HashSet<>()).add(targetId);
        saveBlockades(blockaderId);
        target.getHistory().add("Морская блокада от " + blockader.getName());
        try {
            plugin.getNationManager().save(target);
        } catch (Exception ignored) {}
        return "Блокада установлена.";
    }

    public synchronized boolean isBlockaded(String nationId) {
        for (Set<String> blockaded : blockadedNations.values()) {
            if (blockaded.contains(nationId)) return true;
        }
        return false;
    }

    public synchronized double getTradePenalty(String nationId) {
        return isBlockaded(nationId) ? 0.5 : 1.0; // 50% trade reduction
    }

    private void loadAll() {
        File[] files = blockadesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String blockaderId = f.getName().replace(".json", "");
                Set<String> blockaded = new HashSet<>();
                if (o.has("blockaded")) {
                    for (var e : o.getAsJsonArray("blockaded")) {
                        blockaded.add(e.getAsString());
                    }
                }
                blockadedNations.put(blockaderId, blockaded);
            } catch (Exception ignored) {}
        }
    }

    private void saveBlockades(String blockaderId) {
        File f = new File(blockadesDir, blockaderId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String t : blockadedNations.getOrDefault(blockaderId, Collections.emptySet())) {
            arr.add(t);
        }
        o.add("blockaded", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive blockade statistics.
     */
    public synchronized Map<String, Object> getBlockadeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> blockaded = blockadedNations.get(nationId);
        if (blockaded == null) blockaded = Collections.emptySet();
        
        stats.put("imposedBlockades", blockaded.size());
        stats.put("blockadedNations", new ArrayList<>(blockaded));
        
        // Count blockades targeting this nation
        int targetedBy = 0;
        List<String> targetedByList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : blockadedNations.entrySet()) {
            if (entry.getValue().contains(nationId)) {
                targetedBy++;
                targetedByList.add(entry.getKey());
            }
        }
        stats.put("targetedBy", targetedBy);
        stats.put("targetedByList", targetedByList);
        
        // Calculate trade impact
        double tradePenalty = getTradePenalty(nationId);
        stats.put("tradePenalty", tradePenalty);
        stats.put("tradeReduction", (1.0 - tradePenalty) * 100); // Convert to percentage
        
        // Blockade rating
        String rating = "ОТСУТСТВУЕТ";
        if (targetedBy >= 3) rating = "КРИТИЧЕСКАЯ";
        else if (targetedBy >= 2) rating = "СИЛЬНАЯ";
        else if (targetedBy >= 1) rating = "АКТИВНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Lift blockade.
     */
    public synchronized String liftBlockade(String blockaderId, String targetId) throws IOException {
        Set<String> blockaded = blockadedNations.get(blockaderId);
        if (blockaded == null || !blockaded.contains(targetId)) {
            return "Блокада не найдена.";
        }
        
        blockaded.remove(targetId);
        if (blockaded.isEmpty()) {
            blockadedNations.remove(blockaderId);
        }
        saveBlockades(blockaderId);
        
        Nation blockader = plugin.getNationManager().getNationById(blockaderId);
        Nation target = plugin.getNationManager().getNationById(targetId);
        if (blockader != null && target != null) {
            target.getHistory().add("Морская блокада от " + blockader.getName() + " снята");
            plugin.getNationManager().save(target);
        }
        
        return "Блокада снята.";
    }
    
    /**
     * Get all blockaded nations by this nation.
     */
    public synchronized List<String> getBlockadedNations(String nationId) {
        Set<String> blockaded = blockadedNations.get(nationId);
        return blockaded != null ? new ArrayList<>(blockaded) : Collections.emptyList();
    }
    
    /**
     * Get all nations blockading this nation.
     */
    public synchronized List<String> getBlockadingNations(String nationId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : blockadedNations.entrySet()) {
            if (entry.getValue().contains(nationId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Check if nation can establish blockade (has enough naval power).
     */
    public synchronized boolean canEstablishBlockade(String nationId) {
        if (plugin.getNavalService() == null) return false;
        NavalService.NavalPower power = plugin.getNavalService().getNavalPower(nationId);
        return power.ships >= 5;
    }
    
    /**
     * Get global blockade statistics.
     */
    public synchronized Map<String, Object> getGlobalBlockadeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalBlockades = 0;
        int totalBlockaded = 0;
        Map<String, Integer> byBlockader = new HashMap<>();
        Map<String, Integer> byTarget = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : blockadedNations.entrySet()) {
            int count = entry.getValue().size();
            totalBlockades += count;
            totalBlockaded += count;
            byBlockader.put(entry.getKey(), count);
            for (String target : entry.getValue()) {
                byTarget.put(target, byTarget.getOrDefault(target, 0) + 1);
            }
        }
        
        stats.put("totalBlockades", totalBlockades);
        stats.put("totalBlockaded", totalBlockaded);
        stats.put("uniqueBlockaders", byBlockader.size());
        stats.put("uniqueBlockaded", byTarget.size());
        stats.put("blockadesByBlockader", byBlockader);
        stats.put("blockadesByTarget", byTarget);
        
        // Top blockaders
        List<Map.Entry<String, Integer>> topBlockaders = byBlockader.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBlockaders", topBlockaders);
        
        // Most blockaded nations
        List<Map.Entry<String, Integer>> mostBlockaded = byTarget.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostBlockaded", mostBlockaded);
        
        return stats;
    }
}

