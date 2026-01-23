package com.axiom.listener;

import com.axiom.AXIOM;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener for tracking leader activity for inactive nation detection.
 */
public class LeaderActivityListener implements Listener {
    private final AXIOM plugin;
    
    public LeaderActivityListener(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getBalancingService() != null) {
            plugin.getBalancingService().updateLeaderActivity(event.getPlayer());
        }
    }
}

