package com.gpis.marketplace_link.security.config;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Clase de configuración que centraliza las constantes relacionadas
 * con la creación y validación de tokens JWT.
 *
 * Contiene la clave secreta utilizada para firmar los tokens, así como
 * los valores comunes empleados en las cabeceras HTTP y en las respuestas.
 *
*/
public class TokenJwtConfig {

    private TokenJwtConfig() {
    }

    public static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            "my-secret-key-for-jwt-token-signing-must-be-at-least-256-bits-long".getBytes(StandardCharsets.UTF_8)
    );
    public static final String PREFIX_TOKEN = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "application/json";
}
