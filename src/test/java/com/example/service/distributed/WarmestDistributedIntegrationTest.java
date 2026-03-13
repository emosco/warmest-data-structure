package com.example.service.distributed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("distributed")
@Testcontainers
class WarmestDistributedIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Autowired
    private WarmestRedisKeys redisKeys;

    @BeforeEach
    void clearRedisState() {
        redisTemplate.delete(redisKeys.all());
    }

    @Test
    void distributedProfileSupportsWarmestFlowThroughRealHttpApi() {
        ResponseEntity<Integer> firstPut = restTemplate.exchange(
                "/api/v1/warmest/a",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 100)),
                Integer.class
        );
        assertEquals(HttpStatusCode.valueOf(201), firstPut.getStatusCode());
        assertNull(firstPut.getBody());

        ResponseEntity<Integer> secondPut = restTemplate.exchange(
                "/api/v1/warmest/b",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 200)),
                Integer.class
        );
        assertEquals(HttpStatusCode.valueOf(201), secondPut.getStatusCode());
        assertNull(secondPut.getBody());

        ResponseEntity<String> warmestAfterPut = restTemplate.getForEntity("/api/v1/warmest", String.class);
        assertEquals(HttpStatusCode.valueOf(200), warmestAfterPut.getStatusCode());
        assertEquals("b", warmestAfterPut.getBody());

        ResponseEntity<Integer> getA = restTemplate.getForEntity("/api/v1/warmest/a", Integer.class);
        assertEquals(HttpStatusCode.valueOf(200), getA.getStatusCode());
        assertEquals(100, getA.getBody());

        ResponseEntity<String> warmestAfterGet = restTemplate.getForEntity("/api/v1/warmest", String.class);
        assertEquals(HttpStatusCode.valueOf(200), warmestAfterGet.getStatusCode());
        assertEquals("a", warmestAfterGet.getBody());

        ResponseEntity<Integer> deleteA = restTemplate.exchange(
                "/api/v1/warmest/a",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Integer.class
        );
        assertEquals(HttpStatusCode.valueOf(200), deleteA.getStatusCode());
        assertEquals(100, deleteA.getBody());

        ResponseEntity<String> warmestAfterDelete = restTemplate.getForEntity("/api/v1/warmest", String.class);
        assertEquals(HttpStatusCode.valueOf(200), warmestAfterDelete.getStatusCode());
        assertEquals("b", warmestAfterDelete.getBody());
    }

    @Test
    void distributedProfileReturnsProblemDetailsForValidationAndMissingKeys() {
        ResponseEntity<Map> missingKey = restTemplate.getForEntity("/api/v1/warmest/missing", Map.class);
        assertEquals(HttpStatusCode.valueOf(200), missingKey.getStatusCode());
        assertNull(missingKey.getBody());

        ResponseEntity<Map> invalidPut = restTemplate.exchange(
                "/api/v1/warmest/a",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of()),
                Map.class
        );
        assertEquals(HttpStatusCode.valueOf(400), invalidPut.getStatusCode());
        assertEquals("Validation error", invalidPut.getBody().get("title"));
        assertEquals(400, invalidPut.getBody().get("status"));
        assertEquals("value: value is required", invalidPut.getBody().get("detail"));
        assertEquals("/api/v1/warmest/a", invalidPut.getBody().get("path"));
    }
}
