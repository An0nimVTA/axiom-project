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

/** Manages elections and parliament voting. */
public class ElectionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File electionsDir;
    private final Map<String, Election> activeElections = new HashMap<>(); // nationId -> election

    public static class Election {
        public String nationId;
        public String type; // "president", "parliament", "law", "minister"
        public List<String> candidates = new ArrayList<>();
        public Map<UUID, String> votes = new HashMap<>(); // voter -> candidate
        public long startTime;
        public long endTime;
        public long durationMinutes;
    }

    public ElectionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.electionsDir = new File(plugin.getDataFolder(), "elections");
        this.electionsDir.mkdirs();
        loadElections();
    }

    public synchronized String startElection(String nationId, String type, long durationMinutes, List<String> candidates) throws IOException {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (activeElections.containsKey(nationId + "_" + type)) return "Выборы этого типа уже активны.";
        Election e = new Election();
        e.nationId = nationId;
        e.type = type;
        e.candidates = candidates != null ? candidates : new ArrayList<>();
        e.durationMinutes = durationMinutes;
        e.startTime = System.currentTimeMillis();
        e.endTime = e.startTime + (durationMinutes * 60_000L);
        activeElections.put(nationId + "_" + type, e);
        saveElection(e);
        long timestamp = System.currentTimeMillis();
        String typeName = getTypeName(type);
        n.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Начаты выборы: " + typeName + " (длительность: " + durationMinutes + " мин)");
        nationManager.save(n);
        
        // VISUAL EFFECTS: Announce election to all nation members
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID citizenId : n.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    plugin.getVisualEffectsService().playElectionEffect(citizen, n.getName(), typeName);
                }
            }
        });
        
        return "Выборы начаты. Голосование открыто на " + durationMinutes + " минут. Тип: " + typeName;
    }

    private String getTypeName(String type) {
        switch (type.toLowerCase()) {
            case "president": return "Президент";
            case "parliament": return "Парламент";
            case "law": return "Закон";
            case "minister": return "Министр";
            case "leader": return "Лидер";
            default: return type;
        }
    }

    public synchronized String vote(UUID voter, String nationId, String type, String candidate) throws IOException {
        String key = nationId + "_" + type;
        Election e = activeElections.get(key);
        if (e == null) return "Выборы не активны.";
        if (System.currentTimeMillis() > e.endTime) {
            activeElections.remove(key);
            return "Выборы завершены.";
        }
        Nation n = nationManager.getNationById(nationId);
        if (n == null || !n.isMember(voter)) return "Вы не в этой нации.";
        if (!e.candidates.contains(candidate)) return "Кандидат не найден.";
        e.votes.put(voter, candidate);
        saveElection(e);
        return "Голос засчитан.";
    }

    public synchronized Election getActiveElection(String nationId, String type) {
        return activeElections.get(nationId + "_" + type);
    }

    public synchronized Map<String, Integer> getResults(String nationId, String type) {
        String key = nationId + "_" + type;
        Election e = activeElections.get(key);
        if (e == null) return null;
        Map<String, Integer> results = new HashMap<>();
        for (String candidate : e.candidates) {
            results.put(candidate, 0);
        }
        for (String candidate : e.votes.values()) {
            results.put(candidate, results.getOrDefault(candidate, 0) + 1);
        }
        return results;
    }

    public synchronized String finishElection(String nationId, String type) throws IOException {
        String key = nationId + "_" + type;
        Election e = activeElections.remove(key);
        if (e == null) return "Выборы не найдены.";
        Map<String, Integer> results = getResults(nationId, type);
        if (results == null || results.isEmpty()) return "Нет результатов.";
        String winner = results.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Нет");
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            long timestamp = System.currentTimeMillis();
            n.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Выборы завершены. Победитель: " + winner);
            nationManager.save(n);
            
            // VISUAL EFFECTS: Announce election results to all nation members
            final String typeName = getTypeName(type);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String msg = "§b✓ Выборы '" + typeName + "' завершены! Победитель: §e" + winner;
                for (UUID citizenId : n.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        citizen.sendTitle("§b§l[РЕЗУЛЬТАТЫ]", "§f" + typeName + ": §e" + winner, 10, 80, 20);
                        plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                        // Blue/gold particles
                        org.bukkit.Location loc = citizen.getLocation();
                        for (int i = 0; i < 20; i++) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                        }
                        citizen.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                    }
                }
            });
        }
        return "Выборы завершены. Победитель: " + winner;
    }

    private void saveElection(Election e) throws IOException {
        File f = new File(electionsDir, e.nationId + "_" + e.type + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", e.nationId);
        o.addProperty("type", e.type);
        o.addProperty("startTime", e.startTime);
        o.addProperty("endTime", e.endTime);
        o.addProperty("durationMinutes", e.durationMinutes);
        JsonArray cands = new JsonArray();
        for (String c : e.candidates) cands.add(c);
        o.add("candidates", cands);
        JsonObject votesObj = new JsonObject();
        for (var entry : e.votes.entrySet()) votesObj.addProperty(entry.getKey().toString(), entry.getValue());
        o.add("votes", votesObj);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }

    public void loadElections() {
        File[] files = electionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Election e = new Election();
                e.nationId = o.get("nationId").getAsString();
                e.type = o.get("type").getAsString();
                e.startTime = o.has("startTime") ? o.get("startTime").getAsLong() : System.currentTimeMillis();
                e.endTime = o.get("endTime").getAsLong();
                e.durationMinutes = o.has("durationMinutes") ? o.get("durationMinutes").getAsLong() : 5;
                if (o.has("candidates")) {
                    JsonArray arr = o.getAsJsonArray("candidates");
                    for (var elem : arr) e.candidates.add(elem.getAsString());
                }
                if (o.has("votes")) {
                    JsonObject votesObj = o.getAsJsonObject("votes");
                    for (var entry : votesObj.entrySet()) {
                        e.votes.put(UUID.fromString(entry.getKey()), entry.getValue().getAsString());
                    }
                }
                String key = e.nationId + "_" + e.type;
                if (System.currentTimeMillis() < e.endTime) {
                    activeElections.put(key, e);
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * Get comprehensive election statistics.
     */
    public synchronized Map<String, Object> getElectionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        int activeCount = 0;
        int totalElections = 0;
        for (Election e : activeElections.values()) {
            if (e.nationId.equals(nationId)) {
                activeCount++;
                totalElections++;
            }
        }
        
        // Count all elections (active and finished)
        File[] files = electionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(nationId + "_")) {
                    totalElections++;
                }
            }
        }
        
        stats.put("activeElections", activeCount);
        stats.put("totalElections", totalElections);
        
        // Get active election details
        List<Map<String, Object>> activeDetails = new ArrayList<>();
        for (Election e : activeElections.values()) {
            if (e.nationId.equals(nationId)) {
                Map<String, Object> details = new HashMap<>();
                details.put("type", e.type);
                details.put("typeName", getTypeName(e.type));
                details.put("candidates", new ArrayList<>(e.candidates));
                details.put("votes", e.votes.size());
                details.put("endTime", e.endTime);
                details.put("timeRemaining", Math.max(0, (e.endTime - System.currentTimeMillis()) / 1000 / 60));
                activeDetails.add(details);
            }
        }
        stats.put("activeElectionDetails", activeDetails);
        
        return stats;
    }
    
    /**
     * Get voter turnout percentage for an election.
     */
    public synchronized double getVoterTurnout(String nationId, String type) {
        Election e = getActiveElection(nationId, type);
        if (e == null) return 0.0;
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 0.0;
        
        int totalCitizens = n.getCitizens().size();
        if (totalCitizens == 0) return 0.0;
        
        return (e.votes.size() / (double) totalCitizens) * 100.0;
    }
    
    /**
     * Cancel an active election.
     */
    public synchronized String cancelElection(String nationId, String type) throws IOException {
        String key = nationId + "_" + type;
        Election e = activeElections.remove(key);
        if (e == null) return "Выборы не найдены.";
        
        File f = new File(electionsDir, key + ".json");
        if (f.exists()) f.delete();
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            long timestamp = System.currentTimeMillis();
            n.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Выборы отменены: " + getTypeName(type));
            nationManager.save(n);
        }
        
        return "Выборы отменены.";
    }
    
    /**
     * Get all active elections for a nation.
     */
    public synchronized List<Election> getActiveElections(String nationId) {
        List<Election> result = new ArrayList<>();
        for (Election e : activeElections.values()) {
            if (e.nationId.equals(nationId)) {
                result.add(e);
            }
        }
        return result;
    }
    
    /**
     * Get global election statistics.
     */
    public synchronized Map<String, Object> getGlobalElectionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeElections", activeElections.size());
        
        // Count elections by type
        Map<String, Integer> byType = new HashMap<>();
        for (Election e : activeElections.values()) {
            byType.put(e.type, byType.getOrDefault(e.type, 0) + 1);
        }
        stats.put("activeElectionsByType", byType);
        
        // Count all elections (from files)
        File[] files = electionsDir.listFiles((d, n) -> n.endsWith(".json"));
        int totalElections = files != null ? files.length : 0;
        stats.put("totalElections", totalElections);
        
        // Total votes cast
        int totalVotes = activeElections.values().stream()
            .mapToInt(e -> e.votes.size())
            .sum();
        stats.put("totalVotes", totalVotes);
        
        // Average voter turnout
        double totalTurnout = 0.0;
        int electionsWithVotes = 0;
        for (Election e : activeElections.values()) {
            double turnout = getVoterTurnout(e.nationId, e.type);
            if (turnout > 0) {
                totalTurnout += turnout;
                electionsWithVotes++;
            }
        }
        stats.put("averageVoterTurnout", electionsWithVotes > 0 ? totalTurnout / electionsWithVotes : 0);
        
        // Elections by nation
        Map<String, Integer> byNation = new HashMap<>();
        for (Election e : activeElections.values()) {
            byNation.put(e.nationId, byNation.getOrDefault(e.nationId, 0) + 1);
        }
        stats.put("activeElectionsByNation", byNation);
        
        return stats;
    }
}

