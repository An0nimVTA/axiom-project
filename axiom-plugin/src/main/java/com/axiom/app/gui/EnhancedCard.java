package com.axiom.app.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Улучшенный класс карточки с поддержкой различных визуальных состояний
 */
public class EnhancedCard {
    private Material icon;
    private String title;
    private String description;
    private CardBasedMenu.CardAction action;
    private CardState state;
    private List<String> additionalLore; // Дополнительная информация
    
    public enum CardState {
        LOCKED,          // Заблокирована
        AVAILABLE,       // Доступна для изучения
        UNLOCKED,        // Изучена/разблокирована
        DISABLED         // Недоступна (например, требует других карточек)
    }
    
    public EnhancedCard(Material icon, String title, String description, CardBasedMenu.CardAction action) {
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.action = action;
        this.state = CardState.UNLOCKED; // по умолчанию
        this.additionalLore = new ArrayList<>();
    }
    
    /**
     * Создание ItemStack с учетом текущего состояния карточки
     */
    public ItemStack toItemStack(ColorSchemeManager.ColorScheme colorScheme) {
        ItemStack item = new ItemStack(getStateSpecificMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Устанавливаем цвет заголовка в зависимости от состояния
            ChatColor titleColor = getStateSpecificTitleColor();
            meta.setDisplayName(titleColor + "" + ChatColor.BOLD + title);
            
            // Формируем описание в зависимости от состояния
            List<String> lore = new ArrayList<>();
            
            // Основное описание
            String[] descriptionLines = description.split("\\|");
            for (String line : descriptionLines) {
                lore.add(getStateSpecificDescriptionColor() + line);
            }
            
            // Добавляем статус
            lore.add("");
            lore.add(getStateDescription());
            
            // Добавляем дополнительную информацию
            lore.addAll(additionalLore);
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Получить материал, соответствующий текущему состоянию карточки
     */
    private Material getStateSpecificMaterial() {
        switch (state) {
            case LOCKED:
                return Material.BARRIER; // или полупрозрачный материал
            case AVAILABLE:
            case UNLOCKED:
                return icon; // обычный икон
            case DISABLED:
                return Material.GRAY_DYE; // серый цвет, показывает недоступность
            default:
                return icon;
        }
    }
    
    /**
     * Получить цвет заголовка в зависимости от состояния
     */
    private ChatColor getStateSpecificTitleColor() {
        switch (state) {
            case LOCKED:
                return ChatColor.GRAY; // серый для заблокированных
            case AVAILABLE:
                return ChatColor.YELLOW; // желтый для доступных
            case UNLOCKED:
                return ChatColor.GREEN; // зеленый для разблокированных
            case DISABLED:
                return ChatColor.GRAY; // серый для недоступных
            default:
                return ChatColor.WHITE;
        }
    }
    
    /**
     * Получить цвет описания в зависимости от состояния
     */
    private ChatColor getStateSpecificDescriptionColor() {
        switch (state) {
            case LOCKED:
                return ChatColor.DARK_GRAY; // темно-серый
            case AVAILABLE:
                return ChatColor.GRAY; // обычный серый
            case UNLOCKED:
                return ChatColor.WHITE; // белый
            case DISABLED:
                return ChatColor.DARK_GRAY; // темно-серый
            default:
                return ChatColor.GRAY;
        }
    }
    
    /**
     * Получить текстовое описание состояния
     */
    private String getStateDescription() {
        switch (state) {
            case LOCKED:
                return ChatColor.DARK_RED + "🔒 ЗАБЛОКИРОВАНО";
            case AVAILABLE:
                return ChatColor.YELLOW + "⚡ ДОСТУПНО";
            case UNLOCKED:
                return ChatColor.GREEN + "✅ ИЗУЧЕНО";
            case DISABLED:
                return ChatColor.GRAY + "❌ НЕДОСТУПНО";
            default:
                return "";
        }
    }
    
    // Геттеры и сеттеры
    public Material getIcon() { return icon; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public CardBasedMenu.CardAction getAction() { return action; }
    public CardState getState() { return state; }
    public List<String> getAdditionalLore() { return new ArrayList<>(additionalLore); }
    
    public void setIcon(Material icon) { this.icon = icon; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAction(CardBasedMenu.CardAction action) { this.action = action; }
    public void setState(CardState state) { this.state = state; }
    public void addAdditionalLore(String loreLine) { this.additionalLore.add(loreLine); }
    public void clearAdditionalLore() { this.additionalLore.clear(); }
}