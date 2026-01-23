package com.axiom.balance;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("axiombalance")
public class AxiomBalanceMod {
    
    public AxiomBalanceMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }
    
    private void setup(FMLCommonSetupEvent event) {
        System.out.println("===========================================");
        System.out.println("AXIOM BALANCE MOD - Взаимосвязь модов");
        System.out.println("===========================================");
        System.out.println();
        System.out.println("✅ Все моды теперь ВЗАИМОСВЯЗАНЫ!");
        System.out.println("✅ Для крафта нужны детали из разных модов");
        System.out.println();
        System.out.println("⚠️ Ванильные предметы НЕ изменены");
        System.out.println();
        System.out.println("=== ОРУЖЕЙНЫЕ МОДЫ ===");
        System.out.println("TACZ AK-47 → требует детали из Point Blank + Ballistix + CAPS");
        System.out.println("TACZ M4A1 → требует детали из Point Blank + Ballistix + CAPS");
        System.out.println("Point Blank AK → требует детали из TACZ + Ballistix + Superb Warfare");
        System.out.println("Ballistix винтовка → требует детали из TACZ + Point Blank + CAPS");
        System.out.println();
        System.out.println("Патроны:");
        System.out.println("- TACZ патроны → требуют гильзы из Point Blank");
        System.out.println("- Point Blank патроны → требуют капсюль из Ballistix");
        System.out.println();
        System.out.println("=== ТЕХНИЧЕСКИЕ МОДЫ ===");
        System.out.println("Immersive Engineering:");
        System.out.println("- Дробилка → требует процессор из AE2 + мотор из Industrial");
        System.out.println("- Дуговая печь → требует энергоячейку из AE2 + проводник из Industrial");
        System.out.println();
        System.out.println("Applied Energistics 2:");
        System.out.println("- ME контроллер → требует сталь из IE + схемы из Industrial");
        System.out.println("- ME привод → требует катушки из IE + мотор из Industrial");
        System.out.println();
        System.out.println("Industrial Upgrade:");
        System.out.println("- Электропечь → требует катушки из IE + стекло из AE2");
        System.out.println("- Продвинутая машина → требует сталь из IE + процессор из AE2");
        System.out.println();
        System.out.println("=== ОРУЖИЕ + ТЕХНИКА ===");
        System.out.println("TACZ AWP → требует сталь из IE + процессор из AE2");
        System.out.println("Superb Warfare пулемёт → требует детали из IE + Industrial");
        System.out.println();
        System.out.println("РЕЗУЛЬТАТ: Невозможно развиваться только в одном моде!");
        System.out.println("Нужно использовать ВСЕ моды вместе!");
        System.out.println("===========================================");
    }
}
