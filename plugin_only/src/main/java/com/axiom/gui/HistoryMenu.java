package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.NationManager;
import org.bukkit.entity.Player;

/**
 * GUI-меню истории с использованием карточек
 */
public class HistoryMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;

    public HistoryMenu(AXIOM plugin, Player player) {
        super(plugin, player, "История нации");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Хронология событий (Material.CLOCK)
        addCard(new Card(
            org.bukkit.Material.CLOCK,
            "Хронология",
            "Полная история|нации по датам",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие хронологии событий...");
                player.performCommand("history timeline");
            }
        ));

        // Карточка: Войны (Material.SHIELD)
        addCard(new Card(
            org.bukkit.Material.SHIELD,
            "Войны",
            "Записи о войнах|и конфликтах",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие истории войн...");
                player.performCommand("history wars");
            }
        ));

        // Карточка: Договоры (Material.WRITTEN_BOOK)
        addCard(new Card(
            org.bukkit.Material.WRITTEN_BOOK,
            "Договоры",
            "История дипломатических|соглашений",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие истории договоров...");
                player.performCommand("history treaties");
            }
        ));

        // Карточка: Выборы (Material.PAPER)
        addCard(new Card(
            org.bukkit.Material.PAPER,
            "Выборы",
            "Записи о выборах|и референдумах",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие истории выборов...");
                player.performCommand("history elections");
            }
        ));

        // Карточка: Экономические события (Material.EMERALD)
        addCard(new Card(
            org.bukkit.Material.EMERALD,
            "Экономика",
            "Финансовые кризисы|и рост",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие экономической истории...");
                player.performCommand("history economics");
            }
        ));

        // Карточка: Культурные события (Material.MUSIC_DISC)
        addCard(new Card(
            org.bukkit.Material.MUSIC_DISC,
            "Культура",
            "Праздники и|культурные события",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие культурной истории...");
                player.performCommand("history culture");
            }
        ));

        // Карточка: Природные катаклизмы (Material.WATER_BUCKET)
        addCard(new Card(
            org.bukkit.Material.WATER_BUCKET,
            "Катаклизмы",
            "Землетрясения,|наводнения и т.д.",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие истории катаклизмов...");
                player.performCommand("history disasters");
            }
        ));

        // Карточка: Запись событий (Material.BOOK_AND_QUILL)
        addCard(new Card(
            org.bukkit.Material.BOOK_AND_QUILL,
            "Запись события",
            "Добавить новую|историческую запись",
            (player) -> {
                // Проверяем, является ли игрок лидером нации
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                if (!nationOpt.get().getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Только лидер нации может добавлять записи в историю!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Используйте команду: /history add <событие>");
            }
        ));
    }
}