package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.axiom.domain.service.infrastructure.StatisticsService;
import com.axiom.domain.service.infrastructure.VisualEffectsService;
import com.axiom.domain.service.politics.DiplomacyRelationService;
import com.axiom.domain.service.state.NationManager;

/** Manages trade treaties, sanctions, and embargos between nations. */
public class TradeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File treatiesDir;

    public TradeService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.treatiesDir = new File(plugin.getDataFolder(), "treaties");
        this.treatiesDir.mkdirs();
    }

    public synchronized String imposeSanction(String sanctionerId, String targetId) throws IOException {
        Nation s = nationManager.getNationById(sanctionerId);
        Nation t = nationManager.getNationById(targetId);
        if (s == null || t == null) return "Нация не найдена.";
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return "Сервис дипломатии недоступен.";
        String err = relationService.imposeSanction(sanctionerId, targetId, 0, "trade");
        if (err != null) return err;

        long timestamp = System.currentTimeMillis();
        s.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Введены санкции против " + t.getName());
        t.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Санкции от " + s.getName());
        nationManager.save(s); nationManager.save(t);
        
        // VISUAL EFFECTS: Notify both nations of sanctions
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            VisualEffectsService effectsService = plugin.getVisualEffectsService();
            if (effectsService == null) return;
            String msg1 = "§c⚠ Санкции введены против '" + t.getName() + "'";
            for (java.util.UUID citizenId : s.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    effectsService.sendActionBar(citizen, msg1);
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                }
            }
            String msg2 = "§c⚠ Санкции от '" + s.getName() + "' введены!";
            for (java.util.UUID citizenId : t.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§c§l[САНКЦИИ]", "§fНация '" + s.getName() + "' ввела санкции", 10, 80, 20);
                    effectsService.sendActionBar(citizen, msg2);
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                    citizen.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.8f);
                }
            }
        });
        
        return "Санкции введены.";
    }

    public synchronized boolean hasSanction(String nationA, String nationB) {
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return false;
        return relationService.isSanctioned(nationA, nationB) || relationService.isSanctioned(nationB, nationA);
    }

    public synchronized String createTradeTreaty(String nationAId, String nationBId) throws IOException {
        Nation a = nationManager.getNationById(nationAId);
        Nation b = nationManager.getNationById(nationBId);
        if (a == null || b == null) return "Нация не найдена.";
        File f = new File(treatiesDir, nationAId + "_" + nationBId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationA", nationAId);
        o.addProperty("nationB", nationBId);
        o.addProperty("tradeBonus", 0.1); // 10% trade bonus
        o.addProperty("signedAt", System.currentTimeMillis());
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
        
        // VISUAL EFFECTS: Celebrate trade treaty
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            VisualEffectsService effectsService = plugin.getVisualEffectsService();
            if (effectsService == null) return;
            String msg = "§b🤝 Торговый договор с '" + b.getName() + "' заключён! Бонус: +10%";
            for (java.util.UUID citizenId : a.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    effectsService.sendActionBar(citizen, msg);
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                }
            }
            msg = "§b🤝 Торговый договор с '" + a.getName() + "' заключён! Бонус: +10%";
            for (java.util.UUID citizenId : b.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    effectsService.sendActionBar(citizen, msg);
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                }
            }
        });
        
        return "Торговый договор заключён.";
    }

    /**
     * Get trade treaties for a nation (count).
     */
    public synchronized int getTradeTreatiesCount(String nationId) {
        File[] files = treatiesDir.listFiles((d, n) -> n.endsWith(".json") && !n.equals("sanctions.json"));
        if (files == null) return 0;
        int count = 0;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                if (o.has("nationA") && o.get("nationA").getAsString().equals(nationId)) count++;
                else if (o.has("nationB") && o.get("nationB").getAsString().equals(nationId)) count++;
            } catch (Exception ignored) {}
        }
        return count;
    }
    
    /**
     * Check if nation has trade treaty with another nation.
     */
    public synchronized boolean hasTradeTreaty(String nationAId, String nationBId) {
        File f = new File(treatiesDir, nationAId + "_" + nationBId + ".json");
        if (f.exists()) return true;
        f = new File(treatiesDir, nationBId + "_" + nationAId + ".json");
        return f.exists();
    }
    
    /**
     * Get comprehensive trade statistics for a nation.
     */
    public synchronized Map<String, Object> getTradeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("tradeTreaties", getTradeTreatiesCount(nationId));
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        int imposed = relationService != null ? relationService.getSanctionsImposedBy(nationId).size() : 0;
        stats.put("sanctionsImposed", imposed);
        
        // Count nations that have sanctioned this nation
        int sanctionedBy = relationService != null ? relationService.getSanctioningNations(nationId).size() : 0;
        stats.put("sanctionedBy", sanctionedBy);
        
        // Trade volume from StatisticsService
        StatisticsService statisticsService = plugin.getStatisticsService();
        if (statisticsService != null) {
            StatisticsService.NationStats stat = statisticsService.getStats(nationId);
            stats.put("totalTradeVolume", stat.getTotalTradeVolume());
        }
        
        // Trade routes could be integrated here in future
        
        return stats;
    }
    
    /**
     * Remove sanction.
     */
    public synchronized String removeSanction(String sanctionerId, String targetId) throws IOException {
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return "Сервис дипломатии недоступен.";
        String err = relationService.liftSanction(sanctionerId, targetId);
        if (err != null) return err;
        
        // VISUAL EFFECTS
        Nation s = nationManager.getNationById(sanctionerId);
        Nation t = nationManager.getNationById(targetId);
        if (s != null && t != null) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                VisualEffectsService effectsService = plugin.getVisualEffectsService();
                if (effectsService == null) return;
                String msg = "§a✓ Санкции против '" + t.getName() + "' сняты";
                for (java.util.UUID citizenId : s.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        effectsService.sendActionBar(citizen, msg);
                    }
                }
                msg = "§a✓ Санкции от '" + s.getName() + "' сняты";
                for (java.util.UUID citizenId : t.getCitizens()) {
                    org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                    if (citizen != null && citizen.isOnline()) {
                        effectsService.sendActionBar(citizen, msg);
                    }
                }
            });
        }
        
        return "Санкции сняты.";
    }
    
    /**
     * Get all trade partners (nations with active treaties).
     */
    public synchronized List<String> getTradePartners(String nationId) {
        List<String> partners = new ArrayList<>();
        File[] files = treatiesDir.listFiles((d, n) -> n.endsWith(".json") && !n.equals("sanctions.json"));
        if (files == null) return partners;
        
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                if (o.has("nationA") && o.get("nationA").getAsString().equals(nationId) && o.has("nationB")) {
                    partners.add(o.get("nationB").getAsString());
                } else if (o.has("nationB") && o.get("nationB").getAsString().equals(nationId) && o.has("nationA")) {
                    partners.add(o.get("nationA").getAsString());
                }
            } catch (Exception ignored) {}
        }
        
        return partners;
    }
    
    /**
     * Calculate trade volume bonus from treaties.
     */
    public synchronized double getTradeBonus(String nationId) {
        int treaties = getTradeTreatiesCount(nationId);
        // Each treaty provides +5% trade bonus
        return 1.0 + (treaties * 0.05);
    }
    
    /**
     * Get trade efficiency rating.
     */
    public synchronized String getTradeEfficiencyRating(String nationId) {
        Map<String, Object> stats = getTradeStatistics(nationId);
        int treaties = (Integer) stats.get("tradeTreaties");
        int sanctionedBy = (Integer) stats.get("sanctionedBy");
        
        double efficiency = treaties * 10.0 - sanctionedBy * 20.0;
        
        if (efficiency >= 80) return "ОТЛИЧНО";
        if (efficiency >= 60) return "ХОРОШО";
        if (efficiency >= 40) return "СРЕДНЕ";
        if (efficiency >= 20) return "НИЗКО";
        return "ОЧЕНЬ_НИЗКО";
    }
    
    /**
     * Calculate potential trade income (hourly estimate).
     */
    public synchronized double calculateTradeIncome(String nationId) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return 0.0;
        
        int territories = n.getClaimedChunkKeys() != null ? n.getClaimedChunkKeys().size() : 0;
        double baseIncome = territories * 1.0;
        
        // Trade treaties bonus
        double bonus = getTradeBonus(nationId);
        baseIncome *= bonus;
        
        // Sanctions penalty
        Map<String, Object> stats = getTradeStatistics(nationId);
        int sanctionedBy = (Integer) stats.get("sanctionedBy");
        baseIncome *= Math.max(0.5, 1.0 - (sanctionedBy * 0.1)); // -10% per sanction
        
        return baseIncome;
    }
    
    /**
     * Get global trade statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalTradeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count trade treaties
        File[] treatyFiles = treatiesDir.listFiles((d, n) -> n.endsWith(".json") && !n.equals("sanctions.json"));
        int totalTreaties = treatyFiles != null ? treatyFiles.length : 0;
        stats.put("totalTreaties", totalTreaties);
        
        // Sanctions statistics
        int totalSanctions = 0;
        Map<String, Integer> sanctionsByNation = new HashMap<>(); // nation -> count imposed
        Map<String, Integer> sanctionedByNation = new HashMap<>(); // nation -> count received
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService != null) {
            for (DiplomacyRelationService.Sanction s : relationService.getAllSanctions()) {
                if (s == null) continue;
                totalSanctions++;
                String sanctionerId = s.getSanctionerId();
                String targetId = s.getTargetId();
                sanctionsByNation.put(sanctionerId, sanctionsByNation.getOrDefault(sanctionerId, 0) + 1);
                sanctionedByNation.put(targetId, sanctionedByNation.getOrDefault(targetId, 0) + 1);
            }
        }
        
        stats.put("totalSanctions", totalSanctions);
        stats.put("sanctionsByNation", sanctionsByNation);
        stats.put("sanctionedByNation", sanctionedByNation);
        stats.put("nationsImposingSanctions", sanctionsByNation.size());
        stats.put("nationsSanctioned", sanctionedByNation.size());
        
        // Trade treaties by nation
        Map<String, Integer> treatiesByNation = new HashMap<>();
        for (Nation n : nationManager.getAll()) {
            treatiesByNation.put(n.getId(), getTradeTreatiesCount(n.getId()));
        }
        stats.put("treatiesByNation", treatiesByNation);
        
        // Average treaties per nation
        int totalTreatiesCount = treatiesByNation.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("averageTreatiesPerNation", treatiesByNation.size() > 0 ?
            (double) totalTreatiesCount / treatiesByNation.size() : 0);
        
        // Trade efficiency by nation
        Map<String, String> efficiencyByNation = new HashMap<>();
        for (Nation n : nationManager.getAll()) {
            efficiencyByNation.put(n.getId(), getTradeEfficiencyRating(n.getId()));
        }
        stats.put("efficiencyByNation", efficiencyByNation);
        
        // Trade income by nation
        Map<String, Double> incomeByNation = new HashMap<>();
        for (Nation n : nationManager.getAll()) {
            incomeByNation.put(n.getId(), calculateTradeIncome(n.getId()));
        }
        stats.put("incomeByNation", incomeByNation);
        double totalIncome = incomeByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("totalTradeIncome", totalIncome);
        stats.put("averageTradeIncome", incomeByNation.size() > 0 ? totalIncome / incomeByNation.size() : 0);
        
        // Top nations by treaties
        List<Map.Entry<String, Integer>> topByTreaties = treatiesByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByTreaties", topByTreaties);
        
        // Top nations by trade income
        List<Map.Entry<String, Double>> topByIncome = incomeByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByIncome", topByIncome);
        
        // Top nations imposing sanctions
        List<Map.Entry<String, Integer>> topBySanctions = sanctionsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySanctions", topBySanctions);
        
        // Top nations being sanctioned
        List<Map.Entry<String, Integer>> topBySanctioned = sanctionedByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySanctioned", topBySanctioned);
        
        // Efficiency distribution
        Map<String, Integer> efficiencyDistribution = new HashMap<>();
        for (String efficiency : efficiencyByNation.values()) {
            efficiencyDistribution.put(efficiency, efficiencyDistribution.getOrDefault(efficiency, 0) + 1);
        }
        stats.put("efficiencyDistribution", efficiencyDistribution);
        
        return stats;
    }
}
