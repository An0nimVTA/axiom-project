package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages diplomatic treaties (NAP, trade agreements, etc.). */
public class TreatyService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File treatiesDir;
    private final Map<String, Treaty> activeTreaties = new HashMap<>(); // treatyId -> treaty

    public static class Treaty {
        String id;
        String nationA;
        String nationB;
        String type; // "nap" (Non-Aggression Pact), "trade", "military"
        long signedAt;
        long expiresAt; // -1 = indefinite
        Map<String, String> terms = new HashMap<>();
    }

    public TreatyService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.treatiesDir = new File(plugin.getDataFolder(), "treaties");
        this.treatiesDir.mkdirs();
        loadAll();
    }

    public synchronized String createTreaty(String nationAId, String nationBId, String type, long durationDays) throws IOException {
        if (nationManager == null) return "Сервис недоступен.";
        if (isBlank(nationAId) || isBlank(nationBId) || isBlank(type)) return "Некорректные данные.";
        if (nationAId.equals(nationBId)) return "Нельзя заключить договор с собой.";
        if (durationDays < 0) return "Некорректная длительность.";
        Nation a = nationManager.getNationById(nationAId);
        Nation b = nationManager.getNationById(nationBId);
        if (a == null || b == null) return "Нация не найдена.";
        String id = nationAId + "_" + nationBId + "_" + type;
        if (activeTreaties.containsKey(id)) return "Договор уже существует.";
        Treaty t = new Treaty();
        t.id = id;
        t.nationA = nationAId;
        t.nationB = nationBId;
        t.type = type;
        t.signedAt = System.currentTimeMillis();
        t.expiresAt = durationDays > 0 ? t.signedAt + (durationDays * 24L * 60L * 60L * 1000L) : -1;
        activeTreaties.put(id, t);
        saveTreaty(t);
        String typeName = getTypeName(type);
        a.getHistory().add(java.time.Instant.ofEpochMilli(t.signedAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Договор " + typeName + " с " + b.getName());
        b.getHistory().add(java.time.Instant.ofEpochMilli(t.signedAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Договор " + typeName + " с " + a.getName());
        nationManager.save(a);
        nationManager.save(b);
        return "Договор создан: " + typeName + " (" + (durationDays > 0 ? durationDays + " дней" : "бессрочно") + ")";
    }

    public synchronized boolean hasTreaty(String nationAId, String nationBId, String type) {
        if (isBlank(nationAId) || isBlank(nationBId) || isBlank(type)) return false;
        String id1 = nationAId + "_" + nationBId + "_" + type;
        String id2 = nationBId + "_" + nationAId + "_" + type;
        Treaty t = activeTreaties.get(id1);
        if (t == null) t = activeTreaties.get(id2);
        if (t == null) return false;
        if (t.expiresAt > 0 && t.expiresAt < System.currentTimeMillis()) {
            activeTreaties.remove(id1);
            activeTreaties.remove(id2);
            return false;
        }
        return true;
    }

    private String getTypeName(String type) {
        switch (type.toLowerCase()) {
            case "nap": return "Пакт о ненападении";
            case "trade": return "Торговое соглашение";
            case "military": return "Военный договор";
            default: return type;
        }
    }

    private void loadAll() {
        File[] files = treatiesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Treaty t = new Treaty();
                t.id = o.get("id").getAsString();
                t.nationA = o.get("nationA").getAsString();
                t.nationB = o.get("nationB").getAsString();
                t.type = o.get("type").getAsString();
                t.signedAt = o.get("signedAt").getAsLong();
                t.expiresAt = o.has("expiresAt") ? o.get("expiresAt").getAsLong() : -1;
                if (t.expiresAt < 0 || t.expiresAt > System.currentTimeMillis()) {
                    activeTreaties.put(t.id, t);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveTreaty(Treaty t) throws IOException {
        File f = new File(treatiesDir, t.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", t.id);
        o.addProperty("nationA", t.nationA);
        o.addProperty("nationB", t.nationB);
        o.addProperty("type", t.type);
        o.addProperty("signedAt", t.signedAt);
        o.addProperty("expiresAt", t.expiresAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive treaty statistics.
     */
    public synchronized Map<String, Object> getTreatyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        if (isBlank(nationId)) {
            stats.put("totalTreaties", 0);
            stats.put("byType", new HashMap<String, Integer>());
            stats.put("treaties", new ArrayList<Map<String, Object>>());
            return stats;
        }
        
        int totalTreaties = 0;
        Map<String, Integer> byType = new HashMap<>();
        List<Map<String, Object>> treatiesList = new ArrayList<>();
        
        long now = System.currentTimeMillis();
        for (Treaty t : activeTreaties.values()) {
            if (t.expiresAt > 0 && t.expiresAt < now) continue; // Skip expired
            
            if (t.nationA.equals(nationId) || t.nationB.equals(nationId)) {
                totalTreaties++;
                byType.put(t.type, byType.getOrDefault(t.type, 0) + 1);
                
                Map<String, Object> treatyData = new HashMap<>();
                treatyData.put("id", t.id);
                treatyData.put("partner", t.nationA.equals(nationId) ? t.nationB : t.nationA);
                treatyData.put("type", t.type);
                treatyData.put("typeName", getTypeName(t.type));
                treatyData.put("signedAt", t.signedAt);
                treatyData.put("expiresAt", t.expiresAt);
                treatyData.put("timeRemaining", t.expiresAt > 0 ? Math.max(0, (t.expiresAt - now) / 1000 / 60 / 60 / 24) : -1);
                treatiesList.add(treatyData);
            }
        }
        
        stats.put("totalTreaties", totalTreaties);
        stats.put("byType", byType);
        stats.put("treaties", treatiesList);
        
        return stats;
    }
    
    /**
     * Cancel/terminate a treaty.
     */
    public synchronized String terminateTreaty(String nationId, String partnerId, String type) throws IOException {
        if (isBlank(nationId) || isBlank(partnerId) || isBlank(type)) return "Некорректные данные.";
        String id1 = nationId + "_" + partnerId + "_" + type;
        String id2 = partnerId + "_" + nationId + "_" + type;
        
        Treaty t = activeTreaties.remove(id1);
        if (t == null) t = activeTreaties.remove(id2);
        if (t == null) return "Договор не найден.";
        
        // Delete file
        File f = new File(treatiesDir, t.id + ".json");
        if (f.exists()) f.delete();
        
        if (nationManager != null) {
            Nation n1 = nationManager.getNationById(nationId);
            Nation n2 = nationManager.getNationById(partnerId);
            if (n1 != null && n2 != null) {
                long timestamp = System.currentTimeMillis();
                String typeName = getTypeName(type);
                n1.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Договор " + typeName + " с " + n2.getName() + " расторгнут");
                n2.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Договор " + typeName + " с " + n1.getName() + " расторгнут");
                nationManager.save(n1);
                nationManager.save(n2);
            }
        }
        
        return "Договор расторгнут.";
    }
    
    /**
     * Get all treaties for a nation.
     */
    public synchronized List<Treaty> getNationTreaties(String nationId) {
        List<Treaty> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        for (Treaty t : activeTreaties.values()) {
            if (t.expiresAt > 0 && t.expiresAt < now) continue; // Skip expired
            
            if (t.nationA.equals(nationId) || t.nationB.equals(nationId)) {
                result.add(t);
            }
        }
        
        return result;
    }
    
    /**
     * Renew/extend treaty duration.
     */
    public synchronized String renewTreaty(String nationId, String partnerId, String type, long additionalDays) throws IOException {
        if (isBlank(nationId) || isBlank(partnerId) || isBlank(type)) return "Некорректные данные.";
        if (additionalDays <= 0) return "Некорректная длительность.";
        String id1 = nationId + "_" + partnerId + "_" + type;
        String id2 = partnerId + "_" + nationId + "_" + type;
        
        Treaty t = activeTreaties.get(id1);
        if (t == null) t = activeTreaties.get(id2);
        if (t == null) return "Договор не найден.";
        
        if (t.expiresAt > 0) {
            t.expiresAt += additionalDays * 24L * 60L * 60L * 1000L;
        } else {
            t.expiresAt = System.currentTimeMillis() + (additionalDays * 24L * 60L * 60L * 1000L);
        }
        
        saveTreaty(t);
        
        return "Договор продлён на " + additionalDays + " дней.";
    }
    
    /**
     * Get global treaty statistics.
     */
    public synchronized Map<String, Object> getGlobalTreatyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        if (nationManager == null) return stats;
        
        long now = System.currentTimeMillis();
        int activeCount = 0;
        int expiredCount = 0;
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byNation = new HashMap<>();
        
        for (Treaty t : activeTreaties.values()) {
            if (t.expiresAt > 0 && t.expiresAt < now) {
                expiredCount++;
                continue;
            }
            
            activeCount++;
            byType.put(t.type, byType.getOrDefault(t.type, 0) + 1);
            byNation.put(t.nationA, byNation.getOrDefault(t.nationA, 0) + 1);
            byNation.put(t.nationB, byNation.getOrDefault(t.nationB, 0) + 1);
        }
        
        stats.put("totalTreaties", activeTreaties.size());
        stats.put("activeTreaties", activeCount);
        stats.put("expiredTreaties", expiredCount);
        stats.put("treatiesByType", byType);
        
        // Top nations by treaty count
        List<Map.Entry<String, Integer>> topByTreaties = byNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByTreaties", topByTreaties);
        
        // Average treaties per nation
        stats.put("averageTreatiesPerNation", nationManager.getAll().size() > 0 ? 
            (double) activeCount / nationManager.getAll().size() : 0);
        
        // Treaty age statistics
        List<Long> ages = new ArrayList<>();
        for (Treaty t : activeTreaties.values()) {
            if (t.expiresAt < 0 || t.expiresAt > now) {
                ages.add((now - t.signedAt) / 1000 / 60 / 60 / 24); // days
            }
        }
        if (!ages.isEmpty()) {
            stats.put("averageTreatyAge", ages.stream().mapToLong(Long::longValue).average().orElse(0));
            stats.put("oldestTreaty", ages.stream().mapToLong(Long::longValue).max().orElse(0));
        }
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

