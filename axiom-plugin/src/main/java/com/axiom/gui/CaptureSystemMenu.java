package com.axiom.gui;

import com.axiom.AXIOM;
import com.axiom.service.CountryCaptureService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI меню для системы динамического захвата стран и городов
 * Использует карточную систему для управления модами
 */
public class CaptureSystemMenu extends CardBasedMenu {
    private final AXIOM plugin;
    private final CountryCaptureService captureService;
    private String currentView = "main"; // "main", "country_cores", "city_cores", "trenches"
    private String selectedNationId = null;
    
    public CaptureSystemMenu(AXIOM plugin, Player player) {
        super(plugin, player, "Система захвата");
        this.plugin = plugin;
        this.captureService = plugin.getCountryCaptureService();
    }

    @Override
    protected void fillInventoryWithCards() {
        switch (currentView) {
            case "main":
                fillMainView();
                break;
            case "country_cores":
                fillCountryCoresView();
                break;
            case "city_cores":
                fillCityCoresView();
                break;
            case "trenches":
                fillTrenchesView();
                break;
            default:
                fillMainView();
                break;
        }
    }
    
    private void fillMainView() {
        // Карточка: Ядра стран (Material.BEACON)
        addCard(new Card(
            Material.BEACON,
            "Ядра стран",
            "Проверить состояние|ядер стран",
            (player) -> {
                currentView = "country_cores";
                updateInventory();
            }
        ));
        
        // Карточка: Ядра городов (Material.BRICKS)
        addCard(new Card(
            Material.BRICKS,
            "Ядра городов",
            "Проверить и управлять|ядрами городов",
            (player) -> {
                currentView = "city_cores";
                updateInventory();
            }
        ));
        
        // Карточка: Укрепления (Material.IRON_BLOCK)
        addCard(new Card(
            Material.IRON_BLOCK,
            "Укрепления",
            "Создать и управлять|укреплениями (окопы)",
            (player) -> {
                currentView = "trenches";
                updateInventory();
            }
        ));
        
        // Карточка: Начать осаду (Material.CROSSBOW)
        addCard(new Card(
            Material.CROSSBOW,
            "Начать осаду",
            "Объявить осаду|вражеской нации",
            (player) -> {
                String attackerNationId = plugin.getNationManager().getNationIdOfPlayer(player.getUniqueId());
                if (attackerNationId == null) {
                    player.sendMessage(ChatColor.RED + "Вы должны состоять в нации для начала осады!");
                    return;
                }
                
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Используйте команду: /capture begin_siege <нация_для_захвата>");
            }
        ));
        
        // Карточка: Информация о системе (Material.KNOWLEDGE_BOOK)
        addCard(new Card(
            Material.KNOWLEDGE_BOOK,
            "Информация",
            "Как работает|система захвата",
            (player) -> {
                player.sendMessage(ChatColor.GOLD + "=== Система динамического захвата ===");
                player.sendMessage(ChatColor.YELLOW + "• Для захвата страны нужно разрушить её ядро");
                player.sendMessage(ChatColor.YELLOW + "• Для захвата города нужно разрушить ядро города");
                player.sendMessage(ChatColor.YELLOW + "• Используйте окопы (траншеи) для укрепления позиций");
                player.sendMessage(ChatColor.YELLOW + "• Ядра можно повреждать в осадном режиме");
                player.sendMessage(ChatColor.YELLOW + "• Укрепления можно создавать в нейтральных или вражеских чанках");
            }
        ));
    }
    
    private void fillCountryCoresView() {
        // Карточка "Назад" (Material.ARROW)
        addCard(new Card(
            Material.ARROW,
            "Назад",
            "Вернуться в главное меню",
            (player) -> {
                currentView = "main";
                updateInventory();
            }
        ));
        
        // Отображение ядер стран
        var countryCores = captureService.getAllCountryCores();
        for (var core : countryCores) {
            Material icon;
            if (core.getStability() > core.getMaxStability() * 0.8) {
                icon = Material.GREEN_WOOL; // Здоровая нация
            } else if (core.getStability() > core.getMaxStability() * 0.3) {
                icon = Material.YELLOW_WOOL; // Поврежденная
            } else {
                icon = Material.RED_WOOL; // В критическом состоянии
            }
            
            addCard(new Card(
                icon,
                core.getNationName(),
                "Стабильность: " + core.getStability() + "/" + core.getMaxStability() + 
                "|" + "Военная сила: " + core.getMilitaryStrength() +
                "|" + "Столица: " + core.getCapitalLocation().getBlockX() + "," + core.getCapitalLocation().getBlockZ(),
                (player) -> {
                    selectedNationId = core.getId();
                    // Показываем информацию о ядре
                    player.sendMessage(ChatColor.GOLD + "=== Ядро страны: " + core.getNationName() + " ===");
                    player.sendMessage(ChatColor.YELLOW + "Стабильность: " + ChatColor.WHITE + core.getStability() + "/" + core.getMaxStability());
                    player.sendMessage(ChatColor.YELLOW + "Военная сила: " + ChatColor.WHITE + core.getMilitaryStrength());
                    player.sendMessage(ChatColor.YELLOW + "Правительственные постройки: " + 
                        ChatColor.WHITE + core.getGovernmentBuildingHealth() + "/" + core.getMaxGovernmentBuildingHealth());
                    player.sendMessage(ChatColor.YELLOW + "Под угрозой: " + 
                        (core.isUnderThreat() ? ChatColor.RED + "ДА" : ChatColor.GREEN + "НЕТ"));
                    player.sendMessage(ChatColor.YELLOW + "Контролируется: " + 
                        (core.isFullyControlled() ? ChatColor.GREEN + "ДА" : ChatColor.RED + "НЕТ"));
                    
                    // Возможность нанести урон, если игрок в атакующей нации
                    String attackerNationId = plugin.getNationManager().getNationIdOfPlayer(player.getUniqueId());
                    if (captureService.isUnderSiege(core.getId())) {
                        player.sendMessage(ChatColor.GRAY + "Эта нация находится под осадой!");
                        player.sendMessage(ChatColor.GREEN + "Используйте: /capture damage_core country " + core.getId() + " <урон> для нанесения урона ядру");
                    } else {
                        player.sendMessage(ChatColor.GRAY + "Для начала нанесения урона начните осаду: /capture begin_siege " + core.getId());
                    }
                }
            ));
        }
    }
    
    private void fillCityCoresView() {
        // Карточка "Назад" (Material.ARROW)
        addCard(new Card(
            Material.ARROW,
            "Назад",
            "Вернуться в главное меню",
            (player) -> {
                currentView = "main";
                updateInventory();
            }
        ));
        
        // Отображение ядер городов
        var cityCores = captureService.getAllCityCores();
        for (var core : cityCores) {
            Material icon;
            if (core.getHealth() > core.getMaxHealth() * 0.8) {
                icon = Material.GREEN_CONCRETE; // Здоровый город
            } else if (core.getHealth() > core.getMaxHealth() * 0.3) {
                icon = Material.YELLOW_CONCRETE; // Поврежденный
            } else {
                icon = Material.RED_CONCRETE; // В критическом состоянии
            }
            
            addCard(new Card(
                icon,
                core.getCityName(),
                "Уровень: " + core.getLevel() + 
                "|" + "Здоровье: " + core.getHealth() + "/" + core.getMaxHealth() + 
                "|" + "Нация: " + core.getNationId(),
                (player) -> {
                    // Показываем информацию о ядре города
                    player.sendMessage(ChatColor.GOLD + "=== Ядро города: " + core.getCityName() + " ===");
                    player.sendMessage(ChatColor.YELLOW + "Уровень: " + ChatColor.WHITE + core.getLevel());
                    player.sendMessage(ChatColor.YELLOW + "Здоровье: " + ChatColor.WHITE + core.getHealth() + "/" + core.getMaxHealth());
                    player.sendMessage(ChatColor.YELLOW + "Принадлежит нации: " + ChatColor.WHITE + core.getNationId());
                    
                    // Показываем защищенные чанки
                    player.sendMessage(ChatColor.AQUA + "Защищено чанков: " + ChatColor.WHITE + core.getProtectedChunks().size());
                    
                    // Возможность нанести урон, если игрок в атакующей нации
                    String attackerNationId = plugin.getNationManager().getNationIdOfPlayer(player.getUniqueId());
                    // В реальной системе нужно проверить, под осадой ли город
                    player.sendMessage(ChatColor.GRAY + "Используйте: /capture damage_core city " + core.getId() + " <урон> для нанесения урона ядру");
                }
            ));
        }
    }
    
    private void fillTrenchesView() {
        // Карточка "Назад" (Material.ARROW)
        addCard(new Card(
            Material.ARROW,
            "Назад",
            "Вернуться в главное меню",
            (player) -> {
                currentView = "main";
                updateInventory();
            }
        ));
        
        // Карточка: Захватить чанк (Material.IRON_SHOVEL)
        addCard(new Card(
            Material.IRON_SHOVEL,
            "Создать укрепления",
            "Захватить чанк для|окопов и укреплений",
            (player) -> {
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Используйте: /capture capture_chunk чтобы захватить текущий чанк для укреплений");
            }
        ));
        
        // Показываем существующие укрепления
        String nationId = plugin.getNationManager().getNationIdOfPlayer(player.getUniqueId());
        if (nationId != null) {
            var trenches = captureService.getTrenchChunksForNation(nationId);
            
            if (!trenches.isEmpty()) {
                for (org.bukkit.Chunk chunk : trenches) {
                    addCard(new Card(
                        Material.DIRT,
                        "Укрепление #" + chunk.getX() + "," + chunk.getZ(),
                        "Позиция укрепления|в чанке " + chunk.getX() + "," + chunk.getZ(),
                        (p) -> {
                            p.sendMessage(ChatColor.GREEN + "Укрепление в чанке: " + chunk.getX() + "," + chunk.getZ());
                            p.teleport(chunk.getWorld().getHighestBlockAt(
                                chunk.getX() * 16, chunk.getZ() * 16).getLocation());
                        }
                    ));
                }
            } else {
                addCard(new Card(
                    Material.GRAY_CONCRETE,
                    "Нет укреплений",
                    "Вы не создали|ни одного укрепления",
                    (p) -> {
                        p.sendMessage(ChatColor.YELLOW + "Создайте укрепления с помощью /capture capture_chunk");
                    }
                ));
            }
        } else {
            addCard(new Card(
                Material.BARRIER,
                "Не в нации",
                "Вступите в нацию|для создания укреплений",
                (p) -> {
                    p.sendMessage(ChatColor.RED + "Вы должны состоять в нации для создания укреплений!");
                }
            ));
        }
    }
}