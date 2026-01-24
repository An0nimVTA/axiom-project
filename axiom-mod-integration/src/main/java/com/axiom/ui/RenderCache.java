package com.axiom.ui;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class RenderCache {
    private static final RenderCache INSTANCE = new RenderCache();
    private final Map<String, CachedElement> cache = new ConcurrentHashMap<>();
    private final long CACHE_DURATION = 1000;
    private long lastCleanup = 0;
    private static final long CLEANUP_INTERVAL = 5000;

    public static class CachedElement {
        public final Object data;
        public final long timestamp;

        public CachedElement(Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired(long duration) {
            return System.currentTimeMillis() - timestamp > duration;
        }
    }

    private RenderCache() {}

    public static RenderCache getInstance() {
        return INSTANCE;
    }

    public void put(String key, Object data) {
        autoCleanup();
        cache.put(key, new CachedElement(data));
    }

    public Object get(String key) {
        CachedElement element = cache.get(key);
        if (element != null && !element.isExpired(CACHE_DURATION)) {
            return element.data;
        }
        cache.remove(key);
        return null;
    }

    public void clear() {
        cache.clear();
    }

    private void autoCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL) {
            clearExpired();
            lastCleanup = now;
        }
    }

    public void clearExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(CACHE_DURATION));
    }
}
