package com.tuan.chatserver.exception;

public class MessageAlreadyRecallException extends RuntimeException{
    public MessageAlreadyRecallException(String messageId){
        super("Message is already recalled, messageId="+messageId);
    }
}
