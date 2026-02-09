package com.axiom.domain.service.technology;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.state.EducationService;
import com.axiom.domain.service.state.NationManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages research collaborations between nations. */
public class ResearchCollaborationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File collaborationsDir;
    private final Map<String, ResearchCollaboration> activeCollaborations = new HashMap<>(); // collabId -> collaboration

    public static class ResearchCollaboration {
        String id;
        Set<String> participantNations = new HashSet<>();
        String researchTopic;
        double progress; // 0-100%
        double contributionRate; // per hour
        long startedAt;
        boolean completed;
    }

    public ResearchCollaborationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.collaborationsDir = new File(plugin.getDataFolder(), "researchcollaborations");
        this.collaborationsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processCollaborations, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String startCollaboration(String nationA, String nationB, String topic, double cost) {
        Nation a = nationManager.getNationById(nationA);
        Nation b = nationManager.getNationById(nationB);
        if (a == null || b == null) return "Нация не найдена.";
        if (nationA.equals(nationB)) return "Нельзя начать сотрудничество с собой.";
        if (cost <= 0) return "Неверная сумма.";
        if (a.getTreasury() < cost || b.getTreasury() < cost) return "Недостаточно средств.";
        String collabId = UUID.randomUUID().toString().substring(0, 8);
        ResearchCollaboration collab = new ResearchCollaboration();
        collab.id = collabId;
        collab.participantNations.add(nationA);
        collab.participantNations.add(nationB);
        collab.researchTopic = topic;
        collab.progress = 0.0;
        collab.contributionRate = 2.0; // 2% per 10 minutes
        collab.startedAt = System.currentTimeMillis();
        collab.completed = false;
        activeCollaborations.put(collabId, collab);
        a.setTreasury(a.getTreasury() - cost);
        b.setTreasury(b.getTreasury() - cost);
        a.getHistory().add("Начато совместное исследование: " + topic);
        b.getHistory().add("Начато совместное исследование: " + topic);
        try {
            nationManager.save(a);
            nationManager.save(b);
            saveCollaboration(collab);
        } catch (Exception ignored) {}
        return "Совместное исследование начато: " + topic;
    }

    private synchronized void processCollaborations() {
        List<String> completed = new ArrayList<>();
        EducationService educationService = plugin.getEducationService();
        TechnologyTreeService techService = plugin.getTechnologyTreeService();
        for (Map.Entry<String, ResearchCollaboration> e : activeCollaborations.entrySet()) {
            ResearchCollaboration collab = e.getValue();
            if (collab.completed) {
                completed.add(e.getKey());
                continue;
            }
            collab.progress = Math.min(100, collab.progress + collab.contributionRate);
            if (collab.progress >= 100) {
                collab.completed = true;
                // All participants get research benefits
                for (String nationId : collab.participantNations) {
                    if (educationService != null) {
                        educationService.addResearchProgress(nationId, 100.0);
                    }
                    if (techService != null) {
                        techService.addResearchPoints(nationId, "general", 50.0);
                    }
                    Nation n = nationManager.getNationById(nationId);
                    if (n != null) {
                        n.getHistory().add("Совместное исследование завершено: " + collab.researchTopic);
                        try { nationManager.save(n); } catch (Exception ignored) {}
                    }
                }
            }
            saveCollaboration(collab);
        }
        for (String collabId : completed) {
            activeCollaborations.remove(collabId);
        }
    }

    private void loadAll() {
        File[] files = collaborationsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                ResearchCollaboration collab = new ResearchCollaboration();
                collab.id = o.get("id").getAsString();
                collab.researchTopic = o.get("researchTopic").getAsString();
                collab.progress = o.get("progress").getAsDouble();
                collab.contributionRate = o.get("contributionRate").getAsDouble();
                collab.startedAt = o.get("startedAt").getAsLong();
                collab.completed = o.has("completed") && o.get("completed").getAsBoolean();
                if (o.has("participantNations")) {
                    for (var elem : o.getAsJsonArray("participantNations")) {
                        collab.participantNations.add(elem.getAsString());
                    }
                }
                if (!collab.completed) activeCollaborations.put(collab.id, collab);
            } catch (Exception ignored) {}
        }
    }

    private void saveCollaboration(ResearchCollaboration collab) {
        File f = new File(collaborationsDir, collab.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", collab.id);
        o.addProperty("researchTopic", collab.researchTopic);
        o.addProperty("progress", collab.progress);
        o.addProperty("contributionRate", collab.contributionRate);
        o.addProperty("startedAt", collab.startedAt);
        o.addProperty("completed", collab.completed);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String nationId : collab.participantNations) {
            arr.add(nationId);
        }
        o.add("participantNations", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive research collaboration statistics.
     */
    public synchronized Map<String, Object> getCollaborationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Active collaborations
        List<ResearchCollaboration> activeCollabs = new ArrayList<>();
        List<ResearchCollaboration> completedCollabs = new ArrayList<>();
        
        for (ResearchCollaboration collab : activeCollaborations.values()) {
            if (collab.participantNations.contains(nationId)) {
                activeCollabs.add(collab);
            }
        }
        
        // Load completed collaborations
        File[] files = collaborationsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    if (o.has("completed") && o.get("completed").getAsBoolean()) {
                        ResearchCollaboration collab = new ResearchCollaboration();
                        collab.id = o.get("id").getAsString();
                        collab.researchTopic = o.get("researchTopic").getAsString();
                        collab.progress = 100.0; // Completed
                        collab.startedAt = o.get("startedAt").getAsLong();
                        collab.completed = true;
                        if (o.has("participantNations")) {
                            for (var elem : o.getAsJsonArray("participantNations")) {
                                collab.participantNations.add(elem.getAsString());
                            }
                        }
                        if (collab.participantNations.contains(nationId)) {
                            completedCollabs.add(collab);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        
        stats.put("activeCollaborations", activeCollabs.size());
        stats.put("completedCollaborations", completedCollabs.size());
        stats.put("totalCollaborations", activeCollabs.size() + completedCollabs.size());
        
        // Collaboration details
        List<Map<String, Object>> activeList = new ArrayList<>();
        for (ResearchCollaboration collab : activeCollabs) {
            Map<String, Object> collabData = new HashMap<>();
            collabData.put("id", collab.id);
            collabData.put("topic", collab.researchTopic);
            collabData.put("progress", collab.progress);
            collabData.put("contributionRate", collab.contributionRate);
            collabData.put("participants", new ArrayList<>(collab.participantNations));
            collabData.put("estimatedCompletion", collab.contributionRate > 0 ? (100 - collab.progress) / collab.contributionRate : -1); // cycles
            activeList.add(collabData);
        }
        stats.put("activeCollaborationsList", activeList);
        
        // Collaboration rating
        String rating = "НЕТ СОВМЕСТНЫХ ИССЛЕДОВАНИЙ";
        if (completedCollabs.size() >= 10) rating = "ВЕДУЩИЙ ИССЛЕДОВАТЕЛЬ";
        else if (completedCollabs.size() >= 5) rating = "АКТИВНЫЙ ИССЛЕДОВАТЕЛЬ";
        else if (completedCollabs.size() >= 1) rating = "УЧАСТНИК ИССЛЕДОВАНИЙ";
        else if (activeCollabs.size() >= 3) rating = "АКТИВНОЕ СОТРУДНИЧЕСТВО";
        else if (activeCollabs.size() >= 1) rating = "В ПРОЦЕССЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Add participant to collaboration.
     */
    public synchronized String addParticipant(String collabId, String nationId, double contribution) throws IOException {
        ResearchCollaboration collab = activeCollaborations.get(collabId);
        if (collab == null) return "Сотрудничество не найдено.";
        if (collab.participantNations.contains(nationId)) return "Нация уже участвует.";
        if (contribution <= 0) return "Неверный вклад.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        
        collab.participantNations.add(nationId);
        collab.contributionRate += contribution; // Increase progress rate
        
        n.getHistory().add("Присоединилась к совместному исследованию: " + collab.researchTopic);
        
        nationManager.save(n);
        saveCollaboration(collab);
        
        return "Участник добавлен. Скорость прогресса: " + String.format("%.1f", collab.contributionRate) + "%";
    }
    
    /**
     * Leave collaboration.
     */
    public synchronized String leaveCollaboration(String collabId, String nationId) throws IOException {
        ResearchCollaboration collab = activeCollaborations.get(collabId);
        if (collab == null) return "Сотрудничество не найдено.";
        if (!collab.participantNations.contains(nationId)) return "Вы не участвуете.";
        
        collab.participantNations.remove(nationId);
        
        // If no participants left, cancel collaboration
        if (collab.participantNations.isEmpty()) {
            activeCollaborations.remove(collabId);
            File f = new File(collaborationsDir, collabId + ".json");
            if (f.exists()) f.delete();
        } else {
            saveCollaboration(collab);
        }
        
        return "Вы покинули совместное исследование.";
    }
    
    /**
     * Get all active collaborations for a nation.
     */
    public synchronized List<ResearchCollaboration> getActiveCollaborations(String nationId) {
        return activeCollaborations.values().stream()
            .filter(c -> c.participantNations.contains(nationId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate research bonus from collaborations.
     */
    public synchronized double getCollaborationResearchBonus(String nationId) {
        int activeCount = getActiveCollaborations(nationId).size();
        // +5% research bonus per active collaboration (capped)
        return 1.0 + Math.min(0.50, activeCount * 0.05); // Max +50%
    }
    
    /**
     * Get global research collaboration statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalResearchCollaborationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveCollaborations", activeCollaborations.size());
        
        Map<String, Integer> collaborationsByTopic = new HashMap<>();
        double totalProgress = 0.0;
        double totalContributionRate = 0.0;
        Map<String, Integer> participantsByNation = new HashMap<>();
        Set<String> allTopics = new HashSet<>();
        
        for (ResearchCollaboration collab : activeCollaborations.values()) {
            collaborationsByTopic.put(collab.researchTopic, collaborationsByTopic.getOrDefault(collab.researchTopic, 0) + 1);
            totalProgress += collab.progress;
            totalContributionRate += collab.contributionRate;
            allTopics.add(collab.researchTopic);
            
            for (String nationId : collab.participantNations) {
                participantsByNation.put(nationId, participantsByNation.getOrDefault(nationId, 0) + 1);
            }
        }
        
        stats.put("collaborationsByTopic", collaborationsByTopic);
        stats.put("averageProgress", activeCollaborations.size() > 0 ? totalProgress / activeCollaborations.size() : 0);
        stats.put("averageContributionRate", activeCollaborations.size() > 0 ? totalContributionRate / activeCollaborations.size() : 0);
        stats.put("participantsByNation", participantsByNation);
        stats.put("nationsParticipating", participantsByNation.size());
        stats.put("uniqueTopics", allTopics.size());
        
        // Top nations by participation
        List<Map.Entry<String, Integer>> topByParticipation = participantsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByParticipation", topByParticipation);
        
        // Most common research topics
        List<Map.Entry<String, Integer>> mostCommonTopics = collaborationsByTopic.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTopics", mostCommonTopics);
        
        // Average participants per collaboration
        int totalParticipants = participantsByNation.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageParticipantsPerCollab", activeCollaborations.size() > 0 ? 
            (double) totalParticipants / activeCollaborations.size() : 0);
        
        return stats;
    }
}
