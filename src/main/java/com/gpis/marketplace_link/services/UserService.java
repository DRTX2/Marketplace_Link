package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.dto.user.ChangePasswordRequest;
import com.gpis.marketplace_link.entities.PasswordResetToken;
import com.gpis.marketplace_link.entities.Role;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.AccountStatus;
import com.gpis.marketplace_link.enums.EmailType;
import com.gpis.marketplace_link.exceptions.business.users.AccountBlockedException;
import com.gpis.marketplace_link.exceptions.business.users.AccountInactiveException;
import com.gpis.marketplace_link.exceptions.business.users.AccountPendingVerificationException;
import com.gpis.marketplace_link.valueObjects.PasswordResetUrl;
import com.gpis.marketplace_link.repositories.UserRepository;
import com.gpis.marketplace_link.services.PasswordResetService;
import com.gpis.marketplace_link.services.user.EmailVerificationService;
import com.gpis.marketplace_link.specifications.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final NotificationService notificationService;

    public boolean existsUserByEmail(String email) { return userRepo.existsByEmail(email); }

    public List<User> findAll() { return userRepo.findAll(); }

    public User findByEmail(String email) {
        return userRepo.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con email " + email));
    }

    @Transactional(readOnly = true)
    public User getProfileEnforcingStatus(Long userId) {
        var user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        var st = user.getAccountStatus();
        if (st == AccountStatus.ACTIVE) return user;
        if (st == AccountStatus.BLOCKED) throw new AccountBlockedException();
        if (st == AccountStatus.PENDING_VERIFICATION) throw new AccountPendingVerificationException();
        throw new AccountInactiveException();
    }

    @Transactional(readOnly = true)
    public List<User> findAllWithFilters(Boolean includeDeleted, List<String> roleNames, String searchTerm) {
        if (Boolean.TRUE.equals(includeDeleted)) {
            String q = (searchTerm == null || searchTerm.isBlank()) ? null : "%" + searchTerm.trim() + "%";
            boolean rolesIsNull = (roleNames == null || roleNames.isEmpty());
            String[] roles = rolesIsNull ? null : roleNames.toArray(String[]::new);
            return userRepo.findAllIncludingDeletedNative(q, roles, rolesIsNull);
        }
        Specification<User> spec = UserSpecifications.filterUsers(false, roleNames, searchTerm);
        return userRepo.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Page<User> findAllWithFiltersPaginated(Boolean includeDeleted, List<String> roleNames, String searchTerm, Pageable pageable) {
        if (Boolean.TRUE.equals(includeDeleted)) {
            String q = (searchTerm == null || searchTerm.isBlank()) ? null : "%" + searchTerm.trim() + "%";
            boolean rolesIsNull = (roleNames == null || roleNames.isEmpty());
            String[] roles = rolesIsNull ? null : roleNames.toArray(String[]::new);
            return userRepo.findAllIncludingDeletedNative(q, roles, rolesIsNull, pageable);
        }
        Specification<User> spec = UserSpecifications.filterUsers(false, roleNames, searchTerm);
        return userRepo.findAll(spec, pageable);
    }

    @Transactional
    public User createModerator(User user) {
        Set<Role> moderatorRole = roleService.resolveWithDefault(null, "ROLE_MODERATOR");
        user.setRoles(moderatorRole);
        user.setPassword(passwordEncoder.encode("ChangeMe123!"));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(LocalDateTime.now());

        User savedUser = userRepo.save(user);

        PasswordResetToken resetToken = passwordResetService.createTokenWithCustomExpiration(savedUser.getId(), 1440);
        PasswordResetUrl resetUrl = new PasswordResetUrl("${frontend.url}/reset-password", resetToken.getToken());
        Map<String, String> variables = Map.of(
                "user", savedUser.getFullName(),
                "username", savedUser.getUsername(),
                "email", savedUser.getEmail(),
                "reset_url", resetUrl.toString()
        );
        notificationService.sendAsync(savedUser.getEmail(), EmailType.MODERATOR_ACCOUNT_CREATED, variables);
        return savedUser;
    }

    @Transactional
    public User register(User user, String defaultRole) {
        Set<Role> roles = (user.getRoles() == null || user.getRoles().isEmpty())
                ? roleService.resolveWithDefault(user.getRoles(), defaultRole)
                : roleService.resolveOrThrow(user.getRoles());
        user.setRoles(roles);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getAccountStatus() == null) user.setAccountStatus(AccountStatus.ACTIVE);
        return userRepo.save(user);
    }

    @Transactional
    public User update(Long id, User draft) {
        if (draft.getId() == null) draft.setId(id);
        userRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: " + id));
        return userRepo.save(draft);
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
        if (userRepo.deactivateById(userId) == 0)
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
}
