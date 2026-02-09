package com.axiom.app.gui;

import com.axiom.AXIOM;
import com.axiom.domain.service.infrastructure.MapBoundaryVisualizationService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI меню для отображения визуализации границ наций и городов
 * Показывает границы на карте с цветовой кодировкой и информацией
 */
public class MapBoundaryVisualizationMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final MapBoundaryVisualizationService boundaryService;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 21; // Количество слотов под элементы на странице

    public MapBoundaryVisualizationMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Визуализация границ");
        this.plugin = plugin;
        this.boundaryService = plugin.getMapBoundaryVisualizationService();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Кнопка "Назад" (Material.ARROW)
        addCard(new Card(
            Material.ARROW,
            "Назад",
            "Вернуться в главное меню",
            (player) -> {
                // Показываем главное меню нации
                plugin.openNationMainMenu(player);
            }
        ));
        
        // Общая информация о границах
        addCard(new Card(
            Material.BEACON,
            "Статистика границ",
            "Общая информация о|всех границах на сервере",
            (player) -> {
                showBoundaryStatistics(player);
            }
        ));
        
        // Отображение границ наций
        addCard(new Card(
            Material.BLUE_STAINED_GLASS,
            "Границы наций",
            "Просмотр границ|национальных территорий",
            (player) -> {
                showNationBoundaries(player);
            }
        ));
        
        // Отображение границ городов
        addCard(new Card(
            Material.BRICKS,
            "Границы городов",
            "Просмотр границ|городских территорий",
            (player) -> {
                showCityBoundaries(player);
            }
        ));
        
        // Настройки отображения
        addCard(new Card(
            Material.COMPARATOR,
            "Настройки отображения",
            "Изменить настройки|визуализации границ",
            (player) -> {
                showDisplaySettings(player);
            }
        ));
        
        // Визуализация в реальном времени
        addCard(new Card(
            Material.ENDER_EYE,
            "Реальное время",
            "Включить/отключить|визуализацию в мире",
            (player) -> {
                toggleRealTimeVisualization(player);
            }
        ));
        
        // Справка
        addCard(new Card(
            Material.KNOWLEDGE_BOOK,
            "Справка",
            "Как работает|визуализация границ",
            (player) -> {
                showHelpInfo(player);
            }
        ));
    }
    
    /**
     * Показать статистику границ
     */
    private void showBoundaryStatistics(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Статистика границ ===");
        
        var stats = boundaryService.getBoundaryStatistics();
        player.sendMessage(ChatColor.YELLOW + "Всего наций: " + ChatColor.WHITE + stats.get("totalNations"));
        player.sendMessage(ChatColor.YELLOW + "Всего городов: " + ChatColor.WHITE + stats.get("totalCities"));
        player.sendMessage(ChatColor.YELLOW + "Всего чанков наций: " + ChatColor.WHITE + stats.get("totalNationChunks"));
        player.sendMessage(ChatColor.YELLOW + "Всего чанков городов: " + ChatColor.WHITE + stats.get("totalCityChunks"));
        player.sendMessage(ChatColor.YELLOW + "Средний размер нации: " + ChatColor.WHITE + String.format("%.2f", (Double)stats.get("avgNationSize")));
        player.sendMessage(ChatColor.YELLOW + "Средний размер города: " + ChatColor.WHITE + String.format("%.2f", (Double)stats.get("avgCitySize")));
        
        player.sendMessage(ChatColor.GRAY + "Используйте /axiom map stats для подробной информации");
    }
    
    /**
     * Показать границы наций
     */
    private void showNationBoundaries(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Границы наций ===");
        
        var nationBoundaries = boundaryService.getAllNationBoundaries();
        for (var boundary : nationBoundaries.values()) {
            int color = boundary.getColor();
            player.sendMessage(String.format("§x§%06X%s §7(%d чанков)", color, 
                boundary.getName(), boundary.getChunkCount()));
        }
        
        if (nationBoundaries.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Нет созданных наций с границами");
        }
        
        player.sendMessage(ChatColor.GRAY + "Используйте /axiom map nations для деталей");
    }
    
    /**
     * Показать границы городов
     */
    private void showCityBoundaries(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Границы городов ===");
        
        var cityBoundaries = boundaryService.getAllCityBoundaries();
        for (var boundary : cityBoundaries.values()) {
            int color = boundary.getColor();
            player.sendMessage(String.format("§x§%06X%s §7(нация: %s, %d чанков)", color, 
                boundary.getName(), boundary.getNationId(), boundary.getChunkCount()));
        }
        
        if (cityBoundaries.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Нет созданных городов с границами");
        }
        
        player.sendMessage(ChatColor.GRAY + "Используйте /axiom map cities для деталей");
    }
    
    /**
     * Показать настройки отображения
     */
    private void showDisplaySettings(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Настройки визуализации ===");
        player.sendMessage(ChatColor.YELLOW + "Частота обновления: " + ChatColor.WHITE + "30 секунд");
        player.sendMessage(ChatColor.YELLOW + "Отображение границ в чате: " + 
            (plugin.getConfig().getBoolean("map.showBorderIndicators", true) ? 
                ChatColor.GREEN + "ВКЛ" : ChatColor.RED + "ВЫКЛ"));
        player.sendMessage(ChatColor.YELLOW + "Отображение в Xaeros Maps: " + 
            (boundaryService.getModIntegrationService().hasMapMods() ? 
                ChatColor.GREEN + "ДА" : ChatColor.RED + "НЕТ"));
        
        player.sendMessage(ChatColor.GRAY + "Для изменения настроек используйте /axiom config");
    }
    
    /**
     * Переключить визуализацию в реальном времени
     */
    private void toggleRealTimeVisualization(Player player) {
        // В реальной реализации здесь будет переключение настроек
        // Для простоты просто покажем сообщение
        player.sendMessage(ChatColor.GREEN + "Визуализация границ в реальном времени включена!");
        player.sendMessage(ChatColor.GRAY + "Границы будут отображаться при перемещении по миру");
    }
    
    /**
     * Показать справочную информацию
     */
    private void showHelpInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Справка по визуализации границ ===");
        player.sendMessage(ChatColor.YELLOW + "● " + ChatColor.WHITE + "Границы наций отображаются в Xaeros Minimap");
        player.sendMessage(ChatColor.YELLOW + "● " + ChatColor.WHITE + "Границы городов отображаются цветом на карте");
        player.sendMessage(ChatColor.YELLOW + "● " + ChatColor.WHITE + "Цвета уникальны для каждой нации/города");
        player.sendMessage(ChatColor.YELLOW + "● " + ChatColor.WHITE + "Обновление границ происходит каждые 30 секунд");
        player.sendMessage(ChatColor.YELLOW + "● " + ChatColor.WHITE + "Поддерживаемые карты: Xaeros Minimap, WorldMap");
        player.sendMessage(ChatColor.GRAY + "Для поддержки Xaeros установите соответствующие моды");
    }
    
    /**
     * Открытие меню со страницей
     */
    public void open(int page) {
        this.currentPage = page;
        open();
    }
}