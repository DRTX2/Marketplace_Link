package com.gpis.marketplace_link.security.service;

import com.gpis.marketplace_link.exceptions.business.AuthenticationCredentialsNotFoundException;
import com.gpis.marketplace_link.security.user.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    public Long getCurrentUserId() {
        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.user().getId();
        } else {
            throw new AuthenticationCredentialsNotFoundException("El usuario actual no está autenticado o no es válido.");
        }
    }
}
