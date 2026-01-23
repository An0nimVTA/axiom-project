package com.axiom.gui;

import org.bukkit.ChatColor;

/**
 * Класс для управления динамическими цветовыми схемами
 * В зависимости от типа сервера (современный, средневековый и т.д.)
 */
public class ColorSchemeManager {
    
    public enum ServerType {
        MODERN,          // Синий (#3b82f6) + Серебро (#e0e0e0) + Белый (#ffffff)
        MEDIEVAL,        // Тёмно-зелёный (#166534) + Золото (#d97706) + Кожа (#d2b48c)
        MEDIEVAL_MAGIC,  // Фиолетовый (#a855f7) + Бронза (#cd7f32) + Чёрный (#000000)
        MINIGAMES        // Оранжевый (#f97316) + Белый (#ffffff) + Серый (#94a3b8)
    }
    
    // Цветовые схемы в соответствии с брендбуком AXIOM
    private static final ColorScheme MODERN_SCHEME = new ColorScheme(
        ChatColor.BLUE,          // #3b82f6 - основной синий
        ChatColor.GRAY,          // #e0e0e0 - серебро (при преобразовании в MC цвета)
        ChatColor.WHITE,         // #ffffff - белый
        org.bukkit.Material.LIGHT_BLUE_TERRACOTTA,
        org.bukkit.Material.GRAY_TERRACOTTA,
        org.bukkit.Material.WHITE_TERRACOTTA
    );
    
    private static final ColorScheme MEDIEVAL_SCHEME = new ColorScheme(
        ChatColor.DARK_GREEN,    // #166534 - тёмно-зелёный
        ChatColor.GOLD,          // #d97706 - золото
        ChatColor.ORANGE,        // #d2b48c - кожа (приближённый MC цвет)
        org.bukkit.Material.GREEN_TERRACOTTA,
        org.bukkit.Material.YELLOW_TERRACOTTA,
        org.bukkit.Material.ORANGE_TERRACOTTA
    );
    
    private static final ColorScheme MEDIEVAL_MAGIC_SCHEME = new ColorScheme(
        ChatColor.LIGHT_PURPLE,  // #a855f7 - фиолетовый
        ChatColor.GOLD,          // #cd7f32 - бронза (приближённый MC цвет)
        ChatColor.BLACK,         // #000000 - чёрный
        org.bukkit.Material.PURPLE_TERRACOTTA,
        org.bukkit.Material.YELLOW_TERRACOTTA,
        org.bukkit.Material.BLACK_TERRACOTTA
    );
    
    private static final ColorScheme MINIGAMES_SCHEME = new ColorScheme(
        ChatColor.GOLD,          // #f97316 - оранжевый (приближённый MC цвет)
        ChatColor.WHITE,         // #ffffff - белый
        ChatColor.GRAY,          // #94a3b8 - серый
        org.bukkit.Material.ORANGE_TERRACOTTA,
        org.bukkit.Material.WHITE_TERRACOTTA,
        org.bukkit.Material.LIGHT_GRAY_TERRACOTTA
    );
    
    /**
     * Возвращает цветовую схему для указанного типа сервера
     */
    public static ColorScheme getColorScheme(ServerType serverType) {
        switch (serverType) {
            case MODERN:
                return MODERN_SCHEME;
            case MEDIEVAL:
                return MEDIEVAL_SCHEME;
            case MEDIEVAL_MAGIC:
                return MEDIEVAL_MAGIC_SCHEME;
            case MINIGAMES:
                return MINIGAMES_SCHEME;
            default:
                return MODERN_SCHEME; // по умолчанию
        }
    }
    
    /**
     * Класс, содержащий цвета и материалы для конкретной схемы
     */
    public static class ColorScheme {
        private final ChatColor primaryColor;      // основной цвет
        private final ChatColor secondaryColor;    // вторичный цвет
        private final ChatColor accentColor;       // акцентный цвет
        private final org.bukkit.Material primaryMaterial;    // основной материал для декора
        private final org.bukkit.Material secondaryMaterial;  // вторичный материал для декора
        private final org.bukkit.Material accentMaterial;     // акцентный материал для декора
        
        public ColorScheme(ChatColor primaryColor, ChatColor secondaryColor, ChatColor accentColor,
                          org.bukkit.Material primaryMaterial, org.bukkit.Material secondaryMaterial, org.bukkit.Material accentMaterial) {
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.accentColor = accentColor;
            this.primaryMaterial = primaryMaterial;
            this.secondaryMaterial = secondaryMaterial;
            this.accentMaterial = accentMaterial;
        }
        
        // Геттеры
        public ChatColor getPrimaryColor() { return primaryColor; }
        public ChatColor getSecondaryColor() { return secondaryColor; }
        public ChatColor getAccentColor() { return accentColor; }
        public org.bukkit.Material getPrimaryMaterial() { return primaryMaterial; }
        public org.bukkit.Material getSecondaryMaterial() { return secondaryMaterial; }
        public org.bukkit.Material getAccentMaterial() { return accentMaterial; }
    }
}