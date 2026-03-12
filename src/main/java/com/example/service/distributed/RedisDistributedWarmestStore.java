package com.example.service.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@Profile("distributed")
public class RedisDistributedWarmestStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisDistributedWarmestStore.class);

    private final StringRedisTemplate redisTemplate;
    private final WarmestRedisKeys redisKeys;
    private final WarmestRedisScripts redisScripts;

    public RedisDistributedWarmestStore(
            StringRedisTemplate redisTemplate,
            WarmestRedisKeys redisKeys,
            WarmestRedisScripts redisScripts
    ) {
        this.redisTemplate = redisTemplate;
        this.redisKeys = redisKeys;
        this.redisScripts = redisScripts;
    }

    // Each mutating operation is delegated to a Redis Lua script so the
    // value map, linked-list pointers, and warmest key are updated atomically.
    public Integer put(String key, int value) {
        return executeIntegerScript(redisScripts.put(), key, String.valueOf(value));
    }

    public Integer remove(String key) {
        return executeIntegerScript(redisScripts.remove(), key);
    }

    public Integer get(String key) {
        return executeIntegerScript(redisScripts.get(), key);
    }

    // getWarmest is a single hash lookup, so it can read directly from Redis
    // without a Lua script while still preserving the O(1) contract.
    public String getWarmest() {
        Object warmestKey = redisTemplate.opsForHash().get(redisKeys.meta(), redisKeys.warmestField());
        String resolvedWarmest = warmestKey == null ? null : warmestKey.toString();
        logger.debug("Resolved distributed warmest key: {}", resolvedWarmest);
        return resolvedWarmest;
    }

    // The scripts return Redis strings, so the store converts them back to the
    // Integer contract expected by WarmestDataStructureInterface.
    private Integer executeIntegerScript(RedisScript<String> script, String key) {
        return parseInteger(redisTemplate.execute(script, redisKeys.all(), key));
    }

    private Integer executeIntegerScript(RedisScript<String> script, String key, String value) {
        return parseInteger(redisTemplate.execute(script, redisKeys.all(), key, value));
    }

    private Integer parseInteger(String value) {
        return value == null ? null : Integer.valueOf(value);
    }
}

