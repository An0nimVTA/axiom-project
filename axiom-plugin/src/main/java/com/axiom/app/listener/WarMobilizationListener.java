package com.axiom.app.listener;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.politics.DiplomacySystem;
import com.axiom.domain.service.state.NationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;

/** Applies mobilization buff (damage boost) to citizens at war. */
public class WarMobilizationListener implements Listener {
    private final NationManager nationManager;
    private final DiplomacySystem diplomacySystem;

    public WarMobilizationListener(AXIOM plugin, NationManager nationManager, DiplomacySystem diplomacySystem) {
        this.nationManager = nationManager;
        this.diplomacySystem = diplomacySystem;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        Optional<Nation> opt = nationManager.getNationOfPlayer(attacker.getUniqueId());
        if (opt.isEmpty()) return;
        Nation n = opt.get();
        boolean atWar = n.getEnemies().stream().anyMatch(eid -> {
            Nation other = nationManager.getNationById(eid);
            return other != null && diplomacySystem.isAtWar(n.getId(), other.getId());
        });
        if (!atWar) return;
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 0, false, false));
        
        // VISUAL EFFECTS: Mobilization buff visual feedback (occasionally)
        if (Math.random() < 0.1) { // 10% chance per hit
            org.bukkit.Location loc = attacker.getLocation();
            loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0,
                new org.bukkit.Particle.DustOptions(org.bukkit.Color.ORANGE, 1.5f));
        }
    }
}

