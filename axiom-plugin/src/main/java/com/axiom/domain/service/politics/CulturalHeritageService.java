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

/** Manages cultural heritage sites and monuments. */
public class CulturalHeritageService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File heritageDir;
    private final Map<String, List<HeritageSite>> nationSites = new HashMap<>(); // nationId -> sites

    public static class HeritageSite {
        String id;
        String name;
        String location; // chunk key
        String type; // "monument", "temple", "library", "museum"
        double culturalValue;
        long establishedAt;
        boolean isProtected;
    }

    public CulturalHeritageService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.heritageDir = new File(plugin.getDataFolder(), "heritage");
        this.heritageDir.mkdirs();
        loadAll();
    }

    public synchronized String createSite(String nationId, String name, String location, String type, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (name == null || name.trim().isEmpty()) return "Название не указано.";
        if (location == null || location.trim().isEmpty()) return "Локация не указана.";
        if (type == null || type.trim().isEmpty()) return "Тип не указан.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (cost <= 0) return "Стоимость должна быть больше 0.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        String siteId = UUID.randomUUID().toString().substring(0, 8);
        HeritageSite site = new HeritageSite();
        site.id = siteId;
        site.name = name;
        site.location = location;
        site.type = type;
        site.culturalValue = 10.0; // Base value
        site.establishedAt = System.currentTimeMillis();
        site.isProtected = true;
        nationSites.computeIfAbsent(nationId, k -> new ArrayList<>()).add(site);
        n.setTreasury(n.getTreasury() - cost);
        if (plugin.getCultureService() != null) {
            plugin.getCultureService().developCulture(nationId, site.culturalValue);
        }
        n.getHistory().add("Культурное наследие создано: " + name);
        try {
            nationManager.save(n);
            saveSites(nationId);
        } catch (Exception ignored) {}
        return "Культурное наследие создано: " + name;
    }

    public synchronized String protectSite(String nationId, String siteId) {
        List<HeritageSite> sites = nationSites.get(nationId);
        if (sites == null) return "Наследие не найдено.";
        HeritageSite site = sites.stream()
            .filter(s -> s.id.equals(siteId))
            .findFirst()
            .orElse(null);
        if (site == null) return "Сайт не найден.";
        site.isProtected = true;
        site.culturalValue += 5.0; // Protection increases value
        saveSites(nationId);
        return "Сайт защищён.";
    }

    public synchronized double getCulturalValue(String nationId) {
        List<HeritageSite> sites = nationSites.get(nationId);
        if (sites == null) return 0.0;
        return sites.stream().mapToDouble(s -> s.culturalValue).sum();
    }

    private void loadAll() {
        File[] files = heritageDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                List<HeritageSite> sites = new ArrayList<>();
                if (o.has("sites")) {
                    for (var elem : o.getAsJsonArray("sites")) {
                        JsonObject sObj = elem.getAsJsonObject();
                        HeritageSite site = new HeritageSite();
                        site.id = sObj.get("id").getAsString();
                        site.name = sObj.get("name").getAsString();
                        site.location = sObj.get("location").getAsString();
                        site.type = sObj.get("type").getAsString();
                        site.culturalValue = sObj.get("culturalValue").getAsDouble();
                        site.establishedAt = sObj.get("establishedAt").getAsLong();
                        site.isProtected = sObj.has("protected") && sObj.get("protected").getAsBoolean();
                        sites.add(site);
                    }
                }
                nationSites.put(nationId, sites);
            } catch (Exception ignored) {}
        }
    }

    private void saveSites(String nationId) {
        File f = new File(heritageDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<HeritageSite> sites = nationSites.get(nationId);
        if (sites != null) {
            for (HeritageSite site : sites) {
                JsonObject sObj = new JsonObject();
                sObj.addProperty("id", site.id);
                sObj.addProperty("name", site.name);
                sObj.addProperty("location", site.location);
                sObj.addProperty("type", site.type);
                sObj.addProperty("culturalValue", site.culturalValue);
                sObj.addProperty("establishedAt", site.establishedAt);
                sObj.addProperty("protected", site.isProtected);
                arr.add(sObj);
            }
        }
        o.add("sites", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive cultural heritage statistics for a nation.
     */
    public synchronized Map<String, Object> getCulturalHeritageStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<HeritageSite> sites = nationSites.getOrDefault(nationId, Collections.emptyList());
        stats.put("totalSites", sites.size());
        stats.put("totalCulturalValue", getCulturalValue(nationId));
        
        // Sites by type
        Map<String, Integer> byType = new HashMap<>();
        int protectedSites = 0;
        for (HeritageSite site : sites) {
            byType.put(site.type, byType.getOrDefault(site.type, 0) + 1);
            if (site.isProtected) protectedSites++;
        }
        stats.put("byType", byType);
        stats.put("protectedSites", protectedSites);
        
        // Average cultural value per site
        stats.put("averageValuePerSite", sites.size() > 0 ? getCulturalValue(nationId) / sites.size() : 0);
        
        // Heritage rating
        String rating = "НЕТ НАСЛЕДИЯ";
        if (sites.size() >= 20) rating = "ОБШИРНОЕ";
        else if (sites.size() >= 10) rating = "РАЗВИТОЕ";
        else if (sites.size() >= 5) rating = "РАЗНООБРАЗНОЕ";
        else if (sites.size() >= 1) rating = "НАЧАЛЬНОЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global cultural heritage statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCulturalHeritageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalSites = 0;
        double totalCulturalValue = 0.0;
        Map<String, Integer> sitesByNation = new HashMap<>();
        Map<String, Double> valueByNation = new HashMap<>();
        Map<String, Integer> sitesByType = new HashMap<>();
        int totalProtected = 0;
        
        for (Map.Entry<String, List<HeritageSite>> entry : nationSites.entrySet()) {
            String nationId = entry.getKey();
            List<HeritageSite> sites = entry.getValue();
            
            totalSites += sites.size();
            double value = getCulturalValue(nationId);
            totalCulturalValue += value;
            
            sitesByNation.put(nationId, sites.size());
            valueByNation.put(nationId, value);
            
            for (HeritageSite site : sites) {
                sitesByType.put(site.type, sitesByType.getOrDefault(site.type, 0) + 1);
                if (site.isProtected) totalProtected++;
            }
        }
        
        stats.put("totalSites", totalSites);
        stats.put("totalCulturalValue", totalCulturalValue);
        stats.put("sitesByNation", sitesByNation);
        stats.put("valueByNation", valueByNation);
        stats.put("sitesByType", sitesByType);
        stats.put("totalProtected", totalProtected);
        stats.put("nationsWithHeritage", sitesByNation.size());
        
        // Average statistics
        stats.put("averageSitesPerNation", sitesByNation.size() > 0 ? 
            (double) totalSites / sitesByNation.size() : 0);
        stats.put("averageValuePerNation", valueByNation.size() > 0 ? 
            totalCulturalValue / valueByNation.size() : 0);
        stats.put("averageValuePerSite", totalSites > 0 ? totalCulturalValue / totalSites : 0);
        
        // Top nations by sites
        List<Map.Entry<String, Integer>> topBySites = sitesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySites", topBySites);
        
        // Top nations by cultural value
        List<Map.Entry<String, Double>> topByValue = valueByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByValue", topByValue);
        
        // Protection rate
        stats.put("protectionRate", totalSites > 0 ? (double) totalProtected / totalSites : 0);
        
        return stats;
    }
}

