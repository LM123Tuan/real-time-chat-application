package com.tuan.chatserver.exception;

import java.util.Set;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(Long id){
        super("User not found with id: " + id);
    }
    public UserNotFoundException(Set<Long> MissingIds){
        super("Users not found with ids: "+MissingIds);
    }
}
