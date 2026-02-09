package com.axiom.domain.repo;

import com.axiom.domain.model.Nation;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с нациями
 */
public interface NationRepository {
    
    /**
     * Найти нацию по ID
     * @param id ID нации
     * @return Optional с нацией или пустой если не найдена
     */
    Optional<Nation> findById(String id);
    
    /**
     * Найти все нации
     * @return список всех наций
     */
    List<Nation> findAll();
    
    /**
     * Сохранить нацию
     * @param nation нация для сохранения
     */
    void save(Nation nation);
    
    /**
     * Удалить нацию
     * @param id ID нации для удаления
     */
    void delete(String id);
    
    /**
     * Найти нацию по имени
     * @param name имя нации
     * @return Optional с нацией или пустой если не найдена
     */
    Optional<Nation> findByName(String name);
    
    /**
     * Найти нации по лидеру
     * @param leaderId ID лидера
     * @return список наций с данным лидером
     */
    List<Nation> findByLeader(java.util.UUID leaderId);
    
    /**
     * Проверить существование нации
     * @param id ID нации
     * @return true если нация существует
     */
    boolean exists(String id);
}
