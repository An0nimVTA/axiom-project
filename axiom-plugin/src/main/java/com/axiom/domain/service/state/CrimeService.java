package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages crime rates and police system per nation. */
public class CrimeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File crimeDir;
    private final Map<String, Double> crimeRates = new HashMap<>(); // nationId -> rate (0-100)

    public CrimeService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.crimeDir = new File(plugin.getDataFolder(), "crime");
        this.crimeDir.mkdirs();
        loadCrimeRates();
    }

    public synchronized double getCrimeRate(String nationId) {
        return crimeRates.getOrDefault(nationId, 10.0); // default 10%
    }

    public synchronized void updateCrimeRate(String nationId, double rate) {
        crimeRates.put(nationId, Math.max(0, Math.min(100, rate)));
        saveCrimeRate(nationId);
    }

    public synchronized void addCrime(String nationId, double amount) {
        double current = getCrimeRate(nationId);
        updateCrimeRate(nationId, current + amount);
        if (nationManager == null) return;
        Nation n = nationManager.getNationById(nationId);
        if (n != null && n.getBudgetMilitary() < 1000) {
            // Low military budget = less police = more crime
            updateCrimeRate(nationId, current + amount * 1.5);
        }
    }

    private void loadCrimeRates() {
        File f = new File(crimeDir, "rates.json");
        if (!f.exists()) return;
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            for (var entry : o.entrySet()) {
                crimeRates.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        } catch (Exception ignored) {}
    }

    private void saveCrimeRate(String nationId) {
        File f = new File(crimeDir, "rates.json");
        JsonObject o = new JsonObject();
        for (var entry : crimeRates.entrySet()) {
            o.addProperty(entry.getKey(), entry.getValue());
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive crime statistics.
     */
    public synchronized Map<String, Object> getCrimeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (nationManager == null) {
            stats.put("error", "Сервис наций недоступен.");
            return stats;
        }
        double crimeRate = getCrimeRate(nationId);
        stats.put("crimeRate", crimeRate);
        
        // Crime rating
        String rating = "ОТЛИЧНО";
        if (crimeRate >= 50) rating = "КРИТИЧНО";
        else if (crimeRate >= 30) rating = "ВЫСОКО";
        else if (crimeRate >= 20) rating = "СРЕДНЕ";
        else if (crimeRate >= 10) rating = "НИЗКО";
        stats.put("rating", rating);
        
        // Factors affecting crime
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            Map<String, Double> factors = new HashMap<>();
            factors.put("militaryBudgetEffect", n.getBudgetMilitary() < 1000 ? 1.5 : 1.0);
            factors.put("happinessEffect", plugin.getHappinessService() != null ? 
                plugin.getHappinessService().getNationHappiness(nationId) / 100.0 : 1.0);
            stats.put("factors", factors);
        }
        
        // Calculate crime impact on economy
        double economyPenalty = crimeRate * 0.5; // -0.5% per 1% crime
        stats.put("economyPenalty", economyPenalty);
        
        return stats;
    }
    
    /**
     * Reduce crime through law enforcement spending.
     */
    public synchronized String investInLawEnforcement(String nationId, double amount) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (amount <= 0) return "Сумма должна быть больше 0.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < amount) return "Недостаточно средств.";
        
        double currentCrime = getCrimeRate(nationId);
        double reduction = (amount / 1000.0) * 2.0; // Each 1000 reduces crime by 2%
        double newCrime = Math.max(0, currentCrime - reduction);
        
        updateCrimeRate(nationId, newCrime);
        n.setTreasury(n.getTreasury() - amount);
        n.setBudgetMilitary(n.getBudgetMilitary() + amount * 0.5); // Part goes to military (police)
        
        nationManager.save(n);
        
        return "Преступность снижена. Текущий уровень: " + String.format("%.1f", newCrime) + "%";
    }
    
    /**
     * Calculate police effectiveness.
     */
    public synchronized double getPoliceEffectiveness(String nationId) {
        if (nationManager == null) return 0.0;
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 0.0;
        
        // Based on military budget (police funding)
        double baseEffectiveness = Math.min(100, n.getBudgetMilitary() / 100.0);
        
        // Crime rate affects effectiveness (more crime = harder to control)
        double crimeRate = getCrimeRate(nationId);
        baseEffectiveness *= (1.0 - crimeRate / 200.0);
        
        return Math.max(0, baseEffectiveness);
    }
    
    /**
     * Get global crime statistics.
     */
    public synchronized Map<String, Object> getGlobalCrimeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalCrime = 0.0;
        double maxCrime = 0.0;
        double minCrime = Double.MAX_VALUE;
        int nationsWithCrime = 0;
        
        for (Map.Entry<String, Double> entry : crimeRates.entrySet()) {
            double rate = entry.getValue();
            if (rate > 0) {
                nationsWithCrime++;
                totalCrime += rate;
                maxCrime = Math.max(maxCrime, rate);
                minCrime = Math.min(minCrime, rate);
            }
        }
        
        stats.put("totalNations", crimeRates.size());
        stats.put("nationsWithCrime", nationsWithCrime);
        stats.put("averageCrimeRate", nationsWithCrime > 0 ? totalCrime / nationsWithCrime : 0);
        stats.put("maxCrimeRate", maxCrime);
        stats.put("minCrimeRate", minCrime == Double.MAX_VALUE ? 0 : minCrime);
        
        // Crime distribution
        int critical = 0, high = 0, medium = 0, low = 0, excellent = 0;
        for (double rate : crimeRates.values()) {
            if (rate >= 50) critical++;
            else if (rate >= 30) high++;
            else if (rate >= 20) medium++;
            else if (rate >= 10) low++;
            else excellent++;
        }
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("critical", critical);
        distribution.put("high", high);
        distribution.put("medium", medium);
        distribution.put("low", low);
        distribution.put("excellent", excellent);
        stats.put("crimeDistribution", distribution);
        
        // Top nations by crime rate (worst)
        List<Map.Entry<String, Double>> topByCrime = crimeRates.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCrimeRate", topByCrime);
        
        return stats;
    }
}

