package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.valueObjects.EmailType;
import com.gpis.marketplace_link.valueObjects.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;

@Primary
@Service
@RequiredArgsConstructor
public class NotificationService {

    private EmailNotificationSender sender;

    public boolean send(String to, EmailType emailType, Map<String, String> variables) {

        NotificationMessage message = new NotificationMessage();
        message.setVariables(variables != null ? variables : Map.of());
        message.setTo(to);
        message.setType(emailType);

        return sender.send(message);
    }
}

