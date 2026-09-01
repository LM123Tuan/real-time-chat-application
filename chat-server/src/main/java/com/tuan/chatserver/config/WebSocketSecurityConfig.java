package com.tuan.chatserver.config;

import org.springframework.messaging.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {
    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
                .simpSubscribeDestMatchers("/topic/chatbox/**")
                    .access(AuthorizationManagers.not(AuthorityAuthorizationManager.hasRole("ADMIN")))
                .simpSubscribeDestMatchers("/user/queue/notifications")
                    .access(AuthorizationManagers.not(AuthorityAuthorizationManager.hasRole("ADMIN")))

                .simpSubscribeDestMatchers("/topic/admin/**")
                    .hasRole("ADMIN")

                .simpDestMatchers("/app/admin/**")
                    .access(AuthorizationManagers.not(AuthorityAuthorizationManager.hasRole("ADMIN")));


        return messages.build();
    }
}
