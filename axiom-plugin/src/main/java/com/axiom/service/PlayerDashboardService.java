package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Beautiful player dashboard showing stats, balance, role, war status, etc.
 * Uses Scoreboard for persistent side display.
 */
public class PlayerDashboardService {
    private final AXIOM plugin;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    
    public PlayerDashboardService(AXIOM plugin) {
        this.plugin = plugin;
        
        // Update dashboards every 3 seconds
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAllDashboards, 
            20 * 3, 20 * 3);
    }
    
    /**
     * Initialize dashboard for player.
     */
    public void initializeDashboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("axiom_dash", 
            Criteria.DUMMY, "§b§l⚡ AXIOM ⚡");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
        updateDashboard(player);
    }
    
    /**
     * Update dashboard for specific player.
     */
    public void updateDashboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            initializeDashboard(player);
            scoreboard = playerScoreboards.get(player.getUniqueId());
        }
        
        Objective objective = scoreboard.getObjective("axiom_dash");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("axiom_dash", 
                Criteria.DUMMY, "§b§l⚡ AXIOM ⚡");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        
        // Clear existing scores
        scoreboard.getEntries().forEach(scoreboard::resetScores);
        
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        NationManager nationManager = plugin.getNationManager();
        String nationId = playerDataManager != null ? playerDataManager.getNation(player.getUniqueId()) : null;
        Nation nation = nationId != null && nationManager != null ? nationManager.getNationById(nationId) : null;
        
        int lineNumber = 15;
        
        // Beautiful header with gradient
        addLine(scoreboard, objective, "§7§m━━━━━━━━━━━━━━━━━━━", lineNumber--);
        addLine(scoreboard, objective, "§b§l⚡ AXIOM §7§lСТАТИСТИКА", lineNumber--);
        addLine(scoreboard, objective, "§7§m━━━━━━━━━━━━━━━━━━━", lineNumber--);
        addLine(scoreboard, objective, " ", lineNumber--);
        
        // Player info
        if (nation != null) {
            Nation.Role role = nation.getRole(player.getUniqueId());
            String roleDisplay = getRoleDisplay(role);
            
            // Nation & Role
            addLine(scoreboard, objective, "§f┌─ §b§lНАЦИЯ", lineNumber--);
            addLine(scoreboard, objective, "§f│ Нация: §b" + truncate(nation.getName(), 18), lineNumber--);
            addLine(scoreboard, objective, "§f│ Роль: " + roleDisplay, lineNumber--);
            addLine(scoreboard, objective, " ", lineNumber--);
            
            // Balance & Treasury
            WalletService walletService = plugin.getWalletService();
            double balance = walletService != null ? walletService.getBalance(player.getUniqueId()) : 0.0;
            addLine(scoreboard, objective, "§f┌─ §a§lЭКОНОМИКА", lineNumber--);
            addLine(scoreboard, objective, "§f│ §e💰 Баланс:", lineNumber--);
            addLine(scoreboard, objective, "§f│  §a" + formatLargeNumber(balance) + " " + truncate(nation.getCurrencyCode(), 4), lineNumber--);
            addLine(scoreboard, objective, "§f│ §b🏛️ Казна:", lineNumber--);
            addLine(scoreboard, objective, "§f│  §b" + formatLargeNumber(nation.getTreasury()) + " " + truncate(nation.getCurrencyCode(), 4), lineNumber--);
            
            double inflation = nation.getInflation();
            String inflationColor = inflation > 20 ? "§c" : inflation > 10 ? "§e" : "§a";
            addLine(scoreboard, objective, "§f│ §6📈 Инфляция: " + inflationColor + String.format("%.1f", inflation) + "%", lineNumber--);
            addLine(scoreboard, objective, " ", lineNumber--);
            
            // War status
            boolean atWar = isNationAtWar(nation);
            addLine(scoreboard, objective, "§f┌─ " + (atWar ? "§c§l⚔️ ВОЙНА" : "§a§l☮ МИР"), lineNumber--);
            if (atWar) {
                int warCount = getActiveWarsCount(nation);
                addLine(scoreboard, objective, "§f│ §c⚠ Активных войн: §f" + warCount, lineNumber--);
            } else {
                addLine(scoreboard, objective, "§f│ §a✓ Нация в мире", lineNumber--);
            }
            addLine(scoreboard, objective, " ", lineNumber--);
            
            // Population & Territory
            addLine(scoreboard, objective, "§f┌─ §d§lНАСЕЛЕНИЕ", lineNumber--);
            addLine(scoreboard, objective, "§f│ §d👥 Граждане: §b" + nation.getCitizens().size(), lineNumber--);
            int territories = nation.getClaimedChunkKeys() != null ? nation.getClaimedChunkKeys().size() : 0;
            addLine(scoreboard, objective, "§f│ §b🗺️ Территории: §b" + territories, lineNumber--);
            addLine(scoreboard, objective, " ", lineNumber--);
            
            // Stats
            addLine(scoreboard, objective, "§f┌─ §e§lПОКАЗАТЕЛИ", lineNumber--);
            HappinessService happinessService = plugin.getHappinessService();
            double happiness = happinessService != null ? happinessService.getNationHappiness(nationId) : 50.0;
            String happinessColor = getHappinessColor(happiness);
            String happinessIcon = happiness >= 80 ? "😊" : happiness >= 60 ? "🙂" : happiness >= 40 ? "😐" : "😢";
            addLine(scoreboard, objective, "§f│ " + happinessIcon + " Счастье: " + happinessColor + String.format("%.1f", happiness) + "%", lineNumber--);
            
            EducationService educationService = plugin.getEducationService();
            double education = educationService != null ? educationService.getEducationLevel(nationId) : 0.0;
            addLine(scoreboard, objective, "§f│ §b📚 Образование: §b" + String.format("%.1f", education), lineNumber--);
            
            TechnologyTreeService techService = plugin.getTechnologyTreeService();
            int totalTechs = techService != null ? techService.getAllTechs().size() : 0;
            long unlockedTechs = techService != null ? techService.getUnlockedTechs(nationId).size() : 0;
            addLine(scoreboard, objective, "§f│ §e🔬 Технологий: §b" + unlockedTechs + "/" + totalTechs, lineNumber--);
            
            // Mod integration status (if any mods detected)
            ModIntegrationService modIntegrationService = plugin.getModIntegrationService();
            Set<String> detectedMods = modIntegrationService != null ? modIntegrationService.getDetectedMods() : java.util.Collections.emptySet();
            if (!detectedMods.isEmpty()) {
                ModWarfareService modWarfareService = plugin.getModWarfareService();
                int warfareMods = modWarfareService != null ? modWarfareService.getAvailableWarfareModsCount() : 0;
                boolean hasIndustrial = modIntegrationService.hasIndustrialMods();
                boolean hasEnergy = modIntegrationService.hasEnergyMods();
                
                StringBuilder modInfo = new StringBuilder("§f│ §d🔧 Моды: §7");
                if (warfareMods > 0) modInfo.append("⚔").append(warfareMods);
                if (hasIndustrial) modInfo.append("🏭");
                if (hasEnergy) modInfo.append("⚡");
                if (modInfo.length() > 15) { // Only show if there are mods
                    addLine(scoreboard, objective, modInfo.toString(), lineNumber--);
                }
            }
            
            addLine(scoreboard, objective, " ", lineNumber--);
            
            // Diplomacy
            addLine(scoreboard, objective, "§f┌─ §6§lДИПЛОМАТИЯ", lineNumber--);
            int alliesCount = nation.getAllies() != null ? nation.getAllies().size() : 0;
            int enemiesCount = nation.getEnemies() != null ? nation.getEnemies().size() : 0;
            addLine(scoreboard, objective, "§f│ §a🤝 Союзников: §f" + alliesCount, lineNumber--);
            addLine(scoreboard, objective, "§f│ §c⚔️ Врагов: §f" + enemiesCount, lineNumber--);
            
            // Reputation (if any)
            Map<String, Integer> reputation = nation.getReputation();
            if (reputation != null && !reputation.isEmpty()) {
                int avgRep = (int) reputation.values().stream()
                    .mapToInt(Integer::intValue).average().orElse(0);
                String repColor = avgRep >= 50 ? "§a" : avgRep >= 0 ? "§e" : "§c";
                addLine(scoreboard, objective, "§f│ §6⭐ Репутация: " + repColor + avgRep, lineNumber--);
            }
            
        } else {
            // No nation - beautiful empty state
            addLine(scoreboard, objective, "§f┌─ §c§lСТАТУС", lineNumber--);
            addLine(scoreboard, objective, "§f│ §c✗ Не в нации", lineNumber--);
            addLine(scoreboard, objective, " ", lineNumber--);
            
            WalletService walletService = plugin.getWalletService();
            double balance = walletService != null ? walletService.getBalance(player.getUniqueId()) : 0.0;
            addLine(scoreboard, objective, "§f┌─ §a§lБАЛАНС", lineNumber--);
            addLine(scoreboard, objective, "§f│ §e💰 Баланс:", lineNumber--);
            addLine(scoreboard, objective, "§f│  §a" + formatLargeNumber(balance) + " AXC", lineNumber--);
            addLine(scoreboard, objective, " ", lineNumber--);
            
            addLine(scoreboard, objective, "§f┌─ §b§lПОМОЩЬ", lineNumber--);
            addLine(scoreboard, objective, "§f│ §eИспользуйте:", lineNumber--);
            addLine(scoreboard, objective, "§f│  §b/nation §7для создания", lineNumber--);
            addLine(scoreboard, objective, "§f│  §b/axiom tutorial", lineNumber--);
        }
        
        // Beautiful footer
        addLine(scoreboard, objective, " ", lineNumber--);
        addLine(scoreboard, objective, "§7§m━━━━━━━━━━━━━━━━━━━", lineNumber--);
        addLine(scoreboard, objective, "§b§lAXIOM §7v1.0.0", lineNumber--);
    }
    
    private void addLine(Scoreboard scoreboard, Objective objective, String text, int score) {
        if (score < 0) return;
        
        // Handle duplicate text by appending invisible ChatColor codes
        String uniqueText = text;
        int attempts = 0;
        while (scoreboard.getEntries().contains(uniqueText) && attempts < 15) {
            // Use color codes to create unique entries
            String suffix = "";
            for (int i = 0; i < attempts; i++) {
                suffix += ChatColor.values()[(i % 16)].toString();
            }
            uniqueText = text + ChatColor.RESET + suffix;
            attempts++;
        }
        
        Score scoreObj = objective.getScore(uniqueText);
        scoreObj.setScore(score);
    }
    
    private String getRoleDisplay(Nation.Role role) {
        if (role == null) return "§7Нет";
        switch (role) {
            case LEADER: return "§6§lКОРОЛЬ";
            case MINISTER: return "§dМИНИСТР";
            case GENERAL: return "§cГЕНЕРАЛ";
            case GOVERNOR: return "§bГУБЕРНАТОР";
            case CITIZEN: return "§aГРАЖДАНИН";
            default: return "§7Нет";
        }
    }
    
    private boolean isNationAtWar(Nation nation) {
        if (nation.getEnemies() == null || nation.getEnemies().isEmpty()) return false;
        DiplomacySystem diplomacySystem = plugin.getDiplomacySystem();
        if (diplomacySystem == null) return false;
        
        for (String enemyId : nation.getEnemies()) {
            if (diplomacySystem.isAtWar(nation.getId(), enemyId)) {
                return true;
            }
        }
        return false;
    }
    
    private int getActiveWarsCount(Nation nation) {
        DiplomacySystem diplomacySystem = plugin.getDiplomacySystem();
        if (diplomacySystem == null) return 0;
        int count = 0;
        if (nation.getEnemies() == null) return 0;
        for (String enemyId : nation.getEnemies()) {
            if (diplomacySystem.isAtWar(nation.getId(), enemyId)) {
                count++;
            }
        }
        return count;
    }
    
    private String getHappinessColor(double happiness) {
        if (happiness >= 80) return "§a";
        if (happiness >= 60) return "§e";
        if (happiness >= 40) return "§6";
        return "§c";
    }
    
    /**
     * Update all player dashboards.
     */
    private void updateAllDashboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateDashboard(player);
        }
    }
    
    /**
     * Remove dashboard when player leaves.
     */
    public void removeDashboard(UUID playerId) {
        playerScoreboards.remove(playerId);
    }
    
    /**
     * Force update for specific player.
     */
    public void forceUpdate(Player player) {
        updateDashboard(player);
    }
    
    /**
     * Toggle dashboard visibility.
     */
    public void toggleDashboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard != null && player.getScoreboard().equals(scoreboard)) {
            // Hide dashboard
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            player.sendMessage("§7Dashboard скрыт. Используйте §b/axiom dashboard §7для показа.");
        } else {
            // Show dashboard
            initializeDashboard(player);
            player.sendMessage("§aDashboard показан!");
        }
    }
    
    /**
     * Format large numbers with suffixes (K, M, B).
     */
    private String formatLargeNumber(double number) {
        if (number >= 1_000_000_000) {
            return String.format("%.2fB", number / 1_000_000_000);
        } else if (number >= 1_000_000) {
            return String.format("%.2fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.2fK", number / 1_000);
        } else {
            return String.format("%.2f", number);
        }
    }
    
    /**
     * Truncate string to max length.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Get comprehensive dashboard statistics.
     */
    public synchronized Map<String, Object> getDashboardStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        boolean hasDashboard = playerScoreboards.containsKey(playerId);
        stats.put("hasDashboard", hasDashboard);
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            boolean isVisible = player.getScoreboard().equals(playerScoreboards.get(playerId));
            stats.put("isVisible", isVisible);
        }
        
        // Dashboard update info
        stats.put("updateIntervalSeconds", 3);
        
        // Player nation info
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        String nationId = playerDataManager != null ? playerDataManager.getNation(playerId) : null;
        if (nationId != null) {
            NationManager nationManager = plugin.getNationManager();
            Nation nation = nationManager != null ? nationManager.getNationById(nationId) : null;
            if (nation != null) {
                stats.put("nationId", nationId);
                stats.put("nationName", nation.getName());
                stats.put("playerRole", nation.getRole(playerId) != null ? nation.getRole(playerId).name() : "NONE");
            }
        }
        
        return stats;
    }
    
    /**
     * Get global dashboard statistics.
     */
    public synchronized Map<String, Object> getGlobalDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalDashboards", playerScoreboards.size());
        stats.put("activePlayers", Bukkit.getOnlinePlayers().size());
        
        // Count visible dashboards
        int visibleDashboards = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
            if (scoreboard != null && player.getScoreboard().equals(scoreboard)) {
                visibleDashboards++;
            }
        }
        stats.put("visibleDashboards", visibleDashboards);
        
        // Update frequency
        stats.put("updateIntervalSeconds", 3);
        stats.put("updateIntervalTicks", 60);
        
        return stats;
    }
    
    /**
     * Refresh all dashboards immediately.
     */
    public void refreshAll() {
        updateAllDashboards();
    }
    
    /**
     * Check if player has dashboard.
     */
    public synchronized boolean hasDashboard(UUID playerId) {
        return playerScoreboards.containsKey(playerId);
    }
    
    /**
     * Check if dashboard is visible for player.
     */
    public synchronized boolean isVisible(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        
        Scoreboard scoreboard = playerScoreboards.get(playerId);
        return scoreboard != null && player.getScoreboard().equals(scoreboard);
    }
}

