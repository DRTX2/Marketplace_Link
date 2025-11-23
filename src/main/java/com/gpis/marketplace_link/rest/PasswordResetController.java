package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.user.ForgotPasswordRequest;
import com.gpis.marketplace_link.dto.user.ResetPasswordRequest;
import com.gpis.marketplace_link.services.user.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req){
        passwordResetService.forgotPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/reset"})
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req);
        return ResponseEntity.ok().build();
    }

}
