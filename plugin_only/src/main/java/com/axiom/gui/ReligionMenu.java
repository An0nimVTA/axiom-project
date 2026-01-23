package com.axiom.gui;

import com.axiom.AXIOM;
import org.bukkit.entity.Player;

/**
 * GUI-меню религиозной системы с использованием карточек
 */
public class ReligionMenu extends CardBasedMenu {
    private final AXIOM plugin;

    public ReligionMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Религиозная система");
        this.plugin = plugin;
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Моя религия (Material.NETHER_STAR)
        addCard(new Card(
            org.bukkit.Material.NETHER_STAR,
            "Моя вера",
            "Просмотр и изменение|религии",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие информации о вашей религии...");
                player.performCommand("religion me");
            }
        ));

        // Карточка: Создать религию (Material.ENCHANTING_TABLE)
        addCard(new Card(
            org.bukkit.Material.ENCHANTING_TABLE,
            "Создать религию",
            "Сформировать новую|религию",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "religion");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для создания религии требуется изучить 'Религию'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие создания религии...");
                player.performCommand("religion create");
            }
        ));

        // Карточка: Школа (Material.BOOK)
        addCard(new Card(
            org.bukkit.Material.BOOK,
            "Образование",
            "Получить навыки|и знания",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "education");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для образования требуется изучить 'Образование'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие образовательного меню...");
                player.performCommand("education learn");
            }
        ));

        // Карточка: Культура (Material.PAINTING)
        addCard(new Card(
            org.bukkit.Material.PAINTING,
            "Культура",
            "Создание артефактов|и праздников",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие культурного меню...");
                player.performCommand("culture artifacts");
            }
        ));

        // Карточка: Ритуалы (Material.BLAZE_POWDER)
        addCard(new Card(
            org.bukkit.Material.BLAZE_POWDER,
            "Ритуалы",
            "Выполнение религиозных|обрядов",
            (player) -> {
                // Проверяем технологию
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "advanced_religion");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для ритуалов требуется изучить 'Передовую религию'!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню ритуалов...");
                player.performCommand("religion ritual");
            }
        ));

        // Карточка: Святыни (Material.END_CRYSTAL)
        addCard(new Card(
            org.bukkit.Material.END_CRYSTAL,
            "Святыни",
            "Создание и управление|религиозными сооружениями",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню святынь...");
                player.performCommand("religion shrine");
            }
        ));
    }
}