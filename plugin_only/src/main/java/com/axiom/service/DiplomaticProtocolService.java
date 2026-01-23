package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;

import java.util.*;

/** Manages diplomatic protocols and etiquette. */
public class DiplomaticProtocolService {
    private final AXIOM plugin;
    private final Map<String, Set<String>> violations = new HashMap<>(); // nationId -> violations

    public DiplomaticProtocolService(AXIOM plugin) {
        this.plugin = plugin;
    }

    public synchronized String recordViolation(String nationId, String violationType, String description) {
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        violations.computeIfAbsent(nationId, k -> new HashSet<>()).add(violationType + ": " + description);
        // Reputation penalty
        try {
            for (Nation other : plugin.getNationManager().getAll()) {
                if (!other.getId().equals(nationId)) {
                    plugin.getDiplomacySystem().setReputation(n, other, -2);
                }
            }
        } catch (Exception ignored) {}
        n.getHistory().add("Нарушение дипломатического протокола: " + violationType);
        try { plugin.getNationManager().save(n); } catch (Exception ignored) {}
        return "Нарушение зафиксировано.";
    }

    public synchronized List<String> getViolations(String nationId) {
        Set<String> v = violations.get(nationId);
        return v != null ? new ArrayList<>(v) : new ArrayList<>();
    }

    public synchronized String clearViolations(String nationId) {
        violations.remove(nationId);
        return "Нарушения очищены.";
    }

    public synchronized boolean canDeclareWar(String nationId) {
        // Nations with many violations may face restrictions
        Set<String> v = violations.get(nationId);
        if (v != null && v.size() > 10) {
            return false; // Too many violations - cannot declare war
        }
        return true;
    }
}

