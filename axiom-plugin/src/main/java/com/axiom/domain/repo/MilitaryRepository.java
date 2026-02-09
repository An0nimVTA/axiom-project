package com.axiom.domain.repo;

import com.axiom.domain.model.MilitaryData;
import java.util.Optional;

/**
 * Репозиторий для работы с военными данными
 */
public interface MilitaryRepository {
    
    /**
     * Найти военные данные по ID нации
     * @param nationId ID нации
     * @return Optional с военными данными или пустой если не найдены
     */
    Optional<MilitaryData> findByNationId(String nationId);
    
    /**
     * Сохранить военные данные
     * @param militaryData военные данные для сохранения
     */
    void save(MilitaryData militaryData);
    
    /**
     * Удалить военные данные
     * @param nationId ID нации
     */
    void delete(String nationId);
    
    /**
     * Проверить существование военных данных
     * @param nationId ID нации
     * @return true если данные существуют
     */
    boolean exists(String nationId);
    
    /**
     * Найти все военные данные
     * @return список всех военных данных
     */
    java.util.List<MilitaryData> findAll();
}