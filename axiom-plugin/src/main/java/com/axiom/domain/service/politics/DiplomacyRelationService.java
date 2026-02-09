package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.axiom.domain.service.state.NationManager;

/**
 * Unified diplomacy relations and sanctions storage.
 */
public class DiplomacyRelationService {
    public enum RelationStatus {
        NEUTRAL,
        ALLIANCE,
        WAR,
        CEASEFIRE
    }

    public static class Relation {
        String nationA;
        String nationB;
        RelationStatus status;
        long startedAt;
        long expiresAt;
        String reason;
    }

    public static class Sanction {
        String sanctionerId;
        String targetId;
        long startedAt;
        long expiresAt;
        String reason;

        public String getSanctionerId() {
            return sanctionerId;
        }

        public String getTargetId() {
            return targetId;
        }
    }

    private static final long WAR_DEFAULT_DURATION_MS = 24L * 60L * 60L * 1000L;
    private static final long PERMANENT_DURATION_MS = Long.MAX_VALUE;

    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File diplomacyDir;
    private final File relationsFile;
    private final File sanctionsFile;

    private final Map<String, Relation> relations = new HashMap<>();
    private final Map<String, Sanction> sanctions = new HashMap<>();

    public DiplomacyRelationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.diplomacyDir = new File(plugin.getDataFolder(), "diplomacy");
        this.diplomacyDir.mkdirs();
        this.relationsFile = new File(diplomacyDir, "relations.json");
        this.sanctionsFile = new File(diplomacyDir, "sanctions.json");

        loadRelations();
        loadSanctions();
        importLegacySanctions();
        importLegacyCeasefires();
        bootstrapFromNationData();
        cleanupExpired();
        reconcileNationLinks();

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpired, 0, 20L * 60L * 5L);
    }

    public synchronized RelationStatus getStatus(String nationA, String nationB) {
        if (isBlank(nationA) || isBlank(nationB)) return RelationStatus.NEUTRAL;
        Relation rel = getRelationInternal(nationA, nationB);
        return rel != null ? rel.status : RelationStatus.NEUTRAL;
    }

    public synchronized boolean isAtWar(String nationA, String nationB) {
        return getStatus(nationA, nationB) == RelationStatus.WAR;
    }

    public synchronized boolean isAllied(String nationA, String nationB) {
        return getStatus(nationA, nationB) == RelationStatus.ALLIANCE;
    }

    public synchronized boolean isCeasefire(String nationA, String nationB) {
        return getStatus(nationA, nationB) == RelationStatus.CEASEFIRE;
    }

    public synchronized boolean hasActiveWarWithAny(String nationId) {
        if (isBlank(nationId)) return false;
        long now = System.currentTimeMillis();
        for (Relation rel : relations.values()) {
            if (rel == null || rel.status != RelationStatus.WAR) continue;
            if (rel.expiresAt > 0 && rel.expiresAt <= now) continue;
            if (nationId.equals(rel.nationA) || nationId.equals(rel.nationB)) return true;
        }
        return false;
    }

    public synchronized List<Relation> getRelationsForNation(String nationId) {
        if (isBlank(nationId)) return Collections.emptyList();
        long now = System.currentTimeMillis();
        List<Relation> result = new ArrayList<>();
        for (Relation rel : relations.values()) {
            if (rel == null) continue;
            if (rel.expiresAt > 0 && rel.expiresAt <= now) continue;
            if (nationId.equals(rel.nationA) || nationId.equals(rel.nationB)) {
                result.add(copyRelation(rel));
            }
        }
        return result;
    }

    public synchronized int countRelationsByStatus(RelationStatus status) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (Relation rel : relations.values()) {
            if (rel == null || rel.status != status) continue;
            if (rel.expiresAt > 0 && rel.expiresAt <= now) continue;
            count++;
        }
        return count;
    }

    public synchronized int countRelationsForNation(String nationId, RelationStatus status) {
        if (isBlank(nationId)) return 0;
        long now = System.currentTimeMillis();
        int count = 0;
        for (Relation rel : relations.values()) {
            if (rel == null || rel.status != status) continue;
            if (rel.expiresAt > 0 && rel.expiresAt <= now) continue;
            if (nationId.equals(rel.nationA) || nationId.equals(rel.nationB)) count++;
        }
        return count;
    }

    public synchronized String setStatus(String nationA, String nationB, RelationStatus status, long durationMs, String reason) {
        String validation = validateParticipants(nationA, nationB);
        if (validation != null) return validation;
        if (status == null) return "Некорректный статус.";

        RelationStatus current = getStatus(nationA, nationB);
        if (current == status && status != RelationStatus.NEUTRAL) return "Статус уже установлен.";

        String transitionError = validateTransition(nationA, nationB, current, status);
        if (transitionError != null) return transitionError;

        if (status == RelationStatus.NEUTRAL) {
            removeRelation(nationA, nationB);
            syncNationSets(nationA, nationB, RelationStatus.NEUTRAL);
            saveRelations();
            return null;
        }

        long now = System.currentTimeMillis();
        long expiresAt = resolveExpiresAt(status, now, durationMs);
        if (status == RelationStatus.CEASEFIRE && expiresAt <= now) {
            return "Неверная длительность перемирия.";
        }

        Relation rel = new Relation();
        rel.nationA = canonicalA(nationA, nationB);
        rel.nationB = canonicalB(nationA, nationB);
        rel.status = status;
        rel.startedAt = now;
        rel.expiresAt = expiresAt;
        rel.reason = reason != null ? reason : "";
        relations.put(relationKey(nationA, nationB), rel);
        syncNationSets(nationA, nationB, status);
        saveRelations();
        return null;
    }

    public synchronized String imposeSanction(String sanctionerId, String targetId, long durationMs, String reason) {
        String validation = validateParticipants(sanctionerId, targetId);
        if (validation != null) return validation;
        if (sanctionerId.equals(targetId)) return "Нельзя наложить санкции на свою нацию.";
        if (isAllied(sanctionerId, targetId)) return "Нельзя вводить санкции против союзника.";

        String key = sanctionKey(sanctionerId, targetId);
        Sanction existing = sanctions.get(key);
        if (existing != null && !isExpired(existing)) {
            return "Санкции уже наложены.";
        }

        long now = System.currentTimeMillis();
        long expiresAt = resolveExpiresAt(RelationStatus.NEUTRAL, now, durationMs);
        Sanction sanction = new Sanction();
        sanction.sanctionerId = sanctionerId;
        sanction.targetId = targetId;
        sanction.startedAt = now;
        sanction.expiresAt = expiresAt;
        sanction.reason = reason != null ? reason : "";
        sanctions.put(key, sanction);
        saveSanctions();
        return null;
    }

    public synchronized String liftSanction(String sanctionerId, String targetId) {
        if (isBlank(sanctionerId) || isBlank(targetId)) return "Неверные параметры.";
        if (!sanctions.containsKey(sanctionKey(sanctionerId, targetId))) return "Санкций нет.";
        sanctions.remove(sanctionKey(sanctionerId, targetId));
        saveSanctions();
        return null;
    }

    public synchronized boolean isSanctioned(String sanctionerId, String targetId) {
        if (isBlank(sanctionerId) || isBlank(targetId)) return false;
        Sanction s = sanctions.get(sanctionKey(sanctionerId, targetId));
        if (s == null) return false;
        if (isExpired(s)) {
            sanctions.remove(sanctionKey(sanctionerId, targetId));
            saveSanctions();
            return false;
        }
        return true;
    }

    public synchronized boolean hasMutualSanctions(String nationA, String nationB) {
        return isSanctioned(nationA, nationB) && isSanctioned(nationB, nationA);
    }

    public synchronized List<String> getSanctionsImposedBy(String nationId) {
        if (isBlank(nationId)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Sanction s : sanctions.values()) {
            if (s == null || isExpired(s)) continue;
            if (nationId.equals(s.sanctionerId)) {
                result.add(s.targetId);
            }
        }
        return result;
    }

    public synchronized List<String> getSanctioningNations(String nationId) {
        if (isBlank(nationId)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Sanction s : sanctions.values()) {
            if (s == null || isExpired(s)) continue;
            if (nationId.equals(s.targetId)) {
                result.add(s.sanctionerId);
            }
        }
        return result;
    }

    public synchronized List<Sanction> getAllSanctions() {
        List<Sanction> result = new ArrayList<>();
        for (Sanction s : sanctions.values()) {
            if (s == null || isExpired(s)) continue;
            result.add(copySanction(s));
        }
        return result;
    }

    public synchronized void cleanupNation(String nationId) {
        if (isBlank(nationId)) return;
        boolean changed = false;

        List<String> relationKeys = new ArrayList<>(relations.keySet());
        for (String key : relationKeys) {
            Relation rel = relations.get(key);
            if (rel == null) continue;
            if (nationId.equals(rel.nationA) || nationId.equals(rel.nationB)) {
                String other = nationId.equals(rel.nationA) ? rel.nationB : rel.nationA;
                relations.remove(key);
                syncNationSets(nationId, other, RelationStatus.NEUTRAL);
                changed = true;
            }
        }

        List<String> sanctionKeys = new ArrayList<>(sanctions.keySet());
        for (String key : sanctionKeys) {
            Sanction s = sanctions.get(key);
            if (s == null) continue;
            if (nationId.equals(s.sanctionerId) || nationId.equals(s.targetId)) {
                sanctions.remove(key);
                changed = true;
            }
        }

        if (changed) {
            saveRelations();
            saveSanctions();
        }
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        List<Relation> expiredRelations = new ArrayList<>();
        for (Relation rel : relations.values()) {
            if (rel == null) continue;
            if (rel.expiresAt > 0 && rel.expiresAt <= now) {
                expiredRelations.add(rel);
            }
        }
        for (Relation rel : expiredRelations) {
            expireRelation(rel);
        }

        List<String> expiredSanctions = new ArrayList<>();
        for (Map.Entry<String, Sanction> entry : sanctions.entrySet()) {
            Sanction s = entry.getValue();
            if (s != null && s.expiresAt > 0 && s.expiresAt <= now) {
                expiredSanctions.add(entry.getKey());
            }
        }
        for (String key : expiredSanctions) {
            sanctions.remove(key);
        }
        if (!expiredRelations.isEmpty()) {
            saveRelations();
        }
        if (!expiredSanctions.isEmpty()) {
            saveSanctions();
        }
    }

    private void expireRelation(Relation rel) {
        if (rel == null) return;
        relations.remove(relationKey(rel.nationA, rel.nationB));
        if (rel.status == RelationStatus.WAR) {
            notifyWarEnded(rel.nationA, rel.nationB);
        }
        syncNationSets(rel.nationA, rel.nationB, RelationStatus.NEUTRAL);
    }

    private void notifyWarEnded(String nationAId, String nationBId) {
        Nation a = nationManager.getNationById(nationAId);
        Nation b = nationManager.getNationById(nationBId);
        if (a == null || b == null) return;
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = "§a☮ Война с '" + b.getName() + "' завершилась. Warzone деактивирован.";
            for (java.util.UUID citizenId : a.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[ВОЙНА ЗАВЕРШЕНА]", "§fМир с '" + b.getName() + "'", 10, 80, 20);
                    if (plugin.getVisualEffectsService() != null) {
                        plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                    }
                    org.bukkit.Location loc = citizen.getLocation();
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                        citizen.playSound(loc, org.bukkit.Sound.BLOCK_BELL_USE, 0.7f, 1.0f);
                    }
                }
            }
            msg = "§a☮ Война с '" + a.getName() + "' завершилась. Warzone деактивирован.";
            for (java.util.UUID citizenId : b.getCitizens()) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    citizen.sendTitle("§a§l[ВОЙНА ЗАВЕРШЕНА]", "§fМир с '" + a.getName() + "'", 10, 80, 20);
                    if (plugin.getVisualEffectsService() != null) {
                        plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                    }
                    org.bukkit.Location loc = citizen.getLocation();
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                        citizen.playSound(loc, org.bukkit.Sound.BLOCK_BELL_USE, 0.7f, 1.0f);
                    }
                }
            }
        });
    }

    private void syncNationSets(String nationAId, String nationBId, RelationStatus status) {
        Nation a = nationManager.getNationById(nationAId);
        Nation b = nationManager.getNationById(nationBId);
        if (a == null || b == null) return;
        boolean changed = false;
        if (status == RelationStatus.ALLIANCE) {
            if (a.getAllies().add(b.getId())) changed = true;
            if (b.getAllies().add(a.getId())) changed = true;
            if (a.getEnemies().remove(b.getId())) changed = true;
            if (b.getEnemies().remove(a.getId())) changed = true;
        } else if (status == RelationStatus.WAR) {
            if (a.getEnemies().add(b.getId())) changed = true;
            if (b.getEnemies().add(a.getId())) changed = true;
            if (a.getAllies().remove(b.getId())) changed = true;
            if (b.getAllies().remove(a.getId())) changed = true;
        } else {
            if (a.getAllies().remove(b.getId())) changed = true;
            if (b.getAllies().remove(a.getId())) changed = true;
            if (a.getEnemies().remove(b.getId())) changed = true;
            if (b.getEnemies().remove(a.getId())) changed = true;
        }
        if (changed) {
            try {
                nationManager.save(a);
                nationManager.save(b);
            } catch (Exception ignored) {}
        }
    }

    private void reconcileNationLinks() {
        Map<String, Set<String>> expectedAllies = new HashMap<>();
        Map<String, Set<String>> expectedEnemies = new HashMap<>();

        for (Nation n : nationManager.getAll()) {
            expectedAllies.put(n.getId(), new HashSet<>());
            expectedEnemies.put(n.getId(), new HashSet<>());
        }

        long now = System.currentTimeMillis();
        for (Relation rel : relations.values()) {
            if (rel == null || rel.expiresAt > 0 && rel.expiresAt <= now) continue;
            if (rel.status == RelationStatus.ALLIANCE) {
                expectedAllies.computeIfAbsent(rel.nationA, k -> new HashSet<>()).add(rel.nationB);
                expectedAllies.computeIfAbsent(rel.nationB, k -> new HashSet<>()).add(rel.nationA);
            } else if (rel.status == RelationStatus.WAR) {
                expectedEnemies.computeIfAbsent(rel.nationA, k -> new HashSet<>()).add(rel.nationB);
                expectedEnemies.computeIfAbsent(rel.nationB, k -> new HashSet<>()).add(rel.nationA);
            }
        }

        for (Nation n : nationManager.getAll()) {
            Set<String> allies = expectedAllies.getOrDefault(n.getId(), Collections.emptySet());
            Set<String> enemies = expectedEnemies.getOrDefault(n.getId(), Collections.emptySet());
            if (!n.getAllies().equals(allies) || !n.getEnemies().equals(enemies)) {
                n.getAllies().clear();
                n.getAllies().addAll(allies);
                n.getEnemies().clear();
                n.getEnemies().addAll(enemies);
                try {
                    nationManager.save(n);
                } catch (Exception ignored) {}
            }
        }
    }

    private void bootstrapFromNationData() {
        boolean changed = false;
        List<Nation> nations = new ArrayList<>(nationManager.getAll());
        long now = System.currentTimeMillis();

        // Wars first to avoid conflicts with alliances
        for (Nation n : nations) {
            if (n.getEnemies() == null) continue;
            for (String enemyId : n.getEnemies()) {
                if (isBlank(enemyId)) continue;
                if (nationManager.getNationById(enemyId) == null) continue;
                Relation rel = relations.get(relationKey(n.getId(), enemyId));
                if (rel == null || rel.status != RelationStatus.WAR) {
                    Relation war = new Relation();
                    war.nationA = canonicalA(n.getId(), enemyId);
                    war.nationB = canonicalB(n.getId(), enemyId);
                    war.status = RelationStatus.WAR;
                    war.startedAt = now;
                    war.expiresAt = resolveExpiresAt(RelationStatus.WAR, now, WAR_DEFAULT_DURATION_MS);
                    war.reason = "bootstrap";
                    relations.put(relationKey(n.getId(), enemyId), war);
                    changed = true;
                }
            }
        }

        for (Nation n : nations) {
            if (n.getAllies() == null) continue;
            for (String allyId : n.getAllies()) {
                if (isBlank(allyId)) continue;
                if (nationManager.getNationById(allyId) == null) continue;
                Relation rel = relations.get(relationKey(n.getId(), allyId));
                if (rel == null) {
                    Relation alliance = new Relation();
                    alliance.nationA = canonicalA(n.getId(), allyId);
                    alliance.nationB = canonicalB(n.getId(), allyId);
                    alliance.status = RelationStatus.ALLIANCE;
                    alliance.startedAt = now;
                    alliance.expiresAt = PERMANENT_DURATION_MS;
                    alliance.reason = "bootstrap";
                    relations.put(relationKey(n.getId(), allyId), alliance);
                    changed = true;
                }
            }
        }

        if (changed) {
            saveRelations();
        }
    }

    private Relation getRelationInternal(String nationA, String nationB) {
        Relation rel = relations.get(relationKey(nationA, nationB));
        if (rel == null) return null;
        if (isExpired(rel)) {
            expireRelation(rel);
            saveRelations();
            return null;
        }
        return rel;
    }

    private void removeRelation(String nationA, String nationB) {
        relations.remove(relationKey(nationA, nationB));
    }

    private String validateParticipants(String nationA, String nationB) {
        if (isBlank(nationA) || isBlank(nationB)) return "Некорректные стороны договора.";
        if (nationA.equals(nationB)) return "Некорректные стороны договора.";
        if (nationManager.getNationById(nationA) == null || nationManager.getNationById(nationB) == null) {
            return "Нация не найдена.";
        }
        return null;
    }

    private String validateTransition(String nationA, String nationB, RelationStatus current, RelationStatus target) {
        if (target == RelationStatus.CEASEFIRE && current != RelationStatus.WAR) {
            return "Перемирие возможно только после войны.";
        }
        if (target == RelationStatus.ALLIANCE && current == RelationStatus.WAR) {
            return "Нельзя заключить альянс во время войны.";
        }
        if (target == RelationStatus.WAR && current == RelationStatus.ALLIANCE) {
            return "Нельзя объявить войну союзнику. Расторгните альянс.";
        }
        if (target == RelationStatus.ALLIANCE && (isSanctioned(nationA, nationB) || isSanctioned(nationB, nationA))) {
            return "Нельзя заключить альянс при активных санкциях.";
        }
        return null;
    }

    private long resolveExpiresAt(RelationStatus status, long now, long durationMs) {
        if (status == RelationStatus.WAR && durationMs <= 0) {
            durationMs = WAR_DEFAULT_DURATION_MS;
        }
        if (durationMs <= 0 || durationMs == PERMANENT_DURATION_MS) {
            return PERMANENT_DURATION_MS;
        }
        long expiresAt = now + durationMs;
        if (expiresAt < 0 || expiresAt < now) return PERMANENT_DURATION_MS;
        return expiresAt;
    }

    private boolean isExpired(Relation rel) {
        if (rel == null) return true;
        if (rel.expiresAt == PERMANENT_DURATION_MS) return false;
        return rel.expiresAt > 0 && rel.expiresAt <= System.currentTimeMillis();
    }

    private boolean isExpired(Sanction s) {
        if (s == null) return true;
        if (s.expiresAt == PERMANENT_DURATION_MS) return false;
        return s.expiresAt > 0 && s.expiresAt <= System.currentTimeMillis();
    }

    private void loadRelations() {
        if (!relationsFile.exists()) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(relationsFile.toPath()), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) return;
            for (JsonElement elem : root.getAsJsonArray()) {
                if (!elem.isJsonObject()) continue;
                JsonObject o = elem.getAsJsonObject();
                Relation rel = new Relation();
                rel.nationA = o.has("nationA") ? o.get("nationA").getAsString() : null;
                rel.nationB = o.has("nationB") ? o.get("nationB").getAsString() : null;
                rel.status = o.has("status") ? RelationStatus.valueOf(o.get("status").getAsString()) : RelationStatus.NEUTRAL;
                rel.startedAt = o.has("startedAt") ? o.get("startedAt").getAsLong() : System.currentTimeMillis();
                rel.expiresAt = o.has("expiresAt") ? o.get("expiresAt").getAsLong() : PERMANENT_DURATION_MS;
                rel.reason = o.has("reason") ? o.get("reason").getAsString() : "";
                if (isBlank(rel.nationA) || isBlank(rel.nationB)) continue;
                if (nationManager.getNationById(rel.nationA) == null || nationManager.getNationById(rel.nationB) == null) {
                    continue;
                }
                if (!isExpired(rel)) {
                    relations.put(relationKey(rel.nationA, rel.nationB), rel);
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadSanctions() {
        if (!sanctionsFile.exists()) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(sanctionsFile.toPath()), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) return;
            for (JsonElement elem : root.getAsJsonArray()) {
                if (!elem.isJsonObject()) continue;
                JsonObject o = elem.getAsJsonObject();
                Sanction s = new Sanction();
                s.sanctionerId = o.has("sanctionerId") ? o.get("sanctionerId").getAsString() : null;
                s.targetId = o.has("targetId") ? o.get("targetId").getAsString() : null;
                s.startedAt = o.has("startedAt") ? o.get("startedAt").getAsLong() : System.currentTimeMillis();
                s.expiresAt = o.has("expiresAt") ? o.get("expiresAt").getAsLong() : PERMANENT_DURATION_MS;
                s.reason = o.has("reason") ? o.get("reason").getAsString() : "";
                if (isBlank(s.sanctionerId) || isBlank(s.targetId)) continue;
                if (nationManager.getNationById(s.sanctionerId) == null || nationManager.getNationById(s.targetId) == null) {
                    continue;
                }
                if (!isExpired(s)) {
                    sanctions.put(sanctionKey(s.sanctionerId, s.targetId), s);
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveRelations() {
        JsonArray arr = new JsonArray();
        for (Relation rel : relations.values()) {
            if (rel == null) continue;
            JsonObject o = new JsonObject();
            o.addProperty("nationA", rel.nationA);
            o.addProperty("nationB", rel.nationB);
            o.addProperty("status", rel.status.name());
            o.addProperty("startedAt", rel.startedAt);
            o.addProperty("expiresAt", rel.expiresAt);
            o.addProperty("reason", rel.reason != null ? rel.reason : "");
            arr.add(o);
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(relationsFile), StandardCharsets.UTF_8)) {
            writer.write(arr.toString());
        } catch (Exception ignored) {}
    }

    private void saveSanctions() {
        JsonArray arr = new JsonArray();
        for (Sanction s : sanctions.values()) {
            if (s == null) continue;
            JsonObject o = new JsonObject();
            o.addProperty("sanctionerId", s.sanctionerId);
            o.addProperty("targetId", s.targetId);
            o.addProperty("startedAt", s.startedAt);
            o.addProperty("expiresAt", s.expiresAt);
            o.addProperty("reason", s.reason != null ? s.reason : "");
            arr.add(o);
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(sanctionsFile), StandardCharsets.UTF_8)) {
            writer.write(arr.toString());
        } catch (Exception ignored) {}
    }

    private void importLegacySanctions() {
        boolean changed = false;
        File legacyDir = new File(plugin.getDataFolder(), "sanctions");
        File[] files = legacyDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    String nationId = f.getName().replace(".json", "");
                    if (!o.has("sanctioned")) continue;
                    for (JsonElement elem : o.getAsJsonArray("sanctioned")) {
                        String targetId = elem.getAsString();
                        if (isBlank(nationId) || isBlank(targetId)) continue;
                        if (nationManager.getNationById(nationId) == null || nationManager.getNationById(targetId) == null) continue;
                        String key = sanctionKey(nationId, targetId);
                        if (!sanctions.containsKey(key)) {
                            Sanction s = new Sanction();
                            s.sanctionerId = nationId;
                            s.targetId = targetId;
                            s.startedAt = System.currentTimeMillis();
                            s.expiresAt = PERMANENT_DURATION_MS;
                            s.reason = "legacy";
                            sanctions.put(key, s);
                            changed = true;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        File tradeSanctions = new File(new File(plugin.getDataFolder(), "treaties"), "sanctions.json");
        if (tradeSanctions.exists()) {
            try (Reader r = new InputStreamReader(Files.newInputStream(tradeSanctions.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : o.entrySet()) {
                    String nationId = entry.getKey();
                    JsonArray arr = entry.getValue().getAsJsonArray();
                    for (JsonElement elem : arr) {
                        String targetId = elem.getAsString();
                        if (isBlank(nationId) || isBlank(targetId)) continue;
                        if (nationManager.getNationById(nationId) == null || nationManager.getNationById(targetId) == null) continue;
                        String key = sanctionKey(nationId, targetId);
                        if (!sanctions.containsKey(key)) {
                            Sanction s = new Sanction();
                            s.sanctionerId = nationId;
                            s.targetId = targetId;
                            s.startedAt = System.currentTimeMillis();
                            s.expiresAt = PERMANENT_DURATION_MS;
                            s.reason = "legacy";
                            sanctions.put(key, s);
                            changed = true;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (changed) {
            saveSanctions();
        }
    }

    private void importLegacyCeasefires() {
        File ceasefiresDir = new File(plugin.getDataFolder(), "ceasefires");
        File[] files = ceasefiresDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        boolean changed = false;
        long now = System.currentTimeMillis();
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationA = o.has("nationA") ? o.get("nationA").getAsString() : null;
                String nationB = o.has("nationB") ? o.get("nationB").getAsString() : null;
                long expiresAt = o.has("expiresAt") ? o.get("expiresAt").getAsLong() : PERMANENT_DURATION_MS;
                boolean permanent = o.has("permanent") && o.get("permanent").getAsBoolean();
                if (isBlank(nationA) || isBlank(nationB)) continue;
                if (nationManager.getNationById(nationA) == null || nationManager.getNationById(nationB) == null) continue;
                if (!permanent && expiresAt <= now) continue;
                Relation rel = relations.get(relationKey(nationA, nationB));
                if (rel == null || rel.status != RelationStatus.CEASEFIRE) {
                    Relation ceasefire = new Relation();
                    ceasefire.nationA = canonicalA(nationA, nationB);
                    ceasefire.nationB = canonicalB(nationA, nationB);
                    ceasefire.status = RelationStatus.CEASEFIRE;
                    ceasefire.startedAt = now;
                    ceasefire.expiresAt = permanent ? PERMANENT_DURATION_MS : expiresAt;
                    ceasefire.reason = "legacy";
                    relations.put(relationKey(nationA, nationB), ceasefire);
                    changed = true;
                }
            } catch (Exception ignored) {}
        }
        if (changed) {
            saveRelations();
        }
    }

    private Relation copyRelation(Relation rel) {
        Relation copy = new Relation();
        copy.nationA = rel.nationA;
        copy.nationB = rel.nationB;
        copy.status = rel.status;
        copy.startedAt = rel.startedAt;
        copy.expiresAt = rel.expiresAt;
        copy.reason = rel.reason;
        return copy;
    }

    private Sanction copySanction(Sanction s) {
        Sanction copy = new Sanction();
        copy.sanctionerId = s.sanctionerId;
        copy.targetId = s.targetId;
        copy.startedAt = s.startedAt;
        copy.expiresAt = s.expiresAt;
        copy.reason = s.reason;
        return copy;
    }

    private String relationKey(String nationA, String nationB) {
        String a = canonicalA(nationA, nationB);
        String b = canonicalB(nationA, nationB);
        return a + "|" + b;
    }

    private String canonicalA(String nationA, String nationB) {
        return nationA.compareTo(nationB) <= 0 ? nationA : nationB;
    }

    private String canonicalB(String nationA, String nationB) {
        return nationA.compareTo(nationB) <= 0 ? nationB : nationA;
    }

    private String sanctionKey(String sanctionerId, String targetId) {
        return sanctionerId + "|" + targetId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
