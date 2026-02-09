package com.axiom.domain.service.military;

import java.util.Map;

/**
 * Интерфейс для SiegeService
 * Определяет контракт для работы с осадами городов
 */
public interface SiegeServiceInterface {
    
    /**
     * Начать осаду города
     * @param cityId ID города
     * @param attackerId ID атакующей нации
     * @param defenderId ID защищающейся нации
     * @return сообщение о результате
     */
    String startSiege(String cityId, String attackerId, String defenderId);
    
    /**
     * Получить статистику осады для нации
     * @param nationId ID нации
     * @return статистика осады
     */
    Map<String, Object> getSiegeStatistics(String nationId);
    
    /**
     * Получить глобальную статистику осад
     * @return глобальная статистика
     */
    Map<String, Object> getGlobalSiegeStatistics();
    
    /**
     * Проверить, находится ли город под осадой
     * @param cityId ID города
     * @return true если город под осадой
     */
    boolean isCityUnderSiege(String cityId);
    
    /**
     * Проверить, находится ли нация в состоянии войны
     * @param nationId ID нации
     * @return true если нация в состоянии войны
     */
    boolean isNationAtWar(String nationId);
}