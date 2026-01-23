package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Tracks treaty violations and penalties. */
public class TreatyViolationService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File violationsDir;
    private final Map<String, List<Violation>> nationViolations = new HashMap<>(); // nationId -> violations

    public static class Violation {
        String treatyId;
        String type;
        String description;
        long timestamp;
        double penalty;
        boolean resolved;
    }

    public TreatyViolationService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.violationsDir = new File(plugin.getDataFolder(), "violations");
        this.violationsDir.mkdirs();
        loadAll();
    }

    public synchronized String recordViolation(String nationId, String treatyId, String type, String description, double penalty) {
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        Violation v = new Violation();
        v.treatyId = treatyId;
        v.type = type;
        v.description = description;
        v.timestamp = System.currentTimeMillis();
        v.penalty = penalty;
        v.resolved = false;
        nationViolations.computeIfAbsent(nationId, k -> new ArrayList<>()).add(v);
        // Apply penalty
        n.setTreasury(Math.max(0, n.getTreasury() - penalty));
        try {
            plugin.getDiplomacySystem().setReputation(n, n, -10); // Self-reputation hit
        } catch (Exception ignored) {}
        n.getHistory().add("Нарушение договора: " + description + " (штраф: " + penalty + ")");
        try {
            nationManager.save(n);
            saveViolations(nationId);
        } catch (Exception ignored) {}
        return "Нарушение зафиксировано.";
    }

    private void loadAll() {
        File[] files = violationsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                List<Violation> violations = new ArrayList<>();
                if (o.has("violations")) {
                    for (var elem : o.getAsJsonArray("violations")) {
                        JsonObject vObj = elem.getAsJsonObject();
                        Violation v = new Violation();
                        v.treatyId = vObj.get("treatyId").getAsString();
                        v.type = vObj.get("type").getAsString();
                        v.description = vObj.get("description").getAsString();
                        v.timestamp = vObj.get("timestamp").getAsLong();
                        v.penalty = vObj.get("penalty").getAsDouble();
                        v.resolved = vObj.has("resolved") && vObj.get("resolved").getAsBoolean();
                        violations.add(v);
                    }
                }
                nationViolations.put(nationId, violations);
            } catch (Exception ignored) {}
        }
    }

    private void saveViolations(String nationId) {
        File f = new File(violationsDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<Violation> violations = nationViolations.get(nationId);
        if (violations != null) {
            for (Violation v : violations) {
                JsonObject vObj = new JsonObject();
                vObj.addProperty("treatyId", v.treatyId);
                vObj.addProperty("type", v.type);
                vObj.addProperty("description", v.description);
                vObj.addProperty("timestamp", v.timestamp);
                vObj.addProperty("penalty", v.penalty);
                vObj.addProperty("resolved", v.resolved);
                arr.add(vObj);
            }
        }
        o.add("violations", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
}

