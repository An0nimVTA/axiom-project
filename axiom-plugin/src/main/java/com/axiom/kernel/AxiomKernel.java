package com.axiom.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class AxiomKernel {
    private final Logger logger;
    private final ServiceRegistry services;
    private final Map<String, KernelModule> modules = new LinkedHashMap<>();
    private List<KernelModule> startupOrder = new ArrayList<>();

    public AxiomKernel(Logger logger) {
        this(logger, null);
    }

    public AxiomKernel(Logger logger, ServiceBinder binder) {
        this.logger = logger != null ? logger : Logger.getLogger(AxiomKernel.class.getName());
        this.services = new ServiceRegistry(binder);
    }

    public ServiceRegistry services() {
        return services;
    }

    public void registerModule(KernelModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Kernel module cannot be null");
        }
        String moduleId = module.id();
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Kernel module id cannot be blank");
        }
        if (modules.containsKey(moduleId)) {
            throw new IllegalStateException("Kernel module already registered: " + moduleId);
        }
        modules.put(moduleId, module);
    }

    public void start() {
        startupOrder = resolveOrder();
        logStartupOrder();
        for (KernelModule module : startupOrder) {
            module.register(services);
        }
        for (KernelModule module : startupOrder) {
            module.onEnable();
        }
    }

    public void stop() {
        List<KernelModule> order = startupOrder.isEmpty()
            ? new ArrayList<>(modules.values())
            : startupOrder;
        List<KernelModule> reverse = new ArrayList<>(order);
        Collections.reverse(reverse);
        for (KernelModule module : reverse) {
            try {
                module.onDisable();
            } catch (Exception ex) {
                logger.warning("Kernel module shutdown failed: " + module.id() + " -> " + ex.getMessage());
            }
        }
    }

    private List<KernelModule> resolveOrder() {
        validateDependencies();
        Map<String, KernelModule> remaining = new HashMap<>(modules);
        List<KernelModule> ordered = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (String moduleId : new ArrayList<>(remaining.keySet())) {
            visit(moduleId, remaining, visiting, visited, ordered);
        }

        return ordered;
    }

    private void visit(String moduleId,
                       Map<String, KernelModule> remaining,
                       Set<String> visiting,
                       Set<String> visited,
                       List<KernelModule> ordered) {
        if (visited.contains(moduleId)) {
            return;
        }
        if (visiting.contains(moduleId)) {
            throw new IllegalStateException("Kernel module dependency cycle detected at: " + moduleId);
        }

        KernelModule module = remaining.get(moduleId);
        if (module == null) {
            throw new IllegalStateException("Kernel module missing: " + moduleId);
        }

        visiting.add(moduleId);
        for (String depId : module.dependencies()) {
            if (!modules.containsKey(depId)) {
                throw new IllegalStateException("Kernel module dependency not registered: " + moduleId + " -> " + depId);
            }
            visit(depId, remaining, visiting, visited, ordered);
        }
        visiting.remove(moduleId);
        visited.add(moduleId);
        ordered.add(module);
    }

    private void validateDependencies() {
        for (KernelModule module : modules.values()) {
            for (String depId : module.dependencies()) {
                if (!modules.containsKey(depId)) {
                    throw new IllegalStateException(
                        "Kernel module dependency not registered: " + module.id() + " -> " + depId
                    );
                }
            }
        }
    }

    private void logStartupOrder() {
        if (startupOrder.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Kernel module startup order: ");
        for (int i = 0; i < startupOrder.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(startupOrder.get(i).id());
        }
        logger.info(sb.toString());
    }
}
