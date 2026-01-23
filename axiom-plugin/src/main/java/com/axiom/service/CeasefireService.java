package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages ceasefire agreements and truces. */
public class CeasefireService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File ceasefiresDir;
    private final Map<String, Ceasefire> activeCeasefires = new HashMap<>(); // nationA_nationB -> ceasefire

    public static class Ceasefire {
        String nationA;
        String nationB;
        long expiresAt;
        boolean permanent;
        String terms;
    }

    public CeasefireService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.ceasefiresDir = new File(plugin.getDataFolder(), "ceasefires");
        this.ceasefiresDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkExpiry, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String createCeasefire(String nationA, String nationB, int durationHours, boolean permanent, String terms) {
        DiplomacySystem diplomacySystem = plugin.getDiplomacySystem();
        if (diplomacySystem == null) return "Сервис дипломатии недоступен.";
        if (!permanent && durationHours <= 0) return "Неверная длительность перемирия.";
        if (!diplomacySystem.isAtWar(nationA, nationB)) {
            return "Нации не в состоянии войны.";
        }
        String key = nationA.compareTo(nationB) < 0 ? nationA + "_" + nationB : nationB + "_" + nationA;
        if (activeCeasefires.containsKey(key)) return "Перемирие уже активно.";
        Ceasefire cf = new Ceasefire();
        cf.nationA = nationA;
        cf.nationB = nationB;
        cf.expiresAt = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + durationHours * 60 * 60_000L;
        cf.permanent = permanent;
        cf.terms = terms != null ? terms : "";
        activeCeasefires.put(key, cf);
        saveCeasefire(cf);
        Nation a = nationManager.getNationById(nationA);
        Nation b = nationManager.getNationById(nationB);
        if (a != null) a.getHistory().add("Перемирие с " + (b != null ? b.getName() : nationB));
        if (b != null) b.getHistory().add("Перемирие с " + (a != null ? a.getName() : nationA));
        try {
            if (a != null) nationManager.save(a);
            if (b != null) nationManager.save(b);
            diplomacySystem.declarePeace(nationA, nationB);
        } catch (Exception ignored) {}
        return "Перемирие установлено.";
    }

    private void checkExpiry() {
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, Ceasefire> e : activeCeasefires.entrySet()) {
            if (!e.getValue().permanent && e.getValue().expiresAt <= now) {
                expired.add(e.getKey());
            }
        }
        for (String key : expired) {
            activeCeasefires.remove(key);
            File f = new File(ceasefiresDir, key + ".json");
            if (f.exists()) f.delete();
        }
    }

    private void loadAll() {
        File[] files = ceasefiresDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Ceasefire cf = new Ceasefire();
                cf.nationA = o.get("nationA").getAsString();
                cf.nationB = o.get("nationB").getAsString();
                cf.expiresAt = o.get("expiresAt").getAsLong();
                cf.permanent = o.has("permanent") && o.get("permanent").getAsBoolean();
                cf.terms = o.has("terms") ? o.get("terms").getAsString() : "";
                String key = cf.nationA.compareTo(cf.nationB) < 0 ? cf.nationA + "_" + cf.nationB : cf.nationB + "_" + cf.nationA;
                if (cf.expiresAt > System.currentTimeMillis() || cf.permanent) {
                    activeCeasefires.put(key, cf);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveCeasefire(Ceasefire cf) {
        String key = cf.nationA.compareTo(cf.nationB) < 0 ? cf.nationA + "_" + cf.nationB : cf.nationB + "_" + cf.nationA;
        File f = new File(ceasefiresDir, key + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationA", cf.nationA);
        o.addProperty("nationB", cf.nationB);
        o.addProperty("expiresAt", cf.expiresAt);
        o.addProperty("permanent", cf.permanent);
        o.addProperty("terms", cf.terms);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }

    public synchronized boolean hasCeasefire(String nationA, String nationB) {
        String key = nationA.compareTo(nationB) < 0 ? nationA + "_" + nationB : nationB + "_" + nationA;
        return activeCeasefires.containsKey(key);
    }
}

