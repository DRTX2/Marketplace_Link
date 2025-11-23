package com.gpis.marketplace_link.security.user;

import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.AccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Implementación personalizada de la interfaz UserDetails de Spring Security.
 * Esta clase adapta la entidad User de la aplicación al modelo de seguridad
 * de Spring, permitiendo que el framework maneje autenticación y autorización
 * con los datos del usuario almacenados en la base de datos.
 * Los roles del usuario se convierten en objetos GrantedAuthority,
 * necesarios para el control de acceso dentro de Spring Security.
 */

public record CustomUserDetails(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountStatus() != AccountStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getAccountStatus() == AccountStatus.ACTIVE && !Boolean.TRUE.equals(user.getDeleted());
    }
}
