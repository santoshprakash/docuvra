package com.docuvra.service;

import com.docuvra.config.SecurityProperties;
import com.docuvra.entity.UserEntity;
import com.docuvra.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final SecurityProperties securityProperties;

    public String createToken(UserEntity user) {
        long expiresAt = Instant.now().plusSeconds(securityProperties.jwtExpirationMinutes() * 60).getEpochSecond();
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("""
                {"sub":"%s","username":"%s","role":"%s","exp":%d}
                """.formatted(user.getId(), escape(user.getUsername()), user.getRole(), expiresAt).trim());
        String signingInput = header + "." + payload;
        return signingInput + "." + sign(signingInput);
    }

    public TokenUser parseToken(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token.");
        }

        String signingInput = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(signingInput), parts[2])) {
            throw new IllegalArgumentException("Invalid token signature.");
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        long expiresAt = Long.parseLong(extractJsonValue(payload, "exp"));
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new IllegalArgumentException("Token expired.");
        }

        return new TokenUser(
                UUID.fromString(extractJsonValue(payload, "sub")),
                extractJsonValue(payload, "username"),
                UserRole.valueOf(extractJsonValue(payload, "role"))
        );
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign token.", exception);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigestHolder.equals(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private String extractJsonValue(String payload, String key) {
        String marker = "\"" + key + "\":";
        int start = payload.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("Missing token claim.");
        }
        start += marker.length();
        if (payload.charAt(start) == '"') {
            int end = payload.indexOf('"', start + 1);
            return payload.substring(start + 1, end);
        }
        int end = payload.indexOf(',', start);
        if (end < 0) {
            end = payload.indexOf('}', start);
        }
        return payload.substring(start, end).trim();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record TokenUser(UUID userId, String username, UserRole role) {
    }

    private static final class MessageDigestHolder {
        private static boolean equals(byte[] expected, byte[] actual) {
            return java.security.MessageDigest.isEqual(expected, actual);
        }
    }
}
