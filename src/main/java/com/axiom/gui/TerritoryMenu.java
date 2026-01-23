package com.axiom.gui;

import com.axiom.model.Nation;
import com.axiom.service.DiplomacySystem;
import com.axiom.service.DoubleClickService;
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

import java.util.ArrayList;
import java.util.List;

public class TerritoryMenu implements Listener {

    public TerritoryMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage("§cВы не в нации."); return; }
        Nation n = opt.get();
        
        String title = GuiUtils.formatHeader("Территория: " + n.getName());
        Inventory inv = GuiUtils.createMenu(title, 4);
        
        int pop = n.getCitizens().size();
        int max = 100 + (pop * 2);
        int current = n.getClaimedChunkKeys().size();
        
        Nation.Role role = n.getRole(p.getUniqueId());
        boolean canClaim = (role == Nation.Role.LEADER || role == Nation.Role.GOVERNOR || role == Nation.Role.MINISTER);
        
        List<String> claimLore = new ArrayList<>();
        claimLore.add("§aКлеймнуть текущий чанк");
        claimLore.add(" ");
        claimLore.add("§fТерриторий: §b" + current + "/" + max);
        if (!canClaim) {
            claimLore.add(" ");
            claimLore.add("§cНедостаточно прав!");
        } else {
            claimLore.add(" ");
            claimLore.add("§7ЛКМ: Клеймнуть");
            claimLore.add("§e(Требуется двойной клик)");
        }
        Material claimMat = canClaim ? Material.GREEN_CONCRETE : Material.BARRIER;
        inv.setItem(11, GuiUtils.button(claimMat, canClaim ? "§a§lКлеймнуть чанк" : "§c§lКлеймнуть чанк", claimLore));
        
        List<String> unclaimLore = new ArrayList<>();
        unclaimLore.add("§cАнклеймнуть текущий чанк");
        unclaimLore.add(" ");
        unclaimLore.add("§7Кулдаун: 5 минут");
        if (!canClaim) {
            unclaimLore.add(" ");
            unclaimLore.add("§cНедостаточно прав!");
        } else {
            unclaimLore.add(" ");
            unclaimLore.add("§7ЛКМ: Анклеймнуть");
            unclaimLore.add("§e(Требуется двойной клик)");
        }
        inv.setItem(13, GuiUtils.button(canClaim ? Material.RED_CONCRETE : Material.BARRIER, 
            canClaim ? "§c§lАнклеймнуть чанк" : "§c§lАнклеймнуть чанк", unclaimLore));
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§3§l─── ИНФОРМАЦИЯ ───");
        infoLore.add(" ");
        infoLore.add("§fСтолица:");
        if (n.getCapitalChunkStr() != null && !n.getCapitalChunkStr().isEmpty()) {
            infoLore.add("§b" + n.getCapitalChunkStr());
        } else {
            infoLore.add("§7Не установлена");
        }
        infoLore.add(" ");
        infoLore.add("§fТерриторий: §b" + current);
        infoLore.add("§fМакс. территорий: §b" + max);
        infoLore.add("§fСвободно мест: §b" + (max - current));
        inv.setItem(15, GuiUtils.button(Material.COMPASS, "§b§lИнформация", infoLore));
        
        org.bukkit.Chunk chunk = p.getLocation().getChunk();
        String chunkKey = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
        boolean isClaimed = n.getClaimedChunkKeys().contains(chunkKey);
        boolean isWarzone = ServiceLocator.get(DiplomacySystem.class).isWarzone(chunk.getWorld(), chunk.getX(), chunk.getZ());
        
        List<String> currentLore = new ArrayList<>();
        currentLore.add("§3§l─── ТЕКУЩИЙ ЧАНК ───");
        currentLore.add(" ");
        currentLore.add("§fКоординаты:");
        currentLore.add("§b" + chunkKey);
        currentLore.add(" ");
        if (isClaimed) {
            currentLore.add("§a✓ Заклеймнут");
        } else {
            currentLore.add("§7○ Не заклеймнут");
        }
        if (isWarzone) {
            currentLore.add("§c⚔ Warzone активна!");
        }
        inv.setItem(22, GuiUtils.button(isClaimed ? Material.MAP : Material.PAPER, 
            "§b§lТекущий чанк", currentLore));
        
        inv.setItem(31, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Территория")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        
        if (slot == 31) {
            p.closeInventory();
            return;
        }
        
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) return;
        Nation n = opt.get();
        Nation.Role role = n.getRole(p.getUniqueId());
        boolean canClaim = (role == Nation.Role.LEADER || role == Nation.Role.GOVERNOR || role == Nation.Role.MINISTER);
        
        if (!canClaim) {
            p.sendMessage("§cНедостаточно прав.");
            return;
        }
        
        DoubleClickService doubleClickService = ServiceLocator.get(DoubleClickService.class);
        VisualEffectsService visualEffectsService = ServiceLocator.get(VisualEffectsService.class);

        if (slot == 11) {
            if (!doubleClickService.shouldProceed(p.getUniqueId(), "gui_claim")) {
                org.bukkit.inventory.ItemStack item = e.getCurrentItem();
                if (item != null) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                        item.setItemMeta(meta);
                        e.getInventory().setItem(slot, item);
                    }
                }
                p.sendMessage("§eНажмите ещё раз в течение 5 секунд для подтверждения клейма.");
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                return;
            }
            
            try {
                String result = nationManager.claimChunk(p);
                p.sendMessage(result);
                if (result.contains("успешно") || result.contains("заклеймлен")) {
                    visualEffectsService.sendActionBar(p, "§a✓ Территория заклеймлена!");
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                }
                open(p);
            } catch (Exception ex) {
                p.sendMessage("§cОшибка: " + ex.getMessage());
            }
        }
        else if (slot == 13) {
            if (!doubleClickService.shouldProceed(p.getUniqueId(), "gui_unclaim")) {
                org.bukkit.inventory.ItemStack item = e.getCurrentItem();
                if (item != null) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                        item.setItemMeta(meta);
                        e.getInventory().setItem(slot, item);
                    }
                }
                p.sendMessage("§eНажмите ещё раз в течение 5 секунд для подтверждения анклейма.");
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                return;
            }
            
            try {
                String result = nationManager.unclaimChunk(p);
                p.sendMessage(result);
                if (result.contains("успешно") || result.contains("анклеймлен")) {
                    visualEffectsService.sendActionBar(p, "§7Территория анклеймлена");
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);
                }
                open(p);
            } catch (Exception ex) {
                p.sendMessage("§cОшибка: " + ex.getMessage());
            }
        }
    }
}

