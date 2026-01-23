package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.NationManager;
import org.bukkit.entity.Player;

/**
 * GUI-меню продвинутых функций с использованием карточек
 */
public class AdvancedFeaturesMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;

    public AdvancedFeaturesMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Расширенные функции");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Войны и конфликты (Material.CROSSBOW)
        addCard(new Card(
            org.bukkit.Material.CROSSBOW,
            "Войны и конфликты",
            "Управление военными|действиями и конфликтами",
            (player) -> {
                // Проверяем технологию военных действий
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "basic_military");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для ведения войны нужно изучить 'Базовое военное дело'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие военного меню...");
                player.performCommand("war status");
            }
        ));

        // Карточка: Банкинг (Material.BEACON)
        addCard(new Card(
            org.bukkit.Material.BEACON,
            "Банковская система",
            "Управление казной|и национальной экономикой",
            (player) -> {
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "banking");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для банковской системы нужно изучить 'Банковское дело'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие банковской системы...");
                plugin.openBankingMenu(player);
            }
        ));

        // Карточка: Образование (Material.EXPERIENCE_BOTTLE)
        addCard(new Card(
            org.bukkit.Material.EXPERIENCE_BOTTLE,
            "Образование",
            "Система обучения|и развития навыков",
            (player) -> {
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "education");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для образования нужно изучить 'Образование'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие образовательной системы...");
                player.performCommand("education school");
            }
        ));

        // Карточка: Инфраструктура (Material.IRON_BLOCK)
        addCard(new Card(
            org.bukkit.Material.IRON_BLOCK,
            "Инфраструктура",
            "Управление дорогами|и общественными зданиями",
            (player) -> {
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "basic_construction");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для инфраструктуры нужно изучить 'Базовое строительство'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие инфраструктурной системы...");
                player.performCommand("infrastructure build");
            }
        ));

        // Карточка: Экология (Material.LEAVES)
        addCard(new Card(
            org.bukkit.Material.LEAVES,
            "Экология",
            "Управление природными|ресурсами и средой",
            (player) -> {
                // Проверяем, состоит ли игрок в нации
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации для управления экологией!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие экологической системы...");
                player.performCommand("environment protect");
            }
        ));

        // Карточка: Культура (Material.PAINTING)
        addCard(new Card(
            org.bukkit.Material.PAINTING,
            "Культура",
            "Создание артефактов|и культурных мероприятий",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие культурной системы...");
                player.performCommand("culture festival");
            }
        ));

        // Карточка: Научные исследования (Material.ENCHANTING_TABLE)
        addCard(new Card(
            org.bukkit.Material.ENCHANTING_TABLE,
            "Наука",
            "Исследования и изобретения",
            (player) -> {
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "basic_research");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для науки нужно изучить 'Базовые исследования'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие научной системы...");
                player.performCommand("science lab");
            }
        ));

        // Карточка: Шпионаж (Material.POTION)
        addCard(new Card(
            org.bukkit.Material.POTION,
            "Шпионаж",
            "Сбор разведданных|и диверсии",
            (player) -> {
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "espionage_tech");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для шпионажа нужна соответствующая технология!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие шпионской системы...");
                player.performCommand("spy mission");
            }
        ));
    }
}