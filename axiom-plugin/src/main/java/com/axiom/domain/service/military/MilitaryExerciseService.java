package com.axiom.domain.service.military;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages military exercises and training. */
public class MilitaryExerciseService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File exercisesDir;
    private final Map<String, MilitaryExercise> activeExercises = new HashMap<>(); // nationId -> exercise

    public static class MilitaryExercise {
        String nationId;
        String type; // "land", "naval", "air", "joint"
        double effectiveness; // 0-100
        long startTime;
        long endTime;
        double cost;
    }

    public MilitaryExerciseService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.exercisesDir = new File(plugin.getDataFolder(), "exercises");
        this.exercisesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processExercises, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String startExercise(String nationId, String type, int durationHours, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (nationId == null || nationId.trim().isEmpty()) return "Нация не найдена.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        if (durationHours <= 0) return "Некорректная длительность.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        if (activeExercises.containsKey(nationId)) return "Учения уже идут.";
        MilitaryExercise ex = new MilitaryExercise();
        ex.nationId = nationId;
        ex.type = (type == null || type.trim().isEmpty()) ? "land" : type;
        ex.effectiveness = 50.0; // Base effectiveness
        ex.startTime = System.currentTimeMillis();
        ex.endTime = System.currentTimeMillis() + durationHours * 60 * 60_000L;
        ex.cost = cost;
        activeExercises.put(nationId, ex);
        n.setTreasury(n.getTreasury() - cost);
        n.getHistory().add("Начаты военные учения: " + type);
        try {
            nationManager.save(n);
            saveExercise(ex);
        } catch (Exception ignored) {}
        return "Учения начаты: " + ex.type;
    }

    private synchronized void processExercises() {
        long now = System.currentTimeMillis();
        List<String> completed = new ArrayList<>();
        for (Map.Entry<String, MilitaryExercise> e : activeExercises.entrySet()) {
            MilitaryExercise ex = e.getValue();
            if (now >= ex.endTime) {
                // Exercise completed - boost military strength
                double boost = ex.effectiveness * 0.1; // 0.1x effectiveness = strength boost
                if (plugin.getMilitaryService() != null) {
                    plugin.getMilitaryService().recruitUnits(ex.nationId, "infantry", (int) (boost * 10), 0);
                }
                Nation n = nationManager.getNationById(ex.nationId);
                if (n != null) {
                    n.getHistory().add("Военные учения завершены. Боеспособность +" + String.format("%.1f", boost));
                    try { nationManager.save(n); } catch (Exception ignored) {}
                }
                completed.add(e.getKey());
            } else {
                // Increase effectiveness over time
                ex.effectiveness = Math.min(100, ex.effectiveness + 1.0);
            }
        }
        for (String nationId : completed) {
            activeExercises.remove(nationId);
        }
    }

    private void loadAll() {
        File[] files = exercisesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                MilitaryExercise ex = new MilitaryExercise();
                ex.nationId = o.get("nationId").getAsString();
                ex.type = o.get("type").getAsString();
                ex.effectiveness = o.get("effectiveness").getAsDouble();
                ex.startTime = o.get("startTime").getAsLong();
                ex.endTime = o.get("endTime").getAsLong();
                ex.cost = o.get("cost").getAsDouble();
                if (ex.endTime > System.currentTimeMillis()) {
                    activeExercises.put(ex.nationId, ex);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveExercise(MilitaryExercise ex) {
        File f = new File(exercisesDir, ex.nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", ex.nationId);
        o.addProperty("type", ex.type);
        o.addProperty("effectiveness", ex.effectiveness);
        o.addProperty("startTime", ex.startTime);
        o.addProperty("endTime", ex.endTime);
        o.addProperty("cost", ex.cost);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive military exercise statistics for a nation.
     */
    public synchronized Map<String, Object> getMilitaryExerciseStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        MilitaryExercise exercise = activeExercises.get(nationId);
        if (exercise == null) {
            stats.put("hasActiveExercise", false);
            return stats;
        }
        
        long now = System.currentTimeMillis();
        long remainingMinutes = Math.max(0, (exercise.endTime - now) / (60 * 1000));
        
        stats.put("hasActiveExercise", true);
        stats.put("type", exercise.type);
        stats.put("effectiveness", exercise.effectiveness);
        stats.put("remainingMinutes", remainingMinutes);
        stats.put("cost", exercise.cost);
        
        // Exercise rating
        String rating = "НАЧАЛЬНЫЙ";
        if (exercise.effectiveness >= 90) rating = "ОТЛИЧНЫЙ";
        else if (exercise.effectiveness >= 70) rating = "ХОРОШИЙ";
        else if (exercise.effectiveness >= 50) rating = "СРЕДНИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global military exercise statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalMilitaryExerciseStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long now = System.currentTimeMillis();
        int totalActive = 0;
        Map<String, Integer> exercisesByNation = new HashMap<>();
        Map<String, Integer> exercisesByType = new HashMap<>();
        double totalEffectiveness = 0.0;
        double totalCost = 0.0;
        
        for (MilitaryExercise exercise : activeExercises.values()) {
            if (now < exercise.endTime) {
                totalActive++;
                exercisesByNation.put(exercise.nationId,
                    exercisesByNation.getOrDefault(exercise.nationId, 0) + 1);
                exercisesByType.put(exercise.type,
                    exercisesByType.getOrDefault(exercise.type, 0) + 1);
                totalEffectiveness += exercise.effectiveness;
                totalCost += exercise.cost;
            }
        }
        
        stats.put("totalActiveExercises", totalActive);
        stats.put("exercisesByNation", exercisesByNation);
        stats.put("exercisesByType", exercisesByType);
        stats.put("averageEffectiveness", totalActive > 0 ? totalEffectiveness / totalActive : 0);
        stats.put("totalCost", totalCost);
        stats.put("nationsWithExercises", exercisesByNation.size());
        
        // Top nations by exercises
        List<Map.Entry<String, Integer>> topByExercises = exercisesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByExercises", topByExercises);
        
        // Most common exercise types
        List<Map.Entry<String, Integer>> topByType = exercisesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByType", topByType);
        
        return stats;
    }
}

