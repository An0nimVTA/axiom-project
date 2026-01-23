package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages trade networks between multiple nations. */
public class TradeNetworkService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File networksDir;
    private final Map<String, TradeNetwork> networks = new HashMap<>(); // networkId -> network

    public static class TradeNetwork {
        String id;
        String name;
        Set<String> memberNations = new HashSet<>();
        double tradeBonus; // multiplier for all members
        long establishedAt;
        boolean active;
    }

    public TradeNetworkService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.networksDir = new File(plugin.getDataFolder(), "tradenetworks");
        this.networksDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processNetworks, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String createNetwork(String name, String founderId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(name) || isBlank(founderId)) return "Неверные параметры.";
        Nation founder = nationManager.getNationById(founderId);
        if (founder == null) return "Нация не найдена.";
        String networkId = UUID.randomUUID().toString().substring(0, 8);
        TradeNetwork network = new TradeNetwork();
        network.id = networkId;
        network.name = name;
        network.memberNations.add(founderId);
        network.tradeBonus = 1.1; // +10% base bonus
        network.establishedAt = System.currentTimeMillis();
        network.active = true;
        networks.put(networkId, network);
        if (founder.getHistory() != null) {
            founder.getHistory().add("Создана торговая сеть: " + name);
        }
        try {
            nationManager.save(founder);
            saveNetwork(network);
        } catch (Exception ignored) {}
        return "Торговая сеть создана: " + name;
    }

    public synchronized String joinNetwork(String networkId, String nationId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(networkId) || isBlank(nationId)) return "Неверные параметры.";
        TradeNetwork network = networks.get(networkId);
        if (network == null) return "Сеть не найдена.";
        if (!network.active) return "Сеть неактивна.";
        if (network.memberNations.contains(nationId)) return "Вы уже участник сети.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        network.memberNations.add(nationId);
        network.tradeBonus = Math.min(1.2, network.tradeBonus + 0.02); // cap at +20%
        if (n.getHistory() != null) {
            n.getHistory().add("Присоединилась к торговой сети: " + network.name);
        }
        try {
            nationManager.save(n);
            saveNetwork(network);
        } catch (Exception ignored) {}
        return "Присоединение к сети подтверждено.";
    }

    private synchronized void processNetworks() {
        if (nationManager == null) return;
        for (TradeNetwork network : networks.values()) {
            if (network == null) continue;
            if (!network.active) continue;
            // Apply trade bonuses to member nations
            for (String nationId : network.memberNations) {
                Nation n = nationManager.getNationById(nationId);
                if (n != null) {
                    // Bonus income from trade
                    double bonus = network.tradeBonus * 100;
                    if (!Double.isFinite(bonus) || bonus <= 0) continue;
                    n.setTreasury(n.getTreasury() + bonus);
                    try { nationManager.save(n); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void loadAll() {
        File[] files = networksDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                TradeNetwork network = new TradeNetwork();
                network.id = o.has("id") ? o.get("id").getAsString() : null;
                network.name = o.has("name") ? o.get("name").getAsString() : null;
                network.tradeBonus = o.has("tradeBonus") ? o.get("tradeBonus").getAsDouble() : 1.0;
                network.establishedAt = o.has("establishedAt") ? o.get("establishedAt").getAsLong() : System.currentTimeMillis();
                network.active = o.has("active") && o.get("active").getAsBoolean();
                if (o.has("memberNations")) {
                    for (var elem : o.getAsJsonArray("memberNations")) {
                        network.memberNations.add(elem.getAsString());
                    }
                }
                if (!isBlank(network.id)) {
                    networks.put(network.id, network);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveNetwork(TradeNetwork network) {
        if (network == null || isBlank(network.id)) return;
        File f = new File(networksDir, network.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", network.id);
        o.addProperty("name", network.name);
        o.addProperty("tradeBonus", network.tradeBonus);
        o.addProperty("establishedAt", network.establishedAt);
        o.addProperty("active", network.active);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String nationId : network.memberNations) {
            arr.add(nationId);
        }
        o.add("memberNations", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive trade network statistics for a nation.
     */
    public synchronized Map<String, Object> getTradeNetworkStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        int networksJoined = 0;
        double totalBonus = 0.0;
        List<String> networkNames = new ArrayList<>();
        
        for (TradeNetwork network : networks.values()) {
            if (network != null && network.active && network.memberNations.contains(nationId)) {
                networksJoined++;
                totalBonus += network.tradeBonus;
                networkNames.add(network.name);
            }
        }
        
        stats.put("networksJoined", networksJoined);
        stats.put("totalBonus", totalBonus);
        stats.put("averageBonus", networksJoined > 0 ? totalBonus / networksJoined : 0);
        stats.put("networkNames", networkNames);
        
        // Network rating
        String rating = "НЕТ СЕТЕЙ";
        if (networksJoined >= 5) rating = "МНОГОСЕТЕВОЙ";
        else if (networksJoined >= 3) rating = "АКТИВНЫЙ";
        else if (networksJoined >= 2) rating = "РАЗВИТЫЙ";
        else if (networksJoined >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global trade network statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalTradeNetworkStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalNetworks = 0;
        int activeNetworks = 0;
        int totalMembers = 0;
        double totalBonus = 0.0;
        Map<String, Integer> membersByNetwork = new HashMap<>();
        Map<String, Double> bonusByNetwork = new HashMap<>();
        Map<String, Integer> networksByNation = new HashMap<>();
        
        for (TradeNetwork network : networks.values()) {
            if (network != null && network.active) {
                totalNetworks++;
                activeNetworks++;
                
                int memberCount = network.memberNations.size();
                totalMembers += memberCount;
                totalBonus += network.tradeBonus;
                
                membersByNetwork.put(network.id, memberCount);
                bonusByNetwork.put(network.id, network.tradeBonus);
                
                for (String nationId : network.memberNations) {
                    networksByNation.put(nationId,
                        networksByNation.getOrDefault(nationId, 0) + 1);
                }
            }
        }
        
        stats.put("totalNetworks", totalNetworks);
        stats.put("activeNetworks", activeNetworks);
        stats.put("totalMembers", totalMembers);
        stats.put("averageMembers", activeNetworks > 0 ? (double) totalMembers / activeNetworks : 0);
        stats.put("totalBonus", totalBonus);
        stats.put("averageBonus", activeNetworks > 0 ? totalBonus / activeNetworks : 0);
        stats.put("membersByNetwork", membersByNetwork);
        stats.put("bonusByNetwork", bonusByNetwork);
        stats.put("networksByNation", networksByNation);
        stats.put("nationsInNetworks", networksByNation.size());
        
        // Top networks by members
        List<Map.Entry<String, Integer>> topByMembers = membersByNetwork.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMembers", topByMembers);
        
        // Top networks by bonus
        List<Map.Entry<String, Double>> topByBonus = bonusByNetwork.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByBonus", topByBonus);
        
        // Top nations by networks joined
        List<Map.Entry<String, Integer>> topByNetworks = networksByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByNetworks", topByNetworks);
        
        // Network size distribution
        int large = 0, medium = 0, small = 0;
        for (Integer memberCount : membersByNetwork.values()) {
            if (memberCount >= 10) large++;
            else if (memberCount >= 5) medium++;
            else small++;
        }
        
        Map<String, Integer> sizeDistribution = new HashMap<>();
        sizeDistribution.put("large", large);
        sizeDistribution.put("medium", medium);
        sizeDistribution.put("small", small);
        stats.put("sizeDistribution", sizeDistribution);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

