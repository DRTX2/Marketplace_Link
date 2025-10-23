package com.gpis.marketplace_link.services.user;

import com.gpis.marketplace_link.dto.user.ChangePasswordRequest;
import com.gpis.marketplace_link.entities.PasswordResetToken;
import com.gpis.marketplace_link.entities.Role;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.AccountStatus;
import com.gpis.marketplace_link.enums.EmailType;
import com.gpis.marketplace_link.exceptions.business.users.AccountBlockedException;
import com.gpis.marketplace_link.exceptions.business.users.AccountInactiveException;
import com.gpis.marketplace_link.exceptions.business.users.AccountPendingVerificationException;
import com.gpis.marketplace_link.mail.PasswordResetUrl;
import com.gpis.marketplace_link.repositories.UserRepository;
import com.gpis.marketplace_link.services.NotificationService;
import com.gpis.marketplace_link.specifications.UserSpecifications;
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
public class UserService {

    private static final String MSG_USER_NOT_FOUND = "Usuario no encontrado";
    private static final String MSG_INVALID_CREDS = "Credenciales inválidas";
    private static final String MSG_PASSWORDS_MISMATCH = "Las contraseñas no coinciden";
    private static final String MSG_PASSWORD_SAME = "La nueva contraseña no puede ser igual a la actual";
    private static final String MSG_SOFT_DELETE_FAIL = "No se pudo realizar el borrado lógico del usuario";

    private final UserRepository userRepo;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final NotificationService notificationService;

    public UserService(
            UserRepository userRepo,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            @Lazy PasswordResetService passwordResetService,
            NotificationService notificationService
    ) {
        this.userRepo = userRepo;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.notificationService = notificationService;
    }

    @Value("${moderator.default-password}")
    private String moderatorDefaultPassword;

    @Value("${frontend.url}")
    private String frontendUrl;

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND);
    }

    private ResponseStatusException notFoundWith(String suffix) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND + (suffix == null ? "" : suffix));
    }

    public boolean existsUserByEmail(String email) { return userRepo.existsByEmail(email); }

    public List<User> findAll() { return userRepo.findAll(); }

    /**
     * Finds all users with optional filters.
     * If includeDeleted is true, also returns soft-deleted users.
     */
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

    /**
     * Finds all users with optional filters and pagination.
     * If includeDeleted is true, also returns soft-deleted users.
     */
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

    public User findById(Long id) {
        return userRepo.findById(id).orElseThrow(() ->
                notFoundWith(": " + id));
    }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username).orElseThrow(() ->
                notFoundWith(" con username " + username));
    }

    public User findByEmail(String email) {
        return userRepo.findByEmail(email).orElseThrow(() ->
                notFoundWith(" con email " + email));
    }

    @Transactional(readOnly = true)
    public User getProfileEnforcingStatus(Long userId) {
        var user = userRepo.findById(userId).orElseThrow(this::notFound);
        var st = user.getAccountStatus();

        if (st == AccountStatus.ACTIVE) return user;
        if (st == AccountStatus.BLOCKED) throw new AccountBlockedException();
        if (st == AccountStatus.PENDING_VERIFICATION) throw new AccountPendingVerificationException();

        throw new AccountInactiveException();
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

    /**
     * Creates a moderator account with default password and verified status.
     * Sends an email with a password reset token valid for 24 hours.
     *
     * @param user The user entity with moderator data (without password, roles, or accountStatus)
     * @return The created moderator user
     */
    @Transactional
    public User createModerator(User user) {
        Set<Role> moderatorRole = roleService.resolveWithDefault(null, "ROLE_MODERATOR");
        user.setRoles(moderatorRole);

        user.setPassword(passwordEncoder.encode(moderatorDefaultPassword));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(LocalDateTime.now());

        User savedUser = userRepo.save(user);

        PasswordResetToken resetToken = passwordResetService.createTokenWithCustomExpiration(savedUser.getId(), 1440);

        PasswordResetUrl resetUrl = new PasswordResetUrl(frontendUrl + "/reset-password", resetToken.getToken());
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
    public User update(Long id, User draft) {
        User existing = userRepo.findById(id).orElseThrow(() -> notFoundWith(": " + id));
        apply(existing, draft);
        return userRepo.save(existing);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        var user = userRepo.findById(userId).orElseThrow(this::notFound);

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_INVALID_CREDS);

        if (!req.newPassword().equals(req.confirmNewPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_PASSWORDS_MISMATCH);

        if (passwordEncoder.matches(req.newPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_PASSWORD_SAME);

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepo.save(user);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        var user = userRepo.findById(userId).orElseThrow(this::notFound);

        if (passwordEncoder.matches(newPassword, user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_PASSWORD_SAME);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    @Transactional
    public void blockUser(Long userId) {
        var user = userRepo.findById(userId).orElseThrow(this::notFound);
        if (user.getAccountStatus() != AccountStatus.BLOCKED) {
            user.setAccountStatus(AccountStatus.BLOCKED);
            userRepo.save(user);
        }
    }

    @Transactional
    public void unblockUser(Long userId) {
        var user = userRepo.findById(userId).orElseThrow(this::notFound);
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            userRepo.save(user);
        }
    }

    @Transactional
    public void desactivateUser(Long userId) {
        userRepo.findById(userId).orElseThrow(this::notFound);
        if (userRepo.deactivateById(userId) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_SOFT_DELETE_FAIL);
    }

    @Transactional
    public void activateUser(Long userId) {
        if (userRepo.activateById(userId) == 0)
            throw notFoundWith(" con ID: " + userId);
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
        if (draft.getLocation() != null)        existing.setLocation(draft.getLocation());
        if (draft.getPassword() != null && !draft.getPassword().isBlank()) {
            if (passwordEncoder.matches(draft.getPassword(), existing.getPassword()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_PASSWORD_SAME);
            existing.setPassword(passwordEncoder.encode(draft.getPassword()));
        }
        if (draft.getRoles() != null) {
            existing.setRoles(roleService.resolveOrThrow(draft.getRoles()));
        }
    }
}
