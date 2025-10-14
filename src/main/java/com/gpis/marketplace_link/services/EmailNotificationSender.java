package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.mail.Attachment;
import com.gpis.marketplace_link.mail.NotificationMessage;
import com.gpis.marketplace_link.mail.TemplateCatalog;
import com.gpis.marketplace_link.mail.TemplateInfo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailNotificationSender {

    private final String from = "markeplace.link@hotmail.com";
    private final JavaMailSender mailSender;
    private final TemplateCatalog catalog;

    public boolean send(NotificationMessage msg) {
        try {
            TemplateInfo def = catalog.get(msg.getType());

            // El html estatico
            ClassPathResource resource = new ClassPathResource(def.classpathHtml());
            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Si es q hay variables en ese
            if (msg.getVariables() != null && !msg.getVariables().isEmpty()) {
                for (Map.Entry<String, String> entry : msg.getVariables().entrySet()) {
                    html = html.replace("${" + entry.getKey() + "}", entry.getValue());
                }
            }

            // Y lo construimmos
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(msg.getTo());
            helper.setFrom(from);
            helper.setSubject(def.subject());
            helper.setText(html, true);

            // Si es que hay, los anexos
            if (msg.getAttachments() != null && !msg.getAttachments().isEmpty()) {
                for (Attachment attachment : msg.getAttachments()) {
                    helper.addAttachment(attachment.filename(), new ByteArrayResource(attachment.content(), attachment.contentType()));
                }
            }

            // Enviando
            mailSender.send(mimeMessage);
            return true;
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Somenthing went wrong." + e.getMessage());
        }

    }
}
