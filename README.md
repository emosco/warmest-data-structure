# Warmest Data Structure

A key-value store that tracks the **warmest** (most recently accessed) key. Every `put`, `get`, or `remove` operation updates the warmest pointer, and `getWarmest` returns the most recently touched key — all in O(1) time.

## Prerequisites

To run this project locally, make sure you have:

- Java `21`
- Maven
- Docker / Docker Desktop
- `curl` (optional, for manual API checks and the smoke test script)

## Profiles

| Profile          | Description                                      |
|------------------|--------------------------------------------------|
| `inlocalmemory`  | In-process HashMap + doubly-linked list (default) |
| `distributed`    | Redis-backed store using Lua scripts for atomicity |

## Running

```bash
# Local memory (default)
mvn spring-boot:run

# Distributed (requires Redis)
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=distributed
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Run 3 distributed servers locally

The easiest way to run the distributed solution on a Mac is with Docker Compose. The compose file starts:

- one shared Redis instance
- one Spring Boot app on `8080`
- one Spring Boot app on `8081`
- one Spring Boot app on `8082`

Run:

```bash
docker compose up --build
```

Then open:

- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)

To verify that all three instances share the same Redis-backed state, send a request to one port and read it back from another:

```bash
curl -X PUT http://localhost:8080/api/v1/warmest/a \
  -H "Content-Type: application/json" \
  -d '{"value":100}'

curl http://localhost:8081/api/v1/warmest/a
curl http://localhost:8082/api/v1/warmest
```

## Running Tests

Run the full automated test suite with:

```bash
mvn test
```

This includes:

- unit tests for the in-memory implementation
- controller tests with `MockMvc`
- distributed integration tests with `@SpringBootTest` and Testcontainers

The distributed integration tests require Docker, because they start a real Redis container during the test run.

For a full local deployment smoke test against Redis plus 3 Spring Boot instances, start the stack with Docker Compose and then run:

```bash
./smoke-test-distributed.sh
```

## API

| Method   | Path                  | Description                        |
|----------|-----------------------|------------------------------------|
| `PUT`    | `/api/v1/warmest/{key}` | Upsert a key with an integer value |
| `GET`    | `/api/v1/warmest/{key}` | Get the value for a key            |
| `DELETE` | `/api/v1/warmest/{key}` | Remove a key                       |
| `GET`    | `/api/v1/warmest`       | Get the warmest (MRU) key          |

## Time Complexity

The public interface is designed so that all operations run in `O(1)` time.

| Operation | Complexity | Notes |
|----------|------------|-------|
| `put(key, value)` | `O(1)` | Constant-time lookup/update plus constant-time recency update |
| `get(key)` | `O(1)` | Constant-time lookup plus constant-time promotion to warmest |
| `remove(key)` | `O(1)` | Constant-time lookup plus constant-time unlinking |
| `getWarmest()` | `O(1)` | Direct access to the current warmest key |

### Why this is `O(1)`

In the in-memory implementation:

- a `HashMap` provides constant-time access to nodes by key
- a doubly-linked list allows constant-time promotion and removal once the node is known
- the `warmest` pointer gives direct access to the current warmest key

In the distributed implementation:

- Redis hashes provide constant-time access by key
- the structure is stored using `values`, `prev`, `next`, and `meta`
- Lua scripts update the required Redis hashes atomically in constant time
- `getWarmest()` is a single Redis `HGET`, so it also remains `O(1)`

The distributed implementation preserves the same asymptotic complexity as the local in-memory implementation while allowing multiple application instances to share the same data structure.


## Testing Strategy

The project uses a layered testing approach:

- unit tests verify the core in-memory data structure behavior quickly and in isolation
- controller tests use `@WebMvcTest` and `MockMvc` to verify routing, validation, status codes, and exception handling
- integration tests use `@SpringBootTest` with the `distributed` profile and Testcontainers to verify the real Redis-backed flow over HTTP
- the smoke test script verifies the full local deployment scenario with Redis and 3 running application instances

This keeps most tests fast while still covering the real distributed behavior end to end.

