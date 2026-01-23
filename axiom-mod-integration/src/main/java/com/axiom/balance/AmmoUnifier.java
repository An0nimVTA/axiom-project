package com.axiom.balance;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class AmmoUnifier {
    // Универсальные патроны по калибрам
    private static final Map<String, List<String>> CALIBER_MAP = new HashMap<>();
    
    static {
        // 5.56mm (штурмовые винтовки)
        CALIBER_MAP.put("5.56mm", Arrays.asList(
            "tacz:ammo_556",
            "pointblank:ammo_556",
            "ballistix:ammo_556",
            "caps:ammo_556"
        ));
        
        // 7.62mm (автоматы)
        CALIBER_MAP.put("7.62mm", Arrays.asList(
            "tacz:ammo_762",
            "pointblank:ammo_762",
            "ballistix:ammo_762",
            "caps:ammo_762",
            "superbwarfare:ammo_762"
        ));
        
        // 9mm (пистолеты)
        CALIBER_MAP.put("9mm", Arrays.asList(
            "tacz:ammo_9mm",
            "pointblank:ammo_9mm",
            "ballistix:ammo_9mm"
        ));
        
        // .50 BMG (снайперские винтовки)
        CALIBER_MAP.put(".50bmg", Arrays.asList(
            "tacz:ammo_50bmg",
            "pointblank:ammo_50bmg",
            "ballistix:ammo_50bmg"
        ));
        
        // 12 калибр (дробовики)
        CALIBER_MAP.put("12gauge", Arrays.asList(
            "tacz:ammo_12gauge",
            "pointblank:shell",
            "ballistix:shell"
        ));
    }
    
    // Какое оружие использует какой калибр
    private static final Map<String, String> WEAPON_CALIBER = new HashMap<>();
    
    static {
        // 5.56mm оружие
        WEAPON_CALIBER.put("tacz:m4a1", "5.56mm");
        WEAPON_CALIBER.put("tacz:m16", "5.56mm");
        WEAPON_CALIBER.put("tacz:scar", "5.56mm");
        WEAPON_CALIBER.put("pointblank:m4", "5.56mm");
        WEAPON_CALIBER.put("ballistix:rifle", "5.56mm");
        WEAPON_CALIBER.put("caps:tactical_rifle", "5.56mm");
        
        // 7.62mm оружие
        WEAPON_CALIBER.put("tacz:ak47", "7.62mm");
        WEAPON_CALIBER.put("pointblank:ak47", "7.62mm");
        WEAPON_CALIBER.put("caps:assault_rifle", "7.62mm");
        WEAPON_CALIBER.put("superbwarfare:machine_gun", "7.62mm");
        
        // 9mm оружие
        WEAPON_CALIBER.put("tacz:glock", "9mm");
        WEAPON_CALIBER.put("pointblank:pistol", "9mm");
        
        // .50 BMG оружие
        WEAPON_CALIBER.put("tacz:awp", ".50bmg");
        WEAPON_CALIBER.put("pointblank:awp", ".50bmg");
        WEAPON_CALIBER.put("ballistix:sniper", ".50bmg");
        
        // 12 калибр оружие
        WEAPON_CALIBER.put("tacz:shotgun", "12gauge");
        WEAPON_CALIBER.put("pointblank:shotgun", "12gauge");
    }
    
    public static boolean canUseAmmo(String weaponId, String ammoId) {
        String caliber = WEAPON_CALIBER.get(weaponId);
        if (caliber == null) return false;
        
        List<String> validAmmo = CALIBER_MAP.get(caliber);
        return validAmmo != null && validAmmo.contains(ammoId);
    }
    
    public static String getCaliber(String weaponId) {
        return WEAPON_CALIBER.getOrDefault(weaponId, "unknown");
    }
    
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack stack = event.getCrafting();
        String id = stack.getItem().toString();
        
        // Пометить патроны калибром
        for (Map.Entry<String, List<String>> entry : CALIBER_MAP.entrySet()) {
            if (entry.getValue().stream().anyMatch(id::contains)) {
                stack.getOrCreateTag().putString("Caliber", entry.getKey());
                break;
            }
        }
    }
}
