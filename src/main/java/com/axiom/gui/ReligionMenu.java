package com.axiom.gui;

import com.axiom.service.PlayerDataManager;
import com.axiom.service.ReligionManager;
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

public class ReligionMenu implements Listener {
    public ReligionMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        String title = GuiUtils.formatHeader("Религия");
        Inventory inv = GuiUtils.createMenu(title, 4);
        
        PlayerDataManager pdm = ServiceLocator.get(PlayerDataManager.class);
        ReligionManager religionManager = ServiceLocator.get(ReligionManager.class);
        String playerReligion = pdm.getReligion(p.getUniqueId());
        
        if (playerReligion != null) {
            var religionData = religionManager.getReligion(playerReligion);
            if (religionData != null) {
                List<String> religionLore = new ArrayList<>();
                religionLore.add("§3§l─── ВАША РЕЛИГИЯ ───");
                religionLore.add(" ");
                religionLore.add("§fНазвание: §b" + religionData.name);
                religionLore.add("§fID: §7" + religionData.id);
                religionLore.add(" ");
                religionLore.add("§7Десятина: §b5% §7от дохода");
                religionLore.add("§7Бонусы: §a+5% §7доход с ферм/шахт");
                religionLore.add("§7Регенерация: §aRegen I §7около святынь");
                
                inv.setItem(13, GuiUtils.button(Material.ENCHANTING_TABLE, "§b§l" + religionData.name, religionLore));
            }
        } else {
            List<String> noReligionLore = new ArrayList<>();
            noReligionLore.add("§7Вы не состоите в религии");
            noReligionLore.add(" ");
            noReligionLore.add("§7Основать новую религию:");
            noReligionLore.add("§b/axiom religion found <id> <name>");
            inv.setItem(13, GuiUtils.button(Material.PAPER, "§7§oНет религии", noReligionLore));
        }
        
        List<String> foundLore = new ArrayList<>();
        foundLore.add("§aОсновать новую религию");
        foundLore.add(" ");
        foundLore.add("§7Используйте команду:");
        foundLore.add("§b/axiom religion found <id> <name>");
        inv.setItem(11, GuiUtils.button(Material.BOOK, "§a§lОсновать религию", foundLore));
        
        List<String> titheLore = new ArrayList<>();
        titheLore.add("§6§lДесятина");
        titheLore.add(" ");
        titheLore.add("§7Автоматически 5% от");
        titheLore.add("§7вашего дохода уходит");
        titheLore.add("§7в религию");
        
        if (playerReligion != null) {
            var religionData = religionManager.getReligion(playerReligion);
            if (religionData != null) {
                titheLore.add(" ");
                titheLore.add("§fВаша религия:");
                titheLore.add("§b" + religionData.name);
            }
        }
        
        titheLore.add(" ");
        titheLore.add("§7Бонусы за участие:");
        titheLore.add("§a+5% доход с ферм");
        titheLore.add("§a+5% доход с шахт");
        titheLore.add("§aRegen I у святынь");
        inv.setItem(15, GuiUtils.button(Material.GOLD_NUGGET, "§6§lДесятина", titheLore));
        
        List<String> religionsLore = new ArrayList<>();
        religionsLore.add("§b§lВСЕ РЕЛИГИИ");
        religionsLore.add(" ");
        
        int religionCount = religionManager.getAllReligions().size();
        religionsLore.add("§fВсего религий: §b" + religionCount);
        religionsLore.add(" ");
        
        var allReligions = religionManager.getAllReligions();
        int shown = 0;
        for (var rel : allReligions) {
            if (shown >= 5) break;
            religionsLore.add("§b• " + rel.name);
            shown++;
        }
        
        if (religionCount > 5) {
            religionsLore.add(" ");
            religionsLore.add("§7... и ещё " + (religionCount - 5));
        }
        
        religionsLore.add(" ");
        religionsLore.add("§7ЛКМ: Присоединиться");
        inv.setItem(19, GuiUtils.button(Material.BOOKSHELF, "§b§lРелигии", religionsLore));
        
        List<String> holyLore = new ArrayList<>();
        holyLore.add("§e§lСвятые места");
        holyLore.add(" ");
        holyLore.add("§7Святые места дают:");
        holyLore.add("§aRegen I §7эффект");
        holyLore.add("§7(10 секунд каждый час)");
        holyLore.add(" ");
        holyLore.add("§7Добавить святое место:");
        holyLore.add("§b/axiom religion add-holy <id> <world:x:z>");
        inv.setItem(17, GuiUtils.button(Material.BEACON, "§e§lСвятые места", holyLore));
        
        inv.setItem(31, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Религия")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
        int slot = e.getRawSlot();
        
        if (slot == 31) {
            p.closeInventory();
        } else if (slot == 17) {
            var c = p.getLocation().getChunk();
            String key = c.getWorld().getName() + ":" + c.getX() + ":" + c.getZ();
            p.closeInventory();
            p.sendMessage("§7Добавьте святое место:");
            p.sendMessage("§b/axiom religion add-holy <religionId> " + key);
        } else if (slot == 11) {
            p.closeInventory();
            p.sendMessage("§7Основать религию:");
            p.sendMessage("§b/axiom religion found <id> <name>");
        } else if (slot == 19) {
            p.closeInventory();
            p.sendMessage("§7Доступные религии:");
            ReligionManager religionManager = ServiceLocator.get(ReligionManager.class);
            var allReligions = religionManager.getAllReligions();
            if (allReligions.isEmpty()) {
                p.sendMessage("§7Пока нет религий. Основать: §b/axiom religion found <id> <name>");
            } else {
                for (var rel : allReligions) {
                    int followers = religionManager.getFollowerCount(rel.id);
                    p.sendMessage("§b• " + rel.name + " §7(ID: " + rel.id + ", последователей: " + followers + ")");
                }
                p.sendMessage(" ");
                p.sendMessage("§7Присоединиться: §b/axiom religion join <religionId>");
            }
        }
    }
}


