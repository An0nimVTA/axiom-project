package com.axiom.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServiceRegistry {
    private final Map<Class<?>, Object> byType = new HashMap<>();
    private final Map<String, Object> byName = new HashMap<>();
    private final ServiceBinder binder;

    public ServiceRegistry() {
        this(null);
    }

    public ServiceRegistry(ServiceBinder binder) {
        this.binder = binder;
    }

    public <T> void register(Class<T> type, T service) {
        if (type == null || service == null) {
            return;
        }
        if (byType.containsKey(type)) {
            throw new IllegalStateException("Service already registered: " + type.getName());
        }
        byType.put(type, service);
        if (binder != null) {
            binder.bind(type, service);
        }
    }

    public void register(String key, Object service) {
        if (key == null || key.isEmpty() || service == null) {
            return;
        }
        if (byName.containsKey(key)) {
            throw new IllegalStateException("Service key already registered: " + key);
        }
        byName.put(key, service);
    }

    public <T> Optional<T> resolve(Class<T> type) {
        Object service = byType.get(type);
        if (service == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(service));
    }

    public <T> Optional<T> resolve(String key, Class<T> type) {
        Object service = byName.get(key);
        if (service == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(service));
    }

    public <T> T require(Class<T> type) {
        return resolve(type).orElseThrow(
            () -> new IllegalStateException("Required service not registered: " + type.getName())
        );
    }

    public <T> T require(String key, Class<T> type) {
        return resolve(key, type).orElseThrow(
            () -> new IllegalStateException("Required service not registered: " + key + " (" + type.getName() + ")")
        );
    }

    public boolean contains(Class<?> type) {
        return byType.containsKey(type);
    }
}
