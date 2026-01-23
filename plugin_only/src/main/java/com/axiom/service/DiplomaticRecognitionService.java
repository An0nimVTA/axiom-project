package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages diplomatic recognition between nations. */
public class DiplomaticRecognitionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File recognitionDir;
    private final Map<String, Set<String>> recognizedNations = new HashMap<>(); // recognizerId -> set of recognized IDs

    public DiplomaticRecognitionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.recognitionDir = new File(plugin.getDataFolder(), "recognition");
        this.recognitionDir.mkdirs();
        loadAll();
    }

    public synchronized String recognizeNation(String recognizerId, String targetId) {
        Nation recognizer = nationManager.getNationById(recognizerId);
        if (recognizer == null) return "Нация не найдена.";
        recognizedNations.computeIfAbsent(recognizerId, k -> new HashSet<>()).add(targetId);
        Nation target = nationManager.getNationById(targetId);
        if (target != null) {
            target.getHistory().add("Признание от " + recognizer.getName());
            try {
                plugin.getDiplomacySystem().setReputation(target, recognizer, 10);
            } catch (IOException e) {
                plugin.getLogger().warning("Ошибка установки репутации при признании: " + e.getMessage());
            }
            try {
                nationManager.save(target);
            } catch (IOException e) {
                plugin.getLogger().warning("Ошибка сохранения нации при признании: " + e.getMessage());
            }
            try {
                saveRecognition(recognizerId);
            } catch (IOException e) {
                plugin.getLogger().warning("Ошибка сохранения признания: " + e.getMessage());
            }
        }
        return "Нация признана.";
    }

    public synchronized boolean isRecognized(String recognizerId, String targetId) {
        Set<String> recognized = recognizedNations.get(recognizerId);
        return recognized != null && recognized.contains(targetId);
    }

    public synchronized String revokeRecognition(String recognizerId, String targetId) {
        Set<String> recognized = recognizedNations.get(recognizerId);
        if (recognized == null || !recognized.remove(targetId)) return "Признания не было.";
        Nation target = nationManager.getNationById(targetId);
        Nation recognizer = nationManager.getNationById(recognizerId);
        if (target != null && recognizer != null) {
            try {
                plugin.getDiplomacySystem().setReputation(target, recognizer, -20);
            } catch (IOException e) {
                plugin.getLogger().warning("Ошибка установки репутации при отзыве признания: " + e.getMessage());
            }
            try {
                nationManager.save(target);
            } catch (IOException e) {
                plugin.getLogger().warning("Ошибка сохранения нации при отзыве признания: " + e.getMessage());
            }
        }
        try {
            saveRecognition(recognizerId);
        } catch (IOException e) {
            plugin.getLogger().warning("Ошибка сохранения отзыва признания: " + e.getMessage());
        }
        return "Признание отозвано.";
    }

    private void loadAll() {
        File[] files = recognitionDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                Set<String> recognized = new HashSet<>();
                if (o.has("recognized")) {
                    for (var elem : o.getAsJsonArray("recognized")) {
                        recognized.add(elem.getAsString());
                    }
                }
                recognizedNations.put(nationId, recognized);
            } catch (Exception ignored) {}
        }
    }

    private void saveRecognition(String nationId) throws IOException {
        File f = new File(recognitionDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        Set<String> recognized = recognizedNations.get(nationId);
        if (recognized != null) {
            for (String targetId : recognized) {
                arr.add(targetId);
            }
        }
        o.add("recognized", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (IOException e) {
            throw e; // Re-throw IOException
        } catch (Exception e) {
            throw new IOException("Failed to save recognition: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get comprehensive recognition statistics.
     */
    public synchronized Map<String, Object> getRecognitionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> recognized = recognizedNations.get(nationId);
        if (recognized == null) recognized = Collections.emptySet();
        
        stats.put("recognizedCount", recognized.size());
        stats.put("recognizedNations", new ArrayList<>(recognized));
        
        // Count nations that recognize this nation
        int recognizedBy = 0;
        List<String> recognizedByList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : recognizedNations.entrySet()) {
            if (entry.getValue().contains(nationId)) {
                recognizedBy++;
                recognizedByList.add(entry.getKey());
            }
        }
        stats.put("recognizedBy", recognizedBy);
        stats.put("recognizedByList", recognizedByList);
        
        // Recognition rating
        String rating = "НЕПРИЗНАННАЯ";
        if (recognizedBy >= 10) rating = "ШИРОКО ПРИЗНАННАЯ";
        else if (recognizedBy >= 5) rating = "ХОРОШО ПРИЗНАННАЯ";
        else if (recognizedBy >= 3) rating = "ПРИЗНАННАЯ";
        else if (recognizedBy >= 1) rating = "ЧАСТИЧНО ПРИЗНАННАЯ";
        stats.put("rating", rating);
        
        // Diplomatic bonus from recognition
        double diplomaticBonus = recognizedBy * 0.02; // +2% per recognition
        stats.put("diplomaticBonus", 1.0 + diplomaticBonus);
        
        return stats;
    }
    
    /**
     * Get all nations that recognize this nation.
     */
    public synchronized List<String> getRecognizingNations(String nationId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : recognizedNations.entrySet()) {
            if (entry.getValue().contains(nationId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Get all nations recognized by this nation.
     */
    public synchronized List<String> getRecognizedNations(String nationId) {
        Set<String> recognized = recognizedNations.get(nationId);
        return recognized != null ? new ArrayList<>(recognized) : Collections.emptyList();
    }
    
    /**
     * Check mutual recognition between two nations.
     */
    public synchronized boolean hasMutualRecognition(String nationA, String nationB) {
        return isRecognized(nationA, nationB) && isRecognized(nationB, nationA);
    }
    
    /**
     * Calculate recognition score (international standing).
     */
    public synchronized double calculateRecognitionScore(String nationId) {
        int recognizedBy = getRecognizingNations(nationId).size();
        int recognizedCount = getRecognizedNations(nationId).size();
        
        // Score based on both recognizing and being recognized
        return (recognizedBy * 10.0) + (recognizedCount * 5.0);
    }
    
    /**
     * Get global diplomatic recognition statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalRecognitionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalRecognitionRelations = 0;
        int uniqueRecognizers = recognizedNations.size();
        Map<String, Integer> recognitionByRecognizer = new HashMap<>();
        Map<String, Integer> recognizedByTarget = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : recognizedNations.entrySet()) {
            int count = entry.getValue().size();
            totalRecognitionRelations += count;
            recognitionByRecognizer.put(entry.getKey(), count);
            
            for (String target : entry.getValue()) {
                recognizedByTarget.put(target, recognizedByTarget.getOrDefault(target, 0) + 1);
            }
        }
        
        stats.put("totalRecognitionRelations", totalRecognitionRelations);
        stats.put("uniqueRecognizers", uniqueRecognizers);
        stats.put("uniqueRecognized", recognizedByTarget.size());
        stats.put("averageRecognitionsPerRecognizer", uniqueRecognizers > 0 ? 
            (double) totalRecognitionRelations / uniqueRecognizers : 0);
        
        // Top recognizers
        List<Map.Entry<String, Integer>> topRecognizers = recognitionByRecognizer.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topRecognizers", topRecognizers);
        
        // Most recognized nations
        List<Map.Entry<String, Integer>> mostRecognized = recognizedByTarget.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostRecognized", mostRecognized);
        
        // Mutual recognition count
        int mutualCount = 0;
        for (Map.Entry<String, Set<String>> entry : recognizedNations.entrySet()) {
            for (String target : entry.getValue()) {
                if (isRecognized(target, entry.getKey())) {
                    mutualCount++;
                }
            }
        }
        stats.put("mutualRecognitions", mutualCount / 2); // Each mutual recognition counted twice
        
        // Average recognition score
        double totalRecognitionScore = 0.0;
        for (Nation n : nationManager.getAll()) {
            totalRecognitionScore += calculateRecognitionScore(n.getId());
        }
        stats.put("averageRecognitionScore", nationManager.getAll().size() > 0 ? 
            totalRecognitionScore / nationManager.getAll().size() : 0);
        
        return stats;
    }
}

