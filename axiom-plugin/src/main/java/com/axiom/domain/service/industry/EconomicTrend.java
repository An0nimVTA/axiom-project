package com.axiom.domain.service.industry;

public enum EconomicTrend {
    UP("Рост", "↗"),
    DOWN("Снижение", "↘"),
    STABLE("Стабильность", "→"),
    UNKNOWN("Неизвестно", "?");
    
    private final String displayName;
    private final String symbol;
    
    EconomicTrend(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }
    
    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
}