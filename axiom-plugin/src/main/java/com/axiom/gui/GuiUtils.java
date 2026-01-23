package com.axiom.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating beautiful AXIOM GUI menus.
 * Implements the color scheme: Blue + Silver + White.
 */
public class GuiUtils {
    // Color scheme materials
    public static final Material BACKGROUND = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    public static final Material BORDER = Material.CYAN_STAINED_GLASS_PANE;
    public static final Material BUTTON_ACTIVE = Material.WHITE_STAINED_GLASS_PANE;
    public static final Material BUTTON_INACTIVE = Material.GRAY_STAINED_GLASS_PANE;
    public static final Material CONFIRM_YES = Material.LIME_STAINED_GLASS_PANE;
    public static final Material CONFIRM_NO = Material.RED_STAINED_GLASS_PANE;
    public static final Material CLOSE_BUTTON = Material.BARRIER;
    
    /**
     * Create menu with proper background and borders.
     */
    public static Inventory createMenu(String title, int rows) {
        Inventory inv = Bukkit.createInventory(null, rows * 9, colorize(title));
        fillBackground(inv);
        fillBorders(inv);
        return inv;
    }
    
    /**
     * Fill background with light blue glass panes.
     */
    private static void fillBackground(Inventory inv) {
        ItemStack bg = createGlassPane(BACKGROUND, " ", null);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, bg);
            }
        }
    }
    
    /**
     * Fill borders with cyan glass panes.
     */
    private static void fillBorders(Inventory inv) {
        ItemStack border = createGlassPane(BORDER, " ", null);
        int rows = inv.getSize() / 9;
        
        // Top row - only corners (0 and 8), skip middle slots where buttons go
        inv.setItem(0, border);  // Left corner
        inv.setItem(8, border);   // Right corner
        
        // Bottom row
        for (int i = 0; i < 9; i++) {
            if (inv.getItem((rows - 1) * 9 + i) == null) {
                inv.setItem((rows - 1) * 9 + i, border);
            }
        }
        
        // Left and right columns (skip top and bottom already handled)
        for (int i = 1; i < rows - 1; i++) {
            if (inv.getItem(i * 9) == null) {
                inv.setItem(i * 9, border);  // Left column
            }
            if (inv.getItem(i * 9 + 8) == null) {
                inv.setItem(i * 9 + 8, border);  // Right column
            }
        }
    }
    
    /**
     * Create a button with proper styling.
     */
    public static ItemStack button(Material mat, String name, List<String> lore) {
        return button(mat, name, lore, false);
    }
    
    /**
     * Create a button with optional glow effect.
     */
    public static ItemStack button(Material mat, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> l = new ArrayList<>();
                for (String s : lore) {
                    l.add(colorize(s));
                }
                meta.setLore(l);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, 
                ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE, 
                ItemFlag.HIDE_DESTROYS);
            
            // Add glow effect for confirmation
            if (glow) {
                meta.addEnchant(Enchantment.LURE, 1, true);
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create glass pane with name and lore.
     */
    public static ItemStack createGlassPane(Material mat, String name, List<String> lore) {
        return button(mat, name, lore);
    }
    
    /**
     * Format header title with gradient effect.
     */
    public static String formatHeader(String text) {
        return "§3§l[ §b" + text + " §3]";
    }
    
    /**
     * Format description text (gray, muted).
     */
    public static String formatDescription(String text) {
        return "§7" + text;
    }
    
    /**
     * Format error/warning text (red).
     */
    public static String formatError(String text) {
        return "§c" + text;
    }
    
    /**
     * Format success text (green).
     */
    public static String formatSuccess(String text) {
        return "§a" + text;
    }
    
    /**
     * Format motto with italic style.
     */
    public static String formatMotto(String motto) {
        if (motto == null || motto.isEmpty()) return "";
        return "§7§o\"" + motto + "\"";
    }
    
    /**
     * Colorize string with § codes.
     */
    public static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    
    /**
     * Create close button.
     */
    public static ItemStack createCloseButton() {
        return button(CLOSE_BUTTON, "§cЗакрыть", List.of("§7Нажмите для закрытия"));
    }
    
    /**
     * Create confirm yes button.
     */
    public static ItemStack createConfirmYes() {
        return button(CONFIRM_YES, "§a§l[ ДА ]", List.of("§7Подтвердить действие"));
    }
    
    /**
     * Create confirm no button.
     */
    public static ItemStack createConfirmNo() {
        return button(CONFIRM_NO, "§c§l[ НЕТ ]", List.of("§7Отменить действие"));
    }
}


