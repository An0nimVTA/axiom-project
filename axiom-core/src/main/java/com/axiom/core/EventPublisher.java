package com.axiom.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Публикатор событий для event-driven архитектуры
 */
public class EventPublisher {
    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new HashMap<>();
    
    /**
     * Подписка на событие
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>())
                  .add((Consumer<Object>) handler);
    }
    
    /**
     * Публикация события
     */
    public <T> void publish(T event) {
        List<Consumer<Object>> handlers = subscribers.get(event.getClass());
        if (handlers != null) {
            handlers.forEach(handler -> handler.accept(event));
        }
    }
    
    /**
     * Отписка от события
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        List<Consumer<Object>> handlers = subscribers.get(eventType);
        if (handlers != null) {
            handlers.remove((Consumer<Object>) handler);
        }
    }
}