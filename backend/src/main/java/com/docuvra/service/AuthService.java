package com.docuvra.service;

import com.docuvra.dto.AuthResponse;
import com.docuvra.dto.ChangePasswordRequest;
import com.docuvra.dto.CreateUserRequest;
import com.docuvra.dto.CurrentUserResponse;
import com.docuvra.dto.LoginRequest;
import com.docuvra.dto.RegisterNormalUserRequest;
import com.docuvra.dto.UserResponse;
import com.docuvra.entity.UserEntity;
import com.docuvra.enums.UserRole;
import com.docuvra.config.SecurityProperties;
import com.docuvra.exception.AuthException;
import com.docuvra.exception.ForbiddenException;
import com.docuvra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserService currentUserService;
    private final SecurityProperties securityProperties;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(normalize(request.usernameOrEmail()))
                .or(() -> userRepository.findByEmailIgnoreCaseAndRole(normalize(request.usernameOrEmail()), UserRole.NORMAL_USER))
                .or(() -> userRepository.findByEmailIgnoreCaseAndRole(normalize(request.usernameOrEmail()), UserRole.STAFF))
                .or(() -> userRepository.findByEmailIgnoreCaseAndRole(normalize(request.usernameOrEmail()), UserRole.SUPERVISOR))
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new AuthException("Invalid username or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid username or password.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return new AuthResponse(jwtTokenService.createToken(user), toCurrentUser(user));
    }

    @Transactional
    public AuthResponse signup(RegisterNormalUserRequest request) {
        ensureLoginEnabled("Signup is disabled while login is disabled.");
        UserEntity user = createUser(
                request.username(),
                request.email(),
                request.mobile(),
                request.password(),
                UserRole.NORMAL_USER,
                false
        );
        return new AuthResponse(jwtTokenService.createToken(user), toCurrentUser(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        ensureLoginEnabled("User management is disabled while login is disabled.");
        currentUserService.requireRole(UserRole.SUPERVISOR);
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String search) {
        currentUserService.currentUserEntity();
        String query = normalize(search);
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(UserEntity::isActive)
                .filter(user -> query.isBlank()
                        || user.getUsername().toLowerCase(Locale.ROOT).contains(query)
                        || user.getEmail().toLowerCase(Locale.ROOT).contains(query))
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listStaffUsers() {
        ensureLoginEnabled("Staff management is disabled while login is disabled.");
        currentUserService.requireRole(UserRole.SUPERVISOR);
        return userRepository.findAllByRoleAndActiveTrue(UserRole.STAFF).stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listMentionableUsers() {
        currentUserService.currentUserEntity();
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(UserEntity::isActive)
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse createManagedUser(CreateUserRequest request) {
        ensureLoginEnabled("User management is disabled while login is disabled.");
        currentUserService.requireRole(UserRole.SUPERVISOR);
        if (request.role() == UserRole.NORMAL_USER) {
            throw new ForbiddenException("Supervisors can create only staff and supervisor users here. Normal users can use signup.");
        }
        return toUserResponse(createUser(
                request.username(),
                request.email(),
                request.mobile(),
                request.temporaryPassword(),
                request.role(),
                request.role() == UserRole.STAFF
        ));
    }

    @Transactional
    public CurrentUserResponse changePassword(ChangePasswordRequest request) {
        UserEntity user = currentUserService.currentUserEntity();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setForcePasswordChange(false);
        user.setUpdatedAt(LocalDateTime.now());
        return toCurrentUser(user);
    }

    private UserEntity createUser(
            String username,
            String email,
            String mobile,
            String password,
            UserRole role,
            boolean forcePasswordChange
    ) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);
        String normalizedMobile = normalizeMobile(mobile);

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new AuthException("Username is already used. Please choose a different username.");
        }
        if (userRepository.existsByEmailIgnoreCaseAndRole(normalizedEmail, role)) {
            throw new AuthException("Email is already used for this role.");
        }
        if (userRepository.existsByMobileAndRole(normalizedMobile, role)) {
            throw new AuthException("Mobile number is already used for this role.");
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setMobile(normalizedMobile);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        user.setForcePasswordChange(forcePasswordChange);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
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

    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.isActive(),
                user.isForcePasswordChange(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMobile(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private void ensureLoginEnabled(String message) {
        if (!securityProperties.loginEnabled()) {
            throw new ForbiddenException(message);
        }
    }
}
