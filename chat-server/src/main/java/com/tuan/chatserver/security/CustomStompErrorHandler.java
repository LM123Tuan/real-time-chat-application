package com.tuan.chatserver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuan.chatserver.dto.ErrorResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class CustomStompErrorHandler extends StompSubProtocolErrorHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

        if (cause instanceof AccessDeniedException) {
            return buildErrorMessage(
                    new ErrorResponse("You lack the required permissions to take this action", 403, LocalDateTime.now()));
        }

        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    private Message<byte[]> buildErrorMessage(ErrorResponse errorResponse) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setLeaveMutable(true);
        accessor.setMessage(errorResponse.getMessage());

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(errorResponse);
        } catch (Exception e) {
            payload = errorResponse.getMessage().getBytes(StandardCharsets.UTF_8);
        }

        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }
}