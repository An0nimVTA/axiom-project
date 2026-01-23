package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages trade wars (tariff battles, etc.). */
public class TradeWarService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File tradeWarsDir;
    private final Map<String, TradeWar> activeWars = new HashMap<>(); // nationA_nationB -> war

    public static class TradeWar {
        String nationA;
        String nationB;
        double tariffA; // tariff imposed by A on B
        double tariffB; // tariff imposed by B on A
        long startedAt;
        boolean active;
    }

    public TradeWarService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.tradeWarsDir = new File(plugin.getDataFolder(), "tradewars");
        this.tradeWarsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processTradeWars, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String startTradeWar(String nationA, String nationB, double tariff) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationA) || isBlank(nationB)) return "Неверные параметры.";
        if (nationA.equals(nationB)) return "Нельзя начать торговую войну с собой.";
        if (!Double.isFinite(tariff) || tariff < 0) return "Некорректная пошлина.";
        String key = nationA.compareTo(nationB) < 0 ? nationA + "_" + nationB : nationB + "_" + nationA;
        TradeWar existing = activeWars.get(key);
        if (existing != null && existing.active) return "Торговая война уже идёт.";
        Nation a = nationManager.getNationById(nationA);
        Nation b = nationManager.getNationById(nationB);
        if (a == null || b == null) return "Нация не найдена.";
        TradeWar war = new TradeWar();
        war.nationA = nationA;
        war.nationB = nationB;
        war.tariffA = Math.max(0, Math.min(100, tariff));
        war.tariffB = 0.0;
        war.startedAt = System.currentTimeMillis();
        war.active = true;
        activeWars.put(key, war);
        if (a.getHistory() != null) {
            a.getHistory().add("Торговая война начата с " + b.getName());
        }
        if (b.getHistory() != null) {
            b.getHistory().add("Торговая война начата с " + a.getName());
        }
        try {
            nationManager.save(a);
            nationManager.save(b);
            saveTradeWar(war);
        } catch (Exception ignored) {}
        return "Торговая война начата (пошлина: " + war.tariffA + "%)";
    }

    public synchronized String retaliate(String nationId, String targetId, double tariff) {
        if (isBlank(nationId) || isBlank(targetId)) return "Неверные параметры.";
        if (!Double.isFinite(tariff) || tariff < 0) return "Некорректная пошлина.";
        String key = nationId.compareTo(targetId) < 0 ? nationId + "_" + targetId : targetId + "_" + nationId;
        TradeWar war = activeWars.get(key);
        if (war == null || !war.active) return "Торговой войны нет.";
        if (nationId.equals(war.nationA)) {
            war.tariffA = Math.max(0, Math.min(100, tariff));
        } else if (nationId.equals(war.nationB)) {
            war.tariffB = Math.max(0, Math.min(100, tariff));
        } else {
            return "Неверный участник торговой войны.";
        }
        saveTradeWar(war);
        return "Ответная пошлина установлена: " + (nationId.equals(war.nationA) ? war.tariffA : war.tariffB) + "%";
    }

    private synchronized void processTradeWars() {
        if (nationManager == null) return;
        for (TradeWar war : activeWars.values()) {
            if (war == null || !war.active) continue;
            // Trade wars reduce economic activity
            Nation a = nationManager.getNationById(war.nationA);
            Nation b = nationManager.getNationById(war.nationB);
            if (a != null && b != null) {
                // Both nations lose trade revenue
                double lossA = war.tariffB * 100;
                double lossB = war.tariffA * 100;
                a.setTreasury(Math.max(0, a.getTreasury() - lossA));
                b.setTreasury(Math.max(0, b.getTreasury() - lossB));
                try {
                    nationManager.save(a);
                    nationManager.save(b);
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadAll() {
        File[] files = tradeWarsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                TradeWar war = new TradeWar();
                war.nationA = o.has("nationA") ? o.get("nationA").getAsString() : null;
                war.nationB = o.has("nationB") ? o.get("nationB").getAsString() : null;
                war.tariffA = o.has("tariffA") ? o.get("tariffA").getAsDouble() : 0.0;
                war.tariffB = o.has("tariffB") ? o.get("tariffB").getAsDouble() : 0.0;
                war.startedAt = o.has("startedAt") ? o.get("startedAt").getAsLong() : System.currentTimeMillis();
                war.active = o.has("active") && o.get("active").getAsBoolean();
                if (!isBlank(war.nationA) && !isBlank(war.nationB)) {
                    String key = war.nationA.compareTo(war.nationB) < 0 ? war.nationA + "_" + war.nationB : war.nationB + "_" + war.nationA;
                    if (war.active) activeWars.put(key, war);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveTradeWar(TradeWar war) {
        if (war == null || isBlank(war.nationA) || isBlank(war.nationB)) return;
        String key = war.nationA.compareTo(war.nationB) < 0 ? war.nationA + "_" + war.nationB : war.nationB + "_" + war.nationA;
        File f = new File(tradeWarsDir, key + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationA", war.nationA);
        o.addProperty("nationB", war.nationB);
        o.addProperty("tariffA", war.tariffA);
        o.addProperty("tariffB", war.tariffB);
        o.addProperty("startedAt", war.startedAt);
        o.addProperty("active", war.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive trade war statistics.
     */
    public synchronized Map<String, Object> getTradeWarStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        // Trade wars involving this nation
        List<TradeWar> wars = new ArrayList<>();
        double totalTariffCost = 0.0;
        double totalTariffRevenue = 0.0;
        
        for (TradeWar war : activeWars.values()) {
            if (war == null || !war.active) continue;
            
            if (war.nationA.equals(nationId)) {
                wars.add(war);
                totalTariffCost += war.tariffB * 100; // Cost from B's tariff
                totalTariffRevenue += war.tariffA * 100; // Revenue from A's tariff
            } else if (war.nationB.equals(nationId)) {
                wars.add(war);
                totalTariffCost += war.tariffA * 100; // Cost from A's tariff
                totalTariffRevenue += war.tariffB * 100; // Revenue from B's tariff
            }
        }
        
        stats.put("activeTradeWars", wars.size());
        stats.put("totalTariffCost", totalTariffCost);
        stats.put("totalTariffRevenue", totalTariffRevenue);
        stats.put("netTradeWarImpact", totalTariffRevenue - totalTariffCost);
        
        // Trade war details
        List<Map<String, Object>> warsList = new ArrayList<>();
        for (TradeWar war : wars) {
            Map<String, Object> warData = new HashMap<>();
            warData.put("opponent", war.nationA.equals(nationId) ? war.nationB : war.nationA);
            warData.put("ourTariff", war.nationA.equals(nationId) ? war.tariffA : war.tariffB);
            warData.put("opponentTariff", war.nationA.equals(nationId) ? war.tariffB : war.tariffA);
            warData.put("startedAt", war.startedAt);
            warData.put("duration", (System.currentTimeMillis() - war.startedAt) / 1000 / 60 / 60); // hours
            warsList.add(warData);
        }
        stats.put("wars", warsList);
        
        // Trade war rating
        String rating = "ОТСУТСТВУЮТ";
        if (wars.size() >= 5) rating = "МАССИВНЫЕ";
        else if (wars.size() >= 3) rating = "МНОЖЕСТВЕННЫЕ";
        else if (wars.size() >= 1) rating = "АКТИВНЫЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * End trade war.
     */
    public synchronized String endTradeWar(String nationA, String nationB) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationA) || isBlank(nationB)) return "Неверные параметры.";
        String key = nationA.compareTo(nationB) < 0 ? nationA + "_" + nationB : nationB + "_" + nationA;
        TradeWar war = activeWars.get(key);
        if (war == null) return "Торговой войны нет.";
        
        war.active = false;
        
        Nation a = nationManager.getNationById(war.nationA);
        Nation b = nationManager.getNationById(war.nationB);
        if (a != null && b != null) {
            if (a.getHistory() != null) {
                a.getHistory().add("Торговая война окончена с " + b.getName());
            }
            if (b.getHistory() != null) {
                b.getHistory().add("Торговая война окончена с " + a.getName());
            }
            nationManager.save(a);
            nationManager.save(b);
        }
        
        saveTradeWar(war);
        activeWars.remove(key);
        
        return "Торговая война окончена.";
    }
    
    /**
     * Get trade war with specific nation.
     */
    public synchronized TradeWar getTradeWar(String nationA, String nationB) {
        if (isBlank(nationA) || isBlank(nationB)) return null;
        String key = nationA.compareTo(nationB) < 0 ? nationA + "_" + nationB : nationB + "_" + nationA;
        TradeWar war = activeWars.get(key);
        return war != null && war.active ? war : null;
    }
    
    /**
     * Calculate total economic impact of trade wars.
     */
    public synchronized double getTradeWarEconomicImpact(String nationId) {
        if (isBlank(nationId)) return 0.0;
        double totalCost = 0.0;
        for (TradeWar war : activeWars.values()) {
            if (war == null || !war.active) continue;
            if (war.nationA.equals(nationId)) {
                totalCost += war.tariffB * 100;
            } else if (war.nationB.equals(nationId)) {
                totalCost += war.tariffA * 100;
            }
        }
        // Negative impact (cost per hour)
        return -totalCost;
    }
    
    /**
     * Get global trade war statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalTradeWarStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveTradeWars", activeWars.size());
        
        Map<String, Integer> warsByNation = new HashMap<>();
        double totalTariffCost = 0.0;
        double totalTariffRevenue = 0.0;
        Set<String> nationsInTradeWars = new HashSet<>();
        
        for (TradeWar war : activeWars.values()) {
            if (war == null || !war.active) continue;
            
            warsByNation.put(war.nationA, warsByNation.getOrDefault(war.nationA, 0) + 1);
            warsByNation.put(war.nationB, warsByNation.getOrDefault(war.nationB, 0) + 1);
            nationsInTradeWars.add(war.nationA);
            nationsInTradeWars.add(war.nationB);
            
            totalTariffCost += (war.tariffA + war.tariffB) * 100;
            totalTariffRevenue += (war.tariffA + war.tariffB) * 100; // Both sides collect tariffs
        }
        
        stats.put("warsByNation", warsByNation);
        stats.put("totalTariffCost", totalTariffCost);
        stats.put("totalTariffRevenue", totalTariffRevenue);
        stats.put("nationsInTradeWars", nationsInTradeWars.size());
        stats.put("averageWarsPerNation", warsByNation.size() > 0 ? 
            (double) activeWars.size() * 2 / warsByNation.size() : 0);
        
        // Top nations by trade wars
        List<Map.Entry<String, Integer>> topByWars = warsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByWars", topByWars);
        
        // Average tariff level
        double totalTariffs = 0.0;
        int count = 0;
        for (TradeWar war : activeWars.values()) {
            if (war != null && war.active) {
                totalTariffs += (war.tariffA + war.tariffB);
                count += 2;
            }
        }
        stats.put("averageTariffLevel", count > 0 ? totalTariffs / count : 0);
        
        // Average trade war duration
        long now = System.currentTimeMillis();
        double totalDuration = 0.0;
        int warCount = 0;
        for (TradeWar war : activeWars.values()) {
            if (war != null && war.active) {
                totalDuration += (now - war.startedAt) / 1000.0 / 60.0 / 60.0; // hours
                warCount++;
            }
        }
        stats.put("averageDurationHours", warCount > 0 ? totalDuration / warCount : 0);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

