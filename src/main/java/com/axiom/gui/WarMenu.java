package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.AdvancedWarSystem;
import com.axiom.service.NationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * War Menu - comprehensive war management interface.
 */
public class WarMenu implements Listener {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final AdvancedWarSystem warSystem;
    
    private static class MenuHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return null;
        }
    }
    
    private static final MenuHolder HOLDER = new MenuHolder();

    public WarMenu(AXIOM plugin, NationManager nationManager, AdvancedWarSystem warSystem) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.warSystem = warSystem;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        try {
            var opt = nationManager.getNationOfPlayer(player.getUniqueId());
            if (opt.isEmpty()) {
                player.sendMessage("§cВы не в нации.");
                return;
            }
            Nation n = opt.get();
            
            String title = GuiUtils.formatHeader("Войны: " + n.getName());
            Inventory inv = Bukkit.createInventory(HOLDER, 54, GuiUtils.colorize(title));
            
            // Fill background
            ItemStack bg = GuiUtils.createGlassPane(GuiUtils.BACKGROUND, " ", null);
            for (int i = 0; i < 54; i++) {
                inv.setItem(i, bg);
            }
            
            // Borders
            ItemStack border = GuiUtils.createGlassPane(GuiUtils.BORDER, " ", null);
            inv.setItem(0, border);
            inv.setItem(8, border);
            for (int i = 45; i < 54; i++) {
                inv.setItem(i, border);
            }
            for (int i = 1; i < 5; i++) {
                inv.setItem(i * 9, border);
                inv.setItem(i * 9 + 8, border);
            }
            
            // Active Wars
            List<AdvancedWarSystem.War> wars = warSystem.getNationWars(n.getId());
            List<String> warsLore = new ArrayList<>();
            warsLore.add("§c§lАКТИВНЫЕ ВОЙНЫ: " + wars.size());
            warsLore.add(" ");
            if (wars.isEmpty()) {
                warsLore.add("§aНет активных войн");
                warsLore.add("§7Мирное время");
            } else {
                for (AdvancedWarSystem.War war : wars) {
                    boolean isAttacker = war.attackerId.equals(n.getId());
                    String enemyId = isAttacker ? war.defenderId : war.attackerId;
                    Nation enemy = nationManager.getNationById(enemyId);
                    String enemyName = enemy != null ? enemy.getName() : enemyId;
                    warsLore.add("§c⚔ Война с: §f" + enemyName);
                    warsLore.add("§7  Тип: §f" + war.type.name());
                    warsLore.add("§7  Статус: §f" + war.status.name());
                }
            }
            inv.setItem(10, GuiUtils.button(Material.CROSSBOW, "§c§lАктивные войны", warsLore));
            
            // War Statistics
            Map<String, Object> stats = warSystem.getWarStatistics(n.getId());
            List<String> statsLore = new ArrayList<>();
            statsLore.add("§3§l─── ВОЕННАЯ СТАТИСТИКА ───");
            if (stats != null) {
                statsLore.add(" ");
                statsLore.add("§fВсего войн: §b" + stats.get("totalWars"));
                statsLore.add("§fПобед: §a" + stats.get("totalWins"));
                statsLore.add("§fПоражений: §c" + stats.get("totalLosses"));
                statsLore.add("§fБитв: §b" + stats.get("totalBattles"));
                if (stats.containsKey("warScore")) {
                    statsLore.add(" ");
                    statsLore.add("§fВоенный счёт: §e" + String.format("%.1f", ((Number) stats.get("warScore")).doubleValue()));
                }
            }
            inv.setItem(11, GuiUtils.button(Material.IRON_SWORD, "§b§lСтатистика", statsLore));
            
            // Declare War
            Nation.Role role = n.getRole(player.getUniqueId());
            boolean canDeclare = (role == Nation.Role.LEADER || role == Nation.Role.GENERAL);
            List<String> declareLore = new ArrayList<>();
            declareLore.add("§7Объявление войны");
            declareLore.add(" ");
            if (!canDeclare) {
                declareLore.add("§cТребуется: LEADER или GENERAL");
            } else {
                declareLore.add("§7Используйте команду:");
                declareLore.add("§b/axiom war declare <nationId>");
                declareLore.add(" ");
                declareLore.add("§c⚠ Война стоит 5000 " + n.getCurrencyCode());
                declareLore.add("§c⚠ Продлится 24 часа");
            }
            Material declareMat = canDeclare ? Material.TNT : Material.BARRIER;
            inv.setItem(12, GuiUtils.button(declareMat, canDeclare ? "§c§lОбъявить войну" : "§c§lОбъявить войну", declareLore));
            
            // War Status
            List<String> statusLore = new ArrayList<>();
            statusLore.add("§7Текущий статус войн");
            statusLore.add(" ");
            statusLore.add("§fАктивных: §c" + wars.size());
            if (!wars.isEmpty()) {
                statusLore.add(" ");
                statusLore.add("§7ЛКМ: Детальная информация");
            }
            inv.setItem(13, GuiUtils.button(Material.PAPER, "§b§lСтатус войн", statusLore));
            
            // Mobilization
            List<String> mobLore = new ArrayList<>();
            mobLore.add("§7Мобилизация граждан");
            mobLore.add(" ");
            mobLore.add("§7Мобилизация даёт бонусы");
            mobLore.add("§7в военное время");
            inv.setItem(14, GuiUtils.button(Material.BELL, "§e§lМобилизация", mobLore));
            
            // Raids
            List<String> raidLore = new ArrayList<>();
            raidLore.add("§7Рейды на врагов");
            raidLore.add("§7Грабеж ресурсов");
            if (wars.isEmpty()) {
                raidLore.add(" ");
                raidLore.add("§7Рейды доступны только");
                raidLore.add("§7во время войны");
            }
            inv.setItem(19, GuiUtils.button(Material.GOLDEN_AXE, "§eРейды", raidLore));
            
            // Sieges
            List<String> siegeLore = new ArrayList<>();
            siegeLore.add("§7Осады городов");
            siegeLore.add("§7Штурм укреплений");
            if (wars.isEmpty()) {
                siegeLore.add(" ");
                siegeLore.add("§7Осады доступны только");
                siegeLore.add("§7во время войны");
            }
            inv.setItem(20, GuiUtils.button(Material.REDSTONE_BLOCK, "§cОсады", siegeLore));
            
            // Conquest
            List<String> conquestLore = new ArrayList<>();
            conquestLore.add("§7Захват территорий");
            conquestLore.add("§7Завоевание");
            conquestLore.add(" ");
            conquestLore.add("§7Завоевания отслеживаются");
            conquestLore.add("§7автоматически во время войн");
            inv.setItem(21, GuiUtils.button(Material.WHITE_BANNER, "§cЗавоевания", conquestLore));
            
            // Back button
            inv.setItem(45, GuiUtils.button(Material.ARROW, "§e◀ Назад", Arrays.asList("§7Вернуться в главное меню")));
            
            // Close button
            inv.setItem(49, GuiUtils.createCloseButton());
            
            player.openInventory(inv);
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening war menu: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cОшибка: " + e.getMessage());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Войны")) return;
        
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        if (slot >= e.getInventory().getSize()) return;
        
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        
        // Close button
        if (current.getType() == Material.BARRIER) {
            p.closeInventory();
            return;
        }
        
        // Back button
        if (slot == 45 || (current.getType() == Material.ARROW && current.getItemMeta() != null && current.getItemMeta().getDisplayName().contains("Назад"))) {
            plugin.getNationMainMenu().open(p);
            return;
        }
        
        String displayName = current.getItemMeta() != null ? current.getItemMeta().getDisplayName() : "";
        
        if (displayName.contains("Объявить войну")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте: §b/axiom war declare <nationId>");
        } else if (displayName.contains("Активные войны") || displayName.contains("Статус")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте: §b/axiom war status §7для детальной информации");
        } else if (displayName.contains("Рейды") || displayName.contains("Осады") || displayName.contains("Завоевания")) {
            p.sendMessage("§eЭти функции доступны во время войны");
        }
    }
}

