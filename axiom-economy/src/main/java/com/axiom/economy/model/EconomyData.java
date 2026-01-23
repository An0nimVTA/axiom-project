package com.axiom.economy.model;

/**
 * Модель данных для экономики нации
 */
public class EconomyData {
    private String nationId;
    private double treasury;
    private double gdp;
    private double inflationRate;
    private double unemploymentRate;
    private String currency;
    private double exchangeRate;
    
    public EconomyData() {
        this.treasury = 0.0;
        this.gdp = 0.0;
        this.inflationRate = 0.0;
        this.unemploymentRate = 0.0;
        this.currency = "DEFAULT";
        this.exchangeRate = 1.0;
    }
    
    public EconomyData(String nationId) {
        this();
        this.nationId = nationId;
    }
    
    // Getters and Setters
    public String getNationId() { return nationId; }
    public void setNationId(String nationId) { this.nationId = nationId; }
    
    public double getTreasury() { return treasury; }
    public void setTreasury(double treasury) { this.treasury = treasury; }
    
    public double getGdp() { return gdp; }
    public void setGdp(double gdp) { this.gdp = gdp; }
    
    public double getInflationRate() { return inflationRate; }
    public void setInflationRate(double inflationRate) { this.inflationRate = inflationRate; }
    
    public double getUnemploymentRate() { return unemploymentRate; }
    public void setUnemploymentRate(double unemploymentRate) { this.unemploymentRate = unemploymentRate; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public double getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(double exchangeRate) { this.exchangeRate = exchangeRate; }
    
    /**
     * Добавление средств к казне
     */
    public void addToTreasury(double amount) {
        this.treasury += amount;
    }
    
    /**
     * Вычитание средств из казны
     */
    public boolean removeFromTreasury(double amount) {
        if (treasury >= amount) {
            treasury -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Обновление ВВП
     */
    public void updateGdp(double gdpGrowth) {
        this.gdp += gdpGrowth;
    }
}