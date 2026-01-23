package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages treaty renegotiation requests. */
public class TreatyRenegotiationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File renegotiationsDir;
    private final Map<String, List<RenegotiationRequest>> pendingRequests = new HashMap<>(); // nationId -> requests

    public static class RenegotiationRequest {
        String treatyId;
        String initiatorNationId;
        String otherNationId;
        String newTerms;
        long timestamp;
        boolean accepted;
    }

    public TreatyRenegotiationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.renegotiationsDir = new File(plugin.getDataFolder(), "renegotiations");
        this.renegotiationsDir.mkdirs();
        loadAll();
    }

    public synchronized String requestRenegotiation(String treatyId, String initiatorId, String otherId, String newTerms) {
        Nation initiator = nationManager.getNationById(initiatorId);
        Nation other = nationManager.getNationById(otherId);
        if (initiator == null || other == null) return "Нация не найдена.";
        if (initiatorId.equals(otherId)) return "Нельзя пересматривать договор с собой.";
        if (newTerms == null || newTerms.isBlank()) return "Новые условия не заданы.";
        List<RenegotiationRequest> existing = pendingRequests.get(otherId);
        if (existing != null && existing.stream().anyMatch(r -> r.treatyId.equals(treatyId) && r.initiatorNationId.equals(initiatorId))) {
            return "Запрос уже отправлен.";
        }
        RenegotiationRequest req = new RenegotiationRequest();
        req.treatyId = treatyId;
        req.initiatorNationId = initiatorId;
        req.otherNationId = otherId;
        req.newTerms = newTerms;
        req.timestamp = System.currentTimeMillis();
        req.accepted = false;
        pendingRequests.computeIfAbsent(otherId, k -> new ArrayList<>()).add(req);
        saveRequests(otherId);
        other.getHistory().add("Запрос на пересмотр договора от " + initiator.getName());
        try { nationManager.save(other); } catch (Exception ignored) {}
        return "Запрос на пересмотр отправлен.";
    }

    public synchronized String acceptRenegotiation(String nationId, String treatyId) {
        List<RenegotiationRequest> requests = pendingRequests.get(nationId);
        if (requests == null) return "Нет запросов.";
        RenegotiationRequest req = requests.stream()
            .filter(r -> r.treatyId.equals(treatyId))
            .findFirst()
            .orElse(null);
        if (req == null) return "Запрос не найден.";
        req.accepted = true;
        // Apply new terms (simplified - would integrate with TreatyService)
        Nation initiator = nationManager.getNationById(req.initiatorNationId);
        Nation other = nationManager.getNationById(req.otherNationId);
        if (initiator != null && other != null) {
            initiator.getHistory().add("Договор пересмотрен: " + req.newTerms);
            other.getHistory().add("Договор пересмотрен: " + req.newTerms);
            try {
                nationManager.save(initiator);
                nationManager.save(other);
            } catch (Exception ignored) {}
        }
        requests.remove(req);
        saveRequests(nationId);
        return "Пересмотр принят.";
    }

    private void loadAll() {
        File[] files = renegotiationsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                List<RenegotiationRequest> requests = new ArrayList<>();
                if (o.has("requests")) {
                    for (var elem : o.getAsJsonArray("requests")) {
                        JsonObject reqObj = elem.getAsJsonObject();
                        RenegotiationRequest req = new RenegotiationRequest();
                        req.treatyId = reqObj.get("treatyId").getAsString();
                        req.initiatorNationId = reqObj.get("initiatorNationId").getAsString();
                        req.otherNationId = reqObj.get("otherNationId").getAsString();
                        req.newTerms = reqObj.has("newTerms") ? reqObj.get("newTerms").getAsString() : "";
                        req.timestamp = reqObj.get("timestamp").getAsLong();
                        req.accepted = reqObj.has("accepted") && reqObj.get("accepted").getAsBoolean();
                        if (!req.accepted) requests.add(req);
                    }
                }
                pendingRequests.put(nationId, requests);
            } catch (Exception ignored) {}
        }
    }

    private void saveRequests(String nationId) {
        File f = new File(renegotiationsDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<RenegotiationRequest> requests = pendingRequests.get(nationId);
        if (requests != null) {
            for (RenegotiationRequest req : requests) {
                JsonObject reqObj = new JsonObject();
                reqObj.addProperty("treatyId", req.treatyId);
                reqObj.addProperty("initiatorNationId", req.initiatorNationId);
                reqObj.addProperty("otherNationId", req.otherNationId);
                reqObj.addProperty("newTerms", req.newTerms);
                reqObj.addProperty("timestamp", req.timestamp);
                reqObj.addProperty("accepted", req.accepted);
                arr.add(reqObj);
            }
        }
        o.add("requests", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive treaty renegotiation statistics for a nation.
     */
    public synchronized Map<String, Object> getTreatyRenegotiationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<RenegotiationRequest> requests = pendingRequests.get(nationId);
        int sentRequests = 0;
        int receivedRequests = 0;
        
        // Count requests sent (as initiator)
        for (List<RenegotiationRequest> reqList : pendingRequests.values()) {
            for (RenegotiationRequest req : reqList) {
                if (req.initiatorNationId.equals(nationId)) {
                    sentRequests++;
                }
            }
        }
        
        // Count requests received (as other party)
        if (requests != null) {
            receivedRequests = requests.size();
        }
        
        stats.put("sentRequests", sentRequests);
        stats.put("receivedRequests", receivedRequests);
        stats.put("totalRequests", sentRequests + receivedRequests);
        
        // Renegotiation rating
        String rating = "НЕТ ЗАПРОСОВ";
        int total = sentRequests + receivedRequests;
        if (total >= 20) rating = "ОЧЕНЬ АКТИВНЫЙ";
        else if (total >= 10) rating = "АКТИВНЫЙ";
        else if (total >= 5) rating = "УМЕРЕННЫЙ";
        else if (total >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global treaty renegotiation statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalTreatyRenegotiationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPendingRequests = 0;
        Map<String, Integer> requestsSentByNation = new HashMap<>();
        Map<String, Integer> requestsReceivedByNation = new HashMap<>();
        
        for (Map.Entry<String, List<RenegotiationRequest>> entry : pendingRequests.entrySet()) {
            String nationId = entry.getKey();
            List<RenegotiationRequest> requests = entry.getValue();
            
            int received = requests.size();
            totalPendingRequests += received;
            requestsReceivedByNation.put(nationId, received);
            
            for (RenegotiationRequest req : requests) {
                requestsSentByNation.put(req.initiatorNationId,
                    requestsSentByNation.getOrDefault(req.initiatorNationId, 0) + 1);
            }
        }
        
        stats.put("totalPendingRequests", totalPendingRequests);
        stats.put("requestsSentByNation", requestsSentByNation);
        stats.put("requestsReceivedByNation", requestsReceivedByNation);
        stats.put("nationsSendingRequests", requestsSentByNation.size());
        stats.put("nationsReceivingRequests", requestsReceivedByNation.size());
        
        // Average requests per nation
        stats.put("averageRequestsPerNation", requestsSentByNation.size() > 0 ?
            (double) totalPendingRequests / requestsSentByNation.size() : 0);
        
        // Top nations sending requests
        List<Map.Entry<String, Integer>> topBySending = requestsSentByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySending", topBySending);
        
        // Top nations receiving requests
        List<Map.Entry<String, Integer>> topByReceiving = requestsReceivedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByReceiving", topByReceiving);
        
        return stats;
    }
}

