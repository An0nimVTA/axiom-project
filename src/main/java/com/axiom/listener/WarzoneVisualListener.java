package com.axiom.listener;

import com.axiom.AXIOM;
import com.axiom.service.DiplomacySystem;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Shows visual feedback (particles/messages) when entering warzone. */
public class WarzoneVisualListener implements Listener {
    private final AXIOM plugin;
    private final DiplomacySystem diplomacySystem;
    private final Set<UUID> warnedPlayers = new HashSet<>();

    public WarzoneVisualListener(AXIOM plugin, DiplomacySystem diplomacySystem) {
        this.plugin = plugin;
        this.diplomacySystem = diplomacySystem;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;
        Player p = event.getPlayer();
        var chunk = event.getTo().getChunk();
        boolean warzone = diplomacySystem.isWarzone(chunk.getWorld(), chunk.getX(), chunk.getZ());
        if (warzone && !warnedPlayers.contains(p.getUniqueId())) {
            warnedPlayers.add(p.getUniqueId());
            
            // Beautiful warzone warning
            p.sendTitle("§4§l[БОЕВАЯ ЗОНА]", "§cВойна активна! Все правила отменены.", 10, 80, 20);
            plugin.getVisualEffectsService().sendActionBar(p, "§c⚔ WARZONE: Боевая зона активна!");
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.9f);
            
            // Continuous warzone particles
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks++ > 200 || !diplomacySystem.isWarzone(chunk.getWorld(), chunk.getX(), chunk.getZ())) {
                        // Stop after 10 seconds or if left warzone
                        warnedPlayers.remove(p.getUniqueId());
                        cancel();
                        return;
                    }
                    // Red particles
                    org.bukkit.Location loc = p.getLocation().add(0, 1, 0);
                    chunk.getWorld().spawnParticle(Particle.REDSTONE, loc, 5, 0.5, 1, 0.5, 0, 
                        new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
                    // Dark smoke
                    chunk.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.3, 0.5, 0.3, 0.05);
                }
            }.runTaskTimer(plugin, 0, 10);
        }
    }
}

