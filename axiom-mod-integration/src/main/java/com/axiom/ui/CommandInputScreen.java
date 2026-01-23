package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CommandInputScreen extends Screen {
    private final Screen parent;
    private final CommandInfo command;
    private EditBox inputField;
    private Button executeButton;
    private Button cancelButton;

    public CommandInputScreen(Screen parent, CommandInfo command) {
        super(Component.literal("Выполнить команду"));
        this.parent = parent;
        this.command = command;
    }

    @Override
    protected void init() {
        super.init();
        
        // Input field
        inputField = new EditBox(font, width / 2 - 200, height / 2 - 10, 400, 20, Component.literal("Команда"));
        inputField.setMaxLength(256);
        inputField.setValue(command.getCommand());
        inputField.setFocused(true);
        addRenderableWidget(inputField);
        
        // Execute button
        executeButton = Button.builder(Component.literal("✓ Выполнить"), (btn) -> {
            String cmd = inputField.getValue();
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }
            Minecraft.getInstance().player.connection.sendCommand(cmd);
            NotificationManager.getInstance().success("Команда выполнена: /" + cmd);
            Minecraft.getInstance().setScreen(parent);
        })
        .bounds(width / 2 - 105, height / 2 + 30, 100, 20)
        .build();
        addRenderableWidget(executeButton);
        
        // Cancel button
        cancelButton = Button.builder(Component.literal("✗ Отмена"), (btn) -> {
            Minecraft.getInstance().setScreen(parent);
        })
        .bounds(width / 2 + 5, height / 2 + 30, 100, 20)
        .build();
        addRenderableWidget(cancelButton);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        
        int centerX = width / 2;
        int categoryColor = command.getCategory().getColor();
        
        // Title
        gfx.drawCenteredString(font, Component.literal("§l" + command.getDisplayName()), centerX, height / 2 - 60, 0xFFFFFFFF);
        
        // Description
        gfx.drawCenteredString(font, Component.literal("§7" + command.getShortDesc()), centerX, height / 2 - 45, 0xFFAAAAAA);
        
        // Input label
        gfx.drawString(font, "§eВведите параметры команды:", centerX - 200, height / 2 - 25, 0xFFFFAA00, false);
        
        // Help text
        if (!command.getExamples().isEmpty()) {
            gfx.drawString(font, "§7Пример: §a" + command.getExamples().get(0), 
                centerX - 200, height / 2 + 60, 0xFF00FF00, false);
        }
        
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter or Numpad Enter
            executeButton.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
