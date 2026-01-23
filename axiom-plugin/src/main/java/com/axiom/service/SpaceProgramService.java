package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages space programs and achievements. */
public class SpaceProgramService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File programsDir;
    private final Map<String, SpaceProgram> nationPrograms = new HashMap<>(); // nationId -> program

    public static class SpaceProgram {
        String nationId;
        boolean hasProgram;
        double researchProgress; // 0-100%
        List<String> achievements = new ArrayList<>(); // "satellite", "moon_landing", "mars", etc.
        double prestige; // 0-100
        long startedAt;
    }

    public SpaceProgramService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.programsDir = new File(plugin.getDataFolder(), "spaceprograms");
        this.programsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processPrograms, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String startProgram(String nationId, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        SpaceProgram program = nationPrograms.computeIfAbsent(nationId, k -> new SpaceProgram());
        if (program.hasProgram) return "Программа уже существует.";
        program.nationId = nationId;
        program.hasProgram = true;
        program.researchProgress = 0.0;
        program.prestige = 0.0;
        program.startedAt = System.currentTimeMillis();
        n.setTreasury(n.getTreasury() - cost);
        if (n.getHistory() != null) {
            n.getHistory().add("Космическая программа начата");
        }
        try {
            nationManager.save(n);
            saveProgram(nationId, program);
        } catch (Exception ignored) {}
        return "Космическая программа начата.";
    }

    private synchronized void processPrograms() {
        if (nationManager == null || plugin.getEducationService() == null) return;
        for (Map.Entry<String, SpaceProgram> e : nationPrograms.entrySet()) {
            SpaceProgram program = e.getValue();
            if (program == null) continue;
            if (!program.hasProgram) continue;
            // Research progresses based on education and funding
            double education = plugin.getEducationService().getEducationLevel(e.getKey());
            double progress = education * 0.05; // 5% of education level per 10 minutes
            if (!Double.isFinite(progress)) continue;
            program.researchProgress = Math.min(100, program.researchProgress + progress);
            // Achievements unlock at milestones
            if (program.researchProgress >= 20 && !program.achievements.contains("satellite")) {
                program.achievements.add("satellite");
                program.prestige += 10;
                Nation n = nationManager.getNationById(e.getKey());
                if (n != null) {
                    if (n.getHistory() != null) {
                        n.getHistory().add("Достижение: Спутник запущен!");
                    }
                    try { nationManager.save(n); } catch (Exception ignored) {}
                }
            }
            if (program.researchProgress >= 50 && !program.achievements.contains("moon_landing")) {
                program.achievements.add("moon_landing");
                program.prestige += 25;
                Nation n = nationManager.getNationById(e.getKey());
                if (n != null) {
                    if (n.getHistory() != null) {
                        n.getHistory().add("Достижение: Высадка на Луну!");
                    }
                    try { nationManager.save(n); } catch (Exception ignored) {}
                }
            }
            if (program.researchProgress >= 100 && !program.achievements.contains("mars")) {
                program.achievements.add("mars");
                program.prestige += 50;
                Nation n = nationManager.getNationById(e.getKey());
                if (n != null) {
                    if (n.getHistory() != null) {
                        n.getHistory().add("Достижение: Миссия на Марс!");
                    }
                    if (plugin.getHappinessService() != null) {
                        plugin.getHappinessService().modifyHappiness(e.getKey(), 20.0);
                    }
                    try { nationManager.save(n); } catch (Exception ignored) {}
                }
            }
            saveProgram(e.getKey(), program);
        }
    }

    private void loadAll() {
        File[] files = programsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                SpaceProgram program = new SpaceProgram();
                program.nationId = nationId;
                program.hasProgram = o.has("hasProgram") && o.get("hasProgram").getAsBoolean();
                program.researchProgress = o.has("researchProgress") ? o.get("researchProgress").getAsDouble() : 0.0;
                program.prestige = o.has("prestige") ? o.get("prestige").getAsDouble() : 0.0;
                program.startedAt = o.has("startedAt") ? o.get("startedAt").getAsLong() : System.currentTimeMillis();
                if (o.has("achievements")) {
                    for (var elem : o.getAsJsonArray("achievements")) {
                        program.achievements.add(elem.getAsString());
                    }
                }
                if (!isBlank(nationId)) {
                    nationPrograms.put(nationId, program);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveProgram(String nationId, SpaceProgram program) {
        if (isBlank(nationId) || program == null) return;
        File f = new File(programsDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", program.nationId);
        o.addProperty("hasProgram", program.hasProgram);
        o.addProperty("researchProgress", program.researchProgress);
        o.addProperty("prestige", program.prestige);
        o.addProperty("startedAt", program.startedAt);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String achievement : program.achievements) {
            arr.add(achievement);
        }
        o.add("achievements", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive space program statistics.
     */
    public synchronized Map<String, Object> getSpaceProgramStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        SpaceProgram program = nationPrograms.get(nationId);
        if (program == null) {
            program = new SpaceProgram();
            program.hasProgram = false;
        }
        
        stats.put("hasProgram", program.hasProgram);
        stats.put("researchProgress", program.researchProgress);
        stats.put("prestige", program.prestige);
        stats.put("achievements", new ArrayList<>(program.achievements));
        stats.put("achievementCount", program.achievements.size());
        stats.put("startedAt", program.startedAt);
        stats.put("programAge", program.hasProgram ? (System.currentTimeMillis() - program.startedAt) / 1000 / 60 / 60 / 24 : 0); // days
        
        // Program rating
        String rating = "ОТСУТСТВУЕТ";
        if (program.achievements.contains("mars")) rating = "МЕЖПЛАНЕТНЫЙ";
        else if (program.achievements.contains("moon_landing")) rating = "ЛУННЫЙ";
        else if (program.achievements.contains("satellite")) rating = "ОРБИТАЛЬНЫЙ";
        else if (program.hasProgram && program.researchProgress >= 50) rating = "РАЗВИТЫЙ";
        else if (program.hasProgram && program.researchProgress >= 20) rating = "НАЧАЛЬНЫЙ";
        else if (program.hasProgram) rating = "НАЧИНАЮЩИЙ";
        stats.put("rating", rating);
        
        // Prestige bonus
        double prestigeBonus = program.prestige * 0.01; // +0.01% per prestige point
        stats.put("prestigeBonus", 1.0 + prestigeBonus);
        
        // Next milestone
        String nextMilestone = null;
        if (!program.achievements.contains("satellite") && program.researchProgress < 20) {
            nextMilestone = "satellite (20%)";
        } else if (!program.achievements.contains("moon_landing") && program.researchProgress < 50) {
            nextMilestone = "moon_landing (50%)";
        } else if (!program.achievements.contains("mars") && program.researchProgress < 100) {
            nextMilestone = "mars (100%)";
        }
        stats.put("nextMilestone", nextMilestone);
        
        return stats;
    }
    
    /**
     * Accelerate research progress (through funding).
     */
    public synchronized String accelerateResearch(String nationId, double cost) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (!Double.isFinite(cost) || cost <= 0) return "Некорректная стоимость.";
        SpaceProgram program = nationPrograms.get(nationId);
        if (program == null || !program.hasProgram) return "Программа не найдена.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        // +10% progress per 10000 funding
        double progressBoost = (cost / 10000.0) * 10.0;
        program.researchProgress = Math.min(100, program.researchProgress + progressBoost);
        
        n.setTreasury(n.getTreasury() - cost);
        
        nationManager.save(n);
        saveProgram(nationId, program);
        
        return "Прогресс ускорен. Текущий прогресс: " + String.format("%.1f", program.researchProgress) + "%";
    }
    
    /**
     * Get all space program achievements.
     */
    public synchronized List<String> getAchievements(String nationId) {
        if (isBlank(nationId)) return Collections.emptyList();
        SpaceProgram program = nationPrograms.get(nationId);
        if (program == null) return Collections.emptyList();
        return new ArrayList<>(program.achievements);
    }
    
    /**
     * Calculate total prestige from space program.
     */
    public synchronized double getTotalPrestige(String nationId) {
        if (isBlank(nationId)) return 0.0;
        SpaceProgram program = nationPrograms.get(nationId);
        return program != null ? program.prestige : 0.0;
    }
    
    /**
     * Check if nation has specific achievement.
     */
    public synchronized boolean hasAchievement(String nationId, String achievementId) {
        if (isBlank(nationId) || isBlank(achievementId)) return false;
        SpaceProgram program = nationPrograms.get(nationId);
        return program != null && program.achievements.contains(achievementId);
    }
    
    /**
     * Get global space program statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalSpaceProgramStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPrograms = 0;
        double totalProgress = 0.0;
        double totalPrestige = 0.0;
        int nationsWithPrograms = 0;
        Map<String, Integer> achievementsCount = new HashMap<>();
        Map<String, Double> prestigeByNation = new HashMap<>();
        
        for (Map.Entry<String, SpaceProgram> entry : nationPrograms.entrySet()) {
            SpaceProgram program = entry.getValue();
            
            if (program != null && program.hasProgram) {
                totalPrograms++;
                nationsWithPrograms++;
                totalProgress += program.researchProgress;
                totalPrestige += program.prestige;
                prestigeByNation.put(entry.getKey(), program.prestige);
                
                for (String achievement : program.achievements) {
                    achievementsCount.put(achievement, achievementsCount.getOrDefault(achievement, 0) + 1);
                }
            }
        }
        
        stats.put("totalPrograms", totalPrograms);
        stats.put("nationsWithPrograms", nationsWithPrograms);
        stats.put("averageProgress", totalPrograms > 0 ? totalProgress / totalPrograms : 0);
        stats.put("averagePrestige", totalPrograms > 0 ? totalPrestige / totalPrograms : 0);
        stats.put("totalPrestige", totalPrestige);
        stats.put("achievementsCount", achievementsCount);
        
        // Top nations by prestige
        List<Map.Entry<String, Double>> topByPrestige = prestigeByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPrestige", topByPrestige);
        
        // Achievement milestones
        int satelliteCount = achievementsCount.getOrDefault("satellite", 0);
        int moonLandingCount = achievementsCount.getOrDefault("moon_landing", 0);
        int marsCount = achievementsCount.getOrDefault("mars", 0);
        stats.put("satelliteAchievements", satelliteCount);
        stats.put("moonLandingAchievements", moonLandingCount);
        stats.put("marsAchievements", marsCount);
        
        // Programs by stage
        int orbital = 0, lunar = 0, interplanetary = 0, developing = 0, starting = 0;
        for (SpaceProgram program : nationPrograms.values()) {
            if (program != null && program.hasProgram) {
                if (program.achievements.contains("mars")) interplanetary++;
                else if (program.achievements.contains("moon_landing")) lunar++;
                else if (program.achievements.contains("satellite")) orbital++;
                else if (program.researchProgress >= 50) developing++;
                else starting++;
            }
        }
        
        Map<String, Integer> programsByStage = new HashMap<>();
        programsByStage.put("interplanetary", interplanetary);
        programsByStage.put("lunar", lunar);
        programsByStage.put("orbital", orbital);
        programsByStage.put("developing", developing);
        programsByStage.put("starting", starting);
        stats.put("programsByStage", programsByStage);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

