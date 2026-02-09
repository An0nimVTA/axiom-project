package com.axiom.domain.service.politics;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages diplomatic summits and conferences. */
public class DiplomaticSummitService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File summitsDir;
    private final Map<String, DiplomaticSummit> activeSummits = new HashMap<>(); // summitId -> summit

    public static class DiplomaticSummit {
        String id;
        String name;
        String hostNationId;
        Set<String> participantNations = new HashSet<>();
        String agenda; // discussion topics
        long scheduledFor;
        long durationMinutes;
        boolean active;
        Map<String, String> outcomes = new HashMap<>(); // nationId -> outcome
    }

    public DiplomaticSummitService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.summitsDir = new File(plugin.getDataFolder(), "summits");
        this.summitsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processSummits, 0, 20 * 60 * 5); // every 5 minutes
    }

    public synchronized String scheduleSummit(String name, String hostId, String agenda, int durationHours) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (name == null || name.trim().isEmpty()) return "Название саммита не указано.";
        if (hostId == null) return "Неверные параметры.";
        if (agenda == null || agenda.trim().isEmpty()) return "Повестка не указана.";
        if (durationHours <= 0) return "Длительность должна быть больше 0.";
        Nation host = nationManager.getNationById(hostId);
        if (host == null) return "Нация не найдена.";
        String summitId = UUID.randomUUID().toString().substring(0, 8);
        DiplomaticSummit summit = new DiplomaticSummit();
        summit.id = summitId;
        summit.name = name;
        summit.hostNationId = hostId;
        summit.agenda = agenda;
        summit.scheduledFor = System.currentTimeMillis();
        summit.durationMinutes = durationHours * 60;
        summit.active = true;
        summit.participantNations.add(hostId);
        activeSummits.put(summitId, summit);
        host.getHistory().add("Запланирован саммит: " + name);
        try {
            nationManager.save(host);
            saveSummit(summit);
        } catch (Exception ignored) {}
        return "Саммит запланирован (ID: " + summitId + ")";
    }

    public synchronized String joinSummit(String summitId, String nationId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        DiplomaticSummit summit = activeSummits.get(summitId);
        if (summit == null) return "Саммит не найден.";
        if (!summit.active) return "Саммит завершён.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        summit.participantNations.add(nationId);
        n.getHistory().add("Присоединилась к саммиту: " + summit.name);
        try {
            nationManager.save(n);
            saveSummit(summit);
        } catch (Exception ignored) {}
        return "Участие в саммите подтверждено.";
    }

    private synchronized void processSummits() {
        List<String> completed = new ArrayList<>();
        DiplomacySystem diplomacySystem = plugin.getDiplomacySystem();
        for (Map.Entry<String, DiplomaticSummit> e : activeSummits.entrySet()) {
            DiplomaticSummit summit = e.getValue();
            if (!summit.active) {
                completed.add(e.getKey());
                continue;
            }
            long elapsed = System.currentTimeMillis() - summit.scheduledFor;
            if (elapsed >= summit.durationMinutes * 60_000L) {
                summit.active = false;
                saveSummit(summit);
                // All participants get reputation boost
                Set<String> allParticipants = new HashSet<>(summit.participantNations);
                allParticipants.add(summit.hostNationId);
                for (String participantId : allParticipants) {
                    for (String otherId : allParticipants) {
                        if (!participantId.equals(otherId)) {
                            try {
                                if (diplomacySystem != null) {
                                    Nation p = nationManager.getNationById(participantId);
                                    Nation o = nationManager.getNationById(otherId);
                                    if (p != null && o != null) {
                                        diplomacySystem.setReputation(p, o, 5);
                                        diplomacySystem.setReputation(o, p, 5);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
                completed.add(e.getKey());
            }
        }
        for (String summitId : completed) {
            activeSummits.remove(summitId);
        }
    }

    private void loadAll() {
        File[] files = summitsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                DiplomaticSummit summit = new DiplomaticSummit();
                summit.id = o.get("id").getAsString();
                summit.name = o.get("name").getAsString();
                summit.hostNationId = o.get("hostNationId").getAsString();
                summit.agenda = o.get("agenda").getAsString();
                summit.scheduledFor = o.get("scheduledFor").getAsLong();
                summit.durationMinutes = o.get("durationMinutes").getAsLong();
                summit.active = o.has("active") && o.get("active").getAsBoolean();
                if (o.has("participantNations")) {
                    for (var elem : o.getAsJsonArray("participantNations")) {
                        summit.participantNations.add(elem.getAsString());
                    }
                }
                if (summit.active) activeSummits.put(summit.id, summit);
            } catch (Exception ignored) {}
        }
    }

    private void saveSummit(DiplomaticSummit summit) {
        File f = new File(summitsDir, summit.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", summit.id);
        o.addProperty("name", summit.name);
        o.addProperty("hostNationId", summit.hostNationId);
        o.addProperty("agenda", summit.agenda);
        o.addProperty("scheduledFor", summit.scheduledFor);
        o.addProperty("durationMinutes", summit.durationMinutes);
        o.addProperty("active", summit.active);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String nationId : summit.participantNations) {
            arr.add(nationId);
        }
        o.add("participantNations", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive diplomatic summit statistics.
     */
    public synchronized Map<String, Object> getSummitStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Summits this nation is involved in
        List<DiplomaticSummit> hostedSummits = new ArrayList<>();
        List<DiplomaticSummit> participatedSummits = new ArrayList<>();
        
        for (DiplomaticSummit summit : activeSummits.values()) {
            if (summit.hostNationId.equals(nationId)) {
                hostedSummits.add(summit);
            } else if (summit.participantNations.contains(nationId)) {
                participatedSummits.add(summit);
            }
        }
        
        stats.put("hostedSummits", hostedSummits.size());
        stats.put("participatedSummits", participatedSummits.size());
        stats.put("totalInvolvement", hostedSummits.size() + participatedSummits.size());
        
        // Summit details
        List<Map<String, Object>> summitsList = new ArrayList<>();
        for (DiplomaticSummit summit : hostedSummits) {
            Map<String, Object> summitData = new HashMap<>();
            summitData.put("id", summit.id);
            summitData.put("name", summit.name);
            summitData.put("role", "HOST");
            summitData.put("agenda", summit.agenda);
            summitData.put("participants", summit.participantNations.size());
            summitData.put("durationMinutes", summit.durationMinutes);
            summitData.put("timeRemaining", summit.active ? (summit.scheduledFor + summit.durationMinutes * 60_000L - System.currentTimeMillis()) / 1000 / 60 : -1);
            summitData.put("isActive", summit.active);
            summitsList.add(summitData);
        }
        for (DiplomaticSummit summit : participatedSummits) {
            Map<String, Object> summitData = new HashMap<>();
            summitData.put("id", summit.id);
            summitData.put("name", summit.name);
            summitData.put("role", "PARTICIPANT");
            summitData.put("agenda", summit.agenda);
            summitData.put("participants", summit.participantNations.size());
            summitData.put("durationMinutes", summit.durationMinutes);
            summitData.put("timeRemaining", summit.active ? (summit.scheduledFor + summit.durationMinutes * 60_000L - System.currentTimeMillis()) / 1000 / 60 : -1);
            summitData.put("isActive", summit.active);
            summitsList.add(summitData);
        }
        stats.put("summitsList", summitsList);
        
        // Summit rating
        String rating = "НЕТ УЧАСТИЯ";
        if (hostedSummits.size() >= 5) rating = "ВЕДУЩИЙ ОРГАНИЗАТОР";
        else if (hostedSummits.size() >= 3) rating = "АКТИВНЫЙ ОРГАНИЗАТОР";
        else if (hostedSummits.size() >= 1) rating = "ОРГАНИЗАТОР";
        else if (participatedSummits.size() >= 5) rating = "АКТИВНЫЙ УЧАСТНИК";
        else if (participatedSummits.size() >= 1) rating = "УЧАСТНИК";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Cancel summit.
     */
    public synchronized String cancelSummit(String summitId, String hostId) throws IOException {
        DiplomaticSummit summit = activeSummits.get(summitId);
        if (summit == null) return "Саммит не найден.";
        if (!summit.hostNationId.equals(hostId)) return "Вы не являетесь организатором.";
        
        summit.active = false;
        
        Nation host = nationManager.getNationById(hostId);
        if (host != null) {
            host.getHistory().add("Саммит отменён: " + summit.name);
            nationManager.save(host);
        }
        
        saveSummit(summit);
        activeSummits.remove(summitId);
        
        return "Саммит отменён.";
    }
    
    /**
     * Leave summit.
     */
    public synchronized String leaveSummit(String summitId, String nationId) throws IOException {
        DiplomaticSummit summit = activeSummits.get(summitId);
        if (summit == null) return "Саммит не найден.";
        if (summit.hostNationId.equals(nationId)) return "Организатор не может покинуть саммит.";
        if (!summit.participantNations.contains(nationId)) return "Вы не участвуете в этом саммите.";
        
        summit.participantNations.remove(nationId);
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.getHistory().add("Покинула саммит: " + summit.name);
            nationManager.save(n);
        }
        
        saveSummit(summit);
        
        return "Вы покинули саммит.";
    }
    
    /**
     * Get active summits for a nation.
     */
    public synchronized List<DiplomaticSummit> getActiveSummits(String nationId) {
        return activeSummits.values().stream()
            .filter(s -> s.active && (s.hostNationId.equals(nationId) || s.participantNations.contains(nationId)))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate diplomatic bonus from summits.
     */
    public synchronized double getSummitDiplomaticBonus(String nationId) {
        int activeCount = getActiveSummits(nationId).size();
        // +2% diplomatic reputation per active summit (capped)
        return 1.0 + Math.min(0.20, activeCount * 0.02); // Max +20%
    }
    
    /**
     * Get global diplomatic summit statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalDiplomaticSummitStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveSummits", activeSummits.size());
        
        Map<String, Integer> summitsByHost = new HashMap<>();
        Map<String, Integer> participationByNation = new HashMap<>();
        int totalParticipants = 0;
        Map<String, Integer> summitsByAgenda = new HashMap<>();
        
        for (DiplomaticSummit summit : activeSummits.values()) {
            if (!summit.active) continue;
            
            summitsByHost.put(summit.hostNationId, summitsByHost.getOrDefault(summit.hostNationId, 0) + 1);
            summitsByAgenda.put(summit.agenda, summitsByAgenda.getOrDefault(summit.agenda, 0) + 1);
            
            for (String nationId : summit.participantNations) {
                participationByNation.put(nationId, participationByNation.getOrDefault(nationId, 0) + 1);
            }
            participationByNation.put(summit.hostNationId, participationByNation.getOrDefault(summit.hostNationId, 0) + 1);
            totalParticipants += summit.participantNations.size() + 1; // +1 for host
        }
        
        stats.put("summitsByHost", summitsByHost);
        stats.put("participationByNation", participationByNation);
        stats.put("totalParticipants", totalParticipants);
        stats.put("summitsByAgenda", summitsByAgenda);
        stats.put("uniqueHosts", summitsByHost.size());
        stats.put("nationsParticipating", participationByNation.size());
        
        // Average participants per summit
        stats.put("averageParticipantsPerSummit", activeSummits.size() > 0 ? 
            (double) totalParticipants / activeSummits.size() : 0);
        
        // Top hosts
        List<Map.Entry<String, Integer>> topHosts = summitsByHost.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topHosts", topHosts);
        
        // Top participants
        List<Map.Entry<String, Integer>> topParticipants = participationByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topParticipants", topParticipants);
        
        // Most common agendas
        List<Map.Entry<String, Integer>> mostCommonAgendas = summitsByAgenda.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonAgendas", mostCommonAgendas);
        
        // Average summit duration
        long totalDuration = activeSummits.values().stream()
            .filter(s -> s.active)
            .mapToLong(s -> s.durationMinutes)
            .sum();
        stats.put("averageDurationMinutes", activeSummits.size() > 0 ? 
            (double) totalDuration / activeSummits.size() : 0);
        
        return stats;
    }
}

