package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages religious rituals and ceremonies. */
public class RitualService {
    private final AXIOM plugin;
    private final File ritualsDir;
    private final Map<String, List<Ritual>> religionRituals = new HashMap<>(); // religionId -> rituals

    public static class Ritual {
        String id;
        String religionId;
        String name;
        String type; // "blessing", "sacrifice", "prayer"
        double cost;
        Map<String, Double> effects = new HashMap<>(); // effect type -> value
    }

    public RitualService(AXIOM plugin) {
        this.plugin = plugin;
        this.ritualsDir = new File(plugin.getDataFolder(), "rituals");
        this.ritualsDir.mkdirs();
        loadAll();
    }

    public synchronized String performRitual(String religionId, String ritualId, UUID performerId) {
        List<Ritual> rituals = religionRituals.get(religionId);
        if (rituals == null) return "Религия не найдена.";
        Ritual r = rituals.stream().filter(rit -> rit.id.equals(ritualId)).findFirst().orElse(null);
        if (r == null) return "Ритуал не найден.";
        String playerNationId = plugin.getPlayerDataManager().getNation(performerId);
        if (playerNationId == null) return "Вы не в нации.";
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(playerNationId);
        if (n == null || n.getTreasury() < r.cost) return "Недостаточно средств.";
        n.setTreasury(n.getTreasury() - r.cost);
        applyRitualEffects(playerNationId, r);
        try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        return "Ритуал выполнен: " + r.name;
    }

    private void applyRitualEffects(String nationId, Ritual r) {
        for (var entry : r.effects.entrySet()) {
            String type = entry.getKey();
            double value = entry.getValue();
            switch (type) {
                case "happiness":
                    // Boost happiness for nation cities
                    break;
                case "economy":
                    plugin.getNationModifierService().addModifier(nationId, "economy", "bonus", value, 1440);
                    break;
            }
        }
    }

    private void loadAll() {
        File[] files = ritualsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Ritual rit = new Ritual();
                rit.id = o.get("id").getAsString();
                rit.religionId = o.get("religionId").getAsString();
                rit.name = o.get("name").getAsString();
                rit.type = o.get("type").getAsString();
                rit.cost = o.get("cost").getAsDouble();
                if (o.has("effects")) {
                    JsonObject effects = o.getAsJsonObject("effects");
                    for (var e : effects.entrySet()) {
                        rit.effects.put(e.getKey(), e.getValue().getAsDouble());
                    }
                }
                religionRituals.computeIfAbsent(rit.religionId, k -> new ArrayList<>()).add(rit);
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * Get comprehensive ritual statistics.
     */
    public synchronized Map<String, Object> getRitualStatistics(String religionId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Ritual> rituals = religionRituals.get(religionId);
        if (rituals == null) rituals = Collections.emptyList();
        
        stats.put("totalRituals", rituals.size());
        
        // Rituals by type
        Map<String, Integer> byType = new HashMap<>();
        for (Ritual r : rituals) {
            byType.put(r.type, byType.getOrDefault(r.type, 0) + 1);
        }
        stats.put("byType", byType);
        
        // Ritual details
        List<Map<String, Object>> ritualsList = new ArrayList<>();
        for (Ritual r : rituals) {
            Map<String, Object> ritualData = new HashMap<>();
            ritualData.put("id", r.id);
            ritualData.put("name", r.name);
            ritualData.put("type", r.type);
            ritualData.put("cost", r.cost);
            ritualData.put("effects", new HashMap<>(r.effects));
            ritualsList.add(ritualData);
        }
        stats.put("rituals", ritualsList);
        
        // Average cost
        double avgCost = rituals.stream()
            .mapToDouble(r -> r.cost)
            .average()
            .orElse(0.0);
        stats.put("averageCost", avgCost);
        
        // Ritual rating
        String rating = "НЕТ РИТУАЛОВ";
        if (rituals.size() >= 10) rating = "ОБШИРНЫЙ РЕПЕРТУАР";
        else if (rituals.size() >= 5) rating = "РАЗВИТЫЙ";
        else if (rituals.size() >= 3) rating = "СТАБИЛЬНЫЙ";
        else if (rituals.size() >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Create ritual.
     */
    public synchronized String createRitual(String religionId, String name, String type, double cost, Map<String, Double> effects) throws IOException {
        List<Ritual> rituals = religionRituals.computeIfAbsent(religionId, k -> new ArrayList<>());
        
        Ritual r = new Ritual();
        r.id = UUID.randomUUID().toString();
        r.religionId = religionId;
        r.name = name;
        r.type = type;
        r.cost = cost;
        r.effects = new HashMap<>(effects);
        
        rituals.add(r);
        
        // Save ritual
        File f = new File(ritualsDir, r.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", r.id);
        o.addProperty("religionId", r.religionId);
        o.addProperty("name", r.name);
        o.addProperty("type", r.type);
        o.addProperty("cost", r.cost);
        JsonObject effectsObj = new JsonObject();
        for (Map.Entry<String, Double> entry : r.effects.entrySet()) {
            effectsObj.addProperty(entry.getKey(), entry.getValue());
        }
        o.add("effects", effectsObj);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
        
        return "Ритуал создан: " + name;
    }
    
    /**
     * Get all rituals for religion.
     */
    public synchronized List<Ritual> getReligionRituals(String religionId) {
        return religionRituals.getOrDefault(religionId, Collections.emptyList());
    }
    
    /**
     * Get ritual by ID.
     */
    public synchronized Ritual getRitual(String religionId, String ritualId) {
        List<Ritual> rituals = religionRituals.get(religionId);
        if (rituals == null) return null;
        return rituals.stream()
            .filter(r -> r.id.equals(ritualId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get global ritual statistics across all religions.
     */
    public synchronized Map<String, Object> getGlobalRitualStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalRituals = 0;
        Map<String, Integer> ritualsByReligion = new HashMap<>();
        Map<String, Integer> ritualsByType = new HashMap<>();
        double totalCost = 0.0;
        Map<String, Integer> effectTypes = new HashMap<>();
        
        for (Map.Entry<String, List<Ritual>> entry : religionRituals.entrySet()) {
            List<Ritual> rituals = entry.getValue();
            int count = rituals.size();
            totalRituals += count;
            ritualsByReligion.put(entry.getKey(), count);
            
            for (Ritual r : rituals) {
                ritualsByType.put(r.type, ritualsByType.getOrDefault(r.type, 0) + 1);
                totalCost += r.cost;
                
                for (String effectType : r.effects.keySet()) {
                    effectTypes.put(effectType, effectTypes.getOrDefault(effectType, 0) + 1);
                }
            }
        }
        
        stats.put("totalRituals", totalRituals);
        stats.put("ritualsByReligion", ritualsByReligion);
        stats.put("ritualsByType", ritualsByType);
        stats.put("totalCost", totalCost);
        stats.put("averageCost", totalRituals > 0 ? totalCost / totalRituals : 0);
        stats.put("religionsWithRituals", ritualsByReligion.size());
        stats.put("effectTypes", effectTypes);
        
        // Top religions by rituals
        List<Map.Entry<String, Integer>> topByRituals = ritualsByReligion.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRituals", topByRituals);
        
        // Most common ritual types
        List<Map.Entry<String, Integer>> mostCommonTypes = ritualsByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTypes", mostCommonTypes);
        
        // Average rituals per religion
        stats.put("averageRitualsPerReligion", ritualsByReligion.size() > 0 ? 
            (double) totalRituals / ritualsByReligion.size() : 0);
        
        return stats;
    }
}

