package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ReligionDetailScreen extends Screen {
    private final Screen parent;
    private final ReligionCard card;

    public ReligionDetailScreen(Screen parent, ReligionCard card) {
        super(Component.literal(card.getName()));
        this.parent = parent;
        this.card = card;
    }

    @Override
    protected void init() {
        int buttonWidth = 80;
        int buttonHeight = 20;
        int x = 12;
        int y = height - buttonHeight - 12;
        addRenderableWidget(Button.builder(Component.literal("Назад"), btn -> Minecraft.getInstance().setScreen(parent))
            .bounds(x, y, buttonWidth, buttonHeight)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        UiTheme.drawBackdrop(guiGraphics, width, height);

        guiGraphics.drawCenteredString(font, title, width / 2, 12, UiTheme.TEXT_PRIMARY);

        int y = 40;
        int x = 20;
        for (String line : card.getDetails()) {
            guiGraphics.drawString(font, line, x, y, UiTheme.TEXT_PRIMARY, false);
            y += font.lineHeight + 4;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
