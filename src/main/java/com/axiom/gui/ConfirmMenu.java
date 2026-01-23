package com.axiom.gui;

import com.axiom.service.ConfirmationService;
import com.axiom.service.ServiceLocator;
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

    public ConfirmMenu() {
        // Empty constructor
    }

    public void open(Player p, String title, String description, Runnable onYes, Runnable onNo) {
        Inventory inv = GuiUtils.createMenu(GuiUtils.formatHeader("Подтверждение: " + title), 3);
        
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
        
        inv.setItem(11, GuiUtils.createConfirmYes());
        inv.setItem(15, GuiUtils.createConfirmNo());
        inv.setItem(22, GuiUtils.createCloseButton());
        
        ServiceLocator.get(ConfirmationService.class).set(p.getUniqueId(), onYes, onNo);
        p.openInventory(inv);
    }
    
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
        ConfirmationService confirmationService = ServiceLocator.get(ConfirmationService.class);
        if (slot == 11) {
            Runnable yes = confirmationService.consumeYes(id);
            if (yes != null) yes.run();
            p.closeInventory();
        } else if (slot == 15) {
            Runnable no = confirmationService.consumeNo(id);
            if (no != null) no.run();
            p.closeInventory();
        } else if (slot == 22) {
            p.closeInventory();
        }
    }
}


