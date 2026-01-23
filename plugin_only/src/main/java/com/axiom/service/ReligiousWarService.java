package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages religious wars (crusades, jihads). */
public class ReligiousWarService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File religiousWarsDir;
    private final Map<String, ReligiousWar> activeWars = new HashMap<>(); // warId -> war

    public static class ReligiousWar {
        String id;
        String religionId;
        String attackerNationId;
        String targetNationId;
        String type; // "crusade", "jihad", etc.
        long expiresAt;
    }

    public ReligiousWarService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.religiousWarsDir = new File(plugin.getDataFolder(), "religiouswars");
        this.religiousWarsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkExpiry, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String declareReligiousWar(String religionId, String attackerId, String targetId, String type, int durationHours) {
        Nation attacker = nationManager.getNationById(attackerId);
        Nation target = nationManager.getNationById(targetId);
        if (attacker == null || target == null) return "Нация не найдена.";
        if (attacker.getTreasury() < 5000) return "Недостаточно средств (5000 требуется).";
        String warId = UUID.randomUUID().toString().substring(0, 8);
        ReligiousWar rw = new ReligiousWar();
        rw.id = warId;
        rw.religionId = religionId;
        rw.attackerNationId = attackerId;
        rw.targetNationId = targetId;
        rw.type = type;
        rw.expiresAt = System.currentTimeMillis() + durationHours * 60 * 60_000L;
        activeWars.put(warId, rw);
        attacker.setTreasury(attacker.getTreasury() - 5000);
        // Auto-declare regular war
        try {
            plugin.getDiplomacySystem().declareWar(attacker, target);
        } catch (Exception ignored) {}
        attacker.getHistory().add("Объявлена " + type + " против " + target.getName());
        target.getHistory().add("Объявлена " + type + " от " + attacker.getName());
        try {
            nationManager.save(attacker);
            nationManager.save(target);
            saveWar(rw);
        } catch (Exception ignored) {}
        return type + " объявлена! Война начата.";
    }

    private void checkExpiry() {
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, ReligiousWar> e : activeWars.entrySet()) {
            if (e.getValue().expiresAt <= now) {
                expired.add(e.getKey());
            }
        }
        for (String warId : expired) {
            activeWars.remove(warId);
        }
    }

    private void loadAll() {
        File[] files = religiousWarsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                ReligiousWar rw = new ReligiousWar();
                rw.id = o.get("id").getAsString();
                rw.religionId = o.get("religionId").getAsString();
                rw.attackerNationId = o.get("attackerNationId").getAsString();
                rw.targetNationId = o.get("targetNationId").getAsString();
                rw.type = o.get("type").getAsString();
                rw.expiresAt = o.get("expiresAt").getAsLong();
                if (rw.expiresAt > System.currentTimeMillis()) {
                    activeWars.put(rw.id, rw);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveWar(ReligiousWar rw) {
        File f = new File(religiousWarsDir, rw.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", rw.id);
        o.addProperty("religionId", rw.religionId);
        o.addProperty("attackerNationId", rw.attackerNationId);
        o.addProperty("targetNationId", rw.targetNationId);
        o.addProperty("type", rw.type);
        o.addProperty("expiresAt", rw.expiresAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
}

