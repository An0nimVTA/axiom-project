package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public class RecipeEditorScreen extends Screen {
    private final Screen parent;
    private int selectedRecipe = 0;
    
    private final List<Recipe> recipes = Arrays.asList(
        new Recipe("Железный меч", 
            Arrays.asList("Железо x2", "Палка x1"),
            Arrays.asList("Железо x3", "Палка x2", "Алмаз x1")),
        new Recipe("Алмазная кирка",
            Arrays.asList("Алмаз x3", "Палка x2"),
            Arrays.asList("Алмаз x5", "Палка x3", "Незерит x1")),
        new Recipe("Печь (Create)",
            Arrays.asList("Булыжник x8"),
            Arrays.asList("Булыжник x16", "Железо x4", "Редстоун x2")),
        new Recipe("Генератор (Mekanism)",
            Arrays.asList("Железо x4", "Редстоун x2"),
            Arrays.asList("Железо x8", "Золото x4", "Алмаз x2", "Редстоун x4"))
    );

    public RecipeEditorScreen(Screen parent) {
        super(Component.literal("Редактор крафтов"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> minecraft.setScreen(parent))
            .bounds(10, 10, 80, 20).build());
        
        addRenderableWidget(Button.builder(Component.literal("◀ Пред."), b -> {
            selectedRecipe = Math.max(0, selectedRecipe - 1);
        }).bounds(width / 2 - 110, height - 40, 100, 20).build());
        
        addRenderableWidget(Button.builder(Component.literal("След. ▶"), b -> {
            selectedRecipe = Math.min(recipes.size() - 1, selectedRecipe + 1);
        }).bounds(width / 2 + 10, height - 40, 100, 20).build());
        
        addRenderableWidget(Button.builder(Component.literal("Сохранить крафт"), b -> 
            NotificationManager.getInstance().success("Крафт сохранён!"))
            .bounds(width - 180, 10, 170, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        
        graphics.drawCenteredString(font, "📝 Редактор крафтов", width / 2, 20, 0xFFFFFF);
        
        Recipe recipe = recipes.get(selectedRecipe);
        graphics.drawCenteredString(font, "Крафт: " + recipe.name, width / 2, 45, 0xFFFF00);
        graphics.drawCenteredString(font, (selectedRecipe + 1) + " / " + recipes.size(), width / 2, 60, 0xCCCCCC);
        
        int y = 90;
        int leftX = 50;
        int rightX = width / 2 + 50;
        
        // Original recipe
        graphics.fill(leftX - 10, y, leftX + 250, y + 200, 0xAA1E3A5F);
        graphics.drawString(font, "Оригинальный крафт:", leftX, y + 10, 0xFFFFFF);
        
        int itemY = y + 35;
        for (String item : recipe.original) {
            graphics.drawString(font, "• " + item, leftX + 10, itemY, 0x00FF00);
            itemY += 20;
        }
        
        // New recipe
        graphics.fill(rightX - 10, y, rightX + 250, y + 200, 0xAA5F1E3A);
        graphics.drawString(font, "Новый крафт:", rightX, y + 10, 0xFFFFFF);
        
        itemY = y + 35;
        for (String item : recipe.modified) {
            graphics.drawString(font, "• " + item, rightX + 10, itemY, 0xFFAA00);
            itemY += 20;
        }
        
        // Comparison
        y += 210;
        graphics.fill(50, y, width - 50, y + 60, 0xAA2A2A2A);
        graphics.drawString(font, "Изменения:", 60, y + 10, 0xFFFFFF);
        
        int diff = recipe.modified.size() - recipe.original.size();
        String diffText = diff > 0 ? "Сложнее на " + diff + " ингредиента" : 
                         diff < 0 ? "Проще на " + Math.abs(diff) + " ингредиента" : "Без изменений";
        graphics.drawString(font, diffText, 60, y + 30, diff > 0 ? 0xFF0000 : diff < 0 ? 0x00FF00 : 0xFFFF00);
    }

    static class Recipe {
        String name;
        List<String> original, modified;
        
        Recipe(String name, List<String> original, List<String> modified) {
            this.name = name;
            this.original = original;
            this.modified = modified;
        }
    }
}
