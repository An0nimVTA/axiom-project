package com.axiom.nations.repository;

import com.axiom.core.Repository;
import com.axiom.nations.model.Nation;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Репозиторий для работы с нациями
 */
public class NationRepository implements Repository<Nation, String> {
    private final ConcurrentMap<String, Nation> nations = new ConcurrentHashMap<>();
    
    @Override
    public Optional<Nation> findById(String id) {
        return Optional.ofNullable(nations.get(id));
    }
    
    @Override
    public List<Nation> findAll() {
        return new java.util.ArrayList<>(nations.values());
    }
    
    @Override
    public Nation save(Nation nation) {
        nations.put(nation.getId(), nation);
        return nation;
    }
    
    @Override
    public void delete(String id) {
        nations.remove(id);
    }
    
    @Override
    public boolean exists(String id) {
        return nations.containsKey(id);
    }
    
    /**
     * Найти нацию по имени
     */
    public Optional<Nation> findByName(String name) {
        return nations.values().stream()
                .filter(nation -> nation.getName().equals(name))
                .findFirst();
    }
    
    /**
     * Получить количество наций
     */
    public int getCount() {
        return nations.size();
    }
}