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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Configuración de seguridad específica para el entorno de desarrollo (perfil "dev").
 *
 * Esta clase define la cadena de filtros de Spring Security, habilita CORS y configura
 * el manejo de sesiones sin estado (stateless) utilizando JWT para la autenticación.
 *
 * Se incluyen los filtros personalizados:
 * - JwtAuthenticationFilter: maneja el proceso de login y generación del token.
 * - JwtValidationFilter: valida los tokens JWT en las peticiones posteriores.
 *
 * Anotaciones principales:
 * - @Configuration: indica que esta clase define beans de configuración.
 * - @Profile("dev"): solo se activa cuando el perfil activo es "dev".
 * - @RequiredArgsConstructor: simplifican la inyección de dependencias.
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@Profile("dev")
public class DevSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authManager);
        jwtAuthenticationFilter.setFilterProcessesUrl("/login");

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll() // Health check PRIMERO
                        .requestMatchers(HttpMethod.POST, "/login").permitAll() // Login público
                        .requestMatchers(HttpMethod.POST, "/api/users/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/password-reset/**").permitAll()
                        // Permitir endpoints públicos adicionales si es necesario
                        .requestMatchers(HttpMethod.GET, "/api/publications/**").permitAll() 
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .anyRequest().authenticated())
                .addFilter(jwtAuthenticationFilter)
                .addFilter(new JwtAuthorizationFilter(authManager))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(management ->
                        management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

}