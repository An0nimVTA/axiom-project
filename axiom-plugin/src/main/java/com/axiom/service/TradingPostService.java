package com.axiom.service;

import com.axiom.AXIOM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages trading posts where players can buy/sell resources. */
public class TradingPostService {
    private final AXIOM plugin;
    private final File postsDir;
    private final Map<String, TradingPost> posts = new HashMap<>(); // postId -> post

    public static class TradingPost {
        String id;
        String nationId;
        String cityId;
        String chunkKey; // world:x:z
        Map<String, TradeOffer> offers = new HashMap<>(); // resource -> offer
    }

    public static class TradeOffer {
        String resourceName;
        double buyPrice;
        double sellPrice;
        int quantity;
    }

    public TradingPostService(AXIOM plugin) {
        this.plugin = plugin;
        this.postsDir = new File(plugin.getDataFolder(), "tradingposts");
        this.postsDir.mkdirs();
        loadAll();
    }

    public synchronized String createPost(String nationId, String cityId, String chunkKey) throws IOException {
        if (isBlank(nationId) || isBlank(cityId) || isBlank(chunkKey)) return "Некорректные данные.";
        String id = cityId + "_" + chunkKey.replace(":", "_");
        if (posts.containsKey(id)) return "Торговый пост уже существует.";
        TradingPost post = new TradingPost();
        post.id = id;
        post.nationId = nationId;
        post.cityId = cityId;
        post.chunkKey = chunkKey;
        posts.put(id, post);
        savePost(post);
        return "Торговый пост создан.";
    }

    public void openTradingMenu(org.bukkit.entity.Player player) {
        // Placeholder for trading menu
        player.sendMessage("Trading menu opening...");
    }

    public synchronized String addOffer(String postId, String resourceName, double buyPrice, double sellPrice, int quantity) throws IOException {
        if (isBlank(resourceName) || buyPrice < 0 || sellPrice < 0 || quantity <= 0) {
            return "Некорректное предложение.";
        }
        TradingPost post = posts.get(postId);
        if (post == null) return "Пост не найден.";
        TradeOffer offer = new TradeOffer();
        offer.resourceName = resourceName;
        offer.buyPrice = buyPrice;
        offer.sellPrice = sellPrice;
        offer.quantity = quantity;
        post.offers.put(resourceName, offer);
        savePost(post);
        return "Предложение добавлено: " + resourceName;
    }

    public synchronized String buyResource(String postId, String resourceName, int amount, UUID buyerId) throws IOException {
        if (buyerId == null) return "Покупатель не найден.";
        if (isBlank(resourceName) || amount <= 0) return "Некорректное количество.";
        TradingPost post = posts.get(postId);
        if (post == null) return "Пост не найден.";
        TradeOffer offer = post.offers.get(resourceName);
        if (offer == null) return "Ресурс не найден.";
        if (offer.buyPrice < 0) return "Некорректная цена.";
        if (offer.quantity < amount) return "Недостаточно товара.";
        double cost = offer.buyPrice * amount;
        if (plugin.getWalletService() == null || plugin.getResourceService() == null || plugin.getPlayerDataManager() == null) {
            return "Сервис недоступен.";
        }
        String buyerNationId = plugin.getPlayerDataManager().getNation(buyerId);
        if (buyerNationId == null) return "Вы не состоите в нации.";
        if (plugin.getWalletService().getBalance(buyerId) < cost) return "Недостаточно средств.";
        plugin.getWalletService().withdraw(buyerId, cost);
        plugin.getResourceService().addResource(buyerNationId, resourceName, amount);
        offer.quantity -= amount;
        if (offer.quantity <= 0) post.offers.remove(resourceName);
        savePost(post);
        return "Куплено " + amount + " " + resourceName + " за " + cost;
    }

    private void loadAll() {
        File[] files = postsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                TradingPost post = new TradingPost();
                post.id = o.get("id").getAsString();
                post.nationId = o.get("nationId").getAsString();
                post.cityId = o.get("cityId").getAsString();
                post.chunkKey = o.get("chunkKey").getAsString();
                if (o.has("offers")) {
                    JsonObject offersObj = o.getAsJsonObject("offers");
                    for (var entry : offersObj.entrySet()) {
                        JsonObject offerObj = entry.getValue().getAsJsonObject();
                        TradeOffer offer = new TradeOffer();
                        offer.resourceName = entry.getKey();
                        offer.buyPrice = offerObj.get("buyPrice").getAsDouble();
                        offer.sellPrice = offerObj.get("sellPrice").getAsDouble();
                        offer.quantity = offerObj.get("quantity").getAsInt();
                        post.offers.put(offer.resourceName, offer);
                    }
                }
                posts.put(post.id, post);
            } catch (Exception ignored) {}
        }
    }

    private void savePost(TradingPost post) throws IOException {
        File f = new File(postsDir, post.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", post.id);
        o.addProperty("nationId", post.nationId);
        o.addProperty("cityId", post.cityId);
        o.addProperty("chunkKey", post.chunkKey);
        JsonObject offersObj = new JsonObject();
        for (var entry : post.offers.entrySet()) {
            JsonObject offerObj = new JsonObject();
            offerObj.addProperty("buyPrice", entry.getValue().buyPrice);
            offerObj.addProperty("sellPrice", entry.getValue().sellPrice);
            offerObj.addProperty("quantity", entry.getValue().quantity);
            offersObj.add(entry.getKey(), offerObj);
        }
        o.add("offers", offersObj);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }
    
    /**
     * Get comprehensive trading post statistics.
     */
    public synchronized Map<String, Object> getTradingPostStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        if (isBlank(nationId)) {
            stats.put("totalPosts", 0);
            stats.put("totalOffers", 0);
            stats.put("totalValue", 0.0);
            stats.put("byCity", new HashMap<String, Integer>());
            stats.put("posts", new ArrayList<Map<String, Object>>());
            return stats;
        }
        
        List<TradingPost> nationPosts = new ArrayList<>();
        int totalOffers = 0;
        double totalValue = 0.0;
        
        for (TradingPost post : posts.values()) {
            if (post.nationId.equals(nationId)) {
                nationPosts.add(post);
                totalOffers += post.offers.size();
                
                // Calculate total value of offers
                for (TradeOffer offer : post.offers.values()) {
                    totalValue += offer.buyPrice * offer.quantity;
                }
            }
        }
        
        stats.put("totalPosts", nationPosts.size());
        stats.put("totalOffers", totalOffers);
        stats.put("totalValue", totalValue);
        
        // Posts by city
        Map<String, Integer> byCity = new HashMap<>();
        for (TradingPost post : nationPosts) {
            byCity.put(post.cityId, byCity.getOrDefault(post.cityId, 0) + 1);
        }
        stats.put("byCity", byCity);
        
        // Post details
        List<Map<String, Object>> postsList = new ArrayList<>();
        for (TradingPost post : nationPosts) {
            Map<String, Object> postData = new HashMap<>();
            postData.put("id", post.id);
            postData.put("cityId", post.cityId);
            postData.put("chunkKey", post.chunkKey);
            postData.put("offersCount", post.offers.size());
            postsList.add(postData);
        }
        stats.put("posts", postsList);
        
        return stats;
    }
    
    /**
     * Remove trading post.
     */
    public synchronized String removePost(String postId) throws IOException {
        TradingPost post = posts.remove(postId);
        if (post == null) return "Пост не найден.";
        
        // Delete file
        File f = new File(postsDir, postId + ".json");
        if (f.exists()) f.delete();
        
        return "Торговый пост удалён.";
    }
    
    /**
     * Update offer prices.
     */
    public synchronized String updateOffer(String postId, String resourceName, double newBuyPrice, double newSellPrice) throws IOException {
        TradingPost post = posts.get(postId);
        if (post == null) return "Пост не найден.";
        if (newBuyPrice < 0 || newSellPrice < 0) return "Некорректная цена.";
        
        TradeOffer offer = post.offers.get(resourceName);
        if (offer == null) return "Предложение не найдено.";
        
        offer.buyPrice = newBuyPrice;
        offer.sellPrice = newSellPrice;
        
        savePost(post);
        
        return "Цены обновлены: покупка " + newBuyPrice + ", продажа " + newSellPrice;
    }
    
    /**
     * Sell resource to trading post.
     */
    public synchronized String sellResource(String postId, String resourceName, int amount, UUID sellerId) throws IOException {
        if (sellerId == null) return "Продавец не найден.";
        if (isBlank(resourceName) || amount <= 0) return "Некорректное количество.";
        TradingPost post = posts.get(postId);
        if (post == null) return "Пост не найден.";
        
        TradeOffer offer = post.offers.get(resourceName);
        if (offer == null) return "Ресурс не принимается.";
        if (offer.sellPrice < 0) return "Некорректная цена.";
        if (plugin.getResourceService() == null || plugin.getWalletService() == null || plugin.getPlayerDataManager() == null) {
            return "Сервис недоступен.";
        }
        
        String sellerNationId = plugin.getPlayerDataManager().getNation(sellerId);
        if (sellerNationId == null || plugin.getResourceService().getResource(sellerNationId, resourceName) < amount) {
            return "Недостаточно ресурсов.";
        }
        
        double payment = offer.sellPrice * amount;
        plugin.getResourceService().consumeResource(sellerNationId, resourceName, amount);
        plugin.getWalletService().deposit(sellerId, payment);
        
        offer.quantity += amount;
        savePost(post);
        
        return "Продано " + amount + " " + resourceName + " за " + payment;
    }
    
    /**
     * Get all trading posts in a city.
     */
    public synchronized List<TradingPost> getCityPosts(String cityId) {
        List<TradingPost> result = new ArrayList<>();
        if (isBlank(cityId)) return result;
        for (TradingPost post : posts.values()) {
            if (post.cityId.equals(cityId)) {
                result.add(post);
            }
        }
        return result;
    }
    
    /**
     * Get all available offers across all posts.
     */
    public synchronized List<Map<String, Object>> getAllAvailableOffers(String nationId) {
        List<Map<String, Object>> offers = new ArrayList<>();
        boolean filterOwn = !isBlank(nationId);
        
        for (TradingPost post : posts.values()) {
            if (filterOwn && nationId.equals(post.nationId)) continue; // Skip own posts
            
            for (TradeOffer offer : post.offers.values()) {
                if (offer.quantity > 0) {
                    Map<String, Object> offerData = new HashMap<>();
                    offerData.put("postId", post.id);
                    offerData.put("cityId", post.cityId);
                    offerData.put("resourceName", offer.resourceName);
                    offerData.put("buyPrice", offer.buyPrice);
                    offerData.put("sellPrice", offer.sellPrice);
                    offerData.put("quantity", offer.quantity);
                    offers.add(offerData);
                }
            }
        }
        
        return offers;
    }
    
    /**
     * Get global trading post statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalTradingPostStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPosts", posts.size());
        
        Map<String, Integer> postsByNation = new HashMap<>();
        Map<String, Integer> postsByCity = new HashMap<>();
        int totalOffers = 0;
        double totalValue = 0.0;
        Map<String, Integer> offersByResource = new HashMap<>();
        Map<String, Double> averageBuyPriceByResource = new HashMap<>();
        Map<String, Double> averageSellPriceByResource = new HashMap<>();
        
        for (TradingPost post : posts.values()) {
            String nationId = post.nationId != null ? post.nationId : "unknown";
            String cityId = post.cityId != null ? post.cityId : "unknown";
            postsByNation.put(nationId, postsByNation.getOrDefault(nationId, 0) + 1);
            postsByCity.put(cityId, postsByCity.getOrDefault(cityId, 0) + 1);
            totalOffers += post.offers.size();
            
            for (TradeOffer offer : post.offers.values()) {
                totalValue += offer.buyPrice * offer.quantity;
                offersByResource.put(offer.resourceName, 
                    offersByResource.getOrDefault(offer.resourceName, 0) + 1);
                
                // Track average prices
                int count = offersByResource.get(offer.resourceName);
                averageBuyPriceByResource.put(offer.resourceName,
                    (averageBuyPriceByResource.getOrDefault(offer.resourceName, 0.0) * (count - 1) + offer.buyPrice) / count);
                averageSellPriceByResource.put(offer.resourceName,
                    (averageSellPriceByResource.getOrDefault(offer.resourceName, 0.0) * (count - 1) + offer.sellPrice) / count);
            }
        }
        
        stats.put("postsByNation", postsByNation);
        stats.put("postsByCity", postsByCity);
        stats.put("totalOffers", totalOffers);
        stats.put("totalValue", totalValue);
        stats.put("offersByResource", offersByResource);
        stats.put("averageBuyPriceByResource", averageBuyPriceByResource);
        stats.put("averageSellPriceByResource", averageSellPriceByResource);
        stats.put("nationsWithPosts", postsByNation.size());
        stats.put("citiesWithPosts", postsByCity.size());
        
        // Average posts per nation
        stats.put("averagePostsPerNation", postsByNation.size() > 0 ? 
            (double) posts.size() / postsByNation.size() : 0);
        
        // Average offers per post
        stats.put("averageOffersPerPost", posts.size() > 0 ? 
            (double) totalOffers / posts.size() : 0);
        
        // Top nations by posts
        List<Map.Entry<String, Integer>> topByPosts = postsByNation.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByPosts", topByPosts);
        
        // Top cities by posts
        List<Map.Entry<String, Integer>> topByCity = postsByCity.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByCity", topByCity);
        
        // Most traded resources
        List<Map.Entry<String, Integer>> topByResources = offersByResource.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByResources", topByResources);
        
        // Average value per post
        stats.put("averageValuePerPost", posts.size() > 0 ? totalValue / posts.size() : 0);
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

