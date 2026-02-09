package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.UUID;
import java.io.IOException;
import com.axiom.domain.service.state.NationManager;

/**
 * Manages currencies, nation treasuries, inflation parameters.
 */
public class EconomyService {
    private final AXIOM plugin;
    private final NationManager nationManager;

    private String defaultCurrencyCode;
    private double maxPrintAmountPerCommand;
    private int defaultIncomeTaxRate;
    private int defaultSalesTaxRate;

    // GDP window tracking: nationId -> list of (timestamp, amount)
    private final java.util.Map<String, java.util.Deque<long[]>> nationTransactions = new java.util.HashMap<>();
    private final long gdpWindowMs = 24L * 60L * 60L * 1000L;
    private final java.util.Map<String, Double> printedSinceWindow = new java.util.HashMap<>();

    public EconomyService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        this.defaultCurrencyCode = cfg.getString("economy.defaultCurrencyCode", "AXC");
        this.maxPrintAmountPerCommand = cfg.getDouble("economy.maxPrintAmountPerCommand", 1_000_000.0);
        this.defaultIncomeTaxRate = cfg.getInt("economy.incomeTaxPercent", 10);
        this.defaultSalesTaxRate = cfg.getInt("economy.salesTaxPercent", 5);
    }

    /** Prints money into a nation's treasury with safety checks. */
    public synchronized boolean printMoney(UUID actor, double amount) {
        if (amount <= 0 || amount > maxPrintAmountPerCommand) return false;
        Optional<Nation> opt = nationManager.getNationOfPlayer(actor);
        if (opt.isEmpty()) return false;
        Nation nation = opt.get();
        Nation.Role role = nation.getRole(actor);
        if (role != Nation.Role.LEADER && role != Nation.Role.MINISTER) return false;
        
        // ANTI-GRIEF: Check daily limit
        if (plugin.getBalancingService() != null) {
            if (!plugin.getBalancingService().canPrintMoney(nation.getId(), amount)) {
                return false; // Exceeds daily limit or cooldown
            }
        }
        
        nation.setTreasury(nation.getTreasury() + amount);
        try {
            nationManager.save(nation);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to persist treasury after printing: " + e.getMessage());
        }
        printedSinceWindow.merge(nation.getId(), amount, Double::sum);
        return true;
    }

    public String getDefaultCurrencyCode() { return defaultCurrencyCode; }

    /**
     * Get nation treasury amount.
     */
    public synchronized double getTreasury(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        return n != null ? n.getTreasury() : 0.0;
    }

    /** Apply income to a player, collecting taxes and tithes to the nation. Returns net to player. */
    public synchronized double applyIncomeTaxes(java.util.UUID playerId, double grossAmount) {
        if (grossAmount <= 0) return 0;
        java.util.Optional<Nation> opt = nationManager.getNationOfPlayer(playerId);
        if (opt.isEmpty()) return grossAmount; // no national taxes
        Nation nation = opt.get();
        int tax = nation.getTaxRate() > 0 ? nation.getTaxRate() : defaultIncomeTaxRate;
        double taxAmt = grossAmount * (tax / 100.0);
        double net = grossAmount - taxAmt;
        nation.setTreasury(nation.getTreasury() + taxAmt);
        try { nationManager.save(nation); } catch (Exception ignored) {}
        recordTransaction(nation.getId(), grossAmount);
        
        // Religion tithe (5% of net income)
        String religion = plugin.getPlayerDataManager().getReligion(playerId);
        double titheAmt = 0;
        if (religion != null) {
            titheAmt = net * 0.05;
            plugin.getReligionManager().recordTithe(playerId, religion, titheAmt);
            net -= titheAmt;
        }
        
        // VISUAL EFFECTS: Notify player of large income (only for amounts > 1000 to avoid spam)
        final double finalNet = net;
        final double finalTitheAmt = titheAmt;
        if (grossAmount >= 1000) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                final String currency = nation.getCurrencyCode();
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    String msg = String.format("§a💰 Доход: §f%.2f %s §7(Налог: §c-%.2f %s §7| Десятина: §e-%.2f %s§7) §a= §f%.2f %s", 
                        grossAmount, currency, taxAmt, currency, finalTitheAmt, currency, finalNet, currency);
                    plugin.getVisualEffectsService().sendActionBar(p, msg);
                    
                    // Green particles for income
                    org.bukkit.Location loc = p.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc, 5, 0.3, 1, 0.3, 0.1);
                });
            }
        }
        
        return net;
    }

    public synchronized void recordTransaction(String nationId, double amount) {
        long now = System.currentTimeMillis();
        java.util.Deque<long[]> q = nationTransactions.computeIfAbsent(nationId, k -> new java.util.ArrayDeque<>());
        q.addLast(new long[]{now, (long) Math.round(amount * 100)});
        // prune
        while (!q.isEmpty() && q.peekFirst()[0] < now - gdpWindowMs) q.pollFirst();
        // recompute inflation roughly: (printed / gdp) * 100
        double gdp = 0;
        for (long[] t : q) gdp += t[1] / 100.0;
        double printed = printedSinceWindow.getOrDefault(nationId, 0.0);
        double infl = (gdp <= 0) ? 0.0 : (printed / gdp) * 100.0;
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.setInflation(infl);
            try { nationManager.save(n); } catch (Exception ignored) {}
        }
    }

    /** Get GDP for a nation (last 24h). */
    public synchronized double getGDP(String nationId) {
        java.util.Deque<long[]> q = nationTransactions.get(nationId);
        if (q == null) return 0.0;
        double gdp = 0;
        long now = System.currentTimeMillis();
        for (long[] t : q) {
            if (t[0] >= now - gdpWindowMs) gdp += t[1] / 100.0;
        }
        return gdp;
    }

    /** Get economic health indicator (0-100). */
    public synchronized double getEconomicHealth(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return 0.0;
        double gdp = getGDP(nationId);
        double inflation = n.getInflation();
        double treasuryRatio = Math.min(1.0, n.getTreasury() / 100000.0);
        double health = (gdp > 0 ? 40 : 0) + (100 - Math.min(100, inflation)) * 0.4 + treasuryRatio * 20;
        return Math.max(0, Math.min(100, health));
    }
    
    /**
     * Transfer funds between nations (e.g., reparations, aid).
     */
    public synchronized String transferFunds(String fromNationId, String toNationId, double amount, String reason) throws IOException {
        Nation from = nationManager.getNationById(fromNationId);
        Nation to = nationManager.getNationById(toNationId);
        if (from == null || to == null) return "Нация не найдена.";
        if (amount <= 0) return "Сумма должна быть положительной.";
        if (from.getTreasury() < amount) return "Недостаточно средств.";
        
        from.setTreasury(from.getTreasury() - amount);
        to.setTreasury(to.getTreasury() + amount);
        
        recordTransaction(fromNationId, -amount);
        recordTransaction(toNationId, amount);
        
        nationManager.save(from);
        nationManager.save(to);
        
        // VISUAL EFFECTS
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String msg1 = String.format("§c💰 Переведено: §f%.2f %s §7→ %s (%s)", amount, from.getCurrencyCode(), to.getName(), reason);
            String msg2 = String.format("§a💰 Получено: §f%.2f %s §7от %s (%s)", amount, to.getCurrencyCode(), from.getName(), reason);
            
            for (UUID citizenId : from.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg1);
                }
            }
            for (UUID citizenId : to.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    plugin.getVisualEffectsService().sendActionBar(citizen, msg2);
                }
            }
        });
        
        return String.format("Переведено %.2f %s от '%s' к '%s'", amount, from.getCurrencyCode(), from.getName(), to.getName());
    }
    
    /**
     * Apply sales tax to a transaction.
     */
    public synchronized double applySalesTax(String nationId, double transactionAmount) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return transactionAmount;
        
        int taxRate = defaultSalesTaxRate;
        double tax = transactionAmount * (taxRate / 100.0);
        double net = transactionAmount - tax;
        n.setTreasury(n.getTreasury() + tax);
        
        try {
            nationManager.save(n);
            recordTransaction(nationId, tax);
        } catch (Exception ignored) {}
        
        return net;
    }
    
    /**
     * Get comprehensive economic statistics.
     */
    public synchronized Map<String, Object> getEconomicStatistics(String nationId) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return Collections.emptyMap();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("treasury", n.getTreasury());
        stats.put("gdp", getGDP(nationId));
        stats.put("inflation", n.getInflation());
        stats.put("economicHealth", getEconomicHealth(nationId));
        stats.put("taxRate", n.getTaxRate());
        stats.put("currencyCode", n.getCurrencyCode());
        stats.put("exchangeRate", n.getExchangeRateToAXC());
        
        // Budget breakdown
        stats.put("budgetMilitary", n.getBudgetMilitary());
        stats.put("budgetHealth", n.getBudgetHealth());
        stats.put("budgetEducation", n.getBudgetEducation());
        
        double totalBudget = n.getBudgetMilitary() + n.getBudgetHealth() + n.getBudgetEducation();
        stats.put("totalBudget", totalBudget);
        stats.put("budgetPercentage", n.getTreasury() > 0 ? (totalBudget / n.getTreasury()) * 100 : 0);
        
        return stats;
    }
    
    /**
     * Set budget allocation.
     */
    public synchronized String setBudget(String nationId, String category, double amount) throws IOException {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (amount < 0) return "Бюджет не может быть отрицательным.";
        if (n.getTreasury() < amount) return "Недостаточно средств в казне.";
        
        double oldAmount = 0;
        switch (category.toLowerCase()) {
            case "military":
                oldAmount = n.getBudgetMilitary();
                n.setBudgetMilitary(amount);
                break;
            case "health":
                oldAmount = n.getBudgetHealth();
                n.setBudgetHealth(amount);
                break;
            case "education":
                oldAmount = n.getBudgetEducation();
                n.setBudgetEducation(amount);
                break;
            default:
                return "Неизвестная категория бюджета.";
        }
        
        // Adjust treasury
        n.setTreasury(n.getTreasury() + oldAmount - amount);
        nationManager.save(n);
        
        return String.format("Бюджет '%s' установлен: %.2f %s (было: %.2f)", category, amount, n.getCurrencyCode(), oldAmount);
    }
    
    /**
     * Exchange currency between nations.
     */
    public synchronized String exchangeCurrency(String fromNationId, String toNationId, double amount) throws IOException {
        Nation from = nationManager.getNationById(fromNationId);
        Nation to = nationManager.getNationById(toNationId);
        if (from == null || to == null) return "Нация не найдена.";
        if (amount <= 0) return "Сумма должна быть положительной.";
        
        // Convert using exchange rates
        double fromInAXC = amount / from.getExchangeRateToAXC();
        double toAmount = fromInAXC * to.getExchangeRateToAXC();
        
        if (from.getTreasury() < amount) return "Недостаточно средств.";
        
        from.setTreasury(from.getTreasury() - amount);
        to.setTreasury(to.getTreasury() + toAmount);
        
        recordTransaction(fromNationId, -amount);
        recordTransaction(toNationId, toAmount);
        
        nationManager.save(from);
        nationManager.save(to);
        
        return String.format("Обменено: %.2f %s = %.2f %s", amount, from.getCurrencyCode(), toAmount, to.getCurrencyCode());
    }
    
    /**
     * Get global economic statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalEconomicStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalTreasury = 0.0;
        double totalGDP = 0.0;
        double totalInflation = 0.0;
        double maxTreasury = 0.0;
        double maxGDP = 0.0;
        int nationsWithEconomy = 0;
        
        for (Nation n : nationManager.getAll()) {
            double treasury = n.getTreasury();
            double gdp = getGDP(n.getId());
            double inflation = n.getInflation();
            
            if (treasury > 0 || gdp > 0) {
                nationsWithEconomy++;
            }
            
            totalTreasury += treasury;
            totalGDP += gdp;
            totalInflation += inflation;
            maxTreasury = Math.max(maxTreasury, treasury);
            maxGDP = Math.max(maxGDP, gdp);
        }
        
        stats.put("totalTreasury", totalTreasury);
        stats.put("totalGDP", totalGDP);
        stats.put("maxTreasury", maxTreasury);
        stats.put("maxGDP", maxGDP);
        stats.put("averageTreasury", nationsWithEconomy > 0 ? totalTreasury / nationsWithEconomy : 0);
        stats.put("averageGDP", nationsWithEconomy > 0 ? totalGDP / nationsWithEconomy : 0);
        stats.put("averageInflation", nationsWithEconomy > 0 ? totalInflation / nationsWithEconomy : 0);
        stats.put("totalNations", nationManager.getAll().size());
        stats.put("nationsWithEconomy", nationsWithEconomy);
        
        // Top economies
        List<Map.Entry<String, Double>> topByTreasury = new ArrayList<>();
        List<Map.Entry<String, Double>> topByGDP = new ArrayList<>();
        List<Map.Entry<String, Double>> topByEconomicHealth = new ArrayList<>();
        
        for (Nation n : nationManager.getAll()) {
            topByTreasury.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), n.getTreasury()));
            topByGDP.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), getGDP(n.getId())));
            topByEconomicHealth.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), getEconomicHealth(n.getId())));
        }
        
        topByTreasury.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        topByGDP.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        topByEconomicHealth.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        stats.put("topByTreasury", topByTreasury.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        stats.put("topByGDP", topByGDP.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        stats.put("topByEconomicHealth", topByEconomicHealth.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        // Currency distribution
        Map<String, Integer> currencyDistribution = new HashMap<>();
        for (Nation n : nationManager.getAll()) {
            String currency = n.getCurrencyCode();
            currencyDistribution.put(currency, currencyDistribution.getOrDefault(currency, 0) + 1);
        }
        stats.put("currencyDistribution", currencyDistribution);
        
        return stats;
    }
    
    /**
     * Get economic leaderboard.
     */
    public synchronized Map<String, List<Map.Entry<String, Double>>> getEconomicLeaderboards() {
        Map<String, List<Map.Entry<String, Double>>> leaderboards = new HashMap<>();
        
        List<Map.Entry<String, Double>> treasury = new ArrayList<>();
        List<Map.Entry<String, Double>> gdp = new ArrayList<>();
        List<Map.Entry<String, Double>> health = new ArrayList<>();
        
        for (Nation n : nationManager.getAll()) {
            treasury.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), n.getTreasury()));
            gdp.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), getGDP(n.getId())));
            health.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), getEconomicHealth(n.getId())));
        }
        
        treasury.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        gdp.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        health.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        leaderboards.put("treasury", treasury.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        leaderboards.put("gdp", gdp.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        leaderboards.put("economicHealth", health.stream().limit(10).collect(java.util.stream.Collectors.toList()));
        
        return leaderboards;
    }
    
    /**
     * Get nations with best economic health.
     */
    public synchronized List<Map.Entry<String, Double>> getTopNationsByEconomicHealth(int limit) {
        List<Map.Entry<String, Double>> rankings = new ArrayList<>();
        
        for (Nation n : nationManager.getAll()) {
            rankings.add(new java.util.AbstractMap.SimpleEntry<>(n.getId(), getEconomicHealth(n.getId())));
        }
        
        return rankings.stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get total money printed in the last 24h.
     */
    public synchronized double getTotalMoneyPrinted() {
        return printedSinceWindow.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    
    /**
     * Get money printed by nation.
     */
    public synchronized double getMoneyPrintedByNation(String nationId) {
        return printedSinceWindow.getOrDefault(nationId, 0.0);
    }

    // Wallet compatibility methods for TestBot
    public double getBalance(UUID playerId) {
        if (plugin.getWalletService() != null) {
            return plugin.getWalletService().getBalance(playerId);
        }
        return 0.0;
    }

    public void addBalance(UUID playerId, double amount) {
        if (plugin.getWalletService() != null) {
            plugin.getWalletService().deposit(playerId, amount);
        }
    }

    public void removeBalance(UUID playerId, double amount) {
        if (plugin.getWalletService() != null) {
            plugin.getWalletService().withdraw(playerId, amount);
        }
    }

    public synchronized boolean transfer(UUID from, UUID to, double amount) {
        if (plugin.getWalletService() != null) {
            plugin.getWalletService().withdraw(from, amount);
            plugin.getWalletService().deposit(to, amount);
            return true;
        }
        return false;
    }
}


