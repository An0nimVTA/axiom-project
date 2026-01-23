package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages economic sanctions between nations. */
public class SanctionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File sanctionsDir;
    private final Map<String, Set<String>> sanctionedNations = new HashMap<>(); // sanctionerId -> set of sanctioned IDs

    public SanctionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.sanctionsDir = new File(plugin.getDataFolder(), "sanctions");
        this.sanctionsDir.mkdirs();
        loadAll();
    }

    public synchronized String imposeSanctions(String sanctionerId, String targetId, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(sanctionerId) || isBlank(targetId)) return "Неверные параметры.";
        if (sanctionerId.equals(targetId)) return "Нельзя наложить санкции на свою нацию.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        Nation sanctioner = nationManager.getNationById(sanctionerId);
        if (sanctioner == null) return "Нация не найдена.";
        if (sanctioner.getTreasury() < cost) return "Недостаточно средств.";
        Set<String> sanctionedSet = sanctionedNations.computeIfAbsent(sanctionerId, k -> new HashSet<>());
        if (sanctionedSet.contains(targetId)) return "Санкции уже наложены.";
        sanctionedSet.add(targetId);
        sanctioner.setTreasury(sanctioner.getTreasury() - cost);
        Nation target = nationManager.getNationById(targetId);
        if (target != null) {
            if (target.getHistory() != null) {
                target.getHistory().add("Экономические санкции от " + sanctioner.getName());
            }
        }
        try {
            nationManager.save(sanctioner);
            if (target != null) nationManager.save(target);
            saveSanctions(sanctionerId);
        } catch (Exception ignored) {}
        return "Санкции наложены.";
    }

    public synchronized boolean isSanctioned(String sanctionerId, String targetId) {
        if (isBlank(sanctionerId) || isBlank(targetId)) return false;
        Set<String> sanctioned = sanctionedNations.get(sanctionerId);
        return sanctioned != null && sanctioned.contains(targetId);
    }

    public synchronized String liftSanctions(String sanctionerId, String targetId) {
        if (isBlank(sanctionerId) || isBlank(targetId)) return "Неверные параметры.";
        Set<String> sanctioned = sanctionedNations.get(sanctionerId);
        if (sanctioned == null || !sanctioned.remove(targetId)) return "Санкций нет.";
        saveSanctions(sanctionerId);
        return "Санкции сняты.";
    }

    private void loadAll() {
        File[] files = sanctionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                Set<String> sanctioned = new HashSet<>();
                if (o.has("sanctioned")) {
                    for (var elem : o.getAsJsonArray("sanctioned")) {
                        sanctioned.add(elem.getAsString());
                    }
                }
                if (!isBlank(nationId)) {
                    sanctionedNations.put(nationId, sanctioned);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveSanctions(String nationId) {
        if (isBlank(nationId)) return;
        File f = new File(sanctionsDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        Set<String> sanctioned = sanctionedNations.get(nationId);
        if (sanctioned != null) {
            for (String targetId : sanctioned) {
                arr.add(targetId);
            }
        }
        o.add("sanctioned", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive sanction statistics.
     */
    public synchronized Map<String, Object> getSanctionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        Set<String> imposed = sanctionedNations.get(nationId);
        if (imposed == null) imposed = Collections.emptySet();
        
        stats.put("imposedSanctions", imposed.size());
        stats.put("sanctionedNations", new ArrayList<>(imposed));
        
        // Count sanctions targeting this nation
        int targetedBy = 0;
        List<String> targetedByList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : sanctionedNations.entrySet()) {
            Set<String> targets = entry.getValue();
            if (targets != null && targets.contains(nationId)) {
                targetedBy++;
                targetedByList.add(entry.getKey());
            }
        }
        stats.put("targetedBy", targetedBy);
        stats.put("targetedByList", targetedByList);
        
        // Calculate economic impact
        double economicImpact = -(targetedBy * 8.0); // -8% per sanction
        stats.put("economicImpact", economicImpact);
        
        // Sanction rating
        String rating = "ОТСУТСТВУЮТ";
        if (targetedBy >= 5) rating = "КРИТИЧЕСКИЕ";
        else if (targetedBy >= 3) rating = "СИЛЬНЫЕ";
        else if (targetedBy >= 1) rating = "УМЕРЕННЫЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get all sanctioned nations by this nation.
     */
    public synchronized List<String> getSanctionedNations(String nationId) {
        if (isBlank(nationId)) return Collections.emptyList();
        Set<String> sanctioned = sanctionedNations.get(nationId);
        return sanctioned != null ? new ArrayList<>(sanctioned) : Collections.emptyList();
    }
    
    /**
     * Get all nations that have sanctioned this nation.
     */
    public synchronized List<String> getSanctioningNations(String nationId) {
        if (isBlank(nationId)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : sanctionedNations.entrySet()) {
            Set<String> targets = entry.getValue();
            if (targets != null && targets.contains(nationId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Check if two nations have mutual sanctions.
     */
    public synchronized boolean hasMutualSanctions(String nationA, String nationB) {
        if (isBlank(nationA) || isBlank(nationB)) return false;
        return isSanctioned(nationA, nationB) && isSanctioned(nationB, nationA);
    }
    
    /**
     * Get global sanction statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalSanctionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalSanctions = 0;
        int uniqueSanctioners = sanctionedNations.size();
        Map<String, Integer> sanctionsBySanctioner = new HashMap<>();
        Map<String, Integer> sanctionedByTarget = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : sanctionedNations.entrySet()) {
            Set<String> targets = entry.getValue();
            if (targets == null) continue;
            int count = targets.size();
            totalSanctions += count;
            sanctionsBySanctioner.put(entry.getKey(), count);
            
            for (String target : targets) {
                if (target != null) {
                    sanctionedByTarget.put(target, sanctionedByTarget.getOrDefault(target, 0) + 1);
                }
            }
        }
        
        stats.put("totalSanctions", totalSanctions);
        stats.put("uniqueSanctioners", uniqueSanctioners);
        stats.put("uniqueSanctioned", sanctionedByTarget.size());
        stats.put("averageSanctionsPerSanctioner", uniqueSanctioners > 0 ? 
            (double) totalSanctions / uniqueSanctioners : 0);
        
        // Top sanctioners
        List<Map.Entry<String, Integer>> topSanctioners = sanctionsBySanctioner.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topSanctioners", topSanctioners);
        
        // Most sanctioned nations
        List<Map.Entry<String, Integer>> mostSanctioned = sanctionedByTarget.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostSanctioned", mostSanctioned);
        
        // Mutual sanctions count
        int mutualCount = 0;
        for (Map.Entry<String, Set<String>> entry : sanctionedNations.entrySet()) {
            Set<String> targets = entry.getValue();
            if (targets == null) continue;
            for (String target : targets) {
                if (target != null && isSanctioned(target, entry.getKey())) {
                    mutualCount++;
                }
            }
        }
        stats.put("mutualSanctions", mutualCount / 2); // Each mutual sanction counted twice
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

