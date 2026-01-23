package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.BankingService;
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
 * Banking Menu - comprehensive banking interface.
 */
public class BankingMenu implements Listener {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final BankingService bankingService;
    
    private static class MenuHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return null;
        }
    }
    
    private static final MenuHolder HOLDER = new MenuHolder();

    public BankingMenu(AXIOM plugin, NationManager nationManager, BankingService bankingService) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.bankingService = bankingService;
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
            
            String title = GuiUtils.formatHeader("Банкинг: " + n.getName());
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
            
            // Active Loans
            List<BankingService.Loan> loans = bankingService.getActiveLoans(n.getId());
            List<String> loansLore = new ArrayList<>();
            loansLore.add("§7Ваши активные кредиты");
            loansLore.add("§fВсего: §b" + loans.size());
            if (loans.isEmpty()) {
                loansLore.add(" ");
                loansLore.add("§7Нет активных кредитов");
            } else {
                double totalDebt = 0.0;
                for (BankingService.Loan loan : loans) {
                    totalDebt += loan.remaining;
                }
                loansLore.add("§fОбщий долг: §c" + String.format("%.2f", totalDebt));
                loansLore.add(" ");
                loansLore.add("§7ЛКМ: Детали кредитов");
            }
            inv.setItem(10, GuiUtils.button(Material.BOOK, "§c§lАктивные кредиты", loansLore));
            
            // Loan Statistics
            Map<String, Object> stats = bankingService.getBankingStatistics(n.getId());
            List<String> statsLore = new ArrayList<>();
            statsLore.add("§3§l─── БАНКОВСКАЯ СТАТИСТИКА ───");
            statsLore.add(" ");
            if (stats != null) {
                statsLore.add("§fАктивных кредитов: §b" + stats.get("activeLoans"));
                statsLore.add("§fОбщий долг: §c" + String.format("%.2f", ((Number) stats.get("totalDebt")).doubleValue()));
                statsLore.add("§fВсего заёмов: §b" + String.format("%.2f", ((Number) stats.get("totalBorrowed")).doubleValue()));
                statsLore.add("§fПросрочено: §c" + stats.get("overdueLoans"));
                statsLore.add(" ");
                statsLore.add("§fВыдано кредитов: §b" + stats.get("loansAsLender"));
                statsLore.add("§fВсего выдано: §b" + String.format("%.2f", ((Number) stats.get("totalLent")).doubleValue()));
            }
            inv.setItem(11, GuiUtils.button(Material.EMERALD, "§b§lСтатистика", statsLore));
            
            // Credit Rating
            double rating = bankingService.calculateCreditRating(n.getId());
            List<String> ratingLore = new ArrayList<>();
            ratingLore.add("§7Кредитный рейтинг нации");
            String ratingColor = rating >= 80 ? "§a" : rating >= 60 ? "§e" : rating >= 40 ? "§6" : "§c";
            String ratingText = rating >= 80 ? "ОТЛИЧНЫЙ" : rating >= 60 ? "ХОРОШИЙ" : rating >= 40 ? "СРЕДНИЙ" : "НИЗКИЙ";
            ratingLore.add("§fРейтинг: " + ratingColor + ratingText + " §7(" + String.format("%.1f", rating) + "/100)");
            ratingLore.add(" ");
            if (rating >= 80) {
                ratingLore.add("§aВысокий рейтинг позволяет");
                ratingLore.add("§aполучать кредиты на лучших");
                ratingLore.add("§aусловиях!");
            } else if (rating < 40) {
                ratingLore.add("§cНизкий рейтинг может");
                ratingLore.add("§cограничить доступ к кредитам");
                ratingLore.add("§cили увеличить процентные ставки");
            }
            inv.setItem(12, GuiUtils.button(Material.GOLD_NUGGET, "§e§lКредитный рейтинг", ratingLore));
            
            // Request Loan Info
            List<String> requestLore = new ArrayList<>();
            requestLore.add("§7Запрос кредита у другой нации");
            requestLore.add(" ");
            requestLore.add("§7Используйте дипломатическое меню");
            requestLore.add("§7или команду для запроса кредита");
            inv.setItem(13, GuiUtils.button(Material.WRITABLE_BOOK, "§b§lЗапрос кредита", requestLore));
            
            // Issue Loan (as lender)
            Nation.Role role = n.getRole(player.getUniqueId());
            boolean canIssue = (role == Nation.Role.LEADER || role == Nation.Role.MINISTER);
            List<String> issueLore = new ArrayList<>();
            issueLore.add("§7Выдача кредита другой нации");
            if (!canIssue) {
                issueLore.add(" ");
                issueLore.add("§cТребуется: LEADER или MINISTER");
            } else {
                issueLore.add(" ");
                issueLore.add("§7Используйте дипломатическое");
                issueLore.add("§7меню для выдачи кредитов");
            }
            Material issueMat = canIssue ? Material.GOLD_BLOCK : Material.BARRIER;
            inv.setItem(14, GuiUtils.button(issueMat, canIssue ? "§e§lВыдать кредит" : "§c§lВыдать кредит", issueLore));
            
            // Repay Loan
            List<String> repayLore = new ArrayList<>();
            repayLore.add("§7Погашение кредита");
            if (loans.isEmpty()) {
                repayLore.add(" ");
                repayLore.add("§7Нет кредитов для погашения");
            } else {
                repayLore.add(" ");
                repayLore.add("§7Используйте команду:");
                repayLore.add("§b/axiom banking repay <lenderId> <amount>");
            }
            inv.setItem(15, GuiUtils.button(Material.GOLD_INGOT, "§a§lПогасить кредит", repayLore));
            
            // Back button
            inv.setItem(45, GuiUtils.button(Material.ARROW, "§e◀ Назад", Arrays.asList("§7Вернуться в главное меню")));
            
            // Close button
            inv.setItem(49, GuiUtils.createCloseButton());
            
            player.openInventory(inv);
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening banking menu: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cОшибка: " + e.getMessage());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.contains("Банкинг")) return;
        
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
        
        if (displayName.contains("Активные кредиты")) {
            var opt = nationManager.getNationOfPlayer(p.getUniqueId());
            if (opt.isPresent()) {
                List<BankingService.Loan> loans = bankingService.getActiveLoans(opt.get().getId());
                if (loans.isEmpty()) {
                    p.sendMessage("§aНет активных кредитов.");
                } else {
                    p.sendMessage("§b§l=== АКТИВНЫЕ КРЕДИТЫ ===");
                    for (BankingService.Loan loan : loans) {
                        Nation lender = nationManager.getNationById(loan.lenderNationId);
                        String lenderName = lender != null ? lender.getName() : loan.lenderNationId;
                        p.sendMessage("§7Кредит от §f" + lenderName + ":");
                        p.sendMessage("§7  Сумма: §b" + String.format("%.2f", loan.remaining) + " §7(первоначально: §b" + String.format("%.2f", loan.principal) + "§7)");
                        p.sendMessage("§7  Ставка: §e" + loan.interestRate + "% годовых");
                    }
                }
            }
        } else if (displayName.contains("Запрос кредита") || displayName.contains("Выдать кредит")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте дипломатическое меню для операций с кредитами");
            plugin.getNationMainMenu().open(p);
        } else if (displayName.contains("Погасить")) {
            p.closeInventory();
            p.sendMessage("§eИспользуйте: §b/axiom banking repay <lenderNationId> <amount>");
        }
    }
}

