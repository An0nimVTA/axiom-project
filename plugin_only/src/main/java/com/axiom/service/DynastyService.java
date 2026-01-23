package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages dynasties and family lines for nations. */
public class DynastyService {
    private final AXIOM plugin;
    private final File dynastiesDir;
    private final Map<String, Dynasty> nationDynasties = new HashMap<>(); // nationId -> dynasty

    public static class Dynasty {
        String id;
        String name;
        String nationId;
        UUID founder;
        List<UUID> members = new ArrayList<>();
        int generation = 1;
    }

    public DynastyService(AXIOM plugin) {
        this.plugin = plugin;
        this.dynastiesDir = new File(plugin.getDataFolder(), "dynasties");
        this.dynastiesDir.mkdirs();
        loadAll();
    }

    public synchronized String createDynasty(String nationId, String name, UUID founder) throws IOException {
        if (nationDynasties.containsKey(nationId)) return "Династия уже существует.";
        Dynasty d = new Dynasty();
        d.id = UUID.randomUUID().toString();
        d.name = name;
        d.nationId = nationId;
        d.founder = founder;
        d.members.add(founder);
        nationDynasties.put(nationId, d);
        saveDynasty(d);
        return "Династия создана: " + name;
    }

    public synchronized String addMember(String nationId, UUID member) throws IOException {
        Dynasty d = nationDynasties.get(nationId);
        if (d == null) return "Династия не найдена.";
        if (d.members.contains(member)) return "Уже член династии.";
        d.members.add(member);
        saveDynasty(d);
        return "Член добавлен в династию.";
    }

    public synchronized Dynasty getDynasty(String nationId) {
        return nationDynasties.get(nationId);
    }

    private void loadAll() {
        File[] files = dynastiesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Dynasty d = new Dynasty();
                d.id = o.get("id").getAsString();
                d.name = o.get("name").getAsString();
                d.nationId = o.get("nationId").getAsString();
                d.founder = UUID.fromString(o.get("founder").getAsString());
                d.generation = o.has("generation") ? o.get("generation").getAsInt() : 1;
                if (o.has("members")) {
                    JsonArray arr = o.getAsJsonArray("members");
                    for (var e : arr) d.members.add(UUID.fromString(e.getAsString()));
                }
                nationDynasties.put(d.nationId, d);
            } catch (Exception ignored) {}
        }
    }

    private void saveDynasty(Dynasty d) throws IOException {
        File f = new File(dynastiesDir, d.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", d.id);
        o.addProperty("name", d.name);
        o.addProperty("nationId", d.nationId);
        o.addProperty("founder", d.founder.toString());
        o.addProperty("generation", d.generation);
        JsonArray arr = new JsonArray();
        for (UUID m : d.members) arr.add(m.toString());
        o.add("members", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive dynasty statistics.
     */
    public synchronized Map<String, Object> getDynastyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Dynasty d = nationDynasties.get(nationId);
        if (d == null) {
            stats.put("hasDynasty", false);
            return stats;
        }
        
        stats.put("hasDynasty", true);
        stats.put("id", d.id);
        stats.put("name", d.name);
        stats.put("founder", d.founder.toString());
        stats.put("generation", d.generation);
        stats.put("memberCount", d.members.size());
        
        // Get founder name
        org.bukkit.OfflinePlayer founderPlayer = org.bukkit.Bukkit.getOfflinePlayer(d.founder);
        stats.put("founderName", founderPlayer.getName());
        
        // Dynasty rating
        String rating = "НАЧАЛЬНАЯ";
        if (d.members.size() >= 20) rating = "ВЕЛИКАЯ ДИНАСТИЯ";
        else if (d.members.size() >= 10) rating = "ЗНАЧИТЕЛЬНАЯ";
        else if (d.members.size() >= 5) rating = "РАЗВИТАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Remove member from dynasty.
     */
    public synchronized String removeMember(String nationId, UUID member) throws IOException {
        Dynasty d = nationDynasties.get(nationId);
        if (d == null) return "Династия не найдена.";
        if (!d.members.contains(member)) return "Не является членом династии.";
        if (d.founder.equals(member)) return "Основатель не может быть удалён.";
        
        d.members.remove(member);
        saveDynasty(d);
        
        return "Член удалён из династии.";
    }
    
    /**
     * Promote generation.
     */
    public synchronized String promoteGeneration(String nationId) throws IOException {
        Dynasty d = nationDynasties.get(nationId);
        if (d == null) return "Династия не найдена.";
        
        d.generation++;
        saveDynasty(d);
        
        return "Поколение продвинуто: " + d.generation;
    }
    
    /**
     * Check if player is in dynasty.
     */
    public synchronized boolean isMemberOfDynasty(UUID playerId, String nationId) {
        Dynasty d = nationDynasties.get(nationId);
        return d != null && d.members.contains(playerId);
    }
    
    /**
     * Get all dynasties.
     */
    public synchronized List<Dynasty> getAllDynasties() {
        return new ArrayList<>(nationDynasties.values());
    }
    
    /**
     * Get global dynasty statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalDynastyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalDynasties = nationDynasties.size();
        int totalMembers = 0;
        int totalGenerations = 0;
        Map<String, Integer> dynastiesByGeneration = new HashMap<>();
        Map<String, Integer> membersByDynasty = new HashMap<>();
        
        for (Dynasty d : nationDynasties.values()) {
            totalMembers += d.members.size();
            totalGenerations += d.generation;
            dynastiesByGeneration.put(String.valueOf(d.generation), 
                dynastiesByGeneration.getOrDefault(String.valueOf(d.generation), 0) + 1);
            membersByDynasty.put(d.id, d.members.size());
        }
        
        stats.put("totalDynasties", totalDynasties);
        stats.put("totalMembers", totalMembers);
        stats.put("averageMembers", totalDynasties > 0 ? (double) totalMembers / totalDynasties : 0);
        stats.put("averageGeneration", totalDynasties > 0 ? (double) totalGenerations / totalDynasties : 0);
        stats.put("dynastiesByGeneration", dynastiesByGeneration);
        
        // Top dynasties by members
        List<Map.Entry<String, Integer>> topByMembers = membersByDynasty.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMembers", topByMembers);
        
        // Generation distribution
        int gen1 = 0, gen2 = 0, gen3 = 0, gen4 = 0, gen5plus = 0;
        for (Dynasty d : nationDynasties.values()) {
            if (d.generation == 1) gen1++;
            else if (d.generation == 2) gen2++;
            else if (d.generation == 3) gen3++;
            else if (d.generation == 4) gen4++;
            else gen5plus++;
        }
        
        Map<String, Integer> generationDistribution = new HashMap<>();
        generationDistribution.put("generation1", gen1);
        generationDistribution.put("generation2", gen2);
        generationDistribution.put("generation3", gen3);
        generationDistribution.put("generation4", gen4);
        generationDistribution.put("generation5plus", gen5plus);
        stats.put("generationDistribution", generationDistribution);
        
        // Nations with dynasties
        int nationsWithDynasties = nationDynasties.size();
        int totalNations = plugin.getNationManager().getAll().size();
        stats.put("nationsWithDynasties", nationsWithDynasties);
        stats.put("dynastyAdoptionRate", totalNations > 0 ? 
            (nationsWithDynasties / (double) totalNations) * 100 : 0);
        
        return stats;
    }
}

