package com.axiom.economy.repository;

import com.axiom.core.Repository;
import com.axiom.economy.model.EconomyData;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Репозиторий для работы с экономическими данными
 */
public class EconomyRepository implements Repository<EconomyData, String> {
    private final ConcurrentMap<String, EconomyData> economyData = new ConcurrentHashMap<>();
    
    @Override
    public Optional<EconomyData> findById(String nationId) {
        return Optional.ofNullable(economyData.get(nationId));
    }
    
    @Override
    public List<EconomyData> findAll() {
        return new java.util.ArrayList<>(economyData.values());
    }
    
    @Override
    public EconomyData save(EconomyData economyData) {
        this.economyData.put(economyData.getNationId(), economyData);
        return economyData;
    }
    
    @Override
    public void delete(String nationId) {
        economyData.remove(nationId);
    }
    
    @Override
    public boolean exists(String nationId) {
        return economyData.containsKey(nationId);
    }
    
    /**
     * Получить экономические данные по ID нации
     */
    public Optional<EconomyData> findByNationId(String nationId) {
        return Optional.ofNullable(economyData.get(nationId));
    }
    
    /**
     * Создать или получить экономические данные для нации
     */
    public EconomyData getOrCreateEconomyData(String nationId) {
        return economyData.computeIfAbsent(nationId, EconomyData::new);
    }
}