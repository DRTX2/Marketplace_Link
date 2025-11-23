package com.gpis.marketplace_link.security.filters;

import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.security.user.CustomUserDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import static com.gpis.marketplace_link.security.config.TokenJwtConfig.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro que intercepta las peticiones de inicio de sesión (login)
 * y se encarga de autenticar al usuario mediante su email y contraseña.
 *
 * Si la autenticación es exitosa, genera un token JWT y lo devuelve en la cabecera
 * y en el cuerpo de la respuesta. En caso contrario, responde con un error 401.
 *
 * Extiende de UsernamePasswordAuthenticationFilter, lo que permite integrarse
 * directamente con el flujo de autenticación de Spring Security.
 */
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Intenta autenticar al usuario leyendo las credenciales desde el cuerpo
     * de la petición HTTP. Espera un objeto JSON con los campos "email" y "password".
     *
     * @param request  petición HTTP que contiene las credenciales del usuario
     * @param response respuesta HTTP (no se usa en esta fase)
     * @return objeto Authentication con las credenciales a validar
     * @throws AuthenticationException si ocurre un error durante la autenticación
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        User user = null;
        String userName = null;
        String password = null;

        try {
            user = new ObjectMapper().readValue(request.getInputStream(), User.class);
            userName = user.getEmail();
            password = user.getPassword();
        } catch (IOException e) {
            throw new AuthenticationServiceException("Failed to parse authentication request body.", e);
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userName,
                password);
        return authenticationManager.authenticate(authenticationToken);
    }

    /**
     * Método ejecutado cuando la autenticación es exitosa.
     *
     * Crea el token JWT con la información del usuario autenticado
     * (nombre, ID y roles), lo agrega a la cabecera de la respuesta y
     * también lo devuelve en el cuerpo en formato JSON.
     *
     * @param request  petición original
     * @param response respuesta HTTP donde se añade el JWT
     * @param chain    cadena de filtros (no se utiliza aquí)
     * @param authResult resultado de la autenticación con los datos del usuario
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal();
        User user  = customUserDetails.user();

        String username = user.getUsername();
        Long userId = user.getId();

        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();
        Claims claims = Jwts.claims()
                .add("authorities", new ObjectMapper().writeValueAsString(authorities))
                .add("userId", userId)
                .build();

        String jwt = Jwts.builder()
                .subject(username)
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .issuedAt(new Date())
                .signWith(SECRET_KEY)
                .compact();

        response.addHeader(HEADER_AUTHORIZATION, PREFIX_TOKEN + jwt);

        Map<String, String> body = new HashMap<>();
        body.put("token", jwt);
        body.put("username", username);
        body.put("message", String.format("Hello %s, you have been started session with sucessfully", username));

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setContentType(CONTENT_TYPE);
        response.setStatus(200);
    }

    /**
     * @param request  petición original
     * @param response respuesta HTTP donde se envía el mensaje de error
     * @param failed   excepción lanzada por la autenticación fallida
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        Map<String, String> body = new HashMap<>();
        String message = "Email o contraseña inválidos.";

        if (failed instanceof org.springframework.security.authentication.LockedException) {
            message = "Tu cuenta está bloqueada. Contacta al administrador.";
        } else if (failed instanceof org.springframework.security.authentication.DisabledException) {
            message = "Tu cuenta está pendiente de verificación. Revisa tu correo.";
        }

        body.put("message", message);
        body.put("error", failed.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }
}
