package com.tuan.chatserver.config;

import com.tuan.chatserver.security.CustomStompErrorHandler;
import com.tuan.chatserver.security.JwtChannelInterceptor;
import com.tuan.chatserver.security.JwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${rabbitmq.stomp.host}")
    private String relayHost;

    @Value("${rabbitmq.stomp.port}")
    private int relayPort;

    @Value("${rabbitmq.stomp.login}")
    private String relayLogin;

    @Value("${rabbitmq.stomp.passcode}")
    private String relayPasscode;

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final CustomStompErrorHandler customStompErrorHandler;

    @Autowired
    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor,
                           JwtChannelInterceptor jwtChannelInterceptor,
                           CustomStompErrorHandler customStompErrorHandler){
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.jwtChannelInterceptor = jwtChannelInterceptor;
        this.customStompErrorHandler=customStompErrorHandler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(relayLogin)
                .setClientPasscode(relayPasscode)
                .setSystemHeartbeatSendInterval(10000)
                .setSystemHeartbeatReceiveInterval(10000);
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();
        registry.setErrorHandler(customStompErrorHandler);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration){
        registration.interceptors(jwtChannelInterceptor);
    }
}
