package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Manages monetary policy (interest rates, money supply). */
public class MonetaryPolicyService {
    private final AXIOM plugin;
    private final Map<String, MonetaryPolicy> policies = new HashMap<>(); // nationId -> policy

    public static class MonetaryPolicy {
        double interestRate; // 0-20%
        double reserveRequirement; // 0-100%
        boolean quantitativeEasing; // printing money
        double moneySupplyMultiplier;
    }

    public MonetaryPolicyService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized String setInterestRate(String nationId, double rate) {
        MonetaryPolicy policy = policies.computeIfAbsent(nationId, k -> {
            MonetaryPolicy p = new MonetaryPolicy();
            p.interestRate = 5.0;
            p.reserveRequirement = 10.0;
            p.quantitativeEasing = false;
            p.moneySupplyMultiplier = 1.0;
            return p;
        });
        policy.interestRate = Math.max(0, Math.min(20, rate));
        return "Ставка процента установлена: " + rate + "%";
    }

    public synchronized String setReserveRequirement(String nationId, double requirement) {
        MonetaryPolicy policy = policies.computeIfAbsent(nationId, k -> {
            MonetaryPolicy p = new MonetaryPolicy();
            p.interestRate = 5.0;
            p.reserveRequirement = 10.0;
            p.quantitativeEasing = false;
            p.moneySupplyMultiplier = 1.0;
            return p;
        });
        policy.reserveRequirement = Math.max(0, Math.min(100, requirement));
        return "Требование резерва установлено: " + requirement + "%";
    }

    public synchronized String enableQE(String nationId, double multiplier) {
        MonetaryPolicy policy = policies.computeIfAbsent(nationId, k -> {
            MonetaryPolicy p = new MonetaryPolicy();
            p.interestRate = 5.0;
            p.reserveRequirement = 10.0;
            p.quantitativeEasing = false;
            p.moneySupplyMultiplier = 1.0;
            return p;
        });
        policy.quantitativeEasing = true;
        policy.moneySupplyMultiplier = multiplier;
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n != null) {
            // QE increases inflation
            n.setInflation(n.getInflation() + 5.0);
            try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        }
        return "Количественное смягчение включено (множитель: " + multiplier + ")";
    }

    public synchronized double getInterestRate(String nationId) {
        MonetaryPolicy policy = policies.get(nationId);
        return policy != null ? policy.interestRate : 5.0;
    }
    
    /**
     * Get comprehensive monetary policy statistics for a nation.
     */
    public synchronized Map<String, Object> getMonetaryPolicyStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        MonetaryPolicy policy = policies.get(nationId);
        if (policy == null) {
            stats.put("hasPolicy", false);
            stats.put("interestRate", 5.0); // Default
            stats.put("reserveRequirement", 10.0); // Default
            stats.put("quantitativeEasing", false);
            stats.put("moneySupplyMultiplier", 1.0);
            return stats;
        }
        
        stats.put("hasPolicy", true);
        stats.put("interestRate", policy.interestRate);
        stats.put("reserveRequirement", policy.reserveRequirement);
        stats.put("quantitativeEasing", policy.quantitativeEasing);
        stats.put("moneySupplyMultiplier", policy.moneySupplyMultiplier);
        
        // Policy rating
        String rating = "СТАНДАРТНАЯ";
        if (policy.quantitativeEasing && policy.moneySupplyMultiplier >= 2.0) rating = "АГРЕССИВНАЯ";
        else if (policy.quantitativeEasing) rating = "ЭКСПАНСИОНИСТСКАЯ";
        else if (policy.interestRate >= 15) rating = "ЖЕСТКАЯ";
        else if (policy.interestRate <= 2) rating = "МЯГКАЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global monetary policy statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalMonetaryPolicyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPolicies = policies.size();
        int qeEnabled = 0;
        double totalMoneySupplyMultiplier = 0.0;
        Map<String, Double> interestRateByNation = new HashMap<>();
        Map<String, Double> reserveByNation = new HashMap<>();
        Map<String, Boolean> qeByNation = new HashMap<>();
        
        for (Map.Entry<String, MonetaryPolicy> entry : policies.entrySet()) {
            String nationId = entry.getKey();
            MonetaryPolicy policy = entry.getValue();
            
            if (policy.quantitativeEasing) {
                qeEnabled++;
                totalMoneySupplyMultiplier += policy.moneySupplyMultiplier;
            }
            
            interestRateByNation.put(nationId, policy.interestRate);
            reserveByNation.put(nationId, policy.reserveRequirement);
            qeByNation.put(nationId, policy.quantitativeEasing);
        }
        
        // Include nations without explicit policies (defaults)
        for (Nation n : plugin.getNationManager().getAll()) {
            if (!policies.containsKey(n.getId())) {
                interestRateByNation.put(n.getId(), 5.0);
                reserveByNation.put(n.getId(), 10.0);
                qeByNation.put(n.getId(), false);
            }
        }
        
        stats.put("totalPolicies", totalPolicies);
        stats.put("qeEnabled", qeEnabled);
        stats.put("averageInterestRate", interestRateByNation.size() > 0 ?
            interestRateByNation.values().stream().mapToDouble(Double::doubleValue).sum() / interestRateByNation.size() : 5.0);
        stats.put("averageReserveRequirement", reserveByNation.size() > 0 ?
            reserveByNation.values().stream().mapToDouble(Double::doubleValue).sum() / reserveByNation.size() : 10.0);
        stats.put("averageMoneySupplyMultiplier", qeEnabled > 0 ? totalMoneySupplyMultiplier / qeEnabled : 0);
        stats.put("interestRateByNation", interestRateByNation);
        stats.put("reserveByNation", reserveByNation);
        stats.put("qeByNation", qeByNation);
        
        // Top nations by interest rate (highest)
        List<Map.Entry<String, Double>> topByInterestRate = interestRateByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByInterestRate", topByInterestRate);
        
        // Interest rate distribution
        int high = 0, moderate = 0, low = 0;
        for (Double rate : interestRateByNation.values()) {
            if (rate >= 10) high++;
            else if (rate >= 5) moderate++;
            else low++;
        }
        
        Map<String, Integer> rateDistribution = new HashMap<>();
        rateDistribution.put("high", high);
        rateDistribution.put("moderate", moderate);
        rateDistribution.put("low", low);
        stats.put("rateDistribution", rateDistribution);
        
        return stats;
    }
}

