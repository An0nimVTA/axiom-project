package com.axiom.domain.service.military;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/**
 * Unified Espionage System.
 * Combines Spy Networks, Active Missions, and Intelligence Gathering.
 * Replaces EspionageService, SpyService, and IntelligenceService.
 */
public class UnifiedEspionageService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File dataDir;
    
    // Data stores
    private final Map<String, List<SpyMission>> activeMissions = new HashMap<>();
    private final Map<String, SpyNetwork> networks = new HashMap<>(); // key: ownerId_targetId
    private final Map<String, Map<String, IntelligenceReport>> reports = new HashMap<>(); // key: collectorId -> targetId -> report

    // --- Data Classes ---

    public static class SpyMission {
        public String spyNationId;
        public String targetNationId;
        public String type; // "theft", "sabotage", "intel"
        public long startTime;
        public long durationMs;
    }

    public static class SpyNetwork {
        public String ownerNationId;
        public String targetNationId;
        public int level; // 1-5
        public double successChance;
        public long lastMission;
    }

    public static class IntelligenceReport {
        public String targetNationId;
        public double treasuryEstimate;
        public int militaryStrength;
        public int population;
        public Set<String> knownAllies = new HashSet<>();
        public long lastUpdated;
    }

    public UnifiedEspionageService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.dataDir = new File(plugin.getDataFolder(), "espionage_system");
        this.dataDir.mkdirs();
        
        loadAll();
        
        // Auto-save task (every 5 minutes)
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, 6000L, 6000L);
    }

    // --- Spy Network Methods (ex-SpyService) ---

    public synchronized String createSpyNetwork(String ownerId, String targetId, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (ownerId.equals(targetId)) return "Нельзя создать сеть в своей нации.";
        
        String key = networkKey(ownerId, targetId);
        if (networks.containsKey(key)) return "Шпионская сеть уже существует.";
        
        Nation owner = nationManager.getNationById(ownerId);
        if (owner == null) return "Нация не найдена.";
        if (owner.getTreasury() < cost) return "Недостаточно средств.";
        
        owner.setTreasury(owner.getTreasury() - cost);
        
        SpyNetwork sn = new SpyNetwork();
        sn.ownerNationId = ownerId;
        sn.targetNationId = targetId;
        sn.level = 1;
        sn.successChance = 0.2;
        sn.lastMission = 0;
        
        networks.put(key, sn);
        saveNetwork(sn);
        
        return "Шпионская сеть создана (уровень 1).";
    }
    
    public synchronized String upgradeNetwork(String ownerId, String targetId, double cost) {
        String key = networkKey(ownerId, targetId);
        SpyNetwork sn = networks.get(key);
        if (sn == null) return "Сеть не найдена.";
        if (sn.level >= 5) return "Максимальный уровень.";
        
        Nation owner = nationManager.getNationById(ownerId);
        if (owner.getTreasury() < cost) return "Недостаточно средств.";
        
        owner.setTreasury(owner.getTreasury() - cost);
        sn.level++;
        sn.successChance += 0.15;
        saveNetwork(sn);
        
        return "Сеть улучшена до уровня " + sn.level;
    }

    // --- Mission Methods (ex-EspionageService) ---

    public synchronized String launchMission(String spyId, String targetId, String type) {
        String netKey = networkKey(spyId, targetId);
        SpyNetwork sn = networks.get(netKey);
        
        // Require network for missions (new logic!)
        if (sn == null) return "Для миссии требуется шпионская сеть.";
        
        // Check cooldown
        if (System.currentTimeMillis() - sn.lastMission < 30 * 60_000L) {
            return "Сеть восстанавливается после прошлой операции.";
        }
        
        SpyMission m = new SpyMission();
        m.spyNationId = spyId;
        m.targetNationId = targetId;
        m.type = type;
        m.startTime = System.currentTimeMillis();
        m.durationMs = 5 * 60_000L; // 5 min
        
        activeMissions.computeIfAbsent(spyId, k -> new ArrayList<>()).add(m);
        sn.lastMission = System.currentTimeMillis();
        saveNetwork(sn);
        
        return "Миссия '" + type + "' началась. Ждите отчета.";
    }
    
    public synchronized void processCompletedMissions() {
        long now = System.currentTimeMillis();
        for (List<SpyMission> list : activeMissions.values()) {
            Iterator<SpyMission> it = list.iterator();
            while (it.hasNext()) {
                SpyMission m = it.next();
                if (now >= m.startTime + m.durationMs) {
                    completeMission(m);
                    it.remove();
                }
            }
        }
    }
    
    private void completeMission(SpyMission m) {
        String netKey = networkKey(m.spyNationId, m.targetNationId);
        SpyNetwork sn = networks.get(netKey);
        double chance = (sn != null) ? sn.successChance : 0.1;
        
        Nation spyNation = nationManager.getNationById(m.spyNationId);
        if (spyNation == null) return;

        if (Math.random() < chance) {
            // Success!
            spyNation.getHistory().add("УСПЕХ: Шпионская миссия " + m.type + " против " + m.targetNationId);
            if (m.type.equals("intel")) {
                updateIntelligence(m.spyNationId, m.targetNationId);
            }
        } else {
            // Failure
            spyNation.getHistory().add("ПРОВАЛ: Шпионская миссия " + m.type + " против " + m.targetNationId);
            // Risk of detection
            if (Math.random() < 0.5) {
                 Nation target = nationManager.getNationById(m.targetNationId);
                 if (target != null) {
                     target.getHistory().add("ОБНАРУЖЕНО: Попытка шпионажа от " + spyNation.getName());
                 }
            }
        }
    }

    // --- Intelligence Methods (ex-IntelligenceService) ---

    public synchronized void updateIntelligence(String collectorId, String targetId) {
        Nation target = nationManager.getNationById(targetId);
        if (target == null) return;
        
        IntelligenceReport r = new IntelligenceReport();
        r.targetNationId = targetId;
        r.treasuryEstimate = target.getTreasury() * (0.8 + Math.random() * 0.4); // +/- 20% error
        r.population = target.getCitizens().size();
        r.militaryStrength = 0; // Placeholder
        r.knownAllies.addAll(target.getAllies());
        r.lastUpdated = System.currentTimeMillis();
        
        reports.computeIfAbsent(collectorId, k -> new HashMap<>()).put(targetId, r);
        saveReport(collectorId, r);
    }
    
    public synchronized IntelligenceReport getReport(String collectorId, String targetId) {
        return reports.getOrDefault(collectorId, Collections.emptyMap()).get(targetId);
    }

    // --- Persistence ---
    
    private String networkKey(String o, String t) { return o + "_" + t; }

    private void saveAll() {
        try {
            for (SpyNetwork sn : networks.values()) saveNetwork(sn);
            for (String col : reports.keySet()) {
                for (IntelligenceReport r : reports.get(col).values()) saveReport(col, r);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error saving espionage data: " + e.getMessage());
        }
    }
    
    private void saveNetwork(SpyNetwork sn) {
        File f = new File(dataDir, "net_" + sn.ownerNationId + "_" + sn.targetNationId + ".json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            JsonObject o = new JsonObject();
            o.addProperty("owner", sn.ownerNationId);
            o.addProperty("target", sn.targetNationId);
            o.addProperty("level", sn.level);
            o.addProperty("chance", sn.successChance);
            o.addProperty("lastMission", sn.lastMission);
            w.write(o.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveReport(String collectorId, IntelligenceReport r) {
        File f = new File(dataDir, "intel_" + collectorId + "_" + r.targetNationId + ".json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            JsonObject o = new JsonObject();
            o.addProperty("target", r.targetNationId);
            o.addProperty("treasury", r.treasuryEstimate);
            o.addProperty("pop", r.population);
            o.addProperty("mil", r.militaryStrength);
            o.addProperty("updated", r.lastUpdated);
            JsonArray arr = new JsonArray();
            r.knownAllies.forEach(arr::add);
            o.add("allies", arr);
            w.write(o.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadAll() {
        if (!dataDir.exists()) return;
        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                if (f.getName().startsWith("net_")) {
                    SpyNetwork sn = new SpyNetwork();
                    sn.ownerNationId = o.get("owner").getAsString();
                    sn.targetNationId = o.get("target").getAsString();
                    sn.level = o.get("level").getAsInt();
                    sn.successChance = o.get("chance").getAsDouble();
                    sn.lastMission = o.has("lastMission") ? o.get("lastMission").getAsLong() : 0;
                    networks.put(networkKey(sn.ownerNationId, sn.targetNationId), sn);
                } else if (f.getName().startsWith("intel_")) {
                    String[] parts = f.getName().split("_");
                    if (parts.length >= 2) {
                        String collectorId = parts[1];
                        IntelligenceReport ir = new IntelligenceReport();
                        ir.targetNationId = o.get("target").getAsString();
                        ir.treasuryEstimate = o.get("treasury").getAsDouble();
                        ir.population = o.get("pop").getAsInt();
                        ir.militaryStrength = o.get("mil").getAsInt();
                        ir.lastUpdated = o.get("updated").getAsLong();
                        if (o.has("allies")) {
                            o.getAsJsonArray("allies").forEach(e -> ir.knownAllies.add(e.getAsString()));
                        }
                        reports.computeIfAbsent(collectorId, k -> new HashMap<>()).put(ir.targetNationId, ir);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load espionage file " + f.getName());
            }
        }
    }
}
