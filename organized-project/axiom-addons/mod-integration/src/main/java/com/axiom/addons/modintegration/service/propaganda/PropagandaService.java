package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.api.ModIntegrationAPI;
import com.axiom.model.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for propaganda system using mod items.
 * Allows nations to create and distribute propaganda using both vanilla and mod items.
 */
public class PropagandaService {
    private final AXIOM plugin;
    private final ModIntegrationAPI modAPI;
    private final NationManager nationManager;
    
    private final Map<String, PropagandaCampaign> activeCampaigns = new HashMap<>();
    private final Map<String, List<PropagandaMessage>> campaignMessages = new HashMap<>();
    private final Map<UUID, Long> playerPropagandaCooldowns = new HashMap<>();
    private final Map<String, Double> nationInfluence = new HashMap<>();
    private final Map<String, List<String>> nationPropagandaEvents = new HashMap<>();

    public PropagandaService(AXIOM plugin) {
        this.plugin = plugin;
        this.modAPI = plugin.getModIntegrationAPI();
        this.nationManager = plugin.getNationManager();
        
        // Schedule periodic influence updates
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateInfluence, 0, 20 * 60 * 5); // every 5 minutes
    }

    /**
     * Create a new propaganda campaign.
     */
    public String createCampaign(String originNationId, String targetNationId, String title, 
                                String message, int duration, ItemStack propagandaItem) {
        try {
            Nation originNation = nationManager.getNationById(originNationId);
            Nation targetNation = nationManager.getNationById(targetNationId);
            
            if (originNation == null || targetNation == null) {
                return "Нация не найдена";
            }

            String campaignId = generateCampaignId(originNationId, targetNationId);
            
            PropagandaCampaign campaign = new PropagandaCampaign(
                campaignId,
                originNationId,
                targetNationId,
                title,
                message,
                duration,
                System.currentTimeMillis()
            );
            
            activeCampaigns.put(campaignId, campaign);
            campaignMessages.put(campaignId, new ArrayList<>());
            
            // Add initial message
            addMessageToCampaign(campaignId, originNationId, message, propagandaItem);
            
            plugin.getLogger().info("Created propaganda campaign: " + title + 
                                   " from " + originNationId + " to " + targetNationId);
            
            return "✓ Кампания пропаганды создана: " + title;
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating propaganda campaign: " + e.getMessage());
            return "Ошибка при создании кампании: " + e.getMessage();
        }
    }

    /**
     * Spread propaganda using a mod item as a medium.
     */
    public String spreadPropagandaWithItem(Player player, String targetNationId, ItemStack item) {
        try {
            if (item == null || item.getType() == Material.AIR) {
                return "Неверный предмет";
            }
            
            UUID playerId = player.getUniqueId();
            String originNationId = plugin.getPlayerDataManager().getNation(playerId);
            
            if (originNationId == null) {
                return "Вы не состоите в нации";
            }
            
            // Check cooldown
            long currentTime = System.currentTimeMillis();
            Long lastUse = playerPropagandaCooldowns.get(playerId);
            if (lastUse != null && (currentTime - lastUse) < 30000) { // 30 second cooldown
                return "Подождите перед следующей попыткой пропаганды";
            }
            
            // Detect mod from item
            String modId = modAPI.detectModFromItem(item);
            String itemCategory = "vanilla";
            
            if (modId != null) {
                itemCategory = "mod_" + modId;
            }
            
            // Calculate influence based on item category and mod
            double influence = calculateItemInfluence(item, modId);
            
            // Apply nation technology bonuses
            double techBonus = 1.0;
            if (plugin.getTechnologyTreeService() != null) {
                techBonus = plugin.getTechnologyTreeService().getBonus(originNationId, "propagandaEffectiveness");
            }
            
            influence *= techBonus;
            
            // Update influence between nations
            updateNationInfluence(originNationId, targetNationId, influence);
            
            // Record the action
            String eventId = "propaganda_" + playerId + "_" + System.currentTimeMillis();
            List<String> events = nationPropagandaEvents.computeIfAbsent(originNationId, k -> new ArrayList<>());
            events.add(eventId);
            
            // Update cooldown
            playerPropagandaCooldowns.put(playerId, currentTime);
            
            // Show effect to player
            player.sendMessage("§a[Пропаганда] §fВы распространили идеи своей нации с помощью " + 
                              item.getType().name() + ". Влияние: §e" + String.format("%.2f", influence));
            
            // Show subtle effect to target nation players
            notifyTargetNation(targetNationId, originNationId, item.getType().name());
            
            plugin.getLogger().info("Player " + player.getName() + " spread propaganda using " + 
                                   item.getType() + " from mod: " + modId + " with influence: " + influence);
            
            return "✓ Пропаганда успешно распространена! Влияние: " + influence;
        } catch (Exception e) {
            plugin.getLogger().severe("Error spreading propaganda: " + e.getMessage());
            return "Ошибка при распространении пропаганды: " + e.getMessage();
        }
    }

    /**
     * Create propaganda material using mod resources.
     */
    public String createPropagandaMaterial(Player player, String title, String content, Material baseMaterial) {
        try {
            UUID playerId = player.getUniqueId();
            String nationId = plugin.getPlayerDataManager().getNation(playerId);
            
            if (nationId == null) {
                return "Вы не состоите в нации";
            }
            
            // Check if player has required resources
            // This could involve mod resources like paper from industrial mods, etc.
            boolean hasResources = true; // Simplified for now
            
            if (!hasResources) {
                return "Недостаточно ресурсов для создания пропаганды";
            }
            
            // Create an item representing the propaganda material
            ItemStack propagandaItem = new ItemStack(baseMaterial);
            // In a real implementation, we would add lore, title, etc.
            
            // Add to player's inventory
            Map<Integer, ItemStack> remainingItems = player.getInventory().addItem(propagandaItem);
            
            if (!remainingItems.isEmpty()) {
                player.sendMessage("§c[Пропаганда] §fЧасть материалов упала к вам под ноги");
                for (ItemStack stack : remainingItems.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                }
            } else {
                player.sendMessage("§a[Пропаганда] §fМатериалы пропаганды созданы: §e" + title);
            }
            
            plugin.getLogger().info("Player " + player.getName() + " created propaganda material: " + title);
            
            return "✓ Материалы пропаганды созданы!";
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating propaganda material: " + e.getMessage());
            return "Ошибка при создании материалов: " + e.getMessage();
        }
    }

    /**
     * Calculate influence based on item used.
     */
    private double calculateItemInfluence(ItemStack item, String modId) {
        double baseInfluence = 0.5; // Base influence
        
        // Higher influence for special items
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            baseInfluence += 0.3; // Themed items have more impact
        }
        
        // Check for mod-specific influence
        if (modId != null) {
            switch (modId.toLowerCase()) {
                case "immersiveengineering":
                    baseInfluence += 0.5; // Industrial items can carry messages
                    break;
                case "tacz":
                case "pointblank":
                case "ballistix":
                    baseInfluence += 0.8; // Weapons have psychological impact
                    break;
                case "appliedenergistics2":
                    baseInfluence += 0.4; // Tech items spread information
                    break;
                case "curios":
                case "capsawims":
                case "warium":
                    baseInfluence += 0.6; // Equipment items are visible to others
                    break;
                default:
                    baseInfluence += 0.3; // Other mod items
            }
        }
        
        // Influence scales with item amount
        baseInfluence *= Math.min(2.0, Math.sqrt(item.getAmount()));
        
        return Math.min(5.0, baseInfluence); // Cap at 5.0 influence
    }

    /**
     * Update influence between nations.
     */
    private void updateNationInfluence(String originNationId, String targetNationId, double influence) {
        String key = originNationId + "_to_" + targetNationId;
        double currentInfluence = nationInfluence.getOrDefault(key, 0.0);
        
        // Add new influence with decay of old influence
        double decayedInfluence = currentInfluence * 0.95; // 5% decay
        double newInfluence = decayedInfluence + influence;
        
        // Apply cap
        newInfluence = Math.min(100.0, newInfluence);
        
        nationInfluence.put(key, newInfluence);
        
        plugin.getLogger().info("Influence update: " + originNationId + " -> " + targetNationId + 
                               " = " + newInfluence);
    }

    /**
     * Notify players in target nation about propaganda.
     */
    private void notifyTargetNation(String targetNationId, String originNationId, String itemUsed) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            String playerNation = plugin.getPlayerDataManager().getNation(onlinePlayer.getUniqueId());
            if (targetNationId.equals(playerNation)) {
                // Show a subtle notification
                onlinePlayer.sendMessage("§7[Слухи] §fВы слышите разговоры о " + 
                                        itemUsed.replace("_", " ").toLowerCase() + 
                                        " из нации §e" + originNationId + "§f...");
            }
        }
    }

    /**
     * Add a message to a campaign.
     */
    private void addMessageToCampaign(String campaignId, String originNationId, String message, ItemStack item) {
        PropagandaMessage propMessage = new PropagandaMessage(
            "msg_" + System.currentTimeMillis(),
            originNationId,
            message,
            item,
            System.currentTimeMillis()
        );
        
        campaignMessages.computeIfAbsent(campaignId, k -> new ArrayList<>()).add(propMessage);
    }

    /**
     * Get influence level between two nations.
     */
    public double getInfluenceLevel(String originNationId, String targetNationId) {
        String key = originNationId + "_to_" + targetNationId;
        return nationInfluence.getOrDefault(key, 0.0);
    }

    /**
     * Get active campaigns for a nation.
     */
    public List<Map<String, Object>> getActiveCampaigns(String nationId, boolean isOrigin) {
        List<Map<String, Object>> campaigns = new ArrayList<>();
        
        for (PropagandaCampaign campaign : activeCampaigns.values()) {
            boolean matches = isOrigin ? campaign.originNationId.equals(nationId) :
                                         campaign.targetNationId.equals(nationId);
            
            if (matches) {
                Map<String, Object> campaignInfo = new HashMap<>();
                campaignInfo.put("campaignId", campaign.campaignId);
                campaignInfo.put("originNationId", campaign.originNationId);
                campaignInfo.put("targetNationId", campaign.targetNationId);
                campaignInfo.put("title", campaign.title);
                campaignInfo.put("message", campaign.message);
                campaignInfo.put("duration", campaign.duration);
                campaignInfo.put("startTime", campaign.startTime);
                campaignInfo.put("timeRemaining", (campaign.startTime + campaign.duration) - System.currentTimeMillis());
                campaignInfo.put("isActive", isCampaignActive(campaign.campaignId));
                
                campaigns.add(campaignInfo);
            }
        }
        
        return campaigns;
    }

    /**
     * Check if a campaign is still active.
     */
    public boolean isCampaignActive(String campaignId) {
        PropagandaCampaign campaign = activeCampaigns.get(campaignId);
        if (campaign == null) {
            return false;
        }
        
        long timeRemaining = (campaign.startTime + campaign.duration) - System.currentTimeMillis();
        return timeRemaining > 0;
    }

    /**
     * Get influence statistics for a nation.
     */
    public Map<String, Object> getInfluenceStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Nations we're influencing
        Map<String, Double> influencing = new HashMap<>();
        // Nations influencing us
        Map<String, Double> influencedBy = new HashMap<>();
        
        for (Map.Entry<String, Double> entry : nationInfluence.entrySet()) {
            String key = entry.getKey();
            double influence = entry.getValue();
            
            if (key.startsWith(nationId + "_to_")) {
                String target = key.substring((nationId + "_to_").length());
                influencing.put(target, influence);
            } else if (key.endsWith("_to_" + nationId)) {
                String origin = key.substring(0, key.length() - ("_to_" + nationId).length());
                influencedBy.put(origin, influence);
            }
        }
        
        stats.put("influencing", influencing);
        stats.put("influencedBy", influencedBy);
        stats.put("propagandaEvents", nationPropagandaEvents.getOrDefault(nationId, new ArrayList<>()).size());
        stats.put("activeCampaigns", getActiveCampaigns(nationId, true).size());
        stats.put("defensiveCampaigns", getActiveCampaigns(nationId, false).size());
        
        // Influence effectiveness score
        double effectiveness = calculateInfluenceEffectiveness(nationId);
        stats.put("influenceEffectiveness", effectiveness);
        
        return stats;
    }

    /**
     * Calculate influence effectiveness for a nation.
     */
    private double calculateInfluenceEffectiveness(String nationId) {
        double effectiveness = 1.0;
        
        // Check for mod bonuses
        if (modAPI.hasCommunicationMods()) {
            effectiveness *= 1.2; // Communication mods improve propaganda effectiveness
        }
        
        // Check for technology bonuses
        if (plugin.getTechnologyTreeService() != null) {
            effectiveness *= plugin.getTechnologyTreeService().getBonus(nationId, "propagandaEffectiveness");
        }
        
        // Check for cultural bonuses
        if (plugin.getCultureService() != null) {
            // Simplified - in real implementation would be more complex
            effectiveness *= 1.1;
        }
        
        return effectiveness;
    }

    /**
     * Update influence levels periodically.
     */
    private void updateInfluence() {
        // Gradually decay all influences
        Map<String, Double> newInfluences = new HashMap<>();
        for (Map.Entry<String, Double> entry : nationInfluence.entrySet()) {
            double decayedValue = entry.getValue() * 0.99; // 1% decay per update
            if (decayedValue > 0.1) { // Don't store tiny values
                newInfluences.put(entry.getKey(), decayedValue);
            }
        }
        
        nationInfluence.clear();
        nationInfluence.putAll(newInfluences);
    }

    /**
     * Generate unique campaign ID.
     */
    private String generateCampaignId(String originId, String targetId) {
        return "propaganda_" + originId + "_to_" + targetId + "_" + System.currentTimeMillis();
    }

    /**
     * Counter-act foreign propaganda.
     */
    public String counterPropaganda(Player player, String foreignNationId) {
        try {
            UUID playerId = player.getUniqueId();
            String playerNationId = plugin.getPlayerDataManager().getNation(playerId);
            
            if (playerNationId == null) {
                return "Вы не состоите в нации";
            }
            
            // Check cooldown
            long currentTime = System.currentTimeMillis();
            Long lastUse = playerPropagandaCooldowns.get(playerId);
            if (lastUse != null && (currentTime - lastUse) < 60000) { // 1 minute cooldown for counter
                return "Подождите перед следующей контр-пропагандой";
            }
            
            // Calculate counter-influence
            double counterInfluence = calculateCounterInfluence(player, foreignNationId);
            
            // Reduce influence from foreign nation
            String influenceKey = foreignNationId + "_to_" + playerNationId;
            double currentInfluence = nationInfluence.getOrDefault(influenceKey, 0.0);
            double newInfluence = Math.max(0, currentInfluence - counterInfluence);
            
            nationInfluence.put(influenceKey, newInfluence);
            
            // Increase own influence as a defensive measure
            String reverseKey = playerNationId + "_to_" + foreignNationId;
            double reverseInfluence = nationInfluence.getOrDefault(reverseKey, 0.0);
            nationInfluence.put(reverseKey, reverseInfluence + (counterInfluence * 0.3));
            
            // Update cooldown
            playerPropagandaCooldowns.put(playerId, currentTime);
            
            player.sendMessage("§c[Контр-пропаганда] §fВы боретесь с влиянием нации §e" + 
                              foreignNationId + "§f. Снижение влияния: §6" + counterInfluence);
            
            return "✓ Контр-пропаганда применена!";
        } catch (Exception e) {
            plugin.getLogger().severe("Error applying counter propaganda: " + e.getMessage());
            return "Ошибка при применении контр-пропаганды: " + e.getMessage();
        }
    }

    /**
     * Calculate counter-propaganda effectiveness.
     */
    private double calculateCounterInfluence(Player player, String foreignNationId) {
        double effectiveness = 0.5; // Base counter-effectiveness
        
        // Check for mod items that can help counter (armor, books, etc.)
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String modId = modAPI.detectModFromItem(item);
                if (modId != null) {
                    effectiveness += 0.2; // Mod items help with counter-propaganda
                }
            }
        }
        
        // Check for technology bonuses
        String playerNationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
        if (playerNationId != null && plugin.getTechnologyTreeService() != null) {
            effectiveness *= plugin.getTechnologyTreeService().getBonus(playerNationId, "counterPropaganda");
        }
        
        return Math.min(2.0, effectiveness); // Cap at 2.0
    }

    // Data classes for propaganda structures
    
    public static class PropagandaCampaign {
        public final String campaignId;
        public final String originNationId;
        public final String targetNationId;
        public final String title;
        public final String message;
        public final int duration;
        public final long startTime;
        
        public PropagandaCampaign(String campaignId, String originNationId, String targetNationId,
                                String title, String message, int duration, long startTime) {
            this.campaignId = campaignId;
            this.originNationId = originNationId;
            this.targetNationId = targetNationId;
            this.title = title;
            this.message = message;
            this.duration = duration;
            this.startTime = startTime;
        }
    }
    
    public static class PropagandaMessage {
        public final String messageId;
        public final String originNationId;
        public final String content;
        public final ItemStack itemUsed;
        public final long timestamp;
        
        public PropagandaMessage(String messageId, String originNationId, String content,
                               ItemStack itemUsed, long timestamp) {
            this.messageId = messageId;
            this.originNationId = originNationId;
            this.content = content;
            this.itemUsed = itemUsed;
            this.timestamp = timestamp;
        }
    }
}