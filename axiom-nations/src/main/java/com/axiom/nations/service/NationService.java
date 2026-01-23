package com.axiom.nations.service;

import com.axiom.core.EventPublisher;
import com.axiom.nations.model.Nation;
import com.axiom.nations.repository.NationRepository;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с нациями
 */
public class NationService {
    private final NationRepository nationRepository;
    private final EventPublisher eventPublisher;
    
    public NationService(NationRepository nationRepository, EventPublisher eventPublisher) {
        this.nationRepository = nationRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Создание новой нации
     */
    public Optional<Nation> createNation(String id, String name, String leader) {
        if (nationRepository.exists(id)) {
            return Optional.empty(); // Нация с таким ID уже существует
        }
        
        Nation nation = new Nation(id, name, leader);
        nationRepository.save(nation);
        
        // Публикация события о создании нации
        eventPublisher.publish(new NationCreatedEvent(nation));
        
        return Optional.of(nation);
    }
    
    /**
     * Получение нации по ID
     */
    public Optional<Nation> getNation(String id) {
        return nationRepository.findById(id);
    }
    
    /**
     * Получение нации по имени
     */
    public Optional<Nation> getNationByName(String name) {
        return nationRepository.findByName(name);
    }
    
    /**
     * Обновление нации
     */
    public boolean updateNation(Nation nation) {
        if (!nationRepository.exists(nation.getId())) {
            return false;
        }
        
        nationRepository.save(nation);
        eventPublisher.publish(new NationUpdatedEvent(nation));
        return true;
    }
    
    /**
     * Удаление нации
     */
    public boolean deleteNation(String id) {
        Optional<Nation> nation = nationRepository.findById(id);
        if (nation.isPresent()) {
            nationRepository.delete(id);
            eventPublisher.publish(new NationDeletedEvent(nation.get()));
            return true;
        }
        return false;
    }
    
    /**
     * Получение всех наций
     */
    public List<Nation> getAllNations() {
        return nationRepository.findAll();
    }
    
    /**
     * Добавление города к нации
     */
    public boolean addCityToNation(String nationId, String cityName) {
        Optional<Nation> nation = nationRepository.findById(nationId);
        if (nation.isPresent()) {
            nation.get().addCity(cityName);
            nationRepository.save(nation.get());
            eventPublisher.publish(new CityAddedToNationEvent(nationId, cityName));
            return true;
        }
        return false;
    }
    
    /**
     * Удаление города из нации
     */
    public boolean removeCityFromNation(String nationId, String cityName) {
        Optional<Nation> nation = nationRepository.findById(nationId);
        if (nation.isPresent()) {
            nation.get().removeCity(cityName);
            nationRepository.save(nation.get());
            eventPublisher.publish(new CityRemovedFromNationEvent(nationId, cityName));
            return true;
        }
        return false;
    }
    
    /**
     * Добавление модификатора к нации
     */
    public boolean addModifierToNation(String nationId, String key, Object value) {
        Optional<Nation> nation = nationRepository.findById(nationId);
        if (nation.isPresent()) {
            nation.get().addModifier(key, value);
            nationRepository.save(nation.get());
            eventPublisher.publish(new NationModifierAddedEvent(nationId, key, value));
            return true;
        }
        return false;
    }
    
    public NationRepository getNationRepository() {
        return nationRepository;
    }
}

// События для системы
class NationCreatedEvent {
    public final Nation nation;
    
    public NationCreatedEvent(Nation nation) {
        this.nation = nation;
    }
}

class NationUpdatedEvent {
    public final Nation nation;
    
    public NationUpdatedEvent(Nation nation) {
        this.nation = nation;
    }
}

class NationDeletedEvent {
    public final Nation nation;
    
    public NationDeletedEvent(Nation nation) {
        this.nation = nation;
    }
}

class CityAddedToNationEvent {
    public final String nationId;
    public final String cityName;
    
    public CityAddedToNationEvent(String nationId, String cityName) {
        this.nationId = nationId;
        this.cityName = cityName;
    }
}

class CityRemovedFromNationEvent {
    public final String nationId;
    public final String cityName;
    
    public CityRemovedFromNationEvent(String nationId, String cityName) {
        this.nationId = nationId;
        this.cityName = cityName;
    }
}

class NationModifierAddedEvent {
    public final String nationId;
    public final String key;
    public final Object value;
    
    public NationModifierAddedEvent(String nationId, String key, Object value) {
        this.nationId = nationId;
        this.key = key;
        this.value = value;
    }
}