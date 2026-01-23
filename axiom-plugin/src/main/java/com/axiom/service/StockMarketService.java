package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages corporations and stock market (basic framework). */
public class StockMarketService {
    private final AXIOM plugin;
    private final File corporationsDir;
    private final Map<String, Corporation> corporations = new HashMap<>();

    public static class Corporation {
        public String id;
        public String name;
        public String ownerNationId;
        public String type; // "mine", "farm", "factory", "tech", "bank", "trading"
        public double value;
        public int shares;
        public int totalShares = 100; // Total shares available
        public Map<String, Integer> shareholders = new HashMap<>(); // nationId -> shares owned
        public double lastDividend = 0.0;
        public long foundedDate = System.currentTimeMillis();
        public String description = "";
        public boolean isPublic = false; // If true, can be traded on stock exchange
        public double dividendRate = 0.0; // % of profit paid as dividends
    }

    public StockMarketService(AXIOM plugin) {
        this.plugin = plugin;
        this.corporationsDir = new File(plugin.getDataFolder(), "corporations");
        this.corporationsDir.mkdirs();
        loadAll();
    }

    public synchronized String createCorporation(String nationId, String name, String type) throws IOException {
        if (isBlank(nationId) || isBlank(name) || isBlank(type)) return "Неверные параметры.";
        String id = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        if (corporations.containsKey(id)) return "Корпорация уже существует.";
        
        // Check if nation has enough treasury
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        Nation owner = plugin.getNationManager().getNationById(nationId);
        if (owner == null) return "Нация не найдена.";
        double cost = 50000.0; // Starting cost
        if (owner.getTreasury() < cost) return "Недостаточно средств для создания корпорации. Нужно: " + cost;
        
        Corporation c = new Corporation();
        c.id = id;
        c.name = name;
        c.ownerNationId = nationId;
        c.type = type;
        c.value = 10000.0;
        c.totalShares = 100;
        c.shares = 100;
        c.foundedDate = System.currentTimeMillis();
        c.shareholders.put(nationId, 100); // Owner gets all initial shares
        c.dividendRate = 0.05; // Default 5% dividend
        
        // Charge initial cost
        owner.setTreasury(owner.getTreasury() - cost);
        corporations.put(id, c);
        plugin.getNationManager().save(owner);
        save(c);
        
        // Visual effect for corporation creation
        org.bukkit.entity.Player firstPlayer = null;
        Collection<UUID> citizens = owner.getCitizens();
        if (citizens != null) {
            firstPlayer = citizens.stream()
                .map(uuid -> org.bukkit.Bukkit.getPlayer(uuid))
                .filter(p -> p != null && p.isOnline())
                .findFirst().orElse(null);
        }
        if (firstPlayer != null) {
            if (plugin.getVisualEffectsService() != null) {
                plugin.getVisualEffectsService().sendGlobalEvent(
                    firstPlayer,
                    "§a§l[КОРПОРАЦИЯ СОЗДАНА]", "§f" + name + " §7(" + type + ")", 
                    "§a🏢 Корпорация основана. Начальная стоимость: §f" + String.format("%.0f", c.value));
            }
        }
        
        return "Корпорация создана: " + name + " (Стоимость: " + cost + ")";
    }

    private void loadAll() {
        File[] files = corporationsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Corporation c = new Corporation();
                c.id = o.has("id") ? o.get("id").getAsString() : null;
                c.name = o.has("name") ? o.get("name").getAsString() : null;
                c.ownerNationId = o.has("ownerNationId") ? o.get("ownerNationId").getAsString() : null;
                c.type = o.has("type") ? o.get("type").getAsString() : null;
                c.value = o.has("value") ? o.get("value").getAsDouble() : 0.0;
                c.shares = o.has("shares") ? o.get("shares").getAsInt() : 0;
                c.totalShares = o.has("totalShares") ? o.get("totalShares").getAsInt() : 100;
                c.isPublic = o.has("isPublic") && o.get("isPublic").getAsBoolean();
                c.foundedDate = o.has("foundedDate") ? o.get("foundedDate").getAsLong() : System.currentTimeMillis();
                c.lastDividend = o.has("lastDividend") ? o.get("lastDividend").getAsDouble() : 0.0;
                c.dividendRate = o.has("dividendRate") ? o.get("dividendRate").getAsDouble() : 0.0;
                c.description = o.has("description") ? o.get("description").getAsString() : "";
                
                // Load shareholders
                if (o.has("shareholders")) {
                    JsonObject shareholdersObj = o.getAsJsonObject("shareholders");
                    if (shareholdersObj != null) {
                        for (String key : shareholdersObj.keySet()) {
                            c.shareholders.put(key, shareholdersObj.get(key).getAsInt());
                        }
                    }
                }
                
                if (!isBlank(c.id)) {
                    corporations.put(c.id, c);
                }
            } catch (Exception ignored) {}
        }
        
        // Start auto-update task for corporation values
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::updateAllCorporationValues, 
            20 * 60 * 30, 20 * 60 * 30); // Every 30 minutes
    }

    private void save(Corporation c) throws IOException {
        if (c == null || isBlank(c.id)) return;
        File f = new File(corporationsDir, c.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", c.id);
        o.addProperty("name", c.name);
        o.addProperty("ownerNationId", c.ownerNationId);
        o.addProperty("type", c.type);
        o.addProperty("value", c.value);
        o.addProperty("shares", c.shares);
        o.addProperty("totalShares", c.totalShares);
        o.addProperty("isPublic", c.isPublic);
        o.addProperty("foundedDate", c.foundedDate);
        o.addProperty("lastDividend", c.lastDividend);
        o.addProperty("dividendRate", c.dividendRate);
        o.addProperty("description", c.description);
        
        // Save shareholders
        JsonObject shareholdersObj = new JsonObject();
        for (Map.Entry<String, Integer> entry : c.shareholders.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                shareholdersObj.addProperty(entry.getKey(), entry.getValue());
            }
        }
        o.add("shareholders", shareholdersObj);
        
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Auto-update all corporation values periodically.
     */
    private void updateAllCorporationValues() {
        for (Corporation corp : corporations.values()) {
            if (corp == null) continue;
            try {
                updateCorporationValue(corp.id);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to update corporation value: " + corp.id + " - " + e.getMessage());
            }
        }
    }

    public synchronized List<Corporation> getCorporationsOf(String nationId) {
        List<Corporation> out = new ArrayList<>();
        for (Corporation c : corporations.values()) {
            if (c != null && Objects.equals(c.ownerNationId, nationId)) out.add(c);
        }
        return out;
    }
    
    /**
     * Get comprehensive stock market statistics for a nation.
     */
    public synchronized Map<String, Object> getStockMarketStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        List<Corporation> nationCorps = getCorporationsOf(nationId);
        stats.put("totalCorporations", nationCorps.size());
        
        double totalValue = nationCorps.stream().mapToDouble(c -> c.value).sum();
        stats.put("totalCorporationValue", totalValue);
        
        // Count by type
        Map<String, Integer> byType = new HashMap<>();
        for (Corporation c : nationCorps) {
            if (c != null && c.type != null) {
                byType.put(c.type, byType.getOrDefault(c.type, 0) + 1);
            }
        }
        stats.put("byType", byType);
        
        // Market capitalization
        long totalShares = nationCorps.stream().mapToLong(c -> c.shares).sum();
        stats.put("totalShares", totalShares);
        stats.put("averageSharePrice", totalShares > 0 ? totalValue / totalShares : 0);
        
        return stats;
    }
    
    /**
     * Update corporation value based on nation performance.
     */
    public synchronized void updateCorporationValue(String corporationId) throws IOException {
        if (isBlank(corporationId)) return;
        Corporation c = corporations.get(corporationId);
        if (c == null) return;
        
        // Value fluctuates based on nation economy
        if (plugin.getNationManager() != null) {
            Nation n = plugin.getNationManager().getNationById(c.ownerNationId);
            if (n != null) {
                // Simple value calculation based on treasury and GDP
                double baseValue = 10000.0;
                if (plugin.getEconomyService() != null) {
                    double gdp = plugin.getEconomyService().getGDP(c.ownerNationId);
                    baseValue += gdp * 0.01; // Value increases with GDP
                }
                baseValue += n.getTreasury() * 0.1; // Value increases with treasury
                
                // Random fluctuation (-5% to +5%)
                double fluctuation = 0.95 + (Math.random() * 0.1);
                c.value = baseValue * fluctuation;
                
                save(c);
            }
        }
    }
    
    /**
     * Get all corporations (global market).
     */
    public synchronized List<Corporation> getAllCorporations() {
        return new ArrayList<>(corporations.values());
    }
    
    /**
     * Buy shares in a corporation.
     */
    public synchronized String buyShares(String buyerNationId, String corporationId, int shares, double pricePerShare) throws IOException {
        if (isBlank(buyerNationId) || isBlank(corporationId)) return "Неверные параметры.";
        if (shares <= 0) return "Некорректное количество.";
        if (!Double.isFinite(pricePerShare) || pricePerShare < 0) return "Некорректная цена.";
        Corporation c = corporations.get(corporationId);
        if (c == null) return "Корпорация не найдена.";
        if (!c.isPublic) return "Корпорация не является публичной. Проведите IPO.";
        if (c.shares < shares) return "Недостаточно акций для продажи.";
        
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        Nation buyer = plugin.getNationManager().getNationById(buyerNationId);
        if (buyer == null) return "Нация не найдена.";
        
        double totalCost = shares * pricePerShare;
        if (buyer.getTreasury() < totalCost) return "Недостаточно средств.";
        
        buyer.setTreasury(buyer.getTreasury() - totalCost);
        c.shares -= shares;
        c.shareholders.put(buyerNationId, c.shareholders.getOrDefault(buyerNationId, 0) + shares);
        c.value += totalCost * 0.5; // Value increases with investment
        
        plugin.getNationManager().save(buyer);
        save(c);
        
        return "Куплено " + shares + " акций корпорации '" + c.name + "' за " + totalCost;
    }
    
    /**
     * Conduct IPO (Initial Public Offering) - make corporation publicly tradable.
     */
    public synchronized String conductIPO(String nationId, String corporationId, int sharesToOffer, double pricePerShare) throws IOException {
        if (isBlank(nationId) || isBlank(corporationId)) return "Неверные параметры.";
        if (sharesToOffer <= 0) return "Некорректное количество.";
        if (!Double.isFinite(pricePerShare) || pricePerShare < 0) return "Некорректная цена.";
        Corporation c = corporations.get(corporationId);
        if (c == null) return "Корпорация не найдена.";
        if (!c.ownerNationId.equals(nationId)) return "Только владелец может провести IPO.";
        if (c.isPublic) return "Корпорация уже публичная.";
        if (c.shares < sharesToOffer) return "Недостаточно акций для IPO.";
        
        c.isPublic = true;
        c.shares -= sharesToOffer; // These shares are now available on market
        c.value += sharesToOffer * pricePerShare * 0.3; // IPO increases value
        
        save(c);
        
        // Broadcast IPO announcement
        org.bukkit.Bukkit.broadcastMessage("§6§l📈 IPO! §fКорпорация '" + c.name + "' выходит на биржу!");
        org.bukkit.Bukkit.broadcastMessage("§7Акций в продаже: §f" + sharesToOffer + " §7по цене §f" + pricePerShare + " §7за акцию");
        
        // Visual effect for all online players
        if (plugin.getVisualEffectsService() != null) {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                plugin.getVisualEffectsService().sendActionBar(p, "§6📈 IPO: " + c.name + " §7" + sharesToOffer + " акций × " + pricePerShare);
            }
        }
        
        return "IPO проведено! " + sharesToOffer + " акций доступны по " + pricePerShare;
    }
    
    /**
     * Pay dividends to all shareholders.
     */
    public synchronized String payDividends(String nationId, String corporationId, double dividendAmount) throws IOException {
        if (isBlank(nationId) || isBlank(corporationId)) return "Неверные параметры.";
        Corporation c = corporations.get(corporationId);
        if (c == null) return "Корпорация не найдена.";
        if (!c.ownerNationId.equals(nationId)) return "Только владелец может платить дивиденды.";
        if (!Double.isFinite(dividendAmount) || dividendAmount <= 0) return "Неверная сумма дивидендов.";
        
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        Nation owner = plugin.getNationManager().getNationById(nationId);
        if (owner == null) return "Нация не найдена.";
        if (owner.getTreasury() < dividendAmount) return "Недостаточно средств для дивидендов.";
        
        int totalShareholders = c.shareholders.values().stream().mapToInt(Integer::intValue).sum();
        if (totalShareholders == 0) return "Нет акционеров.";
        
        // Pay dividends proportional to ownership
        owner.setTreasury(owner.getTreasury() - dividendAmount);
        for (Map.Entry<String, Integer> entry : c.shareholders.entrySet()) {
            String shareholderNationId = entry.getKey();
            int sharesOwned = entry.getValue();
            double dividendShare = (dividendAmount * sharesOwned) / totalShareholders;
            
            Nation shareholder = plugin.getNationManager().getNationById(shareholderNationId);
            if (shareholder != null) {
                shareholder.setTreasury(shareholder.getTreasury() + dividendShare);
                plugin.getNationManager().save(shareholder);
            }
        }
        
        c.lastDividend = dividendAmount;
        plugin.getNationManager().save(owner);
        save(c);
        
        return "Дивиденды выплачены всем акционерам.";
    }
    
    /**
     * Sell shares back to corporation or on market.
     */
    public synchronized String sellShares(String sellerNationId, String corporationId, int sharesToSell, double pricePerShare) throws IOException {
        if (isBlank(sellerNationId) || isBlank(corporationId)) return "Неверные параметры.";
        if (sharesToSell <= 0) return "Некорректное количество.";
        if (!Double.isFinite(pricePerShare) || pricePerShare < 0) return "Некорректная цена.";
        Corporation c = corporations.get(corporationId);
        if (c == null) return "Корпорация не найдена.";
        
        int sharesOwned = c.shareholders.getOrDefault(sellerNationId, 0);
        if (sharesOwned < sharesToSell) return "У вас недостаточно акций.";
        
        double totalValue = sharesToSell * pricePerShare;
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        Nation seller = plugin.getNationManager().getNationById(sellerNationId);
        if (seller == null) return "Нация не найдена.";
        
        // Update shareholders
        c.shareholders.put(sellerNationId, sharesOwned - sharesToSell);
        c.shares += sharesToSell; // Shares return to market
        seller.setTreasury(seller.getTreasury() + totalValue);
        
        plugin.getNationManager().save(seller);
        save(c);
        
        return "Продано " + sharesToSell + " акций корпорации '" + c.name + "' за " + totalValue;
    }
    
    /**
     * Get top corporations by value.
     */
    public void openStockMarket(org.bukkit.entity.Player player) {
        // Placeholder
        player.sendMessage("Stock market opening...");
    }

    public synchronized List<Corporation> getTopCorporationsByValue(int limit) {
        if (limit <= 0) return Collections.emptyList();
        return corporations.values().stream()
            .filter(Objects::nonNull)
            .sorted((a, b) -> Double.compare(b.value, a.value))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate market index (average corporation value).
     */
    public synchronized double calculateMarketIndex() {
        if (corporations.isEmpty()) return 0.0;
        return corporations.values().stream()
            .filter(Objects::nonNull)
            .mapToDouble(c -> c.value)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Get corporation by ID.
     */
    public synchronized Corporation getCorporation(String corporationId) {
        if (isBlank(corporationId)) return null;
        return corporations.get(corporationId);
    }
    
    /**
     * Get global stock market statistics.
     */
    public synchronized Map<String, Object> getGlobalStockMarketStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalCorporations", corporations.size());
        
        double totalMarketValue = corporations.values().stream()
            .filter(Objects::nonNull)
            .mapToDouble(c -> c.value)
            .sum();
        double maxCorporationValue = corporations.values().stream()
            .filter(Objects::nonNull)
            .mapToDouble(c -> c.value)
            .max().orElse(0);
        long totalShares = corporations.values().stream()
            .filter(Objects::nonNull)
            .mapToLong(c -> c.shares)
            .sum();
        
        stats.put("totalMarketValue", totalMarketValue);
        stats.put("maxCorporationValue", maxCorporationValue);
        stats.put("averageCorporationValue", corporations.size() > 0 ? totalMarketValue / corporations.size() : 0);
        stats.put("totalShares", totalShares);
        stats.put("marketIndex", calculateMarketIndex());
        
        // Corporations by type
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Double> valueByType = new HashMap<>();
        for (Corporation c : corporations.values()) {
            if (c == null || c.type == null) continue;
            byType.put(c.type, byType.getOrDefault(c.type, 0) + 1);
            valueByType.put(c.type, valueByType.getOrDefault(c.type, 0.0) + c.value);
        }
        stats.put("corporationsByType", byType);
        stats.put("valueByType", valueByType);
        
        // Top corporations
        List<Corporation> topCorps = getTopCorporationsByValue(10);
        List<Map<String, Object>> topCorpsData = new ArrayList<>();
        for (Corporation c : topCorps) {
            Map<String, Object> corpData = new HashMap<>();
            corpData.put("id", c.id);
            corpData.put("name", c.name);
            corpData.put("type", c.type);
            corpData.put("value", c.value);
            corpData.put("shares", c.shares);
            corpData.put("ownerNationId", c.ownerNationId);
            topCorpsData.add(corpData);
        }
        stats.put("topCorporations", topCorpsData);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

