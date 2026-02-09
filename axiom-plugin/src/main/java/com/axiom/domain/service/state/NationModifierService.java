package com.axiom.domain.service.state;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages temporary modifiers for nations (buffs/debuffs from events). */
public class NationModifierService {
    private final AXIOM plugin;
    private final File modifiersDir;
    private final Map<String, List<Modifier>> nationModifiers = new HashMap<>(); // nationId -> modifiers

    public static class Modifier {
        String id;
        String nationId;
        String type; // "economy", "military", "diplomacy", "happiness"
        String effect; // "bonus" or "penalty"
        double value; // multiplier or absolute
        long expiresAt;
    }

    public NationModifierService(AXIOM plugin) {
        this.plugin = plugin;
        this.modifiersDir = new File(plugin.getDataFolder(), "modifiers");
        this.modifiersDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpired, 0, 20 * 60); // every minute
    }

    public synchronized String addModifier(String nationId, String type, String effect, double value, long durationMinutes) {
        if (isBlank(nationId) || isBlank(type) || isBlank(effect)) return "Некорректные данные.";
        if (!Double.isFinite(value) || durationMinutes <= 0) return "Некорректные данные.";
        String normalizedType = type.toLowerCase();
        String normalizedEffect = effect.toLowerCase();
        if (!"bonus".equals(normalizedEffect) && !"penalty".equals(normalizedEffect)) {
            return "Некорректный эффект.";
        }
        Modifier m = new Modifier();
        m.id = UUID.randomUUID().toString();
        m.nationId = nationId;
        m.type = normalizedType;
        m.effect = normalizedEffect;
        m.value = Math.max(0, Math.min(1.0, value));
        m.expiresAt = System.currentTimeMillis() + (durationMinutes * 60_000L);
        nationModifiers.computeIfAbsent(nationId, k -> new ArrayList<>()).add(m);
        saveModifier(m);
        
        // VISUAL EFFECTS: Notify nation members of modifier application
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getNationManager() == null) return;
            com.axiom.domain.model.Nation n = plugin.getNationManager().getNationById(nationId);
            if (n != null) {
                String typeName = getTypeDisplayName(type);
                String effectName = normalizedEffect.equals("bonus") ? "§aБонус" : "§cШтраф";
                String msg = effectName + " §f" + typeName + " §7(" + String.format("%.0f", m.value * 100) + "%) - §7" + durationMinutes + " мин";
                if (n.getCitizens() != null) {
                    for (UUID citizenId : n.getCitizens()) {
                        org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                        if (citizen != null && citizen.isOnline()) {
                            if (plugin.getVisualEffectsService() != null) {
                                plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                            }
                            // Color-coded particles
                            org.bukkit.Location loc = citizen.getLocation();
                            if (loc.getWorld() != null) {
                                if (normalizedEffect.equals("bonus")) {
                                    loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.05);
                                } else {
                                    loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.05);
                                }
                            }
                        }
                    }
                }
            }
        });
        
        return "Модификатор применён: " + type + " " + effect + " " + value;
    }
    
    private String getTypeDisplayName(String type) {
        if (type == null) return "";
        switch (type.toLowerCase()) {
            case "economy": return "Экономика";
            case "military": return "Военная мощь";
            case "diplomacy": return "Дипломатия";
            case "happiness": return "Счастье";
            default: return type;
        }
    }

    public synchronized double getModifierValue(String nationId, String type) {
        if (isBlank(nationId) || isBlank(type)) return 1.0;
        List<Modifier> mods = nationModifiers.get(nationId);
        if (mods == null) return 1.0;
        double total = 1.0;
        long now = System.currentTimeMillis();
        for (Modifier m : mods) {
            if (m.type != null && m.type.equals(type) && m.expiresAt > now) {
                if ("bonus".equals(m.effect)) total *= (1.0 + m.value);
                else total *= (1.0 - m.value);
            }
        }
        return total;
    }

    private synchronized void cleanupExpired() {
        long now = System.currentTimeMillis();
        for (var entry : nationModifiers.entrySet()) {
            List<Modifier> expired = new ArrayList<>();
            for (Modifier m : entry.getValue()) {
                if (m.expiresAt <= now) {
                    expired.add(m);
                }
            }
            if (!expired.isEmpty()) {
                entry.getValue().removeAll(expired);
                
                // VISUAL EFFECTS: Notify nation members of expired modifiers
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (plugin.getNationManager() == null) return;
                    com.axiom.domain.model.Nation n = plugin.getNationManager().getNationById(entry.getKey());
                    if (n != null && !expired.isEmpty()) {
                        Modifier firstExpired = expired.get(0);
                        String typeName = getTypeDisplayName(firstExpired.type);
                        String msg = "§7⏱ Модификатор '" + typeName + "' истёк";
                        if (n.getCitizens() != null) {
                            for (UUID citizenId : n.getCitizens()) {
                                org.bukkit.entity.Player citizen = org.bukkit.Bukkit.getPlayer(citizenId);
                                if (citizen != null && citizen.isOnline() && plugin.getVisualEffectsService() != null) {
                                    plugin.getVisualEffectsService().sendActionBar(citizen, msg);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private void loadAll() {
        File[] files = modifiersDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Modifier m = new Modifier();
                m.id = o.get("id").getAsString();
                m.nationId = o.get("nationId").getAsString();
                m.type = o.get("type").getAsString();
                m.effect = o.get("effect").getAsString();
                m.value = o.get("value").getAsDouble();
                m.expiresAt = o.get("expiresAt").getAsLong();
                if (m.expiresAt > System.currentTimeMillis()) {
                    nationModifiers.computeIfAbsent(m.nationId, k -> new ArrayList<>()).add(m);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveModifier(Modifier m) {
        File f = new File(modifiersDir, m.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", m.id);
        o.addProperty("nationId", m.nationId);
        o.addProperty("type", m.type);
        o.addProperty("effect", m.effect);
        o.addProperty("value", m.value);
        o.addProperty("expiresAt", m.expiresAt);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive modifier statistics.
     */
    public synchronized Map<String, Object> getModifierStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Modifier> mods = nationModifiers.get(nationId);
        if (mods == null) mods = Collections.emptyList();
        
        stats.put("activeModifiers", mods.size());
        
        // Modifiers by type
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byEffect = new HashMap<>();
        long now = System.currentTimeMillis();
        
        List<Map<String, Object>> activeModsList = new ArrayList<>();
        for (Modifier m : mods) {
            if (m.expiresAt > now) {
                byType.put(m.type, byType.getOrDefault(m.type, 0) + 1);
                byEffect.put(m.effect, byEffect.getOrDefault(m.effect, 0) + 1);
                
                Map<String, Object> modData = new HashMap<>();
                modData.put("type", m.type);
                modData.put("effect", m.effect);
                modData.put("value", m.value);
                modData.put("timeRemaining", (m.expiresAt - now) / 1000 / 60);
                activeModsList.add(modData);
            }
        }
        
        stats.put("byType", byType);
        stats.put("byEffect", byEffect);
        stats.put("activeModifiersList", activeModsList);
        
        // Calculate total modifier values
        Map<String, Double> totalValues = new HashMap<>();
        totalValues.put("economy", getModifierValue(nationId, "economy"));
        totalValues.put("military", getModifierValue(nationId, "military"));
        totalValues.put("diplomacy", getModifierValue(nationId, "diplomacy"));
        totalValues.put("happiness", getModifierValue(nationId, "happiness"));
        stats.put("totalValues", totalValues);
        
        return stats;
    }
    
    /**
     * Remove a modifier early.
     */
    public synchronized String removeModifier(String nationId, String modifierId) throws IOException {
        if (isBlank(nationId) || isBlank(modifierId)) return "Некорректные данные.";
        List<Modifier> mods = nationModifiers.get(nationId);
        if (mods == null) return "Модификаторы не найдены.";
        
        Modifier toRemove = mods.stream()
            .filter(m -> m.id.equals(modifierId))
            .findFirst()
            .orElse(null);
        
        if (toRemove == null) return "Модификатор не найден.";
        
        mods.remove(toRemove);
        
        // Delete file
        File f = new File(modifiersDir, modifierId + ".json");
        if (f.exists()) f.delete();
        
        return "Модификатор удалён.";
    }
    
    /**
     * Get all active modifiers for a nation.
     */
    public synchronized List<Modifier> getActiveModifiers(String nationId) {
        if (isBlank(nationId)) return Collections.emptyList();
        List<Modifier> mods = nationModifiers.get(nationId);
        if (mods == null) return Collections.emptyList();
        
        long now = System.currentTimeMillis();
        return mods.stream()
            .filter(m -> m.expiresAt > now)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Extend modifier duration.
     */
    public synchronized String extendModifier(String nationId, String modifierId, long additionalMinutes) throws IOException {
        if (isBlank(nationId) || isBlank(modifierId) || additionalMinutes <= 0) return "Некорректные данные.";
        List<Modifier> mods = nationModifiers.get(nationId);
        if (mods == null) return "Модификаторы не найдены.";
        
        Modifier mod = mods.stream()
            .filter(m -> m.id.equals(modifierId))
            .findFirst()
            .orElse(null);
        
        if (mod == null) return "Модификатор не найден.";
        
        mod.expiresAt += additionalMinutes * 60_000L;
        saveModifier(mod);
        
        return "Модификатор продлён на " + additionalMinutes + " минут.";
    }
    
    /**
     * Get global modifier statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalModifierStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalActiveModifiers = 0;
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byEffect = new HashMap<>();
        Map<String, Integer> modifiersByNation = new HashMap<>();
        long now = System.currentTimeMillis();
        
        for (Map.Entry<String, List<Modifier>> entry : nationModifiers.entrySet()) {
            int activeCount = 0;
            for (Modifier m : entry.getValue()) {
                if (m.expiresAt > now) {
                    totalActiveModifiers++;
                    activeCount++;
                    byType.put(m.type, byType.getOrDefault(m.type, 0) + 1);
                    byEffect.put(m.effect, byEffect.getOrDefault(m.effect, 0) + 1);
                }
            }
            if (activeCount > 0) {
                modifiersByNation.put(entry.getKey(), activeCount);
            }
        }
        
        stats.put("totalActiveModifiers", totalActiveModifiers);
        stats.put("modifiersByType", byType);
        stats.put("modifiersByEffect", byEffect);
        stats.put("modifiersByNation", modifiersByNation);
        stats.put("nationsWithModifiers", modifiersByNation.size());
        stats.put("averageModifiersPerNation", modifiersByNation.size() > 0 ? 
            (double) totalActiveModifiers / modifiersByNation.size() : 0);
        
        // Top nations by modifiers
        List<Map.Entry<String, Integer>> topByModifiers = modifiersByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByModifiers", topByModifiers);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

