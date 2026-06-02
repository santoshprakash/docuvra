package com.docuvra.config;

import com.docuvra.enums.UserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docuvra.security")
public record SecurityProperties(
        boolean loginEnabled,
        UserRole defaultRoleWhenLoginDisabled,
        String defaultUsernameWhenLoginDisabled,
        String jwtSecret,
        long jwtExpirationMinutes
) {
}
