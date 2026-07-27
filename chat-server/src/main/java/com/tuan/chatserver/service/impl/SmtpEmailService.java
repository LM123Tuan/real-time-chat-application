package com.tuan.chatserver.service.impl;

import com.tuan.chatserver.exception.EmailSendingFailureException;
import com.tuan.chatserver.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final JavaMailSender mailSender;

    @Autowired
    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendHtmlMail(String to, String subject, String htmlContent) {
        logger.info("Attempting to send email to {}", to);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("Email sent successfully to {}", to);
        } catch (MessagingException | MailException e) {
            logger.error("Failed to send email to {}", to, e);
            throw new EmailSendingFailureException(e);
        }
    }
}
