package com.axiom.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Менеджер модулей для управления жизненным циклом модулей
 */
public class ModuleManager {
    private final Map<String, Module> modules = new HashMap<>();
    private final ServiceContainer container;
    
    public ModuleManager(ServiceContainer container) {
        this.container = container;
    }
    
    /**
     * Загрузка модуля
     */
    public void loadModule(Module module) {
        String moduleName = module.getName();
        
        if (modules.containsKey(moduleName)) {
            throw new IllegalStateException("Module " + moduleName + " is already loaded");
        }
        
        // Проверка зависимостей
        for (Class<?> requiredService : module.getRequiredServices()) {
            if (!container.hasService(requiredService)) {
                throw new IllegalStateException(
                    "Module " + moduleName + " requires service " + 
                    requiredService.getSimpleName() + " which is not available"
                );
            }
        }
        
        module.initialize(container);
        modules.put(moduleName, module);
        
        System.out.println("Module " + moduleName + " loaded successfully");
    }
    
    /**
     * Выгрузка модуля
     */
    public void unloadModule(String moduleName) {
        Module module = modules.get(moduleName);
        if (module != null) {
            module.shutdown();
            modules.remove(moduleName);
            
            // Удаляем предоставляемые сервисы из контейнера
            for (Class<?> providedService : module.getProvidedServices()) {
                container.unregisterService(providedService);
            }
            
            System.out.println("Module " + moduleName + " unloaded successfully");
        }
    }
    
    /**
     * Получение модуля по имени
     */
    public Module getModule(String moduleName) {
        return modules.get(moduleName);
    }
    
    /**
     * Получение всех загруженных модулей
     */
    public List<Module> getAllModules() {
        return new ArrayList<>(modules.values());
    }
    
    /**
     * Проверка, загружен ли модуль
     */
    public boolean isModuleLoaded(String moduleName) {
        return modules.containsKey(moduleName);
    }
}