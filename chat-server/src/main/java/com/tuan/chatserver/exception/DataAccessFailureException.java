package com.tuan.chatserver.exception;

public class DataAccessFailureException extends RuntimeException{
    public DataAccessFailureException(Throwable cause) {
        super("Cannot access database!", cause);
    }
}
