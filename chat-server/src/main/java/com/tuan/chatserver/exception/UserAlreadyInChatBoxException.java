package com.tuan.chatserver.exception;

public class UserAlreadyInChatBoxException extends RuntimeException {
    public UserAlreadyInChatBoxException(Long chatBoxId, Long userId) {
        super("User already in chatbox, chatBoxId = "+chatBoxId+", userId = "+userId);
    }
}
