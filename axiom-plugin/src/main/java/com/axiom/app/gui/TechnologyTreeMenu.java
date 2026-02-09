package com.axiom.app.gui;

import com.axiom.AXIOM;
import com.axiom.domain.model.TechNode;
import com.axiom.domain.service.technology.TechnologyTreeService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * GUI меню технологического древа
 * Отображает сетку карточек технологий с визуальными состояниями
 */
public class TechnologyTreeMenu implements Listener {
    private final AXIOM plugin;
    private final Player player;
    private final Map<Integer, TechNode> slotToTechNode;
    private Inventory inventory;
    
    public TechnologyTreeMenu(AXIOM plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.slotToTechNode = new HashMap<>();
        createInventory();
        registerEvents();
    }
    
    /**
     * Создание инвентаря (GUI) с сеткой карточек технологий
     */
    private void createInventory() {
        // Создаем GUI 6x4 (24 слота) как в описании
        inventory = Bukkit.createInventory(null, 27, "Технологическое древо");
        
        // Заполняем инвентарь карточками технологий
        fillInventoryWithTechNodes();
        
        // Добавляем декоративные элементы
        addDecorativeElements();
    }
    
    /**
     * Заполнение инвентаря карточками технологий
     */
    private void fillInventoryWithTechNodes() {
        // Получаем список всех технологий из сервиса
        List<TechNode> allTechNodes = plugin.getTechnologyTreeService().getTechNodesForGUI(player.getUniqueId());
        
        // Распределяем технологии по слотам
        int slotIndex = 0;
        for (TechNode techNode : allTechNodes) {
            if (slotIndex >= 24) break; // Занимаем только первые 24 слота под технологии
            
            // Добавляем предмет в инвентарь
            ItemStack techItem = techNode.toItemStack();
            inventory.setItem(slotIndex, techItem);
            
            // Сохраняем соответствие слот -> технология
            slotToTechNode.put(slotIndex, techNode);
            
            slotIndex++;
        }
    }
    
    /**
     * Добавление декоративных элементов в GUI
     */
    private void addDecorativeElements() {
        // Заполняем оставшиеся слоты (24-26) декоративными элементами
        for (int i = 24; i < 27; i++) {
            ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            inventory.setItem(i, glassPane);
        }
        
        // Возможно, в будущем добавим навигационные элементы в последние слоты
    }
    
    /**
     * Открытие GUI для игрока
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Регистрация обработчиков событий
     */
    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Обработка кликов по элементам GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("Технологическое древо")) return;
        
        event.setCancelled(true); // Отменяем стандартное поведение
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        // Проверяем, кликнули ли по карточке технологии
        if (slotToTechNode.containsKey(slot)) {
            TechNode clickedTech = slotToTechNode.get(slot);
            
            // Обработка клика в зависимости от статуса технологии
            handleTechClick(clickedTech, player);
        }
    }
    
    /**
     * Обработка клика по карточке технологии
     */
    private void handleTechClick(TechNode techNode, Player player) {
        switch (techNode.getStatus()) {
            case LOCKED:
                player.sendMessage(ChatColor.RED + "Технология заблокирована! Изучите предварительные технологии.");
                break;
                
            case AVAILABLE:
                TechnologyTreeService.ResearchResult result =
                    plugin.getTechnologyTreeService().attemptResearch(player.getUniqueId(), techNode.getId());
                player.sendMessage(result.message);
                if (result.success) {
                    player.sendMessage(ChatColor.GREEN + "Вы изучили технологию: " + ChatColor.GOLD + techNode.getName());
                    updateInventory();
                }
                break;
                
            case UNLOCKED:
                player.sendMessage(ChatColor.GREEN + "Технология уже изучена!");
                break;
        }
    }
    
    /**
     * Проверка, достаточно ли ресурсов у игрока
     */
    
    
    /**
     * Обновление содержимого инвентаря
     */
    public void updateInventory() {
        // Очищаем текущее содержимое
        inventory.clear();
        
        // Перезаполняем технологиями
        fillInventoryWithTechNodes();
        
        // Добавляем декоративные элементы
        addDecorativeElements();
        
        // Если GUI открыт, обновляем у игрока
        if (player.getOpenInventory().getTopInventory().equals(inventory)) {
            player.updateInventory();
        }
    }
    
    /**
     * Закрытие GUI и отмена слушателей
     */
    public void close() {
        player.closeInventory();
    }
}
