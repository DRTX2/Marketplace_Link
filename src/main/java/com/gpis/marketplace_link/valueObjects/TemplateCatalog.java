package com.gpis.marketplace_link.valueObjects;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class TemplateCatalog {

    private final Map<EmailType, TemplateInfo> byType = Map.of(
            EmailType.GENERIC,            new TemplateInfo("emails/generic.html",          "Notificación"),
            EmailType.EMAIL_CONFIRMATION, new TemplateInfo("emails/email-confirmation.html","Confirma tu correo"),
            EmailType.PASSWORD_RESET,     new TemplateInfo("emails/password-reset.html",   "Restablece tu contraseña"),
            EmailType.APPOINTMENT_CONFIRMATION, new TemplateInfo("emails/booking-confirmation.html", "Cita confirmada"),
            EmailType.APPOINTMENT_UPDATE, new TemplateInfo("emails/update-booking.html", "Cita actualizada"),
            EmailType.APPOINTMENT_REMINDER_24H, new TemplateInfo("emails/appointment_reminder_24h.html", "Recordatorio de cita")
    );

    public TemplateInfo get(EmailType type) {
        return Objects.requireNonNull(byType.get(type), "Tipo de email no soportado: " + type);
    }

}
