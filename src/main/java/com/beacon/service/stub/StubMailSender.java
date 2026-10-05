package com.beacon.service.stub;

import com.beacon.model.Types.Channel;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.util.Arrays;

/**
 * Drop-in replacement for the SMTP-backed JavaMailSender: messages are still built into real
 * MimeMessages (so message-construction cost is realistic), but instead of opening an SMTP
 * connection they go to the {@link StubGateway}.
 */
public class StubMailSender extends JavaMailSenderImpl {
    private final StubGateway gateway;

    public StubMailSender(StubGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) throws MailSendException {
        for (MimeMessage message : mimeMessages) {
            try {
                Address[] recipients = message.getAllRecipients();
                String to = recipients == null ? null : String.join(",",
                        Arrays.stream(recipients).map(Address::toString).toList());
                Object content = message.getContent();
                if (!gateway.accept(Channel.EMAIL, to, message.getSubject(), String.valueOf(content))) {
                    throw new MailSendException("Stub mail provider rejected message to " + to);
                }
            } catch (MessagingException | IOException e) {
                throw new MailSendException("Failed to read stub mail message", e);
            }
        }
    }
}
