package com.axiom.ui.tech;

import com.axiom.ui.UiCardSizing;
import com.axiom.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class HorizontalCardWidget {
    public static final int WIDTH = UiCardSizing.TECH_HORIZONTAL_W;
    public static final int HEIGHT = UiCardSizing.TECH_HORIZONTAL_H;
    private static final int BUTTON_WIDTH = UiCardSizing.TECH_HORIZONTAL_BTN_W;
    private static final int BUTTON_HEIGHT = UiCardSizing.TECH_HORIZONTAL_BTN_H;
    
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
            .bounds(x + WIDTH - BUTTON_WIDTH - 8, y + 6, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
            
        // Button 2: Deep Description
        this.infoBtn = Button.builder(Component.literal("Инфо"), (b) -> onInfo.run())
            .bounds(x + WIDTH - BUTTON_WIDTH - 8, y + 26, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();

        // Button 3: Balance Editor (Admin/Dev)
        this.balanceBtn = Button.builder(Component.literal("⚙ Баланс"), (b) -> onBalance.run())
            .bounds(x + WIDTH - BUTTON_WIDTH - 8, y + 46, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        boolean compact = WIDTH < 160 || HEIGHT < 40;
        UiTheme.drawMinimalCard(gfx, x, y, x + WIDTH, y + HEIGHT, UiTheme.ACCENT, false);
        
        // 3. Icon (Left)
        gfx.renderItem(icon, x + 8, y + Math.max(4, (HEIGHT - 16) / 2));
        
        // 4. Text (Title & Short Desc)
        int textX = x + 30;
        int textY = y + 8;
        String titleLine = Minecraft.getInstance().font.plainSubstrByWidth(title, WIDTH - textX - 8);
        gfx.drawString(Minecraft.getInstance().font, Component.literal(titleLine), textX, textY, UiTheme.TEXT_PRIMARY, false);
        if (!compact) {
            gfx.drawString(Minecraft.getInstance().font, description, textX, y + 26, UiTheme.TEXT_MUTED, false);
        }
        
        // 5. Render Buttons
        if (!compact) {
            recipeBtn.render(gfx, mouseX, mouseY, partialTick);
            infoBtn.render(gfx, mouseX, mouseY, partialTick);
            balanceBtn.render(gfx, mouseX, mouseY, partialTick);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean compact = WIDTH < 160 || HEIGHT < 40;
        if (compact) {
            return false;
        }
        if (recipeBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (infoBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (balanceBtn.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }
}
