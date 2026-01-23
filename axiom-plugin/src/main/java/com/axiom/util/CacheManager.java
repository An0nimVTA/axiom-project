package com.axiom.util;

import com.axiom.AXIOM;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Менеджер кэширования для часто используемых данных
 */
public class CacheManager {
    
    private final AXIOM plugin;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> cleanupTasks = new ConcurrentHashMap<>();
    
    public CacheManager(AXIOM plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Создать новый кэш
     * @param name название кэша
     * @param ttl время жизни в секундах
     * @param maxSize максимальный размер
     * @param <K> тип ключа
     * @param <V> тип значения
     * @return кэш
     */
    public <K, V> Cache<K, V> createCache(String name, long ttl, int maxSize) {
        Cache<K, V> cache = new Cache<>(ttl, maxSize);
        caches.put(name, cache);
        
        // Запланировать очистку кэша
        scheduleCleanup(name, ttl);
        
        return cache;
    }
    
    /**
     * Получить кэш по имени
     * @param name название кэша
     * @param <K> тип ключа
     * @param <V> тип значения
     * @return кэш или null если не найден
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.get(name);
    }
    
    /**
     * Удалить кэш
     * @param name название кэша
     */
    public void removeCache(String name) {
        Cache<?, ?> cache = caches.remove(name);
        BukkitTask task = cleanupTasks.remove(name);
        
        if (cache != null) {
            cache.clear();
        }
        
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Очистить все кэши
     */
    public void clearAllCaches() {
        for (Map.Entry<String, Cache<?, ?>> entry : caches.entrySet()) {
            entry.getValue().clear();
        }
        
        for (BukkitTask task : cleanupTasks.values()) {
            task.cancel();
        }
        
        caches.clear();
        cleanupTasks.clear();
    }
    
    /**
     * Запланировать очистку кэша
     */
    private void scheduleCleanup(String name, long ttl) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Cache<?, ?> cache = caches.get(name);
            if (cache != null) {
                cache.cleanup();
            }
        }, ttl * 20L, ttl * 20L);
        
        cleanupTasks.put(name, task);
    }
    
    /**
     * Кэш с автоматическим удалением устаревших записей
     */
    public static class Cache<K, V> {
        private final long ttl;
        private final int maxSize;
        private final Map<K, CacheEntry<V>> cache = new LinkedHashMap<>();
        
        public Cache(long ttl, int maxSize) {
            this.ttl = ttl * 1000; // Конвертация в миллисекунды
            this.maxSize = maxSize;
        }
        
        /**
         * Получить значение из кэша
         * @param key ключ
         * @return Optional с значением или пустой если не найдено/устарело
         */
        public Optional<V> get(K key) {
            CacheEntry<V> entry = cache.get(key);
            
            if (entry == null) {
                return Optional.empty();
            }
            
            // Проверка устаревания
            if (System.currentTimeMillis() - entry.timestamp > ttl) {
                cache.remove(key);
                return Optional.empty();
            }
            
            return Optional.of(entry.value);
        }
        
        /**
         * Получить значение из кэша или загрузить при отсутствии
         * @param key ключ
         * @param loader функция загрузки
         * @return значение
         */
        public V getOrLoad(K key, Function<K, V> loader) {
            Optional<V> cached = get(key);
            if (cached.isPresent()) {
                return cached.get();
            }
            
            V loaded = loader.apply(key);
            if (loaded != null) {
                put(key, loaded);
            }
            
            return loaded;
        }
        
        /**
         * Поместить значение в кэш
         * @param key ключ
         * @param value значение
         */
        public void put(K key, V value) {
            // Проверка максимального размера
            if (cache.size() >= maxSize) {
                // Удаление самой старой записи
                K oldestKey = cache.keySet().iterator().next();
                cache.remove(oldestKey);
            }
            
            cache.put(key, new CacheEntry<>(value));
        }
        
        /**
         * Удалить значение из кэша
         * @param key ключ
         */
        public void remove(K key) {
            cache.remove(key);
        }
        
        /**
         * Очистить кэш
         */
        public void clear() {
            cache.clear();
        }
        
        /**
         * Получить размер кэша
         * @return размер кэша
         */
        public int size() {
            return cache.size();
        }
        
        /**
         * Очистка устаревших записей
         */
        public void cleanup() {
            long currentTime = System.currentTimeMillis();
            cache.entrySet().removeIf(entry -> 
                currentTime - entry.getValue().timestamp > ttl
            );
        }
        
        /**
         * Запись кэша
         */
        private static class CacheEntry<V> {
            final V value;
            final long timestamp;
            
            CacheEntry(V value) {
                this.value = value;
                this.timestamp = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Кэш с вечной продолжительностью жизни
     */
    public static class PermanentCache<K, V> {
        private final Map<K, V> cache = new ConcurrentHashMap<>();
        private final int maxSize;
        
        public PermanentCache(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public Optional<V> get(K key) {
            return Optional.ofNullable(cache.get(key));
        }
        
        public V getOrLoad(K key, Function<K, V> loader) {
            V cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
            
            V loaded = loader.apply(key);
            if (loaded != null) {
                put(key, loaded);
            }
            
            return loaded;
        }
        
        public void put(K key, V value) {
            if (cache.size() >= maxSize) {
                // Удаление случайной записи при превышении лимита
                K randomKey = cache.keySet().iterator().next();
                cache.remove(randomKey);
            }
            
            cache.put(key, value);
        }
        
        public void remove(K key) {
            cache.remove(key);
        }
        
        public void clear() {
            cache.clear();
        }
        
        public int size() {
            return cache.size();
        }
    }
}
