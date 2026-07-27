package com.tuan.chatserver.exception;

public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException(String token) {
        super("Invalid verification token, token= "+token);
    }
}
