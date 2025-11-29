package com.gpis.marketplace_link.security.config;

import com.gpis.marketplace_link.security.filters.JwtAuthenticationFilter;
import com.gpis.marketplace_link.security.filters.JwtAuthorizationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración de seguridad para el entorno de producción (perfil "prod").
 *
 * Esta clase establece las reglas de autorización, habilita la seguridad basada en JWT
 * y configura los filtros de autenticación y validación.
 *
 * Anotaciones principales:
 * - @Configuration: indica que esta clase define beans de configuración.
 * - @EnableWebSecurity: activa la configuración de seguridad web de Spring Security.
 * - @EnableMethodSecurity: habilita la seguridad a nivel de métodos (@PreAuthorize, etc.).
 * - @Profile("prod"): esta configuración solo se carga cuando el perfil activo es "prod".
 *
 * El enfoque es completamente stateless, ya que se usa JWT en lugar de sesiones tradicionales.
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("prod")
public class ProdSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] WHITELIST = {
            "/actuator/health", "/actuator/health/**", // Docker health check endpoint (PRIMERO)
            "/login",
            "/api/users/**",
            "/api/auth/password/**",
            "/api/auth/resend-verification", "/api/auth/verify-email/resend",
            "/error",
    };

    private static final String[] WHITELIST_GET = {
            "/api/auth/verify-email",
            "/uploads/**",
            "/*.jpg",
            "/*.jpeg",
            "/*.png",
            "/*.gif",
            "/*.webp"
    };

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http,
                                                       AuthenticationManager authManager) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(WHITELIST).permitAll()
                        .requestMatchers(HttpMethod.GET, WHITELIST_GET).permitAll()
                        .anyRequest().authenticated())
                .addFilter(new JwtAuthenticationFilter(authManager))
                .addFilter(new JwtAuthorizationFilter(authManager))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(management ->
                        management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}