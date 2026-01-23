package com.axiom.listener;

import com.axiom.AXIOM;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for dashboard initialization and cleanup.
 */
public class DashboardListener implements Listener {
    private final AXIOM plugin;
    
    public DashboardListener(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Initialize dashboard after a short delay to ensure player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                plugin.getPlayerDashboardService().initializeDashboard(event.getPlayer());
            }
        }, 20); // 1 second delay
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerDashboardService().removeDashboard(event.getPlayer().getUniqueId());
    }
}

