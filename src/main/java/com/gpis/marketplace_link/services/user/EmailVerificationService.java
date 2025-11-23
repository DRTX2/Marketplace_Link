package com.gpis.marketplace_link.services.user;

import com.gpis.marketplace_link.entities.EmailVerificationToken;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.repositories.EmailVerificationTokenRepository;
import com.gpis.marketplace_link.repositories.UserRepository;
import com.gpis.marketplace_link.enums.AccountStatus;
import com.gpis.marketplace_link.enums.EmailType;
import com.gpis.marketplace_link.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    public void sendVerificationEmail(User user) {
        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta ya está activa.");
        }

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(generateToken());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));

        tokenRepository.save(token);

        String confirmUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/verify-email")
                .queryParam("token", token.getToken())
                .build()
                .toUriString();

        notificationService.sendAsync(
                user.getEmail(),
                EmailType.EMAIL_CONFIRMATION,
                Map.of(
                        "user", user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername(),
                        "confirm_url", confirmUrl
                )
        );
    }

    @Transactional
    public void confirm(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido."));

        if (token.isUsed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "El token ya fue utilizado.");
        }

        if (token.isExpired()) {
            throw new ResponseStatusException(HttpStatus.GONE, "El token ha expirado.");
        }

        User user = token.getUser();
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        token.setConsumedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    @Transactional
    public void resend(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta ya está activa.");
        }
        sendVerificationEmail(user);
    }

    @Transactional
    public void resendByToken(String tokenValue) {
        EmailVerificationToken currentToken = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido."));
        User user = currentToken.getUser();

        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta ya está activa.");
        }

        if (!currentToken.isUsed() && !currentToken.isExpired()) {
            currentToken.setExpiresAt(LocalDateTime.now().plusHours(24));
            tokenRepository.save(currentToken);
        }

        sendVerificationEmail(user);
    }

    private String generateToken() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
