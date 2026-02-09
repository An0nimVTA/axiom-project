package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.World;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.axiom.domain.service.military.AdvancedWarSystem;
import com.axiom.domain.service.state.NationManager;

/**
 * Handles alliances, wars, sanctions, treaties. v1: war + warzones for 24h.
 */
public class DiplomacySystem {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final DiplomacyRelationService relationService;

    private final Map<String, Long> lastWarDeclaredAt = new ConcurrentHashMap<>(); // nationId -> ts

    public DiplomacySystem(AXIOM plugin, NationManager nationManager, DiplomacyRelationService relationService) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.relationService = relationService;
    }

    public synchronized String declareWar(Nation attacker, Nation defender) throws IOException {
        double cost = 5000.0;
        long now = System.currentTimeMillis();
        long cooldown = 72L * 60L * 60L * 1000L;
        if (relationService != null) {
            DiplomacyRelationService.RelationStatus status = relationService.getStatus(attacker.getId(), defender.getId());
            if (status == DiplomacyRelationService.RelationStatus.WAR) {
                return "Война уже активна.";
            }
            if (status == DiplomacyRelationService.RelationStatus.ALLIANCE) {
                return "Нельзя объявить войну союзнику. Расторгните альянс.";
            }
        }
        Long last = lastWarDeclaredAt.get(attacker.getId());
        if (last != null && last + cooldown > now) return "Нация на перезарядке войны.";
        if (attacker.getTreasury() < cost) return "Недостаточно средств для объявления войны.";
        attacker.setTreasury(attacker.getTreasury() - cost);
        nationManager.save(attacker);
        lastWarDeclaredAt.put(attacker.getId(), now);
        if (relationService != null) {
            long duration = plugin.getAdvancedWarSystem() != null ? Long.MAX_VALUE : 24L * 60L * 60L * 1000L;
            String err = relationService.setStatus(attacker.getId(), defender.getId(),
                DiplomacyRelationService.RelationStatus.WAR, duration, "declareWar");
            if (err != null) {
                attacker.setTreasury(attacker.getTreasury() + cost);
                nationManager.save(attacker);
                return err;
            }
        }
        long timestamp = System.currentTimeMillis();
        attacker.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Война объявлена " + defender.getName());
        defender.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Война объявлена " + attacker.getName());
        nationManager.save(attacker);
        nationManager.save(defender);
        
        // VISUAL EFFECTS: Broadcast war declaration to all online players
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (org.bukkit.entity.Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                plugin.getVisualEffectsService().playWarDeclarationEffect(
                    onlinePlayer, attacker.getName(), defender.getName());
            }
        });
        
        if (plugin.getAdvancedWarSystem() != null) {
            plugin.getAdvancedWarSystem().registerDiplomaticWar(attacker.getId(), defender.getId());
            return "Война объявлена. Идёт фаза подготовки; Warzone активируется при начале боевых действий.";
        }
        return "Война объявлена. Режим боевой зоны активен на 24 часа.";
    }

    public boolean isAtWar(String nationId, String otherNationId) {
        if (relationService == null) return false;
        return relationService.isAtWar(nationId, otherNationId);
    }

    public boolean isWarzone(World world, int chunkX, int chunkZ) {
        // a chunk is warzone if it belongs to any nation currently at war (either side)
        Optional<Nation> owner = nationManager.getNationClaiming(world, chunkX, chunkZ);
        if (owner.isEmpty()) return false;
        String ownerId = owner.get().getId();
        AdvancedWarSystem warSystem = plugin.getAdvancedWarSystem();
        if (warSystem != null) {
            return warSystem.isNationInActiveWar(ownerId);
        }
        return relationService != null && relationService.hasActiveWarWithAny(ownerId);
    }

    public synchronized String requestAlliance(Nation a, Nation b) throws IOException {
        if (relationService != null) {
            DiplomacyRelationService.RelationStatus status = relationService.getStatus(a.getId(), b.getId());
            if (status == DiplomacyRelationService.RelationStatus.ALLIANCE) return "Уже союзники.";
            if (status == DiplomacyRelationService.RelationStatus.WAR) return "Нельзя заключить союз во время войны.";
            if (status == DiplomacyRelationService.RelationStatus.CEASEFIRE) return "Нельзя заключить союз в перемирии.";
            if (relationService.isSanctioned(a.getId(), b.getId()) || relationService.isSanctioned(b.getId(), a.getId())) {
                return "Нельзя заключить союз при активных санкциях.";
            }
        } else if (a.getAllies().contains(b.getId())) {
            return "Уже союзники.";
        }
        a.getPendingAlliance().add("out:" + b.getId());
        b.getPendingAlliance().add("in:" + a.getId());
        nationManager.save(a); nationManager.save(b);
        
        // VISUAL EFFECTS: Notify target nation's leader and ministers
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID citizenId : b.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    Nation.Role role = b.getRole(citizenId);
                    // Notify leaders and ministers
                    if (role == Nation.Role.LEADER || role == Nation.Role.MINISTER) {
                        citizen.sendTitle("§b§l[ЗАПРОС АЛЬЯНСА]", "§fНация '" + a.getName() + "' предлагает альянс", 10, 80, 20);
                        plugin.getVisualEffectsService().sendActionBar(citizen, "§b🤝 Запрос альянса от '" + a.getName() + "'. Используйте §e/axiom diplomacy accept-ally " + a.getId());
                        // Blue particles
                        org.bukkit.Location loc = citizen.getLocation();
                        loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                        citizen.playSound(loc, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
                    }
                }
            }
        });
        
        return "Запрос на альянс отправлен.";
    }

    public synchronized String acceptAlliance(Nation a, Nation b) throws IOException {
        if (!a.getPendingAlliance().contains("in:" + b.getId())) return "Нет запроса от этой нации.";
        if (relationService != null) {
            String err = relationService.setStatus(a.getId(), b.getId(),
                DiplomacyRelationService.RelationStatus.ALLIANCE, 0, "acceptAlliance");
            if (err != null) return err;
        }
        a.getPendingAlliance().remove("in:" + b.getId());
        b.getPendingAlliance().remove("out:" + a.getId());
        nationManager.save(a); nationManager.save(b);
        
        // VISUAL EFFECTS: Celebrate alliance formation
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID citizenId : a.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[АЛЬЯНС]", "§fНация '" + b.getName() + "' стала союзником!", 10, 80, 20);
                    plugin.getVisualEffectsService().sendActionBar(citizen, "§a🤝 Альянс с '" + b.getName() + "' заключён!");
                    // Green particles
                    org.bukkit.Location loc = citizen.getLocation();
                    for (int i = 0; i < 15; i++) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                    }
                    citizen.playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                }
            }
            for (UUID citizenId : b.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[АЛЬЯНС]", "§fНация '" + a.getName() + "' стала союзником!", 10, 80, 20);
                    plugin.getVisualEffectsService().sendActionBar(citizen, "§a🤝 Альянс с '" + a.getName() + "' заключён!");
                    // Green particles
                    org.bukkit.Location loc = citizen.getLocation();
                    for (int i = 0; i < 15; i++) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                    }
                    citizen.playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                }
            }
        });
        
        return "Альянс заключён.";
    }

    public synchronized void setReputation(Nation a, Nation b, int value) throws IOException {
        int v = Math.max(-100, Math.min(100, value));
        int oldValue = a.getReputation().getOrDefault(b.getId(), 0);
        a.getReputation().put(b.getId(), v);
        nationManager.save(a);
        
        // VISUAL EFFECTS: Notify player of reputation change (only if significant change)
        if (Math.abs(v - oldValue) >= 10) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player actor = org.bukkit.Bukkit.getPlayer(
                    a.getCitizens().stream().filter(u -> a.getRole(u) == Nation.Role.LEADER).findFirst().orElse(null));
                if (actor != null && actor.isOnline()) {
                    String color = v >= 50 ? "§a" : v >= 0 ? "§e" : "§c";
                    plugin.getVisualEffectsService().sendActionBar(actor, 
                        color + "📊 Репутация с '" + b.getName() + "': " + v + " (" + (v - oldValue > 0 ? "+" : "") + (v - oldValue) + ")");
                }
            });
        }
    }

    public synchronized void declarePeace(String nationA, String nationB) throws IOException {
        Nation a = nationManager.getNationById(nationA);
        Nation b = nationManager.getNationById(nationB);
        if (a == null || b == null) return;
        if (relationService != null) {
            relationService.setStatus(nationA, nationB, DiplomacyRelationService.RelationStatus.NEUTRAL, 0, "declarePeace");
        }
        nationManager.save(a);
        nationManager.save(b);
        
        // VISUAL EFFECTS: Celebrate peace treaty
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = "§a☮ МИР заключён между '" + a.getName() + "' и '" + b.getName() + "'!";
            for (UUID citizenId : a.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[МИР]", "§fДоговор с '" + b.getName() + "' подписан", 10, 80, 20);
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                    // Green/white particles
                    org.bukkit.Location loc = citizen.getLocation();
                    for (int i = 0; i < 20; i++) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                    }
                    citizen.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                }
            }
            for (UUID citizenId : b.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[МИР]", "§fДоговор с '" + a.getName() + "' подписан", 10, 80, 20);
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                    // Green/white particles
                    org.bukkit.Location loc = citizen.getLocation();
                    for (int i = 0; i < 20; i++) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1 + i * 0.1, 0), 1, 0.5, 0.5, 0.5, 0.1);
                    }
                    citizen.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                }
            }
        });
    }
    
    /**
     * Deny alliance request.
     */
    public synchronized String denyAlliance(Nation a, Nation b) throws IOException {
        if (!a.getPendingAlliance().contains("in:" + b.getId())) return "Нет запроса от этой нации.";
        a.getPendingAlliance().remove("in:" + b.getId());
        b.getPendingAlliance().remove("out:" + a.getId());
        nationManager.save(a);
        nationManager.save(b);
        return "Запрос альянса отклонён.";
    }
    
    /**
     * Break alliance.
     */
    public synchronized String breakAlliance(Nation a, Nation b) throws IOException {
        if (!a.getAllies().contains(b.getId())) return "Не союзники.";
        if (relationService != null) {
            String err = relationService.setStatus(a.getId(), b.getId(),
                DiplomacyRelationService.RelationStatus.NEUTRAL, 0, "breakAlliance");
            if (err != null) return err;
        } else {
            a.getAllies().remove(b.getId());
            b.getAllies().remove(a.getId());
        }
        // Negative reputation impact
        int currentRep = a.getReputation().getOrDefault(b.getId(), 0);
        a.getReputation().put(b.getId(), Math.max(-100, currentRep - 20));
        b.getReputation().put(a.getId(), Math.max(-100, b.getReputation().getOrDefault(a.getId(), 0) - 20));
        nationManager.save(a);
        nationManager.save(b);
        
        // VISUAL EFFECTS
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = "§c⚠ Альянс с '" + b.getName() + "' расторгнут.";
            for (UUID citizenId : a.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                }
            }
            msg = "§c⚠ Альянс с '" + a.getName() + "' расторгнут.";
            for (UUID citizenId : b.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                }
            }
        });
        
        return "Альянс расторгнут.";
    }
    
    /**
     * Get diplomatic status between two nations.
     */
    public synchronized String getDiplomaticStatus(String nationAId, String nationBId) {
        Nation a = nationManager.getNationById(nationAId);
        Nation b = nationManager.getNationById(nationBId);
        if (a == null || b == null) return "Нация не найдена.";
        
        if (relationService != null) {
            DiplomacyRelationService.RelationStatus status = relationService.getStatus(nationAId, nationBId);
            if (status == DiplomacyRelationService.RelationStatus.WAR) return "ВОЙНА";
            if (status == DiplomacyRelationService.RelationStatus.CEASEFIRE) return "ПЕРЕМИРИЕ";
            if (status == DiplomacyRelationService.RelationStatus.ALLIANCE) return "АЛЬЯНС";
        } else {
            if (isAtWar(nationAId, nationBId)) return "ВОЙНА";
            if (a.getAllies().contains(nationBId)) return "АЛЬЯНС";
        }
        if (a.getEnemies().contains(nationBId)) return "ВРАГ";
        
        int reputation = a.getReputation().getOrDefault(nationBId, 0);
        if (reputation >= 50) return "ДРУЖЕСТВЕННЫЕ";
        if (reputation >= 0) return "НЕЙТРАЛЬНЫЕ";
        if (reputation >= -50) return "НАПРЯЖЁННЫЕ";
        return "ВРАЖДЕБНЫЕ";
    }
    
    /**
     * Get all alliances for a nation.
     */
    public synchronized List<Nation> getAlliances(String nationId) {
        Nation nation = nationManager.getNationById(nationId);
        if (nation == null) return Collections.emptyList();
        
        List<Nation> allies = new ArrayList<>();
        for (String allyId : nation.getAllies()) {
            Nation ally = nationManager.getNationById(allyId);
            if (ally != null) allies.add(ally);
        }
        return allies;
    }
    
    /**
     * Get all pending alliance requests (incoming).
     */
    public synchronized List<String> getPendingAllianceRequests(String nationId) {
        Nation nation = nationManager.getNationById(nationId);
        if (nation == null) return Collections.emptyList();
        
        List<String> requests = new ArrayList<>();
        for (String pending : nation.getPendingAlliance()) {
            if (pending.startsWith("in:")) {
                requests.add(pending.substring(3));
            }
        }
        return requests;
    }
    
    /**
     * Get diplomatic statistics.
     */
    public synchronized Map<String, Object> getDiplomaticStatistics(String nationId) {
        Nation nation = nationManager.getNationById(nationId);
        if (nation == null) return Collections.emptyMap();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("allies", nation.getAllies().size());
        stats.put("enemies", nation.getEnemies().size());
        stats.put("pendingAlliances", getPendingAllianceRequests(nationId).size());
        stats.put("activeWars", relationService != null ?
            relationService.countRelationsForNation(nationId, DiplomacyRelationService.RelationStatus.WAR) : 0);
        
        // Average reputation
        if (!nation.getReputation().isEmpty()) {
            double avgRep = nation.getReputation().values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
            stats.put("averageReputation", avgRep);
        } else {
            stats.put("averageReputation", 0.0);
        }
        
        return stats;
    }
    
    /**
     * Get global diplomatic statistics.
     */
    public synchronized Map<String, Object> getGlobalDiplomaticStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalAlliances = 0;
        int totalWars = 0;
        int totalReputationRelations = 0;
        
        for (Nation n : nationManager.getAll()) {
            totalAlliances += n.getAllies().size();
            totalWars += n.getEnemies().size();
            totalReputationRelations += n.getReputation().size();
        }
        
        stats.put("totalAlliances", totalAlliances / 2); // Each alliance counted twice
        stats.put("totalWarRelations", totalWars / 2); // Each war counted twice
        stats.put("totalReputationRelations", totalReputationRelations);
        stats.put("totalNations", nationManager.getAll().size());
        
        stats.put("activeWars", relationService != null ?
            relationService.countRelationsByStatus(DiplomacyRelationService.RelationStatus.WAR) : 0);
        
        // War cooldowns
        int nationsOnCooldown = 0;
        long cooldown = 72L * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastWarDeclaredAt.entrySet()) {
            if (entry.getValue() + cooldown > now) {
                nationsOnCooldown++;
            }
        }
        stats.put("nationsOnWarCooldown", nationsOnCooldown);
        
        return stats;
    }
    
    /**
     * Get war history for a nation.
     */
    public synchronized List<Map<String, Object>> getWarHistory(String nationId) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        long now = System.currentTimeMillis();
        if (relationService != null) {
            for (DiplomacyRelationService.Relation rel : relationService.getRelationsForNation(nationId)) {
                if (rel.status != DiplomacyRelationService.RelationStatus.WAR) continue;
                Map<String, Object> warData = new HashMap<>();
                String opponent = nationId.equals(rel.nationA) ? rel.nationB : rel.nationA;
                warData.put("opponent", opponent);
                warData.put("activeUntil", rel.expiresAt);
                warData.put("isActive", rel.expiresAt == Long.MAX_VALUE || rel.expiresAt > now);
                long remaining = rel.expiresAt == Long.MAX_VALUE ? -1 : Math.max(0, (rel.expiresAt - now) / 1000 / 60);
                warData.put("timeRemaining", remaining);
                history.add(warData);
            }
        }
        
        return history;
    }
    
    /**
     * Get alliance network for a nation (allies of allies).
     */
    public synchronized Set<String> getAllianceNetwork(String nationId) {
        Set<String> network = new HashSet<>();
        Set<String> toProcess = new HashSet<>();
        toProcess.add(nationId);
        
        while (!toProcess.isEmpty()) {
            String currentId = toProcess.iterator().next();
            toProcess.remove(currentId);
            network.add(currentId);
            
            Nation nation = nationManager.getNationById(currentId);
            if (nation != null) {
                for (String allyId : nation.getAllies()) {
                    if (!network.contains(allyId)) {
                        toProcess.add(allyId);
                    }
                }
            }
        }
        
        network.remove(nationId); // Remove self
        return network;
    }
}


