package com.axiom.core;

import java.util.List;
import java.util.Optional;

/**
 * Унифицированный интерфейс репозитория для доступа к данным
 */
public interface Repository<T, ID> {
    /**
     * Поиск сущности по ID
     */
    Optional<T> findById(ID id);
    
    /**
     * Получение всех сущностей
     */
    List<T> findAll();
    
    /**
     * Сохранение сущности
     */
    T save(T entity);
    
    /**
     * Удаление сущности по ID
     */
    void delete(ID id);
    
    /**
     * Проверка существования сущности
     */
    boolean exists(ID id);
}