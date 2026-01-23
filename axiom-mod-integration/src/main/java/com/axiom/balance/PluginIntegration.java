package com.axiom.balance;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class PluginIntegration {
    
    // Связь между технологиями плагина и предметами мода
    private static final Map<String, List<String>> TECH_TO_ITEMS = new HashMap<>();
    
    static {
        // === УРОВЕНЬ 1: Базовые ресурсы ===
        TECH_TO_ITEMS.put("mining_basics", Arrays.asList(
            "minecraft:iron_ore",
            "minecraft:copper_ore",
            "minecraft:coal"
        ));
        
        // === УРОВЕНЬ 2: Базовые инструменты ===
        TECH_TO_ITEMS.put("basic_tools", Arrays.asList(
            "minecraft:iron_pickaxe",
            "minecraft:iron_axe",
            "minecraft:iron_shovel"
        ));
        
        TECH_TO_ITEMS.put("basic_weapons", Arrays.asList(
            "minecraft:iron_sword",
            "minecraft:bow",
            "minecraft:arrow"
        ));
        
        // === УРОВЕНЬ 3: Детали оружия ===
        TECH_TO_ITEMS.put("gun_parts_basics", Arrays.asList(
            "tacz:gun_parts",
            "pointblank:gun_parts",
            "ballistix:barrel",
            "caps:tactical_grip"
        ));
        
        TECH_TO_ITEMS.put("basic_machinery", Arrays.asList(
            "minecraft:furnace",
            "minecraft:blast_furnace",
            "minecraft:piston"
        ));
        
        // === УРОВЕНЬ 4: Пистолеты ===
        TECH_TO_ITEMS.put("pistols", Arrays.asList(
            "tacz:glock",
            "pointblank:pistol"
        ));
        
        // ПАТРОНЫ ТОЛЬКО ПОСЛЕ ПИСТОЛЕТОВ!
        TECH_TO_ITEMS.put("pistol_ammo", Arrays.asList(
            "tacz:ammo_9mm",
            "pointblank:ammo_9mm",
            "ballistix:ammo_9mm"
        ));
        
        TECH_TO_ITEMS.put("steel_production", Arrays.asList(
            "immersiveengineering:ingot_steel",
            "immersiveengineering:component_steel"
        ));
        
        TECH_TO_ITEMS.put("basic_circuits", Arrays.asList(
            "industrialupgrade:basic_circuit",
            "industrialupgrade:electric_motor"
        ));
        
        // === УРОВЕНЬ 5: Автоматы ===
        TECH_TO_ITEMS.put("assault_rifles", Arrays.asList(
            "tacz:m4a1",
            "tacz:m16",
            "pointblank:m4",
            "ballistix:rifle",
            "caps:tactical_rifle"
        ));
        
        // ПАТРОНЫ ТОЛЬКО ПОСЛЕ ВИНТОВОК!
        TECH_TO_ITEMS.put("rifle_ammo_556", Arrays.asList(
            "tacz:ammo_556",
            "pointblank:ammo_556",
            "ballistix:ammo_556",
            "caps:ammo_556"
        ));
        
        TECH_TO_ITEMS.put("machine_guns", Arrays.asList(
            "tacz:ak47",
            "pointblank:ak47",
            "caps:assault_rifle"
        ));
        
        // ПАТРОНЫ ТОЛЬКО ПОСЛЕ АВТОМАТОВ!
        TECH_TO_ITEMS.put("rifle_ammo_762", Arrays.asList(
            "tacz:ammo_762",
            "pointblank:ammo_762",
            "caps:ammo_762",
            "superbwarfare:ammo_762"
        ));
        
        // === УРОВЕНЬ 6: Снайперки ===
        TECH_TO_ITEMS.put("sniper_rifles", Arrays.asList(
            "tacz:awp",
            "pointblank:awp",
            "ballistix:sniper"
        ));
        
        // ПАТРОНЫ ТОЛЬКО ПОСЛЕ СНАЙПЕРОК!
        TECH_TO_ITEMS.put("sniper_ammo", Arrays.asList(
            "tacz:ammo_50bmg",
            "pointblank:ammo_50bmg",
            "ballistix:ammo_50bmg"
        ));
        
        TECH_TO_ITEMS.put("shotguns", Arrays.asList(
            "tacz:shotgun",
            "pointblank:shotgun"
        ));
        
        // ПАТРОНЫ ТОЛЬКО ПОСЛЕ ДРОБОВИКОВ!
        TECH_TO_ITEMS.put("shotgun_ammo", Arrays.asList(
            "tacz:ammo_12gauge",
            "pointblank:shell"
        ));
        
        TECH_TO_ITEMS.put("advanced_machinery_ie", Arrays.asList(
            "immersiveengineering:crusher",
            "immersiveengineering:arc_furnace"
        ));
        
        TECH_TO_ITEMS.put("me_basics", Arrays.asList(
            "appliedenergistics2:certus_quartz_crystal",
            "appliedenergistics2:engineering_processor"
        ));
        
        // === УРОВЕНЬ 7: Автоматизация ===
        TECH_TO_ITEMS.put("me_storage", Arrays.asList(
            "appliedenergistics2:controller",
            "appliedenergistics2:drive"
        ));
        
        TECH_TO_ITEMS.put("advanced_industry", Arrays.asList(
            "industrialupgrade:electric_furnace",
            "industrialupgrade:advanced_machine"
        ));
        
        // === УРОВЕНЬ 8: Военная техника ===
        TECH_TO_ITEMS.put("heavy_weapons", Arrays.asList(
            "superbwarfare:machine_gun",
            "superbwarfare:grenade_launcher"
        ));
        
        TECH_TO_ITEMS.put("tactical_equipment", Arrays.asList(
            "caps:tactical_vest",
            "caps:night_vision",
            "caps:tactical_helmet"
        ));
        
        // === УРОВЕНЬ 9: Транспорт ===
        TECH_TO_ITEMS.put("vehicles", Arrays.asList(
            "ashvehicle:car",
            "immersivevehicles:vehicle"
        ));
    }
    
    // Проверка, можно ли крафтить предмет
    public static boolean canCraft(String playerId, String itemId) {
        // Получить изученные технологии игрока из плагина
        Set<String> unlockedTechs = getPlayerTechs(playerId);
        
        // Проверить, открыт ли предмет
        for (Map.Entry<String, List<String>> entry : TECH_TO_ITEMS.entrySet()) {
            if (entry.getValue().contains(itemId)) {
                return unlockedTechs.contains(entry.getKey());
            }
        }
        
        // Если предмет не в списке - разрешить (ванильный)
        return true;
    }
    
    // Получить технологии игрока (заглушка, нужно связать с плагином)
    private static Set<String> getPlayerTechs(String playerId) {
        // TODO: Запрос к плагину через API
        // Пока возвращаем базовые технологии
        return new HashSet<>(Arrays.asList("mining_basics", "basic_tools", "basic_weapons"));
    }
    
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack stack = event.getCrafting();
        String itemId = stack.getItem().toString();
        String playerId = event.getEntity().getUUID().toString();
        
        // Проверить, можно ли крафтить
        if (!canCraft(playerId, itemId)) {
            // Отменить крафт
            event.setCanceled(true);
            event.getEntity().sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "§c✗ Технология не изучена! Исследуйте её в дереве технологий."
                )
            );
        }
    }
}
