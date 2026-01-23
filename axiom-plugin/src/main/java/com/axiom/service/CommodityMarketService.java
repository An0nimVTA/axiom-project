package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Manages commodity market prices and trading. */
public class CommodityMarketService {
    private final AXIOM plugin;
    private final File marketDir;
    private final Map<String, Double> commodityPrices = new HashMap<>(); // commodity -> price

    public CommodityMarketService(AXIOM plugin) {
        this.plugin = plugin;
        this.marketDir = new File(plugin.getDataFolder(), "commoditymarket");
        this.marketDir.mkdirs();
        loadPrices();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updatePrices, 0, 20 * 60 * 5); // every 5 minutes
    }

    private void updatePrices() {
        // Prices fluctuate based on supply/demand
        for (String commodity : new String[]{"food", "wood", "iron", "gold", "coal", "oil"}) {
            double basePrice = commodityPrices.getOrDefault(commodity, 10.0);
            double fluctuation = (Math.random() - 0.5) * 0.2; // ±10% fluctuation
            commodityPrices.put(commodity, basePrice * (1 + fluctuation));
        }
        savePrices();
    }

    public synchronized double getPrice(String commodity) {
        return commodityPrices.getOrDefault(commodity, 10.0);
    }

    public synchronized String buyCommodity(String nationId, String commodity, double quantity) {
        if (quantity <= 0) return "Неверное количество.";
        double price = getPrice(commodity) * quantity;
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) return "Сервис наций недоступен.";
        ResourceService resourceService = plugin.getResourceService();
        if (resourceService == null) return "Сервис ресурсов недоступен.";
        com.axiom.model.Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (n.getTreasury() < price) return "Недостаточно средств.";
        n.setTreasury(n.getTreasury() - price);
        resourceService.addResource(nationId, commodity, quantity);
        try {
            nationManager.save(n);
        } catch (Exception ignored) {}
        return "Куплено: " + quantity + " " + commodity + " за " + price;
    }

    public synchronized String sellCommodity(String nationId, String commodity, double quantity) {
        if (quantity <= 0) return "Неверное количество.";
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) return "Сервис наций недоступен.";
        ResourceService resourceService = plugin.getResourceService();
        if (resourceService == null) return "Сервис ресурсов недоступен.";
        if (!resourceService.consumeResource(nationId, commodity, quantity)) {
            return "Недостаточно ресурсов.";
        }
        double price = getPrice(commodity) * quantity;
        com.axiom.model.Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.setTreasury(n.getTreasury() + price);
            try {
                nationManager.save(n);
            } catch (Exception ignored) {}
        }
        return "Продано: " + quantity + " " + commodity + " за " + price;
    }

    private void loadPrices() {
        File f = new File(marketDir, "prices.json");
        if (!f.exists()) {
            // Initialize default prices
            commodityPrices.put("food", 10.0);
            commodityPrices.put("wood", 5.0);
            commodityPrices.put("iron", 15.0);
            commodityPrices.put("gold", 50.0);
            commodityPrices.put("coal", 8.0);
            commodityPrices.put("oil", 20.0);
            return;
        }
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            for (var entry : o.entrySet()) {
                commodityPrices.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        } catch (Exception ignored) {}
    }

    private void savePrices() {
        File f = new File(marketDir, "prices.json");
        JsonObject o = new JsonObject();
        for (var entry : commodityPrices.entrySet()) {
            o.addProperty(entry.getKey(), entry.getValue());
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive commodity market statistics.
     */
    public synchronized Map<String, Object> getCommodityMarketStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalCommodities", commodityPrices.size());
        stats.put("commodityPrices", new HashMap<>(commodityPrices));
        
        // Average price
        double totalPrice = commodityPrices.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("averagePrice", commodityPrices.size() > 0 ? totalPrice / commodityPrices.size() : 0);
        
        // Price distribution
        int veryHigh = 0, high = 0, medium = 0, low = 0;
        for (Double price : commodityPrices.values()) {
            if (price >= 40) veryHigh++;
            else if (price >= 15) high++;
            else if (price >= 8) medium++;
            else low++;
        }
        
        Map<String, Integer> priceDistribution = new HashMap<>();
        priceDistribution.put("veryHigh", veryHigh);
        priceDistribution.put("high", high);
        priceDistribution.put("medium", medium);
        priceDistribution.put("low", low);
        stats.put("priceDistribution", priceDistribution);
        
        // Most/least expensive commodities
        List<Map.Entry<String, Double>> sortedByPrice = commodityPrices.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .collect(java.util.stream.Collectors.toList());
        
        if (!sortedByPrice.isEmpty()) {
            stats.put("mostExpensive", sortedByPrice.get(0));
            stats.put("leastExpensive", sortedByPrice.get(sortedByPrice.size() - 1));
            stats.put("top5MostExpensive", sortedByPrice.stream().limit(5).collect(java.util.stream.Collectors.toList()));
        }
        
        return stats;
    }
    
    /**
     * Get global commodity market statistics (alias for consistency).
     */
    public synchronized Map<String, Object> getGlobalCommodityMarketStatistics() {
        return getCommodityMarketStatistics();
    }
}

