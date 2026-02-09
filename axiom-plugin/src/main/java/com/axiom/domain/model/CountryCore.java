package com.axiom.domain.model;

import org.bukkit.Location;

import java.util.*;

/**
 * Класс, представляющий ядро страны (нации)
 */
public class CountryCore {
    private String id;
    private String nationName;
    private Location capitalLocation; // координаты столицы
    private int stability; // стабильность страны
    private int maxStability; // максимальная стабильность
    private int militaryStrength; // военная сила
    private List<CityCore> subordinateCores; // подчиненные ядра городов
    private Map<String, Object> nationalDefenses; // национальные оборонительные системы
    private List<Location> strategicLocations; // стратегические позиции
    private int governmentBuildingHealth; // здоровье правительственных построек
    private int maxGovernmentBuildingHealth;
    
    public CountryCore(String id, String nationName, Location capitalLocation) {
        this.id = id;
        this.nationName = nationName;
        this.capitalLocation = capitalLocation;
        this.maxStability = 10000;
        this.stability = this.maxStability;
        this.militaryStrength = 500;
        this.subordinateCores = new ArrayList<>();
        this.nationalDefenses = new HashMap<>();
        this.strategicLocations = new ArrayList<>();
        this.maxGovernmentBuildingHealth = 5000;
        this.governmentBuildingHealth = this.maxGovernmentBuildingHealth;
    }
    
    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNationName() { return nationName; }
    public void setNationName(String nationName) { this.nationName = nationName; }
    
    public Location getCapitalLocation() { return capitalLocation; }
    public void setCapitalLocation(Location capitalLocation) { this.capitalLocation = capitalLocation; }
    
    public int getStability() { return stability; }
    public void setStability(int stability) { this.stability = Math.max(0, Math.min(stability, maxStability)); }
    
    public int getMaxStability() { return maxStability; }
    public void setMaxStability(int maxStability) { this.maxStability = maxStability; }
    
    public int getMilitaryStrength() { return militaryStrength; }
    public void setMilitaryStrength(int militaryStrength) { this.militaryStrength = militaryStrength; }
    
    public List<CityCore> getSubordinateCores() { return new ArrayList<>(subordinateCores); }
    public void addSubordinateCore(CityCore core) {
        if (!subordinateCores.contains(core)) {
            subordinateCores.add(core);
        }
    }
    public void removeSubordinateCore(CityCore core) {
        subordinateCores.remove(core);
    }
    
    public Map<String, Object> getNationalDefenses() { return new HashMap<>(nationalDefenses); }
    public void setNationalDefense(String defenseType, Object value) {
        nationalDefenses.put(defenseType, value);
    }
    
    public List<Location> getStrategicLocations() { return new ArrayList<>(strategicLocations); }
    public void addStrategicLocation(Location location) {
        if (!strategicLocations.contains(location)) {
            strategicLocations.add(location);
        }
    }
    public void removeStrategicLocation(Location location) {
        strategicLocations.remove(location);
    }
    
    public int getGovernmentBuildingHealth() { return governmentBuildingHealth; }
    public void setGovernmentBuildingHealth(int health) { 
        this.governmentBuildingHealth = Math.max(0, Math.min(health, maxGovernmentBuildingHealth)); 
    }
    
    public int getMaxGovernmentBuildingHealth() { return maxGovernmentBuildingHealth; }
    public void setMaxGovernmentBuildingHealth(int maxHealth) { 
        this.maxGovernmentBuildingHealth = maxHealth; 
    }
    
    /**
     * Повреждение страны
     */
    public boolean damageCountry(int damageAmount) {
        // Сначала повреждается стабильность
        this.stability -= damageAmount;
        
        // Если стабильность падает ниже порога, начинается повреждение правительственных построек
        if (stability <= maxStability * 0.3) {
            this.governmentBuildingHealth -= damageAmount / 2; // медленнее, чем стабильность
        }
        
        // Проверяем, разрушено ли ядро страны
        if (this.stability <= 0 && this.governmentBuildingHealth <= 0) {
            return true; // ядро страны разрушено
        }
        
        return false;
    }
    
    /**
     * Восстановление страны
     */
    public void restoreStability(int restoreAmount) {
        this.stability = Math.min(this.stability + restoreAmount, this.maxStability);
        if (this.stability > maxStability * 0.3) {
            // Когда стабильность восстанавливается, начинает восстанавливаться и Government Building
            this.governmentBuildingHealth = Math.min(
                this.governmentBuildingHealth + restoreAmount / 2, 
                this.maxGovernmentBuildingHealth
            );
        }
    }
    
    /**
     * Проверка, под угрозой ли страна
     */
    public boolean isUnderThreat() {
        return stability < maxStability * 0.6;
    }
    
    /**
     * Проверка, контролируется ли страна
     */
    public boolean isFullyControlled() {
        // Проверяем, контролирует ли правительство все подчиненные ядра
        for (CityCore core : subordinateCores) {
            if (core.getHealth() <= core.getMaxHealth() * 0.1) {
                // Если ядро города в плохом состоянии, страна не полностью контролируется
                return false;
            }
        }
        return governmentBuildingHealth > maxGovernmentBuildingHealth * 0.5 && stability > maxStability * 0.5;
    }
    
    /**
     * Подсчет эффективности ядра страны
     */
    public double getCountryCoreEfficiency() {
        double stabilityRatio = (double)stability / maxStability;
        double buildingRatio = (double)governmentBuildingHealth / maxGovernmentBuildingHealth;
        double cityControlRatio = subordinateCores.isEmpty() ? 1.0 : 
            subordinateCores.stream()
                .mapToDouble(core -> (double)core.getHealth() / core.getMaxHealth())
                .average()
                .orElse(1.0);
                
        return (stabilityRatio + buildingRatio + cityControlRatio) / 3.0;
    }
    
    /**
     * Добавить подчиненное ядро города
     */
    public int getCityCount() {
        return subordinateCores.size();
    }
}