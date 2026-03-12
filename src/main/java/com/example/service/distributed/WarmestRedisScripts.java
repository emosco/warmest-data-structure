package com.example.service.distributed;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@Profile("distributed")
public class WarmestRedisScripts {

    private final RedisScript<String> putScript;
    private final RedisScript<String> getScript;
    private final RedisScript<String> removeScript;

    public WarmestRedisScripts() {
        this.putScript = loadScript("redis/put.lua");
        this.getScript = loadScript("redis/get.lua");
        this.removeScript = loadScript("redis/remove.lua");
    }

    public RedisScript<String> put() {
        return putScript;
    }

    public RedisScript<String> get() {
        return getScript;
    }

    public RedisScript<String> remove() {
        return removeScript;
    }

    private RedisScript<String> loadScript(String path) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(String.class);
        return script;
    }
}
