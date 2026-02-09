package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.axiom.domain.service.state.NationManager;

/** Manages commodity market prices and trading. */
public class CommodityMarketService {
    private final AXIOM plugin;
    private final File marketDir;
    private final Map<String, Double> commodityPrices = new HashMap<>(); // commodity -> price
    private final ResourceCatalogService catalogService;

    public CommodityMarketService(AXIOM plugin) {
        this.plugin = plugin;
        this.catalogService = plugin.getResourceCatalogService();
        this.marketDir = new File(plugin.getDataFolder(), "commoditymarket");
        this.marketDir.mkdirs();
        loadPrices();
        syncPrices();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updatePrices, 0, 20 * 60 * 5); // every 5 minutes
    }

    private void updatePrices() {
        // Prices fluctuate based on supply/demand
        for (String commodity : getTradableResources()) {
            double basePrice = commodityPrices.getOrDefault(commodity, getBasePrice(commodity));
            double volatility = catalogService != null ? catalogService.getVolatility(commodity) : 0.1;
            double fluctuation = (Math.random() - 0.5) * (volatility * 2.0);
            commodityPrices.put(commodity, basePrice * (1 + fluctuation));
        }
        savePrices();
    }

    public synchronized double getPrice(String commodity) {
        return commodityPrices.getOrDefault(commodity, getBasePrice(commodity));
    }

    public synchronized boolean hasPrice(String commodity) {
        return commodity != null && commodityPrices.containsKey(commodity);
    }

    public synchronized String buyCommodity(String nationId, String commodity, double quantity) {
        if (quantity <= 0) return "Неверное количество.";
        double price = getPrice(commodity) * quantity;
        NationManager nationManager = plugin.getNationManager();
        if (nationManager == null) return "Сервис наций недоступен.";
        ResourceService resourceService = plugin.getResourceService();
        if (resourceService == null) return "Сервис ресурсов недоступен.";
        com.axiom.domain.model.Nation n = nationManager.getNationById(nationId);
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
        com.axiom.domain.model.Nation n = nationManager.getNationById(nationId);
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
            // Initialize default prices from catalog
            for (String commodity : getTradableResources()) {
                commodityPrices.put(commodity, getBasePrice(commodity));
            }
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

    private void syncPrices() {
        for (String commodity : getTradableResources()) {
            commodityPrices.putIfAbsent(commodity, getBasePrice(commodity));
        }
    }

    private Set<String> getTradableResources() {
        if (catalogService != null) {
            Set<String> resources = catalogService.getTradableResources();
            if (!resources.isEmpty()) {
                return resources;
            }
        }
        return new java.util.HashSet<>(List.of("food", "wood", "iron", "gold", "coal", "oil"));
    }

    private double getBasePrice(String commodity) {
        return catalogService != null ? catalogService.getBasePrice(commodity) : 10.0;
    }
}

