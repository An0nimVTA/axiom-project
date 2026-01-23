package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages peace treaties after wars. */
public class PeaceTreatyService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File treatiesDir;
    private final Map<String, PeaceTreaty> activeTreaties = new HashMap<>(); // nationA_nationB -> treaty

    public static class PeaceTreaty {
        String nationA;
        String nationB;
        String terms;
        double reparations; // from A to B
        Set<String> territoryTransfers = new HashSet<>(); // chunk keys
        long signedAt;
        long expiresAt;
        boolean permanent;
    }

    public PeaceTreatyService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.treatiesDir = new File(plugin.getDataFolder(), "peacetreaties");
        this.treatiesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processTreaties, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String signTreaty(String nationA, String nationB, String terms, double reparations, boolean permanent, int durationDays) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (nationA == null || nationA.trim().isEmpty() || nationB == null || nationB.trim().isEmpty()) return "Нация не найдена.";
        if (nationA.equals(nationB)) return "Некорректные стороны договора.";
        if (terms == null || terms.trim().isEmpty() || !Double.isFinite(reparations) || reparations < 0) return "Некорректные данные.";
        if (!permanent && durationDays <= 0) return "Некорректная длительность.";
        Nation a = nationManager.getNationById(nationA);
        Nation b = nationManager.getNationById(nationB);
        if (a == null || b == null) return "Нация не найдена.";
        if (a.getTreasury() < reparations) return "Недостаточно средств для репараций.";
        String key = nationA.compareTo(nationB) < 0 ? nationA + "_" + nationB : nationB + "_" + nationA;
        PeaceTreaty treaty = new PeaceTreaty();
        treaty.nationA = nationA;
        treaty.nationB = nationB;
        treaty.terms = terms;
        treaty.reparations = reparations;
        treaty.signedAt = System.currentTimeMillis();
        treaty.expiresAt = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + durationDays * 24 * 60 * 60_000L;
        treaty.permanent = permanent;
        activeTreaties.put(key, treaty);
        // End war
        try {
            if (plugin.getDiplomacySystem() != null) {
                plugin.getDiplomacySystem().declarePeace(nationA, nationB);
            }
        } catch (Exception ignored) {}
        // Transfer reparations
        a.setTreasury(a.getTreasury() - reparations);
        b.setTreasury(b.getTreasury() + reparations);
        if (a.getHistory() != null) {
            a.getHistory().add("Мирный договор с " + b.getName());
        }
        if (b.getHistory() != null) {
            b.getHistory().add("Мирный договор с " + a.getName());
        }
        try {
            nationManager.save(a);
            nationManager.save(b);
            saveTreaty(treaty);
        } catch (Exception ignored) {}
        return "Мирный договор подписан.";
    }

    private synchronized void processTreaties() {
        // Process reparations if needed
        for (PeaceTreaty treaty : activeTreaties.values()) {
            if (treaty.reparations > 0 && !treaty.permanent) {
                // Could implement periodic reparation payments here
            }
        }
    }

    private void loadAll() {
        File[] files = treatiesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                PeaceTreaty treaty = new PeaceTreaty();
                treaty.nationA = o.get("nationA").getAsString();
                treaty.nationB = o.get("nationB").getAsString();
                treaty.terms = o.get("terms").getAsString();
                treaty.reparations = o.get("reparations").getAsDouble();
                treaty.signedAt = o.get("signedAt").getAsLong();
                treaty.expiresAt = o.get("expiresAt").getAsLong();
                treaty.permanent = o.has("permanent") && o.get("permanent").getAsBoolean();
                if (o.has("territoryTransfers")) {
                    for (var elem : o.getAsJsonArray("territoryTransfers")) {
                        treaty.territoryTransfers.add(elem.getAsString());
                    }
                }
                String key = treaty.nationA.compareTo(treaty.nationB) < 0 ? treaty.nationA + "_" + treaty.nationB : treaty.nationB + "_" + treaty.nationA;
                if (treaty.expiresAt > System.currentTimeMillis() || treaty.permanent) {
                    activeTreaties.put(key, treaty);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveTreaty(PeaceTreaty treaty) {
        String key = treaty.nationA.compareTo(treaty.nationB) < 0 ? treaty.nationA + "_" + treaty.nationB : treaty.nationB + "_" + treaty.nationA;
        File f = new File(treatiesDir, key + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationA", treaty.nationA);
        o.addProperty("nationB", treaty.nationB);
        o.addProperty("terms", treaty.terms);
        o.addProperty("reparations", treaty.reparations);
        o.addProperty("signedAt", treaty.signedAt);
        o.addProperty("expiresAt", treaty.expiresAt);
        o.addProperty("permanent", treaty.permanent);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String chunkKey : treaty.territoryTransfers) {
            arr.add(chunkKey);
        }
        o.add("territoryTransfers", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive peace treaty statistics for a nation.
     */
    public synchronized Map<String, Object> getPeaceTreatyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        if (nationId == null || nationId.trim().isEmpty()) {
            stats.put("totalTreaties", 0);
            stats.put("permanentTreaties", 0);
            stats.put("temporaryTreaties", 0);
            stats.put("totalReparationsPaid", 0.0);
            stats.put("totalReparationsReceived", 0.0);
            stats.put("netReparations", 0.0);
            stats.put("treatyPartners", 0);
            stats.put("rating", "НЕТ ДОГОВОРОВ");
            return stats;
        }
        
        int treatiesAsA = 0;
        int treatiesAsB = 0;
        double totalReparationsPaid = 0.0;
        double totalReparationsReceived = 0.0;
        int permanentTreaties = 0;
        int temporaryTreaties = 0;
        Set<String> treatyPartners = new HashSet<>();
        long now = System.currentTimeMillis();
        
        for (PeaceTreaty treaty : activeTreaties.values()) {
            boolean isPartyA = treaty.nationA.equals(nationId);
            boolean isPartyB = treaty.nationB.equals(nationId);
            
            if (isPartyA || isPartyB) {
                if (isPartyA) treatiesAsA++;
                if (isPartyB) treatiesAsB++;
                
                if (isPartyA) {
                    totalReparationsPaid += treaty.reparations;
                } else {
                    totalReparationsReceived += treaty.reparations;
                }
                
                if (treaty.permanent) permanentTreaties++;
                else if (now < treaty.expiresAt) temporaryTreaties++;
                
                if (isPartyA) treatyPartners.add(treaty.nationB);
                else treatyPartners.add(treaty.nationA);
            }
        }
        
        stats.put("totalTreaties", treatiesAsA + treatiesAsB);
        stats.put("permanentTreaties", permanentTreaties);
        stats.put("temporaryTreaties", temporaryTreaties);
        stats.put("totalReparationsPaid", totalReparationsPaid);
        stats.put("totalReparationsReceived", totalReparationsReceived);
        stats.put("netReparations", totalReparationsReceived - totalReparationsPaid);
        stats.put("treatyPartners", treatyPartners.size());
        
        // Treaty rating
        String rating = "НЕТ ДОГОВОРОВ";
        int total = treatiesAsA + treatiesAsB;
        if (total >= 10) rating = "МНОГОСТОРОННИЙ МИР";
        else if (total >= 5) rating = "МИРНЫЙ";
        else if (total >= 3) rating = "СТАБИЛЬНЫЙ";
        else if (total >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global peace treaty statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalPeaceTreatyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long now = System.currentTimeMillis();
        int totalActive = 0;
        int permanentTreaties = 0;
        int temporaryTreaties = 0;
        double totalReparations = 0.0;
        Map<String, Integer> treatiesByNation = new HashMap<>();
        Map<String, Double> reparationsPaidByNation = new HashMap<>();
        Map<String, Double> reparationsReceivedByNation = new HashMap<>();
        
        for (PeaceTreaty treaty : activeTreaties.values()) {
            if (treaty.expiresAt > now || treaty.permanent) {
                totalActive++;
                if (treaty.permanent) permanentTreaties++;
                else temporaryTreaties++;
                
                totalReparations += treaty.reparations;
                
                treatiesByNation.put(treaty.nationA,
                    treatiesByNation.getOrDefault(treaty.nationA, 0) + 1);
                treatiesByNation.put(treaty.nationB,
                    treatiesByNation.getOrDefault(treaty.nationB, 0) + 1);
                
                reparationsPaidByNation.put(treaty.nationA,
                    reparationsPaidByNation.getOrDefault(treaty.nationA, 0.0) + treaty.reparations);
                reparationsReceivedByNation.put(treaty.nationB,
                    reparationsReceivedByNation.getOrDefault(treaty.nationB, 0.0) + treaty.reparations);
            }
        }
        
        stats.put("totalActiveTreaties", totalActive);
        stats.put("permanentTreaties", permanentTreaties);
        stats.put("temporaryTreaties", temporaryTreaties);
        stats.put("totalReparations", totalReparations);
        stats.put("averageReparations", totalActive > 0 ? totalReparations / totalActive : 0);
        stats.put("treatiesByNation", treatiesByNation);
        stats.put("reparationsPaidByNation", reparationsPaidByNation);
        stats.put("reparationsReceivedByNation", reparationsReceivedByNation);
        stats.put("nationsWithTreaties", treatiesByNation.size());
        
        // Top nations by treaties
        List<Map.Entry<String, Integer>> topByTreaties = treatiesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByTreaties", topByTreaties);
        
        // Top nations by reparations paid
        List<Map.Entry<String, Double>> topByPaid = reparationsPaidByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPaid", topByPaid);
        
        // Top nations by reparations received
        List<Map.Entry<String, Double>> topByReceived = reparationsReceivedByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByReceived", topByReceived);
        
        return stats;
    }
}

