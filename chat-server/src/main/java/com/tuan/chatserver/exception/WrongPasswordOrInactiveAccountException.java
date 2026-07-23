package com.tuan.chatserver.exception;

public class WrongPasswordOrInactiveAccountException extends RuntimeException {
    public WrongPasswordOrInactiveAccountException() {
        super("Wrong password or inactive account!");
    }
}
