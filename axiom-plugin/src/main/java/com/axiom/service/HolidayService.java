package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages religious holidays with scheduled buff effects. */
public class HolidayService {
    private final AXIOM plugin;
    private final ReligionManager religionManager;
    private final File holidaysDir;
    private final Map<String, List<Holiday>> religionHolidays = new HashMap<>(); // religionId -> holidays

    public static class Holiday {
        String id;
        String name;
        String religionId;
        long startTime;
        long durationMinutes;
        String buffType; // "regeneration", "strength", "speed"
    }

    public HolidayService(AXIOM plugin, ReligionManager religionManager) {
        this.plugin = plugin;
        this.religionManager = religionManager;
        this.holidaysDir = new File(plugin.getDataFolder(), "holidays");
        this.holidaysDir.mkdirs();
        loadAll();
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkActiveHolidays, 0, 20 * 60); // check every minute
    }

    public synchronized String createHoliday(String religionId, String name, String buffType, long durationMinutes) throws IOException {
        if (religionId == null || name == null || name.trim().isEmpty()) return "Неверные параметры.";
        if (buffType == null || buffType.trim().isEmpty()) return "Тип баффа не указан.";
        if (durationMinutes <= 0) return "Длительность должна быть больше 0.";
        String id = name.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
        Holiday h = new Holiday();
        h.id = id;
        h.name = name;
        h.religionId = religionId;
        h.buffType = buffType;
        h.durationMinutes = durationMinutes;
        religionHolidays.computeIfAbsent(religionId, k -> new ArrayList<>()).add(h);
        saveHoliday(h);
        return "Праздник создан: " + name;
    }

    public synchronized String startHoliday(String religionId, String holidayId) {
        if (religionId == null || holidayId == null) return "Неверные параметры.";
        List<Holiday> holidays = religionHolidays.get(religionId);
        if (holidays == null) return "Религия не найдена.";
        Holiday h = holidays.stream().filter(hol -> hol.id.equals(holidayId)).findFirst().orElse(null);
        if (h == null) return "Праздник не найден.";
        h.startTime = System.currentTimeMillis();
        saveHoliday(h);
        broadcastHoliday(religionId, h);
        return "Праздник начат: " + h.name;
    }

    private void broadcastHoliday(String religionId, Holiday h) {
        String msg = "§6[ПРАЗДНИК] §fРелигия '" + religionId + "' празднует: " + h.name;
        Bukkit.getServer().broadcastMessage(msg);
        
        // VISUAL EFFECTS: Festival effects for all players in this religion
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            VisualEffectsService effectsService = plugin.getVisualEffectsService();
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getPlayerDataManager() == null) continue;
                String playerReligion = plugin.getPlayerDataManager().getReligion(player.getUniqueId());
                if (playerReligion != null && playerReligion.equals(religionId)) {
                    // Player is part of this religion - show effects
                    player.sendMessage("§b[Религиозный праздник] §f" + h.name + " начинается! Вы получаете бафф.");
                    if (effectsService != null) {
                        effectsService.sendActionBar(player, "§6🎉 " + h.name);
                    }
                    
                    // Fireworks over player
                    org.bukkit.Location loc = player.getLocation();
                    if (effectsService != null) {
                        effectsService.playHolidayEffect(loc);
                    }
                    
                    // Apply buff
                    applyHolidayBuff(player, h.buffType);
                }
            }
        });
    }

    private void applyHolidayBuff(Player p, String buffType) {
        if (buffType == null) return;
        PotionEffectType type = switch (buffType.toLowerCase()) {
            case "regeneration" -> PotionEffectType.REGENERATION;
            case "strength" -> PotionEffectType.INCREASE_DAMAGE;
            case "speed" -> PotionEffectType.SPEED;
            default -> PotionEffectType.SATURATION;
        };
        p.addPotionEffect(new PotionEffect(type, 20 * 60 * 10, 0, false, false)); // 10 minutes
    }

    private void checkActiveHolidays() {
        long now = System.currentTimeMillis();
        for (var entry : religionHolidays.entrySet()) {
            for (Holiday h : entry.getValue()) {
                if (h.startTime > 0 && now < h.startTime + (h.durationMinutes * 60_000L)) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (plugin.getPlayerDataManager() == null) continue;
                        String playerReligion = plugin.getPlayerDataManager().getReligion(p.getUniqueId());
                        if (playerReligion != null && playerReligion.equals(h.religionId)) {
                            applyHolidayBuff(p, h.buffType);
                        }
                    }
                }
            }
        }
    }

    private void loadAll() {
        File[] files = holidaysDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Holiday h = new Holiday();
                h.id = o.get("id").getAsString();
                h.name = o.get("name").getAsString();
                h.religionId = o.get("religionId").getAsString();
                h.buffType = o.get("buffType").getAsString();
                h.durationMinutes = o.get("durationMinutes").getAsLong();
                h.startTime = o.has("startTime") ? o.get("startTime").getAsLong() : 0;
                religionHolidays.computeIfAbsent(h.religionId, k -> new ArrayList<>()).add(h);
            } catch (Exception ignored) {}
        }
    }

    private void saveHoliday(Holiday h) {
        File f = new File(holidaysDir, h.religionId + "_" + h.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", h.id);
        o.addProperty("name", h.name);
        o.addProperty("religionId", h.religionId);
        o.addProperty("buffType", h.buffType);
        o.addProperty("durationMinutes", h.durationMinutes);
        o.addProperty("startTime", h.startTime);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive holiday statistics.
     */
    public synchronized Map<String, Object> getHolidayStatistics(String religionId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Holiday> holidays = religionHolidays.get(religionId);
        if (holidays == null) holidays = Collections.emptyList();
        
        stats.put("totalHolidays", holidays.size());
        
        // Active holidays
        long now = System.currentTimeMillis();
        List<Holiday> activeHolidays = new ArrayList<>();
        for (Holiday h : holidays) {
            if (h.startTime > 0 && now < h.startTime + (h.durationMinutes * 60_000L)) {
                activeHolidays.add(h);
            }
        }
        stats.put("activeHolidays", activeHolidays.size());
        stats.put("activeHolidaysList", activeHolidays);
        
        // Holidays by buff type
        Map<String, Integer> byBuffType = new HashMap<>();
        for (Holiday h : holidays) {
            byBuffType.put(h.buffType, byBuffType.getOrDefault(h.buffType, 0) + 1);
        }
        stats.put("byBuffType", byBuffType);
        
        // Next holiday
        long nextHoliday = holidays.stream()
            .filter(h -> h.startTime == 0 || h.startTime > now)
            .mapToLong(h -> h.startTime)
            .min()
            .orElse(0);
        stats.put("nextHoliday", nextHoliday > 0 ? (nextHoliday - now) / 1000 / 60 : -1); // minutes
        
        // Holiday details
        List<Map<String, Object>> holidaysList = new ArrayList<>();
        for (Holiday h : holidays) {
            Map<String, Object> holidayData = new HashMap<>();
            holidayData.put("id", h.id);
            holidayData.put("name", h.name);
            holidayData.put("buffType", h.buffType);
            holidayData.put("durationMinutes", h.durationMinutes);
            holidayData.put("isActive", h.startTime > 0 && now < h.startTime + (h.durationMinutes * 60_000L));
            holidayData.put("startTime", h.startTime);
            holidaysList.add(holidayData);
        }
        stats.put("holidays", holidaysList);
        
        return stats;
    }
    
    /**
     * Delete holiday.
     */
    public synchronized String deleteHoliday(String religionId, String holidayId) throws IOException {
        List<Holiday> holidays = religionHolidays.get(religionId);
        if (holidays == null) return "Религия не найдена.";
        
        Holiday holiday = holidays.stream()
            .filter(h -> h.id.equals(holidayId))
            .findFirst()
            .orElse(null);
        
        if (holiday == null) return "Праздник не найден.";
        
        holidays.remove(holiday);
        
        // Delete file
        File f = new File(holidaysDir, religionId + "_" + holidayId + ".json");
        if (f.exists()) f.delete();
        
        return "Праздник удалён: " + holiday.name;
    }
    
    /**
     * Get active holidays for a religion.
     */
    public synchronized List<Holiday> getActiveHolidays(String religionId) {
        List<Holiday> holidays = religionHolidays.get(religionId);
        if (holidays == null) return Collections.emptyList();
        
        long now = System.currentTimeMillis();
        return holidays.stream()
            .filter(h -> h.startTime > 0 && now < h.startTime + (h.durationMinutes * 60_000L))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate happiness bonus from active holidays.
     */
    public synchronized double getHolidayHappinessBonus(String religionId) {
        int activeCount = getActiveHolidays(religionId).size();
        // +2% happiness per active holiday
        return activeCount * 0.02;
    }
    
    /**
     * Get global holiday statistics across all religions.
     */
    public synchronized Map<String, Object> getGlobalHolidayStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalHolidays = 0;
        int activeHolidays = 0;
        Map<String, Integer> holidaysByReligion = new HashMap<>();
        Map<String, Integer> holidaysByBuffType = new HashMap<>();
        long now = System.currentTimeMillis();
        
        for (Map.Entry<String, List<Holiday>> entry : religionHolidays.entrySet()) {
            int religionHolidayCount = entry.getValue().size();
            totalHolidays += religionHolidayCount;
            holidaysByReligion.put(entry.getKey(), religionHolidayCount);
            
            for (Holiday h : entry.getValue()) {
                holidaysByBuffType.put(h.buffType, holidaysByBuffType.getOrDefault(h.buffType, 0) + 1);
                
                if (h.startTime > 0 && now < h.startTime + (h.durationMinutes * 60_000L)) {
                    activeHolidays++;
                }
            }
        }
        
        stats.put("totalHolidays", totalHolidays);
        stats.put("activeHolidays", activeHolidays);
        stats.put("holidaysByReligion", holidaysByReligion);
        stats.put("holidaysByBuffType", holidaysByBuffType);
        stats.put("religionsWithHolidays", holidaysByReligion.size());
        stats.put("averageHolidaysPerReligion", holidaysByReligion.size() > 0 ? 
            (double) totalHolidays / holidaysByReligion.size() : 0);
        
        // Top religions by holidays
        List<Map.Entry<String, Integer>> topByHolidays = holidaysByReligion.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByHolidays", topByHolidays);
        
        // Most common buff types
        List<Map.Entry<String, Integer>> mostCommonBuffs = holidaysByBuffType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonBuffs", mostCommonBuffs);
        
        // Upcoming holidays count
        int upcomingHolidays = 0;
        for (List<Holiday> holidays : religionHolidays.values()) {
            for (Holiday h : holidays) {
                if (h.startTime == 0 || (h.startTime > now && h.startTime < now + 7 * 24 * 60 * 60_000L)) {
                    upcomingHolidays++;
                }
            }
        }
        stats.put("upcomingHolidays", upcomingHolidays);
        
        return stats;
    }
}

