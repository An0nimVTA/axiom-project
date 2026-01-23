package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Manages player migration between nations. */
public class MigrationService {
    private final AXIOM plugin;
    private final Map<UUID, Long> migrationCooldown = new HashMap<>(); // player -> cooldown until

    public MigrationService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized String migratePlayer(UUID playerId, String fromNationId, String targetNationId) {
        // Overloaded method with explicit from nation
        String currentNationId = plugin.getPlayerDataManager().getNation(playerId);
        if (currentNationId == null || !currentNationId.equals(fromNationId)) {
            // Use explicit from nation
            currentNationId = fromNationId;
        }
        return migratePlayer(playerId, targetNationId);
    }

    public synchronized String migratePlayer(UUID playerId, String targetNationId) {
        String currentNationId = plugin.getPlayerDataManager().getNation(playerId);
        if (currentNationId == null) return "Вы не в нации.";
        if (currentNationId.equals(targetNationId)) return "Вы уже в этой нации.";
        Long cooldown = migrationCooldown.get(playerId);
        if (cooldown != null && cooldown > System.currentTimeMillis()) {
            long remaining = (cooldown - System.currentTimeMillis()) / 1000 / 60;
            return "Миграция на перезарядке. Осталось: " + remaining + " минут.";
        }
        Nation current = plugin.getNationManager().getNationById(currentNationId);
        Nation target = plugin.getNationManager().getNationById(targetNationId);
        if (current == null || target == null) return "Нация не найдена.";
        // Remove from current
        current.getCitizens().remove(playerId);
        current.getRoles().remove(playerId);
        // Add to target
        target.getCitizens().add(playerId);
        target.getRoles().put(playerId, Nation.Role.CITIZEN);
        migrationCooldown.put(playerId, System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L); // 7 days
        try {
            plugin.getNationManager().save(current);
            plugin.getNationManager().save(target);
            plugin.getPlayerDataManager().setNation(playerId, targetNationId, "CITIZEN");
        } catch (Exception ignored) {}
        return "Миграция выполнена. Вы теперь гражданин " + target.getName();
    }
    
    /**
     * Get comprehensive migration statistics.
     */
    public synchronized Map<String, Object> getMigrationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Count players with active cooldown who migrated to this nation
        int incomingMigrations = 0;
        int outgoingMigrations = 0;
        
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n != null) {
            for (UUID citizenId : n.getCitizens()) {
                Long cooldown = migrationCooldown.get(citizenId);
                if (cooldown != null && cooldown > System.currentTimeMillis()) {
                    incomingMigrations++;
                }
            }
        }
        
        // Count players who migrated away (have cooldown but not in this nation)
        for (Map.Entry<UUID, Long> entry : migrationCooldown.entrySet()) {
            if (entry.getValue() > System.currentTimeMillis()) {
                String playerNation = plugin.getPlayerDataManager().getNation(entry.getKey());
                if (playerNation != null && playerNation.equals(nationId)) {
                    // Already counted above
                } else {
                    // Could be outgoing migration
                    outgoingMigrations++;
                }
            }
        }
        
        stats.put("incomingMigrations", incomingMigrations);
        stats.put("outgoingMigrations", outgoingMigrations);
        stats.put("netMigration", incomingMigrations - outgoingMigrations);
        
        return stats;
    }
    
    /**
     * Get migration cooldown for a player.
     */
    public synchronized long getMigrationCooldown(UUID playerId) {
        Long cooldown = migrationCooldown.get(playerId);
        if (cooldown == null) return 0;
        
        long remaining = cooldown - System.currentTimeMillis();
        return Math.max(0, remaining / 1000 / 60); // Return in minutes
    }
    
    /**
     * Check if player can migrate.
     */
    public synchronized boolean canMigrate(UUID playerId) {
        Long cooldown = migrationCooldown.get(playerId);
        return cooldown == null || cooldown <= System.currentTimeMillis();
    }
    
    /**
     * Force migration (admin/event based).
     */
    public synchronized String forceMigration(UUID playerId, String targetNationId) throws IOException {
        String currentNationId = plugin.getPlayerDataManager().getNation(playerId);
        if (currentNationId == null) return "Игрок не в нации.";
        if (currentNationId.equals(targetNationId)) return "Игрок уже в этой нации.";
        
        // Skip cooldown for forced migration
        Nation current = plugin.getNationManager().getNationById(currentNationId);
        Nation target = plugin.getNationManager().getNationById(targetNationId);
        if (current == null || target == null) return "Нация не найдена.";
        
        current.getCitizens().remove(playerId);
        current.getRoles().remove(playerId);
        target.getCitizens().add(playerId);
        target.getRoles().put(playerId, Nation.Role.CITIZEN);
        
        plugin.getNationManager().save(current);
        plugin.getNationManager().save(target);
        plugin.getPlayerDataManager().setNation(playerId, targetNationId, "CITIZEN");
        
        return "Принудительная миграция выполнена. Игрок теперь гражданин " + target.getName();
    }
    
    /**
     * Get global migration statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalMigrationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPlayersOnCooldown = 0;
        Map<String, Integer> incomingByNation = new HashMap<>();
        Map<String, Integer> outgoingByNation = new HashMap<>();
        long now = System.currentTimeMillis();
        
        for (Map.Entry<UUID, Long> entry : migrationCooldown.entrySet()) {
            if (entry.getValue() > now) {
                totalPlayersOnCooldown++;
                
                String playerNation = plugin.getPlayerDataManager().getNation(entry.getKey());
                if (playerNation != null) {
                    incomingByNation.put(playerNation, incomingByNation.getOrDefault(playerNation, 0) + 1);
                }
                
                // Try to find previous nation (this is approximate)
                // For better tracking, we'd need to store migration history
                // For now, we'll just count total migrations
            }
        }
        
        stats.put("totalPlayersOnCooldown", totalPlayersOnCooldown);
        stats.put("incomingMigrationsByNation", incomingByNation);
        stats.put("nationsReceivingMigrations", incomingByNation.size());
        
        // Top nations receiving migrations
        List<Map.Entry<String, Integer>> topReceiving = incomingByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topReceivingMigrations", topReceiving);
        
        // Average migrations per nation
        stats.put("averageIncomingMigrations", incomingByNation.size() > 0 ? 
            (double) totalPlayersOnCooldown / incomingByNation.size() : 0);
        
        return stats;
    }
}

