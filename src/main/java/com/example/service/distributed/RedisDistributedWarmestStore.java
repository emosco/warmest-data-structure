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
        Integer previousValue = executeIntegerScript("put", redisScripts.put(), key, String.valueOf(value));
        logger.debug("Distributed put completed for key {} with previous value {}", key, previousValue);
        return previousValue;
    }

    public Integer remove(String key) {
        Integer removedValue = executeIntegerScript("remove", redisScripts.remove(), key);
        logger.debug("Distributed remove completed for key {} with previous value {}", key, removedValue);
        return removedValue;
    }

    public Integer get(String key) {
        Integer resolvedValue = executeIntegerScript("get", redisScripts.get(), key);
        logger.debug("Distributed get completed for key {} with resolved value {}", key, resolvedValue);
        return resolvedValue;
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
    private Integer executeIntegerScript(String operation, RedisScript<String> script, String key) {
        try {
            return parseInteger(redisTemplate.execute(script, redisKeys.all(), key));
        } catch (RuntimeException ex) {
            logger.error("Distributed {} failed for key {}", operation, key, ex);
            throw ex;
        }
    }

    private Integer executeIntegerScript(String operation, RedisScript<String> script, String key, String value) {
        try {
            return parseInteger(redisTemplate.execute(script, redisKeys.all(), key, value));
        } catch (RuntimeException ex) {
            logger.error("Distributed {} failed for key {}", operation, key, ex);
            throw ex;
        }
    }

    private Integer parseInteger(String value) {
        return value == null ? null : Integer.valueOf(value);
    }
}

