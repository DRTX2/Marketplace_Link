package com.gpis.marketplace_link.security.user;

import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación personalizada del servicio UserDetailsService de Spring Security.
 *
 * Esta clase se encarga de recuperar los datos del usuario desde la base de datos
 * a través del UserRepository y adaptarlos al formato requerido por Spring Security
 * (objeto UserDetails) utilizando la clase CustomUserDetails.
 *
 * Spring Security invoca este servicio automáticamente durante el proceso de autenticación,
 * pasando el nombre de usuario (en este caso, el email) que se intenta validar.
 *
 * Anotaciones principales:
 * - @Transactional(readOnly = true): asegura que la operación de lectura de datos
 *   se ejecute dentro de una transacción solo de lectura, optimizando el rendimiento.
 */
@Service
@RequiredArgsConstructor
public class UserDetailServiceImp implements UserDetailsService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRoles(email).orElseThrow(() -> new UsernameNotFoundException(("User by username not found.")));
        return new CustomUserDetails(user);
    }
}
