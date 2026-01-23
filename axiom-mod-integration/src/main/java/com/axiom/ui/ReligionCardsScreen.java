package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ReligionCardsScreen extends Screen {
    private static final int CARD_WIDTH = 220;
    private static final int CARD_HEIGHT = 56;
    private static final int CARD_GAP = 10;
    private static final int DETAIL_HEIGHT = 14;

    private final List<ReligionCard> cards;
    private final List<CardEntry> entries = new ArrayList<>();
    private int scrollOffset;
    private int maxScroll;

    public ReligionCardsScreen() {
        super(Component.literal("Религии"));
        this.cards = new ArrayList<>(ReligionCatalog.get());
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    private void rebuildLayout() {
        entries.clear();
        scrollOffset = 0;

        int padding = 24;
        int availableWidth = Math.max(0, width - padding * 2);
        int columns = Math.max(1, Math.min(3, (availableWidth + CARD_GAP) / (CARD_WIDTH + CARD_GAP)));
        int rows = (int) Math.ceil(cards.size() / (double) columns);

        int totalWidth = columns * CARD_WIDTH + (columns - 1) * CARD_GAP;
        int startX = Math.max(padding, (width - totalWidth) / 2);
        int startY = 32;

        for (int i = 0; i < cards.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (CARD_WIDTH + CARD_GAP);
            int y = startY + row * (CARD_HEIGHT + CARD_GAP);
            entries.add(new CardEntry(cards.get(i), x, y));
        }

        int totalHeight = rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
        int viewHeight = height - startY - 24;
        maxScroll = Math.max(0, totalHeight - viewHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0) {
            return false;
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (delta * 12), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CardEntry entry : entries) {
            int drawY = entry.y - scrollOffset;
            if (entry.contains(mouseX, mouseY, drawY)) {
                if (entry.isDetailsClick(mouseY, drawY)) {
                    Minecraft.getInstance().setScreen(new ReligionDetailScreen(this, entry.card));
                    return true;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0xFF000000);

        if (entries.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.literal("Нет данных о религиях"), width / 2, height / 2, 0xFFFFFFFF);
            return;
        }

        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(font, Component.literal("Низ карточки — подробности"), width / 2, height - 12, 0xFFAAAAAA);

        for (CardEntry entry : entries) {
            int drawY = entry.y - scrollOffset;
            if (drawY + CARD_HEIGHT < 0 || drawY > height) {
                continue;
            }
            boolean hovered = entry.contains(mouseX, mouseY, drawY);
            int borderColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;

            guiGraphics.fill(entry.x, drawY, entry.x + CARD_WIDTH, drawY + CARD_HEIGHT, 0xFF000000);
            guiGraphics.renderOutline(entry.x, drawY, CARD_WIDTH, CARD_HEIGHT, borderColor);

            int titleX = entry.x + (CARD_WIDTH - font.width(entry.card.getName())) / 2;
            guiGraphics.drawString(font, entry.card.getName(), titleX, drawY + 10, 0xFFFFFFFF, false);

            int lineY = drawY + CARD_HEIGHT - DETAIL_HEIGHT;
            guiGraphics.hLine(entry.x + 8, entry.x + CARD_WIDTH - 8, lineY, 0xFF666666);

            Component detail = Component.literal("Подробнее");
            int detailX = entry.x + (CARD_WIDTH - font.width(detail)) / 2;
            guiGraphics.drawString(font, detail, detailX, lineY + 2, 0xFFFFFFFF, false);
        }
    }

    private static final class CardEntry {
        private final ReligionCard card;
        private final int x;
        private final int y;

        private CardEntry(ReligionCard card, int x, int y) {
            this.card = card;
            this.x = x;
            this.y = y;
        }

        private boolean contains(double mouseX, double mouseY, int drawY) {
            return mouseX >= x && mouseX <= x + CARD_WIDTH && mouseY >= drawY && mouseY <= drawY + CARD_HEIGHT;
        }

        private boolean isDetailsClick(double mouseY, int drawY) {
            return mouseY >= drawY + CARD_HEIGHT - DETAIL_HEIGHT;
        }
    }
}
