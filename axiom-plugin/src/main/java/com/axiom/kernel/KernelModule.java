package com.axiom.kernel;

import java.util.Collections;
import java.util.Set;

public interface KernelModule {
    String id();

    default Set<String> dependencies() {
        return Collections.emptySet();
    }

    void register(ServiceRegistry services);

    default void onEnable() {
    }

    default void onDisable() {
    }
}
