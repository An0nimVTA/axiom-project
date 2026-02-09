package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages import/export of goods and resources. */
public class ImportExportService {
    private final AXIOM plugin;
    private final File tradeDir;
    private final Map<String, List<TradeTransaction>> nationTrades = new HashMap<>(); // nationId -> transactions

    public static class TradeTransaction {
        String resourceType;
        double quantity;
        double price;
        String partnerNationId;
        boolean isImport; // false = export
        long timestamp;
    }

    public ImportExportService(AXIOM plugin) {
        this.plugin = plugin;
        this.tradeDir = new File(plugin.getDataFolder(), "importexport");
        this.tradeDir.mkdirs();
        loadAll();
    }

    public synchronized String importResource(String nationId, String resourceType, double quantity, double pricePerUnit, String partnerId) {
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (plugin.getResourceService() == null) return "Сервис ресурсов недоступен.";
        if (nationId == null || resourceType == null || resourceType.trim().isEmpty()) return "Неверные параметры.";
        if (quantity <= 0 || pricePerUnit <= 0) return "Количество и цена должны быть больше 0.";
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        double totalCost = quantity * pricePerUnit;
        if (n.getTreasury() < totalCost) return "Недостаточно средств.";
        plugin.getResourceService().addResource(nationId, resourceType, quantity);
        n.setTreasury(n.getTreasury() - totalCost);
        TradeTransaction t = new TradeTransaction();
        t.resourceType = resourceType;
        t.quantity = quantity;
        t.price = totalCost;
        t.partnerNationId = partnerId;
        t.isImport = true;
        t.timestamp = System.currentTimeMillis();
        nationTrades.computeIfAbsent(nationId, k -> new ArrayList<>()).add(t);
        try {
            plugin.getNationManager().save(n);
            saveTransaction(nationId, t);
        } catch (Exception ignored) {}
        return "Импортировано: " + quantity + " " + resourceType;
    }

    public synchronized String exportResource(String nationId, String resourceType, double quantity, double pricePerUnit, String partnerId) {
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (plugin.getResourceService() == null) return "Сервис ресурсов недоступен.";
        if (nationId == null || resourceType == null || resourceType.trim().isEmpty()) return "Неверные параметры.";
        if (quantity <= 0 || pricePerUnit <= 0) return "Количество и цена должны быть больше 0.";
        Nation n = plugin.getNationManager().getNationById(nationId);
        if (n == null) return "Нация не найдена.";
        if (!plugin.getResourceService().consumeResource(nationId, resourceType, quantity)) {
            return "Недостаточно ресурсов.";
        }
        double totalRevenue = quantity * pricePerUnit;
        n.setTreasury(n.getTreasury() + totalRevenue);
        TradeTransaction t = new TradeTransaction();
        t.resourceType = resourceType;
        t.quantity = quantity;
        t.price = totalRevenue;
        t.partnerNationId = partnerId;
        t.isImport = false;
        t.timestamp = System.currentTimeMillis();
        nationTrades.computeIfAbsent(nationId, k -> new ArrayList<>()).add(t);
        try {
            plugin.getNationManager().save(n);
            saveTransaction(nationId, t);
        } catch (Exception ignored) {}
        return "Экспортировано: " + quantity + " " + resourceType + " (+" + totalRevenue + ")";
    }

    private void loadAll() {
        File[] files = tradeDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                String nationId = f.getName().replace(".json", "");
                List<TradeTransaction> transactions = new ArrayList<>();
                if (o.has("transactions")) {
                    for (var elem : o.getAsJsonArray("transactions")) {
                        JsonObject tObj = elem.getAsJsonObject();
                        TradeTransaction t = new TradeTransaction();
                        t.resourceType = tObj.get("resourceType").getAsString();
                        t.quantity = tObj.get("quantity").getAsDouble();
                        t.price = tObj.get("price").getAsDouble();
                        t.partnerNationId = tObj.get("partnerNationId").getAsString();
                        t.isImport = tObj.get("isImport").getAsBoolean();
                        t.timestamp = tObj.get("timestamp").getAsLong();
                        transactions.add(t);
                    }
                }
                nationTrades.put(nationId, transactions);
            } catch (Exception ignored) {}
        }
    }

    private void saveTransaction(String nationId, TradeTransaction t) {
        File f = new File(tradeDir, nationId + ".json");
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        List<TradeTransaction> transactions = nationTrades.get(nationId);
        if (transactions != null) {
            for (TradeTransaction tx : transactions) {
                JsonObject tObj = new JsonObject();
                tObj.addProperty("resourceType", tx.resourceType);
                tObj.addProperty("quantity", tx.quantity);
                tObj.addProperty("price", tx.price);
                tObj.addProperty("partnerNationId", tx.partnerNationId);
                tObj.addProperty("isImport", tx.isImport);
                tObj.addProperty("timestamp", tx.timestamp);
                arr.add(tObj);
            }
        }
        o.add("transactions", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        } catch (Exception ignored) {}
    }
}

