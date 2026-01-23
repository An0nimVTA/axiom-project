package com.axiom.balance;

import java.util.*;

public class TechTree {
    
    // Полное дерево технологий с зависимостями
    private static final Map<String, TechNode> TECH_TREE = new HashMap<>();
    
    static {
        // === УРОВЕНЬ 1: БАЗОВЫЕ РЕСУРСЫ (старт) ===
        
        TECH_TREE.put("mining_basics", new TechNode(
            "Основы добычи",
            "Добыча железа, меди, угля",
            1,
            Arrays.asList(),
            Arrays.asList("minecraft:iron_ore", "minecraft:copper_ore", "minecraft:coal")
        ));
        
        // === УРОВЕНЬ 2: БАЗОВЫЕ ИНСТРУМЕНТЫ ===
        
        TECH_TREE.put("basic_tools", new TechNode(
            "Базовые инструменты",
            "Железные инструменты",
            2,
            Arrays.asList("mining_basics"),
            Arrays.asList("minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_shovel")
        ));
        
        TECH_TREE.put("basic_weapons", new TechNode(
            "Базовое оружие",
            "Железный меч, лук",
            2,
            Arrays.asList("mining_basics"),
            Arrays.asList("minecraft:iron_sword", "minecraft:bow", "minecraft:arrow")
        ));
        
        // === УРОВЕНЬ 3: ПЕРВЫЕ МОДЫ ===
        
        TECH_TREE.put("gun_parts_basics", new TechNode(
            "Детали оружия",
            "Базовые детали для огнестрела",
            3,
            Arrays.asList("basic_tools", "basic_weapons"),
            Arrays.asList("tacz:gun_parts", "pointblank:gun_parts", "ballistix:barrel")
        ));
        
        TECH_TREE.put("basic_machinery", new TechNode(
            "Базовые механизмы",
            "Первые машины",
            3,
            Arrays.asList("basic_tools"),
            Arrays.asList("minecraft:furnace", "minecraft:blast_furnace", "minecraft:piston")
        ));
        
        // === УРОВЕНЬ 4: ПРОСТОЕ ОГНЕСТРЕЛЬНОЕ ОРУЖИЕ ===
        
        TECH_TREE.put("pistols", new TechNode(
            "Пистолеты",
            "Первое огнестрельное оружие",
            4,
            Arrays.asList("gun_parts_basics"),
            Arrays.asList("tacz:glock", "pointblank:pistol")
        ));
        
        TECH_TREE.put("pistol_ammo", new TechNode(
            "Патроны 9mm",
            "Патроны для пистолетов",
            4,
            Arrays.asList("pistols"), // ТОЛЬКО ПОСЛЕ ПИСТОЛЕТОВ!
            Arrays.asList("tacz:ammo_9mm", "pointblank:ammo_9mm")
        ));
        
        // === УРОВЕНЬ 5: АВТОМАТЫ ===
        
        TECH_TREE.put("assault_rifles", new TechNode(
            "Штурмовые винтовки",
            "M4A1, M16, SCAR",
            5,
            Arrays.asList("pistols", "gun_parts_basics"),
            Arrays.asList("tacz:m4a1", "tacz:m16", "pointblank:m4", "ballistix:rifle")
        ));
        
        TECH_TREE.put("rifle_ammo_556", new TechNode(
            "Патроны 5.56mm",
            "Патроны для винтовок",
            5,
            Arrays.asList("assault_rifles"), // ТОЛЬКО ПОСЛЕ ВИНТОВОК!
            Arrays.asList("tacz:ammo_556", "pointblank:ammo_556", "ballistix:ammo_556")
        ));
        
        TECH_TREE.put("machine_guns", new TechNode(
            "Автоматы",
            "AK-47, AKM",
            5,
            Arrays.asList("pistols", "gun_parts_basics"),
            Arrays.asList("tacz:ak47", "pointblank:ak47", "caps:assault_rifle")
        ));
        
        TECH_TREE.put("rifle_ammo_762", new TechNode(
            "Патроны 7.62mm",
            "Патроны для автоматов",
            5,
            Arrays.asList("machine_guns"), // ТОЛЬКО ПОСЛЕ АВТОМАТОВ!
            Arrays.asList("tacz:ammo_762", "pointblank:ammo_762", "caps:ammo_762")
        ));
        
        // === УРОВЕНЬ 6: СНАЙПЕРКИ И ДРОБОВИКИ ===
        
        TECH_TREE.put("sniper_rifles", new TechNode(
            "Снайперские винтовки",
            "AWP, Barrett",
            6,
            Arrays.asList("assault_rifles", "machine_guns"),
            Arrays.asList("tacz:awp", "pointblank:awp", "ballistix:sniper")
        ));
        
        TECH_TREE.put("sniper_ammo", new TechNode(
            "Патроны .50 BMG",
            "Патроны для снайперок",
            6,
            Arrays.asList("sniper_rifles"), // ТОЛЬКО ПОСЛЕ СНАЙПЕРОК!
            Arrays.asList("tacz:ammo_50bmg", "pointblank:ammo_50bmg", "ballistix:ammo_50bmg")
        ));
        
        TECH_TREE.put("shotguns", new TechNode(
            "Дробовики",
            "Shotgun",
            6,
            Arrays.asList("assault_rifles"),
            Arrays.asList("tacz:shotgun", "pointblank:shotgun")
        ));
        
        TECH_TREE.put("shotgun_ammo", new TechNode(
            "Патроны 12 калибр",
            "Патроны для дробовиков",
            6,
            Arrays.asList("shotguns"), // ТОЛЬКО ПОСЛЕ ДРОБОВИКОВ!
            Arrays.asList("tacz:ammo_12gauge", "pointblank:shell")
        ));
        
        // === УРОВЕНЬ 4-5: БАЗОВАЯ ТЕХНИКА ===
        
        TECH_TREE.put("steel_production", new TechNode(
            "Производство стали",
            "Immersive Engineering - сталь",
            4,
            Arrays.asList("basic_machinery"),
            Arrays.asList("immersiveengineering:ingot_steel", "immersiveengineering:component_steel")
        ));
        
        TECH_TREE.put("basic_circuits", new TechNode(
            "Базовые схемы",
            "Industrial Upgrade - схемы",
            4,
            Arrays.asList("basic_machinery"),
            Arrays.asList("industrialupgrade:basic_circuit", "industrialupgrade:electric_motor")
        ));
        
        // === УРОВЕНЬ 6: ПРОДВИНУТАЯ ТЕХНИКА ===
        
        TECH_TREE.put("advanced_machinery_ie", new TechNode(
            "Продвинутые машины IE",
            "Дробилка, дуговая печь",
            6,
            Arrays.asList("steel_production", "basic_circuits"),
            Arrays.asList("immersiveengineering:crusher", "immersiveengineering:arc_furnace")
        ));
        
        TECH_TREE.put("me_basics", new TechNode(
            "Основы ME",
            "Applied Energistics 2 - базовые компоненты",
            6,
            Arrays.asList("steel_production", "basic_circuits"),
            Arrays.asList("appliedenergistics2:certus_quartz_crystal", 
                         "appliedenergistics2:engineering_processor")
        ));
        
        // === УРОВЕНЬ 7: АВТОМАТИЗАЦИЯ ===
        
        TECH_TREE.put("me_storage", new TechNode(
            "ME хранилище",
            "ME контроллер, приводы",
            7,
            Arrays.asList("me_basics", "advanced_machinery_ie"),
            Arrays.asList("appliedenergistics2:controller", "appliedenergistics2:drive")
        ));
        
        TECH_TREE.put("advanced_industry", new TechNode(
            "Продвинутая промышленность",
            "Industrial Upgrade - продвинутые машины",
            7,
            Arrays.asList("basic_circuits", "steel_production"),
            Arrays.asList("industrialupgrade:electric_furnace", "industrialupgrade:advanced_machine")
        ));
        
        // === УРОВЕНЬ 8: ВОЕННАЯ ТЕХНИКА ===
        
        TECH_TREE.put("heavy_weapons", new TechNode(
            "Тяжёлое оружие",
            "Пулемёты, гранатомёты",
            8,
            Arrays.asList("sniper_rifles", "advanced_machinery_ie"),
            Arrays.asList("superbwarfare:machine_gun", "superbwarfare:grenade_launcher")
        ));
        
        TECH_TREE.put("tactical_equipment", new TechNode(
            "Тактическое снаряжение",
            "CAPS - броня, гаджеты",
            8,
            Arrays.asList("assault_rifles", "me_basics"),
            Arrays.asList("caps:tactical_vest", "caps:night_vision", "caps:tactical_helmet")
        ));
        
        // === УРОВЕНЬ 9: ТРАНСПОРТ ===
        
        TECH_TREE.put("vehicles", new TechNode(
            "Транспорт",
            "Машины, вертолёты",
            9,
            Arrays.asList("advanced_machinery_ie", "heavy_weapons"),
            Arrays.asList("ashvehicle:car", "immersivevehicles:vehicle")
        ));
    }
    
    // Проверка, доступна ли технология
    public static boolean isUnlocked(String techId, Set<String> unlockedTechs) {
        TechNode node = TECH_TREE.get(techId);
        if (node == null) return false;
        
        // Проверить все зависимости
        for (String req : node.requirements) {
            if (!unlockedTechs.contains(req)) {
                return false;
            }
        }
        return true;
    }
    
    // Получить все доступные технологии
    public static List<String> getAvailableTechs(Set<String> unlockedTechs) {
        List<String> available = new ArrayList<>();
        for (Map.Entry<String, TechNode> entry : TECH_TREE.entrySet()) {
            if (!unlockedTechs.contains(entry.getKey()) && 
                isUnlocked(entry.getKey(), unlockedTechs)) {
                available.add(entry.getKey());
            }
        }
        return available;
    }
    
    // Получить предметы, которые можно крафтить
    public static List<String> getCraftableItems(Set<String> unlockedTechs) {
        List<String> items = new ArrayList<>();
        for (String techId : unlockedTechs) {
            TechNode node = TECH_TREE.get(techId);
            if (node != null) {
                items.addAll(node.unlocks);
            }
        }
        return items;
    }
    
    static class TechNode {
        String name;
        String description;
        int tier;
        List<String> requirements;
        List<String> unlocks;
        
        TechNode(String name, String description, int tier, 
                List<String> requirements, List<String> unlocks) {
            this.name = name;
            this.description = description;
            this.tier = tier;
            this.requirements = requirements;
            this.unlocks = unlocks;
        }
    }
}
