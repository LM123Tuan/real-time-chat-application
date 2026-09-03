package com.tuan.chatserver.service.impl;

import com.tuan.chatserver.exception.DataAccessFailureException;
import com.tuan.chatserver.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public <T> List<Optional<T>> multiGet(List<String> keys, Class<T> type) {
        logger.info("Batch getting {} keys", keys.size());

        try {
            List<Object> rawValues = redisTemplate.opsForValue().multiGet(keys);

            List<Optional<T>> results = new ArrayList<>();
            if (rawValues == null) {
                keys.forEach(k -> results.add(Optional.empty()));
                return results;
            }

            for (Object rawValue : rawValues) {
                if (rawValue == null) {
                    results.add(Optional.empty());
                } else {
                    results.add(Optional.of(type.cast(rawValue)));
                }
            }

            logger.info("Batch get completed, {} of {} keys found",
                    results.stream().filter(Optional::isPresent).count(), keys.size());
            return results;
        } catch (DataAccessException e) {
            logger.error("Failed to batch get values from Redis", e);
            throw new DataAccessFailureException(e);
        }
    }

    @Override
    public <T> void addToSet(String key, T value) {
        logger.info("Adding value to Redis set, key={}", key);

        try {
            redisTemplate.opsForSet().add(key, value);
        } catch (DataAccessException e) {
            logger.error("Failed to add value to Redis set, key={}", key, e);
            throw new DataAccessFailureException(e);
        }

        logger.info("Successfully added value to Redis set, key={}", key);
    }

    @Override
    public <T> void removeFromSet(String key, T value) {
        logger.info("Removing value from Redis set, key={}", key);

        try {
            redisTemplate.opsForSet().remove(key, value);
        } catch (DataAccessException e) {
            logger.error("Failed to remove value from Redis set, key={}", key, e);
            throw new DataAccessFailureException(e);
        }

        logger.info("Successfully removed value from Redis set, key={}", key);
    }

    @Override
    public long getSetSize(String key) {
        logger.debug("Fetching Redis set size, key={}", key);

        try {
            Long size = redisTemplate.opsForSet().size(key);
            long result = size != null ? size : 0L;
            logger.debug("Redis set size, key={}, size={}", key, result);
            return result;
        } catch (DataAccessException e) {
            logger.error("Failed to fetch Redis set size, key={}", key, e);
            throw new DataAccessFailureException(e);
        }
    }

    @Override
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        logger.info("Executing Redis script, keys={}", keys);

        try {
            T result = redisTemplate.execute(script, keys, args);
            logger.info("Successfully executed Redis script, keys={}", keys);
            return result;
        } catch (DataAccessException e) {
            logger.error("Failed to execute Redis script, keys={}", keys, e);
            throw new DataAccessFailureException(e);
        }
    }
}