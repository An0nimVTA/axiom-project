package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.repository.NationRepository;
import com.axiom.repository.MilitaryRepository;
import com.axiom.repository.impl.JsonNationRepository;
import com.axiom.repository.impl.JsonMilitaryRepository;
import com.axiom.service.adapter.MilitaryServiceAdapter;
import com.axiom.service.adapter.SiegeServiceAdapter;
import com.axiom.service.adapter.EconomyServiceAdapter;
import com.axiom.service.example.NewMilitaryServiceExample;

/**
 * Фабрика для создания сервисов
 * Позволяет легко получать экземпляры сервисов
 */
public class ServiceFactory {
    
    private final AXIOM plugin;
    
    public ServiceFactory(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Создать MilitaryService через адаптер (текущая реализация)
     * @return MilitaryServiceInterface
     */
    public MilitaryServiceInterface createMilitaryService() {
        if (plugin.getMilitaryService() != null) {
            return new MilitaryServiceAdapter(plugin.getMilitaryService());
        }
        return new MilitaryServiceAdapter(plugin);
    }
    
    /**
     * Создать SiegeService через адаптер (текущая реализация)
     * @return SiegeServiceInterface
     */
    public SiegeServiceInterface createSiegeService() {
        if (plugin.getSiegeService() != null) {
            return new SiegeServiceAdapter(plugin.getSiegeService());
        }
        return new SiegeServiceAdapter(plugin);
    }
    
    /**
     * Создать EconomyService через адаптер (текущая реализация)
     * @return EconomyServiceInterface
     */
    public EconomyServiceInterface createEconomyService() {
        return new EconomyServiceAdapter(plugin);
    }
    
    /**
     * Создать NationRepository
     * @return NationRepository
     */
    public NationRepository createNationRepository() {
        return new JsonNationRepository(plugin);
    }
    
    /**
     * Создать MilitaryRepository
     * @return MilitaryRepository
     */
    public MilitaryRepository createMilitaryRepository() {
        return new JsonMilitaryRepository(plugin);
    }
    
    /**
     * Создать новый MilitaryService (будущая реализация)
     * @return MilitaryServiceInterface
     */
    public MilitaryServiceInterface createNewMilitaryService() {
        NationRepository nationRepo = createNationRepository();
        MilitaryRepository militaryRepo = createMilitaryRepository();
        return new NewMilitaryServiceExample(nationRepo, militaryRepo);
    }
}
