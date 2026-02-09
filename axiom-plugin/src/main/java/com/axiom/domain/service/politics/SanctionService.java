package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.axiom.domain.service.state.NationManager;

/** Manages economic sanctions between nations. */
public class SanctionService {
    private final AXIOM plugin;
    private final NationManager nationManager;

    public SanctionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public synchronized String imposeSanctions(String sanctionerId, String targetId, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(sanctionerId) || isBlank(targetId)) return "Неверные параметры.";
        if (sanctionerId.equals(targetId)) return "Нельзя наложить санкции на свою нацию.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        Nation sanctioner = nationManager.getNationById(sanctionerId);
        if (sanctioner == null) return "Нация не найдена.";
        if (sanctioner.getTreasury() < cost) return "Недостаточно средств.";

        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return "Сервис дипломатии недоступен.";
        String err = relationService.imposeSanction(sanctionerId, targetId, 0, "sanction");
        if (err != null) return err;

        sanctioner.setTreasury(sanctioner.getTreasury() - cost);
        Nation target = nationManager.getNationById(targetId);
        if (target != null && target.getHistory() != null) {
            target.getHistory().add("Экономические санкции от " + sanctioner.getName());
        }
        try {
            nationManager.save(sanctioner);
            if (target != null) nationManager.save(target);
        } catch (Exception ignored) {}
        return "Санкции наложены.";
    }

    public synchronized boolean isSanctioned(String sanctionerId, String targetId) {
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return false;
        return relationService.isSanctioned(sanctionerId, targetId);
    }

    public synchronized String liftSanctions(String sanctionerId, String targetId) {
        if (isBlank(sanctionerId) || isBlank(targetId)) return "Неверные параметры.";
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return "Сервис дипломатии недоступен.";
        String err = relationService.liftSanction(sanctionerId, targetId);
        return err != null ? err : "Санкции сняты.";
    }

    /**
     * Get comprehensive sanction statistics.
     */
    public synchronized Map<String, Object> getSanctionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        if (isBlank(nationId)) return stats;

        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return stats;

        List<String> imposed = relationService.getSanctionsImposedBy(nationId);
        List<String> targetedByList = relationService.getSanctioningNations(nationId);

        stats.put("imposedSanctions", imposed.size());
        stats.put("sanctionedNations", new ArrayList<>(imposed));
        stats.put("targetedBy", targetedByList.size());
        stats.put("targetedByList", targetedByList);

        double economicImpact = -(targetedByList.size() * 8.0); // -8% per sanction
        stats.put("economicImpact", economicImpact);

        String rating = "ОТСУТСТВУЮТ";
        if (targetedByList.size() >= 5) rating = "КРИТИЧЕСКИЕ";
        else if (targetedByList.size() >= 3) rating = "СИЛЬНЫЕ";
        else if (targetedByList.size() >= 1) rating = "УМЕРЕННЫЕ";
        stats.put("rating", rating);

        return stats;
    }

    /**
     * Get all sanctioned nations by this nation.
     */
    public synchronized List<String> getSanctionedNations(String nationId) {
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null || isBlank(nationId)) return Collections.emptyList();
        return relationService.getSanctionsImposedBy(nationId);
    }

    /**
     * Get all nations that have sanctioned this nation.
     */
    public synchronized List<String> getSanctioningNations(String nationId) {
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null || isBlank(nationId)) return Collections.emptyList();
        return relationService.getSanctioningNations(nationId);
    }

    /**
     * Check if two nations have mutual sanctions.
     */
    public synchronized boolean hasMutualSanctions(String nationA, String nationB) {
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return false;
        return relationService.hasMutualSanctions(nationA, nationB);
    }

    /**
     * Get global sanction statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalSanctionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        DiplomacyRelationService relationService = plugin.getDiplomacyRelationService();
        if (relationService == null) return stats;

        List<DiplomacyRelationService.Sanction> all = relationService.getAllSanctions();
        int totalSanctions = all.size();
        Set<String> uniqueSanctioners = new HashSet<>();
        Map<String, Integer> sanctionsBySanctioner = new HashMap<>();
        Map<String, Integer> sanctionedByTarget = new HashMap<>();
        Set<String> pairKeys = new HashSet<>();

        for (DiplomacyRelationService.Sanction s : all) {
            if (s == null) continue;
            uniqueSanctioners.add(s.sanctionerId);
            sanctionsBySanctioner.put(s.sanctionerId, sanctionsBySanctioner.getOrDefault(s.sanctionerId, 0) + 1);
            sanctionedByTarget.put(s.targetId, sanctionedByTarget.getOrDefault(s.targetId, 0) + 1);
            pairKeys.add(s.sanctionerId + "|" + s.targetId);
        }

        int mutualCount = 0;
        for (DiplomacyRelationService.Sanction s : all) {
            if (s == null) continue;
            if (pairKeys.contains(s.targetId + "|" + s.sanctionerId)) {
                mutualCount++;
            }
        }

        stats.put("totalSanctions", totalSanctions);
        stats.put("uniqueSanctioners", uniqueSanctioners.size());
        stats.put("uniqueSanctioned", sanctionedByTarget.size());
        stats.put("averageSanctionsPerSanctioner", uniqueSanctioners.size() > 0 ?
            (double) totalSanctions / uniqueSanctioners.size() : 0);

        List<Map.Entry<String, Integer>> topSanctioners = sanctionsBySanctioner.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topSanctioners", topSanctioners);

        List<Map.Entry<String, Integer>> mostSanctioned = sanctionedByTarget.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostSanctioned", mostSanctioned);

        stats.put("mutualSanctions", mutualCount / 2); // Each mutual sanction counted twice

        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
