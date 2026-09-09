package com.typingquiz.service;

import com.typingquiz.dto.PersonalAccessTokenDTO;
import com.typingquiz.entity.PersonalAccessToken;
import com.typingquiz.repository.PersonalAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class PersonalAccessTokenService {
    private final PersonalAccessTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PersonalAccessTokenService(PersonalAccessTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PersonalAccessTokenDTO> list(Long userId) {
        List<PersonalAccessTokenDTO> result = new ArrayList<>();
        for (PersonalAccessToken token : repository.findByUserIdOrderByCreatedAtDesc(userId)) {
            result.add(toDTO(token, false));
        }
        return result;
    }

    @Transactional
    public PersonalAccessTokenDTO create(Long userId, String name) {
        if (userId == null) throw new IllegalArgumentException("用户身份无效");
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new IllegalArgumentException("令牌名称不能为空且不能超过100个字符");
        }
        if (repository.existsByUserIdAndNameIgnoreCase(userId, normalized)) {
            throw new IllegalArgumentException("令牌名称已存在");
        }

        String publicId = UUID.randomUUID().toString();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = "mp_pat_" + publicId + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PersonalAccessToken entity = new PersonalAccessToken();
        entity.setUserId(userId);
        entity.setName(normalized);
        entity.setPublicId(publicId);
        entity.setTokenHash(hash(token));
        return toDTO(repository.save(entity), true, token);
    }

    @Transactional
    public void revoke(Long userId, Long id) {
        PersonalAccessToken token = repository.findById(id)
                .filter(item -> userId.equals(item.getUserId()))
                .orElseThrow(() -> new RuntimeException("令牌不存在"));
        if (token.getRevokedAt() == null) token.setRevokedAt(LocalDateTime.now());
    }

    @Transactional
    public Long authenticate(String rawToken) {
        if (rawToken == null || !rawToken.startsWith("mp_pat_")) return null;
        String remainder = rawToken.substring("mp_pat_".length());
        int separator = remainder.indexOf('_');
        if (separator <= 0) return null;
        String publicId = remainder.substring(0, separator);
        PersonalAccessToken token = repository.findByPublicId(publicId).orElse(null);
        if (token == null || token.getRevokedAt() != null) return null;
        if (!MessageDigest.isEqual(token.getTokenHash().getBytes(StandardCharsets.US_ASCII), hash(rawToken).getBytes(StandardCharsets.US_ASCII))) {
            return null;
        }
        token.setLastUsedAt(LocalDateTime.now());
        return token.getUserId();
    }

    private PersonalAccessTokenDTO toDTO(PersonalAccessToken token, boolean includeToken) {
        return toDTO(token, includeToken, null);
    }

    private PersonalAccessTokenDTO toDTO(PersonalAccessToken token, boolean includeToken, String rawToken) {
        PersonalAccessTokenDTO dto = new PersonalAccessTokenDTO();
        dto.setId(token.getId());
        dto.setName(token.getName());
        dto.setPublicId(token.getPublicId());
        dto.setCreatedAt(token.getCreatedAt());
        dto.setLastUsedAt(token.getLastUsedAt());
        dto.setRevokedAt(token.getRevokedAt());
        if (includeToken) dto.setToken(rawToken);
        return dto;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
