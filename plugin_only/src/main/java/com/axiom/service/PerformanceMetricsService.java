package com.axiom.service;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance metrics and monitoring service.
 * Tracks execution times, warns about slow operations.
 * Integrates with Spark profiler if available.
 */
public class PerformanceMetricsService {
    private final AXIOM plugin;
    private final Map<String, Long> operationTimes = new ConcurrentHashMap<>(); // operation -> total time (ms)
    private final Map<String, Integer> operationCounts = new ConcurrentHashMap<>(); // operation -> count
    private final List<SlowOperation> slowOperations = new ArrayList<>(); // Recent slow operations
    
    public static class SlowOperation {
        String operation;
        long duration; // ms
        long timestamp;
        String details;
        
        SlowOperation(String op, long dur, String det) {
            this.operation = op;
            this.duration = dur;
            this.timestamp = System.currentTimeMillis();
            this.details = det;
        }
    }
    
    public PerformanceMetricsService(AXIOM plugin) {
        this.plugin = plugin;
        
        // Report metrics every 10 minutes
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::reportMetrics, 
            20 * 60 * 10, 20 * 60 * 10);
        
        // Clean old slow operations every hour
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanSlowOperations, 
            20 * 60 * 60, 20 * 60 * 60);
    }
    
    /**
     * Measure operation execution time.
     */
    public <T> T measure(String operationName, java.util.function.Supplier<T> operation) {
        long start = System.nanoTime();
        try {
            return operation.get();
        } finally {
            long duration = (System.nanoTime() - start) / 1_000_000; // Convert to ms
            recordOperation(operationName, duration, "");
        }
    }
    
    /**
     * Measure operation execution time (void).
     */
    public void measure(String operationName, Runnable operation) {
        long start = System.nanoTime();
        try {
            operation.run();
        } finally {
            long duration = (System.nanoTime() - start) / 1_000_000;
            recordOperation(operationName, duration, "");
        }
    }
    
    /**
     * Record operation timing.
     */
    private void recordOperation(String operation, long durationMs, String details) {
        operationTimes.merge(operation, durationMs, Long::sum);
        operationCounts.merge(operation, 1, Integer::sum);
        
        // Warn about slow operations
        if (durationMs > 100) { // More than 100ms
            synchronized (slowOperations) {
                slowOperations.add(new SlowOperation(operation, durationMs, details));
                if (slowOperations.size() > 100) {
                    slowOperations.remove(0); // Keep last 100
                }
            }
            
            if (durationMs > 500) { // More than 500ms - critical
                plugin.getLogger().warning(String.format(
                    "[PERFORMANCE] Медленная операция: %s заняла %d ms. Детали: %s",
                    operation, durationMs, details
                ));
            }
        }
    }
    
    /**
     * Get average time for operation.
     */
    public double getAverageTime(String operation) {
        Long total = operationTimes.get(operation);
        Integer count = operationCounts.get(operation);
        if (total == null || count == null || count == 0) return 0.0;
        return (double) total / count;
    }
    
    /**
     * Get total time for operation.
     */
    public long getTotalTime(String operation) {
        return operationTimes.getOrDefault(operation, 0L);
    }
    
    /**
     * Get count of operations.
     */
    public int getCount(String operation) {
        return operationCounts.getOrDefault(operation, 0);
    }
    
    /**
     * Get recent slow operations.
     */
    public List<SlowOperation> getSlowOperations() {
        synchronized (slowOperations) {
            return new ArrayList<>(slowOperations);
        }
    }
    
    /**
     * Report metrics to console and Spark if available.
     */
    private void reportMetrics() {
        plugin.getLogger().info("=== AXIOM Performance Metrics ===");
        
        // Sort by total time
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(operationTimes.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        plugin.getLogger().info("Топ-10 операций по времени:");
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            String op = entry.getKey();
            long total = entry.getValue();
            int count = operationCounts.getOrDefault(op, 1);
            double avg = (double) total / count;
            plugin.getLogger().info(String.format(
                "  %s: %d ms (всего), %.2f ms (среднее), %d вызовов",
                op, total, avg, count
            ));
        }
        
        // Report slow operations
        synchronized (slowOperations) {
            if (!slowOperations.isEmpty()) {
                plugin.getLogger().info("Медленные операции (>100ms): " + slowOperations.size());
            }
        }
        
        // Try to integrate with Spark if available
        if (isSparkAvailable()) {
            reportToSpark();
        }
    }
    
    private boolean isSparkAvailable() {
        try {
            Class.forName("me.lucko.spark.Spark");
            return Bukkit.getPluginManager().getPlugin("spark") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void reportToSpark() {
        // Spark integration would go here
        // For now, just log that Spark is available
        plugin.getLogger().info("Spark доступен. Метрики можно просмотреть через /spark profiler");
    }
    
    private void cleanSlowOperations() {
        synchronized (slowOperations) {
            long cutoff = System.currentTimeMillis() - 60 * 60_000L; // 1 hour
            slowOperations.removeIf(op -> op.timestamp < cutoff);
        }
    }
    
    /**
     * Reset all metrics.
     */
    public void resetMetrics() {
        operationTimes.clear();
        operationCounts.clear();
        synchronized (slowOperations) {
            slowOperations.clear();
        }
        plugin.getLogger().info("Метрики производительности сброшены.");
    }
    
    /**
     * Get comprehensive performance statistics.
     */
    public synchronized Map<String, Object> getPerformanceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalOperations", operationCounts.values().stream().mapToInt(Integer::intValue).sum());
        stats.put("totalTime", operationTimes.values().stream().mapToLong(Long::longValue).sum());
        
        // Top operations by time
        List<Map.Entry<String, Long>> sortedByTime = new ArrayList<>(operationTimes.entrySet());
        sortedByTime.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        List<Map<String, Object>> topByTime = new ArrayList<>();
        for (int i = 0; i < Math.min(10, sortedByTime.size()); i++) {
            Map.Entry<String, Long> entry = sortedByTime.get(i);
            Map<String, Object> opData = new HashMap<>();
            opData.put("operation", entry.getKey());
            opData.put("totalTime", entry.getValue());
            opData.put("count", operationCounts.getOrDefault(entry.getKey(), 0));
            opData.put("averageTime", getAverageTime(entry.getKey()));
            topByTime.add(opData);
        }
        stats.put("topByTime", topByTime);
        
        // Top operations by count
        List<Map.Entry<String, Integer>> sortedByCount = new ArrayList<>(operationCounts.entrySet());
        sortedByCount.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        List<Map<String, Object>> topByCount = new ArrayList<>();
        for (int i = 0; i < Math.min(10, sortedByCount.size()); i++) {
            Map.Entry<String, Integer> entry = sortedByCount.get(i);
            Map<String, Object> opData = new HashMap<>();
            opData.put("operation", entry.getKey());
            opData.put("count", entry.getValue());
            opData.put("totalTime", operationTimes.getOrDefault(entry.getKey(), 0L));
            opData.put("averageTime", getAverageTime(entry.getKey()));
            topByCount.add(opData);
        }
        stats.put("topByCount", topByCount);
        
        // Slow operations
        synchronized (slowOperations) {
            stats.put("slowOperationsCount", slowOperations.size());
            List<Map<String, Object>> slowOpsList = new ArrayList<>();
            for (SlowOperation op : slowOperations.stream()
                    .sorted((a, b) -> Long.compare(b.duration, a.duration))
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList())) {
                Map<String, Object> opData = new HashMap<>();
                opData.put("operation", op.operation);
                opData.put("duration", op.duration);
                opData.put("details", op.details);
                opData.put("timestamp", op.timestamp);
                slowOpsList.add(opData);
            }
            stats.put("recentSlowOperations", slowOpsList);
        }
        
        // Spark integration status
        stats.put("sparkAvailable", isSparkAvailable());
        
        return stats;
    }
    
    /**
     * Get statistics for specific operation.
     */
    public synchronized Map<String, Object> getOperationStatistics(String operation) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("operation", operation);
        stats.put("totalTime", getTotalTime(operation));
        stats.put("count", getCount(operation));
        stats.put("averageTime", getAverageTime(operation));
        
        // Slow instances of this operation
        synchronized (slowOperations) {
            List<SlowOperation> slowOps = slowOperations.stream()
                .filter(op -> op.operation.equals(operation))
                .sorted((a, b) -> Long.compare(b.duration, a.duration))
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> slowList = new ArrayList<>();
            for (SlowOperation op : slowOps) {
                Map<String, Object> opData = new HashMap<>();
                opData.put("duration", op.duration);
                opData.put("details", op.details);
                opData.put("timestamp", op.timestamp);
                slowList.add(opData);
            }
            stats.put("slowInstances", slowList);
        }
        
        return stats;
    }
    
    /**
     * Get performance rating.
     */
    public synchronized String getPerformanceRating() {
        long totalTime = operationTimes.values().stream().mapToLong(Long::longValue).sum();
        int totalCount = operationCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalCount == 0) return "НЕТ ДАННЫХ";
        
        double avgTime = (double) totalTime / totalCount;
        
        synchronized (slowOperations) {
            long criticalSlow = slowOperations.stream()
                .filter(op -> op.duration > 500)
                .count();
            
            if (criticalSlow > 100) return "КРИТИЧЕСКИЕ ПРОБЛЕМЫ";
            if (criticalSlow > 50) return "СЕРЬЁЗНЫЕ ПРОБЛЕМЫ";
            if (criticalSlow > 10) return "УМЕРЕННЫЕ ПРОБЛЕМЫ";
            if (avgTime > 50) return "МЕДЛЕННО";
            if (avgTime > 20) return "НОРМАЛЬНО";
            return "ОТЛИЧНО";
        }
    }
    
    /**
     * Get global performance statistics (alias for getPerformanceStatistics for consistency).
     */
    public synchronized Map<String, Object> getGlobalPerformanceStatistics() {
        return getPerformanceStatistics();
    }
}

