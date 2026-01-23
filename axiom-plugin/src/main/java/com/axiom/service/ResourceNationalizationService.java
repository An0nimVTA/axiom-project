package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;

import java.util.*;

/** Manages resource nationalization (seizing foreign-owned resources). */
public class ResourceNationalizationService {
    private final AXIOM plugin;
    private final Map<String, Set<String>> nationalizedResources = new HashMap<>(); // nationId -> set of resource types

    public ResourceNationalizationService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized String nationalizeResource(String nationId, String resourceType, double compensation) {
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(resourceType)) return "Неверные параметры.";
        if (!Double.isFinite(compensation) || compensation < 0) return "Некорректная компенсация.";
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < compensation) return "Недостаточно средств для компенсации.";
        nationalizedResources.computeIfAbsent(nationId, k -> new HashSet<>()).add(resourceType);
        n.setTreasury(n.getTreasury() - compensation);
        // Nationalization boosts resource control
        if (plugin.getResourceService() != null) {
            plugin.getResourceService().addResource(nationId, resourceType, 1000.0);
        }
        if (n.getHistory() != null) {
            n.getHistory().add("Национализированы ресурсы: " + resourceType);
        }
        try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        return "Ресурсы национализированы: " + resourceType;
    }

    public synchronized boolean isNationalized(String nationId, String resourceType) {
        if (isBlank(nationId) || isBlank(resourceType)) return false;
        Set<String> resources = nationalizedResources.get(nationId);
        return resources != null && resources.contains(resourceType);
    }

    public synchronized String denationalizeResource(String nationId, String resourceType) {
        if (isBlank(nationId) || isBlank(resourceType)) return "Неверные параметры.";
        Set<String> resources = nationalizedResources.get(nationId);
        if (resources == null || !resources.remove(resourceType)) {
            return "Ресурс не был национализирован.";
        }
        Nation n = plugin.getNationManager() != null ? plugin.getNationManager().getNationById(nationId) : null;
        if (n != null) {
            if (n.getHistory() != null) {
                n.getHistory().add("Приватизированы ресурсы: " + resourceType);
            }
            try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        }
        return "Ресурс приватизирован.";
    }
    
    /**
     * Get comprehensive resource nationalization statistics for a nation.
     */
    public synchronized Map<String, Object> getResourceNationalizationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        Set<String> resources = nationalizedResources.getOrDefault(nationId, Collections.emptySet());
        stats.put("totalNationalized", resources.size());
        stats.put("nationalizedResources", new ArrayList<>(resources));
        
        // Nationalization rating
        String rating = "НЕТ НАЦИОНАЛИЗАЦИИ";
        if (resources.size() >= 10) rating = "МАССОВАЯ";
        else if (resources.size() >= 5) rating = "РАСШИРЕННАЯ";
        else if (resources.size() >= 3) rating = "АКТИВНАЯ";
        else if (resources.size() >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global resource nationalization statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalResourceNationalizationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalNationalized = 0;
        Map<String, Integer> nationalizedByNation = new HashMap<>();
        Map<String, Integer> resourcesByType = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : nationalizedResources.entrySet()) {
            String nationId = entry.getKey();
            Set<String> resources = entry.getValue();
            if (resources == null) continue;
            
            totalNationalized += resources.size();
            nationalizedByNation.put(nationId, resources.size());
            
            for (String resource : resources) {
                if (resource != null) {
                    resourcesByType.put(resource, resourcesByType.getOrDefault(resource, 0) + 1);
                }
            }
        }
        
        stats.put("totalNationalized", totalNationalized);
        stats.put("nationalizedByNation", nationalizedByNation);
        stats.put("resourcesByType", resourcesByType);
        stats.put("nationsWithNationalization", nationalizedByNation.size());
        
        // Average nationalized per nation
        stats.put("averageNationalizedPerNation", nationalizedByNation.size() > 0 ? 
            (double) totalNationalized / nationalizedByNation.size() : 0);
        
        // Top nations by nationalization
        List<Map.Entry<String, Integer>> topByNationalized = nationalizedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByNationalized", topByNationalized);
        
        // Most nationalized resource types
        List<Map.Entry<String, Integer>> topByType = resourcesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByType", topByType);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

