package com.axiom.core;

import java.util.List;

/**
 * Интерфейс модуля для новой архитектуры
 */
public interface Module {
    /**
     * Инициализация модуля
     */
    void initialize(ServiceContainer container);
    
    /**
     * Завершение работы модуля
     */
    void shutdown();
    
    /**
     * Проверка, включен ли модуль
     */
    boolean isEnabled();
    
    /**
     * Имя модуля
     */
    String getName();
    
    /**
     * Список требуемых сервисов для работы модуля
     */
    List<Class<?>> getRequiredServices();
    
    /**
     * Список предоставляемых модулем сервисов
     */
    List<Class<?>> getProvidedServices();
}