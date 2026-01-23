package com.axiom.balance;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class ItemBalancer {
    private static final Map<String, ItemStats> BALANCE = new HashMap<>();
    
    static {
        // TACZ оружие (основной мод оружия)
        BALANCE.put("tacz:ak47", new ItemStats(8, 10.0f, 500));
        BALANCE.put("tacz:m4a1", new ItemStats(7, 11.7f, 500));
        BALANCE.put("tacz:awp", new ItemStats(35, 0.67f, 300));
        BALANCE.put("tacz:shotgun", new ItemStats(48, 1.0f, 400));
        BALANCE.put("tacz:glock", new ItemStats(6, 5.0f, 400));
        BALANCE.put("tacz:m16", new ItemStats(7, 11.7f, 500));
        BALANCE.put("tacz:scar", new ItemStats(7, 11.7f, 500));
        
        // Point Blank (сбалансировать с TACZ)
        BALANCE.put("pointblank:ak47", new ItemStats(8, 10.0f, 500));
        BALANCE.put("pointblank:m4", new ItemStats(7, 11.7f, 500));
        BALANCE.put("pointblank:awp", new ItemStats(35, 0.67f, 300));
        BALANCE.put("pointblank:shotgun", new ItemStats(48, 1.0f, 400));
        
        // Ballistix (сбалансировать)
        BALANCE.put("ballistix:rifle", new ItemStats(7, 11.7f, 500));
        BALANCE.put("ballistix:sniper", new ItemStats(35, 0.67f, 300));
        
        // CAPS Tactical Equipment (сбалансировать)
        BALANCE.put("caps:tactical_rifle", new ItemStats(7, 11.7f, 500));
        BALANCE.put("caps:assault_rifle", new ItemStats(8, 10.0f, 500));
        
        // Superb Warfare (военный мод, сбалансировать)
        BALANCE.put("superbwarfare:rifle", new ItemStats(7, 11.7f, 500));
        BALANCE.put("superbwarfare:machine_gun", new ItemStats(8, 10.0f, 600));
    }
    
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack stack = event.getCrafting();
        applyBalance(stack);
    }
    
    public static void applyBalance(ItemStack stack) {
        String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        ItemStats stats = BALANCE.get(id);
        
        if (stats != null) {
            // Применить характеристики через NBT
            stack.getOrCreateTag().putInt("CustomDamage", stats.damage);
            stack.getOrCreateTag().putFloat("CustomSpeed", stats.speed);
            stack.getOrCreateTag().putInt("CustomDurability", stats.durability);
        }
    }
    
    static class ItemStats {
        int damage;
        float speed;
        int durability;
        
        ItemStats(int damage, float speed, int durability) {
            this.damage = damage;
            this.speed = speed;
            this.durability = durability;
        }
    }
}
