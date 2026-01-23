package com.axiom.service;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple Service Locator pattern implementation to decouple services from the main plugin class.
 * This allows for easier management and access to shared service instances throughout the plugin.
 */
public final class ServiceLocator {

    private static final Map<Class<?>, Object> services = new HashMap<>();

    private ServiceLocator() {
        // Private constructor to prevent instantiation.
    }

    /**
     * Registers a service instance.
     *
     * @param serviceClass The class of the service to register.
     * @param serviceInstance The instance of the service.
     * @param <T> The type of the service.
     */
    public static <T> void register(Class<T> serviceClass, T serviceInstance) {
        services.put(serviceClass, serviceInstance);
    }

    /**
     * Retrieves a service instance.
     *
     * @param serviceClass The class of the service to retrieve.
     * @param <T> The type of the service.
     * @return The registered instance of the service.
     * @throws IllegalStateException if the service is not found.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> serviceClass) {
        T service = (T) services.get(serviceClass);
        if (service == null) {
            throw new IllegalStateException("Service not found: " + serviceClass.getName());
        }
        return service;
    }

    /**
     * Clears all registered services. Should be called on plugin disable.
     */
    public static void clear() {
        services.clear();
    }
}
