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
            UiText.text("Поиск команд...", "Search commands...")
        );
        this.searchField.setBordered(false);
        searchField.setHint(UiText.text("Введите название команды...", "Type a command name..."));
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
        int x0 = searchField.getX();
        int y0 = searchField.getY();
        int x1 = x0 + searchField.getWidth();
        int y1 = y0 + searchField.getHeight();
        gfx.fill(x0, y0, x1, y1, UiTheme.BUTTON_BG);
        gfx.renderOutline(x0, y0, x1 - x0, y1 - y0, UiTheme.BUTTON_BORDER);
        searchField.render(gfx, mouseX, mouseY, partialTick);
        
        // Results count
        if (!searchField.getValue().isEmpty()) {
            String resultsText = UiText.pick("§7Найдено: §f", "§7Found: §f") + filteredCommands.size();
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
