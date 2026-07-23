package com.tuan.chatserver.exception;

public class ChatBoxAlreadyExistsException extends RuntimeException {
    public ChatBoxAlreadyExistsException() {
        super("ChatBox already exists!");
    }
}
