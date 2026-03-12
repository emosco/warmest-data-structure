# Warmest Data Structure

A key-value store that tracks the **warmest** (most recently accessed) key. Every `put`, `get`, or `remove` operation updates the warmest pointer, and `getWarmest` returns the most recently touched key — all in O(1) time.

## Profiles

| Profile          | Description                                      |
|------------------|--------------------------------------------------|
| `inlocalmemory`  | In-process HashMap + doubly-linked list (default) |
| `distributed`    | Redis-backed store using Lua scripts for atomicity |

## Running

```bash
# Local memory (default)
./mvnw spring-boot:run

# Distributed (requires Redis)
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=distributed
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## API

| Method   | Path                  | Description                        |
|----------|-----------------------|------------------------------------|
| `PUT`    | `/api/v1/warmest/{key}` | Upsert a key with an integer value |
| `GET`    | `/api/v1/warmest/{key}` | Get the value for a key            |
| `DELETE` | `/api/v1/warmest/{key}` | Remove a key                       |
| `GET`    | `/api/v1/warmest`       | Get the warmest (MRU) key          |

## Distributed design

### Why `getWarmest` doesn't need a Lua script

`getWarmest()` reads the current warmest key directly from Redis metadata. Since this is already a single O(1) hash lookup (`HGET warmest:distributed:meta warmest`), wrapping it in a Lua script would not add any meaningful benefit.

In contrast, the mutating operations (`put`, `get`, and `remove`) must update multiple Redis hashes together. Those operations use Lua scripts so the value map, linked-list pointers, and warmest key are all updated atomically in one round-trip.

### Example of how `KEYS[]` is used in Lua

For example, if `key = "a"`, then the Lua call below reads the value of key `a` from the values hash:

```lua
redis.call('HGET', KEYS[1], key) -- returns 100
```

In this project, the `KEYS[]` array is mapped as follows:

- `KEYS[1] = "warmest:distributed:values"`
- `KEYS[2] = "warmest:distributed:prev"`
- `KEYS[3] = "warmest:distributed:next"`
- `KEYS[4] = "warmest:distributed:meta"`

### Redis data model

```
Redis
│
├── warmest:distributed:values
│   ├── a -> 100
│   ├── b -> 200
│   └── c -> 300
│
├── warmest:distributed:prev
│   ├── b -> a
│   └── c -> b
│
├── warmest:distributed:next
│   ├── a -> b
│   └── b -> c
│
└── warmest:distributed:meta
    └── warmest -> c
```

These four Redis hashes model the data structure in O(1): `values` stores the actual key/value pairs, `prev` and `next` represent the doubly-linked-list pointers, and `meta` stores the current warmest key.
