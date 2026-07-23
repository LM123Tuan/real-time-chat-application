package com.tuan.chatserver.exception;

import java.util.Set;

public class UserNotInChatBoxException extends RuntimeException {
    public UserNotInChatBoxException(Long chatBoxId, Long userId) {
        super("User not in chatbox, chatBoxId = "+chatBoxId+", userId = "+userId);
    }
    public UserNotInChatBoxException(Long chatBoxId, Set<Long> userIds) {
        super("User not in chatbox, chatBoxId = "+chatBoxId+", userIds = "+userIds);
    }
}
