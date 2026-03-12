package com.example.service.distributed;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("distributed")
public class WarmestRedisKeys {

    private static final String PREFIX = "warmest:distributed:";

    public String values() {
        return PREFIX + "values";
    }

    public String prev() {
        return PREFIX + "prev";
    }

    public String next() {
        return PREFIX + "next";
    }

    public String meta() {
        return PREFIX + "meta";
    }

    public String warmestField() {
        return "warmest";
    }

    public List<String> all() {
        return List.of(values(), prev(), next(), meta());
    }
}
