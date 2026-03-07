package com.typingquiz.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.typingquiz.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * 统计指定时间后创建的用户数
     */
    long countByCreatedAtAfter(LocalDateTime createdAt);

    /**
     * 获取所有用户，按注册时间倒序排列
     */
    List<User> findAllByOrderByCreatedAtDesc();
}
