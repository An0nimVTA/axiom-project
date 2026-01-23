package com.axiom.listener;

import com.axiom.AXIOM;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for mod-related events and integrates with AXIOM systems.
 */
public class ModIntegrationListener implements Listener {
    private final AXIOM plugin;
    
    public ModIntegrationListener(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Track mod resource extraction for economy.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Detect mod block
        String modId = plugin.getModIntegrationService().detectModFromBlock(block);
        if (modId == null) return;
        
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (nationId == null) return;
        
        // Record resource extraction
        if (plugin.getModIntegrationService().isResourceExtractor(block) ||
            plugin.getModIntegrationService().isIndustrialMachine(block)) {
            // Drop items are tracked via BlockBreakEvent drops
            ItemStack[] drops = block.getDrops().toArray(new ItemStack[0]);
            for (ItemStack drop : drops) {
                if (drop != null) {
                    plugin.getModResourceService().recordModItemExtraction(nationId, drop);
                }
            }
        }
    }
    
    /**
     * Track weapon usage in combat.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModWeaponUse(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        
        String weaponMod = plugin.getModWarfareService().getPlayerWeaponType(attacker);
        if (weaponMod != null) {
            plugin.getModWarfareService().recordWeaponUsage(attacker, weaponMod);
        }
    }
    
    /**
     * Track mod item transactions in inventories (AE2, trading, etc.).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModItemTransaction(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        
        String modId = plugin.getModIntegrationService().detectModFromItem(item);
        if (modId == null) return;
        
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (nationId == null) return;
        
        // AE2 network interactions could be tracked here
        if ("appliedenergistics2".equals(modId)) {
            // Track AE2 network usage (potential for trade bonuses)
        }
    }
    
    /**
     * Track mod block interactions (machines, generators, etc.).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onModBlockInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        
        String modId = plugin.getModIntegrationService().detectModFromBlock(block);
        if (modId == null) return;
        
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (nationId == null) return;
        
        // Energy generators - track energy production
        if (plugin.getModIntegrationService().isEnergyBlock(block)) {
            // Energy production is tracked automatically by ModEnergyService
        }
        
        // Industrial machines - could trigger production bonuses
        if (plugin.getModIntegrationService().isIndustrialMachine(block)) {
            // Production bonuses from technology tree
            double bonus = plugin.getTechnologyTreeService().getBonus(nationId, "productionBonus");
            if (bonus > 1.0) {
                // Apply production bonus (implemented in mod-specific production handlers)
            }
        }
    }
}

