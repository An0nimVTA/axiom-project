package com.axiom.gui;

import com.axiom.AXIOM;
import org.bukkit.entity.Player;

/**
 * GUI-меню профиля игрока с использованием карточек
 */
public class ProfileMenu extends CardBasedMenu {
    private final AXIOM plugin;

    public ProfileMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Профиль игрока");
        this.plugin = plugin;
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Профиль (Material.PLAYER_HEAD)
        addCard(new Card(
            org.bukkit.Material.PLAYER_HEAD,
            "Профиль",
            "Информация об игроке|репутация",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие вашего профиля...");
                player.performCommand("profile me");
            }
        ));

        // Карточка: Статистика (Material.CHART)
        addCard(new Card(
            org.bukkit.Material.CHART,
            "Статистика",
            "Все действия|прогресс, достижения",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие вашей статистики...");
                player.performCommand("stats me");
            }
        ));

        // Карточка: История (Material.BOOK)
        addCard(new Card(
            org.bukkit.Material.BOOK,
            "История",
            "Журнал действий",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие истории ваших действий...");
                player.performCommand("history me");
            }
        ));

        // Карточка: Настройки (Material.REDSTONE_TORCH)
        addCard(new Card(
            org.bukkit.Material.REDSTONE_TORCH,
            "Настройки",
            "Персональные настройки",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие персональных настроек...");
                player.performCommand("settings player");
            }
        ));

        // Карточка: Достижения (Material.NETHER_STAR)
        addCard(new Card(
            org.bukkit.Material.NETHER_STAR,
            "Достижения",
            "Просмотр ваших|достижений",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню достижений...");
                player.performCommand("achievement list");
            }
        ));

        // Карточка: Репутация (Material.DIAMOND)
        addCard(new Card(
            org.bukkit.Material.DIAMOND,
            "Репутация",
            "Просмотр и изменение|репутации",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню репутации...");
                player.performCommand("reputation me");
            }
        ));
    }
}