package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manages black market trading (illegal resources, weapons). */
public class BlackMarketService {
    private final AXIOM plugin;
    private final File blackmarketDir;
    private final Map<String, Map<String, Double>> blackMarketPrices = new HashMap<>(); // item -> price

    public BlackMarketService(AXIOM plugin) {
        this.plugin = plugin;
        this.blackmarketDir = new File(plugin.getDataFolder(), "blackmarket");
        this.blackmarketDir.mkdirs();
        loadAll();
    }

    public synchronized String buyFromBlackMarket(UUID buyerId, String item, int quantity, double pricePerUnit) {
        double totalCost = pricePerUnit * quantity;
        if (quantity <= 0 || pricePerUnit <= 0) return "Неверные параметры покупки.";
        WalletService walletService = plugin.getWalletService();
        if (walletService == null) return "Сервис кошелька недоступен.";
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        ResourceService resourceService = plugin.getResourceService();
        if (playerDataManager == null) return "Сервис игроков недоступен.";
        if (resourceService == null) return "Сервис ресурсов недоступен.";
        if (walletService.getBalance(buyerId) < totalCost) return "Недостаточно средств.";
        if (!walletService.withdraw(buyerId, totalCost)) return "Не удалось списать средства.";
        // Add resources to buyer's nation
        String nationId = playerDataManager != null ? playerDataManager.getNation(buyerId) : null;
        if (nationId != null) {
            resourceService.addResource(nationId, item, quantity);
        }
        updatePrices(item, pricePerUnit);
        return "Куплено на чёрном рынке: " + quantity + " " + item + " за " + totalCost;
    }

    public synchronized double getBlackMarketPrice(String item) {
        Map<String, Double> prices = blackMarketPrices.get("prices");
        if (prices == null) {
            prices = new HashMap<>();
            blackMarketPrices.put("prices", prices);
        }
        return prices.getOrDefault(item, 1000.0); // default price
    }

    private void updatePrices(String item, double transactionPrice) {
        Map<String, Double> prices = blackMarketPrices.computeIfAbsent("prices", k -> new HashMap<>());
        double current = prices.getOrDefault(item, 1000.0);
        // Price volatility: ±10% based on demand
        double newPrice = current * (0.9 + Math.random() * 0.2);
        prices.put(item, newPrice);
        savePrices();
    }

    private void loadAll() {
        File f = new File(blackmarketDir, "prices.json");
        if (!f.exists()) return;
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            Map<String, Double> prices = new HashMap<>();
            for (var entry : o.entrySet()) {
                prices.put(entry.getKey(), entry.getValue().getAsDouble());
            }
            blackMarketPrices.put("prices", prices);
        } catch (Exception ignored) {}
    }

    private void savePrices() {
        File f = new File(blackmarketDir, "prices.json");
        JsonObject o = new JsonObject();
        Map<String, Double> prices = blackMarketPrices.get("prices");
        if (prices != null) {
            for (var entry : prices.entrySet()) {
                o.addProperty(entry.getKey(), entry.getValue());
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive black market statistics.
     */
    public synchronized Map<String, Object> getBlackMarketStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Double> prices = blackMarketPrices.get("prices");
        if (prices == null) prices = new HashMap<>();
        
        stats.put("totalItems", prices.size());
        
        // Price range
        if (!prices.isEmpty()) {
            double minPrice = prices.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double maxPrice = prices.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double avgPrice = prices.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            
            stats.put("minPrice", minPrice);
            stats.put("maxPrice", maxPrice);
            stats.put("averagePrice", avgPrice);
        }
        
        // Top items by price
        List<Map.Entry<String, Double>> sortedPrices = new ArrayList<>(prices.entrySet());
        sortedPrices.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        List<Map<String, Object>> topItems = new ArrayList<>();
        for (int i = 0; i < Math.min(10, sortedPrices.size()); i++) {
            Map.Entry<String, Double> entry = sortedPrices.get(i);
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("item", entry.getKey());
            itemData.put("price", entry.getValue());
            topItems.add(itemData);
        }
        stats.put("topItems", topItems);
        stats.put("prices", new HashMap<>(prices));
        
        return stats;
    }
    
    /**
     * Set black market price manually.
     */
    public synchronized String setPrice(String item, double price) throws IOException {
        Map<String, Double> prices = blackMarketPrices.computeIfAbsent("prices", k -> new HashMap<>());
        prices.put(item, price);
        savePrices();
        return "Цена на чёрном рынке установлена: " + item + " = " + price;
    }
    
    /**
     * Get all available items.
     */
    public synchronized List<String> getAvailableItems() {
        Map<String, Double> prices = blackMarketPrices.get("prices");
        return prices != null ? new ArrayList<>(prices.keySet()) : new ArrayList<>();
    }
    
    /**
     * Calculate average price volatility.
     */
    public synchronized double getPriceVolatility(String item) {
        Map<String, Double> prices = blackMarketPrices.get("prices");
        if (prices == null || !prices.containsKey(item)) return 0.0;
        // Simple volatility estimate (10% by default for black market)
        return 0.10;
    }
    
    /**
     * Get global black market statistics (alias for getBlackMarketStatistics for consistency).
     */
    public synchronized Map<String, Object> getGlobalBlackMarketStatistics() {
        return getBlackMarketStatistics();
    }
}

