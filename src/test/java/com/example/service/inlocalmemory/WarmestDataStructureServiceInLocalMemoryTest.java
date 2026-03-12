package com.example.service.inlocalmemory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WarmestDataStructureServiceInLocalMemoryTest {

    @Test
    void testPut() {
        WarmestDataStructureServiceInLocalMemory service = new WarmestDataStructureServiceInLocalMemory();

        // New key returns null
        assertNull(service.put("a", 100));

        // Existing key returns old value
        assertEquals(100, service.put("a", 200));

        // Same key, same value returns that value
        assertEquals(200, service.put("a", 200));

        // Multiple new keys each return null
        assertNull(service.put("b", 300));
        assertNull(service.put("c", 400));

        // Re-insert after remove returns null
        service.remove("a");
        assertNull(service.put("a", 500));

        // Verify the new value stuck
        assertEquals(500, service.get("a"));
    }

    @Test
    void testGet() {
        WarmestDataStructureServiceInLocalMemory service = new WarmestDataStructureServiceInLocalMemory();

        // Get on empty structure returns null
        assertNull(service.get("a"));

        service.put("a", 100);
        service.put("b", 200);

        // Get existing key returns its value
        assertEquals(100, service.get("a"));
        assertEquals(200, service.get("b"));

        // Get non-existent key returns null
        assertNull(service.get("z"));

        // Get after remove returns null
        service.remove("a");
        assertNull(service.get("a"));

        // Get on already-warmest node still returns value
        service.put("c", 300);
        assertEquals("c", service.getWarmest());
        assertEquals(300, service.get("c"));
        assertEquals("c", service.getWarmest());
    }

    @Test
    void testRemove() {
        WarmestDataStructureServiceInLocalMemory service = new WarmestDataStructureServiceInLocalMemory();

        // Remove on empty structure returns null
        assertNull(service.remove("a"));

        // Remove only element
        service.put("a", 100);
        assertEquals(100, service.remove("a"));

        // Double remove returns null
        assertNull(service.remove("a"));

        // Remove non-existent key returns null
        assertNull(service.remove("z"));

        // Remove head (oldest) node, warmest unchanged
        service.put("a", 100);
        service.put("b", 200);
        service.put("c", 300);
        assertEquals("c", service.getWarmest());
        assertEquals(100, service.remove("a"));
        assertEquals("c", service.getWarmest());

        // Remove warmest, falls back to prev
        assertEquals(300, service.remove("c"));
        assertEquals("b", service.getWarmest());

        // Remove last remaining
        assertEquals(200, service.remove("b"));
        assertNull(service.getWarmest());
    }

    @Test
    void testGetWarmest() {
        WarmestDataStructureServiceInLocalMemory service = new WarmestDataStructureServiceInLocalMemory();

        // Empty structure
        assertNull(service.getWarmest());

        // After put, warmest is last put key
        service.put("a", 100);
        assertEquals("a", service.getWarmest());
        service.put("b", 200);
        assertEquals("b", service.getWarmest());

        // After get, warmest changes to accessed key
        service.get("a");
        assertEquals("a", service.getWarmest());

        // After get on already-warmest, no change
        service.get("a");
        assertEquals("a", service.getWarmest());

        // After put on existing key, warmest changes to that key
        service.put("b", 999);
        assertEquals("b", service.getWarmest());

        // After removing non-warmest, warmest unchanged
        service.remove("a");
        assertEquals("b", service.getWarmest());

        // After removing warmest, falls back
        service.put("c", 300);
        assertEquals("c", service.getWarmest());
        service.remove("c");
        assertEquals("b", service.getWarmest());

        // After removing all, null
        service.remove("b");
        assertNull(service.getWarmest());
    }

    @Test
    void testBasicFlow() {
        WarmestDataStructureServiceInLocalMemory service = new WarmestDataStructureServiceInLocalMemory();

        assertNull(service.getWarmest());

        assertNull(service.put("a", 100));
        assertEquals("a", service.getWarmest());

        assertEquals(100, service.put("a", 101));
        assertEquals(101, service.get("a"));
        assertEquals("a", service.getWarmest());

        assertEquals(101, service.remove("a"));
        assertNull(service.getWarmest());
    }

    @Test
    void testNonBasicFlow() {
        WarmestDataStructureServiceInLocalMemory service = new WarmestDataStructureServiceInLocalMemory();

        assertNull(service.put("a", 100));
        assertNull(service.put("b", 200));
        assertEquals("b", service.getWarmest());
        assertEquals(100, service.put("a", 101));
        assertEquals(101, service.get("a"));
        assertEquals(200, service.remove("b"));
        assertEquals("a", service.getWarmest());
        assertNull(service.put("b", 300));
        assertEquals("b", service.getWarmest());
    }

    @Test
    void testFullScenario() {
        WarmestDataStructureServiceInLocalMemory service = new WarmestDataStructureServiceInLocalMemory();

        assertNull(service.getWarmest());
        assertNull(service.put("a", 100));
        assertEquals("a", service.getWarmest());
        assertEquals(100, service.put("a", 101));
        assertEquals(101, service.put("a", 101));
        assertEquals(101, service.get("a"));
        assertEquals("a", service.getWarmest());
        assertEquals(101, service.remove("a"));
        assertNull(service.remove("a"));
        assertNull(service.getWarmest());
        assertNull(service.put("a", 100));
        assertNull(service.put("b", 200));
        assertNull(service.put("c", 300));
        assertEquals("c", service.getWarmest());
        assertEquals(200, service.remove("b"));
        assertEquals("c", service.getWarmest());
        assertEquals(300, service.remove("c"));
        assertEquals("a", service.getWarmest());
        assertEquals(100, service.remove("a"));
        assertNull(service.getWarmest());
        assertNull(service.remove("a"));
    }

    @Test
    void testPromoteMiddleNodeThenRemove() {
        WarmestDataStructureServiceInLocalMemory service = new WarmestDataStructureServiceInLocalMemory();

        // List: A <-> B <-> C, warmest = C
        assertNull(service.put("a", 100));
        assertNull(service.put("b", 200));
        assertNull(service.put("c", 300));
        assertEquals("c", service.getWarmest());

        // Promote B to warmest: list should become A <-> C <-> B
        assertEquals(200, service.get("b"));
        assertEquals("b", service.getWarmest());

        // Remove B (warmest), warmest should fall back to C
        assertEquals(200, service.remove("b"));
        assertEquals("c", service.getWarmest());

        // Remove C, warmest should fall back to A
        assertEquals(300, service.remove("c"));
        assertEquals("a", service.getWarmest());

        // Remove A, warmest should be null
        assertEquals(100, service.remove("a"));
        assertNull(service.getWarmest());
    }
}
