package com.docuvra.controller;

import com.docuvra.dto.AuthResponse;
import com.docuvra.dto.ChangePasswordRequest;
import com.docuvra.dto.CreateUserRequest;
import com.docuvra.dto.CurrentUserResponse;
import com.docuvra.dto.LoginRequest;
import com.docuvra.dto.RegisterNormalUserRequest;
import com.docuvra.dto.UserResponse;
import com.docuvra.service.AuthService;
import com.docuvra.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUserService currentUserService;
    private final AuthService authService;

    @GetMapping("/api/auth/me")
    public CurrentUserResponse me() {
        return currentUserService.currentUser();
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/api/auth/signup")
    public AuthResponse signup(@Valid @RequestBody RegisterNormalUserRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/api/auth/change-password")
    public CurrentUserResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

    @GetMapping("/api/users")
    public List<UserResponse> users(@RequestParam(required = false) String search) {
        return search == null ? authService.listUsers() : authService.listUsers(search);
    }

    @GetMapping("/api/users/staff")
    public List<UserResponse> staffUsers() {
        return authService.listStaffUsers();
    }

    @GetMapping("/api/users/mentionable")
    public List<UserResponse> mentionableUsers() {
        return authService.listMentionableUsers();
    }

    @PostMapping("/api/users")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return authService.createManagedUser(request);
    }

    @PostMapping("/api/auth/logout")
    public void logout() {
        // JWT tokens are stateless; the frontend removes its local token.
    }
}
