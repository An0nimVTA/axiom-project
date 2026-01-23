package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/** Manages nation and diplomatic chat channels. */
public class ChatService {
    private final AXIOM plugin;
    private final Map<UUID, String> playerChannels = new HashMap<>(); // player -> channel
    private final Map<UUID, Boolean> channelEnabled = new HashMap<>(); // player -> enabled

    public ChatService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized void setChannel(UUID playerId, String channel) {
        playerChannels.put(playerId, channel);
    }

    public synchronized void toggleChannel(UUID playerId) {
        channelEnabled.put(playerId, !channelEnabled.getOrDefault(playerId, true));
    }

    public synchronized boolean sendNationChat(UUID senderId, String message) {
        String nationId = plugin.getPlayerDataManager().getNation(senderId);
        if (nationId == null) return false;
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null) return false;
        String format = "§b[N] §f" + sender.getName() + ": §7" + message;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String pNation = plugin.getPlayerDataManager().getNation(p.getUniqueId());
            if (pNation != null && pNation.equals(nationId)) {
                p.sendMessage(format);
            }
        }
        return true;
    }

    public synchronized boolean sendDiplomaticChat(UUID senderId, String message) {
        String nationId = plugin.getPlayerDataManager().getNation(senderId);
        if (nationId == null) return false;
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null) return false;
        com.axiom.model.Nation senderNation = plugin.getNationManager().getNationById(nationId);
        if (senderNation == null) return false;
        String format = "§b[D] §f" + senderNation.getName() + " (" + sender.getName() + "): §7" + message;
        Set<String> recipients = new HashSet<>(senderNation.getAllies());
        recipients.addAll(senderNation.getPendingAlliance().stream()
            .map(s -> s.replace("in:", "").replace("out:", "")).toList());
        for (Player p : Bukkit.getOnlinePlayers()) {
            String pNation = plugin.getPlayerDataManager().getNation(p.getUniqueId());
            if (pNation != null && recipients.contains(pNation)) {
                p.sendMessage(format);
            }
        }
        return true;
    }

    public synchronized String getChannel(UUID playerId) {
        return playerChannels.getOrDefault(playerId, "global");
    }
    
    /**
     * Get comprehensive chat statistics.
     */
    public synchronized Map<String, Object> getChatStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        String channel = getChannel(playerId);
        boolean enabled = channelEnabled.getOrDefault(playerId, true);
        
        stats.put("currentChannel", channel);
        stats.put("channelEnabled", enabled);
        
        // Count players in same channel
        int sameChannelCount = 0;
        String nationId = plugin.getPlayerDataManager().getNation(playerId);
        if (channel.equals("nation") && nationId != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String pNation = plugin.getPlayerDataManager().getNation(p.getUniqueId());
                if (pNation != null && pNation.equals(nationId)) {
                    sameChannelCount++;
                }
            }
        } else if (channel.equals("diplomatic") && nationId != null) {
            com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
            if (n != null) {
                Set<String> allies = new HashSet<>(n.getAllies());
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pNation = plugin.getPlayerDataManager().getNation(p.getUniqueId());
                    if (pNation != null && allies.contains(pNation)) {
                        sameChannelCount++;
                    }
                }
            }
        }
        stats.put("sameChannelPlayers", sameChannelCount);
        
        // Available channels
        List<String> availableChannels = new ArrayList<>();
        availableChannels.add("global");
        if (nationId != null) {
            availableChannels.add("nation");
            com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
            if (n != null && !n.getAllies().isEmpty()) {
                availableChannels.add("diplomatic");
            }
        }
        stats.put("availableChannels", availableChannels);
        
        // Chat rating
        String rating = "ГЛОБАЛЬНЫЙ";
        if (channel.equals("nation")) rating = "НАЦИОНАЛЬНЫЙ";
        else if (channel.equals("diplomatic")) rating = "ДИПЛОМАТИЧЕСКИЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global chat statistics.
     */
    public synchronized Map<String, Object> getGlobalChatStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Integer> channelUsage = new HashMap<>();
        int enabledCount = 0;
        int disabledCount = 0;
        
        for (Map.Entry<UUID, String> entry : playerChannels.entrySet()) {
            channelUsage.put(entry.getValue(), channelUsage.getOrDefault(entry.getValue(), 0) + 1);
        }
        
        for (Boolean enabled : channelEnabled.values()) {
            if (enabled) enabledCount++;
            else disabledCount++;
        }
        
        stats.put("totalPlayersWithChannels", playerChannels.size());
        stats.put("channelUsage", channelUsage);
        stats.put("enabledCount", enabledCount);
        stats.put("disabledCount", disabledCount);
        stats.put("totalChannels", playerChannels.values().stream().distinct().count());
        
        return stats;
    }
    
    /**
     * Get nation chat participants.
     */
    public synchronized List<UUID> getNationChatParticipants(String nationId) {
        List<UUID> participants = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String pNation = plugin.getPlayerDataManager().getNation(p.getUniqueId());
            if (pNation != null && pNation.equals(nationId)) {
                String channel = getChannel(p.getUniqueId());
                if (channel.equals("nation") && channelEnabled.getOrDefault(p.getUniqueId(), true)) {
                    participants.add(p.getUniqueId());
                }
            }
        }
        return participants;
    }
    
    /**
     * Clear player channel.
     */
    public synchronized void clearChannel(UUID playerId) {
        playerChannels.remove(playerId);
        channelEnabled.remove(playerId);
    }
}

