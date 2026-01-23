package com.axiom.listener;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.NationManager;
import com.axiom.service.DiplomacySystem;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

/**
 * Cancels actions outside owned land, allowing only nation members where claimed.
 */
public class TerritoryProtectionListener implements Listener {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final DiplomacySystem diplomacySystem;

    public TerritoryProtectionListener(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.diplomacySystem = plugin.getDiplomacySystem();
    }

    private boolean isAllowed(Player player, Chunk chunk) {
        Optional<Nation> claim = nationManager.getNationClaiming(chunk.getWorld(), chunk.getX(), chunk.getZ());
        if (claim.isEmpty()) return true; // unclaimed = anarchy zone
        if (diplomacySystem.isWarzone(chunk.getWorld(), chunk.getX(), chunk.getZ())) return true; // warzone allows all
        Nation n = claim.get();
        return n.isMember(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("territory.protectionEnabled", true)) return;
        Player p = event.getPlayer();
        org.bukkit.block.Block block = event.getBlock();
        
        // MOD INTEGRATION: Check if block is protected mod resource
        String modId = plugin.getModIntegrationService().detectModFromBlock(block);
        if (modId != null) {
            // Industrial machines, energy generators, quarries are highly protected
            if (plugin.getModIntegrationService().isIndustrialMachine(block) ||
                plugin.getModIntegrationService().isEnergyBlock(block) ||
                plugin.getModIntegrationService().isResourceExtractor(block)) {
                if (!isAllowed(p, block.getChunk())) {
                    event.setCancelled(true);
                    // VISUAL EFFECTS: Block protection feedback
                    plugin.getVisualEffectsService().sendActionBar(p, "§c⚠ Защищённая территория!");
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
                    org.bukkit.Location loc = block.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK_CRACK, loc.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.1,
                        org.bukkit.Material.BARRIER.createBlockData());
                    p.sendMessage("§cВы не можете ломать промышленные блоки на территории другой нации.");
                    return;
                }
            }
        }
        
        if (!isAllowed(p, block.getChunk())) {
            event.setCancelled(true);
            // VISUAL EFFECTS: Block protection feedback
            Optional<Nation> owner = nationManager.getNationClaiming(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
            if (owner.isPresent()) {
                plugin.getVisualEffectsService().sendActionBar(p, "§c⚠ Территория нации '" + owner.get().getName() + "' защищена!");
            } else {
                plugin.getVisualEffectsService().sendActionBar(p, "§c⚠ Доступ запрещён!");
            }
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
            org.bukkit.Location loc = block.getLocation();
            loc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK_CRACK, loc.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.1,
                org.bukkit.Material.BARRIER.createBlockData());
            p.sendMessage("§cВы не можете ломать блоки на территории другой нации.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("territory.protectionEnabled", true)) return;
        Player p = event.getPlayer();
        org.bukkit.block.Block block = event.getBlockPlaced();
        
        // MOD INTEGRATION: Check if placing mod block
        String modId = plugin.getModIntegrationService().detectModFromBlock(block);
        if (modId != null) {
            // Industrial machines, energy generators need territory ownership
            if (plugin.getModIntegrationService().isIndustrialMachine(block) ||
                plugin.getModIntegrationService().isEnergyBlock(block) ||
                plugin.getModIntegrationService().isLogisticsBlock(block)) {
                if (!isAllowed(p, block.getChunk())) {
                    event.setCancelled(true);
                    // VISUAL EFFECTS: Block protection feedback
                    plugin.getVisualEffectsService().sendActionBar(p, "§c⚠ Защищённая территория!");
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
                    org.bukkit.Location loc = block.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK_CRACK, loc.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.1,
                        org.bukkit.Material.BARRIER.createBlockData());
                    p.sendMessage("§cВы не можете устанавливать промышленные блоки на территории другой нации.");
                    return;
                }
            }
            
            // Explosives/artillery only in warzone or own territory
            if (plugin.getModIntegrationService().isExplosiveBlock(block)) {
                if (diplomacySystem.isWarzone(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ())) {
                    return; // Allowed in warzone
                }
                if (!isAllowed(p, block.getChunk())) {
                    event.setCancelled(true);
                    // VISUAL EFFECTS: Block protection feedback
                    plugin.getVisualEffectsService().sendActionBar(p, "§c⚠ Взрывчатые вещества только в warzone!");
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_TNT_PRIMED, 0.5f, 1.5f);
                    org.bukkit.Location loc = block.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, loc.add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
                    p.sendMessage("§cВзрывчатые вещества можно устанавливать только в warzone или на своей территории.");
                    return;
                }
            }
        }
        
        if (!isAllowed(p, block.getChunk())) {
            event.setCancelled(true);
            // VISUAL EFFECTS: Block protection feedback
            Optional<Nation> owner = nationManager.getNationClaiming(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
            if (owner.isPresent()) {
                plugin.getVisualEffectsService().sendActionBar(p, "§c⚠ Территория нации '" + owner.get().getName() + "' защищена!");
            } else {
                plugin.getVisualEffectsService().sendActionBar(p, "§c⚠ Доступ запрещён!");
            }
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
            org.bukkit.Location loc = block.getLocation();
            loc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK_CRACK, loc.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.1,
                org.bukkit.Material.BARRIER.createBlockData());
            p.sendMessage("§cВы не можете ставить блоки на территории другой нации.");
        }
    }

    // PvP is now completely allowed - no restrictions
    // Removed onEntityDamage handler to allow PvP everywhere
}


