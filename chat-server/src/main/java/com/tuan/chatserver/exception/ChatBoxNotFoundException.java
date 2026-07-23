package com.tuan.chatserver.exception;

public class ChatBoxNotFoundException extends RuntimeException {
    public ChatBoxNotFoundException(Long id) {
        super("ChatBox not found, chatBoxId = "+id);
    }
}
