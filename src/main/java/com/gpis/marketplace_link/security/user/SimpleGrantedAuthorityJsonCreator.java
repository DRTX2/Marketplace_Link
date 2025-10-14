package com.gpis.marketplace_link.security.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Clase auxiliar utilizada para la deserialización de objetos
 * {@link org.springframework.security.core.authority.SimpleGrantedAuthority}
 * desde el token JWT.
 *
 * Cuando se generan los tokens, las autoridades (roles) del usuario
 * se serializan en formato JSON. Durante la validación del token,
 * Jackson necesita conocer cómo reconstruir estos objetos.
 *
 * Esta clase define el constructor que Jackson utiliza para crear
 * instancias de SimpleGrantedAuthority a partir de un campo JSON
 * con la propiedad "authority".
 *
 * Se usa mediante la anotación:
 * {@code objectMapper.addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityJsonCreator.class);}
 */
public class SimpleGrantedAuthorityJsonCreator {

    /**
     * Constructor usado por Jackson para deserializar una autoridad desde JSON.
     *
     * El parámetro "authority" corresponde al nombre del rol o permiso
     * (por ejemplo, "ROLE_ADMIN" o "ROLE_USER").
     *
     * @param role nombre del rol o autoridad, extraído del campo "authority" del JSON
     */
    @JsonCreator
    public  SimpleGrantedAuthorityJsonCreator(@JsonProperty("authority") String role) {

    }
}
