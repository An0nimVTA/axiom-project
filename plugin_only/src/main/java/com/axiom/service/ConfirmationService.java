package com.axiom.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfirmationService {
    private static class Pair { Runnable yes; Runnable no; }
    private final Map<UUID, Pair> map = new HashMap<>();
    private final Map<UUID, Long> confirmationTimes = new HashMap<>(); // Track when confirmations were set

    public synchronized void set(UUID uuid, Runnable yes, Runnable no) {
        Pair p = new Pair(); 
        p.yes = yes; 
        p.no = no; 
        map.put(uuid, p);
        confirmationTimes.put(uuid, System.currentTimeMillis());
    }
    
    public synchronized Runnable consumeYes(UUID uuid) {
        Pair p = map.remove(uuid);
        confirmationTimes.remove(uuid);
        return p != null ? p.yes : null;
    }
    
    public synchronized Runnable consumeNo(UUID uuid) {
        Pair p = map.remove(uuid);
        confirmationTimes.remove(uuid);
        return p != null ? p.no : null;
    }
    
    /**
     * Get comprehensive confirmation statistics.
     */
    public synchronized Map<String, Object> getConfirmationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeConfirmations", map.size());
        
        // Confirmation ages
        List<Map<String, Object>> confirmationsList = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : confirmationTimes.entrySet()) {
            Map<String, Object> confData = new HashMap<>();
            confData.put("playerId", entry.getKey().toString());
            long age = now - entry.getValue();
            confData.put("ageSeconds", age / 1000);
            confData.put("ageMinutes", age / 1000 / 60);
            confirmationsList.add(confData);
        }
        stats.put("confirmationsList", confirmationsList);
        
        // Average age
        if (!confirmationTimes.isEmpty()) {
            double avgAge = confirmationTimes.values().stream()
                .mapToLong(time -> now - time)
                .average()
                .orElse(0) / 1000.0;
            stats.put("averageAgeSeconds", avgAge);
        }
        
        // Old confirmations (older than 5 minutes)
        long oldCount = confirmationTimes.values().stream()
            .filter(time -> (now - time) > 5 * 60 * 1000)
            .count();
        stats.put("oldConfirmations", oldCount);
        
        return stats;
    }
    
    /**
     * Get confirmation status for player.
     */
    public synchronized Map<String, Object> getPlayerConfirmationStatus(UUID playerId) {
        Map<String, Object> status = new HashMap<>();
        
        boolean hasConfirmation = map.containsKey(playerId);
        status.put("hasConfirmation", hasConfirmation);
        
        if (hasConfirmation) {
            Long setTime = confirmationTimes.get(playerId);
            if (setTime != null) {
                long age = System.currentTimeMillis() - setTime;
                status.put("ageSeconds", age / 1000);
                status.put("ageMinutes", age / 1000 / 60);
            }
        }
        
        return status;
    }
    
    /**
     * Cancel confirmation for player.
     */
    public synchronized void cancel(UUID playerId) {
        map.remove(playerId);
        confirmationTimes.remove(playerId);
    }
    
    /**
     * Clear old confirmations (older than timeout).
     */
    public synchronized int clearOldConfirmations(long timeoutMs) {
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, Long> entry : confirmationTimes.entrySet()) {
            if ((now - entry.getValue()) > timeoutMs) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (UUID uuid : toRemove) {
            map.remove(uuid);
            confirmationTimes.remove(uuid);
        }
        
        return toRemove.size();
    }
    
    /**
     * Check if player has active confirmation.
     */
    public synchronized boolean hasConfirmation(UUID playerId) {
        return map.containsKey(playerId);
    }
}


