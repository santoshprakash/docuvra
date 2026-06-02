package com.docuvra.service;

import com.docuvra.config.SecurityProperties;
import com.docuvra.dto.CurrentUserResponse;
import com.docuvra.entity.UserEntity;
import com.docuvra.enums.UserRole;
import com.docuvra.exception.AuthException;
import com.docuvra.exception.ForbiddenException;
import com.docuvra.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final UUID MOCK_NORMAL_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID MOCK_STAFF_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    private final SecurityProperties securityProperties;
    private final UserRepository userRepository;
    private final HttpServletRequest request;

    public CurrentUserResponse currentUser() {
        if (securityProperties.loginEnabled()) {
            return toCurrentUser(currentUserEntity());
        }
        UserRole role = openModeRole();
        return new CurrentUserResponse(
                openModeUserId(role),
                openModeUsername(role),
                null,
                null,
                role,
                false,
                false
        );
    }

    public UUID currentUserIdOrNull() {
        if (!securityProperties.loginEnabled()) {
            return openModeUserId(openModeRole());
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return UUID.fromString(authentication.getPrincipal().toString());
    }

    public String currentUsername() {
        if (!securityProperties.loginEnabled()) {
            return openModeUsername(openModeRole());
        }
        return currentUserEntity().getUsername();
    }

    public UserEntity currentUserEntity() {
        if (!securityProperties.loginEnabled()) {
            UserRole role = openModeRole();
            UserEntity user = new UserEntity();
            user.setId(openModeUserId(role));
            user.setUsername(openModeUsername(role));
            user.setRole(role);
            user.setActive(true);
            return user;
        }
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

    private UserRole openModeRole() {
        String requestedRole = request.getHeader("X-Docuvra-Role");
        if ("NORMAL_USER".equalsIgnoreCase(requestedRole)) {
            return UserRole.NORMAL_USER;
        }
        if ("STAFF".equalsIgnoreCase(requestedRole)) {
            return UserRole.STAFF;
        }
        UserRole configuredRole = securityProperties.defaultRoleWhenLoginDisabled();
        return configuredRole == UserRole.NORMAL_USER ? UserRole.NORMAL_USER : UserRole.STAFF;
    }

    private UUID openModeUserId(UserRole role) {
        return role == UserRole.NORMAL_USER ? MOCK_NORMAL_USER_ID : MOCK_STAFF_USER_ID;
    }

    private String openModeUsername(UserRole role) {
        return role == UserRole.NORMAL_USER ? "Normal User" : defaultUsername();
    }
}
