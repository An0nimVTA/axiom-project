package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.axiom.domain.service.state.NationManager;
import com.axiom.domain.service.state.TerritoryService;

/**
 * Web export service for AXIOM data.
 * Generates JSON files for website integration (Leaflet.js, React maps).
 */
public class WebExportService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final TerritoryService territoryService;
    private final File webExportDir;
    
    public WebExportService(AXIOM plugin, NationManager nationManager, TerritoryService territoryService) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryService = territoryService;
        this.webExportDir = new File(plugin.getDataFolder(), "web");
        this.webExportDir.mkdirs();
        
        // Export every 5 minutes
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::exportData, 
            20 * 60 * 5, // First export after 5 minutes
            20 * 60 * 5); // Then every 5 minutes
    }
    
    /**
     * Export all AXIOM data to web format.
     */
    public synchronized void exportData() {
        try {
            if (nationManager == null) return;
            JsonObject root = new JsonObject();
            root.addProperty("timestamp", System.currentTimeMillis());
            root.addProperty("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            
            // Nations data
            JsonArray nations = new JsonArray();
            for (Nation n : nationManager.getAll()) {
                if (n == null) continue;
                JsonObject nationObj = new JsonObject();
                nationObj.addProperty("id", n.getId());
                nationObj.addProperty("name", n.getName());
                String leaderName = "Unknown";
                if (n.getLeader() != null) {
                    leaderName = Bukkit.getOfflinePlayer(n.getLeader()).getName();
                    if (leaderName == null) leaderName = "Unknown";
                }
                nationObj.addProperty("leader", leaderName);
                nationObj.addProperty("government", n.getGovernmentType());
                nationObj.addProperty("treasury", n.getTreasury());
                nationObj.addProperty("currency", n.getCurrencyCode());
                nationObj.addProperty("inflation", n.getInflation());
                nationObj.addProperty("taxRate", n.getTaxRate());
                nationObj.addProperty("citizens", n.getCitizens() != null ? n.getCitizens().size() : 0);
                Set<String> chunkKeys = getNationChunkKeys(n);
                int chunkCount = chunkKeys.size();
                nationObj.addProperty("chunks", chunkCount);
                nationObj.addProperty("motto", n.getMotto() != null ? n.getMotto() : "");
                nationObj.addProperty("flagColor", n.getFlagIconMaterial() != null ? n.getFlagIconMaterial() : "BLUE_BANNER");
                
                // Territories (for map)
                JsonArray territories = new JsonArray();
                for (String chunkKey : chunkKeys) {
                    String[] parts = chunkKey.split(":");
                    if (parts.length == 3) {
                        try {
                            JsonObject territory = new JsonObject();
                            territory.addProperty("world", parts[0]);
                            territory.addProperty("x", Integer.parseInt(parts[1]));
                            territory.addProperty("z", Integer.parseInt(parts[2]));
                            territories.add(territory);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                nationObj.add("territories", territories);
                
                // Capital
                if (n.getCapitalChunkStr() != null && !n.getCapitalChunkStr().isEmpty()) {
                    String[] capitalParts = n.getCapitalChunkStr().split(":");
                    if (capitalParts.length == 3) {
                        try {
                            JsonObject capital = new JsonObject();
                            capital.addProperty("world", capitalParts[0]);
                            capital.addProperty("x", Integer.parseInt(capitalParts[1]));
                            capital.addProperty("z", Integer.parseInt(capitalParts[2]));
                            nationObj.add("capital", capital);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                // Diplomacy
                JsonArray allies = new JsonArray();
                if (n.getAllies() != null) {
                    for (String allyId : n.getAllies()) {
                        Nation ally = nationManager.getNationById(allyId);
                        if (ally != null) {
                            allies.add(ally.getName());
                        }
                    }
                }
                nationObj.add("allies", allies);
                
                JsonArray enemies = new JsonArray();
                if (n.getEnemies() != null) {
                    for (String enemyId : n.getEnemies()) {
                        Nation enemy = nationManager.getNationById(enemyId);
                        if (enemy != null) {
                            enemies.add(enemy.getName());
                        }
                    }
                }
                nationObj.add("enemies", enemies);
                
                // Stats
                JsonObject stats = new JsonObject();
                int warsWon = 0;
                int warsLost = 0;
                int treatiesSigned = 0;
                if (plugin.getStatisticsService() != null) {
                    var nationStats = plugin.getStatisticsService().getStats(n.getId());
                    if (nationStats != null) {
                        warsWon = nationStats.warsWon;
                        warsLost = nationStats.warsLost;
                        treatiesSigned = nationStats.treatiesSigned;
                    }
                }
                stats.addProperty("warsWon", warsWon);
                stats.addProperty("warsLost", warsLost);
                stats.addProperty("treaties", treatiesSigned);
                nationObj.add("stats", stats);
                
                nations.add(nationObj);
            }
            root.add("nations", nations);
            
            // Global stats
            JsonObject globalStats = new JsonObject();
            globalStats.addProperty("totalNations", nations.size());
            // Count total unique players who have joined any nation
            Set<UUID> allPlayers = new HashSet<>();
            for (Nation n : nationManager.getAll()) {
                if (n.getLeader() != null) allPlayers.add(n.getLeader());
                allPlayers.addAll(n.getCitizens());
            }
            globalStats.addProperty("totalPlayers", allPlayers.size());
            root.add("global", globalStats);
            
            // Write to file
            File exportFile = new File(webExportDir, "data.json");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(exportFile), StandardCharsets.UTF_8)) {
                w.write(root.toString());
            }
            
            // Also create a minimal version for fast loading
            createMinimalExport(root);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка экспорта данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create minimal export (nations list only, no territories).
     */
    private void createMinimalExport(JsonObject fullData) throws IOException {
        JsonObject minimal = new JsonObject();
        if (!fullData.has("timestamp") || !fullData.has("nations")) return;
        minimal.addProperty("timestamp", fullData.get("timestamp").getAsLong());
        
        JsonArray nations = new JsonArray();
        for (var nationElem : fullData.getAsJsonArray("nations")) {
            JsonObject nationObj = nationElem.getAsJsonObject();
            JsonObject minimalNation = new JsonObject();
            minimalNation.addProperty("id", nationObj.get("id").getAsString());
            minimalNation.addProperty("name", nationObj.get("name").getAsString());
            minimalNation.addProperty("citizens", nationObj.get("citizens").getAsInt());
            minimalNation.addProperty("chunks", nationObj.get("chunks").getAsInt());
            minimalNation.addProperty("treasury", nationObj.get("treasury").getAsDouble());
            nations.add(minimalNation);
        }
        minimal.add("nations", nations);
        minimal.add("global", fullData.get("global"));
        
        File minimalFile = new File(webExportDir, "data_minimal.json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(minimalFile), StandardCharsets.UTF_8)) {
            w.write(minimal.toString());
        }
    }

    private Set<String> getNationChunkKeys(Nation nation) {
        if (nation == null || nation.getId() == null) {
            return new HashSet<>();
        }
        MapBoundaryService mapBoundaryService = plugin != null ? plugin.getMapBoundaryService() : null;
        if (mapBoundaryService != null) {
            return mapBoundaryService.getNationBoundaries(nation.getId());
        }
        if (territoryService != null) {
            Set<String> keys = new HashSet<>();
            for (com.axiom.domain.model.ChunkPos pos : territoryService.getNationClaims(nation.getId())) {
                keys.add(pos.getWorld() + ":" + pos.getX() + ":" + pos.getZ());
            }
            return keys;
        }
        return nation.getClaimedChunkKeys() != null
            ? new HashSet<>(nation.getClaimedChunkKeys())
            : new HashSet<>();
    }
    
    /**
     * Manual export trigger.
     */
    public String exportNow() {
        exportData();
        return "Данные экспортированы в web/data.json";
    }
    
    /**
     * Get comprehensive web export statistics.
     */
    public synchronized Map<String, Object> getWebExportStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        File dataFile = new File(webExportDir, "data.json");
        File minimalFile = new File(webExportDir, "data_minimal.json");
        
        stats.put("dataFileExists", dataFile.exists());
        stats.put("minimalFileExists", minimalFile.exists());
        
        if (dataFile.exists()) {
            stats.put("dataFileSize", dataFile.length());
            stats.put("dataFileSizeKB", dataFile.length() / 1024.0);
            stats.put("dataFileLastModified", dataFile.lastModified());
            stats.put("dataFileLastModifiedDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(dataFile.lastModified())));
            stats.put("dataFileAge", (System.currentTimeMillis() - dataFile.lastModified()) / 1000 / 60); // minutes
        }
        
        if (minimalFile.exists()) {
            stats.put("minimalFileSize", minimalFile.length());
            stats.put("minimalFileSizeKB", minimalFile.length() / 1024.0);
            stats.put("minimalFileLastModified", minimalFile.lastModified());
        }
        
        // Export configuration
        stats.put("exportIntervalMinutes", 5);
        stats.put("exportDirectory", webExportDir.getAbsolutePath());
        
        // Nation count in export
        int nationCount = nationManager.getAll().size();
        stats.put("nationsExported", nationCount);
        
        // Export rating
        String rating = "НЕТ ЭКСПОРТА";
        if (dataFile.exists() && minimalFile.exists()) {
            long age = (System.currentTimeMillis() - dataFile.lastModified()) / 1000 / 60;
            if (age < 10) rating = "АКТУАЛЬНЫЙ";
            else if (age < 30) rating = "СВЕЖИЙ";
            else if (age < 60) rating = "УСТАРЕВШИЙ";
            else rating = "ОЧЕНЬ СТАРЫЙ";
        }
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Force export with custom format.
     */
    public synchronized String exportCustom(String format) {
        if (!format.equalsIgnoreCase("json")) {
            return "Неподдерживаемый формат. Доступен только JSON.";
        }
        
        exportData();
        return "Данные экспортированы в формате " + format.toUpperCase();
    }
    
    /**
     * Get export file path.
     */
    public synchronized String getExportPath() {
        return webExportDir.getAbsolutePath();
    }
    
    /**
     * Check if export is up to date (within 10 minutes).
     */
    public synchronized boolean isExportUpToDate() {
        File dataFile = new File(webExportDir, "data.json");
        if (!dataFile.exists()) return false;
        
        long age = (System.currentTimeMillis() - dataFile.lastModified()) / 1000 / 60;
        return age < 10; // Less than 10 minutes old
    }
    
    /**
     * Get global web export statistics (alias for getWebExportStatistics for consistency).
     */
    public synchronized Map<String, Object> getGlobalWebExportStatistics() {
        return getWebExportStatistics();
    }
}

