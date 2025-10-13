package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.dto.user.ChangePasswordRequest;
import com.gpis.marketplace_link.entities.Role;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.repository.UserRepository;
import com.gpis.marketplace_link.valueObjects.AccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public boolean existsUserByEmail(String email) { return userRepo.existsByEmail(email); }

    public List<User> findAll() { return userRepo.findAll(); }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con username " + username));
    }

    public User findByEmail(String email) {
        return userRepo.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con email " + email));
    }

    @Transactional
    public User register(User user, String defaultRole) {
        Set<Role> roles = (user.getRoles() == null || user.getRoles().isEmpty())
                ? roleService.resolveWithDefault(user.getRoles(), defaultRole)
                : roleService.resolveOrThrow(user.getRoles());
        user.setRoles(roles);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getAccountStatus() == null) user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);

        User savedUser = userRepo.save(user);
        emailVerificationService.sendVerificationEmail(savedUser);
        return savedUser;
    }

    @Transactional
    public User update(Long id, User draft) {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: " + id));

        apply(existing, draft);
        return userRepo.save(existing);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        var user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");

        if (!req.newPassword().equals(req.confirmNewPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");

        if (passwordEncoder.matches(req.newPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede ser igual a la actual");

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepo.save(user);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        var user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (passwordEncoder.matches(newPassword, user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede ser igual a la actual");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    @Transactional
    public void desactivateUser(Long userId) {
        userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (userRepo.desactivateById(userId) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo realizar el borrado lógico del usuario");
    }

    @Transactional
    public void activateUser(Long userId) {
        if (userRepo.activateById(userId) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + userId);
    }

    @Transactional
    public void blockUser(Long userId) {
        var user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (user.getAccountStatus() != AccountStatus.BLOCKED) {
            user.setAccountStatus(AccountStatus.BLOCKED);
            userRepo.save(user);
        }
    }

    @Transactional
    public void unblockUser(Long userId) {
        var user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            userRepo.save(user);
        }
    }

    private void apply(User existing, User draft) {
        if (draft.getUsername() != null)        existing.setUsername(draft.getUsername());
        if (draft.getEmail() != null)           existing.setEmail(draft.getEmail());
        if (draft.getPhone() != null)           existing.setPhone(draft.getPhone());
        if (draft.getFirstName() != null)       existing.setFirstName(draft.getFirstName());
        if (draft.getLastName() != null)        existing.setLastName(draft.getLastName());
        if (draft.getCedula() != null)          existing.setCedula(draft.getCedula());
        if (draft.getGender() != null)          existing.setGender(draft.getGender());
        if (draft.getAccountStatus() != null)   existing.setAccountStatus(draft.getAccountStatus());

        if (draft.getPassword() != null && !draft.getPassword().isBlank()) {
            if (passwordEncoder.matches(draft.getPassword(), existing.getPassword()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede ser igual a la actual");
            existing.setPassword(passwordEncoder.encode(draft.getPassword()));
        }

        if (draft.getRoles() != null) {
            existing.setRoles(roleService.resolveOrThrow(draft.getRoles()));
        }
    }
}
