package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manages military units and strength. */
public class MilitaryService {
    private final AXIOM plugin;
    private final File militaryDir;
    private final Map<String, MilitaryData> nationMilitary = new HashMap<>(); // nationId -> data

    public static class MilitaryData {
        int infantry;
        int cavalry;
        int artillery;
        int navy;
        int airForce;
        double strength; // calculated total
    }

    public MilitaryService(AXIOM plugin) {
        this.plugin = plugin;
        this.militaryDir = new File(plugin.getDataFolder(), "military");
        this.militaryDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateStrength, 0, 20 * 60 * 5); // every 5 minutes
    }

    public synchronized String recruitUnits(String nationId, String unitType, int count, double cost) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (count <= 0 || cost <= 0) return "Некорректные параметры найма.";
        if (!canRecruitMore(nationId, count)) return "Достигнут лимит военной вместимости.";
        if (n.getTreasury() < cost * count) return "Недостаточно средств.";
        MilitaryData md = nationMilitary.computeIfAbsent(nationId, k -> new MilitaryData());
        switch (unitType.toLowerCase()) {
            case "infantry": md.infantry += count; break;
            case "cavalry": md.cavalry += count; break;
            case "artillery": md.artillery += count; break;
            case "navy": md.navy += count; break;
            case "airforce": md.airForce += count; break;
            default: return "Неизвестный тип войск.";
        }
        n.setTreasury(n.getTreasury() - cost * count);
        updateNationStrength(nationId);
        try {
            plugin.getNationManager().save(n);
            saveMilitary(nationId, md);
        } catch (Exception ignored) {}
        return "Нанято: " + count + " " + unitType;
    }

    private void updateStrength() {
        for (String nationId : nationMilitary.keySet()) {
            updateNationStrength(nationId);
        }
    }

    private void updateNationStrength(String nationId) {
        MilitaryData md = nationMilitary.get(nationId);
        if (md == null) return;
        
        // Base strength calculation
        double baseStrength = md.infantry * 1.0 + md.cavalry * 1.5 + md.artillery * 2.0 + md.navy * 2.5 + md.airForce * 3.0;
        
        // MOD INTEGRATION: Apply mod-based bonuses
        double modBonus = 1.0;
        
        // Technology tree bonuses
        if (plugin.getTechnologyTreeService() != null) {
            double techBonus = plugin.getTechnologyTreeService().getBonus(nationId, "warStrength");
            modBonus *= techBonus;
        }
        
        // Mod availability bonuses
        if (plugin.getModIntegrationService() != null && plugin.getTechnologyTreeService() != null) {
            if (plugin.getModIntegrationService().isTaczAvailable() || 
                plugin.getModIntegrationService().isPointBlankAvailable()) {
                // Firearms technology bonus
                if (plugin.getTechnologyTreeService().getBonus(nationId, "weaponDamage") > 1.0) {
                    modBonus *= 1.1; // +10% if firearms tech researched
                }
            }
            
            if (plugin.getModIntegrationService().isBallistixAvailable()) {
                // Artillery bonus
                if (plugin.getTechnologyTreeService().getBonus(nationId, "siegeStrength") > 1.0) {
                    modBonus *= 1.15; // +15% if artillery tech researched
                }
            }
            
            if (plugin.getModIntegrationService().isSuperWarfareAvailable()) {
                // Military vehicles bonus
                if (plugin.getTechnologyTreeService().getBonus(nationId, "mobility") > 1.0) {
                    modBonus *= 1.2; // +20% if vehicles tech researched
                }
            }
            
            // Elite equipment bonus
            if (plugin.getModIntegrationService().isModAvailable("capsawims") ||
                plugin.getModIntegrationService().isModAvailable("warium")) {
                if (plugin.getTechnologyTreeService().getBonus(nationId, "defenseBonus") > 1.0) {
                    modBonus *= 1.05; // +5% if elite equipment tech researched
                }
            }
        }
        
        md.strength = baseStrength * modBonus;
        saveMilitary(nationId, md);
    }

    private void loadAll() {
        File[] files = militaryDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                MilitaryData md = new MilitaryData();
                md.infantry = o.has("infantry") ? o.get("infantry").getAsInt() : 0;
                md.cavalry = o.has("cavalry") ? o.get("cavalry").getAsInt() : 0;
                md.artillery = o.has("artillery") ? o.get("artillery").getAsInt() : 0;
                md.navy = o.has("navy") ? o.get("navy").getAsInt() : 0;
                md.airForce = o.has("airForce") ? o.get("airForce").getAsInt() : 0;
                md.strength = o.has("strength") ? o.get("strength").getAsDouble() : 0;
                nationMilitary.put(nationId, md);
            } catch (Exception ignored) {}
        }
    }

    private void saveMilitary(String nationId, MilitaryData md) {
        File f = new File(militaryDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("infantry", md.infantry);
        o.addProperty("cavalry", md.cavalry);
        o.addProperty("artillery", md.artillery);
        o.addProperty("navy", md.navy);
        o.addProperty("airForce", md.airForce);
        o.addProperty("strength", md.strength);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    public synchronized double getMilitaryStrength(String nationId) {
        MilitaryData md = nationMilitary.get(nationId);
        return md != null ? md.strength : 0.0;
    }
    
    /**
     * Get comprehensive military statistics for a nation.
     */
    public synchronized Map<String, Object> getMilitaryStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        MilitaryData md = nationMilitary.get(nationId);
        
        if (md == null) {
            stats.put("totalUnits", 0);
            stats.put("infantry", 0);
            stats.put("cavalry", 0);
            stats.put("artillery", 0);
            stats.put("navy", 0);
            stats.put("airForce", 0);
            stats.put("strength", 0.0);
            return stats;
        }
        
        stats.put("totalUnits", md.infantry + md.cavalry + md.artillery + md.navy + md.airForce);
        stats.put("infantry", md.infantry);
        stats.put("cavalry", md.cavalry);
        stats.put("artillery", md.artillery);
        stats.put("navy", md.navy);
        stats.put("airForce", md.airForce);
        stats.put("strength", md.strength);
        
        // Calculate unit composition percentages
        int total = md.infantry + md.cavalry + md.artillery + md.navy + md.airForce;
        if (total > 0) {
            stats.put("infantryPercentage", (md.infantry / (double) total) * 100);
            stats.put("cavalryPercentage", (md.cavalry / (double) total) * 100);
            stats.put("artilleryPercentage", (md.artillery / (double) total) * 100);
            stats.put("navyPercentage", (md.navy / (double) total) * 100);
            stats.put("airForcePercentage", (md.airForce / (double) total) * 100);
        }
        
        // Mod bonuses
        if (plugin.getModWarfareService() != null) {
            double modBonus = plugin.getModWarfareService().getAverageMilitaryBonus(nationId);
            stats.put("modBonus", modBonus);
            stats.put("warfarePotential", plugin.getModWarfareService().calculateWarfarePotential(nationId));
        }
        
        // Technology bonuses
        if (plugin.getTechnologyTreeService() != null) {
            double warTech = plugin.getTechnologyTreeService().getBonus(nationId, "warStrength");
            stats.put("technologyBonus", warTech);
        }
        
        return stats;
    }
    
    /**
     * Calculate military maintenance cost per hour.
     */
    public synchronized double calculateMaintenanceCost(String nationId) {
        MilitaryData md = nationMilitary.get(nationId);
        if (md == null) return 0.0;
        
        // Base cost per unit type
        double cost = md.infantry * 0.1;
        cost += md.cavalry * 0.15;
        cost += md.artillery * 0.25;
        cost += md.navy * 0.30;
        cost += md.airForce * 0.40;
        
        return cost;
    }
    
    /**
     * Get all unit counts for a nation.
     */
    public synchronized MilitaryData getMilitaryData(String nationId) {
        return nationMilitary.get(nationId);
    }
    
    /**
     * Disband units (free up resources).
     */
    public synchronized String disbandUnits(String nationId, String unitType, int count) throws IOException {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (count <= 0) return "Некорректное количество.";
        
        MilitaryData md = nationMilitary.get(nationId);
        if (md == null) return "Нет военных единиц.";
        
        int current = 0;
        switch (unitType.toLowerCase()) {
            case "infantry": current = md.infantry; break;
            case "cavalry": current = md.cavalry; break;
            case "artillery": current = md.artillery; break;
            case "navy": current = md.navy; break;
            case "airforce": current = md.airForce; break;
            default: return "Неизвестный тип войск.";
        }
        
        if (current < count) return "Недостаточно единиц для расформирования.";
        
        switch (unitType.toLowerCase()) {
            case "infantry": md.infantry -= count; break;
            case "cavalry": md.cavalry -= count; break;
            case "artillery": md.artillery -= count; break;
            case "navy": md.navy -= count; break;
            case "airforce": md.airForce -= count; break;
        }
        
        // Refund part of cost (50%)
        double refund = count * 0.5; // Simplified refund
        n.setTreasury(n.getTreasury() + refund);
        
        updateNationStrength(nationId);
        plugin.getNationManager().save(n);
        saveMilitary(nationId, md);
        
        return "Расформировано: " + count + " " + unitType + ". Возвращено: " + refund;
    }
    
    /**
     * Upgrade military units (improve quality/efficiency).
     */
    public synchronized String upgradeUnits(String nationId, String unitType, int count, double cost) throws IOException {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (count <= 0 || cost <= 0) return "Некорректные параметры улучшения.";
        if (n.getTreasury() < cost * count) return "Недостаточно средств.";
        
        MilitaryData md = nationMilitary.get(nationId);
        if (md == null) return "Нет военных единиц для улучшения.";
        
        int available = 0;
        switch (unitType.toLowerCase()) {
            case "infantry": available = md.infantry; break;
            case "cavalry": available = md.cavalry; break;
            case "artillery": available = md.artillery; break;
            case "navy": available = md.navy; break;
            case "airforce": available = md.airForce; break;
            default: return "Неизвестный тип войск.";
        }
        
        if (available < count) return "Недостаточно единиц для улучшения.";
        
        n.setTreasury(n.getTreasury() - cost * count);
        updateNationStrength(nationId); // Upgraded units increase strength
        plugin.getNationManager().save(n);
        saveMilitary(nationId, md);
        
        return "Улучшено: " + count + " " + unitType + ". Сила увеличена.";
    }
    
    /**
     * Calculate total military capacity (max units that can be recruited).
     */
    public synchronized int getMilitaryCapacity(String nationId) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return 0;
        
        // Base capacity from population (estimated from citizens * 100)
        int baseCapacity = n.getCitizens().size() * 100;
        
        // Technology bonuses
        if (plugin.getTechnologyTreeService() != null) {
            double capacityBonus = plugin.getTechnologyTreeService().getBonus(nationId, "militaryCapacity");
            baseCapacity = (int) (baseCapacity * capacityBonus);
        }
        
        return baseCapacity;
    }
    
    /**
     * Check if nation can recruit more units.
     */
    public synchronized boolean canRecruitMore(String nationId, int additionalUnits) {
        MilitaryData md = nationMilitary.get(nationId);
        int currentTotal = md != null ? 
            (md.infantry + md.cavalry + md.artillery + md.navy + md.airForce) : 0;
        
        return (currentTotal + additionalUnits) <= getMilitaryCapacity(nationId);
    }
    
    /**
     * Get global military statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalMilitaryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalInfantry = 0;
        int totalCavalry = 0;
        int totalArtillery = 0;
        int totalNavy = 0;
        int totalAirForce = 0;
        double totalStrength = 0.0;
        double maxStrength = 0.0;
        
        for (MilitaryData md : nationMilitary.values()) {
            totalInfantry += md.infantry;
            totalCavalry += md.cavalry;
            totalArtillery += md.artillery;
            totalNavy += md.navy;
            totalAirForce += md.airForce;
            totalStrength += md.strength;
            maxStrength = Math.max(maxStrength, md.strength);
        }
        
        int totalUnits = totalInfantry + totalCavalry + totalArtillery + totalNavy + totalAirForce;
        
        stats.put("totalUnits", totalUnits);
        stats.put("totalInfantry", totalInfantry);
        stats.put("totalCavalry", totalCavalry);
        stats.put("totalArtillery", totalArtillery);
        stats.put("totalNavy", totalNavy);
        stats.put("totalAirForce", totalAirForce);
        stats.put("totalStrength", totalStrength);
        stats.put("maxStrength", maxStrength);
        stats.put("averageStrength", nationMilitary.size() > 0 ? totalStrength / nationMilitary.size() : 0);
        
        // Unit composition percentages
        if (totalUnits > 0) {
            stats.put("infantryPercentage", (totalInfantry / (double) totalUnits) * 100);
            stats.put("cavalryPercentage", (totalCavalry / (double) totalUnits) * 100);
            stats.put("artilleryPercentage", (totalArtillery / (double) totalUnits) * 100);
            stats.put("navyPercentage", (totalNavy / (double) totalUnits) * 100);
            stats.put("airForcePercentage", (totalAirForce / (double) totalUnits) * 100);
        }
        
        // Top militaries by strength
        List<Map.Entry<String, Double>> topMilitaries = new ArrayList<>();
        for (Map.Entry<String, MilitaryData> entry : nationMilitary.entrySet()) {
            topMilitaries.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().strength));
        }
        topMilitaries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        stats.put("topMilitariesByStrength", topMilitaries.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        // Nations with active militaries
        stats.put("nationsWithMilitary", nationMilitary.size());
        
        return stats;
    }
    
    /**
     * Get military leaderboard.
     */
    public synchronized List<Map.Entry<String, Double>> getMilitaryLeaderboard(int limit) {
        List<Map.Entry<String, Double>> rankings = new ArrayList<>();
        
        for (Map.Entry<String, MilitaryData> entry : nationMilitary.entrySet()) {
            rankings.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().strength));
        }
        
        return rankings.stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get nations with largest militaries by unit count.
     */
    public synchronized List<Map.Entry<String, Integer>> getTopNationsByUnitCount(int limit) {
        List<Map.Entry<String, Integer>> rankings = new ArrayList<>();
        
        for (Map.Entry<String, MilitaryData> entry : nationMilitary.entrySet()) {
            int total = entry.getValue().infantry + entry.getValue().cavalry + 
                       entry.getValue().artillery + entry.getValue().navy + 
                       entry.getValue().airForce;
            rankings.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), total));
        }
        
        return rankings.stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get total maintenance cost for all nations.
     */
    public synchronized double getTotalMaintenanceCost() {
        double total = 0.0;
        for (String nationId : nationMilitary.keySet()) {
            total += calculateMaintenanceCost(nationId);
        }
        return total;
    }
}

