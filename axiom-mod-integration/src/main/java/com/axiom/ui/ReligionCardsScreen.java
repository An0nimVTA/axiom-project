package com.axiom.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class ReligionCardsScreen extends Screen {
    private int cardWidth = 200;
    private int cardHeight = 220;
    private int cardGap = 12;

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

        float scale = UiLayout.scaleFor(width, height);
        int padding = UiLayout.scaled(24, scale);
        int availableWidth = Math.max(0, width - padding * 2);
        cardWidth = UiCardSizing.religionCardWidth(scale, availableWidth);
        cardHeight = UiCardSizing.religionCardHeight(scale);
        cardGap = UiCardSizing.religionGap(scale);
        int columns = Math.max(1, (availableWidth + cardGap) / (cardWidth + cardGap));
        int rows = (int) Math.ceil(cards.size() / (double) columns);

        int totalWidth = columns * cardWidth + (columns - 1) * cardGap;
        int startX = Math.max(padding, (width - totalWidth) / 2);
        int startY = UiCardSizing.religionStartY(scale);

        for (int i = 0; i < cards.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (cardWidth + cardGap);
            int y = startY + row * (cardHeight + cardGap);
            entries.add(new CardEntry(cards.get(i), x, y));
        }

        int totalHeight = rows * cardHeight + Math.max(0, rows - 1) * cardGap;
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
                Minecraft.getInstance().setScreen(new ReligionDetailScreen(this, entry.card));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        UiTheme.drawBackdrop(guiGraphics, width, height);

        if (entries.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.literal("Нет данных о религиях"), width / 2, height / 2, UiTheme.TEXT_PRIMARY);
            return;
        }

        guiGraphics.drawCenteredString(font, title, width / 2, 12, UiTheme.TEXT_PRIMARY);
        guiGraphics.drawCenteredString(font, Component.literal("Выберите религию"), width / 2, 26, UiTheme.TEXT_MUTED);

        for (CardEntry entry : entries) {
            int drawY = entry.y - scrollOffset;
            if (drawY + cardHeight < 0 || drawY > height) {
                continue;
            }
            boolean hovered = entry.contains(mouseX, mouseY, drawY);
            int accent = resolveAccent(entry.card);
            UiTheme.drawMinimalCard(guiGraphics, entry.x, drawY, entry.x + cardWidth, drawY + cardHeight, accent, hovered);

            boolean compact = cardHeight < 140 || cardWidth < 140;
            float scale = Mth.clamp(cardHeight / 240.0f, 0.35f, 1.2f);
            int padding = Math.max(6, Math.round(14 * scale));
            int textX = entry.x + padding;
            int textY = drawY + padding;

            String symbol = entry.card.getSymbol();
            if (symbol == null || symbol.isBlank()) {
                symbol = entry.card.getName().isEmpty() ? "?" : entry.card.getName().substring(0, 1);
            }
            guiGraphics.drawString(font, symbol, textX, textY, UiTheme.TEXT_DIM, false);

            String name = entry.card.getName();
            String nameLine = font.plainSubstrByWidth(name, cardWidth - padding * 2);
            int nameY = textY + (compact ? 8 : 16);
            guiGraphics.drawString(font, nameLine, textX, nameY, UiTheme.TEXT_PRIMARY, false);

            if (!compact) {
                String tagline = entry.card.getTagline();
                if (tagline != null && !tagline.isBlank()) {
                    String cleanTagline = tagline.replace('|', ' ');
                    List<FormattedCharSequence> lines = font.split(Component.literal(cleanTagline), cardWidth - padding * 2);
                    int lineY = textY + 32;
                    int maxLines = Math.min(3, lines.size());
                    for (int i = 0; i < maxLines; i++) {
                        guiGraphics.drawString(font, lines.get(i), textX, lineY, UiTheme.TEXT_MUTED, false);
                        lineY += 10;
                    }
                }

                String hint = "Открыть";
                guiGraphics.drawString(font, hint, textX, drawY + cardHeight - padding - 9, UiTheme.TEXT_DIM, false);
            }
        }
    }

    private final class CardEntry {
        private final ReligionCard card;
        private final int x;
        private final int y;

        private CardEntry(ReligionCard card, int x, int y) {
            this.card = card;
            this.x = x;
            this.y = y;
        }

        private boolean contains(double mouseX, double mouseY, int drawY) {
            return mouseX >= x && mouseX <= x + cardWidth && mouseY >= drawY && mouseY <= drawY + cardHeight;
        }
    }

    private int resolveAccent(ReligionCard card) {
        String raw = card.getColor();
        if (raw == null) {
            return UiTheme.ACCENT;
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }
        try {
            if (cleaned.length() == 6) {
                return 0xFF000000 | Integer.parseInt(cleaned, 16);
            }
            if (cleaned.length() == 8) {
                return (int) Long.parseLong(cleaned, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return UiTheme.ACCENT;
    }
}
