package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;

import java.util.*;

/** Manages diplomatic protocols and etiquette. */
public class DiplomaticProtocolService {
    private final AXIOM plugin;
    private final Map<String, Set<String>> violations = new HashMap<>(); // nationId -> violations

    public DiplomaticProtocolService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized String recordViolation(String nationId, String violationType, String description) {
        if (nationId == null) return "Неверные параметры.";
        if (violationType == null || violationType.trim().isEmpty()) return "Тип нарушения не указан.";
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        String details = description != null ? description : "";
        violations.computeIfAbsent(nationId, k -> new HashSet<>()).add(violationType + ": " + details);
        // Reputation penalty
        try {
            if (plugin.getDiplomacySystem() != null) {
                for (Nation other : plugin.getNationManager().getAll()) {
                    if (!other.getId().equals(nationId)) {
                        plugin.getDiplomacySystem().setReputation(n, other, -2);
                    }
                }
            }
        } catch (Exception ignored) {}
        n.getHistory().add("Нарушение дипломатического протокола: " + violationType);
        try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        return "Нарушение зафиксировано.";
    }

    public synchronized List<String> getViolations(String nationId) {
        if (nationId == null) return new ArrayList<>();
        Set<String> v = violations.get(nationId);
        return v != null ? new ArrayList<>(v) : new ArrayList<>();
    }

    public synchronized String clearViolations(String nationId) {
        if (nationId == null) return "Неверные параметры.";
        violations.remove(nationId);
        return "Нарушения очищены.";
    }

    public synchronized boolean canDeclareWar(String nationId) {
        if (nationId == null) return true;
        // Nations with many violations may face restrictions
        Set<String> v = violations.get(nationId);
        if (v != null && v.size() > 10) {
            return false; // Too many violations - cannot declare war
        }
        return true;
    }
}

