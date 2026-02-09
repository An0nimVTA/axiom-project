package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages economic crises and their effects. */
public class EconomicCrisisService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File crisesDir;
    private final Map<String, EconomicCrisis> activeCrises = new HashMap<>(); // nationId -> crisis

    public static class EconomicCrisis {
        String nationId;
        String type; // "recession", "depression", "hyperinflation", "bankruptcy"
        double severity; // 0-100
        long startedAt;
        long endsAt;
        boolean active;
    }

    public EconomicCrisisService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.crisesDir = new File(plugin.getDataFolder(), "economiccrises");
        this.crisesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processCrises, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String triggerCrisis(String nationId, String type, double severity, int durationDays) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (nationId == null || type == null || type.trim().isEmpty()) return "Неверные параметры.";
        if (durationDays <= 0) return "Длительность должна быть больше 0.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (activeCrises.containsKey(nationId)) return "Кризис уже активен.";
        EconomicCrisis crisis = new EconomicCrisis();
        crisis.nationId = nationId;
        crisis.type = type;
        crisis.severity = Math.max(0, Math.min(100, severity));
        crisis.startedAt = System.currentTimeMillis();
        crisis.endsAt = System.currentTimeMillis() + durationDays * 24 * 60 * 60_000L;
        crisis.active = true;
        activeCrises.put(nationId, crisis);
        // Apply crisis effects
        applyCrisisEffects(n, crisis);
        n.getHistory().add("Экономический кризис: " + type + " (тяжесть: " + severity + "%)");
        try {
            nationManager.save(n);
            saveCrisis(crisis);
        } catch (Exception ignored) {}
        return "Экономический кризис начался: " + type;
    }

    private void applyCrisisEffects(Nation n, EconomicCrisis crisis) {
        switch (crisis.type.toLowerCase()) {
            case "recession":
                n.setTreasury(n.getTreasury() * (1 - crisis.severity * 0.005)); // -0.5% per severity point
                if (plugin.getHappinessService() != null) {
                    plugin.getHappinessService().modifyHappiness(n.getId(), -crisis.severity * 0.2);
                }
                break;
            case "depression":
                n.setTreasury(n.getTreasury() * (1 - crisis.severity * 0.01)); // -1% per severity point
                if (plugin.getHappinessService() != null) {
                    plugin.getHappinessService().modifyHappiness(n.getId(), -crisis.severity * 0.3);
                }
                break;
            case "hyperinflation":
                n.setInflation(n.getInflation() + crisis.severity * 0.5);
                break;
            case "bankruptcy":
                n.setTreasury(0);
                if (plugin.getHappinessService() != null) {
                    plugin.getHappinessService().modifyHappiness(n.getId(), -50);
                }
                break;
        }
    }

    private synchronized void processCrises() {
        if (nationManager == null) return;
        long now = System.currentTimeMillis();
        List<String> ended = new ArrayList<>();
        for (Map.Entry<String, EconomicCrisis> e : activeCrises.entrySet()) {
            EconomicCrisis crisis = e.getValue();
            if (crisis.endsAt <= now) {
                ended.add(e.getKey());
            } else {
                // Continue applying effects
                Nation n = nationManager.getNationById(crisis.nationId);
                if (n != null) {
                    applyCrisisEffects(n, crisis);
                    try { nationManager.save(n); } catch (Exception ignored) {}
                }
            }
        }
        for (String nationId : ended) {
            EconomicCrisis crisis = activeCrises.remove(nationId);
            if (crisis == null) continue;
            crisis.active = false;
            saveCrisis(crisis);
            Nation n = nationManager.getNationById(nationId);
            if (n != null) {
                n.getHistory().add("Экономический кризис завершён.");
                try { nationManager.save(n); } catch (Exception ignored) {}
            }
        }
    }

    private void loadAll() {
        File[] files = crisesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                EconomicCrisis crisis = new EconomicCrisis();
                crisis.nationId = o.get("nationId").getAsString();
                crisis.type = o.get("type").getAsString();
                crisis.severity = o.get("severity").getAsDouble();
                crisis.startedAt = o.get("startedAt").getAsLong();
                crisis.endsAt = o.get("endsAt").getAsLong();
                crisis.active = o.has("active") && o.get("active").getAsBoolean() && crisis.endsAt > System.currentTimeMillis();
                if (crisis.active) activeCrises.put(crisis.nationId, crisis);
            } catch (Exception ignored) {}
        }
    }

    private void saveCrisis(EconomicCrisis crisis) {
        File f = new File(crisesDir, crisis.nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", crisis.nationId);
        o.addProperty("type", crisis.type);
        o.addProperty("severity", crisis.severity);
        o.addProperty("startedAt", crisis.startedAt);
        o.addProperty("endsAt", crisis.endsAt);
        o.addProperty("active", crisis.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive economic crisis statistics for a nation.
     */
    public synchronized Map<String, Object> getEconomicCrisisStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        EconomicCrisis crisis = activeCrises.get(nationId);
        if (crisis == null || !crisis.active) {
            stats.put("hasActiveCrisis", false);
            return stats;
        }
        
        long now = System.currentTimeMillis();
        long remainingDays = Math.max(0, (crisis.endsAt - now) / (24 * 60 * 60 * 1000L));
        
        stats.put("hasActiveCrisis", true);
        stats.put("type", crisis.type);
        stats.put("severity", crisis.severity);
        stats.put("startedAt", crisis.startedAt);
        stats.put("endsAt", crisis.endsAt);
        stats.put("remainingDays", remainingDays);
        
        // Crisis rating
        String rating = "КРИТИЧЕСКИЙ";
        if (crisis.severity >= 80) rating = "КАТАСТРОФИЧЕСКИЙ";
        else if (crisis.severity >= 60) rating = "ТЯЖЕЛЫЙ";
        else if (crisis.severity >= 40) rating = "СРЕДНИЙ";
        else if (crisis.severity >= 20) rating = "ЛЕГКИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global economic crisis statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalEconomicCrisisStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long now = System.currentTimeMillis();
        int totalActive = 0;
        double totalSeverity = 0.0;
        Map<String, Integer> crisesByNation = new HashMap<>();
        Map<String, Integer> crisesByType = new HashMap<>();
        Map<String, Double> severityByNation = new HashMap<>();
        
        for (EconomicCrisis crisis : activeCrises.values()) {
            if (crisis.active && now < crisis.endsAt) {
                totalActive++;
                totalSeverity += crisis.severity;
                
                crisesByNation.put(crisis.nationId,
                    crisesByNation.getOrDefault(crisis.nationId, 0) + 1);
                crisesByType.put(crisis.type,
                    crisesByType.getOrDefault(crisis.type, 0) + 1);
                severityByNation.put(crisis.nationId, crisis.severity);
            }
        }
        
        stats.put("totalActiveCrises", totalActive);
        stats.put("averageSeverity", totalActive > 0 ? totalSeverity / totalActive : 0);
        stats.put("crisesByNation", crisesByNation);
        stats.put("crisesByType", crisesByType);
        stats.put("severityByNation", severityByNation);
        stats.put("nationsWithCrises", crisesByNation.size());
        
        // Top nations by crisis severity
        List<Map.Entry<String, Double>> topBySeverity = severityByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySeverity", topBySeverity);
        
        // Most common crisis types
        List<Map.Entry<String, Integer>> topByType = crisesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByType", topByType);
        
        // Crisis severity distribution
        int catastrophic = 0, severe = 0, moderate = 0, mild = 0;
        for (Double severity : severityByNation.values()) {
            if (severity >= 80) catastrophic++;
            else if (severity >= 60) severe++;
            else if (severity >= 40) moderate++;
            else mild++;
        }
        
        Map<String, Integer> severityDistribution = new HashMap<>();
        severityDistribution.put("catastrophic", catastrophic);
        severityDistribution.put("severe", severe);
        severityDistribution.put("moderate", moderate);
        severityDistribution.put("mild", mild);
        stats.put("severityDistribution", severityDistribution);
        
        return stats;
    }
}

