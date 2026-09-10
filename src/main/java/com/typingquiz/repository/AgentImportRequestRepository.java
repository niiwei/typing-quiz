package com.typingquiz.repository;

import com.typingquiz.entity.AgentImportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentImportRequestRepository extends JpaRepository<AgentImportRequest, Long> {
    Optional<AgentImportRequest> findByUserIdAndRequestId(Long userId, String requestId);
}
