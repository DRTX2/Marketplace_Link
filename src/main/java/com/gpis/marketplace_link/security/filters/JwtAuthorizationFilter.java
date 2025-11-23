package com.gpis.marketplace_link.security.filters;

import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.security.user.CustomUserDetails;
import com.gpis.marketplace_link.security.user.SimpleGrantedAuthorityJsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static com.gpis.marketplace_link.security.config.TokenJwtConfig.*;

/**
 * Filtro encargado de validar los tokens JWT en cada petición HTTP.
 *
 * Este filtro se ejecuta en cada request después de la autenticación inicial
 * y comprueba si el token presente en la cabecera "Authorization" es válido.
 *
 * Si el token es correcto, se extraen las autoridades (roles) y el usuario,
 * se crea un objeto de autenticación y se almacena en el contexto de seguridad
 * de Spring (SecurityContextHolder).
 *
 * Si el token es inválido o no está presente, la petición continúa sin usuario autenticado.
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    /**
     * Método principal del filtro que intercepta todas las peticiones HTTP
     * y verifica si contienen un token JWT válido en la cabecera de autorización.
     *
     * Pasos principales:
     * 1. Lee la cabecera "Authorization".
     * 2. Si existe y comienza con el prefijo configurado (por ejemplo, "Bearer "),
     *    extrae el token y lo valida usando la clave secreta.
     * 3. Si el token es válido, obtiene el usuario y sus roles, y los establece
     *    en el contexto de seguridad.
     * 4. Si no hay token o es inválido, la petición sigue sin autenticación.
     *
     * @param request  petición HTTP entrante
     * @param response respuesta HTTP saliente
     * @param chain    cadena de filtros de Spring Security
     * @throws IOException si ocurre un error de entrada/salida
     * @throws ServletException si ocurre un error en la cadena de filtros
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader(HEADER_AUTHORIZATION);

        if (header == null || !header.startsWith(PREFIX_TOKEN) || header.length() <= PREFIX_TOKEN.length()) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX_TOKEN.length()).trim();
        try {
            Claims claims = Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token).getPayload();
            String username = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            Object authoritiesClaims = claims.get("authorities");

            Collection<? extends GrantedAuthority> authorities = Arrays.asList(
                    new ObjectMapper()
                            .addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityJsonCreator.class)
                            .readValue(authoritiesClaims.toString().getBytes(), SimpleGrantedAuthority[].class)
            );

            User user = new User();
            user.setId(userId);
            user.setEmail(username);
            CustomUserDetails userDetails = new CustomUserDetails(user);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (JwtException ex) {
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }
}
