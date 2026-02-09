package com.axiom.domain.service.military;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.entity.Player;

import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages conscription and draft systems during wartime. */
public class ConscriptionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, Set<UUID>> conscriptedPlayers = new HashMap<>(); // nationId -> conscripts

    public ConscriptionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public synchronized String conscriptPlayer(String nationId, UUID playerId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (nationId == null || playerId == null) return "Неверные параметры.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (!n.isMember(playerId)) return "Игрок не в нации.";
        Set<UUID> conscripts = conscriptedPlayers.computeIfAbsent(nationId, k -> new HashSet<>());
        if (conscripts.contains(playerId)) return "Игрок уже призван.";
        conscripts.add(playerId);
        Player p = plugin.getServer().getPlayer(playerId);
        if (p != null && p.isOnline()) {
            p.sendMessage("§cВы призваны на службу! Мобилизация активна.");
        }
        return "Игрок призван на службу.";
    }

    public synchronized String releaseConscript(String nationId, UUID playerId) {
        if (nationId == null || playerId == null) return "Неверные параметры.";
        Set<UUID> conscripts = conscriptedPlayers.get(nationId);
        if (conscripts == null || !conscripts.contains(playerId)) return "Игрок не призван.";
        conscripts.remove(playerId);
        Player p = plugin.getServer().getPlayer(playerId);
        if (p != null && p.isOnline()) {
            p.sendMessage("§aВы освобождены от службы.");
        }
        return "Игрок освобождён от службы.";
    }

    public synchronized boolean isConscripted(UUID playerId) {
        for (Set<UUID> conscripts : conscriptedPlayers.values()) {
            if (conscripts.contains(playerId)) return true;
        }
        return false;
    }

    public synchronized int getConscriptCount(String nationId) {
        Set<UUID> conscripts = conscriptedPlayers.get(nationId);
        return conscripts != null ? conscripts.size() : 0;
    }
    
    /**
     * Get comprehensive conscription statistics.
     */
    public synchronized Map<String, Object> getConscriptionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        Nation n = nationManager.getNationById(nationId);
        if (n == null) {
            stats.put("error", "Нация не найдена.");
            return stats;
        }
        
        Set<UUID> conscripts = conscriptedPlayers.getOrDefault(nationId, Collections.emptySet());
        int totalCitizens = n.getCitizens() != null ? n.getCitizens().size() : 0;
        double conscriptionRate = totalCitizens > 0 ? ((double) conscripts.size() / totalCitizens) * 100 : 0;
        
        stats.put("conscriptedCount", conscripts.size());
        stats.put("totalCitizens", totalCitizens);
        stats.put("conscriptionRate", conscriptionRate);
        
        // Conscript details
        List<Map<String, Object>> conscriptsList = new ArrayList<>();
        for (UUID playerId : conscripts) {
            Map<String, Object> conscriptData = new HashMap<>();
            conscriptData.put("playerId", playerId.toString());
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(playerId);
            conscriptData.put("playerName", player.getName());
            conscriptData.put("isOnline", player.isOnline());
            conscriptsList.add(conscriptData);
        }
        stats.put("conscriptsList", conscriptsList);
        
        // Check if nation is at war
        boolean isAtWar = false;
        try {
            if (plugin.getAdvancedWarSystem() != null) {
                List<?> wars = plugin.getAdvancedWarSystem().getNationWars(nationId);
                isAtWar = wars != null && !wars.isEmpty();
            }
        } catch (Exception ignored) {}
        stats.put("isAtWar", isAtWar);
        
        // Conscription rating
        String rating = "НЕТ ПРИЗЫВА";
        if (conscriptionRate >= 50) rating = "ТОТАЛЬНАЯ МОБИЛИЗАЦИЯ";
        else if (conscriptionRate >= 30) rating = "МАССОВАЯ МОБИЛИЗАЦИЯ";
        else if (conscriptionRate >= 20) rating = "АКТИВНАЯ МОБИЛИЗАЦИЯ";
        else if (conscriptionRate >= 10) rating = "ЧАСТИЧНАЯ МОБИЛИЗАЦИЯ";
        else if (conscripts.size() >= 1) rating = "НАЧАЛЬНАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Bulk conscript players.
     */
    public synchronized String bulkConscript(String nationId, int count) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (count <= 0) return "Количество должно быть больше 0.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        
        Set<UUID> conscripts = conscriptedPlayers.computeIfAbsent(nationId, k -> new HashSet<>());
        List<UUID> candidates = n.getCitizens() != null ? new ArrayList<>(n.getCitizens()) : new ArrayList<>();
        candidates.removeAll(conscripts);
        
        int conscripted = 0;
        for (int i = 0; i < Math.min(count, candidates.size()); i++) {
            UUID playerId = candidates.get(i);
            conscripts.add(playerId);
            conscripted++;
            
            Player p = plugin.getServer().getPlayer(playerId);
            if (p != null && p.isOnline()) {
                p.sendMessage("§cВы призваны на службу! Мобилизация активна.");
            }
        }
        
        return "Призвано на службу: " + conscripted + " игроков.";
    }
    
    /**
     * Get all conscripts for nation.
     */
    public synchronized Set<UUID> getNationConscripts(String nationId) {
        return new HashSet<>(conscriptedPlayers.getOrDefault(nationId, Collections.emptySet()));
    }
    
    /**
     * Calculate military effectiveness from conscription.
     */
    public synchronized double getMilitaryEffectiveness(String nationId) {
        int conscripts = getConscriptCount(nationId);
        if (nationManager == null) return 1.0;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 1.0;
        
        int totalCitizens = n.getCitizens() != null ? n.getCitizens().size() : 0;
        if (totalCitizens == 0) return 1.0;
        
        // +1% effectiveness per 5% conscription rate (capped at +20%)
        double conscriptionRate = ((double) conscripts / totalCitizens) * 100;
        return 1.0 + Math.min(0.20, (conscriptionRate / 5.0) * 0.01);
    }
    
    /**
     * Get global conscription statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalConscriptionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        int totalConscripts = 0;
        Map<String, Integer> conscriptsByNation = new HashMap<>();
        Map<String, Double> conscriptionRateByNation = new HashMap<>();
        Map<String, Boolean> warStatusByNation = new HashMap<>();
        int nationsAtWar = 0;
        int nationsWithConscripts = 0;
        
        for (Nation n : nationManager.getAll()) {
            String nationId = n.getId();
            int conscripts = getConscriptCount(nationId);
            int totalCitizens = n.getCitizens() != null ? n.getCitizens().size() : 0;
            
            totalConscripts += conscripts;
            conscriptsByNation.put(nationId, conscripts);
            
            if (totalCitizens > 0) {
                double rate = ((double) conscripts / totalCitizens) * 100;
                conscriptionRateByNation.put(nationId, rate);
            }
            
            // Check if at war
            boolean atWar = false;
            if (plugin.getDiplomacySystem() != null) {
                for (Nation other : nationManager.getAll()) {
                    if (!other.getId().equals(nationId)) {
                        if (plugin.getDiplomacySystem().isAtWar(nationId, other.getId())) {
                            atWar = true;
                            break;
                        }
                    }
                }
            }
            warStatusByNation.put(nationId, atWar);
            if (atWar) nationsAtWar++;
            if (conscripts > 0) nationsWithConscripts++;
        }
        
        stats.put("totalConscripts", totalConscripts);
        stats.put("conscriptsByNation", conscriptsByNation);
        stats.put("conscriptionRateByNation", conscriptionRateByNation);
        stats.put("warStatusByNation", warStatusByNation);
        stats.put("nationsAtWar", nationsAtWar);
        stats.put("nationsWithConscripts", nationsWithConscripts);
        
        // Average conscription rate
        double totalRate = conscriptionRateByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("averageConscriptionRate", conscriptionRateByNation.size() > 0 ? 
            totalRate / conscriptionRateByNation.size() : 0);
        
        // Top nations by conscripts
        List<Map.Entry<String, Integer>> topByConscripts = conscriptsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByConscripts", topByConscripts);
        
        // Top nations by conscription rate
        List<Map.Entry<String, Double>> topByRate = conscriptionRateByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRate", topByRate);
        
        // Conscription rate distribution
        int total = 0, mass = 0, active = 0, partial = 0, minimal = 0;
        for (Double rate : conscriptionRateByNation.values()) {
            if (rate >= 50) total++;
            else if (rate >= 30) mass++;
            else if (rate >= 20) active++;
            else if (rate >= 10) partial++;
            else minimal++;
        }
        
        Map<String, Integer> rateDistribution = new HashMap<>();
        rateDistribution.put("total", total);
        rateDistribution.put("mass", mass);
        rateDistribution.put("active", active);
        rateDistribution.put("partial", partial);
        rateDistribution.put("minimal", minimal);
        stats.put("rateDistribution", rateDistribution);
        
        // Average conscripts per nation
        stats.put("averageConscriptsPerNation", conscriptsByNation.size() > 0 ? 
            (double) totalConscripts / conscriptsByNation.size() : 0);
        
        return stats;
    }
}

