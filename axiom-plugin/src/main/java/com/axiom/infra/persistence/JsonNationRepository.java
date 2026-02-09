package com.axiom.infra.persistence;

import com.axiom.domain.repo.NationRepository;
import com.axiom.domain.repo.NationJsonCodec;
import com.axiom.domain.model.Nation;
import com.axiom.AXIOM;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private final Gson gson;
    
    public JsonNationRepository(AXIOM plugin) {
        this.plugin = plugin;
        this.nationsDir = new File(plugin.getDataFolder(), "nations");
        this.nationsDir.mkdirs();
        GsonBuilder builder = new GsonBuilder();
        if (plugin.getConfig().getBoolean("storage.prettyPrintJson", true)) {
            builder.setPrettyPrinting();
        }
        this.gson = builder.create();
    }
    
    @Override
    public Optional<Nation> findById(String id) {
        File file = new File(nationsDir, id + ".json");
        if (!file.exists()) {
            return Optional.empty();
        }
        
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            Nation nation = NationJsonCodec.deserialize(obj,
                plugin.getConfig().getString("economy.defaultCurrencyCode", "AXC"));
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
                Nation nation = NationJsonCodec.deserialize(obj,
                    plugin.getConfig().getString("economy.defaultCurrencyCode", "AXC"));
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
            gson.toJson(NationJsonCodec.serialize(nation), writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save nation " + nation.getId() + ": " + e.getMessage());
        }
    }
    
    @Override
    public void delete(String id) {
        if (plugin.getDiplomacyRelationService() != null) {
            plugin.getDiplomacyRelationService().cleanupNation(id);
        }
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

}
