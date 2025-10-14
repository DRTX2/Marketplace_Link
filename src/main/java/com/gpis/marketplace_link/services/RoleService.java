package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.entities.Role;
import com.gpis.marketplace_link.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public Set<Role> resolveOrThrow(Set<Role> incoming) {
        if (incoming == null || incoming.isEmpty()) return Set.of();

        List<String> missing = new ArrayList<>();
        Set<Role> resolved = new HashSet<>();

        for (Role role : incoming) {
            Role found = findRole(role);
            if (found == null) missing.add(label(role));
            else resolved.add(found);
        }

        if (!missing.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Roles no encontrados: " + String.join(", ", missing));

        return resolved;
    }

    public Set<Role> resolveWithDefault(Set<Role> incoming, String defaultRoleName) {
        if (incoming != null && !incoming.isEmpty()) return resolveOrThrow(incoming);

        Role def = roleRepository.findByName(defaultRoleName);
        if (def == null)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Rol por defecto no configurado: " + defaultRoleName);

        return Set.of(def);
    }

    private Role findRole(Role r) {
        if (r == null) return null;
        if (r.getId() != null) return roleRepository.findById(r.getId()).orElse(null);
        if (r.getName() != null && !r.getName().isBlank()) return roleRepository.findByName(r.getName().trim());
        return null;
    }

    private String label(Role r) {
        if (r == null) return "null";
        if (r.getName() != null && !r.getName().isBlank()) return r.getName().trim();
        return r.getId() != null ? String.valueOf(r.getId()) : "undefined";
    }
}
