package com.axiom.app.gui;

import com.axiom.AXIOM;
import com.axiom.domain.service.infrastructure.ModPackBuilderService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI меню для конструктора модпаков
 * Позволяет создавать, настраивать и управлять модпаками
 */
public class ModPackBuilderMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final ModPackBuilderService modPackService;
    private ModPackBuilderService.ModPack currentModPack = null;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 21;
    
    public ModPackBuilderMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Конструктор модпака");
        this.plugin = plugin;
        this.modPackService = plugin.getModPackBuilderService();
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
        
        // Общая информация
        addCard(new Card(
            Material.BEACON,
            "Создать модпак",
            "Создать новый|кастомный модпак",
            (player) -> {
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Используйте команду: /modpack create <id> <название>");
            }
        ));
        
        // Список модпаков
        addCard(new Card(
            Material.CHEST,
            "Мои модпаки",
            "Просмотр и выбор|моих модпаков",
            (player) -> {
                showPlayerModPacks(player);
            }
        ));
        
        // Управление модами
        addCard(new Card(
            Material.ENCHANTED_BOOK,
            "Управление модами",
            "Включить/выключить|моды в модпаке",
            (player) -> {
                if (currentModPack == null) {
                    player.sendMessage(ChatColor.RED + "Сначала выберите модпак!");
                    return;
                }
                showManageMods(player);
            }
        ));
        
        // Совместимость модов
        addCard(new Card(
            Material.REPEATER,
            "Совместимость",
            "Проверка совместимости|модов с технологиями",
            (player) -> {
                showModCompatibility(player);
            }
        ));
        
        // Настройки отображения
        addCard(new Card(
            Material.REDSTONE_TORCH,
            "Настройки",
            "Изменить настройки|отображения модов",
            (player) -> {
                showDisplaySettings(player);
            }
        ));
        
        // Информация о текущем модпаке
        if (currentModPack != null) {
            addCard(new Card(
                Material.BOOK,
                "Инфо: " + currentModPack.getName(),
                "ID: " + currentModPack.getId() + "|Тип сервера: " + currentModPack.getServerType() + "|Версия: " + currentModPack.getVersion(),
                (player) -> {
                    showCurrentModPackInfo(player);
                }
            ));
            
            // Собрать модпак
            addCard(new Card(
                Material.CRAFTING_TABLE,
                "Собрать модпак",
                "Создать ZIP-архив|с выбранными модами",
                (player) -> {
                    if (currentModPack == null) {
                        player.sendMessage(ChatColor.RED + "Нет выбранного модпака!");
                        return;
                    }
                    
                    boolean success = modPackService.buildModPack(currentModPack.getId(), player);
                    if (success) {
                        player.sendMessage(ChatColor.GREEN + "Модпак '" + currentModPack.getName() + "' собирается...");
                        player.sendMessage(ChatColor.GOLD + "Используйте: /modpack download " + currentModPack.getId() + " для скачивания");
                    } else {
                        player.sendMessage(ChatColor.RED + "Ошибка при сборке модпака!");
                    }
                }
            ));
        }
        
        // Справка
        addCard(new Card(
            Material.KNOWLEDGE_BOOK,
            "Справка",
            "Описание функций|конструктора модпаков",
            (player) -> {
                showHelpInfo(player);
            }
        ));
    }
    
    /**
     * Показать список модпаков игрока
     */
    private void showPlayerModPacks(Player player) {
        List<ModPackBuilderService.ModPack> modPacks = new ArrayList<>();
        
        // Ищем модпаки, созданные игроком
        for (ModPackBuilderService.ModPack pack : modPackService.getAllModPacks()) {
            if (pack.getCreatedBy().equals(player.getName()) || 
                player.hasPermission("axiom.admin")) {
                modPacks.add(pack);
            }
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Мои модпаки ===");
        if (modPacks.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Нет созданных модпаков");
        } else {
            for (int i = 0; i < modPacks.size(); i++) {
                ModPackBuilderService.ModPack pack = modPacks.get(i);
                player.sendMessage(String.format("%d. §b%s §7(ID: %s, %s)", 
                    i + 1, pack.getName(), pack.getId(), pack.getServerType()));
            }
        }
    }
    
    /**
     * Показать управление модами текущего модпака
     */
    private void showManageMods(Player player) {
        if (currentModPack == null) {
            player.sendMessage(ChatColor.RED + "Нет выбранного модпака!");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Моды в '" + currentModPack.getName() + "' ===");
        
        // Показываем включенные моды
        player.sendMessage(ChatColor.GREEN + "Включенные моды:");
        int enabledCount = 0;
        for (String modName : currentModPack.getRequiredMods()) {
            if (currentModPack.isModEnabled(modName)) {
                enabledCount++;
                player.sendMessage("  §a● " + modName);
            }
        }
        
        for (String modName : currentModPack.getOptionalMods()) {
            if (currentModPack.isModEnabled(modName)) {
                enabledCount++;
                player.sendMessage("  §a● " + modName);
            }
        }
        
        // Показываем выключенные моды
        player.sendMessage(ChatColor.RED + "Выключенные моды:");
        int disabledCount = 0;
        for (String modName : currentModPack.getRequiredMods()) {
            if (!currentModPack.isModEnabled(modName)) {
                disabledCount++;
                player.sendMessage("  §c○ " + modName);
            }
        }
        
        for (String modName : currentModPack.getOptionalMods()) {
            if (!currentModPack.isModEnabled(modName)) {
                disabledCount++;
                player.sendMessage("  §c○ " + modName);
            }
        }
        
        player.sendMessage(ChatColor.YELLOW + String.format("Всего модов: %d (вкл: %d, выкл: %d)", 
            enabledCount + disabledCount, enabledCount, disabledCount));
    }
    
    /**
     * Показать совместимость модов
     */
    private void showModCompatibility(Player player) {
        if (currentModPack == null) {
            player.sendMessage(ChatColor.RED + "Нет выбранного модпака!");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Совместимость модов ===");
        
        // Проверяем совместимость каждого мода с изученными технологиями игрока
        var nationOpt = plugin.getNationManager().getNationOfPlayer(player.getUniqueId());
        if (!nationOpt.isPresent()) {
            player.sendMessage(ChatColor.RED + "Вы должны состоять в нации для проверки совместимости!");
            return;
        }
        
        String nationId = nationOpt.get().getId();
        
        for (String modName : currentModPack.getRequiredMods()) {
            boolean isCompatible = modPackService.isModCompatibleWithTech(modName, "basic_industry"); // пример технологии
            ChatColor color = isCompatible ? ChatColor.GREEN : ChatColor.RED;
            player.sendMessage(color + "● " + modName + (isCompatible ? " (совместим)" : " (требует технологии)"));
        }
        
        for (String modName : currentModPack.getOptionalMods()) {
            boolean isCompatible = modPackService.isModCompatibleWithTech(modName, "basic_industry"); // пример технологии
            ChatColor color = isCompatible ? ChatColor.GREEN : ChatColor.RED;
            player.sendMessage(color + "○ " + modName + (isCompatible ? " (совместим)" : " (требует технологии)"));
        }
    }
    
    /**
     * Показать информацию о текущем модпаке
     */
    private void showCurrentModPackInfo(Player player) {
        if (currentModPack == null) {
            player.sendMessage(ChatColor.RED + "Нет выбранного модпака!");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Информация о модпаке ===");
        player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + currentModPack.getId());
        player.sendMessage(ChatColor.YELLOW + "Название: " + ChatColor.WHITE + currentModPack.getName());
        player.sendMessage(ChatColor.YELLOW + "Описание: " + ChatColor.WHITE + currentModPack.getDescription());
        player.sendMessage(ChatColor.YELLOW + "Версия: " + ChatColor.WHITE + currentModPack.getVersion());
        player.sendMessage(ChatColor.YELLOW + "Тип сервера: " + ChatColor.WHITE + currentModPack.getServerType());
        player.sendMessage(ChatColor.YELLOW + "Создатель: " + ChatColor.WHITE + currentModPack.getCreatedBy());
        player.sendMessage(ChatColor.YELLOW + "Число модов: " + ChatColor.WHITE + 
            (currentModPack.getRequiredMods().size() + currentModPack.getOptionalMods().size()));
        player.sendMessage(ChatColor.YELLOW + "Включено модов: " + ChatColor.WHITE + 
            countEnabledMods(currentModPack));
        
        // Дата создания
        player.sendMessage(ChatColor.YELLOW + "Создан: " + ChatColor.WHITE + 
            new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(currentModPack.getCreatedAt())));
    }
    
    /**
     * Подсчитать число включенных модов
     */
    private int countEnabledMods(ModPackBuilderService.ModPack modPack) {
        int count = 0;
        for (String modName : modPack.getRequiredMods()) {
            if (modPack.isModEnabled(modName)) count++;
        }
        for (String modName : modPack.getOptionalMods()) {
            if (modPack.isModEnabled(modName)) count++;
        }
        return count;
    }
    
    /**
     * Показать настройки отображения
     */
    private void showDisplaySettings(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Настройки отображения ===");
        player.sendMessage(ChatColor.YELLOW + "Частота обновления: " + ChatColor.WHITE + "5 минут");
        player.sendMessage(ChatColor.YELLOW + "Сохранение настроек: " + ChatColor.WHITE + "Автоматически");
        player.sendMessage(ChatColor.GRAY + "Для изменения используйте: /axiom config modpack");
    }
    
    /**
     * Показать справочную информацию
     */
    private void showHelpInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Справка по конструктору модпаков ===");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Создание модпаков позволяет настраивать игровые моды под конкретную тему");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Моды должны быть совместимы с изученными технологиями");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Разные типы серверов имеют разные базовые модпаки");
        player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Собранные модпаки можно скачать в формате ZIP");
        player.sendMessage(ChatColor.GRAY + "Для подробной информации используйте: /modpack help");
    }
}