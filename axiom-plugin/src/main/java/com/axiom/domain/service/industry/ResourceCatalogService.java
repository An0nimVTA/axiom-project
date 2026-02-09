package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical resource catalog and processing recipes for economy/industry systems.
 */
public class ResourceCatalogService {
    public static class ResourceDefinition {
        public String id;
        public String category;
        public double basePrice;
        public boolean tradable;
        public boolean scarcity;
        public boolean discoverable;
        public double decayPerHour;
        public double perCitizenNeed;
        public double volatility;
        public List<String> aliases = new ArrayList<>();
    }

    public static class ProcessingRecipe {
        public String outputResource;
        public Map<String, Double> inputs = new HashMap<>();
        public double outputAmount;
        public double baseRatePerHour;
        public double baseEfficiency;
    }

    private static final double DEFAULT_BASE_PRICE = 10.0;
    private static final double DEFAULT_VOLATILITY = 0.1;
    private static final double DEFAULT_NEED = 10.0;

    private final AXIOM plugin;
    private final Map<String, ResourceDefinition> resources = new HashMap<>();
    private final Map<String, ProcessingRecipe> recipes = new HashMap<>();
    private final Map<String, String> aliasToId = new HashMap<>();

    public ResourceCatalogService(AXIOM plugin) {
        this.plugin = plugin;
        loadCatalog();
    }

    public Map<String, ResourceDefinition> getResources() {
        return Collections.unmodifiableMap(resources);
    }

    public Set<String> getTradableResources() {
        return filterResources(def -> def.tradable);
    }

    public Set<String> getScarcityResources() {
        return filterResources(def -> def.scarcity);
    }

    public Set<String> getDiscoveryResources() {
        return filterResources(def -> def.discoverable);
    }

    public Set<String> getDecayResources() {
        return filterResources(def -> def.decayPerHour > 0);
    }

    public ResourceDefinition getResource(String id) {
        if (id == null) return null;
        return resources.get(id);
    }

    public boolean isKnownResource(String id) {
        if (id == null) return false;
        return resources.containsKey(id);
    }

    public String resolveResourceId(String id) {
        if (id == null) return null;
        if (resources.containsKey(id)) return id;
        return aliasToId.getOrDefault(id, id);
    }

    public double getBasePrice(String id) {
        ResourceDefinition def = getResource(resolveResourceId(id));
        return def != null ? def.basePrice : DEFAULT_BASE_PRICE;
    }

    public double getVolatility(String id) {
        ResourceDefinition def = getResource(resolveResourceId(id));
        return def != null && def.volatility > 0 ? def.volatility : DEFAULT_VOLATILITY;
    }

    public double getDecayPerHour(String id) {
        ResourceDefinition def = getResource(resolveResourceId(id));
        return def != null ? def.decayPerHour : 0.0;
    }

    public double getPerCitizenNeed(String id) {
        ResourceDefinition def = getResource(resolveResourceId(id));
        return def != null && def.perCitizenNeed > 0 ? def.perCitizenNeed : DEFAULT_NEED;
    }

    public ProcessingRecipe getProcessingRecipe(String outputResource) {
        if (outputResource == null) return null;
        return recipes.get(outputResource);
    }

    public Map<String, ProcessingRecipe> getProcessingRecipes() {
        return Collections.unmodifiableMap(recipes);
    }

    public List<String> validateRecipes() {
        List<String> issues = new ArrayList<>();
        Set<String> invalid = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String output : new ArrayList<>(recipes.keySet())) {
            detectCycle(output, visiting, visited, invalid);
        }
        for (String output : invalid) {
            issues.add("cycle:" + output);
            recipes.remove(output);
        }
        return issues;
    }

    private void loadCatalog() {
        File catalogFile = new File(plugin.getDataFolder(), "resources.yml");
        if (!catalogFile.exists()) {
            try {
                plugin.saveResource("resources.yml", false);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to save resources.yml: " + ex.getMessage());
            }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(catalogFile);
        loadResources(cfg);
        loadRecipes(cfg);
        List<String> issues = validateRecipes();
        if (!issues.isEmpty()) {
            plugin.getLogger().warning("Resource catalog validation issues: " + issues);
        }
    }

    private void loadResources(FileConfiguration cfg) {
        resources.clear();
        aliasToId.clear();
        ConfigurationSection section = cfg.getConfigurationSection("resources");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection res = section.getConfigurationSection(id);
            if (res == null) continue;
            ResourceDefinition def = new ResourceDefinition();
            def.id = id;
            def.category = res.getString("category", "misc");
            def.basePrice = res.getDouble("basePrice", DEFAULT_BASE_PRICE);
            def.tradable = res.getBoolean("tradable", false);
            def.scarcity = res.getBoolean("scarcity", false);
            def.discoverable = res.getBoolean("discoverable", false);
            def.decayPerHour = res.getDouble("decayPerHour", 0.0);
            def.perCitizenNeed = res.getDouble("perCitizenNeed", DEFAULT_NEED);
            def.volatility = res.getDouble("volatility", DEFAULT_VOLATILITY);
            def.aliases = res.getStringList("aliases");
            resources.put(id, def);
            for (String alias : def.aliases) {
                if (alias != null && !alias.isBlank()) {
                    aliasToId.put(alias, id);
                }
            }
        }
    }

    private void loadRecipes(FileConfiguration cfg) {
        recipes.clear();
        ConfigurationSection section = cfg.getConfigurationSection("processing");
        if (section == null) {
            return;
        }
        for (String output : section.getKeys(false)) {
            ConfigurationSection recipeSec = section.getConfigurationSection(output);
            if (recipeSec == null) continue;
            if (!resources.containsKey(output)) {
                plugin.getLogger().warning("Processing recipe output not in catalog: " + output);
                continue;
            }
            ProcessingRecipe recipe = new ProcessingRecipe();
            recipe.outputResource = output;
            recipe.outputAmount = recipeSec.getDouble("outputAmount", 1.0);
            recipe.baseRatePerHour = recipeSec.getDouble("baseRatePerHour", 10.0);
            recipe.baseEfficiency = recipeSec.getDouble("baseEfficiency", 80.0);
            ConfigurationSection inputs = recipeSec.getConfigurationSection("inputs");
            if (inputs != null) {
                for (String inputId : inputs.getKeys(false)) {
                    double amount = inputs.getDouble(inputId, 0.0);
                    if (!resources.containsKey(inputId)) {
                        plugin.getLogger().warning("Processing recipe input not in catalog: " + inputId);
                        continue;
                    }
                    if (amount > 0) {
                        recipe.inputs.put(inputId, amount);
                    }
                }
            } else if (recipeSec.contains("input")) {
                String inputId = recipeSec.getString("input");
                double amount = recipeSec.getDouble("inputAmount", 1.0);
                if (inputId != null && resources.containsKey(inputId) && amount > 0) {
                    recipe.inputs.put(inputId, amount);
                }
            }
            if (recipe.inputs.isEmpty()) {
                plugin.getLogger().warning("Processing recipe has no inputs: " + output);
                continue;
            }
            recipes.put(output, recipe);
        }
    }

    private Set<String> filterResources(java.util.function.Predicate<ResourceDefinition> predicate) {
        Set<String> result = new HashSet<>();
        for (ResourceDefinition def : resources.values()) {
            if (def != null && predicate.test(def)) {
                result.add(def.id);
            }
        }
        return result;
    }

    private boolean detectCycle(String output,
                                Set<String> visiting,
                                Set<String> visited,
                                Set<String> invalid) {
        if (visited.contains(output)) {
            return invalid.contains(output);
        }
        if (visiting.contains(output)) {
            invalid.add(output);
            return true;
        }
        visiting.add(output);
        ProcessingRecipe recipe = recipes.get(output);
        if (recipe != null) {
            for (String input : recipe.inputs.keySet()) {
                if (recipes.containsKey(input)) {
                    if (detectCycle(input, visiting, visited, invalid)) {
                        invalid.add(output);
                    }
                }
            }
        }
        visiting.remove(output);
        visited.add(output);
        return invalid.contains(output);
    }
}
