package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.ChatEvent;
import com.tuan.chatserver.dto.PresenceBatchResponse;
import com.tuan.chatserver.dto.PresenceResponse;
import com.tuan.chatserver.enums.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PresenceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RedisService redisService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String PRESENCE_KEY_PREFIX = "presence:online:";
    private static final String ONLINE_USERS_SET_KEY = "presence:online_users";

    private static final RedisScript<Long> OFFLINE_SCRIPT = RedisScript.of(
            "local current = redis.call('GET', KEYS[1]) " +
                    "if current == ARGV[1] then " +
                    "    redis.call('DEL', KEYS[1]) " +
                    "    redis.call('SREM', KEYS[2], ARGV[2]) " +
                    "    return 1 " +
                    "else " +
                    "    return 0 " +
                    "end",
            Long.class
    );

    public PresenceService(RedisService redisService,
                           SimpMessagingTemplate messagingTemplate) {
        this.redisService = redisService;
        this.messagingTemplate = messagingTemplate;
    }

    public void markOnline(Long userId, String sessionId) {
        logger.info("Marking user online, userId={}, sessionId={}", userId, sessionId);
        String key = PRESENCE_KEY_PREFIX + userId;
        redisService.set(key, sessionId);
        redisService.addToSet(ONLINE_USERS_SET_KEY, userId.toString());
        logger.info("User marked online, userId={}", userId);

        messagingTemplate.convertAndSend(
                "/topic/presence/" + userId,
                new ChatEvent<>(EventType.PRESENCE_ONLINE, new PresenceResponse(userId, "ONLINE")));
    }

    public void markOffline(Long userId, String sessionId) {
        logger.info("Marking user offline attempt, userId={}, sessionId={}", userId, sessionId);
        String key = PRESENCE_KEY_PREFIX + userId;

        Long result = redisService.execute(
                OFFLINE_SCRIPT,
                Arrays.asList(key, ONLINE_USERS_SET_KEY),
                sessionId, userId.toString()
        );

        if (result != null && result == 1L) {
            logger.info("User marked offline, userId={}", userId);

            messagingTemplate.convertAndSend(
                    "/topic/presence/" + userId,
                    new ChatEvent<>(EventType.PRESENCE_OFFLINE, new PresenceResponse(userId, "OFFLINE")));
        } else {
            logger.info("Skip offline update, session outdated, userId={}, sessionId={}", userId, sessionId);
        }
    }

    public PresenceResponse isOnline(Long userId) {
        logger.debug("Checking online status, userId={}", userId);
        String key = PRESENCE_KEY_PREFIX + userId;
        boolean online = redisService.exists(key);
        logger.debug("Online status result, userId={}, online={}", userId, online);
        return new PresenceResponse(userId, online ? "ONLINE" : "OFFLINE");
    }

    public PresenceBatchResponse getOnlineUserIds(List<Long> userIds) {
        logger.debug("Batch checking online status for {} users", userIds.size());
        List<String> keys = userIds.stream()
                .map(id -> PRESENCE_KEY_PREFIX + id)
                .collect(Collectors.toList());
        List<Optional<String>> values = redisService.multiGet(keys, String.class);

        List<PresenceResponse> responses = new ArrayList<>();
        int onlineCount = 0;
        for (int i = 0; i < userIds.size(); i++) {
            boolean online = values.get(i).isPresent();
            if (online) {
                onlineCount++;
            }
            responses.add(new PresenceResponse(userIds.get(i), online ? "ONLINE" : "OFFLINE"));
        }

        logger.debug("Batch check completed, {} of {} users online", onlineCount, userIds.size());
        return new PresenceBatchResponse(responses);
    }

    public Long getOnlineCount() {
        logger.debug("Fetching total online user count");
        long count = redisService.getSetSize(ONLINE_USERS_SET_KEY);
        logger.debug("Total online user count={}", count);
        return count;
    }
}