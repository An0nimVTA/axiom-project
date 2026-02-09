package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.infrastructure.VisualEffectsService;
import com.axiom.domain.service.state.NationManager;

/** Manages banking: loans, interest, deposits. */
public class BankingService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File loansDir;
    private final Map<String, List<Loan>> activeLoans = new HashMap<>(); // nationId -> loans

    public static class Loan {
        public String lenderNationId;
        public String borrowerNationId;
        public double principal;
        public double remaining;
        public double interestRate; // per year
        public long issuedAt;
        public long dueAt;
    }

    public BankingService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.loansDir = new File(plugin.getDataFolder(), "loans");
        this.loansDir.mkdirs();
        loadAll();
        Bukkit.getScheduler().runTaskTimer(plugin, this::processInterest, 0, 20 * 60 * 60); // hourly
    }

    public synchronized String issueLoan(String lenderId, String borrowerId, double amount, double interestRate, long durationDays) throws IOException {
        Nation lender = nationManager.getNationById(lenderId);
        Nation borrower = nationManager.getNationById(borrowerId);
        if (lender == null || borrower == null) return "Нация не найдена.";
        if (lenderId.equals(borrowerId)) return "Нельзя выдать кредит самому себе.";
        if (amount <= 0 || interestRate < 0 || durationDays <= 0) return "Неверные параметры кредита.";
        if (lender.getTreasury() < amount) return "Недостаточно средств у кредитора.";
        Loan loan = new Loan();
        loan.lenderNationId = lenderId;
        loan.borrowerNationId = borrowerId;
        loan.principal = amount;
        loan.remaining = amount;
        loan.interestRate = interestRate;
        loan.issuedAt = System.currentTimeMillis();
        loan.dueAt = loan.issuedAt + (durationDays * 24L * 60L * 60L * 1000L);
        lender.setTreasury(lender.getTreasury() - amount);
        borrower.setTreasury(borrower.getTreasury() + amount);
        activeLoans.computeIfAbsent(borrowerId, k -> new ArrayList<>()).add(loan);
        nationManager.save(lender);
        nationManager.save(borrower);
        saveLoan(loan);
        
        // VISUAL EFFECTS: Notify both nations of loan
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            VisualEffectsService effectsService = plugin.getVisualEffectsService();
            if (effectsService == null) return;
            String msg1 = "§e💰 Кредит выдан: §f" + String.format("%.0f", amount) + " §7→ '" + borrower.getName() + "'";
            for (UUID citizenId : lender.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    effectsService.sendActionBar(citizen, msg1);
                }
            }
            String msg2 = "§a💰 Кредит получен: §f" + String.format("%.0f", amount) + " §7от '" + lender.getName() + "' (§e" + interestRate + "%§7 годовых)";
            for (UUID citizenId : borrower.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[КРЕДИТ]", "§fПолучено " + String.format("%.0f", amount), 10, 60, 10);
                    effectsService.sendActionBar(citizen, msg2);
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                    citizen.playSound(loc, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                }
            }
        });
        
        return "Кредит выдан: " + amount + " под " + interestRate + "% годовых на " + durationDays + " дней.";
    }

    public synchronized String repayLoan(String borrowerId, String loanId, double amount) throws IOException {
        List<Loan> loans = activeLoans.get(borrowerId);
        if (loans == null) return "Нет активных кредитов.";
        if (loanId == null || loanId.isBlank()) return "Неверный ID кредита.";
        Loan loan = loans.stream()
            .filter(l -> getLoanId(l).equals(loanId))
            .findFirst()
            .orElse(null);
        if (loan == null) return "Кредит не найден.";
        Nation borrower = nationManager.getNationById(borrowerId);
        Nation lender = nationManager.getNationById(loan.lenderNationId);
        if (borrower == null || lender == null) return "Нация не найдена.";
        if (amount <= 0) return "Неверная сумма.";
        if (borrower.getTreasury() < amount) return "Недостаточно средств.";
        borrower.setTreasury(borrower.getTreasury() - amount);
        lender.setTreasury(lender.getTreasury() + amount);
        loan.remaining -= amount;
        boolean fullyPaid = loan.remaining <= 0;
        if (fullyPaid) {
            loans.remove(loan);
            deleteLoan(loan);
        } else {
            saveLoan(loan);
        }
        nationManager.save(borrower);
        nationManager.save(lender);
        
        // VISUAL EFFECTS: Notify of payment
        if (fullyPaid) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                VisualEffectsService effectsService = plugin.getVisualEffectsService();
                if (effectsService == null) return;
                String msg1 = "§a✓ Кредит полностью погашен!";
                for (UUID citizenId : borrower.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        citizen.sendTitle("§a§l[КРЕДИТ ПОГАШЕН]", "§fЗаём перед '" + lender.getName() + "' выплачен", 10, 60, 10);
                        effectsService.sendActionBar(citizen, msg1);
                        org.bukkit.Location loc = citizen.getLocation();
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                        citizen.playSound(loc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                    }
                }
                String msg2 = "§a✓ Получен полный платёж от '" + borrower.getName() + "'";
                for (UUID citizenId : lender.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        effectsService.sendActionBar(citizen, msg2);
                        org.bukkit.Location loc = citizen.getLocation();
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.05);
                    }
                }
            });
        }
        
        return fullyPaid ? "Кредит полностью погашен!" : "Платёж принят. Остаток: " + loan.remaining;
    }

    private synchronized void processInterest() {
        long now = System.currentTimeMillis();
        for (var entry : activeLoans.entrySet()) {
            for (Loan loan : new ArrayList<>(entry.getValue())) {
                if (now > loan.dueAt) {
                    // Loan overdue - apply penalty
                    Nation borrower = nationManager.getNationById(loan.borrowerNationId);
                    if (borrower != null) {
                        double penalty = loan.remaining * 0.1; // 10% penalty
                        borrower.setTreasury(Math.max(0, borrower.getTreasury() - penalty));
                        try { nationManager.save(borrower); } catch (Exception ignored) {}
                    }
                } else {
                    // Apply interest
                    double interest = loan.remaining * (loan.interestRate / 365.0 / 24.0); // daily
                    loan.remaining += interest;
                }
                try { saveLoan(loan); } catch (Exception ignored) {}
            }
        }
    }

    private void loadAll() {
        File[] files = loansDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Loan loan = new Loan();
                loan.lenderNationId = o.get("lenderNationId").getAsString();
                loan.borrowerNationId = o.get("borrowerNationId").getAsString();
                loan.principal = o.get("principal").getAsDouble();
                loan.remaining = o.get("remaining").getAsDouble();
                loan.interestRate = o.get("interestRate").getAsDouble();
                loan.issuedAt = o.get("issuedAt").getAsLong();
                loan.dueAt = o.get("dueAt").getAsLong();
                if (loan.remaining > 0) {
                    activeLoans.computeIfAbsent(loan.borrowerNationId, k -> new ArrayList<>()).add(loan);
                }
            } catch (Exception ignored) {}
        }
    }

    private String getLoanId(Loan loan) {
        return loan.lenderNationId + "_" + loan.borrowerNationId + "_" + loan.issuedAt;
    }

    private void saveLoan(Loan loan) throws IOException {
        File f = new File(loansDir, loan.lenderNationId + "_" + loan.borrowerNationId + "_" + loan.issuedAt + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("lenderNationId", loan.lenderNationId);
        o.addProperty("borrowerNationId", loan.borrowerNationId);
        o.addProperty("principal", loan.principal);
        o.addProperty("remaining", loan.remaining);
        o.addProperty("interestRate", loan.interestRate);
        o.addProperty("issuedAt", loan.issuedAt);
        o.addProperty("dueAt", loan.dueAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }

    private void deleteLoan(Loan loan) {
        File f = new File(loansDir, loan.lenderNationId + "_" + loan.borrowerNationId + "_" + loan.issuedAt + ".json");
        if (f.exists()) f.delete();
    }
    
    /**
     * Get comprehensive banking statistics for a nation.
     */
    public synchronized Map<String, Object> getBankingStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Loan> loans = activeLoans.get(nationId);
        if (loans == null) loans = new ArrayList<>();
        
        stats.put("activeLoans", loans.size());
        stats.put("totalDebt", loans.stream().mapToDouble(l -> l.remaining).sum());
        stats.put("totalBorrowed", loans.stream().mapToDouble(l -> l.principal).sum());
        
        // Calculate total interest paid
        double totalInterest = 0.0;
        for (Loan loan : loans) {
            totalInterest += (loan.principal - loan.remaining);
        }
        stats.put("totalInterestPaid", totalInterest);
        
        // Loans as lender
        long loansAsLender = activeLoans.values().stream()
            .flatMap(List::stream)
            .filter(l -> l.lenderNationId.equals(nationId))
            .count();
        stats.put("loansAsLender", loansAsLender);
        
        // Total lent
        double totalLent = activeLoans.values().stream()
            .flatMap(List::stream)
            .filter(l -> l.lenderNationId.equals(nationId))
            .mapToDouble(l -> l.principal)
            .sum();
        stats.put("totalLent", totalLent);
        
        // Pending repayments
        long overdueLoans = loans.stream()
            .filter(l -> System.currentTimeMillis() > l.dueAt)
            .count();
        stats.put("overdueLoans", overdueLoans);
        
        return stats;
    }
    
    /**
     * Get all active loans for a nation.
     */
    public synchronized List<Loan> getActiveLoans(String nationId) {
        return new ArrayList<>(activeLoans.getOrDefault(nationId, new ArrayList<>()));
    }
    
    /**
     * Calculate credit rating for a nation (based on loan history).
     */
    public synchronized double calculateCreditRating(String nationId) {
        double rating = 100.0; // Start with perfect rating
        
        List<Loan> loans = activeLoans.get(nationId);
        if (loans == null) return rating;
        
        long now = System.currentTimeMillis();
        long overdueCount = loans.stream()
            .filter(l -> now > l.dueAt && l.remaining > 0)
            .count();
        
        // Penalty for overdue loans
        rating -= overdueCount * 10.0;
        
        // Check loan-to-asset ratio
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n != null) {
            double totalDebt = loans.stream().mapToDouble(l -> l.remaining).sum();
            double assets = n.getTreasury();
            if (assets > 0) {
                double ratio = totalDebt / assets;
                if (ratio > 2.0) rating -= 20.0; // High debt ratio
                else if (ratio > 1.0) rating -= 10.0;
            } else if (totalDebt > 0) {
                rating -= 30.0; // No assets but has debt
            }
        }
        
        return Math.max(0.0, Math.min(100.0, rating));
    }
    
    /**
     * Get loan history for a nation (for credit rating calculation).
     */
    public synchronized int getLoanHistoryCount(String nationId, boolean asBorrower) {
        File[] files = loansDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return 0;
        
        int count = 0;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String checkId = asBorrower ? "borrowerNationId" : "lenderNationId";
                if (o.has(checkId) && o.get(checkId).getAsString().equals(nationId)) {
                    count++;
                }
            } catch (Exception ignored) {}
        }
        return count;
    }
    
    /**
     * Calculate recommended interest rate based on credit rating.
     */
    public synchronized double getRecommendedInterestRate(String nationId) {
        double creditRating = calculateCreditRating(nationId);
        
        // Base interest rate of 5%
        double baseRate = 5.0;
        
        // Adjust based on credit rating
        if (creditRating >= 90) {
            baseRate = 3.0; // Excellent credit
        } else if (creditRating >= 70) {
            baseRate = 4.0; // Good credit
        } else if (creditRating >= 50) {
            baseRate = 6.0; // Average credit
        } else if (creditRating >= 30) {
            baseRate = 8.0; // Poor credit
        } else {
            baseRate = 12.0; // Very poor credit
        }
        
        return baseRate;
    }
    
    /**
     * Check if nation can afford to issue a loan.
     */
    public synchronized boolean canIssueLoan(String lenderId, double amount) {
        Nation lender = plugin.getNationManager().getNationById(lenderId);
        if (lender == null) return false;
        
        // Check treasury
        if (lender.getTreasury() < amount) return false;
        
        // Check existing loans as lender (risk management)
        long existingLoans = activeLoans.values().stream()
            .flatMap(List::stream)
            .filter(l -> l.lenderNationId.equals(lenderId))
            .count();
        
        // Limit number of concurrent loans
        return existingLoans < 10;
    }
    
    /**
     * Get global banking statistics.
     */
    public synchronized Map<String, Object> getGlobalBankingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalActiveLoans = 0;
        double totalDebt = 0.0;
        double totalLent = 0.0;
        int overdueLoans = 0;
        double totalInterest = 0.0;
        
        long now = System.currentTimeMillis();
        
        for (List<Loan> loans : activeLoans.values()) {
            for (Loan loan : loans) {
                totalActiveLoans++;
                totalDebt += loan.remaining;
                totalLent += loan.principal;
                totalInterest += (loan.principal - loan.remaining);
                if (now > loan.dueAt && loan.remaining > 0) {
                    overdueLoans++;
                }
            }
        }
        
        stats.put("totalActiveLoans", totalActiveLoans);
        stats.put("totalDebt", totalDebt);
        stats.put("totalLent", totalLent);
        stats.put("totalInterest", totalInterest);
        stats.put("overdueLoans", overdueLoans);
        stats.put("averageLoanAmount", totalActiveLoans > 0 ? totalLent / totalActiveLoans : 0);
        
        // Average interest rate
        double totalInterestRate = 0.0;
        int count = 0;
        for (List<Loan> loans : activeLoans.values()) {
            for (Loan loan : loans) {
                totalInterestRate += loan.interestRate;
                count++;
            }
        }
        stats.put("averageInterestRate", count > 0 ? totalInterestRate / count : 0);
        
        // Nations with loans
        stats.put("nationsWithDebt", activeLoans.size());
        
        return stats;
    }
}

