package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/** Manages in-game notifications for important events. */
public class NotificationService {
    private final AXIOM plugin;
    private final Map<UUID, Queue<String>> playerNotifications = new HashMap<>();

    public NotificationService(AXIOM plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::deliverNotifications, 0, 20 * 5); // every 5 seconds
    }

    public synchronized void notifyPlayer(UUID playerId, String message) {
        if (playerId == null || message == null || message.trim().isEmpty()) return;
        playerNotifications.computeIfAbsent(playerId, k -> new LinkedList<>()).offer(message);
    }

    public synchronized void notifyNation(String nationId, String message) {
        if (nationId == null || nationId.trim().isEmpty() || message == null || message.trim().isEmpty()) return;
        if (plugin.getPlayerDataManager() == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String playerNation = plugin.getPlayerDataManager().getNation(p.getUniqueId());
            if (playerNation != null && playerNation.equals(nationId)) {
                notifyPlayer(p.getUniqueId(), message);
            }
        }
    }

    public synchronized void notifyOnline(String message) {
        if (message == null || message.trim().isEmpty()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            notifyPlayer(p.getUniqueId(), message);
        }
    }

    private synchronized void deliverNotifications() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Queue<String> queue = playerNotifications.get(p.getUniqueId());
            if (queue != null && !queue.isEmpty()) {
                String msg = queue.poll();
                if (msg != null) {
                    p.sendMessage("§b[AXIOM] §f" + msg);
                }
            }
        }
    }
    
    /**
     * Get comprehensive notification statistics.
     */
    public synchronized Map<String, Object> getNotificationStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        if (playerId == null) {
            stats.put("pendingNotifications", 0);
            stats.put("hasNotifications", false);
            return stats;
        }
        
        Queue<String> queue = playerNotifications.get(playerId);
        int pendingCount = queue != null ? queue.size() : 0;
        
        stats.put("pendingNotifications", pendingCount);
        stats.put("hasNotifications", pendingCount > 0);
        
        // Next notification preview
        if (queue != null && !queue.isEmpty()) {
            String preview = queue.peek();
            stats.put("nextNotification", preview);
        }
        
        return stats;
    }
    
    /**
     * Get global notification statistics.
     */
    public synchronized Map<String, Object> getGlobalNotificationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPending = 0;
        int playersWithNotifications = 0;
        int maxQueueSize = 0;
        
        for (Queue<String> queue : playerNotifications.values()) {
            int size = queue.size();
            totalPending += size;
            if (size > 0) playersWithNotifications++;
            maxQueueSize = Math.max(maxQueueSize, size);
        }
        
        stats.put("totalPending", totalPending);
        stats.put("playersWithNotifications", playersWithNotifications);
        stats.put("maxQueueSize", maxQueueSize);
        stats.put("averageQueueSize", playerNotifications.isEmpty() ? 0 : (double) totalPending / playerNotifications.size());
        stats.put("totalPlayersQueued", playerNotifications.size());
        
        return stats;
    }
    
    /**
     * Clear all notifications for player.
     */
    public synchronized void clearNotifications(UUID playerId) {
        Queue<String> queue = playerNotifications.get(playerId);
        if (queue != null) {
            queue.clear();
        }
    }
    
    /**
     * Get notification queue size.
     */
    public synchronized int getQueueSize(UUID playerId) {
        Queue<String> queue = playerNotifications.get(playerId);
        return queue != null ? queue.size() : 0;
    }
    
    /**
     * Check if player has pending notifications.
     */
    public synchronized boolean hasPendingNotifications(UUID playerId) {
        Queue<String> queue = playerNotifications.get(playerId);
        return queue != null && !queue.isEmpty();
    }
}

