package com.axiom.repository.impl;

import com.axiom.repository.NationRepository;
import com.axiom.model.Nation;
import com.axiom.AXIOM;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON реализация репозитория наций
 */
public class JsonNationRepository implements NationRepository {
    
    private final AXIOM plugin;
    private final File nationsDir;
    
    public JsonNationRepository(AXIOM plugin) {
        this.plugin = plugin;
        this.nationsDir = new File(plugin.getDataFolder(), "nations");
        this.nationsDir.mkdirs();
    }
    
    @Override
    public Optional<Nation> findById(String id) {
        File file = new File(nationsDir, id + ".json");
        if (!file.exists()) {
            return Optional.empty();
        }
        
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            Nation nation = deserializeNation(obj);
            return Optional.of(nation);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load nation " + id + ": " + e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public List<Nation> findAll() {
        File[] files = nationsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        
        List<Nation> nations = new ArrayList<>();
        for (File file : files) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                Nation nation = deserializeNation(obj);
                nations.add(nation);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load nation from " + file.getName() + ": " + e.getMessage());
            }
        }
        return nations;
    }
    
    @Override
    public void save(Nation nation) {
        File file = new File(nationsDir, nation.getId() + ".json");
        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(serializeNation(nation).toString());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save nation " + nation.getId() + ": " + e.getMessage());
        }
    }
    
    @Override
    public void delete(String id) {
        File file = new File(nationsDir, id + ".json");
        if (file.exists()) {
            file.delete();
        }
    }
    
    @Override
    public Optional<Nation> findByName(String name) {
        return findAll().stream()
            .filter(nation -> nation.getName().equalsIgnoreCase(name))
            .findFirst();
    }
    
    @Override
    public List<Nation> findByLeader(java.util.UUID leaderId) {
        return findAll().stream()
            .filter(nation -> nation.getLeader().equals(leaderId))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean exists(String id) {
        return findById(id).isPresent();
    }

    private JsonObject serializeNation(Nation n) {
        JsonObject o = new JsonObject();
        o.addProperty("id", n.getId());
        o.addProperty("name", n.getName());
        o.addProperty("leader", n.getLeader().toString());
        if (n.getCapitalChunkStr() != null) {
            o.addProperty("capitalChunk", n.getCapitalChunkStr());
        }
        o.addProperty("currency", n.getCurrencyCode());
        o.addProperty("exchangeRateToAXC", n.getExchangeRateToAXC());
        if (n.getMotto() != null) {
            o.addProperty("motto", n.getMotto());
        }
        if (n.getFlagIconMaterial() != null) {
            o.addProperty("flagIcon", n.getFlagIconMaterial());
        }
        o.addProperty("treasury", n.getTreasury());
        JsonArray claims = new JsonArray();
        for (String key : n.getClaimedChunkKeys()) {
            claims.add(key);
        }
        o.add("claimedChunks", claims);
        o.addProperty("inflation", n.getInflation());
        o.addProperty("taxRate", n.getTaxRate());
        JsonArray allies = new JsonArray();
        n.getAllies().forEach(allies::add);
        o.add("allies", allies);
        JsonArray enemies = new JsonArray();
        n.getEnemies().forEach(enemies::add);
        o.add("enemies", enemies);
        if (!n.getTabIcons().isEmpty()) {
            JsonObject tabs = new JsonObject();
            for (var e : n.getTabIcons().entrySet()) {
                tabs.addProperty(e.getKey(), e.getValue());
            }
            o.add("tabIcons", tabs);
        }
        if (!n.getReputation().isEmpty()) {
            JsonObject rep = new JsonObject();
            for (var e : n.getReputation().entrySet()) {
                rep.addProperty(e.getKey(), e.getValue());
            }
            o.add("reputation", rep);
        }
        if (!n.getPendingAlliance().isEmpty()) {
            JsonArray pa = new JsonArray();
            for (String s : n.getPendingAlliance()) {
                pa.add(s);
            }
            o.add("pendingAlliance", pa);
        }
        if (!n.getHistory().isEmpty()) {
            JsonArray h = new JsonArray();
            for (String s : n.getHistory()) {
                h.add(s);
            }
            o.add("history", h);
        }
        if (n.getBudgetMilitary() > 0 || n.getBudgetHealth() > 0 || n.getBudgetEducation() > 0) {
            o.addProperty("budgetMilitary", n.getBudgetMilitary());
            o.addProperty("budgetHealth", n.getBudgetHealth());
            o.addProperty("budgetEducation", n.getBudgetEducation());
        }
        if (n.getGovernmentType() != null) {
            o.addProperty("governmentType", n.getGovernmentType());
        }
        return o;
    }

    private Nation deserializeNation(JsonObject o) {
        String id = o.get("id").getAsString();
        String name = o.get("name").getAsString();
        java.util.UUID leader = java.util.UUID.fromString(o.get("leader").getAsString());
        String currency = o.has("currency")
            ? o.get("currency").getAsString()
            : o.has("currencyCode")
                ? o.get("currencyCode").getAsString()
                : plugin.getConfig().getString("economy.defaultCurrencyCode", "AXC");
        double treasury = o.has("treasury") ? o.get("treasury").getAsDouble() : 0.0;
        Nation n = new Nation(id, name, leader, currency, treasury);
        if (o.has("capitalChunk")) {
            n.setCapitalChunkStr(o.get("capitalChunk").getAsString());
        } else if (o.has("capitalChunkStr")) {
            n.setCapitalChunkStr(o.get("capitalChunkStr").getAsString());
        }
        if (o.has("exchangeRateToAXC")) {
            n.setExchangeRateToAXC(o.get("exchangeRateToAXC").getAsDouble());
        }
        if (o.has("motto")) {
            n.setMotto(o.get("motto").getAsString());
        }
        if (o.has("flagIcon")) {
            n.setFlagIconMaterial(o.get("flagIcon").getAsString());
        } else if (o.has("flagIconMaterial")) {
            n.setFlagIconMaterial(o.get("flagIconMaterial").getAsString());
        }
        if (o.has("claimedChunks")) {
            JsonArray claims = o.getAsJsonArray("claimedChunks");
            for (JsonElement e : claims) {
                n.getClaimedChunkKeys().add(e.getAsString());
            }
        } else if (o.has("claimedChunkKeys")) {
            JsonArray claims = o.getAsJsonArray("claimedChunkKeys");
            for (JsonElement e : claims) {
                n.getClaimedChunkKeys().add(e.getAsString());
            }
        }
        n.setInflation(o.has("inflation") ? o.get("inflation").getAsDouble() : 0.0);
        n.setTaxRate(o.has("taxRate") ? o.get("taxRate").getAsInt() : 10);
        if (o.has("allies")) {
            for (JsonElement e : o.getAsJsonArray("allies")) {
                n.getAllies().add(e.getAsString());
            }
        }
        if (o.has("enemies")) {
            for (JsonElement e : o.getAsJsonArray("enemies")) {
                n.getEnemies().add(e.getAsString());
            }
        }
        if (o.has("tabIcons")) {
            JsonObject tabs = o.getAsJsonObject("tabIcons");
            for (var entry : tabs.entrySet()) {
                n.getTabIcons().put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        if (o.has("reputation")) {
            JsonObject rep = o.getAsJsonObject("reputation");
            for (var entry : rep.entrySet()) {
                n.getReputation().put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        if (o.has("pendingAlliance")) {
            JsonArray pa = o.getAsJsonArray("pendingAlliance");
            for (JsonElement e : pa) {
                n.getPendingAlliance().add(e.getAsString());
            }
        }
        if (o.has("history")) {
            JsonArray h = o.getAsJsonArray("history");
            for (JsonElement e : h) {
                n.getHistory().add(e.getAsString());
            }
        }
        if (o.has("budgetMilitary")) {
            n.setBudgetMilitary(o.get("budgetMilitary").getAsDouble());
        }
        if (o.has("budgetHealth")) {
            n.setBudgetHealth(o.get("budgetHealth").getAsDouble());
        }
        if (o.has("budgetEducation")) {
            n.setBudgetEducation(o.get("budgetEducation").getAsDouble());
        }
        if (o.has("governmentType")) {
            n.setGovernmentType(o.get("governmentType").getAsString());
        }
        if (o.has("citizens")) {
            for (JsonElement e : o.getAsJsonArray("citizens")) {
                try {
                    java.util.UUID citizenId = java.util.UUID.fromString(e.getAsString());
                    n.getCitizens().add(citizenId);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (o.has("roles")) {
            JsonObject roles = o.getAsJsonObject("roles");
            for (var entry : roles.entrySet()) {
                try {
                    java.util.UUID citizenId = java.util.UUID.fromString(entry.getKey());
                    Nation.Role role = Nation.Role.valueOf(entry.getValue().getAsString());
                    n.getCitizens().add(citizenId);
                    n.getRoles().put(citizenId, role);
                } catch (Exception ignored) {
                }
            }
        }
        n.getRoles().put(leader, Nation.Role.LEADER);
        n.getCitizens().add(leader);
        return n;
    }
}
