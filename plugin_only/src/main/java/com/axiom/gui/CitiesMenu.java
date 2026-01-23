package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.CityGrowthEngine;
import com.axiom.service.NationManager;
import org.bukkit.entity.Player;

/**
 * GUI-меню городской системы с использованием карточек
 */
public class CitiesMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final CityGrowthEngine cityGrowthEngine;

    public CitiesMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Городская система");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
        this.cityGrowthEngine = plugin.getCityGrowthEngine();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Мой город (Material.BRICKS)
        addCard(new Card(
            org.bukkit.Material.BRICKS,
            "Мой город",
            "Управление землёй|налогами, строительством",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие управления вашим городом...");
                player.performCommand("city me");
            }
        ));

        // Карточка: Построить здание (Material.STONE)
        addCard(new Card(
            org.bukkit.Material.STONE,
            "Строительство",
            "Выбрать и построить|здание",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "basic_construction");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для строительства требуется изучить 'Базовое строительство'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню строительства...");
                player.performCommand("city build");
            }
        ));

        // Карточка: Транспорт (Material.MINECART)
        addCard(new Card(
            org.bukkit.Material.MINECART,
            "Транспорт",
            "Управление дорогами|и железными дорогами",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "roads");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для транспорта требуется изучить 'Дороги'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие транспортного меню...");
                player.performCommand("transport road");
            }
        ));

        // Карточка: Энергетика (Material.REDSTONE_BLOCK)
        addCard(new Card(
            org.bukkit.Material.REDSTONE_BLOCK,
            "Энергия",
            "Управление энергоснабжением",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "basic_industry");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для энергетики требуется изучить 'Базовую промышленность'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие энергетического меню...");
                player.performCommand("energy manage");
            }
        ));

        // Карточка: Население (Material.PLAYER_HEAD)
        addCard(new Card(
            org.bukkit.Material.PLAYER_HEAD,
            "Население",
            "Управление населением|города",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню населения...");
                player.performCommand("city population");
            }
        ));

        // Карточка: Инфраструктура (Material.IRON_BLOCK)
        addCard(new Card(
            org.bukkit.Material.IRON_BLOCK,
            "Инфраструктура",
            "Развитие городской|инфраструктуры",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню инфраструктуры...");
                player.performCommand("infrastructure upgrade");
            }
        ));
    }
}