package com.axiom.service.adapter;

import com.axiom.service.SiegeService;
import com.axiom.service.SiegeServiceInterface;
import com.axiom.AXIOM;
import com.axiom.service.NationManager;
import java.util.Map;

/**
 * Адаптер для SiegeService
 * Позволяет использовать существующую реализацию через новый интерфейс
 */
public class SiegeServiceAdapter implements SiegeServiceInterface {
    
    private final SiegeService legacyService;
    
    /**
     * Создать адаптер для существующего SiegeService
     * @param plugin основной плагин
     */
    public SiegeServiceAdapter(AXIOM plugin) {
        SiegeService existing = plugin.getSiegeService();
        if (existing != null) {
            this.legacyService = existing;
        } else {
            NationManager nationManager = plugin.getNationManager();
            this.legacyService = new SiegeService(plugin, nationManager);
        }
    }
    
    /**
     * Создать адаптер для существующего экземпляра
     * @param legacyService существующий сервис
     */
    public SiegeServiceAdapter(SiegeService legacyService) {
        this.legacyService = legacyService;
    }
    
    @Override
    public String startSiege(String cityId, String attackerId, String defenderId) {
        return legacyService.startSiege(cityId, attackerId, defenderId);
    }
    
    @Override
    public Map<String, Object> getSiegeStatistics(String nationId) {
        return legacyService.getSiegeStatistics(nationId);
    }
    
    @Override
    public Map<String, Object> getGlobalSiegeStatistics() {
        return legacyService.getGlobalSiegeStatistics();
    }
    
    @Override
    public boolean isCityUnderSiege(String cityId) {
        // Реализация через проверку активных осад
        try {
            // Это временная реализация - в будущем нужно будет улучшить
            Map<String, Object> globalStats = legacyService.getGlobalSiegeStatistics();
            return ((Map<String, Integer>)globalStats.get("siegesByDefender")).containsKey(cityId);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isNationAtWar(String nationId) {
        // Реализация через проверку активных войн
        try {
            Map<String, Object> stats = legacyService.getSiegeStatistics(nationId);
            int activeAsAttacker = (int) stats.getOrDefault("activeSiegesAsAttacker", 0);
            int activeAsDefender = (int) stats.getOrDefault("activeSiegesAsDefender", 0);
            return activeAsAttacker > 0 || activeAsDefender > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Получить доступ к оригинальному сервису
     * @return оригинальный SiegeService
     */
    public SiegeService getLegacyService() {
        return legacyService;
    }
}
