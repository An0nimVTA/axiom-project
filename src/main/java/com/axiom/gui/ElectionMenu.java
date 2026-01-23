package com.axiom.gui;

import com.axiom.model.Nation;
import com.axiom.service.ElectionService;
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
import java.util.List;
import java.util.Map;

public class ElectionMenu implements Listener {

    public ElectionMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        ElectionService electionService = ServiceLocator.get(ElectionService.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage("§cВы не в нации."); return; }
        Nation n = opt.get();
        
        String title = GuiUtils.formatHeader("Выборы: " + n.getName());
        Inventory inv = GuiUtils.createMenu(title, 5);
        
        Nation.Role role = n.getRole(p.getUniqueId());
        boolean canStart = (role == Nation.Role.LEADER || role == Nation.Role.MINISTER);
        
        List<String> presLore = new ArrayList<>();
        presLore.add("§6§lВыборы Президента");
        presLore.add(" ");
        if (!canStart) {
            presLore.add("§cНедостаточно прав");
        } else {
            presLore.add("§7ЛКМ: начать (5 мин)");
            presLore.add("§7ПКМ: начать (30 мин)");
            presLore.add("§7Shift: начать (2 часа)");
        }
        presLore.add(" ");
        ElectionService.Election presElection = electionService.getActiveElection(n.getId(), "president");
        if (presElection != null) {
            long remaining = (presElection.endTime - System.currentTimeMillis()) / 1000 / 60;
            presLore.add("§a✓ Активны!");
            presLore.add("§fОсталось: §b" + remaining + " мин");
        }
        inv.setItem(11, GuiUtils.button(Material.GOLDEN_HELMET, "§6§lПрезидент", presLore));
        
        List<String> parlLore = new ArrayList<>();
        parlLore.add("§6§lВыборы Парламента");
        parlLore.add(" ");
        if (!canStart) {
            parlLore.add("§cНедостаточно прав");
        } else {
            parlLore.add("§7ЛКМ: начать (10 мин)");
            parlLore.add("§7ПКМ: начать (1 час)");
        }
        ElectionService.Election parlElection = electionService.getActiveElection(n.getId(), "parliament");
        if (parlElection != null) {
            long remaining = (parlElection.endTime - System.currentTimeMillis()) / 1000 / 60;
            parlLore.add(" ");
            parlLore.add("§a✓ Активны!");
            parlLore.add("§fОсталось: §b" + remaining + " мин");
        }
        inv.setItem(13, GuiUtils.button(Material.BOOK, "§6§lПарламент", parlLore));
        
        List<String> lawLore = new ArrayList<>();
        lawLore.add("§6§lГолосование по Законам");
        lawLore.add(" ");
        if (!canStart) {
            lawLore.add("§cНедостаточно прав");
        } else {
            lawLore.add("§7ЛКМ: начать (15 мин)");
            lawLore.add("§7ПКМ: начать (1 час)");
        }
        ElectionService.Election lawElection = electionService.getActiveElection(n.getId(), "law");
        if (lawElection != null) {
            long remaining = (lawElection.endTime - System.currentTimeMillis()) / 1000 / 60;
            lawLore.add(" ");
            lawLore.add("§a✓ Активны!");
            lawLore.add("§fОсталось: §b" + remaining + " мин");
        }
        inv.setItem(15, GuiUtils.button(Material.PAPER, "§6§lЗаконы", lawLore));
        
        List<String> minLore = new ArrayList<>();
        minLore.add("§6§lВыборы Министров");
        minLore.add(" ");
        if (!canStart) {
            minLore.add("§cНедостаточно прав");
        } else {
            minLore.add("§7ЛКМ: начать (20 мин)");
            minLore.add("§7ПКМ: начать (2 часа)");
        }
        ElectionService.Election minElection = electionService.getActiveElection(n.getId(), "minister");
        if (minElection != null) {
            long remaining = (minElection.endTime - System.currentTimeMillis()) / 1000 / 60;
            minLore.add(" ");
            minLore.add("§a✓ Активны!");
            minLore.add("§fОсталось: §b" + remaining + " мин");
        }
        inv.setItem(17, GuiUtils.button(Material.EMERALD, "§6§lМинистры", minLore));
        
        int activeCount = (presElection != null ? 1 : 0) + (parlElection != null ? 1 : 0) + 
                         (lawElection != null ? 1 : 0) + (minElection != null ? 1 : 0);
        List<String> activeLore = new ArrayList<>();
        activeLore.add("§3§l─── АКТИВНЫЕ ВЫБОРЫ ───");
        activeLore.add(" ");
        activeLore.add("§fАктивных: §b" + activeCount);
        activeLore.add(" ");
        activeLore.add("§7ЛКМ: Просмотр деталей");
        inv.setItem(22, GuiUtils.button(Material.COMPASS, "§b§lАктивные выборы", activeLore));
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§3§l─── ИНФОРМАЦИЯ ───");
        infoLore.add(" ");
        infoLore.add("§7Тип правительства влияет");
        infoLore.add("§7на доступность выборов");
        infoLore.add(" ");
        infoLore.add("§fТип: §b" + n.getGovernmentType());
        inv.setItem(30, GuiUtils.button(Material.ENCHANTED_BOOK, "§b§lИнформация", infoLore));
        
        inv.setItem(40, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Выборы")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) return;
        Nation n = opt.get();
        int slot = e.getRawSlot();
        
        if (slot == 40) {
            p.closeInventory();
            return;
        }
        
        if (slot == 22) {
            showActiveElections(p, n);
            return;
        }
        
        Nation.Role role = n.getRole(p.getUniqueId());
        if (role != Nation.Role.LEADER && role != Nation.Role.MINISTER) {
            p.sendMessage("§cНедостаточно прав.");
            return;
        }
        
        if (slot == 11) {
            long duration = e.isShiftClick() ? 120 : (e.isRightClick() ? 30 : 5);
            p.closeInventory();
            p.sendMessage("§7Используйте: §b/axiom election start president <duration> <candidate1> [candidate2] ...");
            p.sendMessage("§7Пример: §b/axiom election start president " + duration + " Игрок1 Игрок2");
        } else if (slot == 13) {
            p.closeInventory();
            long duration = e.isRightClick() ? 60 : 10;
            p.sendMessage("§7Используйте: §b/axiom election start parliament <duration> <candidate1> ...");
            p.sendMessage("§7Пример: §b/axiom election start parliament " + duration + " Игрок1 Игрок2");
        } else if (slot == 15) {
            p.closeInventory();
            long duration = e.isRightClick() ? 60 : 15;
            p.sendMessage("§7Используйте: §b/axiom election start law <duration> <закон1> <закон2>");
            p.sendMessage("§7Пример: §b/axiom election start law " + duration + " Закон1 Закон2");
        } else if (slot == 17) {
            p.closeInventory();
            long duration = e.isRightClick() ? 120 : 20;
            p.sendMessage("§7Используйте: §b/axiom election start minister <duration> <candidate1> ...");
            p.sendMessage("§7Пример: §b/axiom election start minister " + duration + " Игрок1");
        }
    }

    private void showActiveElections(Player p, Nation n) {
        ElectionService electionService = ServiceLocator.get(ElectionService.class);
        String title = GuiUtils.formatHeader("Активные выборы: " + n.getName());
        Inventory inv = GuiUtils.createMenu(title, 4);
        int slot = 10;
        String[] types = {"president", "parliament", "law", "minister"};
        String[] typeNames = {"Президент", "Парламент", "Законы", "Министры"};
        Material[] typeIcons = {Material.GOLDEN_HELMET, Material.BOOK, Material.PAPER, Material.EMERALD};
        
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            ElectionService.Election election = electionService.getActiveElection(n.getId(), type);
            if (election != null) {
                long remaining = (election.endTime - System.currentTimeMillis()) / 1000 / 60;
                List<String> lore = new ArrayList<>();
                lore.add("§3§l─── " + typeNames[i] + " ───");
                lore.add(" ");
                lore.add("§fОсталось: §b" + remaining + " мин");
                lore.add("§fКандидаты: §b" + election.candidates.size());
                lore.add(" ");
                if (!election.votes.isEmpty()) {
                    Map<String, Integer> results = electionService.getResults(n.getId(), type);
                    lore.add("§7Голосов: §b" + election.votes.size());
                    lore.add(" ");
                    lore.add("§fРезультаты:");
                    for (var entry : results.entrySet()) {
                        lore.add("§f" + entry.getKey() + ": §a" + entry.getValue());
                    }
                } else {
                    lore.add("§7Голосов пока нет");
                }
                inv.setItem(slot++, GuiUtils.button(typeIcons[i], "§6§l" + typeNames[i], lore));
                if ((slot - 10) % 7 == 0) slot += 2;
            }
        }
        
        inv.setItem(31, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }
}

