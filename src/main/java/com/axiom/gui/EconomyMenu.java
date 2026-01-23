package com.axiom.gui;

import com.axiom.AXIOM;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

public class EconomyMenu implements Listener {

    public EconomyMenu() {
        // Empty constructor
    }

    public void open(Player p) {
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage("§cВы не в нации."); return; }
        Nation n = opt.get();
        
        String title = GuiUtils.formatHeader("Экономика: " + n.getName());
        Inventory inv = GuiUtils.createMenu(title, 4);
        
        List<String> treasuryLore = new ArrayList<>();
        treasuryLore.add("§fБаланс казны: §a" + String.format("%.2f", n.getTreasury()) + " " + n.getCurrencyCode());
        treasuryLore.add("§7Налог дохода: §b" + n.getTaxRate() + "%");
        treasuryLore.add("§7Инфляция: §" + (n.getInflation() > 20 ? "c" : n.getInflation() > 10 ? "e" : "a") + String.format("%.1f", n.getInflation()) + "%");
        inv.setItem(11, GuiUtils.button(Material.GOLD_BLOCK, "§b§lКазна", treasuryLore));
        
        List<String> currencyLore = new ArrayList<>();
        currencyLore.add("§fКод валюты: §b" + n.getCurrencyCode());
        currencyLore.add("§fКурс к AXC: §b" + String.format("%.2f", n.getExchangeRateToAXC()));
        currencyLore.add(" ");
        currencyLore.add("§7ЛКМ: +0.1");
        currencyLore.add("§7ПКМ: -0.1");
        currencyLore.add("§7(Только LEADER/MINISTER)");
        inv.setItem(13, GuiUtils.button(Material.PAPER, "§b§lВалюта", currencyLore));
        
        Nation.Role role = n.getRole(p.getUniqueId());
        boolean canPrint = (role == Nation.Role.LEADER || role == Nation.Role.MINISTER);
        List<String> printLore = new ArrayList<>();
        printLore.add("§7Требует подтверждения!");
        printLore.add("§fМакс. за раз: §b" + String.format("%.0f", 1000000.0));
        double maxDaily = n.getTreasury() * 0.2;
        printLore.add("§fМакс. в день: §b" + String.format("%.0f", maxDaily) + " (20% казны)");
        printLore.add(" ");
        if (!canPrint) {
            printLore.add("§cНедостаточно прав!");
        } else {
            printLore.add("§7ЛКМ: Открыть окно подтверждения");
        }
        Material printMat = canPrint ? Material.ANVIL : Material.BARRIER;
        inv.setItem(15, GuiUtils.button(printMat, canPrint ? "§b§lПечать денег" : "§c§lПечать денег", printLore));
        
        double playerBalance = ServiceLocator.get(WalletService.class).getBalance(p.getUniqueId());
        List<String> walletLore = new ArrayList<>();
        walletLore.add("§fВаш баланс: §a" + String.format("%.2f", playerBalance) + " " + n.getCurrencyCode());
        walletLore.add(" ");
        walletLore.add("§7ЛКМ: Перевести в казну +100");
        walletLore.add("§7ПКМ: Перевести из казны -100");
        inv.setItem(10, GuiUtils.button(Material.GOLD_INGOT, "§b§lКошелёк игрока", walletLore));
        
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§3§l─── ЭКОНОМИЧЕСКАЯ СТАТИСТИКА ───");
        statsLore.add(" ");
        
        EconomyService economyService = ServiceLocator.get(EconomyService.class);
        double gdp = economyService.getGDP(n.getId());
        String gdpColor = gdp > 50000 ? "§a" : gdp > 10000 ? "§e" : "§7";
        statsLore.add("§fВВП (24ч): " + gdpColor + formatLargeNumber(gdp) + " " + n.getCurrencyCode());
        
        double health = economyService.getEconomicHealth(n.getId());
        String healthColor = health >= 70 ? "§a" : health >= 40 ? "§e" : "§c";
        String healthIcon = health >= 70 ? "✓" : health >= 40 ? "⚠" : "✗";
        statsLore.add("§fЗдоровье экономики: " + healthColor + healthIcon + " " + String.format("%.1f", health) + "%");
        
        statsLore.add(" ");
        statsLore.add("§6§lБЮДЖЕТЫ:");
        double totalBudget = n.getBudgetMilitary() + n.getBudgetHealth() + n.getBudgetEducation();
        if (totalBudget > 0) {
            statsLore.add("§fВоенный: §c" + String.format("%.1f", (n.getBudgetMilitary() / totalBudget) * 100) + "%");
            statsLore.add("§fЗдоровье: §a" + String.format("%.1f", (n.getBudgetHealth() / totalBudget) * 100) + "%");
            statsLore.add("§fОбразование: §b" + String.format("%.1f", (n.getBudgetEducation() / totalBudget) * 100) + "%");
        } else {
            statsLore.add("§7Бюджеты не распределены");
        }
        
        TechnologyTreeService techTreeService = ServiceLocator.get(TechnologyTreeService.class);
        double tradeBonus = techTreeService.getBonus(n.getId(), "tradeBonus");
        if (tradeBonus > 1.0) {
            statsLore.add(" ");
            statsLore.add("§e§lБОНУСЫ:");
            statsLore.add("§fТорговля: §a+" + String.format("%.0f", (tradeBonus - 1.0) * 100) + "%");
            
            double productionBonus = techTreeService.getBonus(n.getId(), "productionBonus");
            if (productionBonus > 1.0) {
                statsLore.add("§fПроизводство: §a+" + String.format("%.0f", (productionBonus - 1.0) * 100) + "%");
            }
        }
        
        inv.setItem(22, GuiUtils.button(Material.ENCHANTED_BOOK, "§b§lСтатистика", statsLore));
        
        inv.setItem(31, GuiUtils.createCloseButton());

        TradeService tradeService = ServiceLocator.get(TradeService.class);
        List<String> tradeLore = new ArrayList<>();
        tradeLore.add("§e§lТОРГОВЛЯ");
        tradeLore.add(" ");
        
        int tradeTreaties = tradeService.getTradeTreatiesCount(n.getId());
        tradeLore.add("§fТорговых договоров: §b" + tradeTreaties);
        
        int allyTrades = 0;
        for (String allyId : n.getAllies()) {
            if (tradeService.hasTradeTreaty(n.getId(), allyId)) {
                allyTrades++;
            }
        }
        tradeLore.add("§fС союзниками: §a" + allyTrades);
        
        if (tradeBonus > 1.0) {
            tradeLore.add(" ");
            tradeLore.add("§aБонус торговли: +" + String.format("%.0f", (tradeBonus - 1.0) * 100) + "%");
        }
        
        tradeLore.add(" ");
        tradeLore.add("§7ЛКМ: Торговые договоры");
        inv.setItem(20, GuiUtils.button(Material.EMERALD, "§e§lТорговля", tradeLore));
        
        BankingService bankingService = ServiceLocator.get(BankingService.class);
        List<String> bankingLore = new ArrayList<>();
        bankingLore.add("§7Банковская система");
        List<com.axiom.service.BankingService.Loan> loans = bankingService.getActiveLoans(n.getId());
        bankingLore.add("§fАктивных кредитов: §b" + loans.size());
        if (!loans.isEmpty()) {
            double totalDebt = loans.stream().mapToDouble(l -> l.remaining).sum();
            bankingLore.add("§fОбщий долг: §c" + String.format("%.2f", totalDebt));
        }
        double rating = bankingService.calculateCreditRating(n.getId());
        String ratingText = rating >= 80 ? "§aОТЛИЧНЫЙ" : rating >= 60 ? "§eХОРОШИЙ" : "§6СРЕДНИЙ";
        bankingLore.add("§fКредитный рейтинг: " + ratingText);
        bankingLore.add(" ");
        bankingLore.add("§7ЛКМ: Открыть банковское меню");
        inv.setItem(28, GuiUtils.button(Material.GOLD_INGOT, "§6§lБанкинг", bankingLore));
        
        StockMarketService stockMarketService = ServiceLocator.get(StockMarketService.class);
        List<String> stockLore = new ArrayList<>();
        stockLore.add("§7Фондовый рынок");
        List<com.axiom.service.StockMarketService.Corporation> corps = stockMarketService.getCorporationsOf(n.getId());
        stockLore.add("§fКорпораций: §b" + corps.size());
        if (!corps.isEmpty()) {
            double totalValue = corps.stream().mapToDouble(c -> c.value).sum();
            stockLore.add("§fОбщая стоимость: §b" + String.format("%.2f", totalValue));
        }
        stockLore.add(" ");
        stockLore.add("§7ЛКМ: Управление корпорациями");
        inv.setItem(29, GuiUtils.button(Material.PAPER, "§e§lФондовый рынок", stockLore));
        
        Nation.Role budgetRole = n.getRole(p.getUniqueId());
        boolean canManageBudget = (budgetRole == Nation.Role.LEADER || budgetRole == Nation.Role.MINISTER);
        if (canManageBudget) {
            List<String> budgetLore = new ArrayList<>();
            budgetLore.add("§6§lУПРАВЛЕНИЕ БЮДЖЕТОМ");
            budgetLore.add(" ");
            budgetLore.add("§7Распределение бюджета");
            budgetLore.add("§7между секторами");
            budgetLore.add(" ");
            budgetLore.add("§7Используйте команду:");
            budgetLore.add("§b/axiom budget set <type> <percent>");
            inv.setItem(24, GuiUtils.button(Material.GOLD_BLOCK, "§6§lБюджет", budgetLore));
        }
        
        inv.setItem(45, GuiUtils.button(Material.ARROW, "§e◀ Назад", Arrays.asList("§7Вернуться в главное меню")));
        
        p.openInventory(inv);
    }
    
    private String formatLargeNumber(double number) {
        if (number >= 1_000_000_000) return String.format("%.2fB", number / 1_000_000_000);
        if (number >= 1_000_000) return String.format("%.2fM", number / 1_000_000);
        if (number >= 1_000) return String.format("%.2fK", number / 1_000);
        return String.format("%.2f", number);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Экономика")) return;
        
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
        int slot = e.getRawSlot();
        
        NationManager nationManager = ServiceLocator.get(NationManager.class);
        var opt = nationManager.getNationOfPlayer(p.getUniqueId());
        if (opt.isEmpty()) return;
        Nation n = opt.get();
        
        if (slot == 15) {
            Nation.Role role = n.getRole(p.getUniqueId());
            if (role != Nation.Role.LEADER && role != Nation.Role.MINISTER) {
                p.sendMessage("§cНедостаточно прав.");
                return;
            }
            p.closeInventory();
            p.sendMessage("§7Введите сумму печати: §b/axiom economy print <amount>");
            p.sendMessage("§7Откроется окно подтверждения.");
        } else if (slot == 13) {
            Nation.Role r = n.getRole(p.getUniqueId());
            if (r != Nation.Role.LEADER && r != Nation.Role.MINISTER) { 
                p.sendMessage("§cНедостаточно прав."); 
                return; 
            }
            boolean rightClick = e.isRightClick();
            double rate = n.getExchangeRateToAXC();
            rate += rightClick ? -0.1 : 0.1;
            if (rate < 0.1) rate = 0.1;
            n.setExchangeRateToAXC(Math.round(rate * 100.0) / 100.0);
            try { 
                nationManager.save(n);
                p.sendMessage("§aКурс обновлён: §b" + String.format("%.2f", rate));
            } catch (Exception ex) {
                p.sendMessage("§cОшибка сохранения: " + ex.getMessage());
            }
            open(p);
        } else if (slot == 10) {
            WalletService walletService = ServiceLocator.get(WalletService.class);
            boolean rightClick = e.isRightClick();
            if (rightClick) {
                if (n.getTreasury() < 100) { 
                    p.sendMessage("§cНедостаточно средств в казне."); 
                    return; 
                }
                n.setTreasury(n.getTreasury() - 100);
                walletService.deposit(p.getUniqueId(), 100);
                p.sendMessage("§aПереведено 100 " + n.getCurrencyCode() + " из казны");
            } else {
                if (!walletService.withdraw(p.getUniqueId(), 100)) { 
                    p.sendMessage("§cНедостаточно средств в кошельке."); 
                    return; 
                }
                n.setTreasury(n.getTreasury() + 100);
                p.sendMessage("§aПереведено 100 " + n.getCurrencyCode() + " в казну");
            }
            try { 
                nationManager.save(n);
            } catch (Exception ex) {
                p.sendMessage("§cОшибка сохранения: " + ex.getMessage());
            }
            open(p);
        } else if (slot == 20) {
            p.sendMessage("§7Торговые договоры:");
            p.sendMessage("§b/axiom diplomacy treaty create <nationId> trade <days>");
        } else if (slot == 24) {
            Nation.Role r = n.getRole(p.getUniqueId());
            if (r != Nation.Role.LEADER && r != Nation.Role.MINISTER) {
                p.sendMessage("§cНедостаточно прав.");
                return;
            }
            p.closeInventory();
            p.sendMessage("§7Управление бюджетом:");
            p.sendMessage("§b/axiom budget set <military|health|education> <percent>");
        } else if (slot == 28) {
            p.closeInventory();
            new com.axiom.gui.BankingMenu(ServiceLocator.get(com.axiom.AXIOM.class), ServiceLocator.get(NationManager.class), ServiceLocator.get(BankingService.class)).open(p);
        } else if (slot == 29) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте: §b/axiom stock list §7для просмотра корпораций");
            p.sendMessage("§eИспользуйте: §b/axiom stock create <name> <type> §7для создания");
        } else if (slot == 45) {
            ItemStack item = e.getCurrentItem();
            if (item != null && item.getType() == Material.ARROW) {
                new NationMainMenu().open(p);
            }
        } else if (slot == 31) {
            p.closeInventory();
        }
    }
}


