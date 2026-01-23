package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Tracks public opinion and approval ratings for nations. */
public class PublicOpinionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File opinionDir;
    private final Map<String, OpinionData> nationOpinions = new HashMap<>(); // nationId -> data

    public static class OpinionData {
        double approvalRating; // 0-100%
        Map<String, Double> issueSupport = new HashMap<>(); // issue -> support %
        long lastUpdated;
    }

    public PublicOpinionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.opinionDir = new File(plugin.getDataFolder(), "publicopinion");
        this.opinionDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateOpinions, 0, 20 * 60 * 10); // every 10 minutes
    }

    private void updateOpinions() {
        for (Nation n : nationManager.getAll()) {
            OpinionData data = nationOpinions.computeIfAbsent(n.getId(), k -> {
                OpinionData d = new OpinionData();
                d.approvalRating = 50.0;
                d.lastUpdated = System.currentTimeMillis();
                return d;
            });
            // Factors affecting approval
            double happiness = plugin.getHappinessService().getNationHappiness(n.getId());
            double economy = n.getTreasury() > 10000 ? 10 : -10;
            double warPenalty = n.getEnemies().isEmpty() ? 0 : -15;
            double corruption = plugin.getCorruptionService().getCorruptionLevel(n.getId()) * -0.3;
            data.approvalRating = Math.max(0, Math.min(100, happiness * 0.5 + economy + warPenalty + corruption));
            data.lastUpdated = System.currentTimeMillis();
            saveOpinion(n.getId(), data);
        }
    }

    public synchronized double getApprovalRating(String nationId) {
        OpinionData data = nationOpinions.get(nationId);
        return data != null ? data.approvalRating : 50.0;
    }

    public synchronized String setIssueSupport(String nationId, String issue, double support) {
        OpinionData data = nationOpinions.computeIfAbsent(nationId, k -> {
            OpinionData d = new OpinionData();
            d.approvalRating = 50.0;
            d.lastUpdated = System.currentTimeMillis();
            return d;
        });
        data.issueSupport.put(issue, Math.max(0, Math.min(100, support)));
        saveOpinion(nationId, data);
        return "Поддержка вопроса установлена: " + support + "%";
    }

    private void loadAll() {
        File[] files = opinionDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                OpinionData data = new OpinionData();
                data.approvalRating = o.get("approvalRating").getAsDouble();
                data.lastUpdated = o.has("lastUpdated") ? o.get("lastUpdated").getAsLong() : System.currentTimeMillis();
                if (o.has("issueSupport")) {
                    JsonObject issues = o.getAsJsonObject("issueSupport");
                    for (var entry : issues.entrySet()) {
                        data.issueSupport.put(entry.getKey(), entry.getValue().getAsDouble());
                    }
                }
                nationOpinions.put(nationId, data);
            } catch (Exception ignored) {}
        }
    }

    private void saveOpinion(String nationId, OpinionData data) {
        File f = new File(opinionDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("approvalRating", data.approvalRating);
        o.addProperty("lastUpdated", data.lastUpdated);
        JsonObject issues = new JsonObject();
        for (var entry : data.issueSupport.entrySet()) {
            issues.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("issueSupport", issues);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive public opinion statistics.
     */
    public synchronized Map<String, Object> getPublicOpinionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        OpinionData data = nationOpinions.get(nationId);
        if (data == null) {
            data = nationOpinions.computeIfAbsent(nationId, k -> {
                OpinionData d = new OpinionData();
                d.approvalRating = 50.0;
                d.lastUpdated = System.currentTimeMillis();
                return d;
            });
        }
        
        stats.put("approvalRating", data.approvalRating);
        stats.put("issueSupport", new HashMap<>(data.issueSupport));
        stats.put("lastUpdated", data.lastUpdated);
        
        // Approval rating category
        String category = "НЕЙТРАЛЬНО";
        if (data.approvalRating >= 70) category = "ОЧЕНЬ_ПОЛОЖИТЕЛЬНО";
        else if (data.approvalRating >= 50) category = "ПОЛОЖИТЕЛЬНО";
        else if (data.approvalRating >= 30) category = "НЕГАТИВНО";
        else category = "ОЧЕНЬ_НЕГАТИВНО";
        stats.put("category", category);
        
        // Factors affecting approval
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            Map<String, Double> factors = new HashMap<>();
            double happiness = plugin.getHappinessService().getNationHappiness(nationId);
            factors.put("happiness", happiness * 0.5);
            factors.put("economy", n.getTreasury() > 10000 ? 10.0 : -10.0);
            factors.put("war", n.getEnemies().isEmpty() ? 0.0 : -15.0);
            
            try {
                double corruption = plugin.getCorruptionService().getCorruptionLevel(nationId) * -0.3;
                factors.put("corruption", corruption);
            } catch (Exception e) {
                factors.put("corruption", 0.0);
            }
            
            stats.put("factors", factors);
        }
        
        return stats;
    }
    
    /**
     * Apply public opinion event (speeches, scandals, etc.).
     */
    public synchronized String applyOpinionEvent(String nationId, String eventType, double impact) {
        OpinionData data = nationOpinions.computeIfAbsent(nationId, k -> {
            OpinionData d = new OpinionData();
            d.approvalRating = 50.0;
            d.lastUpdated = System.currentTimeMillis();
            return d;
        });
        
        double multiplier = 1.0;
        switch (eventType.toLowerCase()) {
            case "victory":
            case "speech":
            case "reform":
                multiplier = 1.5; // Positive events
                break;
            case "scandal":
            case "defeat":
            case "crisis":
                multiplier = 2.0; // Negative events
                break;
        }
        
        data.approvalRating = Math.max(0, Math.min(100, data.approvalRating + (impact * multiplier)));
        data.lastUpdated = System.currentTimeMillis();
        saveOpinion(nationId, data);
        
        return "Общественное мнение обновлено. Рейтинг: " + String.format("%.1f", data.approvalRating) + "%";
    }
    
    /**
     * Get top issues by support.
     */
    public synchronized List<Map.Entry<String, Double>> getTopIssues(String nationId, int limit) {
        OpinionData data = nationOpinions.get(nationId);
        if (data == null || data.issueSupport.isEmpty()) return Collections.emptyList();
        
        return data.issueSupport.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get global public opinion statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPublicOpinionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalApproval = 0.0;
        double maxApproval = 0.0;
        double minApproval = Double.MAX_VALUE;
        int nationsWithOpinion = 0;
        
        // Approval distribution
        int veryHigh = 0, high = 0, medium = 0, low = 0, veryLow = 0;
        
        for (Nation n : nationManager.getAll()) {
            OpinionData data = nationOpinions.get(n.getId());
            double approval = data != null ? data.approvalRating : 50.0;
            
            if (data != null || approval != 50.0) {
                nationsWithOpinion++;
            }
            
            totalApproval += approval;
            maxApproval = Math.max(maxApproval, approval);
            minApproval = Math.min(minApproval, approval);
            
            if (approval >= 70) veryHigh++;
            else if (approval >= 50) high++;
            else if (approval >= 30) medium++;
            else if (approval >= 20) low++;
            else veryLow++;
        }
        
        stats.put("totalNations", nationManager.getAll().size());
        stats.put("nationsWithOpinion", nationsWithOpinion);
        stats.put("averageApproval", nationsWithOpinion > 0 ? totalApproval / nationsWithOpinion : 0);
        stats.put("maxApproval", maxApproval);
        stats.put("minApproval", minApproval == Double.MAX_VALUE ? 0 : minApproval);
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("veryHigh", veryHigh);
        distribution.put("high", high);
        distribution.put("medium", medium);
        distribution.put("low", low);
        distribution.put("veryLow", veryLow);
        stats.put("approvalDistribution", distribution);
        
        // Top nations by approval
        List<Map.Entry<String, Double>> topByApproval = new ArrayList<>();
        for (Nation n : nationManager.getAll()) {
            OpinionData data = nationOpinions.get(n.getId());
            double approval = data != null ? data.approvalRating : 50.0;
            topByApproval.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), approval));
        }
        topByApproval.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByApproval", topByApproval.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

