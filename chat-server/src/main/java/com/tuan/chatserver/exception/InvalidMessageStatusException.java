package com.tuan.chatserver.exception;

import com.tuan.chatserver.enums.MessageStatus;

public class InvalidMessageStatusException extends RuntimeException {
    public InvalidMessageStatusException(String messageId) {
        super("Message already at SEEN status, messageId="+messageId);
    }
    public InvalidMessageStatusException(String messageId, MessageStatus status) {
        super("Invalid message status, messageId= "+messageId+", actual status= "+status);
    }
}
