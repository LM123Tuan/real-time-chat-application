package com.tuan.chatserver.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.tuan.chatserver.service.CustomUserDetailsService;

import java.util.Map;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final CustomUserDetailsService customUserDetailsService;

    public JwtChannelInterceptor(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() == StompCommand.CONNECT) {
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if (attributes != null) {
                Object personId = attributes.get("personId");
                if (personId != null) {
                    UserDetails userDetails = customUserDetailsService.loadUserById(Long.valueOf(personId.toString()));
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(authToken);
                }
            }
        }
        return message;
    }
}