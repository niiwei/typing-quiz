package com.typingquiz.repository;

import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 测验复习状态数据访问接口
 * 所有查询都基于 userId 进行数据隔离
 */
@Repository
public interface QuizReviewStatusRepository extends JpaRepository<QuizReviewStatus, Long> {

    /**
     * 根据用户ID和测验ID查询复习状态（确保数据隔离）
     */
    Optional<QuizReviewStatus> findByQuizIdAndUserId(Long quizId, Long userId);

    /**
     * 查询用户所有复习状态
     */
    List<QuizReviewStatus> findByUserId(Long userId);

    /**
     * 查询用户指定状态的复习记录
     */
    List<QuizReviewStatus> findByUserIdAndStatus(Long userId, ReviewStatus status);

    /**
     * 查询用户到期复习列表（包含REVIEW、RELEARNING、LEARNING，精确到时间）
     * 条件：状态为REVIEW、RELEARNING或LEARNING，下次复习日期时间<=当前时间，未被搁置
     */
    @Query("SELECT qrs FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status IN (com.typingquiz.entity.ReviewStatus.REVIEW, com.typingquiz.entity.ReviewStatus.RELEARNING, com.typingquiz.entity.ReviewStatus.LEARNING) " +
           "AND (qrs.nextReviewDate IS NULL OR qrs.nextReviewDate <= :now) " +
           "AND (qrs.buriedUntil IS NULL OR qrs.buriedUntil <= :now) " +
           "ORDER BY qrs.nextReviewDate ASC")
    List<QuizReviewStatus> findDueToday(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 查询用户新测验列表（未学习）
     */
    List<QuizReviewStatus> findByUserIdAndStatusOrderByCreatedAtAsc(Long userId, ReviewStatus status);

    /**
     * 统计用户各状态数量
     */
    @Query("SELECT qrs.status, COUNT(qrs) FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "GROUP BY qrs.status")
    List<Object[]> countByStatus(@Param("userId") Long userId);

    /**
     * 统计用户到期复习数量（包含REVIEW、RELEARNING、LEARNING，精确到时间）
     */
    @Query("SELECT COUNT(qrs) FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status IN (com.typingquiz.entity.ReviewStatus.REVIEW, com.typingquiz.entity.ReviewStatus.RELEARNING, com.typingquiz.entity.ReviewStatus.LEARNING) " +
           "AND (qrs.nextReviewDate IS NULL OR qrs.nextReviewDate <= :now) " +
           "AND (qrs.buriedUntil IS NULL OR qrs.buriedUntil <= :now)")
    Long countDueToday(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 查询用户今日到期学习中测验（精确到时间）
     */
    @Query("SELECT qrs FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status = com.typingquiz.entity.ReviewStatus.LEARNING " +
           "AND (qrs.nextReviewDate IS NULL OR qrs.nextReviewDate <= :now) " +
           "AND (qrs.buriedUntil IS NULL OR qrs.buriedUntil <= :now) " +
           "ORDER BY qrs.nextReviewDate ASC")
    List<QuizReviewStatus> findLearningDue(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 查询用户到期复习测验（不包括学习中，精确到时间）
     */
    @Query("SELECT qrs FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status IN (com.typingquiz.entity.ReviewStatus.REVIEW, com.typingquiz.entity.ReviewStatus.RELEARNING) " +
           "AND (qrs.nextReviewDate IS NULL OR qrs.nextReviewDate <= :now) " +
           "AND (qrs.buriedUntil IS NULL OR qrs.buriedUntil <= :now) " +
           "ORDER BY qrs.nextReviewDate ASC")
    List<QuizReviewStatus> findReviewDue(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 查询用户新测验（按创建时间排序）
     */
    @Query("SELECT qrs FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status = com.typingquiz.entity.ReviewStatus.NEW " +
           "ORDER BY qrs.createdAt ASC")
    List<QuizReviewStatus> findNewCards(@Param("userId") Long userId);

    /**
     * 统计用户到期学习中测验数量（精确到时间）
     */
    @Query("SELECT COUNT(qrs) FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status = com.typingquiz.entity.ReviewStatus.LEARNING " +
           "AND (qrs.nextReviewDate IS NULL OR qrs.nextReviewDate <= :now) " +
           "AND (qrs.buriedUntil IS NULL OR qrs.buriedUntil <= :now)")
    Long countLearningDue(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 统计用户新测验数量
     */
    Long countByUserIdAndStatus(Long userId, ReviewStatus status);

    /**
     * 查询未来N天内到期的复习卡片（用于预测）
     */
    @Query("SELECT qrs.nextReviewDate, COUNT(qrs) FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status = com.typingquiz.entity.ReviewStatus.REVIEW " +
           "AND qrs.nextReviewDate BETWEEN :startDate AND :endDate " +
           "GROUP BY qrs.nextReviewDate " +
           "ORDER BY qrs.nextReviewDate")
    List<Object[]> findForecast(@Param("userId") Long userId, 
                                @Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);

    /**
     * 检查用户是否有指定测验的复习状态
     */
    boolean existsByQuizIdAndUserId(Long quizId, Long userId);

    /**
     * 删除指定测验的复习状态（用于测验删除时清理）
     */
    void deleteByQuizId(Long quizId);

    /**
     * 查询用户搁置的卡片
     */
    @Query("SELECT q FROM QuizReviewStatus q WHERE q.userId = ?1 AND q.buriedUntil IS NOT NULL AND q.buriedUntil > ?2")
    List<QuizReviewStatus> findBuriedCards(Long userId, LocalDateTime now);
}
