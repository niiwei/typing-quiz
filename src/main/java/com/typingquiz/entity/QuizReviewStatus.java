package com.typingquiz.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    @Transient
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(QuizReviewStatus.class);

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
     * 下次复习日期时间（精确到秒，用于学习和复习阶段）
     */
    @Column(name = "next_review_date", columnDefinition = "DATETIME(6)")
    private LocalDateTime nextReviewDate;

    /**
     * 上次复习日期时间
     */
    @Column(name = "last_review_date", columnDefinition = "DATETIME(6)")
    private LocalDateTime lastReviewDate;

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
     * 搁置截止日期时间（临时推迟复习）
     * null表示未搁置
     */
    @Column(name = "buried_until", columnDefinition = "DATETIME(6)")
    private LocalDateTime buriedUntil;

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
     * 检查卡片是否已被搁置到当前时间之后
     */
    public boolean isBuried() {
        if (buriedUntil == null) {
            return false;
        }
        return buriedUntil.isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
    }

    /**
     * 检查卡片今天是否需要复习
     */
    public boolean isDueForReview() {
        if (status != ReviewStatus.REVIEW || isBuried() || status == ReviewStatus.SUSPENDED) {
            return false;
        }
        return nextReviewDate != null && !nextReviewDate.isAfter(LocalDateTime.now());
    }

    /**
     * 核心准入判定：检查该测验目前是否对用户“可访问”
     */
    public boolean isUserAccessible() {
        if (status == ReviewStatus.SUSPENDED) {
            return false;
        }
        
        // 1. 检查是否被搁置
        if (isBuried()) {
            return false;
        }

        // 2. 检查脏数据屏蔽
        if (intervalDays != null && intervalDays >= 36500) {
            return false;
        }

        // 3. 状态判定
        if (status == ReviewStatus.NEW) {
            return true;
        }

        // 4. 时间判定（增加安全缓冲垫）
        // 强制使用北京时间，并要求当前时间必须超过下次复习时间至少 1 秒
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        
        // 特殊防御：如果下次复习时间刚好是凌晨 0 点且处于学习阶段，通常意味着数据截断损坏
        if (status == ReviewStatus.LEARNING && nextReviewDate != null && 
            nextReviewDate.getHour() == 0 && nextReviewDate.getMinute() == 0 && nextReviewDate.getSecond() == 0) {
            return false;
        }

        boolean accessible = nextReviewDate == null || now.isAfter(nextReviewDate.plusSeconds(1));
        
        return accessible;
    }

    /**
     * 获取当前状态的显示名称
     */
    public String getStatusDisplayName() {
        return status.getDisplayName();
    }

    /**
     * 获取复习标签（统一判断逻辑）
     * @param now 当前时间
     * @return ReviewLabel 业务层标签
     */
    public ReviewLabel getLabel(LocalDateTime now) {
        // 统一使用北京时间
        LocalDateTime beijingNow = now.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                .toLocalDateTime();
        
        if (status == ReviewStatus.SUSPENDED) {
            return ReviewLabel.SUSPENDED;
        }
        
        if (isBuried()) {
            // 搁置状态显示原状态，但不算今日到期
            return getStatusLabel(status);
        }
        
        // 判断今日是否到期
        boolean isDueToday = isDueToday(beijingNow);
        
        switch (status) {
            case NEW:
                // NEW状态算作待学习
                return ReviewLabel.PENDING_LEARN;
            case LEARNING:
                return isDueToday ? ReviewLabel.PENDING_LEARN : ReviewLabel.LEARNING;
            case REVIEW:
                return isDueToday ? ReviewLabel.PENDING_REVIEW : ReviewLabel.REVIEWING;
            case RELEARNING:
                return isDueToday ? ReviewLabel.PENDING_REVIEW : ReviewLabel.RELEARNING;
            default:
                return ReviewLabel.LEARNING;
        }
    }
    
    /**
     * 获取复习标签（使用当前时间）
     */
    public ReviewLabel getLabel() {
        return getLabel(LocalDateTime.now());
    }
    
    /**
     * 判断今日是否到期（统一时区）
     */
    private boolean isDueToday(LocalDateTime beijingNow) {
        if (nextReviewDate == null) {
            return true;  // NEW状态或无期日算到期
        }
        
        // 将nextReviewDate也转为北京时间比较
        LocalDateTime beijingNext = nextReviewDate.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                .toLocalDateTime();
        
        // 今日到期：日期相同 或 时间已过
        return beijingNext.toLocalDate().equals(beijingNow.toLocalDate())
                || !beijingNext.isAfter(beijingNow);
    }
    
    /**
     * 根据状态获取对应标签（非到期状态）
     */
    private ReviewLabel getStatusLabel(ReviewStatus status) {
        switch (status) {
            case NEW: return ReviewLabel.PENDING_LEARN;
            case LEARNING: return ReviewLabel.LEARNING;
            case REVIEW: return ReviewLabel.REVIEWING;
            case RELEARNING: return ReviewLabel.RELEARNING;
            default: return ReviewLabel.LEARNING;
        }
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

    public LocalDateTime getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDateTime nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public LocalDateTime getLastReviewDate() {
        return lastReviewDate;
    }

    public void setLastReviewDate(LocalDateTime lastReviewDate) {
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

    public LocalDateTime getBuriedUntil() {
        return buriedUntil;
    }

    public void setBuriedUntil(LocalDateTime buriedUntil) {
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
