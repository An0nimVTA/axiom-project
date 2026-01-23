package com.axiom.military.repository;

import com.axiom.core.Repository;
import com.axiom.military.model.MilitaryData;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Репозиторий для работы с военными данными
 */
public class MilitaryRepository implements Repository<MilitaryData, String> {
    private final ConcurrentMap<String, MilitaryData> militaryData = new ConcurrentHashMap<>();
    
    @Override
    public Optional<MilitaryData> findById(String nationId) {
        return Optional.ofNullable(militaryData.get(nationId));
    }
    
    @Override
    public List<MilitaryData> findAll() {
        return new java.util.ArrayList<>(militaryData.values());
    }
    
    @Override
    public MilitaryData save(MilitaryData militaryData) {
        this.militaryData.put(militaryData.getNationId(), militaryData);
        return militaryData;
    }
    
    @Override
    public void delete(String nationId) {
        militaryData.remove(nationId);
    }
    
    @Override
    public boolean exists(String nationId) {
        return militaryData.containsKey(nationId);
    }
    
    /**
     * Получить военные данные по ID нации
     */
    public Optional<MilitaryData> findByNationId(String nationId) {
        return Optional.ofNullable(militaryData.get(nationId));
    }
    
    /**
     * Создать или получить военные данные для нации
     */
    public MilitaryData getOrCreateMilitaryData(String nationId) {
        return militaryData.computeIfAbsent(nationId, MilitaryData::new);
    }
}