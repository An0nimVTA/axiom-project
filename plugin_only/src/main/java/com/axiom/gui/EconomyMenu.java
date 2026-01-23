package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.EconomyService;
import com.axiom.service.NationManager;
import org.bukkit.entity.Player;

/**
 * GUI-меню экономической системы с использованием карточек
 */
public class EconomyMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final EconomyService economyService;

    public EconomyMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Экономическая система");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
        this.economyService = plugin.getEconomyService();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Мой кошелёк (Material.EMERALD)
        addCard(new Card(
            org.bukkit.Material.EMERALD,
            "Кошелёк",
            "Проверить личный баланс|и историю транзакций",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Ваш баланс: " + economyService.getBalance(player.getUniqueId()));
                // Открытие GUI кошелька
                plugin.getWalletService().openWalletMenu(player);
            }
        ));

        // Карточка: Банковский счёт (Material.ANVIL)
        addCard(new Card(
            org.bukkit.Material.ANVIL,
            "Банковский счёт",
            "Открыть сберегательный|или текущий счёт",
            (player) -> {
                close();
                // Проверяем, разблокирована ли банковская технология
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "banking");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для доступа к банковским услугам требуется изучить технологию 'Банковское дело'!");
                    return;
                }
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие банковского меню...");
                // Открытие банковского меню
                new com.axiom.gui.BankingMenu(plugin, player).open();
            }
        ));

        // Карточка: Перевод (Material.PAPER)
        addCard(new Card(
            org.bukkit.Material.PAPER,
            "Перевод",
            "Отправить деньги|другому игроку",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Используйте команду: /wallet pay <игрок> <сумма>");
            }
        ));

        // Карточка: Торговля (Material.CHEST)
        addCard(new Card(
            org.bukkit.Material.CHEST,
            "Торговля",
            "Купить/продать ресурсы|и товары",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие торговой площадки...");
                // Открытие торговой системы
                plugin.getTradingPostService().openTradingMenu(player);
            }
        ));

        // Карточка: Налоги (Material.GOLD_INGOT)
        addCard(new Card(
            org.bukkit.Material.GOLD_INGOT,
            "Налоги",
            "Управление налоговыми|ставками нации",
            (player) -> {
                // Проверяем, является ли игрок лидером нации
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации для управления налогами!");
                    return;
                }
                
                if (!nationOpt.get().getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Только лидер нации может управлять налогами!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие настроек налогов...");
                // Открытие настроек налогов
                player.performCommand("nation taxes");
            }
        ));

        // Карточка: Фондовая биржа (Material.DIAMOND)
        addCard(new Card(
            org.bukkit.Material.DIAMOND,
            "Биржа",
            "Торговля акциями|и ценными бумагами",
            (player) -> {
                // Проверяем, разблокирована ли биржевая технология
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "stock_market");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для доступа к бирже требуется изучить технологию 'Фондовый рынок'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие фондовой биржи...");
                plugin.getStockMarketService().openStockMarket(player);
            }
        ));
    }
}