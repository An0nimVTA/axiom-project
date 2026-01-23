package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.UUID;

/** Manages fortifications and defensive structures. */
public class FortificationService {
    private final AXIOM plugin;
    private final File fortsDir;
    private final Map<String, Map<String, Fortification>> nationForts = new HashMap<>(); // nationId -> chunkKey -> fort

    public static class Fortification {
        String chunkKey;
        String nationId;
        int level; // 1-5
        double defenseBonus;
        long builtAt;
    }

    public FortificationService(AXIOM plugin) {
        this.plugin = plugin;
        this.fortsDir = new File(plugin.getDataFolder(), "fortifications");
        this.fortsDir.mkdirs();
        loadAll();
    }

    public synchronized String buildFortification(String nationId, String chunkKey, int level, double cost) {
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (nationId == null || chunkKey == null || chunkKey.trim().isEmpty()) return "Неверные параметры.";
        if (cost <= 0) return "Стоимость должна быть больше 0.";
        if (level < 1 || level > 5) return "Уровень должен быть от 1 до 5.";
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        Fortification f = new Fortification();
        f.chunkKey = chunkKey;
        f.nationId = nationId;
        f.level = level;
        f.defenseBonus = level * 0.1; // 10% per level
        f.builtAt = System.currentTimeMillis();
        nationForts.computeIfAbsent(nationId, k -> new HashMap<>()).put(chunkKey, f);
        n.setTreasury(n.getTreasury() - cost);
        try {
            plugin.getNationManager().save(n);
            saveFortification(f);
        } catch (Exception ignored) {}
        
        // VISUAL EFFECTS: Announce fortification construction
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            VisualEffectsService effectsService = plugin.getVisualEffectsService();
            String msg = "§b🛡 Укрепление построено! Уровень: §f" + level + " §7(Защита: §a+" + String.format("%.0f", f.defenseBonus * 100) + "%§7)";
            Collection<UUID> citizens = n.getCitizens() != null ? n.getCitizens() : Collections.emptyList();
            for (UUID citizenId : citizens) {
                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                if (citizen != null && citizen.isOnline()) {
                    if (effectsService != null) {
                        effectsService.sendActionBar(citizen, msg);
                    }
                    org.bukkit.Location loc = citizen.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.05);
                    citizen.playSound(loc, org.bukkit.Sound.BLOCK_ANVIL_USE, 0.6f, 1.2f);
                }
            }
        });
        
        return "Укрепление построено (уровень " + level + "). Защита: +" + (f.defenseBonus * 100) + "%";
    }

    public synchronized double getDefenseBonus(String nationId, String chunkKey) {
        if (nationId == null || chunkKey == null) return 1.0;
        Map<String, Fortification> forts = nationForts.get(nationId);
        if (forts == null) return 1.0;
        Fortification f = forts.get(chunkKey);
        return f != null ? 1.0 + f.defenseBonus : 1.0;
    }

    private void loadAll() {
        File[] files = fortsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = o.get("nationId").getAsString();
                Fortification fort = new Fortification();
                fort.chunkKey = o.get("chunkKey").getAsString();
                fort.nationId = nationId;
                fort.level = o.get("level").getAsInt();
                fort.defenseBonus = o.get("defenseBonus").getAsDouble();
                fort.builtAt = o.get("builtAt").getAsLong();
                nationForts.computeIfAbsent(nationId, k -> new HashMap<>()).put(fort.chunkKey, fort);
            } catch (Exception ignored) {}
        }
    }

    private void saveFortification(Fortification f) {
        File file = new File(fortsDir, f.nationId + "_" + f.chunkKey.replace(":", "_") + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("chunkKey", f.chunkKey);
        o.addProperty("nationId", f.nationId);
        o.addProperty("level", f.level);
        o.addProperty("defenseBonus", f.defenseBonus);
        o.addProperty("builtAt", f.builtAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive fortification statistics.
     */
    public synchronized Map<String, Object> getFortificationStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Fortification> forts = nationForts.get(nationId);
        if (forts == null) forts = Collections.emptyMap();
        
        stats.put("totalForts", forts.size());
        
        // Forts by level
        Map<Integer, Integer> byLevel = new HashMap<>();
        double totalDefenseBonus = 0.0;
        for (Fortification f : forts.values()) {
            byLevel.put(f.level, byLevel.getOrDefault(f.level, 0) + 1);
            totalDefenseBonus += f.defenseBonus;
        }
        stats.put("byLevel", byLevel);
        stats.put("totalDefenseBonus", totalDefenseBonus * 100); // Convert to percentage
        stats.put("averageDefenseBonus", forts.size() > 0 ? (totalDefenseBonus / forts.size()) * 100 : 0);
        
        // Fort details
        List<Map<String, Object>> fortsList = new ArrayList<>();
        for (Fortification f : forts.values()) {
            Map<String, Object> fortData = new HashMap<>();
            fortData.put("chunkKey", f.chunkKey);
            fortData.put("level", f.level);
            fortData.put("defenseBonus", f.defenseBonus * 100);
            fortData.put("age", (System.currentTimeMillis() - f.builtAt) / (1000 * 60 * 60 * 24)); // days
            fortsList.add(fortData);
        }
        stats.put("forts", fortsList);
        
        return stats;
    }
    
    /**
     * Upgrade fortification.
     */
    public synchronized String upgradeFortification(String nationId, String chunkKey, double cost) throws IOException {
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (nationId == null || chunkKey == null) return "Неверные параметры.";
        if (cost <= 0) return "Стоимость должна быть больше 0.";
        com.axiom.model.Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < cost) return "Недостаточно средств.";
        
        Map<String, Fortification> forts = nationForts.get(nationId);
        if (forts == null) return "Укрепления не найдены.";
        
        Fortification f = forts.get(chunkKey);
        if (f == null) return "Укрепление в этом чанке не найдено.";
        if (f.level >= 5) return "Максимальный уровень достигнут.";
        
        f.level++;
        f.defenseBonus = f.level * 0.1;
        n.setTreasury(n.getTreasury() - cost);
        
        plugin.getNationManager().save(n);
        saveFortification(f);
        
        return "Укрепление улучшено до уровня " + f.level + ". Защита: +" + (f.defenseBonus * 100) + "%";
    }
    
    /**
     * Destroy fortification.
     */
    public synchronized String destroyFortification(String nationId, String chunkKey) throws IOException {
        if (nationId == null || chunkKey == null) return "Неверные параметры.";
        Map<String, Fortification> forts = nationForts.get(nationId);
        if (forts == null) return "Укрепления не найдены.";
        
        Fortification f = forts.remove(chunkKey);
        if (f == null) return "Укрепление не найдено.";
        
        // Delete file
        File file = new File(fortsDir, nationId + "_" + chunkKey.replace(":", "_") + ".json");
        if (file.exists()) file.delete();
        
        return "Укрепление разрушено.";
    }
    
    /**
     * Get all fortifications for a nation.
     */
    public synchronized List<Fortification> getNationFortifications(String nationId) {
        Map<String, Fortification> forts = nationForts.get(nationId);
        if (forts == null) return Collections.emptyList();
        return new ArrayList<>(forts.values());
    }
    
    /**
     * Calculate total defense bonus from all fortifications.
     */
    public synchronized double getTotalDefenseBonus(String nationId) {
        Map<String, Fortification> forts = nationForts.get(nationId);
        if (forts == null) return 1.0;
        
        double totalBonus = 0.0;
        for (Fortification f : forts.values()) {
            totalBonus += f.defenseBonus;
        }
        
        // Cap at +100% total
        return Math.min(2.0, 1.0 + totalBonus);
    }
    
    /**
     * Get global fortification statistics.
     */
    public synchronized Map<String, Object> getGlobalFortificationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalForts = 0;
        double totalDefenseBonus = 0.0;
        Map<Integer, Integer> byLevel = new HashMap<>();
        Map<String, Integer> byNation = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Fortification>> entry : nationForts.entrySet()) {
            for (Fortification f : entry.getValue().values()) {
                totalForts++;
                totalDefenseBonus += f.defenseBonus;
                byLevel.put(f.level, byLevel.getOrDefault(f.level, 0) + 1);
                byNation.put(f.nationId, byNation.getOrDefault(f.nationId, 0) + 1);
            }
        }
        
        stats.put("totalForts", totalForts);
        stats.put("totalDefenseBonus", totalDefenseBonus * 100);
        stats.put("averageDefenseBonus", totalForts > 0 ? (totalDefenseBonus / totalForts) * 100 : 0);
        stats.put("fortsByLevel", byLevel);
        stats.put("fortsByNation", byNation);
        
        // Top nations by forts
        List<Map.Entry<String, Integer>> topByForts = byNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByForts", topByForts);
        
        // Average level
        double totalLevels = 0.0;
        for (Fortification f : nationForts.values().stream()
                .flatMap(m -> m.values().stream())
                .collect(java.util.stream.Collectors.toList())) {
            totalLevels += f.level;
        }
        stats.put("averageLevel", totalForts > 0 ? totalLevels / totalForts : 0);
        
        return stats;
    }
}

