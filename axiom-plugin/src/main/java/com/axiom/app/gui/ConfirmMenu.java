package com.axiom.app.gui;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ConfirmMenu implements Listener {
    private final AXIOM plugin;

    public ConfirmMenu(AXIOM plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player p, String title, String description, Runnable onYes, Runnable onNo) {
        // Beautiful confirmation menu with proper design
        Inventory inv = GuiUtils.createMenu(GuiUtils.formatHeader("Подтверждение: " + title), 3);
        
        // Description in center
        if (description != null && !description.isEmpty()) {
            List<String> desc = Arrays.asList(
                "§7§m━━━━━━━━━━━━━━━━━━━",
                " ",
                "§f" + description,
                " ",
                "§7§m━━━━━━━━━━━━━━━━━━━"
            );
            inv.setItem(13, GuiUtils.button(Material.BOOK, "§6§lПодтверждение действия", desc));
        }
        
        // Yes button (left, green)
        inv.setItem(11, GuiUtils.createConfirmYes());
        
        // No button (right, red)
        inv.setItem(15, GuiUtils.createConfirmNo());
        
        // Close button
        inv.setItem(22, GuiUtils.createCloseButton());
        
        plugin.getConfirmationService().set(p.getUniqueId(), onYes, onNo);
        p.openInventory(inv);
    }
    
    /**
     * Overload for backward compatibility.
     */
    public void open(Player p, String title, Runnable onYes, Runnable onNo) {
        open(p, title, "Вы уверены?", onYes, onNo);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Подтверждение")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        UUID id = p.getUniqueId();
        if (slot == 11) {
            // Yes button
            Runnable yes = plugin.getConfirmationService().consumeYes(id);
            if (yes != null) yes.run();
            p.closeInventory();
        } else if (slot == 15) {
            // No button
            Runnable no = plugin.getConfirmationService().consumeNo(id);
            if (no != null) no.run();
            p.closeInventory();
        } else if (slot == 22) {
            // Close button
            p.closeInventory();
        }
    }
}


