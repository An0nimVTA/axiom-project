package com.axiom.kernel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class AxiomKernelTest {

    @Test
    public void ordersModulesAndRunsLifecycle() {
        Logger logger = Logger.getLogger("kernel-test");
        AxiomKernel kernel = new AxiomKernel(logger);
        List<String> events = new ArrayList<>();

        kernel.registerModule(new TestModule("state", Set.of(), events));
        kernel.registerModule(new TestModule("industry", Set.of("state"), events));
        kernel.registerModule(new TestModule("military", Set.of("state", "industry"), events));

        kernel.start();

        List<String> expected = List.of(
            "register:state",
            "register:industry",
            "register:military",
            "enable:state",
            "enable:industry",
            "enable:military"
        );
        assertEquals(expected, events);
    }

    @Test
    public void missingDependencyThrows() {
        Logger logger = Logger.getLogger("kernel-test");
        AxiomKernel kernel = new AxiomKernel(logger);
        kernel.registerModule(new TestModule("military", Set.of("state"), new ArrayList<>()));

        try {
            kernel.start();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void cycleDetectionThrows() {
        Logger logger = Logger.getLogger("kernel-test");
        AxiomKernel kernel = new AxiomKernel(logger);
        kernel.registerModule(new TestModule("a", Set.of("b"), new ArrayList<>()));
        kernel.registerModule(new TestModule("b", Set.of("a"), new ArrayList<>()));

        try {
            kernel.start();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void duplicateModuleIdThrows() {
        Logger logger = Logger.getLogger("kernel-test");
        AxiomKernel kernel = new AxiomKernel(logger);
        kernel.registerModule(new TestModule("state", Set.of(), new ArrayList<>()));

        try {
            kernel.registerModule(new TestModule("state", Set.of(), new ArrayList<>()));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    private static final class TestModule implements KernelModule {
        private final String id;
        private final Set<String> deps;
        private final List<String> events;

        private TestModule(String id, Set<String> deps, List<String> events) {
            this.id = id;
            this.deps = deps;
            this.events = events;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Set<String> dependencies() {
            return deps;
        }

        @Override
        public void register(ServiceRegistry services) {
            events.add("register:" + id);
        }

        @Override
        public void onEnable() {
            events.add("enable:" + id);
        }
    }
}
