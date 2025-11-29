package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.user.ForgotPasswordRequest;
import com.gpis.marketplace_link.dto.user.ResetPasswordRequest;
import com.gpis.marketplace_link.services.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req){
        passwordResetService.forgotPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/reset"})
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req);
        return ResponseEntity.ok().build();
    }

}
