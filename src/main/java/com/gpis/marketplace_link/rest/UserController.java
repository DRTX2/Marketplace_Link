package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.user.ChangePasswordRequest;
import com.gpis.marketplace_link.dto.user.ModeratorCreateRequest;
import com.gpis.marketplace_link.dto.user.UserCreateRequest;
import com.gpis.marketplace_link.dto.user.UserResponse;
import com.gpis.marketplace_link.dto.user.UserUpdateRequest;
import com.gpis.marketplace_link.mappers.UserMapper;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper mapper;

    /**
     * Get all users with optional filters.
     * Only accessible by ADMIN role.
     *
     * @param includeDeleted If true, includes soft-deleted users; if false or null, only active users
     * @param roles List of role names to filter by (e.g., "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_BUYER")
     * @param search Search term to match against username, email, firstName, lastName, or cedula
     * @return List of users matching the criteria
     */
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR','SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(required = false) String search
    ) {
        List<User> users = userService.findAllWithFilters(includeDeleted, roles, search);
        return ResponseEntity.ok(users.stream().map(mapper::toResponse).toList());
    }

    /**
     * Get all users with pagination and optional filters.
     * Only accessible by ADMIN role.
     *
     * @param includeDeleted If true, includes soft-deleted users; if false or null, only active users
     * @param roles List of role names to filter by (e.g., "ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_BUYER")
     * @param search Search term to match against username, email, firstName, lastName, or cedula
     * @param page Page number (0-indexed), default 0
     * @param size Page size, default 10
     * @param sortBy Field to sort by, default "id"
     * @param sortDir Sort direction ("asc" or "desc"), default "asc"
     * @return Page of users matching the criteria
     */
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR','SUPER_ADMIN')")
    @GetMapping("/paginated")
    public ResponseEntity<Page<UserResponse>> getAllPaginated(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> usersPage = userService.findAllWithFiltersPaginated(includeDeleted, roles, search, pageable);
        Page<UserResponse> responsePage = usersPage.map(mapper::toResponse);
        
        return ResponseEntity.ok(responsePage);
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserCreateRequest req) {
        User user = mapper.toDomain(req);
        User savedUser = userService.register(user, "ROLE_BUYER");
        return ResponseEntity
                .created(URI.create("/api/users/" + savedUser.getId()))
                .body(mapper.toResponse(savedUser));
    }

    /**
     * Creates a moderator account. Only accessible by ADMIN role.
     * The moderator account is created with:
     * - A default password (configured in environment variables)
     * - Active and verified status
     * - ROLE_MODERATOR assigned
     * - A password reset token valid for 24 hours sent via email
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/moderators")
    public ResponseEntity<UserResponse> createModerator(@Valid @RequestBody ModeratorCreateRequest req) {
        User user = mapper.toDomain(req);
        User savedModerator = userService.createModerator(user);
        return ResponseEntity
                .created(URI.create("/api/users/" + savedModerator.getId()))
                .body(mapper.toResponse(savedModerator));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest req
    ) {
        User draft = mapper.toDomain(req);
        draft.setId(id);
        User savedUser = userService.update(id, draft);
        return ResponseEntity.ok(mapper.toResponse(savedUser));
    }

    @PutMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        userService.changePassword(id, req);
        return ResponseEntity.noContent().build();
    }

    // ==============================
    //   Estado: Activar / Desactivar
    // ==============================

    /** Solo ADMIN puede desactivar */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.desactivateUser(id); // (sic) usa tu método actual
        return ResponseEntity.noContent().build();
    }

    /** Solo ADMIN puede activar */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    // ==============================
    //   Bloqueo: Bloquear / Desbloquear
    // ==============================

    /** ADMIN o MODERATOR pueden bloquear */
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @PutMapping("/{id}/block")
    public ResponseEntity<Void> block(@PathVariable Long id) {
        userService.blockUser(id);
        return ResponseEntity.noContent().build();
    }

    /** ADMIN o MODERATOR pueden desbloquear */
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @PutMapping("/{id}/unblock")
    public ResponseEntity<Void> unblock(@PathVariable Long id) {
        userService.unblockUser(id);
        return ResponseEntity.noContent().build();
    }
}
