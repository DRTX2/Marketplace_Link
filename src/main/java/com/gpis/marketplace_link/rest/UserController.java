package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.user.ChangePasswordRequest;
import com.gpis.marketplace_link.dto.user.UserRequest;
import com.gpis.marketplace_link.dto.user.UserResponse;
import com.gpis.marketplace_link.mappers.UserMapper;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.services.UserService;
import com.gpis.marketplace_link.validation.groups.Create;
import com.gpis.marketplace_link.validation.groups.Update;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper mapper;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users.stream()
                .map(mapper::toResponse)
                .toList());
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(
            @Validated(Create.class) @RequestBody UserRequest req
    ) {
        User user = mapper.toDomain(req);
        User savedUser = userService.register(user, "ROLE_BUYER");
        return ResponseEntity
                .created(URI.create("/api/users/" + savedUser.getId()))
                .body(mapper.toResponse(savedUser));
    }

    @PutMapping({"/{id}"})
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Validated(Update.class) @RequestBody UserRequest req
    ) {
        User draft = mapper.toDomain(req);
        draft.setId(id);
        User savedUser = userService.update(id, draft);
        return ResponseEntity.ok(mapper.toResponse(savedUser));
    }

    @PutMapping({"/{id}/change-password"})
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        userService.changePassword(id, req);
        return ResponseEntity.noContent().build();
    }

}
