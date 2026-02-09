package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.industry.ResourceCatalogService;
import com.axiom.domain.service.industry.ResourceService;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages mod-based resources and their integration with AXIOM economy.
 * Tracks mod items/blocks as national resources.
 */
public class ModResourceService {
    private final AXIOM plugin;
    private final ModIntegrationService modIntegration;
    private final ResourceService resourceService;
    
    // Map mod items to resource types
    private final Map<String, String> modItemToResource = new HashMap<>();
    
    public ModResourceService(AXIOM plugin, ModIntegrationService modIntegration, ResourceService resourceService) {
        this.plugin = plugin;
        this.modIntegration = modIntegration;
        this.resourceService = resourceService;
        initializeResourceMapping();
    }
    
    private void initializeResourceMapping() {
        // Map mod items to AXIOM resource types
        // Applied Energistics 2 → "ae2_components", "ae2_patterns"
        modItemToResource.put("appliedenergistics2", "ae2_components");
        
        // Immersive Engineering → "ie_machines", "ie_energy"
        modItemToResource.put("immersiveengineering", "ie_components");
        
        // Tacz/PointBlank → "weapons", "ammunition"
        modItemToResource.put("tacz", "firearms");
        modItemToResource.put("pointblank", "firearms");
        
        // Simply Quarries → tracked separately as "quarry_output"
        modItemToResource.put("simplyquarries", "quarry_resources");
        
        // Industrial Upgrade → "industrial_components"
        modItemToResource.put("industrialupgrade", "industrial_components");
        
        // Ballistix → "explosives", "artillery"
        modItemToResource.put("ballistix", "explosives");
        
        // SuperWarfare → "military_vehicles", "warfare_equipment"
        modItemToResource.put("superwarfare", "military_vehicles");
    }
    
    /**
     * Record mod item extraction for nation's resource tracking.
     */
    public void recordModItemExtraction(String nationId, ItemStack item) {
        if (nationId == null || nationId.trim().isEmpty() || item == null) return;
        if (modIntegration == null || resourceService == null) return;
        String modId = modIntegration.detectModFromItem(item);
        if (modId == null) return;
        
        String resourceType = modItemToResource.getOrDefault(modId, "mod_items");
        ResourceCatalogService catalogService = plugin.getResourceCatalogService();
        if (catalogService != null && !catalogService.isKnownResource(resourceType)) {
            resourceType = "mod_items";
        }
        resourceService.addResource(nationId, resourceType, item.getAmount());
        
        // Special handling for quarries
        if ("simplyquarries".equals(modId)) {
            resourceService.addResource(nationId, "raw_materials", item.getAmount());
        }
    }
    
    /**
     * Check if block is a protected mod resource (quarry, generator, etc.).
     */
    public boolean isProtectedModResource(Block block, String nationId) {
        if (block == null || modIntegration == null) return false;
        String modId = modIntegration.detectModFromBlock(block);
        if (modId == null) return false;
        
        // Industrial machines, energy generators, quarries are valuable
        if (modIntegration.isIndustrialMachine(block) || 
            modIntegration.isEnergyBlock(block) || 
            modIntegration.isResourceExtractor(block)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get resource production rate for mod-based infrastructure.
     */
    public double getModResourceProduction(String nationId, String modId) {
        if (nationId == null || modId == null) return 0.0;
        // Count active mod infrastructure
        // This would require chunk scanning, simplified for now
        double baseProduction = 0.0;
        
        if ("simplyquarries".equals(modId)) {
            // Estimate based on claimed chunks
            Nation n = plugin.getNationManager() != null ? plugin.getNationManager().getNationById(nationId) : null;
            if (n != null) {
                baseProduction = n.getClaimedChunkKeys().size() * 0.1; // 0.1 resources per chunk
            }
        }
        
        return baseProduction;
    }
    
    /**
     * Get comprehensive resource statistics for a nation.
     */
    public Map<String, Object> getResourceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get resources from ResourceService
        if (resourceService != null) {
            Map<String, Double> resources = resourceService.getNationResources(nationId);
            stats.put("resources", resources);
            
            // Calculate total resource value
            double totalValue = resourceService.calculateResourceValue(nationId);
            stats.put("totalResourceValue", totalValue);
        }
        
        // Mod-specific production
        for (Map.Entry<String, String> entry : modItemToResource.entrySet()) {
            String modId = entry.getKey();
            if (modIntegration != null && modIntegration.isModAvailable(modId)) {
                double production = getModResourceProduction(nationId, modId);
                stats.put(modId + "_production", production);
            }
        }
        
        // Resource diversity (how many different mod resources)
        long diverseMods = stats.keySet().stream()
            .filter(k -> k.toString().endsWith("_production"))
            .count();
        stats.put("resourceDiversity", diverseMods);
        
        return stats;
    }
    
    /**
     * Check if nation has sufficient resources for industrial operations.
     */
    public boolean hasSufficientResources(String nationId, Map<String, Double> required) {
        if (resourceService == null) return false;
        if (required == null || required.isEmpty()) return true;
        
        Map<String, Double> available = resourceService.getNationResources(nationId);
        for (Map.Entry<String, Double> req : required.entrySet()) {
            if (available.getOrDefault(req.getKey(), 0.0) < req.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Calculate resource production efficiency.
     */
    public double calculateResourceEfficiency(String nationId) {
        double efficiency = 1.0;
        if (nationId == null) return efficiency;
        
        // Technology bonuses
        if (plugin.getTechnologyTreeService() != null) {
            efficiency *= plugin.getTechnologyTreeService().getBonus(nationId, "resourceExtraction");
        }
        
        // Energy bonus (from ModEnergyService)
        if (plugin.getModEnergyService() != null) {
            double energyProduction = plugin.getModEnergyService().getEnergyProduction(nationId);
            if (energyProduction > 1000) {
                efficiency *= 1.1; // +10% from high energy
            }
        }
        
        return efficiency;
    }
    
    /**
     * Get global mod resource statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalModResourceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Available mods
        Map<String, Boolean> availableMods = new HashMap<>();
        availableMods.put("appliedenergistics2", modIntegration != null && modIntegration.isModAvailable("appliedenergistics2"));
        availableMods.put("immersiveengineering", modIntegration != null && modIntegration.isModAvailable("immersiveengineering"));
        availableMods.put("tacz", modIntegration != null && modIntegration.isModAvailable("tacz"));
        availableMods.put("pointblank", modIntegration != null && modIntegration.isModAvailable("pointblank"));
        availableMods.put("simplyquarries", modIntegration != null && modIntegration.isModAvailable("simplyquarries"));
        availableMods.put("industrialupgrade", modIntegration != null && modIntegration.isModAvailable("industrialupgrade"));
        availableMods.put("ballistix", modIntegration != null && modIntegration.isModAvailable("ballistix"));
        availableMods.put("superwarfare", modIntegration != null && modIntegration.isModAvailable("superwarfare"));
        stats.put("availableMods", availableMods);
        
        int totalAvailableMods = (int) availableMods.values().stream().filter(b -> b).count();
        stats.put("totalAvailableMods", totalAvailableMods);
        
        // Resource statistics by nation
        Map<String, Double> totalResourceValueByNation = new HashMap<>();
        Map<String, Integer> resourceDiversityByNation = new HashMap<>();
        Map<String, Double> efficiencyByNation = new HashMap<>();
        Map<String, Map<String, Double>> productionByMod = new HashMap<>();
        
        if (plugin.getNationManager() == null) {
            stats.put("totalResourceValueByNation", totalResourceValueByNation);
            stats.put("resourceDiversityByNation", resourceDiversityByNation);
            stats.put("efficiencyByNation", efficiencyByNation);
            stats.put("productionByMod", productionByMod);
            stats.put("topByResourceValue", new ArrayList<>());
            stats.put("topByDiversity", new ArrayList<>());
            stats.put("averageResourceValue", 0);
            stats.put("averageEfficiency", 0);
            stats.put("averageDiversity", 0);
            return stats;
        }
        for (com.axiom.domain.model.Nation n : plugin.getNationManager().getAll()) {
            String nationId = n.getId();
            
            // Get resource statistics
            Map<String, Object> nationStats = getResourceStatistics(nationId);
            double totalValue = nationStats.containsKey("totalResourceValue") ? 
                (Double) nationStats.get("totalResourceValue") : 0.0;
            long diversity = nationStats.containsKey("resourceDiversity") ? 
                ((Number) nationStats.get("resourceDiversity")).longValue() : 0;
            
            totalResourceValueByNation.put(nationId, totalValue);
            resourceDiversityByNation.put(nationId, (int) diversity);
            efficiencyByNation.put(nationId, calculateResourceEfficiency(nationId));
            
            // Track production by mod
            for (String modId : modItemToResource.keySet()) {
                if (modIntegration != null && modIntegration.isModAvailable(modId)) {
                    double production = getModResourceProduction(nationId, modId);
                    productionByMod.computeIfAbsent(modId, k -> new HashMap<>()).put(nationId, production);
                }
            }
        }
        
        stats.put("totalResourceValueByNation", totalResourceValueByNation);
        stats.put("resourceDiversityByNation", resourceDiversityByNation);
        stats.put("efficiencyByNation", efficiencyByNation);
        stats.put("productionByMod", productionByMod);
        
        // Top nations by resource value
        List<Map.Entry<String, Double>> topByValue = totalResourceValueByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByResourceValue", topByValue);
        
        // Top nations by diversity
        List<Map.Entry<String, Integer>> topByDiversity = resourceDiversityByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByDiversity", topByDiversity);
        
        // Average statistics
        double totalValue = totalResourceValueByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalEfficiency = efficiencyByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalDiversity = resourceDiversityByNation.values().stream().mapToInt(Integer::intValue).sum();
        
        stats.put("averageResourceValue", totalResourceValueByNation.size() > 0 ? 
            totalValue / totalResourceValueByNation.size() : 0);
        stats.put("averageEfficiency", efficiencyByNation.size() > 0 ? 
            totalEfficiency / efficiencyByNation.size() : 0);
        stats.put("averageDiversity", resourceDiversityByNation.size() > 0 ? 
            (double) totalDiversity / resourceDiversityByNation.size() : 0);
        
        return stats;
    }
}
