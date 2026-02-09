package com.axiom.api;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class AxiomAPI {
    private static final AXIOM plugin = AXIOM.getInstance();

    public static Map<String, Object> getPlayerStats(Player player) {
        Map<String, Object> stats = new HashMap<>();
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        
        if (nation != null) {
            stats.put("nationName", nation.getName());
            stats.put("role", nation.getRole(player.getUniqueId()).name());
            stats.put("treasury", nation.getTreasury());
            stats.put("population", nation.getCitizens().size());
            int territoryCount = plugin.getTerritoryService() != null
                ? plugin.getTerritoryService().getNationClaims(nation.getId()).size()
                : nation.getClaimedChunkKeys().size();
            stats.put("territory", territoryCount);
            stats.put("happiness", 75.0); // Default value
        } else {
            stats.put("nationName", "Нет нации");
            stats.put("role", "NONE");
        }
        
        stats.put("balance", plugin.getEconomyService().getBalance(player.getUniqueId()));
        return stats;
    }

    public static List<Map<String, Object>> getTechnologies(String nationId) {
        List<Map<String, Object>> techs = new ArrayList<>();
        if (nationId == null) return techs;
        
        plugin.getTechnologyTreeService().getAllTechs().forEach(tech -> {
            Map<String, Object> t = new HashMap<>();
            t.put("id", tech.id);
            t.put("name", tech.name);
            t.put("description", tech.description);
            t.put("tier", tech.tier);
            t.put("stage", tech.stage != null ? tech.stage.getId() : null);
            t.put("unlocked", plugin.getTechnologyTreeService().isTechnologyUnlocked(nationId, tech.id));
            techs.add(t);
        });
        return techs;
    }

    public static List<Map<String, Object>> getNations() {
        List<Map<String, Object>> nations = new ArrayList<>();
        plugin.getNationManager().getAllNations().forEach(nation -> {
            Map<String, Object> n = new HashMap<>();
            n.put("id", nation.getId());
            n.put("name", nation.getName());
            n.put("population", nation.getCitizens().size());
            int territoryCount = plugin.getTerritoryService() != null
                ? plugin.getTerritoryService().getNationClaims(nation.getId()).size()
                : nation.getClaimedChunkKeys().size();
            n.put("territory", territoryCount);
            n.put("capital", nation.getCapitalChunkStr());

            String leaderName = "Unknown";
            if (nation.getLeader() != null) {
                OfflinePlayer leader = Bukkit.getOfflinePlayer(nation.getLeader());
                if (leader != null && leader.getName() != null) {
                    leaderName = leader.getName();
                } else {
                    leaderName = nation.getLeader().toString();
                }
            }
            n.put("leader", leaderName);

            long sumX = 0;
            long sumZ = 0;
            int count = 0;
            for (String key : nation.getClaimedChunkKeys()) {
                if (key == null) continue;
                String[] parts = key.split(":");
                if (parts.length < 3) continue;
                try {
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    sumX += x;
                    sumZ += z;
                    count++;
                } catch (NumberFormatException ignored) {
                }
            }
            if (count == 0 && nation.getCapitalChunkStr() != null) {
                String[] parts = nation.getCapitalChunkStr().split(":");
                if (parts.length >= 3) {
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        sumX += x;
                        sumZ += z;
                        count = 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            int centerX = count > 0 ? (int) Math.round(sumX / (double) count) : 0;
            int centerZ = count > 0 ? (int) Math.round(sumZ / (double) count) : 0;
            n.put("centerX", centerX);
            n.put("centerZ", centerZ);
            nations.add(n);
        });
        return nations;
    }

    public static List<Map<String, Object>> getTerritories() {
        List<Map<String, Object>> territories = new ArrayList<>();
        if (plugin.getTerritoryService() != null) {
            for (var square : plugin.getTerritoryService().getAllSquares()) {
                Map<String, Object> t = new HashMap<>();
                t.put("world", square.getWorld());
                t.put("x", square.getX());
                t.put("z", square.getZ());
                t.put("nationId", square.getNationId());
                territories.add(t);
            }
            return territories;
        }

        plugin.getNationManager().getAllNations().forEach(nation -> {
            for (String key : nation.getClaimedChunkKeys()) {
                if (key == null) continue;
                String[] parts = key.split(":");
                if (parts.length < 3) continue;
                try {
                    Map<String, Object> t = new HashMap<>();
                    t.put("world", parts[0]);
                    t.put("x", Integer.parseInt(parts[1]));
                    t.put("z", Integer.parseInt(parts[2]));
                    t.put("nationId", nation.getId());
                    territories.add(t);
                } catch (NumberFormatException ignored) {
                }
            }
        });
        return territories;
    }

    public static boolean executeCommand(Player player, String command) {
        return player.performCommand(command);
    }
}
