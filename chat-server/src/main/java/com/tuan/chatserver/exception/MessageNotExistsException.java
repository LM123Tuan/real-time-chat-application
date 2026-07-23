package com.tuan.chatserver.exception;

public class MessageNotExistsException extends RuntimeException {
    public MessageNotExistsException(String messageId) {
        super("Message is not exists, messageId="+messageId);
    }
}
