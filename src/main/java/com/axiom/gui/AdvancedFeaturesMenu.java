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

public class AdvancedFeaturesMenu implements Listener {
    
    private static class MenuHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return null;
        }
    }
    
    private static final MenuHolder HOLDER = new MenuHolder();

    public AdvancedFeaturesMenu() {
        // Empty constructor
    }

    public void open(Player player) {
        try {
            NationManager nationManager = ServiceLocator.get(NationManager.class);
            var opt = nationManager.getNationOfPlayer(player.getUniqueId());
            if (opt.isEmpty()) {
                player.sendMessage("§cВы не в нации.");
                return;
            }
            Nation n = opt.get();
            
            String title = GuiUtils.formatHeader("Расширенные функции");
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
            
            AdvancedWarSystem advancedWarSystem = ServiceLocator.get(AdvancedWarSystem.class);
            List<String> warLore = new ArrayList<>();
            if (advancedWarSystem != null) {
                List<com.axiom.service.AdvancedWarSystem.War> wars = advancedWarSystem.getNationWars(n.getId());
                warLore.add("§7Управление войнами");
                warLore.add("§fАктивных войн: §c" + wars.size());
            }
            inv.setItem(10, GuiUtils.button(Material.CROSSBOW, "§c§lВойны и боевые действия", warLore));
            
            List<String> militaryLore = Arrays.asList("§7Мобилизация", "§7Сила армии", "§7Военная служба");
            inv.setItem(11, GuiUtils.button(Material.IRON_SWORD, "§cВоенная служба", militaryLore));
            
            List<String> raidLore = Arrays.asList("§7Рейды на врагов", "§7Грабеж ресурсов");
            inv.setItem(12, GuiUtils.button(Material.GOLDEN_AXE, "§eРейды", raidLore));
            
            List<String> siegeLore = Arrays.asList("§7Осада городов", "§7Штурм укреплений");
            inv.setItem(13, GuiUtils.button(Material.TNT, "§cОсады", siegeLore));
            
            List<String> conquestLore = Arrays.asList("§7Захват территорий", "§7Завоевание");
            inv.setItem(14, GuiUtils.button(Material.WHITE_BANNER, "§cЗавоевания", conquestLore));
            
            List<String> allianceLore = Arrays.asList("§7Военные альянсы", "§7НАТО-подобные организации");
            inv.setItem(19, GuiUtils.button(Material.SHIELD, "§bАльянсы", allianceLore));
            
            TreatyService treatyService = ServiceLocator.get(TreatyService.class);
            List<String> treatyLore = Arrays.asList("§7Дипломатические договоры", "§7Соглашения");
            if (treatyService != null) {
                int treaties = treatyService.getNationTreaties(n.getId()).size();
                treatyLore = Arrays.asList("§7Дипломатические договоры", "§fАктивных: §b" + treaties);
            }
            inv.setItem(20, GuiUtils.button(Material.WRITABLE_BOOK, "§bДоговоры", treatyLore));
            
            List<String> sanctionLore = Arrays.asList("§7Экономические санкции", "§7Ограничения торговли");
            inv.setItem(21, GuiUtils.button(Material.BARRIER, "§cСанкции", sanctionLore));
            
            List<String> embargoLore = Arrays.asList("§7Торговые эмбарго", "§7Блокада торговли");
            inv.setItem(22, GuiUtils.button(Material.ANVIL, "§cЭмбарго", embargoLore));
            
            List<String> recognitionLore = Arrays.asList("§7Дипломатическое признание", "§7Легитимность");
            inv.setItem(23, GuiUtils.button(Material.EMERALD, "§aПризнание", recognitionLore));
            
            List<String> bankingLore = Arrays.asList("§7Банковская система", "§7Кредиты и депозиты");
            inv.setItem(28, GuiUtils.button(Material.GOLD_INGOT, "§6Банкинг", bankingLore));
            
            List<String> stockLore = Arrays.asList("§7Фондовый рынок", "§7Корпорации и акции");
            inv.setItem(29, GuiUtils.button(Material.PAPER, "§eФондовый рынок", stockLore));
            
            List<String> tradeLore = Arrays.asList("§7Торговые маршруты", "§7Международная торговля");
            inv.setItem(30, GuiUtils.button(Material.MINECART, "§bТорговля", tradeLore));
            
            ResourceService resourceService = ServiceLocator.get(ResourceService.class);
            List<String> resourcesLore = Arrays.asList("§7Ресурсы нации", "§7Сырьевые запасы");
            if (resourceService != null) {
                int resCount = resourceService.getNationResources(n.getId()).size();
                resourcesLore = Arrays.asList("§7Ресурсы нации", "§fТипов ресурсов: §b" + resCount);
            }
            inv.setItem(31, GuiUtils.button(Material.DIAMOND_ORE, "§bРесурсы", resourcesLore));
            
            List<String> currencyLore = Arrays.asList("§7Обмен валют", "§7Курсы валют");
            inv.setItem(32, GuiUtils.button(Material.EMERALD_BLOCK, "§aВалюта", currencyLore));
            
            List<String> cultureLore = Arrays.asList("§7Культура нации", "§7Культурное влияние");
            inv.setItem(37, GuiUtils.button(Material.PAINTING, "§dКультура", cultureLore));
            
            List<String> propagandaLore = Arrays.asList("§7Пропаганда", "§7Публичные кампании");
            inv.setItem(38, GuiUtils.button(Material.OAK_SIGN, "§cПропаганда", propagandaLore));
            
            List<String> espionageLore = Arrays.asList("§7Шпионаж", "§7Разведка");
            inv.setItem(39, GuiUtils.button(Material.SPYGLASS, "§5Шпионаж", espionageLore));
            
            EducationService educationService = ServiceLocator.get(EducationService.class);
            List<String> educationLore = Arrays.asList("§7Образование", "§7Научный прогресс");
            if (educationService != null) {
                double eduLevel = educationService.getEducationLevel(n.getId());
                educationLore = Arrays.asList("§7Образование", "§fУровень: §b" + String.format("%.1f", eduLevel));
            }
            inv.setItem(40, GuiUtils.button(Material.BOOKSHELF, "§bОбразование", educationLore));
            
            HappinessService happinessService = ServiceLocator.get(HappinessService.class);
            List<String> happinessLore = Arrays.asList("§7Счастье граждан", "§7Удовлетворенность");
            if (happinessService != null) {
                double happy = happinessService.getNationHappiness(n.getId());
                happinessLore = Arrays.asList("§7Счастье граждан", "§fУровень: §a" + String.format("%.1f", happy) + "%");
            }
            inv.setItem(41, GuiUtils.button(Material.CAKE, "§aСчастье", happinessLore));
            
            inv.setItem(45, GuiUtils.button(Material.ARROW, "§e◀ Назад", Arrays.asList("§7Вернуться в главное меню")));
            
            inv.setItem(49, GuiUtils.createCloseButton());
            
            player.openInventory(inv);
        } catch (Exception e) {
            ServiceLocator.get(AXIOM.class).getLogger().severe("Error opening advanced features menu: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cОшибка: " + e.getMessage());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Расширенные функции")) return;
        
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        if (slot >= e.getInventory().getSize()) return;
        
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        
        if (current.getType() == Material.BARRIER) {
            p.closeInventory();
            return;
        }
        
        if (slot == 45 || (current.getType() == Material.ARROW && current.getItemMeta() != null && current.getItemMeta().getDisplayName().contains("Назад"))) {
            new NationMainMenu().open(p);
            return;
        }
        
        String displayName = current.getItemMeta() != null ? current.getItemMeta().getDisplayName() : "";
        
        if (displayName.contains("Войны")) {
            p.closeInventory();
            AdvancedWarSystem advancedWarSystem = ServiceLocator.get(AdvancedWarSystem.class);
            if (advancedWarSystem != null) {
                new com.axiom.gui.WarMenu(ServiceLocator.get(AXIOM.class), ServiceLocator.get(NationManager.class), advancedWarSystem).open(p);
            } else {
                p.sendMessage("§eИспользуйте: §b/axiom war §7для управления войнами");
            }
        } else if (displayName.contains("Военная")) {
            p.sendMessage("§eВоенная служба: доступ через главное меню");
        } else if (displayName.contains("Рейды")) {
            p.sendMessage("§eИспользуйте: §b/axiom raid §7для рейдов");
        } else if (displayName.contains("Осады")) {
            p.sendMessage("§eИспользуйте: §b/axiom siege §7для осад");
        } else if (displayName.contains("Завоевания")) {
            p.sendMessage("§eЗавоевания отслеживаются автоматически");
        } else if (displayName.contains("Альянсы")) {
            p.sendMessage("§eИспользуйте: §b/axiom diplomacy ally §7для альянсов");
        } else if (displayName.contains("Договоры")) {
            p.sendMessage("§eИспользуйте: §b/axiom diplomacy treaty §7для договоров");
        } else if (displayName.contains("Санкции")) {
            p.sendMessage("§eСанкции применяются автоматически");
        } else if (displayName.contains("Эмбарго")) {
            p.sendMessage("§eТорговые эмбарго: доступ через дипломатию");
        } else if (displayName.contains("Признание")) {
            p.sendMessage("§eДипломатическое признание: автоматически");
        } else if (displayName.contains("Банкинг")) {
            p.closeInventory();
            BankingService bankingService = ServiceLocator.get(BankingService.class);
            if (bankingService != null) {
                new com.axiom.gui.BankingMenu(ServiceLocator.get(com.axiom.AXIOM.class), ServiceLocator.get(com.axiom.service.NationManager.class), bankingService).open(p);
            } else {
                p.sendMessage("§eИспользуйте: §b/axiom banking §7для банковских операций");
            }
        } else if (displayName.contains("Фондовый")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте: §b/axiom stock list §7для просмотра корпораций");
            p.sendMessage("§eИспользуйте: §b/axiom stock create <name> <type> §7для создания");
        } else if (displayName.contains("Торговля")) {
            p.sendMessage("§eТорговля: доступ через экономику");
        } else if (displayName.contains("Ресурсы")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте: §b/axiom resources list §7для просмотра ресурсов");
        } else if (displayName.contains("Валюта")) {
            p.sendMessage("§eОбмен валют: доступ через экономику");
        } else if (displayName.contains("Культура")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте: §b/axiom culture §7для просмотра культурного влияния");
        } else if (displayName.contains("Пропаганда")) {
            p.sendMessage("§eПропаганда: доступ через дипломатию");
        } else if (displayName.contains("Шпионаж")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте: §b/axiom espionage §7для шпионажа");
        } else if (displayName.contains("Образование")) {
            p.sendMessage("§eОбразование: доступ через технологии");
        } else if (displayName.contains("Счастье")) {
            p.sendMessage("§eСчастье: отслеживается автоматически");
        }
        
        if (!displayName.contains("Назад") && !displayName.contains("Банкинг") && !displayName.contains("Фондовый") 
            && !displayName.contains("Войны") && !displayName.contains("Ресурсы") && !displayName.contains("Культура") 
            && !displayName.contains("Шпионаж")) {
            p.sendMessage("§7Все функции доступны через команды или меню");
        }
    }
}

