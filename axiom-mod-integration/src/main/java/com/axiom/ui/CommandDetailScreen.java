package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CommandDetailScreen extends Screen {
    private final Screen parent;
    private final CommandInfo command;
    private Button backButton;
    private Button executeButton;

    public CommandDetailScreen(Screen parent, CommandInfo command) {
        super(Component.literal(command.getDisplayName()));
        this.parent = parent;
        this.command = command;
    }

    @Override
    protected void init() {
        super.init();
        
        // Back button
        backButton = Button.builder(UiText.text("Назад", "Back"), (btn) -> {
            Minecraft.getInstance().setScreen(parent);
        })
        .bounds(width / 2 - 210, height - 40, 100, 20)
        .build();
        addRenderableWidget(backButton);
        
        // Execute button
        executeButton = Button.builder(UiText.text("Выполнить", "Execute"), (btn) -> {
            Minecraft.getInstance().setScreen(new CommandInputScreen(parent, command));
        })
        .bounds(width / 2 + 110, height - 40, 100, 20)
        .build();
        addRenderableWidget(executeButton);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UiTheme.drawBackdrop(gfx, width, height);
        gfx.fill(0, 0, width, 38, UiTheme.withAlpha(UiTheme.PANEL, 0xE0));
        gfx.fill(0, 36, width, 38, UiTheme.PANEL_BORDER);
        
        int centerX = width / 2;
        int y = 40;
        int categoryColor = command.getCategory().getColor();
        
        // Category badge
        String categoryName = command.getCategory().getDisplayName();
        int badgeWidth = font.width(categoryName) + 20;
        gfx.fill(centerX - badgeWidth / 2, y, centerX + badgeWidth / 2, y + 20, categoryColor);
        gfx.drawCenteredString(font, categoryName, centerX, y + 6, UiTheme.TEXT_PRIMARY);
        y += 35;
        
        // Title
        gfx.drawCenteredString(font, Component.literal("§l" + command.getDisplayName()), centerX, y, UiTheme.TEXT_PRIMARY);
        y += 25;
        
        // Command syntax (in box)
        int boxWidth = 500;
        int boxHeight = 30;
        UiTheme.drawPanel(gfx, centerX - boxWidth / 2, y, centerX + boxWidth / 2, y + boxHeight);
        gfx.renderOutline(centerX - boxWidth / 2, y, boxWidth, boxHeight, categoryColor);
        gfx.drawCenteredString(font, Component.literal("§e" + command.getCommand()), centerX, y + 10, UiTheme.ACCENT_WARM);
        y += boxHeight + 20;
        
        // Full description
        gfx.drawString(font, UiText.pick("§lОписание:", "§lDescription:"), centerX - 250, y, UiTheme.TEXT_PRIMARY, false);
        y += 15;
        
        // Word wrap description
        String[] words = command.getFullDesc().split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (font.width(line + word) > 480) {
                gfx.drawString(font, line.toString(), centerX - 240, y, 0xFFB7C0CB, false);
                y += 12;
                line = new StringBuilder(word + " ");
            } else {
                line.append(word).append(" ");
            }
        }
        if (line.length() > 0) {
            gfx.drawString(font, line.toString(), centerX - 240, y, 0xFFB7C0CB, false);
            y += 20;
        }
        
        // Aliases
        if (!command.getAliases().isEmpty()) {
            y += 10;
            gfx.drawString(font, UiText.pick("§lАльтернативные команды:", "§lAliases:"), centerX - 250, y, UiTheme.TEXT_PRIMARY, false);
            y += 15;
            for (String alias : command.getAliases()) {
                gfx.drawString(font, "  §7• §e" + alias, centerX - 240, y, 0xFFFFAA00, false);
                y += 12;
            }
        }
        
        // Examples
        if (!command.getExamples().isEmpty()) {
            y += 10;
            gfx.drawString(font, UiText.pick("§lПримеры использования:", "§lExamples:"), centerX - 250, y, UiTheme.TEXT_PRIMARY, false);
            y += 15;
            for (String example : command.getExamples()) {
                gfx.drawString(font, "  §7• §a" + example, centerX - 240, y, 0xFF00FF00, false);
                y += 12;
            }
        }
        
        // Requirements
        y += 10;
        gfx.drawString(font, UiText.pick("§lТребования:", "§lRequirements:"), centerX - 250, y, UiTheme.TEXT_PRIMARY, false);
        y += 15;
        
        if (command.requiresNation()) {
            gfx.drawString(font, UiText.pick("  §7• §eДолжны состоять в нации", "  §7• §eMust be in a nation"), centerX - 240, y, 0xFFFFAA00, false);
            y += 12;
        }
        
        if (command.getPermission() != null) {
            gfx.drawString(font, UiText.pick("  §7• §7Права: §c", "  §7• §7Permission: §c") + command.getPermission(), centerX - 240, y, 0xFFFF5555, false);
            y += 12;
        }
        
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
