package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.*;
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
 * Main nation menu with beautiful AXIOM design.
 * Implements the full UI specification with proper colors, icons, and layout.
 */
public class NationMainMenu implements Listener {
    
    private static class MenuHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return null;
        }
    }
    
    private static final MenuHolder HOLDER = new MenuHolder();

    public NationMainMenu() {
        // Constructor is now empty. Listener registration is handled in the main plugin class.
    }

    public void open(Player player) {
        try {
            NationManager nationManager = ServiceLocator.get(NationManager.class);
            var opt = nationManager.getNationOfPlayer(player.getUniqueId());
            if (opt.isEmpty()) {
                openCreateNationMenu(player);
                return;
            }
            Nation n = opt.get();
            
            String title = GuiUtils.formatHeader("Нация: " + n.getName());
            Inventory inv = Bukkit.createInventory(HOLDER, 54, GuiUtils.colorize(title));
            
            ItemStack bg = GuiUtils.createGlassPane(GuiUtils.BACKGROUND, " ", null);
            for (int i = 0; i < 54; i++) inv.setItem(i, bg);
            
            ItemStack border = GuiUtils.createGlassPane(GuiUtils.BORDER, " ", null);
            inv.setItem(0, border);
            inv.setItem(8, border);
            for (int i = 45; i < 54; i++) inv.setItem(i, border);
            for (int i = 1; i < 5; i++) {
                inv.setItem(i * 9, border);
                inv.setItem(i * 9 + 8, border);
            }
            
            Map<String, String> tabIcons = n.getTabIcons();
            
            inv.setItem(1, createTabButton("Профиль", getMaterialFromString(tabIcons.getOrDefault("profile", "BLUE_BANNER")), Arrays.asList("§7Флаг, девиз, валюта", "§fПравительство: §b" + n.getGovernmentType())));
            inv.setItem(2, createTabButton("Территория", getMaterialFromString(tabIcons.getOrDefault("territory", "MAP")), Arrays.asList("§7Клейм/Анклейм", "§fУчастков: §b" + n.getClaimedChunkKeys().size())));
            inv.setItem(3, createTabButton("Гражданство", getMaterialFromString(tabIcons.getOrDefault("citizenship", "PLAYER_HEAD")), Arrays.asList("§7Вступление/выход/приглашение")));
            inv.setItem(4, createTabButton("Экономика", getMaterialFromString(tabIcons.getOrDefault("economy", "EMERALD")), Arrays.asList("§7Казна, налоги, печать", "§fКазна: §a" + String.format("%.2f", n.getTreasury()) + " " + n.getCurrencyCode())));
            inv.setItem(5, createTabButton("Дипломатия", getMaterialFromString(tabIcons.getOrDefault("diplomacy", "WRITTEN_BOOK")), Arrays.asList("§7Альянсы, репутация, война")));
            inv.setItem(6, createTabButton("Религия", getMaterialFromString(tabIcons.getOrDefault("religion", "ENCHANTING_TABLE")), Arrays.asList("§7Религии, десятина, святыни")));
            inv.setItem(7, createTabButton("Города", getMaterialFromString(tabIcons.getOrDefault("cities", "BRICKS")), Arrays.asList("§7Уровни, население, инфраструктура")));
            
            List<String> nationInfo = new ArrayList<>();
            nationInfo.add("§3§l─── ИНФОРМАЦИЯ О НАЦИИ ───");
            nationInfo.add(" ");
            nationInfo.add("§fЛидер: §b" + (n.getLeader() != null ? Bukkit.getOfflinePlayer(n.getLeader()).getName() : "Неизвестно"));
            nationInfo.add("§fГраждан: §b" + n.getCitizens().size());
            nationInfo.add("§fТерриторий: §b" + n.getClaimedChunkKeys().size());
            nationInfo.add("§fКазна: §a" + String.format("%.2f", n.getTreasury()) + " §f" + n.getCurrencyCode());
            
            double power = nationManager.calculateNationPower(n.getId());
            nationInfo.add(" ");
            nationInfo.add("§6§lМОЩЬ НАЦИИ: §e" + String.format("%.1f", power) + "/100");
            
            if (n.getMotto() != null && !n.getMotto().isEmpty()) {
                nationInfo.add(" ");
                nationInfo.add(GuiUtils.formatMotto(n.getMotto()));
            }
        
            Material flagMaterial = getMaterialFromString(n.getFlagIconMaterial());
            inv.setItem(22, GuiUtils.button(flagMaterial, "§b§l" + n.getName(), nationInfo));
            
            EducationService educationService = ServiceLocator.get(EducationService.class);
            TechnologyTreeService techTreeService = ServiceLocator.get(TechnologyTreeService.class);
            double eduLevel = educationService.getEducationLevel(n.getId());
            int techCount = techTreeService.getResearchableTechs(n.getId()).size();
            int unlockedCount = techTreeService.getUnlockedTechs(n.getId()).size();
            List<String> techLore = new ArrayList<>();
            techLore.add("§7Дерево исследований");
            techLore.add("§fОбразование: §b" + String.format("%.1f", eduLevel));
            techLore.add("§fИзучено: §a" + unlockedCount);
            techLore.add("§fДоступно: §b" + techCount);
            double techPower = techTreeService.calculateTechnologyPower(n.getId());
            techLore.add("§fСила технологий: §e" + String.format("%.1f", techPower));
            inv.setItem(30, GuiUtils.button(Material.ENCHANTED_BOOK, "§b§lТехнологии", techLore));
            
            int elections = 0; // TODO
            inv.setItem(31, GuiUtils.button(Material.PAPER, "§b§lВыборы", Arrays.asList("§7Президент, парламент, законы", "§fАктивных выборов: §b" + elections)));
            
            List<String> historyLore = new ArrayList<>();
            historyLore.add("§7События: войны, выборы, кризисы");
            int historySize = n.getHistory().size();
            historyLore.add("§fЗаписей: §b" + historySize);
            if (!n.getHistory().isEmpty()) {
                String lastEvent = n.getHistory().get(n.getHistory().size() - 1);
                if (lastEvent.length() > 30) lastEvent = lastEvent.substring(0, 27) + "...";
                historyLore.add(" ");
                historyLore.add("§7Последнее: §f" + lastEvent);
            }
            inv.setItem(40, GuiUtils.button(Material.CLOCK, "§b§lИстория", historyLore));
            
            AdvancedWarSystem advancedWarSystem = ServiceLocator.get(AdvancedWarSystem.class);
            List<AdvancedWarSystem.War> wars = advancedWarSystem.getNationWars(n.getId());
            if (!wars.isEmpty()) {
                List<String> warLore = new ArrayList<>();
                warLore.add("§c§lАКТИВНЫЕ ВОЙНЫ: " + wars.size());
                for (AdvancedWarSystem.War war : wars) {
                    boolean isAttacker = war.attackerId.equals(n.getId());
                    String enemyId = isAttacker ? war.defenderId : war.attackerId;
                    Nation enemy = nationManager.getNationById(enemyId);
                    String enemyName = enemy != null ? enemy.getName() : enemyId;
                    warLore.add("§c⚔ " + enemyName);
                    warLore.add("§7  Тип: §f" + war.type.name());
                    warLore.add("§7  Статус: §f" + war.status.name());
                    warLore.add("§7  Битв: §f" + war.battlesFought);
                    warLore.add("§7  Побед: §a" + (isAttacker ? war.attackerWins : war.defenderWins));
                }
                inv.setItem(32, GuiUtils.button(Material.CROSSBOW, "§c§lВОЙНА", warLore));
            }
            
            List<String> advancedLore = new ArrayList<>();
            advancedLore.add("§7Расширенные функции AXIOM");
            advancedLore.add("§fВойны, банкинг, культура,");
            advancedLore.add("§fшпионаж, торговля и многое другое");
            inv.setItem(41, GuiUtils.button(Material.BEACON, "§b§lРасширенные функции", advancedLore));
            
            inv.setItem(49, GuiUtils.createCloseButton());
            
            player.openInventory(inv);
        } catch (Exception e) {
            ServiceLocator.get(AXIOM.class).getLogger().severe("Error opening nation menu for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cОшибка при открытии меню: " + e.getMessage());
        }
    }
    
    private void openCreateNationMenu(Player player) {
        try {
            String title = GuiUtils.formatHeader("Создание нации");
            Inventory inv = GuiUtils.createMenu(title, 3);
            
            List<String> createLore = new ArrayList<>();
            createLore.add("§7Создайте свою нацию и");
            createLore.add("§7станьте её лидером!");
            createLore.add(" ");
            createLore.add("§eИспользуйте команду:");
            createLore.add("§b/axiom nation create <название>");
            
            inv.setItem(13, GuiUtils.button(Material.GOLD_BLOCK, "§6§lСоздать нацию", createLore));
            inv.setItem(26, GuiUtils.createCloseButton());
            
            player.openInventory(inv);
        } catch (Exception e) {
            ServiceLocator.get(AXIOM.class).getLogger().severe("Error opening create nation menu for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cОшибка при открытии меню: " + e.getMessage());
        }
    }
    
    private ItemStack createTabButton(String name, Material icon, List<String> lore) {
        return GuiUtils.button(icon, "§b§l" + name, lore);
    }
    
    private Material getMaterialFromString(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return Material.BLUE_BANNER;
        }
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            ServiceLocator.get(AXIOM.class).getLogger().warning("Invalid material name: " + materialName);
            return Material.BLUE_BANNER;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || (!title.contains("Нация:") && !title.contains("Создание нации"))) return;
        
        e.setCancelled(true);
        
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        
        if (slot >= e.getInventory().getSize()) return;
        
        org.bukkit.inventory.meta.ItemMeta meta = current.getItemMeta();
        if (meta == null) return;
        String name = meta.getDisplayName();
        if (name == null) return;
        
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        
        if (current.getType() == Material.BARRIER) {
            p.closeInventory();
            return;
        }
        
        NationManager nationManager = ServiceLocator.get(NationManager.class);

        if (slot == 1 || name.contains("Профиль")) {
            new ProfileMenu().open(p);
            return;
        }
        if (slot == 2 || name.contains("Территория")) {
            new TerritoryMenu().open(p);
            return;
        }
        if (slot == 3 || name.contains("Гражданство")) {
            new CitizenshipMenu().open(p);
            return;
        }
        if (slot == 4 || name.contains("Экономика")) {
            new EconomyMenu().open(p);
            return;
        }
        if (slot == 5 || name.contains("Дипломатия")) {
            new DiplomacyMenu().open(p);
            return;
        }
        if (slot == 6 || name.contains("Религия")) {
            ServiceLocator.get(ReligionMenu.class).open(p);
            return;
        }
        if (slot == 7 || name.contains("Города")) {
            new CitiesMenu().open(p);
            return;
        }
        if (slot == 30 || name.contains("Технологии")) {
            ServiceLocator.get(TechnologyMenu.class).openMainMenu(p);
            return;
        }
        if (slot == 31 || name.contains("Выборы")) {
            new com.axiom.gui.ElectionMenu().open(p);
            return;
        }
        if (slot == 40 || name.contains("История")) {
            new HistoryMenu().open(p);
            return;
        }
        if (slot == 32 || name.contains("ВОЙНА")) {
            p.sendMessage("§cСистема войн доступна через /axiom war");
            return;
        }
        if (slot == 41 || name.contains("Расширенные функции")) {
            new com.axiom.gui.AdvancedFeaturesMenu().open(p);
            return;
        }
        if (slot == 49 && current.getType() == Material.BARRIER) {
            p.closeInventory();
            return;
        }
        
        if (slot == 13 && current.getType() == Material.GOLD_BLOCK && name.contains("Создать")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте команду: §b/axiom nation create <название>");
            p.sendMessage("§7Или введите название в чат после закрытия этого сообщения.");
            return;
        }
    }
}
