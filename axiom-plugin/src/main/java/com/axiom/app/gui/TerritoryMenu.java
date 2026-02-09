package com.axiom.app.gui;

import com.axiom.AXIOM;
import com.axiom.domain.service.state.NationManager;
import org.bukkit.entity.Player;

/**
 * GUI-меню управления территорией с использованием карточек
 */
public class TerritoryMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;

    public TerritoryMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Управление территорией");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Карточка: Захват территории (Material.GOLDEN_SHOVEL)
        addCard(new Card(
            org.bukkit.Material.GOLDEN_SHOVEL,
            "Захват",
            "Заявить права|на текущий чанк",
            (player) -> {
                // Проверяем, состоит ли игрок в нации
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации для захвата территории!");
                    return;
                }
                
                // Проверяем, разблокирована ли технология захвата
                boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), "territory_claim");
                if (!hasTech) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Для захвата территории необходима технология 'Захват территории'!");
                    return;
                }
                
                close();
                player.performCommand("claim");
            }
        ));

        // Карточка: Освободить территорию (Material.WOODEN_SHOVEL)
        addCard(new Card(
            org.bukkit.Material.WOODEN_SHOVEL,
            "Освободить",
            "Освободить захваченный|чанк",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                close();
                player.performCommand("unclaim");
            }
        ));

        // Карточка: Карта территории (Material.MAP)
        addCard(new Card(
            org.bukkit.Material.MAP,
            "Карта",
            "Просмотреть карту|владений нации",
            (player) -> {
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие карты территории...");
                player.performCommand("nation map");
            }
        ));

        // Карточка: Защита территории (Material.SHIELD)
        addCard(new Card(
            org.bukkit.Material.SHIELD,
            "Защита",
            "Настройка защиты|владений",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие настроек защиты...");
                player.performCommand("territory protection");
            }
        ));

        // Карточка: Условия владения (Material.NAME_TAG)
        addCard(new Card(
            org.bukkit.Material.NAME_TAG,
            "Условия",
            "Настроить права|и доступ к земле",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                if (!nationOpt.get().getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Только лидер нации может изменять условия владения!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие настроек условий владения...");
                player.performCommand("territory rules");
            }
        ));

        // Карточка: Налоги с территории (Material.EMERALD)
        addCard(new Card(
            org.bukkit.Material.EMERALD,
            "Налоги",
            "Установка налогов|на владения",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                if (!nationOpt.get().getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Только лидер нации может управлять налогами!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие настроек налогов...");
                player.performCommand("territory taxes");
            }
        ));

        // Карточка: Строительство (Material.BRICKS)
        addCard(new Card(
            org.bukkit.Material.BRICKS,
            "Строительство",
            "Разрешения|на строительство",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие настроек строительства...");
                player.performCommand("territory build");
            }
        ));

        // Карточка: Охраняемые зоны (Material.EMERALD_ORE)
        addCard(new Card(
            org.bukkit.Material.EMERALD_ORE,
            "Зоны",
            "Создание охраняемых|и специальных зон",
            (player) -> {
                var nationOpt = nationManager.getNationOfPlayer(player.getUniqueId());
                if (nationOpt.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Вы должны состоять в нации!");
                    return;
                }
                
                close();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Открытие меню зон...");
                player.performCommand("territory zones");
            }
        ));
    }
}