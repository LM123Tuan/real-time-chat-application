package com.tuan.chatserver.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisConfigTest {

    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, Object> redisTemplate;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", 6379);
        connectionFactory.setPassword(System.getenv("REDIS_PASSWORD"));
        connectionFactory.afterPropertiesSet();

        RedisConfig redisConfig = new RedisConfig();
        redisTemplate = redisConfig.redisTemplate(connectionFactory);
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void testRedisConnection() {
        redisTemplate.opsForValue().set("test_key", "hello redis");
        Object value = redisTemplate.opsForValue().get("test_key");
        assertEquals("hello redis", value);
        redisTemplate.delete("test_key");
    }
}