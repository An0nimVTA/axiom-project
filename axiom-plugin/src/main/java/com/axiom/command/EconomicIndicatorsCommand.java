package com.axiom.command;

import com.axiom.AXIOM;
import com.axiom.service.EconomicIndicatorsService;
import com.axiom.service.EconomicTrend;
import com.axiom.gui.EconomicIndicatorsMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Команда для управления экономическим индикатором
 */
public class EconomicIndicatorsCommand implements CommandExecutor {
    private final AXIOM plugin;
    
    public EconomicIndicatorsCommand(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Открыть GUI меню экономических индикаторов
            new EconomicIndicatorsMenu(plugin, player).open();
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "global":
                return handleGlobalCommand(sender);
            case "gdp":
                return handleGdpCommand(sender);
            case "development":
                return handleDevelopmentCommand(sender);
            case "inflation":
                return handleInflationCommand(sender);
            case "unemployment":
                return handleUnemploymentCommand(sender);
            case "trade":
                return handleTradeCommand(sender);
            case "budget":
                return handleBudgetCommand(sender);
            case "my":
            case "nation":
                return handleMyNationCommand(sender);
            case "trends":
                return handleTrendsCommand(sender);
            case "help":
                showHelp(sender);
                return true;
            case "stats":
                return handleStatsCommand(sender);
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная подкоманда: " + subCommand);
                showHelp(sender);
                return true;
        }
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.BOLD + "Команды экономических индикаторов" + ChatColor.RESET + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators" + ChatColor.GRAY + " - Открыть GUI меню");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators stats" + ChatColor.GRAY + " - Глобальные статистики");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators gdp" + ChatColor.GRAY + " - Рейтинг по ВВП");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators development" + ChatColor.GRAY + " - Рейтинг по уровню развития");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators inflation" + ChatColor.GRAY + " - Рейтинг по инфляции");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators unemployment" + ChatColor.GRAY + " - Рейтинг по безработице");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators trade" + ChatColor.GRAY + " - Торговые балансы");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators budget" + ChatColor.GRAY + " - Бюджетные балансы");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators my" + ChatColor.GRAY + " - Показатели моей нации");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators trends" + ChatColor.GRAY + " - Тренды экономических показателей");
        sender.sendMessage(ChatColor.YELLOW + "/econom indicators help" + ChatColor.GRAY + " - Показать эту справку");
    }
    
    private boolean handleStatsCommand(CommandSender sender) {
        var stats = plugin.getEconomicIndicatorsService().getEconomicStatistics();
        
        sender.sendMessage(ChatColor.GOLD + "=== Экономические показатели ===");
        sender.sendMessage(ChatColor.YELLOW + "Всего отслеживаемых наций: " + ChatColor.WHITE + stats.get("totalNationsTracked"));
        sender.sendMessage(ChatColor.YELLOW + "Интервал обновления: " + ChatColor.WHITE + stats.get("updateIntervalSeconds") + " секунд");
        sender.sendMessage(ChatColor.YELLOW + "Средний ВВП: " + ChatColor.WHITE + String.format("%.2f", (double)stats.get("averageGDP")));
        sender.sendMessage(ChatColor.YELLOW + "Средняя инфляция: " + ChatColor.WHITE + String.format("%.2f%%", (double)stats.get("averageInflation")));
        sender.sendMessage(ChatColor.YELLOW + "Средняя безработица: " + ChatColor.WHITE + String.format("%.2f%%", (double)stats.get("averageUnemployment")));
        sender.sendMessage(ChatColor.YELLOW + "Средний индекс развития: " + ChatColor.WHITE + String.format("%.2f", (double)stats.get("averageDevelopment")));
        sender.sendMessage(ChatColor.YELLOW + "Топ нация по ВВП: " + ChatColor.WHITE + stats.get("topGDPNation"));
        sender.sendMessage(ChatColor.YELLOW + "Топ нация по развитию: " + ChatColor.WHITE + stats.get("topDevelopmentNation"));
        
        return true;
    }
    
    private boolean handleGlobalCommand(CommandSender sender) {
        var globalData = plugin.getEconomicIndicatorsService().getGlobalEconomicData();
        
        sender.sendMessage(ChatColor.GOLD + "=== Глобальные экономические показатели ===");
        sender.sendMessage(ChatColor.YELLOW + "Глобальный ВВП: " + ChatColor.WHITE + String.format("%.2f", globalData.getGlobalGdp()));
        sender.sendMessage(ChatColor.YELLOW + "Средняя инфляция: " + ChatColor.WHITE + String.format("%.2f%%", globalData.getAverageInflation()));
        sender.sendMessage(ChatColor.YELLOW + "Средняя безработица: " + ChatColor.WHITE + String.format("%.2f%%", globalData.getAverageUnemployment()));
        sender.sendMessage(ChatColor.YELLOW + "Объем глобальной торговли: " + ChatColor.WHITE + String.format("%.2f", globalData.getGlobalTradeVolume()));
        sender.sendMessage(ChatColor.YELLOW + "Средний индекс развития: " + ChatColor.WHITE + String.format("%.2f", globalData.getGlobalDevelopmentIndex()));
        
        return true;
    }
    
    private boolean handleGdpCommand(CommandSender sender) {
        var allData = plugin.getEconomicIndicatorsService().getAllEconomicData();
        
        // Сортировка по ВВП
        var sortedByGdp = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByGdp.sort((a, b) -> Double.compare(b.getGDP(), a.getGDP()));
        
        sender.sendMessage(ChatColor.GOLD + "=== Рейтинг наций по ВВП ===");
        int rank = 1;
        for (var data : sortedByGdp) {
            if (rank > 10) break; // Топ-10
            
            String grade = data.getEconomicHealthGrade();
            ChatColor gradeColor;
            switch (grade) {
                case "A": gradeColor = ChatColor.GREEN; break;
                case "B": gradeColor = ChatColor.YELLOW; break;
                case "C": gradeColor = ChatColor.GOLD; break;
                case "D": gradeColor = ChatColor.RED; break;
                case "E": gradeColor = ChatColor.DARK_RED; break;
                default: gradeColor = ChatColor.GRAY; break;
            }
            
            sender.sendMessage(String.format("%d. §b%s §7(ВВП: §e%.2f§7, Оценка: %s§s%s§7)", 
                rank++, data.getNationId(), data.getGDP(), gradeColor, grade));
        }
        
        return true;
    }
    
    private boolean handleDevelopmentCommand(CommandSender sender) {
        var allData = plugin.getEconomicIndicatorsService().getAllEconomicData();
        
        // Сортировка по индексу развития
        var sortedByDev = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByDev.sort((a, b) -> Double.compare(b.getDevelopmentIndex(), a.getDevelopmentIndex()));
        
        sender.sendMessage(ChatColor.GOLD + "=== Рейтинг наций по развитию ===");
        int rank = 1;
        for (var data : sortedByDev) {
            if (rank > 10) break; // Топ-10
            
            String grade = data.getEconomicHealthGrade();
            ChatColor gradeColor;
            switch (grade) {
                case "A": gradeColor = ChatColor.GREEN; break;
                case "B": gradeColor = ChatColor.YELLOW; break;
                case "C": gradeColor = ChatColor.GOLD; break;
                case "D": gradeColor = ChatColor.RED; break;
                case "E": gradeColor = ChatColor.DARK_RED; break;
                default: gradeColor = ChatColor.GRAY; break;
            }
            
            sender.sendMessage(String.format("%d. §b%s §7(Развитие: §a%.2f§7, Оценка: %s§s%s§7)", 
                rank++, data.getNationId(), data.getDevelopmentIndex(), gradeColor, grade));
        }
        
        return true;
    }
    
    private boolean handleInflationCommand(CommandSender sender) {
        var allData = plugin.getEconomicIndicatorsService().getAllEconomicData();
        
        // Сортировка по инфляции (от низкой к высокой)
        var sortedByLowInflation = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByLowInflation.sort((a, b) -> Double.compare(a.getInflationRate(), b.getInflationRate()));
        
        sender.sendMessage(ChatColor.GOLD + "=== Рейтинг по инфляции (низкая - первая) ===");
        int rank = 1;
        for (var data : sortedByLowInflation) {
            if (rank > 10) break;
            sender.sendMessage(String.format("%d. §b%s §7(Инфляция: §a%.2f%%§7)", 
                rank++, data.getNationId(), data.getInflationRate()));
        }
        
        // Топ-10 по высокой инфляции
        var sortedByHighInflation = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByHighInflation.sort((a, b) -> Double.compare(b.getInflationRate(), a.getInflationRate()));
        
        sender.sendMessage(ChatColor.RED + "=== Рейтинг по инфляции (высокая - первая) ===");
        rank = 1;
        for (var data : sortedByHighInflation) {
            if (rank > 10) break;
            sender.sendMessage(String.format("%d. §b%s §7(Инфляция: §c%.2f%%§7)", 
                rank++, data.getNationId(), data.getInflationRate()));
        }
        
        return true;
    }
    
    private boolean handleUnemploymentCommand(CommandSender sender) {
        var allData = plugin.getEconomicIndicatorsService().getAllEconomicData();
        
        // Сортировка по безработице (от низкой к высокой)
        var sortedByLowUnemployment = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByLowUnemployment.sort((a, b) -> Double.compare(a.getUnemploymentRate(), b.getUnemploymentRate()));
        
        sender.sendMessage(ChatColor.GOLD + "=== Рейтинг по безработице (низкая - первая) ===");
        int rank = 1;
        for (var data : sortedByLowUnemployment) {
            if (rank > 10) break;
            sender.sendMessage(String.format("%d. §b%s §7(Безработица: §a%.2f%%§7)", 
                rank++, data.getNationId(), data.getUnemploymentRate()));
        }
        
        // Топ-10 по высокой безработице
        var sortedByHighUnemployment = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByHighUnemployment.sort((a, b) -> Double.compare(b.getUnemploymentRate(), a.getUnemploymentRate()));
        
        sender.sendMessage(ChatColor.RED + "=== Рейтинг по безработице (высокая - первая) ===");
        rank = 1;
        for (var data : sortedByHighUnemployment) {
            if (rank > 10) break;
            sender.sendMessage(String.format("%d. §b%s §7(Безработица: §c%.2f%%§7)", 
                rank++, data.getNationId(), data.getUnemploymentRate()));
        }
        
        return true;
    }
    
    private boolean handleTradeCommand(CommandSender sender) {
        var allData = plugin.getEconomicIndicatorsService().getAllEconomicData();
        
        // Сортировка по торговому балансу (от лучшего к худшему)
        var sortedByTrade = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByTrade.sort((a, b) -> Double.compare(b.getTradeBalance(), a.getTradeBalance()));
        
        sender.sendMessage(ChatColor.GOLD + "=== Рейтинг по торговому балансу ===");
        int rank = 1;
        for (var data : sortedByTrade) {
            if (rank > 10) break;
            String tradeSymbol = data.getTradeBalance() >= 0 ? "§a+" : "§c";
            sender.sendMessage(String.format("%d. §b%s §7(Экспорт: §e%.2f§7, Импорт: §e%.2f§7, Баланс: %s%.2f§7)", 
                rank++, data.getNationId(), 
                data.getExportValue(), data.getImportValue(), 
                tradeSymbol, data.getTradeBalance()));
        }
        
        return true;
    }
    
    private boolean handleBudgetCommand(CommandSender sender) {
        var allData = plugin.getEconomicIndicatorsService().getAllEconomicData();
        
        // Сортировка по бюджетному балансу (от лучшего к худшему)
        var sortedByBudgetBalance = new java.util.ArrayList<com.axiom.service.EconomicIndicatorsService.EconomicData>(allData);
        sortedByBudgetBalance.sort((a, b) -> Double.compare(b.getBudgetBalance(), a.getBudgetBalance()));
        
        sender.sendMessage(ChatColor.GOLD + "=== Рейтинг по бюджетному балансу ===");
        int rank = 1;
        for (var data : sortedByBudgetBalance) {
            if (rank > 10) break;
            String budgetSymbol = data.getBudgetBalance() >= 0 ? "§a+" : "§c";
            sender.sendMessage(String.format("%d. §b%s §7(Доходы: §e%.2f§7, Расходы: §e%.2f§7, Баланс: %s%.2f§7)", 
                rank++, data.getNationId(), 
                data.getTaxRevenue(), data.getExpenses(), 
                budgetSymbol, data.getBudgetBalance()));
        }
        
        return true;
    }
    
    private boolean handleMyNationCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
        if (!nationOpt.isPresent()) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в нации!");
            return true;
        }
        
        String nationId = nationOpt.get().getId();
        var econData = plugin.getEconomicIndicatorsService().getEconomicData(nationId);
        
        if (econData == null) {
            player.sendMessage(ChatColor.RED + "Данные для вашей нации не доступны.");
            return true;
        }
        
        String grade = econData.getEconomicHealthGrade();
        ChatColor gradeColor;
        switch (grade) {
            case "A": gradeColor = ChatColor.GREEN; break;
            case "B": gradeColor = ChatColor.YELLOW; break;
            case "C": gradeColor = ChatColor.GOLD; break;
            case "D": gradeColor = ChatColor.RED; break;
            case "E": gradeColor = ChatColor.DARK_RED; break;
            default: gradeColor = ChatColor.GRAY; break;
        }
        
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
        
        return true;
    }
    
    private boolean handleTrendsCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
        if (!nationOpt.isPresent()) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в нации!");
            return true;
        }
        
        String nationId = nationOpt.get().getId();
        
        player.sendMessage(ChatColor.GOLD + "=== Тренды экономических показателей ===");
        
        // Здесь должна быть логика получения трендов, но так как мы не реализовали метод в сервисе, просто покажем пример
        player.sendMessage(ChatColor.YELLOW + "ВВП: §a↗ Рост (тренд не реализован в демо-версии)");
        player.sendMessage(ChatColor.YELLOW + "Инфляция: §c↘ Снижение (тренд не реализован в демо-версии)");
        player.sendMessage(ChatColor.YELLOW + "Безработица: §e→ Стабильность (тренд не реализован в демо-версии)");
        player.sendMessage(ChatColor.YELLOW + "Развитие: §a↗ Рост (тренд не реализован в демо-версии)");
        
        // В реальной реализации здесь будет:
        /*
        var gdpTrend = plugin.getEconomicIndicatorsService().getEconomicTrend(nationId, EconomicIndicatorsService.EconomicIndicator.GDP);
        var inflationTrend = plugin.getEconomicIndicatorsService().getEconomicTrend(nationId, EconomicIndicatorsService.EconomicIndicator.INFLATION);
        var unemploymentTrend = plugin.getEconomicIndicatorsService().getEconomicTrend(nationId, EconomicIndicatorsService.EconomicIndicator.UNEMPLOYMENT);
        var developmentTrend = plugin.getEconomicIndicatorsService().getEconomicTrend(nationId, EconomicIndicatorsService.EconomicIndicator.DEVELOPMENT_INDEX);
        
        player.sendMessage(ChatColor.YELLOW + "ВВП: " + getTrendColor(gdpTrend) + gdpTrend.getSymbol() + " " + gdpTrend.getDisplayName());
        player.sendMessage(ChatColor.YELLOW + "Инфляция: " + getTrendColor(inflationTrend) + inflationTrend.getSymbol() + " " + inflationTrend.getDisplayName());
        player.sendMessage(ChatColor.YELLOW + "Безработица: " + getTrendColor(unemploymentTrend) + unemploymentTrend.getSymbol() + " " + unemploymentTrend.getDisplayName());
        player.sendMessage(ChatColor.YELLOW + "Развитие: " + getTrendColor(developmentTrend) + developmentTrend.getSymbol() + " " + developmentTrend.getDisplayName());
        */
        
        return true;
    }
    
    private ChatColor getTrendColor(com.axiom.service.EconomicTrend trend) {
        switch (trend) {
            case UP: return ChatColor.GREEN;
            case DOWN: return ChatColor.RED;
            case STABLE: return ChatColor.YELLOW;
            default: return ChatColor.GRAY;
        }
    }
}