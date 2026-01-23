package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages quests and missions for nations/players. */
public class QuestService {
    private final AXIOM plugin;
    private final File questsDir;
    private final Map<String, List<Quest>> nationQuests = new HashMap<>(); // nationId -> quests

    public static class Quest {
        String id;
        String nationId;
        String type; // "trade", "diplomacy", "war", "economy", "construction"
        String title;
        String description;
        Map<String, Double> objectives = new HashMap<>(); // objective -> target value
        Map<String, Double> progress = new HashMap<>(); // objective -> current value
        double reward;
        boolean completed;
    }

    public QuestService(AXIOM plugin) {
        this.plugin = plugin;
        this.questsDir = new File(plugin.getDataFolder(), "quests");
        this.questsDir.mkdirs();
        loadAll();
    }

    public synchronized String createQuest(String nationId, String type, String title, String description, Map<String, Double> objectives, double reward) throws IOException {
        if (objectives == null || objectives.isEmpty()) return "Цели квеста не заданы.";
        if (reward < 0) return "Неверная награда.";
        String id = nationId + "_" + System.currentTimeMillis();
        Quest q = new Quest();
        q.id = id;
        q.nationId = nationId;
        q.type = type;
        q.title = title;
        q.description = description;
        q.objectives = new HashMap<>(objectives);
        q.progress = new HashMap<>();
        for (String obj : objectives.keySet()) q.progress.put(obj, 0.0);
        q.reward = reward;
        q.completed = false;
        nationQuests.computeIfAbsent(nationId, k -> new ArrayList<>()).add(q);
        saveQuest(q);
        return "Квест создан: " + title;
    }

    public synchronized void updateProgress(String nationId, String objective, double amount) {
        List<Quest> quests = nationQuests.get(nationId);
        if (quests == null) return;
        for (Quest q : quests) {
            if (q.completed) continue;
            if (q.progress.containsKey(objective)) {
                q.progress.put(objective, q.progress.get(objective) + amount);
                if (isQuestComplete(q)) {
                    q.completed = true;
                    completeQuest(q);
                }
                try { saveQuest(q); } catch (Exception ignored) {}
            }
        }
    }

    private boolean isQuestComplete(Quest q) {
        for (var entry : q.objectives.entrySet()) {
            if (q.progress.getOrDefault(entry.getKey(), 0.0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void completeQuest(Quest q) {
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(q.nationId);
        if (n != null) {
            n.setTreasury(n.getTreasury() + q.reward);
            try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        }
    }

    private void loadAll() {
        File[] files = questsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Quest q = new Quest();
                q.id = o.get("id").getAsString();
                q.nationId = o.get("nationId").getAsString();
                q.type = o.get("type").getAsString();
                q.title = o.get("title").getAsString();
                q.description = o.get("description").getAsString();
                q.reward = o.has("reward") ? o.get("reward").getAsDouble() : 0.0;
                q.completed = o.has("completed") && o.get("completed").getAsBoolean();
                if (o.has("objectives")) {
                    JsonObject obj = o.getAsJsonObject("objectives");
                    for (var e : obj.entrySet()) q.objectives.put(e.getKey(), e.getValue().getAsDouble());
                }
                if (o.has("progress")) {
                    JsonObject prog = o.getAsJsonObject("progress");
                    for (var e : prog.entrySet()) q.progress.put(e.getKey(), e.getValue().getAsDouble());
                }
                nationQuests.computeIfAbsent(q.nationId, k -> new ArrayList<>()).add(q);
            } catch (Exception ignored) {}
        }
    }

    private void saveQuest(Quest q) throws IOException {
        File f = new File(questsDir, q.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", q.id);
        o.addProperty("nationId", q.nationId);
        o.addProperty("type", q.type);
        o.addProperty("title", q.title);
        o.addProperty("description", q.description);
        o.addProperty("reward", q.reward);
        o.addProperty("completed", q.completed);
        JsonObject obj = new JsonObject();
        for (var entry : q.objectives.entrySet()) obj.addProperty(entry.getKey(), entry.getValue());
        o.add("objectives", obj);
        JsonObject prog = new JsonObject();
        for (var entry : q.progress.entrySet()) prog.addProperty(entry.getKey(), entry.getValue());
        o.add("progress", prog);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive quest statistics.
     */
    public synchronized Map<String, Object> getQuestStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Quest> quests = nationQuests.get(nationId);
        if (quests == null) quests = Collections.emptyList();
        
        int total = quests.size();
        int completed = 0;
        int active = 0;
        double totalRewardsEarned = 0.0;
        
        Map<String, Integer> byType = new HashMap<>();
        
        for (Quest q : quests) {
            if (q.completed) {
                completed++;
                totalRewardsEarned += q.reward;
            } else {
                active++;
            }
            byType.put(q.type, byType.getOrDefault(q.type, 0) + 1);
        }
        
        stats.put("totalQuests", total);
        stats.put("completedQuests", completed);
        stats.put("activeQuests", active);
        stats.put("completionRate", total > 0 ? (completed / (double) total) * 100 : 0);
        stats.put("totalRewardsEarned", totalRewardsEarned);
        stats.put("byType", byType);
        
        // Active quest details
        List<Map<String, Object>> activeQuestsList = new ArrayList<>();
        for (Quest q : quests) {
            if (!q.completed) {
                Map<String, Object> questData = new HashMap<>();
                questData.put("id", q.id);
                questData.put("type", q.type);
                questData.put("title", q.title);
                questData.put("description", q.description);
                questData.put("progress", new HashMap<>(q.progress));
                questData.put("objectives", new HashMap<>(q.objectives));
                questData.put("reward", q.reward);
                
                // Calculate progress percentage
                double totalProgress = 0.0;
                double totalObjectives = 0.0;
                for (String obj : q.objectives.keySet()) {
                    totalObjectives += q.objectives.get(obj);
                    totalProgress += Math.min(q.objectives.get(obj), q.progress.getOrDefault(obj, 0.0));
                }
                double progressPercent = totalObjectives > 0 ? (totalProgress / totalObjectives) * 100 : 0;
                questData.put("progressPercent", progressPercent);
                
                activeQuestsList.add(questData);
            }
        }
        stats.put("activeQuestsList", activeQuestsList);
        
        return stats;
    }
    
    /**
     * Cancel/remove quest.
     */
    public synchronized String cancelQuest(String nationId, String questId) throws IOException {
        List<Quest> quests = nationQuests.get(nationId);
        if (quests == null) return "Квесты не найдены.";
        
        Quest quest = quests.stream()
            .filter(q -> q.id.equals(questId))
            .findFirst()
            .orElse(null);
        
        if (quest == null) return "Квест не найден.";
        
        quests.remove(quest);
        
        // Delete file
        File f = new File(questsDir, questId + ".json");
        if (f.exists()) f.delete();
        
        return "Квест отменён: " + quest.title;
    }
    
    /**
     * Get all active quests for a nation.
     */
    public synchronized List<Quest> getActiveQuests(String nationId) {
        List<Quest> quests = nationQuests.get(nationId);
        if (quests == null) return Collections.emptyList();
        
        List<Quest> active = new ArrayList<>();
        for (Quest q : quests) {
            if (!q.completed) {
                active.add(q);
            }
        }
        return active;
    }
    
    /**
     * Get quest by ID.
     */
    public synchronized Quest getQuest(String nationId, String questId) {
        List<Quest> quests = nationQuests.get(nationId);
        if (quests == null) return null;
        
        return quests.stream()
            .filter(q -> q.id.equals(questId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Calculate quest completion bonus (for completing multiple quests).
     */
    public synchronized double calculateQuestBonus(String nationId) {
        List<Quest> quests = nationQuests.get(nationId);
        if (quests == null) return 0.0;
        
        long completedCount = quests.stream()
            .filter(q -> q.completed)
            .count();
        
        // +1% to various bonuses per completed quest (capped)
        return Math.min(0.50, completedCount * 0.01); // Max +50% bonus
    }
    
    /**
     * Get global quest statistics.
     */
    public synchronized Map<String, Object> getGlobalQuestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalQuests = 0;
        int completedQuests = 0;
        int activeQuests = 0;
        double totalRewardsEarned = 0.0;
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byNation = new HashMap<>();
        
        for (Map.Entry<String, List<Quest>> entry : nationQuests.entrySet()) {
            for (Quest q : entry.getValue()) {
                totalQuests++;
                if (q.completed) {
                    completedQuests++;
                    totalRewardsEarned += q.reward;
                } else {
                    activeQuests++;
                }
                byType.put(q.type, byType.getOrDefault(q.type, 0) + 1);
                byNation.put(q.nationId, byNation.getOrDefault(q.nationId, 0) + 1);
            }
        }
        
        stats.put("totalQuests", totalQuests);
        stats.put("completedQuests", completedQuests);
        stats.put("activeQuests", activeQuests);
        stats.put("completionRate", totalQuests > 0 ? (completedQuests / (double) totalQuests) * 100 : 0);
        stats.put("totalRewardsEarned", totalRewardsEarned);
        stats.put("averageReward", completedQuests > 0 ? totalRewardsEarned / completedQuests : 0);
        stats.put("questsByType", byType);
        stats.put("questsByNation", byNation);
        
        // Top nations by quests
        List<Map.Entry<String, Integer>> topByQuests = byNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByQuests", topByQuests);
        
        // Nations with quests
        stats.put("nationsWithQuests", byNation.size());
        
        return stats;
    }
}

