package com.docuvra.service;

import com.docuvra.config.SecurityProperties;
import com.docuvra.dto.CurrentUserResponse;
import com.docuvra.entity.UserEntity;
import com.docuvra.enums.UserRole;
import com.docuvra.exception.AuthException;
import com.docuvra.exception.ForbiddenException;
import com.docuvra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final UUID MOCK_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    private final SecurityProperties securityProperties;
    private final UserRepository userRepository;

    public CurrentUserResponse currentUser() {
        if (securityProperties.loginEnabled()) {
            return toCurrentUser(currentUserEntity());
        }
        return new CurrentUserResponse(
                MOCK_USER_ID,
                defaultUsername(),
                null,
                null,
                securityProperties.defaultRoleWhenLoginDisabled(),
                false,
                false
        );
    }

    public UUID currentUserIdOrNull() {
        if (!securityProperties.loginEnabled()) {
            return MOCK_USER_ID;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return UUID.fromString(authentication.getPrincipal().toString());
    }

    public String currentUsername() {
        if (!securityProperties.loginEnabled()) {
            return defaultUsername();
        }
        return currentUserEntity().getUsername();
    }

    public UserEntity currentUserEntity() {
        UUID userId = currentUserIdOrNull();
        if (userId == null) {
            throw new AuthException("Login is required.");
        }
        return userRepository.findById(userId)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new AuthException("Login is required."));
    }

    public void requireRole(UserRole requiredRole) {
        if (!securityProperties.loginEnabled()) {
            return;
        }
        UserEntity user = currentUserEntity();
        if (user.getRole() != requiredRole) {
            throw new ForbiddenException("This action requires " + requiredRole + " access.");
        }
    }

    private CurrentUserResponse toCurrentUser(UserEntity user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.isForcePasswordChange(),
                true
        );
    }

    private String defaultUsername() {
        return securityProperties.defaultUsernameWhenLoginDisabled() == null
                || securityProperties.defaultUsernameWhenLoginDisabled().isBlank()
                ? "Staff"
                : securityProperties.defaultUsernameWhenLoginDisabled();
    }
}
