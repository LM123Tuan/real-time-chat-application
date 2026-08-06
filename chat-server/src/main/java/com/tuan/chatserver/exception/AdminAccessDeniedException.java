package com.tuan.chatserver.exception;

public class AdminAccessDeniedException extends RuntimeException{
    public AdminAccessDeniedException(Long userId){
        super("This user is not an admin, userId = "+userId);
    }
}
