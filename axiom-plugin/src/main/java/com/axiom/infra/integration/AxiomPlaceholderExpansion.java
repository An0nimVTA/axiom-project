package com.axiom.infra.integration;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion for AXIOM.
 * Provides placeholders for nation, balance, government, etc.
 * 
 * NOTE: This requires PlaceholderAPI plugin to be installed.
 * If PlaceholderAPI is not available, this will be skipped.
 */
public class AxiomPlaceholderExpansion {
    private final AXIOM plugin;
    
    public AxiomPlaceholderExpansion(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        try {
            // Dynamic registration using reflection (PlaceholderAPI is available, checked in AXIOM.java)
            Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            // Create a proxy that extends PlaceholderExpansion
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                expansionClass.getClassLoader(),
                new Class[]{expansionClass},
                (proxy1, method, args) -> {
                    if ("getIdentifier".equals(method.getName())) return "axiom";
                    if ("getAuthor".equals(method.getName())) return "AXIOM Team";
                    if ("getVersion".equals(method.getName())) return plugin.getDescription().getVersion();
                    if ("persist".equals(method.getName())) return true;
                    if ("onPlaceholderRequest".equals(method.getName()) && args.length == 2) {
                        return onPlaceholderRequest((Player) args[0], (String) args[1]);
                    }
                    if ("register".equals(method.getName())) {
                        // Register via PlaceholderAPI's API
                        Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                        papiClass.getMethod("registerExpansion", expansionClass).invoke(null, proxy1);
                        plugin.getLogger().info("PlaceholderAPI expansion зарегистрирована!");
                        return null;
                    }
                    return null;
                }
            );
            // Register the expansion
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            papiClass.getMethod("registerExpansion", expansionClass).invoke(null, proxy);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка регистрации PlaceholderAPI expansion: " + e.getMessage());
        }
    }
    
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        
        String nationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        Nation nation = nationId != null ? plugin.getNationManager().getNationById(nationId) : null;
        
        // Player placeholders
        switch (identifier.toLowerCase()) {
            case "nation":
            case "nation_name":
                return nation != null ? nation.getName() : "Нет нации";
                
            case "nation_id":
                return nationId != null ? nationId : "";
                
            case "role":
                if (nation == null) return "";
                Nation.Role role = nation.getRole(player.getUniqueId());
                return role != null ? role.name() : "CITIZEN";
                
            case "balance":
            case "money":
                return String.format("%.2f", plugin.getWalletService().getBalance(player.getUniqueId()));
                
            case "government":
                return nation != null ? nation.getGovernmentType() : "";
                
            case "treasury":
                return nation != null ? String.format("%.0f", nation.getTreasury()) : "0";
                
            case "currency":
                return nation != null ? nation.getCurrencyCode() : "AXC";
                
            case "inflation":
                return nation != null ? String.format("%.1f", nation.getInflation()) + "%" : "0%";
                
            case "tax_rate":
                return nation != null ? nation.getTaxRate() + "%" : "0%";
                
            case "chunks":
                return nation != null ? String.valueOf(nation.getClaimedChunkKeys().size()) : "0";
                
            case "citizens":
                return nation != null ? String.valueOf(nation.getCitizens().size()) : "0";
                
            case "motto":
                return nation != null && nation.getMotto() != null ? nation.getMotto() : "";
                
            case "allies_count":
                return nation != null ? String.valueOf(nation.getAllies().size()) : "0";
                
            case "enemies_count":
                return nation != null ? String.valueOf(nation.getEnemies().size()) : "0";
                
            case "religion":
                String religion = plugin.getPlayerDataManager().getReligion(player.getUniqueId());
                return religion != null ? religion : "Нет религии";
                
            case "education_level":
                return nationId != null ? 
                    String.format("%.1f", plugin.getEducationService().getEducationLevel(nationId)) : "0";
                
            case "happiness":
                return nationId != null ? 
                    String.format("%.1f", plugin.getHappinessService().getNationHappiness(nationId)) : "0";
                
            case "military_strength":
                return nationId != null ? 
                    String.format("%.0f", plugin.getMilitaryService().getMilitaryStrength(nationId)) : "0";
                
            case "available_techs":
                return nationId != null ? 
                    String.valueOf(plugin.getTechnologyTreeService().getAvailableTechs(nationId).size()) : "0";
        }
        
        // Nation-specific placeholders (requires nation parameter)
        if (identifier.startsWith("nation_")) {
            String[] parts = identifier.split("_", 3);
            if (parts.length >= 3 && nation != null) {
                String targetNationId = parts[1];
                String property = parts[2];
                Nation targetNation = plugin.getNationManager().getNationById(targetNationId);
                if (targetNation != null) {
                    switch (property.toLowerCase()) {
                        case "name":
                            return targetNation.getName();
                        case "treasury":
                            return String.format("%.0f", targetNation.getTreasury());
                        case "government":
                            return targetNation.getGovernmentType();
                        case "chunks":
                            return String.valueOf(targetNation.getClaimedChunkKeys().size());
                        case "reputation":
                            Integer rep = targetNation.getReputation().get(nationId);
                            return rep != null ? String.valueOf(rep) : "0";
                    }
                }
            }
        }
        
        return null;
    }
}

