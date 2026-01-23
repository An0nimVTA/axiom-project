package com.axiom.model;

import java.util.ArrayList;
import java.util.List;

// NOTE: This file was recreated from scratch to remove potential invisible characters causing compilation errors.

/**
 * Модель мода для модпака
 */
public class Mod {
    private String id;
    private String name;
    private String description;
    private String version;
    private String author;
    private String category;
    private String compatibility;
    private List<String> dependencies;
    private List<String> conflicts;
    private boolean isEnabled;
    private boolean isRequired;
    private String serverType;
    private String requiredTechnology;
    private int resourceCost;
    private boolean isHidden;
    
    public Mod(String id, String name, String description, String version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.dependencies = new ArrayList<>();
        this.conflicts = new ArrayList<>();
        this.isEnabled = false;
        this.isRequired = false;
        this.resourceCost = 0;
        this.isHidden = false;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getCompatibility() { return compatibility; }
    public void setCompatibility(String compatibility) { this.compatibility = compatibility; }
    
    public List<String> getDependencies() { return new ArrayList<>(dependencies); }
    public void addDependency(String dependency) { this.dependencies.add(dependency); }
    public void removeDependency(String dependency) { this.dependencies.remove(dependency); }
    
    public List<String> getConflicts() { return new ArrayList<>(conflicts); }
    public void addConflict(String conflict) { this.conflicts.add(conflict); }
    public void removeConflict(String conflict) { this.conflicts.remove(conflict); }
    
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { this.isEnabled = enabled; }
    
    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { this.isRequired = required; }
    
    public String getServerType() { return serverType; }
    public void setServerType(String serverType) { this.serverType = serverType; }
    
    public String getRequiredTechnology() { return requiredTechnology; }
    public void setRequiredTechnology(String requiredTechnology) { this.requiredTechnology = requiredTechnology; }
    
    public int getResourceCost() { return resourceCost; }
    public void setResourceCost(int resourceCost) { this.resourceCost = resourceCost; }
    
    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { this.isHidden = hidden; }
}