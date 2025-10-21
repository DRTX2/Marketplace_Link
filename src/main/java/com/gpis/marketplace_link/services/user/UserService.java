package com.gpis.marketplace_link.services.user;

import com.gpis.marketplace_link.dto.user.ChangePasswordRequest;
import com.gpis.marketplace_link.entities.Role;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.AccountStatus;
import com.gpis.marketplace_link.exceptions.business.users.AccountBlockedException;
import com.gpis.marketplace_link.exceptions.business.users.AccountInactiveException;
import com.gpis.marketplace_link.exceptions.business.users.AccountPendingVerificationException;
import com.gpis.marketplace_link.repositories.UserRepository;
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

    private static final String MSG_USER_NOT_FOUND = "Usuario no encontrado";
    private static final String MSG_INVALID_CREDS = "Credenciales inválidas";
    private static final String MSG_PASSWORDS_MISMATCH = "Las contraseñas no coinciden";
    private static final String MSG_PASSWORD_SAME = "La nueva contraseña no puede ser igual a la actual";
    private static final String MSG_SOFT_DELETE_FAIL = "No se pudo realizar el borrado lógico del usuario";

    private final UserRepository userRepo;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND);
    }

    private ResponseStatusException notFoundWith(String suffix) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND + (suffix == null ? "" : suffix));
    }

    public boolean existsUserByEmail(String email) { return userRepo.existsByEmail(email); }

    public List<User> findAll() { return userRepo.findAll(); }

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
    public void desactivateUser(Long userId) {
        userRepo.findById(userId).orElseThrow(this::notFound);
        if (userRepo.desactivateById(userId) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_SOFT_DELETE_FAIL);
    }

    @Transactional
    public void activateUser(Long userId) {
        if (userRepo.activateById(userId) == 0)
            throw notFoundWith(" con ID: " + userId);
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
