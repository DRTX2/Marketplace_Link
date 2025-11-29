package com.gpis.marketplace_link.security.config;

import com.gpis.marketplace_link.security.filters.JwtAuthenticationFilter;
import com.gpis.marketplace_link.security.filters.JwtValidationFilter;
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

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("prod")
public class ProdSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] WHITELIST = {
            "/login",
            "/api/users/**",
            "/api/auth/password-reset/**"
    };

    private static final String[] WHITELIST_GET = {};

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
                .addFilter(new JwtValidationFilter(authManager))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(management ->
                        management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
