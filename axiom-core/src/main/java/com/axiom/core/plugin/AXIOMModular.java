package com.axiom.core.plugin;

import com.axiom.core.ServiceContainer;
import com.axiom.core.ModuleManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Новый основной класс плагина с модульной архитектурой
 */
public class AXIOMModular extends JavaPlugin {
    private ServiceContainer container;
    private ModuleManager moduleManager;
    
    @Override
    public void onEnable() {
        // Инициализация контейнера сервисов
        this.container = new ServiceContainer(this);
        
        // Инициализация менеджера модулей
        this.moduleManager = new ModuleManager(container);
        
        // Загрузка ядра
        initializeCore();
        
        // Загрузка модулей (в реальном приложении - из конфигурации)
        loadModules();
        
        getLogger().info("AXIOM Modular Plugin enabled successfully");
    }
    
    @Override
    public void onDisable() {
        // Выгрузка всех модулей
        unloadModules();
        
        getLogger().info("AXIOM Modular Plugin disabled");
    }
    
    private void initializeCore() {
        // Регистрация основных компонентов
        container.registerService(ServiceContainer.class, container);
        container.registerService(ModuleManager.class, moduleManager);
    }
    
    private void loadModules() {
        // В реальном приложении модули будут загружаться из конфигурации
        // или автоматически обнаруживаться
    }
    
    private void unloadModules() {
        // Выгрузка всех модулей
        moduleManager.getAllModules().forEach(module -> 
            moduleManager.unloadModule(module.getName()));
    }
    
    public ServiceContainer getContainer() {
        return container;
    }
    
    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}