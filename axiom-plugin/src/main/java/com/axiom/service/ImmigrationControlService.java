package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages immigration policies and player migration requests. */
public class ImmigrationControlService {
    private final AXIOM plugin;
    private final File immigrationDir;
    private final Map<String, List<ImmigrationRequest>> pendingRequests = new HashMap<>(); // nationId -> requests

    public static class ImmigrationRequest {
        String playerId;
        String fromNationId;
        String toNationId;
        long timestamp;
        String reason;
        boolean approved;
    }

    public ImmigrationControlService(AXIOM plugin) {
        this.plugin = plugin;
        this.immigrationDir = new File(plugin.getDataFolder(), "immigration");
        this.immigrationDir.mkdirs();
        loadAll();
    }

    public synchronized String requestImmigration(Player player, String targetNationId, String reason) {
        String playerNationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (playerNationId == null) return "Вы не в нации.";
        if (playerNationId.equals(targetNationId)) return "Вы уже в этой нации.";
        Nation target = plugin.getNationManager().getNationById(targetNationId);
        if (target == null) return "Нация не найдена.";
        List<ImmigrationRequest> existing = pendingRequests.get(targetNationId);
        if (existing != null && existing.stream().anyMatch(r -> r.playerId.equals(player.getUniqueId().toString()))) {
            return "Заявка уже отправлена.";
        }
        ImmigrationRequest req = new ImmigrationRequest();
        req.playerId = player.getUniqueId().toString();
        req.fromNationId = playerNationId;
        req.toNationId = targetNationId;
        req.timestamp = System.currentTimeMillis();
        req.reason = reason != null ? reason : "";
        req.approved = false;
        pendingRequests.computeIfAbsent(targetNationId, k -> new ArrayList<>()).add(req);
        saveRequests(targetNationId);
        return "Заявка на иммиграцию отправлена.";
    }

    public synchronized String approveImmigration(String nationId, String playerId, UUID approverId) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        RolePermissionService rolePermissionService = plugin.getRolePermissionService();
        if (rolePermissionService == null || !rolePermissionService.hasPermission(approverId, "approve_immigration")) {
            return "Нет прав.";
        }
        List<ImmigrationRequest> requests = pendingRequests.get(nationId);
        if (requests == null) return "Нет заявок.";
        ImmigrationRequest req = requests.stream()
            .filter(r -> r.playerId.equals(playerId))
            .findFirst()
            .orElse(null);
        if (req == null) return "Заявка не найдена.";
        req.approved = true;
        // Process immigration
        UUID playerUUID = UUID.fromString(playerId);
        MigrationService migrationService = plugin.getMigrationService();
        if (migrationService == null) return "Сервис миграции недоступен.";
        migrationService.migratePlayer(playerUUID, req.fromNationId, req.toNationId);
        requests.remove(req);
        saveRequests(nationId);
        return "Иммиграция одобрена.";
    }

    private void loadAll() {
        File[] files = immigrationDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                List<ImmigrationRequest> requests = new ArrayList<>();
                if (o.has("requests")) {
                    for (var elem : o.getAsJsonArray("requests")) {
                        JsonObject reqObj = elem.getAsJsonObject();
                        ImmigrationRequest req = new ImmigrationRequest();
                        req.playerId = reqObj.get("playerId").getAsString();
                        req.fromNationId = reqObj.get("fromNationId").getAsString();
                        req.toNationId = reqObj.get("toNationId").getAsString();
                        req.timestamp = reqObj.get("timestamp").getAsLong();
                        req.reason = reqObj.has("reason") ? reqObj.get("reason").getAsString() : "";
                        req.approved = reqObj.has("approved") && reqObj.get("approved").getAsBoolean();
                        if (!req.approved) requests.add(req); // Only load pending
                    }
                }
                pendingRequests.put(nationId, requests);
            } catch (Exception ignored) {}
        }
    }

    private void saveRequests(String nationId) {
        File f = new File(immigrationDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<ImmigrationRequest> requests = pendingRequests.get(nationId);
        if (requests != null) {
            for (ImmigrationRequest req : requests) {
                JsonObject reqObj = new JsonObject();
                reqObj.addProperty("playerId", req.playerId);
                reqObj.addProperty("fromNationId", req.fromNationId);
                reqObj.addProperty("toNationId", req.toNationId);
                reqObj.addProperty("timestamp", req.timestamp);
                reqObj.addProperty("reason", req.reason);
                reqObj.addProperty("approved", req.approved);
                arr.add(reqObj);
            }
        }
        o.add("requests", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive immigration statistics for a nation.
     */
    public synchronized Map<String, Object> getImmigrationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<ImmigrationRequest> requests = pendingRequests.get(nationId);
        if (requests == null || requests.isEmpty()) {
            stats.put("hasPendingRequests", false);
            stats.put("pendingRequests", 0);
            return stats;
        }
        
        int pending = requests.size();
        Map<String, Integer> requestsBySourceNation = new HashMap<>();
        
        for (ImmigrationRequest req : requests) {
            requestsBySourceNation.put(req.fromNationId,
                requestsBySourceNation.getOrDefault(req.fromNationId, 0) + 1);
        }
        
        stats.put("hasPendingRequests", true);
        stats.put("pendingRequests", pending);
        stats.put("requestsBySourceNation", requestsBySourceNation);
        stats.put("uniqueSourceNations", requestsBySourceNation.size());
        
        // Immigration rating
        String rating = "НЕТ ЗАЯВОК";
        if (pending >= 20) rating = "ПОПУЛЯРНОЕ НАПРАВЛЕНИЕ";
        else if (pending >= 10) rating = "ВОСТРЕБОВАНОЕ";
        else if (pending >= 5) rating = "АКТИВНОЕ";
        else if (pending >= 1) rating = "НАЧАЛЬНОЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global immigration statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalImmigrationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPendingRequests = 0;
        Map<String, Integer> requestsByTargetNation = new HashMap<>();
        Map<String, Integer> requestsBySourceNation = new HashMap<>();
        
        for (Map.Entry<String, List<ImmigrationRequest>> entry : pendingRequests.entrySet()) {
            String targetNationId = entry.getKey();
            List<ImmigrationRequest> requests = entry.getValue();
            
            int count = requests.size();
            totalPendingRequests += count;
            requestsByTargetNation.put(targetNationId, count);
            
            for (ImmigrationRequest req : requests) {
                requestsBySourceNation.put(req.fromNationId,
                    requestsBySourceNation.getOrDefault(req.fromNationId, 0) + 1);
            }
        }
        
        stats.put("totalPendingRequests", totalPendingRequests);
        stats.put("requestsByTargetNation", requestsByTargetNation);
        stats.put("requestsBySourceNation", requestsBySourceNation);
        stats.put("nationsReceivingRequests", requestsByTargetNation.size());
        stats.put("nationsSendingRequests", requestsBySourceNation.size());
        
        // Average requests per target nation
        stats.put("averageRequestsPerTarget", requestsByTargetNation.size() > 0 ?
            (double) totalPendingRequests / requestsByTargetNation.size() : 0);
        
        // Top nations receiving requests
        List<Map.Entry<String, Integer>> topByReceiving = requestsByTargetNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByReceiving", topByReceiving);
        
        // Top nations sending requests
        List<Map.Entry<String, Integer>> topBySending = requestsBySourceNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySending", topBySending);
        
        return stats;
    }
}

