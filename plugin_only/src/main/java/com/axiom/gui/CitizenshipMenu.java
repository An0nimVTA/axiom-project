package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.NationManager;
import org.bukkit.entity.Player;

/**
 * GUI-меню гражданства с использованием карточек
 */
public class CitizenshipMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;

    public CitizenshipMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Гражданство нации");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Мои права (Material.PLAYER_HEAD)
        addCard(new Card(
            org.bukkit.Material.PLAYER_HEAD,
            "Мои права",
            "Просмотреть статус|и права гражданства",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие информации о ваших правах...");
                player.performCommand("citizen rights");
            }
        ));

        // Карточка: Пригласить гражданина (Material.GREEN_BED)
        addCard(new Card(
            org.bukkit.Material.GREEN_BED,
            "Пригласить",
            "Пригласить игрока|в вашу нацию",
            (player) -> {
                // Проверяем, является ли игрок лидером нации или имеет разрешение на приглашение
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                if (!nationOpt.get().getLeader().equals(player.getUniqueId()) &&
                    !nationOpt.get().getOfficerIds().contains(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "У вас нет прав приглашать игроков!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Используйте команду: /nation invite <игрок>");
            }
        ));

        // Карточка: Список граждан (Material.BOOK)
        addCard(new Card(
            org.bukkit.Material.BOOK,
            "Граждане",
            "Просмотреть список|всех граждан нации",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие списка граждан...");
                player.performCommand("nation citizens");
            }
        ));

        // Карточка: Исключить (Material.RED_BED)
        addCard(new Card(
            org.bukkit.Material.RED_BED,
            "Исключить",
            "Исключить гражданина|из нации",
            (player) -> {
                // Проверяем права исключения
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                if (!nationOpt.get().getLeader().equals(player.getUniqueId()) &&
                    !nationOpt.get().getOfficerIds().contains(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "У вас нет прав исключать граждан!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Используйте команду: /nation kick <игрок>");
            }
        ));

        // Карточка: Назначения (Material.GOLDEN_HELMET)
        addCard(new Card(
            org.bukkit.Material.GOLDEN_HELMET,
            "Роли",
            "Назначения и права|граждан нации",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                if (!nationOpt.get().getLeader().equals(player.getUniqueId()) &&
                    !nationOpt.get().getOfficerIds().contains(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "У вас нет прав управлять ролями!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню ролей...");
                player.performCommand("nation roles");
            }
        ));

        // Карточка: Присяга (Material.SHIELD)
        addCard(new Card(
            org.bukkit.Material.SHIELD,
            "Присяга",
            "Дать или отозвать|присягу нации",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню присяги...");
                player.performCommand("citizen oath");
            }
        ));

        // Карточка: Репутация (Material.DIAMOND)
        addCard(new Card(
            org.bukkit.Material.DIAMOND,
            "Репутация",
            "Просмотреть репутацию|граждан",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие репутации граждан...");
                player.performCommand("citizen reputation");
            }
        ));

        // Карточка: Статистика (Material.CHART)
        addCard(new Card(
            org.bukkit.Material.CHART,
            "Статистика",
            "Активность и вклад|граждан",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие статистики граждан...");
                player.performCommand("citizen stats");
            }
        ));
    }
}