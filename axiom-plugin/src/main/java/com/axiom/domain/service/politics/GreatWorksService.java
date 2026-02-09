package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages great works and mega-projects that require multiple nations. */
public class GreatWorksService {
    private final AXIOM plugin;
    private final File worksDir;
    private final Map<String, GreatWork> activeWorks = new HashMap<>(); // workId -> work

    public static class GreatWork {
        String id;
        String name;
        String type; // "wonder", "canal", "railroad", "mega_building"
        Set<String> participatingNations = new HashSet<>();
        Map<String, Double> contributions = new HashMap<>(); // nationId -> contribution amount
        double totalCost;
        double progress; // 0-100%
        boolean completed;
    }

    public GreatWorksService(AXIOM plugin) {
        this.plugin = plugin;
        this.worksDir = new File(plugin.getDataFolder(), "greatworks");
        this.worksDir.mkdirs();
        loadAll();
    }

    public synchronized String contributeToWork(String nationId, String workId, double amount) {
        GreatWork work = activeWorks.get(workId);
        if (work == null) return "Проект не найден.";
        if (work.completed) return "Проект уже завершён.";
        if (amount <= 0) return "Сумма должна быть больше 0.";
        if (work.totalCost <= 0) return "Некорректная стоимость проекта.";
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null || n.getTreasury() < amount) return "Недостаточно средств.";
        n.setTreasury(n.getTreasury() - amount);
        double current = work.contributions.getOrDefault(nationId, 0.0);
        work.contributions.put(nationId, current + amount);
        work.progress = Math.min(100, work.progress + (amount / work.totalCost * 100.0));
        work.participatingNations.add(nationId);
        if (work.progress >= 100) {
            work.completed = true;
            completeWork(work);
        }
        try {
            plugin.getNationManager().save(n);
            saveWork(work);
        } catch (Exception ignored) {}
        return "Вклад в проект: " + amount + ". Прогресс: " + work.progress + "%";
    }

    private void completeWork(GreatWork work) {
        // Grant bonuses to all participating nations
        if (plugin.getNationModifierService() == null) return;
        for (String nationId : work.participatingNations) {
            plugin.getNationModifierService().addModifier(nationId, "economy", "bonus", 0.1, 7200); // 5 days
        }
    }

    private void loadAll() {
        File[] files = worksDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                GreatWork work = new GreatWork();
                work.id = o.get("id").getAsString();
                work.name = o.get("name").getAsString();
                work.type = o.get("type").getAsString();
                work.totalCost = o.get("totalCost").getAsDouble();
                work.progress = o.get("progress").getAsDouble();
                work.completed = o.has("completed") && o.get("completed").getAsBoolean();
                if (o.has("participatingNations")) {
                    for (var e : o.getAsJsonArray("participatingNations")) {
                        work.participatingNations.add(e.getAsString());
                    }
                }
                if (o.has("contributions")) {
                    JsonObject contribs = o.getAsJsonObject("contributions");
                    for (var e : contribs.entrySet()) {
                        work.contributions.put(e.getKey(), e.getValue().getAsDouble());
                    }
                }
                if (!work.completed) activeWorks.put(work.id, work);
            } catch (Exception ignored) {}
        }
    }

    private void saveWork(GreatWork work) throws IOException {
        File f = new File(worksDir, work.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", work.id);
        o.addProperty("name", work.name);
        o.addProperty("type", work.type);
        o.addProperty("totalCost", work.totalCost);
        o.addProperty("progress", work.progress);
        o.addProperty("completed", work.completed);
        JsonArray nations = new JsonArray();
        for (String n : work.participatingNations) nations.add(n);
        o.add("participatingNations", nations);
        JsonObject contribs = new JsonObject();
        for (var e : work.contributions.entrySet()) {
            contribs.addProperty(e.getKey(), e.getValue());
        }
        o.add("contributions", contribs);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive great works statistics.
     */
    public synchronized Map<String, Object> getGreatWorksStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Works this nation is participating in
        List<GreatWork> participatingWorks = new ArrayList<>();
        double totalContributions = 0.0;
        
        for (GreatWork work : activeWorks.values()) {
            if (work.participatingNations.contains(nationId)) {
                participatingWorks.add(work);
                totalContributions += work.contributions.getOrDefault(nationId, 0.0);
            }
        }
        
        stats.put("participatingWorks", participatingWorks.size());
        stats.put("totalContributions", totalContributions);
        
        // Works by type
        Map<String, Integer> byType = new HashMap<>();
        for (GreatWork work : participatingWorks) {
            byType.put(work.type, byType.getOrDefault(work.type, 0) + 1);
        }
        stats.put("byType", byType);
        
        // Work details
        List<Map<String, Object>> worksList = new ArrayList<>();
        for (GreatWork work : participatingWorks) {
            Map<String, Object> workData = new HashMap<>();
            workData.put("id", work.id);
            workData.put("name", work.name);
            workData.put("type", work.type);
            workData.put("progress", work.progress);
            workData.put("totalCost", work.totalCost);
            workData.put("contribution", work.contributions.getOrDefault(nationId, 0.0));
            workData.put("participants", work.participatingNations.size());
            workData.put("isCompleted", work.completed);
            workData.put("remainingCost", work.completed ? 0 : (work.totalCost * (1.0 - work.progress / 100.0)));
            worksList.add(workData);
        }
        stats.put("worksList", worksList);
        
        // Great works rating
        String rating = "НЕТ УЧАСТИЯ";
        if (participatingWorks.size() >= 10) rating = "ВЕДУЩИЙ БЛАГОТВОРИТЕЛЬ";
        else if (participatingWorks.size() >= 5) rating = "АКТИВНЫЙ УЧАСТНИК";
        else if (participatingWorks.size() >= 3) rating = "ЗНАЧИТЕЛЬНЫЙ";
        else if (participatingWorks.size() >= 1) rating = "УЧАСТНИК";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Create great work.
     */
    public synchronized String createWork(String name, String type, double totalCost) throws IOException {
        if (name == null || name.trim().isEmpty() || type == null || type.trim().isEmpty()) {
            return "Неверные параметры.";
        }
        if (totalCost <= 0) return "Стоимость должна быть больше 0.";
        GreatWork work = new GreatWork();
        work.id = UUID.randomUUID().toString();
        work.name = name;
        work.type = type;
        work.totalCost = totalCost;
        work.progress = 0.0;
        work.completed = false;
        
        activeWorks.put(work.id, work);
        saveWork(work);
        
        return "Проект создан: " + name + " (ID: " + work.id + ")";
    }
    
    /**
     * Get all active works.
     */
    public synchronized List<GreatWork> getActiveWorks() {
        return new ArrayList<>(activeWorks.values());
    }
    
    /**
     * Get work by ID.
     */
    public synchronized GreatWork getWork(String workId) {
        return activeWorks.get(workId);
    }
    
    /**
     * Calculate completion bonus.
     */
    public synchronized double getCompletionBonus(String nationId) {
        long completedCount = activeWorks.values().stream()
            .filter(w -> w.completed && w.participatingNations.contains(nationId))
            .count();
        
        // +5% economy bonus per completed work (capped)
        return 1.0 + Math.min(0.50, completedCount * 0.05); // Max +50%
    }
    
    /**
     * Get global great works statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalGreatWorksStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveWorks", activeWorks.size());
        
        int completedWorks = 0;
        double totalProgress = 0.0;
        double totalCost = 0.0;
        Map<String, Integer> worksByType = new HashMap<>();
        Map<String, Integer> participantsByNation = new HashMap<>();
        Map<String, Double> contributionsByNation = new HashMap<>();
        
        for (GreatWork work : activeWorks.values()) {
            if (work.completed) completedWorks++;
            
            totalProgress += work.progress;
            totalCost += work.totalCost;
            worksByType.put(work.type, worksByType.getOrDefault(work.type, 0) + 1);
            
            for (String nationId : work.participatingNations) {
                participantsByNation.put(nationId, participantsByNation.getOrDefault(nationId, 0) + 1);
                contributionsByNation.put(nationId, 
                    contributionsByNation.getOrDefault(nationId, 0.0) + work.contributions.getOrDefault(nationId, 0.0));
            }
        }
        
        stats.put("completedWorks", completedWorks);
        stats.put("inProgressWorks", activeWorks.size() - completedWorks);
        stats.put("averageProgress", activeWorks.size() > 0 ? totalProgress / activeWorks.size() : 0);
        stats.put("totalCost", totalCost);
        stats.put("averageCost", activeWorks.size() > 0 ? totalCost / activeWorks.size() : 0);
        stats.put("worksByType", worksByType);
        stats.put("participantsByNation", participantsByNation);
        stats.put("contributionsByNation", contributionsByNation);
        stats.put("nationsParticipating", participantsByNation.size());
        
        // Top nations by participation
        List<Map.Entry<String, Integer>> topByParticipation = participantsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByParticipation", topByParticipation);
        
        // Top nations by contributions
        List<Map.Entry<String, Double>> topByContributions = contributionsByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByContributions", topByContributions);
        
        // Most common work types
        List<Map.Entry<String, Integer>> mostCommonTypes = worksByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTypes", mostCommonTypes);
        
        // Average participants per work
        int totalParticipants = participantsByNation.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageParticipantsPerWork", activeWorks.size() > 0 ? 
            (double) totalParticipants / activeWorks.size() : 0);
        
        // Completion rate
        stats.put("completionRate", activeWorks.size() > 0 ? 
            (completedWorks / (double) activeWorks.size()) * 100 : 0);
        
        return stats;
    }
}

