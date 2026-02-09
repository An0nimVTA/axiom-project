package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class CommandCardWidget {
    public static final int DEFAULT_WIDTH = 220;
    public static final int DEFAULT_HEIGHT = 280;
    
    private final CommandInfo command;
    private final int width;
    private final int height;
    private int x, y;
    private final Runnable onExecute;
    private final Runnable onDetails;
    private final Runnable onFavorite;
    private boolean isFavorite;

    public CommandCardWidget(int x, int y, int width, int height, CommandInfo command, Runnable onExecute, Runnable onDetails, Runnable onFavorite) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.command = command;
        this.isFavorite = false;
        this.onExecute = onExecute;
        this.onDetails = onDetails;
        this.onFavorite = onFavorite;
    }
    
    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int categoryColor = command.getCategory().getColor();
        boolean compact = height < 140 || width < 140;
        float scale = Mth.clamp(height / 260.0f, 0.35f, 1.2f);
        int padding = Math.max(6, Math.round(14 * scale));
        boolean hovered = isHovered(mouseX, mouseY);

        if (hovered) {
            gfx.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x16000000);
        }

        UiTheme.drawMinimalCard(gfx, x, y, x + width, y + height, categoryColor, hovered);

        int iconSize = Math.max(12, Math.round(Math.min(width, height) * (compact ? 0.42f : 0.16f)));
        int iconX = x + padding;
        int iconY = y + padding;
        ItemStack icon = UiIcons.resolve(command);
        gfx.renderItem(icon, iconX, iconY);

        if (compact) {
            int titleX = iconX + iconSize + 6;
            int titleY = iconY + Math.max(0, (iconSize - Minecraft.getInstance().font.lineHeight) / 2);
            String titleLine = Minecraft.getInstance().font.plainSubstrByWidth(command.getDisplayName(),
                width - (titleX - x) - padding);
            gfx.drawString(Minecraft.getInstance().font, titleLine, titleX, titleY, UiTheme.TEXT_PRIMARY, false);
            return;
        }

        int textX = x + padding;
        int titleY = iconY + iconSize + 10;
        String titleLine = Minecraft.getInstance().font.plainSubstrByWidth(command.getDisplayName(), width - padding * 2);
        gfx.drawString(Minecraft.getInstance().font, titleLine, textX, titleY, UiTheme.TEXT_PRIMARY, false);

        int cmdY = titleY + 12;
        String cmdLine = Minecraft.getInstance().font.plainSubstrByWidth(command.getCommand(), width - padding * 2);
        gfx.drawString(Minecraft.getInstance().font, cmdLine, textX, cmdY, UiTheme.TEXT_DIM, false);

        int descWidth = width - padding * 2;
        List<FormattedCharSequence> lines = Minecraft.getInstance().font
            .split(Component.literal(command.getShortDesc()), Math.max(10, descWidth));
        int lineY = cmdY + 12;
        int maxLines = Math.min(2, lines.size());
        for (int i = 0; i < maxLines; i++) {
            gfx.drawString(Minecraft.getInstance().font, lines.get(i), textX, lineY, UiTheme.TEXT_MUTED, false);
            lineY += 10;
        }

        List<String> tags = new java.util.ArrayList<>();
        if (command.getRarity() != UiRarity.COMMON) {
            tags.add(rarityLabel(command.getRarity()));
        }
        if (command.requiresNation()) {
            tags.add(UiText.pick("Нужна нация", "Nation required"));
        }
        if (!tags.isEmpty()) {
            String tagLine = String.join(" · ", tags);
            int tagY = y + height - padding - 9;
            gfx.drawString(Minecraft.getInstance().font, tagLine, textX, tagY, UiTheme.TEXT_DIM, false);
        }

        if (isFavorite) {
            gfx.drawString(Minecraft.getInstance().font, "★", x + width - 18, y + 6, UiTheme.ACCENT_WARM, false);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }
        float scale = Mth.clamp(height / 220.0f, 0.85f, 1.25f);
        int favoriteX = x + width - 22;
        int favoriteY = y + 6;
        if (mouseX >= favoriteX && mouseX <= favoriteX + 12 && mouseY >= favoriteY && mouseY <= favoriteY + 12) {
            onFavorite.run();
            isFavorite = !isFavorite;
            return true;
        }
        if (onExecute != null) {
            onExecute.run();
            return true;
        }
        return true;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public CommandInfo getCommand() {
        return command;
    }
    
    public int getY() {
        return y;
    }

    public void openDetails() {
        onDetails.run();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private String rarityLabel(UiRarity rarity) {
        return switch (rarity) {
            case RARE -> UiText.pick("Редкая", "Rare");
            case LEGENDARY -> UiText.pick("Легендарная", "Legendary");
            default -> UiText.pick("Обычная", "Common");
        };
    }
}
