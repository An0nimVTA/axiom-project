package com.axiom.nations;

import com.axiom.core.Module;
import com.axiom.core.ServiceContainer;
import com.axiom.nations.repository.NationRepository;
import com.axiom.nations.service.NationService;
import com.axiom.core.EventPublisher;

import java.util.Arrays;
import java.util.List;

/**
 * Модуль наций для новой архитектуры
 */
public class NationsModule implements Module {
    private NationService nationService;
    private NationRepository nationRepository;
    private boolean enabled = false;
    
    @Override
    public void initialize(ServiceContainer container) {
        // Создание репозитория
        this.nationRepository = new NationRepository();
        
        // Получение event publisher из контейнера
        EventPublisher eventPublisher = container.getService(EventPublisher.class);
        if (eventPublisher == null) {
            throw new IllegalStateException("EventPublisher is required but not available");
        }
        
        // Создание сервиса
        this.nationService = new NationService(nationRepository, eventPublisher);
        
        // Регистрация сервисов в контейнере
        container.registerService(NationService.class, nationService);
        container.registerService(NationRepository.class, nationRepository);
        
        this.enabled = true;
    }
    
    @Override
    public void shutdown() {
        // Освобождение ресурсов
        this.nationService = null;
        this.nationRepository = null;
        this.enabled = false;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public String getName() {
        return "NationsModule";
    }
    
    @Override
    public List<Class<?>> getRequiredServices() {
        return Arrays.asList(EventPublisher.class);
    }
    
    @Override
    public List<Class<?>> getProvidedServices() {
        return Arrays.asList(NationService.class, NationRepository.class);
    }
    
    public NationService getNationService() {
        return nationService;
    }
    
    public NationRepository getNationRepository() {
        return nationRepository;
    }
}