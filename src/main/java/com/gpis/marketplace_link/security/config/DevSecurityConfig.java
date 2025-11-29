package com.gpis.marketplace_link.security.config;

import com.gpis.marketplace_link.security.filters.JwtAuthenticationFilter;
import com.gpis.marketplace_link.security.filters.JwtValidationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@RequiredArgsConstructor
@Configuration
@Profile("dev")
@Slf4j
public class DevSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authManager);
        jwtAuthenticationFilter.setFilterProcessesUrl("/api/auth/login");

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/password-reset/**").permitAll()
                        .anyRequest().permitAll())
                .addFilter(jwtAuthenticationFilter)
                .addFilter(new JwtValidationFilter(authManager))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(management ->
                        management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

}
