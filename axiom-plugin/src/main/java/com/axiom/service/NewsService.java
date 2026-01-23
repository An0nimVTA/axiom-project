package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;

import java.util.*;

public class NewsService {
    private final AXIOM plugin;
    private final NationManager nationManager;

    public NewsService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        long interval = plugin.getConfig().getLong("events.newsBroadcastIntervalSeconds", 900) * 20L;
        if (interval > 0) {
            Bukkit.getScheduler().runTaskTimer(plugin, this::broadcast, interval, interval);
        }
    }

    public void broadcast() {
        List<String> feed = collectRecent();
        if (feed.isEmpty()) return;
        String msg = feed.get((int) (Math.random() * feed.size()));
        
        // Beautiful news broadcast with actionbar
        String newsMessage = "§b[AXIOM News] §f" + msg;
        Bukkit.getServer().broadcastMessage(newsMessage);
        
        // VISUAL EFFECTS: Actionbar for all players
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getVisualEffectsService() != null) {
                plugin.getVisualEffectsService().sendActionBar(player, "§b📰 " + msg);
            }
        }
    }

    private List<String> collectRecent() {
        List<String> out = new ArrayList<>();
        if (nationManager == null) return out;
        // naive: collect last 3 entries per nation
        for (Nation n : nationManager.getAll()) {
            List<String> h = n.getHistory();
            if (h == null) continue;
            for (int i = Math.max(0, h.size() - 3); i < h.size(); i++) {
                out.add("§7[" + n.getName() + "] §f" + h.get(i));
            }
        }
        return out;
    }
    
    /**
     * Get comprehensive news statistics.
     */
    public synchronized Map<String, Object> getNewsStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Collect all recent news
        List<String> recentNews = collectRecent();
        stats.put("totalRecentNews", recentNews.size());
        
        // News by nation
        Map<String, Integer> newsByNation = new HashMap<>();
        if (nationManager != null) {
            for (Nation n : nationManager.getAll()) {
                List<String> history = n.getHistory();
                int recentCount = history != null ? Math.min(3, history.size()) : 0;
                if (recentCount > 0) {
                    newsByNation.put(n.getName(), recentCount);
                }
            }
        }
        stats.put("newsByNation", newsByNation);
        
        // Most active nations (by news)
        List<Map.Entry<String, Integer>> topNations = new ArrayList<>(newsByNation.entrySet());
        topNations.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        stats.put("topNationsByNews", topNations.stream().limit(5).collect(java.util.stream.Collectors.toList()));
        
        // Next broadcast time
        long interval = plugin.getConfig().getLong("events.newsBroadcastIntervalSeconds", 900);
        stats.put("broadcastInterval", interval);
        
        return stats;
    }
    
    /**
     * Manually broadcast a specific news item.
     */
    public synchronized void broadcastNews(String message) {
        if (message == null || message.trim().isEmpty()) return;
        String newsMessage = "§b[AXIOM News] §f" + message;
        org.bukkit.Bukkit.getServer().broadcastMessage(newsMessage);
        
        // VISUAL EFFECTS: Actionbar for all players
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (plugin.getVisualEffectsService() != null) {
                plugin.getVisualEffectsService().sendActionBar(player, "§b📰 " + message);
            }
        }
    }
    
    /**
     * Get news feed for a specific nation.
     */
    public synchronized List<String> getNationNews(String nationId) {
        if (nationId == null || nationId.trim().isEmpty() || nationManager == null) return Collections.emptyList();
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return Collections.emptyList();
        
        List<String> news = new ArrayList<>();
        List<String> history = n.getHistory();
        if (history != null) {
            for (int i = Math.max(0, history.size() - 10); i < history.size(); i++) {
                news.add("§7[" + n.getName() + "] §f" + history.get(i));
            }
        }
        return news;
    }
    
    /**
     * Get global news statistics across all nations (alias for consistency).
     */
    public synchronized Map<String, Object> getGlobalNewsStatistics() {
        return getNewsStatistics();
    }
}


