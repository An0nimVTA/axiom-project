package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Mod;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Система проверки совместимости модов
 * Проверяет конфликты между модами и их зависимости
 */
public class ModCompatibilityChecker {
    private final AXIOM plugin;
    
    // Карта зависимостей модов: мод -> список зависимостей
    private final Map<String, Set<String>> modDependencies = new HashMap<>();
    
    // Карта конфликтов модов: мод -> список конфликтующих модов
    private final Map<String, Set<String>> modConflicts = new HashMap<>();
    
    // Карта требований технологий: мод -> необходимая технология
    private final Map<String, String> modTechnologyRequirements = new HashMap<>();
    
    public ModCompatibilityChecker(AXIOM plugin) {
        this.plugin = plugin;
        initializeCompatibilityRules();
    }
    
    /**
     * Инициализация базовых правил совместимости
     */
    private void initializeCompatibilityRules() {
        // Зависимости модов
        // Immersive Engineering
        modDependencies.put("ImmersiveEngineering", Set.of("Forge", "Minecraft"));
        
        // Mekanism
        modDependencies.put("Mekanism", Set.of("Minecraft", "Forge"));
        
        // Tinkers Construct
        modDependencies.put("TinkersConstruct", Set.of("Minecraft", "Forge", "mantle"));
        
        // Tesla Coils
        modDependencies.put("TeslaCoils", Set.of("TeslaAPI", "Minecraft"));
        
        // Advanced Rocketry
        modDependencies.put("AdvancedRocketry", Set.of("LibVulpes", "Minecraft"));
        
        // Конфликты модов
        // IC2 vs Thermal Foundation (оба добавляют энергетические системы)
        modConflicts.put("IC2", Set.of("ThermalFoundation"));
        modConflicts.put("ThermalFoundation", Set.of("IC2"));
        
        // Thaumcraft vs Blood Magic (оба магические моды, могут конфликтовать)
        modConflicts.put("Thaumcraft", Set.of("BloodMagic"));
        modConflicts.put("BloodMagic", Set.of("Thaumcraft"));
        
        // Моды, конфликтующие с модом на оптимизацию
        modConflicts.put("Optifine", Set.of("ShadersMod"));
        modConflicts.put("ShadersMod", Set.of("Optifine"));
        
        // Требования к технологиям
        modTechnologyRequirements.put("ImmersiveEngineering", "advanced_industry");
        modTechnologyRequirements.put("Mekanism", "energy_processing");
        modTechnologyRequirements.put("AdvancedRocketry", "space_program");
        modTechnologyRequirements.put("TeslaCoils", "electrical_engineering");
        modTechnologyRequirements.put("IC2", "energy_generation");
        modTechnologyRequirements.put("Thaumcraft", "arcane_knowledge");
        modTechnologyRequirements.put("BloodMagic", "life_magic");
        modTechnologyRequirements.put("Botania", "botanical_magic");
    }
    
    /**
     * Проверить совместимость модов в модпаке
     */
    public List<String> checkModPackCompatibility(List<Mod> modList) {
        List<String> issues = new ArrayList<>();
        if (modList == null) return issues;
        
        for (Mod mod : modList) {
            if (mod == null) continue;
            if (!mod.isEnabled()) continue;
            
            // Проверяем зависимости
            List<String> missingDependencies = checkMissingDependencies(mod, modList);
            if (!missingDependencies.isEmpty()) {
                issues.add("Мод '" + mod.getName() + "' требует: " + String.join(", ", missingDependencies));
            }
            
            // Проверяем конфликты
            List<String> conflictingMods = checkConflictingMods(mod, modList);
            if (!conflictingMods.isEmpty()) {
                issues.add("Мод '" + mod.getName() + "' конфликтует с: " + String.join(", ", conflictingMods));
            }
            
            // Проверяем требования к технологиям
            String techIssue = checkTechnologyRequirement(mod);
            if (techIssue != null) {
                issues.add(techIssue);
            }
        }
        
        return issues;
    }
    
    /**
     * Проверить, удовлетворены ли зависимости мода
     */
    private List<String> checkMissingDependencies(Mod mod, List<Mod> allMods) {
        List<String> missing = new ArrayList<>();
        if (mod == null || allMods == null) return missing;
        
        if (modDependencies.containsKey(mod.getId())) {
            Set<String> required = modDependencies.get(mod.getId());
            for (String dependency : required) {
                boolean found = false;
                for (Mod otherMod : allMods) {
                    if (otherMod != null && dependency.equals(otherMod.getId()) && otherMod.isEnabled()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missing.add(dependency);
                }
            }
        }
        
        return missing;
    }
    
    /**
     * Проверить, конфликты с другими модами
     */
    private List<String> checkConflictingMods(Mod mod, List<Mod> allMods) {
        List<String> conflicts = new ArrayList<>();
        if (mod == null || allMods == null) return conflicts;
        
        if (modConflicts.containsKey(mod.getId())) {
            Set<String> conflictingModIds = modConflicts.get(mod.getId());
            for (String conflictingModId : conflictingModIds) {
                // Проверяем, есть ли конфликтующий мод в активных
                for (Mod otherMod : allMods) {
                    if (otherMod.getId().equals(conflictingModId) && otherMod.isEnabled()) {
                        conflicts.add(otherMod.getName());
                    }
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Проверить требования к технологиям
     */
    private String checkTechnologyRequirement(Mod mod) {
        if (mod == null) return null;
        if (modTechnologyRequirements.containsKey(mod.getId())) {
            String requiredTech = modTechnologyRequirements.get(mod.getId());
            // Здесь должна быть проверка, изучена ли технология у игрока/нации
            // Пока возвращаем сообщение, что технология не изучена
            return "Мод '" + mod.getName() + "' требует изучить технологию '" + requiredTech + "'";
        }
        return null;
    }
    
    /**
     * Проверить, может ли игрок использовать мод
     */
    public boolean canPlayerUseMod(Player player, Mod mod) {
        if (player == null || mod == null) return false;
        if (!modTechnologyRequirements.containsKey(mod.getId())) {
            return true; // Если нет требований, мод доступен
        }
        
        String requiredTech = modTechnologyRequirements.get(mod.getId());
        // В реальной системе проверяем, изучена ли технология
        // Пока упрощенно возвращаем true
        return plugin != null && plugin.getTechnologyTreeService() != null &&
            plugin.getTechnologyTreeService().isPlayerHasTech(player.getUniqueId(), requiredTech);
    }
    
    /**
     * Проверить совместимость двух модов
     */
    public boolean areModsCompatible(String mod1Id, String mod2Id) {
        if (mod1Id == null || mod2Id == null) return true;
        if (modConflicts.containsKey(mod1Id)) {
            return !modConflicts.get(mod1Id).contains(mod2Id);
        }
        return true;
    }
    
    /**
     * Проверить, есть ли у мода требования к технологии
     */
    public boolean hasTechnologyRequirement(String modId) {
        return modId != null && modTechnologyRequirements.containsKey(modId);
    }
    
    /**
     * Получить требуемую технологию для мода
     */
    public String getRequiredTechnology(String modId) {
        return modId != null ? modTechnologyRequirements.get(modId) : null;
    }
    
    /**
     * Протестировать совместимость модов и сообщить игроку
     */
    public void testAndReportCompatibility(Player player, List<Mod> modList) {
        if (player == null) return;
        List<String> issues = checkModPackCompatibility(modList);
        
        if (issues.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "✓ Все моды в модпаке совместимы!");
        } else {
            player.sendMessage(ChatColor.RED + "✗ Найдены проблемы совместимости:");
            for (String issue : issues) {
                player.sendMessage(ChatColor.YELLOW + "  - " + issue);
            }
        }
    }
    
    /**
     * Получить рекомендации по улучшению модпака
     */
    public List<String> getRecommendations(List<Mod> modList) {
        List<String> recommendations = new ArrayList<>();
        if (modList == null) return recommendations;
        
        // Рекомендации на основе типа сервера
        long modernMods = modList.stream()
            .filter(mod -> mod != null && ("modern".equals(mod.getServerType()) || isModernMod(mod.getId())))
            .count();
        
        long magicalMods = modList.stream()
            .filter(mod -> mod != null && ("magic".equals(mod.getServerType()) || isMagicalMod(mod.getId())))
            .count();
        
        if (modernMods > 0 && magicalMods > 0) {
            recommendations.add("Совет: Современные и магические моды могут конфликтовать, рассмотрите разделение модпаков");
        }
        
        // Рекомендации на основе отсутствующих модов
        if (modList.stream().anyMatch(mod -> mod.getId().equals("ImmersiveEngineering")) &&
            modList.stream().noneMatch(mod -> mod.getId().equals("Mekanism"))) {
            recommendations.add("Совет: Рассмотрите добавление Mekanism для лучшей совместимости с промышленными модами");
        }
        
        return recommendations;
    }
    
    // Вспомогательные методы для определения типов модов
    private boolean isModernMod(String modId) {
        if (modId == null) return false;
        String lower = modId.toLowerCase();
        return lower.contains("mekanism") || 
               lower.contains("immersive") ||
               lower.contains("tesla") ||
               lower.contains("rocket");
    }
    
    private boolean isMagicalMod(String modId) {
        if (modId == null) return false;
        String lower = modId.toLowerCase();
        return lower.contains("thaum") ||
               lower.contains("blood") ||
               lower.contains("botania") ||
               lower.contains("ars");
    }
}
