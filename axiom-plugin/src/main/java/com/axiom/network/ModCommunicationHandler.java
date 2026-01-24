package com.axiom.network;

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

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String action = in.readUTF();

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
                case "execute_command":
                    String cmd = in.readUTF();
                    AxiomAPI.executeCommand(player, cmd);
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
