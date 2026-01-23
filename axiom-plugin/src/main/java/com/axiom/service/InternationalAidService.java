package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages international aid and humanitarian assistance. */
public class InternationalAidService {
    private final AXIOM plugin;
    private final NationManager nationManager;
    private final File aidDir;
    private final Map<String, List<AidPackage>> aidPackages = new HashMap<>(); // recipientId -> packages

    public static class AidPackage {
        String id;
        String donorNationId;
        String recipientNationId;
        String type; // "humanitarian", "economic", "military", "technical"
        double amount;
        long sentAt;
        boolean delivered;
    }

    public InternationalAidService(AXIOM plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.aidDir = new File(plugin.getDataFolder(), "aid");
        this.aidDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processAid, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String sendAid(String donorId, String recipientId, String type, double amount) {
        Nation donor = nationManager.getNationById(donorId);
        Nation recipient = nationManager.getNationById(recipientId);
        if (donor == null || recipient == null) return "Нация не найдена.";
        if (donorId.equals(recipientId)) return "Нельзя отправить помощь себе.";
        if (amount <= 0) return "Неверная сумма.";
        if (donor.getTreasury() < amount) return "Недостаточно средств.";
        String aidId = UUID.randomUUID().toString().substring(0, 8);
        AidPackage aid = new AidPackage();
        aid.id = aidId;
        aid.donorNationId = donorId;
        aid.recipientNationId = recipientId;
        aid.type = type;
        aid.amount = amount;
        aid.sentAt = System.currentTimeMillis();
        aid.delivered = false;
        aidPackages.computeIfAbsent(recipientId, k -> new ArrayList<>()).add(aid);
        donor.setTreasury(donor.getTreasury() - amount);
        donor.getHistory().add("Отправлена помощь: " + type + " для " + recipient.getName());
        recipient.getHistory().add("Получена помощь от " + donor.getName());
        try {
            nationManager.save(donor);
            nationManager.save(recipient);
            saveAid(recipientId);
        } catch (Exception ignored) {}
        return "Помощь отправлена (ID: " + aidId + ")";
    }

    private synchronized void processAid() {
        HappinessService happinessService = plugin.getHappinessService();
        MilitaryService militaryService = plugin.getMilitaryService();
        EducationService educationService = plugin.getEducationService();
        for (Map.Entry<String, List<AidPackage>> e : aidPackages.entrySet()) {
            List<AidPackage> packages = e.getValue();
            List<AidPackage> delivered = new ArrayList<>();
            for (AidPackage aid : packages) {
                if (aid.delivered) continue;
                // Delivery takes 1 hour
                if (System.currentTimeMillis() - aid.sentAt > 60 * 60_000L) {
                    aid.delivered = true;
                    Nation recipient = nationManager.getNationById(e.getKey());
                    if (recipient != null) {
                        switch (aid.type.toLowerCase()) {
                            case "humanitarian":
                                recipient.setTreasury(recipient.getTreasury() + aid.amount);
                                if (happinessService != null) {
                                    happinessService.modifyHappiness(e.getKey(), 10.0);
                                }
                                break;
                            case "economic":
                                recipient.setTreasury(recipient.getTreasury() + aid.amount * 1.5);
                                break;
                            case "military":
                                if (militaryService != null) {
                                    militaryService.recruitUnits(e.getKey(), "infantry", 10, 0);
                                }
                                break;
                            case "technical":
                                if (educationService != null) {
                                    educationService.addResearchProgress(e.getKey(), 50.0);
                                }
                                break;
                        }
                        try { nationManager.save(recipient); } catch (Exception ignored) {}
                    }
                    delivered.add(aid);
                }
            }
            if (!delivered.isEmpty()) {
                saveAid(e.getKey());
            }
        }
    }

    private void loadAll() {
        File[] files = aidDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String recipientId = f.getName().replace(".json", "");
                List<AidPackage> packages = new ArrayList<>();
                if (o.has("packages")) {
                    for (var elem : o.getAsJsonArray("packages")) {
                        JsonObject aObj = elem.getAsJsonObject();
                        AidPackage aid = new AidPackage();
                        aid.id = aObj.get("id").getAsString();
                        aid.donorNationId = aObj.get("donorNationId").getAsString();
                        aid.recipientNationId = aObj.get("recipientNationId").getAsString();
                        aid.type = aObj.get("type").getAsString();
                        aid.amount = aObj.get("amount").getAsDouble();
                        aid.sentAt = aObj.get("sentAt").getAsLong();
                        aid.delivered = aObj.has("delivered") && aObj.get("delivered").getAsBoolean();
                        if (!aid.delivered) packages.add(aid);
                    }
                }
                aidPackages.put(recipientId, packages);
            } catch (Exception ignored) {}
        }
    }

    private void saveAid(String recipientId) {
        File f = new File(aidDir, recipientId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<AidPackage> packages = aidPackages.get(recipientId);
        if (packages != null) {
            for (AidPackage aid : packages) {
                JsonObject aObj = new JsonObject();
                aObj.addProperty("id", aid.id);
                aObj.addProperty("donorNationId", aid.donorNationId);
                aObj.addProperty("recipientNationId", aid.recipientNationId);
                aObj.addProperty("type", aid.type);
                aObj.addProperty("amount", aid.amount);
                aObj.addProperty("sentAt", aid.sentAt);
                aObj.addProperty("delivered", aid.delivered);
                arr.add(aObj);
            }
        }
        o.add("packages", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
    
    /**
     * Get comprehensive aid statistics.
     */
    public synchronized Map<String, Object> getAidStatistics(String nationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // As donor
        double totalDonated = 0.0;
        int donatedPackages = 0;
        Map<String, Integer> donatedByType = new HashMap<>();
        
        for (List<AidPackage> packages : aidPackages.values()) {
            for (AidPackage aid : packages) {
                if (aid.donorNationId.equals(nationId)) {
                    totalDonated += aid.amount;
                    donatedPackages++;
                    donatedByType.put(aid.type, donatedByType.getOrDefault(aid.type, 0) + 1);
                }
            }
        }
        
        stats.put("totalDonated", totalDonated);
        stats.put("donatedPackages", donatedPackages);
        stats.put("donatedByType", donatedByType);
        
        // As recipient
        List<AidPackage> receivedPackages = aidPackages.get(nationId);
        if (receivedPackages == null) receivedPackages = Collections.emptyList();
        
        double totalReceived = 0.0;
        int pendingPackages = 0;
        int deliveredPackages = 0;
        Map<String, Integer> receivedByType = new HashMap<>();
        
        for (AidPackage aid : receivedPackages) {
            totalReceived += aid.amount;
            receivedByType.put(aid.type, receivedByType.getOrDefault(aid.type, 0) + 1);
            if (aid.delivered) {
                deliveredPackages++;
            } else {
                pendingPackages++;
            }
        }
        
        stats.put("totalReceived", totalReceived);
        stats.put("receivedPackages", receivedPackages.size());
        stats.put("pendingPackages", pendingPackages);
        stats.put("deliveredPackages", deliveredPackages);
        stats.put("receivedByType", receivedByType);
        
        // Net aid (received - donated)
        stats.put("netAid", totalReceived - totalDonated);
        
        // Aid rating
        String rating = "НЕЙТРАЛЬНЫЙ";
        if (totalDonated >= 50000) rating = "ВЕЛИКОДУШНЫЙ ДОНОР";
        else if (totalDonated >= 20000) rating = "ЩЕДРЫЙ ДОНОР";
        else if (totalDonated >= 5000) rating = "ДОНОР";
        else if (totalReceived >= 30000) rating = "ПОЛУЧАТЕЛЬ ПОМОЩИ";
        else if (totalReceived >= 10000) rating = "УМЕРЕННЫЙ ПОЛУЧАТЕЛЬ";
        stats.put("rating", rating);
        
        return stats;
    }
    
    /**
     * Cancel aid package.
     */
    public synchronized String cancelAid(String recipientId, String aidId) throws IOException {
        List<AidPackage> packages = aidPackages.get(recipientId);
        if (packages == null) return "Пакеты помощи не найдены.";
        
        AidPackage aid = packages.stream()
            .filter(a -> a.id.equals(aidId))
            .findFirst()
            .orElse(null);
        
        if (aid == null) return "Пакет помощи не найден.";
        if (aid.delivered) return "Помощь уже доставлена.";
        
        // Refund donor
        Nation donor = nationManager.getNationById(aid.donorNationId);
        if (donor != null) {
            donor.setTreasury(donor.getTreasury() + aid.amount);
            nationManager.save(donor);
        }
        
        packages.remove(aid);
        saveAid(recipientId);
        
        return "Помощь отменена, средства возвращены донору.";
    }
    
    /**
     * Get all pending aid for a nation.
     */
    public synchronized List<AidPackage> getPendingAid(String nationId) {
        List<AidPackage> packages = aidPackages.get(nationId);
        if (packages == null) return Collections.emptyList();
        
        return packages.stream()
            .filter(a -> !a.delivered)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Calculate diplomatic bonus from aid.
     */
    public synchronized double getAidDiplomaticBonus(String nationId) {
        double totalDonated = 0.0;
        for (List<AidPackage> packages : aidPackages.values()) {
            for (AidPackage aid : packages) {
                if (aid.donorNationId.equals(nationId) && aid.delivered) {
                    totalDonated += aid.amount;
                }
            }
        }
        
        // +0.01% diplomatic bonus per 1000 donated
        return 1.0 + (totalDonated / 1000.0) * 0.01;
    }
    
    /**
     * Get global international aid statistics across all nations.
     */
    public synchronized Map<String, Object> getGlobalAidStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPackages = 0;
        int pendingPackages = 0;
        int deliveredPackages = 0;
        double totalDonated = 0.0;
        double totalReceived = 0.0;
        
        Map<String, Integer> packagesByType = new HashMap<>();
        Map<String, Double> donatedByNation = new HashMap<>();
        Map<String, Double> receivedByNation = new HashMap<>();
        
        for (Map.Entry<String, List<AidPackage>> entry : aidPackages.entrySet()) {
            double received = 0.0;
            for (AidPackage aid : entry.getValue()) {
                totalPackages++;
                received += aid.amount;
                
                if (aid.delivered) {
                    deliveredPackages++;
                } else {
                    pendingPackages++;
                }
                
                packagesByType.put(aid.type, packagesByType.getOrDefault(aid.type, 0) + 1);
                donatedByNation.put(aid.donorNationId, 
                    donatedByNation.getOrDefault(aid.donorNationId, 0.0) + aid.amount);
            }
            if (received > 0) {
                receivedByNation.put(entry.getKey(), received);
                totalReceived += received;
            }
        }
        
        // Calculate total donated
        totalDonated = donatedByNation.values().stream().mapToDouble(Double::doubleValue).sum();
        
        stats.put("totalPackages", totalPackages);
        stats.put("pendingPackages", pendingPackages);
        stats.put("deliveredPackages", deliveredPackages);
        stats.put("deliveryRate", totalPackages > 0 ? (deliveredPackages / (double) totalPackages) * 100 : 0);
        stats.put("totalDonated", totalDonated);
        stats.put("totalReceived", totalReceived);
        stats.put("packagesByType", packagesByType);
        stats.put("uniqueDonors", donatedByNation.size());
        stats.put("uniqueRecipients", receivedByNation.size());
        
        // Top donors
        List<Map.Entry<String, Double>> topDonors = donatedByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topDonors", topDonors);
        
        // Top recipients
        List<Map.Entry<String, Double>> topRecipients = receivedByNation.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        stats.put("topRecipients", topRecipients);
        
        // Average donation per donor
        stats.put("averageDonationPerDonor", donatedByNation.size() > 0 ? 
            totalDonated / donatedByNation.size() : 0);
        
        // Most common aid types
        List<Map.Entry<String, Integer>> mostCommonTypes = packagesByType.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        stats.put("mostCommonTypes", mostCommonTypes);
        
        return stats;
    }
}

