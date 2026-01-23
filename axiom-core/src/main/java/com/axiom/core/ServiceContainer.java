package com.axiom.core;

import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.Map;

/**
 * Контейнер для управления сервисами и зависимостями
 */
public class ServiceContainer {
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Plugin plugin;
    
    public ServiceContainer(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Регистрация сервиса в контейнере
     */
    public <T> void registerService(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
    }
    
    /**
     * Получение сервиса из контейнера
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }
    
    /**
     * Проверка наличия сервиса в контейнере
     */
    public boolean hasService(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }
    
    /**
     * Удаление сервиса из контейнера
     */
    public void unregisterService(Class<?> serviceClass) {
        services.remove(serviceClass);
    }
    
    public Plugin getPlugin() {
        return plugin;
    }
}