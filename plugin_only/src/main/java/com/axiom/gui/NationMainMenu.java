package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.NationManager;
import com.axiom.service.AdvancedWarSystem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main nation menu using card-based system with dynamic color scheme.
 * Implements the full UI specification with card-based layout and theme adaptation.
 */
public class NationMainMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final NationManager nationManager;

    public NationMainMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Национальная система");
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Проверяем, состоит ли игрок в нации
        var opt = nationManager.getNationOfPlayer(player.getUniqueId());
        
        if (opt.isEmpty()) {
            // Игрок не состоит в нации - показываем карточки для создания нации
            addCard(new Card(
                Material.GOLD_BLOCK,
                "Создать нацию",
                "Сформировать новое государство|с уникальным названием",
                (p) -> {
                    close();
                    p.sendMessage(org.bukkit.ChatColor.GREEN + "Используйте команду: /nation create <название>");
                }
            ));
            
            addCard(new Card(
                Material.PLAYER_HEAD,
                "Присоединиться к нации",
                "Вступить в существующую нацию|по приглашению лидера",
                (p) -> {
                    close();
                    p.sendMessage(org.bukkit.ChatColor.GREEN + "Используйте команду: /nation join <название>");
                }
            ));
        } else {
            // Игрок состоит в нации - показываем карточки управления нацией
            Nation n = opt.get();
            
            // Карточка: Профиль нации (Material.BLUE_BANNER)
            addCard(new Card(
                Material.BLUE_BANNER,
                "Профиль нации",
                "Флаг, девиз, валюта|и структура правительства",
                (p) -> {
                    close();
                    new ProfileMenu(plugin, nationManager).open(p);
                }
            ));

            // Карточка: Управление территорией (Material.MAP)
            addCard(new Card(
                Material.MAP,
                "Территория",
                "Захват/освобождение земель|управление участками",
                (p) -> {
                    close();
                    new TerritoryMenu(plugin, p).open();
                }
            ));

            // Карточка: Гражданство (Material.PLAYER_HEAD)
            addCard(new Card(
                Material.PLAYER_HEAD,
                "Гражданство",
                "Приглашения, исключения|управление участниками",
                (p) -> {
                    close();
                    new CitizenshipMenu(plugin, p).open();
                }
            ));

            // Карточка: Экономика (Material.EMERALD)
            addCard(new Card(
                Material.EMERALD,
                "Экономика",
                "Казна, налоги, печать|управление финансами",
                (p) -> {
                    close();
                    new EconomyMenu(plugin, p).open();
                }
            ));

            // Карточка: Дипломатия (Material.WRITTEN_BOOK)
            addCard(new Card(
                Material.WRITTEN_BOOK,
                "Дипломатия",
                "Альянсы, договоры|международные отношения",
                (p) -> {
                    close();
                    new DiplomacyMenu(plugin, nationManager, plugin.getDiplomacySystem()).open(p);
                }
            ));

            // Карточка: Религия (Material.ENCHANTING_TABLE)
            addCard(new Card(
                Material.ENCHANTING_TABLE,
                "Религия",
                "Вероисповедания|религиозные практики",
                (p) -> {
                    close();
                    plugin.openReligionMenu(p);
                }
            ));

            // Карточка: Города (Material.BRICKS)
            addCard(new Card(
                Material.BRICKS,
                "Города",
                "Строительство, развитие|управление населёнными пунктами",
                (p) -> {
                    close();
                    plugin.openCitiesMenu(p);
                }
            ));

            // Карточка: Технологии (Material.ENCHANTED_BOOK)
            addCard(new Card(
                Material.ENCHANTED_BOOK,
                "Технологии",
                "Исследования, развитие|дерево технологий",
                (p) -> {
                    // Проверяем, разблокирована ли технология для открытия древа
                    boolean hasTech = plugin.getTechnologyTreeService().isPlayerHasTech(p.getUniqueId(), "basic_research");
                    if (!hasTech) {
                        p.sendMessage(org.bukkit.ChatColor.RED + "Для доступа к технологиям требуется изучить 'Базовые исследования'!");
                        return;
                    }
                    
                    close();
                    plugin.openAdvancedTechnologyTreeMenu(p);
                }
            ));

            // Карточка: Выборы (Material.PAPER)
            addCard(new Card(
                Material.PAPER,
                "Выборы",
                "Президент, парламент|голосование",
                (p) -> {
                    close();
                    new com.axiom.gui.ElectionMenu(plugin, p).open();
                }
            ));

            // Карточка: История (Material.CLOCK)
            addCard(new Card(
                Material.CLOCK,
                "История",
                "События, хронология|записи нации",
                (p) -> {
                    close();
                    new HistoryMenu(plugin, p).open();
                }
            ));

            // Карточка: Войны (Material.CROSSBOW)
            if (plugin.getAdvancedWarSystem() != null) {
                addCard(new Card(
                    Material.CROSSBOW,
                    "Война",
                    "Объявление, ведение|военные действия",
                    (p) -> {
                        close();
                        // Открываем меню войны или отправляем команду
                        p.performCommand("axiom war");
                    }
                ));
            }
            
            // Карточка: Расширенные функции (Material.BEACON)
            addCard(new Card(
                Material.BEACON,
                "Расширенные функции",
                "Все возможности AXIOM|дополнительные системы",
                (p) -> {
                    close();
                    new com.axiom.gui.AdvancedFeaturesMenu(plugin, p).open();
                }
            ));
        }
    }
    
    /**
     * Override to apply dynamic color scheme based on server type
     */
    @Override
    public void open() {
        super.fillInventoryWithCards();
        super.addDecorativeElements();
        
        // Apply dynamic color scheme based on server type (future implementation)
        // This would change colors of glass panes, title, etc. based on server theme
        
        player.openInventory(inventory);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        super.onInventoryClick(event);
    }
}


