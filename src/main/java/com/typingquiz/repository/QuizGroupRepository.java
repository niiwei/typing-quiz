package com.typingquiz.repository;

import com.typingquiz.entity.QuizGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测验分组数据访问接口
 */
@Repository
public interface QuizGroupRepository extends JpaRepository<QuizGroup, Long> {

    java.util.Optional<QuizGroup> findByIdAndUserId(Long id, Long userId);

    /**
     * 根据名称查询分组
     */
    List<QuizGroup> findByNameContaining(String name);

    /**
     * 根据名称查询分组(忽略大小写)
     */
    List<QuizGroup> findByNameContainingIgnoreCase(String name);

    /**
     * 按排序顺序获取所有分组
     */
    List<QuizGroup> findAllByOrderByDisplayOrderAsc();

    /**
     * 根据名称和用户ID精确查询分组（可能返回多个，需要业务层处理）
     */
    List<QuizGroup> findByNameAndUserId(String name, Long userId);

    List<QuizGroup> findByNameIgnoreCaseAndUserId(String name, Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    /**
     * 根据用户ID查询所有分组
     */
    List<QuizGroup> findByUserIdOrderByDisplayOrderAsc(Long userId);

    /**
     * 根据用户ID和名称查询分组
     */
    java.util.Optional<QuizGroup> findByUserIdAndName(Long userId, String name);

    /**
     * 根据测验ID查询包含该测验的所有分组
     */
    List<QuizGroup> findByQuizzesId(Long quizId);

    @Query("SELECT g FROM QuizGroup g JOIN g.quizzes q WHERE q.id = :quizId AND g.userId = :userId")
    List<QuizGroup> findByQuizzesIdAndUserId(@Param("quizId") Long quizId, @Param("userId") Long userId);

    @Query("SELECT DISTINCT g FROM QuizGroup g LEFT JOIN FETCH g.quizzes WHERE g.id = :groupId AND g.userId = :userId")
    java.util.Optional<QuizGroup> findByIdAndUserIdWithQuizzes(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 根据用户ID查询所有分组（带测验关联，避免N+1）
     */
    @Query("SELECT DISTINCT g FROM QuizGroup g LEFT JOIN FETCH g.quizzes WHERE g.userId = :userId ORDER BY g.displayOrder ASC")
    List<QuizGroup> findByUserIdWithQuizzes(@Param("userId") Long userId);

    /**
     * 根据分组ID查询分组（带测验关联，避免N+1）
     */
    @Query("SELECT DISTINCT g FROM QuizGroup g LEFT JOIN FETCH g.quizzes WHERE g.id = :groupId")
    java.util.Optional<QuizGroup> findByIdWithQuizzes(@Param("groupId") Long groupId);
}
