package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.dto.mail.SendEmailRequest;
import com.gpis.marketplace_link.dto.user.ForgotPasswordRequest;
import com.gpis.marketplace_link.dto.user.ResetPasswordRequest;
import com.gpis.marketplace_link.entities.PasswordResetToken;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.repository.PasswordResetTokenRepository;
import com.gpis.marketplace_link.repository.UserRepository;
import com.gpis.marketplace_link.valueObjects.EmailType;
import com.gpis.marketplace_link.valueObjects.PasswordResetUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int EXPIRATION_MINUTES = 10;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String createTokenValue() {
        return UUID.randomUUID().toString();
    }

    private PasswordResetToken save(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(createTokenValue());
        token.setExpiration(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));
        token.setUser(user);
        return passwordResetTokenRepository.save(token);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.email()).ifPresent(user -> {
            PasswordResetToken token = save(user.getId());

            SendEmailRequest mail = new SendEmailRequest();
            mail.setEmailTo(req.email());
            mail.setEmailType(EmailType.PASSWORD_RESET);

            PasswordResetUrl url = new PasswordResetUrl(frontendUrl + "/reset-password", token.getToken());
            Map<String, String> variables = Map.of(
                    "user", user.getUsername(),
                    "reset_url", url.toString()
            );

            notificationService.send(mail.getEmailTo(), mail.getEmailType(), variables);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(req.tokenValue())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Token no encontrado"));

        if (Boolean.TRUE.equals(token.getUsed()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token ya fue utilizado");

        if (token.getExpiration().isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token ha expirado");

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        userService.resetPassword(token.getUser().getId(), req.newPassword());
    }
}
