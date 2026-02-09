package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.axiom.domain.service.state.NationManager;

/** Manages currency exchange between nations. */
public class CurrencyExchangeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final EconomyService economyService;

    public CurrencyExchangeService(AXIOM plugin, NationManager nationManager, EconomyService economyService) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.economyService = economyService;
    }

    public synchronized String exchangeCurrency(UUID playerId, String fromNationId, String toNationId, double amount) {
        if (playerId == null || fromNationId == null || toNationId == null) return "Неверные параметры.";
        if (amount <= 0) return "Сумма должна быть больше 0.";
        if (fromNationId.equals(toNationId)) return "Валюты совпадают.";
        if (nationManager == null) return "Сервис наций недоступен.";
        if (plugin.getWalletService() == null) return "Сервис кошелька недоступен.";
        Nation from = nationManager.getNationById(fromNationId);
        Nation to = nationManager.getNationById(toNationId);
        if (from == null || to == null) return "Нация не найдена.";
        double playerBalance = plugin.getWalletService().getBalance(playerId);
        if (playerBalance < amount) return "Недостаточно средств.";
        // Exchange rate: from currency -> AXC -> to currency
        double fromToAXC = from.getExchangeRateToAXC();
        if (fromToAXC <= 0) return "Некорректный курс исходной валюты.";
        double axcAmount = amount * fromToAXC;
        double toFromAXC = to.getExchangeRateToAXC();
        if (toFromAXC <= 0) return "Некорректный курс целевой валюты.";
        double result = axcAmount / toFromAXC;
        // Fee: 2%
        double fee = result * 0.02;
        double finalAmount = Math.max(0, result - fee);
        plugin.getWalletService().withdraw(playerId, amount);
        plugin.getWalletService().deposit(playerId, finalAmount);
        // Add fee to nation treasury
        to.setTreasury(to.getTreasury() + fee);
        try {
            nationManager.save(to);
        } catch (Exception ignored) {}
        return "Обменено: " + amount + " " + from.getCurrencyCode() + " → " + finalAmount + " " + to.getCurrencyCode() + " (комиссия: " + fee + ")";
    }

    public synchronized double getExchangeRate(String fromNationId, String toNationId) {
        if (nationManager == null || fromNationId == null || toNationId == null) return 1.0;
        Nation from = nationManager.getNationById(fromNationId);
        Nation to = nationManager.getNationById(toNationId);
        if (from == null || to == null) return 1.0;
        double fromToAXC = from.getExchangeRateToAXC();
        double toFromAXC = to.getExchangeRateToAXC();
        if (fromToAXC <= 0 || toFromAXC <= 0) return 1.0;
        return fromToAXC / toFromAXC;
    }
    
    /**
     * Get comprehensive currency exchange statistics.
     */
    public synchronized Map<String, Object> getExchangeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        Nation n = nationManager.getNationById(nationId);
        if (n == null) {
            stats.put("error", "Нация не найдена.");
            return stats;
        }
        
        stats.put("currencyCode", n.getCurrencyCode());
        stats.put("exchangeRateToAXC", n.getExchangeRateToAXC());
        
        // Compare with other nations
        List<Map<String, Object>> exchangeRates = new ArrayList<>();
        for (Nation other : nationManager.getAll()) {
            if (!other.getId().equals(nationId)) {
                Map<String, Object> rateData = new HashMap<>();
                rateData.put("nation", other.getName());
                rateData.put("currencyCode", other.getCurrencyCode());
                rateData.put("exchangeRate", getExchangeRate(nationId, other.getId()));
                exchangeRates.add(rateData);
            }
        }
        stats.put("exchangeRates", exchangeRates);
        
        // Exchange rating
        String rating = "СТАНДАРТНАЯ";
        if (n.getExchangeRateToAXC() > 2.0) rating = "ВЫСОКИЙ КУРС";
        else if (n.getExchangeRateToAXC() < 0.5) rating = "НИЗКИЙ КУРС";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Calculate exchange fee.
     */
    public synchronized double calculateFee(String fromNationId, String toNationId, double amount) {
        if (nationManager == null || fromNationId == null || toNationId == null) return 0.0;
        if (amount <= 0) return 0.0;
        Nation from = nationManager.getNationById(fromNationId);
        Nation to = nationManager.getNationById(toNationId);
        if (from == null || to == null) return 0.0;
        
        double fromToAXC = from.getExchangeRateToAXC();
        double axcAmount = amount * fromToAXC;
        double toFromAXC = to.getExchangeRateToAXC();
        if (fromToAXC <= 0 || toFromAXC <= 0) return 0.0;
        double result = axcAmount / toFromAXC;
        return result * 0.02; // 2% fee
    }
    
    /**
     * Get exchange rate history (simplified).
     */
    public synchronized Map<String, Double> getAllExchangeRates(String nationId) {
        Map<String, Double> rates = new HashMap<>();
        if (nationManager == null) return rates;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return rates;
        
        for (Nation other : nationManager.getAll()) {
            if (!other.getId().equals(nationId)) {
                rates.put(other.getName(), getExchangeRate(nationId, other.getId()));
            }
        }
        
        return rates;
    }
    
    /**
     * Get global currency exchange statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCurrencyExchangeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        int totalNations = nationManager.getAll().size();
        Map<String, Integer> currenciesByCode = new HashMap<>();
        Map<String, Double> exchangeRatesByNation = new HashMap<>();
        double totalExchangeRate = 0.0;
        double minRate = Double.MAX_VALUE;
        double maxRate = 0.0;
        
        for (Nation n : nationManager.getAll()) {
            String code = n.getCurrencyCode();
            double rate = n.getExchangeRateToAXC();
            
            currenciesByCode.put(code, currenciesByCode.getOrDefault(code, 0) + 1);
            exchangeRatesByNation.put(n.getId(), rate);
            totalExchangeRate += rate;
            
            if (rate < minRate) minRate = rate;
            if (rate > maxRate) maxRate = rate;
        }
        
        stats.put("totalNations", totalNations);
        stats.put("uniqueCurrencies", currenciesByCode.size());
        stats.put("currenciesByCode", currenciesByCode);
        stats.put("exchangeRatesByNation", exchangeRatesByNation);
        stats.put("averageExchangeRate", totalNations > 0 ? totalExchangeRate / totalNations : 0);
        stats.put("minExchangeRate", minRate == Double.MAX_VALUE ? 0 : minRate);
        stats.put("maxExchangeRate", maxRate);
        
        // Most common currencies
        List<Map.Entry<String, Integer>> mostCommonCurrencies = currenciesByCode.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonCurrencies", mostCommonCurrencies);
        
        // Nations by exchange rate (high to low)
        List<Map.Entry<String, Double>> topByRate = exchangeRatesByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByExchangeRate", topByRate);
        
        // Exchange rate distribution
        int highRate = 0, mediumRate = 0, lowRate = 0;
        for (Double rate : exchangeRatesByNation.values()) {
            if (rate > 2.0) highRate++;
            else if (rate < 0.5) lowRate++;
            else mediumRate++;
        }
        
        Map<String, Integer> rateDistribution = new HashMap<>();
        rateDistribution.put("high", highRate);
        rateDistribution.put("medium", mediumRate);
        rateDistribution.put("low", lowRate);
        stats.put("rateDistribution", rateDistribution);
        
        return stats;
    }
}

