package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages arms deals and weapons trading between nations. */
public class ArmsDealService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File dealsDir;
    private final Map<String, ArmsDeal> activeDeals = new HashMap<>(); // dealId -> deal

    public static class ArmsDeal {
        String id;
        String sellerNationId;
        String buyerNationId;
        String weaponType;
        int quantity;
        double pricePerUnit;
        long deliveryDate;
        boolean delivered;
    }

    public ArmsDealService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.dealsDir = new File(plugin.getDataFolder(), "armsdeals");
        this.dealsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processDeliveries, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String createDeal(String sellerId, String buyerId, String weaponType, int quantity, double pricePerUnit, int deliveryDays) {
        Nation seller = nationManager.getNationById(sellerId);
        Nation buyer = nationManager.getNationById(buyerId);
        if (seller == null || buyer == null) return "Нация не найдена.";
        if (sellerId.equals(buyerId)) return "Нельзя заключить сделку с самим собой.";
        if (quantity <= 0 || pricePerUnit <= 0) return "Неверные параметры сделки.";
        if (deliveryDays < 0) return "Неверный срок доставки.";
        double totalCost = quantity * pricePerUnit;
        if (buyer.getTreasury() < totalCost) return "Недостаточно средств.";
        String dealId = UUID.randomUUID().toString().substring(0, 8);
        ArmsDeal deal = new ArmsDeal();
        deal.id = dealId;
        deal.sellerNationId = sellerId;
        deal.buyerNationId = buyerId;
        deal.weaponType = weaponType;
        deal.quantity = quantity;
        deal.pricePerUnit = pricePerUnit;
        deal.deliveryDate = System.currentTimeMillis() + deliveryDays * 24 * 60 * 60_000L;
        deal.delivered = false;
        activeDeals.put(dealId, deal);
        buyer.setTreasury(buyer.getTreasury() - totalCost);
        seller.setTreasury(seller.getTreasury() + totalCost);
        try {
            nationManager.save(seller);
            nationManager.save(buyer);
            saveDeal(deal);
        } catch (Exception ignored) {}
        return "Оружейная сделка заключена (ID: " + dealId + "). Доставка через " + deliveryDays + " дней.";
    }

    private synchronized void processDeliveries() {
        long now = System.currentTimeMillis();
        MilitaryService militaryService = plugin.getMilitaryService();
        for (ArmsDeal deal : activeDeals.values()) {
            if (deal.delivered || now < deal.deliveryDate) continue;
            // Deliver weapons
            if (militaryService == null) continue;
            militaryService.recruitUnits(deal.buyerNationId, deal.weaponType, deal.quantity, 0);
            deal.delivered = true;
            Nation buyer = nationManager.getNationById(deal.buyerNationId);
            if (buyer != null) {
                buyer.getHistory().add("Получено оружие: " + deal.quantity + " " + deal.weaponType);
                try { nationManager.save(buyer); } catch (Exception ignored) {}
            }
            saveDeal(deal);
        }
    }

    private void loadAll() {
        File[] files = dealsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                ArmsDeal deal = new ArmsDeal();
                deal.id = o.get("id").getAsString();
                deal.sellerNationId = o.get("sellerNationId").getAsString();
                deal.buyerNationId = o.get("buyerNationId").getAsString();
                deal.weaponType = o.get("weaponType").getAsString();
                deal.quantity = o.get("quantity").getAsInt();
                deal.pricePerUnit = o.get("pricePerUnit").getAsDouble();
                deal.deliveryDate = o.get("deliveryDate").getAsLong();
                deal.delivered = o.has("delivered") && o.get("delivered").getAsBoolean();
                if (!deal.delivered) activeDeals.put(deal.id, deal);
            } catch (Exception ignored) {}
        }
    }

    private void saveDeal(ArmsDeal deal) {
        File f = new File(dealsDir, deal.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", deal.id);
        o.addProperty("sellerNationId", deal.sellerNationId);
        o.addProperty("buyerNationId", deal.buyerNationId);
        o.addProperty("weaponType", deal.weaponType);
        o.addProperty("quantity", deal.quantity);
        o.addProperty("pricePerUnit", deal.pricePerUnit);
        o.addProperty("deliveryDate", deal.deliveryDate);
        o.addProperty("delivered", deal.delivered);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive arms deal statistics for a nation.
     */
    public synchronized Map<String, Object> getArmsDealStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        int dealsAsSeller = 0;
        int dealsAsBuyer = 0;
        int deliveredDeals = 0;
        int pendingDeals = 0;
        double totalRevenue = 0.0;
        double totalSpent = 0.0;
        Map<String, Integer> weaponsSold = new HashMap<>();
        Map<String, Integer> weaponsBought = new HashMap<>();
        
        long now = System.currentTimeMillis();
        for (ArmsDeal deal : activeDeals.values()) {
            if (deal.sellerNationId.equals(nationId)) {
                dealsAsSeller++;
                if (deal.delivered) deliveredDeals++;
                else pendingDeals++;
                totalRevenue += deal.quantity * deal.pricePerUnit;
                weaponsSold.put(deal.weaponType,
                    weaponsSold.getOrDefault(deal.weaponType, 0) + deal.quantity);
            }
            if (deal.buyerNationId.equals(nationId)) {
                dealsAsBuyer++;
                if (deal.delivered) deliveredDeals++;
                else if (now >= deal.deliveryDate) pendingDeals++;
                totalSpent += deal.quantity * deal.pricePerUnit;
                weaponsBought.put(deal.weaponType,
                    weaponsBought.getOrDefault(deal.weaponType, 0) + deal.quantity);
            }
        }
        
        stats.put("dealsAsSeller", dealsAsSeller);
        stats.put("dealsAsBuyer", dealsAsBuyer);
        stats.put("totalDeals", dealsAsSeller + dealsAsBuyer);
        stats.put("deliveredDeals", deliveredDeals);
        stats.put("pendingDeals", pendingDeals);
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalSpent", totalSpent);
        stats.put("netProfit", totalRevenue - totalSpent);
        stats.put("weaponsSold", weaponsSold);
        stats.put("weaponsBought", weaponsBought);
        
        // Deal rating
        String rating = "НЕТ СДЕЛОК";
        int total = dealsAsSeller + dealsAsBuyer;
        if (total >= 20) rating = "КРУПНЫЙ ТОРГОВЕЦ";
        else if (total >= 10) rating = "АКТИВНЫЙ";
        else if (total >= 5) rating = "УМЕРЕННЫЙ";
        else if (total >= 1) rating = "НАЧАЛЬНЫЙ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Get global arms deal statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalArmsDealStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long now = System.currentTimeMillis();
        int totalActiveDeals = 0;
        int deliveredDeals = 0;
        int pendingDeals = 0;
        double totalValue = 0.0;
        Map<String, Integer> dealsAsSeller = new HashMap<>();
        Map<String, Integer> dealsAsBuyer = new HashMap<>();
        Map<String, Integer> dealsByWeapon = new HashMap<>();
        Map<String, Double> revenueByNation = new HashMap<>();
        Map<String, Double> spendingByNation = new HashMap<>();
        
        for (ArmsDeal deal : activeDeals.values()) {
            totalActiveDeals++;
            double value = deal.quantity * deal.pricePerUnit;
            totalValue += value;
            
            if (deal.delivered) deliveredDeals++;
            else if (now >= deal.deliveryDate) pendingDeals++;
            
            dealsAsSeller.put(deal.sellerNationId,
                dealsAsSeller.getOrDefault(deal.sellerNationId, 0) + 1);
            dealsAsBuyer.put(deal.buyerNationId,
                dealsAsBuyer.getOrDefault(deal.buyerNationId, 0) + 1);
            dealsByWeapon.put(deal.weaponType,
                dealsByWeapon.getOrDefault(deal.weaponType, 0) + 1);
            
            revenueByNation.put(deal.sellerNationId,
                revenueByNation.getOrDefault(deal.sellerNationId, 0.0) + value);
            spendingByNation.put(deal.buyerNationId,
                spendingByNation.getOrDefault(deal.buyerNationId, 0.0) + value);
        }
        
        stats.put("totalActiveDeals", totalActiveDeals);
        stats.put("deliveredDeals", deliveredDeals);
        stats.put("pendingDeals", pendingDeals);
        stats.put("totalValue", totalValue);
        stats.put("averageDealValue", totalActiveDeals > 0 ? totalValue / totalActiveDeals : 0);
        stats.put("dealsAsSeller", dealsAsSeller);
        stats.put("dealsAsBuyer", dealsAsBuyer);
        stats.put("dealsByWeapon", dealsByWeapon);
        stats.put("revenueByNation", revenueByNation);
        stats.put("spendingByNation", spendingByNation);
        stats.put("nationsAsSellers", dealsAsSeller.size());
        stats.put("nationsAsBuyers", dealsAsBuyer.size());
        
        // Top nations by revenue
        List<Map.Entry<String, Double>> topByRevenue = revenueByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByRevenue", topByRevenue);
        
        // Top nations by spending
        List<Map.Entry<String, Double>> topBySpending = spendingByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topBySpending", topBySpending);
        
        // Most traded weapon types
        List<Map.Entry<String, Integer>> topByWeapon = dealsByWeapon.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByWeapon", topByWeapon);
        
        return stats;
    }
}

