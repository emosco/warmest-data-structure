package com.example.service.distributed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

    private static final String BASE_PATH = "/api/v1/warmest";

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
                BASE_PATH + "/a",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 100)),
                Integer.class
        );
        assertEquals(HttpStatus.CREATED, firstPut.getStatusCode());
        assertNull(firstPut.getBody());

        ResponseEntity<Integer> secondPut = restTemplate.exchange(
                BASE_PATH + "/b",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 200)),
                Integer.class
        );
        assertEquals(HttpStatus.CREATED, secondPut.getStatusCode());
        assertNull(secondPut.getBody());

        ResponseEntity<String> warmestAfterPut = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.OK, warmestAfterPut.getStatusCode());
        assertEquals("b", warmestAfterPut.getBody());

        ResponseEntity<Integer> getA = restTemplate.getForEntity(BASE_PATH + "/a", Integer.class);
        assertEquals(HttpStatus.OK, getA.getStatusCode());
        assertEquals(100, getA.getBody());

        ResponseEntity<String> warmestAfterGet = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.OK, warmestAfterGet.getStatusCode());
        assertEquals("a", warmestAfterGet.getBody());

        ResponseEntity<Integer> deleteA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Integer.class
        );
        assertEquals(HttpStatus.OK, deleteA.getStatusCode());
        assertEquals(100, deleteA.getBody());

        ResponseEntity<String> warmestAfterDelete = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.OK, warmestAfterDelete.getStatusCode());
        assertEquals("b", warmestAfterDelete.getBody());
    }

    @Test
    void distributedProfileReturnsProblemDetailsForValidationAndMissingKeys() {
        ResponseEntity<Map> missingKey = restTemplate.getForEntity(BASE_PATH + "/missing", Map.class);
        assertEquals(HttpStatus.OK, missingKey.getStatusCode());
        assertNull(missingKey.getBody());

        ResponseEntity<Map> invalidPut = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of()),
                Map.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidPut.getStatusCode());
        assertEquals("Validation error", invalidPut.getBody().get("title"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), invalidPut.getBody().get("status"));
        assertEquals("value: value is required", invalidPut.getBody().get("detail"));
        assertEquals(BASE_PATH + "/a", invalidPut.getBody().get("path"));
    }

    @Test
    void testFullScenario(){
        ResponseEntity<String> warmest = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.NOT_FOUND, warmest.getStatusCode());

        ResponseEntity<Integer> putA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 100)),
                Integer.class
        );
        assertEquals(HttpStatus.CREATED, putA.getStatusCode());
        assertNull(putA.getBody());

        warmest = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.OK, warmest.getStatusCode());
        assertEquals("a", warmest.getBody());

        putA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 101)),
                Integer.class
        );
        assertEquals(HttpStatus.OK, putA.getStatusCode());
        assertEquals(100, putA.getBody());

        putA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 101)),
                Integer.class
        );
        assertEquals(HttpStatus.OK, putA.getStatusCode());
        assertEquals(101, putA.getBody());

        ResponseEntity<Integer> valueA = restTemplate.getForEntity(BASE_PATH + "/a", Integer.class);
        assertEquals(HttpStatus.OK, valueA.getStatusCode());
        assertEquals(101, valueA.getBody());

        warmest = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.OK, warmest.getStatusCode());
        assertEquals("a", warmest.getBody());

        ResponseEntity<Integer> removeA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Integer.class
        );
        assertEquals(HttpStatus.OK, removeA.getStatusCode());
        assertEquals(101, removeA.getBody());

        removeA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Integer.class
        );
        assertEquals(HttpStatus.OK, removeA.getStatusCode());
        assertNull(removeA.getBody());

        warmest = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.NOT_FOUND, warmest.getStatusCode());

        putA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 100)),
                Integer.class
        );
        assertEquals(HttpStatus.CREATED, putA.getStatusCode());
        assertNull(putA.getBody());

        ResponseEntity<Integer> putB = restTemplate.exchange(
                BASE_PATH + "/b",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 200)),
                Integer.class
        );
        assertEquals(HttpStatus.CREATED, putB.getStatusCode());
        assertNull(putB.getBody());

        ResponseEntity<Integer> putC = restTemplate.exchange(
                BASE_PATH + "/c",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("value", 300)),
                Integer.class
        );
        assertEquals(HttpStatus.CREATED, putC.getStatusCode());
        assertNull(putC.getBody());

        warmest = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.OK, warmest.getStatusCode());
        assertEquals("c", warmest.getBody());

        ResponseEntity<Integer> removeB = restTemplate.exchange(
                BASE_PATH + "/b",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Integer.class
        );
        assertEquals(HttpStatus.OK, removeB.getStatusCode());
        assertEquals(200, removeB.getBody());

        warmest = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.OK, warmest.getStatusCode());
        assertEquals("c", warmest.getBody());

        ResponseEntity<Integer> removeC = restTemplate.exchange(
                BASE_PATH + "/c",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Integer.class
        );
        assertEquals(HttpStatus.OK, removeC.getStatusCode());
        assertEquals(300, removeC.getBody());

        warmest = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.OK, warmest.getStatusCode());
        assertEquals("a", warmest.getBody());

        removeA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Integer.class
        );
        assertEquals(HttpStatus.OK, removeA.getStatusCode());
        assertEquals(100, removeA.getBody());

        warmest = restTemplate.getForEntity(BASE_PATH, String.class);
        assertEquals(HttpStatus.NOT_FOUND, warmest.getStatusCode());

        removeA = restTemplate.exchange(
                BASE_PATH + "/a",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Integer.class
        );
        assertEquals(HttpStatus.OK, removeA.getStatusCode());
        assertNull(removeA.getBody());

    }
}
