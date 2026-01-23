package com.axiom.listener;

import com.axiom.AXIOM;
import com.axiom.service.VisualEffectsService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener for visual effects based on player location and events.
 */
public class VisualEffectsListener implements Listener {
    private final AXIOM plugin;
    private final VisualEffectsService visualEffects;
    
    public VisualEffectsListener(AXIOM plugin, VisualEffectsService visualEffects) {
        this.plugin = plugin;
        this.visualEffects = visualEffects;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        
        // Check if in warzone
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (nationId != null) {
            var nation = plugin.getNationManager().getNationById(nationId);
            if (nation != null) {
                String chunkKey = loc.getChunk().getWorld().getName() + ":" + 
                    loc.getChunk().getX() + ":" + loc.getChunk().getZ();
                
                if (plugin.getDiplomacySystem().isWarzone(
                    loc.getWorld(), loc.getChunk().getX(), loc.getChunk().getZ())) {
                    // Play warzone effect occasionally
                    if (Math.random() < 0.05) { // 5% chance per move
                        visualEffects.playWarzoneEffect(player, loc);
                    }
                }
                
                // Check for holy sites (if player has religion)
                String religion = plugin.getPlayerDataManager().getReligion(player.getUniqueId());
                if (religion != null) {
                    // Check if current chunk is a holy site for this religion
                    if (plugin.getReligionManager().isHolySite(religion, chunkKey)) {
                        if (Math.random() < 0.1) { // 10% chance per move
                            visualEffects.playHolySiteEffect(player, loc);
                        }
                    }
                }
            }
        }
    }
}

