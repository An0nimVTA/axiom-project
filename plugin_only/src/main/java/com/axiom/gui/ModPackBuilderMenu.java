package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.ModPackBuilderService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI меню для создания и настройки модпаков
 * Использует карточную систему для управления модами
 */
public class ModPackBuilderMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final ModPackBuilderService modPackService;
    private ModPackBuilderService.ModPack currentModPack;
    private int currentPage = 0;
    private final int ITEMS_PER_PAGE = 21; // Количество слотов под моды на странице
    
    public ModPackBuilderMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Создание модпака");
        this.plugin = plugin;
        this.modPackService = plugin.getModPackBuilderService();
    }

    @Override
    protected void fillInventoryWithCards() {
        // Основные действия
        addCard(createNewModPackCard());
        addCard(loadExistingModPackCard());
        addCard(saveCurrentModPackCard());
        
        // Если есть активный модпак, добавляем управление модами
        if (currentModPack != null) {
            addModManagementCards();
        } else {
            // Если нет активного модпака, показываем список доступных
            addAvailableModPacksCards();
        }
        
        // Навигационные элементы
        addNavigationCards();
    }
    
    private CardBasedMenu.Card createNewModPackCard() {
        return new CardBasedMenu.Card(
            Material.ANVIL,
            "Создать модпак",
            "Начать создание|нового модпака",
            (player) -> {
                close();
                player.sendMessage(ChatColor.GREEN + "Введите команду: /modpack create <id> <название>");
            }
        );
    }
    
    private CardBasedMenu.Card loadExistingModPackCard() {
        return new CardBasedMenu.Card(
            Material.CHEST,
            "Загрузить модпак",
            "Выбрать существующий|модпак для редактирования",
            (player) -> {
                // Открываем меню выбора модпака
                openModPackSelectionMenu();
            }
        );
    }
    
    private CardBasedMenu.Card saveCurrentModPackCard() {
        return new CardBasedMenu.Card(
            Material.WRITABLE_BOOK,
            "Сохранить",
            "Сохранить текущий|модпак",
            (player) -> {
                if (currentModPack != null) {
                    // Сохраняем модпак
                    player.sendMessage(ChatColor.GREEN + "Модпак '" + currentModPack.getName() + "' сохранен!");
                } else {
                    player.sendMessage(ChatColor.RED + "Нет активного модпака для сохранения!");
                }
            }
        );
    }
    
    private void addModManagementCards() {
        // Информация о текущем модпаке
        addCard(createModPackInfoCard());
        
        // Управление модами
        addCard(createAddModCard());
        addCard(createRemoveModCard());
        addCard(createConfigureModCard());
        
        // Управление сложными модами (страницы)
        addModPageCards();
    }
    
    private CardBasedMenu.Card createModPackInfoCard() {
        return new CardBasedMenu.Card(
            Material.BEACON,
            "Инфо: " + currentModPack.getName(),
            "Тип: " + currentModPack.getServerType() + "|Версия: " + currentModPack.getVersion() + "|Создатель: " + currentModPack.getCreatedBy(),
            (player) -> {
                // Показываем подробную информацию
                player.sendMessage(ChatColor.GOLD + "=== Информация о модпаке ===");
                player.sendMessage(ChatColor.YELLOW + "Название: " + ChatColor.WHITE + currentModPack.getName());
                player.sendMessage(ChatColor.YELLOW + "Описание: " + ChatColor.WHITE + currentModPack.getDescription());
                player.sendMessage(ChatColor.YELLOW + "Тип сервера: " + ChatColor.WHITE + currentModPack.getServerType());
                player.sendMessage(ChatColor.YELLOW + "Версия: " + ChatColor.WHITE + currentModPack.getVersion());
                player.sendMessage(ChatColor.YELLOW + "Создан: " + ChatColor.WHITE + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(currentModPack.getCreatedAt())));
                player.sendMessage(ChatColor.YELLOW + "Количество модов: " + ChatColor.WHITE + currentModPack.getModConfig().size());
            }
        );
    }
    
    private CardBasedMenu.Card createAddModCard() {
        return new CardBasedMenu.Card(
            Material.GREEN_WOOL,
            "Добавить мод",
            "Выбрать и добавить|новый мод в модпак",
            (player) -> {
                // Открываем меню выбора мода
                openModSelectionMenu();
            }
        );
    }
    
    private CardBasedMenu.Card createRemoveModCard() {
        return new CardBasedMenu.Card(
            Material.RED_WOOL,
            "Удалить мод",
            "Удалить мод|из модпака",
            (player) -> {
                // Открываем меню удаления мода
                openModRemovalMenu();
            }
        );
    }
    
    private CardBasedMenu.Card createConfigureModCard() {
        return new CardBasedMenu.Card(
            Material.COMPARATOR,
            "Настроить моды",
            "Включить/выключить|моды в модпаке",
            (player) -> {
                // Переходим к настройке модов
                openModConfigurationMenu();
            }
        );
    }
    
    private void addModPageCards() {
        List<String> enabledMods = new ArrayList<>();
        List<String> disabledMods = new ArrayList<>();
        
        for (String mod : currentModPack.getModConfig().keySet()) {
            if (currentModPack.isModEnabled(mod)) {
                enabledMods.add(mod);
            } else {
                disabledMods.add(mod);
            }
        }
        
        // Показываем включенные моды (до определенного лимита на страницу)
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, enabledMods.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String modName = enabledMods.get(i);
            addCard(createModCard(modName, true));
        }
    }
    
    private CardBasedMenu.Card createModCard(String modName, boolean isEnabled) {
        Material icon = isEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
        String status = isEnabled ? "ВКЛЮЧЕН" : "ВЫКЛЮЧЕН";
        ChatColor statusColor = isEnabled ? ChatColor.GREEN : ChatColor.RED;
        
        return new CardBasedMenu.Card(
            icon,
            modName,
            statusColor + status + "|Нажмите для изменения",
            (player) -> {
                // Переключаем состояние мода
                currentModPack.setModEnabled(modName, !isEnabled);
                
                player.sendMessage(statusColor + "Мод " + ChatColor.WHITE + modName + statusColor + " теперь " + (!isEnabled ? "включен" : "выключен") + "!");
                
                // Обновляем меню
                updateInventory();
            }
        );
    }
    
    private void addAvailableModPacksCards() {
        // Показываем доступные модпаки для выбора
        List<ModPackBuilderService.ModPack> availableModPacks = modPackService.getAllModPacks();
        
        for (ModPackBuilderService.ModPack modPack : availableModPacks) {
            addCard(createModPackSelectionCard(modPack));
        }
    }
    
    private CardBasedMenu.Card createModPackSelectionCard(ModPackBuilderService.ModPack modPack) {
        Material icon;
        switch (modPack.getServerType()) {
            case "modern":
                icon = Material.BLUE_STAINED_GLASS;
                break;
            case "medieval":
                icon = Material.BROWN_STAINED_GLASS;
                break;
            case "magic":
                icon = Material.PURPLE_STAINED_GLASS;
                break;
            case "minigames":
                icon = Material.ORANGE_STAINED_GLASS;
                break;
            default:
                icon = Material.LIGHT_BLUE_STAINED_GLASS;
        }
        
        return new CardBasedMenu.Card(
            icon,
            modPack.getName(),
            modPack.getDescription() + "|" + ChatColor.GRAY + "Тип: " + modPack.getServerType(),
            (player) -> {
                // Загружаем выбранный модпак
                currentModPack = modPack;
                player.sendMessage(ChatColor.GREEN + "Загружен модпак: " + modPack.getName());
                
                // Обновляем меню
                updateInventory();
            }
        );
    }
    
    private void addNavigationCards() {
        // Кнопка "Назад" если есть текущий модпак
        if (currentModPack != null) {
            addCard(createBackToMainMenuCard());
        }
        
        // Кнопка "Назад к списку модпаков"
        addCard(createBackToListCard());
        
        // Кнопка "Собрать модпак"
        if (currentModPack != null) {
            addCard(createBuildModPackCard());
        }
    }
    
    private CardBasedMenu.Card createBackToMainMenuCard() {
        return new CardBasedMenu.Card(
            Material.REDSTONE,
            "Назад к главному",
            "Вернуться к|основному меню",
            (player) -> {
                currentModPack = null;
                updateInventory();
            }
        );
    }
    
    private CardBasedMenu.Card createBackToListCard() {
        return new CardBasedMenu.Card(
            Material.ARROW,
            "К списку модпаков",
            "Просмотреть список|доступных модпаков",
            (player) -> {
                if (currentModPack == null) {
                    // Обновляем меню для отображения списка модпаков
                    updateInventory();
                } else {
                    // Сохраняем текущий и возвращаемся к списку
                    currentModPack = null;
                    updateInventory();
                }
            }
        );
    }
    
    private CardBasedMenu.Card createBuildModPackCard() {
        return new CardBasedMenu.Card(
            Material.CRAFTING_TABLE,
            "Собрать модпак",
            "Создать ZIP-архив|с выбранными модами",
            (player) -> {
                // Запускаем процесс сборки модпака
                boolean success = modPackService.buildModPack(currentModPack.getId(), player);
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "Модпак '" + currentModPack.getName() + "' успешно собран!");
                    player.sendMessage(ChatColor.GOLD + "Скачать можно по команде: /modpack download " + currentModPack.getId());
                } else {
                    player.sendMessage(ChatColor.RED + "Ошибка при сборке модпака!");
                }
            }
        );
    }
    
    /**
     * Открытие меню выбора мода
     */
    private void openModSelectionMenu() {
        // Создаем временное меню для выбора мода
        List<String> availableMods = modPackService.getAvailableMods();
        
        // В реальной реализации это будет отдельное GUI меню
        player.sendMessage(ChatColor.GOLD + "=== Доступные моды ===");
        for (String mod : availableMods) {
            player.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + mod);
        }
        player.sendMessage(ChatColor.GREEN + "Используйте команду: /modpack enable <название_мода> для добавления");
    }
    
    /**
     * Открытие меню удаления мода
     */
    private void openModRemovalMenu() {
        if (currentModPack == null) {
            player.sendMessage(ChatColor.RED + "Нет активного модпака!");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Моды в текущем модпаке ===");
        for (String mod : currentModPack.getModConfig().keySet()) {
            ChatColor color = currentModPack.isModEnabled(mod) ? ChatColor.GREEN : ChatColor.RED;
            player.sendMessage(color + "- " + ChatColor.WHITE + mod);
        }
        player.sendMessage(ChatColor.GREEN + "Используйте команду: /modpack disable <название_мода> для удаления");
    }
    
    /**
     * Открытие меню настройки модов
     */
    private void openModConfigurationMenu() {
        if (currentModPack == null) {
            player.sendMessage(ChatColor.RED + "Нет активного модпака!");
            return;
        }
        
        // Обновляем меню для показа настройки модов
        updateInventory();
    }
    
    /**
     * Открытие меню выбора модпака
     */
    private void openModPackSelectionMenu() {
        // Обновляем меню для отображения списка модпаков
        currentModPack = null;
        updateInventory();
    }
}