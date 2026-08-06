package com.tuan.chatserver.exception;

import java.util.Set;

public class UserAlreadyInChatBoxException extends RuntimeException {
    public UserAlreadyInChatBoxException(Long chatBoxId, Long userId) {
        super("User already in chatbox, chatBoxId = "+chatBoxId+", userId = "+userId);
    }
    public UserAlreadyInChatBoxException(Long chatBoxId, Set<Long> userIds){
        super("User already in chatbox, chatBoxId = "+chatBoxId+", userIds = "+userIds);
    }
}
