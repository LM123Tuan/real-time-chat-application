package com.tuan.chatserver.exception;

public class EmptyMessageContentException extends RuntimeException {
    public EmptyMessageContentException() {
        super("Message content cannot be empty!");
    }
}
