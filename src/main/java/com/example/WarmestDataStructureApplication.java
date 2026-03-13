package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class WarmestDataStructureApplication {

    private static final Logger logger = LoggerFactory.getLogger(WarmestDataStructureApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(WarmestDataStructureApplication.class, args);
        Environment environment = applicationContext.getEnvironment();

        String[] activeProfiles = environment.getActiveProfiles();
        String[] effectiveProfiles = activeProfiles.length == 0
                ? environment.getDefaultProfiles()
                : activeProfiles;

        logger.info("Warmest Data Structure Application is running with profiles {}", Arrays.toString(effectiveProfiles));

        if (Arrays.asList(effectiveProfiles).contains("distributed")) {
            logger.info(
                    "Distributed mode is configured to use Redis at {}:{}",
                    environment.getProperty("spring.data.redis.host"),
                    environment.getProperty("spring.data.redis.port")
            );
        }
    }
}