package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages international court cases and rulings. */
public class InternationalCourtService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File casesDir;
    private final Map<String, CourtCase> activeCases = new HashMap<>(); // caseId -> case

    public static class CourtCase {
        String id;
        String plaintiffNationId;
        String defendantNationId;
        String charge;
        String description;
        long filedAt;
        String verdict; // null if pending
        double penalty;
        boolean resolved;
    }

    public InternationalCourtService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.casesDir = new File(plugin.getDataFolder(), "courtcases");
        this.casesDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processCases, 0, 20 * 60 * 30); // every 30 minutes
    }

    public synchronized String fileCase(String plaintiffId, String defendantId, String charge, String description) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (plaintiffId == null || defendantId == null) return "Неверные параметры.";
        if (plaintiffId.equals(defendantId)) return "Нельзя подать дело против себя.";
        if (charge == null || charge.trim().isEmpty()) return "Обвинение не указано.";
        Nation plaintiff = nationManager.getNationById(plaintiffId);
        Nation defendant = nationManager.getNationById(defendantId);
        if (plaintiff == null || defendant == null) return "Нация не найдена.";
        String caseId = UUID.randomUUID().toString().substring(0, 8);
        CourtCase courtCase = new CourtCase();
        courtCase.id = caseId;
        courtCase.plaintiffNationId = plaintiffId;
        courtCase.defendantNationId = defendantId;
        courtCase.charge = charge;
        courtCase.description = description;
        courtCase.filedAt = System.currentTimeMillis();
        courtCase.resolved = false;
        activeCases.put(caseId, courtCase);
        plaintiff.getHistory().add("Подано дело против " + defendant.getName());
        defendant.getHistory().add("Получено дело от " + plaintiff.getName());
        try {
            nationManager.save(plaintiff);
            nationManager.save(defendant);
            saveCase(courtCase);
        } catch (Exception ignored) {}
        return "Дело подано (ID: " + caseId + ")";
    }

    private synchronized void processCases() {
        if (nationManager == null) return;
        long now = System.currentTimeMillis();
        List<String> resolved = new ArrayList<>();
        for (Map.Entry<String, CourtCase> e : activeCases.entrySet()) {
            CourtCase courtCase = e.getValue();
            if (courtCase.resolved) {
                resolved.add(e.getKey());
                continue;
            }
            // Auto-resolve after 24 hours
            if (now - courtCase.filedAt > 24 * 60 * 60_000L) {
                // Random verdict based on evidence
                boolean guilty = Math.random() < 0.6; // 60% chance of guilty verdict
                courtCase.verdict = guilty ? "guilty" : "not_guilty";
                courtCase.penalty = guilty ? 5000.0 : 0.0;
                courtCase.resolved = true;
                Nation defendant = nationManager.getNationById(courtCase.defendantNationId);
                Nation plaintiff = nationManager.getNationById(courtCase.plaintiffNationId);
                if (guilty && defendant != null) {
                    if (defendant.getTreasury() >= courtCase.penalty) {
                        defendant.setTreasury(defendant.getTreasury() - courtCase.penalty);
                        if (plaintiff != null) {
                            plaintiff.setTreasury(plaintiff.getTreasury() + courtCase.penalty);
                        }
                        try {
                            nationManager.save(defendant);
                            if (plaintiff != null) nationManager.save(plaintiff);
                        } catch (Exception ignored) {}
                    }
                    defendant.getHistory().add("Приговор: виновен. Штраф: " + courtCase.penalty);
                    if (plaintiff != null) {
                        plaintiff.getHistory().add("Выиграно дело против " + defendant.getName());
                    }
                } else if (!guilty && defendant != null) {
                    defendant.getHistory().add("Приговор: невиновен");
                }
                saveCase(courtCase);
                resolved.add(e.getKey());
            }
        }
        for (String caseId : resolved) {
            activeCases.remove(caseId);
        }
    }

    private void loadAll() {
        File[] files = casesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                CourtCase courtCase = new CourtCase();
                courtCase.id = o.get("id").getAsString();
                courtCase.plaintiffNationId = o.get("plaintiffNationId").getAsString();
                courtCase.defendantNationId = o.get("defendantNationId").getAsString();
                courtCase.charge = o.get("charge").getAsString();
                courtCase.description = o.get("description").getAsString();
                courtCase.filedAt = o.get("filedAt").getAsLong();
                courtCase.verdict = o.has("verdict") && !o.get("verdict").isJsonNull() ? o.get("verdict").getAsString() : null;
                courtCase.penalty = o.has("penalty") ? o.get("penalty").getAsDouble() : 0.0;
                courtCase.resolved = o.has("resolved") && o.get("resolved").getAsBoolean();
                if (!courtCase.resolved) activeCases.put(courtCase.id, courtCase);
            } catch (Exception ignored) {}
        }
    }

    private void saveCase(CourtCase courtCase) {
        File f = new File(casesDir, courtCase.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", courtCase.id);
        o.addProperty("plaintiffNationId", courtCase.plaintiffNationId);
        o.addProperty("defendantNationId", courtCase.defendantNationId);
        o.addProperty("charge", courtCase.charge);
        o.addProperty("description", courtCase.description);
        o.addProperty("filedAt", courtCase.filedAt);
        if (courtCase.verdict != null) o.addProperty("verdict", courtCase.verdict);
        o.addProperty("penalty", courtCase.penalty);
        o.addProperty("resolved", courtCase.resolved);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive international court statistics for a nation.
     */
    public synchronized Map<String, Object> getInternationalCourtStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        int casesAsPlaintiff = 0;
        int casesAsDefendant = 0;
        int wonCases = 0;
        int lostCases = 0;
        double totalPenaltiesReceived = 0.0;
        double totalPenaltiesPaid = 0.0;
        List<String> pendingCases = new ArrayList<>();
        
        for (CourtCase courtCase : activeCases.values()) {
            if (courtCase.plaintiffNationId.equals(nationId)) {
                casesAsPlaintiff++;
                if (courtCase.resolved && "guilty".equals(courtCase.verdict)) {
                    wonCases++;
                    totalPenaltiesReceived += courtCase.penalty;
                } else if (!courtCase.resolved) {
                    pendingCases.add(courtCase.id);
                }
            }
            if (courtCase.defendantNationId.equals(nationId)) {
                casesAsDefendant++;
                if (courtCase.resolved && "guilty".equals(courtCase.verdict)) {
                    lostCases++;
                    totalPenaltiesPaid += courtCase.penalty;
                } else if (!courtCase.resolved) {
                    pendingCases.add(courtCase.id);
                }
            }
        }
        
        stats.put("casesAsPlaintiff", casesAsPlaintiff);
        stats.put("casesAsDefendant", casesAsDefendant);
        stats.put("totalCases", casesAsPlaintiff + casesAsDefendant);
        stats.put("wonCases", wonCases);
        stats.put("lostCases", lostCases);
        stats.put("pendingCases", pendingCases.size());
        stats.put("totalPenaltiesReceived", totalPenaltiesReceived);
        stats.put("totalPenaltiesPaid", totalPenaltiesPaid);
        stats.put("winRate", (casesAsPlaintiff + casesAsDefendant) > 0 ? 
            (double) wonCases / (casesAsPlaintiff + casesAsDefendant) : 0);
        
        // Court rating
        String rating = "БЕЗ ДЕЛ";
        if (wonCases >= 10) rating = "ДОМИНИРУЮЩИЙ";
        else if (wonCases >= 5) rating = "УСПЕШНЫЙ";
        else if (casesAsPlaintiff + casesAsDefendant >= 5) rating = "АКТИВНЫЙ";
        else if (casesAsPlaintiff + casesAsDefendant >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global international court statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalInternationalCourtStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveCases", activeCases.size());
        
        Map<String, Integer> casesAsPlaintiffByNation = new HashMap<>();
        Map<String, Integer> casesAsDefendantByNation = new HashMap<>();
        Map<String, Integer> wonCasesByNation = new HashMap<>();
        Map<String, Integer> lostCasesByNation = new HashMap<>();
        Map<String, Double> penaltiesReceivedByNation = new HashMap<>();
        Map<String, Double> penaltiesPaidByNation = new HashMap<>();
        int pendingCases = 0;
        int resolvedCases = 0;
        Map<String, Integer> casesByCharge = new HashMap<>();
        
        for (CourtCase courtCase : activeCases.values()) {
            if (!courtCase.resolved) {
                pendingCases++;
            } else {
                resolvedCases++;
            }
            
            casesAsPlaintiffByNation.put(courtCase.plaintiffNationId,
                casesAsPlaintiffByNation.getOrDefault(courtCase.plaintiffNationId, 0) + 1);
            casesAsDefendantByNation.put(courtCase.defendantNationId,
                casesAsDefendantByNation.getOrDefault(courtCase.defendantNationId, 0) + 1);
            
            if (courtCase.resolved && "guilty".equals(courtCase.verdict)) {
                wonCasesByNation.put(courtCase.plaintiffNationId,
                    wonCasesByNation.getOrDefault(courtCase.plaintiffNationId, 0) + 1);
                lostCasesByNation.put(courtCase.defendantNationId,
                    lostCasesByNation.getOrDefault(courtCase.defendantNationId, 0) + 1);
                
                penaltiesReceivedByNation.put(courtCase.plaintiffNationId,
                    penaltiesReceivedByNation.getOrDefault(courtCase.plaintiffNationId, 0.0) + courtCase.penalty);
                penaltiesPaidByNation.put(courtCase.defendantNationId,
                    penaltiesPaidByNation.getOrDefault(courtCase.defendantNationId, 0.0) + courtCase.penalty);
            }
            
            casesByCharge.put(courtCase.charge, casesByCharge.getOrDefault(courtCase.charge, 0) + 1);
        }
        
        stats.put("casesAsPlaintiffByNation", casesAsPlaintiffByNation);
        stats.put("casesAsDefendantByNation", casesAsDefendantByNation);
        stats.put("wonCasesByNation", wonCasesByNation);
        stats.put("lostCasesByNation", lostCasesByNation);
        stats.put("penaltiesReceivedByNation", penaltiesReceivedByNation);
        stats.put("penaltiesPaidByNation", penaltiesPaidByNation);
        stats.put("pendingCases", pendingCases);
        stats.put("resolvedCases", resolvedCases);
        stats.put("casesByCharge", casesByCharge);
        
        // Total penalties
        double totalReceived = penaltiesReceivedByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalPaid = penaltiesPaidByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("totalPenaltiesReceived", totalReceived);
        stats.put("totalPenaltiesPaid", totalPaid);
        
        // Nations involved
        Set<String> allNations = new HashSet<>();
        allNations.addAll(casesAsPlaintiffByNation.keySet());
        allNations.addAll(casesAsDefendantByNation.keySet());
        stats.put("nationsInvolved", allNations.size());
        
        // Top nations by cases filed
        List<Map.Entry<String, Integer>> topByCases = casesAsPlaintiffByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCases", topByCases);
        
        // Top nations by wins
        List<Map.Entry<String, Integer>> topByWins = wonCasesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByWins", topByWins);
        
        // Average win rate
        double totalWins = wonCasesByNation.values().stream().mapToInt(Integer::intValue).sum();
        double totalCases = casesAsPlaintiffByNation.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageWinRate", totalCases > 0 ? totalWins / totalCases : 0);
        
        return stats;
    }
}

