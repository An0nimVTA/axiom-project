package com.axiom.ui.tech;

import com.axiom.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class TechTreeScreen extends Screen {
    private final List<HorizontalCardWidget> cards = new ArrayList<>();
    private int scrollOffset = 0;

    public TechTreeScreen() {
        super(Component.literal("Древо Технологий"));
    }

    @Override
    protected void init() {
        cards.clear();
        int startX = (width - HorizontalCardWidget.WIDTH) / 2;
        int startY = 40;
        int spacing = 12;
        
        // Mock Data (In reality, this comes from Server Packets)
        addMockCard(startX, startY, "Горное дело", "Добыча руды и камня.", Items.IRON_PICKAXE);
        addMockCard(startX, startY + HorizontalCardWidget.HEIGHT + spacing, "Бронза", "Сплав меди и олова.", Items.COPPER_INGOT);
        addMockCard(startX, startY + (HorizontalCardWidget.HEIGHT + spacing) * 2, "Железо", "Обработка железа.", Items.IRON_INGOT);
        addMockCard(startX, startY + (HorizontalCardWidget.HEIGHT + spacing) * 3, "Сила Пара", "Энергия пара и машин.", Items.CAMPFIRE);
    }
    
    private void addMockCard(int x, int y, String title, String desc, net.minecraft.world.item.Item item) {
        cards.add(new HorizontalCardWidget(x, y, title, desc, new ItemStack(item),
            () -> {
                if (this.minecraft != null) this.minecraft.setScreen(new RecipeChainScreen(this, title, new ItemStack(item)));
            },
            () -> {
                // Info action (could imply opening another detailed screen or printing to chat)
                System.out.println("Info clicked for " + title);
            },
            () -> {
                if (this.minecraft != null) this.minecraft.setScreen(new ItemBalanceScreen(this, title, new ItemStack(item)));
            }
        ));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UiTheme.drawBackdrop(gfx, width, height);
        
        // Title
        gfx.drawCenteredString(font, title, width / 2, 10, UiTheme.TEXT_PRIMARY);
        
        // Render cards
        for (HorizontalCardWidget card : cards) {
            card.render(gfx, mouseX, mouseY, partialTick);
        }
        
        super.render(gfx, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (HorizontalCardWidget card : cards) {
            if (card.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
