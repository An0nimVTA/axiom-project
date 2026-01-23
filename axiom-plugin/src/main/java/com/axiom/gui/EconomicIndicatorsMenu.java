package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.EconomicIndicatorsService;
import com.axiom.service.EconomicTrend;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.ArrayList;

/**
 * GUI меню для отображения экономических показателей наций
 * Показывает ВВП, инфляцию, безработицу, развитие и другие показатели
 */
public class EconomicIndicatorsMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final EconomicIndicatorsService economicService;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 21; // Количество слотов под элементы на странице

    public EconomicIndicatorsMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Экономические показатели");
        this.plugin = plugin;
        this.economicService = plugin.getEconomicIndicatorsService();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Кнопка "Назад" (Material.ARROW)
        addCard(new Card(
            Material.ARROW,
            "Назад",
            "Вернуться в главное меню",
            (player) -> {
                // Показываем главное меню нации
                plugin.openNationMainMenu(player);
            }
        ));
        
        // Общая статистика
        addCard(new Card(
            Material.BEACON,
            "Статистика экономики",
            "Глобальные экономические|показатели сервера",
            (player) -> {
                showGlobalEconomicStats(player);
            }
        ));
        
        // Рейтинг наций по ВВП
        addCard(new Card(
            Material.EMERALD_BLOCK,
            "ВВП наций",
            "Рейтинг по ВВП|всех наций сервера",
            (player) -> {
                showGdpRankings(player);
            }
        ));
        
        // Рейтинг по уровню развития
        addCard(new Card(
            Material.ENCHANTING_TABLE,
            "Развитие наций",
            "Рейтинг по уровню|развития",
            (player) -> {
                showDevelopmentRankings(player);
            }
        ));
        
        // Инфляция и безработица
        addCard(new Card(
            Material.REDSTONE_BLOCK,
            "Инфляция и безработица",
            "Уровень инфляции|и безработицы",
            (player) -> {
                showInflationUnemploymentStats(player);
            }
        ));
        
        // Торговля и бюджет
        addCard(new Card(
            Material.GOLD_BLOCK,
            "Торговля и бюджет",
            "Экспорт, импорт|и бюджетный баланс",
            (player) -> {
                showTradeBudgetStats(player);
            }
        ));
        
        // Персональная статистика (если игрок в нации)
        addCard(new Card(
            Material.PLAYER_HEAD,
            "Моя нация",
            "Экономические показатели|моей нации",
            (player) -> {
                showPersonalNationStats(player);
            }
        ));
        
        // Настройки отображения
        addCard(new Card(
            Material.COMPARATOR,
            "Настройки",
            "Изменить настройки|отображения статистики",
            (player) -> {
                showDisplaySettings(player);
            }
        ));
        
        // Справка
        addCard(new Card(
            Material.KNOWLEDGE_BOOK,
            "Справка",
            "Описание экономических|показателей",
            (player) -> {
                showHelpInfo(player);
            }
        ));
    }
    
    /**
     * Показать глобальную экономическую статистику
     */
    private void showGlobalEconomicStats(Player player) {
        var globalData = economicService.getGlobalEconomicData();
        
        player.sendMessage(ChatColor.GOLD + "=== Глобальные экономические показатели ===");
        player.sendMessage(ChatColor.YELLOW + "Глобальный ВВП: " + ChatColor.WHITE + String.format("%.2f", globalData.getGlobalGdp()));
        player.sendMessage(ChatColor.YELLOW + "Средняя инфляция: " + ChatColor.WHITE + String.format("%.2f%%", globalData.getAverageInflation()));
        player.sendMessage(ChatColor.YELLOW + "Средняя безработица: " + ChatColor.WHITE + String.format("%.2f%%", globalData.getAverageUnemployment()));
        player.sendMessage(ChatColor.YELLOW + "Глобальный объем торговли: " + ChatColor.WHITE + String.format("%.2f", globalData.getGlobalTradeVolume()));
        player.sendMessage(ChatColor.YELLOW + "Средний индекс развития: " + ChatColor.WHITE + String.format("%.2f", globalData.getGlobalDevelopmentIndex()));
    }
    
    /**
     * Показать рейтинг наций по ВВП
     */
    private void showGdpRankings(Player player) {
        var allData = economicService.getAllEconomicData();
        
        // Сортировка по ВВП
        var sortedByGdp = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByGdp.sort((a, b) -> Double.compare(b.getGDP(), a.getGDP()));
        
        player.sendMessage(ChatColor.GOLD + "=== Рейтинг наций по ВВП ===");
        int rank = 1;
        for (var data : sortedByGdp) {
            if (rank > 10) break; // Топ-10
            
            String grade = data.getEconomicHealthGrade();
            ChatColor gradeColor = getGradeColor(grade);
            
            player.sendMessage(String.format("%d. §b%s §7(ВВП: §e%.2f§7, Оценка: %s§s%s§7)", 
                rank++, data.getNationId(), data.getGDP(), gradeColor, grade));
        }
    }
    
    /**
     * Показать рейтинг по уровню развития
     */
    private void showDevelopmentRankings(Player player) {
        var allData = economicService.getAllEconomicData();
        
        // Сортировка по индексу развития
        var sortedByDev = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByDev.sort((a, b) -> Double.compare(b.getDevelopmentIndex(), a.getDevelopmentIndex()));
        
        player.sendMessage(ChatColor.GOLD + "=== Рейтинг наций по развитию ===");
        int rank = 1;
        for (var data : sortedByDev) {
            if (rank > 10) break; // Топ-10
            
            String grade = data.getEconomicHealthGrade();
            ChatColor gradeColor = getGradeColor(grade);
            
            player.sendMessage(String.format("%d. §b%s §7(Развитие: §a%.2f§7, Оценка: %s§s%s§7)", 
                rank++, data.getNationId(), data.getDevelopmentIndex(), gradeColor, grade));
        }
    }
    
    /**
     * Показать статистику инфляции и безработицы
     */
    private void showInflationUnemploymentStats(Player player) {
        var allData = economicService.getAllEconomicData();
        
        // Сортировка по инфляции
        var sortedByLowInflation = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByLowInflation.sort((a, b) -> Double.compare(a.getInflationRate(), b.getInflationRate()));
        
        player.sendMessage(ChatColor.AQUA + "=== Низкая инфляция (топ-5) ===");
        int rank = 1;
        for (var data : sortedByLowInflation) {
            if (rank > 5) break;
            player.sendMessage(String.format("%d. §b%s §7(Инфляция: §a%.2f%%§7)", 
                rank++, data.getNationId(), data.getInflationRate()));
        }
        
        // Топ-5 по высокой инфляции
        var sortedByHighInflation = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByHighInflation.sort((a, b) -> Double.compare(b.getInflationRate(), a.getInflationRate()));
        
        player.sendMessage(ChatColor.RED + "=== Высокая инфляция (топ-5) ===");
        rank = 1;
        for (var data : sortedByHighInflation) {
            if (rank > 5) break;
            player.sendMessage(String.format("%d. §b%s §7(Инфляция: §c%.2f%%§7)", 
                rank++, data.getNationId(), data.getInflationRate()));
        }
        
        // Показываем топ-5 с низкой безработицей
        var sortedByLowUnemployment = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByLowUnemployment.sort((a, b) -> Double.compare(a.getUnemploymentRate(), b.getUnemploymentRate()));
        
        player.sendMessage(ChatColor.AQUA + "=== Низкая безработица (топ-5) ===");
        rank = 1;
        for (var data : sortedByLowUnemployment) {
            if (rank > 5) break;
            player.sendMessage(String.format("%d. §b%s §7(Безработица: §a%.2f%%§7)", 
                rank++, data.getNationId(), data.getUnemploymentRate()));
        }
        
        // Показываем топ-5 с высокой безработицей
        var sortedByHighUnemployment = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByHighUnemployment.sort((a, b) -> Double.compare(b.getUnemploymentRate(), a.getUnemploymentRate()));
        
        player.sendMessage(ChatColor.RED + "=== Высокая безработица (топ-5) ===");
        rank = 1;
        for (var data : sortedByHighUnemployment) {
            if (rank > 5) break;
            player.sendMessage(String.format("%d. §b%s §7(Безработица: §c%.2f%%§7)", 
                rank++, data.getNationId(), data.getUnemploymentRate()));
        }
    }
    
    /**
     * Показать статистику торговли и бюджета
     */
    private void showTradeBudgetStats(Player player) {
        var allData = economicService.getAllEconomicData();
        
        // Сортировка по торговому балансу
        var sortedByTrade = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByTrade.sort((a, b) -> Double.compare(b.getTradeBalance(), a.getTradeBalance()));
        
        player.sendMessage(ChatColor.GOLD + "=== Торговый баланс (топ-5) ===");
        int rank = 1;
        for (var data : sortedByTrade) {
            if (rank > 5) break; // Топ-5
            String tradeSymbol = data.getTradeBalance() >= 0 ? "§a+" : "§c";
            player.sendMessage(String.format("%d. §b%s §7(Экспорт: §e%.2f§7, Импорт: §e%.2f§7, Баланс: %s%.2f§7)", 
                rank++, data.getNationId(), 
                data.getExportValue(), data.getImportValue(), 
                tradeSymbol, data.getTradeBalance()));
        }
        
        // Сортировка по бюджетному балансу
        var sortedByBudget = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByBudget.sort((a, b) -> Double.compare(b.getBudgetBalance(), a.getBudgetBalance()));
        
        player.sendMessage(ChatColor.GOLD + "=== Бюджетный баланс (топ-5) ===");
        rank = 1;
        for (var data : sortedByBudget) {
            if (rank > 5) break; // Топ-5
            String budgetSymbol = data.getBudgetBalance() >= 0 ? "§a+" : "§c";
            player.sendMessage(String.format("%d. §b%s §7(Бюджет: %s%.2f§7)", 
                rank++, data.getNationId(), 
                budgetSymbol, data.getBudgetBalance()));
        }
    }
    
    /**
     * Показать статистику своей нации
     */
    private void showPersonalNationStats(Player player) {
        var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
        if (!nationOpt.isPresent()) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в нации!");
            return;
        }
        
        String nationId = nationOpt.get().getId();
        var econData = economicService.getEconomicData(nationId);
        
        if (econData == null) {
            player.sendMessage(ChatColor.RED + "Данные для вашей нации недоступны.");
            return;
        }
        
        String grade = econData.getEconomicHealthGrade();
        ChatColor gradeColor = getGradeColor(grade);
        
        player.sendMessage(ChatColor.GOLD + "=== Экономика вашей нации ===");
        player.sendMessage(ChatColor.YELLOW + "Нация: " + ChatColor.WHITE + econData.getNationId());
        player.sendMessage(ChatColor.YELLOW + "ВВП: " + ChatColor.WHITE + String.format("%.2f", econData.getGDP()));
        player.sendMessage(ChatColor.YELLOW + "Инфляция: " + ChatColor.WHITE + String.format("%.2f%%", econData.getInflationRate()));
        player.sendMessage(ChatColor.YELLOW + "Безработица: " + ChatColor.WHITE + String.format("%.2f%%", econData.getUnemploymentRate()));
        player.sendMessage(ChatColor.YELLOW + "Развитие: " + ChatColor.WHITE + String.format("%.2f", econData.getDevelopmentIndex()));
        player.sendMessage(ChatColor.YELLOW + "Экспорт: " + ChatColor.WHITE + String.format("%.2f", econData.getExportValue()));
        player.sendMessage(ChatColor.YELLOW + "Импорт: " + ChatColor.WHITE + String.format("%.2f", econData.getImportValue()));
        player.sendMessage(ChatColor.YELLOW + "Торговый баланс: " + ChatColor.WHITE + String.format("%.2f", econData.getTradeBalance()));
        player.sendMessage(ChatColor.YELLOW + "Бюджетный баланс: " + ChatColor.WHITE + String.format("%.2f", econData.getBudgetBalance()));
        player.sendMessage(ChatColor.YELLOW + "Оценка: " + gradeColor + ChatColor.BOLD + grade);
        
        // Показываем тренды
        showEconomicTrends(player, nationId);
    }
    
    /**
     * Показать тренды экономических показателей
     */
    private void showEconomicTrends(Player player, String nationId) {
        player.sendMessage(ChatColor.GOLD + "=== Тренды ===");
        
        var trends = economicService.getEconomicTrends(nationId);
        // Using mock/simple trend display for now as getEconomicTrend method is not available
        player.sendMessage(ChatColor.YELLOW + "ВВП: " + ChatColor.GREEN + "↗ " + trends.getOrDefault("gdp_trend", "stable"));
        player.sendMessage(ChatColor.YELLOW + "Инфляция: " + ChatColor.YELLOW + "→ " + trends.getOrDefault("inflation_trend", "stable"));
        player.sendMessage(ChatColor.YELLOW + "Развитие: " + ChatColor.GREEN + "↗ " + trends.getOrDefault("investment_trend", "stable"));
    }
    
    /**
     * Получить цвет для тренда
     */
    private ChatColor getTrendColor(com.axiom.service.EconomicTrend trend) {
        switch (trend) {
            case UP: return ChatColor.GREEN;
            case DOWN: return ChatColor.RED;
            case STABLE: return ChatColor.YELLOW;
            default: return ChatColor.GRAY;
        }
    }
    
    /**
     * Получить цвет для оценки
     */
    private ChatColor getGradeColor(String grade) {
        switch (grade.toUpperCase()) {
            case "A": return ChatColor.GREEN;
            case "B": return ChatColor.YELLOW;
            case "C": return ChatColor.GOLD;
            case "D": return ChatColor.RED;
            case "E": return ChatColor.DARK_RED;
            default: return ChatColor.GRAY;
        }
    }
    
    /**
     * Показать настройки отображения
     */
    private void showDisplaySettings(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Настройки отображения ===");
        player.sendMessage(ChatColor.YELLOW + "Интервал обновления: " + ChatColor.WHITE + "5 минут");
        player.sendMessage(ChatColor.YELLOW + "Ретеншен данных: " + ChatColor.WHITE + "Последние 10 значений");
        player.sendMessage(ChatColor.GOLD + "Для изменения настроек используйте: /axiom config economic");
    }
    
    /**
     * Показать справку по экономическим показателям
     */
    private void showHelpInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Справка по экономическим показателям ===");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "ВВП (Валовой Внутренний Продукт): Общая стоимость товаров и услуг");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Инфляция: Темп роста цен на товары и услуги");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Безработица: Процент трудоспособного населения без работы");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Индекс развития: Комбинированный показатель экономического/социального развития");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Торговый баланс: Разница между экспортом и импортом");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Бюджетный баланс: Разница между доходами и расходами казны");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Оценка (A-F): Общая оценка экономического здоровья нации");
    }
}