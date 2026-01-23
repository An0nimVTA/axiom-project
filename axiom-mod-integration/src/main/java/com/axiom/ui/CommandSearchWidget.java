package com.axiom.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CommandSearchWidget {
    private final EditBox searchField;
    private final List<CommandInfo> allCommands;
    private List<CommandInfo> filteredCommands;

    public CommandSearchWidget(int x, int y, int width, List<CommandInfo> commands) {
        this.allCommands = commands;
        this.filteredCommands = new ArrayList<>(commands);
        
        this.searchField = new EditBox(
            net.minecraft.client.Minecraft.getInstance().font,
            x, y, width, 20,
            Component.literal("Поиск команд...")
        );
        searchField.setHint(Component.literal("🔍 Введите название команды..."));
        searchField.setResponder(this::onSearchChanged);
    }

    private void onSearchChanged(String query) {
        if (query.isEmpty()) {
            filteredCommands = new ArrayList<>(allCommands);
            return;
        }
        
        String lowerQuery = query.toLowerCase();
        filteredCommands = allCommands.stream()
            .filter(cmd -> 
                cmd.getDisplayName().toLowerCase().contains(lowerQuery) ||
                cmd.getCommand().toLowerCase().contains(lowerQuery) ||
                cmd.getShortDesc().toLowerCase().contains(lowerQuery) ||
                cmd.getCategory().getDisplayName().toLowerCase().contains(lowerQuery)
            )
            .toList();
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        searchField.render(gfx, mouseX, mouseY, partialTick);
        
        // Results count
        if (!searchField.getValue().isEmpty()) {
            String resultsText = "§7Найдено: §f" + filteredCommands.size();
            gfx.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                resultsText,
                searchField.getX() + searchField.getWidth() + 10,
                searchField.getY() + 6,
                0xFFFFFFFF,
                false
            );
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return searchField.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return searchField.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return searchField.charTyped(codePoint, modifiers);
    }

    public List<CommandInfo> getFilteredCommands() {
        return filteredCommands;
    }

    public EditBox getSearchField() {
        return searchField;
    }

    public void setFocused(boolean focused) {
        searchField.setFocused(focused);
    }
}
