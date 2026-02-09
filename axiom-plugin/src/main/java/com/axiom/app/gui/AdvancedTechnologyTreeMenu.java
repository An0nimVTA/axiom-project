package com.axiom.app.gui;

import com.axiom.AXIOM;
import com.axiom.domain.service.technology.TechnologyTreeService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Улучшенное меню технологического древа с прогрессом и визуальными эффектами
 * Использует все новые возможности карточной системы
 */
public class AdvancedTechnologyTreeMenu implements Listener {
    private final AXIOM plugin;
    private final Player player;
    private final Map<Integer, TechCard> slotToTechCard;
    private final Map<Integer, ProgressVisualizer.ProgressTracker> slotProgressTrackers;
    private Inventory inventory;
    
    /**
     * Класс для карточки технологии с расширенными возможностями
     */
    public static class TechCard {
        private String id;
        private String name;
        private String description;
        private Material icon;
        private int cost;
        private boolean isUnlocked;
        private boolean isAvailable;
        private java.util.List<String> prerequisites; // ID предыдущих технологий
        private java.util.List<String> unlocks; // ID карточек/меню, которые разблокирует
        
        public TechCard(String id, String name, String description, Material icon, int cost) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.cost = cost;
            this.isUnlocked = false;
            this.isAvailable = false;
            this.prerequisites = new java.util.ArrayList<>();
            this.unlocks = new java.util.ArrayList<>();
        }
        
        public ItemStack toItemStack() {
            ItemStack item = new ItemStack(getAppropriateMaterial());
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(getColoredName());
                
                // Создаем lore в зависимости от состояния технологии
                java.util.List<String> lore = new java.util.ArrayList<>();
                
                // Основное описание
                lore.add(ChatColor.GRAY + getDescription());
                
                // Добавляем информацию о стоимости
                lore.add("");
                lore.add(ChatColor.GOLD + "Стоимость: " + ChatColor.YELLOW + getCost());
                
                // Добавляем статус технологии
                lore.add("");
                if (isUnlocked()) {
                    lore.add(ChatColor.GREEN + "✓ ИЗУЧЕНО");
                } else if (isAvailable()) {
                    lore.add(ChatColor.YELLOW + "⚡ ДОСТУПНО ДЛЯ ИЗУЧЕНИЯ");
                    lore.add(ChatColor.AQUA + "Нажмите для изучения");
                } else {
                    lore.add(ChatColor.RED + "🔒 ЗАБЛОКИРОВАНО");
                    lore.add(ChatColor.GRAY + "Требуется предварительное изучение");
                    // Показываем пререквизиты, если они есть
                    if (!getPrerequisites().isEmpty()) {
                        lore.add(ChatColor.DARK_GRAY + "Предварительно:");
                        for (String prereq : getPrerequisites()) {
                            lore.add("  " + ChatColor.GRAY + "- " + prereq);
                        }
                    }
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
            return item;
        }
        
        private Material getAppropriateMaterial() {
            if (isUnlocked()) {
                return getIcon(); // Обычная иконка для изученных
            } else if (isAvailable()) {
                // Используем светлый вариант иконки для доступных
                switch (getIcon()) {
                    case BEACON:
                        return Material.LIGHT_BLUE_CONCRETE; // Более светлый Beacon
                    case BOOK:
                        return Material.KNOWLEDGE_BOOK;
                    case EMERALD:
                        return Material.EMERALD_BLOCK;
                    default:
                        return getIcon();
                }
            } else {
                // Тусклый вариант иконки для заблокированных
                switch (getIcon()) {
                    case BEACON:
                        return Material.GRAY_CONCRETE;
                    case BOOK:
                        return Material.BOOK;
                    case EMERALD:
                        return Material.GREEN_WOOL;
                    default:
                        return getIcon();
                }
            }
        }
        
        private String getColoredName() {
            if (isUnlocked()) {
                return ChatColor.GREEN + "" + ChatColor.BOLD + getName();
            } else if (isAvailable()) {
                return ChatColor.YELLOW + "" + ChatColor.BOLD + getName();
            } else {
                return ChatColor.GRAY + "" + ChatColor.BOLD + getName();
            }
        }
        
        // Геттеры и сеттеры
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Material getIcon() { return icon; }
        public int getCost() { return cost; }
        public boolean isUnlocked() { return isUnlocked; }
        public boolean isAvailable() { return isAvailable; }
        public java.util.List<String> getPrerequisites() { return prerequisites; }
        public java.util.List<String> getUnlocks() { return unlocks; }
        
        public void setId(String id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setIcon(Material icon) { this.icon = icon; }
        public void setCost(int cost) { this.cost = cost; }
        public void setUnlocked(boolean unlocked) { this.isUnlocked = unlocked; }
        public void setAvailable(boolean available) { this.isAvailable = available; }
        public void setPrerequisites(java.util.List<String> prerequisites) { this.prerequisites = prerequisites; }
        public void setUnlocks(java.util.List<String> unlocks) { this.unlocks = unlocks; }
        
        public void addPrerequisite(String prereq) { this.prerequisites.add(prereq); }
        public void addUnlock(String unlock) { this.unlocks.add(unlock); }
    }
    
    public AdvancedTechnologyTreeMenu(AXIOM plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.slotToTechCard = new HashMap<>();
        this.slotProgressTrackers = new HashMap<>();
        
        createInventory();
        registerEvents();
    }
    
    private void createInventory() {
        // Создаем GUI 6x4 (24 слота) как в описании
        inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "Технологическое древо");
        
        // Заполняем инвентарь карточками технологий
        fillInventoryWithTechCards();
        
        // Добавляем декоративные элементы
        addDecorativeElements();
    }
    
    private void fillInventoryWithTechCards() {
        // Здесь мы получаем список технологий из сервиса и создаем карточки
        java.util.List<TechCard> techCards = createTechCardsFromService();
        
        int slotIndex = 0;
        for (TechCard techCard : techCards) {
            if (slotIndex >= 24) break; // Занимаем только первые 24 слота под технологии
            
            // Обновляем статус технологии в зависимости от прогресса игрока
            updateTechCardStatus(techCard, player);
            
            // Добавляем предмет в инвентарь
            ItemStack techItem = techCard.toItemStack();
            inventory.setItem(slotIndex, techItem);
            
            // Сохраняем соответствие слот -> карта технологии
            slotToTechCard.put(slotIndex, techCard);
            
            slotIndex++;
        }
    }
    
    private void updateTechCardStatus(TechCard techCard, Player player) {
        TechnologyTreeService techService = plugin.getTechnologyTreeService();
        if (techService == null) {
            techCard.setUnlocked(false);
            techCard.setAvailable(false);
            return;
        }

        // Проверяем, изучена ли технология
        if (techService.isPlayerHasTech(player.getUniqueId(), techCard.getId())) {
            techCard.setUnlocked(true);
            return;
        }

        techCard.setAvailable(techService.canPlayerLearnTech(player.getUniqueId(), techCard.getId()));
    }
    
    private java.util.List<TechCard> createTechCardsFromService() {
        TechnologyTreeService techService = plugin.getTechnologyTreeService();
        if (techService == null) {
            return new ArrayList<>();
        }

        List<TechnologyTreeService.Technology> techs = new ArrayList<>(techService.getAllTechs());
        techs.sort(Comparator
            .comparing((TechnologyTreeService.Technology t) -> t.stage != null ? t.stage.ordinal() : 0)
            .thenComparingInt(t -> t.tier)
            .thenComparing(t -> t.branch)
            .thenComparing(t -> t.name));

        List<TechCard> techCards = new ArrayList<>();
        for (TechnologyTreeService.Technology tech : techs) {
            Material icon = techService.getMaterialForTech(tech);
            TechCard card = new TechCard(tech.id, tech.name, tech.description, icon, (int) tech.researchCost);
            card.setPrerequisites(new ArrayList<>(tech.prerequisites));
            techCards.add(card);
        }

        return techCards;
    }
    
    private void addDecorativeElements() {
        // Заполняем оставшиеся слоты (24-26) декоративными элементами
        for (int i = 24; i < 27; i++) {
            ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            inventory.setItem(i, glassPane);
        }
        
        // Возможно, в будущем добавим навигационные элементы в последние слоты
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("Технологическое древо")) return;
        
        event.setCancelled(true); // Отменяем стандартное поведение
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        // Проверяем, кликнули ли по карточке технологии
        if (slotToTechCard.containsKey(slot)) {
            TechCard clickedTech = slotToTechCard.get(slot);
            
            // Обработка клика в зависимости от статуса технологии
            handleTechClick(clickedTech, player);
        }
    }
    
    private void handleTechClick(TechCard techCard, Player player) {
        if (techCard.isUnlocked()) {
            player.sendMessage(ChatColor.GREEN + "Технология '" + techCard.getName() + "' уже изучена!");
            CardVisualEffects.playSelectionEffect(player, 0); // Показываем эффект выбора
        } else if (techCard.isAvailable()) {
            TechnologyTreeService.ResearchResult result =
                plugin.getTechnologyTreeService().attemptResearch(player.getUniqueId(), techCard.getId());
            player.sendMessage(result.message);
            if (result.success) {
                player.sendMessage(ChatColor.AQUA + "Эффект: " + techCard.getDescription());
                CardVisualEffects.playUnlockEffect(player, 0);
                Bukkit.getScheduler().runTaskLater(plugin, this::updateInventory, 10L);
            } else {
                CardVisualEffects.playLockEffect(player, 0);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Технология заблокирована! Изучите предварительные технологии.");
            CardVisualEffects.playLockEffect(player, 0); // Эффект блокировки
        }
    }
    
    public void updateInventory() {
        // Очищаем текущее содержимое
        inventory.clear();
        
        // Перезаполняем технологиями
        fillInventoryWithTechCards();
        
        // Добавляем декоративные элементы
        addDecorativeElements();
        
        // Если GUI открыт, обновляем у игрока
        if (player.getOpenInventory().getTopInventory().equals(inventory)) {
            player.updateInventory();
        }
    }
    
    // Добавляем getter для инвентаря для использования в тестировании
    public Inventory getInventory() {
        return inventory;
    }
}
