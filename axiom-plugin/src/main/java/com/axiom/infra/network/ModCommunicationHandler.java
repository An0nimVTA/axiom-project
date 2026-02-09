package com.axiom.infra.network;

import com.axiom.AXIOM;
import com.axiom.api.AxiomAPI;
import com.google.gson.Gson;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.Map;

public class ModCommunicationHandler implements PluginMessageListener {
    private static final Gson gson = new Gson();
    private final AXIOM plugin;

    public ModCommunicationHandler(AXIOM plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("axiom:ui")) return;
        if (message == null || message.length == 0) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String action;
            try {
                action = in.readUTF();
            } catch (EOFException eof) {
                return;
            }

            switch (action) {
                case "get_stats":
                    sendStats(player);
                    break;
                case "get_techs":
                    sendTechs(player);
                    break;
                case "get_nations":
                    sendNations(player);
                    break;
                case "get_territories_snapshot":
                    sendTerritoriesSnapshot(player);
                    break;
                case "get_territories_delta":
                    long since = -1L;
                    try {
                        since = in.readLong();
                    } catch (EOFException eof) {
                        // no version provided
                    }
                    sendTerritoriesDelta(player, since);
                    break;
                case "get_language":
                    sendLanguage(player);
                    break;
                case "set_language":
                    String lang;
                    try {
                        lang = in.readUTF();
                    } catch (EOFException eof) {
                        return;
                    }
                    setLanguage(player, lang);
                    break;
                case "execute_command":
                    String rawCmd;
                    try {
                        rawCmd = in.readUTF();
                    } catch (EOFException eof) {
                        return;
                    }
                    String cmd = rawCmd.startsWith("/") ? rawCmd.substring(1) : rawCmd;
                    boolean success = AxiomAPI.executeCommand(player, cmd);
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("command", rawCmd);
                    result.put("success", success);
                    result.put("message", success ? "ok" : "failed");
                    sendData(player, "command_result", result);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendStats(Player player) {
        Map<String, Object> stats = AxiomAPI.getPlayerStats(player);
        sendData(player, "stats", stats);
    }

    private void sendTechs(Player player) {
        String nationId = plugin.getNationManager().getNationIdOfPlayer(player.getUniqueId());
        sendData(player, "techs", AxiomAPI.getTechnologies(nationId));
    }

    private void sendNations(Player player) {
        sendData(player, "nations", AxiomAPI.getNations());
    }

    private void sendTerritoriesSnapshot(Player player) {
        long version = plugin.getTerritoryService() != null ? plugin.getTerritoryService().getVersion() : 0L;
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("version", version);
        payload.put("territories", AxiomAPI.getTerritories());
        sendData(player, "territories_snapshot", payload);
        plugin.getLogger().fine("Sent territories snapshot to " + player.getName() + " (v" + version + ")");
    }

    private void sendTerritoriesDelta(Player player, long sinceVersion) {
        var territoryService = plugin.getTerritoryService();
        if (territoryService == null) {
            sendTerritoriesSnapshot(player);
            return;
        }
        if (sinceVersion < 0) {
            sendTerritoriesSnapshot(player);
            return;
        }
        var delta = territoryService.getDeltaSince(sinceVersion);
        if (delta.requiresSnapshot()) {
            sendTerritoriesSnapshot(player);
            return;
        }
        java.util.List<Map<String, Object>> changes = new java.util.ArrayList<>();
        for (var change : delta.getChanges()) {
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("op", change.getOp());
            entry.put("world", change.getWorld());
            entry.put("x", change.getX());
            entry.put("z", change.getZ());
            entry.put("nationId", change.getNationId());
            changes.add(entry);
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("version", delta.getVersion());
        payload.put("changes", changes);
        sendData(player, "territories_delta", payload);
        if (!changes.isEmpty()) {
            plugin.getLogger().info("Sent territories delta to " + player.getName() + " (" + changes.size() + " changes, v" + delta.getVersion() + ")");
        }
    }

    private void sendLanguage(Player player) {
        String lang = plugin.getPlayerDataManager().getField(player.getUniqueId(), "uiLanguage");
        sendData(player, "language", lang == null ? "" : lang);
    }

    private void setLanguage(Player player, String lang) {
        String normalized = lang == null ? "" : lang.trim().toLowerCase();
        if (!normalized.equals("ru") && !normalized.equals("en")) {
            normalized = "ru";
        }
        plugin.getPlayerDataManager().setField(player.getUniqueId(), "uiLanguage", normalized);
        sendData(player, "language", normalized);
    }

    private void sendData(Player player, String type, Object data) {
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(msgBytes);
            out.writeUTF(type);
            out.writeUTF(gson.toJson(data));
            player.sendPluginMessage(plugin, "axiom:ui", msgBytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
