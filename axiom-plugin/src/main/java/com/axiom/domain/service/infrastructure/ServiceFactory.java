package com.axiom.domain.service.infrastructure;

import com.axiom.AXIOM;
import com.axiom.domain.repo.NationRepository;
import com.axiom.domain.repo.MilitaryRepository;
import com.axiom.infra.persistence.JsonNationRepository;
import com.axiom.infra.persistence.JsonMilitaryRepository;
import com.axiom.service.adapter.MilitaryServiceAdapter;
import com.axiom.service.adapter.SiegeServiceAdapter;
import com.axiom.service.adapter.EconomyServiceAdapter;
import com.axiom.service.example.NewMilitaryServiceExample;
import com.axiom.domain.service.industry.EconomyService;
import com.axiom.domain.service.industry.EconomyServiceInterface;
import com.axiom.domain.service.military.MilitaryService;
import com.axiom.domain.service.military.MilitaryServiceInterface;
import com.axiom.domain.service.military.SiegeService;
import com.axiom.domain.service.military.SiegeServiceInterface;

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
