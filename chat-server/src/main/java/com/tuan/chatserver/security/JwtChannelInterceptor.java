package com.tuan.chatserver.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor{
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel){
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if(accessor.getCommand() == StompCommand.CONNECT){
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if(attributes != null){
                Object personId = attributes.get("personId");
                Principal principal = new Principal() {
                    @Override
                    public String getName() {
                        return String.valueOf(personId);
                    }
                };
                accessor.setUser(principal);
            }
        }
        return message;
    }
}
