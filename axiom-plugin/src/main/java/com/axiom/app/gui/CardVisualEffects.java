package com.axiom.app.gui;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Класс для работы с визуальными эффектами в GUI карточной системы
 * Позволяет добавлять частицы и другие визуальные эффекты для карточных меню
 */
public class CardVisualEffects {
    
    /**
     * Воспроизвести эффект нажатия на карточку
     */
    public static void playClickEffect(Player player, int slot) {
        // Воспроизводим звук клика
        player.playSound(player.getLocation(), "block.stone_button.click_on", 0.5f, 1.0f);
        
        // Воспроизводим визуальный эффект (в реальной игре это будет отображаться над GUI, 
        // но в качестве примера добавим частицы)
        try {
            // Эффект клика - кратковременное вспыхивание
            org.bukkit.Location loc = player.getEyeLocation().clone();
            loc.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, -1, 0), 5, 0.2, 0.2, 0.2, 0.05);
        } catch (Exception e) {
            // Игнорируем ошибки частиц, если плагин не имеет соответствующих разрешений
        }
    }
    
    /**
     * Воспроизвести эффект наведения на карточку
     */
    public static void playHoverEffect(Player player, int slot) {
        try {
            org.bukkit.Location loc = player.getEyeLocation().clone();
            loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.add(0, -1, 0), 3, 0.15, 0.15, 0.15, 0.03);
        } catch (Exception e) {
            // Игнорируем ошибки частиц
        }
    }
    
    /**
     * Воспроизвести эффект выбора карточки
     */
    public static void playSelectionEffect(Player player, int slot) {
        try {
            org.bukkit.Location loc = player.getEyeLocation().clone();
            loc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc.add(0, -0.5, 0), 8, 0.3, 0.3, 0.3, 0.1);
        } catch (Exception e) {
            // Игнорируем ошибки частиц
        }
    }
    
    /**
     * Воспроизвести эффект разблокировки карточки (когда карточка становится доступной)
     */
    public static void playUnlockEffect(Player player, int slot) {
        try {
            org.bukkit.Location loc = player.getEyeLocation().clone();
            loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.add(0, -1, 0), 10, 0.2, 0.2, 0.2, 0.05);
        } catch (Exception e) {
            // Игнорируем ошибки частиц
        }
    }
    
    /**
     * Воспроизвести эффект блокировки карточки (когда карточка заблокирована)
     */
    public static void playLockEffect(Player player, int slot) {
        try {
            org.bukkit.Location loc = player.getEyeLocation().clone();
            loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc.add(0, -1, 0), 5, 0.1, 0.1, 0.1, 0.05);
        } catch (Exception e) {
            // Игнорируем ошибки частиц
        }
    }
    
    /**
     * Воспроизвести эффект изучения/разблокировки технологии
     */
    public static void playTechnologyUnlockEffect(Player player, int slot) {
        try {
            org.bukkit.Location loc = player.getEyeLocation().clone();
            loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc.add(0, -1, 0), 15, 0.3, 0.3, 0.3, 0.1);
            loc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc.add(0, -1, 0), 20, 0.3, 0.3, 0.3, 0.1);
            
            // Также проигрываем эффект звука
            player.playSound(player.getLocation(), "entity.player.levelup", 0.8f, 1.0f);
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    /**
     * Воспроизвести эффект ошибки (например, при недоступной технологии)
     */
    public static void playErrorEffect(Player player, int slot) {
        try {
            org.bukkit.Location loc = player.getEyeLocation().clone();
            loc.getWorld().spawnParticle(Particle.CRIT, loc.add(0, -0.5, 0), 8, 0.1, 0.1, 0.1, 0.05);
            
            // Звук ошибки
            player.playSound(player.getLocation(), "block.note_block.bass", 0.5f, 0.5f);
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    /**
     * Воспроизвести эффект загрузки/ожидания
     */
    public static void playLoadingEffect(Player player, int slot) {
        try {
            org.bukkit.Location loc = player.getEyeLocation().clone();
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, -1, 0), 3, 0.05, 0.1, 0.05, 0.02);
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
}