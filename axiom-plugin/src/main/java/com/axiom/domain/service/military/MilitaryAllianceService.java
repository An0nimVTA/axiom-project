package com.axiom.domain.service.military;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages military alliances (NATO-like organizations). */
public class MilitaryAllianceService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File alliancesDir;
    private final Map<String, MilitaryAlliance> alliances = new HashMap<>(); // allianceId -> alliance

    public static class MilitaryAlliance {
        String id;
        String name;
        Set<String> memberNations = new HashSet<>();
        String leaderNationId;
        double defenseCommitment; // 0-100%
        long establishedAt;
        boolean active;
    }

    public MilitaryAllianceService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.alliancesDir = new File(plugin.getDataFolder(), "militaryalliances");
        this.alliancesDir.mkdirs();
        loadAll();
    }

    public synchronized String createAlliance(String name, String leaderId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(name) || isBlank(leaderId)) return "Некорректные данные.";
        Nation leader = nationManager.getNationById(leaderId);
        if (leader == null) return "Нация не найдена.";
        String allianceId = UUID.randomUUID().toString().substring(0, 8);
        MilitaryAlliance alliance = new MilitaryAlliance();
        alliance.id = allianceId;
        alliance.name = name;
        alliance.memberNations.add(leaderId);
        alliance.leaderNationId = leaderId;
        alliance.defenseCommitment = 50.0; // Default 50%
        alliance.establishedAt = System.currentTimeMillis();
        alliance.active = true;
        alliances.put(allianceId, alliance);
        if (leader.getHistory() != null) {
            leader.getHistory().add("Создан военный альянс: " + name);
        }
        try {
            nationManager.save(leader);
            saveAlliance(alliance);
        } catch (Exception ignored) {}
        return "Военный альянс создан: " + name;
    }

    public synchronized String joinAlliance(String allianceId, String nationId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(allianceId) || isBlank(nationId)) return "Некорректные данные.";
        MilitaryAlliance alliance = alliances.get(allianceId);
        if (alliance == null) return "Альянс не найден.";
        if (!alliance.active) return "Альянс неактивен.";
        if (alliance.memberNations.contains(nationId)) return "Нация уже в альянсе.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        alliance.memberNations.add(nationId);
        // Auto-form diplomatic alliance
        try {
            if (plugin.getDiplomacySystem() != null) {
                for (String memberId : alliance.memberNations) {
                    if (!memberId.equals(nationId)) {
                        Nation member = nationManager.getNationById(memberId);
                        if (member != null) {
                            plugin.getDiplomacySystem().setReputation(n, member, 20);
                            plugin.getDiplomacySystem().setReputation(member, n, 20);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        if (n.getHistory() != null) {
            n.getHistory().add("Присоединилась к военному альянсу: " + alliance.name);
        }
        try {
            nationManager.save(n);
            saveAlliance(alliance);
        } catch (Exception ignored) {}
        return "Присоединение к альянсу подтверждено.";
    }

    public synchronized boolean isAllied(String nationA, String nationB) {
        if (isBlank(nationA) || isBlank(nationB)) return false;
        for (MilitaryAlliance alliance : alliances.values()) {
            if (!alliance.active) continue;
            if (alliance.memberNations.contains(nationA) && alliance.memberNations.contains(nationB)) {
                return true;
            }
        }
        return false;
    }

    private void loadAll() {
        File[] files = alliancesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                MilitaryAlliance alliance = new MilitaryAlliance();
                alliance.id = o.get("id").getAsString();
                alliance.name = o.get("name").getAsString();
                alliance.leaderNationId = o.get("leaderNationId").getAsString();
                alliance.defenseCommitment = o.get("defenseCommitment").getAsDouble();
                alliance.establishedAt = o.get("establishedAt").getAsLong();
                alliance.active = o.has("active") && o.get("active").getAsBoolean();
                if (o.has("memberNations")) {
                    for (var elem : o.getAsJsonArray("memberNations")) {
                        alliance.memberNations.add(elem.getAsString());
                    }
                }
                alliances.put(alliance.id, alliance);
            } catch (Exception ignored) {}
        }
    }

    private void saveAlliance(MilitaryAlliance alliance) {
        File f = new File(alliancesDir, alliance.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", alliance.id);
        o.addProperty("name", alliance.name);
        o.addProperty("leaderNationId", alliance.leaderNationId);
        o.addProperty("defenseCommitment", alliance.defenseCommitment);
        o.addProperty("establishedAt", alliance.establishedAt);
        o.addProperty("active", alliance.active);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String nationId : alliance.memberNations) {
            arr.add(nationId);
        }
        o.add("memberNations", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive military alliance statistics.
     */
    public synchronized Map<String, Object> getAllianceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Alliances this nation is member of
        List<MilitaryAlliance> memberAlliances = new ArrayList<>();
        if (isBlank(nationId)) {
            stats.put("memberAlliances", 0);
            stats.put("alliances", memberAlliances);
            stats.put("totalAllianceStrength", 0.0);
            stats.put("averageDefenseCommitment", 0.0);
            stats.put("rating", "НЕ В АЛЬЯНСАХ");
            return stats;
        }
        for (MilitaryAlliance alliance : alliances.values()) {
            if (alliance.memberNations.contains(nationId)) {
                memberAlliances.add(alliance);
            }
        }
        
        stats.put("memberAlliances", memberAlliances.size());
        stats.put("alliances", memberAlliances);
        
        // Calculate combined alliance strength
        double totalAllianceStrength = 0.0;
        for (MilitaryAlliance alliance : memberAlliances) {
            if (alliance.active) {
                for (String memberId : alliance.memberNations) {
                    if (!memberId.equals(nationId)) {
                        Nation member = nationManager.getNationById(memberId);
                        if (member != null && plugin.getMilitaryService() != null) {
                            totalAllianceStrength += plugin.getMilitaryService().getMilitaryStrength(memberId) * (alliance.defenseCommitment / 100.0);
                        }
                    }
                }
            }
        }
        stats.put("totalAllianceStrength", totalAllianceStrength);
        
        // Defense commitment average
        double avgDefenseCommitment = memberAlliances.stream()
            .filter(a -> a.active)
            .mapToDouble(a -> a.defenseCommitment)
            .average()
            .orElse(0.0);
        stats.put("averageDefenseCommitment", avgDefenseCommitment);
        
        // Alliance rating
        String rating = "НЕ В АЛЬЯНСАХ";
        if (memberAlliances.size() >= 3) rating = "МНОЖЕСТВЕННЫЙ";
        else if (memberAlliances.size() >= 2) rating = "ДВОЙНОЙ";
        else if (memberAlliances.size() >= 1) rating = "ЧЛЕН АЛЬЯНСА";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Leave alliance.
     */
    public synchronized String leaveAlliance(String allianceId, String nationId) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(allianceId) || isBlank(nationId)) return "Некорректные данные.";
        MilitaryAlliance alliance = alliances.get(allianceId);
        if (alliance == null) return "Альянс не найден.";
        if (!alliance.memberNations.contains(nationId)) return "Вы не являетесь членом этого альянса.";
        
        alliance.memberNations.remove(nationId);
        
        // If leader leaves, transfer leadership or disband
        if (nationId.equals(alliance.leaderNationId)) {
            if (alliance.memberNations.isEmpty()) {
                alliance.active = false;
                alliances.remove(allianceId);
                File f = new File(alliancesDir, allianceId + ".json");
                if (f.exists()) f.delete();
            } else {
                alliance.leaderNationId = alliance.memberNations.iterator().next();
                saveAlliance(alliance);
            }
        } else {
            saveAlliance(alliance);
        }
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            if (n.getHistory() != null) {
                n.getHistory().add("Покинула военный альянс: " + alliance.name);
            }
            nationManager.save(n);
        }
        
        return "Вы покинули альянс: " + alliance.name;
    }
    
    /**
     * Update defense commitment.
     */
    public synchronized String updateDefenseCommitment(String allianceId, String leaderId, double newCommitment) throws IOException {
        if (isBlank(allianceId) || isBlank(leaderId) || !Double.isFinite(newCommitment)) return "Некорректные данные.";
        MilitaryAlliance alliance = alliances.get(allianceId);
        if (alliance == null) return "Альянс не найден.";
        if (!alliance.leaderNationId.equals(leaderId)) return "Вы не являетесь лидером альянса.";
        
        alliance.defenseCommitment = Math.max(0, Math.min(100, newCommitment));
        saveAlliance(alliance);
        
        return "Обязательство защиты обновлено: " + String.format("%.1f", alliance.defenseCommitment) + "%";
    }
    
    /**
     * Get all alliances for a nation.
     */
    public synchronized List<MilitaryAlliance> getNationAlliances(String nationId) {
        List<MilitaryAlliance> result = new ArrayList<>();
        if (isBlank(nationId)) return result;
        for (MilitaryAlliance alliance : alliances.values()) {
            if (alliance.memberNations.contains(nationId) && alliance.active) {
                result.add(alliance);
            }
        }
        return result;
    }
    
    /**
     * Calculate defensive bonus from alliances.
     */
    public synchronized double getDefensiveBonus(String nationId) {
        double bonus = 1.0;
        if (isBlank(nationId)) return bonus;
        for (MilitaryAlliance alliance : alliances.values()) {
            if (alliance.active && alliance.memberNations.contains(nationId)) {
                // +0.01% defense per 1% commitment per alliance
                bonus += (alliance.defenseCommitment / 100.0) * 0.01;
            }
        }
        return Math.min(2.0, bonus); // Cap at +100%
    }
    
    /**
     * Get global military alliance statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalMilitaryAllianceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalAlliances = alliances.size();
        int activeAlliances = 0;
        Map<String, Integer> membersByAlliance = new HashMap<>();
        int totalMembers = 0;
        double totalDefenseCommitment = 0.0;
        Map<String, Integer> nationsByMembership = new HashMap<>();
        
        for (MilitaryAlliance alliance : alliances.values()) {
            if (alliance.active) {
                activeAlliances++;
                int memberCount = alliance.memberNations.size();
                membersByAlliance.put(alliance.id, memberCount);
                totalMembers += memberCount;
                totalDefenseCommitment += alliance.defenseCommitment;
                
                for (String nationId : alliance.memberNations) {
                    nationsByMembership.put(nationId, nationsByMembership.getOrDefault(nationId, 0) + 1);
                }
            }
        }
        
        stats.put("totalAlliances", totalAlliances);
        stats.put("activeAlliances", activeAlliances);
        stats.put("totalMembers", totalMembers);
        stats.put("averageMembersPerAlliance", activeAlliances > 0 ? (double) totalMembers / activeAlliances : 0);
        stats.put("averageDefenseCommitment", activeAlliances > 0 ? totalDefenseCommitment / activeAlliances : 0);
        stats.put("membersByAlliance", membersByAlliance);
        stats.put("nationsByMembership", nationsByMembership);
        stats.put("nationsInAlliances", nationsByMembership.size());
        
        // Top alliances by members
        List<Map.Entry<String, Integer>> topByMembers = membersByAlliance.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMembers", topByMembers);
        
        // Top nations by alliance membership
        List<Map.Entry<String, Integer>> topByMembership = nationsByMembership.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMembership", topByMembership);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

