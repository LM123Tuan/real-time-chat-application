package com.tuan.chatserver.exception;

public class EmailSendingFailureException extends RuntimeException {
    public EmailSendingFailureException(Throwable e) {
        super("Failed to send email", e);
    }
}
