package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;

import java.util.*;

/** Manages resource processing and refinement. */
public class ResourceProcessingService {
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
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        ProcessingFacility facility = new ProcessingFacility();
        facility.nationId = nationId;
        facility.resourceType = resourceType;
        facility.processingRate = 10.0; // Base rate
        facility.efficiency = 50.0;
        facility.active = true;
        facilities.put(nationId + "_" + resourceType, facility);
        n.setTreasury(n.getTreasury() - cost);
        try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        return "Перерабатывающее предприятие построено: " + resourceType;
    }

    private void processResources() {
        for (ProcessingFacility facility : facilities.values()) {
            if (!facility.active) continue;
            // Process raw resources into refined ones
            String rawResource = "raw_" + facility.resourceType;
            double consumed = facility.processingRate * (facility.efficiency / 100.0);
            if (plugin.getResourceService().consumeResource(facility.nationId, rawResource, consumed)) {
                double output = consumed * 0.8; // 80% conversion rate
                plugin.getResourceService().addResource(facility.nationId, facility.resourceType, output);
            }
        }
    }

    public synchronized double getProcessingRate(String nationId, String resourceType) {
        ProcessingFacility facility = facilities.get(nationId + "_" + resourceType);
        return facility != null && facility.active ? facility.processingRate : 0.0;
    }
}

