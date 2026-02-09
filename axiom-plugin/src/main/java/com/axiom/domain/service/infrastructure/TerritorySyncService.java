package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.axiom.domain.service.state.TerritoryService;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pushes territory snapshots/deltas to UI clients over axiom:ui.
 */
public class TerritorySyncService {
    private static final String CHANNEL = "axiom:ui";
    private static final long PUSH_INTERVAL_TICKS = 40L;

    private final AXIOM plugin;
    private final TerritoryService territoryService;
    private final Gson gson = new Gson();
    private final Map<UUID, Long> lastVersionByPlayer = new ConcurrentHashMap<>();
    private int taskId = -1;

    public TerritorySyncService(AXIOM plugin, TerritoryService territoryService) {
        this.plugin = plugin;
        this.territoryService = territoryService;
        start();
    }

    private void start() {
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::pushUpdates, 20L, PUSH_INTERVAL_TICKS).getTaskId();
        plugin.getLogger().info("TerritorySyncService started (push every " + PUSH_INTERVAL_TICKS + " ticks)");
    }

    private void pushUpdates() {
        if (territoryService == null) {
            return;
        }
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (!player.getListeningPluginChannels().contains(CHANNEL)) {
                continue;
            }
            UUID id = player.getUniqueId();
            online.add(id);
            long lastVersion = lastVersionByPlayer.getOrDefault(id, -1L);
            if (lastVersion < 0) {
                sendSnapshot(player);
                lastVersionByPlayer.put(id, territoryService.getVersion());
                continue;
            }
            TerritoryService.DeltaResult delta = territoryService.getDeltaSince(lastVersion);
            if (delta.requiresSnapshot()) {
                sendSnapshot(player);
                lastVersionByPlayer.put(id, delta.getVersion());
                continue;
            }
            if (!delta.getChanges().isEmpty()) {
                sendDelta(player, delta);
                lastVersionByPlayer.put(id, delta.getVersion());
            } else if (delta.getVersion() != lastVersion) {
                lastVersionByPlayer.put(id, delta.getVersion());
            }
        }
        lastVersionByPlayer.keySet().removeIf(id -> !online.contains(id));
    }

    private void sendSnapshot(Player player) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("version", territoryService.getVersion());
        payload.put("territories", com.axiom.api.AxiomAPI.getTerritories());
        sendData(player, "territories_snapshot", payload);
        plugin.getLogger().fine("Pushed territories snapshot to " + player.getName());
    }

    private void sendDelta(Player player, TerritoryService.DeltaResult delta) {
        List<TerritoryService.TerritoryChange> changes = delta.getChanges();
        if (changes.isEmpty()) {
            return;
        }
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (TerritoryService.TerritoryChange change : changes) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("op", change.getOp());
            entry.put("world", change.getWorld());
            entry.put("x", change.getX());
            entry.put("z", change.getZ());
            entry.put("nationId", change.getNationId());
            out.add(entry);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("version", delta.getVersion());
        payload.put("changes", out);
        sendData(player, "territories_delta", payload);
        plugin.getLogger().info("Pushed territories delta to " + player.getName() + " (" + out.size() + ")");
    }

    private void sendData(Player player, String type, Object data) {
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(msgBytes);
            out.writeUTF(type);
            out.writeUTF(gson.toJson(data));
            player.sendPluginMessage(plugin, CHANNEL, msgBytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to push territory sync: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
