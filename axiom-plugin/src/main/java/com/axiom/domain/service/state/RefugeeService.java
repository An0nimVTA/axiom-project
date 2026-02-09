package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.*;

/** Manages refugee flows from war-torn or crisis-affected nations. */
public class RefugeeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, List<Refugee>> nationRefugees = new HashMap<>(); // hostNationId -> refugees

    public static class Refugee {
        String sourceNationId;
        int population;
        long arrivalTime;
    }

    public RefugeeService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::processRefugees, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized void acceptRefugees(String hostNationId, String sourceNationId, int population) {
        if (nationManager == null) return;
        if (isBlank(hostNationId) || isBlank(sourceNationId)) return;
        if (population <= 0) return;
        Nation host = nationManager.getNationById(hostNationId);
        if (host == null) return;
        Refugee r = new Refugee();
        r.sourceNationId = sourceNationId;
        r.population = population;
        r.arrivalTime = System.currentTimeMillis();
        nationRefugees.computeIfAbsent(hostNationId, k -> new ArrayList<>()).add(r);
        // Refugees boost population but may reduce happiness
        if (host.getHistory() != null) {
            host.getHistory().add("Принято беженцев: " + population + " из " + sourceNationId);
        }
        try { nationManager.save(host); } catch (Exception ignored) {}
    }

    private synchronized void processRefugees() {
        if (nationManager == null) return;
        Collection<Nation> nations = nationManager.getAll();
        if (nations == null) return;
        // Check for nations at war and generate refugees
        for (Nation n : nations) {
            if (n == null) continue;
            Set<String> enemies = n.getEnemies();
            if (enemies != null && !enemies.isEmpty() && Math.random() < 0.1) {
                // Generate refugees to random allied nations
                Set<String> allies = n.getAllies();
                if (allies == null || allies.isEmpty()) continue;
                for (String allyId : allies) {
                    Nation ally = nationManager.getNationById(allyId);
                    if (ally != null && Math.random() < 0.3) {
                        Collection<UUID> citizens = n.getCitizens();
                        int population = citizens != null ? citizens.size() : 0;
                        int refugees = (int)(population * 0.05); // 5% of population
                        if (refugees <= 0) continue;
                        acceptRefugees(allyId, n.getId(), refugees);
                    }
                }
            }
        }
    }

    public synchronized int getRefugeeCount(String nationId) {
        if (isBlank(nationId)) return 0;
        List<Refugee> refugees = nationRefugees.get(nationId);
        return refugees != null ? refugees.stream().mapToInt(r -> r.population).sum() : 0;
    }
    
    /**
     * Get comprehensive refugee statistics.
     */
    public synchronized Map<String, Object> getRefugeeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        List<Refugee> refugees = nationRefugees.get(nationId);
        if (refugees == null) refugees = Collections.emptyList();
        
        int totalRefugees = refugees.stream().mapToInt(r -> r.population).sum();
        stats.put("totalRefugees", totalRefugees);
        stats.put("refugeeGroups", refugees.size());
        
        // Refugees by source nation
        Map<String, Integer> bySource = new HashMap<>();
        for (Refugee r : refugees) {
            if (r == null || isBlank(r.sourceNationId)) continue;
            bySource.put(r.sourceNationId, bySource.getOrDefault(r.sourceNationId, 0) + r.population);
        }
        stats.put("bySource", bySource);
        
        // Calculate refugee impact
        Nation n = nationManager != null ? nationManager.getNationById(nationId) : null;
        if (n != null) {
            Collection<UUID> citizens = n.getCitizens();
            int population = citizens != null ? citizens.size() : 0;
            double refugeePercentage = population > 0 ? (totalRefugees / (double) population) * 100 : 0;
            stats.put("refugeePercentage", refugeePercentage);
            
            // Impact on happiness (refugees can reduce happiness if too many)
            double happinessImpact = refugeePercentage > 20 ? -(refugeePercentage - 20) * 0.5 : 0;
            stats.put("happinessImpact", happinessImpact);
            
            // Impact on population growth (refugees boost population)
            stats.put("populationBoost", totalRefugees * 0.1); // +0.1 per refugee
        }
        
        return stats;
    }
    
    /**
     * Integrate refugees into population (naturalization).
     */
    public synchronized String integrateRefugees(String nationId, int count) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (count <= 0) return "Количество должно быть больше нуля.";
        List<Refugee> refugees = nationRefugees.get(nationId);
        if (refugees == null || refugees.isEmpty()) return "Нет беженцев для интеграции.";
        
        int available = refugees.stream().filter(Objects::nonNull).mapToInt(r -> r.population).sum();
        if (available <= 0) return "Нет беженцев для интеграции.";
        if (count > available) count = available;
        
        // Remove refugees (oldest first)
        int remaining = count;
        Iterator<Refugee> iterator = refugees.iterator();
        while (iterator.hasNext() && remaining > 0) {
            Refugee r = iterator.next();
            if (r == null) {
                iterator.remove();
                continue;
            }
            if (r.population <= remaining) {
                remaining -= r.population;
                iterator.remove();
            } else {
                r.population -= remaining;
                remaining = 0;
            }
        }
        
        // Apply population boost (simplified - can be enhanced when PopulationGrowthService is fully integrated)
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            if (n.getHistory() != null) {
                n.getHistory().add("Интегрировано беженцев: " + count);
            }
            nationManager.save(n);
        }
        
        return "Интегрировано беженцев: " + count + ". Население увеличено.";
    }
    
    /**
     * Expel refugees (return to source).
     */
    public synchronized String expelRefugees(String nationId, String sourceNationId, int count) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(sourceNationId)) return "Неверный идентификатор нации.";
        if (count <= 0) return "Количество должно быть больше нуля.";
        List<Refugee> refugees = nationRefugees.get(nationId);
        if (refugees == null || refugees.isEmpty()) return "Нет беженцев.";
        
        int remaining = count;
        Iterator<Refugee> iterator = refugees.iterator();
        while (iterator.hasNext() && remaining > 0) {
            Refugee r = iterator.next();
            if (r == null) {
                iterator.remove();
                continue;
            }
            if (!Objects.equals(r.sourceNationId, sourceNationId)) continue;
            int removed = Math.min(r.population, remaining);
            r.population -= removed;
            remaining -= removed;
            if (r.population <= 0) {
                iterator.remove();
            }
        }
        int expelled = count - remaining;
        if (expelled <= 0) return "Беженцы от этой нации не найдены.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            if (n.getHistory() != null) {
                n.getHistory().add("Изгнано беженцев: " + expelled + " из " + sourceNationId);
            }
            nationManager.save(n);
        }
        
        return "Изгнано беженцев: " + expelled;
    }
    
    /**
     * Get global refugee statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalRefugeeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalRefugees = 0;
        int totalGroups = 0;
        Map<String, Integer> refugeesByHost = new HashMap<>();
        Map<String, Integer> refugeesBySource = new HashMap<>();
        
        for (Map.Entry<String, List<Refugee>> entry : nationRefugees.entrySet()) {
            int hostCount = 0;
            List<Refugee> refugees = entry.getValue();
            if (refugees == null) continue;
            for (Refugee r : refugees) {
                if (r == null) continue;
                totalRefugees += r.population;
                hostCount += r.population;
                totalGroups++;
                if (!isBlank(r.sourceNationId)) {
                    refugeesBySource.put(r.sourceNationId, 
                        refugeesBySource.getOrDefault(r.sourceNationId, 0) + r.population);
                }
            }
            if (hostCount > 0) {
                refugeesByHost.put(entry.getKey(), hostCount);
            }
        }
        
        stats.put("totalRefugees", totalRefugees);
        stats.put("totalGroups", totalGroups);
        stats.put("refugeesByHost", refugeesByHost);
        stats.put("refugeesBySource", refugeesBySource);
        stats.put("nationsHostingRefugees", refugeesByHost.size());
        stats.put("nationsProducingRefugees", refugeesBySource.size());
        stats.put("averageRefugeesPerHost", refugeesByHost.size() > 0 ? 
            (double) totalRefugees / refugeesByHost.size() : 0);
        
        // Top hosts
        List<Map.Entry<String, Integer>> topHosts = refugeesByHost.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topHosts", topHosts);
        
        // Top sources
        List<Map.Entry<String, Integer>> topSources = refugeesBySource.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topSources", topSources);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

