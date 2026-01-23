package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages resource cartels (OPEC-like organizations). */
public class ResourceCartelService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File cartelsDir;
    private final Map<String, ResourceCartel> cartels = new HashMap<>(); // cartelId -> cartel

    public static class ResourceCartel {
        String id;
        String name;
        String resourceType;
        Set<String> memberNations = new HashSet<>();
        double priceControl; // target price multiplier
        double productionQuota; // per member
        boolean active;
    }

    public ResourceCartelService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.cartelsDir = new File(plugin.getDataFolder(), "resourcecartels");
        this.cartelsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processCartels, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String createCartel(String name, String resourceType, String initiatorId, double priceControl) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(name) || isBlank(resourceType) || isBlank(initiatorId)) return "Неверные параметры.";
        if (!Double.isFinite(priceControl) || priceControl < 0) return "Некорректный ценовой контроль.";
        Nation initiator = nationManager.getNationById(initiatorId);
        if (initiator == null) return "Нация не найдена.";
        String cartelId = UUID.randomUUID().toString().substring(0, 8);
        ResourceCartel cartel = new ResourceCartel();
        cartel.id = cartelId;
        cartel.name = name;
        cartel.resourceType = resourceType;
        cartel.memberNations.add(initiatorId);
        cartel.priceControl = priceControl;
        cartel.productionQuota = 100.0; // Base quota
        cartel.active = true;
        cartels.put(cartelId, cartel);
        if (initiator.getHistory() != null) {
            initiator.getHistory().add("Создан ресурсный картель: " + name);
        }
        try {
            nationManager.save(initiator);
            saveCartel(cartel);
        } catch (Exception ignored) {}
        return "Ресурсный картель создан: " + name;
    }

    public synchronized String joinCartel(String cartelId, String nationId) {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(cartelId) || isBlank(nationId)) return "Неверные параметры.";
        ResourceCartel cartel = cartels.get(cartelId);
        if (cartel == null) return "Картель не найден.";
        if (!cartel.active) return "Картель неактивен.";
        if (cartel.memberNations.contains(nationId)) return "Вы уже член этого картеля.";
        Nation n = nationManager.getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        cartel.memberNations.add(nationId);
        if (n.getHistory() != null) {
            n.getHistory().add("Присоединилась к ресурсному картелю: " + cartel.name);
        }
        try {
            nationManager.save(n);
            saveCartel(cartel);
        } catch (Exception ignored) {}
        return "Присоединение к картелю подтверждено.";
    }

    private synchronized void processCartels() {
        if (nationManager == null) return;
        for (ResourceCartel cartel : cartels.values()) {
            if (cartel == null) continue;
            if (!cartel.active) continue;
            // Cartel members get price benefits
            for (String nationId : cartel.memberNations) {
                Nation n = nationManager.getNationById(nationId);
                if (n != null) {
                    // Increased revenue from controlled pricing
                    double bonus = cartel.priceControl * 100;
                    if (!Double.isFinite(bonus) || bonus <= 0) continue;
                    n.setTreasury(n.getTreasury() + bonus);
                    try { nationManager.save(n); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void loadAll() {
        File[] files = cartelsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                ResourceCartel cartel = new ResourceCartel();
                cartel.id = o.has("id") ? o.get("id").getAsString() : null;
                cartel.name = o.has("name") ? o.get("name").getAsString() : null;
                cartel.resourceType = o.has("resourceType") ? o.get("resourceType").getAsString() : null;
                cartel.priceControl = o.has("priceControl") ? o.get("priceControl").getAsDouble() : 0.0;
                cartel.productionQuota = o.has("productionQuota") ? o.get("productionQuota").getAsDouble() : 0.0;
                cartel.active = o.has("active") && o.get("active").getAsBoolean();
                if (o.has("memberNations")) {
                    for (var elem : o.getAsJsonArray("memberNations")) {
                        cartel.memberNations.add(elem.getAsString());
                    }
                }
                if (!isBlank(cartel.id)) {
                    cartels.put(cartel.id, cartel);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveCartel(ResourceCartel cartel) {
        if (cartel == null || isBlank(cartel.id)) return;
        File f = new File(cartelsDir, cartel.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", cartel.id);
        o.addProperty("name", cartel.name);
        o.addProperty("resourceType", cartel.resourceType);
        o.addProperty("priceControl", cartel.priceControl);
        o.addProperty("productionQuota", cartel.productionQuota);
        o.addProperty("active", cartel.active);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String nationId : cartel.memberNations) {
            arr.add(nationId);
        }
        o.add("memberNations", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive cartel statistics.
     */
    public synchronized Map<String, Object> getCartelStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (isBlank(nationId)) return stats;
        // Cartels this nation is member of
        List<ResourceCartel> memberCartels = new ArrayList<>();
        for (ResourceCartel cartel : cartels.values()) {
            if (cartel != null && cartel.memberNations.contains(nationId)) {
                memberCartels.add(cartel);
            }
        }
        
        stats.put("memberCartels", memberCartels.size());
        stats.put("cartels", memberCartels);
        
        // Calculate total cartel benefits
        double totalBenefits = 0.0;
        for (ResourceCartel cartel : memberCartels) {
            if (cartel.active) {
                totalBenefits += cartel.priceControl * 100; // Benefit per cycle
            }
        }
        stats.put("totalBenefits", totalBenefits);
        
        // Cartel membership by resource
        Map<String, Integer> byResource = new HashMap<>();
        for (ResourceCartel cartel : memberCartels) {
            byResource.put(cartel.resourceType, byResource.getOrDefault(cartel.resourceType, 0) + 1);
        }
        stats.put("byResource", byResource);
        
        // Cartel rating
        String rating = "НЕ В КАРТЕЛЯХ";
        if (memberCartels.size() >= 5) rating = "МНОЖЕСТВЕННОЕ ЧЛЕНСТВО";
        else if (memberCartels.size() >= 3) rating = "АКТИВНОЕ ЧЛЕНСТВО";
        else if (memberCartels.size() >= 1) rating = "ЧЛЕН КАРТЕЛЯ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Leave cartel.
     */
    public synchronized String leaveCartel(String cartelId, String nationId) throws IOException {
        if (nationManager == null) return "Сервис наций недоступен.";
        if (isBlank(cartelId) || isBlank(nationId)) return "Неверные параметры.";
        ResourceCartel cartel = cartels.get(cartelId);
        if (cartel == null) return "Картель не найден.";
        if (!cartel.memberNations.contains(nationId)) return "Вы не являетесь членом этого картеля.";
        
        cartel.memberNations.remove(nationId);
        
        // If no members left, disband cartel
        if (cartel.memberNations.isEmpty()) {
            cartel.active = false;
            cartels.remove(cartelId);
            File f = new File(cartelsDir, cartelId + ".json");
            if (f.exists()) f.delete();
        } else {
            saveCartel(cartel);
        }
        
        Nation n = nationManager.getNationById(nationId);
        if (n != null) {
            if (n.getHistory() != null) {
                n.getHistory().add("Покинула ресурсный картель: " + cartel.name);
            }
            nationManager.save(n);
        }
        
        return "Вы покинули картель: " + cartel.name;
    }
    
    /**
     * Update cartel price control.
     */
    public synchronized String updatePriceControl(String cartelId, String leaderId, double newPriceControl) throws IOException {
        if (isBlank(cartelId) || isBlank(leaderId)) return "Неверные параметры.";
        if (!Double.isFinite(newPriceControl) || newPriceControl < 0) return "Некорректный ценовой контроль.";
        ResourceCartel cartel = cartels.get(cartelId);
        if (cartel == null) return "Картель не найден.";
        if (!cartel.memberNations.contains(leaderId) || cartel.memberNations.size() > 0 && !cartel.memberNations.iterator().next().equals(leaderId)) {
            return "Вы не являетесь лидером картеля.";
        }
        
        cartel.priceControl = newPriceControl;
        saveCartel(cartel);
        
        return "Ценовой контроль обновлён: " + newPriceControl;
    }
    
    /**
     * Get all cartels for a resource type.
     */
    public synchronized List<ResourceCartel> getCartelsForResource(String resourceType) {
        if (isBlank(resourceType)) return java.util.Collections.emptyList();
        List<ResourceCartel> result = new ArrayList<>();
        for (ResourceCartel cartel : cartels.values()) {
            if (cartel != null && cartel.active && resourceType.equals(cartel.resourceType)) {
                result.add(cartel);
            }
        }
        return result;
    }
    
    /**
     * Calculate market power from cartels.
     */
    public synchronized double getMarketPower(String nationId, String resourceType) {
        if (isBlank(nationId) || isBlank(resourceType)) return 1.0;
        double power = 1.0;
        for (ResourceCartel cartel : cartels.values()) {
            if (cartel != null && cartel.active && resourceType.equals(cartel.resourceType) && cartel.memberNations.contains(nationId)) {
                // Market power increases with cartel membership
                power += cartel.priceControl * 0.1; // +0.1% per 1% price control
            }
        }
        return Math.min(2.0, power); // Cap at +100%
    }
    
    /**
     * Get global resource cartel statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalResourceCartelStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalCartels = cartels.size();
        int activeCartels = 0;
        Map<String, Integer> cartelsByResource = new HashMap<>();
        Map<String, Integer> membersByCartel = new HashMap<>();
        int totalMembers = 0;
        double totalPriceControl = 0.0;
        
        for (ResourceCartel cartel : cartels.values()) {
            if (cartel == null) continue;
            if (cartel.active) {
                activeCartels++;
                cartelsByResource.put(cartel.resourceType, cartelsByResource.getOrDefault(cartel.resourceType, 0) + 1);
                membersByCartel.put(cartel.id, cartel.memberNations.size());
                totalMembers += cartel.memberNations.size();
                totalPriceControl += cartel.priceControl;
            }
        }
        
        stats.put("totalCartels", totalCartels);
        stats.put("activeCartels", activeCartels);
        stats.put("cartelsByResource", cartelsByResource);
        stats.put("totalMembers", totalMembers);
        stats.put("averageMembersPerCartel", activeCartels > 0 ? (double) totalMembers / activeCartels : 0);
        stats.put("averagePriceControl", activeCartels > 0 ? totalPriceControl / activeCartels : 0);
        stats.put("membersByCartel", membersByCartel);
        
        // Top cartels by members
        List<Map.Entry<String, Integer>> topByMembers = membersByCartel.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topByMembers", topByMembers);
        
        // Most common resource types
        List<Map.Entry<String, Integer>> mostCommonResources = cartelsByResource.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonResources", mostCommonResources);
        
        // Nations by cartel membership
        Map<String, Integer> nationsByMembership = new HashMap<>();
        for (ResourceCartel cartel : cartels.values()) {
            if (cartel != null && cartel.active) {
                for (String nationId : cartel.memberNations) {
                    nationsByMembership.put(nationId, nationsByMembership.getOrDefault(nationId, 0) + 1);
                }
            }
        }
        stats.put("nationsByMembership", nationsByMembership);
        stats.put("nationsInCartels", nationsByMembership.size());
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

