package com.axiom.core.service;

import com.axiom.core.model.Nation;
import com.google.gson.*;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Manages nations, claims, and persistence.
 * Core version.
 */
public class NationManager {
    private final JavaPlugin plugin;
    private final File nationsDir;
    private final Gson gson;
    private final Map<String, Nation> idToNation = new HashMap<>();
    private final Map<String, Long> unclaimCooldownUntil = new HashMap<>(); // key: world:x:z

    public NationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.nationsDir = new File(plugin.getDataFolder(), "nations");
        this.nationsDir.mkdirs();
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        this.gson = builder.create();
        loadAll();
    }

    public synchronized Nation createNation(Player founder, String name, String currencyCode, double startingTreasury) throws IOException {
        String id = sanitizeId(name);
        if (idToNation.containsKey(id)) throw new IOException("Нация с таким id уже существует: " + id);
        Nation nation = new Nation(id, name, founder.getUniqueId(), currencyCode, startingTreasury);
        // Initial capital and claim
        Chunk chunk = founder.getLocation().getChunk();
        String key = chunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        nation.setCapitalChunkStr(key);
        nation.getClaimedChunkKeys().add(key);
        long timestamp = System.currentTimeMillis();
        nation.getHistory().add(java.time.Instant.ofEpochMilli(timestamp).toString() + " - Nation founded by " + founder.getName());
        idToNation.put(id, nation);
        save(nation);
        return nation;
    }

    public synchronized void save(Nation nation) throws IOException {
        File f = new File(nationsDir, nation.getId() + ".json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            writer.write(serializeNation(nation).toString());
        }
    }

    public synchronized void flush() throws IOException {
        for (Nation n : idToNation.values()) {
            save(n);
        }
    }

    public synchronized Nation getNationById(String id) { return idToNation.get(id); }
    public synchronized Collection<Nation> getAll() { return new ArrayList<>(idToNation.values()); }

    public synchronized Optional<Nation> getNationOfPlayer(UUID uuid) {
        return idToNation.values().stream().filter(n -> n.isMember(uuid)).findFirst();
    }

    public synchronized Nation getPlayerNation(UUID playerId) {
        return getNationOfPlayer(playerId).orElse(null);
    }

    public synchronized Optional<Nation> getNationClaiming(World world, int chunkX, int chunkZ) {
        String key = chunkKey(world.getName(), chunkX, chunkZ);
        return idToNation.values().stream().filter(n -> n.getClaimedChunkKeys().contains(key)).findFirst();
    }

    private void loadAll() {
        File[] files = nationsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                Nation n = deserializeNation(o);
                idToNation.put(n.getId(), n);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load nation file " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private String chunkKey(String world, int x, int z) { return world + ":" + x + ":" + z; }
    private String sanitizeId(String name) { return name.toLowerCase().replaceAll("[^a-z0-9_-]", "-"); }

    // JSON serialization (simplified for Core)
    private JsonObject serializeNation(Nation n) {
        JsonObject o = new JsonObject();
        o.addProperty("id", n.getId());
        o.addProperty("name", n.getName());
        o.addProperty("leader", n.getLeader().toString());
        o.addProperty("capitalChunk", n.getCapitalChunkStr());
        o.addProperty("currency", n.getCurrencyCode());
        o.addProperty("treasury", n.getTreasury());
        JsonArray arr = new JsonArray();
        for (String key : n.getClaimedChunkKeys()) arr.add(key);
        o.add("claimedChunks", arr);
        
        // Members
        JsonArray citizens = new JsonArray();
        for (UUID cid : n.getCitizens()) citizens.add(cid.toString());
        o.add("citizens", citizens);
        
        // Roles
        JsonObject rolesObj = new JsonObject();
        for (Map.Entry<UUID, Nation.Role> entry : n.getRoles().entrySet()) {
            rolesObj.addProperty(entry.getKey().toString(), entry.getValue().name());
        }
        o.add("roles", rolesObj);

        return o;
    }

    private Nation deserializeNation(JsonObject o) {
        String id = o.get("id").getAsString();
        String name = o.get("name").getAsString();
        UUID leader = UUID.fromString(o.get("leader").getAsString());
        String currency = o.has("currency") ? o.get("currency").getAsString() : "AXC";
        double treasury = o.has("treasury") ? o.get("treasury").getAsDouble() : 0.0;
        
        Nation n = new Nation(id, name, leader, currency, treasury);
        
        if (o.has("capitalChunk")) n.setCapitalChunkStr(o.get("capitalChunk").getAsString());
        
        if (o.has("claimedChunks")) {
            for (JsonElement e : o.getAsJsonArray("claimedChunks")) n.getClaimedChunkKeys().add(e.getAsString());
        }
        
        if (o.has("citizens")) {
            for (JsonElement e : o.getAsJsonArray("citizens")) n.getCitizens().add(UUID.fromString(e.getAsString()));
        }
        
        if (o.has("roles")) {
            JsonObject rolesObj = o.getAsJsonObject("roles");
            for (String key : rolesObj.keySet()) {
                n.getRoles().put(UUID.fromString(key), Nation.Role.valueOf(rolesObj.get(key).getAsString()));
            }
        }
        
        return n;
    }
}
