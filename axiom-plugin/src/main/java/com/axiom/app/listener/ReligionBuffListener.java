package com.axiom.app.listener;

import com.axiom.AXIOM;
import com.axiom.domain.service.politics.ReligionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Applies religious buffs: holy site proximity (Regen I) and holiday effects. */
public class ReligionBuffListener implements Listener {
    private final AXIOM plugin;
    private final ReligionManager religionManager;
    private final Map<UUID, Long> lastHolySiteBuff = new HashMap<>();

    public ReligionBuffListener(AXIOM plugin, ReligionManager religionManager) {
        this.plugin = plugin;
        this.religionManager = religionManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;
        Player p = event.getPlayer();
        var chunk = event.getTo().getChunk();
        String key = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
        String religion = plugin.getPlayerDataManager().getReligion(p.getUniqueId());
        if (religion == null) return;
        
        // Check if near holy site (same chunk for now)
        boolean isHoly = religionManager.isHolySite(religion, key);
        if (isHoly) {
            long now = System.currentTimeMillis();
            Long last = lastHolySiteBuff.get(p.getUniqueId());
            if (last == null || now - last > 60_000) { // once per minute
                // Apply regeneration buff
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0, false, false));
                lastHolySiteBuff.put(p.getUniqueId(), now);
                
                // VISUAL EFFECTS: Holy site buff
                org.bukkit.Location loc = p.getLocation();
                plugin.getVisualEffectsService().playHolySiteEffect(p, loc);
                
                // Actionbar notification (only first time in chunk)
                if (last == null) {
                    plugin.getVisualEffectsService().sendActionBar(p, "§e✨ Вы рядом со святым местом! Regeneration I");
                }
            }
        }
    }
}

