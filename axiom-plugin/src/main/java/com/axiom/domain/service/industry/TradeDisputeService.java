package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages trade disputes and arbitration. */
public class TradeDisputeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File disputesDir;
    private final Map<String, TradeDispute> activeDisputes = new HashMap<>(); // disputeId -> dispute

    public static class TradeDispute {
        String id;
        String complainantNationId;
        String defendantNationId;
        String issue;
        String description;
        double damages;
        long filedAt;
        boolean resolved;
        String resolution;
    }

    public TradeDisputeService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.disputesDir = new File(plugin.getDataFolder(), "tradedisputes");
        this.disputesDir.mkdirs();
        loadAll();
    }

    public synchronized String fileDispute(String complainantId, String defendantId, String issue, String description, double damages) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(complainantId) || isBlank(defendantId)) return "Неверные параметры.";
        if (complainantId.equals(defendantId)) return "Нельзя подать жалобу на себя.";
        if (isBlank(issue)) return "Не указана причина спора.";
        if (!Double.isFinite(damages) || damages < 0) return "Некорректный размер ущерба.";
        Nation complainant = nationManager.getNationById(complainantId);
        Nation defendant = nationManager.getNationById(defendantId);
        if (complainant == null || defendant == null) return "Нация не найдена.";
        String disputeId = UUID.randomUUID().toString().substring(0, 8);
        TradeDispute dispute = new TradeDispute();
        dispute.id = disputeId;
        dispute.complainantNationId = complainantId;
        dispute.defendantNationId = defendantId;
        dispute.issue = issue;
        dispute.description = description != null ? description : "";
        dispute.damages = damages;
        dispute.filedAt = System.currentTimeMillis();
        dispute.resolved = false;
        activeDisputes.put(disputeId, dispute);
        if (complainant.getHistory() != null) {
            complainant.getHistory().add("Подана жалоба на " + defendant.getName());
        }
        if (defendant.getHistory() != null) {
            defendant.getHistory().add("Получена жалоба от " + complainant.getName());
        }
        try {
            nationManager.save(complainant);
            nationManager.save(defendant);
            saveDispute(dispute);
        } catch (Exception ignored) {}
        return "Жалоба подана (ID: " + disputeId + ")";
    }

    public synchronized String resolveDispute(String disputeId, String resolution, boolean favorComplainant) {
        if (isBlank(disputeId)) return "Неверный идентификатор жалобы.";
        TradeDispute dispute = activeDisputes.get(disputeId);
        if (dispute == null) return "Жалоба не найдена.";
        if (dispute.resolved) return "Жалоба уже решена.";
        dispute.resolved = true;
        dispute.resolution = resolution != null ? resolution : "";
        if (favorComplainant) {
            Nation defendant = nationManager != null ? nationManager.getNationById(dispute.defendantNationId) : null;
            if (defendant != null && defendant.getTreasury() >= dispute.damages) {
                defendant.setTreasury(defendant.getTreasury() - dispute.damages);
                Nation complainant = nationManager.getNationById(dispute.complainantNationId);
                if (complainant != null) {
                    complainant.setTreasury(complainant.getTreasury() + dispute.damages);
                }
                try {
                    nationManager.save(defendant);
                    if (complainant != null) nationManager.save(complainant);
                } catch (Exception ignored) {}
            }
        }
        saveDispute(dispute);
        return "Жалоба решена: " + resolution;
    }

    private void loadAll() {
        File[] files = disputesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                TradeDispute dispute = new TradeDispute();
                dispute.id = o.has("id") ? o.get("id").getAsString() : null;
                dispute.complainantNationId = o.has("complainantNationId") ? o.get("complainantNationId").getAsString() : null;
                dispute.defendantNationId = o.has("defendantNationId") ? o.get("defendantNationId").getAsString() : null;
                dispute.issue = o.has("issue") ? o.get("issue").getAsString() : null;
                dispute.description = o.has("description") ? o.get("description").getAsString() : "";
                dispute.damages = o.has("damages") ? o.get("damages").getAsDouble() : 0.0;
                dispute.filedAt = o.has("filedAt") ? o.get("filedAt").getAsLong() : 0L;
                dispute.resolved = o.has("resolved") && o.get("resolved").getAsBoolean();
                dispute.resolution = o.has("resolution") ? o.get("resolution").getAsString() : "";
                if (!isBlank(dispute.id)) {
                    activeDisputes.put(dispute.id, dispute);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveDispute(TradeDispute dispute) {
        if (dispute == null || isBlank(dispute.id)) return;
        File f = new File(disputesDir, dispute.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", dispute.id);
        o.addProperty("complainantNationId", dispute.complainantNationId);
        o.addProperty("defendantNationId", dispute.defendantNationId);
        o.addProperty("issue", dispute.issue);
        o.addProperty("description", dispute.description);
        o.addProperty("damages", dispute.damages);
        o.addProperty("filedAt", dispute.filedAt);
        o.addProperty("resolved", dispute.resolved);
        o.addProperty("resolution", dispute.resolution);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive trade dispute statistics for a nation.
     */
    public synchronized Map<String, Object> getTradeDisputeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        int disputesAsComplainant = 0;
        int disputesAsDefendant = 0;
        int wonDisputes = 0;
        int lostDisputes = 0;
        double totalDamagesReceived = 0.0;
        double totalDamagesPaid = 0.0;
        List<String> pendingDisputes = new ArrayList<>();
        Map<String, Integer> disputesByIssue = new HashMap<>();
        
        for (TradeDispute dispute : activeDisputes.values()) {
            if (dispute == null) continue;
            if (Objects.equals(dispute.complainantNationId, nationId)) {
                disputesAsComplainant++;
                if (dispute.issue != null) {
                    disputesByIssue.put(dispute.issue, disputesByIssue.getOrDefault(dispute.issue, 0) + 1);
                }
                if (!dispute.resolved) {
                    pendingDisputes.add(dispute.id);
                } else if (dispute.resolved && dispute.resolution != null && dispute.resolution.contains("favor")) {
                    wonDisputes++;
                    totalDamagesReceived += dispute.damages;
                }
            }
            if (Objects.equals(dispute.defendantNationId, nationId)) {
                disputesAsDefendant++;
                if (!dispute.resolved) {
                    pendingDisputes.add(dispute.id);
                } else if (dispute.resolved && dispute.resolution != null && dispute.resolution.contains("favor")) {
                    lostDisputes++;
                    totalDamagesPaid += dispute.damages;
                }
            }
        }
        
        stats.put("disputesAsComplainant", disputesAsComplainant);
        stats.put("disputesAsDefendant", disputesAsDefendant);
        stats.put("totalDisputes", disputesAsComplainant + disputesAsDefendant);
        stats.put("wonDisputes", wonDisputes);
        stats.put("lostDisputes", lostDisputes);
        stats.put("pendingDisputes", pendingDisputes.size());
        stats.put("totalDamagesReceived", totalDamagesReceived);
        stats.put("totalDamagesPaid", totalDamagesPaid);
        stats.put("disputesByIssue", disputesByIssue);
        stats.put("winRate", (disputesAsComplainant + disputesAsDefendant) > 0 ? 
            (double) wonDisputes / (disputesAsComplainant + disputesAsDefendant) : 0);
        
        // Dispute rating
        String rating = "БЕЗ СПОРОВ";
        if (wonDisputes >= 10) rating = "ДОМИНИРУЮЩИЙ";
        else if (wonDisputes >= 5) rating = "УСПЕШНЫЙ";
        else if (disputesAsComplainant + disputesAsDefendant >= 5) rating = "АКТИВНЫЙ";
        else if (disputesAsComplainant + disputesAsDefendant >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global trade dispute statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalTradeDisputeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveDisputes", activeDisputes.size());
        
        int resolvedDisputes = 0;
        int pendingDisputes = 0;
        Map<String, Integer> disputesAsComplainantByNation = new HashMap<>();
        Map<String, Integer> disputesAsDefendantByNation = new HashMap<>();
        Map<String, Integer> wonDisputesByNation = new HashMap<>();
        Map<String, Integer> lostDisputesByNation = new HashMap<>();
        Map<String, Double> damagesReceivedByNation = new HashMap<>();
        Map<String, Double> damagesPaidByNation = new HashMap<>();
        Map<String, Integer> disputesByIssue = new HashMap<>();
        
        for (TradeDispute dispute : activeDisputes.values()) {
            if (dispute == null) continue;
            if (dispute.resolved) {
                resolvedDisputes++;
            } else {
                pendingDisputes++;
            }
            
            if (dispute.complainantNationId != null) {
                disputesAsComplainantByNation.put(dispute.complainantNationId,
                    disputesAsComplainantByNation.getOrDefault(dispute.complainantNationId, 0) + 1);
            }
            if (dispute.defendantNationId != null) {
                disputesAsDefendantByNation.put(dispute.defendantNationId,
                    disputesAsDefendantByNation.getOrDefault(dispute.defendantNationId, 0) + 1);
            }
            
            if (dispute.issue != null) {
                disputesByIssue.put(dispute.issue, disputesByIssue.getOrDefault(dispute.issue, 0) + 1);
            }
            
            if (dispute.resolved && dispute.resolution != null && dispute.resolution.contains("favor")) {
                if (dispute.complainantNationId != null) {
                    wonDisputesByNation.put(dispute.complainantNationId,
                        wonDisputesByNation.getOrDefault(dispute.complainantNationId, 0) + 1);
                    damagesReceivedByNation.put(dispute.complainantNationId,
                        damagesReceivedByNation.getOrDefault(dispute.complainantNationId, 0.0) + dispute.damages);
                }
                if (dispute.defendantNationId != null) {
                    lostDisputesByNation.put(dispute.defendantNationId,
                        lostDisputesByNation.getOrDefault(dispute.defendantNationId, 0) + 1);
                    damagesPaidByNation.put(dispute.defendantNationId,
                        damagesPaidByNation.getOrDefault(dispute.defendantNationId, 0.0) + dispute.damages);
                }
            }
        }
        
        stats.put("resolvedDisputes", resolvedDisputes);
        stats.put("pendingDisputes", pendingDisputes);
        stats.put("disputesAsComplainantByNation", disputesAsComplainantByNation);
        stats.put("disputesAsDefendantByNation", disputesAsDefendantByNation);
        stats.put("wonDisputesByNation", wonDisputesByNation);
        stats.put("lostDisputesByNation", lostDisputesByNation);
        stats.put("damagesReceivedByNation", damagesReceivedByNation);
        stats.put("damagesPaidByNation", damagesPaidByNation);
        stats.put("disputesByIssue", disputesByIssue);
        
        // Total damages
        double totalReceived = damagesReceivedByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalPaid = damagesPaidByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("totalDamagesReceived", totalReceived);
        stats.put("totalDamagesPaid", totalPaid);
        
        // Top nations by disputes filed
        List<Map.Entry<String, Integer>> topByDisputes = disputesAsComplainantByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByDisputes", topByDisputes);
        
        // Top nations by wins
        List<Map.Entry<String, Integer>> topByWins = wonDisputesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByWins", topByWins);
        
        // Average win rate
        double totalWins = wonDisputesByNation.values().stream().mapToInt(Integer::intValue).sum();
        double totalCases = disputesAsComplainantByNation.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageWinRate", totalCases > 0 ? totalWins / totalCases : 0);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

