package com.axiom.ui.tech;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class HorizontalCardWidget {
    public static final int WIDTH = 300;
    public static final int HEIGHT = 60;
    
    private final String title;
    private final String description;
    private final ItemStack icon;
    private final Button recipeBtn;
    private final Button infoBtn;
    private final Button balanceBtn;
    
    private int x, y;

    public HorizontalCardWidget(int x, int y, String title, String desc, ItemStack icon, Runnable onRecipe, Runnable onInfo, Runnable onBalance) {
        this.x = x;
        this.y = y;
        this.title = title;
        this.description = desc;
        this.icon = icon;
        
        // Button 1: Recipe Path
        this.recipeBtn = Button.builder(Component.literal("Крафт"), (b) -> onRecipe.run())
            .bounds(x + 220, y + 5, 70, 16)
            .build();
            
        // Button 2: Deep Description
        this.infoBtn = Button.builder(Component.literal("Инфо"), (b) -> onInfo.run())
            .bounds(x + 220, y + 23, 70, 16)
            .build();

        // Button 3: Balance Editor (Admin/Dev)
        this.balanceBtn = Button.builder(Component.literal("⚙ Баланс"), (b) -> onBalance.run())
            .bounds(x + 220, y + 41, 70, 16)
            .build();
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 1. Black Background
        gfx.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF000000);
        
        // 2. White Border (Outline)
        gfx.renderOutline(x, y, WIDTH, HEIGHT, 0xFFFFFFFF);
        
        // 3. Icon (Left)
        gfx.renderItem(icon, x + 10, y + 22);
        
        // 4. Text (Title & Short Desc)
        gfx.drawString(Minecraft.getInstance().font, Component.literal("§l" + title), x + 40, y + 10, 0xFFFFFFFF, false);
        gfx.drawString(Minecraft.getInstance().font, description, x + 40, y + 25, 0xFFAAAAAA, false);
        
        // 5. Render Buttons
        recipeBtn.render(gfx, mouseX, mouseY, partialTick);
        infoBtn.render(gfx, mouseX, mouseY, partialTick);
        balanceBtn.render(gfx, mouseX, mouseY, partialTick);
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (recipeBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (infoBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (balanceBtn.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }
}
