package com.typingquiz.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 测验复习状态实体类
 * 用于追踪基于Anki SM-2算法的间隔重复复习状态
 * 每个用户的每个测验只有一条记录，确保数据隔离
 */
@Entity
@Table(name = "quiz_review_status",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"quiz_id", "user_id"}, name = "uk_quiz_user")
       },
       indexes = {
           @Index(name = "idx_user_next_review", columnList = "user_id, next_review_date"),
           @Index(name = "idx_user_status", columnList = "user_id, status")
       })
public class QuizReviewStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的测验ID
     */
    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    /**
     * 用户ID，确保数据隔离
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 复习状态
     * NEW: 新测验，从未学习
     * LEARNING: 学习中，使用学习间隔
     * REVIEW: 待复习，使用SM-2算法
     * RELEARNING: 重学中，遗忘后重新学习
     * SUSPENDED: 暂停，不参与复习
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.NEW;

    /**
     * 当前间隔天数（两次复习之间的天数）
     */
    @Column(name = "interval_days")
    private Integer intervalDays = 0;

    /**
     * 简易度系数（千分比，如2500表示2.5倍）
     * 影响间隔增长倍数
     */
    @Column(name = "ease_factor")
    private Integer easeFactor = 2500;

    /**
     * 下次复习日期
     */
    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    /**
     * 上次复习日期
     */
    @Column(name = "last_review_date")
    private LocalDate lastReviewDate;

    /**
     * 累计复习次数
     */
    @Column(name = "review_count")
    private Integer reviewCount = 0;

    /**
     * 遗忘次数（重来次数）
     */
    @Column(name = "lapse_count")
    private Integer lapseCount = 0;

    /**
     * 当前学习阶段步数（0表示未开始）
     */
    @Column(name = "learning_step")
    private Integer learningStep = 0;

    /**
     * 搁置截止日期（临时推迟复习）
     * null表示未搁置
     */
    @Column(name = "buried_until")
    private LocalDate buriedUntil;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 构造函数
    public QuizReviewStatus() {
    }

    public QuizReviewStatus(Long quizId, Long userId) {
        this.quizId = quizId;
        this.userId = userId;
        this.status = ReviewStatus.NEW;
        this.easeFactor = 2500;
        this.intervalDays = 0;
        this.reviewCount = 0;
        this.lapseCount = 0;
        this.learningStep = 0;
    }

    // 生命周期回调
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 业务方法

    /**
     * 检查卡片是否已被搁置到今天之后
     */
    public boolean isBuried() {
        if (buriedUntil == null) {
            return false;
        }
        return !buriedUntil.isBefore(LocalDate.now());
    }

    /**
     * 检查卡片今天是否需要复习
     */
    public boolean isDueToday() {
        if (status != ReviewStatus.REVIEW || isBuried() || status == ReviewStatus.SUSPENDED) {
            return false;
        }
        return nextReviewDate != null && !nextReviewDate.isAfter(LocalDate.now());
    }

    /**
     * 获取当前状态的显示名称
     */
    public String getStatusDisplayName() {
        return status.getDisplayName();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Integer getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(Integer easeFactor) {
        this.easeFactor = easeFactor;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public LocalDate getLastReviewDate() {
        return lastReviewDate;
    }

    public void setLastReviewDate(LocalDate lastReviewDate) {
        this.lastReviewDate = lastReviewDate;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getLapseCount() {
        return lapseCount;
    }

    public void setLapseCount(Integer lapseCount) {
        this.lapseCount = lapseCount;
    }

    public Integer getLearningStep() {
        return learningStep;
    }

    public void setLearningStep(Integer learningStep) {
        this.learningStep = learningStep;
    }

    public LocalDate getBuriedUntil() {
        return buriedUntil;
    }

    public void setBuriedUntil(LocalDate buriedUntil) {
        this.buriedUntil = buriedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "QuizReviewStatus{" +
                "id=" + id +
                ", quizId=" + quizId +
                ", userId=" + userId +
                ", status=" + status +
                ", intervalDays=" + intervalDays +
                ", easeFactor=" + easeFactor +
                ", nextReviewDate=" + nextReviewDate +
                ", reviewCount=" + reviewCount +
                ", lapseCount=" + lapseCount +
                '}';
    }
}
