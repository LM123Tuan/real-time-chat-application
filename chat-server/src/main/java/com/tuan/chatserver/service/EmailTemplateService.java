package com.tuan.chatserver.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public EmailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String buildVerificationEmail(String username, String verificationLink) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("verificationLink", verificationLink);
        return templateEngine.process("email/verification-email", context);
    }

    public String buildResetPasswordEmail(String username, String resetLink) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("resetLink", resetLink);
        return templateEngine.process("email/reset-password-email", context);
    }
}