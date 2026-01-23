package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public class ItemBalanceScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;
    private String selectedCategory = "Оружие";
    
    private final Map<String, List<ItemStat>> items = new HashMap<>();

    public ItemBalanceScreen(Screen parent) {
        super(Component.literal("Балансировка предметов"));
        this.parent = parent;
        initItems();
    }

    private void initItems() {
        // Оружие
        items.put("Оружие", Arrays.asList(
            new ItemStat("Железный меч", "Урон", 7, 10, "⚔️"),
            new ItemStat("Алмазный меч", "Урон", 8, 12, "⚔️"),
            new ItemStat("Лук", "Урон", 9, 15, "🏹"),
            new ItemStat("Арбалет", "Урон", 11, 18, "🏹")
        ));
        
        // Броня
        items.put("Броня", Arrays.asList(
            new ItemStat("Железный шлем", "Защита", 2, 4, "🛡️"),
            new ItemStat("Алмазный нагрудник", "Защита", 8, 12, "🛡️"),
            new ItemStat("Незеритовые поножи", "Защита", 6, 10, "🛡️")
        ));
        
        // Инструменты
        items.put("Инструменты", Arrays.asList(
            new ItemStat("Железная кирка", "Скорость", 6, 8, "⛏️"),
            new ItemStat("Алмазный топор", "Скорость", 8, 10, "🪓"),
            new ItemStat("Незеритовая лопата", "Скорость", 9, 12, "🔨")
        ));
        
        // Машины (моды)
        items.put("Машины", Arrays.asList(
            new ItemStat("Печь (Create)", "Скорость", 100, 200, "🔥"),
            new ItemStat("Дробилка (Create)", "Скорость", 150, 300, "⚙️"),
            new ItemStat("Генератор (Mekanism)", "Выход", 400, 800, "⚡"),
            new ItemStat("Обогатитель (Mekanism)", "Расход", 50, 100, "🔋")
        ));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> minecraft.setScreen(parent))
            .bounds(10, 10, 80, 20).build());
        
        // Category buttons
        String[] categories = {"Оружие", "Броня", "Инструменты", "Машины"};
        for (int i = 0; i < categories.length; i++) {
            String cat = categories[i];
            addRenderableWidget(Button.builder(Component.literal(cat), b -> {
                selectedCategory = cat;
                scrollOffset = 0;
            }).bounds(100 + i * 110, 10, 100, 20).build());
        }
        
        addRenderableWidget(Button.builder(Component.literal("Применить изменения"), b -> 
            NotificationManager.getInstance().success("Изменения применены!"))
            .bounds(width - 180, 10, 170, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        
        graphics.drawCenteredString(font, "⚖️ Балансировка предметов", width / 2, 45, 0xFFFFFF);
        graphics.drawString(font, "Категория: " + selectedCategory, 30, 70, 0xFFFF00);
        
        int y = 90;
        List<ItemStat> categoryItems = items.getOrDefault(selectedCategory, new ArrayList<>());
        
        for (ItemStat item : categoryItems) {
            if (y + 100 > height - 10) break;
            
            graphics.fill(20, y, width - 20, y + 95, 0xAA2A2A2A);
            graphics.drawString(font, item.icon + " " + item.name, 30, y + 10, 0xFFFFFF);
            graphics.drawString(font, item.stat + ":", 30, y + 30, 0xCCCCCC);
            
            // Current value
            graphics.drawString(font, "Текущее: " + item.current, 30, y + 50, 0x00FF00);
            
            // New value (editable)
            graphics.drawString(font, "Новое: " + item.newValue, 30, y + 70, 0xFFAA00);
            
            // Buttons
            int btnY = y + 50;
            graphics.fill(width - 150, btnY, width - 120, btnY + 15, 0xFF00AA00);
            graphics.drawCenteredString(font, "+", width - 135, btnY + 3, 0xFFFFFF);
            
            graphics.fill(width - 110, btnY, width - 80, btnY + 15, 0xFFAA0000);
            graphics.drawCenteredString(font, "-", width - 95, btnY + 3, 0xFFFFFF);
            
            graphics.fill(width - 70, btnY, width - 30, btnY + 15, 0xFF0088FF);
            graphics.drawCenteredString(font, "↺", width - 50, btnY + 3, 0xFFFFFF);
            
            y += 100;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int y = 90;
        List<ItemStat> categoryItems = items.getOrDefault(selectedCategory, new ArrayList<>());
        
        for (ItemStat item : categoryItems) {
            if (y + 100 > height - 10) break;
            int btnY = y + 50;
            
            // + button
            if (mouseX >= width - 150 && mouseX <= width - 120 && mouseY >= btnY && mouseY <= btnY + 15) {
                item.newValue = Math.min(item.newValue + 1, 999);
                NotificationManager.getInstance().info("Значение увеличено");
                return true;
            }
            
            // - button
            if (mouseX >= width - 110 && mouseX <= width - 80 && mouseY >= btnY && mouseY <= btnY + 15) {
                item.newValue = Math.max(item.newValue - 1, 1);
                NotificationManager.getInstance().info("Значение уменьшено");
                return true;
            }
            
            // Reset button
            if (mouseX >= width - 70 && mouseX <= width - 30 && mouseY >= btnY && mouseY <= btnY + 15) {
                item.newValue = item.current;
                NotificationManager.getInstance().info("Значение сброшено");
                return true;
            }
            
            y += 100;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static class ItemStat {
        String name, stat, icon;
        int current, newValue;
        
        ItemStat(String name, String stat, int current, int newValue, String icon) {
            this.name = name;
            this.stat = stat;
            this.current = current;
            this.newValue = newValue;
            this.icon = icon;
        }
    }
}
