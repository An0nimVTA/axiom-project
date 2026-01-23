package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages spy networks and intelligence gathering. */
public class SpyService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File spiesDir;
    private final Map<String, SpyNetwork> networks = new HashMap<>(); // nationId_targetNationId -> network

    public static class SpyNetwork {
        String ownerNationId;
        String targetNationId;
        int level; // 1-5
        double successChance;
        long lastMission;
    }

    public SpyService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.spiesDir = new File(plugin.getDataFolder(), "spies");
        this.spiesDir.mkdirs();
        loadAll();
    }

    public synchronized String createSpyNetwork(String ownerId, String targetId, double cost) {
        Nation owner = nationManager.getNationById(ownerId);
        if (owner == null) return "Нация не найдена.";
        if (owner.getTreasury() < cost) return "Недостаточно средств.";
        String key = ownerId + "_" + targetId;
        if (networks.containsKey(key)) return "Шпионская сеть уже существует.";
        SpyNetwork sn = new SpyNetwork();
        sn.ownerNationId = ownerId;
        sn.targetNationId = targetId;
        sn.level = 1;
        sn.successChance = 0.2;
        sn.lastMission = 0;
        networks.put(key, sn);
        owner.setTreasury(owner.getTreasury() - cost);
        try {
            nationManager.save(owner);
            saveNetwork(sn);
        } catch (Exception ignored) {}
        return "Шпионская сеть создана (уровень 1).";
    }

    public synchronized String conductMission(String ownerId, String targetId, String missionType) {
        String key = ownerId + "_" + targetId;
        SpyNetwork sn = networks.get(key);
        if (sn == null) return "Шпионская сеть не найдена.";
        if (System.currentTimeMillis() - sn.lastMission < 60 * 60_000L) return "Сеть на перезарядке (1 час).";
        sn.lastMission = System.currentTimeMillis();
        boolean success = Math.random() < sn.successChance;
        Nation target = nationManager.getNationById(targetId);
        if (success && target != null) {
            String info = "Разведданные получены: ";
            switch (missionType.toLowerCase()) {
                case "treasury": info += "Казна: " + target.getTreasury(); break;
                case "military": info += "Военная сила: " + plugin.getMilitaryService().getMilitaryStrength(targetId); break;
                case "resources": info += "Ресурсы доступны"; break;
                default: info += "Общая информация";
            }
            Nation owner = nationManager.getNationById(ownerId);
            if (owner != null) owner.getHistory().add(info);
            target.getHistory().add("Обнаружена шпионская активность!");
            try {
                if (owner != null) nationManager.save(owner);
                nationManager.save(target);
                saveNetwork(sn);
            } catch (Exception ignored) {}
            return info;
        }
        saveNetwork(sn);
        return "Миссия провалена.";
    }

    private void loadAll() {
        File[] files = spiesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                SpyNetwork sn = new SpyNetwork();
                sn.ownerNationId = o.get("ownerNationId").getAsString();
                sn.targetNationId = o.get("targetNationId").getAsString();
                sn.level = o.get("level").getAsInt();
                sn.successChance = o.get("successChance").getAsDouble();
                sn.lastMission = o.has("lastMission") ? o.get("lastMission").getAsLong() : 0;
                networks.put(sn.ownerNationId + "_" + sn.targetNationId, sn);
            } catch (Exception ignored) {}
        }
    }

    private void saveNetwork(SpyNetwork sn) {
        File f = new File(spiesDir, sn.ownerNationId + "_" + sn.targetNationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("ownerNationId", sn.ownerNationId);
        o.addProperty("targetNationId", sn.targetNationId);
        o.addProperty("level", sn.level);
        o.addProperty("successChance", sn.successChance);
        o.addProperty("lastMission", sn.lastMission);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive spy network statistics.
     */
    public synchronized Map<String, Object> getSpyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Networks owned by this nation
        List<SpyNetwork> ownedNetworks = new ArrayList<>();
        List<SpyNetwork> targetNetworks = new ArrayList<>(); // Networks targeting this nation
        
        for (SpyNetwork network : networks.values()) {
            if (network.ownerNationId.equals(nationId)) {
                ownedNetworks.add(network);
            } else if (network.targetNationId.equals(nationId)) {
                targetNetworks.add(network);
            }
        }
        
        stats.put("ownedNetworks", ownedNetworks.size());
        stats.put("targetingNetworks", targetNetworks.size());
        
        // Network details
        List<Map<String, Object>> networksList = new ArrayList<>();
        for (SpyNetwork network : ownedNetworks) {
            Map<String, Object> networkData = new HashMap<>();
            networkData.put("target", network.targetNationId);
            networkData.put("level", network.level);
            networkData.put("successChance", network.successChance);
            long cooldownRemaining = 60 * 60_000L - (System.currentTimeMillis() - network.lastMission);
            networkData.put("cooldownRemaining", cooldownRemaining > 0 ? cooldownRemaining / 1000 / 60 : 0); // minutes
            networkData.put("canConductMission", cooldownRemaining <= 0);
            networksList.add(networkData);
        }
        stats.put("networksList", networksList);
        
        // Average level
        double avgLevel = ownedNetworks.stream()
            .mapToInt(n -> n.level)
            .average()
            .orElse(0.0);
        stats.put("averageLevel", avgLevel);
        
        // Average success chance
        double avgSuccess = ownedNetworks.stream()
            .mapToDouble(n -> n.successChance)
            .average()
            .orElse(0.0);
        stats.put("averageSuccessChance", avgSuccess);
        
        // Spy rating
        String rating = "НЕТ СЕТЕЙ";
        if (ownedNetworks.size() >= 10) rating = "РАЗВИТАЯ ШПИОНСКАЯ ИМПЕРИЯ";
        else if (ownedNetworks.size() >= 5) rating = "ОБШИРНАЯ СЕТЬ";
        else if (ownedNetworks.size() >= 3) rating = "АКТИВНАЯ СЕТЬ";
        else if (ownedNetworks.size() >= 1) rating = "НАЧАЛЬНАЯ СЕТЬ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Upgrade spy network.
     */
    public synchronized String upgradeNetwork(String ownerId, String targetId, double cost) throws IOException {
        String key = ownerId + "_" + targetId;
        SpyNetwork sn = networks.get(key);
        if (sn == null) return "Шпионская сеть не найдена.";
        if (sn.level >= 5) return "Сеть уже максимального уровня.";
        
        Nation owner = nationManager.getNationById(ownerId);
        if (owner == null) return "Нация не найдена.";
        if (owner.getTreasury() < cost) return "Недостаточно средств.";
        
        sn.level++;
        sn.successChance += 0.1; // +10% per level
        owner.setTreasury(owner.getTreasury() - cost);
        
        owner.getHistory().add("Шпионская сеть улучшена до уровня " + sn.level);
        
        nationManager.save(owner);
        saveNetwork(sn);
        
        return "Сеть улучшена до уровня " + sn.level + ". Шанс успеха: " + String.format("%.1f", sn.successChance * 100) + "%";
    }
    
    /**
     * Disband spy network.
     */
    public synchronized String disbandNetwork(String ownerId, String targetId) throws IOException {
        String key = ownerId + "_" + targetId;
        SpyNetwork sn = networks.get(key);
        if (sn == null) return "Шпионская сеть не найдена.";
        
        networks.remove(key);
        File f = new File(spiesDir, key + ".json");
        if (f.exists()) f.delete();
        
        return "Шпионская сеть расформирована.";
    }
    
    /**
     * Get all networks for a nation.
     */
    public synchronized List<SpyNetwork> getNationNetworks(String nationId) {
        return networks.values().stream()
            .filter(n -> n.ownerNationId.equals(nationId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Check if nation has network targeting them.
     */
    public synchronized boolean isBeingSpiedOn(String nationId) {
        return networks.values().stream()
            .anyMatch(n -> n.targetNationId.equals(nationId));
    }
    
    /**
     * Calculate counter-intelligence effectiveness.
     */
    public synchronized double getCounterIntelligenceEffectiveness(String nationId) {
        long targetingCount = networks.values().stream()
            .filter(n -> n.targetNationId.equals(nationId))
            .count();
        
        // Effectiveness decreases with more spies
        // Base 100%, -10% per spy network
        return Math.max(0.1, 1.0 - (targetingCount * 0.10));
    }
    
    /**
     * Get global spy service statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalSpyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalNetworks", networks.size());
        
        Map<String, Integer> networksByOwner = new HashMap<>();
        Map<String, Integer> networksByTarget = new HashMap<>();
        int totalLevel = 0;
        double totalSuccessChance = 0.0;
        Map<String, Double> avgLevelByOwner = new HashMap<>();
        
        for (SpyNetwork network : networks.values()) {
            networksByOwner.put(network.ownerNationId, networksByOwner.getOrDefault(network.ownerNationId, 0) + 1);
            networksByTarget.put(network.targetNationId, networksByTarget.getOrDefault(network.targetNationId, 0) + 1);
            totalLevel += network.level;
            totalSuccessChance += network.successChance;
            
            // Track average level per owner
            avgLevelByOwner.put(network.ownerNationId, 
                (avgLevelByOwner.getOrDefault(network.ownerNationId, 0.0) + network.level) / 2.0);
        }
        
        stats.put("networksByOwner", networksByOwner);
        stats.put("networksByTarget", networksByTarget);
        stats.put("nationsWithNetworks", networksByOwner.size());
        stats.put("nationsBeingSpiedOn", networksByTarget.size());
        stats.put("averageLevel", networks.size() > 0 ? (double) totalLevel / networks.size() : 0);
        stats.put("averageSuccessChance", networks.size() > 0 ? totalSuccessChance / networks.size() : 0);
        
        // Top nations by owned networks
        List<Map.Entry<String, Integer>> topByNetworks = networksByOwner.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByNetworks", topByNetworks);
        
        // Most spied on nations
        List<Map.Entry<String, Integer>> mostSpiedOn = networksByTarget.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostSpiedOn", mostSpiedOn);
        
        // Top nations by average network level
        List<Map.Entry<String, Double>> topByLevel = avgLevelByOwner.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByLevel", topByLevel);
        
        // Level distribution
        int level1 = 0, level2 = 0, level3 = 0, level4 = 0, level5 = 0;
        for (SpyNetwork network : networks.values()) {
            switch (network.level) {
                case 1: level1++; break;
                case 2: level2++; break;
                case 3: level3++; break;
                case 4: level4++; break;
                case 5: level5++; break;
            }
        }
        
        Map<String, Integer> levelDistribution = new HashMap<>();
        levelDistribution.put("level1", level1);
        levelDistribution.put("level2", level2);
        levelDistribution.put("level3", level3);
        levelDistribution.put("level4", level4);
        levelDistribution.put("level5", level5);
        stats.put("levelDistribution", levelDistribution);
        
        return stats;
    }
}

