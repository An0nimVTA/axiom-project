package com.axiom.gui;

import com.axiom.model.Nation;
import com.axiom.service.NationManager;
import com.axiom.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.UUID;

public class HistoryMenu implements Listener {
    private final Map<UUID, String> playerFilters = new HashMap<>();

    public HistoryMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        open(p, null);
    }
    
    public void open(Player p, String filterType) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage("§cВы не в нации."); return; }
        Nation n = opt.get();
        
        String title = GuiUtils.formatHeader("История: " + n.getName());
        List<String> hist = n.getHistory();
        
        List<String> filteredHist = hist;
        if (filterType != null && !filterType.isEmpty()) {
            filteredHist = new ArrayList<>();
            String filterLower = filterType.toLowerCase();
            for (String entry : hist) {
                String entryLower = entry.toLowerCase();
                if (filterLower.equals("война") && entryLower.contains("война")) filteredHist.add(entry);
                else if (filterLower.equals("выборы") && (entryLower.contains("выборы") || entryLower.contains("президент"))) filteredHist.add(entry);
                else if (filterLower.equals("кризис") && (entryLower.contains("кризис") || entryLower.contains("пандемия"))) filteredHist.add(entry);
                else if (filterLower.equals("дипломатия") && (entryLower.contains("альянс") || entryLower.contains("договор") || entryLower.contains("санкции"))) filteredHist.add(entry);
                else if (filterLower.equals("экономика") && (entryLower.contains("экономик") || entryLower.contains("печать") || entryLower.contains("валюта"))) filteredHist.add(entry);
            }
        }
        
        int rows = Math.max(4, (filteredHist.size() / 7) + 3);
        Inventory inv = GuiUtils.createMenu(title, rows);
        
        inv.setItem(1, GuiUtils.button(Material.DIAMOND_SWORD, "§cВсе", Arrays.asList("§7Показать все события")));
        inv.setItem(2, GuiUtils.button(Material.CROSSBOW, "§cВойны", Arrays.asList("§7Фильтр: Войны")));
        inv.setItem(3, GuiUtils.button(Material.GOLDEN_HELMET, "§eВыборы", Arrays.asList("§7Фильтр: Выборы")));
        inv.setItem(4, GuiUtils.button(Material.TNT, "§6Кризисы", Arrays.asList("§7Фильтр: Кризисы")));
        inv.setItem(5, GuiUtils.button(Material.TURTLE_HELMET, "§bДипломатия", Arrays.asList("§7Фильтр: Дипломатия")));
        
        if (filteredHist.isEmpty()) {
            List<String> emptyLore = new ArrayList<>();
            if (filterType != null) {
                emptyLore.add("§7Нет событий по фильтру:");
                emptyLore.add("§7'" + filterType + "'");
            } else {
                emptyLore.add("§7История нации пуста");
                emptyLore.add(" ");
                emptyLore.add("§7Здесь будут отображаться:");
                emptyLore.add("§7- Войны и дипломатия");
                emptyLore.add("§7- Выборы и кризисы");
                emptyLore.add("§7- Экономические события");
            }
            inv.setItem(22, GuiUtils.button(Material.PAPER, "§7§oИстория пуста", emptyLore));
        } else {
            int slot = 10;
            for (int i = filteredHist.size() - 1; i >= 0; i--) {
                if (slot > 53) break;
                String entry = filteredHist.get(i);
                
                List<String> lore = new ArrayList<>();
                lore.add("§7" + entry);
                
                Material icon = Material.BOOK;
                String entryLower = entry.toLowerCase();
                if (entryLower.contains("война")) icon = Material.DIAMOND_SWORD;
                else if (entryLower.contains("выборы") || entryLower.contains("президент")) icon = Material.GOLDEN_HELMET;
                else if (entryLower.contains("кризис") || entryLower.contains("пандемия")) icon = Material.TNT;
                else if (entryLower.contains("альянс")) icon = Material.TURTLE_HELMET;
                else if (entryLower.contains("основан")) icon = Material.GOLD_BLOCK;
                
                String displayName = entry.length() > 32 ? entry.substring(0, 29) + "..." : entry;
                inv.setItem(slot, GuiUtils.button(icon, "§b§l" + displayName, lore));
                slot++;
                if ((slot - 10) % 7 == 0) slot += 2;
            }
        }
        
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§3§l─── СТАТИСТИКА ───");
        statsLore.add(" ");
        statsLore.add("§fВсего событий: §b" + hist.size());
        if (filterType != null && !filterType.isEmpty()) {
            statsLore.add("§fПо фильтру: §b" + filteredHist.size());
        }
        statsLore.add(" ");
        int wars = (int) hist.stream().filter(h -> h.toLowerCase().contains("война")).count();
        int elections = (int) hist.stream().filter(h -> h.toLowerCase().contains("выборы") || h.toLowerCase().contains("президент")).count();
        int crises = (int) hist.stream().filter(h -> h.toLowerCase().contains("кризис") || h.toLowerCase().contains("пандемия")).count();
        int diplomacy = (int) hist.stream().filter(h -> h.toLowerCase().contains("альянс") || h.toLowerCase().contains("договор") || h.toLowerCase().contains("санкции")).count();
        statsLore.add("§fВойн: §c" + wars);
        statsLore.add("§fВыборов: §e" + elections);
        statsLore.add("§fКризисов: §6" + crises);
        statsLore.add("§fДипломатия: §b" + diplomacy);
        inv.setItem((rows - 2) * 9 + 8, GuiUtils.button(Material.ENCHANTED_BOOK, "§b§lСтатистика", statsLore));
        
        inv.setItem((rows - 1) * 9 - 1, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("История")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        
        if (slot >= 1 && slot <= 5) {
            String filterType = null;
            switch (slot) {
                case 1: filterType = null; break;
                case 2: filterType = "война"; break;
                case 3: filterType = "выборы"; break;
                case 4: filterType = "кризис"; break;
                case 5: filterType = "дипломатия"; break;
            }
            playerFilters.put(p.getUniqueId(), filterType);
            open(p, filterType);
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
            return;
        }
        
        if (slot == (e.getInventory().getSize() - 1)) {
            p.closeInventory();
        }
    }
}

