package com.axiom.app.gui;

import com.axiom.AXIOM;
import org.bukkit.entity.Player;

/**
 * GUI-меню банковской системы с использованием карточек
 */
public class BankingMenu extends CardBasedMenu {
    private final AXIOM plugin;

    public BankingMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Банковская система");
        this.plugin = plugin;
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Открыть счёт (Material.ANVIL)
        addCard(new Card(
            org.bukkit.Material.ANVIL,
            "Открыть счёт",
            "Создать банковский счёт|для депозитов",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие создания банковского счёта...");
                // Открытие процесса открытия счёта
                // Проверяем наличие банковской технологии
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "banking");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для открытия счёта требуется изучить технологию 'Банковское дело'!");
                    return;
                }
                player.performCommand("bank open");
            }
        ));

        // Карточка: Депозит (Material.EMERALD_BLOCK)
        addCard(new Card(
            org.bukkit.Material.EMERALD_BLOCK,
            "Депозит",
            "Внести деньги на счёт|и заработать проценты",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие депозитного меню...");
                player.performCommand("bank deposit");
            }
        ));

        // Карточка: Снятие (Material.REDSTONE_BLOCK)
        addCard(new Card(
            org.bukkit.Material.REDSTONE_BLOCK,
            "Снятие",
            "Снять деньги со счёта|по вашему усмотрению",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню снятия...");
                player.performCommand("bank withdraw");
            }
        ));

        // Карточка: История операций (Material.BOOK)
        addCard(new Card(
            org.bukkit.Material.BOOK,
            "История",
            "Просмотреть все|банковские операции",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие истории операций...");
                player.performCommand("bank history");
            }
        ));

        // Карточка: Кредиты (Material.PAPER)
        addCard(new Card(
            org.bukkit.Material.PAPER,
            "Кредиты",
            "Взять или выдать кредит|с процентами",
            (player) -> {
                // Проверяем наличие кредитной технологии
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "advanced_banking");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для работы с кредитами требуется изучить 'Передовое банковское дело'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню кредитов...");
                // Открытие меню кредитов
                player.performCommand("bank loan");
            }
        ));

        // Карточка: Инвестиции (Material.DIAMOND)
        addCard(new Card(
            org.bukkit.Material.DIAMOND,
            "Инвестиции",
            "Инвестировать средства|для получения дохода",
            (player) -> {
                // Проверяем наличие инвестиционной технологии
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "investment_system");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для инвестирования требуется изучить соответствующую технологию!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие инвестиционного меню...");
                player.performCommand("bank invest");
            }
        ));
    }
}