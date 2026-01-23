package com.axiom.ui.tech;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class RecipeChainScreen extends Screen {
    private final Screen parent;
    private final String itemName;
    private final ItemStack icon;

    public RecipeChainScreen(Screen parent, String itemName, ItemStack icon) {
        super(Component.literal("Цепочка крафта: " + itemName));
        this.parent = parent;
        this.itemName = itemName;
        this.icon = icon;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        
        // Title
        gfx.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        
        // Central Item
        int centerX = width / 2;
        int centerY = height / 2;
        gfx.renderItem(icon, centerX - 8, centerY - 8);
        
        // Placeholder for the tree visualization
        // (In a real implementation, this would recursively render ingredients)
        gfx.drawCenteredString(font, "§7[Здесь будет полное древо крафта от сырых ресурсов]", centerX, centerY + 20, 0xFFAAAAAA);
        
        // Back Button Hint
        gfx.drawCenteredString(font, "Нажмите ESC для возврата", width / 2, height - 20, 0xFF555555);
        
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
