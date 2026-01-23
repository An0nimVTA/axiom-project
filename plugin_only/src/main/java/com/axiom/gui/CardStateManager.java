package com.axiom.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс для отслеживания состояния карточек в GUI
 * Позволяет реализовать визуальные эффекты и анимации
 */
public class CardStateManager {
    private final Map<Integer, Boolean> hoveredSlots = new HashMap<>();
    
    /**
     * Установить состояние наведения для слота
     */
    public void setHovered(int slot, boolean hovered) {
        hoveredSlots.put(slot, hovered);
    }
    
    /**
     * Получить состояние наведения для слота
     */
    public boolean isHovered(int slot) {
        return hoveredSlots.getOrDefault(slot, false);
    }
    
    /**
     * Обновить карточку в слоте с учетом состояния
     */
    public void updateCardInSlot(CardBasedMenu.Card card, int slot, Player player, CardBasedMenu menu) {
        // Получаем текущую цветовую схему меню
        var colorScheme = menu.getColorScheme();
        boolean isHovered = isHovered(slot);
        
        // Создаем ItemStack с учетом состояния
        ItemStack itemStack = card.toItemStack(colorScheme, isHovered);
        
        // Обновляем предмет в инвентаре
        menu.getInventory().setItem(slot, itemStack);
    }
    
    /**
     * Сбросить состояние всех слотов
     */
    public void reset() {
        hoveredSlots.clear();
    }
    
    /**
     * Сбросить состояние для конкретного слота
     */
    public void resetSlot(int slot) {
        hoveredSlots.remove(slot);
    }
}