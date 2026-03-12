package com.example.service;

import com.example.api.WarmestDataStructureInterface;
import com.example.service.distributed.DistributedWarmestStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("distributed")
public class WarmestDataStructureServiceDistributed implements WarmestDataStructureInterface {

    private final DistributedWarmestStore distributedWarmestStore;

    public WarmestDataStructureServiceDistributed(DistributedWarmestStore distributedWarmestStore) {
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
