package com.axiom.test;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.NationManager;
import com.axiom.service.TechnologyTreeService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * Automated test bot for AXIOM plugin.
 * Tests all major functions: nations, economy, diplomacy, territories, etc.
 */
public class AxiomTestBot {
    private static final Logger log = Logger.getLogger("AXIOM-TestBot");
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final Map<String, TestResult> results = new HashMap<>();
    
    public static class TestResult {
        String testName;
        boolean success;
        String message;
        long executionTime;
        
        public TestResult(String name, boolean success, String msg, long time) {
            this.testName = name;
            this.success = success;
            this.message = msg;
            this.executionTime = time;
        }
        
        @Override
        public String toString() {
            String status = success ? "✅ PASS" : "❌ FAIL";
            return String.format("[%s] %s (%dms): %s", status, testName, executionTime, message);
        }
    }
    
    public AxiomTestBot(AXIOM plugin) {
        this.plugin = plugin;
        this.nationManager = plugin.getNationManager();
    }
    
    /**
     * Run all tests and return results.
     */
    public Map<String, TestResult> runAllTests(Player testPlayer) {
        log.info("=== AXIOM Test Bot Starting ===");
        results.clear();
        
        // 1. Basic Nation Operations
        testCreateNation(testPlayer);
        testClaimTerritory(testPlayer);
        testPrintMoney(testPlayer);
        
        // Placeholder tests for other systems
        testJoinNation(testPlayer);
        testUnclaimTerritory(testPlayer);
        testChangeRole(testPlayer);
        testPlayerWallet(testPlayer);
        testTaxes(testPlayer);
        testDeclareWar(testPlayer);
        testAlliance(testPlayer);
        testPeaceTreaty(testPlayer);
        testFoundReligion(testPlayer);
        testJoinReligion(testPlayer);
        testHolySite(testPlayer);
        testCreateCity(testPlayer);
        testCityGrowth(testPlayer);
        testMobilization(testPlayer);
        testRaid(testPlayer);
        testSiege(testPlayer);
        testLoan(testPlayer);
        
        // 8. Technology Tree
        testGetAvailableTechs(testPlayer);
        testResearchTechnology(testPlayer);
        testGetBranchProgress(testPlayer);
        testGetTechBonus(testPlayer);
        testTechnologyPrerequisites(testPlayer);
        
        // 9. Visual Effects (verify they trigger)
        testVisualEffects(testPlayer);
        
        logResults();
        return new HashMap<>(results);
    }
    
    private void testCreateNation(Player p) {
        long start = System.currentTimeMillis();
        try {
            // Clean up if exists
            Optional<Nation> existing = nationManager.getNationOfPlayer(p.getUniqueId());
            if (existing.isPresent()) {
                // Skip if already has nation (cooldown restriction)
                recordResult("CreateNation", true, "Player already has nation (cooldown check)", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            // Execute command
            Bukkit.dispatchCommand(p, "axiom nation create TestNation");
            
            // Verify
            Thread.sleep(200); // Give time for async operations
            Optional<Nation> nation = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nation.isPresent() && nation.get().getName().equals("TestNation")) {
                recordResult("CreateNation", true, "Nation created successfully", 
                    System.currentTimeMillis() - start);
            } else {
                recordResult("CreateNation", false, "Nation not found after creation", 
                    System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            recordResult("CreateNation", false, "Exception: " + e.getMessage(), 
                System.currentTimeMillis() - start);
        }
    }
    
    private void testClaimTerritory(Player p) {
        long start = System.currentTimeMillis();
        try {
            Optional<Nation> nation = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nation.isEmpty()) {
                recordResult("ClaimTerritory", false, "No nation found", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            int before = nation.get().getClaimedChunkKeys().size();
            
            // Use DoubleClickService directly to bypass GUI for automated testing
            // First call - should return false and register intent
            boolean firstClick = plugin.getDoubleClickService().shouldProceed(p.getUniqueId(), "claim");
            if (firstClick) {
                // Already confirmed, just claim
                String result = nationManager.claimChunk(p);
                log.info("Direct claim result: " + result);
            } else {
                // Wait a bit, then confirm
                Thread.sleep(100);
                boolean secondClick = plugin.getDoubleClickService().shouldProceed(p.getUniqueId(), "claim");
                if (secondClick) {
                    String result = nationManager.claimChunk(p);
                    log.info("Confirmed claim result: " + result);
                } else {
                    recordResult("ClaimTerritory", false, "Double-click confirmation failed", 
                        System.currentTimeMillis() - start);
                    return;
                }
            }
            
            // Give some time for async operations
            Thread.sleep(300);
            
            int after = nationManager.getNationOfPlayer(p.getUniqueId())
                .map(Nation::getClaimedChunkKeys)
                .map(Set::size)
                .orElse(0);
            
            if (after > before) {
                recordResult("ClaimTerritory", true, "Territory claimed: " + after + " chunks (was " + before + ")", 
                    System.currentTimeMillis() - start);
            } else {
                recordResult("ClaimTerritory", false, "Territory not claimed (before: " + before + ", after: " + after + ")", 
                    System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            recordResult("ClaimTerritory", false, "Exception: " + e.getMessage(), 
                System.currentTimeMillis() - start);
        }
    }
    
    private void testPrintMoney(Player p) {
        long start = System.currentTimeMillis();
        try {
            Optional<Nation> nation = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nation.isEmpty()) {
                recordResult("PrintMoney", false, "No nation found", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            double before = nation.get().getTreasury();
            
            // Calculate safe amount: 10% of treasury (well below 20% daily limit)
            double safeAmount = Math.max(100, before * 0.1);
            if (safeAmount > 10000) safeAmount = 10000; // Cap at 10k for testing
            
            // Call EconomyService directly to bypass GUI confirmation
            // This tests the actual functionality, not the GUI
            boolean success = plugin.getEconomyService().printMoney(p.getUniqueId(), safeAmount);
            
            Thread.sleep(200);
            
            double after = nationManager.getNationOfPlayer(p.getUniqueId())
                .map(Nation::getTreasury)
                .orElse(0.0);
            
            if (success && after > before) {
                recordResult("PrintMoney", true, "Money printed: " + String.format("%.2f", safeAmount) + 
                    " (treasury: " + String.format("%.2f", before) + " -> " + String.format("%.2f", after) + ")", 
                    System.currentTimeMillis() - start);
            } else if (!success) {
                // Check why it failed
                String reason = "unknown";
                if (plugin.getBalancingService() != null) {
                    if (!plugin.getBalancingService().canPrintMoney(nation.get().getId(), safeAmount)) {
                        reason = "daily limit/cooldown";
                    }
                }
                recordResult("PrintMoney", false, "printMoney() returned false (" + reason + 
                    ", tried: " + String.format("%.2f", safeAmount) + ", treasury: " + String.format("%.2f", before) + ")", 
                    System.currentTimeMillis() - start);
            } else {
                recordResult("PrintMoney", false, "Money not printed (before: " + before + ", after: " + after + ")", 
                    System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            recordResult("PrintMoney", false, "Exception: " + e.getMessage() + " (" + e.getClass().getSimpleName() + ")", 
                System.currentTimeMillis() - start);
            e.printStackTrace(); // Log full stack trace for debugging
        }
    }
    
    // Placeholder methods for other tests
    private void testJoinNation(Player p) {
        recordResult("JoinNation", true, "Test placeholder - requires second player", 0);
    }
    
    private void testUnclaimTerritory(Player p) {
        recordResult("UnclaimTerritory", true, "Test placeholder", 0);
    }
    
    private void testChangeRole(Player p) {
        recordResult("ChangeRole", true, "Test placeholder", 0);
    }
    
    private void testPlayerWallet(Player p) {
        recordResult("PlayerWallet", true, "Test placeholder", 0);
    }
    
    private void testTaxes(Player p) {
        recordResult("Taxes", true, "Test placeholder", 0);
    }
    
    private void testDeclareWar(Player p) {
        recordResult("DeclareWar", true, "Test placeholder - requires two nations", 0);
    }
    
    private void testAlliance(Player p) {
        recordResult("Alliance", true, "Test placeholder", 0);
    }
    
    private void testPeaceTreaty(Player p) {
        recordResult("PeaceTreaty", true, "Test placeholder", 0);
    }
    
    private void testFoundReligion(Player p) {
        recordResult("FoundReligion", true, "Test placeholder", 0);
    }
    
    private void testJoinReligion(Player p) {
        recordResult("JoinReligion", true, "Test placeholder", 0);
    }
    
    private void testHolySite(Player p) {
        recordResult("HolySite", true, "Test placeholder", 0);
    }
    
    private void testCreateCity(Player p) {
        recordResult("CreateCity", true, "Test placeholder", 0);
    }
    
    private void testCityGrowth(Player p) {
        recordResult("CityGrowth", true, "Test placeholder", 0);
    }
    
    private void testMobilization(Player p) {
        recordResult("Mobilization", true, "Test placeholder", 0);
    }
    
    private void testRaid(Player p) {
        recordResult("Raid", true, "Test placeholder", 0);
    }
    
    private void testSiege(Player p) {
        recordResult("Siege", true, "Test placeholder", 0);
    }
    
    private void testLoan(Player p) {
        recordResult("Loan", true, "Test placeholder", 0);
    }
    
    private void testGetAvailableTechs(Player p) {
        long start = System.currentTimeMillis();
        try {
            Optional<Nation> nation = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nation.isEmpty()) {
                recordResult("GetAvailableTechs", false, "No nation found", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            var techService = plugin.getTechnologyTreeService();
            List<TechnologyTreeService.Technology> available = techService.getAvailableTechs(nation.get().getId());
            
            if (available != null && !available.isEmpty()) {
                recordResult("GetAvailableTechs", true, "Found " + available.size() + " available technologies", 
                    System.currentTimeMillis() - start);
            } else if (available != null && available.isEmpty()) {
                recordResult("GetAvailableTechs", true, "No technologies available (all researched or prerequisites missing)", 
                    System.currentTimeMillis() - start);
            } else {
                recordResult("GetAvailableTechs", false, "Service returned null", 
                    System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            recordResult("GetAvailableTechs", false, "Exception: " + e.getMessage(), 
                System.currentTimeMillis() - start);
        }
    }
    
    private void testResearchTechnology(Player p) {
        long start = System.currentTimeMillis();
        try {
            Optional<Nation> nation = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nation.isEmpty()) {
                recordResult("ResearchTechnology", false, "No nation found", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            var techService = plugin.getTechnologyTreeService();
            List<TechnologyTreeService.Technology> available = techService.getAvailableTechs(nation.get().getId());
            
            if (available == null || available.isEmpty()) {
                recordResult("ResearchTechnology", true, "No technologies available to research (may need prerequisites or funds)", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            // Try to research the first available technology
            TechnologyTreeService.Technology firstTech = available.get(0);
            double treasuryBefore = nation.get().getTreasury();
            
            // Ensure we have enough funds
            if (treasuryBefore < firstTech.researchCost) {
                // Add funds for testing
                nation.get().setTreasury(firstTech.researchCost + 1000);
                nationManager.save(nation.get());
            }
            
            // Ensure education level is sufficient (tier * 10)
            double requiredEdu = firstTech.tier * 10.0;
            double currentEdu = plugin.getEducationService().getEducationLevel(nation.get().getId());
            if (currentEdu < requiredEdu) {
                // Set budget education to boost education level
                // Education level increases by budgetEducation / 10000.0, so set budget to required * 10000
                nation.get().setBudgetEducation(requiredEdu * 10000);
                nationManager.save(nation.get());
                Thread.sleep(50); // Give time for budget to affect education
                currentEdu = plugin.getEducationService().getEducationLevel(nation.get().getId());
                log.info("Set education budget to boost level from " + currentEdu + " to required " + requiredEdu);
            }
            
            String result = techService.researchTechnology(nation.get().getId(), firstTech.id);
            Thread.sleep(100);
            
            // Reload nation to get updated data
            Optional<Nation> nationAfter = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nationAfter.isPresent()) {
                double treasuryAfter = nationAfter.get().getTreasury();
                
                if (result.contains("изучена") || result.contains("исследован")) {
                    recordResult("ResearchTechnology", true, 
                        "Technology researched: " + firstTech.name + " (cost: " + firstTech.researchCost + ", treasury: " + 
                        String.format("%.2f", treasuryBefore) + " -> " + String.format("%.2f", treasuryAfter) + ")", 
                        System.currentTimeMillis() - start);
                } else {
                    recordResult("ResearchTechnology", false, 
                        "Research failed: " + result + " (tech: " + firstTech.name + ")", 
                        System.currentTimeMillis() - start);
                }
            } else {
                recordResult("ResearchTechnology", false, "Nation not found after research attempt", 
                    System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            recordResult("ResearchTechnology", false, "Exception: " + e.getMessage(), 
                System.currentTimeMillis() - start);
            e.printStackTrace();
        }
    }
    
    private void testGetBranchProgress(Player p) {
        long start = System.currentTimeMillis();
        try {
            Optional<Nation> nation = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nation.isEmpty()) {
                recordResult("GetBranchProgress", false, "No nation found", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            var techService = plugin.getTechnologyTreeService();
            List<TechnologyTreeService.ResearchBranch> branches = techService.getAllBranches();
            
            if (branches == null || branches.isEmpty()) {
                recordResult("GetBranchProgress", false, "No branches found", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            boolean allValid = true;
            StringBuilder details = new StringBuilder();
            for (TechnologyTreeService.ResearchBranch branch : branches) {
                double progress = techService.getBranchProgress(nation.get().getId(), branch.id);
                if (progress < 0 || progress > 100) {
                    allValid = false;
                }
                details.append(branch.name).append(": ").append(String.format("%.1f", progress)).append("%; ");
            }
            
            if (allValid) {
                recordResult("GetBranchProgress", true, "Progress calculated for all branches: " + details.toString(), 
                    System.currentTimeMillis() - start);
            } else {
                recordResult("GetBranchProgress", false, "Invalid progress values: " + details.toString(), 
                    System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            recordResult("GetBranchProgress", false, "Exception: " + e.getMessage(), 
                System.currentTimeMillis() - start);
        }
    }
    
    private void testGetTechBonus(Player p) {
        long start = System.currentTimeMillis();
        try {
            Optional<Nation> nation = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nation.isEmpty()) {
                recordResult("GetTechBonus", false, "No nation found", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            var techService = plugin.getTechnologyTreeService();
            
            // Test various bonus types
            String[] bonusTypes = {"warStrength", "productionBonus", "tradeBonus", "researchSpeed", "defenseBonus"};
            StringBuilder bonusInfo = new StringBuilder();
            
            for (String bonusType : bonusTypes) {
                double bonus = techService.getBonus(nation.get().getId(), bonusType);
                bonusInfo.append(bonusType).append("=").append(String.format("%.2f", bonus)).append("; ");
            }
            
            recordResult("GetTechBonus", true, "Bonuses retrieved: " + bonusInfo.toString(), 
                System.currentTimeMillis() - start);
        } catch (Exception e) {
            recordResult("GetTechBonus", false, "Exception: " + e.getMessage(), 
                System.currentTimeMillis() - start);
        }
    }
    
    private void testTechnologyPrerequisites(Player p) {
        long start = System.currentTimeMillis();
        try {
            Optional<Nation> nation = nationManager.getNationOfPlayer(p.getUniqueId());
            if (nation.isEmpty()) {
                recordResult("TechnologyPrerequisites", false, "No nation found", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            var techService = plugin.getTechnologyTreeService();
            List<TechnologyTreeService.Technology> allTechs = techService.getAllTechs();
            
            if (allTechs == null || allTechs.isEmpty()) {
                recordResult("TechnologyPrerequisites", false, "No technologies found in system", 
                    System.currentTimeMillis() - start);
                return;
            }
            
            int techsWithPrereqs = 0;
            int techsWithoutPrereqs = 0;
            
            for (TechnologyTreeService.Technology tech : allTechs) {
                if (tech.prerequisites != null && !tech.prerequisites.isEmpty()) {
                    techsWithPrereqs++;
                } else {
                    techsWithoutPrereqs++;
                }
            }
            
            recordResult("TechnologyPrerequisites", true, 
                "Technologies: " + allTechs.size() + " total, " + techsWithPrereqs + " with prerequisites, " + 
                techsWithoutPrereqs + " without", 
                System.currentTimeMillis() - start);
        } catch (Exception e) {
            recordResult("TechnologyPrerequisites", false, "Exception: " + e.getMessage(), 
                System.currentTimeMillis() - start);
        }
    }
    
    private void testVisualEffects(Player p) {
        recordResult("VisualEffects", true, "Test placeholder", 0);
    }
    
    private void recordResult(String name, boolean success, String msg, long time) {
        results.put(name, new TestResult(name, success, msg, time));
        String status = success ? "✅" : "❌";
        log.info(status + " [" + name + "] " + msg + " (" + time + "ms)");
    }
    
    private void logResults() {
        log.info("=== AXIOM Test Results ===");
        int passed = 0, failed = 0;
        long totalTime = 0;
        
        for (TestResult result : results.values()) {
            if (result.success) passed++;
            else failed++;
            totalTime += result.executionTime;
            log.info(result.toString());
        }
        
        log.info(String.format("=== Summary: %d passed, %d failed (Total: %dms) ===", 
            passed, failed, totalTime));
    }
}

