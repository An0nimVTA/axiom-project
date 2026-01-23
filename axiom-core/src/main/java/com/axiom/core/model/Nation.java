package com.axiom.core.model;

import java.util.*;

/**
 * Represents a nation with members, roles, treasury and claimed chunks.
 * Core model shared by all modules.
 */
public class Nation {
    public enum Role { LEADER, MINISTER, GENERAL, GOVERNOR, CITIZEN }

    private String id; // stable identifier
    private String name;
    private UUID leader;
    private Set<UUID> citizens = new HashSet<>();
    private Map<UUID, Role> roles = new HashMap<>();
    private double treasury;
    private String currencyCode;
    private double exchangeRateToAXC = 1.0; // nation currency -> AXC
    private String capitalChunkStr; // world:x:z
    private final Set<String> claimedChunkKeys = new HashSet<>(); // world:x:z
    private String motto; // up to 64 chars
    private String flagIconMaterial = "BLUE_BANNER"; // default
    private Map<String, String> tabIcons = new HashMap<>(); // section -> material name
    private Map<String, Integer> reputation = new HashMap<>(); // nationId -> -100..+100
    private Set<String> pendingAlliance = new HashSet<>(); // incoming/outgoing requests
    private List<String> history = new ArrayList<>();
    private double budgetMilitary = 0.0;
    private double budgetHealth = 0.0;
    private double budgetEducation = 0.0;
    private double inflation = 0.0;
    private int taxRate = 10;
    private Set<String> allies = new HashSet<>();
    private Set<String> enemies = new HashSet<>();
    private String governmentType = "republic";

    public Nation() {}

    public Nation(String id, String name, UUID leader, String currencyCode, double treasury) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.currencyCode = currencyCode;
        this.treasury = treasury;
        this.citizens.add(leader);
        this.roles.put(leader, Role.LEADER);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID v) { this.leader = v; }
    
    public Set<UUID> getCitizens() { return citizens; }
    public Map<UUID, Role> getRoles() { return roles; }
    
    // Helper method to get officer IDs (Ministers, Generals, Governors)
    public Set<UUID> getOfficerIds() {
        Set<UUID> officers = new HashSet<>();
        for (Map.Entry<UUID, Role> entry : roles.entrySet()) {
            if (entry.getValue() != Role.CITIZEN && entry.getValue() != Role.LEADER) {
                officers.add(entry.getKey());
            }
        }
        return officers;
    }
    
    public double getTreasury() { return treasury; }
    public void setTreasury(double v) { this.treasury = v; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String v) { this.currencyCode = v; }
    public double getExchangeRateToAXC() { return exchangeRateToAXC; }
    public void setExchangeRateToAXC(double v) { this.exchangeRateToAXC = v; }
    
    public String getCapitalChunkStr() { return capitalChunkStr; }
    public void setCapitalChunkStr(String key) { this.capitalChunkStr = key; }
    public Set<String> getClaimedChunkKeys() { return claimedChunkKeys; }
    
    public String getMotto() { return motto; }
    public void setMotto(String motto) { this.motto = motto; }
    public String getFlagIconMaterial() { return flagIconMaterial; }
    public void setFlagIconMaterial(String flagIconMaterial) { this.flagIconMaterial = flagIconMaterial; }
    
    public Map<String, String> getTabIcons() { return tabIcons; }
    public Map<String, Integer> getReputation() { return reputation; }
    public Set<String> getPendingAlliance() { return pendingAlliance; }
    public List<String> getHistory() { return history; }
    
    public double getBudgetMilitary() { return budgetMilitary; }
    public void setBudgetMilitary(double v) { this.budgetMilitary = v; }
    public double getBudgetHealth() { return budgetHealth; }
    public void setBudgetHealth(double v) { this.budgetHealth = v; }
    public double getBudgetEducation() { return budgetEducation; }
    public void setBudgetEducation(double v) { this.budgetEducation = v; }
    
    public double getInflation() { return inflation; }
    public void setInflation(double v) { this.inflation = v; }
    public int getTaxRate() { return taxRate; }
    public void setTaxRate(int v) { this.taxRate = v; }
    
    public Set<String> getAllies() { return allies; }
    public Set<String> getEnemies() { return enemies; }
    
    public String getGovernmentType() { return governmentType; }
    public void setGovernmentType(String v) { this.governmentType = v; }

    public boolean isMember(UUID uuid) { return citizens.contains(uuid); }

    public Role getRole(UUID uuid) { return roles.getOrDefault(uuid, Role.CITIZEN); }
}
