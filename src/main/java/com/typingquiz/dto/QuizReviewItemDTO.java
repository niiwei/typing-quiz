package com.typingquiz.dto;

import com.typingquiz.entity.ReviewStatus;

import java.time.LocalDate;

/**
 * 测验复习项DTO
 * 用于分组展开后显示单个测验的学习状态
 */
public class QuizReviewItemDTO {
    
    private Long quizId;
    private String quizTitle;
    private String quizDescription;
    
    // 复习状态
    private ReviewStatus status;
    private String statusDisplay;
    
    // 学习阶段信息
    private Integer learningStep;
    private Integer totalLearningSteps;
    
    // 复习安排
    private LocalDate nextReviewDate;
    private Integer intervalDays;
    private Integer easeFactor;
    
    // 统计
    private Integer reviewCount;
    private Integer lapseCount;
    
    // 是否被搁置
    private boolean isBuried;
    private LocalDate buriedUntil;
    
    // 是否逾期
    private boolean isOverdue;

    public QuizReviewItemDTO() {
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public String getQuizDescription() {
        return quizDescription;
    }

    public void setQuizDescription(String quizDescription) {
        this.quizDescription = quizDescription;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
        this.statusDisplay = status != null ? status.getDisplayName() : null;
    }

    public String getStatusDisplay() {
        return statusDisplay;
    }

    public Integer getLearningStep() {
        return learningStep;
    }

    public void setLearningStep(Integer learningStep) {
        this.learningStep = learningStep;
    }

    public Integer getTotalLearningSteps() {
        return totalLearningSteps;
    }

    public void setTotalLearningSteps(Integer totalLearningSteps) {
        this.totalLearningSteps = totalLearningSteps;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
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

    public boolean isBuried() {
        return isBuried;
    }

    public void setBuried(boolean buried) {
        isBuried = buried;
    }

    public LocalDate getBuriedUntil() {
        return buriedUntil;
    }

    public void setBuriedUntil(LocalDate buriedUntil) {
        this.buriedUntil = buriedUntil;
    }

    public boolean isOverdue() {
        return isOverdue;
    }

    public void setOverdue(boolean overdue) {
        isOverdue = overdue;
    }

    @Override
    public String toString() {
        return "QuizReviewItemDTO{" +
                "quizId=" + quizId +
                ", quizTitle='" + quizTitle + '\'' +
                ", status=" + status +
                ", nextReviewDate=" + nextReviewDate +
                '}';
    }
}
