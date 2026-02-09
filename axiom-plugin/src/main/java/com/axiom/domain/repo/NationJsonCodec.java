package com.axiom.domain.repo;

import com.axiom.domain.model.Nation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.UUID;

/**
 * Shared JSON codec for Nation persistence.
 * Keeps schema consistent across NationManager and repository adapters.
 */
public final class NationJsonCodec {
    private NationJsonCodec() {}

    public static JsonObject serialize(Nation n) {
        JsonObject o = new JsonObject();
        if (n.getId() != null) o.addProperty("id", n.getId());
        if (n.getName() != null) o.addProperty("name", n.getName());
        if (n.getLeader() != null) o.addProperty("leader", n.getLeader().toString());
        if (n.getCapitalChunkStr() != null) o.addProperty("capitalChunk", n.getCapitalChunkStr());
        if (n.getCurrencyCode() != null) o.addProperty("currency", n.getCurrencyCode());
        o.addProperty("exchangeRateToAXC", n.getExchangeRateToAXC());
        if (n.getMotto() != null) o.addProperty("motto", n.getMotto());
        if (n.getFlagIconMaterial() != null) o.addProperty("flagIcon", n.getFlagIconMaterial());
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
            for (Map.Entry<String, String> e : n.getTabIcons().entrySet()) {
                tabs.addProperty(e.getKey(), e.getValue());
            }
            o.add("tabIcons", tabs);
        }

        if (!n.getReputation().isEmpty()) {
            JsonObject rep = new JsonObject();
            for (Map.Entry<String, Integer> e : n.getReputation().entrySet()) {
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

        if (n.getGovernmentType() != null) o.addProperty("governmentType", n.getGovernmentType());

        if (!n.getCitizens().isEmpty()) {
            JsonArray citizens = new JsonArray();
            for (UUID uuid : n.getCitizens()) {
                citizens.add(uuid.toString());
            }
            o.add("citizens", citizens);
        }

        if (!n.getRoles().isEmpty()) {
            JsonObject roles = new JsonObject();
            for (Map.Entry<UUID, Nation.Role> entry : n.getRoles().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    roles.addProperty(entry.getKey().toString(), entry.getValue().name());
                }
            }
            o.add("roles", roles);
        }

        // Optional structured blocks for future schema alignment.
        JsonObject economy = new JsonObject();
        economy.addProperty("currency", n.getCurrencyCode());
        economy.addProperty("exchangeRateToAXC", n.getExchangeRateToAXC());
        economy.addProperty("treasury", n.getTreasury());
        economy.addProperty("inflation", n.getInflation());
        economy.addProperty("taxRate", n.getTaxRate());
        economy.addProperty("budgetMilitary", n.getBudgetMilitary());
        economy.addProperty("budgetHealth", n.getBudgetHealth());
        economy.addProperty("budgetEducation", n.getBudgetEducation());
        o.add("economy", economy);

        JsonObject territory = new JsonObject();
        if (n.getCapitalChunkStr() != null) {
            territory.addProperty("capitalChunk", n.getCapitalChunkStr());
        }
        territory.add("claimedChunks", claims.deepCopy());
        o.add("territory", territory);

        return o;
    }

    public static Nation deserialize(JsonObject o, String defaultCurrencyCode) {
        String id = getString(o, "id");
        String name = getString(o, "name");
        String leaderRaw = getString(o, "leader");
        if (id == null || name == null || leaderRaw == null) {
            throw new IllegalArgumentException("Invalid nation JSON (missing id/name/leader)");
        }
        UUID leader = UUID.fromString(leaderRaw);

        JsonObject economy = o.has("economy") && o.get("economy").isJsonObject()
            ? o.getAsJsonObject("economy")
            : null;

        String currency = getString(economy, "currency");
        if (currency == null) {
            currency = getString(o, "currency");
        }
        if (currency == null) {
            currency = getString(o, "currencyCode");
        }
        if (currency == null) {
            currency = defaultCurrencyCode;
        }

        double treasury = getDouble(economy, "treasury", getDouble(o, "treasury", 0.0));

        Nation n = new Nation(id, name, leader, currency, treasury);

        JsonObject territory = o.has("territory") && o.get("territory").isJsonObject()
            ? o.getAsJsonObject("territory")
            : null;

        String capital = getString(territory, "capitalChunk");
        if (capital == null) {
            capital = getString(o, "capitalChunk");
        }
        if (capital == null) {
            capital = getString(o, "capitalChunkStr");
        }
        if (capital != null) {
            n.setCapitalChunkStr(capital);
        }

        n.setExchangeRateToAXC(getDouble(economy, "exchangeRateToAXC", getDouble(o, "exchangeRateToAXC", 1.0)));
        if (o.has("motto")) n.setMotto(getString(o, "motto"));
        if (o.has("flagIcon")) n.setFlagIconMaterial(getString(o, "flagIcon"));
        else if (o.has("flagIconMaterial")) n.setFlagIconMaterial(getString(o, "flagIconMaterial"));

        JsonArray claims = null;
        if (territory != null && territory.has("claimedChunks")) {
            claims = territory.getAsJsonArray("claimedChunks");
        } else if (o.has("claimedChunks")) {
            claims = o.getAsJsonArray("claimedChunks");
        } else if (o.has("claimedChunkKeys")) {
            claims = o.getAsJsonArray("claimedChunkKeys");
        }
        if (claims != null) {
            for (JsonElement e : claims) {
                if (e != null && e.isJsonPrimitive()) {
                    n.getClaimedChunkKeys().add(e.getAsString());
                }
            }
        }
        if (n.getCapitalChunkStr() == null && !n.getClaimedChunkKeys().isEmpty()) {
            n.setCapitalChunkStr(n.getClaimedChunkKeys().iterator().next());
        }

        n.setInflation(getDouble(economy, "inflation", getDouble(o, "inflation", 0.0)));
        n.setTaxRate((int) getDouble(economy, "taxRate", getDouble(o, "taxRate", 10)));

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
            for (Map.Entry<String, JsonElement> entry : tabs.entrySet()) {
                n.getTabIcons().put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        if (o.has("reputation")) {
            JsonObject rep = o.getAsJsonObject("reputation");
            for (Map.Entry<String, JsonElement> entry : rep.entrySet()) {
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

        n.setBudgetMilitary(getDouble(economy, "budgetMilitary", getDouble(o, "budgetMilitary", 0.0)));
        n.setBudgetHealth(getDouble(economy, "budgetHealth", getDouble(o, "budgetHealth", 0.0)));
        n.setBudgetEducation(getDouble(economy, "budgetEducation", getDouble(o, "budgetEducation", 0.0)));

        if (o.has("governmentType")) n.setGovernmentType(getString(o, "governmentType"));

        if (o.has("citizens")) {
            for (JsonElement e : o.getAsJsonArray("citizens")) {
                try {
                    UUID citizenId = UUID.fromString(e.getAsString());
                    n.getCitizens().add(citizenId);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (o.has("roles")) {
            JsonObject roles = o.getAsJsonObject("roles");
            for (Map.Entry<String, JsonElement> entry : roles.entrySet()) {
                try {
                    UUID citizenId = UUID.fromString(entry.getKey());
                    Nation.Role role = Nation.Role.valueOf(entry.getValue().getAsString());
                    n.getCitizens().add(citizenId);
                    n.getRoles().put(citizenId, role);
                } catch (Exception ignored) {
                }
            }
        }

        // Ensure leader is present and owns LEADER role.
        n.getCitizens().add(leader);
        n.getRoles().put(leader, Nation.Role.LEADER);
        return n;
    }

    private static String getString(JsonObject o, String key) {
        if (o == null || key == null || !o.has(key)) return null;
        try {
            return o.get(key).getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double getDouble(JsonObject o, String key, double fallback) {
        if (o == null || key == null || !o.has(key)) return fallback;
        try {
            return o.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
