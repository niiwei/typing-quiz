package com.typingquiz.repository;

import com.typingquiz.entity.PersonalAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, Long> {
    List<PersonalAccessToken> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PersonalAccessToken> findByPublicId(String publicId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
