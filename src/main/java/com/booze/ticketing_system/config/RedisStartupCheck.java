package com.booze.ticketing_system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Tells you at startup whether Redis is reachable, instead of you finding out when the first
 * login request comes back as 503. Never stops the app from starting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStartupCheck implements CommandLineRunner {
    private final StringRedisTemplate redis;

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Override
    public void run(String... args) {
        try {
            redis.hasKey("ticketing:startup-check");
            log.info("Redis reachable at {}:{}", host, port);
        } catch (RuntimeException exception) {
            log.warn("Redis NOT reachable at {}:{} -> {}. API requests will fail until it is fixed "
                            + "(start Redis with `docker compose up -d redis`, or set REDIS_HOST / REDIS_PORT).",
                    host, port, NestedExceptionUtils.getMostSpecificCause(exception).getMessage());
        }
    }
}
