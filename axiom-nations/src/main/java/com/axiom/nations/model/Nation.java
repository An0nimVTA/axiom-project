package com.axiom.nations.model;

import java.util.List;
import java.util.Map;

/**
 * Модель данных нации
 */
public class Nation {
    private String id;
    private String name;
    private String leader;
    private double treasury;
    private int citizens;
    private List<String> cities;
    private Map<String, Object> modifiers;
    private long createdTimestamp;
    
    public Nation() {
        this.createdTimestamp = System.currentTimeMillis();
        this.cities = new java.util.ArrayList<>();
        this.modifiers = new java.util.HashMap<>();
    }
    
    public Nation(String id, String name, String leader) {
        this();
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.treasury = 0.0;
        this.citizens = 0;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }
    
    public double getTreasury() { return treasury; }
    public void setTreasury(double treasury) { this.treasury = treasury; }
    
    public int getCitizens() { return citizens; }
    public void setCitizens(int citizens) { this.citizens = citizens; }
    
    public List<String> getCities() { return cities; }
    public void setCities(List<String> cities) { this.cities = cities; }
    
    public Map<String, Object> getModifiers() { return modifiers; }
    public void setModifiers(Map<String, Object> modifiers) { this.modifiers = modifiers; }
    
    public long getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(long createdTimestamp) { this.createdTimestamp = createdTimestamp; }
    
    public void addCity(String city) {
        if (!cities.contains(city)) {
            cities.add(city);
        }
    }
    
    public void removeCity(String city) {
        cities.remove(city);
    }
    
    public void addModifier(String key, Object value) {
        modifiers.put(key, value);
    }
    
    public Object getModifier(String key) {
        return modifiers.get(key);
    }
}