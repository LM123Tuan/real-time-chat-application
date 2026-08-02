package com.tuan.chatserver.exception;

public class RefreshTokenExpiredOrNotExistsException extends RuntimeException{
    public RefreshTokenExpiredOrNotExistsException(Long personId){
        super("Refresh token is expired! personId="+personId);
    }
}
