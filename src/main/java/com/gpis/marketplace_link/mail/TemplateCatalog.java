package com.gpis.marketplace_link.mail;

import com.gpis.marketplace_link.enums.EmailType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class TemplateCatalog {

    private final Map<EmailType, TemplateInfo> byType = Map.ofEntries(
            Map.entry(EmailType.GENERIC, new TemplateInfo("emails/generic.html", "Notificación")),
            Map.entry(EmailType.EMAIL_CONFIRMATION, new TemplateInfo("emails/email-confirmation.html", "Confirma tu correo")),
            Map.entry(EmailType.PASSWORD_RESET, new TemplateInfo("emails/password-reset.html", "Restablece tu contraseña")),
            Map.entry(EmailType.MODERATOR_ACCOUNT_CREATED, new TemplateInfo("emails/moderator-account-created.html", "Cuenta de Moderador Creada")),
            Map.entry(EmailType.APPOINTMENT_CONFIRMATION, new TemplateInfo("emails/booking-confirmation.html", "Cita confirmada")),
            Map.entry(EmailType.APPOINTMENT_UPDATE, new TemplateInfo("emails/update-booking.html", "Cita actualizada")),
            Map.entry(EmailType.APPOINTMENT_REMINDER_24H, new TemplateInfo("emails/appointment_reminder_24h.html", "Recordatorio de cita")),
            Map.entry(EmailType.PUBLICATION_BLOCKED_NOTIFICATION, new TemplateInfo("emails/publication_blocked.html", "Publicación bloqueada"))
    );

    public TemplateInfo get(EmailType type) {
        return Objects.requireNonNull(byType.get(type), "Tipo de email no soportado: " + type);
    }

}
