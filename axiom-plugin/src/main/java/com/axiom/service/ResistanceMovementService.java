package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.ArrayList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Manages resistance movements in occupied or oppressed territories. */
public class ResistanceMovementService {
    private final AXIOM plugin;
    private final File resistanceDir;
    private final Map<String, ResistanceMovement> movements = new HashMap<>(); // territoryKey -> movement

    public static class ResistanceMovement {
        String territoryKey;
        String targetNationId;
        double supportLevel; // 0-100%
        long establishedAt;
        List<String> actions = new ArrayList<>();
    }

    public ResistanceMovementService(AXIOM plugin) {
        this.plugin = plugin;
        this.resistanceDir = new File(plugin.getDataFolder(), "resistance");
        this.resistanceDir.mkdirs();
        loadAll();
    }

    public synchronized void establishMovement(String territoryKey, String targetNationId) {
        if (isBlank(territoryKey) || isBlank(targetNationId)) return;
        ResistanceMovement rm = new ResistanceMovement();
        rm.territoryKey = territoryKey;
        rm.targetNationId = targetNationId;
        rm.supportLevel = 10.0;
        rm.establishedAt = System.currentTimeMillis();
        movements.put(territoryKey, rm);
        Nation n = plugin.getNationManager() != null ? plugin.getNationManager().getNationById(targetNationId) : null;
        if (n != null) {
            if (n.getHistory() != null) {
                n.getHistory().add("Создано движение сопротивления в " + territoryKey);
            }
        }
        saveMovement(rm);
    }

    public synchronized void increaseSupport(String territoryKey, double amount) {
        if (isBlank(territoryKey) || !Double.isFinite(amount) || amount <= 0) return;
        ResistanceMovement rm = movements.get(territoryKey);
        if (rm == null) return;
        rm.supportLevel = Math.min(100, rm.supportLevel + amount);
        if (rm.supportLevel >= 80) {
            triggerUprising(rm);
        }
        saveMovement(rm);
    }

    private void triggerUprising(ResistanceMovement rm) {
        if (rm == null || isBlank(rm.targetNationId)) return;
        if (plugin.getNationManager() == null) return;
        Nation target = plugin.getNationManager().getNationById(rm.targetNationId);
        if (target == null) return;
        // Sabotage effects
        if (plugin.getNationModifierService() != null) {
            plugin.getNationModifierService().addModifier(rm.targetNationId, "economy", "penalty", 0.2, 720);
        }
        if (target.getHistory() != null) {
            target.getHistory().add("Восстание! Территория " + rm.territoryKey + " восстала.");
        }
        try { plugin.getNationManager().save(target); } catch (Exception ignored) {}
    }

    private void loadAll() {
        File[] files = resistanceDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                ResistanceMovement rm = new ResistanceMovement();
                rm.territoryKey = o.has("territoryKey") ? o.get("territoryKey").getAsString() : null;
                rm.targetNationId = o.has("targetNationId") ? o.get("targetNationId").getAsString() : null;
                rm.supportLevel = o.has("supportLevel") ? o.get("supportLevel").getAsDouble() : 0.0;
                rm.establishedAt = o.has("establishedAt") ? o.get("establishedAt").getAsLong() : System.currentTimeMillis();
                if (!isBlank(rm.territoryKey)) {
                    movements.put(rm.territoryKey, rm);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveMovement(ResistanceMovement rm) {
        if (rm == null || isBlank(rm.territoryKey)) return;
        File f = new File(resistanceDir, rm.territoryKey.replace(":", "_") + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("territoryKey", rm.territoryKey);
        o.addProperty("targetNationId", rm.targetNationId);
        o.addProperty("supportLevel", rm.supportLevel);
        o.addProperty("establishedAt", rm.establishedAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive resistance movement statistics.
     */
    public synchronized Map<String, Object> getResistanceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        // Movements targeting this nation
        List<ResistanceMovement> targetingMovements = new ArrayList<>();
        // Movements supported by this nation
        List<ResistanceMovement> supportedMovements = new ArrayList<>();
        
        for (ResistanceMovement rm : movements.values()) {
            if (rm != null && Objects.equals(rm.targetNationId, nationId)) {
                targetingMovements.add(rm);
            }
            // Could add support tracking later
        }
        
        stats.put("targetingMovements", targetingMovements.size());
        stats.put("supportedMovements", supportedMovements.size());
        
        // Movement details
        List<Map<String, Object>> movementsList = new ArrayList<>();
        for (ResistanceMovement rm : targetingMovements) {
            Map<String, Object> movementData = new HashMap<>();
            movementData.put("territoryKey", rm.territoryKey);
            movementData.put("supportLevel", rm.supportLevel);
            movementData.put("establishedAt", rm.establishedAt);
            movementData.put("age", (System.currentTimeMillis() - rm.establishedAt) / 1000 / 60 / 60 / 24); // days
            movementData.put("isDangerous", rm.supportLevel >= 80);
            movementsList.add(movementData);
        }
        stats.put("movementsList", movementsList);
        
        // Average support level
        double avgSupport = targetingMovements.stream()
            .mapToDouble(rm -> rm.supportLevel)
            .average()
            .orElse(0.0);
        stats.put("averageSupportLevel", avgSupport);
        
        // Resistance rating
        String rating = "НЕТ УГРОЗ";
        if (targetingMovements.size() >= 5) rating = "КРИТИЧЕСКАЯ СИТУАЦИЯ";
        else if (targetingMovements.size() >= 3) rating = "СЕРЬЁЗНАЯ УГРОЗА";
        else if (targetingMovements.size() >= 1) rating = "УГРОЗА";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Suppress resistance movement.
     */
    public synchronized String suppressMovement(String territoryKey, String nationId, double cost) throws IOException {
        if (isBlank(territoryKey) || isBlank(nationId)) return "Неверные параметры.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        ResistanceMovement rm = movements.get(territoryKey);
        if (rm == null) return "Движение сопротивления не найдено.";
        if (!rm.targetNationId.equals(nationId)) return "Это не ваша территория.";
        
        Nation n = plugin.getNationManager() != null ? plugin.getNationManager().getNationById(nationId) : null;
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        rm.supportLevel = Math.max(0, rm.supportLevel - 20);
        n.setTreasury(n.getTreasury() - cost);
        
        if (rm.supportLevel <= 0) {
            movements.remove(territoryKey);
            File f = new File(resistanceDir, territoryKey.replace(":", "_") + ".json");
            if (f.exists()) f.delete();
        } else {
            saveMovement(rm);
        }
        
        plugin.getNationManager().save(n);
        
        return "Движение подавлено. Уровень поддержки: " + String.format("%.1f", rm.supportLevel) + "%";
    }
    
    /**
     * Get all movements targeting nation.
     */
    public synchronized List<ResistanceMovement> getTargetingMovements(String nationId) {
        if (isBlank(nationId)) return java.util.Collections.emptyList();
        return movements.values().stream()
            .filter(rm -> rm != null && Objects.equals(rm.targetNationId, nationId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate resistance threat level.
     */
    public synchronized double getThreatLevel(String nationId) {
        if (isBlank(nationId)) return 0.0;
        double totalThreat = 0.0;
        for (ResistanceMovement rm : movements.values()) {
            if (rm != null && Objects.equals(rm.targetNationId, nationId)) {
                totalThreat += rm.supportLevel / 100.0; // 0-1 per movement
            }
        }
        return Math.min(1.0, totalThreat / 5.0); // Normalized to 0-1, 5 movements = max threat
    }
    
    /**
     * Get global resistance movement statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalResistanceMovementStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalMovements", movements.size());
        
        Map<String, Integer> movementsByTarget = new HashMap<>();
        double totalSupportLevel = 0.0;
        Map<String, Double> averageSupportByNation = new HashMap<>();
        Map<String, Double> supportSumByNation = new HashMap<>();
        Map<String, Integer> supportCountByNation = new HashMap<>();
        Map<String, Integer> threatLevelByNation = new HashMap<>();
        
        for (ResistanceMovement rm : movements.values()) {
            if (rm == null || isBlank(rm.targetNationId)) continue;
            movementsByTarget.put(rm.targetNationId, movementsByTarget.getOrDefault(rm.targetNationId, 0) + 1);
            totalSupportLevel += rm.supportLevel;
            
            // Track average support per target nation
            supportSumByNation.put(rm.targetNationId,
                supportSumByNation.getOrDefault(rm.targetNationId, 0.0) + rm.supportLevel);
            supportCountByNation.put(rm.targetNationId,
                supportCountByNation.getOrDefault(rm.targetNationId, 0) + 1);
        }
        for (Map.Entry<String, Double> entry : supportSumByNation.entrySet()) {
            String nationId = entry.getKey();
            int count = supportCountByNation.getOrDefault(nationId, 0);
            averageSupportByNation.put(nationId, count > 0 ? entry.getValue() / count : 0.0);
        }
        
        stats.put("movementsByTarget", movementsByTarget);
        stats.put("averageSupportLevel", movements.size() > 0 ? totalSupportLevel / movements.size() : 0);
        stats.put("nationsTargeted", movementsByTarget.size());
        
        // Top nations by movements targeting them
        List<Map.Entry<String, Integer>> topByMovements = movementsByTarget.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMovements", topByMovements);
        
        // Top nations by average support level (most threatened)
        List<Map.Entry<String, Double>> topBySupport = averageSupportByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySupport", topBySupport);
        
        // Threat level distribution
        for (String nationId : movementsByTarget.keySet()) {
            double threat = getThreatLevel(nationId);
            if (threat >= 0.8) {
                threatLevelByNation.put("critical", threatLevelByNation.getOrDefault("critical", 0) + 1);
            } else if (threat >= 0.5) {
                threatLevelByNation.put("high", threatLevelByNation.getOrDefault("high", 0) + 1);
            } else if (threat >= 0.2) {
                threatLevelByNation.put("moderate", threatLevelByNation.getOrDefault("moderate", 0) + 1);
            } else {
                threatLevelByNation.put("low", threatLevelByNation.getOrDefault("low", 0) + 1);
            }
        }
        stats.put("threatLevelDistribution", threatLevelByNation);
        
        // Average movements per target
        stats.put("averageMovementsPerTarget", movementsByTarget.size() > 0 ? 
            (double) movements.size() / movementsByTarget.size() : 0);
        
        // Average movement age
        long now = System.currentTimeMillis();
        double totalAge = 0.0;
        for (ResistanceMovement rm : movements.values()) {
            if (rm != null) {
                totalAge += (now - rm.establishedAt) / 1000.0 / 60.0 / 60.0 / 24.0; // days
            }
        }
        stats.put("averageMovementAgeDays", movements.size() > 0 ? totalAge / movements.size() : 0);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

