package com.axiom.ui.tech;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemBalanceScreen extends Screen {
    private final Screen parent;
    private final String itemName;
    private final ItemStack icon;
    
    private EditBox damageField;
    private EditBox speedField;
    private EditBox durabilityField;

    public ItemBalanceScreen(Screen parent, String itemName, ItemStack icon) {
        super(Component.literal("Баланс предмета: " + itemName));
        this.parent = parent;
        this.itemName = itemName;
        this.icon = icon;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = 60;
        
        // Damage Input
        this.damageField = new EditBox(font, centerX - 50, startY, 100, 20, Component.literal("Урон"));
        this.damageField.setValue("10.0"); // Mock current value
        addRenderableWidget(damageField);
        
        // Speed Input
        this.speedField = new EditBox(font, centerX - 50, startY + 30, 100, 20, Component.literal("Скорость"));
        this.speedField.setValue("1.5");
        addRenderableWidget(speedField);
        
        // Durability Input
        this.durabilityField = new EditBox(font, centerX - 50, startY + 60, 100, 20, Component.literal("Прочность"));
        this.durabilityField.setValue("500");
        addRenderableWidget(durabilityField);
        
        // Save Button
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), (b) -> {
            // Logic to send packet to server to update item stats
            System.out.println("Saving stats for " + itemName);
            onClose();
        }).bounds(centerX - 50, startY + 100, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        
        gfx.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        gfx.renderItem(icon, width / 2 - 8, 40);
        
        // Labels
        gfx.drawString(font, "Урон:", width / 2 - 90, 66, 0xFFFFFFFF);
        gfx.drawString(font, "Скорость:", width / 2 - 100, 96, 0xFFFFFFFF);
        gfx.drawString(font, "Прочность:", width / 2 - 105, 126, 0xFFFFFFFF);
        
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
