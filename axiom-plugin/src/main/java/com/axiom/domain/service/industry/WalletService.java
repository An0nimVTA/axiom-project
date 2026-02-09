package com.axiom.domain.service.industry;

import com.axiom.AXIOM;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.axiom.domain.service.state.PlayerDataManager;

/** Simple per-player wallet using PlayerDataManager. */
public class WalletService {
    private final AXIOM plugin;
    private final PlayerDataManager playerDataManager;

    public WalletService(AXIOM plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public synchronized double getBalance(UUID uuid) {
        if (plugin.getPlayerDataManager() == null) return 0.0;
        String bal = plugin.getPlayerDataManager().getField(uuid, "balance");
        try { return bal == null ? 0.0 : Double.parseDouble(bal); } catch (Exception e) { return 0.0; }
    }

    public synchronized void setBalance(UUID uuid, double amount) {
        if (plugin.getPlayerDataManager() == null) return;
        plugin.getPlayerDataManager().setField(uuid, "balance", String.valueOf(amount));
    }

    public synchronized void deposit(UUID uuid, double amount) { 
        setBalance(uuid, getBalance(uuid) + Math.max(0, amount)); 
        
        // VISUAL EFFECTS: Notify player of large deposits (>1000)
        if (amount >= 1000 && plugin.getVisualEffectsService() != null) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    plugin.getVisualEffectsService().sendActionBar(p, 
                        String.format("§a💰 Получено: §f%.2f §7(Баланс: §f%.2f§7)", amount, getBalance(uuid)));
                    // Green particles
                    org.bukkit.Location loc = p.getLocation();
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                    }
                }
            });
        }
    }
    
    public synchronized boolean withdraw(UUID uuid, double amount) {
        double b = getBalance(uuid);
        if (amount <= 0 || b < amount) return false;
        setBalance(uuid, b - amount);
        
        // VISUAL EFFECTS: Notify player of large withdrawals (>1000)
        if (amount >= 1000 && plugin.getVisualEffectsService() != null) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    plugin.getVisualEffectsService().sendActionBar(p, 
                        String.format("§c💰 Списано: §f%.2f §7(Баланс: §f%.2f§7)", amount, getBalance(uuid)));
                    // Red particles
                    org.bukkit.Location loc = p.getLocation();
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                    }
                }
            });
        }
        
        return true;
    }
    
    public synchronized boolean transfer(UUID from, UUID to, double amount) {
        if (withdraw(from, amount)) {
            deposit(to, amount);
            
            // VISUAL EFFECTS: Notify both players of large transfers (>500)
            if (amount >= 500 && plugin.getVisualEffectsService() != null) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    org.bukkit.entity.Player fromP = org.bukkit.Bukkit.getPlayer(from);
                    org.bukkit.entity.Player toP = org.bukkit.Bukkit.getPlayer(to);
                    
                    String toName = toP != null ? toP.getName() : "Игрок";
                    String fromName = fromP != null ? fromP.getName() : "Игрок";
                    
                    if (fromP != null && fromP.isOnline()) {
                        plugin.getVisualEffectsService().sendActionBar(fromP, 
                            String.format("§e📤 Переведено §f%.2f §7→ %s", amount, toName));
                    }
                    if (toP != null && toP.isOnline()) {
                        plugin.getVisualEffectsService().sendActionBar(toP, 
                            String.format("§e📥 Получено §f%.2f §7от %s", amount, fromName));
                        // Gold particles for receiver
                        org.bukkit.Location loc = toP.getLocation();
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.05);
                        }
                        toP.playSound(loc, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                    }
                });
            }
            
            return true;
        }
        return false;
    }
    
    public void openWalletMenu(org.bukkit.entity.Player player) {
        // Placeholder for wallet GUI
        player.sendMessage("Wallet menu opening...");
    }

    /**
     * Get comprehensive wallet statistics.
     */
    public synchronized Map<String, Object> getWalletStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        // Calculate total balance across all players
        double totalBalance = 0.0;
        int playersWithBalance = 0;
        double maxBalance = 0.0;
        double minBalance = Double.MAX_VALUE;
        
        File playersDir = new File(plugin.getDataFolder(), "players");
        if (playersDir.exists()) {
            File[] files = playersDir.listFiles((d, n) -> n.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    try {
                        UUID uuid = UUID.fromString(f.getName().replace(".json", ""));
                        double balance = getBalance(uuid);
                        if (balance > 0) {
                            playersWithBalance++;
                            totalBalance += balance;
                            maxBalance = Math.max(maxBalance, balance);
                            minBalance = Math.min(minBalance, balance);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        
        stats.put("totalBalance", totalBalance);
        stats.put("playersWithBalance", playersWithBalance);
        stats.put("maxBalance", maxBalance == 0 ? 0 : maxBalance);
        stats.put("minBalance", minBalance == Double.MAX_VALUE ? 0 : minBalance);
        stats.put("averageBalance", playersWithBalance > 0 ? totalBalance / playersWithBalance : 0);
        
        return stats;
    }
    
    /**
     * Get wallet statistics for specific player.
     */
    public synchronized Map<String, Object> getPlayerWalletStatistics(UUID uuid) {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        double balance = getBalance(uuid);
        stats.put("balance", balance);
        stats.put("hasBalance", balance > 0);
        
        // Player info
        org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        stats.put("playerName", player.getName());
        stats.put("isOnline", player.isOnline());
        
        // Nation info (if applicable)
        if (plugin.getPlayerDataManager() == null) return stats;
        String nationId = plugin.getPlayerDataManager().getNation(uuid);
        if (nationId != null) {
            if (plugin.getNationManager() == null) return stats;
            com.axiom.domain.model.Nation nation = plugin.getNationManager().getNationById(nationId);
            if (nation != null) {
                stats.put("nationId", nationId);
                stats.put("nationName", nation.getName());
                stats.put("currencyCode", nation.getCurrencyCode());
            }
        }
        
        return stats;
    }
    
    /**
     * Check if player has sufficient balance.
     */
    public synchronized boolean hasBalance(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }
    
    /**
     * Get total balance for a nation (sum of all citizens).
     */
    public synchronized double getNationTotalBalance(String nationId) {
        if (plugin.getNationManager() == null) return 0.0;
        com.axiom.domain.model.Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return 0.0;
        
        double total = 0.0;
        for (UUID citizenId : nation.getCitizens()) {
            total += getBalance(citizenId);
        }
        return total;
    }
    
    /**
     * Get global wallet statistics across all players and nations.
     */
    public synchronized Map<String, Object> getGlobalWalletStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalBalance = 0.0;
        int totalPlayers = 0;
        int playersWithBalance = 0;
        double maxBalance = 0.0;
        double minBalance = Double.MAX_VALUE;
        Map<String, Double> balanceByNation = new HashMap<>();
        Map<String, Integer> playersByNation = new HashMap<>();
        
        File playersDir = new File(plugin.getDataFolder(), "players");
        if (playersDir.exists()) {
            File[] files = playersDir.listFiles((d, n) -> n.endsWith(".json"));
            if (files != null) {
                totalPlayers = files.length;
                for (File f : files) {
                    try {
                        UUID uuid = UUID.fromString(f.getName().replace(".json", ""));
                        double balance = getBalance(uuid);
                        totalBalance += balance;
                        
                        if (balance > 0) {
                            playersWithBalance++;
                            maxBalance = Math.max(maxBalance, balance);
                            minBalance = Math.min(minBalance, balance);
                        }
                        
                        // Group by nation
                        if (plugin.getPlayerDataManager() != null) {
                            String nationId = plugin.getPlayerDataManager().getNation(uuid);
                            if (nationId != null) {
                                balanceByNation.put(nationId, balanceByNation.getOrDefault(nationId, 0.0) + balance);
                                playersByNation.put(nationId, playersByNation.getOrDefault(nationId, 0) + 1);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        
        stats.put("totalBalance", totalBalance);
        stats.put("totalPlayers", totalPlayers);
        stats.put("playersWithBalance", playersWithBalance);
        stats.put("playersWithoutBalance", totalPlayers - playersWithBalance);
        stats.put("maxBalance", maxBalance == 0 ? 0 : maxBalance);
        stats.put("minBalance", minBalance == Double.MAX_VALUE ? 0 : minBalance);
        stats.put("averageBalance", totalPlayers > 0 ? totalBalance / totalPlayers : 0);
        stats.put("averageBalanceActive", playersWithBalance > 0 ? totalBalance / playersWithBalance : 0);
        stats.put("balanceByNation", balanceByNation);
        stats.put("playersByNation", playersByNation);
        stats.put("nationsWithPlayers", balanceByNation.size());
        
        // Top nations by total balance
        List<Map.Entry<String, Double>> topByBalance = balanceByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByBalance", topByBalance);
        
        // Top nations by average balance per player
        Map<String, Double> avgBalanceByNation = new HashMap<>();
        for (String nationId : balanceByNation.keySet()) {
            double total = balanceByNation.get(nationId);
            int players = playersByNation.get(nationId);
            avgBalanceByNation.put(nationId, players > 0 ? total / players : 0);
        }
        List<Map.Entry<String, Double>> topByAvgBalance = avgBalanceByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByAvgBalance", topByAvgBalance);
        
        // Balance distribution
        int veryRich = 0, rich = 0, moderate = 0, poor = 0;
        for (Double balance : balanceByNation.values()) {
            if (balance >= 100000) veryRich++;
            else if (balance >= 50000) rich++;
            else if (balance >= 10000) moderate++;
            else poor++;
        }
        
        Map<String, Integer> balanceDistribution = new HashMap<>();
        balanceDistribution.put("veryRich", veryRich);
        balanceDistribution.put("rich", rich);
        balanceDistribution.put("moderate", moderate);
        balanceDistribution.put("poor", poor);
        stats.put("balanceDistribution", balanceDistribution);
        
        return stats;
    }
}


