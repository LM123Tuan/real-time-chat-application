package com.tuan.chatserver.exception;

public class MessageAlreadySeenException extends RuntimeException {
    public MessageAlreadySeenException(String messageId) {
        super("Message already at SEEN status, messageId="+messageId);
    }
}
