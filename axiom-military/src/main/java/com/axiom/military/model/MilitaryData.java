package com.axiom.military.model;

/**
 * Модель данных для военной силы нации
 */
public class MilitaryData {
    private String nationId;
    private int infantry;
    private int cavalry;
    private int artillery;
    private int navy;
    private int airForce;
    private double strength;
    private double maintenanceCost;
    
    public MilitaryData() {
        this.infantry = 0;
        this.cavalry = 0;
        this.artillery = 0;
        this.navy = 0;
        this.airForce = 0;
        this.strength = 0.0;
        this.maintenanceCost = 0.0;
    }
    
    public MilitaryData(String nationId) {
        this();
        this.nationId = nationId;
    }
    
    // Getters and Setters
    public String getNationId() { return nationId; }
    public void setNationId(String nationId) { this.nationId = nationId; }
    
    public int getInfantry() { return infantry; }
    public void setInfantry(int infantry) { this.infantry = infantry; }
    
    public int getCavalry() { return cavalry; }
    public void setCavalry(int cavalry) { this.cavalry = cavalry; }
    
    public int getArtillery() { return artillery; }
    public void setArtillery(int artillery) { this.artillery = artillery; }
    
    public int getNavy() { return navy; }
    public void setNavy(int navy) { this.navy = navy; }
    
    public int getAirForce() { return airForce; }
    public void setAirForce(int airForce) { this.airForce = airForce; }
    
    public double getStrength() { return strength; }
    public void setStrength(double strength) { this.strength = strength; }
    
    public double getMaintenanceCost() { return maintenanceCost; }
    public void setMaintenanceCost(double maintenanceCost) { this.maintenanceCost = maintenanceCost; }
    
    /**
     * Получить общее количество войск
     */
    public int getTotalUnits() {
        return infantry + cavalry + artillery + navy + airForce;
    }
    
    /**
     * Увеличить пехоту
     */
    public void incrementInfantry(int count) {
        this.infantry += count;
        updateStrength();
    }
    
    /**
     * Уменьшить пехоту
     */
    public void decrementInfantry(int count) {
        this.infantry = Math.max(0, this.infantry - count);
        updateStrength();
    }
    
    /**
     * Увеличить кавалерию
     */
    public void incrementCavalry(int count) {
        this.cavalry += count;
        updateStrength();
    }
    
    /**
     * Уменьшить кавалерию
     */
    public void decrementCavalry(int count) {
        this.cavalry = Math.max(0, this.cavalry - count);
        updateStrength();
    }
    
    /**
     * Увеличить артиллерию
     */
    public void incrementArtillery(int count) {
        this.artillery += count;
        updateStrength();
    }
    
    /**
     * Уменьшить артиллерию
     */
    public void decrementArtillery(int count) {
        this.artillery = Math.max(0, this.artillery - count);
        updateStrength();
    }
    
    /**
     * Увеличить флот
     */
    public void incrementNavy(int count) {
        this.navy += count;
        updateStrength();
    }
    
    /**
     * Уменьшить флот
     */
    public void decrementNavy(int count) {
        this.navy = Math.max(0, this.navy - count);
        updateStrength();
    }
    
    /**
     * Увеличить авиацию
     */
    public void incrementAirForce(int count) {
        this.airForce += count;
        updateStrength();
    }
    
    /**
     * Уменьшить авиацию
     */
    public void decrementAirForce(int count) {
        this.airForce = Math.max(0, this.airForce - count);
        updateStrength();
    }
    
    /**
     * Обновить боевую мощь на основе состава войск
     */
    private void updateStrength() {
        // Базовый расчет мощи
        this.strength = infantry * 1.0 +
                       cavalry * 1.5 +
                       artillery * 2.0 +
                       navy * 2.5 +
                       airForce * 3.0;
    }
    
    /**
     * Рассчитать стоимость содержания
     */
    public double calculateMaintenanceCost() {
        return infantry * 0.1 +
               cavalry * 0.2 +
               artillery * 0.5 +
               navy * 1.0 +
               airForce * 1.5;
    }
}