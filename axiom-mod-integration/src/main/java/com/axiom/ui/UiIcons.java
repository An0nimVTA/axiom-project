package com.axiom.ui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class UiIcons {
    private UiIcons() {}

    public static ItemStack resolve(CommandInfo command) {
        String iconId = command.getIconItemId();
        if (iconId == null || iconId.isBlank()) {
            iconId = guessIcon(command);
        }
        if (iconId == null || iconId.isBlank()) {
            iconId = command.getCategory().getIconItemId();
        }
        return resolveItem(iconId, Items.PAPER);
    }

    public static ItemStack resolveItem(String itemId, Item fallback) {
        if (itemId == null || itemId.isBlank()) {
            return new ItemStack(fallback);
        }
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        if (key == null) {
            return new ItemStack(fallback);
        }
        Item item = ForgeRegistries.ITEMS.getValue(key);
        if (item == null || item == Items.AIR) {
            return new ItemStack(fallback);
        }
        return new ItemStack(item);
    }

    private static String guessIcon(CommandInfo command) {
        if (command == null) {
            return null;
        }
        String name = (command.getDisplayName() + " " + command.getCommand()).toLowerCase();
        if (name.contains("войн") || name.contains("war")) return "minecraft:iron_sword";
        if (name.contains("альянс") || name.contains("ally") || name.contains("treaty")) return "minecraft:book";
        if (name.contains("нация") || name.contains("nation")) return "minecraft:white_banner";
        if (name.contains("город") || name.contains("city")) return "minecraft:bricks";
        if (name.contains("кошел") || name.contains("wallet") || name.contains("money")) return "minecraft:gold_ingot";
        if (name.contains("банк") || name.contains("bank")) return "minecraft:emerald";
        if (name.contains("крафт") || name.contains("recipe")) return "minecraft:crafting_table";
        if (name.contains("технолог") || name.contains("tech")) return "minecraft:redstone";
        if (name.contains("религ") || name.contains("faith")) return "minecraft:enchanting_table";
        if (name.contains("карта") || name.contains("map")) return "minecraft:map";
        if (name.contains("claim") || name.contains("клейм") || name.contains("террит")) return "minecraft:grass_block";
        if (name.contains("шпионаж") || name.contains("espionage")) return "minecraft:spyglass";
        if (name.contains("аналит") || name.contains("stat")) return "minecraft:paper";
        return null;
    }
}
