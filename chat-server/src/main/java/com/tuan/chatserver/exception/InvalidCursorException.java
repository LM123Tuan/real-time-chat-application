package com.tuan.chatserver.exception;

public class InvalidCursorException extends RuntimeException {
    public InvalidCursorException(String message, Throwable e) {
        super(message, e);
    }
}
