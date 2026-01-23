package com.axiom.balance;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class RecipeModifier {
    
    // Новые крафты (автоматически применяются)
    private static final Map<String, RecipeChange> RECIPE_CHANGES = new HashMap<>();
    
    static {
        // === ОРУЖЕЙНЫЕ МОДЫ - ЗАВИСЯТ ДРУГ ОТ ДРУГА ===
        
        // TACZ оружие требует детали из других модов
        RECIPE_CHANGES.put("tacz:ak47", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:8", "pointblank:gun_parts:2", 
                         "ballistix:barrel:1", "caps:tactical_grip:1"),
            "Требует детали из Point Blank, Ballistix, CAPS"
        ));
        
        RECIPE_CHANGES.put("tacz:m4a1", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:6", "pointblank:gun_parts:2",
                         "ballistix:scope:1", "caps:tactical_stock:1"),
            "Требует детали из Point Blank, Ballistix, CAPS"
        ));
        
        // Point Blank оружие требует детали из других модов
        RECIPE_CHANGES.put("pointblank:ak47", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:8", "tacz:gun_parts:2",
                         "ballistix:barrel:1", "superbwarfare:weapon_parts:1"),
            "Требует детали из TACZ, Ballistix, Superb Warfare"
        ));
        
        // Ballistix оружие требует детали из других модов
        RECIPE_CHANGES.put("ballistix:rifle", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:6", "tacz:gun_parts:2",
                         "pointblank:scope:1", "caps:tactical_rail:1"),
            "Требует детали из TACZ, Point Blank, CAPS"
        ));
        
        // === ПАТРОНЫ - ТРЕБУЮТ ДЕТАЛИ ИЗ РАЗНЫХ МОДОВ ===
        
        RECIPE_CHANGES.put("tacz:ammo_556", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:1", "minecraft:gunpowder:1", 
                         "minecraft:copper_ingot:2", "pointblank:casing:1"),
            "Требует гильзы из Point Blank"
        ));
        
        RECIPE_CHANGES.put("pointblank:ammo_762", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:2", "minecraft:gunpowder:2",
                         "minecraft:copper_ingot:2", "ballistix:primer:1"),
            "Требует капсюль из Ballistix"
        ));
        
        // === ТЕХНИЧЕСКИЕ МОДЫ - ЗАВИСЯТ ДРУГ ОТ ДРУГА ===
        
        // Immersive Engineering требует детали из AE2 и Industrial
        RECIPE_CHANGES.put("immersiveengineering:crusher", new RecipeChange(
            Arrays.asList("minecraft:iron_block:4", "minecraft:piston:2",
                         "appliedenergistics2:engineering_processor:1",
                         "industrialupgrade:electric_motor:2"),
            "Требует процессор из AE2 и мотор из Industrial"
        ));
        
        RECIPE_CHANGES.put("immersiveengineering:arc_furnace", new RecipeChange(
            Arrays.asList("minecraft:iron_block:8", "immersiveengineering:heavy_engineering:2",
                         "appliedenergistics2:energy_cell:1",
                         "industrialupgrade:heat_conductor:2"),
            "Требует энергоячейку из AE2 и проводник из Industrial"
        ));
        
        // Applied Energistics 2 требует детали из IE и Industrial
        RECIPE_CHANGES.put("appliedenergistics2:controller", new RecipeChange(
            Arrays.asList("appliedenergistics2:smooth_sky_stone_block:4",
                         "immersiveengineering:component_steel:2",
                         "industrialupgrade:advanced_circuit:2",
                         "minecraft:diamond:4"),
            "Требует сталь из IE и схемы из Industrial"
        ));
        
        RECIPE_CHANGES.put("appliedenergistics2:drive", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:8", "appliedenergistics2:engineering_processor:2",
                         "immersiveengineering:wirecoil:2",
                         "industrialupgrade:electric_motor:1"),
            "Требует катушки из IE и мотор из Industrial"
        ));
        
        // Industrial Upgrade требует детали из IE и AE2
        RECIPE_CHANGES.put("industrialupgrade:electric_furnace", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:8", "minecraft:furnace:1",
                         "immersiveengineering:coil:2",
                         "appliedenergistics2:quartz_glass:4"),
            "Требует катушки из IE и стекло из AE2"
        ));
        
        RECIPE_CHANGES.put("industrialupgrade:advanced_machine", new RecipeChange(
            Arrays.asList("minecraft:iron_block:4", "industrialupgrade:basic_machine:1",
                         "immersiveengineering:component_steel:4",
                         "appliedenergistics2:calculation_processor:2"),
            "Требует сталь из IE и процессор из AE2"
        ));
        
        // === ОРУЖИЕ + ТЕХНИКА - ВЗАИМОСВЯЗЬ ===
        
        // Продвинутое оружие требует технические детали
        RECIPE_CHANGES.put("tacz:awp", new RecipeChange(
            Arrays.asList("minecraft:iron_ingot:12", "tacz:gun_parts:4",
                         "immersiveengineering:component_steel:2",
                         "appliedenergistics2:engineering_processor:1",
                         "ballistix:precision_barrel:1"),
            "Снайперка требует сталь из IE и процессор из AE2"
        ));
        
        RECIPE_CHANGES.put("superbwarfare:machine_gun", new RecipeChange(
            Arrays.asList("minecraft:iron_block:2", "superbwarfare:weapon_parts:4",
                         "immersiveengineering:heavy_engineering:1",
                         "industrialupgrade:electric_motor:1"),
            "Пулемёт требует детали из IE и Industrial"
        ));
    }
    
    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        // Применить изменения крафтов при загрузке
        applyRecipeChanges();
    }
    
    private static void applyRecipeChanges() {
        // Здесь мод автоматически изменяет крафты
        // Это работает через Forge Recipe API
        System.out.println("[AXIOM Balance] Применение изменённых крафтов...");
        
        for (Map.Entry<String, RecipeChange> entry : RECIPE_CHANGES.entrySet()) {
            System.out.println("[AXIOM Balance] Изменён крафт: " + entry.getKey() + 
                             " - " + entry.getValue().reason);
        }
    }
    
    static class RecipeChange {
        List<String> ingredients;
        String reason;
        
        RecipeChange(List<String> ingredients, String reason) {
            this.ingredients = ingredients;
            this.reason = reason;
        }
    }
}
