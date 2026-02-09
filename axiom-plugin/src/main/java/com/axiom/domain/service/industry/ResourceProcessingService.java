package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;

import java.util.*;

/** Manages resource processing and refinement. */
public class ResourceProcessingService {
    private static final double PROCESS_INTERVAL_MINUTES = 10.0;
    private final AXIOM plugin;
    private final Map<String, ProcessingFacility> facilities = new HashMap<>(); // nationId -> facility

    public static class ProcessingFacility {
        String nationId;
        String resourceType;
        double processingRate; // per hour
        double efficiency; // 0-100%
        boolean active;
    }

    public ResourceProcessingService(AXIOM plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processResources, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String buildProcessingFacility(String nationId, String resourceType, double cost) {
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(resourceType)) return "Неверные параметры.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        ResourceCatalogService.ProcessingRecipe recipe = getRecipe(resourceType);
        if (recipe == null) {
            return "Рецепт переработки не найден: " + resourceType;
        }
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        ProcessingFacility facility = new ProcessingFacility();
        facility.nationId = nationId;
        facility.resourceType = resourceType;
        facility.processingRate = recipe.baseRatePerHour;
        facility.efficiency = recipe.baseEfficiency;
        facility.active = true;
        facilities.put(nationId + "_" + resourceType, facility);
        n.setTreasury(n.getTreasury() - cost);
        try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        return "Перерабатывающее предприятие построено: " + resourceType;
    }

    private synchronized void processResources() {
        if (plugin.getResourceService() == null) return;
        for (ProcessingFacility facility : facilities.values()) {
            if (facility == null || isBlank(facility.nationId) || isBlank(facility.resourceType)) continue;
            if (!facility.active) continue;
            ResourceCatalogService.ProcessingRecipe recipe = getRecipe(facility.resourceType);
            if (recipe == null || recipe.inputs.isEmpty()) continue;
            double efficiency = Math.max(0.0, Math.min(100.0, facility.efficiency));
            double unitsPerHour = facility.processingRate * (efficiency / 100.0);
            double unitsThisCycle = unitsPerHour * (PROCESS_INTERVAL_MINUTES / 60.0);
            if (!Double.isFinite(unitsThisCycle) || unitsThisCycle <= 0) continue;

            double maxUnits = unitsThisCycle;
            for (Map.Entry<String, Double> input : recipe.inputs.entrySet()) {
                double perUnit = input.getValue();
                if (perUnit <= 0) continue;
                double available = plugin.getResourceService().getResource(facility.nationId, input.getKey());
                maxUnits = Math.min(maxUnits, available / perUnit);
            }
            if (maxUnits <= 0) continue;

            for (Map.Entry<String, Double> input : recipe.inputs.entrySet()) {
                double perUnit = input.getValue();
                if (perUnit <= 0) continue;
                plugin.getResourceService().consumeResource(facility.nationId, input.getKey(), perUnit * maxUnits);
            }

            double output = maxUnits * recipe.outputAmount;
            if (output > 0) {
                plugin.getResourceService().addResource(facility.nationId, facility.resourceType, output);
            }
        }
    }

    public synchronized double getProcessingRate(String nationId, String resourceType) {
        if (isBlank(nationId) || isBlank(resourceType)) return 0.0;
        ProcessingFacility facility = facilities.get(nationId + "_" + resourceType);
        return facility != null && facility.active ? facility.processingRate : 0.0;
    }

    public synchronized boolean hasFacility(String nationId, String resourceType) {
        if (isBlank(nationId) || isBlank(resourceType)) return false;
        ProcessingFacility facility = facilities.get(nationId + "_" + resourceType);
        return facility != null && facility.active;
    }

    private ResourceCatalogService.ProcessingRecipe getRecipe(String outputResource) {
        ResourceCatalogService catalog = plugin.getResourceCatalogService();
        return catalog != null ? catalog.getProcessingRecipe(outputResource) : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

