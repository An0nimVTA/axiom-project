package com.axiom.military;

import com.axiom.core.Module;
import com.axiom.core.ServiceContainer;
import com.axiom.military.repository.MilitaryRepository;
import com.axiom.military.service.MilitaryService;
import com.axiom.core.EventPublisher;

import java.util.Arrays;
import java.util.List;

/**
 * Модуль военных сил для новой архитектуры
 */
public class MilitaryModule implements Module {
    private MilitaryService militaryService;
    private MilitaryRepository militaryRepository;
    private boolean enabled = false;
    
    @Override
    public void initialize(ServiceContainer container) {
        // Создание репозитория
        this.militaryRepository = new MilitaryRepository();
        
        // Получение event publisher из контейнера
        EventPublisher eventPublisher = container.getService(EventPublisher.class);
        if (eventPublisher == null) {
            throw new IllegalStateException("EventPublisher is required but not available");
        }
        
        // Создание сервиса
        this.militaryService = new MilitaryService(militaryRepository, eventPublisher);
        
        // Регистрация сервисов в контейнере
        container.registerService(MilitaryService.class, militaryService);
        container.registerService(MilitaryRepository.class, militaryRepository);
        
        this.enabled = true;
    }
    
    @Override
    public void shutdown() {
        // Освобождение ресурсов
        this.militaryService = null;
        this.militaryRepository = null;
        this.enabled = false;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public String getName() {
        return "MilitaryModule";
    }
    
    @Override
    public List<Class<?>> getRequiredServices() {
        return Arrays.asList(EventPublisher.class);
    }
    
    @Override
    public List<Class<?>> getProvidedServices() {
        return Arrays.asList(MilitaryService.class, MilitaryRepository.class);
    }
    
    public MilitaryService getMilitaryService() {
        return militaryService;
    }
    
    public MilitaryRepository getMilitaryRepository() {
        return militaryRepository;
    }
}