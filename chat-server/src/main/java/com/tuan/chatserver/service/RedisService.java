package com.tuan.chatserver.service;

import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface RedisService {
    <T> void set(String key, T value);
    <T> void set(String key, T value, Duration ttl);
    <T> Optional<T> get(String key, Class<T> type);
    boolean exists(String key);
    void delete(String key);
    <T> Optional<T> getAndDelete(String key, Class<T> type);
    <T> boolean setIfAbsent(String key, T value, Duration ttl);
    <T> List<Optional<T>> multiGet(List<String> keys, Class<T> type);
    <T> void addToSet(String key, T value);
    <T> void removeFromSet(String key, T value);
    long getSetSize(String key);
    <T> T execute(RedisScript<T> script, List<String> keys, Object... args);
}