package com.tuan.chatserver.exception;

public class InvalidResetPasswordTokenException extends RuntimeException {
    public InvalidResetPasswordTokenException(String token) {
        super("Invalid reset password token, tokenId= "+token);
    }
}
