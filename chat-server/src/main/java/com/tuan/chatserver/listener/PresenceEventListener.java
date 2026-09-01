package com.tuan.chatserver.listener;

import com.tuan.chatserver.dto.ChatEvent;
import com.tuan.chatserver.enums.EventType;
import com.tuan.chatserver.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.security.Principal;

@Component
public class PresenceEventListener {
    private final Logger logger= LoggerFactory.getLogger(this.getClass());
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceEventListener(PresenceService presenceService,
                                 SimpMessagingTemplate messagingTemplate){
        this.presenceService = presenceService;
        this.messagingTemplate=messagingTemplate;
    }

    @EventListener
    @MessageMapping("/admin/online")
    public void handleSessionConnected(SessionConnectedEvent event){
        SimpMessageHeaderAccessor accessor= SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal= accessor.getUser();
        Long userId= extractUserId(principal);
        if(userId==null){
            logger.warn("SessionConnectedEvent without valid userId, sessionId={}", sessionId);
            return;
        }
        logger.info("User connected, userId={}, sessionId={}", userId, sessionId);
        presenceService.markOnline(userId, sessionId);
        messagingTemplate.convertAndSend("/topic/admin/online",
                new ChatEvent<>(EventType.ADMIN_USER_ONLINE, userId)
        );
    }

    @EventListener
    @MessageMapping("/admin/online")
    public void handleSessionDisconnected(SessionDisconnectEvent event){
        SimpMessageHeaderAccessor accessor= SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal= event.getUser();
        Long userId= extractUserId(principal);
        if(userId==null){
            logger.warn("SessionDisconnectEvent without valid userId, sessionId={}", sessionId);
            return;
        }
        logger.info("User disconnected, userId={}, sessionId={}", userId, sessionId);
        presenceService.markOffline(userId, sessionId);
        messagingTemplate.convertAndSend("/topic/admin/online",
                new ChatEvent<>(EventType.ADMIN_USER_OFFLINE, userId)
        );
    }

    private Long extractUserId(Principal principal){
        if (principal == null) {
            return null;
        }
        try{
            return Long.valueOf(principal.getName());
        }catch(NumberFormatException e){
            logger.error("Failed to parse userId from principal name: {}", principal.getName(), e);
            return null;
        }
    }
}