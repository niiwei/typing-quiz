package com.typingquiz.repository;

import com.typingquiz.entity.QuizReviewStatus;
import com.typingquiz.entity.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
     * 查询用户今日到期复习列表
     * 条件：状态为REVIEW或RELEARNING，下次复习日期<=今天，未被搁置，未暂停
     */
    @Query("SELECT qrs FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status IN (com.typingquiz.entity.ReviewStatus.REVIEW, com.typingquiz.entity.ReviewStatus.RELEARNING) " +
           "AND (qrs.nextReviewDate IS NULL OR qrs.nextReviewDate <= :today) " +
           "AND (qrs.buriedUntil IS NULL OR qrs.buriedUntil < :today) " +
           "ORDER BY qrs.nextReviewDate ASC")
    List<QuizReviewStatus> findDueToday(@Param("userId") Long userId, @Param("today") LocalDate today);

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
     * 统计用户今日到期复习数量
     */
    @Query("SELECT COUNT(qrs) FROM QuizReviewStatus qrs " +
           "WHERE qrs.userId = :userId " +
           "AND qrs.status IN (com.typingquiz.entity.ReviewStatus.REVIEW, com.typingquiz.entity.ReviewStatus.RELEARNING) " +
           "AND (qrs.nextReviewDate IS NULL OR qrs.nextReviewDate <= :today) " +
           "AND (qrs.buriedUntil IS NULL OR qrs.buriedUntil < :today)")
    Long countDueToday(@Param("userId") Long userId, @Param("today") LocalDate today);

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
                                @Param("startDate") LocalDate startDate, 
                                @Param("endDate") LocalDate endDate);

    /**
     * 检查用户是否有指定测验的复习状态
     */
    boolean existsByQuizIdAndUserId(Long quizId, Long userId);

    /**
     * 删除指定测验的复习状态（用于测验删除时清理）
     */
    void deleteByQuizId(Long quizId);
    /**
     * 根据状态查询用户的复习状态
     */
    List<QuizReviewStatus> findByUserIdAndStatus(Long userId, ReviewStatus status);

    /**
     * 查询用户搁置的卡片
     */
    @Query("SELECT q FROM QuizReviewStatus q WHERE q.userId = ?1 AND q.buriedUntil IS NOT NULL AND q.buriedUntil > ?2")
    List<QuizReviewStatus> findBuriedCards(Long userId, LocalDate today);
