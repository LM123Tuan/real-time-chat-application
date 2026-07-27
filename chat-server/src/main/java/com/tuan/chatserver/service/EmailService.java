package com.tuan.chatserver.service;

public interface EmailService {
    void sendHtmlMail(String to, String subject, String htmlContent);
}