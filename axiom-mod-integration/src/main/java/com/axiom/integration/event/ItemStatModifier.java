package com.axiom.integration.event;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "axiomui", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemStatModifier {
    
    // Server sends these values: "minecraft:iron_sword" -> 2.0 (Double damage)
    public static final Map<String, Double> damageMultipliers = new HashMap<>();
    
    private static final UUID AXIOM_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

    @SubscribeEvent
    public static void onItemAttribute(ItemAttributeModifierEvent event) {
        if (event.getItemStack().isEmpty()) return;
        
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).toString();
        
        if (damageMultipliers.containsKey(itemId)) {
            double multiplier = damageMultipliers.get(itemId);
            
            // Add custom attribute modifier
            // Note: We are adding to the EXISTING modifiers
            if (event.getSlotType() == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
                event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    AXIOM_DAMAGE_UUID, 
                    "Axiom Tech Bonus", 
                    multiplier - 1.0, // e.g. 2.0 multiplier means +100% (value 1.0)
                    AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }
        }
    }
}
