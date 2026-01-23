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
 * Service for crowd-funding system using mod resources.
 * Allows nations to fund projects using both vanilla and mod items/resources.
 */
public class CrowdFundingService {
    private final AXIOM plugin;
    private final ModIntegrationAPI modAPI;
    private final EconomyService economyService;
    private final ResourceService resourceService;
    
    private final Map<String, FundingProject> activeProjects = new HashMap<>();
    private final Map<String, Map<UUID, Double>> projectContributions = new HashMap<>();
    private final Map<String, List<FundingReward>> projectRewards = new HashMap<>();

    public CrowdFundingService(AXIOM plugin) {
        this.plugin = plugin;
        this.modAPI = plugin.getModIntegrationAPI();
        this.economyService = plugin.getEconomyService();
        this.resourceService = plugin.getResourceService();
        
        // Schedule periodic project updates
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateProjects, 0, 20 * 60); // every minute
    }

    /**
     * Create a new crowd-funding project.
     */
    public String createProject(String nationId, String title, String description, 
                               double targetAmount, long duration, 
                               Map<String, Object> requiredResources,
                               List<FundingReward> rewards) {
        try {
            Nation nation = plugin.getNationManager().getNationById(nationId);
            if (nation == null) {
                return "Нация не найдена";
            }

            String projectId = generateProjectId(nationId);
            
            FundingProject project = new FundingProject(
                projectId,
                nationId,
                title,
                description,
                targetAmount,
                duration,
                System.currentTimeMillis(),
                requiredResources
            );
            
            activeProjects.put(projectId, project);
            projectContributions.put(projectId, new HashMap<>());
            projectRewards.put(projectId, rewards != null ? rewards : new ArrayList<>());
            
            plugin.getLogger().info("Created crowd-funding project: " + title + " for nation " + nationId);
            
            return "✓ Проект краудфандинга создан: " + title;
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating funding project: " + e.getMessage());
            return "Ошибка при создании проекта: " + e.getMessage();
        }
    }

    /**
     * Contribute to a project using money.
     */
    public String contributeMoney(String projectId, UUID playerId, double amount) {
        try {
            FundingProject project = activeProjects.get(projectId);
            if (project == null) {
                return "Проект не найден";
            }
            
            if (isProjectCompleted(projectId)) {
                return "Проект уже завершен";
            }
            
            // Check if player has enough money
            String playerNationId = plugin.getPlayerDataManager().getNation(playerId);
            if (playerNationId == null) {
                return "Вы не состоите в нации";
            }
            
            double currentTreasury = economyService.getTreasury(playerNationId);
            if (currentTreasury < amount) {
                return "Недостаточно средств";
            }
            
            // Process the contribution
            double currentTotal = project.currentAmount;
            double newTotal = currentTotal + amount;
            
            // Update project
            project.currentAmount = newTotal;
            
            // Record player contribution
            Map<UUID, Double> contributions = projectContributions.get(projectId);
            double playerContribution = contributions.getOrDefault(playerId, 0.0) + amount;
            contributions.put(playerId, playerContribution);
            
            // Deduct money from player's nation
            economyService.withdraw(playerNationId, amount);
            
            // Determine and award rewards
            String rewardMessage = awardRewards(projectId, playerId, amount);
            
            plugin.getLogger().info("Player " + playerId + " contributed " + amount + 
                                   " to project " + projectId + ". Total: " + newTotal);
            
            return "✓ Вклад в размере " + amount + " успешно внесён!" + rewardMessage;
        } catch (Exception e) {
            plugin.getLogger().severe("Error contributing money: " + e.getMessage());
            return "Ошибка при внесении вклада: " + e.getMessage();
        }
    }

    /**
     * Contribute to a project using resources (including mod resources).
     */
    public String contributeResources(String projectId, UUID playerId, String resourceType, double amount) {
        try {
            FundingProject project = activeProjects.get(projectId);
            if (project == null) {
                return "Проект не найден";
            }
            
            if (isProjectCompleted(projectId)) {
                return "Проект уже завершен";
            }
            
            // Check if this resource type is required for the project
            Map<String, Object> requiredResources = project.requiredResources;
            if (requiredResources != null && !requiredResources.containsKey(resourceType)) {
                return "Этот проект не нуждается в данном ресурсе";
            }
            
            String playerNationId = plugin.getPlayerDataManager().getNation(playerId);
            if (playerNationId == null) {
                return "Вы не состоите в нации";
            }
            
            // Check if nation has required resources
            Map<String, Double> nationResources = resourceService.getNationResources(playerNationId);
            double availableAmount = nationResources.getOrDefault(resourceType, 0.0);
            
            if (availableAmount < amount) {
                return "Недостаточно ресурсов: " + resourceType;
            }
            
            // Process the contribution
            double currentValue = project.resourceContributions.getOrDefault(resourceType, 0.0);
            double newValue = currentValue + amount;
            project.resourceContributions.put(resourceType, newValue);
            
            // Update nation resources
            resourceService.removeResource(playerNationId, resourceType, amount);
            
            // Record player contribution
            Map<UUID, Double> contributions = projectContributions.get(projectId);
            double playerContribution = contributions.getOrDefault(playerId, 0.0) + amount;
            contributions.put(playerId, playerContribution);
            
            // Calculate monetary equivalent for rewards
            double monetaryValue = calculateResourceValue(resourceType, amount);
            String rewardMessage = awardRewards(projectId, playerId, monetaryValue);
            
            plugin.getLogger().info("Player " + playerId + " contributed " + amount + " " + 
                                   resourceType + " to project " + projectId);
            
            return "✓ Вклад ресурсов (" + amount + " " + resourceType + ") успешно внесён!" + rewardMessage;
        } catch (Exception e) {
            plugin.getLogger().severe("Error contributing resources: " + e.getMessage());
            return "Ошибка при внесении ресурсов: " + e.getMessage();
        }
    }

    /**
     * Contribute to a project using mod items directly.
     */
    public String contributeModItem(String projectId, Player player, ItemStack itemStack) {
        try {
            FundingProject project = activeProjects.get(projectId);
            if (project == null) {
                return "Проект не найден";
            }
            
            if (isProjectCompleted(projectId)) {
                return "Проект уже завершен";
            }
            
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                return "Неверный предмет";
            }
            
            // Detect mod from item
            String modId = modAPI.detectModFromItem(itemStack);
            if (modId == null) {
                return "Предмет не из модов или не поддерживается";
            }
            
            String resourceType = "mod_item_" + modId;
            double amount = itemStack.getAmount();
            double value = calculateModItemValue(modId, itemStack);
            
            String playerNationId = plugin.getPlayerDataManager().getNation(player.getUniqueId());
            if (playerNationId == null) {
                return "Вы не состоите в нации";
            }
            
            // Process the contribution
            double currentValue = project.resourceContributions.getOrDefault(resourceType, 0.0);
            double newValue = currentValue + value;
            project.resourceContributions.put(resourceType, newValue);
            
            // Remove item from player inventory
            player.getInventory().removeItem(itemStack);
            
            // Record player contribution
            Map<UUID, Double> contributions = projectContributions.get(projectId);
            double playerContribution = contributions.getOrDefault(player.getUniqueId(), 0.0) + value;
            contributions.put(player.getUniqueId(), playerContribution);
            
            String rewardMessage = awardRewards(projectId, player.getUniqueId(), value);
            
            plugin.getLogger().info("Player " + player.getName() + " contributed mod item " + 
                                   itemStack.getType() + " (from mod: " + modId + ") to project " + projectId);
            
            return "✓ Вклад мод-предмета успешно внесён! Оценочная стоимость: " + value + rewardMessage;
        } catch (Exception e) {
            plugin.getLogger().severe("Error contributing mod item: " + e.getMessage());
            return "Ошибка при внесении мод-предмета: " + e.getMessage();
        }
    }

    /**
     * Award rewards to contributor.
     */
    private String awardRewards(String projectId, UUID playerId, double contributionAmount) {
        List<FundingReward> rewards = projectRewards.get(projectId);
        if (rewards == null || rewards.isEmpty()) {
            return "";
        }
        
        StringBuilder rewardMessage = new StringBuilder();
        rewardMessage.append("\nНаграды: ");
        
        // Find rewards based on contribution tiers
        double rewardTier = 0.0;
        String rewardDescription = "Участник";
        
        for (FundingReward reward : rewards) {
            if (contributionAmount >= reward.tierAmount) {
                if (reward.tierAmount > rewardTier) {
                    rewardTier = reward.tierAmount;
                    rewardDescription = reward.description;
                }
            }
        }
        
        rewardMessage.append(rewardDescription);
        return rewardMessage.toString();
    }

    /**
     * Calculate equivalent monetary value for resources.
     */
    private double calculateResourceValue(String resourceType, double amount) {
        // Base values - in a real implementation these would be more dynamic
        double baseValue = 1.0;
        
        // Higher value for rare mod resources
        if (resourceType.contains("mod_")) {
            baseValue = 5.0; // Mod resources are more valuable
        } else if (resourceType.contains("rare")) {
            baseValue = 3.0;
        }
        
        return amount * baseValue;
    }

    /**
     * Calculate value for mod items.
     */
    private double calculateModItemValue(String modId, ItemStack itemStack) {
        double baseValue = 10.0; // Base value for mod items
        
        // Different mods have different values
        switch (modId.toLowerCase()) {
            case "tacz":
            case "pointblank":
            case "ballistix":
                baseValue = 50.0; // Weapons are valuable
                break;
            case "appliedenergistics2":
                baseValue = 25.0; // AE2 items are useful
                break;
            case "immersiveengineering":
                baseValue = 20.0; // Industrial items
                break;
            case "industrialupgrade":
                baseValue = 30.0; // Industrial upgrades
                break;
            default:
                baseValue = 15.0; // Default for other mods
        }
        
        return baseValue * itemStack.getAmount();
    }

    /**
     * Get project status information.
     */
    public Map<String, Object> getProjectStatus(String projectId) {
        FundingProject project = activeProjects.get(projectId);
        if (project == null) {
            return null;
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("projectId", project.projectId);
        status.put("title", project.title);
        status.put("description", project.description);
        status.put("nationId", project.nationId);
        status.put("targetAmount", project.targetAmount);
        status.put("currentAmount", project.currentAmount);
        status.put("resourceContributions", new HashMap<>(project.resourceContributions));
        status.put("progressPercentage", (project.currentAmount / project.targetAmount) * 100);
        status.put("timeRemaining", (project.startTime + project.duration) - System.currentTimeMillis());
        status.put("isCompleted", isProjectCompleted(projectId));
        status.put("contributors", projectContributions.get(projectId).size());
        status.put("requiredResources", project.requiredResources);
        
        return status;
    }

    /**
     * Get all active projects for a nation.
     */
    public List<Map<String, Object>> getNationProjects(String nationId) {
        List<Map<String, Object>> projects = new ArrayList<>();
        
        for (FundingProject project : activeProjects.values()) {
            if (project.nationId.equals(nationId)) {
                projects.add(getProjectStatus(project.projectId));
            }
        }
        
        return projects;
    }

    /**
     * Get projects a player has contributed to.
     */
    public List<Map<String, Object>> getPlayerContributions(UUID playerId) {
        List<Map<String, Object>> contributions = new ArrayList<>();
        
        for (Map.Entry<String, Map<UUID, Double>> entry : projectContributions.entrySet()) {
            String projectId = entry.getKey();
            Double amount = entry.getValue().get(playerId);
            
            if (amount != null && amount > 0) {
                Map<String, Object> projectInfo = getProjectStatus(projectId);
                if (projectInfo != null) {
                    projectInfo.put("playerContribution", amount);
                    contributions.add(projectInfo);
                }
            }
        }
        
        return contributions;
    }

    /**
     * Check if a project is completed.
     */
    public boolean isProjectCompleted(String projectId) {
        FundingProject project = activeProjects.get(projectId);
        if (project == null) {
            return true;
        }
        
        // Check if time is up
        long timeRemaining = (project.startTime + project.duration) - System.currentTimeMillis();
        if (timeRemaining <= 0) {
            return true;
        }
        
        // Check if funding goal is reached
        if (project.currentAmount >= project.targetAmount) {
            return true;
        }
        
        return false;
    }

    /**
     * Process completed projects.
     */
    private void updateProjects() {
        List<String> completedProjects = new ArrayList<>();
        
        for (Map.Entry<String, FundingProject> entry : activeProjects.entrySet()) {
            String projectId = entry.getKey();
            FundingProject project = entry.getValue();
            
            if (isProjectCompleted(projectId)) {
                completedProjects.add(projectId);
                
                // Process project completion
                processProjectCompletion(projectId, project);
            }
        }
        
        // Remove completed projects
        for (String projectId : completedProjects) {
            activeProjects.remove(projectId);
            projectContributions.remove(projectId);
            projectRewards.remove(projectId);
        }
    }

    /**
     * Process project completion (success or failure).
     */
    private void processProjectCompletion(String projectId, FundingProject project) {
        boolean success = project.currentAmount >= project.targetAmount;
        
        plugin.getLogger().info("Project " + projectId + " completed. Success: " + success + 
                               ", Amount: " + project.currentAmount + "/" + project.targetAmount);
        
        if (success) {
            // Project succeeded - transfer funds/resources to nation
            Nation nation = plugin.getNationManager().getNationById(project.nationId);
            if (nation != null) {
                // Add funds to nation treasury
                economyService.deposit(project.nationId, project.currentAmount);
                
                plugin.getLogger().info("Project success! Added " + project.currentAmount + 
                                       " to nation " + project.nationId);
            }
        } else {
            // Project failed - return contributions to contributors proportionally
            returnContributions(projectId, project);
        }
    }

    /**
     * Return contributions when project fails.
     */
    private void returnContributions(String projectId, FundingProject project) {
        Map<UUID, Double> contributions = projectContributions.get(projectId);
        if (contributions == null) {
            return;
        }
        
        // Return proportional amounts to contributors
        for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
            UUID playerId = entry.getKey();
            Double amount = entry.getValue();
            
            String playerNationId = plugin.getPlayerDataManager().getNation(playerId);
            if (playerNationId != null) {
                economyService.deposit(playerNationId, amount);
                
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.sendMessage("§e[Краудфандинг] §fВаш вклад в проект \"" + 
                                      project.title + "\" возвращён: §6" + amount);
                }
            }
        }
    }

    /**
     * Generate unique project ID.
     */
    private String generateProjectId(String nationId) {
        return "funding_" + nationId + "_" + System.currentTimeMillis();
    }

    /**
     * Get all active projects across all nations.
     */
    public List<Map<String, Object>> getAllActiveProjects() {
        List<Map<String, Object>> allProjects = new ArrayList<>();
        
        for (String projectId : activeProjects.keySet()) {
            Map<String, Object> status = getProjectStatus(projectId);
            if (status != null) {
                allProjects.add(status);
            }
        }
        
        return allProjects;
    }

    /**
     * Cancel a project (before completion).
     */
    public String cancelProject(String projectId, String nationId) {
        FundingProject project = activeProjects.get(projectId);
        if (project == null) {
            return "Проект не найден";
        }
        
        if (!project.nationId.equals(nationId)) {
            return "Вы не можете отменить этот проект";
        }
        
        if (isProjectCompleted(projectId)) {
            return "Проект уже завершен";
        }
        
        // Return contributions to contributors
        returnContributions(projectId, project);
        
        // Remove the project
        activeProjects.remove(projectId);
        projectContributions.remove(projectId);
        projectRewards.remove(projectId);
        
        return "✓ Проект отменён, вклады возвращены участникам";
    }

    // Data classes for crowd-funding structures
    
    public static class FundingProject {
        public final String projectId;
        public final String nationId;
        public final String title;
        public final String description;
        public final double targetAmount;
        public final long duration;
        public final long startTime;
        public final Map<String, Object> requiredResources;
        
        public double currentAmount = 0.0;
        public Map<String, Double> resourceContributions = new HashMap<>();
        
        public FundingProject(String projectId, String nationId, String title, String description,
                             double targetAmount, long duration, long startTime,
                             Map<String, Object> requiredResources) {
            this.projectId = projectId;
            this.nationId = nationId;
            this.title = title;
            this.description = description;
            this.targetAmount = targetAmount;
            this.duration = duration;
            this.startTime = startTime;
            this.requiredResources = requiredResources != null ? requiredResources : new HashMap<>();
        }
    }
    
    public static class FundingReward {
        public final double tierAmount;
        public final String description;
        public final Map<String, Object> rewardContent;
        
        public FundingReward(double tierAmount, String description, Map<String, Object> rewardContent) {
            this.tierAmount = tierAmount;
            this.description = description;
            this.rewardContent = rewardContent != null ? rewardContent : new HashMap<>();
        }
    }
}