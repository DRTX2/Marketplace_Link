package com.gpis.marketplace_link.security.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(frontendUrl));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH","DELETE"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterFilterRegistrationBean(@Qualifier("corsConfigurationSource") CorsConfigurationSource source) {
        FilterRegistrationBean<CorsFilter> corsBean = new FilterRegistrationBean<>(new CorsFilter(source));
        corsBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return corsBean;
    }
}
