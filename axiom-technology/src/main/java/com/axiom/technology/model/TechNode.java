package com.axiom.technology.model;

import java.util.*;

/**
 * Rich Technology Node for UI and Logic.
 */
public class TechNode {
    // Basic Info
    public String id;
    public String name;
    public String iconItem; // e.g. "minecraft:iron_sword"
    public String shortDescription;
    public String deepDescription;
    
    // Requirements
    public int cost;
    public List<String> requirements = new ArrayList<>();
    public String type; // "military", "economy", "science"
    
    // Rewards / Unlocks
    public List<String> unlockedRecipes = new ArrayList<>(); // List of recipe IDs
    public Map<String, Double> statModifiers = new HashMap<>(); // "minecraft:iron_sword.damage" -> 2.0
    
    public TechNode(String id, String name, String icon, int cost, String type) {
        this.id = id;
        this.name = name;
        this.iconItem = icon;
        this.cost = cost;
        this.type = type;
    }
    
    public void addRecipeUnlock(String recipeId) {
        unlockedRecipes.add(recipeId);
    }
    
    public void addStatModifier(String item, String attribute, double value) {
        statModifiers.put(item + "." + attribute, value);
    }
}
