package com.tuan.chatserver.exception;

public class UsernameOrEmailAlreadyExistsException extends RuntimeException{
    public UsernameOrEmailAlreadyExistsException() {
        super("Username or email already exists!");
    }
}
