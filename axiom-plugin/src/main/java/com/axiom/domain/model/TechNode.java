package com.axiom.domain.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.ArrayList;

/**
 * Класс, представляющий узел технологического древа
 * Соответствует требованиям в описании проекта
 */
public class TechNode {
    private String id;
    private String name;
    private String description;
    private Material icon;
    private List<String> prerequisites; // ID предыдущих технологий
    private List<String> unlocks; // ID карточек/меню, которые разблокирует
    private int cost; // Стоимость изучения
    private boolean isUnlocked; // Статус
    private TechStatus status; // Статус: LOCKED, AVAILABLE, UNLOCKED
    
    public TechNode(String id, String name, String description, Material icon, int cost) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.cost = cost;
        this.prerequisites = new ArrayList<>();
        this.unlocks = new ArrayList<>();
        this.isUnlocked = false;
        this.status = TechStatus.LOCKED;
    }
    
    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }
    
    public List<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }
    
    public void addPrerequisite(String techId) { this.prerequisites.add(techId); }
    
    public List<String> getUnlocks() { return unlocks; }
    public void setUnlocks(List<String> unlocks) { this.unlocks = unlocks; }
    
    public void addUnlock(String unlockId) { this.unlocks.add(unlockId); }
    
    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
    
    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { 
        this.isUnlocked = unlocked;
        this.status = unlocked ? TechStatus.UNLOCKED : TechStatus.LOCKED;
    }
    
    public TechStatus getStatus() { return status; }
    public void setStatus(TechStatus status) { 
        this.status = status;
        this.isUnlocked = (status == TechStatus.UNLOCKED);
    }
    
    /**
     * Создание ItemStack (предмета) для отображения в GUI
     * Цвет иконки зависит от статуса технологии
     */
    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Установка названия с цветом в зависимости от статуса
            String displayName = getStatusColor() + getName();
            meta.setDisplayName(displayName);
            
            // Установка описания
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + getDescription());
            lore.add(ChatColor.DARK_GRAY + "Стоимость: " + getCost());
            
            // Добавление информации о статусе
            switch (getStatus()) {
                case LOCKED:
                    lore.add(ChatColor.RED + "Заблокировано");
                    break;
                case AVAILABLE:
                    lore.add(ChatColor.YELLOW + "Доступно для изучения");
                    lore.add(ChatColor.GOLD + "Нажмите для изучения");
                    break;
                case UNLOCKED:
                    lore.add(ChatColor.GREEN + "Изучено");
                    lore.add(ChatColor.AQUA + "✓");
                    break;
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Цвет названия в зависимости от статуса
     */
    private String getStatusColor() {
        switch (getStatus()) {
            case LOCKED: return ChatColor.GRAY.toString();
            case AVAILABLE: return ChatColor.YELLOW.toString();
            case UNLOCKED: return ChatColor.GREEN.toString();
            default: return ChatColor.WHITE.toString();
        }
    }
    
    /**
     * Перечисление статусов технологии
     */
    public enum TechStatus {
        LOCKED,      // Заблокирована
        AVAILABLE,   // Доступна для изучения
        UNLOCKED     // Изучена
    }
}