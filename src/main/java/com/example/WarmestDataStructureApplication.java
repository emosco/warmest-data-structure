package com.example;

import com.example.service.WarmestDataStructureServiceInLocalMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WarmestDataStructureApplication {

    private static final Logger logger = LoggerFactory.getLogger(WarmestDataStructureApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(WarmestDataStructureApplication.class, args);
        logger.info("Warmest Data Structure Application is running! 🚀");
    }
}