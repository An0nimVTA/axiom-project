package com.axiom.gui;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Базовый класс для GUI-меню на основе карточек
 * Реализует общую функциональность для всех меню с карточками
 */
public abstract class CardBasedMenu implements Listener {
    protected final AXIOM plugin;
    protected final Player player;
    protected final Map<Integer, CardAction> slotActions;
    protected Inventory inventory;
    protected String title;
    
    /**
     * Класс, представляющий карточку в меню
     */
    public static class Card {
        private Material icon;
        private String title;
        private String description;
        private CardAction action;
        
        public Card(Material icon, String title, String description, CardAction action) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.action = action;
        }
        
        public ItemStack toItemStack() {
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + title);
                
                // Добавляем описание
                String[] descriptionLines = description.split("\\|"); // Разделяем по | для многострочного описания
                for (int i = 0; i < descriptionLines.length; i++) {
                    descriptionLines[i] = ChatColor.GRAY + descriptionLines[i];
                }
                
                java.util.List<String> loreList = java.util.Arrays.asList(descriptionLines);
                meta.setLore(loreList);
                item.setItemMeta(meta);
            }
            
            return item;
        }
        
        // Геттеры
        public Material getIcon() { return icon; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public CardAction getAction() { return action; }
        
        // Сеттеры
        public void setIcon(Material icon) { this.icon = icon; }
        public void setTitle(String title) { this.title = title; }
        public void setDescription(String description) { this.description = description; }
        public void setAction(CardAction action) { this.action = action; }
    }
    
    /**
     * Интерфейс для действий, выполняемых при клике на карточку
     */
    public interface CardAction {
        void execute(Player player);
    }
    
    public CardBasedMenu(AXIOM plugin, Player player, String title) {
        this.plugin = plugin;
        this.player = player;
        this.title = title;
        this.slotActions = new HashMap<>();
        
        // Создаем инвентарь
        createInventory();
        
        // Регистрируем слушатель событий
        registerEvents();
    }
    
    /**
     * Создание GUI инвентаря
     */
    protected void createInventory() {
        // Создаем GUI 6x4 (24 слота) как в описании
        inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "" + ChatColor.BOLD + title);
    }
    
    /**
     * Заполнение инвентаря карточками
     */
    protected abstract void fillInventoryWithCards();
    
    /**
     * Добавление карточки в определенный слот
     */
    protected void addCard(int slot, Card card) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, card.toItemStack());
            slotActions.put(slot, card.getAction());
        }
    }
    
    /**
     * Добавление карточки в следующий свободный слот
     */
    protected void addCard(Card card) {
        for (int slot = 0; slot < 24; slot++) { // Используем первые 24 слота для карточек
            if (inventory.getItem(slot) == null) {
                addCard(slot, card);
                break;
            }
        }
    }
    
    /**
     * Открытие GUI для игрока
     */
    public void open() {
        // Заполняем инвентарь карточками
        fillInventoryWithCards();
        
        // Добавляем декоративные элементы
        addDecorativeElements();
        
        // Открываем инвентарь игроку
        player.openInventory(inventory);
    }
    
    /**
     * Добавление декоративных элементов в GUI
     */
    protected void addDecorativeElements() {
        // Заполняем оставшиеся слоты (24-26) декоративными элементами
        for (int i = 24; i < 27; i++) {
            ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            inventory.setItem(i, glassPane);
        }
    }
    
    /**
     * Регистрация обработчиков событий
     */
    protected void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Обработка кликов по элементам GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(inventory.getTitle())) return;
        
        event.setCancelled(true); // Отменяем стандартное поведение
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        // Проверяем, есть ли действие для этого слота
        if (slotActions.containsKey(slot)) {
            CardAction action = slotActions.get(slot);
            if (action != null) {
                // Выполняем действие
                action.execute(player);
                
                // Обновляем инвентарь (вдруг содержимое изменилось)
                updateInventory();
            }
        }
    }
    
    /**
     * Обновление содержимого инвентаря
     */
    public void updateInventory() {
        // Очищаем карту действий
        slotActions.clear();
        
        // Очищаем инвентарь
        inventory.clear();
        
        // Перезаполняем карточками
        fillInventoryWithCards();
        
        // Добавляем декоративные элементы
        addDecorativeElements();
        
        // Если GUI открыт, обновляем у игрока
        if (player.getOpenInventory().getTopInventory().equals(inventory)) {
            player.updateInventory();
        }
    }
    
    /**
     * Закрытие GUI
     */
    public void close() {
        player.closeInventory();
    }
    
    // Геттеры
    public AXIOM getPlugin() { return plugin; }
    public Player getPlayer() { return player; }
    public Inventory getInventory() { return inventory; }
}