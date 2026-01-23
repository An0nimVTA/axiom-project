package com.axiom.gui;

import com.axiom.AXIOM;
import java.util.ArrayList;
import java.util.List;
import com.axiom.model.Nation;
import com.axiom.service.NationManager;
import com.axiom.service.ServiceLocator;
import com.axiom.service.VisualEffectsService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ProfileMenu implements Listener {

    private static final String[] GOVTS = new String[]{"Демократия","Монархия","Диктатура","Технократия","Теократия","Анархия","Федерация","Республика","Тирания"};

    public ProfileMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage("§cВы не в нации."); return; }
        Nation n = opt.get();
        
        String title = GuiUtils.formatHeader("Профиль: " + n.getName());
        Inventory inv = GuiUtils.createMenu(title, 4);
        
        List<String> flagLore = new ArrayList<>();
        flagLore.add("§7Shift-ЛКМ: установить из руки");
        flagLore.add("§fТекущий: §b" + n.getFlagIconMaterial());
        Material flagMat = Material.valueOf(n.getFlagIconMaterial() != null ? n.getFlagIconMaterial() : "BLUE_BANNER");
        inv.setItem(11, GuiUtils.button(flagMat, "§b§lФлаг нации", flagLore));
        
        List<String> mottoLore = new ArrayList<>();
        mottoLore.add("§7Девиз вашей нации (до 64 символов)");
        if (n.getMotto() != null && !n.getMotto().isEmpty()) {
            mottoLore.add(" ");
            mottoLore.add(GuiUtils.formatMotto(n.getMotto()));
        } else {
            mottoLore.add("§c§oНе установлен");
        }
        mottoLore.add(" ");
        mottoLore.add("§7Используйте команду:");
        mottoLore.add("§b/nation motto <текст>");
        inv.setItem(13, GuiUtils.button(Material.NAME_TAG, "§b§lДевиз", mottoLore));
        
        List<String> govLore = new ArrayList<>();
        govLore.add("§fТекущий тип: §b" + n.getGovernmentType());
        govLore.add(" ");
        govLore.add("§7ЛКМ: следующий тип");
        govLore.add("§7ПКМ: предыдущий тип");
        inv.setItem(15, GuiUtils.button(Material.BOOK, "§b§lПравительство", govLore));
        
        List<String> currencyLore = new ArrayList<>();
        currencyLore.add("§fКод валюты: §b" + n.getCurrencyCode());
        currencyLore.add("§fКурс к AXC: §b" + String.format("%.2f", n.getExchangeRateToAXC()));
        inv.setItem(17, GuiUtils.button(Material.PAPER, "§b§lВалюта", currencyLore));
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§3§l─── ИНФОРМАЦИЯ ───");
        infoLore.add(" ");
        infoLore.add("§fID: §7" + n.getId());
        infoLore.add("§fЛидер: §b" + (n.getLeader() != null ? Bukkit.getOfflinePlayer(n.getLeader()).getName() : "Неизвестно"));
        infoLore.add("§fГраждан: §b" + n.getCitizens().size());
        infoLore.add("§fТерриторий: §b" + n.getClaimedChunkKeys().size());
        
        Material nationFlag = Material.valueOf(n.getFlagIconMaterial() != null ? n.getFlagIconMaterial() : "BLUE_BANNER");
        inv.setItem(22, GuiUtils.button(nationFlag, "§b§l" + n.getName(), infoLore));
        
        inv.setItem(31, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Профиль")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
        int slot = e.getRawSlot();
        
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) return;
        Nation n = opt.get();
        Nation.Role r = n.getRole(p.getUniqueId());
        boolean canEdit = (r == Nation.Role.LEADER || r == Nation.Role.MINISTER);
        if (!canEdit) { 
            p.sendMessage("§cНедостаточно прав. Требуется: LEADER или MINISTER");
            return; 
        }
        if (slot == 11) {
            if (!e.isLeftClick() || !e.isShiftClick()) return;
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) { 
                p.sendMessage("§cВозьмите предмет-иконку в руку."); 
                return; 
            }
            n.setFlagIconMaterial(hand.getType().name());
            try { 
                nationManager.save(n);
                p.sendMessage("§aФлаг нации изменён!");
                ServiceLocator.get(VisualEffectsService.class).playNationJoinEffect(p);
            } catch (Exception ex) {
                p.sendMessage("§cОшибка сохранения: " + ex.getMessage());
            }
            open(p);
        } else if (slot == 15) {
            String cur = n.getGovernmentType();
            int idx = 0; 
            for (int i = 0; i < GOVTS.length; i++) {
                if (GOVTS[i].equalsIgnoreCase(cur)) { 
                    idx = i; 
                    break; 
                }
            }
            idx = e.isRightClick() ? (idx - 1 + GOVTS.length) % GOVTS.length : (idx + 1) % GOVTS.length;
            n.setGovernmentType(GOVTS[idx]);
            try { 
                nationManager.save(n);
                p.sendMessage("§aТип правительства изменён на: §b" + GOVTS[idx]);
            } catch (Exception ex) {
                p.sendMessage("§cОшибка сохранения: " + ex.getMessage());
            }
            open(p);
        } else if (slot == 31) {
            p.closeInventory();
        }
    }
}


