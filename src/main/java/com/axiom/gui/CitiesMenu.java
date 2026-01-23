package com.axiom.gui;

import com.axiom.model.Nation;
import com.axiom.service.CityGrowthEngine;
import com.axiom.service.NationManager;
import com.axiom.service.ServiceLocator;
import com.axiom.service.TechnologyTreeService;
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

public class CitiesMenu implements Listener {

    public CitiesMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        CityGrowthEngine cityEngine = ServiceLocator.get(CityGrowthEngine.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage("§cВы не в нации."); return; }
        Nation n = opt.get();
        
        String title = GuiUtils.formatHeader("Города: " + n.getName());
        var list = cityEngine.getCitiesOf(n.getId());
        int rows = Math.max(4, (list.size() / 7) + 3);
        Inventory inv = GuiUtils.createMenu(title, rows);
        
        int slot = 10;
        for (var c : list) {
            if (slot > 53) break;
            
            List<String> lore = new ArrayList<>();
            lore.add("§3§l─── " + c.getName() + " ───");
            lore.add(" ");
            lore.add("§fУровень: §b" + c.getLevel());
            lore.add("§fНаселение: §b" + formatLargeNumber(c.getPopulation()));
            
            if (cityEngine != null) {
                Map<String, Object> cityStats = cityEngine.getCityStatistics(c.getId());
                if (cityStats.containsKey("happiness")) {
                    double happiness = (Double) cityStats.get("happiness");
                    String happinessColor = happiness >= 70 ? "§a" : happiness >= 40 ? "§e" : "§c";
                    lore.add("§fСчастье: " + happinessColor + String.format("%.1f", happiness) + "%");
                }
                if (cityStats.containsKey("productivity")) {
                    double productivity = (Double) cityStats.get("productivity");
                    lore.add("§fПроизводительность: §e" + String.format("%.0f", productivity * 100) + "%");
                }
                double cityIncome = cityEngine.calculateCityIncome(c.getId());
                if (cityIncome > 0) {
                    lore.add("§fДоход (в час): §a" + String.format("%.2f", cityIncome) + " " + n.getCurrencyCode());
                }
            }
            
            if (c.getLevel() >= 5) {
                lore.add(" ");
                lore.add("§e§lБОНУСЫ МЕТРОПОЛИИ:");
                lore.add("§a+20% производство");
                lore.add("§a+15% торговля");
                lore.add("§a+10% исследование");
            } else if (c.getLevel() >= 3) {
                lore.add(" ");
                lore.add("§e§lБОНУСЫ ГОРОДА:");
                lore.add("§a+10% производство");
                lore.add("§a+5% торговля");
            }
            
            TechnologyTreeService techTreeService = ServiceLocator.get(TechnologyTreeService.class);
            if (techTreeService != null) {
                double cityTechBonus = techTreeService.getBonus(n.getId(), "cityProduction");
                if (cityTechBonus > 1.0) {
                    lore.add(" ");
                    lore.add("§bТехнологии: §a+" + String.format("%.0f", (cityTechBonus - 1.0) * 100) + "%");
                }
            }
            
            lore.add(" ");
            lore.add("§6§lИНФРАСТРУКТУРА:");
            if (c.hasHospital()) lore.add("§a✓ Больница");
            if (c.hasSchool()) lore.add("§a✓ Школа");
            if (c.hasUniversity()) lore.add("§a✓ Университет");
            if (!c.hasHospital() && !c.hasSchool() && !c.hasUniversity()) {
                lore.add("§7Нет инфраструктуры");
            }
            
            lore.add(" ");
            lore.add("§fЦентр: §7" + c.getCenterChunk());
            lore.add(" ");
            lore.add("§7ЛКМ: Подробнее");
            
            Material cityIcon = Material.BRICKS;
            if (c.getLevel() >= 5) cityIcon = Material.EMERALD_BLOCK;
            else if (c.getLevel() >= 3) cityIcon = Material.GOLD_BLOCK;
            
            inv.setItem(slot++, GuiUtils.button(cityIcon, "§b§l" + c.getName(), lore));
            if ((slot - 10) % 7 == 0) slot += 2;
        }
        
        Nation.Role role = n.getRole(p.getUniqueId());
        boolean canFound = (role == Nation.Role.LEADER || role == Nation.Role.GOVERNOR || role == Nation.Role.MINISTER);
        
        if (canFound) {
            List<String> foundLore = new ArrayList<>();
            foundLore.add("§aОсновать новый город");
            foundLore.add(" ");
            foundLore.add("§7Город будет основан");
            foundLore.add("§7по центру текущего чанка");
            foundLore.add(" ");
            foundLore.add("§7Используйте команду:");
            foundLore.add("§b/axiom city found <name>");
            inv.setItem((rows - 2) * 9 + 4, GuiUtils.button(Material.GREEN_BED, "§a§lОсновать город", foundLore));
        }
        
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§3§l─── СТАТИСТИКА ГОРОДОВ ───");
        statsLore.add(" ");
        statsLore.add("§fВсего городов: §b" + list.size());
        int totalPopulation = list.stream().mapToInt(c -> c.getPopulation()).sum();
        statsLore.add("§fОбщее население: §b" + formatLargeNumber(totalPopulation));
        double avgLevel = list.isEmpty() ? 0 : list.stream().mapToInt(c -> c.getLevel()).average().orElse(0);
        statsLore.add("§fСредний уровень: §b" + String.format("%.1f", avgLevel));
        
        long metropolis = list.stream().filter(c -> c.getLevel() >= 5).count();
        long large = list.stream().filter(c -> c.getLevel() >= 3 && c.getLevel() < 5).count();
        long small = list.stream().filter(c -> c.getLevel() < 3).count();
        
        if (list.size() > 0) {
            statsLore.add(" ");
            statsLore.add("§6§lКАТЕГОРИИ:");
            if (metropolis > 0) statsLore.add("§fМетрополии (§e≥5§f): §b" + metropolis);
            if (large > 0) statsLore.add("§fКрупные (§e3-4§f): §b" + large);
            if (small > 0) statsLore.add("§fМалые (§e<3§f): §b" + small);
        }
        
        double totalProductionBonus = 0;
        double totalTradeBonus = 0;
        for (var city : list) {
            if (city.getLevel() >= 5) {
                totalProductionBonus += 0.20;
                totalTradeBonus += 0.15;
            } else if (city.getLevel() >= 3) {
                totalProductionBonus += 0.10;
                totalTradeBonus += 0.05;
            }
        }
        
        if (totalProductionBonus > 0 || totalTradeBonus > 0) {
            statsLore.add(" ");
            statsLore.add("§a§lОБЩИЕ БОНУСЫ:");
            if (totalProductionBonus > 0) statsLore.add("§fПроизводство: §a+" + String.format("%.0f", totalProductionBonus * 100) + "%");
            if (totalTradeBonus > 0) statsLore.add("§fТорговля: §a+" + String.format("%.0f", totalTradeBonus * 100) + "%");
        }
        
        inv.setItem((rows - 2) * 9 + 8, GuiUtils.button(Material.ENCHANTED_BOOK, "§b§lСтатистика", statsLore));
        
        inv.setItem((rows - 1) * 9 - 1, GuiUtils.createCloseButton());
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Города")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        
        if (slot == (e.getInventory().getSize() - 1)) {
            p.closeInventory();
            return;
        }
        
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) return;
        Nation n = opt.get();
        Nation.Role role = n.getRole(p.getUniqueId());
        boolean canFound = (role == Nation.Role.LEADER || role == Nation.Role.GOVERNOR || role == Nation.Role.MINISTER);
        
        int rows = e.getInventory().getSize() / 9;
        if (slot == (rows - 2) * 9 + 4 && canFound) {
            p.closeInventory();
            p.sendMessage("§7Основать город:");
            p.sendMessage("§b/axiom city found <name>");
            return;
        }
        
        if (slot >= 10 && slot < 53) {
            // City details would go here
        }
    }
    
    private String formatLargeNumber(int number) {
        if (number >= 1_000_000) return String.format("%.2fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.2fK", number / 1_000.0);
        return String.valueOf(number);
    }
}


