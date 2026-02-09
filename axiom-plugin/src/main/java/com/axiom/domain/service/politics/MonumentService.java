package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages monuments and landmarks for nations. */
public class MonumentService {
    private final AXIOM plugin;
    private final File monumentsDir;
    private final Map<String, List<Monument>> nationMonuments = new HashMap<>(); // nationId -> monuments

    public static class Monument {
        String id;
        String nationId;
        String name;
        String type; // "statue", "building", "landmark"
        String chunkKey;
        double prestigeBonus;
        long builtAt;
    }

    public MonumentService(AXIOM plugin) {
        this.plugin = plugin;
        this.monumentsDir = new File(plugin.getDataFolder(), "monuments");
        this.monumentsDir.mkdirs();
        loadAll();
    }

    public synchronized String buildMonument(String nationId, String name, String type, String chunkKey, double cost) throws IOException {
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(name) || isBlank(type) || isBlank(chunkKey)) return "Некорректные данные.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        com.axiom.domain.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        String id = UUID.randomUUID().toString();
        Monument m = new Monument();
        m.id = id;
        m.nationId = nationId;
        m.name = name;
        m.type = type;
        m.chunkKey = chunkKey;
        String lowerType = type.toLowerCase();
        m.prestigeBonus = lowerType.equals("statue") ? 5.0 : (lowerType.equals("building") ? 10.0 : 3.0);
        m.builtAt = System.currentTimeMillis();
        n.setTreasury(n.getTreasury() - cost);
        nationMonuments.computeIfAbsent(nationId, k -> new ArrayList<>()).add(m);
        plugin.getNationManager().save(n);
        saveMonument(m);
        return "Памятник построен: " + name + " (+" + m.prestigeBonus + " престиж)";
    }

    public synchronized double getPrestigeBonus(String nationId) {
        if (isBlank(nationId)) return 0.0;
        List<Monument> monuments = nationMonuments.get(nationId);
        if (monuments == null) return 0.0;
        return monuments.stream().mapToDouble(m -> m.prestigeBonus).sum();
    }

    private void loadAll() {
        File[] files = monumentsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Monument m = new Monument();
                m.id = o.get("id").getAsString();
                m.nationId = o.get("nationId").getAsString();
                m.name = o.get("name").getAsString();
                m.type = o.get("type").getAsString();
                m.chunkKey = o.get("chunkKey").getAsString();
                m.prestigeBonus = o.get("prestigeBonus").getAsDouble();
                m.builtAt = o.get("builtAt").getAsLong();
                nationMonuments.computeIfAbsent(m.nationId, k -> new ArrayList<>()).add(m);
            } catch (Exception ignored) {}
        }
    }

    private void saveMonument(Monument m) throws IOException {
        File f = new File(monumentsDir, m.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", m.id);
        o.addProperty("nationId", m.nationId);
        o.addProperty("name", m.name);
        o.addProperty("type", m.type);
        o.addProperty("chunkKey", m.chunkKey);
        o.addProperty("prestigeBonus", m.prestigeBonus);
        o.addProperty("builtAt", m.builtAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive monument statistics.
     */
    public synchronized Map<String, Object> getMonumentStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Monument> monuments = nationMonuments.get(nationId);
        if (monuments == null) monuments = Collections.emptyList();
        
        stats.put("totalMonuments", monuments.size());
        stats.put("totalPrestigeBonus", getPrestigeBonus(nationId));
        
        // Monuments by type
        Map<String, Integer> byType = new HashMap<>();
        for (Monument m : monuments) {
            byType.put(m.type, byType.getOrDefault(m.type, 0) + 1);
        }
        stats.put("byType", byType);
        
        // Monument details
        List<Map<String, Object>> monumentsList = new ArrayList<>();
        for (Monument m : monuments) {
            Map<String, Object> monumentData = new HashMap<>();
            monumentData.put("id", m.id);
            monumentData.put("name", m.name);
            monumentData.put("type", m.type);
            monumentData.put("prestigeBonus", m.prestigeBonus);
            monumentData.put("chunkKey", m.chunkKey);
            monumentData.put("age", (System.currentTimeMillis() - m.builtAt) / (1000 * 60 * 60 * 24)); // days
            monumentsList.add(monumentData);
        }
        stats.put("monuments", monumentsList);
        
        // Prestige rating
        double totalPrestige = getPrestigeBonus(nationId);
        String rating = "НИЗКИЙ";
        if (totalPrestige >= 100) rating = "ЛЕГЕНДАРНЫЙ";
        else if (totalPrestige >= 50) rating = "ВЕЛИКИЙ";
        else if (totalPrestige >= 25) rating = "ЗНАЧИТЕЛЬНЫЙ";
        else if (totalPrestige >= 10) rating = "СРЕДНИЙ";
        stats.put("prestigeRating", rating);
        
        return stats;
    }
    
    /**
     * Upgrade monument (increase prestige bonus).
     */
    public synchronized String upgradeMonument(String nationId, String monumentId, double cost) throws IOException {
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(monumentId)) return "Некорректные данные.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        com.axiom.domain.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        
        List<Monument> monuments = nationMonuments.get(nationId);
        if (monuments == null) return "Памятники не найдены.";
        
        Monument monument = monuments.stream()
            .filter(m -> m.id.equals(monumentId))
            .findFirst()
            .orElse(null);
        
        if (monument == null) return "Памятник не найден.";
        
        monument.prestigeBonus += 2.0; // +2 prestige per upgrade
        n.setTreasury(n.getTreasury() - cost);
        
        plugin.getNationManager().save(n);
        saveMonument(monument);
        
        return "Памятник улучшен. Новый бонус престижа: " + String.format("%.1f", monument.prestigeBonus);
    }
    
    /**
     * Destroy monument.
     */
    public synchronized String destroyMonument(String nationId, String monumentId) throws IOException {
        if (isBlank(nationId) || isBlank(monumentId)) return "Некорректные данные.";
        List<Monument> monuments = nationMonuments.get(nationId);
        if (monuments == null) return "Памятники не найдены.";
        
        Monument monument = monuments.stream()
            .filter(m -> m.id.equals(monumentId))
            .findFirst()
            .orElse(null);
        
        if (monument == null) return "Памятник не найден.";
        
        monuments.remove(monument);
        
        // Delete file
        File f = new File(monumentsDir, monumentId + ".json");
        if (f.exists()) f.delete();
        
        return "Памятник разрушен.";
    }
    
    /**
     * Get all monuments for a nation.
     */
    public synchronized List<Monument> getNationMonuments(String nationId) {
        if (isBlank(nationId)) return Collections.emptyList();
        List<Monument> monuments = nationMonuments.get(nationId);
        return monuments != null ? new ArrayList<>(monuments) : Collections.emptyList();
    }
    
    /**
     * Calculate total cultural impact from monuments.
     */
    public synchronized double getCulturalImpact(String nationId) {
        double prestige = getPrestigeBonus(nationId);
        // Prestige affects culture level
        return prestige * 0.5; // +0.5 culture per prestige point
    }
    
    /**
     * Get global monument statistics.
     */
    public synchronized Map<String, Object> getGlobalMonumentStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalMonuments = 0;
        double totalPrestige = 0.0;
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byNation = new HashMap<>();
        
        for (Map.Entry<String, List<Monument>> entry : nationMonuments.entrySet()) {
            for (Monument m : entry.getValue()) {
                totalMonuments++;
                totalPrestige += m.prestigeBonus;
                byType.put(m.type, byType.getOrDefault(m.type, 0) + 1);
                byNation.put(m.nationId, byNation.getOrDefault(m.nationId, 0) + 1);
            }
        }
        
        stats.put("totalMonuments", totalMonuments);
        stats.put("totalPrestige", totalPrestige);
        stats.put("averagePrestigePerMonument", totalMonuments > 0 ? totalPrestige / totalMonuments : 0);
        stats.put("monumentsByType", byType);
        stats.put("monumentsByNation", byNation);
        
        // Top nations by monuments
        List<Map.Entry<String, Integer>> topByMonuments = byNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMonuments", topByMonuments);
        
        // Top nations by prestige
        Map<String, Double> prestigeByNation = new HashMap<>();
        for (Map.Entry<String, List<Monument>> entry : nationMonuments.entrySet()) {
            double prestige = entry.getValue().stream().mapToDouble(m -> m.prestigeBonus).sum();
            prestigeByNation.put(entry.getKey(), prestige);
        }
        List<Map.Entry<String, Double>> topByPrestige = prestigeByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPrestige", topByPrestige);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

