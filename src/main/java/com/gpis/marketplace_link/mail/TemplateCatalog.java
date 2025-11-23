package com.gpis.marketplace_link.mail;

import com.gpis.marketplace_link.enums.EmailType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class TemplateCatalog {

    private final Map<EmailType, TemplateInfo> byType = Map.ofEntries(
            Map.entry(EmailType.EMAIL_CONFIRMATION, new TemplateInfo("emails/email-confirmation.html", "Confirma tu correo")),
            Map.entry(EmailType.PASSWORD_RESET, new TemplateInfo("emails/password-reset.html", "Restablece tu contraseña")),
            Map.entry(EmailType.PUBLICATION_BLOCKED_NOTIFICATION, new TemplateInfo("emails/publication_blocked_notification.html", "Publicación bloqueada")),
            Map.entry(EmailType.PUBLICATION_UNLOCK_NOTIFICATION, new TemplateInfo("emails/publication_unlock_notification.html", "Decisión sobre incidencia")),
            Map.entry(EmailType.PUBLICATION_APPEAL_AVAILABLE_NOTIFICATION, new TemplateInfo("emails/publication_appeal_available_notification.html", "Apelación disponible")),
            Map.entry(EmailType.APPEAL_RECEIVED_CONFIRMATION_NOTIFICATION, new TemplateInfo("emails/appeal_received_confirmation_notification.html", "Confirmación de apelación recibida")),
            Map.entry(EmailType.APPEAL_ASSIGNED_TO_MODERATOR_NOTIFICATION, new TemplateInfo("emails/appeal_assigned_to_moderator_notification.html", "Apelación asignada")),
            Map.entry(EmailType.APPEAL_PENDING_ASSIGNMENT_NOTIFICATION, new TemplateInfo("emails/appeal_pending_assigment_notification.html", "Apelación pendiente de asignación")),
            Map.entry(EmailType.APPEAL_APPROVED_NOTIFICATION, new TemplateInfo("emails/appeal_approved_notification.html", "Apelación aprobada")),
            Map.entry(EmailType.APPEAL_REJECTED_NOTIFICATION, new TemplateInfo("emails/appeal_rejected_notification.html", "Apelación rechazada")),
            Map.entry(EmailType.NEW_APPEAL_ASSIGNED_TO_MODERATOR_NOTIFICATION, new TemplateInfo("emails/new_appeal_assigned_to_moderator_notification.html", "Nueva apelación asignada")),
            Map.entry(EmailType.APPEAL_SELLER_MODERATOR_ASSIGNED_NOTIFICATION, new TemplateInfo("emails/appeal_seller_moderator_assigned_notification.html", "Moderador asignado a apelación")),
            Map.entry(EmailType.MODERATOR_ACCOUNT_CREATED, new TemplateInfo("emails/moderator-account-created.html", "Cuenta de Moderador Creada"))
            );

    public TemplateInfo get(EmailType type) {
        return Objects.requireNonNull(byType.get(type), "Tipo de email no soportado: " + type);
    }

}
