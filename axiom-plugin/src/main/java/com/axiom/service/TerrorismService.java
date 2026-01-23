package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages terrorist cells and attacks. */
public class TerrorismService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File cellsDir;
    private final Map<String, TerroristCell> cells = new HashMap<>(); // cellId -> cell

    public static class TerroristCell {
        String id;
        String nationId; // operating in this nation
        String sponsorNationId; // who funds them (can be null)
        int activity; // 0-100
        long nextAttack;
    }

    public TerrorismService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.cellsDir = new File(plugin.getDataFolder(), "terrorism");
        this.cellsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processAttacks, 0, 20 * 60 * 30); // every 30 minutes
    }

    public synchronized String createCell(String nationId, String sponsorId, double funding) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(sponsorId)) return "Неверные параметры.";
        if (!Double.isFinite(funding) || funding < 0) return "Некорректное финансирование.";
        Nation sponsor = nationManager.getNationById(sponsorId);
        if (sponsor == null) return "Нация-спонсор не найдена.";
        if (nationManager.getNationById(nationId) == null) return "Целевая нация не найдена.";
        if (sponsor.getTreasury() < funding) return "Недостаточно средств.";
        String cellId = UUID.randomUUID().toString().substring(0, 8);
        TerroristCell cell = new TerroristCell();
        cell.id = cellId;
        cell.nationId = nationId;
        cell.sponsorNationId = sponsorId;
        cell.activity = 10;
        cell.nextAttack = System.currentTimeMillis() + 24 * 60 * 60_000L; // 24 hours
        cells.put(cellId, cell);
        sponsor.setTreasury(sponsor.getTreasury() - funding);
        try {
            nationManager.save(sponsor);
            saveCell(cell);
        } catch (Exception ignored) {}
        return "Террористическая ячейка создана (ID: " + cellId + ")";
    }

    private synchronized void processAttacks() {
        if (nationManager == null || plugin.getHappinessService() == null) return;
        long now = System.currentTimeMillis();
        for (TerroristCell cell : cells.values()) {
            if (cell == null || isBlank(cell.nationId)) continue;
            if (cell.nextAttack > now || cell.activity < 50) continue;
            // Attack happens
            Nation target = nationManager.getNationById(cell.nationId);
            if (target != null) {
                if (target.getHistory() != null) {
                    target.getHistory().add("Террористическая атака! Счастье -10%");
                }
                plugin.getHappinessService().modifyHappiness(cell.nationId, -10.0);
                cell.activity = Math.max(0, cell.activity - 30); // Reduced after attack
                cell.nextAttack = now + 24 * 60 * 60_000L;
                try {
                    nationManager.save(target);
                    saveCell(cell);
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadAll() {
        File[] files = cellsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                TerroristCell cell = new TerroristCell();
                cell.id = o.has("id") ? o.get("id").getAsString() : null;
                cell.nationId = o.has("nationId") ? o.get("nationId").getAsString() : null;
                cell.sponsorNationId = o.has("sponsorNationId") ? o.get("sponsorNationId").getAsString() : null;
                cell.activity = o.has("activity") ? o.get("activity").getAsInt() : 0;
                cell.nextAttack = o.has("nextAttack") ? o.get("nextAttack").getAsLong() : 0L;
                if (!isBlank(cell.id)) {
                    cells.put(cell.id, cell);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveCell(TerroristCell cell) {
        if (cell == null || isBlank(cell.id)) return;
        File f = new File(cellsDir, cell.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", cell.id);
        o.addProperty("nationId", cell.nationId);
        if (cell.sponsorNationId != null) o.addProperty("sponsorNationId", cell.sponsorNationId);
        o.addProperty("activity", cell.activity);
        o.addProperty("nextAttack", cell.nextAttack);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive terrorism statistics.
     */
    public synchronized Map<String, Object> getTerrorismStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        // Cells operating in this nation
        List<TerroristCell> operatingCells = new ArrayList<>();
        for (TerroristCell cell : cells.values()) {
            if (cell != null && nationId.equals(cell.nationId)) {
                operatingCells.add(cell);
            }
        }
        
        stats.put("operatingCells", operatingCells.size());
        stats.put("cells", operatingCells);
        
        // Cells sponsored by this nation
        List<TerroristCell> sponsoredCells = new ArrayList<>();
        for (TerroristCell cell : cells.values()) {
            if (cell != null && cell.sponsorNationId != null && cell.sponsorNationId.equals(nationId)) {
                sponsoredCells.add(cell);
            }
        }
        
        stats.put("sponsoredCells", sponsoredCells.size());
        stats.put("sponsoredCellsList", sponsoredCells);
        
        // Terrorism threat level
        int totalActivity = operatingCells.stream().mapToInt(c -> c.activity).sum();
        double threatLevel = Math.min(100, totalActivity);
        stats.put("threatLevel", threatLevel);
        
        // Threat rating
        String rating = "ОТСУТСТВУЕТ";
        if (threatLevel >= 80) rating = "КРИТИЧЕСКИЙ";
        else if (threatLevel >= 60) rating = "ВЫСОКИЙ";
        else if (threatLevel >= 40) rating = "СРЕДНИЙ";
        else if (threatLevel >= 20) rating = "НИЗКИЙ";
        else if (threatLevel > 0) rating = "МИНИМАЛЬНЫЙ";
        stats.put("rating", rating);
        
        // Expected next attack
        long nextAttack = operatingCells.stream()
            .filter(c -> c.nextAttack > System.currentTimeMillis())
            .mapToLong(c -> c.nextAttack)
            .min()
            .orElse(0);
        stats.put("nextAttackExpected", nextAttack > 0 ? (nextAttack - System.currentTimeMillis()) / 1000 / 60 : -1); // minutes
        
        return stats;
    }
    
    /**
     * Eliminate terrorist cell (counter-terrorism operation).
     */
    public synchronized String eliminateCell(String nationId, String cellId, double cost) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(nationId) || isBlank(cellId)) return "Неверные параметры.";
        if (!Double.isFinite(cost) || cost < 0) return "Некорректная стоимость.";
        TerroristCell cell = cells.get(cellId);
        if (cell == null) return "Ячейка не найдена.";
        
        Nation n = nationManager.getNationById(nationId);
        if (n == null || n.getTreasury() < cost) return "Недостаточно средств.";
        
        // Check if nation has right to eliminate (must be operating in their territory or be the victim)
        if (!nationId.equals(cell.nationId)) {
            return "Нет прав на ликвидацию этой ячейки.";
        }
        
        cells.remove(cellId);
        n.setTreasury(n.getTreasury() - cost);
        
        // Delete file
        File f = new File(cellsDir, cellId + ".json");
        if (f.exists()) f.delete();
        
        nationManager.save(n);
        if (n.getHistory() != null) {
            n.getHistory().add("Террористическая ячейка ликвидирована");
        }
        
        return "Ячейка ликвидирована.";
    }
    
    /**
     * Increase cell activity (through funding, etc.).
     */
    public synchronized String fundCell(String sponsorId, String cellId, double amount) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(sponsorId) || isBlank(cellId)) return "Неверные параметры.";
        if (!Double.isFinite(amount) || amount <= 0) return "Некорректная сумма.";
        TerroristCell cell = cells.get(cellId);
        if (cell == null) return "Ячейка не найдена.";
        if (cell.sponsorNationId == null || !cell.sponsorNationId.equals(sponsorId)) return "Вы не спонсор этой ячейки.";
        
        Nation sponsor = nationManager.getNationById(sponsorId);
        if (sponsor == null || sponsor.getTreasury() < amount) return "Недостаточно средств.";
        
        cell.activity = Math.min(100, cell.activity + (int)(amount / 1000.0)); // +1 activity per 1000
        sponsor.setTreasury(sponsor.getTreasury() - amount);
        
        nationManager.save(sponsor);
        saveCell(cell);
        
        return "Ячейка профинансирована. Активность: " + cell.activity;
    }
    
    /**
     * Get all cells in a nation.
     */
    public synchronized List<TerroristCell> getNationCells(String nationId) {
        List<TerroristCell> result = new ArrayList<>();
        for (TerroristCell cell : cells.values()) {
            if (cell != null && nationId.equals(cell.nationId)) {
                result.add(cell);
            }
        }
        return result;
    }
    
    /**
     * Calculate counter-terrorism cost.
     */
    public synchronized double calculateCounterTerrorismCost(String nationId) {
        if (isBlank(nationId)) return 0.0;
        int cellCount = getNationCells(nationId).size();
        return cellCount * 500.0; // Base cost per cell
    }
    
    /**
     * Get global terrorism statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalTerrorismStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalCells", cells.size());
        
        Map<String, Integer> cellsByNation = new HashMap<>();
        Map<String, Integer> cellsBySponsor = new HashMap<>();
        double totalActivity = 0.0;
        
        for (TerroristCell cell : cells.values()) {
            if (cell == null) continue;
            if (cell.nationId != null) {
                cellsByNation.put(cell.nationId, cellsByNation.getOrDefault(cell.nationId, 0) + 1);
            }
            if (cell.sponsorNationId != null) {
                cellsBySponsor.put(cell.sponsorNationId, cellsBySponsor.getOrDefault(cell.sponsorNationId, 0) + 1);
            }
            totalActivity += cell.activity;
        }
        
        stats.put("cellsByNation", cellsByNation);
        stats.put("cellsBySponsor", cellsBySponsor);
        stats.put("nationsWithCells", cellsByNation.size());
        stats.put("nationsSponsoringCells", cellsBySponsor.size());
        stats.put("averageActivity", cells.size() > 0 ? totalActivity / cells.size() : 0);
        
        // Activity distribution
        int critical = 0, high = 0, medium = 0, low = 0, minimal = 0;
        for (TerroristCell cell : cells.values()) {
            if (cell == null) continue;
            if (cell.activity >= 80) critical++;
            else if (cell.activity >= 60) high++;
            else if (cell.activity >= 40) medium++;
            else if (cell.activity >= 20) low++;
            else minimal++;
        }
        
        Map<String, Integer> activityDistribution = new HashMap<>();
        activityDistribution.put("critical", critical);
        activityDistribution.put("high", high);
        activityDistribution.put("medium", medium);
        activityDistribution.put("low", low);
        activityDistribution.put("minimal", minimal);
        stats.put("activityDistribution", activityDistribution);
        
        // Top nations by cells (most affected)
        List<Map.Entry<String, Integer>> topByCells = cellsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCells", topByCells);
        
        // Top sponsors
        List<Map.Entry<String, Integer>> topSponsors = cellsBySponsor.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topSponsors", topSponsors);
        
        // Upcoming attacks count
        long now = System.currentTimeMillis();
        int upcomingAttacks = 0;
        for (TerroristCell cell : cells.values()) {
            if (cell != null && cell.nextAttack > now && cell.activity >= 50) {
                upcomingAttacks++;
            }
        }
        stats.put("upcomingAttacks", upcomingAttacks);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

