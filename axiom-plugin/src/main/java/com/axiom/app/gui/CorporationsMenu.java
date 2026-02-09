package com.axiom.app.gui;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.industry.StockMarketService;
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

/**
 * GUI menu for managing corporations and stock market.
 */
public class CorporationsMenu implements Listener {
    private final AXIOM plugin;
    private final StockMarketService stockMarketService;
    
    private static class MenuHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return null;
        }
    }
    
    private static final MenuHolder HOLDER = new MenuHolder();

    public CorporationsMenu(AXIOM plugin, StockMarketService stockMarketService) {
        this.plugin = plugin;
        this.stockMarketService = stockMarketService;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        try {
            var opt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (opt.isEmpty()) {
                player.sendMessage("§cВы не в нации.");
                return;
            }
            Nation nation = opt.get();
            
            String title = GuiUtils.formatHeader("Корпорации: " + nation.getName());
            Inventory inv = Bukkit.createInventory(HOLDER, 54, GuiUtils.colorize(title));
            
            // Background
            ItemStack bg = GuiUtils.createGlassPane(GuiUtils.BACKGROUND, " ", null);
            for (int i = 0; i < 54; i++) {
                inv.setItem(i, bg);
            }
            
            // Borders
            ItemStack border = GuiUtils.createGlassPane(GuiUtils.BORDER, " ", null);
            for (int i = 0; i < 9; i++) inv.setItem(i, border);
            for (int i = 45; i < 54; i++) inv.setItem(i, border);
            for (int i = 0; i < 6; i++) {
                inv.setItem(i * 9, border);
                inv.setItem(i * 9 + 8, border);
            }
            
            // Header
            List<String> nationInfo = new ArrayList<>();
            nationInfo.add("§3§l─── КОРПОРАЦИИ НАЦИИ ───");
            nationInfo.add(" ");
            nationInfo.add("§fНация: §b" + nation.getName());
            
            List<StockMarketService.Corporation> corps = stockMarketService.getCorporationsOf(nation.getId());
            nationInfo.add("§fВсего корпораций: §b" + corps.size());
            
            double totalValue = corps.stream().mapToDouble(c -> c.value).sum();
            nationInfo.add("§fОбщая стоимость: §b" + String.format("%.2f", totalValue));
            nationInfo.add(" ");
            nationInfo.add("§7Используйте GUI для управления");
            
            inv.setItem(4, GuiUtils.button(Material.DIAMOND, "§b§lВаши Корпорации", nationInfo));
            
            // Corporations list (slots 19-43)
            int slot = 19;
            for (int i = 0; i < Math.min(corps.size(), 25); i++) {
                StockMarketService.Corporation corp = corps.get(i);
                
                Material icon = getCorpIcon(corp.type);
                List<String> corpLore = new ArrayList<>();
                corpLore.add("§3§l───────");
                corpLore.add("§fТип: §e" + corp.type);
                corpLore.add("§fСтоимость: §b" + String.format("%.2f", corp.value));
                corpLore.add("§fАкций доступно: §b" + corp.shares + "/" + corp.totalShares);
                corpLore.add(" ");
                
                // Show shareholders if public
                if (corp.isPublic && !corp.shareholders.isEmpty()) {
                    corpLore.add("§7Акционеры:");
                    int count = 0;
                    for (String shareholderId : corp.shareholders.keySet()) {
                        if (count >= 3) {
                            corpLore.add("§7  ... и ещё");
                            break;
                        }
                        Nation shareholder = plugin.getNationManager().getNationById(shareholderId);
                        String name = shareholder != null ? shareholder.getName() : shareholderId;
                        corpLore.add("§7  - §f" + name + ": §b" + corp.shareholders.get(shareholderId) + " акций");
                        count++;
                    }
                }
                
                corpLore.add(" ");
                String status = corp.isPublic ? "§a[ПУБЛИЧНАЯ]" : "§7[ПРИВАТНАЯ]";
                corpLore.add(status);
                
                if (!corp.isPublic) {
                    corpLore.add("§7Нажмите для проведения IPO");
                }
                
                inv.setItem(slot, GuiUtils.button(icon, "§f§l" + corp.name, corpLore));
                slot++;
                
                if (slot == 26) slot = 28;
                if (slot == 35) slot = 37;
            }
            
            // Global market button
            List<String> globalLore = new ArrayList<>();
            globalLore.add("§7Открыть глобальный рынок");
            globalLore.add("§7Все публичные корпорации");
            globalLore.add(" ");
            globalLore.add("§fВсего корпораций: §b" + stockMarketService.getAllCorporations().size());
            globalLore.add("§fИндекс рынка: §b" + String.format("%.2f", stockMarketService.calculateMarketIndex()));
            
            inv.setItem(49, GuiUtils.button(Material.EMERALD, "§a§lГлобальный Рынок", globalLore));
            
            // Close button
            inv.setItem(53, GuiUtils.button(Material.BARRIER, "§c§lЗакрыть", Arrays.asList("§7Нажмите для закрытия")));
            
            player.openInventory(inv);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to open corporations menu: " + e.getMessage());
            player.sendMessage("§cОшибка открытия меню корпораций.");
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        
        String title = e.getView().getTitle();
        if (!title.contains("Корпорации:")) return;
        
        e.setCancelled(true);
        
        int slot = e.getSlot();
        if (slot < 0 || slot >= 54) return;
        
        // Close button
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String displayName = clicked.getItemMeta().getDisplayName();
        if (displayName == null) return;
        
        // Handle clicks on corporations
        if (slot >= 19 && slot < 43) {
            String corpName = displayName.replace("§f§l", "").replace("§l", "");
            
            var opt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
            if (opt.isEmpty()) {
                player.sendMessage("§cВы не в нации.");
                return;
            }
            
            StockMarketService.Corporation corp = stockMarketService.getAllCorporations().stream()
                .filter(c -> c.name.equals(corpName))
                .findFirst()
                .orElse(null);
            
            if (corp != null && !corp.isPublic) {
                player.sendMessage("§6Для проведения IPO используйте команду:");
                player.sendMessage("§e/axiom stock ipo " + corp.id + " <shares> <pricePerShare>");
            }
        }
        
        // Global market button
        if (slot == 49) {
            player.closeInventory();
            player.sendMessage("§bОткройте глобальный рынок командой: /axiom stock global");
        }
    }
    
    private Material getCorpIcon(String type) {
        switch (type.toLowerCase()) {
            case "mine": return Material.IRON_PICKAXE;
            case "farm": return Material.GOLDEN_HOE;
            case "factory": return Material.FURNACE;
            case "tech": return Material.REDSTONE;
            case "bank": return Material.GOLD_BLOCK;
            case "trading": return Material.EMERALD;
            default: return Material.DIAMOND;
        }
    }
}

