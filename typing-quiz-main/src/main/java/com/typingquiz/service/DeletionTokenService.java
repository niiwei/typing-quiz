package com.typingquiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeletionTokenService {
    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public DeletionTokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        String configured = System.getenv("JWT_SECRET");
        if (configured != null && !configured.trim().isEmpty()) {
            this.secret = configured.getBytes(StandardCharsets.UTF_8);
        } else {
            byte[] random = new byte[32];
            new java.security.SecureRandom().nextBytes(random);
            this.secret = random;
        }
    }

    public String issue(String kind, Long id, Long userId, Long version) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("kind", kind);
            claims.put("id", id);
            claims.put("userId", userId);
            claims.put("version", version);
            claims.put("expiresAt", Instant.now().plusSeconds(300).getEpochSecond());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    objectMapper.writeValueAsBytes(claims));
            return payload + "." + sign(payload);
        } catch (Exception e) {
            throw new IllegalStateException("无法生成删除确认", e);
        }
    }

    public boolean verify(String token, String kind, Long id, Long userId, Long version) {
        try {
            if (token == null) return false;
            int dot = token.lastIndexOf('.');
            if (dot <= 0 || !constantTimeEquals(token.substring(dot + 1), sign(token.substring(0, dot)))) return false;
            Map<?, ?> claims = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(token.substring(0, dot)), Map.class);
            Number expiresAt = (Number) claims.get("expiresAt");
            return kind.equals(claims.get("kind"))
                    && id.toString().equals(String.valueOf(claims.get("id")))
                    && userId.toString().equals(String.valueOf(claims.get("userId")))
                    && version.toString().equals(String.valueOf(claims.get("version")))
                    && expiresAt != null && expiresAt.longValue() >= Instant.now().getEpochSecond();
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII), right.getBytes(StandardCharsets.US_ASCII));
    }
}
