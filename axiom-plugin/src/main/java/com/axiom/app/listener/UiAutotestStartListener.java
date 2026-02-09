package com.axiom.app.listener;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UiAutotestStartListener implements Listener {
    private final AXIOM plugin;
    private boolean started = false;

    public UiAutotestStartListener(AXIOM plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (started || !plugin.isUiAutotestEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        started = true;
        int delay = plugin.getUiAutotestDelayTicks();
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.sendUiAutotestStart(player), Math.max(0, delay));
    }
}
