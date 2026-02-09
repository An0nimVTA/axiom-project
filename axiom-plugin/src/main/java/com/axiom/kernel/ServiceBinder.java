package com.axiom.kernel;

public interface ServiceBinder {
    void bind(Class<?> type, Object service);
}
