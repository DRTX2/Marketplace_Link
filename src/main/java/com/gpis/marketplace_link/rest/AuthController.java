package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.user.UserResponse;
import com.gpis.marketplace_link.mappers.UserMapper;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

}
