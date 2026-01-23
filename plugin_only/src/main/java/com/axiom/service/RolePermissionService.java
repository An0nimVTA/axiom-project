package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Configurable permissions per role. Defaults: LEADER=all, MINISTER=economy/diplomacy, GENERAL=war, GOVERNOR=territory, CITIZEN=none. */
public class RolePermissionService {
    private final AXIOM plugin;
    private final Map<Nation.Role, Set<String>> permissions = new HashMap<>();

    public RolePermissionService(AXIOM plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        // Load from config if exists, else use defaults
        permissions.clear();
        // Default permissions (can be extended via config later)
        permissions.put(Nation.Role.LEADER, Set.of("all"));
        permissions.put(Nation.Role.MINISTER, Set.of("economy", "diplomacy", "taxes"));
        permissions.put(Nation.Role.GENERAL, Set.of("war", "mobilize"));
        permissions.put(Nation.Role.GOVERNOR, Set.of("territory", "claim", "unclaim"));
        permissions.put(Nation.Role.CITIZEN, Set.of());
    }

    public boolean has(Nation.Role role, String permission) {
        Set<String> perms = permissions.get(role);
        if (perms == null) return false;
        return perms.contains("all") || perms.contains(permission);
    }

    public boolean check(Nation nation, java.util.UUID uuid, String permission) {
        Nation.Role r = nation.getRole(uuid);
        return has(r, permission);
    }

    public boolean hasPermission(java.util.UUID uuid, String permission) {
        String nationId = plugin.getPlayerDataManager().getNation(uuid);
        if (nationId == null) return false;
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) return false;
        return check(nation, uuid, permission);
    }
    
    /**
     * Get comprehensive role permission statistics.
     */
    public synchronized Map<String, Object> getRolePermissionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Permissions by role
        Map<String, Set<String>> rolePermissions = new HashMap<>();
        for (Map.Entry<Nation.Role, Set<String>> entry : permissions.entrySet()) {
            rolePermissions.put(entry.getKey().name(), entry.getValue());
        }
        stats.put("rolePermissions", rolePermissions);
        
        // Total permissions per role
        Map<String, Integer> permissionsCount = new HashMap<>();
        for (Map.Entry<Nation.Role, Set<String>> entry : permissions.entrySet()) {
            permissionsCount.put(entry.getKey().name(), entry.getValue().size());
        }
        stats.put("permissionsCount", permissionsCount);
        
        // Available permissions list
        Set<String> allPermissions = new java.util.HashSet<>();
        for (Set<String> perms : permissions.values()) {
            allPermissions.addAll(perms);
        }
        allPermissions.remove("all"); // Remove special "all" permission
        stats.put("availablePermissions", new java.util.ArrayList<>(allPermissions));
        
        // Default permissions info
        stats.put("defaultPermissionsConfigured", true);
        stats.put("totalRoles", permissions.size());
        
        return stats;
    }
    
    /**
     * Get permissions for specific role.
     */
    public synchronized Map<String, Object> getRolePermissions(Nation.Role role) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> perms = permissions.get(role);
        if (perms == null) {
            stats.put("error", "Роль не найдена.");
            return stats;
        }
        
        stats.put("role", role.name());
        stats.put("permissions", new java.util.ArrayList<>(perms));
        stats.put("permissionsCount", perms.size());
        stats.put("hasAllPermission", perms.contains("all"));
        
        return stats;
    }
    
    /**
     * Get player permissions.
     */
    public synchronized Map<String, Object> getPlayerPermissions(java.util.UUID uuid) {
        Map<String, Object> stats = new HashMap<>();
        
        String nationId = plugin.getPlayerDataManager().getNation(uuid);
        if (nationId == null) {
            stats.put("error", "Игрок не в нации.");
            return stats;
        }
        
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) {
            stats.put("error", "Нация не найдена.");
            return stats;
        }
        
        Nation.Role role = nation.getRole(uuid);
        if (role == null) {
            stats.put("error", "Роль не назначена.");
            return stats;
        }
        
        stats.put("role", role.name());
        stats.put("nationId", nationId);
        stats.put("nationName", nation.getName());
        
        Set<String> perms = permissions.get(role);
        stats.put("permissions", perms != null ? new java.util.ArrayList<>(perms) : new java.util.ArrayList<>());
        stats.put("hasAllPermission", perms != null && perms.contains("all"));
        
        return stats;
    }
    
    /**
     * Check if role has specific permission.
     */
    public synchronized boolean roleHasPermission(Nation.Role role, String permission) {
        return has(role, permission);
    }
    
    /**
     * Get all roles with their permission counts.
     */
    public synchronized Map<String, Integer> getAllRolesPermissionCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<Nation.Role, Set<String>> entry : permissions.entrySet()) {
            counts.put(entry.getKey().name(), entry.getValue().size());
        }
        return counts;
    }
    
    /**
     * Get global role permission statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalRolePermissionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Permission configuration statistics
        Map<String, Set<String>> rolePermissions = new HashMap<>();
        Map<String, Integer> permissionsCount = new HashMap<>();
        
        for (Map.Entry<Nation.Role, Set<String>> entry : permissions.entrySet()) {
            rolePermissions.put(entry.getKey().name(), entry.getValue());
            permissionsCount.put(entry.getKey().name(), entry.getValue().size());
        }
        
        stats.put("rolePermissions", rolePermissions);
        stats.put("permissionsCount", permissionsCount);
        stats.put("totalRoles", permissions.size());
        
        // Available permissions
        Set<String> allPermissions = new java.util.HashSet<>();
        for (Set<String> perms : permissions.values()) {
            allPermissions.addAll(perms);
        }
        allPermissions.remove("all");
        stats.put("availablePermissions", new java.util.ArrayList<>(allPermissions));
        stats.put("totalUniquePermissions", allPermissions.size());
        
        // Role distribution across all nations
        Map<String, Integer> roleDistribution = new HashMap<>();
        int totalLeaders = 0;
        int totalMinisters = 0;
        int totalGenerals = 0;
        int totalGovernors = 0;
        int totalCitizens = 0;
        
        for (Nation nation : plugin.getNationManager().getAll()) {
            for (UUID citizenId : nation.getCitizens()) {
                Nation.Role role = nation.getRole(citizenId);
                if (role != null) {
                    roleDistribution.put(role.name(), roleDistribution.getOrDefault(role.name(), 0) + 1);
                    switch (role) {
                        case LEADER: totalLeaders++; break;
                        case MINISTER: totalMinisters++; break;
                        case GENERAL: totalGenerals++; break;
                        case GOVERNOR: totalGovernors++; break;
                        case CITIZEN: totalCitizens++; break;
                    }
                }
            }
        }
        
        stats.put("roleDistribution", roleDistribution);
        stats.put("totalLeaders", totalLeaders);
        stats.put("totalMinisters", totalMinisters);
        stats.put("totalGenerals", totalGenerals);
        stats.put("totalGovernors", totalGovernors);
        stats.put("totalCitizens", totalCitizens);
        
        // Average permissions per role (statistical)
        Map<String, Double> avgPermissionsPerRoleType = new HashMap<>();
        for (Nation.Role role : Nation.Role.values()) {
            Set<String> perms = permissions.get(role);
            avgPermissionsPerRoleType.put(role.name(), perms != null ? (double) perms.size() : 0.0);
        }
        stats.put("avgPermissionsPerRoleType", avgPermissionsPerRoleType);
        
        // Permission usage (which permissions are granted most often)
        Map<String, Integer> permissionUsage = new HashMap<>();
        for (Nation nation : plugin.getNationManager().getAll()) {
            for (UUID citizenId : nation.getCitizens()) {
                Nation.Role role = nation.getRole(citizenId);
                if (role != null) {
                    Set<String> perms = permissions.get(role);
                    if (perms != null) {
                        for (String perm : perms) {
                            if (!perm.equals("all")) {
                                permissionUsage.put(perm, permissionUsage.getOrDefault(perm, 0) + 1);
                            } else {
                                // "all" grants all permissions
                                for (String allPerm : allPermissions) {
                                    permissionUsage.put(allPerm, permissionUsage.getOrDefault(allPerm, 0) + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        stats.put("permissionUsage", permissionUsage);
        
        // Top roles by population
        List<Map.Entry<String, Integer>> topByPopulation = roleDistribution.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPopulation", topByPopulation);
        
        // Most used permissions
        List<Map.Entry<String, Integer>> topByUsage = permissionUsage.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByUsage", topByUsage);
        
        return stats;
    }
}

