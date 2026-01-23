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
        if (isBlank(religionId) || isBlank(ritualId)) return "Неверные параметры.";
        if (performerId == null) return "Неверный исполнитель.";
        List<Ritual> rituals = religionRituals.get(religionId);
        if (rituals == null) return "Религия не найдена.";
        Ritual r = rituals.stream().filter(rit -> rit != null && ritualId.equals(rit.id)).findFirst().orElse(null);
        if (r == null) return "Ритуал не найден.";
        if (plugin.getPlayerDataManager() == null || plugin.getNationManager() == null) {
            return "Сервис наций недоступен.";
        }
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
        if (r == null || r.effects == null) return;
        for (var entry : r.effects.entrySet()) {
            String type = entry.getKey();
            double value = entry.getValue();
            switch (type) {
                case "happiness":
                    // Boost happiness for nation cities
                    break;
                case "economy":
                    if (plugin.getNationModifierService() != null) {
                        plugin.getNationModifierService().addModifier(nationId, "economy", "bonus", value, 1440);
                    }
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
                rit.id = o.has("id") ? o.get("id").getAsString() : null;
                rit.religionId = o.has("religionId") ? o.get("religionId").getAsString() : null;
                rit.name = o.has("name") ? o.get("name").getAsString() : null;
                rit.type = o.has("type") ? o.get("type").getAsString() : null;
                rit.cost = o.has("cost") ? o.get("cost").getAsDouble() : 0.0;
                if (o.has("effects")) {
                    JsonObject effects = o.getAsJsonObject("effects");
                    if (effects != null) {
                        for (var e : effects.entrySet()) {
                            rit.effects.put(e.getKey(), e.getValue().getAsDouble());
                        }
                    }
                }
                if (!isBlank(rit.religionId)) {
                    religionRituals.computeIfAbsent(rit.religionId, k -> new ArrayList<>()).add(rit);
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * Get comprehensive ritual statistics.
     */
    public synchronized Map<String, Object> getRitualStatistics(String religionId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(religionId)) return stats;
        List<Ritual> rituals = religionRituals.get(religionId);
        if (rituals == null) rituals = Collections.emptyList();
        
        stats.put("totalRituals", rituals.size());
        
        // Rituals by type
        Map<String, Integer> byType = new HashMap<>();
        for (Ritual r : rituals) {
            if (r != null && r.type != null) {
                byType.put(r.type, byType.getOrDefault(r.type, 0) + 1);
            }
        }
        stats.put("byType", byType);
        
        // Ritual details
        List<Map<String, Object>> ritualsList = new ArrayList<>();
        for (Ritual r : rituals) {
            if (r == null) continue;
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
        if (isBlank(religionId) || isBlank(name) || isBlank(type)) return "Неверные параметры.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        List<Ritual> rituals = religionRituals.computeIfAbsent(religionId, k -> new ArrayList<>());
        
        Ritual r = new Ritual();
        r.id = UUID.randomUUID().toString();
        r.religionId = religionId;
        r.name = name;
        r.type = type;
        r.cost = cost;
        r.effects = effects != null ? new HashMap<>(effects) : new HashMap<>();
        
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
            if (entry.getKey() != null && entry.getValue() != null) {
                effectsObj.addProperty(entry.getKey(), entry.getValue());
            }
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
        if (isBlank(religionId)) return Collections.emptyList();
        return new ArrayList<>(religionRituals.getOrDefault(religionId, Collections.emptyList()));
    }
    
    /**
     * Get ritual by ID.
     */
    public synchronized Ritual getRitual(String religionId, String ritualId) {
        if (isBlank(religionId) || isBlank(ritualId)) return null;
        List<Ritual> rituals = religionRituals.get(religionId);
        if (rituals == null) return null;
        return rituals.stream()
            .filter(r -> r != null && ritualId.equals(r.id))
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
            if (rituals == null) continue;
            int count = rituals.size();
            totalRituals += count;
            ritualsByReligion.put(entry.getKey(), count);
            
            for (Ritual r : rituals) {
                if (r == null) continue;
                if (r.type != null) {
                    ritualsByType.put(r.type, ritualsByType.getOrDefault(r.type, 0) + 1);
                }
                totalCost += r.cost;
                
                if (r.effects != null) {
                    for (String effectType : r.effects.keySet()) {
                        effectTypes.put(effectType, effectTypes.getOrDefault(effectType, 0) + 1);
                    }
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

