package com.axiom.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.HashMap;

/**
 * Класс для отображения прогресса и анимации в карточной системе
 * Позволяет отображать прогресс изучения технологий, выполнения задач и т.д.
 */
public class ProgressVisualizer {
    private final Map<Integer, ProgressTracker> slotProgressTrackers = new HashMap<>();
    
    /**
     * Класс для отслеживания прогресса
     */
    public static class ProgressTracker {
        private double currentProgress;
        private double maxProgress;
        private String taskName;
        private boolean isAnimating;
        
        public ProgressTracker(double maxProgress, String taskName) {
            this.currentProgress = 0;
            this.maxProgress = maxProgress;
            this.taskName = taskName;
            this.isAnimating = false;
        }
        
        public void setProgress(double progress) {
            this.currentProgress = Math.min(progress, maxProgress);
        }
        
        public void incrementProgress(double amount) {
            this.currentProgress = Math.min(currentProgress + amount, maxProgress);
        }
        
        public double getProgress() { return currentProgress; }
        public double getMaxProgress() { return maxProgress; }
        public String getTaskName() { return taskName; }
        public boolean isComplete() { return currentProgress >= maxProgress; }
        public double getPercentage() { return maxProgress > 0 ? (currentProgress / maxProgress) * 100 : 0; }
    }
    
    /**
     * Проверить, идет ли анимация в слоте
     */
    public boolean isAnimating(int slot) {
        ProgressTracker tracker = slotProgressTrackers.get(slot);
        return tracker != null && tracker.isAnimating;
    }
    
    /**
     * Запустить анимацию прогресса в слоте
     */
    public void startProgressAnimation(int slot, double maxProgress, String taskName, Player player, Inventory inventory) {
        ProgressTracker tracker = new ProgressTracker(maxProgress, taskName);
        tracker.isAnimating = true;
        slotProgressTrackers.put(slot, tracker);
        
        // Запускаем анимацию
        new BukkitRunnable() {
            @Override
            public void run() {
                ProgressTracker currentTracker = slotProgressTrackers.get(slot);
                if (currentTracker == null || currentTracker.isComplete()) {
                    // Анимация завершена
                    currentTracker.isAnimating = false;
                    updateProgressDisplay(slot, currentTracker, player, inventory);
                    this.cancel();
                    return;
                }
                
                // Увеличиваем прогресс
                currentTracker.incrementProgress(maxProgress * 0.05); // 5% за тик
                updateProgressDisplay(slot, currentTracker, player, inventory);
            }
        }.runTaskTimer(Bukkit.getWorldContainer().iterator().next().getServer().getPluginManager().getPlugin("AXIOM"), 0, 2); // Каждые 2 тика (0.1 сек)
    }
    
    /**
     * Обновить отображение прогресса в слоте
     */
    public void updateProgressDisplay(int slot, ProgressTracker tracker, Player player, Inventory inventory) {
        ItemStack item = createProgressBarItem(tracker);
        inventory.setItem(slot, item);
    }
    
    /**
     * Создать предмет, отображающий прогресс
     */
    private ItemStack createProgressBarItem(ProgressTracker tracker) {
        // В зависимости от прогресса, будем использовать разные материалы
        Material iconMaterial;
        if (tracker.getPercentage() <= 25) {
            iconMaterial = Material.RED_WOOL; // Низкий прогресс
        } else if (tracker.getPercentage() <= 50) {
            iconMaterial = Material.ORANGE_WOOL; // Средний прогресс
        } else if (tracker.getPercentage() <= 75) {
            iconMaterial = Material.YELLOW_WOOL; // Высокий прогресс
        } else if (tracker.getPercentage() < 100) {
            iconMaterial = Material.LIME_WOOL; // Очень высокий
        } else {
            iconMaterial = Material.GREEN_WOOL; // Завершено
        }
        
        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§e§l" + tracker.taskName);
            
            // Создаем визуальный прогресс-бар
            StringBuilder progressBar = new StringBuilder("§7[");
            int barLength = 20;
            int filledBlocks = (int) (barLength * (tracker.getProgress() / tracker.getMaxProgress()));
            
            for (int i = 0; i < barLength; i++) {
                if (i < filledBlocks) {
                    progressBar.append("§a█"); // Заполненная часть
                } else {
                    progressBar.append("§7░"); // Пустая часть
                }
            }
            progressBar.append("§7]");
            
            String percentageText = String.format("§6%.1f%%", tracker.getPercentage());
            
            meta.setLore(java.util.List.of(
                progressBar.toString(),
                percentageText,
                "§7Прогресс: §f" + String.format("%.2f", tracker.getProgress()) + " §7из §f" + String.format("%.2f", tracker.getMaxProgress())
            ));
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Получить трекер прогресса для слота
     */
    public ProgressTracker getProgressTracker(int slot) {
        return slotProgressTrackers.get(slot);
    }
    
    /**
     * Удалить трекер прогресса для слота
     */
    public void removeProgressTracker(int slot) {
        slotProgressTrackers.remove(slot);
    }
    
    /**
     * Остановить анимацию прогресса
     */
    public void stopProgressAnimation(int slot) {
        ProgressTracker tracker = slotProgressTrackers.get(slot);
        if (tracker != null) {
            tracker.isAnimating = false;
        }
    }
    
    /**
     * Сбросить все трекеры прогресса
     */
    public void resetAll() {
        slotProgressTrackers.clear();
    }
}