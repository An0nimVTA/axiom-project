package com.axiom.service.adapter;

import com.axiom.service.MilitaryService;
import com.axiom.service.MilitaryServiceInterface;
import com.axiom.AXIOM;
import java.util.Map;

/**
 * Адаптер для MilitaryService
 * Позволяет использовать существующую реализацию через новый интерфейс
 */
public class MilitaryServiceAdapter implements MilitaryServiceInterface {
    
    private final MilitaryService legacyService;
    
    /**
     * Создать адаптер для существующего MilitaryService
     * @param plugin основной плагин
     */
    public MilitaryServiceAdapter(AXIOM plugin) {
        MilitaryService existing = plugin.getMilitaryService();
        this.legacyService = existing != null ? existing : new MilitaryService(plugin);
    }
    
    /**
     * Создать адаптер для существующего экземпляра
     * @param legacyService существующий сервис
     */
    public MilitaryServiceAdapter(MilitaryService legacyService) {
        this.legacyService = legacyService;
    }
    
    @Override
    public String recruitUnits(String nationId, String unitType, int count, double cost) {
        return legacyService.recruitUnits(nationId, unitType, count, cost);
    }
    
    @Override
    public double getMilitaryStrength(String nationId) {
        return legacyService.getMilitaryStrength(nationId);
    }
    
    @Override
    public Map<String, Object> getMilitaryStatistics(String nationId) {
        return legacyService.getMilitaryStatistics(nationId);
    }
    
    @Override
    public double calculateMaintenanceCost(String nationId) {
        return legacyService.calculateMaintenanceCost(nationId);
    }
    
    @Override
    public int getMilitaryCapacity(String nationId) {
        return legacyService.getMilitaryCapacity(nationId);
    }
    
    @Override
    public boolean canRecruitMore(String nationId, int additionalUnits) {
        return legacyService.canRecruitMore(nationId, additionalUnits);
    }
    
    @Override
    public String disbandUnits(String nationId, String unitType, int count) throws Exception {
        return legacyService.disbandUnits(nationId, unitType, count);
    }
    
    @Override
    public String upgradeUnits(String nationId, String unitType, int count, double cost) throws Exception {
        return legacyService.upgradeUnits(nationId, unitType, count, cost);
    }
    
    /**
     * Получить доступ к оригинальному сервису
     * @return оригинальный MilitaryService
     */
    public MilitaryService getLegacyService() {
        return legacyService;
    }
}
