package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges plugin commands to the client UI mod via plugin messages.
 */
public class ClientUiService implements PluginMessageListener, Listener {
    public static final String UI_CHANNEL = "axiom:ui";

    private final AXIOM plugin;
    private final Set<UUID> uiCapablePlayers = ConcurrentHashMap.newKeySet();

    public ClientUiService(AXIOM plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, UI_CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, UI_CHANNEL);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean openReligionUi(Player player) {
        if (!uiCapablePlayers.contains(player.getUniqueId())) {
            return false;
        }
        sendAction(player, "open_religions");
        return true;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!UI_CHANNEL.equals(channel) || player == null || message == null) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String action = in.readUTF();
            if ("hello".equalsIgnoreCase(action)) {
                uiCapablePlayers.add(player.getUniqueId());
            }
        } catch (Exception ignored) {
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        uiCapablePlayers.remove(event.getPlayer().getUniqueId());
    }

    private void sendAction(Player player, String action) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeUTF(action);
            player.sendPluginMessage(plugin, UI_CHANNEL, out.toByteArray());
        } catch (Exception ignored) {
        }
    }
}
