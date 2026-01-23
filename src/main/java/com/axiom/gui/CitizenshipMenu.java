package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
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
import java.util.UUID;

public class CitizenshipMenu implements Listener {

    public CitizenshipMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage("§cВы не в нации."); return; }
        Nation n = opt.get();
        
        String title = GuiUtils.formatHeader("Гражданство: " + n.getName());
        Inventory inv = GuiUtils.createMenu(title, 5);
        
        int slot = 10;
        List<UUID> citizens = new ArrayList<>(n.getCitizens());
        Nation.Role viewerRole = n.getRole(p.getUniqueId());
        boolean canManage = (viewerRole == Nation.Role.LEADER || viewerRole == Nation.Role.MINISTER || viewerRole == Nation.Role.GOVERNOR);
        
        for (UUID uid : citizens) {
            if (slot > 43) break;
            String name = Bukkit.getOfflinePlayer(uid).getName();
            if (name == null) name = uid.toString().substring(0, 8);
            Nation.Role r = n.getRole(uid);
            List<String> lore = new ArrayList<>();
            
            String roleDisplay = getRoleDisplay(r);
            lore.add("§fРоль: " + roleDisplay);
            lore.add(" ");
            
            Player onlinePlayer = Bukkit.getPlayer(uid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                lore.add("§a✓ В сети");
            } else {
                lore.add("§7Оффлайн");
            }
            
            if (canManage && !uid.equals(p.getUniqueId())) {
                lore.add(" ");
                lore.add("§7ЛКМ: Изменить роль");
                lore.add("§7ПКМ: Исключить");
            }
            
            Material icon = onlinePlayer != null && onlinePlayer.isOnline() ? Material.PLAYER_HEAD : Material.SKELETON_SKULL;
            inv.setItem(slot, GuiUtils.button(icon, "§b" + name, lore));
            slot++;
            if ((slot - 10) % 7 == 0) slot += 2;
        }
        
        if (canManage) {
            List<String> inviteLore = new ArrayList<>();
            inviteLore.add("§aПригласить нового игрока");
            inviteLore.add(" ");
            inviteLore.add("§7Используйте команду:");
            inviteLore.add("§b/axiom citizenship invite <player>");
            inv.setItem(40, GuiUtils.button(Material.EMERALD, "§a§lПригласить игрока", inviteLore));
        }
        
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§3§l─── СТАТИСТИКА ───");
        statsLore.add(" ");
        statsLore.add("§fВсего граждан: §b" + citizens.size());
        statsLore.add("§fОнлайн: §b" + citizens.stream().filter(u -> {
            Player pl = Bukkit.getPlayer(u);
            return pl != null && pl.isOnline();
        }).count());
        statsLore.add(" ");
        statsLore.add("§fЛидер: §6" + (n.getLeader() != null ? Bukkit.getOfflinePlayer(n.getLeader()).getName() : "Неизвестно"));
        inv.setItem(49, GuiUtils.button(Material.BOOK, "§b§lСтатистика", statsLore));
        
        inv.setItem(53, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }
    
    private String getRoleDisplay(Nation.Role role) {
        if (role == null) return "§7Нет";
        switch (role) {
            case LEADER: return "§6§lКОРОЛЬ";
            case MINISTER: return "§dМИНИСТР";
            case GENERAL: return "§cГЕНЕРАЛ";
            case GOVERNOR: return "§bГУБЕРНАТОР";
            case CITIZEN: return "§aГРАЖДАНИН";
            default: return "§7Нет";
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Гражданство")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
        int slot = e.getRawSlot();
        
        if (slot == 53) {
            p.closeInventory();
            return;
        }
        
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) return;
        Nation n = opt.get();
        Nation.Role viewerRole = n.getRole(p.getUniqueId());
        boolean canManage = (viewerRole == Nation.Role.LEADER || viewerRole == Nation.Role.MINISTER || viewerRole == Nation.Role.GOVERNOR);
        
        if (slot >= 10 && slot <= 43) {
            org.bukkit.inventory.ItemStack item = e.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;
            String clickedName = item.getItemMeta().getDisplayName();
            if (clickedName != null && clickedName.startsWith("§b")) {
                String playerName = clickedName.replace("§b", "").trim();
                UUID targetUuid = null;
                for (UUID uid : n.getCitizens()) {
                    String name = Bukkit.getOfflinePlayer(uid).getName();
                    if (name != null && name.equals(playerName)) {
                        targetUuid = uid;
                        break;
                    }
                }
                
                if (targetUuid == null || !canManage || targetUuid.equals(p.getUniqueId())) {
                    if (!canManage) p.sendMessage("§cНедостаточно прав.");
                    return;
                }
                
                DoubleClickService doubleClickService = ServiceLocator.get(DoubleClickService.class);
                VisualEffectsService visualEffectsService = ServiceLocator.get(VisualEffectsService.class);
                AXIOM plugin = ServiceLocator.get(AXIOM.class);

                if (e.isRightClick()) {
                    if (!doubleClickService.shouldProceed(p.getUniqueId(), "gui_exclude_" + targetUuid)) {
                        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                            item.setItemMeta(meta);
                            e.getInventory().setItem(slot, item);
                        }
                        p.sendMessage("§eНажмите ПКМ ещё раз в течение 5 секунд для исключения игрока.");
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                        return;
                    }
                    
                    n.getCitizens().remove(targetUuid);
                    try {
                        nationManager.save(n);
                        p.sendMessage("§aИгрок " + playerName + " исключён из нации.");
                        visualEffectsService.sendActionBar(p, "§c" + playerName + " исключён");
                        
                        org.bukkit.entity.Player excludedPlayer = Bukkit.getPlayer(targetUuid);
                        if (excludedPlayer != null && excludedPlayer.isOnline()) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                excludedPlayer.sendTitle("§c§l[ИСКЛЮЧЕНИЕ]", "§fВы исключены из нации '" + n.getName() + "'", 10, 80, 20);
                                visualEffectsService.sendActionBar(excludedPlayer, "§c⚠ Вы исключены из нации '" + n.getName() + "'");
                                org.bukkit.Location loc = excludedPlayer.getLocation();
                                loc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, loc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 2.0f));
                                excludedPlayer.playSound(loc, org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.8f, 0.7f);
                            });
                        }
                        open(p);
                    } catch (Exception ex) {
                        p.sendMessage("§cОшибка: " + ex.getMessage());
                    }
                }
                else if (e.isLeftClick()) {
                    p.closeInventory();
                    p.sendMessage("§7Изменить роль игрока:");
                    p.sendMessage("§b/axiom citizenship set-role <player> <role>");
                    p.sendMessage("§7Роли: LEADER, MINISTER, GENERAL, GOVERNOR, CITIZEN");
                }
            }
        }
    }
}

