package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CommandCardWidget {
    public static final int WIDTH = 420;
    public static final int HEIGHT = 80;
    
    private final CommandInfo command;
    private int x, y;
    private final Button executeBtn;
    private final Button detailsBtn;
    private final Button favoriteBtn;
    private boolean isFavorite;

    public CommandCardWidget(int x, int y, CommandInfo command, Runnable onExecute, Runnable onDetails, Runnable onFavorite) {
        this.x = x;
        this.y = y;
        this.command = command;
        this.isFavorite = false;
        
        // Execute button
        this.executeBtn = Button.builder(Component.literal("▶ Выполнить"), (b) -> onExecute.run())
            .bounds(x + 280, y + 15, 120, 20)
            .build();
            
        // Details button
        this.detailsBtn = Button.builder(Component.literal("ℹ Подробнее"), (b) -> onDetails.run())
            .bounds(x + 280, y + 45, 120, 20)
            .build();
        
        // Favorite button (small star)
        this.favoriteBtn = Button.builder(Component.literal("★"), (b) -> {
            onFavorite.run();
            isFavorite = !isFavorite;
        })
            .bounds(x + 250, y + 30, 20, 20)
            .build();
    }
    
    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int categoryColor = command.getCategory().getColor();
        int bgColor = (categoryColor & 0x00FFFFFF) | 0x40000000; // Semi-transparent
        
        // Background with category color
        gfx.fill(x, y, x + WIDTH, y + HEIGHT, bgColor);
        
        // Left colored stripe (category indicator)
        gfx.fill(x, y, x + 5, y + HEIGHT, categoryColor);
        
        // Border
        gfx.renderOutline(x, y, WIDTH, HEIGHT, categoryColor);
        
        // Category badge (top-right)
        String categoryName = command.getCategory().getDisplayName();
        int badgeWidth = Minecraft.getInstance().font.width(categoryName) + 10;
        gfx.fill(x + WIDTH - badgeWidth - 5, y + 5, x + WIDTH - 5, y + 18, categoryColor);
        gfx.drawString(Minecraft.getInstance().font, categoryName, 
            x + WIDTH - badgeWidth, y + 8, 0xFFFFFFFF, false);
        
        // Favorite star indicator (left side)
        if (isFavorite) {
            gfx.drawString(Minecraft.getInstance().font, "§e★", x + 10, y + 10, 0xFFFFAA00, false);
        }
        
        // Command title (bold)
        gfx.drawString(Minecraft.getInstance().font, 
            Component.literal("§l" + command.getDisplayName()), 
            x + (isFavorite ? 25 : 15), y + 10, 0xFFFFFFFF, false);
        
        // Command syntax (gray)
        gfx.drawString(Minecraft.getInstance().font, 
            Component.literal("§7" + command.getCommand()), 
            x + 15, y + 25, 0xFFAAAAAA, false);
        
        // Short description
        gfx.drawString(Minecraft.getInstance().font, 
            command.getShortDesc(), 
            x + 15, y + 40, 0xFFCCCCCC, false);
        
        // Requirements indicator
        if (command.requiresNation()) {
            gfx.drawString(Minecraft.getInstance().font, 
                Component.literal("§e⚠ Требует нацию"), 
                x + 15, y + 55, 0xFFFFAA00, false);
        }
        
        // Render buttons
        executeBtn.render(gfx, mouseX, mouseY, partialTick);
        detailsBtn.render(gfx, mouseX, mouseY, partialTick);
        
        // Render favorite button with color
        int oldColor = favoriteBtn.getMessage().getStyle().getColor() != null ? 
            favoriteBtn.getMessage().getStyle().getColor().getValue() : 0xFFFFFFFF;
        favoriteBtn.setMessage(Component.literal(isFavorite ? "§e★" : "§7☆"));
        favoriteBtn.render(gfx, mouseX, mouseY, partialTick);
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (favoriteBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (executeBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (detailsBtn.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        executeBtn.setPosition(x + 280, y + 15);
        detailsBtn.setPosition(x + 280, y + 45);
        favoriteBtn.setPosition(x + 250, y + 30);
    }
    
    public int getY() {
        return y;
    }
}
