package com.tuan.chatserver.dto;

import com.tuan.chatserver.enums.EventType;

public class ChatEvent<T>{
    private EventType type;
    private T payload;

    public ChatEvent(){
    }

    public ChatEvent(EventType type, T payload){
        this.type=type;
        this.payload=payload;
    }
}
