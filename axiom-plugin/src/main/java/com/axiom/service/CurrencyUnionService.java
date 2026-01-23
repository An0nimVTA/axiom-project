package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages currency unions between nations. */
public class CurrencyUnionService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File unionsDir;
    private final Map<String, CurrencyUnion> unions = new HashMap<>(); // unionId -> union

    public static class CurrencyUnion {
        String id;
        String name;
        Set<String> memberNations = new HashSet<>();
        String commonCurrency;
        double exchangeRate;
        long establishedAt;
        boolean active;
    }

    public CurrencyUnionService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.unionsDir = new File(plugin.getDataFolder(), "currencyunions");
        this.unionsDir.mkdirs();
        loadAll();
    }

    public synchronized String createUnion(String name, String initiatorId, String commonCurrency, double rate) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (name == null || name.trim().isEmpty()) return "Название союза не указано.";
        if (initiatorId == null) return "Неверные параметры.";
        if (commonCurrency == null || commonCurrency.trim().isEmpty()) return "Валюта не указана.";
        if (rate <= 0) return "Курс должен быть больше 0.";
        Nation initiator = nationManager.getNationById(initiatorId);
        if (initiator == null) return "Нация не найдена.";
        String unionId = UUID.randomUUID().toString().substring(0, 8);
        CurrencyUnion union = new CurrencyUnion();
        union.id = unionId;
        union.name = name;
        union.memberNations.add(initiatorId);
        union.commonCurrency = commonCurrency;
        union.exchangeRate = rate;
        union.establishedAt = System.currentTimeMillis();
        union.active = true;
        unions.put(unionId, union);
        initiator.setCurrencyCode(commonCurrency);
        initiator.setExchangeRateToAXC(rate);
        initiator.getHistory().add("Создан валютный союз: " + name);
        try {
            nationManager.save(initiator);
            saveUnion(union);
        } catch (Exception ignored) {}
        return "Валютный союз создан: " + name;
    }

    public synchronized String joinUnion(String unionId, String nationId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        CurrencyUnion union = unions.get(unionId);
        if (union == null) return "Союз не найден.";
        if (!union.active) return "Союз неактивен.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        union.memberNations.add(nationId);
        n.setCurrencyCode(union.commonCurrency);
        n.setExchangeRateToAXC(union.exchangeRate);
        n.getHistory().add("Присоединилась к валютному союзу: " + union.name);
        try {
            nationManager.save(n);
            saveUnion(union);
        } catch (Exception ignored) {}
        return "Присоединение к союзу подтверждено.";
    }

    private void loadAll() {
        File[] files = unionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                CurrencyUnion union = new CurrencyUnion();
                union.id = o.get("id").getAsString();
                union.name = o.get("name").getAsString();
                union.commonCurrency = o.get("commonCurrency").getAsString();
                union.exchangeRate = o.get("exchangeRate").getAsDouble();
                union.establishedAt = o.get("establishedAt").getAsLong();
                union.active = o.has("active") && o.get("active").getAsBoolean();
                if (o.has("memberNations")) {
                    for (var elem : o.getAsJsonArray("memberNations")) {
                        union.memberNations.add(elem.getAsString());
                    }
                }
                unions.put(union.id, union);
            } catch (Exception ignored) {}
        }
    }

    private void saveUnion(CurrencyUnion union) {
        File f = new File(unionsDir, union.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", union.id);
        o.addProperty("name", union.name);
        o.addProperty("commonCurrency", union.commonCurrency);
        o.addProperty("exchangeRate", union.exchangeRate);
        o.addProperty("establishedAt", union.establishedAt);
        o.addProperty("active", union.active);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String nationId : union.memberNations) {
            arr.add(nationId);
        }
        o.add("memberNations", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive currency union statistics.
     */
    public synchronized Map<String, Object> getCurrencyUnionStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Unions this nation is member of
        List<CurrencyUnion> memberUnions = new ArrayList<>();
        for (CurrencyUnion union : unions.values()) {
            if (union.memberNations.contains(nationId)) {
                memberUnions.add(union);
            }
        }
        
        stats.put("memberUnions", memberUnions.size());
        stats.put("unions", memberUnions);
        
        // Union details
        List<Map<String, Object>> unionsList = new ArrayList<>();
        for (CurrencyUnion union : memberUnions) {
            Map<String, Object> unionData = new HashMap<>();
            unionData.put("id", union.id);
            unionData.put("name", union.name);
            unionData.put("currency", union.commonCurrency);
            unionData.put("exchangeRate", union.exchangeRate);
            unionData.put("members", union.memberNations.size());
            unionData.put("establishedAt", union.establishedAt);
            unionData.put("age", (System.currentTimeMillis() - union.establishedAt) / 1000 / 60 / 60 / 24); // days
            unionsList.add(unionData);
        }
        stats.put("unionsList", unionsList);
        
        // Calculate economic benefits
        double tradeBonus = memberUnions.stream()
            .filter(u -> u.active)
            .mapToDouble(u -> 0.05) // +5% trade per union
            .sum();
        stats.put("tradeBonus", Math.min(0.25, tradeBonus)); // Cap at +25%
        
        // Union rating
        String rating = "НЕ В ВАЛЮТНЫХ СОЮЗАХ";
        if (memberUnions.size() >= 3) rating = "МНОЖЕСТВЕННОЕ ЧЛЕНСТВО";
        else if (memberUnions.size() >= 2) rating = "ДВОЙНОЕ ЧЛЕНСТВО";
        else if (memberUnions.size() >= 1) rating = "ЧЛЕН ВАЛЮТНОГО СОЮЗА";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Leave currency union.
     */
    public synchronized String leaveUnion(String unionId, String nationId) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        CurrencyUnion union = unions.get(unionId);
        if (union == null) return "Союз не найден.";
        if (!union.memberNations.contains(nationId)) return "Вы не являетесь членом этого союза.";
        
        union.memberNations.remove(nationId);
        
        // If no members left, disband union
        if (union.memberNations.isEmpty()) {
            union.active = false;
            unions.remove(unionId);
            File f = new File(unionsDir, unionId + ".json");
            if (f.exists()) f.delete();
        } else {
            saveUnion(union);
        }
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            n.setCurrencyCode("AXC"); // Reset to default
            n.setExchangeRateToAXC(1.0);
            n.getHistory().add("Покинула валютный союз: " + union.name);
            nationManager.save(n);
        }
        
        return "Вы покинули валютный союз: " + union.name;
    }
    
    /**
     * Update exchange rate (by union leader).
     */
    public synchronized String updateExchangeRate(String unionId, String leaderId, double newRate) throws IOException {
        CurrencyUnion union = unions.get(unionId);
        if (union == null) return "Союз не найден.";
        if (!union.memberNations.contains(leaderId)) return "Вы не являетесь членом союза.";
        if (newRate <= 0) return "Курс должен быть больше 0.";
        
        union.exchangeRate = newRate;
        
        // Update all members' exchange rates
        for (String memberId : union.memberNations) {
            Nation member = nationManager.getNationById(memberId);
            if (member != null) {
                member.setExchangeRateToAXC(newRate);
                nationManager.save(member);
            }
        }
        
        saveUnion(union);
        
        return "Курс обмена обновлён: " + newRate;
    }
    
    /**
     * Get union for a nation.
     */
    public synchronized CurrencyUnion getNationUnion(String nationId) {
        for (CurrencyUnion union : unions.values()) {
            if (union.active && union.memberNations.contains(nationId)) {
                return union;
            }
        }
        return null;
    }
    
    /**
     * Calculate trade bonus from currency unions.
     */
    public synchronized double getCurrencyUnionTradeBonus(String nationId) {
        int unionCount = 0;
        for (CurrencyUnion union : unions.values()) {
            if (union.active && union.memberNations.contains(nationId)) {
                unionCount++;
            }
        }
        // +5% trade bonus per union (capped)
        return 1.0 + Math.min(0.25, unionCount * 0.05); // Max +25%
    }
    
    /**
     * Get global currency union statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalCurrencyUnionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalUnions = unions.size();
        int activeUnions = 0;
        Map<String, Integer> membersByUnion = new HashMap<>();
        int totalMembers = 0;
        double totalExchangeRate = 0.0;
        Map<String, Integer> nationsByMembership = new HashMap<>();
        Map<String, Integer> unionsByCurrency = new HashMap<>();
        
        for (CurrencyUnion union : unions.values()) {
            if (union.active) {
                activeUnions++;
                int memberCount = union.memberNations.size();
                membersByUnion.put(union.id, memberCount);
                totalMembers += memberCount;
                totalExchangeRate += union.exchangeRate;
                unionsByCurrency.put(union.commonCurrency, unionsByCurrency.getOrDefault(union.commonCurrency, 0) + 1);
                
                for (String nationId : union.memberNations) {
                    nationsByMembership.put(nationId, nationsByMembership.getOrDefault(nationId, 0) + 1);
                }
            }
        }
        
        stats.put("totalUnions", totalUnions);
        stats.put("activeUnions", activeUnions);
        stats.put("totalMembers", totalMembers);
        stats.put("averageMembersPerUnion", activeUnions > 0 ? (double) totalMembers / activeUnions : 0);
        stats.put("averageExchangeRate", activeUnions > 0 ? totalExchangeRate / activeUnions : 0);
        stats.put("membersByUnion", membersByUnion);
        stats.put("nationsByMembership", nationsByMembership);
        stats.put("nationsInUnions", nationsByMembership.size());
        stats.put("unionsByCurrency", unionsByCurrency);
        
        // Top unions by members
        List<Map.Entry<String, Integer>> topByMembers = membersByUnion.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMembers", topByMembers);
        
        // Top nations by union membership
        List<Map.Entry<String, Integer>> topByMembership = nationsByMembership.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMembership", topByMembership);
        
        // Most common currencies
        List<Map.Entry<String, Integer>> mostCommonCurrencies = unionsByCurrency.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonCurrencies", mostCommonCurrencies);
        
        return stats;
    }
}

