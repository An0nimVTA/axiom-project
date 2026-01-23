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

/** Manages education levels, research, and scientific progress per nation. */
public class EducationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File educationDir;
    private final Map<String, EducationData> educationData = new HashMap<>(); // nationId -> data

    public static class EducationData {
        double educationLevel = 0.0; // 0-100
        List<String> researchProjects = new ArrayList<>();
        double researchProgress = 0.0;
    }

    public EducationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.educationDir = new File(plugin.getDataFolder(), "education");
        this.educationDir.mkdirs();
        loadAll();
    }

    public synchronized double getEducationLevel(String nationId) {
        EducationData data = educationData.computeIfAbsent(nationId, k -> new EducationData());
        Nation n = nationManager.getNationById(nationId);
        if (n != null && n.getBudgetEducation() > 0) {
            // Education budget boosts level
            data.educationLevel = Math.min(100, data.educationLevel + n.getBudgetEducation() / 10000.0);
        }
        return Math.min(100, data.educationLevel);
    }

    public synchronized String startResearch(String nationId, String projectName) {
        EducationData data = educationData.computeIfAbsent(nationId, k -> new EducationData());
        if (data.researchProjects.contains(projectName)) return "Исследование уже идёт.";
        data.researchProjects.add(projectName);
        saveEducationData(nationId, data);
        return "Исследование начато: " + projectName;
    }

    public synchronized void addResearchProgress(String nationId, double amount) {
        EducationData data = educationData.computeIfAbsent(nationId, k -> new EducationData());
        data.researchProgress += amount;
        saveEducationData(nationId, data);
    }

    private void loadAll() {
        File[] files = educationDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                EducationData data = new EducationData();
                data.educationLevel = o.get("educationLevel").getAsDouble();
                data.researchProgress = o.has("researchProgress") ? o.get("researchProgress").getAsDouble() : 0.0;
                if (o.has("researchProjects")) {
                    JsonArray arr = o.getAsJsonArray("researchProjects");
                    for (var e : arr) data.researchProjects.add(e.getAsString());
                }
                String nationId = f.getName().replace(".json", "");
                educationData.put(nationId, data);
            } catch (Exception ignored) {}
        }
    }

    private void saveEducationData(String nationId, EducationData data) {
        File f = new File(educationDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("educationLevel", data.educationLevel);
        o.addProperty("researchProgress", data.researchProgress);
        JsonArray arr = new JsonArray();
        for (String p : data.researchProjects) arr.add(p);
        o.add("researchProjects", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive education statistics.
     */
    public synchronized Map<String, Object> getEducationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        EducationData data = educationData.get(nationId);
        
        if (data == null) {
            data = educationData.computeIfAbsent(nationId, k -> new EducationData());
        }
        
        stats.put("educationLevel", getEducationLevel(nationId));
        stats.put("researchProjects", data.researchProjects.size());
        stats.put("researchProgress", data.researchProgress);
        stats.put("activeProjects", new ArrayList<>(data.researchProjects));
        
        // Calculate research efficiency
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            double efficiency = 1.0;
            if (n.getBudgetEducation() > 10000) efficiency += 0.2; // +20% for high education budget
            if (plugin.getCityGrowthEngine() != null) {
                long citiesWithUniversities = plugin.getCityGrowthEngine().getCitiesOf(nationId).stream()
                    .filter(c -> c.hasUniversity())
                    .count();
                efficiency += citiesWithUniversities * 0.1; // +10% per university
            }
            stats.put("researchEfficiency", efficiency);
        }
        
        return stats;
    }
    
    /**
     * Complete a research project.
     */
    public synchronized String completeResearch(String nationId, String projectName) throws IOException {
        EducationData data = educationData.get(nationId);
        if (data == null || !data.researchProjects.contains(projectName)) {
            return "Проект не найден.";
        }
        
        data.researchProjects.remove(projectName);
        data.researchProgress = 0.0; // Reset progress
        data.educationLevel = Math.min(100, data.educationLevel + 5.0); // +5 education level
        
        saveEducationData(nationId, data);
        
        // VISUAL EFFECTS
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String msg = "§b📚 Исследование '" + projectName + "' завершено!";
                for (UUID citizenId : n.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        citizen.sendTitle("§b§l[ИССЛЕДОВАНИЕ]", "§f" + projectName + " завершено", 10, 60, 10);
                        plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                        org.bukkit.Location loc = citizen.getLocation();
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                        citizen.playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                    }
                }
            });
        }
        
        return "Исследование завершено! Уровень образования: +5";
    }
    
    /**
     * Invest in education (increase education level).
     */
    public synchronized String investInEducation(String nationId, double amount) throws IOException {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < amount) return "Недостаточно средств.";
        
        EducationData data = educationData.computeIfAbsent(nationId, k -> new EducationData());
        double levelIncrease = amount / 10000.0; // 10000 = 1 level
        data.educationLevel = Math.min(100, data.educationLevel + levelIncrease);
        
        n.setTreasury(n.getTreasury() - amount);
        n.setBudgetEducation(n.getBudgetEducation() + amount);
        
        nationManager.save(n);
        saveEducationData(nationId, data);
        
        return "Инвестировано в образование: " + amount + ". Уровень: +" + String.format("%.2f", levelIncrease);
    }
    
    /**
     * Get education bonus for technology research.
     */
    public synchronized double getResearchBonus(String nationId) {
        double level = getEducationLevel(nationId);
        // Education level directly affects research speed (0-200% bonus)
        return 1.0 + (level / 100.0);
    }
    
    /**
     * Get global education statistics.
     */
    public synchronized Map<String, Object> getGlobalEducationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalEducation = 0.0;
        double maxEducation = 0.0;
        double minEducation = Double.MAX_VALUE;
        int nationsWithEducation = 0;
        int totalResearchProjects = 0;
        double totalResearchProgress = 0.0;
        
        for (Nation n : nationManager.getAll()) {
            double eduLevel = getEducationLevel(n.getId());
            EducationData data = educationData.get(n.getId());
            
            if (eduLevel > 0 || (data != null && !data.researchProjects.isEmpty())) {
                nationsWithEducation++;
            }
            
            totalEducation += eduLevel;
            maxEducation = Math.max(maxEducation, eduLevel);
            minEducation = Math.min(minEducation, eduLevel);
            
            if (data != null) {
                totalResearchProjects += data.researchProjects.size();
                totalResearchProgress += data.researchProgress;
            }
        }
        
        stats.put("totalNations", nationManager.getAll().size());
        stats.put("nationsWithEducation", nationsWithEducation);
        stats.put("averageEducationLevel", nationsWithEducation > 0 ? totalEducation / nationsWithEducation : 0);
        stats.put("maxEducationLevel", maxEducation);
        stats.put("minEducationLevel", minEducation == Double.MAX_VALUE ? 0 : minEducation);
        stats.put("totalResearchProjects", totalResearchProjects);
        stats.put("totalResearchProgress", totalResearchProgress);
        stats.put("averageResearchProgress", nationsWithEducation > 0 ? totalResearchProgress / nationsWithEducation : 0);
        
        // Top nations by education
        List<Map.Entry<String, Double>> topByEducation = new ArrayList<>();
        for (Nation n : nationManager.getAll()) {
            topByEducation.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), getEducationLevel(n.getId())));
        }
        topByEducation.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByEducation", topByEducation.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

