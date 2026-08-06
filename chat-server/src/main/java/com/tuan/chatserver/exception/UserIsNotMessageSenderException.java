package com.tuan.chatserver.exception;

public class UserIsNotMessageSenderException extends RuntimeException {
    public UserIsNotMessageSenderException(Long userId, String messageId) {
        super("User is not a message sender, userId= "+userId+", messageId= "+messageId);
    }
}
