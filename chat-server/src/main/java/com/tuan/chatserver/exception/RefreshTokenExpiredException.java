package com.tuan.chatserver.exception;

public class RefreshTokenExpiredException extends RuntimeException{
    public RefreshTokenExpiredException(Long personId){
        super("Refresh token is expired! personId="+personId);
    }
}
