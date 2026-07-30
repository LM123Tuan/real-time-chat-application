package com.tuan.chatserver.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
    private static final Logger logger =
            LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        logger.error("Google OAuth2 login failed", exception);

        String error = "oauth_failed";

        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String errorCode = oauthException.getError().getErrorCode();

            switch (errorCode) {
                case "access_denied" -> error = "access_denied";
                case "invalid_request" -> error = "invalid_request";
                default -> error = "oauth_failed";
            }
        }

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/login")
                .queryParam("error", error)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
