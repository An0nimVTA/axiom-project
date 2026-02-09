package com.axiom.domain.service.military;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import com.axiom.domain.service.state.NationManager;

/** Manages nuclear weapons programs and deterrence. */
public class NuclearWeaponsService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File nuclearDir;
    private final Map<String, NuclearProgram> programs = new HashMap<>(); // nationId -> program

    public static class NuclearProgram {
        String nationId;
        int warheads;
        boolean hasProgram;
        double researchProgress; // 0-100%
        long startedAt;
        double deterrenceLevel; // 0-100%
    }

    public NuclearWeaponsService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.nuclearDir = new File(plugin.getDataFolder(), "nuclear");
        this.nuclearDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processPrograms, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String startProgram(String nationId, double cost) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (nationId == null || nationId.trim().isEmpty() || !Double.isFinite(cost) || cost < 0) {
            return "Некорректные данные.";
        }
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        NuclearProgram program = programs.computeIfAbsent(nationId, k -> new NuclearProgram());
        if (program.hasProgram) return "Программа уже существует.";
        program.nationId = nationId;
        program.hasProgram = true;
        program.researchProgress = 0.0;
        program.startedAt = System.currentTimeMillis();
        program.deterrenceLevel = 0.0;
        n.setTreasury(n.getTreasury() - cost);
        n.getHistory().add("Ядерная программа начата");
        try {
            nationManager.save(n);
            saveProgram(nationId, program);
        } catch (Exception ignored) {}
        return "Ядерная программа начата.";
    }

    private synchronized void processPrograms() {
        if (nationManager == null) return;
        for (Map.Entry<String, NuclearProgram> e : programs.entrySet()) {
            NuclearProgram program = e.getValue();
            if (!program.hasProgram) continue;
            // Research progresses
            if (program.researchProgress < 100) {
                double education = plugin.getEducationService() != null
                    ? plugin.getEducationService().getEducationLevel(e.getKey())
                    : 0.0;
                program.researchProgress = Math.min(100, program.researchProgress + (education * 0.01));
            }
            // Build warheads if research complete
            if (program.researchProgress >= 100 && program.warheads < 100) {
                program.warheads = Math.min(100, program.warheads + 1);
                program.deterrenceLevel = Math.min(100, program.deterrenceLevel + 1.0);
            }
            // Deterrence effect - reduces chance of war declaration
            if (program.deterrenceLevel > 50) {
                // High deterrence makes other nations less likely to declare war
                if (plugin.getDiplomacySystem() != null) {
                    for (Nation other : nationManager.getAll()) {
                        if (!other.getId().equals(e.getKey())) {
                            try {
                                Nation source = nationManager.getNationById(e.getKey());
                                if (source != null) {
                                    plugin.getDiplomacySystem().setReputation(other, source, 5);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            saveProgram(e.getKey(), program);
        }
    }

    public synchronized int getWarheads(String nationId) {
        NuclearProgram program = programs.get(nationId);
        return program != null ? program.warheads : 0;
    }

    private void loadAll() {
        File[] files = nuclearDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                NuclearProgram program = new NuclearProgram();
                program.nationId = nationId;
                program.hasProgram = o.get("hasProgram").getAsBoolean();
                program.warheads = o.get("warheads").getAsInt();
                program.researchProgress = o.get("researchProgress").getAsDouble();
                program.startedAt = o.get("startedAt").getAsLong();
                program.deterrenceLevel = o.get("deterrenceLevel").getAsDouble();
                programs.put(nationId, program);
            } catch (Exception ignored) {}
        }
    }

    private void saveProgram(String nationId, NuclearProgram program) {
        File f = new File(nuclearDir, nationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("nationId", program.nationId);
        o.addProperty("hasProgram", program.hasProgram);
        o.addProperty("warheads", program.warheads);
        o.addProperty("researchProgress", program.researchProgress);
        o.addProperty("startedAt", program.startedAt);
        o.addProperty("deterrenceLevel", program.deterrenceLevel);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive nuclear weapons statistics for a nation.
     */
    public synchronized Map<String, Object> getNuclearWeaponsStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        NuclearProgram program = programs.get(nationId);
        if (program == null || !program.hasProgram) {
            stats.put("hasProgram", false);
            return stats;
        }
        
        stats.put("hasProgram", true);
        stats.put("warheads", program.warheads);
        stats.put("researchProgress", program.researchProgress);
        stats.put("deterrenceLevel", program.deterrenceLevel);
        stats.put("startedAt", program.startedAt);
        
        long ageHours = (System.currentTimeMillis() - program.startedAt) / 1000 / 60 / 60;
        stats.put("programAgeHours", ageHours);
        stats.put("researchComplete", program.researchProgress >= 100);
        stats.put("hasWarheads", program.warheads > 0);
        
        // Nuclear status rating
        String rating = "НЕТ ПРОГРАММЫ";
        if (program.deterrenceLevel >= 80 && program.warheads >= 50) rating = "СУПЕРДЕРЖАВА";
        else if (program.deterrenceLevel >= 60 && program.warheads >= 20) rating = "ЯДЕРНАЯ ДЕРЖАВА";
        else if (program.deterrenceLevel >= 40 && program.warheads >= 10) rating = "РАЗВИВАЮЩАЯСЯ";
        else if (program.deterrenceLevel >= 20 || program.warheads >= 1) rating = "НАЧАЛЬНАЯ";
        else if (program.researchProgress >= 50) rating = "ИССЛЕДОВАНИЕ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global nuclear weapons statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalNuclearWeaponsStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPrograms = 0;
        int programsWithResearch = 0;
        int programsWithWarheads = 0;
        int totalWarheads = 0;
        double totalDeterrence = 0.0;
        double totalResearchProgress = 0.0;
        Map<String, Integer> warheadsByNation = new HashMap<>();
        Map<String, Double> deterrenceByNation = new HashMap<>();
        Map<String, Double> researchByNation = new HashMap<>();
        
        for (Map.Entry<String, NuclearProgram> entry : programs.entrySet()) {
            NuclearProgram program = entry.getValue();
            if (program.hasProgram) {
                totalPrograms++;
                totalWarheads += program.warheads;
                totalDeterrence += program.deterrenceLevel;
                totalResearchProgress += program.researchProgress;
                
                if (program.researchProgress > 0) programsWithResearch++;
                if (program.warheads > 0) programsWithWarheads++;
                
                warheadsByNation.put(entry.getKey(), program.warheads);
                deterrenceByNation.put(entry.getKey(), program.deterrenceLevel);
                researchByNation.put(entry.getKey(), program.researchProgress);
            }
        }
        
        stats.put("totalPrograms", totalPrograms);
        stats.put("programsWithResearch", programsWithResearch);
        stats.put("programsWithWarheads", programsWithWarheads);
        stats.put("totalWarheads", totalWarheads);
        stats.put("averageDeterrence", totalPrograms > 0 ? totalDeterrence / totalPrograms : 0);
        stats.put("averageResearchProgress", totalPrograms > 0 ? totalResearchProgress / totalPrograms : 0);
        stats.put("averageWarheads", totalPrograms > 0 ? (double) totalWarheads / totalPrograms : 0);
        stats.put("warheadsByNation", warheadsByNation);
        stats.put("deterrenceByNation", deterrenceByNation);
        stats.put("researchByNation", researchByNation);
        
        // Top nations by warheads
        List<Map.Entry<String, Integer>> topByWarheads = warheadsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByWarheads", topByWarheads);
        
        // Top nations by deterrence
        List<Map.Entry<String, Double>> topByDeterrence = deterrenceByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByDeterrence", topByDeterrence);
        
        // Top nations by research progress
        List<Map.Entry<String, Double>> topByResearch = researchByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByResearch", topByResearch);
        
        // Nuclear status distribution
        int superpower = 0, power = 0, developing = 0, initial = 0, research = 0;
        for (NuclearProgram program : programs.values()) {
            if (!program.hasProgram) continue;
            if (program.deterrenceLevel >= 80 && program.warheads >= 50) superpower++;
            else if (program.deterrenceLevel >= 60 && program.warheads >= 20) power++;
            else if (program.deterrenceLevel >= 40 && program.warheads >= 10) developing++;
            else if (program.deterrenceLevel >= 20 || program.warheads >= 1) initial++;
            else if (program.researchProgress >= 50) research++;
        }
        
        Map<String, Integer> statusDistribution = new HashMap<>();
        statusDistribution.put("superpower", superpower);
        statusDistribution.put("power", power);
        statusDistribution.put("developing", developing);
        statusDistribution.put("initial", initial);
        statusDistribution.put("research", research);
        stats.put("statusDistribution", statusDistribution);
        
        return stats;
    }
}

