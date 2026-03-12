package com.example.service.distributed;

import com.example.api.WarmestDataStructureInterface;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("distributed")
public class WarmestDataStructureServiceDistributed implements WarmestDataStructureInterface {

    private final RedisDistributedWarmestStore distributedWarmestStore;

    public WarmestDataStructureServiceDistributed(RedisDistributedWarmestStore distributedWarmestStore) {
        this.distributedWarmestStore = distributedWarmestStore;
    }

    @Override
    public Integer put(String key, int value) {
        return distributedWarmestStore.put(key, value);
    }

    @Override
    public Integer remove(String key) {
        return distributedWarmestStore.remove(key);
    }

    @Override
    public Integer get(String key) {
        return distributedWarmestStore.get(key);
    }

    @Override
    public String getWarmest() {
        return distributedWarmestStore.getWarmest();
    }
}
