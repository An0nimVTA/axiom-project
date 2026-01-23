package com.axiom.economy;

import com.axiom.core.Module;
import com.axiom.core.ServiceContainer;
import com.axiom.economy.repository.EconomyRepository;
import com.axiom.economy.service.EconomyService;
import com.axiom.core.EventPublisher;

import java.util.Arrays;
import java.util.List;

/**
 * Модуль экономики для новой архитектуры
 */
public class EconomyModule implements Module {
    private EconomyService economyService;
    private EconomyRepository economyRepository;
    private boolean enabled = false;
    
    @Override
    public void initialize(ServiceContainer container) {
        // Создание репозитория
        this.economyRepository = new EconomyRepository();
        
        // Получение event publisher из контейнера
        EventPublisher eventPublisher = container.getService(EventPublisher.class);
        if (eventPublisher == null) {
            throw new IllegalStateException("EventPublisher is required but not available");
        }
        
        // Создание сервиса
        this.economyService = new EconomyService(economyRepository, eventPublisher);
        
        // Регистрация сервисов в контейнере
        container.registerService(EconomyService.class, economyService);
        container.registerService(EconomyRepository.class, economyRepository);
        
        this.enabled = true;
    }
    
    @Override
    public void shutdown() {
        // Освобождение ресурсов
        this.economyService = null;
        this.economyRepository = null;
        this.enabled = false;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public String getName() {
        return "EconomyModule";
    }
    
    @Override
    public List<Class<?>> getRequiredServices() {
        return Arrays.asList(EventPublisher.class);
    }
    
    @Override
    public List<Class<?>> getProvidedServices() {
        return Arrays.asList(EconomyService.class, EconomyRepository.class);
    }
    
    public EconomyService getEconomyService() {
        return economyService;
    }
    
    public EconomyRepository getEconomyRepository() {
        return economyRepository;
    }
}