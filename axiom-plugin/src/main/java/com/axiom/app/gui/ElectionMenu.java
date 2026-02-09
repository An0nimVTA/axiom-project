package com.axiom.app.gui;

import com.axiom.AXIOM;
import com.axiom.domain.service.politics.ElectionService;
import com.axiom.domain.service.state.NationManager;
import org.bukkit.entity.Player;

/**
 * GUI-меню выборов с использованием карточек
 */
public class ElectionMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final ElectionService electionService;

    public ElectionMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Система выборов");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
        this.electionService = plugin.getElectionService();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Активные выборы (Material.PAPER)
        addCard(new Card(
            org.bukkit.Material.PAPER,
            "Активные выборы",
            "Просмотр текущих|процессов голосования",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие активных выборов...");
                player.performCommand("election active");
            }
        ));

        // Карточка: Голосование (Material.WRITTEN_BOOK)
        addCard(new Card(
            org.bukkit.Material.WRITTEN_BOOK,
            "Голосование",
            "Принять участие|в активном голосовании",
            (player) -> {
                // Проверяем, есть ли активные выборы
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                close();
                player.performCommand("election vote");
            }
        ));

        // Карточка: Кандидаты (Material.PLAYER_HEAD)
        addCard(new Card(
            org.bukkit.Material.PLAYER_HEAD,
            "Кандидаты",
            "Просмотреть список|кандидатов",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие списка кандидатов...");
                player.performCommand("election candidates");
            }
        ));

        // Карtoчка: Создать выборы (Material.GOLDEN_APPLE)
        addCard(new Card(
            org.bukkit.Material.GOLDEN_APPLE,
            "Создать выборы",
            "Инициировать|новые выборы",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                // Проверяем, является ли игрок лидером или имеет право на создание выборов
                if (!nationOpt.get().getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Только лидер нации может инициировать выборы!");
                    return;
                }
                
                close();
                player.performCommand("election create");
            }
        ));

        // Карточка: Выборы лидера (Material.ENCHANTED_GOLDEN_APPLE)
        addCard(new Card(
            org.bukkit.Material.ENCHANTED_GOLDEN_APPLE,
            "Лидер нации",
            "Выборы высшего|руководителя",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                close();
                player.performCommand("election leader");
            }
        ));

        // Карточка: Парламент (Material.BEACON)
        addCard(new Card(
            org.bukkit.Material.BEACON,
            "Парламент",
            "Выборы представителей|в органы власти",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                close();
                player.performCommand("election parliament");
            }
        ));

        // Карточка: Законы (Material.WRITABLE_BOOK)
        addCard(new Card(
            org.bukkit.Material.WRITABLE_BOOK,
            "Законы",
            "Голосование|за новые законы",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                close();
                player.performCommand("election laws");
            }
        ));

        // Карточка: Референдум (Material.DIAMOND)
        addCard(new Card(
            org.bukkit.Material.DIAMOND,
            "Референдум",
            "Народное голосование|по важным вопросам",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                // Проверяем права
                if (!nationOpt.get().getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Только лидер нации может инициировать референдум!");
                    return;
                }
                
                close();
                player.performCommand("election referendum");
            }
        ));
    }
}