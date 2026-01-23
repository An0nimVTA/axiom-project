package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages vassal-overlord relationships between nations. */
public class VassalService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File vassalsDir;
    private final Map<String, String> vassalToOverlord = new HashMap<>(); // vassalId -> overlordId

    public VassalService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.vassalsDir = new File(plugin.getDataFolder(), "vassals");
        this.vassalsDir.mkdirs();
        loadAll();
    }

    public synchronized String becomeVassal(String vassalId, String overlordId) throws IOException {
        if (isBlank(vassalId) || isBlank(overlordId)) return "Некорректные данные.";
        if (vassalId.equals(overlordId)) return "Нельзя стать вассалом самому себе.";
        Nation vassal = nationManager.getNationById(vassalId);
        Nation overlord = nationManager.getNationById(overlordId);
        if (vassal == null || overlord == null) return "Нация не найдена.";
        if (vassalToOverlord.containsKey(vassalId)) return "Уже вассал другой нации.";
        vassalToOverlord.put(vassalId, overlordId);
        saveVassal(vassalId, overlordId);
        vassal.getHistory().add("Стали вассалом " + overlord.getName());
        overlord.getHistory().add(vassal.getName() + " стал вассалом");
        nationManager.save(vassal);
        nationManager.save(overlord);
        return vassal.getName() + " стал вассалом " + overlord.getName();
    }

    public synchronized String breakVassalage(String vassalId) throws IOException {
        String overlordId = vassalToOverlord.remove(vassalId);
        if (overlordId == null) return "Не является вассалом.";
        deleteVassal(vassalId);
        Nation vassal = nationManager.getNationById(vassalId);
        Nation overlord = nationManager.getNationById(overlordId);
        if (vassal != null) vassal.getHistory().add("Прекратили вассалитет");
        if (overlord != null) {
            String vassalName = vassal != null ? vassal.getName() : vassalId;
            overlord.getHistory().add(vassalName + " прекратили вассалитет");
        }
        if (vassal != null) nationManager.save(vassal);
        if (overlord != null) nationManager.save(overlord);
        return "Вассалитет расторгнут.";
    }

    public synchronized boolean isVassal(String nationId) {
        return vassalToOverlord.containsKey(nationId);
    }

    public synchronized String getOverlord(String vassalId) {
        return vassalToOverlord.get(vassalId);
    }

    private void loadAll() {
        File[] files = vassalsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String vassalId = o.get("vassalId").getAsString();
                String overlordId = o.get("overlordId").getAsString();
                vassalToOverlord.put(vassalId, overlordId);
            } catch (Exception ignored) {}
        }
    }

    private void saveVassal(String vassalId, String overlordId) throws IOException {
        File f = new File(vassalsDir, vassalId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("vassalId", vassalId);
        o.addProperty("overlordId", overlordId);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }

    private void deleteVassal(String vassalId) {
        File f = new File(vassalsDir, vassalId + ".json");
        if (f.exists()) f.delete();
    }
    
    /**
     * Get comprehensive vassal statistics.
     */
    public synchronized Map<String, Object> getVassalStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // As vassal
        String overlordId = vassalToOverlord.get(nationId);
        stats.put("isVassal", overlordId != null);
        stats.put("overlordId", overlordId);
        if (overlordId != null) {
            Nation overlord = nationManager.getNationById(overlordId);
            stats.put("overlordName", overlord != null ? overlord.getName() : overlordId);
        }
        
        // As overlord
        List<String> vassals = new ArrayList<>();
        for (Map.Entry<String, String> entry : vassalToOverlord.entrySet()) {
            if (entry.getValue().equals(nationId)) {
                vassals.add(entry.getKey());
            }
        }
        stats.put("vassals", vassals);
        stats.put("vassalCount", vassals.size());
        stats.put("isOverlord", !vassals.isEmpty());
        
        // Calculate vassal benefits
        Map<String, Double> benefits = new HashMap<>();
        if (!vassals.isEmpty()) {
            // Overlord gets tribute and military support
            double tributeBonus = vassals.size() * 0.05; // +5% tribute per vassal
            double militarySupport = vassals.size() * 0.1; // +10% military per vassal
            benefits.put("tributeBonus", 1.0 + tributeBonus);
            benefits.put("militarySupport", 1.0 + militarySupport);
        }
        stats.put("benefits", benefits);
        
        // Vassal status rating
        String status = "НЕЗАВИСИМЫЙ";
        if (overlordId != null) status = "ВАССАЛ";
        else if (!vassals.isEmpty()) status = "СЮЗЕРЕН";
        stats.put("status", status);
        
        return stats;
    }
    
    /**
     * Get all vassals of a nation.
     */
    public synchronized List<String> getVassals(String overlordId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : vassalToOverlord.entrySet()) {
            if (entry.getValue().equals(overlordId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Calculate total vassal power (combined strength of vassals).
     */
    public synchronized double getTotalVassalPower(String overlordId) {
        List<String> vassals = getVassals(overlordId);
        double totalPower = 0.0;
        
        for (String vassalId : vassals) {
            Nation vassal = nationManager.getNationById(vassalId);
            if (vassal != null) {
                if (plugin.getMilitaryService() != null) {
                    totalPower += plugin.getMilitaryService().getMilitaryStrength(vassalId);
                } else {
                    totalPower += vassal.getCitizens().size() * 10.0; // Estimate
                }
            }
        }
        
        return totalPower;
    }
    
    /**
     * Check if two nations have vassal relationship.
     */
    public synchronized boolean hasVassalRelationship(String nationA, String nationB) {
        String overlordA = vassalToOverlord.get(nationA);
        String overlordB = vassalToOverlord.get(nationB);
        return nationA.equals(overlordB) || nationB.equals(overlordA);
    }
    
    /**
     * Get global vassal statistics.
     */
    public synchronized Map<String, Object> getGlobalVassalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalVassalages", vassalToOverlord.size());
        
        // Count overlords and their vassals
        Map<String, Integer> overlordVassalCount = new HashMap<>();
        for (String overlordId : vassalToOverlord.values()) {
            overlordVassalCount.put(overlordId, overlordVassalCount.getOrDefault(overlordId, 0) + 1);
        }
        
        stats.put("totalOverlords", overlordVassalCount.size());
        stats.put("averageVassalsPerOverlord", overlordVassalCount.size() > 0 ? 
            (double) vassalToOverlord.size() / overlordVassalCount.size() : 0);
        
        int maxVassals = overlordVassalCount.values().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
        stats.put("maxVassals", maxVassals);
        
        // Top overlords
        List<Map.Entry<String, Integer>> topOverlords = overlordVassalCount.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topOverlords", topOverlords);
        
        // Calculate total vassal power
        double totalVassalPower = 0.0;
        for (String overlordId : overlordVassalCount.keySet()) {
            totalVassalPower += getTotalVassalPower(overlordId);
        }
        stats.put("totalVassalPower", totalVassalPower);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

