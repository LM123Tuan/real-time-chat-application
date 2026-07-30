package com.tuan.chatserver.exception;

import com.tuan.chatserver.enums.AuthProvider;

import java.util.Set;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(Long id){
        super("User not found with id: " + id);
    }
    public UserNotFoundException(Set<Long> MissingIds){
        super("Users not found with ids: "+MissingIds);
    }
    public UserNotFoundException(String usernameOrEmail){
        super("User not found with username or email: " + usernameOrEmail);
    }
    public UserNotFoundException(AuthProvider provider, String providerId){
        super("User not found with provider "+provider.toString()+" and providerId: "+providerId);
    }
}
