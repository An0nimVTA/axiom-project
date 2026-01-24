package com.axiom.api;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
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
            stats.put("territory", nation.getClaimedChunkKeys().size());
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
            n.put("territory", nation.getClaimedChunkKeys().size());
            nations.add(n);
        });
        return nations;
    }

    public static boolean executeCommand(Player player, String command) {
        return player.performCommand(command);
    }
}
