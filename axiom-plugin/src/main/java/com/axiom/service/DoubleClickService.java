package com.axiom.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DoubleClickService {
    private final Map<String, Long> lastClick = new HashMap<>();
    private final Map<String, Integer> clickCounts = new HashMap<>(); // Track click counts
    private final long windowMs = 5000;

    public synchronized boolean shouldProceed(UUID uuid, String actionKey) {
        if (uuid == null || actionKey == null) {
            return false;
        }
        String key = uuid.toString() + ":" + actionKey;
        long now = System.currentTimeMillis();
        Long last = lastClick.get(key);
        lastClick.put(key, now);
        
        if (last != null && (now - last) <= windowMs) {
            clickCounts.put(key, clickCounts.getOrDefault(key, 0) + 1);
            return true;
        } else {
            clickCounts.put(key, 1);
            return false;
        }
    }
    
    /**
     * Get comprehensive double-click statistics.
     */
    public synchronized Map<String, Object> getDoubleClickStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalTrackedActions", lastClick.size());
        stats.put("windowMs", windowMs);
        stats.put("windowSeconds", windowMs / 1000);
        
        // Action usage
        Map<String, Integer> actionUsage = new HashMap<>();
        for (String key : lastClick.keySet()) {
            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                String action = parts[1];
                actionUsage.put(action, actionUsage.getOrDefault(action, 0) + 1);
            }
        }
        stats.put("actionUsage", actionUsage);
        
        // Successful double-clicks
        int successfulDoubleClicks = 0;
        for (Integer count : clickCounts.values()) {
            if (count >= 2) {
                successfulDoubleClicks++;
            }
        }
        stats.put("successfulDoubleClicks", successfulDoubleClicks);
        stats.put("totalClickEvents", clickCounts.values().stream().mapToInt(Integer::intValue).sum());
        
        // Average clicks per action
        double avgClicks = clickCounts.isEmpty() ? 0 : 
            clickCounts.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        stats.put("averageClicksPerAction", avgClicks);
        
        return stats;
    }
    
    /**
     * Get statistics for specific player.
     */
    public synchronized Map<String, Object> getPlayerStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (playerId == null) {
            return stats;
        }
        String playerPrefix = playerId.toString() + ":";
        int actionCount = 0;
        int totalClicks = 0;
        List<String> actions = new ArrayList<>();
        
        for (String key : lastClick.keySet()) {
            if (key.startsWith(playerPrefix)) {
                actionCount++;
                String action = key.substring(playerPrefix.length());
                actions.add(action);
                totalClicks += clickCounts.getOrDefault(key, 1);
            }
        }
        
        stats.put("trackedActions", actionCount);
        stats.put("totalClicks", totalClicks);
        stats.put("actions", actions);
        
        return stats;
    }
    
    /**
     * Clear tracking for player.
     */
    public synchronized void clearPlayer(UUID playerId) {
        if (playerId == null) return;
        String playerPrefix = playerId.toString() + ":";
        List<String> toRemove = new ArrayList<>();
        
        for (String key : lastClick.keySet()) {
            if (key.startsWith(playerPrefix)) {
                toRemove.add(key);
            }
        }
        
        for (String key : toRemove) {
            lastClick.remove(key);
            clickCounts.remove(key);
        }
    }
    
    /**
     * Clear old entries (older than window).
     */
    public synchronized int clearOldEntries() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : lastClick.entrySet()) {
            if ((now - entry.getValue()) > windowMs) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String key : toRemove) {
            lastClick.remove(key);
            clickCounts.remove(key);
        }
        
        return toRemove.size();
    }
    
    /**
     * Reset click count for action.
     */
    public synchronized void resetAction(UUID uuid, String actionKey) {
        if (uuid == null || actionKey == null) return;
        String key = uuid.toString() + ":" + actionKey;
        lastClick.remove(key);
        clickCounts.remove(key);
    }
}


