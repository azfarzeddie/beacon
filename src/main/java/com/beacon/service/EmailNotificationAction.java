package com.beacon.service;

import com.beacon.model.NotificationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import static com.beacon.model.Types.Channel;

@Component
public class EmailNotificationAction implements NotificationAction {
    @Value("${spring.mail.username}")
    String sender;
    final JavaMailSender mailSender;

    public EmailNotificationAction(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public Channel getChannel() {
        return Channel.EMAIL;
    }

    @Override
    public boolean send(NotificationContext notificationContext) {
        // create the email message and then call the email server API
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(sender);
            mailMessage.setTo(notificationContext.getEmail());
            mailMessage.setSubject(notificationContext.getSubject());
            mailMessage.setText(notificationContext.getMessage());

            mailSender.send(mailMessage);
            return true;
        } catch (MailException e) {
            return false;
        }
    }
}
