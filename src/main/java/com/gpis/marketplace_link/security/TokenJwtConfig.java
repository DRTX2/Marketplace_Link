package com.gpis.marketplace_link.security;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class TokenJwtConfig {
    // Clave secreta fija - en producción debería venir de variables de entorno
    public static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            "my-secret-key-for-jwt-token-signing-must-be-at-least-256-bits-long".getBytes(StandardCharsets.UTF_8)
    );
    public static final String PREFIX_TOKEN = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "application/json";
}
