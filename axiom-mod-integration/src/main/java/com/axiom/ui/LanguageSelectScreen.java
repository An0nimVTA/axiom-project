package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LanguageSelectScreen extends Screen {
    private final Screen parent;

    public LanguageSelectScreen(Screen parent) {
        super(Component.literal("Select Language"));
        this.parent = parent;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UiTheme.drawBackdrop(gfx, width, height);
        gfx.drawCenteredString(font, Component.literal("Выбор языка / Select language"), width / 2, 20, UiTheme.TEXT_PRIMARY);
        gfx.drawCenteredString(font,
            Component.literal("Можно изменить позже в меню / You can change later in the menu"),
            width / 2, 36, UiTheme.TEXT_MUTED);

        int cardWidth = getCardWidth();
        int cardHeight = getCardHeight();
        int cardGap = getCardGap();
        int totalWidth = cardWidth * 2 + cardGap;
        int startX = (width - totalWidth) / 2;
        int y = height / 2 - cardHeight / 2;

        renderCard(gfx, startX, y, cardWidth, cardHeight, UiLanguage.RU, mouseX, mouseY);
        renderCard(gfx, startX + cardWidth + cardGap, y, cardWidth, cardHeight, UiLanguage.EN, mouseX, mouseY);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderCard(GuiGraphics gfx, int x, int y, int w, int h, UiLanguage lang, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int accent = lang == UiLanguage.RU ? UiTheme.ACCENT : UiTheme.ACCENT_WARM;
        UiTheme.drawMinimalCard(gfx, x, y, x + w, y + h, accent, hovered);

        String title = lang == UiLanguage.RU ? "Русский" : "English";
        String subtitle = lang == UiLanguage.RU ? "Russian" : "English";
        int padding = 14;
        gfx.renderItem(UiIcons.resolveItem(lang == UiLanguage.RU ? "minecraft:written_book" : "minecraft:book",
            net.minecraft.world.item.Items.BOOK), x + padding, y + padding);

        gfx.drawString(font, title, x + padding, y + padding + 20, UiTheme.TEXT_PRIMARY, false);
        gfx.drawString(font, subtitle, x + padding, y + padding + 34, UiTheme.TEXT_MUTED, false);
        gfx.drawString(font, lang.getCode().toUpperCase(), x + w - padding - font.width(lang.getCode().toUpperCase()),
            y + padding, UiTheme.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cardWidth = getCardWidth();
        int cardHeight = getCardHeight();
        int cardGap = getCardGap();
        int totalWidth = cardWidth * 2 + cardGap;
        int startX = (width - totalWidth) / 2;
        int y = height / 2 - cardHeight / 2;

        if (isInside(mouseX, mouseY, startX, y, cardWidth, cardHeight)) {
            selectLanguage(UiLanguage.RU);
            return true;
        }
        if (isInside(mouseX, mouseY, startX + cardWidth + cardGap, y, cardWidth, cardHeight)) {
            selectLanguage(UiLanguage.EN);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private int getCardWidth() {
        float scale = UiLayout.scaleFor(width, height);
        return UiCardSizing.languageCardWidth(scale);
    }

    private int getCardHeight() {
        float scale = UiLayout.scaleFor(width, height);
        return UiCardSizing.languageCardHeight(scale);
    }

    private int getCardGap() {
        float scale = UiLayout.scaleFor(width, height);
        return UiCardSizing.languageCardGap(scale);
    }

    private void selectLanguage(UiLanguage language) {
        UiText.setLanguage(language, true);
        AxiomUiClientEvents.sendLanguageToServer(language.getCode());
        if (parent != null) {
            Minecraft.getInstance().setScreen(new MainMenuScreen());
        } else {
            MainMenuScreen.open();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
