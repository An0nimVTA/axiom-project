package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages arms control treaties and weapon restrictions. */
public class ArmsControlService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File treatiesDir;
    private final Map<String, ArmsControlTreaty> treaties = new HashMap<>(); // treatyId -> treaty

    public static class ArmsControlTreaty {
        String id;
        Set<String> signatories = new HashSet<>();
        String weaponType; // "nuclear", "chemical", "biological", "conventional"
        int limit; // max allowed
        boolean active;
        long signedAt;
    }

    public ArmsControlService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.treatiesDir = new File(plugin.getDataFolder(), "armscontrol");
        this.treatiesDir.mkdirs();
        loadAll();
    }

    public synchronized String createTreaty(String initiatorId, String weaponType, int limit) {
        Nation initiator = nationManager.getNationById(initiatorId);
        if (initiator == null) return "Нация не найдена.";
        if (weaponType == null || weaponType.isBlank()) return "Тип оружия не задан.";
        if (limit <= 0) return "Неверный лимит.";
        String treatyId = UUID.randomUUID().toString().substring(0, 8);
        ArmsControlTreaty treaty = new ArmsControlTreaty();
        treaty.id = treatyId;
        treaty.signatories.add(initiatorId);
        treaty.weaponType = weaponType;
        treaty.limit = limit;
        treaty.active = false; // Not active until other nations sign
        treaty.signedAt = System.currentTimeMillis();
        treaties.put(treatyId, treaty);
        initiator.getHistory().add("Создан договор о контроле вооружений: " + weaponType);
        try {
            nationManager.save(initiator);
            saveTreaty(treaty);
        } catch (Exception ignored) {}
        return "Договор создан (ID: " + treatyId + "). Ожидается подписание других наций.";
    }

    public synchronized String signTreaty(String treatyId, String nationId) {
        ArmsControlTreaty treaty = treaties.get(treatyId);
        if (treaty == null) return "Договор не найден.";
        if (treaty.signatories.contains(nationId)) return "Нация уже подписала договор.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        treaty.signatories.add(nationId);
        if (treaty.signatories.size() >= 3) {
            treaty.active = true; // Active when 3+ nations sign
        }
        n.getHistory().add("Подписан договор о контроле вооружений: " + treaty.weaponType);
        try {
            nationManager.save(n);
            saveTreaty(treaty);
        } catch (Exception ignored) {}
        return "Договор подписан.";
    }

    public synchronized boolean isWeaponAllowed(String nationId, String weaponType) {
        if (weaponType == null) return true;
        for (ArmsControlTreaty treaty : treaties.values()) {
            if (!treaty.active || !treaty.weaponType.equals(weaponType)) continue;
            if (treaty.signatories.contains(nationId)) {
                // Check if nation exceeds limit
                if (weaponType.equals("nuclear")) {
                    NuclearWeaponsService nuclearWeaponsService = plugin.getNuclearWeaponsService();
                    if (nuclearWeaponsService == null) return true;
                    int warheads = nuclearWeaponsService.getWarheads(nationId);
                    return warheads < treaty.limit;
                }
                return true;
            }
        }
        return true; // Not part of treaty, allowed
    }

    private void loadAll() {
        File[] files = treatiesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                ArmsControlTreaty treaty = new ArmsControlTreaty();
                treaty.id = o.get("id").getAsString();
                treaty.weaponType = o.get("weaponType").getAsString();
                treaty.limit = o.get("limit").getAsInt();
                treaty.active = o.has("active") && o.get("active").getAsBoolean();
                treaty.signedAt = o.get("signedAt").getAsLong();
                if (o.has("signatories")) {
                    for (var elem : o.getAsJsonArray("signatories")) {
                        treaty.signatories.add(elem.getAsString());
                    }
                }
                treaties.put(treaty.id, treaty);
            } catch (Exception ignored) {}
        }
    }

    private void saveTreaty(ArmsControlTreaty treaty) {
        File f = new File(treatiesDir, treaty.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", treaty.id);
        o.addProperty("weaponType", treaty.weaponType);
        o.addProperty("limit", treaty.limit);
        o.addProperty("active", treaty.active);
        o.addProperty("signedAt", treaty.signedAt);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String nationId : treaty.signatories) {
            arr.add(nationId);
        }
        o.add("signatories", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive arms control statistics.
     */
    public synchronized Map<String, Object> getArmsControlStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Treaties this nation is signatory of
        List<ArmsControlTreaty> signedTreaties = new ArrayList<>();
        for (ArmsControlTreaty treaty : treaties.values()) {
            if (treaty.signatories.contains(nationId)) {
                signedTreaties.add(treaty);
            }
        }
        
        stats.put("signedTreaties", signedTreaties.size());
        stats.put("activeTreaties", signedTreaties.stream().filter(t -> t.active).count());
        stats.put("treaties", signedTreaties);
        
        // Treaty compliance
        Map<String, Boolean> compliance = new HashMap<>();
        for (ArmsControlTreaty treaty : signedTreaties) {
            if (treaty.active) {
                compliance.put(treaty.weaponType, isWeaponAllowed(nationId, treaty.weaponType));
            }
        }
        stats.put("compliance", compliance);
        
        // Treaty details
        List<Map<String, Object>> treatiesList = new ArrayList<>();
        for (ArmsControlTreaty treaty : signedTreaties) {
            Map<String, Object> treatyData = new HashMap<>();
            treatyData.put("id", treaty.id);
            treatyData.put("weaponType", treaty.weaponType);
            treatyData.put("limit", treaty.limit);
            treatyData.put("active", treaty.active);
            treatyData.put("signatories", treaty.signatories.size());
            treatyData.put("signedAt", treaty.signedAt);
            treatyData.put("age", (System.currentTimeMillis() - treaty.signedAt) / 1000 / 60 / 60 / 24); // days
            treatiesList.add(treatyData);
        }
        stats.put("treatiesList", treatiesList);
        
        // Arms control rating
        String rating = "НЕТ ДОГОВОРОВ";
        if (signedTreaties.size() >= 5) rating = "МНОЖЕСТВЕННЫЕ";
        else if (signedTreaties.size() >= 3) rating = "АКТИВНЫЕ";
        else if (signedTreaties.size() >= 1) rating = "ПОДПИСАННЫЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Withdraw from treaty.
     */
    public synchronized String withdrawFromTreaty(String treatyId, String nationId) throws IOException {
        ArmsControlTreaty treaty = treaties.get(treatyId);
        if (treaty == null) return "Договор не найден.";
        if (!treaty.signatories.contains(nationId)) return "Вы не подписаны на этот договор.";
        
        treaty.signatories.remove(nationId);
        
        // If less than 3 signatories, deactivate treaty
        if (treaty.signatories.size() < 3) {
            treaty.active = false;
        }
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Выход из договора о контроле вооружений: " + treaty.weaponType);
            nationManager.save(n);
        }
        
        saveTreaty(treaty);
        
        return "Вы вышли из договора.";
    }
    
    /**
     * Get all treaties for a weapon type.
     */
    public synchronized List<ArmsControlTreaty> getTreatiesForWeapon(String weaponType) {
        return treaties.values().stream()
            .filter(t -> t.weaponType.equals(weaponType) && t.active)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Check compliance status.
     */
    public synchronized boolean isCompliant(String nationId) {
        for (ArmsControlTreaty treaty : treaties.values()) {
            if (treaty.active && treaty.signatories.contains(nationId)) {
                if (!isWeaponAllowed(nationId, treaty.weaponType)) {
                    return false; // Violating a treaty
                }
            }
        }
        return true;
    }
    
    /**
     * Get treaty violation penalty.
     */
    public synchronized double getViolationPenalty(String nationId) {
        double penalty = 0.0;
        for (ArmsControlTreaty treaty : treaties.values()) {
            if (treaty.active && treaty.signatories.contains(nationId)) {
                if (!isWeaponAllowed(nationId, treaty.weaponType)) {
                    penalty += 10000.0; // Penalty per violation
                }
            }
        }
        return penalty;
    }
    
    /**
     * Get global arms control statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalArmsControlStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalTreaties = treaties.size();
        int activeTreaties = 0;
        Map<String, Integer> treatiesByWeapon = new HashMap<>();
        Map<String, Integer> signatoriesByNation = new HashMap<>();
        Map<String, Integer> complianceByNation = new HashMap<>();
        
        for (ArmsControlTreaty treaty : treaties.values()) {
            if (treaty.active) {
                activeTreaties++;
                treatiesByWeapon.put(treaty.weaponType, treatiesByWeapon.getOrDefault(treaty.weaponType, 0) + 1);
                
                for (String nationId : treaty.signatories) {
                    signatoriesByNation.put(nationId, signatoriesByNation.getOrDefault(nationId, 0) + 1);
                    if (isCompliant(nationId)) {
                        complianceByNation.put(nationId, complianceByNation.getOrDefault(nationId, 0) + 1);
                    }
                }
            }
        }
        
        stats.put("totalTreaties", totalTreaties);
        stats.put("activeTreaties", activeTreaties);
        stats.put("treatiesByWeapon", treatiesByWeapon);
        stats.put("signatoriesByNation", signatoriesByNation);
        stats.put("nationsSigned", signatoriesByNation.size());
        stats.put("compliantNations", complianceByNation.size());
        
        // Compliance rate
        int compliantCount = 0;
        int totalSigned = signatoriesByNation.size();
        for (String nationId : signatoriesByNation.keySet()) {
            if (isCompliant(nationId)) {
                compliantCount++;
            }
        }
        stats.put("complianceRate", totalSigned > 0 ? (compliantCount / (double) totalSigned) * 100 : 0);
        
        // Top nations by treaty participation
        List<Map.Entry<String, Integer>> topByParticipation = signatoriesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByParticipation", topByParticipation);
        
        // Most common weapon types
        List<Map.Entry<String, Integer>> mostCommonWeapons = treatiesByWeapon.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonWeapons", mostCommonWeapons);
        
        // Average signatories per treaty
        int totalSignatories = signatoriesByNation.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageSignatoriesPerTreaty", activeTreaties > 0 ? (double) totalSignatories / activeTreaties : 0);
        
        // Violations
        int violations = 0;
        double totalPenalties = 0.0;
        for (String nationId : signatoriesByNation.keySet()) {
            double penalty = getViolationPenalty(nationId);
            if (penalty > 0) {
                violations++;
                totalPenalties += penalty;
            }
        }
        stats.put("totalViolations", violations);
        stats.put("totalPenalties", totalPenalties);
        
        return stats;
    }
}

