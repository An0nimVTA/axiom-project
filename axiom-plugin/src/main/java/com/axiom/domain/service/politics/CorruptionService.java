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

/** Tracks corruption levels in nations. */
public class CorruptionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File corruptionDir;
    private final Map<String, CorruptionData> nationCorruption = new HashMap<>(); // nationId -> data

    public static class CorruptionData {
        double level; // 0-100
        List<CorruptionIncident> incidents = new ArrayList<>();
        long lastCheck;
    }

    public static class CorruptionIncident {
        String type;
        String description;
        double amount;
        long timestamp;
    }

    public CorruptionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.corruptionDir = new File(plugin.getDataFolder(), "corruption");
        this.corruptionDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateCorruption, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String reportCorruption(String nationId, String type, String description, double amount) {
        if (nationManager == null) return "Сервис наций недоступен.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (amount <= 0) return "Сумма должна быть больше 0.";
        CorruptionData data = nationCorruption.computeIfAbsent(nationId, k -> new CorruptionData());
        CorruptionIncident incident = new CorruptionIncident();
        incident.type = type;
        incident.description = description;
        incident.amount = amount;
        incident.timestamp = System.currentTimeMillis();
        data.incidents.add(incident);
        data.level = Math.min(100, data.level + 5.0); // +5% per incident
        // Apply effects
        if (data.level > 50) {
            if (plugin.getHappinessService() != null) {
                plugin.getHappinessService().modifyHappiness(nationId, -10.0);
            }
            n.setTreasury(Math.max(0, n.getTreasury() - amount * 0.1)); // Corruption tax loss
        }
        n.getHistory().add("Коррупция: " + description);
        try {
            nationManager.save(n);
            saveCorruption(nationId, data);
        } catch (Exception ignored) {}
        return "Коррупция зафиксирована (уровень: " + data.level + "%)";
    }

    private synchronized void updateCorruption() {
        // Corruption naturally decreases if there's good governance
        long now = System.currentTimeMillis();
        for (Map.Entry<String, CorruptionData> e : nationCorruption.entrySet()) {
            CorruptionData data = e.getValue();
            Nation n = nationManager.getNationById(e.getKey());
            if (n == null) continue;
            // Good education and low crime reduce corruption
            double education = plugin.getEducationService() != null ? plugin.getEducationService().getEducationLevel(e.getKey()) : 0.0;
            double crime = plugin.getCrimeService() != null ? plugin.getCrimeService().getCrimeRate(e.getKey()) : 0.0;
            double reduction = (education * 0.1) - (crime * 0.05);
            data.level = Math.max(0, Math.min(100, data.level - reduction * 0.1));
            data.lastCheck = now;
            saveCorruption(e.getKey(), data);
        }
    }

    public synchronized double getCorruptionLevel(String nationId) {
        CorruptionData data = nationCorruption.get(nationId);
        return data != null ? data.level : 0.0;
    }

    private void loadAll() {
        File[] files = corruptionDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                CorruptionData data = new CorruptionData();
                data.level = o.has("level") ? o.get("level").getAsDouble() : 0.0;
                data.lastCheck = o.has("lastCheck") ? o.get("lastCheck").getAsLong() : 0;
                if (o.has("incidents")) {
                    for (var elem : o.getAsJsonArray("incidents")) {
                        JsonObject iObj = elem.getAsJsonObject();
                        CorruptionIncident incident = new CorruptionIncident();
                        incident.type = iObj.get("type").getAsString();
                        incident.description = iObj.get("description").getAsString();
                        incident.amount = iObj.get("amount").getAsDouble();
                        incident.timestamp = iObj.get("timestamp").getAsLong();
                        data.incidents.add(incident);
                    }
                }
                nationCorruption.put(nationId, data);
            } catch (Exception ignored) {}
        }
    }

    private void saveCorruption(String nationId, CorruptionData data) {
        File f = new File(corruptionDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("level", data.level);
        o.addProperty("lastCheck", data.lastCheck);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (CorruptionIncident incident : data.incidents) {
            JsonObject iObj = new JsonObject();
            iObj.addProperty("type", incident.type);
            iObj.addProperty("description", incident.description);
            iObj.addProperty("amount", incident.amount);
            iObj.addProperty("timestamp", incident.timestamp);
            arr.add(iObj);
        }
        o.add("incidents", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive corruption statistics.
     */
    public synchronized Map<String, Object> getCorruptionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        CorruptionData data = nationCorruption.get(nationId);
        if (data == null) {
            stats.put("level", 0.0);
            stats.put("incidents", 0);
            stats.put("rating", "ОТСУТСТВУЕТ");
            return stats;
        }
        
        stats.put("level", data.level);
        stats.put("incidents", data.incidents.size());
        stats.put("recentIncidents", data.incidents.stream()
            .filter(i -> System.currentTimeMillis() - i.timestamp < 7 * 24 * 60 * 60 * 1000L)
            .count());
        
        // Corruption rating
        String rating = "ОТСУТСТВУЕТ";
        if (data.level >= 80) rating = "КРИТИЧЕСКАЯ";
        else if (data.level >= 60) rating = "ВЫСОКАЯ";
        else if (data.level >= 40) rating = "СРЕДНЯЯ";
        else if (data.level >= 20) rating = "НИЗКАЯ";
        else if (data.level > 0) rating = "МИНИМАЛЬНАЯ";
        stats.put("rating", rating);
        
        // Economic impact
        double economicImpact = data.level * 0.5; // -0.5% per 1% corruption
        stats.put("economicImpact", economicImpact);
        
        // Recent incidents
        List<Map<String, Object>> recentIncidentsList = new ArrayList<>();
        for (CorruptionIncident incident : data.incidents) {
            if (System.currentTimeMillis() - incident.timestamp < 30 * 24 * 60 * 60 * 1000L) {
                Map<String, Object> incidentData = new HashMap<>();
                incidentData.put("type", incident.type);
                incidentData.put("description", incident.description);
                incidentData.put("amount", incident.amount);
                incidentData.put("timestamp", incident.timestamp);
                recentIncidentsList.add(incidentData);
            }
        }
        stats.put("recentIncidentsList", recentIncidentsList);
        
        return stats;
    }
    
    /**
     * Fight corruption through anti-corruption measures.
     */
    public synchronized String fightCorruption(String nationId, double cost) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (cost <= 0) return "Сумма должна быть больше 0.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        
        CorruptionData data = nationCorruption.get(nationId);
        if (data == null) return "Коррупции не обнаружено.";
        
        double reduction = (cost / 1000.0) * 2.0; // Each 1000 reduces corruption by 2%
        double newLevel = Math.max(0, data.level - reduction);
        
        data.level = newLevel;
        n.setTreasury(n.getTreasury() - cost);
        
        nationManager.save(n);
        saveCorruption(nationId, data);
        
        return "Коррупция снижена. Текущий уровень: " + String.format("%.1f", newLevel) + "%";
    }
    
    /**
     * Calculate corruption impact on various systems.
     */
    public synchronized Map<String, Double> getCorruptionImpacts(String nationId) {
        Map<String, Double> impacts = new HashMap<>();
        double level = getCorruptionLevel(nationId);
        
        impacts.put("economy", -level * 0.5); // -0.5% per 1% corruption
        impacts.put("happiness", -level * 0.3); // -0.3% per 1% corruption
        impacts.put("publicOpinion", -level * 0.4); // -0.4% per 1% corruption
        
        return impacts;
    }
    
    /**
     * Get global corruption statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCorruptionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        double totalCorruption = 0.0;
        double maxCorruption = 0.0;
        double minCorruption = Double.MAX_VALUE;
        int nationsWithCorruption = 0;
        int totalIncidents = 0;
        
        // Corruption distribution
        int critical = 0, high = 0, medium = 0, low = 0, minimal = 0, none = 0;
        
        for (Nation n : nationManager.getAll()) {
            CorruptionData data = nationCorruption.get(n.getId());
            double corruption = data != null ? data.level : 0.0;
            
            if (corruption > 0 || data != null) {
                nationsWithCorruption++;
                if (data != null) {
                    totalIncidents += data.incidents.size();
                }
            }
            
            totalCorruption += corruption;
            maxCorruption = Math.max(maxCorruption, corruption);
            minCorruption = Math.min(minCorruption, corruption);
            
            if (corruption >= 80) critical++;
            else if (corruption >= 60) high++;
            else if (corruption >= 40) medium++;
            else if (corruption >= 20) low++;
            else if (corruption > 0) minimal++;
            else none++;
        }
        
        stats.put("totalNations", nationManager.getAll().size());
        stats.put("nationsWithCorruption", nationsWithCorruption);
        stats.put("averageCorruption", nationsWithCorruption > 0 ? totalCorruption / nationsWithCorruption : 0);
        stats.put("maxCorruption", maxCorruption);
        stats.put("minCorruption", minCorruption == Double.MAX_VALUE ? 0 : minCorruption);
        stats.put("totalIncidents", totalIncidents);
        stats.put("averageIncidents", nationsWithCorruption > 0 ? (double) totalIncidents / nationsWithCorruption : 0);
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("critical", critical);
        distribution.put("high", high);
        distribution.put("medium", medium);
        distribution.put("low", low);
        distribution.put("minimal", minimal);
        distribution.put("none", none);
        stats.put("corruptionDistribution", distribution);
        
        // Top nations by corruption (worst)
        List<Map.Entry<String, Double>> topByCorruption = new ArrayList<>();
        for (Nation n : nationManager.getAll()) {
            CorruptionData data = nationCorruption.get(n.getId());
            double corruption = data != null ? data.level : 0.0;
            topByCorruption.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), corruption));
        }
        topByCorruption.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topByCorruption", topByCorruption.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return stats;
    }
}

