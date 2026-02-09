package com.axiom.infra.persistence;

import com.axiom.domain.repo.MilitaryRepository;
import com.axiom.domain.model.MilitaryData;
import com.axiom.AXIOM;
import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * JSON реализация репозитория военных данных
 */
public class JsonMilitaryRepository implements MilitaryRepository {
    
    private final AXIOM plugin;
    private final File militaryDir;
    private final Gson gson;
    
    public JsonMilitaryRepository(AXIOM plugin) {
        this.plugin = plugin;
        this.militaryDir = new File(plugin.getDataFolder(), "military");
        this.gson = new Gson();
        this.militaryDir.mkdirs();
    }
    
    @Override
    public Optional<MilitaryData> findByNationId(String nationId) {
        File file = new File(militaryDir, nationId + ".json");
        if (!file.exists()) {
            return Optional.empty();
        }
        
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            MilitaryData data = gson.fromJson(reader, MilitaryData.class);
            if (data != null) {
                data.recomputeDerivedStats();
            }
            return Optional.of(data);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load military data for " + nationId + ": " + e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void save(MilitaryData militaryData) {
        File file = new File(militaryDir, militaryData.getNationId() + ".json");
        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(militaryData, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save military data for " + militaryData.getNationId() + ": " + e.getMessage());
        }
    }
    
    @Override
    public void delete(String nationId) {
        File file = new File(militaryDir, nationId + ".json");
        if (file.exists()) {
            file.delete();
        }
    }
    
    @Override
    public boolean exists(String nationId) {
        return findByNationId(nationId).isPresent();
    }
    
    @Override
    public List<MilitaryData> findAll() {
        File[] files = militaryDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        
        List<MilitaryData> dataList = new ArrayList<>();
        for (File file : files) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
                MilitaryData data = gson.fromJson(reader, MilitaryData.class);
                if (data != null) {
                    data.recomputeDerivedStats();
                }
                dataList.add(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load military data from " + file.getName() + ": " + e.getMessage());
            }
        }
        return dataList;
    }
}
