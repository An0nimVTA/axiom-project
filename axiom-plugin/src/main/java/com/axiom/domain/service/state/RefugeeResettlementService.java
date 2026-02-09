package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages refugee resettlement programs. */
public class RefugeeResettlementService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File resettlementDir;
    private final Map<String, ResettlementProgram> programs = new HashMap<>(); // nationId -> program

    public static class ResettlementProgram {
        String nationId;
        int refugeeCapacity;
        int currentRefugees;
        double fundingPerRefugee;
        boolean active;
    }

    public RefugeeResettlementService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.resettlementDir = new File(plugin.getDataFolder(), "resettlement");
        this.resettlementDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processResettlement, 0, 20 * 60 * 15); // every 15 minutes
    }

    public synchronized String createProgram(String nationId, int capacity, double funding) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId)) return "Неверный идентификатор нации.";
        if (capacity < 0) return "Вместимость не может быть отрицательной.";
        if (funding < 0) return "Финансирование не может быть отрицательным.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        ResettlementProgram program = programs.computeIfAbsent(nationId, k -> new ResettlementProgram());
        program.nationId = nationId;
        program.refugeeCapacity = capacity;
        if (program.currentRefugees < 0) program.currentRefugees = 0;
        program.fundingPerRefugee = funding;
        program.active = true;
        if (n.getHistory() != null) {
            n.getHistory().add("Программа переселения беженцев создана");
        }
        try {
            nationManager.save(n);
            saveProgram(nationId, program);
        } catch (Exception ignored) {}
        return "Программа переселения создана (вместимость: " + capacity + ")";
    }

    public synchronized String acceptRefugees(String nationId, String sourceNationId, int count) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(sourceNationId)) return "Неверный идентификатор нации.";
        if (count <= 0) return "Количество беженцев должно быть больше нуля.";
        ResettlementProgram program = programs.get(nationId);
        if (program == null || !program.active) return "Программа не найдена.";
        if (program.currentRefugees + count > program.refugeeCapacity) {
            return "Недостаточно места (" + (program.refugeeCapacity - program.currentRefugees) + " доступно)";
        }
        Nation n = nationManager.getNationById(nationId);
        Nation source = nationManager.getNationById(sourceNationId);
        if (n == null || source == null) return "Нация не найдена.";
        double cost = count * program.fundingPerRefugee;
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость переселения.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        program.currentRefugees += count;
        n.setTreasury(n.getTreasury() - cost);
        // Refugees boost population and culture
        if (plugin.getCultureService() != null) {
            plugin.getCultureService().developCulture(nationId, count * 0.1);
        }
        if (n.getHistory() != null) {
            n.getHistory().add("Принято беженцев: " + count + " из " + source.getName());
        }
        try {
            nationManager.save(n);
            saveProgram(nationId, program);
        } catch (Exception ignored) {}
        return "Беженцы приняты: " + count;
    }

    private synchronized void processResettlement() {
        // Gradually integrate refugees into population
        for (Map.Entry<String, ResettlementProgram> e : programs.entrySet()) {
            ResettlementProgram program = e.getValue();
            if (program == null) continue;
            if (!program.active || program.currentRefugees <= 0) continue;
            // Refugees gradually become citizens (simplified)
            if (Math.random() < 0.1) { // 10% chance per 15 minutes
                program.currentRefugees = Math.max(0, program.currentRefugees - 1);
                saveProgram(e.getKey(), program);
            }
        }
    }

    private void loadAll() {
        File[] files = resettlementDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                ResettlementProgram program = new ResettlementProgram();
                program.nationId = nationId;
                program.refugeeCapacity = o.get("refugeeCapacity").getAsInt();
                program.currentRefugees = o.get("currentRefugees").getAsInt();
                program.fundingPerRefugee = o.get("fundingPerRefugee").getAsDouble();
                program.active = o.has("active") && o.get("active").getAsBoolean();
                programs.put(nationId, program);
            } catch (Exception ignored) {}
        }
    }

    private void saveProgram(String nationId, ResettlementProgram program) {
        if (isBlank(nationId) || program == null) return;
        File f = new File(resettlementDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", program.nationId);
        o.addProperty("refugeeCapacity", program.refugeeCapacity);
        o.addProperty("currentRefugees", program.currentRefugees);
        o.addProperty("fundingPerRefugee", program.fundingPerRefugee);
        o.addProperty("active", program.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive refugee resettlement statistics for a nation.
     */
    public synchronized Map<String, Object> getRefugeeResettlementStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        ResettlementProgram program = programs.get(nationId);
        if (program == null || !program.active) {
            stats.put("hasProgram", false);
            return stats;
        }
        
        stats.put("hasProgram", true);
        stats.put("refugeeCapacity", program.refugeeCapacity);
        stats.put("currentRefugees", program.currentRefugees);
        stats.put("availableCapacity", program.refugeeCapacity - program.currentRefugees);
        stats.put("fundingPerRefugee", program.fundingPerRefugee);
        stats.put("totalFunding", program.currentRefugees * program.fundingPerRefugee);
        stats.put("utilizationRate", program.refugeeCapacity > 0 ? 
            (double) program.currentRefugees / program.refugeeCapacity : 0);
        
        // Program rating
        double utilization = stats.containsKey("utilizationRate") ? (Double) stats.get("utilizationRate") : 0.0;
        String rating = "НЕАКТИВНАЯ";
        if (program.active && utilization >= 0.9) rating = "ПОЛНАЯ";
        else if (program.active && utilization >= 0.7) rating = "АКТИВНАЯ";
        else if (program.active && utilization >= 0.5) rating = "УМЕРЕННАЯ";
        else if (program.active) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global refugee resettlement statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalRefugeeResettlementStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPrograms = 0;
        int activePrograms = 0;
        int totalCapacity = 0;
        int totalRefugees = 0;
        double totalFunding = 0.0;
        Map<String, Integer> capacityByNation = new HashMap<>();
        Map<String, Integer> refugeesByNation = new HashMap<>();
        Map<String, Double> fundingByNation = new HashMap<>();
        
        for (Map.Entry<String, ResettlementProgram> entry : programs.entrySet()) {
            String nationId = entry.getKey();
            ResettlementProgram program = entry.getValue();
            if (program == null) continue;
            
            if (program.active) {
                totalPrograms++;
                activePrograms++;
                totalCapacity += program.refugeeCapacity;
                totalRefugees += program.currentRefugees;
                double funding = program.currentRefugees * program.fundingPerRefugee;
                totalFunding += funding;
                
                capacityByNation.put(nationId, program.refugeeCapacity);
                refugeesByNation.put(nationId, program.currentRefugees);
                fundingByNation.put(nationId, funding);
            }
        }
        
        stats.put("totalPrograms", totalPrograms);
        stats.put("activePrograms", activePrograms);
        stats.put("totalCapacity", totalCapacity);
        stats.put("totalRefugees", totalRefugees);
        stats.put("totalFunding", totalFunding);
        stats.put("capacityByNation", capacityByNation);
        stats.put("refugeesByNation", refugeesByNation);
        stats.put("fundingByNation", fundingByNation);
        
        // Average statistics
        stats.put("averageCapacity", activePrograms > 0 ? (double) totalCapacity / activePrograms : 0);
        stats.put("averageRefugees", activePrograms > 0 ? (double) totalRefugees / activePrograms : 0);
        stats.put("globalUtilizationRate", totalCapacity > 0 ? (double) totalRefugees / totalCapacity : 0);
        
        // Top nations by capacity
        List<Map.Entry<String, Integer>> topByCapacity = capacityByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCapacity", topByCapacity);
        
        // Top nations by refugees
        List<Map.Entry<String, Integer>> topByRefugees = refugeesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRefugees", topByRefugees);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

