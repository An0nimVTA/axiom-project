package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.DiplomacySystem;
import com.axiom.service.NationManager;
import org.bukkit.entity.Player;

/**
 * GUI-меню дипломатической системы с использованием карточек
 */
public class DiplomacyMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final DiplomacySystem diplomacySystem;

    public DiplomacyMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Дипломатическая система");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
        this.diplomacySystem = plugin.getDiplomacySystem();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Договоры (Material.WRITABLE_BOOK)
        addCard(new Card(
            org.bukkit.Material.WRITABLE_BOOK,
            "Договоры",
            "Заключить/разорвать мир|торговые соглашения",
            (player) -> {
                // Проверяем, состоит ли игрок в нации и является ли лидером
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации для дипломатии!");
                    return;
                }
                
                if (!nationOpt.get().getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Только лидер нации может вести дипломатию!");
                    return;
                }
                
                // Проверяем, разблокирована ли технология дипломатии
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "diplomacy");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для дипломатии требуется изучить технологию 'Дипломатия'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню договоров...");
                player.performCommand("treaty list");
            }
        ));

        // Карточка: Союзы (Material.DIAMOND)
        addCard(new Card(
            org.bukkit.Material.DIAMOND,
            "Союзы",
            "Создать или вступить|в альянс наций",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "advanced_diplomacy");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для союзов требуется изучить 'Передовую дипломатию'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню союзов...");
                player.performCommand("ally list");
            }
        ));

        // Карточка: Война (Material.IRON_SWORD)
        addCard(new Card(
            org.bukkit.Material.IRON_SWORD,
            "Война",
            "Объявить войну|другой нации",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "basic_military");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для войны требуется изучить 'Базовое военное дело'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие военного меню...");
                player.performCommand("war declare");
            }
        ));

        // Карточка: Шпионаж (Material.POTION)
        addCard(new Card(
            org.bukkit.Material.POTION,
            "Шпионаж",
            "Сбор информации|о других нациях",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "espionage_tech");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для шпионажа требуется соответствующая технология!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие шпионского меню...");
                player.performCommand("spy intel");
            }
        ));

        // Карточка: Дипломатические отношения (Material.ENDER_PEARL)
        addCard(new Card(
            org.bukkit.Material.ENDER_PEARL,
            "Отношения",
            "Просмотреть статус|дипломатии с нациями",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню дипломатических отношений...");
                player.performCommand("diplomacy status");
            }
        ));

        // Карточка: Посольства (Material.BEACON)
        addCard(new Card(
            org.bukkit.Material.BEACON,
            "Посольства",
            "Установить постоянные|дипломатические миссии",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "advanced_diplomacy");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для посольств требуется изучить 'Передовую дипломатию'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню посольств...");
                player.performCommand("diplomacy embassy");
            }
        ));
    }
}