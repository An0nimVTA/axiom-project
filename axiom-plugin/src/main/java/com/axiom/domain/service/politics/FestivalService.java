package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages cultural festivals and celebrations. */
public class FestivalService {
    private final AXIOM plugin;
    private final File festivalsDir;
    private final Map<String, List<Festival>> nationFestivals = new HashMap<>(); // nationId -> festivals

    public static class Festival {
        String id;
        String nationId;
        String name;
        String type; // "cultural", "victory", "harvest", "religious"
        long scheduledTime;
        double happinessBoost;
        boolean active;
    }

    public FestivalService(AXIOM plugin) {
        this.plugin = plugin;
        this.festivalsDir = new File(plugin.getDataFolder(), "festivals");
        this.festivalsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkActiveFestivals, 0, 20 * 60); // every minute
    }

    public synchronized String scheduleFestival(String nationId, String name, String type, long scheduledTime, double happinessBoost) throws IOException {
        String id = UUID.randomUUID().toString();
        Festival f = new Festival();
        f.id = id;
        f.nationId = nationId;
        f.name = name;
        f.type = type;
        f.scheduledTime = scheduledTime;
        f.happinessBoost = happinessBoost;
        f.active = false;
        nationFestivals.computeIfAbsent(nationId, k -> new ArrayList<>()).add(f);
        saveFestival(f);
        return "Фестиваль запланирован: " + name;
    }

    private void checkActiveFestivals() {
        long now = System.currentTimeMillis();
        for (var entry : nationFestivals.entrySet()) {
            for (Festival f : entry.getValue()) {
                if (!f.active && f.scheduledTime <= now && now < f.scheduledTime + 60 * 60_000L) {
                    activateFestival(f);
                }
            }
        }
    }

    private void activateFestival(Festival f) {
        f.active = true;
        try {
            saveFestival(f);
        } catch (IOException ignored) {}
        // Boost happiness for all cities
        if (plugin.getCityGrowthEngine() != null) {
            List<com.axiom.domain.model.City> cities = plugin.getCityGrowthEngine().getCitiesOf(f.nationId);
            for (com.axiom.domain.model.City c : cities) {
                double boosted = c.getHappiness() + f.happinessBoost;
                c.setHappiness(Math.max(0, Math.min(100, boosted)));
            }
        }
        
        // VISUAL EFFECTS: Announce festival to all nation members
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            com.axiom.domain.model.Nation n = plugin.getNationManager().getNationById(f.nationId);
            if (n != null) {
                String msg = "§6[ФЕСТИВАЛЬ] §f" + f.name + " в нации '" + n.getName() + "'!";
                org.bukkit.Bukkit.getServer().broadcastMessage(msg);
                
                for (java.util.UUID citizenId : n.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        if (plugin.getVisualEffectsService() != null) {
                            plugin.getVisualEffectsService().sendActionBar(citizen, "§6🎊 " + f.name);
                            plugin.getVisualEffectsService().playHolidayEffect(citizen.getLocation());
                        }
                    }
                }
            }
        });
    }

    private void loadAll() {
        File[] files = festivalsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Festival fest = new Festival();
                fest.id = o.get("id").getAsString();
                fest.nationId = o.get("nationId").getAsString();
                fest.name = o.get("name").getAsString();
                fest.type = o.get("type").getAsString();
                fest.scheduledTime = o.get("scheduledTime").getAsLong();
                fest.happinessBoost = o.get("happinessBoost").getAsDouble();
                fest.active = o.has("active") && o.get("active").getAsBoolean();
                nationFestivals.computeIfAbsent(fest.nationId, k -> new ArrayList<>()).add(fest);
            } catch (Exception ignored) {}
        }
    }

    private void saveFestival(Festival f) throws IOException {
        File file = new File(festivalsDir, f.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", f.id);
        o.addProperty("nationId", f.nationId);
        o.addProperty("name", f.name);
        o.addProperty("type", f.type);
        o.addProperty("scheduledTime", f.scheduledTime);
        o.addProperty("happinessBoost", f.happinessBoost);
        o.addProperty("active", f.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive festival statistics.
     */
    public synchronized Map<String, Object> getFestivalStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Festival> festivals = nationFestivals.get(nationId);
        if (festivals == null) festivals = Collections.emptyList();
        
        stats.put("totalFestivals", festivals.size());
        
        // Active festivals
        long now = System.currentTimeMillis();
        List<Festival> activeFestivals = festivals.stream()
            .filter(f -> f.active && now < f.scheduledTime + 60 * 60_000L)
            .collect(java.util.stream.Collectors.toList());
        stats.put("activeFestivals", activeFestivals.size());
        
        // Festivals by type
        Map<String, Integer> byType = new HashMap<>();
        double totalHappinessBoost = 0.0;
        for (Festival f : festivals) {
            byType.put(f.type, byType.getOrDefault(f.type, 0) + 1);
            if (f.active) totalHappinessBoost += f.happinessBoost;
        }
        stats.put("byType", byType);
        stats.put("totalActiveHappinessBoost", totalHappinessBoost);
        
        // Upcoming festivals
        List<Festival> upcoming = festivals.stream()
            .filter(f -> f.scheduledTime > now && !f.active)
            .sorted((a, b) -> Long.compare(a.scheduledTime, b.scheduledTime))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("upcomingFestivals", upcoming.size());
        
        // Festival details
        List<Map<String, Object>> festivalsList = new ArrayList<>();
        for (Festival f : festivals) {
            Map<String, Object> festivalData = new HashMap<>();
            festivalData.put("id", f.id);
            festivalData.put("name", f.name);
            festivalData.put("type", f.type);
            festivalData.put("happinessBoost", f.happinessBoost);
            festivalData.put("isActive", f.active);
            festivalData.put("scheduledTime", f.scheduledTime);
            festivalData.put("timeUntil", f.scheduledTime > now ? (f.scheduledTime - now) / 1000 / 60 : -1); // minutes
            festivalsList.add(festivalData);
        }
        stats.put("festivals", festivalsList);
        
        return stats;
    }
    
    /**
     * Cancel festival.
     */
    public synchronized String cancelFestival(String nationId, String festivalId) throws IOException {
        List<Festival> festivals = nationFestivals.get(nationId);
        if (festivals == null) return "Фестивали не найдены.";
        
        Festival festival = festivals.stream()
            .filter(f -> f.id.equals(festivalId))
            .findFirst()
            .orElse(null);
        
        if (festival == null) return "Фестиваль не найден.";
        
        festivals.remove(festival);
        
        // Delete file
        File f = new File(festivalsDir, festivalId + ".json");
        if (f.exists()) f.delete();
        
        return "Фестиваль отменён: " + festival.name;
    }
    
    /**
     * Get active festivals for a nation.
     */
    public synchronized List<Festival> getActiveFestivals(String nationId) {
        List<Festival> festivals = nationFestivals.get(nationId);
        if (festivals == null) return Collections.emptyList();
        
        long now = System.currentTimeMillis();
        return festivals.stream()
            .filter(f -> f.active && now < f.scheduledTime + 60 * 60_000L)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate total happiness bonus from active festivals.
     */
    public synchronized double getTotalHappinessBoost(String nationId) {
        List<Festival> festivals = nationFestivals.get(nationId);
        if (festivals == null) return 0.0;
        
        long now = System.currentTimeMillis();
        return festivals.stream()
            .filter(f -> f.active && now < f.scheduledTime + 60 * 60_000L)
            .mapToDouble(f -> f.happinessBoost)
            .sum();
    }
    
    /**
     * Get global festival statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalFestivalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalFestivals = 0;
        int activeFestivals = 0;
        Map<String, Integer> festivalsByNation = new HashMap<>();
        Map<String, Integer> festivalsByType = new HashMap<>();
        double totalHappinessBoost = 0.0;
        long now = System.currentTimeMillis();
        
        for (Map.Entry<String, List<Festival>> entry : nationFestivals.entrySet()) {
            int nationFestivalCount = entry.getValue().size();
            totalFestivals += nationFestivalCount;
            festivalsByNation.put(entry.getKey(), nationFestivalCount);
            
            for (Festival f : entry.getValue()) {
                festivalsByType.put(f.type, festivalsByType.getOrDefault(f.type, 0) + 1);
                
                if (f.active && now < f.scheduledTime + 60 * 60_000L) {
                    activeFestivals++;
                    totalHappinessBoost += f.happinessBoost;
                }
            }
        }
        
        stats.put("totalFestivals", totalFestivals);
        stats.put("activeFestivals", activeFestivals);
        stats.put("festivalsByNation", festivalsByNation);
        stats.put("festivalsByType", festivalsByType);
        stats.put("totalActiveHappinessBoost", totalHappinessBoost);
        stats.put("nationsWithFestivals", festivalsByNation.size());
        stats.put("averageFestivalsPerNation", festivalsByNation.size() > 0 ? 
            (double) totalFestivals / festivalsByNation.size() : 0);
        
        // Top nations by festivals
        List<Map.Entry<String, Integer>> topByFestivals = festivalsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByFestivals", topByFestivals);
        
        // Most common festival types
        List<Map.Entry<String, Integer>> mostCommonTypes = festivalsByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTypes", mostCommonTypes);
        
        // Upcoming festivals count
        int upcomingFestivals = 0;
        for (List<Festival> festivals : nationFestivals.values()) {
            for (Festival f : festivals) {
                if (f.scheduledTime > now && !f.active) {
                    upcomingFestivals++;
                }
            }
        }
        stats.put("upcomingFestivals", upcomingFestivals);
        
        return stats;
    }
}

