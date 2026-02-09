package com.axiom.kernel;

import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceRegistryTest {

    @Test
    public void registerAndResolveByType() {
        ServiceRegistry registry = new ServiceRegistry();
        String service = "alpha";
        registry.register(String.class, service);

        assertTrue(registry.resolve(String.class).isPresent());
        assertEquals("alpha", registry.require(String.class));
    }

    @Test
    public void requireMissingThrows() {
        ServiceRegistry registry = new ServiceRegistry();
        try {
            registry.require(Integer.class);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Required service"));
        }
    }

    @Test
    public void duplicateRegistrationThrows() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.register(String.class, "first");
        try {
            registry.register(String.class, "second");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }
    }
}
