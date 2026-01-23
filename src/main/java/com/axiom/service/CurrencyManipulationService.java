package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;

import java.util.*;

/** Manages currency manipulation and exchange rate controls. */
public class CurrencyManipulationService {
    private final AXIOM plugin;
    private final Map<String, CurrencyPolicy> policies = new HashMap<>(); // nationId -> policy

    public static class CurrencyPolicy {
        double exchangeRate; // manipulated rate
        boolean fixedRate; // fixed or floating
        double reserveRequirement; // foreign currency reserves
    }

    public CurrencyManipulationService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized String setExchangeRate(String nationId, double rate) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        CurrencyPolicy policy = policies.computeIfAbsent(nationId, k -> {
            CurrencyPolicy p = new CurrencyPolicy();
            p.exchangeRate = 1.0;
            p.fixedRate = false;
            p.reserveRequirement = 0.0;
            return p;
        });
        policy.exchangeRate = rate;
        policy.fixedRate = true;
        n.setExchangeRateToAXC(rate);
        n.getHistory().add("Валютный курс установлен: " + rate);
        try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        return "Курс установлен: " + rate;
    }

    public synchronized String devalueCurrency(String nationId, double amount) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        double currentRate = n.getExchangeRateToAXC();
        double newRate = Math.max(0.1, currentRate - amount);
        n.setExchangeRateToAXC(newRate);
        n.setInflation(n.getInflation() + 5.0); // Devaluation causes inflation
        n.getHistory().add("Валюта обесценена: " + newRate);
        try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        return "Валюта обесценена до: " + newRate;
    }

    public synchronized double getEffectiveExchangeRate(String nationId) {
        CurrencyPolicy policy = policies.get(nationId);
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (policy != null && policy.fixedRate) {
            return policy.exchangeRate;
        }
        return n != null ? n.getExchangeRateToAXC() : 1.0;
    }
    
    /**
     * Get comprehensive currency manipulation statistics for a nation.
     */
    public synchronized Map<String, Object> getCurrencyManipulationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        CurrencyPolicy policy = policies.get(nationId);
        Nation n = plugin.getNationManager().getNationById(nationId);
        
        if (policy == null) {
            stats.put("hasPolicy", false);
            if (n != null) {
                stats.put("exchangeRate", n.getExchangeRateToAXC());
                stats.put("fixedRate", false);
            }
            return stats;
        }
        
        stats.put("hasPolicy", true);
        stats.put("exchangeRate", policy.exchangeRate);
        stats.put("fixedRate", policy.fixedRate);
        stats.put("reserveRequirement", policy.reserveRequirement);
        
        double effectiveRate = getEffectiveExchangeRate(nationId);
        stats.put("effectiveRate", effectiveRate);
        
        // Currency policy rating
        String rating = "ПЛАВАЮЩИЙ";
        if (policy.fixedRate && policy.exchangeRate >= 2.0) rating = "СИЛЬНАЯ";
        else if (policy.fixedRate && policy.exchangeRate >= 1.0) rating = "СТАБИЛЬНАЯ";
        else if (policy.fixedRate && policy.exchangeRate >= 0.5) rating = "ОСЛАБЛЕННАЯ";
        else if (policy.fixedRate) rating = "СЛАБАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global currency manipulation statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCurrencyManipulationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPolicies = policies.size();
        int fixedRates = 0;
        int floatingRates = 0;
        double totalReserves = 0.0;
        Map<String, Double> exchangeRateByNation = new HashMap<>();
        Map<String, Boolean> fixedRateByNation = new HashMap<>();
        
        for (Map.Entry<String, CurrencyPolicy> entry : policies.entrySet()) {
            String nationId = entry.getKey();
            CurrencyPolicy policy = entry.getValue();
            
            if (policy.fixedRate) {
                fixedRates++;
            } else {
                floatingRates++;
            }
            totalReserves += policy.reserveRequirement;
            
            exchangeRateByNation.put(nationId, policy.exchangeRate);
            fixedRateByNation.put(nationId, policy.fixedRate);
        }
        
        // Also include nations without explicit policies
        for (Nation n : plugin.getNationManager().getAll()) {
            if (!policies.containsKey(n.getId())) {
                exchangeRateByNation.put(n.getId(), n.getExchangeRateToAXC());
                fixedRateByNation.put(n.getId(), false);
            }
        }
        
        stats.put("totalPolicies", totalPolicies);
        stats.put("fixedRates", fixedRates);
        stats.put("floatingRates", floatingRates);
        stats.put("exchangeRateByNation", exchangeRateByNation);
        stats.put("fixedRateByNation", fixedRateByNation);
        stats.put("averageExchangeRate", exchangeRateByNation.size() > 0 ? 
            exchangeRateByNation.values().stream().mapToDouble(Double::doubleValue).sum() / exchangeRateByNation.size() : 1.0);
        stats.put("averageReserves", totalPolicies > 0 ? totalReserves / totalPolicies : 0);
        
        // Top nations by exchange rate (strongest currencies)
        List<Map.Entry<String, Double>> topByRate = exchangeRateByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRate", topByRate);
        
        // Exchange rate distribution
        int strong = 0, stable = 0, weak = 0, veryWeak = 0;
        for (Double rate : exchangeRateByNation.values()) {
            if (rate >= 2.0) strong++;
            else if (rate >= 1.0) stable++;
            else if (rate >= 0.5) weak++;
            else veryWeak++;
        }
        
        Map<String, Integer> rateDistribution = new HashMap<>();
        rateDistribution.put("strong", strong);
        rateDistribution.put("stable", stable);
        rateDistribution.put("weak", weak);
        rateDistribution.put("veryWeak", veryWeak);
        stats.put("rateDistribution", rateDistribution);
        
        return stats;
    }
}

