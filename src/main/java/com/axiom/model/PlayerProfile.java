package com.axiom.model;

import java.util.UUID;

/**
 * Player profile kept by AXIOM for linking players to nations and stats.
 */
public class PlayerProfile {
    private UUID uuid;
    private String lastKnownName;
    private String nationId; // may be null
    private String role; // Nation.Role name
    private double balance;
    private String religion; // id
    private long lastTaxPaid;
    private boolean pvpEnabled;

    public PlayerProfile() {}

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.lastKnownName = name;
    }

    public UUID getUuid() { return uuid; }
    public String getLastKnownName() { return lastKnownName; }
    public void setLastKnownName(String n) { this.lastKnownName = n; }
    public String getNationId() { return nationId; }
    public void setNationId(String nationId) { this.nationId = nationId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }
    public long getLastTaxPaid() { return lastTaxPaid; }
    public void setLastTaxPaid(long lastTaxPaid) { this.lastTaxPaid = lastTaxPaid; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
}


