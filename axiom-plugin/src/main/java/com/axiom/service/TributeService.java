package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages tribute payments between nations (vassals, victors). */
public class TributeService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File tributeDir;
    private final Map<String, Tribute> activeTributes = new HashMap<>(); // payerId -> tribute

    public static class Tribute {
        String payerNationId;
        String receiverNationId;
        double amount;
        long nextPayment;
        long intervalMinutes;
    }

    public TributeService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.tributeDir = new File(plugin.getDataFolder(), "tributes");
        this.tributeDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::collectTributes, 0, 20 * 60); // every minute
    }

    public synchronized String establishTribute(String payerId, String receiverId, double amount, long intervalMinutes) {
        if (isBlank(payerId) || isBlank(receiverId)) return "Некорректные данные.";
        if (payerId.equals(receiverId)) return "Нельзя платить дань самому себе.";
        if (amount <= 0 || intervalMinutes <= 0) return "Некорректные параметры.";
        if (activeTributes.containsKey(payerId)) return "Дань уже установлена.";
        Nation payer = nationManager.getNationById(payerId);
        Nation receiver = nationManager.getNationById(receiverId);
        if (payer == null || receiver == null) return "Нация не найдена.";
        Tribute t = new Tribute();
        t.payerNationId = payerId;
        t.receiverNationId = receiverId;
        t.amount = amount;
        t.intervalMinutes = intervalMinutes;
        t.nextPayment = System.currentTimeMillis() + (intervalMinutes * 60_000L);
        activeTributes.put(payerId, t);
        saveTribute(t);
        return "Дань установлена: " + amount + " каждые " + intervalMinutes + " минут";
    }

    private synchronized void collectTributes() {
        long now = System.currentTimeMillis();
        for (var entry : new HashMap<>(activeTributes).entrySet()) {
            Tribute t = entry.getValue();
            if (now >= t.nextPayment) {
                Nation payer = nationManager.getNationById(t.payerNationId);
                Nation receiver = nationManager.getNationById(t.receiverNationId);
                if (payer != null && receiver != null && payer.getTreasury() >= t.amount) {
                    payer.setTreasury(payer.getTreasury() - t.amount);
                    receiver.setTreasury(receiver.getTreasury() + t.amount);
                    t.nextPayment = now + (t.intervalMinutes * 60_000L);
                    saveTribute(t);
                    try {
                        nationManager.save(payer);
                        nationManager.save(receiver);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private void loadAll() {
        File[] files = tributeDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                Tribute t = new Tribute();
                t.payerNationId = o.get("payerNationId").getAsString();
                t.receiverNationId = o.get("receiverNationId").getAsString();
                t.amount = o.get("amount").getAsDouble();
                t.intervalMinutes = o.get("intervalMinutes").getAsLong();
                t.nextPayment = o.has("nextPayment") ? o.get("nextPayment").getAsLong() : System.currentTimeMillis();
                activeTributes.put(t.payerNationId, t);
            } catch (Exception ignored) {}
        }
    }

    private void saveTribute(Tribute t) {
        File f = new File(tributeDir, t.payerNationId + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("payerNationId", t.payerNationId);
        o.addProperty("receiverNationId", t.receiverNationId);
        o.addProperty("amount", t.amount);
        o.addProperty("intervalMinutes", t.intervalMinutes);
        o.addProperty("nextPayment", t.nextPayment);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive tribute statistics.
     */
    public synchronized Map<String, Object> getTributeStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // As payer
        Tribute paid = activeTributes.get(nationId);
        if (paid != null) {
            Map<String, Object> paidData = new HashMap<>();
            paidData.put("receiverNationId", paid.receiverNationId);
            paidData.put("amount", paid.amount);
            paidData.put("intervalMinutes", paid.intervalMinutes);
            paidData.put("nextPayment", paid.nextPayment);
            paidData.put("timeUntilPayment", Math.max(0, (paid.nextPayment - System.currentTimeMillis()) / 1000 / 60));
            stats.put("payingTribute", paidData);
            stats.put("isPaying", true);
        } else {
            stats.put("isPaying", false);
        }
        
        // As receiver
        List<Map<String, Object>> receivingList = new ArrayList<>();
        double totalReceived = 0.0;
        for (Tribute t : activeTributes.values()) {
            if (t.receiverNationId.equals(nationId)) {
                Map<String, Object> receiveData = new HashMap<>();
                receiveData.put("payerNationId", t.payerNationId);
                receiveData.put("amount", t.amount);
                receiveData.put("intervalMinutes", t.intervalMinutes);
                receiveData.put("nextPayment", t.nextPayment);
                receivingList.add(receiveData);
                totalReceived += t.amount;
            }
        }
        stats.put("receivingTributes", receivingList);
        stats.put("totalReceived", totalReceived);
        stats.put("tributeCount", receivingList.size());
        
        // Economic impact
        double netTribute = paid != null ? -paid.amount : 0.0;
        netTribute += totalReceived;
        stats.put("netTribute", netTribute);
        
        return stats;
    }
    
    /**
     * Cancel tribute.
     */
    public synchronized String cancelTribute(String payerId) throws IOException {
        Tribute t = activeTributes.remove(payerId);
        if (t == null) return "Дань не найдена.";
        
        // Delete file
        File f = new File(tributeDir, payerId + ".json");
        if (f.exists()) f.delete();
        
        Nation payer = nationManager.getNationById(payerId);
        Nation receiver = nationManager.getNationById(t.receiverNationId);
        if (payer != null && receiver != null) {
            long timestamp = System.currentTimeMillis();
            payer.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Дань " + receiver.getName() + " отменена");
            receiver.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " — Дань от " + payer.getName() + " отменена");
            nationManager.save(payer);
            nationManager.save(receiver);
        } else if (payer != null) {
            payer.getHistory().add("Дань отменена");
            nationManager.save(payer);
        } else if (receiver != null) {
            receiver.getHistory().add("Дань от " + payerId + " отменена");
            nationManager.save(receiver);
        }
        
        return "Дань отменена.";
    }
    
    /**
     * Modify tribute amount.
     */
    public synchronized String modifyTribute(String payerId, double newAmount) throws IOException {
        Tribute t = activeTributes.get(payerId);
        if (t == null) return "Дань не найдена.";
        if (newAmount <= 0) return "Некорректная сумма.";
        
        t.amount = newAmount;
        saveTribute(t);
        
        return "Дань изменена: " + newAmount;
    }
    
    /**
     * Get all tributes received by a nation.
     */
    public synchronized List<Tribute> getReceivedTributes(String nationId) {
        List<Tribute> result = new ArrayList<>();
        for (Tribute t : activeTributes.values()) {
            if (t.receiverNationId.equals(nationId)) {
                result.add(t);
            }
        }
        return result;
    }
    
    /**
     * Calculate total tribute income per hour.
     */
    public synchronized double calculateTributeIncome(String nationId) {
        double hourlyIncome = 0.0;
        for (Tribute t : activeTributes.values()) {
            if (t.receiverNationId.equals(nationId)) {
                if (t.intervalMinutes <= 0) continue;
                // Convert to hourly rate
                double hourlyRate = (t.amount / t.intervalMinutes) * 60.0;
                hourlyIncome += hourlyRate;
            }
        }
        return hourlyIncome;
    }
    
    /**
     * Get global tribute statistics.
     */
    public synchronized Map<String, Object> getGlobalTributeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalTributes", activeTributes.size());
        
        double totalAmount = 0.0;
        double maxAmount = 0.0;
        double totalReceived = 0.0;
        Map<String, Integer> byReceiver = new HashMap<>();
        Map<String, Double> totalByReceiver = new HashMap<>();
        
        for (Tribute t : activeTributes.values()) {
            totalAmount += t.amount;
            maxAmount = Math.max(maxAmount, t.amount);
            totalReceived += t.amount;
            byReceiver.put(t.receiverNationId, byReceiver.getOrDefault(t.receiverNationId, 0) + 1);
            totalByReceiver.put(t.receiverNationId, totalByReceiver.getOrDefault(t.receiverNationId, 0.0) + t.amount);
        }
        
        stats.put("totalTributeAmount", totalAmount);
        stats.put("maxTributeAmount", maxAmount);
        stats.put("averageTributeAmount", activeTributes.size() > 0 ? totalAmount / activeTributes.size() : 0);
        stats.put("totalReceivedPerInterval", totalReceived);
        
        // Top receivers
        List<Map.Entry<String, Double>> topReceivers = totalByReceiver.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topReceivers", topReceivers);
        
        // Tribute distribution
        stats.put("tributesByReceiver", byReceiver);
        
        // Nations paying vs receiving
        Set<String> payingNations = new HashSet<>();
        Set<String> receivingNations = new HashSet<>();
        for (Tribute t : activeTributes.values()) {
            payingNations.add(t.payerNationId);
            receivingNations.add(t.receiverNationId);
        }
        stats.put("nationsPayingTribute", payingNations.size());
        stats.put("nationsReceivingTribute", receivingNations.size());
        
        return stats;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

