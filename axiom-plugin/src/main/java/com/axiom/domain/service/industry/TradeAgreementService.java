package com.axiom.domain.service.industry;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Manages specific trade agreements between nations. */
public class TradeAgreementService {
    private final AXIOM plugin;
    private final File agreementsDir;
    private final Map<String, TradeAgreement> agreements = new HashMap<>(); // agreementId -> agreement

    public static class TradeAgreement {
        String id;
        String nationA;
        String nationB;
        String resourceType;
        double pricePerUnit;
        int quantityPerPeriod;
        long nextTrade;
        boolean active;
    }

    public TradeAgreementService(AXIOM plugin) {
        this.plugin = plugin;
        this.agreementsDir = new File(plugin.getDataFolder(), "tradeagreements");
        this.agreementsDir.mkdirs();
        loadAll();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processAgreements, 0, 20 * 60 * 10); // every 10 minutes
    }

    public synchronized String createAgreement(String nationA, String nationB, String resourceType, double pricePerUnit, int quantityPerPeriod) throws IOException {
        if (isBlank(nationA) || isBlank(nationB) || isBlank(resourceType)) return "Неверные параметры.";
        if (nationA.equals(nationB)) return "Нельзя создать соглашение с собой.";
        if (!Double.isFinite(pricePerUnit) || pricePerUnit < 0) return "Некорректная цена.";
        if (quantityPerPeriod <= 0) return "Некорректное количество.";
        if (plugin.getNationManager() == null) return "Сервис наций недоступен.";
        if (plugin.getNationManager().getNationById(nationA) == null
            || plugin.getNationManager().getNationById(nationB) == null) {
            return "Нация не найдена.";
        }
        String id = nationA + "_" + nationB + "_" + resourceType;
        if (agreements.containsKey(id)) return "Соглашение уже существует.";
        TradeAgreement ta = new TradeAgreement();
        ta.id = id;
        ta.nationA = nationA;
        ta.nationB = nationB;
        ta.resourceType = resourceType;
        ta.pricePerUnit = pricePerUnit;
        ta.quantityPerPeriod = quantityPerPeriod;
        ta.nextTrade = System.currentTimeMillis() + 60 * 60_000L; // 1 hour
        ta.active = true;
        agreements.put(id, ta);
        saveAgreement(ta);
        return "Торговое соглашение создано: " + resourceType + " (" + quantityPerPeriod + " за период)";
    }

    private synchronized void processAgreements() {
        if (plugin.getResourceService() == null || plugin.getNationManager() == null) return;
        long now = System.currentTimeMillis();
        for (TradeAgreement ta : agreements.values()) {
            if (ta == null) continue;
            if (!ta.active || now < ta.nextTrade) continue;
            if (isBlank(ta.nationA) || isBlank(ta.nationB) || isBlank(ta.resourceType)) {
                ta.nextTrade = now + 60 * 60_000L;
                continue;
            }
            // Execute trade
            double payment = ta.quantityPerPeriod * ta.pricePerUnit;
            if (!Double.isFinite(payment) || payment < 0 || ta.quantityPerPeriod <= 0) {
                ta.nextTrade = now + 60 * 60_000L;
                continue;
            }
            Nation buyer = plugin.getNationManager().getNationById(ta.nationB);
            Nation seller = plugin.getNationManager().getNationById(ta.nationA);
            if (buyer != null && seller != null && buyer.getTreasury() >= payment) {
                if (plugin.getResourceService().consumeResource(ta.nationA, ta.resourceType, ta.quantityPerPeriod)) {
                    plugin.getResourceService().addResource(ta.nationB, ta.resourceType, ta.quantityPerPeriod);
                    buyer.setTreasury(buyer.getTreasury() - payment);
                    seller.setTreasury(seller.getTreasury() + payment);
                    try {
                        plugin.getNationManager().save(buyer);
                        plugin.getNationManager().save(seller);
                    } catch (Exception ignored) {}
                }
            }
            ta.nextTrade = now + 60 * 60_000L;
        }
    }

    private void loadAll() {
        File[] files = agreementsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                TradeAgreement ta = new TradeAgreement();
                ta.id = o.has("id") ? o.get("id").getAsString() : null;
                ta.nationA = o.has("nationA") ? o.get("nationA").getAsString() : null;
                ta.nationB = o.has("nationB") ? o.get("nationB").getAsString() : null;
                ta.resourceType = o.has("resourceType") ? o.get("resourceType").getAsString() : null;
                ta.pricePerUnit = o.has("pricePerUnit") ? o.get("pricePerUnit").getAsDouble() : 0.0;
                ta.quantityPerPeriod = o.has("quantityPerPeriod") ? o.get("quantityPerPeriod").getAsInt() : 0;
                ta.nextTrade = o.has("nextTrade") ? o.get("nextTrade").getAsLong() : 0L;
                ta.active = o.has("active") && o.get("active").getAsBoolean();
                if (!isBlank(ta.id)) {
                    agreements.put(ta.id, ta);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveAgreement(TradeAgreement ta) throws IOException {
        if (ta == null || isBlank(ta.id)) return;
        File f = new File(agreementsDir, ta.id + ".json");
        JsonObject o = new JsonObject();
        o.addProperty("id", ta.id);
        o.addProperty("nationA", ta.nationA);
        o.addProperty("nationB", ta.nationB);
        o.addProperty("resourceType", ta.resourceType);
        o.addProperty("pricePerUnit", ta.pricePerUnit);
        o.addProperty("quantityPerPeriod", ta.quantityPerPeriod);
        o.addProperty("nextTrade", ta.nextTrade);
        o.addProperty("active", ta.active);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(o.toString());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

