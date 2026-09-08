package com.typingquiz.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日活动统计实体类
 * 用于记录每日学习统计数据（热力图和今日统计的数据源）
 */
@Entity
@Table(name = "user_daily_activity",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "activity_date"}, name = "uk_user_date")
       },
       indexes = {
           @Index(name = "idx_user_date", columnList = "user_id, activity_date")
       })
public class UserDailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID，确保数据隔离
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 活动日期
     */
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    // ========== 学习统计 ==========

    /**
     * 复习卡片数（已学习的旧卡片）
     */
    @Column(name = "review_count")
    private Integer reviewCount = 0;

    /**
     * 新学卡片数
     */
    @Column(name = "new_learned_count")
    private Integer newLearnedCount = 0;

    /**
     * 重学卡片数（遗忘后重新学习）
     */
    @Column(name = "relearning_count")
    private Integer relearningCount = 0;

    /**
     * 总耗时（秒）
     */
    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds = 0;

    // ========== 评级分布 ==========

    /**
     * 重来次数（评级1）
     */
    @Column(name = "again_count")
    private Integer againCount = 0;

    /**
     * 困难次数（评级2）
     */
    @Column(name = "hard_count")
    private Integer hardCount = 0;

    /**
     * 良好次数（评级3）
     */
    @Column(name = "good_count")
    private Integer goodCount = 0;

    /**
     * 简单次数（评级4）
     */
    @Column(name = "easy_count")
    private Integer easyCount = 0;

    // ========== 连续天数 ==========

    /**
     * 当前连续天数
     */
    @Column(name = "streak_days")
    private Integer streakDays = 0;

    /**
     * 今日是否活跃（用于热力图颜色计算）
     */
    @Column(name = "is_active")
    private Boolean isActive = false;

    // ========== 时间戳 ==========

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
    public UserDailyActivity() {
    }

    public UserDailyActivity(Long userId, LocalDate activityDate) {
        this.userId = userId;
        this.activityDate = activityDate;
        this.reviewCount = 0;
        this.newLearnedCount = 0;
        this.relearningCount = 0;
        this.totalTimeSeconds = 0;
        this.againCount = 0;
        this.hardCount = 0;
        this.goodCount = 0;
        this.easyCount = 0;
        this.streakDays = 0;
        this.isActive = false;
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
     * 获取总卡片数（复习 + 新学 + 重学）
     */
    public Integer getTotalCards() {
        int total = 0;
        if (reviewCount != null) total += reviewCount;
        if (newLearnedCount != null) total += newLearnedCount;
        if (relearningCount != null) total += relearningCount;
        return total;
    }

    /**
     * 根据评级增加计数
     */
    public void incrementRatingCount(int rating) {
        switch (rating) {
            case 1:
                this.againCount = (this.againCount == null ? 0 : this.againCount) + 1;
                break;
            case 2:
                this.hardCount = (this.hardCount == null ? 0 : this.hardCount) + 1;
                break;
            case 3:
                this.goodCount = (this.goodCount == null ? 0 : this.goodCount) + 1;
                break;
            case 4:
                this.easyCount = (this.easyCount == null ? 0 : this.easyCount) + 1;
                break;
        }
    }

    /**
     * 计算活动强度等级（0-4，用于热力图颜色）
     */
    public Integer calculateActivityLevel() {
        int total = getTotalCards();
        if (total == 0) return 0;
        if (total < 5) return 1;
        if (total < 10) return 2;
        if (total < 20) return 3;
        return 4;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getNewLearnedCount() {
        return newLearnedCount;
    }

    public void setNewLearnedCount(Integer newLearnedCount) {
        this.newLearnedCount = newLearnedCount;
    }

    public Integer getRelearningCount() {
        return relearningCount;
    }

    public void setRelearningCount(Integer relearningCount) {
        this.relearningCount = relearningCount;
    }

    public Integer getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(Integer totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public Integer getAgainCount() {
        return againCount;
    }

    public void setAgainCount(Integer againCount) {
        this.againCount = againCount;
    }

    public Integer getHardCount() {
        return hardCount;
    }

    public void setHardCount(Integer hardCount) {
        this.hardCount = hardCount;
    }

    public Integer getGoodCount() {
        return goodCount;
    }

    public void setGoodCount(Integer goodCount) {
        this.goodCount = goodCount;
    }

    public Integer getEasyCount() {
        return easyCount;
    }

    public void setEasyCount(Integer easyCount) {
        this.easyCount = easyCount;
    }

    public Integer getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(Integer streakDays) {
        this.streakDays = streakDays;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
        return "UserDailyActivity{" +
                "id=" + id +
                ", userId=" + userId +
                ", activityDate=" + activityDate +
                ", reviewCount=" + reviewCount +
                ", newLearnedCount=" + newLearnedCount +
                ", totalCards=" + getTotalCards() +
                ", streakDays=" + streakDays +
                '}';
    }
}
