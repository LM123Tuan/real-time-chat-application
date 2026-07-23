package com.tuan.chatserver.exception;

public class NotEnoughMembersException extends RuntimeException{
    public NotEnoughMembersException(){
        super("Chatbox not enough members!");
    }
}
