package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.util.*;

/** Generates global crises/events periodically. */
public class EventGenerator {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Random random = new Random();
    
    // Event tracking
    private final List<GlobalEvent> generatedEvents = new ArrayList<>();
    private final Map<String, Integer> eventTypeCounts = new HashMap<>();
    private long lastEventTime = 0;
    private int totalEventsGenerated = 0;
    
    public static class GlobalEvent {
        String type;
        long timestamp;
        String message;
    }

    public EventGenerator(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        long interval = plugin.getConfig().getLong("events.crisisIntervalMinutes", 120) * 60L * 20L;
        if (interval > 0) {
            Bukkit.getScheduler().runTaskTimer(plugin, this::generateCrisis, interval, interval);
        }
    }

    private synchronized void generateCrisis() {
        if (nationManager == null) return;
        String[] types = {"Пандемия", "Экономический кризис", "Природная катастрофа", "Технологический сбой"};
        String type = types[random.nextInt(types.length)];
        String msg = "§c[КРИЗИС] §f" + type + " поразил мир! Все нации должны принять меры.";
        
        // Track event
        GlobalEvent event = new GlobalEvent();
        event.type = type;
        event.timestamp = System.currentTimeMillis();
        event.message = msg;
        generatedEvents.add(event);
        
        // Keep only last 100 events
        if (generatedEvents.size() > 100) {
            generatedEvents.remove(0);
        }
        
        eventTypeCounts.put(type, eventTypeCounts.getOrDefault(type, 0) + 1);
        lastEventTime = System.currentTimeMillis();
        totalEventsGenerated++;
        
        // VISUAL EFFECTS: Broadcast crisis to all players with effects
        VisualEffectsService effectsService = plugin.getVisualEffectsService();
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            // Title announcement
            player.sendTitle("§c§l[КРИЗИС]", "§f" + type + " поразил мир!", 10, 100, 20);
            
            // Actionbar
            if (effectsService != null) {
                effectsService.sendActionBar(player, msg);
            }
            
            // Visual effects based on crisis type
            if (type.equals("Экономический кризис")) {
                if (effectsService != null) {
                    effectsService.playEconomicCrisisEffect(player, player.getLocation());
                }
            } else if (type.equals("Пандемия")) {
                // Gray particles for pandemic
                player.getLocation().getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, 
                    player.getLocation(), 20, 2, 2, 2, 0.1);
            } else if (type.equals("Природная катастрофа")) {
                // Explosion particles
                player.getLocation().getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, 
                    player.getLocation(), 3, 5, 2, 5, 0);
            }
        }
        
        Bukkit.getServer().broadcastMessage(msg);
        for (Nation n : nationManager.getAll()) {
            long timestamp = System.currentTimeMillis();
            n.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Глобальный кризис: " + type);
            try { nationManager.save(n); } catch (Exception ignored) {}
        }
    }
    
    /**
     * Get comprehensive event generator statistics.
     */
    public synchronized Map<String, Object> getEventGeneratorStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalEventsGenerated", totalEventsGenerated);
        stats.put("lastEventTime", lastEventTime);
        stats.put("lastEventTimeAgo", lastEventTime > 0 ? (System.currentTimeMillis() - lastEventTime) / 1000 / 60 : -1);
        
        // Event type distribution
        stats.put("eventTypeCounts", new HashMap<>(eventTypeCounts));
        
        // Recent events (last 10)
        List<Map<String, Object>> recentEvents = new ArrayList<>();
        for (int i = Math.max(0, generatedEvents.size() - 10); i < generatedEvents.size(); i++) {
            GlobalEvent e = generatedEvents.get(i);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", e.type);
            eventData.put("timestamp", e.timestamp);
            eventData.put("timeAgo", (System.currentTimeMillis() - e.timestamp) / 1000 / 60);
            eventData.put("message", e.message);
            recentEvents.add(eventData);
        }
        stats.put("recentEvents", recentEvents);
        
        // Configuration
        long interval = plugin.getConfig().getLong("events.crisisIntervalMinutes", 120);
        stats.put("crisisIntervalMinutes", interval);
        stats.put("crisisIntervalHours", interval / 60.0);
        
        // Event rating
        String rating = "НЕТ СОБЫТИЙ";
        if (totalEventsGenerated >= 50) rating = "АКТИВНЫЙ";
        else if (totalEventsGenerated >= 20) rating = "УМЕРЕННЫЙ";
        else if (totalEventsGenerated >= 10) rating = "СТАБИЛЬНЫЙ";
        else if (totalEventsGenerated >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get recent events.
     */
    public synchronized List<GlobalEvent> getRecentEvents(int count) {
        int start = Math.max(0, generatedEvents.size() - count);
        return new ArrayList<>(generatedEvents.subList(start, generatedEvents.size()));
    }
    
    /**
     * Get events by type.
     */
    public synchronized List<GlobalEvent> getEventsByType(String type) {
        List<GlobalEvent> filtered = new ArrayList<>();
        for (GlobalEvent event : generatedEvents) {
            if (event.type.equals(type)) {
                filtered.add(event);
            }
        }
        return filtered;
    }
    
    /**
     * Get event count by type.
     */
    public synchronized int getEventCountByType(String type) {
        return eventTypeCounts.getOrDefault(type, 0);
    }
    
    /**
     * Check if event was generated recently (within last N minutes).
     */
    public synchronized boolean wasEventRecent(int minutesAgo) {
        if (lastEventTime == 0) return false;
        long threshold = System.currentTimeMillis() - (minutesAgo * 60 * 1000L);
        return lastEventTime > threshold;
    }
    
    /**
     * Get average time between events.
     */
    public synchronized double getAverageEventInterval() {
        if (totalEventsGenerated < 2) return -1;
        
        if (generatedEvents.size() < 2) return -1;
        
        long totalInterval = 0;
        int intervals = 0;
        for (int i = 1; i < generatedEvents.size(); i++) {
            totalInterval += generatedEvents.get(i).timestamp - generatedEvents.get(i - 1).timestamp;
            intervals++;
        }
        
        return intervals > 0 ? (totalInterval / (double) intervals) / 1000.0 / 60.0 : -1; // minutes
    }
    
    /**
     * Get global event generator statistics (alias for getEventGeneratorStatistics for consistency).
     */
    public synchronized Map<String, Object> getGlobalEventGeneratorStatistics() {
        return getEventGeneratorStatistics();
    }
}

