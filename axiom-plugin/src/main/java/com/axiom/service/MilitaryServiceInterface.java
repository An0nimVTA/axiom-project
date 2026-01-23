package com.axiom.service;

import java.util.Map;

/**
 * Интерфейс для MilitaryService
 * Определяет контракт для работы с военными силами
 */
public interface MilitaryServiceInterface {
    
    /**
     * Нанять военные единицы
     * @param nationId ID нации
     * @param unitType тип войск
     * @param count количество
     * @param cost стоимость за единицу
     * @return сообщение о результате
     */
    String recruitUnits(String nationId, String unitType, int count, double cost);
    
    /**
     * Получить боевую мощь нации
     * @param nationId ID нации
     * @return боевая мощь
     */
    double getMilitaryStrength(String nationId);
    
    /**
     * Получить статистику военных сил
     * @param nationId ID нации
     * @return карта со статистикой
     */
    Map<String, Object> getMilitaryStatistics(String nationId);
    
    /**
     * Рассчитать стоимость содержания войск
     * @param nationId ID нации
     * @return стоимость содержания
     */
    double calculateMaintenanceCost(String nationId);
    
    /**
     * Получить максимальную военную мощность
     * @param nationId ID нации
     * @return максимальная мощность
     */
    int getMilitaryCapacity(String nationId);
    
    /**
     * Проверить возможность найма дополнительных войск
     * @param nationId ID нации
     * @param additionalUnits дополнительные единицы
     * @return true если можно нанять
     */
    boolean canRecruitMore(String nationId, int additionalUnits);
    
    /**
     * Расформировать военные единицы
     * @param nationId ID нации
     * @param unitType тип войск
     * @param count количество
     * @return сообщение о результате
     */
    String disbandUnits(String nationId, String unitType, int count) throws Exception;
    
    /**
     * Улучшить военные единицы
     * @param nationId ID нации
     * @param unitType тип войск
     * @param count количество
     * @param cost стоимость улучшения
     * @return сообщение о результате
     */
    String upgradeUnits(String nationId, String unitType, int count, double cost) throws Exception;
}