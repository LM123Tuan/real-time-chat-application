package com.tuan.chatserver.service.impl;

import com.tuan.chatserver.exception.DataAccessFailureException;
import com.tuan.chatserver.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisServiceImpl implements RedisService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> void set(String key, T value) {
        logger.info("Saving value to Redis, key={}", key);

        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (DataAccessException e) {
            logger.error("Failed to save value to Redis, key={}", key, e);
            throw new DataAccessFailureException(e);
        }

        logger.info("Successfully saved value to Redis, key={}", key);
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        logger.info("Saving value to Redis with TTL, key={}, ttl={}", key, ttl);

        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (DataAccessException e) {
            logger.error("Failed to save value to Redis, key={}", key, e);
            throw new DataAccessFailureException(e);
        }

        logger.info("Successfully saved value to Redis, key={}", key);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        logger.info("Retrieving value from Redis, key={}", key);

        try {
            Object value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                logger.info("No value found in Redis, key={}", key);
                return Optional.empty();
            }

            return Optional.of(type.cast(value));
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve value from Redis, key={}", key, e);
            throw new DataAccessFailureException(e);
        }
    }

    @Override
    public boolean exists(String key) {
        logger.info("Checking Redis key existence, key={}", key);

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (DataAccessException e) {
            logger.error("Failed to check Redis key existence, key={}", key, e);
            throw new DataAccessFailureException(e);
        }
    }

    @Override
    public void delete(String key) {
        logger.info("Deleting Redis key, key={}", key);

        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            logger.error("Failed to delete Redis key, key={}", key, e);
            throw new DataAccessFailureException(e);
        }

        logger.info("Successfully deleted Redis key, key={}", key);
    }

    @Override
    public <T> Optional<T> getAndDelete(String key, Class<T> type) {
        logger.info("Retrieving and deleting value from Redis, key={}", key);

        try {
            Object value = redisTemplate.opsForValue().getAndDelete(key);

            if (value == null) {
                logger.info("No value found in Redis, key={}", key);
                return Optional.empty();
            }

            logger.info("Successfully retrieved and deleted Redis value, key={}", key);
            return Optional.of(type.cast(value));
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve and delete Redis value, key={}", key, e);
            throw new DataAccessFailureException(e);
        }
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration ttl){
        logger.info("Attempting to acquire lock in Redis, key={}, ttl={}", key, ttl);
        try{
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            boolean result = Boolean.TRUE.equals(acquired);

            if(result){
                logger.info("Successfully acquired lock in Redis, key={}", key);
            } else {
                logger.info("Failed to acquire lock in Redis, key already exists, key={}", key);
            }
            return result;
        }catch(DataAccessException e){
            logger.error("Failed to acquire lock in Redis, key={}", key, e);
            throw new DataAccessFailureException(e);
        }
    }
}